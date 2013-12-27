/*
 * DetailActivity.java
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
	
	private Uri jobUri;

	HTTPTask asyncTask;

	AsyncTask<Void, Void, Void> mUpdateTask;

	private Cursor cursor;

	private String mCalendarId;

	// Member variables that are passed to map activity
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

		if (bundle != null) {
			// todoUri retrieved from saved instance
			jobUri = (Uri) bundle.getParcelable(TaskContentProvider.CONTENT_ITEM_TYPE);
		} else {
			// todoUri passed from the list activity or the intent service
			jobUri = extras.getParcelable(TaskContentProvider.CONTENT_ITEM_TYPE);
			Log.d(TAG, "jobUri: " + jobUri);
		}
		
		fillData(jobUri);

		setTitle(mDepartment);

		mBtnStart.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String action;
				long now = (System.currentTimeMillis()) / 1000L;
				final String startTime = String.valueOf(now);				
				
				if (mBtnStart.getText().equals("Start")) {					
					Date d = new Date(now * 1000);
					mTxtStartText.setText(DateFormat.format("dd/MM hh:mm", d));
					ContentValues values = new ContentValues();					
					values.put(TaskTable.COLUMN_STATUS, "started");
					values.put(TaskTable.COLUMN_START_ACTUAL, startTime);
					getContentResolver().update(jobUri, values, null, null);
					action = "start";
				} else {
					action = "resume";
				}
				serverAction(action, startTime);				
				updateButtons("started");
			}
		});
		
		mBtnPause.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {				
				long now = (System.currentTimeMillis()) / 1000L;
				final String endTime = String.valueOf(now);

				ContentValues values = new ContentValues();				
				values.put(TaskTable.COLUMN_STATUS, "paused");
				getContentResolver().update(jobUri, values, null, null);
				
				serverAction("pause", endTime);
				updateButtons("paused");
			}
		});

		mBtnEnd.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				long now = (System.currentTimeMillis()) / 1000L;
				final String endTime = String.valueOf(now);
				
				Date d = new Date(now * 1000);
				mTxtStopText.setText(DateFormat.format("dd/MM hh:mm", d));				
				ContentValues values = new ContentValues();
				values.put(TaskTable.COLUMN_END_ACTUAL, now);
				values.put(TaskTable.COLUMN_STATUS, "completed");
				getContentResolver().update(jobUri, values, null, null);
				
				serverAction("end", endTime);
				updateButtons("completed");
			}
		});
		
	}
	
	private void updateButtons(String status) {
		if (status.equals("outstanding")) {
			mBtnStart.setEnabled(true);
			mBtnPause.setEnabled(false);
			mBtnEnd.setEnabled(false);
		} else if (status.equals("started")) {
			mBtnStart.setEnabled(false);
			mBtnPause.setEnabled(true);
			mBtnEnd.setEnabled(true);
		} else if (status.equals("paused")) {
			mBtnStart.setText("Resume");
			mBtnStart.setEnabled(true);
			mBtnPause.setEnabled(false);
			mBtnEnd.setEnabled(false);
		} else if (status.equals("completed")) {
			mBtnStart.setEnabled(false);
			mBtnPause.setEnabled(false);
			mBtnEnd.setEnabled(false);
		}
	}
	
	private void serverAction(String action, String value) {
		ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
		nameValuePairs.add(new BasicNameValuePair("calendar_id", mCalendarId));
		nameValuePairs.add(new BasicNameValuePair("action", action));		
		nameValuePairs.add(new BasicNameValuePair("value", value));
		doAsyncTask(nameValuePairs);
	}

	@SuppressWarnings("unchecked")
	private void doAsyncTask(ArrayList<NameValuePair> nameValuePairs) {
		asyncTask = new HTTPTask();
		asyncTask.delegate = JobDetailActivity.this;
		asyncTask.execute(nameValuePairs);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.job_detail_menu, menu);
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
			mCalendarId = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_CALENDAR_ID));
			mDepartment = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_DEPARTMENT));
			mClient = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_CLIENT_NAME));
			String address1 = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_ADDRESS1));
			String address2 = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_ADDRESS2));
			String city = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_CITY));
			mLocationName = address1 + ", " + address2 + ", " + city;
			String status = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_STATUS));
			updateButtons(status);
			mTxtLocationText.setText(mLocationName);
			mTxtClientText.setText(mClient);
			// Obtain Unix time from stored database field and display real date
			long unixTime = cursor.getLong(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_START_ACTUAL));
			Date d = new Date(unixTime * 1000);
			mTxtStartText.setText(DateFormat.format("dd/MM hh:mm", d));
			cursor.close();
		}
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// saveState();
		outState.putParcelable(TaskContentProvider.CONTENT_ITEM_TYPE, jobUri);
	}

	@Override
	protected void onPause() {
		super.onPause();
		// saveState();
	}

	@Override
	public void asyncProcessFinish(String output) {
		Toast.makeText(JobDetailActivity.this, output, Toast.LENGTH_SHORT).show();
	}
}