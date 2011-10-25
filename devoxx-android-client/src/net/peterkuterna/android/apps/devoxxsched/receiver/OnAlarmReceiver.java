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

import net.peterkuterna.android.apps.devoxxsched.service.AppEngineSyncService;
import net.peterkuterna.android.apps.devoxxsched.service.CfpSyncService;
import net.peterkuterna.android.apps.devoxxsched.service.NewsSyncService;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * {@link BroadcastReceiver} that is called when the installed alarm goes of.
 * Trigger various {@link IntentService} implementations to perform background
 * updates.
 */
public class OnAlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		final Intent cfpIntent = new Intent(context, CfpSyncService.class);
		context.startService(cfpIntent);

		final Intent newsIntent = new Intent(context, NewsSyncService.class);
		context.startService(newsIntent);

		final Intent appEngineIntent = new Intent(context,
				AppEngineSyncService.class);
		context.startService(appEngineIntent);
	}

}
