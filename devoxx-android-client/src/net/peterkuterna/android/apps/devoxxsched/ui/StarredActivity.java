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

import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Sessions;
import net.peterkuterna.android.apps.devoxxsched.ui.phone.SessionDetailActivity;
import net.peterkuterna.android.apps.devoxxsched.ui.phone.SessionsActivity;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.ScrollableTabs;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.ScrollableTabsAdapter;
import net.peterkuterna.android.apps.devoxxsched.util.ActivityHelper;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * An activity that shows the user's starred sessions. This activity can be
 * either single or multi-pane, depending on the device configuration. We want
 * the multi-pane support that {@link BaseMultiPaneActivity} offers, so we
 * inherit from it instead of {@link BaseSinglePaneActivity}.
 */
public class StarredActivity extends BaseMultiPaneActivity {

	private FragmentManager mFragmentManager;
	private SessionsFragment mSessionsFragment;

	private ViewPager mViewPager;
	private ViewGroup mTabsContainer;
	private ScrollableTabs mTabs;
	private HomePagerAdapter mAdapter;
	private boolean mDuringConference = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_starred);

		Intent intent = new Intent();
		intent.setData(CfpContract.Sessions.CONTENT_STARRED_URI);

		final FragmentManager fm = getSupportFragmentManager();
		if (findViewById(R.id.root_container) != null) {
			mSessionsFragment = (SessionsFragment) fm
					.findFragmentByTag("sessions");
			if (mSessionsFragment == null) {
				mSessionsFragment = new SessionsFragment();
				mSessionsFragment
						.setArguments(intentToFragmentArguments(intent));
				fm.beginTransaction()
						.add(R.id.root_container, mSessionsFragment, "sessions")
						.commit();
			}
		}

		mViewPager = (ViewPager) findViewById(R.id.viewpager);
		if (mViewPager != null) {
			mViewPager.setPageMargin(getResources().getDimensionPixelSize(
					R.dimen.viewpager_page_margin));
			mViewPager.setPageMarginDrawable(R.drawable.viewpager_margin);
			mAdapter = new HomePagerAdapter(getSupportFragmentManager());
			mViewPager.setAdapter(mAdapter);

			mTabsContainer = (ViewGroup) findViewById(R.id.viewpagerheader_container);
			mTabs = (ScrollableTabs) findViewById(R.id.viewpagerheader);
			mTabs.setAdapter(mAdapter);
			mViewPager.setOnPageChangeListener(mTabs);

			refreshPager();
		}

		mActivityHelper.onCreate(savedInstanceState);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		ViewGroup detailContainer = (ViewGroup) findViewById(R.id.fragment_container_starred_detail);
		if (detailContainer != null && detailContainer.getChildCount() > 1) {
			findViewById(android.R.id.empty).setVisibility(View.GONE);
		}
	}

	@Override
	public FragmentReplaceInfo onSubstituteFragmentForActivityLaunch(
			String activityClassName) {
		if (findViewById(R.id.fragment_container_starred_detail) != null) {
			findViewById(android.R.id.empty).setVisibility(View.GONE);
			if (SessionDetailActivity.class.getName().equals(activityClassName)) {
				clearSelectedItems();
				return new FragmentReplaceInfo(SessionDetailFragment.class,
						"session_detail",
						R.id.fragment_container_starred_detail);
			} else if (SessionsActivity.class.getName().equals(
					activityClassName)) {
				return new FragmentReplaceInfo(SessionsFragment.class,
						"sessions", R.id.root_container);
			}
		}
		return null;
	}

	private void clearSelectedItems() {
		if (mSessionsFragment != null) {
			mSessionsFragment.clearCheckedPosition();
		}
	}

	private void refreshPager() {
		final long currentTimeMillis = UIUtils.getCurrentTime(this);

		if (currentTimeMillis >= UIUtils.START_DAYS_IN_MILLIS[0]
				&& currentTimeMillis < (UIUtils.START_DAYS_IN_MILLIS[UIUtils.NUMBER_DAYS - 1] + DateUtils.DAY_IN_MILLIS)) {
			mDuringConference = true;
		} else {
			mDuringConference = false;
		}

		mTabsContainer.setVisibility(mDuringConference ? View.VISIBLE
				: View.GONE);

		mViewPager.setAdapter(mAdapter);
		mTabs.setAdapter(mAdapter);
	}

	private class HomePagerAdapter extends FragmentPagerAdapter implements
			ScrollableTabsAdapter {

		public HomePagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0:
				SessionsFragment f = new SessionsFragment();
				final Intent intent = new Intent(Intent.ACTION_VIEW,
						Sessions.CONTENT_STARRED_URI);
				f.setArguments(ActivityHelper.intentToFragmentArguments(intent));
				return f;
			case 1:
				return SessionScheduleItemsFragment.newInstance(UIUtils
						.getCurrentTime(StarredActivity.this));
			}
			return null;
		}

		@Override
		public int getCount() {
			return mDuringConference ? 2 : 1;
		}

		@Override
		public TextView getTab(final int position, LinearLayout root) {
			final TextView indicator = (TextView) getLayoutInflater().inflate(
					R.layout.tab_indicator_home, null, false);
			indicator.setText(position == 0 ? R.string.title_all_starred
					: R.string.title_todays_starred);
			indicator.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					mViewPager.setCurrentItem(position);
				}
			});
			return indicator;
		}

	}

}
