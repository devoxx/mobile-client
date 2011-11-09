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

package net.peterkuterna.android.apps.devoxxsched.ui.tablet;

import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Sessions;
import net.peterkuterna.android.apps.devoxxsched.ui.BaseMultiPaneActivity;
import net.peterkuterna.android.apps.devoxxsched.ui.SessionDetailFragment;
import net.peterkuterna.android.apps.devoxxsched.ui.SessionsFragment;
import net.peterkuterna.android.apps.devoxxsched.ui.phone.SessionDetailActivity;
import net.peterkuterna.android.apps.devoxxsched.ui.phone.SessionsActivity;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.ViewGroup;

/**
 * An activity that shows currently playing sessions in a two-pane view.
 */
public class NowPlayingMultiPaneActivity extends BaseMultiPaneActivity {

	private FragmentManager mFragmentManager;
	private SessionsFragment mSessionsFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = new Intent();
		intent.setData(Sessions.buildSessionsAtDirUri(UIUtils.getCurrentTime(this)));

		setContentView(R.layout.activity_now_playing);

		mFragmentManager = getSupportFragmentManager();
		mSessionsFragment = (SessionsFragment) mFragmentManager
				.findFragmentByTag("sessions");
		if (mSessionsFragment == null) {
			mSessionsFragment = new SessionsFragment();
			mSessionsFragment.setArguments(intentToFragmentArguments(intent));
			mFragmentManager
					.beginTransaction()
					.add(R.id.fragment_container_sessions, mSessionsFragment,
							"sessions").commit();
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		ViewGroup detailContainer = (ViewGroup) findViewById(R.id.fragment_container_now_playing_detail);
		if (detailContainer != null && detailContainer.getChildCount() > 1) {
			findViewById(android.R.id.empty).setVisibility(View.GONE);
		}
	}

	@Override
	public FragmentReplaceInfo onSubstituteFragmentForActivityLaunch(
			String activityClassName) {
		findViewById(android.R.id.empty).setVisibility(View.GONE);
		if (SessionsActivity.class.getName().equals(activityClassName)) {
			return new FragmentReplaceInfo(SessionsFragment.class, "sessions",
					R.id.fragment_container_sessions);
		} else if (SessionDetailActivity.class.getName().equals(
				activityClassName)) {
			clearSelectedItems();
			return new FragmentReplaceInfo(SessionDetailFragment.class,
					"session_detail",
					R.id.fragment_container_now_playing_detail);
		}
		return null;
	}

	private void clearSelectedItems() {
		if (mSessionsFragment != null) {
			mSessionsFragment.clearCheckedPosition();
		}
	}
}
