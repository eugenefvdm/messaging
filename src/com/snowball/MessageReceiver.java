/**
 * Message receiver class called from GCMIntentService onMessage
 * 
 * Checks message action and interprets JSON received
 */
package com.snowball;

import java.util.Iterator;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.snowball.db.MyContentProvider;
import com.snowball.db.Table;

/**
 * @author eugene
 *
 */
public class MessageReceiver {

	private static final String TAG = "MessageReceiver";
	private Context ctx;
	
	public enum Actions {
		INSERTJOB, UPDATE, DELETE, ADDNOTE
	}

	/**
	 * 
	 */
	public MessageReceiver(Context ctx) {
		this.ctx = ctx;
	}
	
	public String checkType(String message) {
		
		int dbRecordId = 0;
		ContentValues values;
		String messageType = "";
		String department = null;
		String extra = null;
		
		String action = getAction(message);
		Log.v(TAG, "The action is " + action);
		
		if (action != null) {
			Log.d(TAG, "convertMessageToContentValues");
			values = convertMessageToContentValues(action, message);
    		department = values.getAsString("department");
    		extra = values.getAsString("extra");
			// Switch case
			Actions theAction = Actions.valueOf(action.toUpperCase(Locale.ENGLISH));
			Log.d(TAG, "checking theAction");
			switch (theAction) {
			case INSERTJOB :
				// Insert record and obtain ID for use in NotificationIntent
    			Uri jobUri = ctx.getContentResolver().insert(MyContentProvider.CONTENT_URI_JOBS, values);
    			long newDbRecord = ContentUris.parseId(jobUri);
    			// Convert long to int because Pending Intent Request Codes need to be integer
    			dbRecordId = (int) newDbRecord;
    			Log.d(TAG, "New ID generated after GCM payload: " + dbRecordId);
    			messageType = "New " + department;
				break;
			case UPDATE :
				dbRecordId = values.getAsInteger("calendar_id");
    			Uri todoUri = Uri.parse(MyContentProvider.CONTENT_URI_JOBS + "/ticket" + "/" + dbRecordId);        			
    			ctx.getContentResolver().update(todoUri, values, null, null);    						
    			messageType = extra;
				break;
			case DELETE :
				Log.d(TAG, "Action is DELETE");
				// {"delete":{"calendar_id":"758"}}
				dbRecordId = values.getAsInteger("calendar_id");
				Log.d(TAG, "Got dbRecordId of " + dbRecordId);
    			Log.d(TAG, "Action delete being executed on calendar_id " + String.valueOf(dbRecordId));
    			Uri todoUri2 = Uri.parse(MyContentProvider.CONTENT_URI_JOBS + "/ticket" + "/" + dbRecordId);        			
    			ctx.getContentResolver().delete(todoUri2, null, null);
    			//messageType = "Deleted " + department;
    			messageType = "Deleted item";
				break;
			case ADDNOTE :
				// Update notes (for now, insert)
    			ContentValues values2 = getNotes(action, message);
    			ctx.getContentResolver().insert(MyContentProvider.CONTENT_URI_NOTES, values2);
    			String note = values2.getAsString("message");
				messageType = "Added note " + note;
				break;
			default:
				// Unknown action so output raw message
				messageType = "System Message: " + message;
				break;				
			}
		
		} else {
			// Null action, should never happen
			messageType = "Action was NULL" + message;
		}
		Log.w(TAG, "Returning this messageType: " + messageType);
    	return messageType;
	}
	
	/**
	 * Determine action by evaluating JSON from message and iterating over first
	 * item
	 * 
	 * @param message
	 * @return action or NULL if no action
	 */
	private String getAction(String message) {
		JSONObject jObject = null;
		try {
			jObject = new JSONObject(message);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		Iterator<?> iter = jObject.keys();
		String action = null;
		while (iter.hasNext()) {
			action = (String) iter.next();
		}		
		return action;
	}
	
	/**
	 * Interpret JSON and convert to content values
	 * 
	 * @param action
	 * @param message
	 * @return
	 */
	private ContentValues convertMessageToContentValues(String action,
			String message) {
		JSONObject jObject = null;
		int userid = 0;
		int calendar_id = 0;
		int ticket_id = 0;
		String title = null;
		String department = null;
		String client_name = null;
		String companyname = null;
		String phonenumber = null;
		String address1 = null;
		String address2 = null;
		String city = null;
		String extra = null;
		int start = 0;
		try {
			jObject = new JSONObject(message);
			JSONObject payload = jObject.getJSONObject(action);
			calendar_id = payload.getInt("calendar_id");
			if (action.equals("insertJob") || action.equals("update")) {
				start = payload.getInt("start");
				userid = payload.getInt("userid");
				ticket_id = payload.getInt("ticket_id");
				title = payload.getString("title");
				department = payload.getString("department");
				client_name = payload.getString("client_name");
				companyname = payload.getString("companyname");
				phonenumber = payload.getString("phonenumber");
				address1 = payload.getString("address1");
				address2 = payload.getString("address2");
				city = payload.getString("city");
				// "customfields":[{"id":"11","type":"support","relid":"5","fieldname":"High Site","fieldtype":"dropdown","description":"","fieldoptions":"Bottelary,Hawekwa,Simonsberg","required":"on"},{"id":"13","type":"support","relid":"5","fieldname":"Survey Successful?","fieldtype":"tickbox","description":"","fieldoptions":"","required":"on"}]
				JSONArray customfields = payload.getJSONArray("customfields");
				int len = customfields.length();
				for (int i = 0; i < len; i++) {
					JSONObject obj = customfields.getJSONObject(i);
					Log.d(TAG, "Custom fields" + obj.getString("fieldname"));
				}
				extra = payload.getString("extra");
			}
		} catch (JSONException e) {
			Log.e(TAG, "Error parsing JSON");
			e.printStackTrace();
		} catch (Exception e) {
			Log.e(TAG, "In JSON, some other exception occurred");
			e.printStackTrace();
		}

		ContentValues values = new ContentValues();
		values.put(Table.COLUMN_JOB_CALENDAR_ID, calendar_id);
		values.put(Table.COLUMN_JOB_START, start);
		values.put(Table.COLUMN_JOB_TICKET_ID, ticket_id);
		values.put(Table.COLUMN_JOB_CLIENT_ID, userid);
		values.put(Table.COLUMN_JOB_DEPARTMENT, department);
		values.put(Table.COLUMN_JOB_TITLE, title);
		values.put(Table.COLUMN_JOB_CLIENT_NAME, client_name);
		values.put(Table.COLUMN_JOB_COMPANYNAME, companyname);
		values.put(Table.COLUMN_JOB_PHONENUMBER, phonenumber);
		values.put(Table.COLUMN_JOB_ADDRESS1, address1);
		values.put(Table.COLUMN_JOB_ADDRESS2, address2);
		values.put(Table.COLUMN_JOB_CITY, city);
		values.put(Table.COLUMN_JOB_EXTRA, extra);
		return values;
	}
	
	private ContentValues getNotes(String action, String message) {
		JSONObject jObject = null;
		ContentValues values = new ContentValues();
		try {
			jObject = new JSONObject(message);
			JSONObject payload = jObject.getJSONObject(action);
			String note = payload.getString("message");
			Log.i(TAG, "The latest note reads " + note);
			//JSONObject notes = payload.getJSONObject("notes");
			//Log.i(TAG, "The latest note reads " + notes.getString("message"));
			values.put(Table.COLUMN_NOTE_CALENDAR_ID, payload.getString("calendar_id"));
			values.put(Table.COLUMN_NOTE_TICKET_ID, payload.getString("ticket_id"));
			values.put(Table.COLUMN_NOTE_ADMIN_NAME, payload.getString("admin_name"));			
			values.put(Table.COLUMN_NOTE_MESSAGE, payload.getString("message"));
		} catch (JSONException e) {
			Log.e(TAG, "Error parsing Notes JSON");
			e.printStackTrace();
		}
		return values;
	}
}
