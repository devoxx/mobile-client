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
 * Various utility methods use to build Twitter search {@link Uri}'s.
 */
public class TwitterApiUriUtils {

	private static final String URI_SCHEME = "http";
	private static final String TWITTER_HOST = "twitter.com";
	private static final String TWITTER_SEARCH_HOST = "search.twitter.com";

	private static final String PATH_STATUS = "status";
	private static final String PATH_SEARCH = "search.json";

	private static final String PARAMETER_QUERY = "q";
	private static final String PARAMETER_RESULT_TYPE = "result_type";
	private static final String PARAMETER_RPP = "rpp";
	private static final String PARAMETER_SINCE_ID = "since_id";

	public static final Uri buildTwitterSearchUri(final String query,
			final String since_id) {
		return buildTwitterSearchBaseUri()
				.appendQueryParameter(PARAMETER_QUERY, query)
				.appendQueryParameter(PARAMETER_RESULT_TYPE, "mixed")
				.appendQueryParameter(PARAMETER_SINCE_ID, since_id)
				.appendQueryParameter(PARAMETER_RPP, "100").build();
	}

	public static final Uri buildTwitterSearchUri(final String query,
			final long sinceId) {
		return buildTwitterSearchBaseUri()
				.appendQueryParameter(PARAMETER_QUERY, query)
				.appendQueryParameter(PARAMETER_RESULT_TYPE, "mixed")
				.appendQueryParameter(PARAMETER_RPP, "100")
				.appendQueryParameter(PARAMETER_SINCE_ID,
						String.valueOf(sinceId)).build();
	}

	public static final Uri buildTwitterTweetUri(final String user,
			final String tweetId) {
		return buildTwitterBaseUri().appendPath(user).appendPath(PATH_STATUS)
				.appendPath(tweetId).build();
	}

	private static final Uri.Builder buildTwitterBaseUri() {
		return new Uri.Builder().scheme(URI_SCHEME).authority(TWITTER_HOST);
	}

	private static final Uri.Builder buildTwitterSearchBaseUri() {
		return new Uri.Builder().scheme(URI_SCHEME)
				.authority(TWITTER_SEARCH_HOST).appendPath(PATH_SEARCH);
	}

}
