package za.co.snowball.jobtracker.db;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class Table {
	
	// Constants used throughout the app to refer to statuses, e.g. when referring to tabs
	public static final String JOB_STATUS_OUTSTANDING = "Outstanding";
	public static final String JOB_STATUS_STARTED = "Started";
	public static final String JOB_STATUS_PAUSED = "Paused";
	public static final String JOB_STATUS_COMPLETED = "Completed";
	public static final String JOB_STATUS_DECLINED = "Declined";

	// Database table
	public static final String TABLE_JOBS = "jobs";
	public static final String COLUMN_JOB_ID = "_id";
	public static final String COLUMN_JOB_CLIENT_ID = "client_id";
	public static final String COLUMN_JOB_CALENDAR_ID = "calendar_id";	
	public static final String COLUMN_JOB_TICKET_ID = "ticket_id";
	public static final String COLUMN_JOB_TITLE = "title";
	public static final String COLUMN_JOB_DEPARTMENT = "department";
	public static final String COLUMN_JOB_CLIENT_NAME = "client_name";
	public static final String COLUMN_JOB_COMPANYNAME = "companyname";
	public static final String COLUMN_JOB_PHONENUMBER = "phonenumber";
	public static final String COLUMN_JOB_ALTERNATE_PHONE = "alternate_phone";
	public static final String COLUMN_JOB_ADDRESS1 = "address1";
	public static final String COLUMN_JOB_ADDRESS2 = "address2";
	public static final String COLUMN_JOB_CITY = "city";
	public static final String COLUMN_JOB_START = "start";
	public static final String COLUMN_JOB_START_ACTUAL = "start_actual";
	public static final String COLUMN_JOB_END = "stop";
	public static final String COLUMN_JOB_END_ACTUAL = "stop_actual";
	public static final String COLUMN_JOB_CUSTOM_FIELDS = "custom_fields";
	// Status can be Outstanding / Started / Paused / Completed / Cancelled / In Progress / Starting Travel / Ended Travel
	public static final String COLUMN_JOB_STATUS = "status";
	public static final String COLUMN_JOB_EXTRA = "extra";	

	// Database creation SQL statement
	private static final String CREATE_TABLE_JOBS = "create table " + TABLE_JOBS
		+ "(" 
		+ COLUMN_JOB_ID + " integer primary key autoincrement, "
		+ COLUMN_JOB_CLIENT_ID + " integer not null, "
		+ COLUMN_JOB_CALENDAR_ID + " integer not null, "
		+ COLUMN_JOB_TICKET_ID + " integer not null, "
		+ COLUMN_JOB_DEPARTMENT + " text not null, "
		+ COLUMN_JOB_TITLE + " text, "
		+ COLUMN_JOB_CLIENT_NAME + " text not null,"
		+ COLUMN_JOB_COMPANYNAME + " text, "
		+ COLUMN_JOB_PHONENUMBER + " text not null, "
		+ COLUMN_JOB_ALTERNATE_PHONE + " text, "
		+ COLUMN_JOB_ADDRESS1 + " text not null,"
		+ COLUMN_JOB_ADDRESS2 + " text not null,"
		+ COLUMN_JOB_CITY + " text not null,"		
		+ COLUMN_JOB_START + " int,"
		+ COLUMN_JOB_START_ACTUAL + " int,"
		+ COLUMN_JOB_END + " int,"	
		+ COLUMN_JOB_END_ACTUAL + " int,"
		+ COLUMN_JOB_CUSTOM_FIELDS + " text,"
		+ COLUMN_JOB_STATUS + " text default '" + JOB_STATUS_OUTSTANDING + "',"
		+ COLUMN_JOB_EXTRA + " text"
		
		+ ");";
	
	// Database table
	public static final String TABLE_NOTES = "notes";
	public static final String COLUMN_NOTE_ID = "_id";
	public static final String COLUMN_NOTE_CALENDAR_ID = "calendar_id";
	public static final String COLUMN_NOTE_TICKET_ID = "ticket_id";
	public static final String COLUMN_NOTE_ADMIN_NAME = "admin_name";
	public static final String COLUMN_NOTE_DATE = "date";
	public static final String COLUMN_NOTE_MESSAGE = "message";

	// Database creation SQL statement
	private static final String CREATE_TABLE_NOTES = "create table " + TABLE_NOTES
		+ "(" 
		+ COLUMN_NOTE_ID + " integer primary key autoincrement, "
		+ COLUMN_NOTE_CALENDAR_ID + " integer not null, "
		+ COLUMN_NOTE_TICKET_ID + " integer not null, "
		+ COLUMN_NOTE_ADMIN_NAME + " text not null, "
		+ COLUMN_NOTE_DATE + " datetime default current_timestamp, "
		+ COLUMN_NOTE_MESSAGE + " text not null"			
		+ ");";

	public static void onCreate(SQLiteDatabase database) {
		Log.v("database", "onCreate execSQL start");
		database.execSQL(CREATE_TABLE_JOBS);
		database.execSQL(CREATE_TABLE_NOTES);
		Log.v("database", "onCreate execSQL end");
	}
	
	public static void delete(SQLiteDatabase database) {
		// For now we drop the database every time the database is upgraded
		//database.execSQL("DROP TABLE IF EXISTS " + TABLE_TASK);		
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		//if (oldVersion < 10) {
			//onCreate(database);
		//}
		//
		Log.w("database", "Upgrading database from version "
				+ oldVersion + " to " + newVersion
				+ ", which will destroy all old data");
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_JOBS);
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
		onCreate(database);
	}
}