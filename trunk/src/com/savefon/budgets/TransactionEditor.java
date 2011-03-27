package com.savefon.budgets;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

public class TransactionEditor extends Activity {
    private static final String TAG = "TransactionEditor";

    /**
     * Standard projection for the interesting columns of a normal transaction.
     */
    private static final String[] PROJECTION = new String[] { Budgets.Transactions._ID, // 0
            Budgets.Transactions.TITLE, // 1
            Budgets.Transactions.AMOUNT, // 2
            Budgets.Transactions.CREATE_DATE // 3
    };
    /** The index of the transaction column */
    private static final int PROJECTION_INDEX_TITLE = 1;
    private static final int PROJECTION_INDEX_AMOUNT = 2;
    private static final int PROJECTION_INDEX_DATE = 3;

    // Identifiers for our menu items.
    private static final int REVERT_ID = Menu.FIRST;
    private static final int DISCARD_ID = Menu.FIRST + 1;
    private static final int DELETE_ID = Menu.FIRST + 2;
    private static final int SAVE_ID = Menu.FIRST + 3;

    // The different distinct states the activity can be run in.
    private static final int STATE_EDIT = 0;
    private static final int STATE_INSERT = 1;

    private int mState;
    private Uri mUri;
    private Cursor mCursor;
    private EditText mTitle;
    private EditText mAmount;
    private DateControlSet mDate;
    private String mOriginalTitle;
    private float mOriginalAmount;
    private long mOriginalDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final String action = intent.getAction();
        if (Intent.ACTION_EDIT.equals(action)) {
            mState = STATE_EDIT;
            mUri = intent.getData();
        } else if (Intent.ACTION_INSERT.equals(action)) {
            mState = STATE_INSERT;
            mUri = getContentResolver().insert(intent.getData(), null);

            // If we were unable to create a new transaction, then just finish
            // this activity. A RESULT_CANCELED will be sent back to the
            // original activity if they requested a result.
            if (mUri == null) {
                Log.e(TAG, "Failed to insert new transaction into " + getIntent().getData());
                finish();
                return;
            }
            // The new entry was created, so assume all will end well and
            // set the result to be returned.
            setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));
        } else {
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }

        setContentView(R.layout.transaction_editor);

        mTitle = (EditText) findViewById(R.id.transaction_title);
        mAmount = (EditText) findViewById(R.id.transaction_amount);
        mDate = new DateControlSet(this, R.id.create_date, R.id.create_time);

        mCursor = managedQuery(mUri, PROJECTION, null, null, null);

        // If an instance of this activity had previously stopped, we can
        // get the original text it started with.
        if (savedInstanceState != null) {
            mOriginalTitle = savedInstanceState.getString(Budgets.Transactions.TITLE);
            mOriginalAmount = savedInstanceState.getFloat(Budgets.Transactions.AMOUNT);
            mOriginalDate = savedInstanceState.getLong(Budgets.Transactions.CREATE_DATE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If we didn't have any trouble retrieving the data, it is now
        // time to get at the stuff.
        if (mCursor != null) {
            mCursor.moveToFirst();

            if (mState == STATE_EDIT) {
                setTitle(getText(R.string.menu_transaction_edit));
            } else if (mState == STATE_INSERT) {
                setTitle(getText(R.string.menu_transaction_insert));
            }

            // This is a little tricky: we may be resumed after previously being
            // paused/stopped. We want to put the new text in the text view,
            // but leave the user where they were (retain the cursor position
            // etc). This version of setText does that for us.
            String title = mCursor.getString(PROJECTION_INDEX_TITLE);
            float amount = mCursor.getFloat(PROJECTION_INDEX_AMOUNT);
            long date = mCursor.getLong(PROJECTION_INDEX_DATE);

            mTitle.setText(title);
            mAmount.setText(amount == 0 ? "" : String.valueOf(amount));
            mDate.setDate(date);

            // If we hadn't previously retrieved the original text, do so
            // now. This allows the user to revert their changes.
            if (mOriginalTitle == null) {
                mOriginalTitle = title;
            }
            if (mOriginalAmount == 0) {
                mOriginalAmount = amount;
            }
            if (mOriginalDate == 0) {
                mOriginalDate = date;
            }
        } else {
            setTitle(getText(R.string.error_title));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save away the original text, so we still have it if the activity
        // needs to be killed while paused.
        outState.putString(Budgets.Transactions.TITLE, mOriginalTitle);
        outState.putFloat(Budgets.Transactions.AMOUNT, mOriginalAmount);
        outState.putLong(Budgets.Transactions.CREATE_DATE, mOriginalDate);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // The user is going somewhere else, so make sure their current
        // changes are safely saved away in the provider. We don't need
        // to do this if only editing.
        if (mCursor != null && mCursor.getCount() > 0) {
            final float amount = getNewTransactionAmount();
            if (isFinishing() && (amount <= 0)) {
                setResult(RESULT_CANCELED);
                deleteTransaction();
            } else {
                saveTransaction();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Build the menus that are shown when editing.
        menu.add(0, SAVE_ID, 0, R.string.menu_save).setShortcut('0', 's').setIcon(android.R.drawable.ic_menu_save);
        if (mState == STATE_EDIT) {
            menu.add(0, REVERT_ID, 0, R.string.menu_revert).setShortcut('1', 'r')
                    .setIcon(android.R.drawable.ic_menu_revert);
            menu.add(0, DELETE_ID, 0, R.string.menu_delete).setShortcut('2', 'd')
                    .setIcon(android.R.drawable.ic_menu_delete);

            // Build the menus that are shown when inserting.
        } else {
            menu.add(0, DISCARD_ID, 0, R.string.menu_discard).setShortcut('1', 'd')
                    .setIcon(android.R.drawable.ic_menu_delete);
        }

        // If we are working on a full transaction, then append to the
        // menu items for any other activities that can do stuff with it
        // as well. This does a query on the system for any activities that
        // implement the ALTERNATIVE_ACTION for our data, adding a menu item
        // for each one that is found.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0, new ComponentName(this, TransactionEditor.class), null,
                intent, 0, null);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final float amount = getNewTransactionAmount();
        menu.findItem(SAVE_ID).setEnabled(isTransactionChanged() && amount > 0);
        if (mState == STATE_EDIT) {
            menu.findItem(REVERT_ID).setEnabled(isTransactionChanged());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        switch (item.getItemId()) {
        case DELETE_ID:
            deleteTransaction();
            finish();
            break;
        case DISCARD_ID:
            cancelTransaction();
            setResult(RESULT_CANCELED);
            finish();
            break;
        case REVERT_ID:
            cancelTransaction();
            setResult(RESULT_CANCELED);
            finish();
            break;
        case SAVE_ID:
            saveTransaction();
            finish();
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private float getNewTransactionAmount() {
        float amount;
        try {
            amount = Float.parseFloat(mAmount.getText().toString());
        } catch (Exception e) {
            amount = 0;
        }
        return amount;
    }

    private boolean isTransactionChanged() {
        final float amount = getNewTransactionAmount();
        final String title = mTitle.getText().toString();
        final long createDate = mDate.getDate().getTime();
        return !title.equals(mOriginalTitle) || amount != mOriginalAmount || createDate != mOriginalDate;
    }

    private void saveTransaction() {
        final float amount = getNewTransactionAmount();
        ContentValues values = new ContentValues();
        values.put(Budgets.Transactions.TITLE, mTitle.getText().toString());
        values.put(Budgets.Transactions.AMOUNT, amount);
        values.put(Budgets.Transactions.CREATE_DATE, mDate.getDate().getTime());
        getContentResolver().update(mUri, values, null, null);
    }

    /**
     * Take care of canceling work on a transaction. Deletes the transaction if we had created it, otherwise reverts to
     * the original text.
     */
    private final void cancelTransaction() {
        if (mCursor != null) {
            if (mState == STATE_EDIT) {
                // Put the original transaction text back into the database
                mCursor.close();
                mCursor = null;
                if (isTransactionChanged()) {
                    ContentValues values = new ContentValues();
                    values.put(Budgets.Transactions.TITLE, mOriginalTitle);
                    values.put(Budgets.Transactions.AMOUNT, mOriginalAmount);
                    values.put(Budgets.Transactions.CREATE_DATE, mOriginalDate);
                    getContentResolver().update(mUri, values, null, null);
                }
            } else if (mState == STATE_INSERT) {
                // We inserted an empty transaction, make sure to delete it
                deleteTransaction();
            }
        }
    }

    /**
     * Take care of deleting a transaction. Simply deletes the entry.
     */
    private final void deleteTransaction() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
            getContentResolver().delete(mUri, null, null);
            mTitle.setText("");
            mAmount.setText("");
        }
    }
}
