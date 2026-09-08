-- PayFlow V2 — Default fraud rules

INSERT INTO fraud_rules (code, name, description, weight, threshold_value, enabled) VALUES
('HIGH_AMOUNT', 'High value transaction', 'Transaction amount exceeds high-value threshold', 30, 2000.00, TRUE),
('NEW_DEVICE', 'New device', 'Payment initiated from a previously unseen device', 20, NULL, TRUE),
('UNUSUAL_LOCATION', 'Unusual location', 'Transaction from a high-risk or unusual location', 20, NULL, TRUE),
('VELOCITY', 'Multiple transactions within short time', 'Customer exceeded velocity threshold', 15, 5.00, TRUE),
('FIRST_MERCHANT', 'First transaction with merchant', 'Customer first payment with this merchant', 10, NULL, TRUE),
('SUSPICIOUS_IP', 'Suspicious IP', 'IP address matches known suspicious patterns', 25, NULL, TRUE);
