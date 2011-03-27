package com.savefon.budgets;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ToggleButton;

import com.savefon.budgets.Budgets.Accounts;

public class AccountEditor extends Activity {
    private static final String TAG = "AccountEditor";

    private static final String[] PROJECTION = new String[] { Accounts._ID, // 0
            Accounts.TITLE, // 1
            Accounts.AMOUNT, // 2
            Accounts.ROLLOVER_FLAG, // 3
            Accounts.START_DATE // 4
    };
    /** The index of the account column */
    private static final int PROJECTION_INDEX_TITLE = 1;
    private static final int PROJECTION_INDEX_AMOUNT = 2;
    private static final int PROJECTION_INDEX_ROLLOVER_FLAG = 3;
    private static final int PROJECTION_INDEX_START_DATE = 4;

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
    private ToggleButton mRollover;
    private DateControlSet mStartDate;

    private String mOriginalTitle;
    private float mOriginalAmount;
    private int mOriginalRollover;
    private long mOriginalStartDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final String action = intent.getAction();
        if (Intent.ACTION_EDIT.equals(action)) {
            // Requested to edit: set that state, and the data being edited.
            mState = STATE_EDIT;
            mUri = intent.getData();
        } else if (Intent.ACTION_INSERT.equals(action)) {
            // Requested to insert: set that state, and create a new entry
            // in the container.
            mState = STATE_INSERT;
            mUri = getContentResolver().insert(intent.getData(), null);

            // If we were unable to create a new account, then just finish
            // this activity. A RESULT_CANCELED will be sent back to the
            // original activity if they requested a result.
            if (mUri == null) {
                Log.e(TAG, "Failed to insert new account into " + getIntent().getData());
                finish();
                return;
            }

            // The new entry was created, so assume all will end well and
            // set the result to be returned.
            setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));
        } else {
            // Whoops, unknown action! Bail.
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }

        setContentView(R.layout.account_editor);

        mTitle = (EditText) findViewById(R.id.account_title);
        mAmount = (EditText) findViewById(R.id.account_amount);
        mRollover = (ToggleButton) findViewById(R.id.account_rollover);
        mStartDate = new DateControlSet(this, R.id.account_start_date, R.id.account_start_time);

        mCursor = managedQuery(mUri, PROJECTION, null, null, null);

        // If an instance of this activity had previously stopped, we can
        // get the original text it started with.
        if (savedInstanceState != null) {
            mOriginalTitle = savedInstanceState.getString(Budgets.Accounts.TITLE);
            mOriginalAmount = savedInstanceState.getFloat(Budgets.Accounts.AMOUNT);
            mOriginalRollover = savedInstanceState.getInt(Budgets.Accounts.ROLLOVER_FLAG);
            mOriginalStartDate = savedInstanceState.getLong(Budgets.Accounts.START_DATE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If we didn't have any trouble retrieving the data, it is now
        // time to get at the stuff.
        if (mCursor != null) {
            // Make sure we are at the one and only row in the cursor.
            mCursor.moveToFirst();

            // Modify our overall title depending on the mode we are running in.
            if (mState == STATE_EDIT) {
                setTitle(getText(R.string.title_edit));
            } else if (mState == STATE_INSERT) {
                setTitle(getText(R.string.title_create));
            }

            // This is a little tricky: we may be resumed after previously being
            // paused/stopped. We want to put the new text in the text view,
            // but leave the user where they were (retain the cursor position
            // etc). This version of setText does that for us.
            String title = mCursor.getString(PROJECTION_INDEX_TITLE);
            float amount = mCursor.getFloat(PROJECTION_INDEX_AMOUNT);
            int rollover = mCursor.getInt(PROJECTION_INDEX_ROLLOVER_FLAG);
            long startDate = mCursor.getLong(PROJECTION_INDEX_START_DATE);

            mTitle.setTextKeepState(title);
            mAmount.setText(String.valueOf(amount));
            mRollover.setChecked(rollover == 1);
            mStartDate.setDate(startDate);

            // If we hadn't previously retrieved the original text, do so
            // now. This allows the user to revert their changes.
            if (mOriginalTitle == null) {
                mOriginalTitle = title;
            }
            if (mOriginalAmount == 0) {
                mOriginalAmount = amount;
            }
            if (mOriginalRollover == 0) {
                mOriginalRollover = rollover;
            }
            if (mOriginalStartDate == 0) {
                mOriginalStartDate = startDate;
            }
        } else {
            setTitle(getText(R.string.error_title));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save away the original text, so we still have it if the activity
        // needs to be killed while paused.
        outState.putString(Budgets.Accounts.TITLE, mOriginalTitle);
        outState.putFloat(Budgets.Accounts.AMOUNT, mOriginalAmount);
        outState.putInt(Budgets.Accounts.ROLLOVER_FLAG, mOriginalRollover);
        outState.putLong(Budgets.Accounts.START_DATE, mOriginalStartDate);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // The user is going somewhere else, so make sure their current
        // changes are safely saved away in the provider. We don't need
        // to do this if only editing.
        if (mCursor != null) {
            final String title = mTitle.getText().toString();

            // If this activity is finished, and there is no text, then we
            // do something a little special: simply delete the account entry.
            // Account that we do this both for editing and inserting... it
            // would be reasonable to only do it when inserting.
            if (isFinishing() && TextUtils.isEmpty(title)) {
                setResult(RESULT_CANCELED);
                deleteAccount();
            } else {
                saveAccount();
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

        // If we are working on a full account, then append to the
        // menu items for any other activities that can do stuff with it
        // as well. This does a query on the system for any activities that
        // implement the ALTERNATIVE_ACTION for our data, adding a menu item
        // for each one that is found.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0, new ComponentName(this, AccountEditor.class), null,
                intent, 0, null);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final String title = mTitle.getText().toString();
        menu.findItem(SAVE_ID).setEnabled(isAccountChanged() && !TextUtils.isEmpty(title));
        if (mState == STATE_EDIT) {
            menu.findItem(REVERT_ID).setEnabled(isAccountChanged());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        switch (item.getItemId()) {
        case DELETE_ID:
            deleteAccount();
            finish();
            break;
        case DISCARD_ID:
            cancelAccount();
            setResult(RESULT_CANCELED);
            finish();
            break;
        case REVERT_ID:
            cancelAccount();
            setResult(RESULT_CANCELED);
            finish();
            break;
        case SAVE_ID:
            saveAccount();
            finish();
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isAccountChanged() {
        final float amount = getNewAccountAmount();
        final String title = mTitle.getText().toString();
        final int rollover = mRollover.isChecked() ? 1 : 0;
        final long startDate = mStartDate.getDate().getTime();
        return !title.equals(mOriginalTitle) || amount != mOriginalAmount || rollover != mOriginalRollover
                || startDate != mOriginalStartDate;
    }

    private float getNewAccountAmount() {
        float amount;
        try {
            amount = Float.parseFloat(mAmount.getText().toString());
        } catch (Exception e) {
            amount = 0;
        }
        return amount;
    }

    /**
     * Take care of canceling work on a account. Deletes the account if we had created it, otherwise reverts to the
     * original text.
     */
    private final void cancelAccount() {
        if (mCursor != null) {
            if (mState == STATE_EDIT) {
                // Put the original account text back into the database
                mCursor.close();
                mCursor = null;
                if (isAccountChanged()) {
                    ContentValues values = new ContentValues();
                    values.put(Accounts.TITLE, mOriginalTitle);
                    values.put(Accounts.AMOUNT, mOriginalAmount);
                    values.put(Accounts.ROLLOVER_FLAG, mOriginalRollover);
                    values.put(Accounts.START_DATE, mOriginalStartDate);
                    getContentResolver().update(mUri, values, null, null);
                }
            } else if (mState == STATE_INSERT) {
                // We inserted an empty account, make sure to delete it
                deleteAccount();
            }
        }
    }

    private void saveAccount() {
        final float amount = getNewAccountAmount();
        ContentValues values = new ContentValues();
        values.put(Accounts.TITLE, mTitle.getText().toString());
        values.put(Accounts.AMOUNT, amount);
        values.put(Accounts.ROLLOVER_FLAG, mRollover.isChecked() ? 1 : 0);
        values.put(Accounts.START_DATE, mStartDate.getDate().getTime());
        getContentResolver().update(mUri, values, null, null);
    }

    /**
     * Take care of deleting a account. Simply deletes the entry.
     */
    private final void deleteAccount() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;

            getContentResolver().delete(mUri, null, null);

            mTitle.setText("");
        }
    }
}
