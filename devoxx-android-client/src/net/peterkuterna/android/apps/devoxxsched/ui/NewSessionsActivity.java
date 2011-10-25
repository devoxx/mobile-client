/*
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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.ViewGroup;

/**
 * An activity to show the new session received with a notification. This
 * activity can be either single or multi-pane, depending on the device
 * configuration. We want the multi-pane support that
 * {@link BaseMultiPaneActivity} offers, so we inherit from it instead of
 * {@link BaseSinglePaneActivity}.
 */
public class NewSessionsActivity extends BaseMultiPaneActivity {

	public static final String EXTRA_NEW_SESSION_TIMESTAMP = "net.peterkuterna.android.apps.devoxxsched.extra.NEW_SESSION_TIMESTAMP";

	private SessionsFragment mSessionsFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_starred);

		final long timestamp = getIntent().getLongExtra(
				EXTRA_NEW_SESSION_TIMESTAMP, -1);

		Intent intent = new Intent();
		intent.setData(CfpContract.Sessions.CONTENT_URI);
		intent.putExtra(EXTRA_NEW_SESSION_TIMESTAMP, timestamp);

		final FragmentManager fm = getSupportFragmentManager();
		mSessionsFragment = (SessionsFragment) fm.findFragmentByTag("sessions");
		if (mSessionsFragment == null) {
			mSessionsFragment = new SessionsFragment();
			mSessionsFragment.setArguments(intentToFragmentArguments(intent));
			fm.beginTransaction()
					.add(R.id.root_container, mSessionsFragment, "sessions")
					.commit();
		}
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
