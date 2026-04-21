import java.sql.*;
import java.util.*;

public class BankService {

    private static final double MIN_BAL      = 500.0;
    private static final String ADMIN_PASS   = "admin123";

    // ───── ADMIN ─────
    public static boolean adminLogin(String pass) {
        return ADMIN_PASS.equals(pass);
    }

    // ───── REGISTER ─────
    public static long register(String name, String phone, String email,
                                String type, double deposit, String pin) throws Exception {
        if (!phone.matches("\\d{10}"))   throw new Exception("Phone must be 10 digits.");
        if (!pin.matches("\\d{4,6}"))    throw new Exception("PIN must be 4-6 digits.");
        if (deposit < MIN_BAL)           throw new Exception("Minimum opening deposit is Rs." + MIN_BAL);

        try (Connection con = DBConnection.get()) {
            con.setAutoCommit(false);
            try {
                // Insert customer
                PreparedStatement ps1 = con.prepareStatement(
                    "INSERT INTO customers(name,phone,email) VALUES(?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
                ps1.setString(1, name); ps1.setString(2, phone); ps1.setString(3, email);
                ps1.executeUpdate();
                int cid = getGeneratedKey(ps1);

                // Insert account
                PreparedStatement ps2 = con.prepareStatement(
                    "INSERT INTO accounts(customer_id,account_type,balance,pin) VALUES(?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
                ps2.setInt(1, cid); ps2.setString(2, type);
                ps2.setDouble(3, deposit); ps2.setString(4, pin);
                ps2.executeUpdate();
                long accNo = getGeneratedKeyLong(ps2);

                // Opening deposit txn
                insertTxn(con, accNo, "DEPOSIT", deposit, deposit, "Account Opening Deposit");
                con.commit();
                return accNo;
            } catch (Exception e) { con.rollback(); throw e; }
        }
    }

    // ───── LOGIN ─────
    public static Map<String,String> login(long accNo, String pin) throws Exception {
        Map<String,String> acc = getAccount(accNo);
        if (acc == null)                          throw new Exception("Account not found.");
        if (!acc.get("pin").equals(pin))          throw new Exception("Incorrect PIN.");
        if (!"ACTIVE".equals(acc.get("status")))  throw new Exception("Account is " + acc.get("status") + ". Contact admin.");
        return acc;
    }

    // ───── BALANCE ─────
    public static double getBalance(long accNo) throws Exception {
        Map<String,String> acc = getAccount(accNo);
        if (acc == null) throw new Exception("Account not found.");
        return Double.parseDouble(acc.get("balance"));
    }

    // ───── DEPOSIT ─────
    public static void deposit(long accNo, double amount) throws Exception {
        if (amount <= 0) throw new Exception("Amount must be greater than 0.");
        Map<String,String> acc = getAccount(accNo);
        if (acc == null) throw new Exception("Account not found.");
        double newBal = Double.parseDouble(acc.get("balance")) + amount;
        try (Connection con = DBConnection.get()) {
            updateBalance(con, accNo, newBal);
            insertTxn(con, accNo, "DEPOSIT", amount, newBal, "Cash Deposit");
        }
    }

    // ───── WITHDRAW ─────
    public static void withdraw(long accNo, double amount, String pin) throws Exception {
        if (amount <= 0) throw new Exception("Amount must be greater than 0.");
        Map<String,String> acc = getAccount(accNo);
        if (acc == null)                         throw new Exception("Account not found.");
        if (!acc.get("pin").equals(pin))         throw new Exception("Incorrect PIN.");
        double bal = Double.parseDouble(acc.get("balance"));
        if (bal < amount)                        throw new Exception("Insufficient balance.");
        if ((bal - amount) < MIN_BAL)            throw new Exception("Must keep minimum balance of Rs." + MIN_BAL);
        double newBal = bal - amount;
        try (Connection con = DBConnection.get()) {
            updateBalance(con, accNo, newBal);
            insertTxn(con, accNo, "WITHDRAW", amount, newBal, "Cash Withdrawal");
        }
    }

    // ───── TRANSFER ─────
    public static void transfer(long from, long to, double amount, String pin) throws Exception {
        if (from == to)  throw new Exception("Cannot transfer to same account.");
        if (amount <= 0) throw new Exception("Amount must be greater than 0.");
        Map<String,String> sender   = getAccount(from);
        Map<String,String> receiver = getAccount(to);
        if (sender == null)                        throw new Exception("Your account not found.");
        if (receiver == null)                      throw new Exception("Receiver account not found.");
        if (!sender.get("pin").equals(pin))        throw new Exception("Incorrect PIN.");
        double senderBal = Double.parseDouble(sender.get("balance"));
        if (senderBal < amount)                    throw new Exception("Insufficient balance.");
        if ((senderBal - amount) < MIN_BAL)        throw new Exception("Must keep minimum balance of Rs." + MIN_BAL);

        double senderNew   = senderBal - amount;
        double receiverNew = Double.parseDouble(receiver.get("balance")) + amount;

        try (Connection con = DBConnection.get()) {
            con.setAutoCommit(false);
            try {
                updateBalance(con, from, senderNew);
                updateBalance(con, to,   receiverNew);
                insertTxn(con, from, "TRANSFER_DEBIT",  amount, senderNew,   "Transfer to Acc#" + to);
                insertTxn(con, to,   "TRANSFER_CREDIT", amount, receiverNew, "Transfer from Acc#" + from);
                con.commit();
            } catch (Exception e) { con.rollback(); throw e; }
        }
    }

    // ───── CHANGE PIN ─────
    public static void changePin(long accNo, String oldPin, String newPin, String confirm) throws Exception {
        Map<String,String> acc = getAccount(accNo);
        if (acc == null)                    throw new Exception("Account not found.");
        if (!acc.get("pin").equals(oldPin)) throw new Exception("Old PIN is incorrect.");
        if (!newPin.equals(confirm))        throw new Exception("New PINs do not match.");
        if (!newPin.matches("\\d{4,6}"))    throw new Exception("PIN must be 4-6 digits.");
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement("UPDATE accounts SET pin=? WHERE account_no=?")) {
            ps.setString(1, newPin); ps.setLong(2, accNo); ps.executeUpdate();
        }
    }

    // ───── MINI STATEMENT ─────
    public static List<Map<String,String>> miniStatement(long accNo) throws Exception {
        return getTransactions(accNo, null, null, 5);
    }

    // ───── FULL STATEMENT ─────
    public static List<Map<String,String>> fullStatement(long accNo, String from, String to) throws Exception {
        return getTransactions(accNo, from, to, -1);
    }

    // ───── ADMIN: ALL ACCOUNTS ─────
    public static List<Map<String,String>> getAllAccounts() throws Exception {
        List<Map<String,String>> list = new ArrayList<>();
        String sql = "SELECT a.*,c.name FROM accounts a JOIN customers c ON a.customer_id=c.customer_id ORDER BY a.account_no";
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String,String> m = new LinkedHashMap<>();
                m.put("accountNo",   rs.getString("account_no"));
                m.put("name",        rs.getString("name"));
                m.put("accountType", rs.getString("account_type"));
                m.put("balance",     rs.getString("balance"));
                m.put("status",      rs.getString("status"));
                list.add(m);
            }
        }
        return list;
    }

    // ───── ADMIN: UPDATE STATUS ─────
    public static void updateStatus(long accNo, String status) throws Exception {
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement("UPDATE accounts SET status=? WHERE account_no=?")) {
            ps.setString(1, status); ps.setLong(2, accNo); ps.executeUpdate();
        }
    }

    // ───── ADMIN: ALL TRANSACTIONS ─────
    public static List<Map<String,String>> getAllTransactions() throws Exception {
        List<Map<String,String>> list = new ArrayList<>();
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM transactions ORDER BY txn_date DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapTxn(rs));
        }
        return list;
    }

    // ═══════════════ PRIVATE HELPERS ═══════════════

    private static Map<String,String> getAccount(long accNo) throws Exception {
        String sql = "SELECT a.*,c.name FROM accounts a JOIN customers c ON a.customer_id=c.customer_id WHERE a.account_no=?";
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, accNo);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            Map<String,String> m = new LinkedHashMap<>();
            m.put("accountNo",   rs.getString("account_no"));
            m.put("name",        rs.getString("name"));
            m.put("accountType", rs.getString("account_type"));
            m.put("balance",     rs.getString("balance"));
            m.put("pin",         rs.getString("pin"));
            m.put("status",      rs.getString("status"));
            return m;
        }
    }

    private static void updateBalance(Connection con, long accNo, double bal) throws SQLException {
        PreparedStatement ps = con.prepareStatement("UPDATE accounts SET balance=? WHERE account_no=?");
        ps.setDouble(1, bal); ps.setLong(2, accNo); ps.executeUpdate();
    }

    private static void insertTxn(Connection con, long accNo, String type,
                                   double amount, double balAfter, String desc) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
            "INSERT INTO transactions(account_no,txn_type,amount,balance_after,description) VALUES(?,?,?,?,?)");
        ps.setLong(1, accNo); ps.setString(2, type);
        ps.setDouble(3, amount); ps.setDouble(4, balAfter); ps.setString(5, desc);
        ps.executeUpdate();
    }

    private static List<Map<String,String>> getTransactions(long accNo, String from, String to, int limit) throws Exception {
        List<Map<String,String>> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM transactions WHERE account_no=?");
        if (from != null) sql.append(" AND txn_date >= ?");
        if (to   != null) sql.append(" AND txn_date <= ?");
        sql.append(" ORDER BY txn_date DESC");
        if (limit > 0) sql.append(" LIMIT ").append(limit);
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int i = 1;
            ps.setLong(i++, accNo);
            if (from != null) ps.setString(i++, from + " 00:00:00");
            if (to   != null) ps.setString(i++, to   + " 23:59:59");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapTxn(rs));
        }
        return list;
    }

    private static Map<String,String> mapTxn(ResultSet rs) throws SQLException {
        Map<String,String> m = new LinkedHashMap<>();
        m.put("txnId",       rs.getString("txn_id"));
        m.put("accountNo",   rs.getString("account_no"));
        m.put("txnType",     rs.getString("txn_type"));
        m.put("amount",      rs.getString("amount"));
        m.put("balanceAfter",rs.getString("balance_after"));
        m.put("description", rs.getString("description") != null ? rs.getString("description") : "");
        m.put("txnDate",     rs.getString("txn_date"));
        return m;
    }

    private static int getGeneratedKey(PreparedStatement ps) throws SQLException {
        ResultSet rs = ps.getGeneratedKeys(); rs.next(); return rs.getInt(1);
    }
    private static long getGeneratedKeyLong(PreparedStatement ps) throws SQLException {
        ResultSet rs = ps.getGeneratedKeys(); rs.next(); return rs.getLong(1);
    }


    // ───── REQUEST ACCOUNT DELETE ─────
    public static void requestDelete(long accNo, String pin) throws Exception {
        Map<String,String> acc = getAccount(accNo);
        if (acc == null)                throw new Exception("Account not found.");
        if (!acc.get("pin").equals(pin)) throw new Exception("Incorrect PIN.");

        // Check if request already pending
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT * FROM delete_requests WHERE account_no=? AND status='PENDING'")) {
            ps.setLong(1, accNo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) throw new Exception("Delete request already submitted. Waiting for admin approval.");
        }

        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(
                 "INSERT INTO delete_requests(account_no, customer_name) VALUES(?,?)")) {
            ps.setLong(1, accNo);
            ps.setString(2, acc.get("name"));
            ps.executeUpdate();
        }
    }

    // ───── GET DELETE STATUS (for customer) ─────
    public static String getDeleteStatus(long accNo) throws Exception {
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT status FROM delete_requests WHERE account_no=? ORDER BY requested_at DESC LIMIT 1")) {
            ps.setLong(1, accNo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("status");
        }
        return "NONE";
    }

    // ───── GET ALL DELETE REQUESTS (admin) ─────
    public static List<Map<String,String>> getDeleteRequests() throws Exception {
        List<Map<String,String>> list = new ArrayList<>();
        try (Connection con = DBConnection.get();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT * FROM delete_requests ORDER BY requested_at DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String,String> m = new LinkedHashMap<>();
                m.put("requestId",    rs.getString("request_id"));
                m.put("accountNo",    rs.getString("account_no"));
                m.put("customerName", rs.getString("customer_name"));
                m.put("status",       rs.getString("status"));
                m.put("requestedAt",  rs.getString("requested_at"));
                list.add(m);
            }
        }
        return list;
    }
        // ───── ACCEPT DELETE REQUEST (admin) ─────
    public static void acceptDelete(int requestId) throws Exception {
        long accNo;
        try (Connection con = DBConnection.get();
            PreparedStatement ps = con.prepareStatement(
                "SELECT account_no FROM delete_requests WHERE request_id=?")) {
            ps.setInt(1, requestId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new Exception("Request not found.");
            accNo = rs.getLong("account_no");
        }

        try (Connection con = DBConnection.get()) {
            con.setAutoCommit(false);
            try {
                // Step 1 — Delete transactions manually
                PreparedStatement ps1 = con.prepareStatement(
                    "DELETE FROM transactions WHERE account_no=?");
                ps1.setLong(1, accNo);
                ps1.executeUpdate();

                // Step 2 — Delete delete_requests row manually
                PreparedStatement ps2 = con.prepareStatement(
                    "DELETE FROM delete_requests WHERE account_no=?");
                ps2.setLong(1, accNo);
                ps2.executeUpdate();

                // Step 3 — Now safely delete the account
                PreparedStatement ps3 = con.prepareStatement(
                    "DELETE FROM accounts WHERE account_no=?");
                ps3.setLong(1, accNo);
                ps3.executeUpdate();

                con.commit();
            } catch (Exception e) { con.rollback(); throw e; }
        }
    }

}

