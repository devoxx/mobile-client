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

package net.peterkuterna.android.apps.devoxxfrsched.ui;

import net.peterkuterna.android.apps.devoxxfrsched.R;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract;
import net.peterkuterna.android.apps.devoxxfrsched.ui.phone.SessionDetailActivity;
import net.peterkuterna.android.apps.devoxxfrsched.ui.phone.SessionsActivity;
import net.peterkuterna.android.apps.devoxxfrsched.util.UIUtils;
import android.annotation.SuppressLint;
import android.app.FragmentBreadCrumbs;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListView;

/**
 * An activity to show the speakers in a {@link ListView} or {@link GridView},
 * depending whether the device is a phone or a tablet. This activity can be
 * either single or multi-pane, depending on the device configuration. We want
 * the multi-pane support that {@link BaseMultiPaneActivity} offers, so we
 * inherit from it instead of {@link BaseSinglePaneActivity}.
 */
public class SpeakersActivity extends BaseMultiPaneActivity implements
		View.OnClickListener, FragmentManager.OnBackStackChangedListener {

	private FragmentManager mFragmentManager;

	private FragmentBreadCrumbs mFragmentBreadCrumbs;
	private SpeakersListFragment mSpeakersListFragment;
	private SpeakersGridFragment mSpeakersGridFragment;

	@SuppressLint("NewApi") @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_speakers);

		mFragmentManager = getSupportFragmentManager();
		mFragmentManager.addOnBackStackChangedListener(this);

		Intent intent = new Intent();
		intent.setData(CfpContract.Speakers.CONTENT_URI);

		final FragmentManager fm = getSupportFragmentManager();
		if (UIUtils.isHoneycombTablet(this)) {
			mSpeakersGridFragment = (SpeakersGridFragment) fm
					.findFragmentByTag("speakers");
			if (mSpeakersGridFragment == null) {
				mSpeakersGridFragment = new SpeakersGridFragment();
				mSpeakersGridFragment
						.setArguments(intentToFragmentArguments(intent));
				fm.beginTransaction()
						.add(R.id.root_container, mSpeakersGridFragment,
								"speakers").commit();
			}
		} else {
			mSpeakersListFragment = (SpeakersListFragment) fm
					.findFragmentByTag("speakers");
			if (mSpeakersListFragment == null) {
				mSpeakersListFragment = new SpeakersListFragment();
				mSpeakersListFragment
						.setArguments(intentToFragmentArguments(intent));
				fm.beginTransaction()
						.add(R.id.root_container, mSpeakersListFragment,
								"speakers").commit();
			}
		}

		if (UIUtils.isHoneycombTablet(this)) {
			mFragmentBreadCrumbs = (FragmentBreadCrumbs) findViewById(R.id.breadcrumbs);
			mFragmentBreadCrumbs.setActivity(this);
		}
		mFragmentManager.addOnBackStackChangedListener(this);

		updateBreadCrumb();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		ViewGroup detailContainer = (ViewGroup) findViewById(R.id.fragment_container_speaker_detail);
		if (detailContainer != null && detailContainer.getChildCount() > 0) {
			findViewById(R.id.fragment_container_speaker_detail)
					.setBackgroundColor(0xffffffff);
		}
	}

	@Override
	public FragmentReplaceInfo onSubstituteFragmentForActivityLaunch(
			String activityClassName) {
		if (findViewById(R.id.fragment_container_speaker_detail) != null) {
			if (SessionsActivity.class.getName().equals(activityClassName)) {
				mFragmentManager.popBackStack();
				findViewById(R.id.fragment_container_speaker_detail)
						.setBackgroundColor(0);
				return new FragmentReplaceInfo(SessionsFragment.class,
						"sessions", R.id.fragment_container_speaker_detail);
			} else if (SessionDetailActivity.class.getName().equals(
					activityClassName)) {
				findViewById(R.id.fragment_container_speaker_detail)
						.setBackgroundColor(0);
				return new FragmentReplaceInfo(SessionDetailFragment.class,
						"session_detail",
						R.id.fragment_container_speaker_detail);
			}
		}
		return null;
	}

	@Override
	public void onBeforeCommitReplaceFragment(FragmentManager fm,
			FragmentTransaction ft, Fragment fragment) {
		super.onBeforeCommitReplaceFragment(fm, ft, fragment);
		if (fragment instanceof SessionsFragment) {
			fm.popBackStack();
		} else if (fragment instanceof SessionDetailFragment) {
			if (fm.getBackStackEntryCount() > 0) {
				fm.popBackStack();
			}
			ft.addToBackStack(null);
		}

		updateBreadCrumb();
	}

	public void onBackStackChanged() {
		updateBreadCrumb();
	}

	/**
	 * Handler for the breadcrumb parent.
	 */
	public void onClick(View view) {
		mFragmentManager.popBackStack();
	}

	@SuppressLint({ "NewApi", "NewApi", "NewApi", "NewApi" }) public void updateBreadCrumb() {
		if (UIUtils.isHoneycombTablet(this)) {
			final String title = getString(R.string.title_sessions);
			final String detailTitle = getString(R.string.title_session_detail);

			if (mFragmentManager.getBackStackEntryCount() >= 1) {
				mFragmentBreadCrumbs.setParentTitle(title, title, this);
				mFragmentBreadCrumbs.setTitle(detailTitle, detailTitle);
			} else {
				mFragmentBreadCrumbs.setParentTitle(null, null, null);
				mFragmentBreadCrumbs.setTitle(title, title);
			}
		}
	}

}
