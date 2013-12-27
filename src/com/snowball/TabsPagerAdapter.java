package com.snowball;

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
			jobListFragment = JobListFragment.newInstance(index, "completed");
			return jobListFragment;
		case 1:
			jobListFragment = JobListFragment.newInstance(index, "outstanding");
			return jobListFragment;
		}

		return null;
	}

	@Override
	public int getCount() {	
		return 2;
	}

}
