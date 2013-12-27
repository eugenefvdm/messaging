/*
 * PrefsFragment also displays actual values as summary
 * 
 * Taken from StackOverflow example
 * 
 * TODO If the application is launched for the first time then the device name must equal Build.MODEL
 * TODO If the device name is changed, the name on the server must also be updated
 * TODO Further in the application the actual values for Server URL must be obtained from the preferences
 * TODO Although the notification sound is set here, it should also be used when a notification happens
 * 
 */

package com.snowball;

import com.snowball.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.RingtonePreference;
import android.util.Log;

public class PrefsFragment extends PreferenceFragment implements
		OnSharedPreferenceChangeListener {
	
	protected static final String TAG = "PrefsFragment";
	SharedPreferences prefs = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		addPreferencesFromResource(R.xml.preferences);
		//getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
		prefs = getPreferenceManager().getSharedPreferences();
		prefs.registerOnSharedPreferenceChangeListener(this);
		// Also show values
		for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); ++i) {
			Preference preference = getPreferenceScreen().getPreference(i);
			if (preference instanceof PreferenceGroup) {
				PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
				for (int j = 0; j < preferenceGroup.getPreferenceCount(); ++j) {
					setPreferenceSummaries(preferenceGroup.getPreference(j));
				}
			} else {
				setPreferenceSummaries(preference);
			}
		}
	}
	
	@Override
	public void onPause() {
		Log.d(TAG, "onPause");
		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Log.d(TAG, "onSharedPreferenceChanged with key: " + key);
		if ("pref_key_server_url".equals(key)) {			
			Log.d(TAG, "Checking if Server URL is valid...");
		}
		setPreferenceSummaries(findPreference(key));
	}

	/**
	 * Normally Android doesn't display the preference and this is the workaround (see also onResume)
	 * 
	 * @param preference
	 */
	private void setPreferenceSummaries(Preference preference) {
		Context ctx = getActivity();
		if (preference instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) preference;
            preference.setSummary(editTextPref.getText());
        }
		if (preference instanceof ListPreference) {
			ListPreference listPreference = (ListPreference) preference;
			listPreference.setSummary(listPreference.getEntry());
		}
		if (preference instanceof RingtonePreference) {
			// Note: This will only work for 'notification_sound'!
			// TODO Find a better way
			String strRingtonePreference = prefs.getString("notification_sound", "DEFAULT_RINGTONE_URI");
			Uri ringtoneUri = Uri.parse(strRingtonePreference);
			Ringtone ringtone = RingtoneManager.getRingtone(ctx, ringtoneUri);			
			RingtonePreference ringTonePreference = (RingtonePreference) preference;
			ringTonePreference.setSummary(ringtone.getTitle(ctx));
		}
	}	

}