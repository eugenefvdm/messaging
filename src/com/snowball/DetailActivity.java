package com.snowball;

import java.util.Date;

import com.snowball.R;
import com.snowball.db.TaskContentProvider;
import com.snowball.db.TaskTable;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/*
 * 
 */
public class DetailActivity extends Activity implements AsyncResponse {
	private TextView mDepartment;
	private TextView mLocationText;
	private TextView mClientText;

	private TextView mStartText;
	private TextView mStopText;
	
	private Button startButton;
	private Button finishButton;
	private Button pauseButton;

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
		mStopText = (TextView) findViewById(R.id.task_edit_stop_text);

		startButton = (Button) findViewById(R.id.task_edit_start_button);
		finishButton = (Button) findViewById(R.id.task_edit_finish);
		pauseButton = (Button) findViewById(R.id.task_edit_pause);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);

		Bundle extras = getIntent().getExtras();

		// The taskUri can either come from a saved instance or it could have been passed from the list activity
		if (bundle != null) {
			// todoUri retrieved from saved instance
			todoUri = (Uri) bundle.getParcelable(TaskContentProvider.CONTENT_ITEM_TYPE);
		} else {
			todoUri = extras.getParcelable(TaskContentProvider.CONTENT_ITEM_TYPE);
		}
		fillData(todoUri);

		startButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {				
				long unixTime = (System.currentTimeMillis()) / 1000L;
				Date d = new Date(unixTime * 1000);
				mStartText.setText(DateFormat.format("dd/MM hh:mm", d));				
				updateTimerData("start", unixTime);
				pauseButton.setEnabled(true);
				finishButton.setEnabled(true);
				startButton.setEnabled(false);
//				getContentResolver().update(todoUri, values, null, null);
//				mStartText.setText(DateFormat.format("dd/MM hh:mm", d));
//				// Send to server
//				String[] projection = {
//						TaskTable.COLUMN_CLIENT, TaskTable.COLUMN_CITY,
//						TaskTable.COLUMN_DEPARTMENT, TaskTable.COLUMN_START_ACTUAL,
//						TaskTable.COLUMN_TICKET_ID };
//				cursor = getContentResolver().query(todoUri, projection, null, null, null);
//				cursor.moveToFirst();
//				String ticket_id = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_TICKET_ID));
//				Long start_actual = cursor.getLong(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_START_ACTUAL));
//				cursor.close();
//				// String id = todoUri.getPathSegments().get(1);
//				String action = "start";
//				// String id = String.valueOf(unixTime);
//				asyncTask = new HTTPTask();
//				asyncTask.delegate = TaskDetailActivity.this;
//				asyncTask.execute("http://196.201.6.235/whmcs/modules/addons/messaging/action.php?action=" + action + "&id=" + ticket_id + "&start_actual=" + start_actual);
//				pauseButton.setEnabled(true);
//				finishButton.setEnabled(true);
//				startButton.setEnabled(false);
			}
		});
		
		finishButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {				
				long unixTime = (System.currentTimeMillis()) / 1000L;
				Date d = new Date(unixTime * 1000);
				mStopText.setText(DateFormat.format("dd/MM hh:mm", d));
				updateTimerData("stop", unixTime);												
				pauseButton.setEnabled(false);
				finishButton.setEnabled(false);				
			}
		});

		pauseButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Put a start time next to this task in the database

			}
		});

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case android.R.id.home:
	        //do your own thing here
	    	finish();
	        //return true;
	    default: return super.onOptionsItemSelected(item);  
	    }
	}
	
	/**
	 * Update SQLite with values (start or stop) and sends message to messaging server to indicate such
	 * 
	 * @param values
	 * @param action
	 */
	private void updateTimerData(String action, long unixTime) {
		ContentValues values = new ContentValues();		
		if (action.equals("start")) {
			values.put(TaskTable.COLUMN_START_ACTUAL, unixTime);				
		} else if (action.equals("stop")) {
			values.put(TaskTable.COLUMN_STOP_ACTUAL, unixTime);
		}		
		getContentResolver().update(todoUri, values, null, null);		
		String[] projection = {
				TaskTable.COLUMN_TICKET_ID, TaskTable.COLUMN_START_ACTUAL,
				TaskTable.COLUMN_STOP_ACTUAL };
		cursor = getContentResolver().query(todoUri, projection, null, null, null);
		cursor.moveToFirst();
		String ticket_id = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_TICKET_ID));
		
		cursor.close();
		asyncTask = new HTTPTask();
		asyncTask.delegate = DetailActivity.this;
		// TODO Move URL to preferences
		asyncTask.execute("http://196.201.6.235/whmcs/modules/addons/messaging/action.php?action=" + action + "&id=" + ticket_id + "&value=" + unixTime);
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
		outState.putParcelable(TaskContentProvider.CONTENT_ITEM_TYPE, todoUri);
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
			todoUri = getContentResolver().insert(TaskContentProvider.CONTENT_URI, values);
		} else {
			// Update task
			getContentResolver().update(todoUri, values, null, null);
		}
	}

	@Override
	public void processFinish(String output) {
		Toast.makeText(DetailActivity.this, output, Toast.LENGTH_SHORT).show();

	}
}