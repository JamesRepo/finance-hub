-- =====================================================
-- Personal Finance Dashboard - Dummy Data
-- =====================================================
-- This file contains realistic test data for local development
-- Run this after creating the schema
-- =====================================================

-- Clear existing data (if any)
TRUNCATE TABLE debt_payments, debts, savings_goals, housing_expenses, budgets, transactions, categories, accounts, users RESTART IDENTITY CASCADE;

-- =====================================================
-- USERS
-- =====================================================
INSERT INTO users (email, password_hash, first_name, last_name, created_at) VALUES
    ('alice.johnson@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Alice', 'Johnson', '2024-01-15 10:30:00+00'),
    ('bob.smith@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Bob', 'Smith', '2024-02-20 14:20:00+00'),
    ('charlie.brown@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Charlie', 'Brown', '2024-03-10 09:15:00+00'),
    ('diana.prince@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Diana', 'Prince', '2024-04-05 16:45:00+00');

-- Note: Password hash above is for 'password123' (bcrypt) - DO NOT use in production!

-- =====================================================
-- ACCOUNTS
-- =====================================================
-- Alice's Accounts
INSERT INTO accounts (user_id, account_name, account_type, balance, currency, is_active, created_at) VALUES
    (1, 'Main Checking', 'CHECKING', 3245.67, 'GBP', true, '2024-01-15 11:00:00+00'),
    (1, 'Savings Account', 'SAVINGS', 12500.00, 'GBP', true, '2024-01-15 11:05:00+00'),
    (1, 'Emergency Fund', 'SAVINGS', 8000.00, 'GBP', true, '2024-01-20 10:00:00+00'),
    (1, 'Credit Card', 'CREDIT_CARD', -850.25, 'GBP', true, '2024-01-15 11:10:00+00');

-- Bob's Accounts
INSERT INTO accounts (user_id, account_name, account_type, balance, currency, is_active, created_at) VALUES
    (2, 'Current Account', 'CHECKING', 5670.90, 'GBP', true, '2024-02-20 15:00:00+00'),
    (2, 'Investment Account', 'INVESTMENT', 25000.00, 'GBP', true, '2024-02-20 15:10:00+00'),
    (2, 'Cash', 'CASH', 250.00, 'GBP', true, '2024-02-20 15:15:00+00');

-- Charlie's Accounts
INSERT INTO accounts (user_id, account_name, account_type, balance, currency, is_active, created_at) VALUES
    (3, 'Checking', 'CHECKING', 1890.45, 'GBP', true, '2024-03-10 10:00:00+00'),
    (3, 'Savings', 'SAVINGS', 5000.00, 'GBP', true, '2024-03-10 10:05:00+00'),
    (3, 'Old Account', 'CHECKING', 0.00, 'GBP', false, '2024-03-10 10:10:00+00');

-- Diana's Accounts
INSERT INTO accounts (user_id, account_name, account_type, balance, currency, is_active, created_at) VALUES
    (4, 'Main Account', 'CHECKING', 4320.50, 'GBP', true, '2024-04-05 17:00:00+00'),
    (4, 'Travel Savings', 'SAVINGS', 3500.00, 'GBP', true, '2024-04-05 17:05:00+00');

-- =====================================================
-- CATEGORIES (System + User-specific)
-- =====================================================
-- System Categories (available to all users)
INSERT INTO categories (user_id, category_name, category_type, color_code, is_system, created_at) VALUES
    (NULL, 'Uncategorized Expense', 'EXPENSE', '#808080', true, '2024-01-01 00:00:00+00'),
    (NULL, 'Uncategorized Income', 'INCOME', '#808080', true, '2024-01-01 00:00:00+00'),
    (NULL, 'Salary', 'INCOME', '#2ecc71', true, '2024-01-01 00:00:00+00'),
    (NULL, 'Freelance', 'INCOME', '#3498db', true, '2024-01-01 00:00:00+00'),
    (NULL, 'Investment Income', 'INCOME', '#9b59b6', true, '2024-01-01 00:00:00+00');

-- Alice's Custom Categories
INSERT INTO categories (user_id, category_name, category_type, color_code, is_system, created_at) VALUES
    (1, 'Groceries', 'EXPENSE', '#e74c3c', false, '2024-01-15 11:30:00+00'),
    (1, 'Transportation', 'EXPENSE', '#f39c12', false, '2024-01-15 11:31:00+00'),
    (1, 'Dining Out', 'EXPENSE', '#e67e22', false, '2024-01-15 11:32:00+00'),
    (1, 'Entertainment', 'EXPENSE', '#9b59b6', false, '2024-01-15 11:33:00+00'),
    (1, 'Healthcare', 'EXPENSE', '#1abc9c', false, '2024-01-15 11:34:00+00'),
    (1, 'Shopping', 'EXPENSE', '#e91e63', false, '2024-01-15 11:35:00+00'),
    (1, 'Utilities', 'EXPENSE', '#3498db', false, '2024-01-15 11:36:00+00'),
    (1, 'Bonus', 'INCOME', '#27ae60', false, '2024-01-15 11:37:00+00');

-- Bob's Custom Categories
INSERT INTO categories (user_id, category_name, category_type, color_code, is_system, created_at) VALUES
    (2, 'Groceries', 'EXPENSE', '#e74c3c', false, '2024-02-20 15:30:00+00'),
    (2, 'Petrol', 'EXPENSE', '#f39c12', false, '2024-02-20 15:31:00+00'),
    (2, 'Restaurants', 'EXPENSE', '#e67e22', false, '2024-02-20 15:32:00+00'),
    (2, 'Gym', 'EXPENSE', '#16a085', false, '2024-02-20 15:33:00+00'),
    (2, 'Subscriptions', 'EXPENSE', '#8e44ad', false, '2024-02-20 15:34:00+00'),
    (2, 'Consulting Income', 'INCOME', '#2ecc71', false, '2024-02-20 15:35:00+00');

-- Charlie's Custom Categories
INSERT INTO categories (user_id, category_name, category_type, color_code, is_system, created_at) VALUES
    (3, 'Groceries', 'EXPENSE', '#e74c3c', false, '2024-03-10 10:30:00+00'),
    (3, 'Transport', 'EXPENSE', '#f39c12', false, '2024-03-10 10:31:00+00'),
    (3, 'Coffee', 'EXPENSE', '#795548', false, '2024-03-10 10:32:00+00'),
    (3, 'Books', 'EXPENSE', '#607d8b', false, '2024-03-10 10:33:00+00');

-- Diana's Custom Categories
INSERT INTO categories (user_id, category_name, category_type, color_code, is_system, created_at) VALUES
    (4, 'Groceries', 'EXPENSE', '#e74c3c', false, '2024-04-05 17:30:00+00'),
    (4, 'Travel', 'EXPENSE', '#00bcd4', false, '2024-04-05 17:31:00+00'),
    (4, 'Fashion', 'EXPENSE', '#e91e63', false, '2024-04-05 17:32:00+00'),
    (4, 'Gifts', 'EXPENSE', '#ff5722', false, '2024-04-05 17:33:00+00');

-- =====================================================
-- TRANSACTIONS
-- =====================================================
-- Alice's Transactions (Last 3 months)
INSERT INTO transactions (account_id, category_id, transaction_type, amount, transaction_date, description, payment_method, created_at) VALUES
-- November 2024
(1, 3, 'INCOME', 3200.00, '2024-11-01', 'Monthly Salary - November', 'BANK_TRANSFER', '2024-11-01 09:00:00+00'),
(1, 6, 'EXPENSE', 145.50, '2024-11-02', 'Tesco - Weekly shop', 'DEBIT_CARD', '2024-11-02 14:30:00+00'),
(1, 7, 'EXPENSE', 65.00, '2024-11-03', 'Petrol - Shell', 'DEBIT_CARD', '2024-11-03 08:15:00+00'),
(1, 8, 'EXPENSE', 42.50, '2024-11-05', 'Pizza Express', 'CREDIT_CARD', '2024-11-05 19:30:00+00'),
(1, 9, 'EXPENSE', 89.99, '2024-11-07', 'Cinema tickets and snacks', 'DEBIT_CARD', '2024-11-07 18:00:00+00'),
(1, 6, 'EXPENSE', 132.75, '2024-11-09', 'Sainsburys - Groceries', 'DEBIT_CARD', '2024-11-09 11:20:00+00'),
(1, 10, 'EXPENSE', 45.00, '2024-11-09', 'GP consultation', 'DEBIT_CARD', '2024-11-12 10:30:00+00'),
(1, 11, 'EXPENSE', 125.00, '2024-11-14', 'New trainers - Nike', 'CREDIT_CARD', '2024-11-14 15:45:00+00'),
(1, 12, 'EXPENSE', 85.50, '2024-11-15', 'Electricity bill', 'DIRECT_DEBIT', '2024-11-15 00:00:00+00'),
(1, 6, 'EXPENSE', 156.20, '2024-11-16', 'Waitrose - Weekly shop', 'DEBIT_CARD', '2024-11-16 16:00:00+00'),
(1, 8, 'EXPENSE', 55.00, '2024-11-18', 'Dinner at local pub', 'DEBIT_CARD', '2024-11-18 20:15:00+00'),
(1, 7, 'EXPENSE', 72.00, '2024-11-20', 'Train ticket - London', 'DEBIT_CARD', '2024-11-20 07:30:00+00'),
(1, 6, 'EXPENSE', 98.45, '2024-11-23', 'Aldi - Groceries', 'DEBIT_CARD', '2024-11-23 12:00:00+00'),
(1, 9, 'EXPENSE', 15.99, '2024-11-25', 'Netflix subscription', 'CREDIT_CARD', '2024-11-25 00:00:00+00'),
(1, 8, 'EXPENSE', 38.50, '2024-11-27', 'Starbucks and lunch', 'DEBIT_CARD', '2024-11-27 13:15:00+00'),
(1, 6, 'EXPENSE', 142.30, '2024-11-30', 'Tesco - Monthly shop', 'DEBIT_CARD', '2024-11-30 10:00:00+00'),

-- October 2024
(1, 3, 'INCOME', 3200.00, '2024-10-01', 'Monthly Salary - October', 'BANK_TRANSFER', '2024-10-01 09:00:00+00'),
(1, 13, 'INCOME', 500.00, '2024-10-05', 'Performance Bonus', 'BANK_TRANSFER', '2024-10-05 09:00:00+00'),
(1, 6, 'EXPENSE', 165.80, '2024-10-03', 'Asda - Weekly shop', 'DEBIT_CARD', '2024-10-03 15:30:00+00'),
(1, 7, 'EXPENSE', 68.00, '2024-10-05', 'Petrol - BP', 'DEBIT_CARD', '2024-10-05 18:00:00+00'),
(1, 8, 'EXPENSE', 75.50, '2024-10-08', 'Birthday dinner', 'CREDIT_CARD', '2024-10-08 19:45:00+00'),
(1, 11, 'EXPENSE', 89.99, '2024-10-10', 'New jeans - Zara', 'DEBIT_CARD', '2024-10-10 14:20:00+00'),
(1, 6, 'EXPENSE', 128.90, '2024-10-12', 'Morrisons - Groceries', 'DEBIT_CARD', '2024-10-12 11:00:00+00'),
(1, 9, 'EXPENSE', 45.00, '2024-10-15', 'Theatre tickets', 'CREDIT_CARD', '2024-10-15 20:00:00+00'),
(1, 12, 'EXPENSE', 92.30, '2024-10-15', 'Gas and electric', 'DIRECT_DEBIT', '2024-10-15 00:00:00+00'),
(1, 6, 'EXPENSE', 145.60, '2024-10-19', 'Tesco - Weekly shop', 'DEBIT_CARD', '2024-10-19 16:30:00+00'),
(1, 10, 'EXPENSE', 35.00, '2024-10-22', 'Pharmacy - Prescriptions', 'DEBIT_CARD', '2024-10-22 11:45:00+00'),
(1, 7, 'EXPENSE', 85.00, '2024-10-25', 'Uber rides', 'CREDIT_CARD', '2024-10-25 23:30:00+00'),
(1, 6, 'EXPENSE', 112.45, '2024-10-26', 'Sainsburys - Groceries', 'DEBIT_CARD', '2024-10-26 13:00:00+00'),

-- September 2024
(1, 3, 'INCOME', 3200.00, '2024-09-01', 'Monthly Salary - September', 'BANK_TRANSFER', '2024-09-01 09:00:00+00'),
(1, 6, 'EXPENSE', 152.30, '2024-09-02', 'Tesco - Weekly shop', 'DEBIT_CARD', '2024-09-02 10:30:00+00'),
(1, 7, 'EXPENSE', 75.00, '2024-09-04', 'Petrol - Tesco', 'DEBIT_CARD', '2024-09-04 17:00:00+00'),
(1, 8, 'EXPENSE', 58.90, '2024-09-07', 'Italian restaurant', 'DEBIT_CARD', '2024-09-07 19:00:00+00'),
(1, 6, 'EXPENSE', 138.75, '2024-09-09', 'Asda - Groceries', 'DEBIT_CARD', '2024-09-09 14:15:00+00'),
(1, 11, 'EXPENSE', 65.00, '2024-09-12', 'H&M - Clothes', 'CREDIT_CARD', '2024-09-12 15:30:00+00'),
(1, 12, 'EXPENSE', 88.20, '2024-09-15', 'Utilities bill', 'DIRECT_DEBIT', '2024-09-15 00:00:00+00'),
(1, 6, 'EXPENSE', 125.50, '2024-09-16', 'Waitrose - Weekly shop', 'DEBIT_CARD', '2024-09-16 12:00:00+00'),
(1, 9, 'EXPENSE', 32.50, '2024-09-20', 'Amazon Prime', 'CREDIT_CARD', '2024-09-20 00:00:00+00'),
(1, 7, 'EXPENSE', 45.00, '2024-09-22', 'Bus pass', 'DEBIT_CARD', '2024-09-22 08:00:00+00'),
(1, 6, 'EXPENSE', 145.80, '2024-09-23', 'Tesco - Groceries', 'DEBIT_CARD', '2024-09-23 16:45:00+00'),
(1, 8, 'EXPENSE', 48.50, '2024-09-28', 'Pub with friends', 'DEBIT_CARD', '2024-09-28 21:00:00+00'),
(1, 6, 'EXPENSE', 132.90, '2024-09-30', 'Sainsburys - Monthly shop', 'DEBIT_CARD', '2024-09-30 11:30:00+00');

-- Bob's Transactions
INSERT INTO transactions (account_id, category_id, transaction_type, amount, transaction_date, description, payment_method, created_at) VALUES
-- November 2024
(5, 3, 'INCOME', 4500.00, '2024-11-01', 'Monthly Salary', 'BANK_TRANSFER', '2024-11-01 09:00:00+00'),
(5, 19, 'INCOME', 750.00, '2024-11-15', 'Consulting Project', 'BANK_TRANSFER', '2024-11-15 10:00:00+00'),
(5, 14, 'EXPENSE', 185.50, '2024-11-03', 'Tesco - Weekly shop', 'DEBIT_CARD', '2024-11-03 13:00:00+00'),
(5, 15, 'EXPENSE', 95.00, '2024-11-05', 'Petrol - Shell', 'DEBIT_CARD', '2024-11-05 07:45:00+00'),
(5, 16, 'EXPENSE', 68.50, '2024-11-08', 'Nandos', 'DEBIT_CARD', '2024-11-08 19:30:00+00'),
(5, 17, 'EXPENSE', 55.00, '2024-11-10', 'Gym membership', 'DIRECT_DEBIT', '2024-11-10 00:00:00+00'),
(5, 18, 'EXPENSE', 45.98, '2024-11-12', 'Spotify + YouTube Premium', 'CREDIT_CARD', '2024-11-12 00:00:00+00'),
(5, 14, 'EXPENSE', 172.30, '2024-11-14', 'Sainsburys - Groceries', 'DEBIT_CARD', '2024-11-14 15:00:00+00'),
(5, 15, 'EXPENSE', 88.00, '2024-11-18', 'Petrol - BP', 'DEBIT_CARD', '2024-11-18 18:30:00+00'),
(5, 16, 'EXPENSE', 92.50, '2024-11-20', 'Restaurant dinner', 'CREDIT_CARD', '2024-11-20 20:00:00+00'),
(7, 1, 'EXPENSE', 45.00, '2024-11-22', 'Taxi', 'CASH', '2024-11-22 23:00:00+00'),
(5, 14, 'EXPENSE', 165.75, '2024-11-25', 'Waitrose - Weekly shop', 'DEBIT_CARD', '2024-11-25 12:30:00+00'),

-- October 2024
(5, 3, 'INCOME', 4500.00, '2024-10-01', 'Monthly Salary', 'BANK_TRANSFER', '2024-10-01 09:00:00+00'),
(5, 19, 'INCOME', 1200.00, '2024-10-20', 'Consulting Project', 'BANK_TRANSFER', '2024-10-20 14:00:00+00'),
(5, 14, 'EXPENSE', 195.60, '2024-10-04', 'Asda - Weekly shop', 'DEBIT_CARD', '2024-10-04 16:00:00+00'),
(5, 15, 'EXPENSE', 102.00, '2024-10-07', 'Petrol - Tesco', 'DEBIT_CARD', '2024-10-07 19:00:00+00'),
(5, 16, 'EXPENSE', 85.00, '2024-10-10', 'Pizza and drinks', 'DEBIT_CARD', '2024-10-10 20:30:00+00'),
(5, 17, 'EXPENSE', 55.00, '2024-10-10', 'Gym membership', 'DIRECT_DEBIT', '2024-10-10 00:00:00+00'),
(5, 14, 'EXPENSE', 178.45, '2024-10-15', 'Morrisons - Groceries', 'DEBIT_CARD', '2024-10-15 14:30:00+00'),
(5, 18, 'EXPENSE', 45.98, '2024-10-15', 'Streaming services', 'CREDIT_CARD', '2024-10-15 00:00:00+00'),
(5, 15, 'EXPENSE', 92.50, '2024-10-22', 'Petrol - Shell', 'DEBIT_CARD', '2024-10-22 08:00:00+00'),
(5, 14, 'EXPENSE', 188.90, '2024-10-26', 'Tesco - Weekly shop', 'DEBIT_CARD', '2024-10-26 11:00:00+00');

-- Charlie's Transactions
INSERT INTO transactions (account_id, category_id, transaction_type, amount, transaction_date, description, payment_method, created_at) VALUES
-- November 2024
(8, 3, 'INCOME', 2800.00, '2024-11-01', 'Monthly Salary', 'BANK_TRANSFER', '2024-11-01 09:00:00+00'),
(8, 20, 'EXPENSE', 95.60, '2024-11-02', 'Lidl - Weekly shop', 'DEBIT_CARD', '2024-11-02 10:00:00+00'),
(8, 21, 'EXPENSE', 125.00, '2024-11-05', 'Monthly travel card', 'DEBIT_CARD', '2024-11-05 08:30:00+00'),
(8, 22, 'EXPENSE', 12.50, '2024-11-07', 'Costa Coffee', 'DEBIT_CARD', '2024-11-07 09:00:00+00'),
(8, 23, 'EXPENSE', 28.99, '2024-11-10', 'Programming book - Amazon', 'CREDIT_CARD', '2024-11-10 20:00:00+00'),
(8, 20, 'EXPENSE', 88.75, '2024-11-12', 'Aldi - Groceries', 'DEBIT_CARD', '2024-11-12 17:00:00+00'),
(8, 22, 'EXPENSE', 15.00, '2024-11-15', 'Cafe breakfast', 'DEBIT_CARD', '2024-11-15 08:45:00+00'),
(8, 20, 'EXPENSE', 102.30, '2024-11-19', 'Tesco - Weekly shop', 'DEBIT_CARD', '2024-11-19 13:30:00+00'),
(8, 22, 'EXPENSE', 18.50, '2024-11-22', 'Coffee and pastries', 'DEBIT_CARD', '2024-11-22 10:15:00+00'),
(8, 20, 'EXPENSE', 95.45, '2024-11-26', 'Sainsburys - Groceries', 'DEBIT_CARD', '2024-11-26 14:00:00+00'),

-- October 2024
(8, 3, 'INCOME', 2800.00, '2024-10-01', 'Monthly Salary', 'BANK_TRANSFER', '2024-10-01 09:00:00+00'),
(8, 20, 'EXPENSE', 92.80, '2024-10-03', 'Asda - Weekly shop', 'DEBIT_CARD', '2024-10-03 16:30:00+00'),
(8, 21, 'EXPENSE', 125.00, '2024-10-05', 'Monthly travel card', 'DEBIT_CARD', '2024-10-05 08:00:00+00'),
(8, 22, 'EXPENSE', 22.00, '2024-10-08', 'Starbucks', 'DEBIT_CARD', '2024-10-08 09:30:00+00'),
(8, 20, 'EXPENSE', 105.50, '2024-10-14', 'Morrisons - Groceries', 'DEBIT_CARD', '2024-10-14 12:00:00+00'),
(8, 23, 'EXPENSE', 45.00, '2024-10-18', 'Technical books bundle', 'CREDIT_CARD', '2024-10-18 19:00:00+00'),
(8, 20, 'EXPENSE', 98.20, '2024-10-22', 'Lidl - Weekly shop', 'DEBIT_CARD', '2024-10-22 15:30:00+00'),
(8, 22, 'EXPENSE', 16.50, '2024-10-25', 'Coffee shop', 'DEBIT_CARD', '2024-10-25 11:00:00+00'),
(8, 20, 'EXPENSE', 88.90, '2024-10-29', 'Aldi - Groceries', 'DEBIT_CARD', '2024-10-29 18:00:00+00');

-- Diana's Transactions
INSERT INTO transactions (account_id, category_id, transaction_type, amount, transaction_date, description, payment_method, created_at) VALUES
-- November 2024
(11, 3, 'INCOME', 3800.00, '2024-11-01', 'Monthly Salary', 'BANK_TRANSFER', '2024-11-01 09:00:00+00'),
(11, 24, 'EXPENSE', 125.80, '2024-11-03', 'Waitrose - Weekly shop', 'DEBIT_CARD', '2024-11-03 11:00:00+00'),
(11, 25, 'EXPENSE', 450.00, '2024-11-05', 'Flight booking - Paris', 'CREDIT_CARD', '2024-11-05 20:00:00+00'),
(11, 26, 'EXPENSE', 189.99, '2024-11-08', 'New dress - Zara', 'CREDIT_CARD', '2024-11-08 15:30:00+00'),
(11, 27, 'EXPENSE', 85.00, '2024-11-12', 'Birthday gift for friend', 'DEBIT_CARD', '2024-11-12 16:00:00+00'),
(11, 24, 'EXPENSE', 142.50, '2024-11-15', 'Sainsburys - Groceries', 'DEBIT_CARD', '2024-11-15 13:00:00+00'),
(11, 25, 'EXPENSE', 220.00, '2024-11-18', 'Hotel booking - Edinburgh', 'CREDIT_CARD', '2024-11-18 21:30:00+00'),
(11, 26, 'EXPENSE', 95.00, '2024-11-20', 'Shoes - Office', 'DEBIT_CARD', '2024-11-20 14:45:00+00'),
(11, 24, 'EXPENSE', 135.70, '2024-11-24', 'Tesco - Weekly shop', 'DEBIT_CARD', '2024-11-24 12:00:00+00'),
(11, 27, 'EXPENSE', 65.00, '2024-11-28', 'Secret Santa gift', 'DEBIT_CARD', '2024-11-28 17:00:00+00'),

-- October 2024
(11, 3, 'INCOME', 3800.00, '2024-10-01', 'Monthly Salary', 'BANK_TRANSFER', '2024-10-01 09:00:00+00'),
(11, 24, 'EXPENSE', 152.30, '2024-10-04', 'M&S - Weekly shop', 'DEBIT_CARD', '2024-10-04 14:00:00+00'),
(11, 25, 'EXPENSE', 380.00, '2024-10-08', 'Weekend break hotel', 'CREDIT_CARD', '2024-10-08 19:00:00+00'),
(11, 26, 'EXPENSE', 145.00, '2024-10-12', 'New coat - John Lewis', 'CREDIT_CARD', '2024-10-12 16:30:00+00'),
(11, 24, 'EXPENSE', 138.90, '2024-10-16', 'Waitrose - Groceries', 'DEBIT_CARD', '2024-10-16 11:30:00+00'),
(11, 27, 'EXPENSE', 120.00, '2024-10-20', 'Wedding gift', 'DEBIT_CARD', '2024-10-20 15:00:00+00'),
(11, 26, 'EXPENSE', 78.50, '2024-10-24', 'Accessories - Accessorize', 'DEBIT_CARD', '2024-10-24 13:45:00+00'),
(11, 24, 'EXPENSE', 145.60, '2024-10-28', 'Sainsburys - Weekly shop', 'DEBIT_CARD', '2024-10-28 10:00:00+00');

-- =====================================================
-- BUDGETS
-- =====================================================
-- Alice's Budgets (Monthly for November 2024)
INSERT INTO budgets (user_id, category_id, amount, period_type, start_date, end_date, created_at) VALUES
    (1, 6, 600.00, 'MONTHLY', '2024-11-01', '2024-11-30', '2024-11-01 08:00:00+00'),
    (1, 7, 200.00, 'MONTHLY', '2024-11-01', '2024-11-30', '2024-11-01 08:05:00+00'),
    (1, 8, 300.00, 'MONTHLY', '2024-11-01', '2024-11-30', '2024-11-01 08:10:00+00'),
    (1, 9, 150.00, 'MONTHLY', '2024-11-01', '2024-11-30', '2024-11-01 08:15:00+00'),
    (1, 11, 200.00, 'MONTHLY', '2024-11-01', '2024-11-30', '2024-11-01 08:20:00+00'),
    (1, 12, 150.00, 'MONTHLY', '2024-11-01', '2024-11-30', '2024-11-01 08:25:00+00');

-- Bob's Budgets (Monthly for November 2024)
INSERT INTO budgets (user_id, category_id, amount, period_type, start_date, end_date, created_at) VALUES
    (2, 14, 750.00, 'MONTHLY', '2024-11-01', '2024-11-30', '2024-11-01 08:00:00+00'),
    (2, 15, 300.00, 'MONTHLY', '2024-11-01', '2024-11-30', '2024-11-01 08:05:00+00'),
    (2, 16, 400.00, 'MONTHLY', '2024-11-01', '2024-11-30', '2024-11-01 08:10:00+00'),
    (2, 17, 60.00, 'MONTHLY', '2024-11-01', '2024-11-30', '2024-11-01 08:15:00+00');

-- Charlie's Budgets (Monthly for November 2024)
INSERT INTO budgets (user_id, category_id, amount, period_type, start_date, end_date, created_at) VALUES
    (3, 20, 400.00, 'MONTHLY', '2024-11-01', '2024-11-30', '2024-11-01 08:00:00+00'),
    (3, 21, 150.00, 'MONTHLY', '2024-11-01', '2024-11-30', '2024-11-01 08:05:00+00'),
    (3, 22, 100.00, 'MONTHLY', '2024-11-01', '2024-11-30', '2024-11-01 08:10:00+00');

-- Diana's Budgets (Monthly for November 2024)
INSERT INTO budgets (user_id, category_id, amount, period_type, start_date, end_date, created_at) VALUES
    (4, 24, 500.00, 'MONTHLY', '2024-11-01', '2024-11-30', '2024-11-01 08:00:00+00'),
    (4, 25, 800.00, 'MONTHLY', '2024-11-01', '2024-11-30', '2024-11-01 08:05:00+00'),
    (4, 26, 300.00, 'MONTHLY', '2024-11-01', '2024-11-30', '2024-11-01 08:10:00+00'),
    (4, 27, 200.00, 'MONTHLY', '2024-11-01', '2024-11-30', '2024-11-01 08:15:00+00');

-- =====================================================
-- DEBTS
-- =====================================================
-- Alice's Debts
INSERT INTO debts (user_id, debt_name, debt_type, principal_amount, current_balance, interest_rate, start_date, target_payoff_date, minimum_payment, created_at) VALUES
    (1, 'Credit Card Balance', 'CREDIT_CARD', 2000.00, 850.25, 18.90, '2023-06-01', '2025-12-31', 50.00, '2024-01-15 12:00:00+00'),
    (1, 'Car Loan', 'AUTO_LOAN', 15000.00, 8500.00, 4.50, '2022-03-15', '2027-03-15', 285.00, '2024-01-15 12:05:00+00');

-- Bob's Debts
INSERT INTO debts (user_id, debt_name, debt_type, principal_amount, current_balance, interest_rate, start_date, target_payoff_date, minimum_payment, created_at) VALUES
    (2, 'Student Loan', 'STUDENT_LOAN', 35000.00, 28500.00, 2.75, '2018-09-01', '2033-09-01', 195.00, '2024-02-20 16:00:00+00');

-- Charlie's Debts
INSERT INTO debts (user_id, debt_name, debt_type, principal_amount, current_balance, interest_rate, start_date, target_payoff_date, minimum_payment, created_at) VALUES
    (3, 'Personal Loan', 'PERSONAL_LOAN', 5000.00, 3200.00, 7.90, '2023-01-10', '2026-01-10', 145.00, '2024-03-10 11:00:00+00');

-- Diana's Debts
INSERT INTO debts (user_id, debt_name, debt_type, principal_amount, current_balance, interest_rate, start_date, target_payoff_date, minimum_payment, created_at) VALUES
    (4, 'Credit Card', 'CREDIT_CARD', 3500.00, 1850.00, 21.90, '2023-08-01', '2025-08-01', 100.00, '2024-04-05 18:00:00+00');

-- =====================================================
-- DEBT PAYMENTS
-- =====================================================
-- Alice's Debt Payments
INSERT INTO debt_payments (debt_id, payment_amount, principal_paid, interest_paid, payment_date, created_at) VALUES
-- Credit Card payments
(1, 150.00, 123.65, 26.35, '2024-09-15', '2024-09-15 10:00:00+00'),
(1, 150.00, 125.12, 24.88, '2024-10-15', '2024-10-15 10:00:00+00'),
(1, 150.00, 126.61, 23.39, '2024-11-15', '2024-11-15 10:00:00+00'),
-- Car Loan payments
(2, 285.00, 253.13, 31.87, '2024-09-15', '2024-09-15 10:30:00+00'),
(2, 285.00, 254.08, 30.92, '2024-10-15', '2024-10-15 10:30:00+00'),
(2, 285.00, 255.03, 29.97, '2024-11-15', '2024-11-15 10:30:00+00');

-- Bob's Debt Payments
INSERT INTO debt_payments (debt_id, payment_amount, principal_paid, interest_paid, payment_date, created_at) VALUES
    (3, 195.00, 129.75, 65.25, '2024-09-01', '2024-09-01 09:00:00+00'),
    (3, 195.00, 130.04, 64.96, '2024-10-01', '2024-10-01 09:00:00+00'),
    (3, 195.00, 130.33, 64.67, '2024-11-01', '2024-11-01 09:00:00+00');

-- Charlie's Debt Payments
INSERT INTO debt_payments (debt_id, payment_amount, principal_paid, interest_paid, payment_date, created_at) VALUES
    (4, 145.00, 123.88, 21.12, '2024-09-10', '2024-09-10 10:00:00+00'),
    (4, 145.00, 124.70, 20.30, '2024-10-10', '2024-10-10 10:00:00+00'),
    (4, 145.00, 125.52, 19.48, '2024-11-10', '2024-11-10 10:00:00+00');

-- Diana's Debt Payments
INSERT INTO debt_payments (debt_id, payment_amount, principal_paid, interest_paid, payment_date, created_at) VALUES
    (5, 200.00, 166.39, 33.61, '2024-09-01', '2024-09-01 11:00:00+00'),
    (5, 200.00, 169.42, 30.58, '2024-10-01', '2024-10-01 11:00:00+00'),
    (5, 200.00, 172.51, 27.49, '2024-11-01', '2024-11-01 11:00:00+00');

-- =====================================================
-- SAVINGS GOALS
-- =====================================================
-- Alice's Savings Goals
INSERT INTO savings_goals (user_id, goal_name, target_amount, current_amount, target_date, priority, created_at) VALUES
    (1, 'Emergency Fund', 10000.00, 8000.00, '2025-12-31', 'HIGH', '2024-01-15 12:30:00+00'),
    (1, 'Holiday to Japan', 5000.00, 2500.00, '2025-06-30', 'MEDIUM', '2024-01-15 12:35:00+00'),
    (1, 'New Laptop', 1500.00, 850.00, '2025-03-31', 'LOW', '2024-01-15 12:40:00+00');

-- Bob's Savings Goals
INSERT INTO savings_goals (user_id, goal_name, target_amount, current_amount, target_date, priority, created_at) VALUES
    (2, 'House Deposit', 50000.00, 25000.00, '2026-12-31', 'HIGH', '2024-02-20 16:30:00+00'),
    (2, 'New Car', 15000.00, 8500.00, '2025-09-30', 'MEDIUM', '2024-02-20 16:35:00+00');

-- Charlie's Savings Goals
INSERT INTO savings_goals (user_id, goal_name, target_amount, current_amount, target_date, priority, created_at) VALUES
    (3, 'Emergency Fund', 6000.00, 5000.00, '2025-06-30', 'HIGH', '2024-03-10 11:30:00+00'),
    (3, 'Masters Degree', 12000.00, 3500.00, '2026-09-01', 'HIGH', '2024-03-10 11:35:00+00');

-- Diana's Savings Goals
INSERT INTO savings_goals (user_id, goal_name, target_amount, current_amount, target_date, priority, created_at) VALUES
    (4, 'World Trip', 15000.00, 3500.00, '2026-06-30', 'HIGH', '2024-04-05 18:30:00+00'),
    (4, 'Wedding Fund', 20000.00, 8000.00, '2025-12-31', 'HIGH', '2024-04-05 18:35:00+00'),
    (4, 'Camera Equipment', 3000.00, 1200.00, '2025-08-31', 'MEDIUM', '2024-04-05 18:40:00+00');

-- =====================================================
-- HOUSING EXPENSES
-- =====================================================
-- Alice's Housing Expenses
INSERT INTO housing_expenses (user_id, expense_type, amount, frequency, start_date, end_date, is_active, created_at) VALUES
    (1, 'RENT', 1200.00, 'MONTHLY', '2024-01-01', NULL, true, '2024-01-15 13:00:00+00'),
    (1, 'UTILITIES', 150.00, 'MONTHLY', '2024-01-01', NULL, true, '2024-01-15 13:05:00+00'),
    (1, 'INSURANCE', 35.00, 'MONTHLY', '2024-01-01', NULL, true, '2024-01-15 13:10:00+00'),
    (1, 'MAINTENANCE', 50.00, 'MONTHLY', '2024-01-01', NULL, true, '2024-01-15 13:15:00+00');

-- Bob's Housing Expenses
INSERT INTO housing_expenses (user_id, expense_type, amount, frequency, start_date, end_date, is_active, created_at) VALUES
    (2, 'MORTGAGE', 1850.00, 'MONTHLY', '2020-06-01', NULL, true, '2024-02-20 17:00:00+00'),
    (2, 'PROPERTY_TAX', 250.00, 'MONTHLY', '2020-06-01', NULL, true, '2024-02-20 17:05:00+00'),
    (2, 'INSURANCE', 85.00, 'MONTHLY', '2020-06-01', NULL, true, '2024-02-20 17:10:00+00'),
    (2, 'UTILITIES', 180.00, 'MONTHLY', '2020-06-01', NULL, true, '2024-02-20 17:15:00+00'),
    (2, 'MAINTENANCE', 100.00, 'MONTHLY', '2020-06-01', NULL, true, '2024-02-20 17:20:00+00');

-- Charlie's Housing Expenses
INSERT INTO housing_expenses (user_id, expense_type, amount, frequency, start_date, end_date, is_active, created_at) VALUES
    (3, 'RENT', 850.00, 'MONTHLY', '2024-03-01', NULL, true, '2024-03-10 12:00:00+00'),
    (3, 'UTILITIES', 95.00, 'MONTHLY', '2024-03-01', NULL, true, '2024-03-10 12:05:00+00'),
    (3, 'INSURANCE', 25.00, 'MONTHLY', '2024-03-01', NULL, true, '2024-03-10 12:10:00+00');

-- Diana's Housing Expenses
INSERT INTO housing_expenses (user_id, expense_type, amount, frequency, start_date, end_date, is_active, created_at) VALUES
    (4, 'RENT', 1400.00, 'MONTHLY', '2024-04-01', NULL, true, '2024-04-05 19:00:00+00'),
    (4, 'UTILITIES', 165.00, 'MONTHLY', '2024-04-01', NULL, true, '2024-04-05 19:05:00+00'),
    (4, 'INSURANCE', 45.00, 'MONTHLY', '2024-04-01', NULL, true, '2024-04-05 19:10:00+00');

