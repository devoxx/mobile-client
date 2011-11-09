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
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Sessions;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Speakers;
import net.peterkuterna.android.apps.devoxxsched.ui.phone.SessionDetailActivity;
import net.peterkuterna.android.apps.devoxxsched.ui.phone.SessionsActivity;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.ScrollableTabs;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.ScrollableTabsAdapter;
import net.peterkuterna.android.apps.devoxxsched.util.Lists;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * An activity that shows session search results. This activity can be either
 * single or multi-pane, depending on the device configuration. We want the
 * multi-pane support that {@link BaseMultiPaneActivity} offers, so we inherit
 * from it instead of {@link BaseSinglePaneActivity}.
 */
public class SearchActivity extends BaseMultiPaneActivity {

	protected final static String TAG = "SearchActivity";

	private static final int[] TAB_TITLES = { R.string.search_sessions,
			R.string.search_speakers };

	public static final String TAG_SESSIONS = "sessions";
	public static final String TAG_VENDORS = "speakers";

	private String mQuery;

	private ScrollableTabs mTabs;
	private ViewPager mViewPager;
	private SearchPagerAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		mQuery = intent.getStringExtra(SearchManager.QUERY);

		setContentView(R.layout.activity_search);

		if (!UIUtils.isHoneycombTablet(this)) {
			final CharSequence title = getString(R.string.title_search_query,
					mQuery);
			setTitle(title);
		}

		mTabs = (ScrollableTabs) findViewById(R.id.viewpagerheader_search);
		mViewPager = (ViewPager) findViewById(R.id.viewpager_search);
		mViewPager.setPageMargin(getResources().getDimensionPixelSize(
				R.dimen.viewpager_page_margin));
		mViewPager.setPageMarginDrawable(R.drawable.viewpager_margin);
		mAdapter = new SearchPagerAdapter(getSupportFragmentManager());
		mViewPager.setAdapter(mAdapter);
		mTabs.setAdapter(mAdapter);
		mViewPager.setOnPageChangeListener(mTabs);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		ViewGroup detailContainer = (ViewGroup) findViewById(R.id.fragment_container_search_detail);
		if (detailContainer != null && detailContainer.getChildCount() > 1) {
			findViewById(android.R.id.empty).setVisibility(View.GONE);
		}
	}

	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent);
		mQuery = intent.getStringExtra(SearchManager.QUERY);

		if (!UIUtils.isHoneycombTablet(this)) {
			final CharSequence title = getString(R.string.title_search_query,
					mQuery);
			setTitle(title);
		}

		mAdapter.notifyDataSetChanged();

		mViewPager.setCurrentItem(0);
	}

	private Bundle getSessionsFragmentArguments() {
		return intentToFragmentArguments(new Intent(Intent.ACTION_VIEW,
				Sessions.buildSearchUri(mQuery)));
	}

	private Bundle getSpeakersFragmentArguments() {
		return intentToFragmentArguments(new Intent(Intent.ACTION_VIEW,
				Speakers.buildSearchUri(mQuery)));
	}

	@Override
	public BaseMultiPaneActivity.FragmentReplaceInfo onSubstituteFragmentForActivityLaunch(
			String activityClassName) {
		if (findViewById(R.id.fragment_container_search_detail) != null) {
			// The layout we currently have has a detail container, we can add
			// fragments there.
			findViewById(android.R.id.empty).setVisibility(View.GONE);
			if (SessionDetailActivity.class.getName().equals(activityClassName)) {
				clearSelectedItems();
				return new BaseMultiPaneActivity.FragmentReplaceInfo(
						SessionDetailFragment.class, "session_detail",
						R.id.fragment_container_search_detail);
			} else if (SessionsActivity.class.getName().equals(
					activityClassName)) {
				clearSelectedItems();
				return new BaseMultiPaneActivity.FragmentReplaceInfo(
						SessionsFragment.class, "sessions_detail",
						R.id.fragment_container_search_detail);
			}
		}
		return null;
	}

	private void clearSelectedItems() {
		mAdapter.clearSelectedItems();
	}

	private class SearchPagerAdapter extends FragmentStatePagerAdapter
			implements ScrollableTabsAdapter {

		private final ArrayList<Fragment> mFragments = Lists.newArrayList();

		public SearchPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public TextView getTab(final int position, LinearLayout root) {
			final TextView indicator = (TextView) SearchActivity.this
					.getLayoutInflater().inflate(R.layout.tab_indicator, null,
							false);
			indicator.setText(TAB_TITLES[position]);
			indicator.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					mViewPager.setCurrentItem(position);
				}
			});
			return indicator;
		}

		@Override
		public Object instantiateItem(View container, int position) {
			Fragment fragment = (Fragment) super.instantiateItem(container,
					position);

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
		public Fragment getItem(int position) {
			switch (position) {
			case 0: {
				SessionsFragment f = new SessionsFragment();
				f.setArguments(getSessionsFragmentArguments());
				return f;
			}
			case 1: {
				SpeakersListFragment f = new SpeakersListFragment();
				f.setArguments(getSpeakersFragmentArguments());
				return f;
			}
			}
			return null;
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public void finishUpdate(View container) {
			try {
				super.finishUpdate(container);
			} catch (IllegalStateException e) {
				Log.e(TAG, "executePendingTransactions is already executing");
			}
		}

		@Override
		public void notifyDataSetChanged() {
			super.notifyDataSetChanged();

			for (Fragment fragment : mFragments) {
				if (fragment != null) {
					if (fragment instanceof SessionsFragment) {
						((SessionsFragment) fragment).reloadFromArguments(
								getSessionsFragmentArguments(), true);
					} else if (fragment instanceof SpeakersListFragment) {
						((SpeakersListFragment) fragment).reloadFromArguments(
								getSpeakersFragmentArguments(), true);
					}
				}
			}
		}

		public void clearSelectedItems() {
			for (Fragment fragment : mFragments) {
				if (fragment != null) {
					if (fragment instanceof SessionsFragment) {
						((SessionsFragment) fragment).clearCheckedPosition();
					} else if (fragment instanceof SpeakersListFragment) {
						((SpeakersListFragment) fragment)
								.clearCheckedPosition();
					}
				}
			}
		}

	}

}
