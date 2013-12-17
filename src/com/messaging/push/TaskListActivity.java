package com.messaging.push;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.messaging.push.contentprovider.MyTaskContentProvider;
import com.messaging.push.db.TaskTable;

/*
 * TaskListActivity displays the existing task items in a list
 * 
 * You can delete existing ones via a long press on the item
 */

public class TaskListActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {

	private static final int DELETE_ID = Menu.FIRST;
	private SimpleCursorAdapter adapter;

	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.task_list);
		this.getListView().setDividerHeight(2);
		fillData();
		registerForContextMenu(getListView());
	}

	// create the menu based on the XML definition
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.listmenu, menu);
		return true;
	}

	// Reaction to the menu selection
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {		
		case R.id.delete_all:
			deleteAll();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case DELETE_ID:
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			Uri uri = Uri.parse(MyTaskContentProvider.CONTENT_URI + "/"	+ info.id);
			getContentResolver().delete(uri, null, null);
			fillData();
			return true;
		}
		return super.onContextItemSelected(item);
	}
	
	private void deleteAll() {
		Uri uri = Uri.parse(MyTaskContentProvider.CONTENT_URI + "/");
		getContentResolver().delete(uri, null, null);
		fillData();
	}

	// Opens the second activity if an entry is clicked
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent i = new Intent(this, TaskDetailActivity.class);
		Uri taskUri = Uri.parse(MyTaskContentProvider.CONTENT_URI + "/" + id);
		i.putExtra(MyTaskContentProvider.CONTENT_ITEM_TYPE, taskUri);

		startActivity(i);
	}

	private void fillData() {

		// Fields from the database (projection)
		// Must include the _id column for the adapter to work
		String[] from = new String[] { TaskTable.COLUMN_DEPARTMENT, TaskTable.COLUMN_START, TaskTable.COLUMN_CITY};
		// Fields on the UI to which we map
		int[] to = new int[] { R.id.department, R.id.start, R.id.city };

		getLoaderManager().initLoader(0, null, this);
		adapter = new SimpleCursorAdapter(this, R.layout.task_row, null, from,
				to, 0);

		setListAdapter(adapter);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, DELETE_ID, 0, R.string.menu_delete);
	}

	// creates a new loader after the initLoader () call
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = { TaskTable.COLUMN_ID, TaskTable.COLUMN_DEPARTMENT, TaskTable.COLUMN_CITY };
		CursorLoader cursorLoader = new CursorLoader(this,
				MyTaskContentProvider.CONTENT_URI, projection, null, null, null);
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		adapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// data is not available anymore, delete reference
		adapter.swapCursor(null);
	}

}