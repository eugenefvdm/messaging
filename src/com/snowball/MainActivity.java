package com.snowball;

import static com.snowball.CommonUtilities.DISPLAY_MESSAGE_ACTION;
import static com.snowball.CommonUtilities.EXTRA_MESSAGE;
import static com.snowball.CommonUtilities.SENDER_ID;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.app.ActionBar.Tab;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;
import com.snowball.R;
import com.snowball.db.TaskContentProvider;

public class MainActivity extends FragmentActivity implements
		ActionBar.TabListener {
	
	protected static final String TAG = "MainActivity";
	private ViewPager viewPager;
	private TabsPagerAdapter mAdapter;
	private ActionBar actionBar;
	
	private String[] tabs = { "Outstanding" , "Completed" };
	
	AsyncTask<Void, Void, Void> mRegisterTask;
	
	public static String name;
	//public static String email;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Initialization
		viewPager = (ViewPager) findViewById(R.id.pager);
		actionBar = getActionBar();
		mAdapter = new TabsPagerAdapter(getSupportFragmentManager());

		viewPager.setAdapter(mAdapter);
		actionBar.setHomeButtonEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);		

		// Adding Tabs
		for (String tab_name : tabs) {
			actionBar.addTab(actionBar.newTab().setText(tab_name)
					.setTabListener(this));
		}
		
		// Init prefs
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		// GCM stuff

		// Getting name, email from intent
		Intent i = getIntent();

		name = i.getStringExtra("name");
		//email = i.getStringExtra("email");

		// Make sure the device has the proper dependencies.
		GCMRegistrar.checkDevice(this);

		// Make sure the manifest was properly set - uncomment when ready
		// http://developer.android.com/reference/com/google/android/gcm/GCMRegistrar.html#checkManifest(android.content.Context)
		// GCMRegistrar.checkManifest(this);

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
				Toast.makeText(getApplicationContext(), "Ready to receive cloud messages :-)", Toast.LENGTH_SHORT).show();
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
						ServerUtilities.register(context, name, CommonUtilities.getDeviceAccounts(context), regId);
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

		/**
		 * On swiping the view pager make respective tab selected
		 * */
		viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
				// on changing the page
				// make respected tab selected
				actionBar.setSelectedNavigationItem(position);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});
	}
	
	/**
	 * Receive push message
	 * */
	private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "BroadcastReceiver called");
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
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings:
			Intent i = new Intent();
			i.setClass(MainActivity.this, PrefsActivity.class);
			startActivityForResult(i,  0);
			return true;
		case R.id.delete_all:
			deleteAll();
			return true;
		case R.id.unregister:
			GCMRegistrar.unregister(this);
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
			Editor editor = settings.edit();
		    editor.clear();
		    editor.commit();
			return true;
		}
		return false;
	}
	
	private void deleteAll() {
		Uri uri = Uri.parse(TaskContentProvider.CONTENT_URI + "/");
		getContentResolver().delete(uri, null, null);		
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		// on tab selected
		// show respected fragment view
		viewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	}

}