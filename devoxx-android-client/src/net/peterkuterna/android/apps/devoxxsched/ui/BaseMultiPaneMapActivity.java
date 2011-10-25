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

/**
 * A {@link AbstractMapActivity} that can contain multiple panes, and has the
 * ability to substitute fragments for activities when intents are fired using
 * {@link AbstractMapActivity#openActivityOrFragment(android.content.Intent)}.
 */
public abstract class BaseMultiPaneMapActivity extends AbstractMapActivity
		implements MultiPaneActivity {

	@Override
	public FragmentReplaceInfo onSubstituteFragmentForActivityLaunch(
			String activityClassName) {
		return null;
	}

	@Override
	public void onBeforeCommitReplaceFragment(FragmentManager fm,
			FragmentTransaction ft, Fragment fragment) {
	}

	@Override
	public void onAfterCommitReplaceFragment(FragmentManager fm,
			FragmentTransaction ft, Fragment fragment) {
	}

}
