package com.savefon.budgets;

import java.text.NumberFormat;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ContentUris;
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
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

public class TransactionsList extends ListActivity {
    private static final String TAG = "TransactionsList";

    // Menu item ids
    public static final int MENU_ITEM_EDIT = Menu.FIRST;
    public static final int MENU_ITEM_DELETE = Menu.FIRST + 1;
    public static final int MENU_ITEM_INSERT = Menu.FIRST + 2;

    /**
     * The columns we are interested in from the database
     */
    private static final String[] PROJECTION = new String[] {
    	Budgets.Transactions._ID, // 0
    	Budgets.Transactions.TITLE, // 1
    	Budgets.Transactions.AMOUNT, // 2
    	Budgets.Transactions.CREATE_DATE // 3
    };
    /** The index of the title column */
    public static final int PROJECTION_INDEX_TITLE = 1;
    public static final int PROJECTION_INDEX_AMOUNT = 2;
    public static final int PROJECTION_INDEX_CREATE_DATE = 3;

    private static final String[] PROJECTION_ACCOUNT = new String[] {
		Budgets.Accounts.TITLE, // 0
		Budgets.Accounts.AMOUNT, // 1
		Budgets.Accounts.SPEND // 2
    };
    private static final int PROJECTION_ACCOUNT_TITLE = 0;
    private static final int PROJECTION_ACCOUNT_AMOUNT = 1;
    private static final int PROJECTION_ACCOUNT_SPEND = 2;

    private Cursor mAccountCursor;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.transactionslist);
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(Budgets.Transactions.CONTENT_URI);
        } else {
        	String accountId = intent.getData().getPathSegments().get(1);
        	mAccountCursor = managedQuery(ContentUris.withAppendedId(Budgets.Accounts.CONTENT_URI, Long.valueOf(accountId)),
        			PROJECTION_ACCOUNT, null, null, null);
        }

        Button button = (Button) findViewById(R.id.transactionslist_button_insert);
        button.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
            }
        });

        mProgressBar = (ProgressBar) findViewById(R.id.transactionslist_progress);
        mProgressBar.setVisibility(View.GONE);

        // Inform the list we provide context menus for items
        getListView().setOnCreateContextMenuListener(this);

        // Perform a managed query. The Activity will handle closing and requiring the cursor
        // when needed.
        Cursor cursor = managedQuery(getIntent().getData(), PROJECTION, null, null,
        		Budgets.Transactions.DEFAULT_SORT_ORDER);

        // Used to map accounts entries from the database to views
        TransactionsAdapter adapter = new TransactionsAdapter(this, cursor);
        setListAdapter(adapter);
    }

    @Override
	protected void onResume() {
		super.onResume();
		refresh();
	}

	private void refresh() {
		int amount = 0, spend = 0;
		String title = "";
		if (mAccountCursor.getCount() > 0) {
			mAccountCursor.moveToFirst();
			title = mAccountCursor.getString(PROJECTION_ACCOUNT_TITLE);
			amount = mAccountCursor.getInt(PROJECTION_ACCOUNT_AMOUNT);
			spend = mAccountCursor.getInt(PROJECTION_ACCOUNT_SPEND);
		}
		mProgressBar.setVisibility(View.GONE);
		if (spend > 0 && amount > 0) {
			if (amount > spend) {
				mProgressBar.setVisibility(View.VISIBLE);
				mProgressBar.setProgress((spend * 100) / amount);
			}
			setTitle(getResources().getString(R.string.title_transactions_list_balance,
					NumberFormat.getCurrencyInstance().format(spend),
					NumberFormat.getCurrencyInstance().format(amount),
					title));
		} else if (amount > 0) {
			setTitle(getResources().getString(R.string.title_transactions_list_amount,
					NumberFormat.getCurrencyInstance().format(amount),
					title));
		} else {
			setTitle(title);
		}
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // This is our one standard application action -- inserting a
        // new account into the list.
        menu.add(0, MENU_ITEM_INSERT, 0, R.string.menu_transaction_insert)
                .setShortcut('3', 'a')
                .setIcon(android.R.drawable.ic_menu_add);

        // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, TransactionsList.class), null, intent, 0, null);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final boolean haveItems = getListAdapter().getCount() > 0;

        if (haveItems) {
            // This is the selected item.
            Uri uri = ContentUris.withAppendedId(Budgets.Transactions.CONTENT_URI, getSelectedItemId());

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
                items[0].setShortcut('1', 'e');
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

        String title = cursor.getString(PROJECTION_INDEX_TITLE);
        menu.setHeaderTitle(title.length() > 0 ? title : getResources().getString(R.string.transaction_contextMenu_title));

        // Add a menu item to delete the account
        menu.add(0, MENU_ITEM_EDIT, 0, R.string.menu_transaction_edit);
        menu.add(0, MENU_ITEM_DELETE, 0, R.string.menu_transaction_delete);
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
	        case MENU_ITEM_EDIT: {
	            Uri itemUri = ContentUris.withAppendedId(Budgets.Transactions.CONTENT_URI, info.id);
	        	startActivity(new Intent(Intent.ACTION_EDIT, itemUri));
	        	return true;
	        }
            case MENU_ITEM_DELETE: {
                // Delete the account that the context menu is for
                final Uri itemUri = ContentUris.withAppendedId(Budgets.Transactions.CONTENT_URI, info.id);
                //TODO make this dialog persist across screen rotations
                new AlertDialog.Builder(TransactionsList.this)
                    .setTitle(R.string.deleteConfirmation_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.transaction_deleteConfirmation)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
                    	public void onClick(DialogInterface dialog, int whichButton) {
                    		getContentResolver().delete(itemUri, null, null);
                    		mAccountCursor.requery();
                    		refresh();
                    	}
                    })
                    .show();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(Budgets.Transactions.CONTENT_URI, id);

        String action = getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
            // The caller is waiting for us to return a account selected by
            // the user.  The have clicked on one, so return it now.
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {
            // Launch activity to view/edit the currently selected item
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }

}
