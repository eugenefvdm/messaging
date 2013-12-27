package com.snowball;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class PrefsActivity extends Activity {

	private static final String TAG = "PrefsActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		setTitle("Settings");
		getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
	}

}