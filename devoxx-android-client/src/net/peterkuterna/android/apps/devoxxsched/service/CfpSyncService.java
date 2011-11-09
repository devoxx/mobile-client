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

package net.peterkuterna.android.apps.devoxxsched.service;

import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.io.LocalExecutor;
import net.peterkuterna.android.apps.devoxxsched.io.ParleysPresentationsHandler;
import net.peterkuterna.android.apps.devoxxsched.io.RemoteExecutor;
import net.peterkuterna.android.apps.devoxxsched.io.RoomsHandler;
import net.peterkuterna.android.apps.devoxxsched.io.ScheduleHandler;
import net.peterkuterna.android.apps.devoxxsched.io.SearchSuggestHandler;
import net.peterkuterna.android.apps.devoxxsched.io.SessionTypesHandler;
import net.peterkuterna.android.apps.devoxxsched.io.SessionsHandler;
import net.peterkuterna.android.apps.devoxxsched.io.SpeakersHandler;
import net.peterkuterna.android.apps.devoxxsched.io.TracksHandler;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Sessions;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpDatabase;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpProvider;
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
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.util.Log;

/**
 * Background {@link Service} that synchronizes data living in
 * {@link CfpProvider}. Reads data from both local {@link Resources} and from
 * remote sources (CFP REST interface).
 */
public class CfpSyncService extends AbstractSyncService {

	protected static final String TAG = "CfpSyncService";

	public static final String BASE_URL = "https://cfp.devoxx.com/rest/v1/events/4/";
	public static final String SCHEDULE_URL = BASE_URL + "schedule/";
	public static final String PRESENTATIONS_URL = BASE_URL + "presentations/";
	public static final String SPEAKERS_URL = BASE_URL + "speakers/";

	public static final int NOTIFICATION_NEW_SESSIONS = 1;

	private static final int VERSION_NONE = 0;
	private static final int VERSION_CURRENT = 2;

	private LocalExecutor mLocalExecutor;
	private RemoteExecutor mRemoteExecutor;
	private HttpClient mHttpClient;
	private ContentResolver mResolver;

	public CfpSyncService() {
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
		Log.d(TAG, "Start sync");
		
		Thread.sleep(5000);

		final Context context = this;

		final SharedPreferences settings = Prefs.get(context);
		final int localVersion = settings.getInt(DevoxxPrefs.CFP_LOCAL_VERSION,
				VERSION_NONE);

		final long startLocal = System.currentTimeMillis();
		final boolean localParse = localVersion < VERSION_CURRENT;
		Log.d(TAG, "found localVersion=" + localVersion
				+ " and VERSION_CURRENT=" + VERSION_CURRENT);
		if (localParse) {
			// Parse values from local cache first
			mLocalExecutor.execute(R.xml.search_suggest,
					new SearchSuggestHandler());
			mLocalExecutor.execute(R.xml.rooms, new RoomsHandler());
			mLocalExecutor.execute(R.xml.tracks, new TracksHandler());
			mLocalExecutor.execute(R.xml.presentationtypes,
					new SessionTypesHandler());
			mLocalExecutor.execute(context, "cache-speakers.json",
					new SpeakersHandler());
			mLocalExecutor.execute(context, "cache-presentations.json",
					new SessionsHandler());
			mLocalExecutor.execute(context, "cache-schedule.json",
					new ScheduleHandler());

			mLocalExecutor.execute(context, "cache-parleys-presentations.json",
					new ParleysPresentationsHandler());

			// Save local parsed version
			settings.edit()
					.putInt(DevoxxPrefs.CFP_LOCAL_VERSION, VERSION_CURRENT)
					.commit();

			final ContentValues values = new ContentValues();
			values.put(Sessions.SESSION_NEW, 0);
			getContentResolver().update(Sessions.CONTENT_NEW_URI, values, null,
					null);
		}
		Log.d(TAG, "Local sync took "
				+ (System.currentTimeMillis() - startLocal) + "ms");

		final CfpSyncManager syncManager = new CfpSyncManager(context);
		if (syncManager.shouldPerformRemoteSync(Intent.ACTION_SYNC
				.equals(intent.getAction()))) {
			Log.d(TAG, "Should perform remote sync");
			final long startRemote = System.currentTimeMillis();

			Editor prefsEditor = syncManager
					.hasRemoteContentChanged(mHttpClient);
			if (prefsEditor != null) {
				Log.d(TAG, "Remote content was changed");
				mRemoteExecutor.executeGet(SPEAKERS_URL, new SpeakersHandler());
				mRemoteExecutor.executeGet(PRESENTATIONS_URL,
						new SessionsHandler());
				mRemoteExecutor.executeGet(SCHEDULE_URL, new ScheduleHandler());
				prefsEditor.commit();
			}
			Log.d(TAG, "Remote sync took "
					+ (System.currentTimeMillis() - startRemote) + "ms");
		} else {
			Log.d(TAG, "Should not perform remote sync");
		}

		final CfpDatabase database = new CfpDatabase(this);
		database.cleanupLinkTables();

		final NotifierManager notifierManager = new NotifierManager(this);
		notifierManager.notifyNewSessions();

		Log.d(TAG, "Sync finished");
	}

}
