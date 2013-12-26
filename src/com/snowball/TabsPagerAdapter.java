package com.snowball;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;

public class TabsPagerAdapter extends FragmentPagerAdapter {

	private ListFragment mJobListFragment;

	public TabsPagerAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Fragment getItem(int index) {

		switch (index) {
		case 0:
			mJobListFragment = JobListFragment.newInstance(index, "outstanding");
			return mJobListFragment;
		case 1:
			mJobListFragment = JobListFragment.newInstance(index, "completed");
			return mJobListFragment;
		}

		return null;
	}

	@Override
	public int getCount() {	
		return 2;
	}

}
