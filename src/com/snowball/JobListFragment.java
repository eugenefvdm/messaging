/*
 * Display tasks based on filter in a list view 
 */

package com.snowball;

import java.util.Date;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.content.Context;
import android.content.Intent;

import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.snowball.R;
import com.snowball.db.TaskContentProvider;
import com.snowball.db.TaskTable;

public class JobListFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<Cursor> {
	
	private static final int CALL_CLIENT_ID = Menu.FIRST;

	private SimpleCursorAdapter adapter;

	/**
	 * Helper because getActivity is called often
	 */
	private FragmentActivity ctx;

	private String mFilter;

	private static final String TAG = "TasksFragment";
	
	public static JobListFragment newInstance(int num, String filter) {
		JobListFragment f = new JobListFragment();
        Bundle args = new Bundle();
        args.putInt("num", num);
        args.putString("filter", filter);
        f.setArguments(args);
        return f;
    }
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		mFilter = args.getString("filter");					
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		ctx = getActivity();
		View rootView = inflater.inflate(R.layout.fragment_tasks, container, false);		
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		fillData(mFilter);
		setHasOptionsMenu(true);
		registerForContextMenu(getListView());
	}

	// Opens the Detail activity if an entry is clicked
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent i = new Intent(ctx, JobDetailActivity.class);
		Uri taskUri = Uri.parse(TaskContentProvider.CONTENT_URI + "/" + id);
		i.putExtra(TaskContentProvider.CONTENT_ITEM_TYPE, taskUri);
		startActivity(i);
	}

	private String getPhonenumber(Uri uri) {
		String[] projection = { TaskTable.COLUMN_PHONENUMBER };
		Cursor cursor = ctx.getContentResolver().query(uri, projection, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
			String phonenumber = cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.COLUMN_PHONENUMBER));
			cursor.close();
			return phonenumber;
		}
		return null;
	}

	/**
	 * Populate cursor for row inflation
	 * 
	 * Note: Every time new fields are added the String[] has to be updated TODO
	 * Find more elegant way of doing this, e.g. use global
	 */
	private void fillData(String filter) {
		Log.v(TAG, "Filter specified is " + filter);
		// Fields from the database (projection) must include the _id column for
		// the adapter to work
		String[] from = new String[] {
				TaskTable.COLUMN_DEPARTMENT, TaskTable.COLUMN_CITY,
				TaskTable.COLUMN_CLIENT_NAME, TaskTable.COLUMN_START };
		// Fields on the UI to which we map
		int[] to = new int[] {
				R.id.department, R.id.start, R.id.city, R.id.client_name };
		getLoaderManager().initLoader(0, null, this);
		adapter = new TasksAdapter(ctx, R.layout.task_row, null, from, to, 0);
		setListAdapter(adapter);
	}

	// creates a new loader after the initLoader () call
	// NB!NB! important to update if you add new fields!!
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = {
				TaskTable.COLUMN_ID, TaskTable.COLUMN_DEPARTMENT,
				TaskTable.COLUMN_CITY, TaskTable.COLUMN_CLIENT_NAME,
				TaskTable.COLUMN_START };
		String selection = "status = ?";
		String[] selectionArgs = { mFilter };
		CursorLoader cursorLoader = new CursorLoader(ctx, TaskContentProvider.CONTENT_URI, projection, selection, selectionArgs, null);
		return cursorLoader;
	}

	/**
	 * Tasks row inflater TODO might be able to move this to separate class
	 */
	public static class TasksAdapter extends SimpleCursorAdapter {
		private LayoutInflater layoutInflater;
		private int layout;

		public TasksAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to, int flags) {
			super(context, layout, c, from, to, 0);
			this.layout = layout;
			layoutInflater = LayoutInflater.from(context);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = layoutInflater.inflate(layout, parent, false);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			String department = cursor.getString(cursor.getColumnIndex(TaskTable.COLUMN_DEPARTMENT));			
			String city = cursor.getString(cursor.getColumnIndex(TaskTable.COLUMN_CITY));
			String client_name = cursor.getString(cursor.getColumnIndex(TaskTable.COLUMN_CLIENT_NAME));
			long unixStart = cursor.getLong(cursor.getColumnIndex(TaskTable.COLUMN_START));
			Date d = new Date(unixStart * 1000);
			TextView tv1 = (TextView) view.findViewById(R.id.start);
			tv1.setText(DateFormat.format("E d hh:mm", d));
			TextView tv2 = (TextView) view.findViewById(R.id.department);
			tv2.setText(department);
			TextView tv3 = (TextView) view.findViewById(R.id.city);
			tv3.setText(city);
			TextView tv4 = (TextView) view.findViewById(R.id.client_name);
			tv4.setText(client_name);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, CALL_CLIENT_ID, 0, R.string.context_menu_list_call);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case CALL_CLIENT_ID:
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			Uri uri = Uri.parse(TaskContentProvider.CONTENT_URI + "/" + info.id);
			String phonenumber = getPhonenumber(uri);
			Log.i(TAG, "Calling " + phonenumber);
			Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phonenumber));
			startActivity(intent);
			return true;
		}
		return super.onContextItemSelected(item);
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
