CREATE DATABASE IF NOT EXISTS bankdb;
USE bankdb;

CREATE TABLE IF NOT EXISTS customers (
    customer_id INT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    phone       VARCHAR(15),
    email       VARCHAR(100),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS accounts (
    account_no   BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id  INT,
    account_type ENUM('SAVINGS','CURRENT') DEFAULT 'SAVINGS',
    balance      DECIMAL(15,2) DEFAULT 0.00,
    pin          VARCHAR(6) NOT NULL,
    status       ENUM('ACTIVE','FROZEN') DEFAULT 'ACTIVE',
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY  (customer_id) REFERENCES customers(customer_id)
) AUTO_INCREMENT = 100001;

CREATE TABLE IF NOT EXISTS transactions (
    txn_id        INT PRIMARY KEY AUTO_INCREMENT,
    account_no    BIGINT,
    txn_type      ENUM('DEPOSIT','WITHDRAW','TRANSFER_DEBIT','TRANSFER_CREDIT'),
    amount        DECIMAL(15,2),
    balance_after DECIMAL(15,2),
    description   VARCHAR(255),
    txn_date      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY   (account_no) REFERENCES accounts(account_no) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS delete_requests (
    request_id    INT PRIMARY KEY AUTO_INCREMENT,
    account_no    BIGINT,
    customer_name VARCHAR(100),
    status        ENUM('PENDING','ACCEPTED') DEFAULT 'PENDING',
    requested_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    accepted_at   TIMESTAMP NULL,
    FOREIGN KEY   (account_no) REFERENCES accounts(account_no) ON DELETE CASCADE
);