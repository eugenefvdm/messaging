package com.snowball;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.os.AsyncTask;

public class HTTPTask extends AsyncTask<ArrayList<NameValuePair>, Void, String> {

	public AsyncResponse delegate = null;
	
	private Context mContext;

	public HTTPTask(Context context) {
		mContext = context;
		// TODO Auto-generated constructor stub
	}

	@Override
	protected String doInBackground(ArrayList<NameValuePair>... params) {

		HttpClient httpclient = new DefaultHttpClient();
		//HttpPost httppost = new HttpPost(CommonUtilities.SERVER_ACTION_URL);
		HttpPost httppost = new HttpPost(ServerUtilities.getServerUrl(mContext) + "action.php");

		String responseBody = null;
		try {
			httppost.setEntity(new UrlEncodedFormEntity(params[0]));
			try {
				HttpResponse response = httpclient.execute(httppost);
				responseBody  = EntityUtils.toString(response.getEntity());				
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return responseBody;
	}

	@Override
	protected void onPostExecute(String result) {
		delegate.asyncProcessFinish(result);
	}

}