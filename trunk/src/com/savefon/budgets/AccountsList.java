package com.savefon.budgets;

import java.text.NumberFormat;

import com.savefon.budgets.Budgets.Accounts;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * Displays a list of accounts. Will display accounts from the {@link Uri}
 * provided in the intent if there is one, otherwise defaults to displaying the
 * contents of the {@link BudgetsProvider}
 */
public class AccountsList extends ListActivity {
    private static final String TAG = "AccountsList";

    // Menu item ids
    public static final int MENU_ITEM_TRANSACTION_INSERT = Menu.FIRST;
    public static final int MENU_ITEM_TRANSACTION_BROWSE = Menu.FIRST + 1;
    public static final int MENU_ITEM_EDIT = Menu.FIRST + 2;
    public static final int MENU_ITEM_RESTART = Menu.FIRST + 3;
    public static final int MENU_ITEM_DELETE = Menu.FIRST + 4;
    public static final int MENU_ITEM_INSERT = Menu.FIRST + 5;

    /**
     * The columns we are interested in from the database
     */
    private static final String[] PROJECTION = new String[] {
            Accounts._ID, // 0
            Accounts.TITLE, // 1
            Accounts.AMOUNT, // 2
            Accounts.SPEND, // 3
            Accounts.BALANCE, // 4
            Accounts.INCOME // 5
    };

    /** The index of the title column */
    public static final int PROJECTION_INDEX_TITLE = 1;
    public static final int PROJECTION_INDEX_AMOUNT = 2;
    public static final int PROJECTION_INDEX_SPEND = 3;
    public static final int PROJECTION_INDEX_BALANCE = 4;
    public static final int PROJECTION_INDEX_INCOME = 5;

    Cursor mCursor;

    private class RestartClickListener implements DialogInterface.OnClickListener {
        private Uri mUri;
        private int mBalance = 0;

        public RestartClickListener(Uri uri, int balance) {
            mUri = uri;
            mBalance = balance;
        }

        public void onClick(DialogInterface dialog, int which) {
			ContentValues values = new ContentValues();
			values.put(Budgets.Accounts.SPEND, 0);
			values.put(Budgets.Accounts.INCOME, mBalance);
			values.put(Budgets.Accounts.START_DATE, System.currentTimeMillis());
			getContentResolver().update(mUri, values, null, null);
        }
    }

    private class DeleteClickListener implements DialogInterface.OnClickListener {
        private Uri mUri;

        public DeleteClickListener(Uri uri) {
            mUri = uri;
        }

        public void onClick(DialogInterface dialog, int which) {
            getContentResolver().delete(mUri, null, null);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.accountslist);
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        // If no data was given in the intent (because we were started
        // as a MAIN activity), then use our default content provider.
        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(Accounts.CONTENT_URI);
        }

        // Inform the list we provide context menus for items
        getListView().setOnCreateContextMenuListener(this);

        mCursor = managedQuery(getIntent().getData(), PROJECTION, null, null, Accounts.DEFAULT_SORT_ORDER);
        AccountsAdapter adapter = new AccountsAdapter(this, mCursor);
        setListAdapter(adapter);
    }

    @Override
	protected void onResume() {
		super.onResume();

		int amount = 0, spend = 0;
		if (mCursor.getCount() > 0) {
			mCursor.moveToFirst();
			while (! mCursor.isAfterLast()) {
				amount += mCursor.getInt(PROJECTION_INDEX_AMOUNT);
				spend += mCursor.getInt(PROJECTION_INDEX_SPEND);
				mCursor.moveToNext();
			}
		}
		if (spend > 0 && amount > 0) {
			int balance = amount - spend;
			setTitle(getResources().getString(R.string.title_accounts_list_balance,
					NumberFormat.getCurrencyInstance().format(balance > 0 ? balance : 0),
					NumberFormat.getCurrencyInstance().format(amount)));
		} else if (amount > 0) {
			setTitle(getResources().getString(R.string.title_accounts_list_amount,
					NumberFormat.getCurrencyInstance().format(amount)));
		} else {
			setTitle(getResources().getString(R.string.title_accounts_list_empty));
		}
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // This is our one standard application action -- inserting a
        // new account into the list.
        menu.add(0, MENU_ITEM_INSERT, 0, R.string.menu_insert)
                .setShortcut('3', 'a')
                .setIcon(android.R.drawable.ic_menu_add);

        // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, AccountsList.class), null, intent, 0, null);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final boolean haveItems = getListAdapter().getCount() > 0;

        // If there are any accounts in the list (which implies that one of
        // them is selected), then we need to generate the actions that
        // can be performed on the current selection.  This will be a combination
        // of our own specific actions along with any extensions that can be
        // found.
        if (haveItems) {
            // This is the selected item.
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            // Build menu...  always starts with the EDIT action...
            Intent[] specifics = new Intent[1];
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);
            MenuItem[] items = new MenuItem[1];

            // ... is followed by whatever other actions are available...
            Intent intent = new Intent(null, uri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0, null, specifics, intent, 0,
                    items);

            // Give a shortcut to the edit action.
            if (items[0] != null) {
                items[0]
                      .setShortcut('1', 'e')
                      .setIcon(android.R.drawable.ic_menu_edit);
            }
        } else {
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_INSERT:
            // Launch activity to insert a new item
            startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }
        menu.setHeaderTitle(cursor.getString(PROJECTION_INDEX_TITLE));
        int amout = cursor.getInt(PROJECTION_INDEX_AMOUNT);

        menu.add(0, MENU_ITEM_TRANSACTION_INSERT, Menu.NONE, R.string.menu_transaction_insert);
        if (amout > 0) {
        	menu.add(0, MENU_ITEM_TRANSACTION_BROWSE, Menu.NONE, R.string.menu_transaction_browse);
        	menu.add(0, MENU_ITEM_RESTART, Menu.NONE, R.string.menu_account_restart);
        }
        menu.add(0, MENU_ITEM_EDIT, Menu.NONE, R.string.menu_account_edit);
        menu.add(0, MENU_ITEM_DELETE, Menu.NONE, R.string.menu_account_delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        switch (item.getItemId()) {
	        case MENU_ITEM_TRANSACTION_INSERT: {
	        	Uri uri = ContentUris.withAppendedId(Budgets.Accounts.Transactions.CONTENT_URI, info.id);
	        	startActivity(new Intent(Intent.ACTION_INSERT, uri));
	            return true;
	        }
	        case MENU_ITEM_TRANSACTION_BROWSE: {
	        	Uri uri = ContentUris.withAppendedId(Budgets.Accounts.Transactions.CONTENT_URI, info.id);
	        	startActivity(new Intent(Intent.ACTION_VIEW, uri));
	        	return true;
	        }
	        case MENU_ITEM_RESTART: {
	        	Uri itemUri = ContentUris.withAppendedId(getIntent().getData(), info.id);
	        	int balance = 0;

	        	Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
	        	if (cursor != null) {
	        		balance = cursor.getInt(PROJECTION_INDEX_BALANCE);
	        	}

                //TODO make this dialog persist across screen rotations
                new AlertDialog.Builder(AccountsList.this)
                    .setTitle(R.string.account_restartConfirmation_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.account_restartConfirmation_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new RestartClickListener(itemUri, balance))
                    .show();
	            return true;
	        }
	        case MENU_ITEM_EDIT: {
	            Uri itemUri = ContentUris.withAppendedId(getIntent().getData(), info.id);
	        	startActivity(new Intent(Intent.ACTION_EDIT, itemUri));
	        	return true;
	        }
	        case MENU_ITEM_DELETE: {
	            final Uri itemUri = ContentUris.withAppendedId(getIntent().getData(), info.id);
                //TODO make this dialog persist across screen rotations
                new AlertDialog.Builder(AccountsList.this)
                    .setTitle(R.string.deleteConfirmation_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.account_deleteConfirmation)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DeleteClickListener(itemUri))
                    .show();
	            return true;
	        }
        }
        return false;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        String action = getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
            // The caller is waiting for us to return a account selected by
            // the user.  The have clicked on one, so return it now.
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {
            uri = ContentUris.withAppendedId(Budgets.Accounts.Transactions.CONTENT_URI, id);

        	Cursor cursor = (Cursor) getListAdapter().getItem(position);
        	if (cursor != null && cursor.getInt(PROJECTION_INDEX_SPEND) == 0) {
        		startActivity(new Intent(Intent.ACTION_INSERT, uri));
        	} else {
        		startActivity(new Intent(Intent.ACTION_VIEW, uri));
			}
        }
    }
}
