/*
 * DetailActivity.java
 * 
 */

package com.snowball;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.snowball.R;
import com.snowball.db.MyContentProvider;
import com.snowball.db.Table;

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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/*
 * 
 */
public class JobDetailActivity extends Activity implements AsyncResponse {

	private static final String TAG = "JobDetailActivity";
	private TextView mTxtLocationText;
	private TextView mTxtClientText;
	
	private ListView mNotesList;

	private TextView mTxtStartText;
	private TextView mTxtStopText;

	private Button mCustomFields;
	private Button mBtnStart;
	private Button mBtnEnd;
	private Button mBtnPause;
	private Button mBtnCancel;
	private Button mBtnDecline;
	
	private ArrayList<String> mNotes = new ArrayList<String>();

	// GPSTracker class
	GPSTracker mGps;

	private Uri mJobUri;

	// TODO Doesn't need to be a member variable
	HTTPTask mAsyncTask;
	AsyncTask<Void, Void, Void> mUpdateJob;

	private Cursor mCursor;
	private String mCalendarId;

	// Member variables that are passed to map activity and used to set activity
	// title
	private String mDepartment, mClient, mAddressSuburb, mCity;

	// TODO Doesn't need to be a member variable
	String[] mProjection = {
			Table.COLUMN_JOB_CLIENT_ID, Table.COLUMN_JOB_CALENDAR_ID,
			Table.COLUMN_JOB_TICKET_ID, Table.COLUMN_JOB_CLIENT_NAME,
			Table.COLUMN_JOB_ADDRESS1, Table.COLUMN_JOB_ADDRESS2,
			Table.COLUMN_JOB_CITY, Table.COLUMN_JOB_DEPARTMENT,
			Table.COLUMN_JOB_START_ACTUAL, Table.COLUMN_JOB_END_ACTUAL,
			Table.COLUMN_JOB_STATUS };

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		
		setContentView(R.layout.job_detail);

		mTxtLocationText = (TextView) findViewById(R.id.job_location);
		mTxtClientText = (TextView) findViewById(R.id.job_client);
		
		mNotesList = (ListView) findViewById(R.id.notes_list);

		mTxtStartText = (TextView) findViewById(R.id.job_edit_start_text);
		mTxtStopText = (TextView) findViewById(R.id.job_edit_stop_text);

		mCustomFields = (Button) findViewById(R.id.job_custom_fields_button);
		mBtnStart = (Button) findViewById(R.id.job_start_button);
		mBtnEnd = (Button) findViewById(R.id.job_finish_button);
		mBtnPause = (Button) findViewById(R.id.job_pause_button);
		mBtnCancel = (Button) findViewById(R.id.job_cancel_button);
		mBtnDecline = (Button) findViewById(R.id.job_decline_button);

		Bundle extras = getIntent().getExtras();
		if (bundle != null) {
			// todoUri retrieved from saved instance
			mJobUri = (Uri) bundle.getParcelable(MyContentProvider.CONTENT_ITEM_TYPE);
		} else {
			// todoUri passed from the list activity or the intent service
			mJobUri = extras.getParcelable(MyContentProvider.CONTENT_ITEM_TYPE);
			Log.d(TAG, "jobUri: " + mJobUri);
		}
		fillData(mJobUri);

		ActionBar ab = getActionBar();
		ab.setDisplayHomeAsUpEnabled(true);
		ab.setTitle(mDepartment);
		ab.setSubtitle(mCity);
		
		mCustomFields.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(JobDetailActivity.this, BuildCustomLayout.class);
				//Intent i = new Intent(JobDetailActivity.this, BuildAlertDialog.class);
				//i.putExtra("custom_fields", custom_fields);				
				startActivity(i); // For result?
			}
		});

		mBtnStart.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String action;
				long now = (System.currentTimeMillis()) / 1000L;
				final String startTime = String.valueOf(now);
				if (mBtnStart.getText().equals("Start")) {
					Date d = new Date(now * 1000);
					mTxtStartText.setText(DateFormat.format("dd/MM hh:mm", d));
					ContentValues values = new ContentValues();
					values.put(Table.COLUMN_JOB_STATUS, "started");
					values.put(Table.COLUMN_JOB_START_ACTUAL, startTime);
					getContentResolver().update(mJobUri, values, null, null);
					action = "start";
				} else {
					action = "resume";
				}
				postToMessagingServer(action, startTime, null);
				setButtonStatus("started");
			}
		});

		mBtnPause.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				long now = (System.currentTimeMillis()) / 1000L;
				final String endTime = String.valueOf(now);

				ContentValues values = new ContentValues();
				values.put(Table.COLUMN_JOB_STATUS, "paused");
				getContentResolver().update(mJobUri, values, null, null);

				postToMessagingServer("pause", endTime, null);
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
				values.put(Table.COLUMN_JOB_END_ACTUAL, now);
				values.put(Table.COLUMN_JOB_STATUS, "completed");
				getContentResolver().update(mJobUri, values, null, null);

				postToMessagingServer("end", endTime, null);
				setButtonStatus("completed");
			}
		});

		mBtnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				final EditText input = new EditText(JobDetailActivity.this);
				new AlertDialog.Builder(JobDetailActivity.this).setTitle("Enter Cancel Reason").setView(input).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String reason = input.getText().toString();
						postToMessagingServer("cancel", reason, "");
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
						postToMessagingServer("decline", reason, "");
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
	 * TODO Looks like extra is never used, also check server side
	 * 
	 * @param action
	 * @param value
	 * @param extra
	 *            Optional parameter used when declining or canceling a job
	 */
	private void postToMessagingServer(String action, String value, String extra) {
		ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("calendar_id", mCalendarId));
		nameValuePairs.add(new BasicNameValuePair("action", action));
		nameValuePairs.add(new BasicNameValuePair("value", value));
		if (extra != null) {
			nameValuePairs.add(new BasicNameValuePair("extra", extra));
		}

		mGps = new GPSTracker(JobDetailActivity.this);
		// check if GPS enabled
		if (mGps.canGetLocation()) {
			double lat = mGps.getLatitude();
			double lng = mGps.getLongitude();
			String message = "GPS Location is - \nLat: " + lat + "\nLong: " + lng;
			Log.w(TAG, message);			
			nameValuePairs.add(new BasicNameValuePair("lat", String.valueOf(lat)));
			nameValuePairs.add(new BasicNameValuePair("lng", String.valueOf(lng)));
		} else {
			Log.e(TAG, "Can't get location, GPS or Network is not enabled");
			// Ask user to enable GPS/network in settings
			mGps.showSettingsAlert();
		}
		
		doAsyncTask(nameValuePairs);
	}

	// I had problems with this optional parameters working so at places I just send null instead of extra
	// private void serverAction(String action, String value) {
	// serverAction(action, value);
	// }

	/**
	 * Send JSON to the Messaging Server asynchronously and receive a callback on asyncProcessFinish
	 * 
	 * @param nameValuePairs
	 */
	@SuppressWarnings("unchecked")
	private void doAsyncTask(ArrayList<NameValuePair> nameValuePairs) {
		mAsyncTask = new HTTPTask(this);
		mAsyncTask.delegate = JobDetailActivity.this;
		mAsyncTask.execute(nameValuePairs);
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

	/**
	 * Look up address and start map activity
	 * 
	 * It appears Geocoder doesn't always work properly, see Stack Overflow
	 * 
	 * http://developer.android.com/reference/android/location/Geocoder.html#getFromLocationName(java.lang.String, int, double, double, double, double)
	 * Note: "It may be useful to call this method from a thread separate from your primary UI thread"
	 */
	private void showMap() {
		List<Address> foundGeocode = null;
		try {
			foundGeocode = new Geocoder(this).getFromLocationName(mAddressSuburb, 1);
			//foundGeocode = new Geocoder(this).getFromLocationName("22 Anesta Street, Stellenbosch, South Africa", 1);
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
		//} catch (IOException e) {
		} catch (Exception e) {
			Log.e(TAG, "GPS problem or activity could not be started");
			Toast.makeText(this, "Cannot determine GPS for " + mAddressSuburb, Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
	}

	/**
	 * Populate the job detail form with values from the database.
	 * Branch off to fillNotes to populate the notes for this job.
	 * 
	 * @param uri Current job passed from activity or intent
	 */
	private void fillData(Uri uri) {
		mCursor = getContentResolver().query(uri, mProjection, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
			Long ticketId = mCursor.getLong(mCursor.getColumnIndexOrThrow(Table.COLUMN_JOB_TICKET_ID));
			mCalendarId = mCursor.getString(mCursor.getColumnIndexOrThrow(Table.COLUMN_JOB_CALENDAR_ID));
			mDepartment = mCursor.getString(mCursor.getColumnIndexOrThrow(Table.COLUMN_JOB_DEPARTMENT));
			mClient = mCursor.getString(mCursor.getColumnIndexOrThrow(Table.COLUMN_JOB_CLIENT_NAME));
			String address1 = mCursor.getString(mCursor.getColumnIndexOrThrow(Table.COLUMN_JOB_ADDRESS1));
			String address2 = mCursor.getString(mCursor.getColumnIndexOrThrow(Table.COLUMN_JOB_ADDRESS2));
			mAddressSuburb = address1 + ", " + address2;
			mCity = mCursor.getString(mCursor.getColumnIndexOrThrow(Table.COLUMN_JOB_CITY));
			String status = mCursor.getString(mCursor.getColumnIndexOrThrow(Table.COLUMN_JOB_STATUS));
			setButtonStatus(status);
			mTxtLocationText.setText(mAddressSuburb);
			mTxtClientText.setText(mClient);
			// Obtain Unix time from stored database field and display real date
			long unixTime = mCursor.getLong(mCursor.getColumnIndexOrThrow(Table.COLUMN_JOB_START_ACTUAL));
			if (unixTime != 0) {
				Date d = new Date(unixTime * 1000);
				mTxtStartText.setText(DateFormat.format("dd/MM hh:mm", d));
			}
			Uri notesUri = Uri.parse(MyContentProvider.CONTENT_URI_NOTES + "/" + ticketId);
			Log.w(TAG, "notesUri: " + notesUri);
			fillNotes(notesUri);
			mCursor.close();			
		}		
	}
	
	/**
	 * Get notes based on JOB uri and then setAdaptor for the notes array list
	 * 
	 * Based on the job ID, get the ticket ID. Then based on the ticket ID, get notes
	 * 
	 * @param uri
	 */
	private void fillNotes(Uri notesUri) {
		String[] projection2 = { Table.COLUMN_NOTE_MESSAGE }; 
		Cursor cursor2 = getContentResolver().query(notesUri, projection2, null, null, "date DESC");
		if (cursor2 != null) {
			cursor2.moveToFirst();
			while(!cursor2.isAfterLast()) {
				mNotes.add(cursor2.getString(cursor2.getColumnIndexOrThrow(Table.COLUMN_NOTE_MESSAGE)));
				cursor2.moveToNext();
			}
			mNotesList.setAdapter(new ArrayAdapter<String>(this,
	                R.layout.notes_list_row, mNotes));			
		}
		cursor2.close();
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// saveState();
		outState.putParcelable(MyContentProvider.CONTENT_ITEM_TYPE, mJobUri);
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