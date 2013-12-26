package com.snowball;

import static com.snowball.CommonUtilities.TAG;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import android.os.AsyncTask;
import android.util.Log;

public class CopyOfHTTPTask2 extends AsyncTask<Map<String, String>, Void, String> {

	public AsyncResponse delegate = null;

	@Override
	protected String doInBackground(Map<String, String>... params) {

		URL url = null;
		try {
			url = new URL(CommonUtilities.SERVER_ACTION_URL);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("invalid url");
		}

		StringBuilder bodyBuilder = new StringBuilder();
		Iterator<Entry<String, String>> iterator = params[0].entrySet().iterator();
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
		String responseString = null;
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
			responseString = out.toString();
			// handle the response
			int status = conn.getResponseCode();
			if (status != 200) {
				throw new IOException("Post failed with error code " + status);
			} else {
//				ByteArrayOutputStream out2 = new ByteArrayOutputStream();
//                response.getEntity().writeTo(out);
//                out.close();
//                responseString = out.toString();
			}
		} catch (ClientProtocolException e) {

		} catch (IOException e) {
			
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
		return responseString;
	}

	@Override
	protected void onPostExecute(String result) {
		delegate.asyncProcessFinish(result);
	}

	// @Override
	// protected String doInBackground(Map<String, String>... params) {
	// // TODO Auto-generated method stub
	// return null;
	// }

}