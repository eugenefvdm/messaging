package com.snowball;

import java.util.regex.Pattern;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Patterns;

public final class CommonUtilities {
	
	// give your server registration url here
	// TODO Combine these two constants and move to preferences / static variable
//    static final String SERVER_REGISTER_URL = "http://196.201.6.235/whmcs/modules/addons/messaging/register.php"; 
//    static final String SERVER_ACTION_URL = "http://196.201.6.235/whmcs/modules/addons/messaging/action.php";

    // Google project id for "messaging" as set on the API console
    static final String SENDER_ID = "818143334463"; 

    /**
     * Tag used on log messages.
     */
    static final String TAG = "CommonUtilities";

    static final String DISPLAY_MESSAGE_ACTION =
            "com.messaging.push.DISPLAY_MESSAGE";

    static final String EXTRA_MESSAGE = "message";

    /**
     * Notifies UI to display a message.
     * 
     * This method is defined in the common helper because it's used both by
     * the UI and the background service.
     *
     * @param context application's context.
     * @param message message to be displayed.
     */
    static void displayMessage(Context context, String message) {
        Intent intent = new Intent(DISPLAY_MESSAGE_ACTION);
        intent.putExtra(EXTRA_MESSAGE, message);
        context.sendBroadcast(intent);
    }
    
    public static String getDeviceAccounts(Context context) {
		Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
		Account[] accounts = AccountManager.get(context).getAccounts();
		for (Account account : accounts) {
		    if (emailPattern.matcher(account.name).matches()) {
		        String possibleEmail = account.name;		        
		        Log.d("Register", "E-mail found " + possibleEmail);
		        return possibleEmail;		       
		    }
		}
		return null;
	}
    
    public static boolean isEmailAddressPresent(Context context) {		
		String email = getEmailAddress(context);
		Log.d(TAG, "email returned from getEmailAddress: " + email); 
		if (email.equals("") || email == null) {
			return false;
		} else {
			return true;
		}
	}
    
    public static String getEmailAddress(Context context) {
    	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		String emailDefault = context.getResources().getString(R.string.email_default);
		String email = sharedPref.getString(context.getString(R.string.email_key), emailDefault);
		return email;
    }
    
}
