package com.snowball;

import static com.snowball.CommonUtilities.DISPLAY_MESSAGE_ACTION;
import static com.snowball.CommonUtilities.EXTRA_MESSAGE;
import static com.snowball.CommonUtilities.SENDER_ID;

import java.lang.reflect.Field;
import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.app.ActionBar.Tab;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewConfiguration;
import android.widget.Toast;

import com.snowball.gcm.GCMRegistrar;
import com.snowball.db.JobsContentProvider;

public class MainActivity extends FragmentActivity implements
		ActionBar.TabListener, AsyncResponse {

	protected static final String TAG = "MainActivity";
	private ViewPager viewPager;
	private TabsPagerAdapter mAdapter;
	private ActionBar actionBar;

	private String[] tabs = {
			"Outstanding", "Completed" };

	AsyncTask<Void, Void, Void> mRegisterTask;

	HTTPTask asyncTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		setTitle("Job Tracker");

		viewPager = (ViewPager) findViewById(R.id.pager);
		actionBar = getActionBar();
		mAdapter = new TabsPagerAdapter(getSupportFragmentManager());

		viewPager.setAdapter(mAdapter);
		actionBar.setHomeButtonEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Add Tabs
		for (String tab_name : tabs) {
			actionBar.addTab(actionBar.newTab().setText(tab_name).setTabListener(this));
		}

		/**
		 * On swiping the view pager make respective tab selected
		 * */
		viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
				// on changing the page make the respected tab selected
				actionBar.setSelectedNavigationItem(position);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});
				
		forceActionOverflowMenu();
		checkDeviceRegistered();
	}

	/**
	 * Check GCM dependencies and see if user is already registered on the server
	 */
	private void checkDeviceRegistered() {
		// Make sure the device has the proper dependencies
//		Log.d(TAG, "Checking if device has proper dependencies...");
//		GCMRegistrar.checkDevice(this);
//		Log.d(TAG, "...finished checking if device has proper dependencies");
//		// Make sure the manifest was properly set
//		GCMRegistrar.checkManifest(this);

		registerReceiver(mHandleMessageReceiver, new IntentFilter(DISPLAY_MESSAGE_ACTION));

		// Get GCM registration id		
		final String regId = GCMRegistrar.getRegistrationId(this);
		Log.v(TAG, "GCMRegistrar.getRegistrationId, regId: " + regId);

		// Check if a registration ID is already present
		if (regId.equals("")) {
			// Registration is not present, register with GCM
			Log.w(TAG, "regId was '' so now doing GCMRegistrar.register");
			GCMRegistrar.register(getApplicationContext(), SENDER_ID);
		} else {
			// Device is already registered on GCM
			Log.v(TAG, "GCMRegistrar.isRegisteredOnServer check");
			if (GCMRegistrar.isRegisteredOnServer(this)) {
				Log.d(TAG, "User isRegisteredOnServer");
				// Toast.makeText(getApplicationContext(), "Online",
				// Toast.LENGTH_SHORT).show();
			} else {
				Log.w(TAG, "User is not registered on server");
				checkEmailAddress();
				String email = CommonUtilities.getEmailAddress(this);
				registerUser(regId, email);
			}
		}
	}

	private void registerUser(String regId, String email) {
		ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
		nameValuePairs.add(new BasicNameValuePair("action", "register"));
		nameValuePairs.add(new BasicNameValuePair("regId", regId));
		nameValuePairs.add(new BasicNameValuePair("name", Build.MODEL));		
		nameValuePairs.add(new BasicNameValuePair("email", email));
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		nameValuePairs.add(new BasicNameValuePair("device_id", telephonyManager.getDeviceId()));
		doAsyncTask(nameValuePairs);
		GCMRegistrar.setRegisteredOnServer(this, true);
	}

	@SuppressWarnings("unchecked")
	private void doAsyncTask(ArrayList<NameValuePair> nameValuePairs) {
		asyncTask = new HTTPTask(this);
		asyncTask.delegate = MainActivity.this;
		asyncTask.execute(nameValuePairs);
	}

	@Override
	public void asyncProcessFinish(String output) {
		Toast.makeText(MainActivity.this, output, Toast.LENGTH_SHORT).show();
	}

	private void checkEmailAddress() {
		Log.i(TAG, "Checking if e-mail address is present in preferences");
		// Check if e-mail address is in preferences
		if (!CommonUtilities.isEmailAddressPresent(this)) {
			Log.w(TAG, "Not present so storing it");
			String firstEmailAccount = CommonUtilities.getDeviceAccounts(this);
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(getString(R.string.email_key), firstEmailAccount);
			editor.commit();
		} else {
			Log.i(TAG, "E-mail address is present in preferences");
		}		
	}

	/**
	 * Always show the overflow menu --- See
	 * http://stackoverflow.com/questions/9286822
	 * /how-to-force-use-of-overflow-menu-on-devices-with-menu-button
	 * 
	 * @param context
	 */
	private void forceActionOverflowMenu() {
		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception ex) {
			// Ignore
		}
	}

	@Override
	protected void onStart() {
		Log.v(TAG, "onStart");
		super.onStart();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		Log.v(TAG, "onRestoreInstanceState");
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onResume() {
		Log.v(TAG, "onResume");
		super.onResume();
	}

	@Override
	protected void onPause() {
		Log.v(TAG, "onPause");
		super.onPause();
	}

	@Override
	protected void onStop() {
		Log.v(TAG, "onStop");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.v(TAG, "onDestroy");
		if (mRegisterTask != null) {
			mRegisterTask.cancel(true);
		}
		try {
			Log.v(TAG, "Calling unregisterReceiver");
			unregisterReceiver(mHandleMessageReceiver);
			Log.v(TAG, "Calling GCMRegistrar.onDestroy");
			// http://stackoverflow.com/questions/11935680/gcmregistrar-ondestroycontext-crashing-receiver-not-registered
			GCMRegistrar.onDestroy(getApplicationContext());
		} catch (Exception e) {
			Log.e(TAG, "MainActivity->onDestroy unregisterReceiver exception: " + e.getMessage());
		}
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.v(TAG, "onSaveInstanceState");
		super.onSaveInstanceState(outState);
	}

	/**
	 * Receive push message - see displayMessage in CommonUtilities
	 * */
	private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String newMessage = intent.getExtras().getString(EXTRA_MESSAGE);
			Log.d(TAG, "BroadcastReceiver called with this message " + newMessage);
			// Waking up device if it is sleeping
			WakeLocker.acquire(getApplicationContext());

			// Show received message
			Toast.makeText(getApplicationContext(), newMessage, Toast.LENGTH_LONG).show();

			// Release wake lock
			WakeLocker.release();
			// This was an attempt to reset notifications when you enter the app
			// from the notification drawer, but it fails
			// because the broadcast receiver is called on any notify, not only
			// when you click
			// App.setPendingNotificationsCount(0);
			// App.clearMessages();
		}
	};

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
			startActivityForResult(i, 0);
			return true;
		case R.id.delete_all:
			deleteAll();
			return true;
		case R.id.unregister:
			GCMRegistrar.unregister(this);
			// SharedPreferences settings =
			// PreferenceManager.getDefaultSharedPreferences(this);
			// Editor editor = settings.edit();
			// editor.clear();
			// editor.commit();
			return true;
		}
		return false;
	}

	private void deleteAll() {
		Uri uri = Uri.parse(JobsContentProvider.CONTENT_URI_JOBS + "/");
		getContentResolver().delete(uri, null, null);
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		// on tab selected show respected fragment view
		viewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	}

}