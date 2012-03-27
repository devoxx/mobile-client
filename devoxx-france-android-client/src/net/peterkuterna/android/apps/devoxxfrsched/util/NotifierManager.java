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

package net.peterkuterna.android.apps.devoxxfrsched.util;

import java.lang.ref.WeakReference;

import net.peterkuterna.android.apps.devoxxfrsched.R;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.News;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.Sessions;
import net.peterkuterna.android.apps.devoxxfrsched.receiver.OnClearAllNotificationsReceiver;
import net.peterkuterna.android.apps.devoxxfrsched.receiver.OnNotifyNewNewsItemsReceiver;
import net.peterkuterna.android.apps.devoxxfrsched.receiver.OnNotifyNewSessionsReceiver;
import net.peterkuterna.android.apps.devoxxfrsched.ui.NewSessionsActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

/**
 * Manager to interact with the {@link NotificationManager}.
 */
public class NotifierManager {

	private static final int NEW_SESSIONS_NOTIFICATION_ID = 1;
	private static final int NEW_NEWS_NOTIFICATION_ID = 2;

	private final WeakReference<Context> mContext;

	public NotifierManager(Context context) {
		this.mContext = new WeakReference<Context>(context);
	}

	public void clearNotifications() {
		final Context context = getContext();
		NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancelAll();
	}

	public void clearNewNewsNotification() {
		final Context context = getContext();
		NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(NEW_NEWS_NOTIFICATION_ID);
	}

	public void notifyNewSessions() {
		final Context context = getContext();
		final NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		final ContentResolver resolver = context.getContentResolver();
		final int newSessions = getNewSessionCount(resolver);

		if (newSessions > 0) {
			final long when = System.currentTimeMillis();
			final String notificationText = context.getResources().getString(
					R.string.new_sessions_notification_text);
			Notification notification = new Notification(
					R.drawable.ic_stat_devoxx, notificationText, when);
			notification.setLatestEventInfo(context, notificationText,
					newSessions + " in total.", null);
			PendingIntent contentIntent = createNotifyNewSessionsPendingIntent();
			PendingIntent deleteIntent = createClearAllNotificationsPendingIntent();
			notification.contentIntent = contentIntent;
			notification.deleteIntent = deleteIntent;
			notificationManager.notify(NEW_SESSIONS_NOTIFICATION_ID,
					notification);
		}
	}

	public void notifyNewNewsItems() {
		final Context context = getContext();
		final NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		final ContentResolver resolver = context.getContentResolver();
		final int newNewsItems = getNewNewsCount(resolver);

		if (newNewsItems > 0) {
			final long when = System.currentTimeMillis();
			final String notificationText = context.getResources().getString(
					R.string.new_news_items_notification_text);
			Notification notification = new Notification(
					R.drawable.ic_stat_devoxx, notificationText, when);
			notification.setLatestEventInfo(context, notificationText,
					newNewsItems + " in total.", null);
			PendingIntent contentIntent = createNotifyNewNewsItemsPendingIntent();
			PendingIntent deleteIntent = createClearAllNotificationsPendingIntent();
			notification.contentIntent = contentIntent;
			notification.deleteIntent = deleteIntent;
			notificationManager.notify(NEW_NEWS_NOTIFICATION_ID, notification);
		}
	}

	private PendingIntent createClearAllNotificationsPendingIntent() {
		final Context context = getContext();
		final Intent intent = new Intent(context,
				OnClearAllNotificationsReceiver.class);
		return PendingIntent
				.getBroadcast(context, 0, intent,
						PendingIntent.FLAG_CANCEL_CURRENT
								| PendingIntent.FLAG_ONE_SHOT);
	}

	private PendingIntent createNotifyNewSessionsPendingIntent() {
		final Context context = getContext();
		final ContentResolver resolver = context.getContentResolver();
		final ContentValues values = new ContentValues();
		final long timestamp = System.currentTimeMillis();
		values.put(Sessions.SESSION_NEW_TIMESTAMP, timestamp);
		resolver.update(Sessions.CONTENT_NEW_URI, values, null, null);
		final Intent intent = new Intent(context,
				OnNotifyNewSessionsReceiver.class);
		intent.putExtra(NewSessionsActivity.EXTRA_NEW_SESSION_TIMESTAMP,
				timestamp);
		return PendingIntent
				.getBroadcast(context, 0, intent,
						PendingIntent.FLAG_CANCEL_CURRENT
								| PendingIntent.FLAG_ONE_SHOT);
	}

	private PendingIntent createNotifyNewNewsItemsPendingIntent() {
		final Context context = getContext();
		final Intent intent = new Intent(context,
				OnNotifyNewNewsItemsReceiver.class);
		return PendingIntent
				.getBroadcast(context, 0, intent,
						PendingIntent.FLAG_CANCEL_CURRENT
								| PendingIntent.FLAG_ONE_SHOT);
	}

	protected Context getContext() {
		return this.mContext.get();
	}

	private static int getNewSessionCount(ContentResolver resolver) {
		return getCount(resolver, Sessions.CONTENT_NEW_URI,
				SessionsQuery.PROJECTION);
	}

	private static int getNewNewsCount(ContentResolver resolver) {
		return getCount(resolver, News.CONTENT_NEW_URI, NewsQuery.PROJECTION);
	}

	private static int getCount(ContentResolver resolver, Uri uri,
			String[] projection) {
		final Cursor cursor = resolver.query(uri, projection, null, null, null);
		try {
			if (!cursor.moveToFirst())
				return 0;
			return cursor.getCount();
		} finally {
			cursor.close();
		}
	}

	/**
	 * {@link Sessions} query parameters.
	 */
	private interface SessionsQuery {
		String[] PROJECTION = { Sessions.SESSION_ID, };

		int SESSION_ID = 0;
	}

	/**
	 * {@link News} query parameters.
	 */
	private interface NewsQuery {
		String[] PROJECTION = { News.NEWS_ID, };

		int NEWS_ID = 0;
	}

}
