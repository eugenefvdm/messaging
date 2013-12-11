package com.messaging.push.contentprovider;

import java.util.Arrays;
import java.util.HashSet;

import org.json.JSONException;
import org.json.JSONObject;

import com.messaging.push.db.TaskDatabaseHelper;
import com.messaging.push.db.TaskTable;

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

public class MyTaskContentProvider extends ContentProvider {

	// database
	private TaskDatabaseHelper database;

	// used for the UriMacher
	private static final int TASKS = 10;
	private static final int TASK_ID = 20;

	private static final String AUTHORITY = "com.messaging.push";

	private static final String BASE_PATH = "tasks";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + BASE_PATH);

	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/tasks";
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/task";

	private static final UriMatcher sURIMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);
	static {
		sURIMatcher.addURI(AUTHORITY, BASE_PATH, TASKS);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", TASK_ID);
	}

	@Override
	public boolean onCreate() {
		Log.v("database MyTaskContentProvider", "onCreate start");
		database = new TaskDatabaseHelper(getContext());
		Log.v("database", "onCreate end");
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
		queryBuilder.setTables(TaskTable.TABLE_TASK);

		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case TASKS:
			break;
		case TASK_ID:
			// adding the ID to the original query
			queryBuilder.appendWhere(TaskTable.COLUMN_ID + "="
					+ uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		SQLiteDatabase db = database.getWritableDatabase();
		Cursor cursor = queryBuilder.query(db, projection, selection,
				selectionArgs, null, null, sortOrder);
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
			id = sqlDB.insert(TaskTable.TABLE_TASK, null, values);
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
			rowsDeleted = sqlDB.delete(TaskTable.TABLE_TASK, selection,
					selectionArgs);
			break;
		case TASK_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = sqlDB.delete(TaskTable.TABLE_TASK,
						TaskTable.COLUMN_ID + "=" + id, null);
			} else {
				rowsDeleted = sqlDB.delete(TaskTable.TABLE_TASK,
						TaskTable.COLUMN_ID + "=" + id + " and " + selection,
						selectionArgs);
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
			rowsUpdated = sqlDB.update(TaskTable.TABLE_TASK, values, selection,
					selectionArgs);
			break;
		case TASK_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = sqlDB.update(TaskTable.TABLE_TASK, values,
						TaskTable.COLUMN_ID + "=" + id, null);
			} else {
				rowsUpdated = sqlDB.update(TaskTable.TABLE_TASK, values,
						TaskTable.COLUMN_ID + "=" + id + " and " + selection,
						selectionArgs);
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
			TaskTable.COLUMN_TICKET_ID,
			TaskTable.COLUMN_DEPARTMENT,
			TaskTable.COLUMN_CLIENT,
			TaskTable.COLUMN_ADDRESS,
			TaskTable.COLUMN_ID,
			TaskTable.COLUMN_START
		};
		//String[] available = { TaskTable.COLUMN_DEPARTMENT,	TaskTable.COLUMN_CLIENT, TaskTable.COLUMN_ADDRESS,	TaskTable.COLUMN_ID };
		if (projection != null) {
			HashSet<String> requestedColumns = new HashSet<String>(
					Arrays.asList(projection));
			HashSet<String> availableColumns = new HashSet<String>(
					Arrays.asList(available));
			// check if all columns which are requested are available
			if (!availableColumns.containsAll(requestedColumns)) {
				throw new IllegalArgumentException(
						"Unknown columns in projection");
			}
		}
	}
	
	public static ContentValues convertMessageToContentValues(String message) {
		int ticket_id = 0;
		String department = null;
		String client = null;
		String address = null;

		JSONObject jObject;
		try {
			jObject = new JSONObject(message);
			ticket_id = jObject.getInt("ticket_id");
			department = jObject.getString("department");
			client = jObject.getString("client");
			address = jObject.getString("address");
		} catch (JSONException e) {
			Log.e("JSON", "Error parsing JSON in MainActivity");
			e.printStackTrace();
			return null;
		}
//		Toast.makeText(getApplicationContext(), "Department: " + department,
//				Toast.LENGTH_LONG).show();

		ContentValues values = new ContentValues();
		values.put(TaskTable.COLUMN_TICKET_ID, ticket_id);
		values.put(TaskTable.COLUMN_DEPARTMENT, department);
		values.put(TaskTable.COLUMN_CLIENT, client);
		values.put(TaskTable.COLUMN_ADDRESS, address);
		return values;
	}

}