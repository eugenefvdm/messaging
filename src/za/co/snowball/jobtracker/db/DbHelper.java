package za.co.snowball.jobtracker.db;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbHelper extends SQLiteOpenHelper {

  private static final String DATABASE_NAME = "jobs.db";
  private static final int DATABASE_VERSION = 31;

  public DbHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  // Method is called during creation of the database
  @Override
  public void onCreate(SQLiteDatabase database) {
	  Log.v("database TaskDatabaseHelper", "onCreate");
    Table.onCreate(database);
  }

  // Method is called during an upgrade of the database,
  // e.g. if you increase the database version
  @Override
  public void onUpgrade(SQLiteDatabase database, int oldVersion,  int newVersion) {
	  Log.v("database TaskDatabaseHelper","Checking if update start");
	  //TaskTable.delete(database);
    Table.onUpgrade(database, oldVersion, newVersion);
    Log.v("database TaskDatabaseHelper","Checking if update stop");
  }
}
 