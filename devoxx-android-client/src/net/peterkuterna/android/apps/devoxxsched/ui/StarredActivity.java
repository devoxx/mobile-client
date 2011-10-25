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
import net.peterkuterna.android.apps.devoxxsched.ui.phone.SessionDetailActivity;
import net.peterkuterna.android.apps.devoxxsched.ui.phone.SessionsActivity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.ViewGroup;

/**
 * An activity that shows the user's starred sessions. This activity can be
 * either single or multi-pane, depending on the device configuration. We want
 * the multi-pane support that {@link BaseMultiPaneActivity} offers, so we
 * inherit from it instead of {@link BaseSinglePaneActivity}.
 */
public class StarredActivity extends BaseMultiPaneActivity {

	private SessionsFragment mSessionsFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_starred);

		Intent intent = new Intent();
		intent.setData(CfpContract.Sessions.CONTENT_STARRED_URI);

		final FragmentManager fm = getSupportFragmentManager();
		mSessionsFragment = (SessionsFragment) fm.findFragmentByTag("sessions");
		if (mSessionsFragment == null) {
			mSessionsFragment = new SessionsFragment();
			mSessionsFragment.setArguments(intentToFragmentArguments(intent));
			fm.beginTransaction()
					.add(R.id.root_container, mSessionsFragment, "sessions")
					.commit();
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

}
