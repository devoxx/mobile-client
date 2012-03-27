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

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

/**
 * Abstract base class that serves as basis for all our {@link IntentService}
 * implementations. A {@link ResultReceiver} can be passed to deliver status
 * updates.
 */
public abstract class AbstractSyncService extends IntentService {

	public static final String EXTRA_STATUS_RECEIVER = "net.peterkuterna.android.apps.devoxxfrsched.extra.STATUS_RECEIVER";

	private final String mName;

	public static final int STATUS_RUNNING = 0x1;
	public static final int STATUS_ERROR = 0x2;
	public static final int STATUS_FINISHED = 0x3;

	public AbstractSyncService(String name) {
		super(name);

		mName = name;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		final ResultReceiver receiver = intent
				.getParcelableExtra(EXTRA_STATUS_RECEIVER);
		Log.d("SyncService", "receiver="+receiver);
		if (receiver != null) {
			receiver.send(STATUS_RUNNING, Bundle.EMPTY);
		}

		try {
			doSync(intent);
		} catch (Exception e) {
			Log.e(mName, "Problem while syncing", e);

			if (receiver != null) {
				final Bundle bundle = new Bundle();
				bundle.putString(Intent.EXTRA_TEXT, e.toString());
				receiver.send(STATUS_ERROR, bundle);
			}
		}

		if (receiver != null) {
			receiver.send(STATUS_FINISHED, Bundle.EMPTY);
		}
	}

	protected abstract void doSync(Intent intent) throws Exception;

}
