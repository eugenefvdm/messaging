package com.messaging.push;

import static com.messaging.push.CommonUtilities.DISPLAY_MESSAGE_ACTION;
import static com.messaging.push.CommonUtilities.EXTRA_MESSAGE;
import static com.messaging.push.CommonUtilities.SENDER_ID;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;
import com.messaging.push._backup.EventDatabaseActivity;
import com.messaging.push.contentprovider.MyTaskContentProvider;
import com.messaging.push.db.TaskTable;

public class CopyOfMainActivity extends Activity {
	// label to display GCM messages
	TextView lblMessage;

	AsyncTask<Void, Void, Void> mRegisterTask;

	// Alert dialog manager
	AlertDialogManager alert = new AlertDialogManager();

	// Connection detector
	ConnectionDetector cd;

	public static String name;
	public static String email;

	// TODO Move to class MessageActions
	private Uri todoUri;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Bundle extras = getIntent().getExtras();

		// check from the saved Instance
		todoUri = (savedInstanceState == null) ? null : (Uri) savedInstanceState
				.getParcelable(MyTaskContentProvider.CONTENT_ITEM_TYPE);

		// Or passed from the other activity
		if (extras != null) {
			todoUri = extras
					.getParcelable(MyTaskContentProvider.CONTENT_ITEM_TYPE);

			//fillData(todoUri);
		}

		cd = new ConnectionDetector(getApplicationContext());

		// Check if Internet present
		if (!cd.isConnectingToInternet()) {
			// Internet Connection is not present
			alert.showAlertDialog(CopyOfMainActivity.this,
					"Internet Connection Error",
					"Please connect to working Internet connection", false);
			// stop executing code by return
			return;
		}

		// Getting name, email from intent
		Intent i = getIntent();

		name = i.getStringExtra("name");
		email = i.getStringExtra("email");		

		// Make sure the device has the proper dependencies.
		GCMRegistrar.checkDevice(this);

		// Make sure the manifest was properly set - comment out this line
		// while developing the app, then uncomment it when it's ready.
		GCMRegistrar.checkManifest(this);

		lblMessage = (TextView) findViewById(R.id.lblMessage);

		registerReceiver(mHandleMessageReceiver, new IntentFilter(
				DISPLAY_MESSAGE_ACTION));

		// Get GCM registration id
		final String regId = GCMRegistrar.getRegistrationId(this);

		// Check if registration id already presents
		if (regId.equals("")) {
			// Registration is not present, register now with GCM			
			GCMRegistrar.register(this, SENDER_ID);
		} else {
			// Device is already registered on GCM
			if (GCMRegistrar.isRegisteredOnServer(this)) {
				// Skips registration.				
				Toast.makeText(getApplicationContext(), "Already registered with GCM", Toast.LENGTH_LONG).show();
			} else {
				// Try to register again, but not in the UI thread.
				// It's also necessary to cancel the thread onDestroy(),
				// hence the use of AsyncTask instead of a raw thread.
				final Context context = this;
				mRegisterTask = new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						// Register on our server
						// On server creates a new user
						ServerUtilities.register(context, name, email, regId);
						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
						mRegisterTask = null;
					}

				};
				mRegisterTask.execute(null, null, null);
			}
		}
	}		

	/**
	 * Receiving push messages
	 * */
	private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String newMessage = intent.getExtras().getString(EXTRA_MESSAGE);
			// Waking up mobile if it is sleeping
			WakeLocker.acquire(getApplicationContext());

			/**
			 * Take appropriate action on this message
			 * depending upon your app requirement
			 * For now i am just displaying it on the screen
			 * */

			// Showing received message
			lblMessage.append(newMessage + "\n");
			//Toast.makeText(getApplicationContext(), "New Message: " + newMessage, Toast.LENGTH_LONG).show();
			saveState(newMessage);

			// Releasing wake lock
			WakeLocker.release();
		}
	};

	private void saveState(String str) {

		int ticket_id = 0;
		String department = null;
		String client = null;
		String address = null;

		JSONObject jObject;
		try {
			jObject = new JSONObject(str);
			ticket_id	= jObject.getInt("ticket_id");
			department	= jObject.getString("department");
			client		= jObject.getString("client");
			address		= jObject.getString("address");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Toast.makeText(getApplicationContext(), "Department: " + department, Toast.LENGTH_LONG).show();

		ContentValues values = new ContentValues();
		values.put(TaskTable.COLUMN_TICKET_ID, ticket_id);
		values.put(TaskTable.COLUMN_DEPARTMENT, department);	    
		values.put(TaskTable.COLUMN_CLIENT, client);
		values.put(TaskTable.COLUMN_ADDRESS, address);

		if (todoUri == null) {
			// New task
			todoUri = getContentResolver().insert(MyTaskContentProvider.CONTENT_URI, values);
		} else {
			// Update task
			todoUri = getContentResolver().insert(MyTaskContentProvider.CONTENT_URI, values);
			//getContentResolver().update(todoUri, values, null, null);
		}
	}

	@Override
	protected void onDestroy() {
		if (mRegisterTask != null) {
			mRegisterTask.cancel(true);
		}
		try {
			unregisterReceiver(mHandleMessageReceiver);
			GCMRegistrar.onDestroy(this);
		} catch (Exception e) {
			Log.e("UnRegister Receiver Error", "> " + e.getMessage());
		}
		super.onDestroy();
	}

	
	

}
