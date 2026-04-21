# 🏦 NovBank — Simple Full Stack Bank App
HTML + CSS + Java (JDBC) + MySQL | No Tomcat, No Servlets, No JSP

---

## 📁 Structure
```
BankApp/
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
    └── mysql-connector-j-8.x.x.jar  ← Add this
```

---

## ⚙️ Setup (3 Steps Only)

### Step 1 — Setup Database
Open MySQL Workbench and run:
```sql
source path/to/BankApp/sql/schema.sql
```

### Step 2 — Configure Password
Open `backend/DBConnection.java`, change:
```java
private static final String PASS = "your_password";  // ← your MySQL password
```

### Step 3 — Add MySQL JAR
Download: https://dev.mysql.com/downloads/connector/j/
→ Select "Platform Independent" → Download ZIP
→ Extract → copy `mysql-connector-j-8.x.x.jar` → paste into `BankApp/lib/`

---

## ▶️ Run the App

Open terminal inside the `BankApp/` folder:

### Windows:
```bash
mkdir out
javac -cp "lib\mysql-connector-j-8.x.x.jar" -d out backend\DBConnection.java backend\BankService.java backend\Server.java
java -cp "out;lib\mysql-connector-j-8.x.x.jar" Server
```

### Mac / Linux:
```bash
mkdir out
javac -cp "lib/mysql-connector-j-8.x.x.jar" -d out backend/DBConnection.java backend/BankService.java backend/Server.java
java -cp "out:lib/mysql-connector-j-8.x.x.jar" Server
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
| View All Accounts | admin.html |
| Freeze / Unfreeze | admin.html |
| View All Transactions | admin.html |

---

## 🛑 Common Errors

| Error | Fix |
|-------|-----|
| `DB Connection Failed` | Wrong password or MySQL not running |
| `Cannot reach server` | Java server not started — run the compile/run commands |
| `ClassNotFoundException` | JAR file missing from lib/ folder |
| Port 8080 in use | Change port in `Server.java` line 1: `new InetSocketAddress(8080)` |
