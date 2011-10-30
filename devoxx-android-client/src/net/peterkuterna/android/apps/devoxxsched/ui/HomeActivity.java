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
import net.peterkuterna.android.apps.devoxxsched.ui.phone.TwitterSearchActivity;
import net.peterkuterna.android.apps.devoxxsched.util.AnalyticsUtils;
import net.peterkuterna.android.apps.devoxxsched.util.DetachableResultReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class HomeActivity extends AbstractActivity {

	private static final String TAG = "HomeActivity";

	private static final long FIRST_TRIGGER = 30 * 60 * DateUtils.SECOND_IN_MILLIS;

	private SyncStatusUpdaterFragment mSyncStatusUpdaterFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mActivityHelper.onCreate(savedInstanceState);

		AnalyticsUtils.getInstance(this).trackPageView("/Home");

		setContentView(R.layout.activity_home);

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

		FragmentManager fm = getSupportFragmentManager();

		mSyncStatusUpdaterFragment = (SyncStatusUpdaterFragment) fm
				.findFragmentByTag(SyncStatusUpdaterFragment.TAG);
		if (mSyncStatusUpdaterFragment == null) {
			mSyncStatusUpdaterFragment = new SyncStatusUpdaterFragment();
			fm.beginTransaction()
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

}
