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

import android.net.Uri;

/**
 * Various utility methods use to build Parleys.com {@link Uri}'s.
 */
public class ParleysUriUtils {

	private static final String URI_SCHEME = "http";
	private static final String PARLEYS_HOST = "parleys.com";

	private static final String PARAMETER_HASH_ST = "#st";
	private static final String PARAMETER_ID = "id";

	public static final Uri buildParleysViewUri(final String id) {
		final StringBuilder sb = new StringBuilder();
		sb.append(PARAMETER_HASH_ST);
		sb.append("=5&");
		sb.append(PARAMETER_ID);
		sb.append("=");
		sb.append(id);
		return buildParleysBaseUri().appendEncodedPath(sb.toString()).build();
	}

	private static final Uri.Builder buildParleysBaseUri() {
		return new Uri.Builder().scheme(URI_SCHEME).authority(PARLEYS_HOST);
	}

}
