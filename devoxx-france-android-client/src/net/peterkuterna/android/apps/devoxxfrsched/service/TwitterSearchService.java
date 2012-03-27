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

package net.peterkuterna.android.apps.devoxxfrsched.service;

import net.peterkuterna.android.apps.devoxxfrsched.io.RemoteExecutor;
import net.peterkuterna.android.apps.devoxxfrsched.io.TwitterSearchHandler;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.Tweets;
import net.peterkuterna.android.apps.devoxxfrsched.util.HttpUtils;
import net.peterkuterna.android.apps.devoxxfrsched.util.TwitterApiUriUtils;

import org.apache.http.client.HttpClient;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * Background {@link Service} that performs twitter search operations through
 * the Twitter search API.
 */
public class TwitterSearchService extends AbstractSyncService {

	protected static final String TAG = "TwitterSearchService";

	private RemoteExecutor mRemoteExecutor;
	private HttpClient mHttpClient;
	private ContentResolver mResolver;

	public TwitterSearchService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mHttpClient = HttpUtils.getHttpClient(this);
		mResolver = getContentResolver();

		mRemoteExecutor = new RemoteExecutor(mHttpClient, mResolver);
	}

	@Override
	protected void doSync(Intent intent) throws Exception {
		Log.d(TAG, "Refreshing twitter results");

		final Cursor c = mResolver.query(Tweets.CONTENT_URI,
				TweetsQuery.PROJECTION, null, null, null);

		String tweetId = "0";
		try {
			if (c.moveToFirst()) {
				tweetId = c.getString(TweetsQuery.MAX_TWEET_ID);
			}
		} finally {
			c.close();
		}

		final Uri uri = TwitterApiUriUtils.buildTwitterSearchUri(
				"#devoxxfr +exclude:retweets", tweetId);

		mRemoteExecutor.executeGet(uri.toString(), new TwitterSearchHandler());
	}

	/**
	 * {@link Tweets} query parameters.
	 */
	private interface TweetsQuery {

		String PROJECTION[] = { "MAX(" + CfpContract.Tweets.TWEET_ID
				+ ") AS max_tweet_id" };

		int MAX_TWEET_ID = 0;

	}

}
