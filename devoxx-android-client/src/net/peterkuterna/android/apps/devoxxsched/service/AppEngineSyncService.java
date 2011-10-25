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

import java.util.ArrayList;
import java.util.List;

import net.peterkuterna.android.apps.devoxxsched.c2dm.Util;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Sessions;
import net.peterkuterna.android.apps.devoxxsched.util.Lists;
import net.peterkuterna.android.apps.devoxxsched.util.Prefs;
import net.peterkuterna.android.apps.devoxxsched.util.Prefs.DevoxxPrefs;
import net.peterkuterna.appengine.apps.devoxxsched.shared.DevoxxRequest;
import net.peterkuterna.appengine.apps.devoxxsched.shared.DevoxxRequestFactory;
import net.peterkuterna.appengine.apps.devoxxsched.shared.SessionProxy;
import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.util.Log;

import com.google.web.bindery.requestfactory.shared.Receiver;

/**
 * {@link IntentService} that performs the sync operation with the AppEngine
 * part. It will send pending starring/unstarring operations and retrieve the
 * starred session id's back from AppEngine.
 */
public class AppEngineSyncService extends AbstractSyncService {

	protected static final String TAG = "AppEngineSyncService";

	public static final String EXTRA_INITIAL_SYNC = "net.peterkuterna.android.apps.devoxxsched.extra.INITIAL_SYNC";

	private ContentResolver mResolver;

	public AppEngineSyncService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mResolver = getContentResolver();
	}

	@Override
	protected void doSync(Intent intent) throws Exception {
		Log.d(TAG, "Start session sync");

		final Context context = this;
		final SharedPreferences prefs = Prefs.get(context);
		String deviceRegistrationID = prefs.getString(
				DevoxxPrefs.DEVICE_REGISTRATION_ID, null);
		if (deviceRegistrationID != null) {
			Cursor c = null;
			if (intent.hasExtra(EXTRA_INITIAL_SYNC)) {
				c = mResolver.query(Sessions.CONTENT_STARRED_URI,
						SessionsQuery.PROJECTION, null, null, null);
			} else {
				c = mResolver.query(Sessions.CONTENT_URI,
						SessionsQuery.PROJECTION,
						Sessions.SESSION_OPERATION_PENDING + "=1", null, null);
			}

			DevoxxRequestFactory factory = (DevoxxRequestFactory) Util
					.getRequestFactory(context, DevoxxRequestFactory.class);
			DevoxxRequest request = factory.devoxxRequest();

			final ArrayList<SessionProxy> toStar = Lists.newArrayList();
			final ArrayList<SessionProxy> toUnstar = Lists.newArrayList();

			try {
				while (c.moveToNext()) {
					final String sessionId = c
							.getString(SessionsQuery.SESSION_ID);
					final int isStarred = c
							.getInt(SessionsQuery.SESSION_STARRED);
					final SessionProxy session = request
							.create(SessionProxy.class);
					session.setSessionId(sessionId);
					if (isStarred == 1) {
						toStar.add(session);
					} else {
						toUnstar.add(session);
					}
				}
			} finally {
				c.close();
			}

			Log.d(TAG, "Going to star " + toStar.size()
					+ " sessions and unstar " + toUnstar.size() + " sessions.");
			request.sync(toStar, toUnstar).fire(
					new Receiver<List<SessionProxy>>() {
						@Override
						public void onSuccess(List<SessionProxy> sessions) {
							if (sessions != null) {
								Log.d(TAG, "Received " + sessions.size()
										+ " starred sessions in response");
								ArrayList<ContentProviderOperation> batch = Lists
										.newArrayList();
								batch.add(ContentProviderOperation
										.newUpdate(Sessions.CONTENT_STARRED_URI)
										.withValue(Sessions.SESSION_STARRED, 0)
										.build());
								for (SessionProxy session : sessions) {
									final Uri sessionUri = Sessions
											.buildSessionUri(session
													.getSessionId());
									batch.add(ContentProviderOperation
											.newUpdate(sessionUri)
											.withValue(
													Sessions.SESSION_STARRED, 1)
											.build());
								}
								batch.add(ContentProviderOperation
										.newUpdate(Sessions.CONTENT_URI)
										.withValue(
												Sessions.SESSION_OPERATION_PENDING,
												0).build());
								try {
									mResolver.applyBatch(
											CfpContract.CONTENT_AUTHORITY,
											batch);
								} catch (RemoteException e) {
									Log.w(TAG, e.getMessage());
								} catch (OperationApplicationException e) {
									Log.w(TAG, e.getMessage());
								}
							}
						}
					});
		}

		Log.d(TAG, "Session sync finished");
	}

	/**
	 * {@link Sessions} query parameters.
	 */
	private interface SessionsQuery {

		String PROJECTION[] = { BaseColumns._ID,
				CfpContract.Sessions.SESSION_ID,
				CfpContract.Sessions.SESSION_STARRED };

		int _ID = 0;
		int SESSION_ID = 1;
		int SESSION_STARRED = 2;

	}

}
