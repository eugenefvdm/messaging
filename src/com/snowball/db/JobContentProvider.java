package com.snowball.db;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * Content Provider for Tasks
 * 
 * In order to get ticket_ids updated instead of _id, I added TASK_TICKET_ID and
 * CONTENT_ITEM_TICKET everywhere At present update is only called from GCM
 * intent
 * 
 * @author eugene
 * 
 */
public class JobContentProvider extends ContentProvider {

	private static final String TAG = "MyTaskContentProvider";

	// database
	private JobDatabaseHelper database;

	// used for the UriMacher
	private static final int TASKS = 10;
	private static final int TASK_ID = 20;
	private static final int TASK_CALENDAR_ID = 30;

	private static final String AUTHORITY = "com.snowball";

	private static final String BASE_PATH = "tasks";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
	public static final Uri CONTENT_URI_SANS_BASE = Uri.parse("content://" + AUTHORITY);

	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/tasks";
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/task";
	public static final String CONTENT_ITEM_TICKET = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/ticket";

	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		sURIMatcher.addURI(AUTHORITY, BASE_PATH, TASKS);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", TASK_ID);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/ticket" + "/#", TASK_CALENDAR_ID);
	}

	@Override
	public boolean onCreate() {
		database = new JobDatabaseHelper(getContext());
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		// Using SQLiteQueryBuilder instead of query() method
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		// check if the caller has requested a column which does not exists
		checkColumns(projection);

		// Set the table
		queryBuilder.setTables(JobTable.TABLE_TASK);

		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case TASKS:
			break;
		case TASK_ID:
			// adding the ID to the original query
			queryBuilder.appendWhere(JobTable.COLUMN_ID + "=" + uri.getLastPathSegment());
			break;
		case TASK_CALENDAR_ID:
			// adding the ticket_id to the original query
			queryBuilder.appendWhere(JobTable.COLUMN_TICKET_ID + "=" + uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		SQLiteDatabase db = database.getWritableDatabase();
		Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		// make sure that potential listeners are getting notified
		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		// int rowsDeleted = 0;
		long id = 0;
		switch (uriType) {
		case TASKS:
			id = sqlDB.insert(JobTable.TABLE_TASK, null, values);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return Uri.parse(BASE_PATH + "/" + id);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsDeleted = 0;
		switch (uriType) {
		case TASKS:
			rowsDeleted = sqlDB.delete(JobTable.TABLE_TASK, selection, selectionArgs);
			break;
		case TASK_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = sqlDB.delete(JobTable.TABLE_TASK, JobTable.COLUMN_ID + "=" + id, null);
			} else {
				rowsDeleted = sqlDB.delete(JobTable.TABLE_TASK, JobTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
			}
			break;
		case TASK_CALENDAR_ID:
			String calendar_id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = sqlDB.delete(JobTable.TABLE_TASK, JobTable.COLUMN_CALENDAR_ID + "=" + calendar_id, null);
			} else {
				rowsDeleted = sqlDB.delete(JobTable.TABLE_TASK, JobTable.COLUMN_CALENDAR_ID + "=" + calendar_id + " and " + selection, selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsDeleted;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {

		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsUpdated = 0;
		switch (uriType) {
		case TASKS:
			rowsUpdated = sqlDB.update(JobTable.TABLE_TASK, values, selection, selectionArgs);
			break;
		case TASK_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = sqlDB.update(JobTable.TABLE_TASK, values, JobTable.COLUMN_ID + "=" + id, null);
			} else {
				rowsUpdated = sqlDB.update(JobTable.TABLE_TASK, values, JobTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
			}
			break;
		case TASK_CALENDAR_ID:
			String calendar_id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = sqlDB.update(JobTable.TABLE_TASK, values, JobTable.COLUMN_CALENDAR_ID + "=" + calendar_id, null);
			} else {
				rowsUpdated = sqlDB.update(JobTable.TABLE_TASK, values, JobTable.COLUMN_CALENDAR_ID + "=" + calendar_id + " and " + selection, selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsUpdated;
	}

	private void checkColumns(String[] projection) {
		String[] available = {
				JobTable.COLUMN_USERID,
				JobTable.COLUMN_CALENDAR_ID, JobTable.COLUMN_TICKET_ID,
				JobTable.COLUMN_DEPARTMENT, JobTable.COLUMN_CLIENT_NAME,
				JobTable.COLUMN_COMPANYNAME, JobTable.COLUMN_PHONENUMBER,
				JobTable.COLUMN_ADDRESS1, JobTable.COLUMN_ADDRESS2,
				JobTable.COLUMN_CITY, JobTable.COLUMN_ID,
				JobTable.COLUMN_START, JobTable.COLUMN_START_ACTUAL,
				JobTable.COLUMN_END, JobTable.COLUMN_END_ACTUAL, JobTable.COLUMN_STATUS };
		if (projection != null) {
			HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
			HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
			// check if all columns which are requested are available
			if (!availableColumns.containsAll(requestedColumns)) {
				throw new IllegalArgumentException("Unknown columns in projection");
			}
		}
	}

	/**
	 * Determine action by evaluating JSON from message
	 * 
	 * @param message
	 * @return action or NULL if no action
	 */
	public static String getAction(String message) {
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
		Log.v(TAG, "The action is " + action);
		return action;
	}

	public static ContentValues convertMessageToContentValues(String action,
			String message) {
		JSONObject jObject = null;
		int userid = 0;
		int calendar_id = 0;
		int ticket_id = 0;
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
			if (action.equals("insert") || action.equals("update")) {
				start = payload.getInt("start");
				userid = payload.getInt("userid");			
				ticket_id = payload.getInt("ticket_id");
				department = payload.getString("department");
				client_name = payload.getString("client_name");
				companyname = payload.getString("companyname");
				phonenumber = payload.getString("phonenumber");
				address1 = payload.getString("address1");
				address2 = payload.getString("address2");
				city = payload.getString("city");
				extra = payload.getString("extra");	
			}			
		} catch (JSONException e) {
			Log.e(TAG, "Error parsing JSON");
			e.printStackTrace();
		}

		ContentValues values = new ContentValues();		
		values.put(JobTable.COLUMN_CALENDAR_ID, calendar_id);
		values.put(JobTable.COLUMN_START, start);
		values.put(JobTable.COLUMN_TICKET_ID, ticket_id);
		values.put(JobTable.COLUMN_USERID, userid);
		values.put(JobTable.COLUMN_DEPARTMENT, department);
		values.put(JobTable.COLUMN_CLIENT_NAME, client_name);
		values.put(JobTable.COLUMN_COMPANYNAME, companyname);
		values.put(JobTable.COLUMN_PHONENUMBER, phonenumber);
		values.put(JobTable.COLUMN_ADDRESS1, address1);
		values.put(JobTable.COLUMN_ADDRESS2, address2);
		values.put(JobTable.COLUMN_CITY, city);
		values.put(JobTable.COLUMN_EXTRA, extra);
		return values;
	}

}