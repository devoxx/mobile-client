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

package net.peterkuterna.android.apps.devoxxsched.util;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {

	private static final String DEVOXXSCHED = "devoxxsched";

	public static SharedPreferences get(Context context) {
		return context.getSharedPreferences(DEVOXXSCHED, Context.MODE_PRIVATE);
	}

	public interface DevoxxPrefs {

		String CFP_LOCAL_VERSION = "cfp_local_version";
		String NEWS_LOCAL_VERSION = "news_local_version";
		String DISPLAYED_BANNER_INDEX = "displayed_banner_index";
		String ACCOUNT_NAME = "account_name";
		String DEVICE_REGISTRATION_ID = "device_registration_id";
		String AUTH_COOKIE = "auth_cookie";

	}

}
