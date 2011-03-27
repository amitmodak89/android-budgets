package com.savefon.budgets;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class TransactionsAdapter extends SimpleCursorAdapter {

    public TransactionsAdapter(Context context, Cursor cursor) {
        super(context, R.layout.transactionslist_item, cursor, new String[] {}, new int[] {});
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.transactionslist_item, parent, false);
        }

        AccountViewHolder holder = (AccountViewHolder) view.getTag();
        if (holder == null) {
            holder = new AccountViewHolder();
            holder.title = (TextView) view.findViewById(R.id.transaction_item_title);
            holder.date = (TextView) view.findViewById(R.id.transaction_item_date);
            holder.amount = (TextView) view.findViewById(R.id.transaction_item_amount);
            view.setTag(holder);
        }

        SimpleDateFormat pattern = new SimpleDateFormat("MMMMM d, yyyy HH:mm");

        Cursor cursor = (Cursor) getItem(position);
        String title = cursor.getString(TransactionsList.PROJECTION_INDEX_TITLE);
        float amount = cursor.getFloat(TransactionsList.PROJECTION_INDEX_AMOUNT);
        Date date = new Date(cursor.getLong(TransactionsList.PROJECTION_INDEX_CREATE_DATE));

        if (title.length() > 0) {
            holder.amount.setVisibility(View.VISIBLE);
            holder.title.setText(title);
            holder.amount.setText(NumberFormat.getCurrencyInstance().format(amount));
        } else {
            holder.amount.setVisibility(View.GONE);
            holder.title.setText(NumberFormat.getCurrencyInstance().format(amount));
        }
        holder.date.setText(pattern.format(date));

        return view;
    }

    private static class AccountViewHolder {
        public TextView title;
        public TextView date;
        public TextView amount;
    }
}
