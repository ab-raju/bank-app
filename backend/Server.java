import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

public class Server {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        server.createContext("/api/requestdelete",          new Handler(Server::handleRequestDelete));
        server.createContext("/api/deletestatus",           new Handler(Server::handleDeleteStatus));
        server.createContext("/api/admin/deleterequests",   new Handler(Server::handleGetDeleteRequests));
        server.createContext("/api/admin/acceptdelete",     new Handler(Server::handleAcceptDelete));

        // API routes
        server.createContext("/api/register",            new Handler(Server::handleRegister));
        server.createContext("/api/login",               new Handler(Server::handleLogin));
        server.createContext("/api/balance",             new Handler(Server::handleBalance));
        server.createContext("/api/deposit",             new Handler(Server::handleDeposit));
        server.createContext("/api/withdraw",            new Handler(Server::handleWithdraw));
        server.createContext("/api/transfer",            new Handler(Server::handleTransfer));
        server.createContext("/api/changepin",           new Handler(Server::handleChangePin));
        server.createContext("/api/ministatement",       new Handler(Server::handleMiniStatement));
        server.createContext("/api/statement",           new Handler(Server::handleStatement));
        server.createContext("/api/admin/login",         new Handler(Server::handleAdminLogin));
        server.createContext("/api/admin/accounts",      new Handler(Server::handleAllAccounts));
        server.createContext("/api/admin/updatestatus",  new Handler(Server::handleUpdateStatus));
        server.createContext("/api/admin/transactions",  new Handler(Server::handleAllTransactions));

        // Serve frontend files
        server.createContext("/", new Handler(Server::serveFile));

        server.setExecutor(null);
        server.start();

        System.out.println("====================================");
        System.out.println("  Bank App running!");
        System.out.println("  Open: http://localhost:8080");
        System.out.println("====================================");
    }

    // ═══ API HANDLERS ═══

    static void handleRegister(HttpExchange ex) throws Exception {
        String body = readBody(ex);
        Map<String,String> d = parseJson(body);
        long accNo = BankService.register(
            d.get("name"), d.get("phone"), d.get("email"),
            d.get("accountType"), Double.parseDouble(d.get("initialDeposit")), d.get("pin"));
        sendOk(ex, "\"accountNo\":" + accNo);
    }

    static void handleLogin(HttpExchange ex) throws Exception {
        Map<String,String> d = parseJson(readBody(ex));
        Map<String,String> acc = BankService.login(Long.parseLong(d.get("accountNo")), d.get("pin"));
        sendOk(ex, "\"account\":" + mapToJson(acc, "pin")); // exclude pin from response
    }

    static void handleBalance(HttpExchange ex) throws Exception {
        long accNo = Long.parseLong(queryParam(ex, "accountNo"));
        double bal = BankService.getBalance(accNo);
        sendOk(ex, "\"balance\":" + String.format("%.2f", bal));
    }

    static void handleDeposit(HttpExchange ex) throws Exception {
        Map<String,String> d = parseJson(readBody(ex));
        BankService.deposit(Long.parseLong(d.get("accountNo")), Double.parseDouble(d.get("amount")));
        sendOk(ex, null);
    }

    static void handleWithdraw(HttpExchange ex) throws Exception {
        Map<String,String> d = parseJson(readBody(ex));
        BankService.withdraw(Long.parseLong(d.get("accountNo")), Double.parseDouble(d.get("amount")), d.get("pin"));
        sendOk(ex, null);
    }

    static void handleTransfer(HttpExchange ex) throws Exception {
        Map<String,String> d = parseJson(readBody(ex));
        BankService.transfer(Long.parseLong(d.get("fromAccNo")), Long.parseLong(d.get("toAccNo")),
                             Double.parseDouble(d.get("amount")), d.get("pin"));
        sendOk(ex, null);
    }

    static void handleChangePin(HttpExchange ex) throws Exception {
        Map<String,String> d = parseJson(readBody(ex));
        BankService.changePin(Long.parseLong(d.get("accountNo")), d.get("oldPin"), d.get("newPin"), d.get("confirmPin"));
        sendOk(ex, null);
    }

    static void handleMiniStatement(HttpExchange ex) throws Exception {
        long accNo = Long.parseLong(queryParam(ex, "accountNo"));
        List<Map<String,String>> txns = BankService.miniStatement(accNo);
        sendOk(ex, "\"transactions\":" + listToJson(txns));
    }

    static void handleStatement(HttpExchange ex) throws Exception {
        String q = ex.getRequestURI().getQuery();
        long accNo = Long.parseLong(queryParam(ex, "accountNo"));
        String from = queryParam(ex, "fromDate");
        String to   = queryParam(ex, "toDate");
        List<Map<String,String>> txns = BankService.fullStatement(accNo, from, to);
        sendOk(ex, "\"transactions\":" + listToJson(txns));
    }

    static void handleAdminLogin(HttpExchange ex) throws Exception {
        Map<String,String> d = parseJson(readBody(ex));
        if (BankService.adminLogin(d.get("password"))) sendOk(ex, null);
        else throw new Exception("Incorrect admin password.");
    }

    static void handleAllAccounts(HttpExchange ex) throws Exception {
        List<Map<String,String>> list = BankService.getAllAccounts();
        sendOk(ex, "\"accounts\":" + listToJson(list));
    }

    static void handleUpdateStatus(HttpExchange ex) throws Exception {
        Map<String,String> d = parseJson(readBody(ex));
        BankService.updateStatus(Long.parseLong(d.get("accountNo")), d.get("status"));
        sendOk(ex, null);
    }

    static void handleAllTransactions(HttpExchange ex) throws Exception {
        List<Map<String,String>> txns = BankService.getAllTransactions();
        sendOk(ex, "\"transactions\":" + listToJson(txns));
    }

    // ═══ STATIC FILE SERVER ═══
    static void serveFile(HttpExchange ex) throws Exception {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";

        // Look for file relative to frontend folder
        File file = new File("frontend" + path);
        if (!file.exists()) {
            byte[] msg = "404 Not Found".getBytes();
            ex.sendResponseHeaders(404, msg.length);
            ex.getResponseBody().write(msg);
            ex.getResponseBody().close();
            return;
        }

        String mime = "text/html";
        if (path.endsWith(".css"))  mime = "text/css";
        if (path.endsWith(".js"))   mime = "application/javascript";

        byte[] bytes = Files.readAllBytes(file.toPath());
        ex.getResponseHeaders().set("Content-Type", mime);
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    // ═══ HANDLER WRAPPER ═══
    @FunctionalInterface interface ApiHandler { void handle(HttpExchange ex) throws Exception; }

    static class Handler implements HttpHandler {
        private final ApiHandler fn;
        Handler(ApiHandler fn) { this.fn = fn; }

        @Override
        public void handle(HttpExchange ex) throws IOException {
            // CORS headers
            ex.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
            ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(200, -1); return;
            }
            try {
                fn.handle(ex);
            } catch (Exception e) {
                sendError(ex, e.getMessage());
            }
        }
    }

    // ═══ RESPONSE HELPERS ═══
    static void sendOk(HttpExchange ex, String extra) throws IOException {
        String json = "{\"success\":true" + (extra != null ? "," + extra : "") + "}";
        sendJson(ex, 200, json);
    }

    static void sendError(HttpExchange ex, String msg) {
        try {
            String json = "{\"success\":false,\"message\":\"" + (msg != null ? msg.replace("\"","'") : "Error") + "\"}";
            sendJson(ex, 200, json);
        } catch (Exception ignored) {}
    }

    static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    // ═══ UTILITY HELPERS ═══
    static String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), "UTF-8");
    }

    static String queryParam(HttpExchange ex, String key) {
        String query = ex.getRequestURI().getQuery();
        if (query == null) return null;
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) return kv[1];
        }
        return null;
    }

    // Simple JSON parser (no external library)
    static Map<String,String> parseJson(String json) {
        Map<String,String> map = new LinkedHashMap<>();
        json = json.trim().replaceAll("^\\{|\\}$", "");
        // Match key:value pairs
        int i = 0;
        while (i < json.length()) {
            // skip whitespace and commas
            while (i < json.length() && (json.charAt(i) == ',' || json.charAt(i) == ' ' || json.charAt(i) == '\n' || json.charAt(i) == '\r')) i++;
            if (i >= json.length() || json.charAt(i) != '"') break;
            i++; // skip opening quote of key
            int keyStart = i;
            while (i < json.length() && json.charAt(i) != '"') i++;
            String key = json.substring(keyStart, i);
            i++; // skip closing quote of key
            while (i < json.length() && (json.charAt(i) == ':' || json.charAt(i) == ' ')) i++;
            String value;
            if (i < json.length() && json.charAt(i) == '"') {
                i++; // skip opening quote of value
                int valStart = i;
                while (i < json.length() && json.charAt(i) != '"') i++;
                value = json.substring(valStart, i);
                i++; // skip closing quote
            } else {
                int valStart = i;
                while (i < json.length() && json.charAt(i) != ',' && json.charAt(i) != '}') i++;
                value = json.substring(valStart, i).trim();
            }
            map.put(key, value);
        }
        return map;
    }

    // Map to JSON string, optionally skipping a key
    static String mapToJson(Map<String,String> map, String... skip) {
        Set<String> skipSet = new HashSet<>(Arrays.asList(skip));
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String,String> e : map.entrySet()) {
            if (skipSet.contains(e.getKey())) continue;
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    static String listToJson(List<Map<String,String>> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(mapToJson(list.get(i)));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    static void handleRequestDelete(HttpExchange ex) throws Exception {
        Map<String,String> d = parseJson(readBody(ex));
        BankService.requestDelete(Long.parseLong(d.get("accountNo")), d.get("pin"));
        sendOk(ex, null);
    }

    static void handleDeleteStatus(HttpExchange ex) throws Exception {
        long accNo = Long.parseLong(queryParam(ex, "accountNo"));
        String status = BankService.getDeleteStatus(accNo);
        sendOk(ex, "\"status\":\"" + status + "\"");
    }

    static void handleGetDeleteRequests(HttpExchange ex) throws Exception {
        List<Map<String,String>> list = BankService.getDeleteRequests();
        sendOk(ex, "\"requests\":" + listToJson(list));
    }

    static void handleAcceptDelete(HttpExchange ex) throws Exception {
        Map<String,String> d = parseJson(readBody(ex));
        BankService.acceptDelete(Integer.parseInt(d.get("requestId")));
        sendOk(ex, null);
    }
}
