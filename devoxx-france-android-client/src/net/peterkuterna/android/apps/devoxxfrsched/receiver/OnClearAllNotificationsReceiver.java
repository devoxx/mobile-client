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

package net.peterkuterna.android.apps.devoxxfrsched.receiver;

import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.News;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.Sessions;
import net.peterkuterna.android.apps.devoxxfrsched.util.NotifierManager;
import net.peterkuterna.android.apps.devoxxfrsched.util.NotifyingAsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

/**
 * {@link BroadcastReceiver} that is called when the user has pressed 'Clear
 * all' in the notification drop down. updates. Perform the actions that need to
 * occur to clear all of our notifications.
 */
public class OnClearAllNotificationsReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		final NotifierManager notifierHelper = new NotifierManager(context);
		final NotifyingAsyncQueryHandler handler = new NotifyingAsyncQueryHandler(
				context.getContentResolver(), null);

		final ContentValues sessionValues = new ContentValues();
		sessionValues.put(Sessions.SESSION_NEW, 0);
		handler.startUpdate(Sessions.CONTENT_NEW_URI, sessionValues);

		final ContentValues newsValues = new ContentValues();
		newsValues.put(News.NEWS_NEW, 0);
		handler.startUpdate(News.CONTENT_NEW_URI, newsValues);

		notifierHelper.clearNotifications();
	}

}
