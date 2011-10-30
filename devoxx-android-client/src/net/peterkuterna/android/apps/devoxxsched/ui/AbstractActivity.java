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
import net.peterkuterna.android.apps.devoxxsched.util.ActionBarHelper;
import net.peterkuterna.android.apps.devoxxsched.util.ActivityHelper;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public abstract class AbstractActivity extends FragmentActivity implements
		BaseActivity {

	final ActionBarHelper mActionBarHelper = ActionBarHelper
			.createInstance(this);
	final ActivityHelper mActivityHelper = ActivityHelper.createInstance(this);

	/**
	 * Returns the {@link ActionBarHelper} object associated with this activity.
	 */
	public ActionBarHelper getActionBarHelper() {
		return mActionBarHelper;
	}

	/**
	 * Returns the {@link ActivityHelper} object associated with this activity.
	 */
	public ActivityHelper getActivityHelper() {
		return mActivityHelper;
	}

	@Override
	public MenuInflater getMenuInflater() {
		return mActionBarHelper.getMenuInflater(super.getMenuInflater());
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mActionBarHelper.onCreate(savedInstanceState);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mActionBarHelper.onPostCreate(savedInstanceState);
	}

	/**
	 * Base action bar-aware implementation for
	 * {@link Activity#onCreateOptionsMenu(android.view.Menu)}.
	 * 
	 * Note: marking menu items as invisible/visible is not currently supported.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.default_menu_items, menu);
		boolean retValue = false;
		retValue |= mActionBarHelper.onCreateOptionsMenu(menu);
		retValue |= super.onCreateOptionsMenu(menu);
		return retValue;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			mActivityHelper.goHome();
			return true;
		case R.id.menu_search:
			mActivityHelper.goSearch();
			return true;
		default:
			return false;
		}
	}

	@Override
	protected void onTitleChanged(CharSequence title, int color) {
		mActionBarHelper.onTitleChanged(title, color);
		super.onTitleChanged(title, color);
	}

	@Override
	public void openActivityOrFragment(Intent intent) {
		mActivityHelper.openActivityOrFragment(intent);
	}

	@Override
	public Bundle intentToFragmentArguments(Intent intent) {
		return ActivityHelper.intentToFragmentArguments(intent);
	}

	@Override
	public Intent fragmentArgumentsToIntent(Bundle arguments) {
		return ActivityHelper.fragmentArgumentsToIntent(arguments);
	}

}
