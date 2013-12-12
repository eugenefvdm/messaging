package com.messaging.push;

import static com.messaging.push.CommonUtilities.DISPLAY_MESSAGE_ACTION;
import static com.messaging.push.CommonUtilities.EXTRA_MESSAGE;
import static com.messaging.push.CommonUtilities.SENDER_ID;

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
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;
import com.messaging.push.contentprovider.MyTaskContentProvider;
import com.messaging.push.db.TaskTable;

public class MainActivity extends ListActivity implements
		LoaderManager.LoaderCallbacks<Cursor> {
	// label to display GCM messages
	// TextView lblMessage;

	AsyncTask<Void, Void, Void> mRegisterTask;

	AlertDialogManager alert = new AlertDialogManager();

	ConnectionDetector cd;

	public static String name;
	public static String email;

	private static final int DELETE_ID = Menu.FIRST + 1;
	// private Cursor cursor;
	private SimpleCursorAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.task_list);
		this.getListView().setDividerHeight(2);
		fillData();
		registerForContextMenu(getListView());

		cd = new ConnectionDetector(getApplicationContext());

		// Check if Internet present
		if (!cd.isConnectingToInternet()) {
			// Internet Connection is not present
			alert.showAlertDialog(MainActivity.this, "Internet Connection Error", "Please connect to working Internet connection", false);
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
		//GCMRegistrar.checkManifest(this);

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
				// For debugging we used to notify the user that the device is already registered...
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
		Intent i = new Intent(this, TaskDetailActivity.class);
		Uri taskUri = Uri.parse(MyTaskContentProvider.CONTENT_URI + "/" + id);
		i.putExtra(MyTaskContentProvider.CONTENT_ITEM_TYPE, taskUri);
		startActivity(i);
	}

	private void fillData() {

		// Fields from the database (projection)
		// Must include the _id column for the adapter to work
		String[] from = new String[] {
				TaskTable.COLUMN_CLIENT, TaskTable.COLUMN_DEPARTMENT };
		// Fields on the UI to which we map
		int[] to = new int[] {
				R.id.client, R.id.category };

		getLoaderManager().initLoader(0, null, this);
		adapter = new SimpleCursorAdapter(this, R.layout.task_row, null, from, to, 0);

		setListAdapter(adapter);
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
		inflater.inflate(R.menu.listmenu, menu);
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
		Uri uri = Uri.parse(MyTaskContentProvider.CONTENT_URI + "/");
		getContentResolver().delete(uri, null, null);
		fillData();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, DELETE_ID, 0, R.string.menu_delete);
	}

	// creates a new loader after the initLoader () call
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = {
				TaskTable.COLUMN_ID, TaskTable.COLUMN_CLIENT,
				TaskTable.COLUMN_DEPARTMENT };
		CursorLoader cursorLoader = new CursorLoader(this, MyTaskContentProvider.CONTENT_URI, projection, null, null, null);
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
