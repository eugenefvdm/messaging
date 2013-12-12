package com.messaging.push;

import static com.messaging.push.CommonUtilities.SENDER_ID;
import static com.messaging.push.CommonUtilities.SERVER_URL;

import java.util.regex.Pattern;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class RegisterActivity extends Activity {
	// alert dialog manager
	AlertDialogManager alert = new AlertDialogManager();

	// Internet detector
	ConnectionDetector cd;

	// UI elements
	EditText txtName;
	EditText txtEmail;

	// Register button
	Button btnRegister;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);

		cd = new ConnectionDetector(getApplicationContext());

		// Check if Internet present
		if (!cd.isConnectingToInternet()) {
			// Internet Connection is not present
			alert.showAlertDialog(RegisterActivity.this, "Internet Connection Error", "Please connect to working Internet connection", false);
			// stop executing code by return
			return;
		}

		// Check if GCM configuration is set
		if (SERVER_URL == null || SENDER_ID == null || SERVER_URL.length() == 0 || SENDER_ID.length() == 0) {
			// GCM sender id / server url is missing
			alert.showAlertDialog(RegisterActivity.this, "Configuration Error!", "Please set your Server URL and GCM Sender ID", false);
			// stop executing code by return
			return;
		}

		txtName = (EditText) findViewById(R.id.txtName);
		txtEmail = (EditText) findViewById(R.id.txtEmail);
		btnRegister = (Button) findViewById(R.id.btnRegister);
		// Obtain e-mail address from device
		// http://stackoverflow.com/questions/2112965/how-to-get-the-android-devices-primary-e-mail-address
		// TODO This grabs the last e-mail address but the user should rather be presented with a choice
		Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
		Account[] accounts = AccountManager.get(this).getAccounts();
		for (Account account : accounts) {
		    if (emailPattern.matcher(account.name).matches()) {
		        String possibleEmail = account.name;
		        txtEmail.setText(possibleEmail);
		        Log.d("Register", "E-mail found " + possibleEmail);
		    }
		}
		
		/*
		 * Click event on Register button
		 */
		btnRegister.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// Read EditText data
				String name = txtName.getText().toString();
				String email = txtEmail.getText().toString();

				// Check if user filled the form
				if (name.trim().length() > 0 && email.trim().length() > 0) {
					// Launch Main Activity
					Intent i = new Intent(getApplicationContext(), MainActivity.class);

					// Registering user on our server
					// Sending registration details to MainActivity
					i.putExtra("name", name);
					i.putExtra("email", email);
					startActivity(i);
					// This was commented out because when you exited Main Activity the application closed but
					// for debugging we want to redisplay the Register screen
					//finish();
					
				} else {
					// user doen't filled that data
					// ask him to fill the form
					alert.showAlertDialog(RegisterActivity.this, "Registration Error!", "Please enter your details", false);
				}
			}
		});
	}

}