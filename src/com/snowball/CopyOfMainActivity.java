package com.snowball;

import static com.snowball.CommonUtilities.DISPLAY_MESSAGE_ACTION;
import static com.snowball.CommonUtilities.EXTRA_MESSAGE;
import static com.snowball.CommonUtilities.SENDER_ID;

import java.util.Date;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;
import com.snowball.R;
import com.snowball.db.TaskContentProvider;
import com.snowball.db.TaskTable;

public class CopyOfMainActivity extends ListActivity implements
		LoaderManager.LoaderCallbacks<Cursor> {
	// label to display GCM messages
	// TextView lblMessage;

	AsyncTask<Void, Void, Void> mRegisterTask;

	AlertDialogManager alert = new AlertDialogManager();

	ConnectionDetector cd;

	public static String name;
	public static String email;

	private static final int DELETE_ID = Menu.FIRST + 1;

	private SimpleCursorAdapter adapter;

	private static final String TAG = "MainActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.fragment_tasks);
		this.getListView().setDividerHeight(2);
		Log.v(TAG, "About to fillData()");
		fillData();
		Log.v(TAG, "Done with fillData()");
		registerForContextMenu(getListView());

		cd = new ConnectionDetector(getApplicationContext());

		// Check if Internet present
		if (!cd.isConnectingToInternet()) {
			// Internet Connection is not present
			alert.showAlertDialog(CopyOfMainActivity.this, "Internet Connection Error", "Please check your internet connection", false);
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
		// http://developer.android.com/reference/com/google/android/gcm/GCMRegistrar.html#checkManifest(android.content.Context)
		// GCMRegistrar.checkManifest(this);

		// lblMessage = (TextView) findViewById(R.id.lblMessage);

		registerReceiver(mHandleMessageReceiver, new IntentFilter(DISPLAY_MESSAGE_ACTION));

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
				// For debugging we used to notify the user that the device is
				// already registered...
				Toast.makeText(getApplicationContext(), "Ready to receive cloud messages :-)", Toast.LENGTH_LONG).show();
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

	// Opens the second activity if an entry is clicked
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent i = new Intent(this, JobDetailActivity.class);
		Uri taskUri = Uri.parse(TaskContentProvider.CONTENT_URI + "/" + id);
		i.putExtra(TaskContentProvider.CONTENT_ITEM_TYPE, taskUri);
		startActivity(i);
	}

	private void fillData() {

		// Fields from the database (projection)
		// Must include the _id column for the adapter to work
		String[] from = new String[] {
				TaskTable.COLUMN_DEPARTMENT, TaskTable.COLUMN_START,
				TaskTable.COLUMN_ADDRESS1, TaskTable.COLUMN_ADDRESS2,
				TaskTable.COLUMN_CITY };
		// Fields on the UI to which we map
		int[] to = new int[] { R.id.department, R.id.start, R.id.city };

		getLoaderManager().initLoader(0, null, this);
		adapter = new TasksAdapter(this, R.layout.task_row, null, from, to, 0);

		setListAdapter(adapter);
	}
	
	public static class TasksAdapter extends SimpleCursorAdapter {
				
		private LayoutInflater layoutInflater;
		private int layout;		

		public TasksAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {					
			super(context, layout, c, from, to, 0);
			this.layout = layout;
			layoutInflater = LayoutInflater.from(context);			
		}
		
		@Override		
	    public View newView(Context context, Cursor cursor, ViewGroup parent) {			
			View view = layoutInflater.inflate(layout, parent, false);
	        return view;
	    }
		
		@Override
	    public void bindView(View view,Context context,Cursor cursor) {
			
	        String department = cursor.getString(cursor.getColumnIndex(TaskTable.COLUMN_DEPARTMENT));
	        String address1 = cursor.getString(cursor.getColumnIndex(TaskTable.COLUMN_ADDRESS1));
	        String address2 = cursor.getString(cursor.getColumnIndex(TaskTable.COLUMN_ADDRESS2));
	        String city = cursor.getString(cursor.getColumnIndex(TaskTable.COLUMN_CITY));
	        long unixStart = cursor.getLong(cursor.getColumnIndex(TaskTable.COLUMN_START));
	        Date d = new Date(unixStart * 1000);			
	        
	        TextView tv1 = (TextView)view.findViewById(R.id.department);
	        tv1.setText(department);
	        
	        TextView tv2 = (TextView)view.findViewById(R.id.start);
	        tv2.setText(DateFormat.format("E d hh:mm", d));
	        
	        TextView tv3 = (TextView)view.findViewById(R.id.city);
	        tv3.setText(address1 + ", " + address2 + "," + city);
	        
	    }		
	}

	/**
	 * Receive push message
	 * */
	private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String newMessage = intent.getExtras().getString(EXTRA_MESSAGE);
			// Waking up device if it is sleeping
			WakeLocker.acquire(getApplicationContext());

			// Show received message
			Toast.makeText(getApplicationContext(), newMessage, Toast.LENGTH_LONG).show();

			// Release wake lock
			WakeLocker.release();
		}
	};

	@Override
	protected void onDestroy() {
		if (mRegisterTask != null) {
			mRegisterTask.cancel(true);
		}
		try {
			unregisterReceiver(mHandleMessageReceiver);
			GCMRegistrar.onDestroy(this);
		} catch (Exception e) {
			Log.e("unregisterReceiver error", "> " + e.getMessage());
		}
		super.onDestroy();
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
		case R.id.delete_all:
			deleteAll();
			return true;
		case R.id.unregister:
			GCMRegistrar.unregister(this);
			return true;
		}
		return false;
	}

	private void deleteAll() {
		Uri uri = Uri.parse(TaskContentProvider.CONTENT_URI + "/");
		getContentResolver().delete(uri, null, null);
		fillData();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, DELETE_ID, 0, R.string.context_menu_list_call);
	}

	// creates a new loader after the initLoader () call
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = {
				TaskTable.COLUMN_ID, TaskTable.COLUMN_DEPARTMENT,
				TaskTable.COLUMN_START, TaskTable.COLUMN_ADDRESS1,
				TaskTable.COLUMN_ADDRESS2, TaskTable.COLUMN_CITY };
		CursorLoader cursorLoader = new CursorLoader(this, TaskContentProvider.CONTENT_URI, projection, null, null, null);
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		adapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// data is not available anymore, delete reference
		adapter.swapCursor(null);
	}

}
