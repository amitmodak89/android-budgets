CREATE TABLE IF NOT EXISTS accounts(
  _id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT COLLATE LOCALIZED,
  amount REAL NOT NULL DEFAULT 0.00,
  spend REAL NOT NULL DEFAULT 0.00,
  income REAL NOT NULL DEFAULT 0.00,
  rollover_flag INTEGER NOT NULL DEFAULT 0,
  start_date INTEGER
);

CREATE TABLE IF NOT EXISTS transactions(
  _id INTEGER PRIMARY KEY AUTOINCREMENT,
  account_id INTEGER,
  title TEXT COLLATE LOCALIZED,
  amount REAL NOT NULL DEFAULT 0.00,
  archive_flag INTEGER NOT NULL DEFAULT 0,
  create_date INTEGER
);

CREATE INDEX IF NOT EXISTS transactions_account_id ON transactions(account_id);

CREATE TRIGGER IF NOT EXISTS accounts_update_start_date AFTER UPDATE OF start_date ON accounts BEGIN
  UPDATE transactions SET archive_flag = 0
   WHERE account_id = OLD._id AND create_date >= NEW.start_date AND OLD.start_date <> NEW.start_date;
   
  UPDATE transactions SET archive_flag = 1
   WHERE account_id = OLD._id AND create_date < NEW.start_date AND OLD.start_date <> NEW.start_date;
END

CREATE TRIGGER IF NOT EXISTS accounts_delete AFTER DELETE ON accounts BEGIN
  DELETE FROM transactions WHERE account_id = OLD._id;
END

CREATE TRIGGER IF NOT EXISTS transactions_insert INSERT ON transactions BEGIN
  UPDATE accounts SET spend = spend + NEW.amount WHERE _id = NEW.account_id AND NEW.amount <> 0;
END

CREATE TRIGGER IF NOT EXISTS transactions_update_amount UPDATE OF amount ON transactions BEGIN
  UPDATE accounts SET spend = spend - OLD.amount + NEW.amount WHERE _id = OLD.account_id AND OLD.archive_flag = 0;
END

CREATE TRIGGER IF NOT EXISTS transactions_delete DELETE ON transactions BEGIN
  UPDATE accounts SET spend = spend - OLD.amount WHERE _id = OLD.account_id AND OLD.amount <> 0 AND OLD.archive_flag = 0;
END
