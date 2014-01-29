/* 
 * Drawer example from Commonsware 
 * 
 */

package za.co.snowball.jobtracker;

import za.co.snowball.jobtracker.db.Table;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements OnItemClickListener,
		CJobListViewFragment.Contract, OnBackStackChangedListener {
	static private final String STATE_CHECKED = "com.commonsware.android.drawer.simple.STATE_CHECKED";
	private DrawerLayout drawerLayout = null;
	private ActionBarDrawerToggle toggle = null;

	private JobListFragment mOutstandingJobs = null;
	private JobListFragment mCompletedJobs = null;
	private ContentFragment content = null;
	private ListView drawer = null;
	private StuffFragment stuff = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Check if WHMCS e-mail address is blank. If so, fire up settings Activity
		if (CommonUtilities.getEmailAddress(this).equals("")) {
			Toast.makeText(this, "Please choose your WHMCS e-mail address", Toast.LENGTH_LONG).show();
			startActivity(new Intent().setClass(this, PrefsActivity.class));
		}

		if (getFragmentManager().findFragmentById(R.id.content) == null) {
			showOutstandingJobs();
		}

		getFragmentManager().addOnBackStackChangedListener(this);

		drawer = (ListView) findViewById(R.id.drawer);
		drawer.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		String[] rows = getResources().getStringArray(R.array.drawer_rows);

		drawer.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_row, rows));

		if (savedInstanceState == null) {
			drawer.setItemChecked(0, true); // starting here
		}

		drawer.setOnItemClickListener(this);

		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		toggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close);
		drawerLayout.setDrawerListener(toggle);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
	}

	@Override
	public void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putInt(STATE_CHECKED, drawer.getCheckedItemPosition());
	}

	@Override
	public void onRestoreInstanceState(Bundle state) {
		int position = state.getInt(STATE_CHECKED, -1);
		if (position > -1) {
			drawer.setItemChecked(position, true);
		}
	}
			
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		toggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		toggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (toggle.onOptionsItemSelected(item)) {
			return (true);
		}
		return (super.onOptionsItemSelected(item));
	}

	@Override
	public void onItemClick(AdapterView<?> listView, View row, int position,
			long id) {
		switch (position) {
		case 0:
			showOutstandingJobs();
			break;
		case 1:
			showCompletedJobs();
			break;
		case 2:			
			startActivity(new Intent().setClass(this, PrefsActivity.class));
			break;
		default :
			showContent();
			break;
		}

		drawerLayout.closeDrawers();
	}

	@Override
	public void onBackStackChanged() {
		if (mOutstandingJobs.isVisible()) {
			drawer.setItemChecked(0, true);
		} else if (content != null && content.isVisible()) {
			drawer.setItemChecked(1, true);
		}
	}

	@Override
	public void wordClicked() {
		int toClear = drawer.getCheckedItemPosition();		
		if (toClear >= 0) {
			drawer.setItemChecked(toClear, false);
		}

		if (stuff == null) {
			stuff = new StuffFragment();
		}

		getFragmentManager().beginTransaction().replace(R.id.content, stuff).addToBackStack(null).commit();
	}

	private void showOutstandingJobs() {
		if (mOutstandingJobs == null) {						
			mOutstandingJobs = JobListFragment.newInstance(0, Table.JOB_STATUS_OUTSTANDING);			
		}
		if (!mOutstandingJobs.isVisible()) {
			android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.popBackStack();			
			fragmentManager.beginTransaction().replace(R.id.content, mOutstandingJobs).commit();
		}
	}
	
	private void showCompletedJobs() {
		if (mCompletedJobs == null) {
			mCompletedJobs = JobListFragment.newInstance(1, Table.JOB_STATUS_COMPLETED);
		}
		if (!mCompletedJobs.isVisible()) {
			android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.popBackStack();
			fragmentManager.beginTransaction().replace(R.id.content, mCompletedJobs).commit();
		}
	}

	private void showContent() {
		if (content == null) {
			content = new ContentFragment();
		}

		if (!content.isVisible()) {
			getFragmentManager().popBackStack();
			getFragmentManager().beginTransaction().replace(R.id.content, content).commit();
		}
	}
}