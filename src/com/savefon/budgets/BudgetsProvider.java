package com.savefon.budgets;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.savefon.budgets.Budgets.Accounts;
import com.savefon.budgets.Budgets.Transactions;

/**
 * Provides access to a database of accounts. adb pull /data/data/com.savefon.budgets/databases/budgets.db /budgets.db
 */
public class BudgetsProvider extends ContentProvider {

    static final String TAG = "BudgetsProvider";

    static final String DATABASE_NAME = "budgets.db";
    static final int DATABASE_VERSION = 3;

    static final String ACCOUNTS_TABLE_NAME = "accounts";
    static final String TRANSACTIONS_TABLE_NAME = "transactions";

    private static HashMap<String, String> sAccountsProjectionMap;
    private static HashMap<String, String> sTransactionsProjectionMap;

    private static final int ACCOUNTS = 1;
    private static final int ACCOUNT_ID = 2;
    private static final int ACCOUNT_TRANSACTIONS = 3;
    private static final int TRANSACTIONS = 4;
    private static final int TRANSACTION_ID = 5;

    private static final UriMatcher sUriMatcher;

    private DbHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String orderBy = null;

        int action = sUriMatcher.match(uri);
        switch (action) {
        case ACCOUNTS:
            qb.setTables(ACCOUNTS_TABLE_NAME);
            qb.setProjectionMap(sAccountsProjectionMap);
            orderBy = Accounts.DEFAULT_SORT_ORDER;
            break;

        case ACCOUNT_ID:
            qb.setTables(ACCOUNTS_TABLE_NAME);
            qb.setProjectionMap(sAccountsProjectionMap);
            qb.appendWhere(Accounts._ID + " = " + uri.getPathSegments().get(1));
            break;

        case ACCOUNT_TRANSACTIONS:
            qb.setTables(TRANSACTIONS_TABLE_NAME);
            qb.setProjectionMap(sTransactionsProjectionMap);
            qb.appendWhere(Transactions.ACCOUNT_ID + " = " + uri.getPathSegments().get(1));
            qb.appendWhere(" AND " + Transactions.ARCHIVE_FLAG + " = 0");
            orderBy = Transactions.DEFAULT_SORT_ORDER;
            break;

        case TRANSACTIONS:
            qb.setTables(TRANSACTIONS_TABLE_NAME);
            qb.setProjectionMap(sTransactionsProjectionMap);
            qb.appendWhere(Transactions.ARCHIVE_FLAG + " = 0");
            orderBy = Transactions.DEFAULT_SORT_ORDER;
            break;

        case TRANSACTION_ID:
            qb.setTables(TRANSACTIONS_TABLE_NAME);
            qb.setProjectionMap(sTransactionsProjectionMap);
            qb.appendWhere(Transactions._ID + " = " + uri.getPathSegments().get(1));
            break;

        default:
            throw new IllegalArgumentException(TAG + " Unknown URI " + uri);
        }

        if (!TextUtils.isEmpty(sortOrder)) {
            orderBy = sortOrder;
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case ACCOUNTS:
            return Accounts.CONTENT_TYPE;

        case ACCOUNT_ID:
            return Accounts.CONTENT_ITEM_TYPE;

        case ACCOUNT_TRANSACTIONS:
        case TRANSACTIONS:
            return Transactions.CONTENT_TYPE;

        case TRANSACTION_ID:
            return Transactions.CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException(TAG + " Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        switch (sUriMatcher.match(uri)) {
        case ACCOUNTS:
        case ACCOUNT_TRANSACTIONS:
        case TRANSACTIONS:
            break;
        default:
            throw new IllegalArgumentException(TAG + " Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        Long now = System.currentTimeMillis();
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        switch (sUriMatcher.match(uri)) {
        case ACCOUNTS:
            if (values.containsKey(Accounts.TITLE) == false) {
                values.put(Accounts.TITLE, "");
            }
            if (values.containsKey(Accounts.START_DATE) == false) {
                values.put(Accounts.START_DATE, now);
            }

            long accountId = db.insert(ACCOUNTS_TABLE_NAME, Accounts.TITLE, values);
            if (accountId > 0) {
                Uri accountUri = ContentUris.withAppendedId(Accounts.CONTENT_URI, accountId);
                getContext().getContentResolver().notifyChange(accountUri, null);
                return accountUri;
            }
            break;

        case ACCOUNT_TRANSACTIONS:
            values.put(Transactions.ACCOUNT_ID, uri.getPathSegments().get(1));

        case TRANSACTIONS:
            if (values.containsKey(Transactions.CREATE_DATE) == false) {
                values.put(Transactions.CREATE_DATE, now);
            }
            if (values.containsKey(Accounts.TITLE) == false) {
                values.put(Accounts.TITLE, "");
            }

            long transactionId = db.insert(TRANSACTIONS_TABLE_NAME, Transactions.TITLE, values);
            if (transactionId > 0) {
                Uri transactionUri = ContentUris.withAppendedId(Transactions.CONTENT_URI, transactionId);
                getContext().getContentResolver().notifyChange(transactionUri, null);
                return transactionUri;
            }
            break;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case ACCOUNTS:
            count = db.delete(ACCOUNTS_TABLE_NAME, where, whereArgs);
            break;

        case ACCOUNT_ID:
            String id = uri.getPathSegments().get(1);
            count = db.delete(ACCOUNTS_TABLE_NAME, Accounts._ID + "=" + id
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        case TRANSACTIONS:
            count = db.delete(TRANSACTIONS_TABLE_NAME, where, whereArgs);
            break;

        case TRANSACTION_ID:
            String transactionId = uri.getPathSegments().get(1);
            long accountId = getAccountId(transactionId);

            count = db.delete(TRANSACTIONS_TABLE_NAME,
                    Transactions._ID + "=" + transactionId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
                    whereArgs);

            if (accountId > 0) {
                Uri accountUri = ContentUris.withAppendedId(Accounts.CONTENT_URI, accountId);
                getContext().getContentResolver().notifyChange(accountUri, null);

                Uri itemUri = ContentUris.withAppendedId(Accounts.Transactions.CONTENT_URI, accountId);
                getContext().getContentResolver().notifyChange(itemUri, null);
            }
            break;

        default:
            throw new IllegalArgumentException(TAG + " Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case ACCOUNTS:
            count = db.update(ACCOUNTS_TABLE_NAME, values, where, whereArgs);
            break;

        case ACCOUNT_ID:
            String accountId = uri.getPathSegments().get(1);
            count = db.update(ACCOUNTS_TABLE_NAME, values, Accounts._ID + "=" + accountId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        case TRANSACTIONS:
            count = db.update(TRANSACTIONS_TABLE_NAME, values, where, whereArgs);
            break;

        case TRANSACTION_ID:
            String transactionId = uri.getPathSegments().get(1);
            long id = getAccountId(transactionId);

            count = db.update(TRANSACTIONS_TABLE_NAME, values,
                    Transactions._ID + "=" + transactionId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
                    whereArgs);

            if (id > 0) {
                Uri accountUri = ContentUris.withAppendedId(Accounts.CONTENT_URI, id);
                getContext().getContentResolver().notifyChange(accountUri, null);

                Uri itemUri = ContentUris.withAppendedId(Accounts.Transactions.CONTENT_URI, id);
                getContext().getContentResolver().notifyChange(itemUri, null);
            }
            break;

        default:
            throw new IllegalArgumentException(TAG + " Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    private long getAccountId(String transactionId) {
        Cursor cursor = getContext().getContentResolver().query(
                ContentUris.withAppendedId(Transactions.CONTENT_URI, Long.valueOf(transactionId)),
                new String[] { Transactions.ACCOUNT_ID }, null, null, null);
        long id = 0;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                id = cursor.getLong(0);
            }
            cursor.deactivate();
        }
        return id;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(Budgets.AUTHORITY, "accounts", ACCOUNTS);
        sUriMatcher.addURI(Budgets.AUTHORITY, "accounts/#", ACCOUNT_ID);
        sUriMatcher.addURI(Budgets.AUTHORITY, "account.transactions/#", ACCOUNT_TRANSACTIONS);
        sUriMatcher.addURI(Budgets.AUTHORITY, "transactions", TRANSACTIONS);
        sUriMatcher.addURI(Budgets.AUTHORITY, "transactions/#", TRANSACTION_ID);

        sAccountsProjectionMap = new HashMap<String, String>();
        sAccountsProjectionMap.put(Accounts._ID, Accounts._ID);
        sAccountsProjectionMap.put(Accounts.TITLE, Accounts.TITLE);
        sAccountsProjectionMap.put(Accounts.AMOUNT, Accounts.AMOUNT);
        sAccountsProjectionMap.put(Accounts.SPEND, Accounts.SPEND);
        sAccountsProjectionMap.put(Accounts.BALANCE, Accounts.AMOUNT + " - " + Accounts.SPEND + " + " + Accounts.INCOME
                + " AS " + Accounts.BALANCE);
        sAccountsProjectionMap.put(Accounts.INCOME, Accounts.INCOME);
        sAccountsProjectionMap.put(Accounts.ROLLOVER_FLAG, Accounts.ROLLOVER_FLAG);
        sAccountsProjectionMap.put(Accounts.START_DATE, Accounts.START_DATE);

        sTransactionsProjectionMap = new HashMap<String, String>();
        sTransactionsProjectionMap.put(Transactions._ID, Transactions._ID);
        sTransactionsProjectionMap.put(Transactions.ACCOUNT_ID, Transactions.ACCOUNT_ID);
        sTransactionsProjectionMap.put(Transactions.TITLE, Transactions.TITLE);
        sTransactionsProjectionMap.put(Transactions.AMOUNT, Transactions.AMOUNT);
        sTransactionsProjectionMap.put(Transactions.ARCHIVE_FLAG, Transactions.ARCHIVE_FLAG);
        sTransactionsProjectionMap.put(Transactions.CREATE_DATE, Transactions.CREATE_DATE);
    }
}
