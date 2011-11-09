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
import net.peterkuterna.android.apps.devoxxsched.service.AbstractSyncService;
import net.peterkuterna.android.apps.devoxxsched.service.AppEngineSyncService;
import net.peterkuterna.android.apps.devoxxsched.service.CfpSyncManager;
import net.peterkuterna.android.apps.devoxxsched.service.CfpSyncService;
import net.peterkuterna.android.apps.devoxxsched.service.NewsSyncService;
import net.peterkuterna.android.apps.devoxxsched.ui.phone.SessionDetailActivity;
import net.peterkuterna.android.apps.devoxxsched.ui.phone.SessionsActivity;
import net.peterkuterna.android.apps.devoxxsched.ui.phone.TwitterSearchActivity;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.ScrollableTabs;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.ScrollableTabsAdapter;
import net.peterkuterna.android.apps.devoxxsched.util.AnalyticsUtils;
import net.peterkuterna.android.apps.devoxxsched.util.DetachableResultReceiver;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HomeActivity extends BaseMultiPaneActivity implements
		FragmentManager.OnBackStackChangedListener {

	private static final String TAG = "HomeActivity";

	private static final long FIRST_TRIGGER = 30 * 60 * DateUtils.SECOND_IN_MILLIS;

	private FragmentManager mFragmentManager;

	private ViewPager mViewPager;
	private ViewGroup mTabsContainer;
	private ScrollableTabs mTabs;
	private HomePagerAdapter mAdapter;

	private boolean mDuringConference = false;
	private boolean mPauseBackStackWatcher = false;

	private SyncStatusUpdaterFragment mSyncStatusUpdaterFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mActivityHelper.onCreate(savedInstanceState);

		AnalyticsUtils.getInstance(this).trackPageView("/Home");

		setContentView(R.layout.activity_home);

		mFragmentManager = getSupportFragmentManager();

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

		View v = findViewById(R.id.whats_on_stream);
		if (v != null) {
			v.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					AnalyticsUtils.getInstance(HomeActivity.this).trackEvent(
							"Home Screen Dashboard", "Click",
							"Twitter Search Stream", 0);
					Intent intent = new Intent(HomeActivity.this,
							TwitterSearchActivity.class);
					startActivity(intent);
				}
			});
		}

		final View closeButton = findViewById(R.id.close_button);
		if (closeButton != null) {
			closeButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					showHideDetail(false);
				}
			});
		}

		mSyncStatusUpdaterFragment = (SyncStatusUpdaterFragment) mFragmentManager
				.findFragmentByTag(SyncStatusUpdaterFragment.TAG);
		if (mSyncStatusUpdaterFragment == null) {
			mSyncStatusUpdaterFragment = new SyncStatusUpdaterFragment();
			mFragmentManager
					.beginTransaction()
					.add(mSyncStatusUpdaterFragment,
							SyncStatusUpdaterFragment.TAG).commit();

			triggerRefresh();

			final CfpSyncManager syncManager = new CfpSyncManager(this);
			syncManager.setSyncAlarm(FIRST_TRIGGER);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.home_menu_items, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_refresh:
			triggerRefresh();
			return true;
		case R.id.menu_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public FragmentReplaceInfo onSubstituteFragmentForActivityLaunch(
			String activityClassName) {
		if (findViewById(R.id.session_detail_popup) != null) {
			if (SessionsActivity.class.getName().equals(activityClassName)) {
				showHideDetail(true);
				return new FragmentReplaceInfo(SessionsFragment.class,
						"sessions", R.id.fragment_container_session_detail);
			} else if (SessionDetailActivity.class.getName().equals(
					activityClassName)) {
				showHideDetail(true);
				return new FragmentReplaceInfo(SessionDetailFragment.class,
						"session_detail",
						R.id.fragment_container_session_detail);
			}
		}
		return null;
	}

	@Override
	public void onBeforeCommitReplaceFragment(FragmentManager fm,
			FragmentTransaction ft, Fragment fragment) {
		super.onBeforeCommitReplaceFragment(fm, ft, fragment);
		if (fragment instanceof SessionsFragment) {
            mPauseBackStackWatcher = true;
			if (fm.getBackStackEntryCount() > 0) {
				fm.popBackStackImmediate();
			}
            mPauseBackStackWatcher = false;
			ft.addToBackStack(null);
		} else if (fragment instanceof SessionDetailFragment) {
            mPauseBackStackWatcher = true;
			if (fm.getBackStackEntryCount() > 0) {
				fm.popBackStackImmediate();
			}
            mPauseBackStackWatcher = false;
			ft.addToBackStack(null);
		}
	}

	@Override
	public void onAfterCommitReplaceFragment(FragmentManager fm,
			FragmentTransaction ft, Fragment fragment) {
		super.onAfterCommitReplaceFragment(fm, ft, fragment);
	}

	@Override
	public void onBackStackChanged() {
		if (mPauseBackStackWatcher) {
			return;
		}
		
		if (mFragmentManager.getBackStackEntryCount() == 0) {
			showHideDetail(false);
		}
	}

	private void showHideDetail(boolean show) {
		final View detailPopup = findViewById(R.id.session_detail_popup);
		if (show != (detailPopup.getVisibility() == View.VISIBLE)) {
			detailPopup.setVisibility(show ? View.VISIBLE : View.GONE);
			if (!show) {
				Fragment f = mFragmentManager
						.findFragmentByTag("session_detail");
				if (f != null) {
					mFragmentManager.beginTransaction().remove(f).commit();
				}
			}
		}
	}

	private void triggerRefresh() {
		final Intent cfpIntent = new Intent(Intent.ACTION_SYNC, null, this,
				CfpSyncService.class);
		cfpIntent.putExtra(AbstractSyncService.EXTRA_STATUS_RECEIVER,
				mSyncStatusUpdaterFragment.mReceiver);
		startService(cfpIntent);

		final Intent newsIntent = new Intent(Intent.ACTION_SYNC, null, this,
				NewsSyncService.class);
		startService(newsIntent);

		final Intent appEngineIntent = new Intent(Intent.ACTION_SYNC, null,
				this, AppEngineSyncService.class);
		startService(appEngineIntent);
	}

	private void updateRefreshStatus(boolean refreshing) {
		mActionBarHelper.setRefreshActionItemState(refreshing);
	}

	public static class SyncStatusUpdaterFragment extends Fragment implements
			DetachableResultReceiver.Receiver {

		private static final String TAG = SyncStatusUpdaterFragment.class
				.getName();

		private boolean mSyncing = false;
		private DetachableResultReceiver mReceiver = new DetachableResultReceiver(
				new Handler());

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
			mReceiver.setReceiver(this);
		}

		@Override
		public void onReceiveResult(int resultCode, Bundle resultData) {
			HomeActivity activity = (HomeActivity) getSupportActivity();
			if (activity == null) {
				return;
			}

			switch (resultCode) {
			case AbstractSyncService.STATUS_RUNNING: {
				mSyncing = true;
				break;
			}
			case AbstractSyncService.STATUS_FINISHED: {
				mSyncing = false;
				break;
			}
			case AbstractSyncService.STATUS_ERROR: {
				// Error happened down in SyncService, show as toast.
				mSyncing = false;
				// final String errorText = getString(R.string.toast_sync_error,
				// resultData
				// .getString(Intent.EXTRA_TEXT));
				// Toast.makeText(activity, errorText,
				// Toast.LENGTH_LONG).show();
				break;
			}
			}

			activity.updateRefreshStatus(mSyncing);
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			((HomeActivity) getActivity()).updateRefreshStatus(mSyncing);
		}

	}

	private void refreshPager() {
		final long currentTimeMillis = UIUtils.getCurrentTime(this);

		if (currentTimeMillis >= UIUtils.START_DAYS_IN_MILLIS[0]
				&& currentTimeMillis < (UIUtils.START_DAYS_IN_MILLIS[UIUtils.NUMBER_DAYS - 1] + DateUtils.DAY_IN_MILLIS)) {
			mDuringConference = true;
			mFragmentManager.addOnBackStackChangedListener(this);
		} else {
			mDuringConference = false;
			mFragmentManager.removeOnBackStackChangedListener(this);
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
				return TwitterSearchFragment.newInstance();
			case 1:
				return SessionScheduleItemsFragment.newInstance(UIUtils
						.getCurrentTime(HomeActivity.this));
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
			indicator.setText(position == 0 ? R.string.title_twitter_stream
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
