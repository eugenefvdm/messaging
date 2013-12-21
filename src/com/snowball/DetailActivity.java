/*
 * DetailActivity.java
 * 
 * TODO avoid bad addresses by checking from sending application
 *  
 */

package com.snowball;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import com.snowball.R;
import com.snowball.db.TaskContentProvider;
import com.snowball.db.TaskTable;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
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

	private static final String TAG = "DetailActivity";
	private TextView mTxtLocationText;
	private TextView mTxtClientText;

	private TextView mTxtStartText;
	private TextView mTxtStopText;

	private Button mBtnStart;
	private Button mBtnFinish;
	private Button mBtnPause;

	private Uri todoUri;

	HTTPTask asyncTask;

	private Cursor cursor;
	private String mClient;
	private String mDepartment;
	private String mLocationName;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.task_edit);

		mTxtLocationText = (TextView) findViewById(R.id.task_location);
		mTxtClientText = (TextView) findViewById(R.id.task_client);

		mTxtStartText = (TextView) findViewById(R.id.task_edit_start_text);
		mTxtStopText = (TextView) findViewById(R.id.task_edit_stop_text);

		mBtnStart = (Button) findViewById(R.id.task_start_button);
		mBtnFinish = (Button) findViewById(R.id.task_finish_button);
		mBtnPause = (Button) findViewById(R.id.task_pause_button);

		getActionBar().setDisplayHomeAsUpEnabled(true);

		Bundle extras = getIntent().getExtras();

		// The taskUri can either come from a saved instance or it could have
		// been passed from the list activity
		if (bundle != null) {
			// todoUri retrieved from saved instance
			todoUri = (Uri) bundle.getParcelable(TaskContentProvider.CONTENT_ITEM_TYPE);
		} else {
			todoUri = extras.getParcelable(TaskContentProvider.CONTENT_ITEM_TYPE);
		}
		fillData(todoUri);

		setTitle(mDepartment);

		mBtnStart.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				long unixTime = (System.currentTimeMillis()) / 1000L;
				Date d = new Date(unixTime * 1000);
				mTxtStartText.setText(DateFormat.format("dd/MM hh:mm", d));
				updateTimerData("start", unixTime);
				mBtnPause.setEnabled(true);
				mBtnFinish.setEnabled(true);
				mBtnStart.setEnabled(false);
				// getContentResolver().update(todoUri, values, null, null);
				// mStartText.setText(DateFormat.format("dd/MM hh:mm", d));
				// // Send to server
				// String[] projection = {
				// TaskTable.COLUMN_CLIENT, TaskTable.COLUMN_CITY,
				// TaskTable.COLUMN_DEPARTMENT, TaskTable.COLUMN_START_ACTUAL,
				// TaskTable.COLUMN_TICKET_ID };
				// cursor = getContentResolver().query(todoUri, projection,
				// null, null, null);
				// cursor.moveToFirst();
				// String ticket_id =
				// cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_TICKET_ID));
				// Long start_actual =
				// cursor.getLong(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_START_ACTUAL));
				// cursor.close();
				// // String id = todoUri.getPathSegments().get(1);
				// String action = "start";
				// // String id = String.valueOf(unixTime);
				// asyncTask = new HTTPTask();
				// asyncTask.delegate = TaskDetailActivity.this;
				// asyncTask.execute("http://196.201.6.235/whmcs/modules/addons/messaging/action.php?action="
				// + action + "&id=" + ticket_id + "&start_actual=" +
				// start_actual);
				// pauseButton.setEnabled(true);
				// finishButton.setEnabled(true);
				// startButton.setEnabled(false);
			}
		});

		mBtnFinish.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				long unixTime = (System.currentTimeMillis()) / 1000L;
				Date d = new Date(unixTime * 1000);
				mTxtStopText.setText(DateFormat.format("dd/MM hh:mm", d));
				updateTimerData("stop", unixTime);
				mBtnPause.setEnabled(false);
				mBtnFinish.setEnabled(false);
			}
		});

		mBtnPause.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Put a start time next to this task in the database

			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.list_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		List<Address> foundGeocode = null;
		switch (item.getItemId()) {
		case R.id.map:
			try {
				foundGeocode = new Geocoder(this).getFromLocationName(mLocationName, 1);				
			} catch (IOException e) {
				Toast.makeText(this, "Cannot determine GPS for " + mLocationName, Toast.LENGTH_SHORT).show();
				// e.printStackTrace();
				return false;
			}
			double lat = foundGeocode.get(0).getLatitude();																
			double lng = foundGeocode.get(0).getLongitude();
			Log.d(TAG, "lat" + Double.valueOf(lat));
			Log.d(TAG, "lng" + Double.valueOf(lng));
			Intent i = new Intent(this, MapPane.class);			
			i.putExtra("department", mDepartment);
			i.putExtra("client", mClient);
			i.putExtra("address", mLocationName);
			i.putExtra("lat", lat);
			i.putExtra("lng", lng);
			startActivity(i);
			return true;
		case android.R.id.home:
			finish();
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Update SQLite with values (start or stop) and sends message to messaging
	 * server to indicate such
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
				TaskTable.COLUMN_CLIENT_NAME, TaskTable.COLUMN_ADDRESS1,
				TaskTable.COLUMN_ADDRESS2, TaskTable.COLUMN_CITY,
				TaskTable.COLUMN_DEPARTMENT, TaskTable.COLUMN_START_ACTUAL,
				TaskTable.COLUMN_TICKET_ID };
		cursor = getContentResolver().query(uri, projection, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
			mDepartment = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_DEPARTMENT));
			mClient = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_CLIENT_NAME));
			String address1 = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_ADDRESS1));
			String address2 = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_ADDRESS2));
			String city = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_CITY));
			mLocationName = address1 + ", " + address2 + ", " + city;
			mTxtLocationText.setText(mLocationName);
			mTxtClientText.setText(mClient);
			// Obtain Unix time from stored database field and work out real
			// date
			long unixTime = cursor.getLong(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_START_ACTUAL));
			Date d = new Date(unixTime * 1000);
			mTxtStartText.setText(DateFormat.format("dd/MM hh:mm", d));
			cursor.close();
		}
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		//saveState();
		outState.putParcelable(TaskContentProvider.CONTENT_ITEM_TYPE, todoUri);
	}

	@Override
	protected void onPause() {
		super.onPause();
		//saveState();
	}

//	private void saveState() {
//		
//		String summary = mTxtLocationText.getText().toString();
//		String description = mTxtClientText.getText().toString();
//
//		ContentValues values = new ContentValues();
//		
//		values.put(TaskTable.COLUMN_CLIENT, summary);
//		values.put(TaskTable.COLUMN_CITY, description);
//
//		if (todoUri == null) {
//			// New task
//			todoUri = getContentResolver().insert(TaskContentProvider.CONTENT_URI, values);
//		} else {
//			// Update task
//			getContentResolver().update(todoUri, values, null, null);
//		}
//	}

	@Override
	public void processFinish(String output) {
		Toast.makeText(DetailActivity.this, output, Toast.LENGTH_SHORT).show();

	}
}