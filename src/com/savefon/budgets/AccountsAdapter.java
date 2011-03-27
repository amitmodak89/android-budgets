package com.savefon.budgets;

import java.text.NumberFormat;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class AccountsAdapter extends SimpleCursorAdapter {

    public AccountsAdapter(Context context, Cursor cursor) {
        super(context, R.layout.accountslist_item, cursor, new String[] {}, new int[] {});
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.accountslist_item, parent, false);
        }

        AccountViewHolder holder = (AccountViewHolder) view.getTag();
        if (holder == null) {
            holder = new AccountViewHolder();
            holder.title = (TextView) view.findViewById(R.id.account_item_title);
            holder.info = (TextView) view.findViewById(R.id.account_item_info);
            holder.amount = (TextView) view.findViewById(R.id.account_item_amount);
            holder.hint = (TextView) view.findViewById(R.id.account_item_hint);
            view.setTag(holder);
        }

        Cursor cursor = (Cursor) getItem(position);
        float amount = cursor.getFloat(AccountsList.PROJECTION_INDEX_AMOUNT);
        float spend = cursor.getFloat(AccountsList.PROJECTION_INDEX_SPEND);
        float balance = cursor.getFloat(AccountsList.PROJECTION_INDEX_BALANCE);
        float income = cursor.getFloat(AccountsList.PROJECTION_INDEX_INCOME);

        holder.title.setText(cursor.getString(AccountsList.PROJECTION_INDEX_TITLE));
        if (amount > 0) {
            holder.info.setVisibility(View.VISIBLE);
            holder.hint.setVisibility(View.VISIBLE);
            if (income > 0) {
                holder.info.setText(parent
                        .getContext()
                        .getResources()
                        .getString(R.string.accounts_list_spent_income_positive,
                                NumberFormat.getCurrencyInstance().format(spend),
                                NumberFormat.getCurrencyInstance().format(amount),
                                NumberFormat.getCurrencyInstance().format(income)));
            } else if (income < 0) {
                holder.info.setText(parent
                        .getContext()
                        .getResources()
                        .getString(R.string.accounts_list_spent_income_negative,
                                NumberFormat.getCurrencyInstance().format(spend),
                                NumberFormat.getCurrencyInstance().format(amount),
                                NumberFormat.getCurrencyInstance().format(Math.abs(income))));
            } else {
                holder.info.setText(parent
                        .getContext()
                        .getResources()
                        .getString(R.string.accounts_list_spent, NumberFormat.getCurrencyInstance().format(spend),
                                NumberFormat.getCurrencyInstance().format(amount)));
            }
            holder.amount.setText(NumberFormat.getCurrencyInstance().format(balance));
            if (balance > 0) {
                holder.hint.setText(R.string.accounts_list_hint_spent);
                holder.hint.setTextColor(android.graphics.Color.WHITE);
                holder.amount.setTextColor(android.graphics.Color.WHITE);
            } else if (balance == 0) {
                holder.hint.setVisibility(View.GONE);
                holder.amount.setTextColor(android.graphics.Color.GRAY);
            } else {
                holder.hint.setText(R.string.accounts_list_hint_over);
                holder.hint.setTextColor(android.graphics.Color.RED);
                holder.amount.setTextColor(android.graphics.Color.RED);
            }
        } else {
            holder.info.setVisibility(View.GONE);
            holder.hint.setVisibility(View.GONE);
            holder.amount.setText(NumberFormat.getCurrencyInstance().format(spend));
        }

        return view;
    }

    private static class AccountViewHolder {
        public TextView title;
        public TextView info;
        public TextView amount;
        public TextView hint;
    }
}
