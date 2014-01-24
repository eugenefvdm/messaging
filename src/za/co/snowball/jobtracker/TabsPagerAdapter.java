package za.co.snowball.jobtracker;

import za.co.snowball.jobtracker.db.Table;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;

public class TabsPagerAdapter extends FragmentPagerAdapter {

	public TabsPagerAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Fragment getItem(int index) {
		final ListFragment jobListFragment;
		switch (index) {
		case 0:
			// Return a new instance of the fragment based on 'completed' job record
			jobListFragment = JobListFragment.newInstance(index, Table.JOB_STATUS_COMPLETED);
			return jobListFragment;
		case 1:
			jobListFragment = JobListFragment.newInstance(index, Table.JOB_STATUS_OUTSTANDING);
			return jobListFragment;
		}

		return null;
	}

	@Override
	public int getCount() {	
		return 2;
	}

}
