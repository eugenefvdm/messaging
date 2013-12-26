/*
 * PrefsFragment also displays actual values as summary
 * Taken from StackOverflow example
 */

package com.snowball;

import com.snowball.R;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;

public class PrefsFragment extends PreferenceFragment implements
		OnSharedPreferenceChangeListener {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		addPreferencesFromResource(R.xml.preferences);
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); ++i) {
			Preference preference = getPreferenceScreen().getPreference(i);
			if (preference instanceof PreferenceGroup) {
				PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
				for (int j = 0; j < preferenceGroup.getPreferenceCount(); ++j) {
					updatePreference(preferenceGroup.getPreference(j));
				}
			} else {
				updatePreference(preference);
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		updatePreference(findPreference(key));

	}

	private void updatePreference(Preference preference) {
		if (preference instanceof ListPreference) {
			ListPreference listPreference = (ListPreference) preference;
			listPreference.setSummary(listPreference.getEntry());
		}
		if (preference instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) preference;
            preference.setSummary(editTextPref.getText());
        }
		
	}

}