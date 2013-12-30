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
import com.snowball.db.JobContentProvider;
import com.snowball.db.JobTable;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
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
import android.widget.EditText;
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
	private Button mBtnCancel;
	private Button mBtnDecline;

	// GPSTracker class
	GPSTracker gps;

	private Uri jobUri;

	HTTPTask asyncTask;
	AsyncTask<Void, Void, Void> mUpdateJob;

	private Cursor cursor;
	private String mCalendarId;

	// Member variables that are passed to map activity and used to set activity
	// title
	private String mDepartment, mClient, mAddressSuburb, mCity;

	String[] mProjection = {
			JobTable.COLUMN_USERID, JobTable.COLUMN_CALENDAR_ID,
			JobTable.COLUMN_TICKET_ID, JobTable.COLUMN_CLIENT_NAME,
			JobTable.COLUMN_ADDRESS1, JobTable.COLUMN_ADDRESS2,
			JobTable.COLUMN_CITY, JobTable.COLUMN_DEPARTMENT,
			JobTable.COLUMN_START_ACTUAL, JobTable.COLUMN_END_ACTUAL,
			JobTable.COLUMN_STATUS };

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.job_detail);

		mTxtLocationText = (TextView) findViewById(R.id.job_location);
		mTxtClientText = (TextView) findViewById(R.id.job_client);

		mTxtStartText = (TextView) findViewById(R.id.job_edit_start_text);
		mTxtStopText = (TextView) findViewById(R.id.job_edit_stop_text);

		mBtnStart = (Button) findViewById(R.id.job_start_button);
		mBtnEnd = (Button) findViewById(R.id.job_finish_button);
		mBtnPause = (Button) findViewById(R.id.job_pause_button);
		mBtnCancel = (Button) findViewById(R.id.job_cancel_button);
		mBtnDecline = (Button) findViewById(R.id.job_decline_button);

		Bundle extras = getIntent().getExtras();
		if (bundle != null) {
			// todoUri retrieved from saved instance
			jobUri = (Uri) bundle.getParcelable(JobContentProvider.CONTENT_ITEM_TYPE);
		} else {
			// todoUri passed from the list activity or the intent service
			jobUri = extras.getParcelable(JobContentProvider.CONTENT_ITEM_TYPE);
			Log.d(TAG, "jobUri: " + jobUri);
		}
		fillData(jobUri);

		ActionBar ab = getActionBar();
		ab.setDisplayHomeAsUpEnabled(true);
		ab.setTitle(mDepartment);
		ab.setSubtitle(mCity);

		mBtnStart.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String action;
				long now = (System.currentTimeMillis()) / 1000L;
				final String startTime = String.valueOf(now);
				if (mBtnStart.getText().equals("Start")) {
					Date d = new Date(now * 1000);
					mTxtStartText.setText(DateFormat.format("dd/MM hh:mm", d));
					ContentValues values = new ContentValues();
					values.put(JobTable.COLUMN_STATUS, "started");
					values.put(JobTable.COLUMN_START_ACTUAL, startTime);
					getContentResolver().update(jobUri, values, null, null);
					action = "start";
				} else {
					action = "resume";
				}
				serverAction(action, startTime, null);
				setButtonStatus("started");
			}
		});

		mBtnPause.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				long now = (System.currentTimeMillis()) / 1000L;
				final String endTime = String.valueOf(now);

				ContentValues values = new ContentValues();
				values.put(JobTable.COLUMN_STATUS, "paused");
				getContentResolver().update(jobUri, values, null, null);

				serverAction("pause", endTime, null);
				setButtonStatus("paused");
			}
		});

		mBtnEnd.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				long now = (System.currentTimeMillis()) / 1000L;
				final String endTime = String.valueOf(now);

				Date d = new Date(now * 1000);
				mTxtStopText.setText(DateFormat.format("dd/MM hh:mm", d));
				ContentValues values = new ContentValues();
				values.put(JobTable.COLUMN_END_ACTUAL, now);
				values.put(JobTable.COLUMN_STATUS, "completed");
				getContentResolver().update(jobUri, values, null, null);

				serverAction("end", endTime, null);
				setButtonStatus("completed");
			}
		});

		mBtnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				final EditText input = new EditText(JobDetailActivity.this);
				new AlertDialog.Builder(JobDetailActivity.this).setTitle("Enter Cancel Reason").setView(input).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String reason = input.getText().toString();
						serverAction("cancel", reason, "");
						// setButtonStatus("cancelled");
					}
				}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Do nothing.
					}
				}).show();
			}
		});

		mBtnDecline.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				final EditText input = new EditText(JobDetailActivity.this);
				new AlertDialog.Builder(JobDetailActivity.this).setTitle("Enter Decline Reason").setView(input).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String reason = input.getText().toString();
						serverAction("decline", reason, "");
						// setButtonStatus("declined");
					}
				}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Do nothing.
					}
				}).show();
			}
		});
	}

	private void setButtonStatus(String status) {
		if (status.equals("outstanding")) {
			mBtnStart.setEnabled(true);
			mBtnPause.setEnabled(false);
			mBtnEnd.setEnabled(false);
			mBtnDecline.setEnabled(true);
		} else if (status.equals("started")) {
			mBtnStart.setEnabled(false);
			mBtnPause.setEnabled(true);
			mBtnEnd.setEnabled(true);
			mBtnCancel.setEnabled(false);
			mBtnDecline.setEnabled(false);
		} else if (status.equals("paused")) {
			mBtnStart.setText("Resume");
			mBtnStart.setEnabled(true);
			mBtnPause.setEnabled(false);
			mBtnEnd.setEnabled(false);
			mBtnCancel.setEnabled(false);
			mBtnDecline.setEnabled(false);
		} else if (status.equals("completed")) {
			mBtnStart.setEnabled(false);
			mBtnPause.setEnabled(false);
			mBtnEnd.setEnabled(false);
			mBtnDecline.setEnabled(false);
		} else if (status.equals("declined")) {
			mBtnStart.setEnabled(false);
			mBtnPause.setEnabled(false);
			mBtnEnd.setEnabled(false);
			mBtnDecline.setEnabled(false);
		}
	}

//	private String getCurrentLocation() {
//		gps = new GPSTracker(JobDetailActivity.this);
//		// check if GPS enabled
//		if (gps.canGetLocation()) {
//			double latitude = gps.getLatitude();
//			double longitude = gps.getLongitude();
//			String message = "GPS Location is - \nLat: " + latitude + "\nLong: " + longitude;
//			Log.w(TAG, message);
//			return ("GPS:" + latitude + "," + longitude);
//		} else {
//			Log.e(TAG, "Can't get location, GPS or Network is not enabled");
//			// Ask user to enable GPS/network in settings
//			gps.showSettingsAlert();
//			return null;
//		}
//	}

	/**
	 * Post a message to the server for this calendar ID
	 * 
	 * @param action
	 * @param value
	 * @param extra
	 *            Optional parameter used by decline
	 */
	private void serverAction(String action, String value, String extra) {
		ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
		nameValuePairs.add(new BasicNameValuePair("calendar_id", mCalendarId));
		nameValuePairs.add(new BasicNameValuePair("action", action));
		nameValuePairs.add(new BasicNameValuePair("value", value));

		gps = new GPSTracker(JobDetailActivity.this);
		// check if GPS enabled
		if (gps.canGetLocation()) {
			double lat = gps.getLatitude();
			double lng = gps.getLongitude();
			String message = "GPS Location is - \nLat: " + lat + "\nLong: " + lng;
			Log.w(TAG, message);			
			nameValuePairs.add(new BasicNameValuePair("lat", String.valueOf(lat)));
			nameValuePairs.add(new BasicNameValuePair("lng", String.valueOf(lng)));
		} else {
			Log.e(TAG, "Can't get location, GPS or Network is not enabled");
			// Ask user to enable GPS/network in settings
			gps.showSettingsAlert();
		}

		if (extra != null) {
			nameValuePairs.add(new BasicNameValuePair("extra", extra));
		}
		doAsyncTask(nameValuePairs);
	}

	// I had problems with this optional parameters working
	// private void serverAction(String action, String value) {
	// serverAction(action, value);
	// }

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
			foundGeocode = new Geocoder(this).getFromLocationName(mAddressSuburb, 1);
			double lat = foundGeocode.get(0).getLatitude();
			double lng = foundGeocode.get(0).getLongitude();
			Log.d(TAG, "lat" + Double.valueOf(lat));
			Log.d(TAG, "lng" + Double.valueOf(lng));
			Intent i = new Intent(this, MapPane.class);
			i.putExtra("department", mDepartment);
			i.putExtra("client", mClient);
			i.putExtra("address", mAddressSuburb);
			i.putExtra("lat", lat);
			i.putExtra("lng", lng);
			startActivity(i);
		} catch (IOException e) {
			Log.e(TAG, "GPS problem or activity could not be started");
			Toast.makeText(this, "Cannot determine GPS for " + mAddressSuburb, Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
	}

	private void fillData(Uri uri) {
		cursor = getContentResolver().query(uri, mProjection, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
			mCalendarId = cursor.getString(cursor.getColumnIndexOrThrow(JobTable.COLUMN_CALENDAR_ID));
			mDepartment = cursor.getString(cursor.getColumnIndexOrThrow(JobTable.COLUMN_DEPARTMENT));
			mClient = cursor.getString(cursor.getColumnIndexOrThrow(JobTable.COLUMN_CLIENT_NAME));
			String address1 = cursor.getString(cursor.getColumnIndexOrThrow(JobTable.COLUMN_ADDRESS1));
			String address2 = cursor.getString(cursor.getColumnIndexOrThrow(JobTable.COLUMN_ADDRESS2));
			mAddressSuburb = address1 + ", " + address2;
			mCity = cursor.getString(cursor.getColumnIndexOrThrow(JobTable.COLUMN_CITY));
			String status = cursor.getString(cursor.getColumnIndexOrThrow(JobTable.COLUMN_STATUS));
			setButtonStatus(status);
			mTxtLocationText.setText(mAddressSuburb);
			mTxtClientText.setText(mClient);
			// Obtain Unix time from stored database field and display real date
			long unixTime = cursor.getLong(cursor.getColumnIndexOrThrow(JobTable.COLUMN_START_ACTUAL));
			if (unixTime != 0) {
				Date d = new Date(unixTime * 1000);
				mTxtStartText.setText(DateFormat.format("dd/MM hh:mm", d));
			}
			cursor.close();
		}
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// saveState();
		outState.putParcelable(JobContentProvider.CONTENT_ITEM_TYPE, jobUri);
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