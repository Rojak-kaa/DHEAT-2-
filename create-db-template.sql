CREATE DATABASE IF NOT EXISTS billing;
USE billing;

CREATE TABLE bills (
    bill_id VARCHAR(10) PRIMARY KEY,
    order_id VARCAHR(10) NOT NULL,
    grand_total DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    billing_date DATE NOT NULL,
    FOREIGN KEY (order_id) REFERENCES cus_order(order_id)
);

SELECT * FROM bills;
    