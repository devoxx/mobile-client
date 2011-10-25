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

package net.peterkuterna.android.apps.devoxxsched.receiver;

import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.News;
import net.peterkuterna.android.apps.devoxxsched.ui.BulletinActivity;
import net.peterkuterna.android.apps.devoxxsched.util.NotifierManager;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

/**
 * {@link BroadcastReceiver} that is called when the user has pressed the
 * notification of new Devoxx news items.
 */
public class OnNotifyNewNewsItemsReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		final NotifierManager notifierHelper = new NotifierManager(context);
		final NotifyingAsyncQueryHandler handler = new NotifyingAsyncQueryHandler(
				context.getContentResolver(), null);

		final ContentValues values = new ContentValues();
		values.put(News.NEWS_NEW, 0);
		handler.startUpdate(News.CONTENT_NEW_URI, values);

		notifierHelper.clearNotifications();

		final Intent i = new Intent(context, BulletinActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(i);
	}
}
