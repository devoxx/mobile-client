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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;

import net.peterkuterna.android.apps.devoxxfrsched.receiver.OnAlarmReceiver;
import net.peterkuterna.android.apps.devoxxfrsched.ui.SettingsActivity;
import net.peterkuterna.android.apps.devoxxfrsched.util.NetworkUtils;
import net.peterkuterna.android.apps.devoxxfrsched.util.ParserUtils;
import net.peterkuterna.android.apps.devoxxfrsched.util.Prefs;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Manager class that sets up alarms, creates {@link PendingIntent}'s, and
 * provided helper methods necessary during syncing operations.
 */
public class CfpSyncManager {

	private static final String TAG = "CfpSyncManager";

	private static final long INTERVAL = AlarmManager.INTERVAL_HOUR;
	private static final String BASE_MD5_URL = "INSERT_YOUR_URL_HERE";

	private final WeakReference<Context> mContext;

	public CfpSyncManager(Context context) {
		this.mContext = new WeakReference<Context>(context);
	}

	/**
	 * Set up the alarm.
	 * 
	 * @param trigger
	 *            when the alarm should first trigger in milliseconds.
	 */
	public void setSyncAlarm(long trigger) {
		Log.d(TAG, "Setting up sync alarm");

		final Context context = getContext();
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		final boolean doBackgroundUpdates = prefs.getBoolean(
				SettingsActivity.KEY_BACKGROUND_UPDATES, true);
		if (doBackgroundUpdates) {
			AlarmManager am = (AlarmManager) context
					.getSystemService(Context.ALARM_SERVICE);
			PendingIntent pi = getAlarmPendingIntent();
			if (pi == null) {
				pi = createAlarmPendingIntent();
			}
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
					SystemClock.elapsedRealtime() + trigger, INTERVAL, pi);
		}
	}

	/**
	 * Cancel a currently installed alarm.
	 */
	public void cancelSyncAlarm() {
		Log.d(TAG, "Cancelling sync alarm");

		final Context context = getContext();
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		PendingIntent localPendingIntent = getAlarmPendingIntent();
		am.cancel(localPendingIntent);
	}

	/**
	 * Checks is a remote sync operation should be performed based on the fact
	 * if we are connected to a network and if we are allowed to perform
	 * background sync operations over mobile networks.
	 * 
	 * @param forceSync
	 *            pass true if you want to force the remote sync
	 * @return
	 */
	public boolean shouldPerformRemoteSync(boolean forceSync) {
		final Context context = getContext();
		final SharedPreferences prefs = Prefs.get(context);
		final boolean isConnectedToNetwork = NetworkUtils
				.isConnectedToNetwork(context);
		final boolean isConnectedToWifi = NetworkUtils
				.isConnectedToWifi(context);
		final boolean onlySyncOnWifi = prefs.getBoolean(
				SettingsActivity.KEY_AUTO_UPDATE_WIFI_ONLY, true);
		return (isConnectedToNetwork && (forceSync || !onlySyncOnWifi || isConnectedToWifi));
	}

	/**
	 * Checks if the remote content was changed by comparing MD5 keys.
	 * 
	 * @param client
	 * @return
	 */
	public Editor hasRemoteContentChanged(HttpClient client) {
		final Context context = getContext();
		final SharedPreferences prefs = Prefs.get(context);
		final String localSpeakersMd5 = getLocalMd5(CfpSyncService.SPEAKERS_URL);
		final String remoteSpeakersMd5 = getRemoteMd5(client,
				CfpSyncService.SPEAKERS_URL);
		final String localSessionsMd5 = getLocalMd5(CfpSyncService.PRESENTATIONS_URL);
		final String remoteSessionsMd5 = getRemoteMd5(client,
				CfpSyncService.PRESENTATIONS_URL);
		final String localScheduleMd5 = getLocalMd5(CfpSyncService.SCHEDULE_URL);
		final String remoteScheduleMd5 = getRemoteMd5(client,
				CfpSyncService.SCHEDULE_URL);
		if (isMd5Equal(localSpeakersMd5, remoteSpeakersMd5)
				&& isMd5Equal(localSessionsMd5, remoteSessionsMd5)
				&& isMd5Equal(localScheduleMd5, remoteScheduleMd5)) {
			return null;
		} else {
			Editor editor = prefs.edit();
			editor.putString(
					ParserUtils.sanitizeId(CfpSyncService.SPEAKERS_URL),
					remoteSpeakersMd5);
			editor.putString(
					ParserUtils.sanitizeId(CfpSyncService.PRESENTATIONS_URL),
					remoteSessionsMd5);
			editor.putString(
					ParserUtils.sanitizeId(CfpSyncService.SCHEDULE_URL),
					remoteScheduleMd5);
			return editor;
		}
	}

	private static boolean isMd5Equal(String md5Local, String md5Remote) {
		return (md5Local != null && md5Local.equals(md5Remote));
	}

	/**
	 * Retreive the currently installed {@link PendingIntent} for the alarm.
	 * 
	 * @return
	 */
	private PendingIntent getAlarmPendingIntent() {
		final Context context = getContext();
		final Intent intent = new Intent(context, OnAlarmReceiver.class);
		return PendingIntent.getBroadcast(context, 0, intent,
				PendingIntent.FLAG_NO_CREATE);
	}

	/**
	 * Create a {@link PendingIntent} for the alarm.
	 * 
	 * @return
	 */
	private PendingIntent createAlarmPendingIntent() {
		final Context context = getContext();
		final Intent intent = new Intent(context, OnAlarmReceiver.class);
		return PendingIntent.getBroadcast(context, 0, intent, 0);
	}

	/**
	 * Retrieve the locally stored MD5 key from the preferences.
	 * 
	 * @param url
	 * @return
	 */
	public String getLocalMd5(String url) {
		final Context context = getContext();
		final SharedPreferences prefs = Prefs.get(context);
		final String sanitizedUrl = ParserUtils.sanitizeId(url);
		return prefs.getString(sanitizedUrl, null);
	}

	/**
	 * Retrieve the remotely stored MD5 key in AppEngine.
	 * 
	 * @param httpClient
	 * @param url
	 * @return
	 */
	private String getRemoteMd5(HttpClient httpClient, String url) {
		try {
			final String requestMd5KeyUrl = BASE_MD5_URL + url;
			final HttpUriRequest request = new HttpGet(requestMd5KeyUrl);
			final HttpResponse resp = httpClient.execute(request);
			final int status = resp.getStatusLine().getStatusCode();
			if (status != HttpStatus.SC_OK) {
				return null;
			}

			final InputStream input = resp.getEntity().getContent();

			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(input));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}
				String md5 = sb.toString().trim();
				if (md5.length() > 0 && !"NOK".equals(md5)) {
					return md5;
				}
			} finally {
				if (input != null)
					input.close();
			}
		} catch (ClientProtocolException e) {
			return null;
		} catch (IOException e) {
			return null;
		}

		return null;
	}

	protected Context getContext() {
		return this.mContext.get();
	}

}
