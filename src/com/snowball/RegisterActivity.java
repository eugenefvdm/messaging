package com.snowball;

import java.util.regex.Pattern;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.snowball.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;

public class RegisterActivity extends Activity {
	private static final String TAG = "RegisterActivity";

	// alert dialog manager
	AlertDialogManager alert = new AlertDialogManager();

	// Internet detector
	ConnectionDetector cd;

	// UI elements
	EditText txtDeviceName;
	EditText txtEmail;

	// Register button
	Button btnRegister;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);
		
		txtDeviceName = (EditText) findViewById(R.id.txtName);
		txtEmail = (EditText) findViewById(R.id.txtEmail);		
		btnRegister = (Button) findViewById(R.id.btnRegister);

		cd = new ConnectionDetector(getApplicationContext());

		// Check if Internet present
		if (!cd.isConnectingToInternet()) {
			// Internet Connection is not present
			alert.showAlertDialog(RegisterActivity.this, "Internet Connection Error", "Please connect to working Internet connection", false);
			// stop executing code by return
			return;
		}
		
		//checkGooglePlayServicesAvailability();
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		String deviceName = sharedPrefs.getString("pref_key_device_name", null);
		String serverURL = sharedPrefs.getString("pref_key_server_url", null);
		
		String email = getDeviceAccounts();
		txtEmail.setText(email);
		
		if (serverURL == null || deviceName == null) {
			Intent i = new Intent(getApplicationContext(), PrefsActivity.class);
			startActivity(i);
//			String deviceName2 = sharedPrefs.getString("pref_key_device_name", null);
//			String serverURL2 = sharedPrefs.getString("pref_key_server_url", null);
//			i.putExtra("name", deviceName2);
//			i.putExtra("email", email);
//			startActivity(i);
			return;
		} else {
			Intent i = new Intent(getApplicationContext(), MainActivity.class);
			i.putExtra("name", deviceName);
			i.putExtra("email", email);
			startActivity(i);			
			return;
		}
	}
		
//		if (!(deviceName == null) && !(email == null)) {
//			Intent i = new Intent(getApplicationContext(), MainActivity.class);
//			i.putExtra("name", deviceName);
//			i.putExtra("email", email);
//			startActivity(i);
//			//finish();
//		} else {
//			// Check if GCM configuration is set
//			//if (serverURL == null || SENDER_ID == null || serverURL.length() == 0 || SENDER_ID.length() == 0) {
//			if (serverURL == null || deviceName == null) {
//				Intent i = new Intent();
//				i.setClass(this, PrefsActivity.class);
//				startActivityForResult(i,  0);
//				
//			//if (SERVER_URL == null || SENDER_ID == null || SERVER_URL.length() == 0 || SENDER_ID.length() == 0) {
//				// GCM sender id / server url is missing
//				alert.showAlertDialog(RegisterActivity.this, "Configuration Error!", "Please set your Server URL and GCM Sender ID", false);
//				// stop executing code by return
//				return;
//			}			
//		}

		

		
		
		
		
		

		
		/*
		 * Click event on Register button
		 */
//		btnRegister.setOnClickListener(new View.OnClickListener() {
//
//			@Override
//			public void onClick(View arg0) {
//				// Read EditText data
//				String name = txtDeviceName.getText().toString();
//				String email = txtEmail.getText().toString();
//
//				// Check if user filled the form
//				if (name.trim().length() > 0 && email.trim().length() > 0) {
//					// Launch Main Activity
//					Intent i = new Intent(getApplicationContext(), MainActivity.class);
//
//					// Registering user on our server
//					// Sending registration details to MainActivity
//					i.putExtra("name", name);
//					i.putExtra("email", email);
//					startActivity(i);
//					// This was commented out because when you exited Main Activity the application closed but
//					// for debugging we want to redisplay the Register screen
//					//finish();
//					
//				} else {
//					// user doen't filled that data
//					// ask him to fill the form
//					alert.showAlertDialog(RegisterActivity.this, "Registration Error!", "Please enter your details", false);
//				}
//			}
//		});
//	}
	
	public void checkGooglePlayServicesAvailability()
	{
		int RQS_GooglePlayServices = 1;
		Log.d(TAG, "Checking for play services");
	    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
	    Log.d(TAG, "Result: " + Integer.valueOf(resultCode));
	    if(resultCode == ConnectionResult.SERVICE_MISSING || resultCode == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED || resultCode == ConnectionResult.SERVICE_DISABLED)
	    {
	    	Log.d(TAG, "Calling up dialog: " + Integer.valueOf(resultCode));
	    	GooglePlayServicesUtil.getErrorDialog(resultCode, this, RQS_GooglePlayServices).show();
//	        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 69);
//	        if(dialog != null)
//	        {
//	            dialog.show();                
//	        }
//	        else
//	        {
//	            showOkDialogWithText(this, "Something went wrong. Please make sure that you have the Play Store installed and that you are connected to the internet. Contact developer with details if this persists.");
//	        }
	    }

	    //Log.d("GooglePlayServicesUtil Check", "Result is: " + resultCode);
	}

	public static void showOkDialogWithText(Context context, String messageText)
	{
	    new AlertDialog.Builder(context)
	    .setMessage(messageText)
	    .setCancelable(true)
	    .setPositiveButton("OK", null)
	    .show();
	}
		
	private String getDeviceAccounts() {
		Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
		Account[] accounts = AccountManager.get(this).getAccounts();
		for (Account account : accounts) {
		    if (emailPattern.matcher(account.name).matches()) {
		        String possibleEmail = account.name;		        
		        Log.d("Register", "E-mail found " + possibleEmail);
		        return possibleEmail;		       
		    }
		}
		return null;
	}


}