package com.snowball.db;

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
public class JobsContentProvider extends ContentProvider {

	private static final String TAG = "JobContentProvider";

	// database
	private JobsDatabaseHelper database;

	// used for the UriMacher
	private static final int JOBS = 10;	
	private static final int JOB_ID = 20;
	private static final int JOB_CALENDAR_ID = 30;
	private static final int NOTES = 14;
	private static final int NOTE_ID = 15;

	private static final String AUTHORITY = "com.snowball";

	private static final String BASE_PATH_JOBS = "jobs";
	private static final String BASE_PATH_NOTES = "notes";
	
	public static final Uri CONTENT_URI_JOBS = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_JOBS);
	public static final Uri CONTENT_URI_SANS_BASE = Uri.parse("content://" + AUTHORITY);
	
	public static final Uri CONTENT_URI_NOTES = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_NOTES);

	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/jobs";
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/job";
	public static final String CONTENT_ITEM_TICKET = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/ticket";

	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		sURIMatcher.addURI(AUTHORITY, BASE_PATH_JOBS, JOBS);		
		sURIMatcher.addURI(AUTHORITY, BASE_PATH_JOBS + "/#", JOB_ID);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH_JOBS + "/ticket" + "/#", JOB_CALENDAR_ID);
		
		sURIMatcher.addURI(AUTHORITY, BASE_PATH_NOTES, NOTES);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH_NOTES + "/#", NOTE_ID);
	}

	@Override
	public boolean onCreate() {
		database = new JobsDatabaseHelper(getContext());
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		// Using SQLiteQueryBuilder instead of query() method
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		// check if the caller has requested a column which does not exists
		//checkColumns(projection);

		// Set the table
		//String join = "foo LEFT OUTER JOIN bar ON (foo.id = bar.foo_id)";
		//String join = JobsTable.TABLE_JOBS + " LEFT JOIN " + JobsTable.TABLE_JOB_NOTES + " ON (jobs.ticket_id = job_notes.ticketid)";
		//queryBuilder.setTables(JobsTable.TABLE_JOBS);
		//queryBuilder.setTables(join);

		int uriType = sURIMatcher.match(uri);
		Log.v(TAG, "Trying to match uri '" + uri + "' uriType on: " + uriType);
		switch (uriType) {
		case JOBS:
			queryBuilder.setTables(JobsTable.TABLE_JOBS);
			break;
		case JOB_ID:
			Log.i(TAG, "uriType match on JOB_ID");
			// adding the ID to the original query
			queryBuilder.setTables(JobsTable.TABLE_JOBS);
			queryBuilder.appendWhere(JobsTable.COLUMN_ID + "=" + uri.getLastPathSegment());
			break;
		case JOB_CALENDAR_ID:
			// adding the ticket_id to the original query
			queryBuilder.setTables(JobsTable.TABLE_JOBS);
			queryBuilder.appendWhere(JobsTable.COLUMN_TICKET_ID + "=" + uri.getLastPathSegment());
			break;
		case NOTE_ID:
			queryBuilder.setTables(JobsTable.TABLE_NOTES);
			queryBuilder.appendWhere(JobsTable.COLUMN_NOTE_TICKET_ID + "=" + uri.getLastPathSegment());
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
		case JOBS:
			id = sqlDB.insert(JobsTable.TABLE_JOBS, null, values);
			break;
		case NOTES:
			id = sqlDB.insert(JobsTable.TABLE_NOTES, null, values);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return Uri.parse(BASE_PATH_JOBS + "/" + id);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsDeleted = 0;
		switch (uriType) {
		case JOBS:
			rowsDeleted = sqlDB.delete(JobsTable.TABLE_JOBS, selection, selectionArgs);
			break;
		case JOB_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = sqlDB.delete(JobsTable.TABLE_JOBS, JobsTable.COLUMN_ID + "=" + id, null);
			} else {
				rowsDeleted = sqlDB.delete(JobsTable.TABLE_JOBS, JobsTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
			}
			break;
		case JOB_CALENDAR_ID:
			String calendar_id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = sqlDB.delete(JobsTable.TABLE_JOBS, JobsTable.COLUMN_CALENDAR_ID + "=" + calendar_id, null);
			} else {
				rowsDeleted = sqlDB.delete(JobsTable.TABLE_JOBS, JobsTable.COLUMN_CALENDAR_ID + "=" + calendar_id + " and " + selection, selectionArgs);
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
		case JOBS:
			rowsUpdated = sqlDB.update(JobsTable.TABLE_JOBS, values, selection, selectionArgs);
			break;
		case JOB_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = sqlDB.update(JobsTable.TABLE_JOBS, values, JobsTable.COLUMN_ID + "=" + id, null);
			} else {
				rowsUpdated = sqlDB.update(JobsTable.TABLE_JOBS, values, JobsTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
			}
			break;
		case JOB_CALENDAR_ID:
			String calendar_id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = sqlDB.update(JobsTable.TABLE_JOBS, values, JobsTable.COLUMN_CALENDAR_ID + "=" + calendar_id, null);
			} else {
				rowsUpdated = sqlDB.update(JobsTable.TABLE_JOBS, values, JobsTable.COLUMN_CALENDAR_ID + "=" + calendar_id + " and " + selection, selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsUpdated;
	}

//	private void checkColumns(String[] projection) {
//		String[] available = {
//				JobTable.COLUMN_USERID, JobTable.COLUMN_CALENDAR_ID,
//				JobTable.COLUMN_TICKET_ID, JobTable.COLUMN_DEPARTMENT,
//				JobTable.COLUMN_CLIENT_NAME, JobTable.COLUMN_COMPANYNAME,
//				JobTable.COLUMN_PHONENUMBER, JobTable.COLUMN_ADDRESS1,
//				JobTable.COLUMN_ADDRESS2, JobTable.COLUMN_CITY,
//				JobTable.COLUMN_ID, JobTable.COLUMN_START,
//				JobTable.COLUMN_START_ACTUAL, JobTable.COLUMN_END,
//				JobTable.COLUMN_END_ACTUAL, JobTable.COLUMN_STATUS };
//		if (projection != null) {
//			HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
//			HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
//			// check if all columns which are requested are available
//			if (!availableColumns.containsAll(requestedColumns)) {
//				throw new IllegalArgumentException("Unknown columns in projection");
//			}
//		}
//	}

	/**
	 * Determine action by evaluating JSON from message and iterating over first
	 * item TODO Move to MessageReceiver
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

	public static ContentValues getNotes(String action, String message) {
		JSONObject jObject = null;
		ContentValues values = new ContentValues();
		try {
			jObject = new JSONObject(message);
			JSONObject payload = jObject.getJSONObject(action);
			JSONObject notes = payload.getJSONObject("notes");
			Log.i(TAG, "The latest note reads " + notes.getString("message"));
			values.put(JobsTable.COLUMN_NOTE_WHMCS_ID, notes.getString("id"));
			values.put(JobsTable.COLUMN_NOTE_TICKET_ID, notes.getString("ticketid"));
			values.put(JobsTable.COLUMN_NOTE_ADMIN, notes.getString("admin"));
			values.put(JobsTable.COLUMN_NOTE_DATE, notes.getString("date"));
			values.put(JobsTable.COLUMN_NOTE_MESSAGE, notes.getString("message"));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return values;
	}

	/**
	 * Interpret JSON and convert to content values TODO Move to MessageReceiver
	 * 
	 * @param action
	 * @param message
	 * @return
	 */
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
//				JSONObject notes = payload.getJSONObject("notes");
//				Log.i(TAG, "The latest note reads " + notes.getString("message"));
			}
		} catch (JSONException e) {
			Log.e(TAG, "Error parsing JSON");
			e.printStackTrace();
		}

		ContentValues values = new ContentValues();
		values.put(JobsTable.COLUMN_CALENDAR_ID, calendar_id);
		values.put(JobsTable.COLUMN_START, start);
		values.put(JobsTable.COLUMN_TICKET_ID, ticket_id);
		values.put(JobsTable.COLUMN_CLIENT_ID, userid);
		values.put(JobsTable.COLUMN_DEPARTMENT, department);
		values.put(JobsTable.COLUMN_CLIENT_NAME, client_name);
		values.put(JobsTable.COLUMN_COMPANYNAME, companyname);
		values.put(JobsTable.COLUMN_PHONENUMBER, phonenumber);
		values.put(JobsTable.COLUMN_ADDRESS1, address1);
		values.put(JobsTable.COLUMN_ADDRESS2, address2);
		values.put(JobsTable.COLUMN_CITY, city);
		values.put(JobsTable.COLUMN_EXTRA, extra);
		return values;
	}

}