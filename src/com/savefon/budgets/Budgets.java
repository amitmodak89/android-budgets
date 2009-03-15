package com.savefon.budgets;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Convenience definitions for BudgetsProvider
 */
public final class Budgets {
    public static final String AUTHORITY = "com.savefon.provider.Budgets";

    // This class cannot be instantiated
    private Budgets() {}

    /**
     * Accounts table
     */
    public static final class Accounts implements BaseColumns {
        // This class cannot be instantiated
        private Accounts() {}

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/accounts");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of accounts.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.savefon.budgets.accounts";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single account.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.savefon.budgets.accounts";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = _ID + " DESC";

        /**
         * The title of the account
         * <P>Type: TEXT</P>
         */
        public static final String TITLE = "title";

        /**
         * <P>Type: INTEGER</P>
         */
        public static final String AMOUNT = "amount";

        /**
         * <P>Type: INTEGER</P>
         */
        public static final String SPEND = "spend";

        /**
         * <P>Type: INTEGER</P>
         */
        public static final String BALANCE = "balance";

        /**
         * <P>Type: INTEGER</P>
         */
        public static final String INCOME = "income";

        /**
         * <P>Type: INTEGER</P>
         */
        public static final String ROLLOVER_FLAG = "rollover_flag";

        /**
         * The timestamp for when the account was started
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String START_DATE = "start_date";

        /**
         * Transactions table
         */
        public static final class Transactions {
            private Transactions() {}

            /**
             * The content:// style URL for this table
             */
            public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/account.transactions");
        }
    }

    /**
     * Transactions table
     */
    public static final class Transactions implements BaseColumns {
        // This class cannot be instantiated
        private Transactions() {}

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/transactions");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of accounts.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.savefon.budgets.transactions";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single account.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.savefon.budgets.transactions";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "create_date DESC";

        /**
         * <P>Type: INTEGER</P>
         */
        public static final String ACCOUNT_ID = "account_id";

        /**
         * The title of the transaction
         * <P>Type: TEXT</P>
         */
        public static final String TITLE = "title";

        /**
         * <P>Type: INTEGER</P>
         */
        public static final String AMOUNT = "amount";

        /**
         * <P>Type: INTEGER</P>
         */
        public static final String ARCHIVE_FLAG = "archive_flag";

        /**
         * The timestamp for when the account was transaction
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String CREATE_DATE = "create_date";
    }
}
