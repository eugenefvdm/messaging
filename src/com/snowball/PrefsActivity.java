package com.snowball;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class PrefsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle("Settings");
		getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
		
//		Intent returnIntent = new Intent();
//		setResult(RESULT_CANCELED, returnIntent);        
//		finish();
		
	}

}