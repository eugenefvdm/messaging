package com.messaging.push;

import java.util.Date;

import com.messaging.push.contentprovider.MyTaskContentProvider;
import com.messaging.push.db.TaskTable;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/*
 * 
 */
public class TaskDetailActivity extends Activity implements AsyncResponse {
	private Spinner mDepartment;
	private EditText mTitleText;
	private EditText mBodyText;

	private TextView mStartText;

	private Uri todoUri;

	HTTPTask asyncTask;

	private Cursor cursor;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.task_edit);

		mDepartment = (Spinner) findViewById(R.id.category);
		mTitleText = (EditText) findViewById(R.id.task_edit_summary);
		mBodyText = (EditText) findViewById(R.id.task_edit_description);		

		Button startButton = (Button) findViewById(R.id.task_edit_start_button);
		mStartText = (TextView) findViewById(R.id.task_edit_start_text);

		Button stopButton = (Button) findViewById(R.id.task_edit_stop);

		Button pauseButton = (Button) findViewById(R.id.task_edit_pause);

		Bundle extras = getIntent().getExtras();
		
		// The taskUri can either come from a saved instance or it could have been passed from the list activity		
		if (bundle != null) {
			// todoUri retrieved from saved instance
			todoUri = (Uri) bundle.getParcelable(MyTaskContentProvider.CONTENT_ITEM_TYPE);
		} else {
			todoUri = extras.getParcelable(MyTaskContentProvider.CONTENT_ITEM_TYPE);
		}
		fillData(todoUri);

		startButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				long unixTime = System.currentTimeMillis() / 1000L;
				Date d = new Date(unixTime * 1000);
				ContentValues values = new ContentValues();
				values.put(TaskTable.COLUMN_START, unixTime);
				getContentResolver().update(todoUri, values, null, null);
				mStartText.setText(d.toString());
				// Send to server
				String[] projection = {
						TaskTable.COLUMN_CLIENT, TaskTable.COLUMN_ADDRESS,
						TaskTable.COLUMN_DEPARTMENT, TaskTable.COLUMN_START,
						TaskTable.COLUMN_TICKET_ID };
				cursor = getContentResolver().query(todoUri, projection, null, null, null);
				cursor.moveToFirst();
				String ticket_id = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_TICKET_ID));
				Long start_actual = cursor.getLong(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_START));
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
				TaskTable.COLUMN_CLIENT, TaskTable.COLUMN_ADDRESS,
				TaskTable.COLUMN_DEPARTMENT, TaskTable.COLUMN_START,
				TaskTable.COLUMN_TICKET_ID };
		cursor = getContentResolver().query(uri, projection, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();

			String category = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_DEPARTMENT));

			// Fill up the Departments
			for (int i = 0; i < mDepartment.getCount(); i++) {
				String s = (String) mDepartment.getItemAtPosition(i);
				if (s.equalsIgnoreCase(category)) {
					mDepartment.setSelection(i);
				}
			}

			// Work out real date from Unix time
			long unixTime = cursor.getLong(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_START));
			Date d = new Date(unixTime * 1000);
			mStartText.setText(d.toString());

			mTitleText.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_CLIENT)));
			mBodyText.setText(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_ADDRESS)));

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
		String category = (String) mDepartment.getSelectedItem();
		String summary = mTitleText.getText().toString();
		String description = mBodyText.getText().toString();
		// String start = mStartText.getText().toString();

		// only save if either summary or description
		// is available

		if (description.length() == 0 && summary.length() == 0) {
			return;
		}

		ContentValues values = new ContentValues();
		values.put(TaskTable.COLUMN_DEPARTMENT, category);
		values.put(TaskTable.COLUMN_CLIENT, summary);
		values.put(TaskTable.COLUMN_ADDRESS, description);
		// values.put(TaskTable.COLUMN_START, start);

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