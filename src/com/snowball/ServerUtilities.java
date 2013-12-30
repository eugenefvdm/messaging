package com.snowball;

import static com.snowball.CommonUtilities.SERVER_ACTION_URL;
import static com.snowball.CommonUtilities.SERVER_REGISTER_URL;
import static com.snowball.CommonUtilities.displayMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;
import com.snowball.R;

public final class ServerUtilities {
	private static final int MAX_ATTEMPTS = 5;
	private static final int BACKOFF_MILLI_SECONDS = 2000;
	private static final Random random = new Random();
	private static final String TAG = "ServerUtilities";

	/**
	 * Register this account/device pair within the server.
	 * 
	 */
	static void register(final Context context, final String regId) {
		String phoneModel = Build.MODEL;
		String firstEmailAccount = CommonUtilities.getDeviceAccounts(context); 
		Log.i(TAG, "Registering device regId:  " + regId);
		// Obtain serverUrl from prefs
		// Was SERVER_REGISTER_URL = "http://196.201.6.235/whmcs/modules/addons/messaging/register.php";
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		String defaultValue = context.getResources().getString(R.string.server_url_default);
		String serverUrl = sharedPref.getString(context.getString(R.string.server_url), defaultValue);
		Map<String, String> params = new HashMap<String, String>();
		params.put("regId", regId);
		params.put("name", phoneModel);
		params.put("email", firstEmailAccount);
		// TODO determine if device_id code should be here or
		// RegisterActivity->MainActivity->ServerUtilities
		TelephonyManager telephonyManager;
		telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		params.put("device_id", telephonyManager.getDeviceId());

		long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
		// Once GCM returns a registration id, we need to register on our server
		// As the server might be down, we will retry it a couple
		// times.
		for (int i = 1; i <= MAX_ATTEMPTS; i++) {
			Log.d(TAG, "Attempt #" + i + " to register");
			try {
				displayMessage(context, context.getString(R.string.server_registering, i, MAX_ATTEMPTS));
				post(serverUrl, params);
				GCMRegistrar.setRegisteredOnServer(context, true);
				String message = context.getString(R.string.server_registered);
				CommonUtilities.displayMessage(context, message);
				return;
			} catch (IOException e) {
				// Here we are simplifying and retrying on any error; in a real
				// application, it should retry only on unrecoverable errors
				// (like HTTP error code 503).
				Log.e(TAG, "Failed to register on attempt " + i + ":" + e);
				if (i == MAX_ATTEMPTS) {
					break;
				}
				try {
					Log.d(TAG, "Sleeping for " + backoff + " ms before retry");
					Thread.sleep(backoff);
				} catch (InterruptedException e1) {
					// Activity finished before we complete - exit.
					Log.d(TAG, "Thread interrupted: abort remaining retries!");
					Thread.currentThread().interrupt();
					return;
				}
				// increase back off exponentially
				backoff *= 2;
			}
		}
		String message = context.getString(R.string.server_register_error, MAX_ATTEMPTS);
		CommonUtilities.displayMessage(context, message);
	}

//	/**
//	 * Update ticket time on server based on start and end values
//	 * 
//	 * This should be combined with the existing server post routine
//	 * 
//	 */
//	static void updateTimeOnServer(final Context context, String id, String start, String end) {
//		String calendar_id = String.valueOf(id);
//		String serverUrl = SERVER_ACTION_URL;
//		Map<String, String> params = new HashMap<String, String>();
//		params.put("action", "update");
//		params.put("calendar_id", calendar_id);
//		params.put("start", start);
//		params.put("end", end);
//		try {
//			post(serverUrl, params);
//		} catch (IOException e) {
//			Log.e(TAG, "Unable to update ticket time on server");
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

	/**
	 * Unregister this account/device pair within the server.
	 */
	static void unregister(final Context context, final String regId) {
		Log.i(TAG, "Unregistering device (regId = " + regId + ")");
		//String serverUrl = SERVER_ACTION_URL + "?action=unregister";
		String serverUrl = SERVER_ACTION_URL;
		Map<String, String> params = new HashMap<String, String>();
		params.put("action", "unregister");		
		params.put("regId", regId);
		try {
			post(serverUrl, params);
			GCMRegistrar.setRegisteredOnServer(context, false);
			String message = context.getString(R.string.server_unregistered);
			CommonUtilities.displayMessage(context, message);
		} catch (IOException e) {
			// At this point the device is unregistered from GCM, but still
			// registered in the server.
			// We could try to unregister again, but it is not necessary:
			// if the server tries to send a message to the device, it will get
			// a "NotRegistered" error message and should unregister the device.
			String message = context.getString(R.string.server_unregister_error, e.getMessage());
			CommonUtilities.displayMessage(context, message);
		}
	}

	/**
	 * Issue a POST request to the messaging server
	 * 
	 * @param endpoint
	 *            POST address.
	 * @param params
	 *            request parameters.
	 * 
	 * @throws IOException
	 *             propagated from POST.
	 */
	private static void post(String endpoint, Map<String, String> params)
			throws IOException {

		URL url;
		try {
			url = new URL(endpoint);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("invalid url: " + endpoint);
		}
		StringBuilder bodyBuilder = new StringBuilder();
		Iterator<Entry<String, String>> iterator = params.entrySet().iterator();
		// constructs the POST body using the parameters
		while (iterator.hasNext()) {
			Entry<String, String> param = iterator.next();
			bodyBuilder.append(param.getKey()).append('=').append(param.getValue());
			if (iterator.hasNext()) {
				bodyBuilder.append('&');
			}
		}
		String body = bodyBuilder.toString();
		Log.v(TAG, "Posting '" + body + "' to " + url);
		byte[] bytes = body.getBytes();
		HttpURLConnection conn = null;
		try {
			Log.d(TAG, "> " + url);
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setFixedLengthStreamingMode(bytes.length);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
			// post the request
			OutputStream out = conn.getOutputStream();
			out.write(bytes);
			out.close();
			// handle the response
			int status = conn.getResponseCode();
			if (status != 200) {
				throw new IOException("Post failed with error code " + status);
			}
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}
}
