package com.snowball;

import static com.snowball.CommonUtilities.DISPLAY_MESSAGE_ACTION;
import static com.snowball.CommonUtilities.EXTRA_MESSAGE;
import static com.snowball.CommonUtilities.SENDER_ID;

import java.lang.reflect.Field;

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
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewConfiguration;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;
import com.snowball.R;
import com.snowball.db.JobContentProvider;

public class MainActivity extends FragmentActivity implements
		ActionBar.TabListener {
	
	protected static final String TAG = "MainActivity";
	private ViewPager viewPager;
	private TabsPagerAdapter mAdapter;
	private ActionBar actionBar;
	
	private String[] tabs = { "Outstanding" , "Completed" };
	
	AsyncTask<Void, Void, Void> mRegisterTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		viewPager = (ViewPager) findViewById(R.id.pager);
		actionBar = getActionBar();
		mAdapter = new TabsPagerAdapter(getSupportFragmentManager());

		viewPager.setAdapter(mAdapter);
		actionBar.setHomeButtonEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);		

		// Add Tabs
		for (String tab_name : tabs) {
			actionBar.addTab(actionBar.newTab().setText(tab_name)
					.setTabListener(this));
		}
		
		// Initialize preferences
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		setTitle("Snowball Job List");
				
		// Make sure the device has the proper dependencies
		Log.i(TAG, "Checking if device has proper dependencies...");
		GCMRegistrar.checkDevice(this);
		Log.i(TAG, "...finished checking if device has proper dependencies");
		// Make sure the manifest was properly set - uncomment when ready
		// http://developer.android.com/reference/com/google/android/gcm/GCMRegistrar.html#checkManifest(android.content.Context)
		// GCMRegistrar.checkManifest(this);

		registerReceiver(mHandleMessageReceiver, new IntentFilter(DISPLAY_MESSAGE_ACTION));

		// Get GCM registration id
		Log.i(TAG, "GCMRegistrar.getRegistrationId");
		final String regId = GCMRegistrar.getRegistrationId(this);

		// Check if a registration ID is already present
		if (regId.equals("")) {
			// Registration is not present, register with GCM
			Log.i(TAG, "GCMRegistrar.register");
			GCMRegistrar.register(this, SENDER_ID);
		} else {
			// Device is already registered on GCM
			if (GCMRegistrar.isRegisteredOnServer(this)) {
				// Skip registration
				// Notify the user that the device is registered...
				Toast.makeText(getApplicationContext(), "Online", Toast.LENGTH_SHORT).show();
			} else {
				// Try to register again, but not in the UI thread.
				// It's also necessary to cancel the thread onDestroy(),
				// hence the use of AsyncTask instead of a raw thread.
				final Context context = this;
				mRegisterTask = new AsyncTask<Void, Void, Void>() {
					@Override
					protected Void doInBackground(Void... params) {
						// Create a new user on the messaging server
						ServerUtilities.register(context, Build.MODEL, CommonUtilities.getDeviceAccounts(context), regId);
						return null;
					}
					@Override
					protected void onPostExecute(Void result) {
						mRegisterTask = null;
					}
				};
				mRegisterTask.execute(null, null, null);
			}
			
			// Force action overflow see http://stackoverflow.com/questions/9286822/how-to-force-use-of-overflow-menu-on-devices-with-menu-button
			try {
		        ViewConfiguration config = ViewConfiguration.get(this);
		        Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
		        if(menuKeyField != null) {
		            menuKeyField.setAccessible(true);
		            menuKeyField.setBoolean(config, false);
		        }
		    } catch (Exception ex) {
		        // Ignore
		    }
			
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
			Log.e("MainActivity->onDestroy unregisterReceiver error", "> " + e.getMessage());
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
		Uri uri = Uri.parse(JobContentProvider.CONTENT_URI + "/");
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