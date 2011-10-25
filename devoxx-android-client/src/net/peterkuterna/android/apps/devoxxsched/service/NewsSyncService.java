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

package net.peterkuterna.android.apps.devoxxsched.service;

import net.peterkuterna.android.apps.devoxxsched.io.LocalExecutor;
import net.peterkuterna.android.apps.devoxxsched.io.NewsHandler;
import net.peterkuterna.android.apps.devoxxsched.io.RemoteExecutor;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.News;
import net.peterkuterna.android.apps.devoxxsched.util.HttpUtils;
import net.peterkuterna.android.apps.devoxxsched.util.NotifierManager;
import net.peterkuterna.android.apps.devoxxsched.util.Prefs;
import net.peterkuterna.android.apps.devoxxsched.util.Prefs.DevoxxPrefs;

import org.apache.http.client.HttpClient;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Background {@link Service} that synchronizes the Devoxx {@link News} items.
 */
public class NewsSyncService extends AbstractSyncService {

	protected static final String TAG = "NewsSyncService";

	public static final String NEWS_URL = "https://www.devoxx.com/download/attachments/4751370/news.json";

	private static final int VERSION_NONE = 0;
	private static final int VERSION_CURRENT = 1;

	private LocalExecutor mLocalExecutor;
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

		mLocalExecutor = new LocalExecutor(getResources(), mResolver);
		mRemoteExecutor = new RemoteExecutor(mHttpClient, mResolver);
	}

	@Override
	protected void doSync(Intent intent) throws Exception {
		Log.d(TAG, "Start news sync");

		final Context context = this;

		final SharedPreferences settings = Prefs.get(context);
		final int localVersion = settings.getInt(
				DevoxxPrefs.NEWS_LOCAL_VERSION, VERSION_NONE);

		final long startLocal = System.currentTimeMillis();
		final boolean localParse = localVersion < VERSION_CURRENT;
		Log.d(TAG, "found localVersion=" + localVersion
				+ " and VERSION_CURRENT=" + VERSION_CURRENT);
		if (localParse) {
			mLocalExecutor.execute(context, "cache-news.json",
					new NewsHandler());

			// Save local parsed version
			settings.edit()
					.putInt(DevoxxPrefs.NEWS_LOCAL_VERSION, VERSION_CURRENT)
					.commit();

			final ContentValues values = new ContentValues();
			values.put(News.NEWS_NEW, 0);
			getContentResolver().update(News.CONTENT_NEW_URI, values, null,
					null);
		}
		Log.d(TAG, "Local sync took "
				+ (System.currentTimeMillis() - startLocal) + "ms");

		final CfpSyncManager syncManager = new CfpSyncManager(context);
		if (syncManager.shouldPerformRemoteSync(Intent.ACTION_SYNC
				.equals(intent.getAction()))) {
			Log.d(TAG, "Should perform remote sync");
			final long startRemote = System.currentTimeMillis();
			mRemoteExecutor.executeGet(NEWS_URL, new NewsHandler());
			Log.d(TAG, "Remote sync took "
					+ (System.currentTimeMillis() - startRemote) + "ms");
		} else {
			Log.d(TAG, "Should not perform remote sync");
		}

		final NotifierManager notifierManager = new NotifierManager(this);
		notifierManager.notifyNewNewsItems();

		Log.d(TAG, "News sync finished");
	}

}
