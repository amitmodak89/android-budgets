package com.savefon.budgets;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.savefon.budgets.Budgets.Accounts;
import com.savefon.budgets.Budgets.Transactions;

/**
 * This class helps open, create, and upgrade the database file.
 */
class DbHelper extends SQLiteOpenHelper {

    DbHelper(Context context) {
        super(context, BudgetsProvider.DATABASE_NAME, null, BudgetsProvider.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        bootstrapDb(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(BudgetsProvider.TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        if (oldVersion == 2) {
            upgradeToVersion3(db);
        }
    }

    private void bootstrapDb(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + BudgetsProvider.ACCOUNTS_TABLE_NAME + " (" + Accounts._ID
                + " INTEGER PRIMARY KEY AUTOINCREMENT," + Accounts.TITLE + " TEXT COLLATE LOCALIZED," + Accounts.AMOUNT
                + " REAL NOT NULL DEFAULT 0.00," + Accounts.SPEND + " REAL NOT NULL DEFAULT 0.00," + Accounts.INCOME
                + " REAL NOT NULL DEFAULT 0.00," + Accounts.ROLLOVER_FLAG + " INTEGER NOT NULL DEFAULT 0,"
                + Accounts.START_DATE + " INTEGER" + ");");

        db.execSQL("CREATE TABLE " + BudgetsProvider.TRANSACTIONS_TABLE_NAME + " (" + Transactions._ID
                + " INTEGER PRIMARY KEY AUTOINCREMENT," + Transactions.ACCOUNT_ID + " INTEGER," + Transactions.TITLE
                + " TEXT COLLATE LOCALIZED," + Transactions.AMOUNT + " REAL NOT NULL DEFAULT 0.00,"
                + Transactions.ARCHIVE_FLAG + " INTEGER NOT NULL DEFAULT 0," + Transactions.CREATE_DATE + " INTEGER"
                + ");");

        db.execSQL("CREATE INDEX " + BudgetsProvider.TRANSACTIONS_TABLE_NAME + "_" + Transactions.ACCOUNT_ID + " ON "
                + BudgetsProvider.TRANSACTIONS_TABLE_NAME + " ( " + Transactions.ACCOUNT_ID + " );");

        db.execSQL("CREATE TRIGGER " + BudgetsProvider.ACCOUNTS_TABLE_NAME + "_update_" + Accounts.START_DATE + " "
                + "AFTER UPDATE OF " + Accounts.START_DATE + " ON " + BudgetsProvider.ACCOUNTS_TABLE_NAME + " BEGIN "
                + "UPDATE " + BudgetsProvider.TRANSACTIONS_TABLE_NAME + " SET " + Transactions.ARCHIVE_FLAG + " = 0 "
                + "WHERE " + Transactions.ACCOUNT_ID + " = OLD." + Accounts._ID + " " + "AND "
                + Transactions.CREATE_DATE + " >= NEW." + Accounts.START_DATE + " " + "AND OLD." + Accounts.START_DATE
                + " <> NEW." + Accounts.START_DATE + "; " + "UPDATE " + BudgetsProvider.TRANSACTIONS_TABLE_NAME
                + " SET " + Transactions.ARCHIVE_FLAG + " = 1 " + "WHERE " + Transactions.ACCOUNT_ID + " = OLD."
                + Accounts._ID + " " + "AND " + Transactions.CREATE_DATE + " < NEW." + Accounts.START_DATE + " "
                + "AND OLD." + Accounts.START_DATE + " <> NEW." + Accounts.START_DATE + "; " + "END");

        db.execSQL("CREATE TRIGGER " + BudgetsProvider.ACCOUNTS_TABLE_NAME + "_delete " + "AFTER DELETE ON "
                + BudgetsProvider.ACCOUNTS_TABLE_NAME + " BEGIN " + "DELETE FROM "
                + BudgetsProvider.TRANSACTIONS_TABLE_NAME + " " + "WHERE " + Transactions.ACCOUNT_ID + " = OLD."
                + Accounts._ID + "; " + "END");

        db.execSQL("CREATE TRIGGER " + BudgetsProvider.TRANSACTIONS_TABLE_NAME + "_insert " + "INSERT ON "
                + BudgetsProvider.TRANSACTIONS_TABLE_NAME + " BEGIN " + "UPDATE " + BudgetsProvider.ACCOUNTS_TABLE_NAME
                + " SET " + Accounts.SPEND + " = " + Accounts.SPEND + " + NEW." + Transactions.AMOUNT + " " + "WHERE "
                + Accounts._ID + " = NEW." + Transactions.ACCOUNT_ID + " " + "AND NEW." + Transactions.AMOUNT
                + " <> 0; " + "END");

        db.execSQL("CREATE TRIGGER " + BudgetsProvider.TRANSACTIONS_TABLE_NAME + "_update_" + Transactions.AMOUNT + " "
                + "UPDATE OF " + Transactions.AMOUNT + " ON " + BudgetsProvider.TRANSACTIONS_TABLE_NAME + " BEGIN "
                + "UPDATE " + BudgetsProvider.ACCOUNTS_TABLE_NAME + " SET " + Accounts.SPEND + " = " + Accounts.SPEND
                + " - OLD." + Transactions.AMOUNT + " + NEW." + Transactions.AMOUNT + " " + "WHERE " + Accounts._ID
                + " = OLD." + Transactions.ACCOUNT_ID + " " + "AND OLD." + Transactions.ARCHIVE_FLAG + " = 0; " + "END");

        db.execSQL("CREATE TRIGGER " + BudgetsProvider.TRANSACTIONS_TABLE_NAME + "_delete " + "DELETE ON "
                + BudgetsProvider.TRANSACTIONS_TABLE_NAME + " BEGIN " + "UPDATE " + BudgetsProvider.ACCOUNTS_TABLE_NAME
                + " SET " + Accounts.SPEND + " = " + Accounts.SPEND + " - OLD." + Transactions.AMOUNT + " " + "WHERE "
                + Accounts._ID + " = OLD." + Transactions.ACCOUNT_ID + " " + "AND OLD." + Transactions.AMOUNT
                + " <> 0 " + "AND OLD." + Transactions.ARCHIVE_FLAG + " = 0; " + "END");
    }

    private void upgradeToVersion3(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE accounts RENAME TO accounts_backup;");
        db.execSQL("ALTER TABLE transactions RENAME TO transactions_backup;");

        db.execSQL("DROP INDEX account_id;");

        db.execSQL("DROP TRIGGER accounts_update_start_date;");
        db.execSQL("DROP TRIGGER accounts_delete;");

        db.execSQL("DROP TRIGGER transactions_insert;");
        db.execSQL("DROP TRIGGER transactions_update_amount;");
        db.execSQL("DROP TRIGGER transactions_delete;");

        bootstrapDb(db);

        db.execSQL("INSERT INTO " + BudgetsProvider.ACCOUNTS_TABLE_NAME + "(" + Accounts._ID + "," + Accounts.TITLE
                + "," + Accounts.AMOUNT + "," + Accounts.SPEND + "," + Accounts.INCOME + "," + Accounts.ROLLOVER_FLAG
                + "," + Accounts.START_DATE
                + ") SELECT _id, title, amount, spend, income, rollover_flag, start_date from accounts_backup;");

        db.execSQL("INSERT INTO " + BudgetsProvider.TRANSACTIONS_TABLE_NAME + "(" + Transactions._ID + ","
                + Transactions.ACCOUNT_ID + "," + Transactions.TITLE + "," + Transactions.AMOUNT + ","
                + Transactions.ARCHIVE_FLAG + "," + Transactions.CREATE_DATE
                + ") SELECT _id, account_id, title, amount, archive_flag, create_date from transactions_backup;");

        db.execSQL("DROP TABLE accounts_backup;");
        db.execSQL("DROP TABLE transactions_backup;");
    }
}
