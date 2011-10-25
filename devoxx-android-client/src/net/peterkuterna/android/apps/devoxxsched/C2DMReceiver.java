/*
 * Copyright 2010 Google Inc.
 * Copyright 2011 Peter Kuterna
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package net.peterkuterna.android.apps.devoxxsched;

import java.io.IOException;

import net.peterkuterna.android.apps.devoxxsched.c2dm.DeviceRegistrar;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Sessions;
import net.peterkuterna.android.apps.devoxxsched.ui.SettingsActivity;
import net.peterkuterna.android.apps.devoxxsched.util.Prefs;
import net.peterkuterna.android.apps.devoxxsched.util.Prefs.DevoxxPrefs;
import net.peterkuterna.appengine.apps.devoxxsched.shared.Config;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.google.android.c2dm.C2DMBaseReceiver;
import com.google.android.c2dm.C2DMessaging;

/**
 * Receive a push message from the Cloud to Device Messaging (C2DM) service.
 * This class should be modified to include functionality specific to your
 * application. This class must have a no-arg constructor and pass the sender id
 * to the superclass constructor.
 */
public class C2DMReceiver extends C2DMBaseReceiver {

	protected static final String TAG = "C2DMReceiver";

	public C2DMReceiver() {
		super(Config.C2DM_SENDER);
	}

	/**
	 * Called when a registration token has been received.
	 * 
	 * @param context
	 *            the Context
	 * @param registrationId
	 *            the registration id as a String
	 * @throws IOException
	 *             if registration cannot be performed
	 */
	@Override
	public void onRegistered(Context context, String registration) {
		DeviceRegistrar.registerOrUnregister(context, registration, true);
	}

	/**
	 * Called when the device has been unregistered.
	 * 
	 * @param context
	 *            the Context
	 */
	@Override
	public void onUnregistered(Context context) {
		SharedPreferences prefs = Prefs.get(context);
		String deviceRegistrationID = prefs.getString(
				DevoxxPrefs.DEVICE_REGISTRATION_ID, null);
		DeviceRegistrar.registerOrUnregister(context, deviceRegistrationID,
				false);
	}

	/**
	 * Called on registration error. This is called in the context of a Service
	 * - no dialog or UI.
	 * 
	 * @param context
	 *            the Context
	 * @param errorId
	 *            an error message, defined in {@link C2DMBaseReceiver}
	 */
	@Override
	public void onError(Context context, String errorId) {
		context.sendBroadcast(new Intent(SettingsActivity.UPDATE_UI_ACTION));
	}

	/**
	 * Called when a cloud message has been received.
	 */
	@Override
	public void onMessage(Context context, Intent intent) {
		SharedPreferences prefs = Prefs.get(context);
		String deviceRegistrationID = prefs.getString(
				DevoxxPrefs.DEVICE_REGISTRATION_ID, null);
		if (deviceRegistrationID != null) {
			final String message = intent.getExtras().getString(
					Config.C2DM_MESSAGE_EXTRA);
			final ContentResolver resolver = getContentResolver();
			if (Config.C2DM_MESSAGE_STAR.equals(message)) {
				final String sessionId = intent.getExtras().getString(
						Config.C2DM_SESSION_EXTRA);
				final Uri sessionUri = Sessions.buildSessionUri(sessionId);
				ContentValues cv = new ContentValues();
				cv.put(Sessions.SESSION_STARRED, 1);
				resolver.update(sessionUri, cv, null, null);
			} else if (Config.C2DM_MESSAGE_UNSTAR.equals(message)) {
				final String sessionId = intent.getExtras().getString(
						Config.C2DM_SESSION_EXTRA);
				final Uri sessionUri = Sessions.buildSessionUri(sessionId);
				ContentValues cv = new ContentValues();
				cv.put(Sessions.SESSION_STARRED, 0);
				resolver.update(sessionUri, cv, null, null);
			}
		} else {
			C2DMessaging.unregister(this);
		}
	}

}
