package com.messaging.push._backup;

import java.util.ArrayList;
import java.util.List;



import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * @author eugene
 *
 */
public class EventsDataSource {

  // Database fields
  private SQLiteDatabase database;
  private MySQLiteHelper dbHelper;
  private String[] allColumns = { MySQLiteHelper.COLUMN_ID,
      MySQLiteHelper.COLUMN_EVENT };

  public EventsDataSource(Context context) {
    dbHelper = new MySQLiteHelper(context);
  }

  public void open() throws SQLException {
    database = dbHelper.getWritableDatabase();
  }

  public void close() {
    dbHelper.close();
  }

  public Event createEvent(String action) {
    ContentValues values = new ContentValues();
    values.put(MySQLiteHelper.COLUMN_EVENT, action);
    long insertId = database.insert(MySQLiteHelper.TABLE_EVENTS, null,
        values);
    Cursor cursor = database.query(MySQLiteHelper.TABLE_EVENTS,
        allColumns, MySQLiteHelper.COLUMN_ID + " = " + insertId, null,
        null, null, null);
    cursor.moveToFirst();
    Event newEvent = cursorToEvent(cursor);
    cursor.close();
    return newEvent;
  }

  public void deleteComment(Event event) {
    long id = event.getId();
    System.out.println("Comment deleted with id: " + id);
    database.delete(MySQLiteHelper.TABLE_EVENTS, MySQLiteHelper.COLUMN_ID
        + " = " + id, null);
  }

  public List<Event> getAllComments() {
    List<Event> events = new ArrayList<Event>();

    Cursor cursor = database.query(MySQLiteHelper.TABLE_EVENTS,
        allColumns, null, null, null, null, null);

    cursor.moveToFirst();
    while (!cursor.isAfterLast()) {
    	Event event = cursorToEvent(cursor);
      events.add(event);
      cursor.moveToNext();
    }
    // make sure to close the cursor
    cursor.close();
    return events;
  }

  private Event cursorToEvent(Cursor cursor) {
    Event event = new Event();
    event.setId(cursor.getLong(0));
    event.setEvent(cursor.getString(1));
    return event;
  }
} 