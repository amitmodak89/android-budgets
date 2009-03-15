package com.savefon.budgets;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;

public class DateControlSet implements OnTimeSetListener, OnDateSetListener, View.OnClickListener {

	private static final Format dateFormatter = new SimpleDateFormat("EEE, MMMM d, yyyy");
	private static final Format timeFormatter = new SimpleDateFormat("h:mm a");

	protected final Activity activity;
	protected Button dateButton;
	protected Button timeButton;
	protected Date date;

	protected DateControlSet(Activity activity) {
		this.activity = activity;
	}

	public DateControlSet(Activity activity, Button dateButton, Button timeButton) {
		this.activity = activity;
		this.dateButton = dateButton;
		this.timeButton = timeButton;

		dateButton.setOnClickListener(this);
		timeButton.setOnClickListener(this);

		setDate(null);
	}

	public DateControlSet(Activity activity, int dateButtonId, int timeButtonId) {
		this.activity = activity;
		this.dateButton = (Button) activity.findViewById(dateButtonId);
		this.timeButton = (Button) activity.findViewById(timeButtonId);

		dateButton.setOnClickListener(this);
		timeButton.setOnClickListener(this);

		setDate(0);
	}

	public Date getDate() {
		return date;
	}

	/** Initialize the components for the given date field */
	public void setDate(Date newDate) {
		if (newDate == null) {
			setDate(0);
		} else {
			setDate(newDate.getTime());
		}
	}

	public void setDate(long newDate) {
		if (newDate == 0) {
			date = new Date();
		} else {
			date = new Date(newDate);
		}
		updateDate();
		updateTime();
	}

	public void onDateSet(DatePicker view, int year, int month, int monthDay) {
		date.setYear(year - 1900);
		date.setMonth(month);
		date.setDate(monthDay);
		updateDate();
	}

	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		date.setHours(hourOfDay);
		date.setMinutes(minute);
		updateTime();
	}

	public void updateDate() {
		dateButton.setText(dateFormatter.format(date));
	}

	public void updateTime() {
		timeButton.setText(timeFormatter.format(date));
	}

	public void onClick(View view) {
		if (view == timeButton)
			new TimePickerDialog(activity, this, date.getHours(), date.getMinutes(), false).show();
		else
			new DatePickerDialog(activity, this, 1900 + date.getYear(), date.getMonth(), date.getDate()).show();
	}
}