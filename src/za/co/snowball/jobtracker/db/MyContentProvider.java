package za.co.snowball.jobtracker.db;

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
public class MyContentProvider extends ContentProvider {

	private static final String TAG = "MyContentProvider";

	// database
	private DbHelper database;

	// used for the UriMacher
	private static final int JOBS = 10;	
	private static final int JOB_ID = 20;
	private static final int JOB_CALENDAR_ID = 30;
	private static final int NOTES = 40;
	private static final int NOTE_ID = 50;

	private static final String AUTHORITY = "za.co.snowball.jobtracker";

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
		database = new DbHelper(getContext());
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
		Log.v(TAG, "Uri match " + uri + " = uriType " + uriType);
		switch (uriType) {
		case JOBS:
			queryBuilder.setTables(Table.TABLE_JOBS);
			break;
		case JOB_ID:
			Log.i(TAG, "uriType match on JOB_ID");
			// adding the ID to the original query
			queryBuilder.setTables(Table.TABLE_JOBS);
			queryBuilder.appendWhere(Table.COLUMN_JOB_ID + "=" + uri.getLastPathSegment());
			break;
		case JOB_CALENDAR_ID:
			// adding the ticket_id to the original query
			queryBuilder.setTables(Table.TABLE_JOBS);
			queryBuilder.appendWhere(Table.COLUMN_JOB_TICKET_ID + "=" + uri.getLastPathSegment());
			break;
		case NOTE_ID:
			queryBuilder.setTables(Table.TABLE_NOTES);
			queryBuilder.appendWhere(Table.COLUMN_NOTE_TICKET_ID + "=" + uri.getLastPathSegment());
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
		String returnUri;
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		// int rowsDeleted = 0;
		long id = 0;
		Log.d(TAG, "Switching on Uri: " + uri);
		switch (uriType) {
		
		case JOBS:
			id = sqlDB.insert(Table.TABLE_JOBS, null, values);
			returnUri = BASE_PATH_JOBS + "/" + id;
			break;
		case NOTES:
			Log.d(TAG, "ContentProvider insert adding a note message " + values.getAsString("message"));
			id = sqlDB.insert(Table.TABLE_NOTES, null, values);
			returnUri = BASE_PATH_NOTES + "/" + id;
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		// TODO return correct Uri based on table
		//return Uri.parse(BASE_PATH_JOBS + "/" + id);
		return Uri.parse(returnUri);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsDeleted = 0;
		switch (uriType) {
		case JOBS:
			rowsDeleted = sqlDB.delete(Table.TABLE_JOBS, selection, selectionArgs);
			break;
		case JOB_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = sqlDB.delete(Table.TABLE_JOBS, Table.COLUMN_JOB_ID + "=" + id, null);
			} else {
				rowsDeleted = sqlDB.delete(Table.TABLE_JOBS, Table.COLUMN_JOB_ID + "=" + id + " and " + selection, selectionArgs);
			}
			break;
		case JOB_CALENDAR_ID:
			String calendar_id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = sqlDB.delete(Table.TABLE_JOBS, Table.COLUMN_JOB_CALENDAR_ID + "=" + calendar_id, null);
			} else {
				rowsDeleted = sqlDB.delete(Table.TABLE_JOBS, Table.COLUMN_JOB_CALENDAR_ID + "=" + calendar_id + " and " + selection, selectionArgs);
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
			rowsUpdated = sqlDB.update(Table.TABLE_JOBS, values, selection, selectionArgs);
			break;
		case JOB_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = sqlDB.update(Table.TABLE_JOBS, values, Table.COLUMN_JOB_ID + "=" + id, null);
			} else {
				rowsUpdated = sqlDB.update(Table.TABLE_JOBS, values, Table.COLUMN_JOB_ID + "=" + id + " and " + selection, selectionArgs);
			}
			break;
		case JOB_CALENDAR_ID:
			String calendar_id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = sqlDB.update(Table.TABLE_JOBS, values, Table.COLUMN_JOB_CALENDAR_ID + "=" + calendar_id, null);
			} else {
				rowsUpdated = sqlDB.update(Table.TABLE_JOBS, values, Table.COLUMN_JOB_CALENDAR_ID + "=" + calendar_id + " and " + selection, selectionArgs);
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
	

}