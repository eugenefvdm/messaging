/*
 * DetailActivity.java
 * 
 * TODO ?avoid bad addresses by checking from sending application
 *  
 */

package com.snowball;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

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
import android.os.AsyncTask;
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
public class JobDetailActivity extends Activity implements AsyncResponse {

	private static final String TAG = "DetailActivity";
	private TextView mTxtLocationText;
	private TextView mTxtClientText;

	private TextView mTxtStartText;
	private TextView mTxtStopText;

	private Button mBtnStart;
	private Button mBtnEnd;
	private Button mBtnPause;

	private Uri todoUri;

	HTTPTask asyncTask;

	AsyncTask<Void, Void, Void> mUpdateTask;

	private Cursor cursor;
	// These variables are passed to map activity
	private String mDepartment, mClient, mLocationName;

	String[] mProjection = {
			TaskTable.COLUMN_USERID, TaskTable.COLUMN_CALENDAR_ID,
			TaskTable.COLUMN_TICKET_ID, TaskTable.COLUMN_CLIENT_NAME,
			TaskTable.COLUMN_ADDRESS1, TaskTable.COLUMN_ADDRESS2,
			TaskTable.COLUMN_CITY, TaskTable.COLUMN_DEPARTMENT,
			TaskTable.COLUMN_START_ACTUAL, TaskTable.COLUMN_END_ACTUAL,
			TaskTable.COLUMN_STATUS };

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.task_edit);

		mTxtLocationText = (TextView) findViewById(R.id.task_location);
		mTxtClientText = (TextView) findViewById(R.id.task_client);

		mTxtStartText = (TextView) findViewById(R.id.task_edit_start_text);
		mTxtStopText = (TextView) findViewById(R.id.task_edit_stop_text);

		mBtnStart = (Button) findViewById(R.id.task_start_button);
		mBtnEnd = (Button) findViewById(R.id.task_finish_button);
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
				// Update start time on GUI and database
				long unixTimeNow = (System.currentTimeMillis()) / 1000L;
				Date d = new Date(unixTimeNow * 1000);
				mTxtStartText.setText(DateFormat.format("dd/MM hh:mm", d));
				ContentValues values = new ContentValues();
				values.put(TaskTable.COLUMN_START_ACTUAL, unixTimeNow);
				values.put(TaskTable.COLUMN_STATUS, "started");
				getContentResolver().update(todoUri, values, null, null);
				// Get values from database for POST
				cursor = getContentResolver().query(todoUri, mProjection, null, null, null);
				cursor.moveToFirst();
				final String userid = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_USERID));
				final String calendar_id = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_CALENDAR_ID));
				final String start = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_START_ACTUAL));
				cursor.close();
				// Prep POST
				ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
				if (mBtnStart.getText().equals("Start")) {
					nameValuePairs.add(new BasicNameValuePair("action", "start"));
				} else {
					nameValuePairs.add(new BasicNameValuePair("action", "restart"));	
				}				
				nameValuePairs.add(new BasicNameValuePair("userid", userid));
				nameValuePairs.add(new BasicNameValuePair("calendar_id", calendar_id));
				nameValuePairs.add(new BasicNameValuePair("start", start));
				doAsyncTask(nameValuePairs);
				setButtonStatus("started");
				// mBtnPause.setEnabled(true);
				// mBtnEnd.setEnabled(true);
				// mBtnStart.setEnabled(false);
			}
		});

		mBtnEnd.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				long unixTimeNow = (System.currentTimeMillis()) / 1000L;
				Date d = new Date(unixTimeNow * 1000);
				mTxtStopText.setText(DateFormat.format("dd/MM hh:mm", d));
				ContentValues values = new ContentValues();
				values.put(TaskTable.COLUMN_END_ACTUAL, unixTimeNow);
				values.put(TaskTable.COLUMN_STATUS, "completed");
				getContentResolver().update(todoUri, values, null, null);
				// Get values from database for POST
				cursor = getContentResolver().query(todoUri, mProjection, null, null, null);
				cursor.moveToFirst();
				final String userid = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_USERID));
				final String calendar_id = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_CALENDAR_ID));
				cursor.close();
				// Prep POST
				ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
				nameValuePairs.add(new BasicNameValuePair("action", "end"));
				final String end = String.valueOf(unixTimeNow);
				nameValuePairs.add(new BasicNameValuePair("userid", userid));
				nameValuePairs.add(new BasicNameValuePair("calendar_id", calendar_id));
				nameValuePairs.add(new BasicNameValuePair("end", end));
				doAsyncTask(nameValuePairs);
				setButtonStatus("completed");
				// mBtnPause.setEnabled(false);
				// mBtnEnd.setEnabled(false);
			}
		});

		mBtnPause.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Get current time
				long unixTime = (System.currentTimeMillis()) / 1000L;
				final String end = String.valueOf(unixTime);

				ContentValues values = new ContentValues();
				values.put(TaskTable.COLUMN_START_ACTUAL, unixTime);
				values.put(TaskTable.COLUMN_STATUS, "paused");
				getContentResolver().update(todoUri, values, null, null);

				cursor = getContentResolver().query(todoUri, mProjection, null, null, null);
				cursor.moveToFirst();
				final String userid = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_USERID));
				final String calendar_id = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_CALENDAR_ID));
				// final String start =
				// cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_START_ACTUAL));
				cursor.close();
				ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);
				nameValuePairs.add(new BasicNameValuePair("action", "pause"));
				nameValuePairs.add(new BasicNameValuePair("userid", userid));
				nameValuePairs.add(new BasicNameValuePair("calendar_id", calendar_id));
				nameValuePairs.add(new BasicNameValuePair("end", end));
				doAsyncTask(nameValuePairs);
				setButtonStatus("paused");
				// mBtnStart.setText("Restart");
				// mBtnStart.setEnabled(true);
				// mBtnPause.setEnabled(false);
				// mBtnEnd.setEnabled(false);
			}
		});

	}

	// Added to avoid
	// "Type safety: A generic array of Map<String,String> is created for a varargs parameter"
	// See
	// http://stackoverflow.com/questions/1445233/is-it-possible-to-solve-the-a-generic-array-of-t-is-created-for-a-varargs-param
	@SuppressWarnings("unchecked")
	private void doAsyncTask(ArrayList<NameValuePair> nameValuePairs) {
		asyncTask = new HTTPTask();
		asyncTask.delegate = JobDetailActivity.this;
		asyncTask.execute(nameValuePairs);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.list_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.map:
			showMap();
			return true;
		case android.R.id.home:
			finish();
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void showMap() {
		List<Address> foundGeocode = null;
		try {
			foundGeocode = new Geocoder(this).getFromLocationName(mLocationName, 1);
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
		} catch (IOException e) {
			Log.e(TAG, "GPS problem or activity could not be started");
			Toast.makeText(this, "Cannot determine GPS for " + mLocationName, Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
	}

	private void fillData(Uri uri) {
		cursor = getContentResolver().query(uri, mProjection, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
			mDepartment = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_DEPARTMENT));
			mClient = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_CLIENT_NAME));
			String address1 = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_ADDRESS1));
			String address2 = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_ADDRESS2));
			String city = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_CITY));
			mLocationName = address1 + ", " + address2 + ", " + city;
			String status = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_STATUS));
			setButtonStatus(status);
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

	private void setButtonStatus(String status) {
		if (status.equals("outstanding")) {
			mBtnStart.setEnabled(true);
			mBtnPause.setEnabled(false);
			mBtnEnd.setEnabled(false);
		} else if (status.equals("started")) {
			mBtnStart.setEnabled(false);
			mBtnPause.setEnabled(true);
			mBtnEnd.setEnabled(true);
		} else if (status.equals("paused")) {
			mBtnStart.setText("Restart");
			mBtnStart.setEnabled(true);
			mBtnPause.setEnabled(false);
			mBtnEnd.setEnabled(false);
		} else if (status.equals("completed")) {
			mBtnStart.setEnabled(false);
			mBtnPause.setEnabled(false);
			mBtnEnd.setEnabled(false);
		}
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// saveState();
		outState.putParcelable(TaskContentProvider.CONTENT_ITEM_TYPE, todoUri);
	}

	@Override
	protected void onPause() {
		super.onPause();
		// saveState();
	}

	// private void saveState() {
	//
	// String summary = mTxtLocationText.getText().toString();
	// String description = mTxtClientText.getText().toString();
	//
	// ContentValues values = new ContentValues();
	//
	// values.put(TaskTable.COLUMN_CLIENT, summary);
	// values.put(TaskTable.COLUMN_CITY, description);
	//
	// if (todoUri == null) {
	// // New task
	// todoUri = getContentResolver().insert(TaskContentProvider.CONTENT_URI,
	// values);
	// } else {
	// // Update task
	// getContentResolver().update(todoUri, values, null, null);
	// }
	// }

	@Override
	public void asyncProcessFinish(String output) {
		Toast.makeText(JobDetailActivity.this, output, Toast.LENGTH_SHORT).show();
	}
}