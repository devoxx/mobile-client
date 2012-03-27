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

import net.peterkuterna.android.apps.devoxxfrsched.io.NewsHandler;
import net.peterkuterna.android.apps.devoxxfrsched.io.RemoteExecutor;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.News;
import net.peterkuterna.android.apps.devoxxfrsched.util.HttpUtils;
import net.peterkuterna.android.apps.devoxxfrsched.util.NotifierManager;
import net.peterkuterna.android.apps.devoxxfrsched.util.TwitterApiUriUtils;

import org.apache.http.client.HttpClient;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * Background {@link Service} that synchronizes the Devoxx {@link News} items.
 */
public class NewsSyncService extends AbstractSyncService {

	protected static final String TAG = "NewsSyncService";

	public static final String EXTRA_NO_NOTIFICATIONS = "net.peterkuterna.android.apps.devoxxfrsched.extra.NO_NOTIFICATIONS";

	private RemoteExecutor mRemoteExecutor;
	private HttpClient mHttpClient;
	private ContentResolver mResolver;

	public NewsSyncService() {
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
		Log.d(TAG, "Refreshing DevoxxFr twitter results");
		
		boolean noNotifications = intent.getBooleanExtra(EXTRA_NO_NOTIFICATIONS, false);

		final Cursor c = mResolver.query(News.CONTENT_URI,
				NewsQuery.PROJECTION, null, null, null);

		String newsId = "0";
		try {
			if (c.moveToFirst()) {
				newsId = c.getString(NewsQuery.MAX_NEWS_ID);
			}
		} finally {
			c.close();
		}

		final Uri uri = TwitterApiUriUtils.buildTwitterSearchUri(
				"from:DevoxxFr +exclude:retweets", newsId);

		mRemoteExecutor.executeGet(uri.toString(), new NewsHandler(noNotifications));

		final NotifierManager notifierManager = new NotifierManager(this);
		notifierManager.notifyNewNewsItems();

		Log.d(TAG, "News sync finished");
	}

	/**
	 * {@link News} query parameters.
	 */
	private interface NewsQuery {

		String PROJECTION[] = { "MAX(" + CfpContract.News.NEWS_ID
				+ ") AS max_news_id" };

		int MAX_NEWS_ID = 0;

	}
	
}
