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

package net.peterkuterna.android.apps.devoxxsched.util;

import java.util.List;

import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.ui.HomeActivity;
import net.peterkuterna.android.apps.devoxxsched.ui.MultiPaneActivity;
import net.peterkuterna.android.apps.devoxxsched.ui.MultiPaneActivity.FragmentReplaceInfo;
import net.peterkuterna.android.apps.devoxxsched.ui.SinglePaneActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.SupportActivity;
import android.view.View;

public class ActivityHelper {

	protected SupportActivity mActivity;

	public static ActivityHelper createInstance(SupportActivity activity) {
		return new ActivityHelper(activity);
	}

	protected ActivityHelper(SupportActivity activity) {
		mActivity = activity;
	}

	/**
	 * Construct the single pane layout.
	 * 
	 * @param savedInstanceState
	 */
	public void onCreate(Bundle savedInstanceState) {
		if (mActivity instanceof SinglePaneActivity) {
			SinglePaneActivity singlePaneActivity = (SinglePaneActivity) mActivity;
			singlePaneActivity
					.setContentView(R.layout.activity_singlepane_empty);

			if (savedInstanceState == null) {
				Fragment fragment = singlePaneActivity.onCreatePane();
				singlePaneActivity.setFragment(fragment);
				fragment.setArguments(intentToFragmentArguments(singlePaneActivity
						.getIntent()));

				singlePaneActivity.getSupportFragmentManager()
						.beginTransaction().add(R.id.root_container, fragment)
						.commit();
			}
		}
	}
	
	public void setupActionBar() {
	}

	public void goHome() {
		if (mActivity instanceof HomeActivity) {
			return;
		}

		final Intent intent = new Intent(mActivity.asActivity(),
				HomeActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		mActivity.startActivity(intent);
		mActivity.asActivity().overridePendingTransition(R.anim.home_enter,
				R.anim.home_exit);
	}

	/**
	 * Invoke "search" action, triggering a default search.
	 */
	public void goSearch() {
		mActivity.startSearch(null, false, Bundle.EMPTY, false);
	}

	/**
	 * Sets the action bar color to the given color.
	 */
	public void setActionBarColor(int color) {
		if (color == 0) {
			return;
		}

		final View colorstrip = mActivity.findViewById(R.id.colorstrip);
		if (colorstrip == null) {
			return;
		}

		colorstrip.setBackgroundColor(color);
	}

	/**
	 * Takes a given intent and either starts a new activity to handle it (the
	 * default behavior), or creates/updates a fragment (in the case of a
	 * multi-pane activity) that can handle the intent.
	 * 
	 * Must be called from the main (UI) thread.
	 */
	public void openActivityOrFragment(Intent intent) {
		if (mActivity instanceof MultiPaneActivity) {
			MultiPaneActivity multiPaneActivity = (MultiPaneActivity) mActivity;
			final PackageManager pm = multiPaneActivity.getPackageManager();
			List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(
					intent, PackageManager.MATCH_DEFAULT_ONLY);
			for (ResolveInfo resolveInfo : resolveInfoList) {
				final FragmentReplaceInfo fri = multiPaneActivity
						.onSubstituteFragmentForActivityLaunch(resolveInfo.activityInfo.name);
				if (fri != null) {
					final Bundle arguments = intentToFragmentArguments(intent);
					final FragmentManager fm = multiPaneActivity
							.getSupportFragmentManager();

					try {
						Fragment fragment = (Fragment) fri.getFragmentClass()
								.newInstance();
						fragment.setArguments(arguments);

						FragmentTransaction ft = fm.beginTransaction();
						ft.replace(fri.getContainerId(), fragment,
								fri.getFragmentTag());
						multiPaneActivity.onBeforeCommitReplaceFragment(fm, ft,
								fragment);
						ft.commit();
						multiPaneActivity.onAfterCommitReplaceFragment(fm, ft,
								fragment);
					} catch (InstantiationException e) {
						throw new IllegalStateException(
								"Error creating new fragment.", e);
					} catch (IllegalAccessException e) {
						throw new IllegalStateException(
								"Error creating new fragment.", e);
					}
					return;
				}
			}
		}
		mActivity.startActivity(intent);

	}

	/**
	 * Converts an intent into a {@link Bundle} suitable for use as fragment
	 * arguments.
	 */
	public static Bundle intentToFragmentArguments(Intent intent) {
		Bundle arguments = new Bundle();
		if (intent == null) {
			return arguments;
		}

		final Uri data = intent.getData();
		if (data != null) {
			arguments.putParcelable("_uri", data);
		}

		final Bundle extras = intent.getExtras();
		if (extras != null) {
			arguments.putAll(intent.getExtras());
		}

		return arguments;
	}

	/**
	 * Converts a fragment arguments bundle into an intent.
	 */
	public static Intent fragmentArgumentsToIntent(Bundle arguments) {
		Intent intent = new Intent();
		if (arguments == null) {
			return intent;
		}

		final Uri data = arguments.getParcelable("_uri");
		if (data != null) {
			intent.setData(data);
		}

		intent.putExtras(arguments);
		intent.removeExtra("_uri");
		return intent;
	}

}
