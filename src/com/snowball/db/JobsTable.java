package com.snowball.db;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class JobsTable {

	// Database table
	public static final String TABLE_JOBS = "jobs";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_CLIENT_ID = "client_id";
	public static final String COLUMN_CALENDAR_ID = "calendar_id";
	public static final String COLUMN_START = "start";
	public static final String COLUMN_END = "stop";
	public static final String COLUMN_TICKET_ID = "ticket_id";
	public static final String COLUMN_DEPARTMENT = "department";
	public static final String COLUMN_COMPANYNAME = "companyname";
	public static final String COLUMN_PHONENUMBER = "phonenumber";
	public static final String COLUMN_CLIENT_NAME = "client_name";	
	public static final String COLUMN_ADDRESS1 = "address1";
	public static final String COLUMN_ADDRESS2 = "address2";
	public static final String COLUMN_CITY = "city";
	public static final String COLUMN_EXTRA = "extra";
	public static final String COLUMN_START_ACTUAL = "start_actual";
	public static final String COLUMN_END_ACTUAL = "stop_actual";
	// Status can be outstanding / started / paused / completed / cancelled / in progress
	public static final String COLUMN_STATUS = "status";

	// Database creation SQL statement
	private static final String CREATE_TABLE_JOBS = "create table " + TABLE_JOBS
		+ "(" 
		+ COLUMN_ID + " integer primary key autoincrement, "
		+ COLUMN_CLIENT_ID + " integer not null, "
		+ COLUMN_CALENDAR_ID + " integer not null, "
		+ COLUMN_TICKET_ID + " integer not null, "
		+ COLUMN_DEPARTMENT + " text not null, "
		+ COLUMN_COMPANYNAME + " text, "
		+ COLUMN_PHONENUMBER + " text not null, "
		+ COLUMN_CLIENT_NAME + " text not null,"
		+ COLUMN_ADDRESS1 + " text not null,"
		+ COLUMN_ADDRESS2 + " text not null,"
		+ COLUMN_CITY + " text not null,"
		+ COLUMN_EXTRA + " text,"
		+ COLUMN_START + " int,"
		+ COLUMN_END + " int,"
		+ COLUMN_START_ACTUAL + " int,"
		+ COLUMN_END_ACTUAL + " int,"
		+ COLUMN_STATUS + " text default 'outstanding'"
		+ ");";
	
	// Database table
	public static final String TABLE_NOTES = "notes";
	public static final String COLUMN_NOTE_ID = "_id";
	public static final String COLUMN_NOTE_WHMCS_ID = "whmcs_id";
	public static final String COLUMN_NOTE_TICKET_ID = "ticket_id";
	public static final String COLUMN_NOTE_ADMIN = "admin";
	public static final String COLUMN_NOTE_DATE = "date";
	public static final String COLUMN_NOTE_MESSAGE = "message";

	// Database creation SQL statement
	private static final String CREATE_TABLE_NOTES = "create table " + TABLE_NOTES
		+ "(" 
		+ COLUMN_NOTE_ID + " integer primary key autoincrement, "
		+ COLUMN_NOTE_WHMCS_ID + " integer, "
		+ COLUMN_NOTE_TICKET_ID + " integer, "
		+ COLUMN_NOTE_ADMIN + " integer not null, "
		+ COLUMN_NOTE_DATE + " text not null, "
		+ COLUMN_NOTE_MESSAGE + " text not null"			
		+ ");";

	public static void onCreate(SQLiteDatabase database) {
		Log.v("database", "onCreate execSQL start");
		database.execSQL(CREATE_TABLE_JOBS);
		database.execSQL(CREATE_TABLE_NOTES);
		Log.v("database", "onCreate execSQL end");
	}
	
	public static void delete(SQLiteDatabase database) {
		// TODO For now we drop the database every time the database is upgraded
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