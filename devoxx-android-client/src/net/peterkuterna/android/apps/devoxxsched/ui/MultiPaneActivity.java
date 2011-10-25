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

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public interface MultiPaneActivity extends BaseActivity {

	/**
	 * Callback that's triggered to find out if a fragment can substitute the
	 * given activity class. Base activites should return a
	 * {@link FragmentReplaceInfo} if a fragment can act in place of the given
	 * activity class name.
	 */
	FragmentReplaceInfo onSubstituteFragmentForActivityLaunch(
			String activityClassName);

	/**
	 * Called just before a fragment replacement transaction is committed in
	 * response to an intent being fired and substituted for a fragment.
	 */
	void onBeforeCommitReplaceFragment(FragmentManager fm,
			FragmentTransaction ft, Fragment fragment);

	/**
	 * Called just after a fragment replacement transaction is committed in
	 * response to an intent being fired and substituted for a fragment.
	 */
	void onAfterCommitReplaceFragment(FragmentManager fm,
			FragmentTransaction ft, Fragment fragment);

	/**
	 * A class describing information for a fragment-substitution, used when a
	 * fragment can act in place of an activity.
	 */
	public class FragmentReplaceInfo {
		private Class mFragmentClass;
		private String mFragmentTag;
		private int mContainerId;

		public FragmentReplaceInfo(Class fragmentClass, String fragmentTag,
				int containerId) {
			mFragmentClass = fragmentClass;
			mFragmentTag = fragmentTag;
			mContainerId = containerId;
		}

		public Class getFragmentClass() {
			return mFragmentClass;
		}

		public String getFragmentTag() {
			return mFragmentTag;
		}

		public int getContainerId() {
			return mContainerId;
		}
	}

}
