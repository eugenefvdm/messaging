package com.messaging.push;

import java.util.Date;
import java.util.TimeZone;

import com.messaging.push.contentprovider.MyTaskContentProvider;
import com.messaging.push.db.TaskTable;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/*
 * 
 */
public class TaskDetailActivity extends Activity implements AsyncResponse {
	private TextView mDepartment;
	private TextView mLocationText;
	private TextView mClientText;

	private TextView mStartText;

	private Uri todoUri;

	HTTPTask asyncTask;

	private Cursor cursor;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.task_edit);

		mDepartment = (TextView) findViewById(R.id.task_edit_department);
		mLocationText = (TextView) findViewById(R.id.task_edit_location);
		mClientText = (TextView) findViewById(R.id.task_edit_client);
		mStartText = (TextView) findViewById(R.id.task_edit_start_text);

		Button startButton = (Button) findViewById(R.id.task_edit_start_button);
		Button stopButton = (Button) findViewById(R.id.task_edit_stop);
		Button pauseButton = (Button) findViewById(R.id.task_edit_pause);

		Bundle extras = getIntent().getExtras();

		// The taskUri can either come from a saved instance or it could have
		// been passed from the list activity
		if (bundle != null) {
			// todoUri retrieved from saved instance
			todoUri = (Uri) bundle.getParcelable(MyTaskContentProvider.CONTENT_ITEM_TYPE);
		} else {
			todoUri = extras.getParcelable(MyTaskContentProvider.CONTENT_ITEM_TYPE);
		}
		fillData(todoUri);

		startButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//int gmtOffset = TimeZone.getDefault().getRawOffset();
				//long unixTime = (System.currentTimeMillis() + gmtOffset) / 1000L;
				long unixTime = (System.currentTimeMillis()) / 1000L;
				Date d = new Date(unixTime * 1000);
				ContentValues values = new ContentValues();
				values.put(TaskTable.COLUMN_START_ACTUAL, unixTime);
				getContentResolver().update(todoUri, values, null, null);
				mStartText.setText(DateFormat.format("dd/MM hh:mm", d));
				// Send to server
				String[] projection = {
						TaskTable.COLUMN_CLIENT, TaskTable.COLUMN_CITY,
						TaskTable.COLUMN_DEPARTMENT, TaskTable.COLUMN_START_ACTUAL,
						TaskTable.COLUMN_TICKET_ID };
				cursor = getContentResolver().query(todoUri, projection, null, null, null);
				cursor.moveToFirst();
				String ticket_id = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_TICKET_ID));
				Long start_actual = cursor.getLong(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_START_ACTUAL));
				cursor.close();
				// String id = todoUri.getPathSegments().get(1);
				String action = "start";
				// String id = String.valueOf(unixTime);
				asyncTask = new HTTPTask();
				asyncTask.delegate = TaskDetailActivity.this;
				asyncTask.execute("http://196.201.6.235/whmcs/modules/addons/messaging/action.php?action=" + action + "&id=" + ticket_id + "&start_actual=" + start_actual);
			}
		});

		pauseButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Put a start time next to this task in the database

			}
		});

		stopButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Put a stop time next to this task in the database

			}
		});

	}

	private void fillData(Uri uri) {
		String[] projection = {
				TaskTable.COLUMN_CLIENT, TaskTable.COLUMN_CITY,
				TaskTable.COLUMN_DEPARTMENT, TaskTable.COLUMN_START_ACTUAL,
				TaskTable.COLUMN_TICKET_ID };
		cursor = getContentResolver().query(uri, projection, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
			mDepartment.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_DEPARTMENT)));
			mLocationText.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_CLIENT)));
			mClientText.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_CITY)));
			// Obtain Unix time from stored database field and work out real date
			long unixTime = cursor.getLong(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_START_ACTUAL));			
			Date d = new Date(unixTime * 1000);			
			mStartText.setText(DateFormat.format("dd/MM hh:mm", d));
			// always close the cursor
			cursor.close();
		}
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		saveState();
		outState.putParcelable(MyTaskContentProvider.CONTENT_ITEM_TYPE, todoUri);
	}

	@Override
	protected void onPause() {
		super.onPause();
		saveState();
	}

	private void saveState() {
		String category = (String) mDepartment.getText().toString();
		String summary = mLocationText.getText().toString();
		String description = mClientText.getText().toString();

		
		ContentValues values = new ContentValues();
		values.put(TaskTable.COLUMN_DEPARTMENT, category);
		values.put(TaskTable.COLUMN_CLIENT, summary);
		values.put(TaskTable.COLUMN_CITY, description);

		if (todoUri == null) {
			// New task
			todoUri = getContentResolver().insert(MyTaskContentProvider.CONTENT_URI, values);
		} else {
			// Update task
			getContentResolver().update(todoUri, values, null, null);
		}
	}

	@Override
	public void processFinish(String output) {
		Toast.makeText(TaskDetailActivity.this, output, Toast.LENGTH_LONG).show();

	}
}