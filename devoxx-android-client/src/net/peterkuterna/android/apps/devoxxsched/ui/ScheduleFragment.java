/*
 * Copyright 2011 Google Inc.
 * Copyright 2011 Peter Kuterna
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.peterkuterna.android.apps.devoxxsched.ui;

import java.util.ArrayList;

import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.ObservableScrollView;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.ObservableScrollView.OnScrollListener;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.SwipeyTabs;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.SwipeyTabsAdapter;
import net.peterkuterna.android.apps.devoxxsched.util.AnalyticsUtils;
import net.peterkuterna.android.apps.devoxxsched.util.Lists;
import net.peterkuterna.android.apps.devoxxsched.util.ParserUtils;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.view.ViewPager;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class ScheduleFragment extends Fragment {

	protected static final String TAG = "ScheduleFragment";

	private static final int NUMBER_DAYS = 5;
	private static final long[] START_DAYS_IN_MILLIS = {
			ParserUtils.parseTime("2011-11-14T00:00:00.000+01:00"),
			ParserUtils.parseTime("2011-11-15T00:00:00.000+01:00"),
			ParserUtils.parseTime("2011-11-16T00:00:00.000+01:00"),
			ParserUtils.parseTime("2011-11-17T00:00:00.000+01:00"),
			ParserUtils.parseTime("2011-11-18T00:00:00.000+01:00"), };

	private ViewPager mViewPager;
	private SwipeyTabs mTabs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		AnalyticsUtils.getInstance(getActivity()).trackPageView("/Schedule");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		ViewGroup root = (ViewGroup) inflater.inflate(
				R.layout.fragment_schedule, null);

		mViewPager = (ViewPager) root.findViewById(R.id.viewpager);
		mTabs = (SwipeyTabs) root.findViewById(R.id.viewpagerheader);

		return root;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		final BlocksPagerAdapter adapter = new BlocksPagerAdapter(
				getActivity(), getSupportFragmentManager());
		mViewPager.setAdapter(adapter);
		mTabs.setAdapter(adapter);
		mViewPager.setOnPageChangeListener(mTabs);

		getActivity().runOnUiThread(new Runnable() {
			public void run() {
				updateNowView(true);
			}
		});
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.schedule_menu_items, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_now) {
			if (!updateNowView(true)) {
				Toast.makeText(getActivity(), R.string.toast_now_not_visible,
						Toast.LENGTH_SHORT).show();
				return true;
			}
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Update position and visibility of "now" view.
	 */
	private boolean updateNowView(boolean forceScroll) {
		final long now = UIUtils.getCurrentTime(getActivity());

		int nowDay = -1;
		for (int i = 0; i < START_DAYS_IN_MILLIS.length; i++) {
			if (now >= START_DAYS_IN_MILLIS[i]
					&& now <= (START_DAYS_IN_MILLIS[i] + DateUtils.DAY_IN_MILLIS)) {
				nowDay = i;
				break;
			}
		}

		if (nowDay != -1 && forceScroll) {
			mViewPager.setCurrentItem(nowDay);
			return true;
		}

		return false;
	}

	private class BlocksPagerAdapter extends FragmentPagerAdapter implements
			SwipeyTabsAdapter, OnScrollListener {

		private static final int TIME_FLAGS = DateUtils.FORMAT_SHOW_WEEKDAY;

		private final Context mContext;
		private final ArrayList<BlocksFragment> mFragments = Lists
				.newArrayList();
		private int mScrollY = -1;

		public BlocksPagerAdapter(Context context, FragmentManager fm) {
			super(fm);

			this.mContext = context;
		}

		@Override
		public Fragment getItem(int position) {
			return BlocksFragment.newInstance(position,
					START_DAYS_IN_MILLIS[position], mScrollY);
		}

		@Override
		public Object instantiateItem(View container, int position) {
			BlocksFragment fragment = (BlocksFragment) super.instantiateItem(
					container, position);

			fragment.setOnScrollListener(this);
			if (mScrollY != -1) {
				fragment.setScrollY(mScrollY);
			}

			while (mFragments.size() <= position) {
				mFragments.add(null);
			}
			mFragments.set(position, fragment);

			return fragment;
		}

		@Override
		public void destroyItem(View container, int position, Object object) {
			super.destroyItem(container, position, object);

			mFragments.set(position, null);
		}

		@Override
		public TextView getTab(final int position, SwipeyTabs root) {
			TextView view = (TextView) LayoutInflater.from(mContext).inflate(
					R.layout.swipey_tab_indicator, root, false);
			view.setText(DateUtils.formatDateTime(getActivity(),
					START_DAYS_IN_MILLIS[position], TIME_FLAGS).toUpperCase());
			view.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					mViewPager.setCurrentItem(position);
				}
			});
			return view;
		}

		@Override
		public int getCount() {
			return NUMBER_DAYS;
		}

		@Override
		public void onScrollChanged(ObservableScrollView view) {
			mScrollY = view.getScrollY();

			for (BlocksFragment fragment : mFragments) {
				if (fragment != null) {
					final ScrollView scrollView = fragment.getScrollView();
					if (!view.equals(scrollView)) {
						fragment.setScrollY(mScrollY);
					}
				}
			}
		}

	}

}
