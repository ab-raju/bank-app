# 🏦 Bank Of Bharat — Simple Full Stack Bank App
HTML + CSS + Java (JDBC) + MySQL | No Tomcat, No Servlets, No JSP

---

## 📁 Structure
```
BankApp/
├── .vscode/
│   └── settings.json     → Java source paths & referenced libraries
│
├── frontend/
│   ├── index.html       → Login
│   ├── register.html    → Open Account
│   ├── dashboard.html   → Banking Dashboard
│   ├── admin.html       → Admin Panel
│   ├── style.css        → All styles
│   └── app.js           → API & session helpers
│
├── backend/
│   ├── DBConnection.java → MySQL connection
│   ├── BankService.java  → All business logic + JDBC
│   └── Server.java       → Built-in Java HTTP server (main)
│
├── sql/
│   └── schema.sql        → DB setup
│
└── lib/
    └── mysql-connector-j-9.6.0.jar  ← Add this
```

---

## ⚙️ Setup (3 Steps Only)

### Step 1 — Setup Database
Open **MySQL Workbench**:
1. `File → Open SQL Script...` → select `sql/schema.sql`
2. Click the ⚡ (execute script) icon, or press `Ctrl+Shift+Enter`
3. Refresh the Schemas panel — you should see `bankdb` with 4 tables: `customers`, `accounts`, `transactions`, `delete_requests`

Or from the command line:
```bash
mysql -u root -p < sql/schema.sql
```

### Step 2 — Configure Password
Open `backend/DBConnection.java` and set your MySQL password (or use an environment variable `DB_PASS` instead of hardcoding it):
```java
private static final String PASS = System.getenv("DB_PASS") != null
    ? System.getenv("DB_PASS") : "your_password";  // ← your MySQL password
```

### Step 3 — Add MySQL JAR
Download: https://dev.mysql.com/downloads/connector/j/
→ Select "Platform Independent" → Download ZIP
→ Extract → copy `mysql-connector-j-9.6.0.jar` → paste into `BankApp/lib/`

---

## ▶️ Run the App

### Option A — VS Code
1. Install the **Extension Pack for Java** (Microsoft) from the Extensions marketplace.
2. `File → Open Folder` → select the `BankApp` root folder.
3. Wait for VS Code to load the Java project (uses `.vscode/settings.json` for source paths & the MySQL jar).
4. Open `backend/Server.java` and click the **▶ Run** link above `main()`.

### Option B — Terminal

Open a terminal inside the `BankApp/` folder:

**Windows:**
```bash
mkdir out
javac -cp "lib\mysql-connector-j-9.6.0.jar" -d out backend\DBConnection.java backend\BankService.java backend\Server.java
java -cp "out;lib\mysql-connector-j-9.6.0.jar" Server
```

**Mac / Linux:**
```bash
mkdir out
javac -cp "lib/mysql-connector-j-9.6.0.jar" -d out backend/DBConnection.java backend/BankService.java backend/Server.java
java -cp "out:lib/mysql-connector-j-9.6.0.jar" Server
```

You'll see:
```
====================================
  Bank App running!
  Open: http://localhost:8080
====================================
```

Open your browser → http://localhost:8080

---

## 🔐 Admin Panel
- URL: http://localhost:8080/admin.html
- Password: `admin123`
- Change in `backend/BankService.java` → `ADMIN_PASS`

---

## 🌟 Features
| Feature | Page |
|---------|------|
| Create Account | register.html |
| Login | index.html |
| Check Balance | dashboard → Overview |
| Deposit | dashboard → Deposit |
| Withdraw | dashboard → Withdraw |
| Fund Transfer | dashboard → Transfer |
| Mini Statement | dashboard → Overview |
| Full Statement | dashboard → Statement |
| Change PIN | dashboard → Settings |
| Request Account Deletion | dashboard → Delete Account |
| View All Accounts | admin.html |
| Freeze / Unfreeze | admin.html |
| View All Transactions | admin.html |
| Approve Delete Requests | admin.html → Delete Requests |

---

## 🛑 Common Errors

| Error | Fix |
|-------|-----|
| `DB Connection Failed` | Wrong password or MySQL not running |
| `Cannot reach server` | Java server not started — run the compile/run commands |
| `ClassNotFoundException` | JAR file missing from `lib/` folder, or not on the classpath |
| Port 8080 in use | Change port in `Server.java` line 1: `new InetSocketAddress(8080)` |
| VS Code shows red squiggly errors on imports | Run `Java: Clean Java Language Server Workspace` from the Command Palette and reload |