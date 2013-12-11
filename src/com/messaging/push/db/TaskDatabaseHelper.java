package com.messaging.push.db;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TaskDatabaseHelper extends SQLiteOpenHelper {

  private static final String DATABASE_NAME = "tasktable.db";
  private static final int DATABASE_VERSION = 12;

  public TaskDatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  // Method is called during creation of the database
  @Override
  public void onCreate(SQLiteDatabase database) {
	  Log.v("database TaskDatabaseHelper", "onCreate");
    TaskTable.onCreate(database);
  }

  // Method is called during an upgrade of the database,
  // e.g. if you increase the database version
  @Override
  public void onUpgrade(SQLiteDatabase database, int oldVersion,  int newVersion) {
	  Log.v("database TaskDatabaseHelper","Checking if update start");
	  //TaskTable.delete(database);
    TaskTable.onUpgrade(database, oldVersion, newVersion);
    Log.v("database TaskDatabaseHelper","Checking if update stop");
  }
}
 