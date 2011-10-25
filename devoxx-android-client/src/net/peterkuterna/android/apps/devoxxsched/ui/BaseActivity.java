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

import net.peterkuterna.android.apps.devoxxsched.util.ActivityHelper;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.SupportActivity;

public interface BaseActivity extends SupportActivity {

	ActivityHelper getActivityHelper();

	void openActivityOrFragment(Intent intent);

	Bundle intentToFragmentArguments(Intent intent);

	Intent fragmentArgumentsToIntent(Bundle arguments);

}
