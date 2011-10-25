/*
 * Copyright 2011 Google Inc.
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

package net.peterkuterna.appengine.apps.devoxxsched.c2dm;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletContext;

import net.peterkuterna.appengine.apps.devoxxsched.shared.Config;

import com.google.android.c2dm.server.C2DMessaging;
import com.google.android.c2dm.server.PMF;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

/**
 * Send a message using C2DM.
 */
public class C2DMMessage {

	private static final Logger log = Logger.getLogger(C2DMMessage.class
			.getName());

	public static void enqueueStarSessionMessage(ServletContext context,
			String recipient, String sessionId) {
		enqueueMessage(context, recipient, Config.C2DM_MESSAGE_EXTRA,
				Config.C2DM_MESSAGE_STAR, Config.C2DM_SESSION_EXTRA, sessionId);
	}

	public static void enqueueUnstarSessionMessage(ServletContext context,
			String recipient, String sessionId) {
		enqueueMessage(context, recipient, Config.C2DM_MESSAGE_EXTRA,
				Config.C2DM_MESSAGE_UNSTAR, Config.C2DM_SESSION_EXTRA,
				sessionId);
	}

	private static void enqueueMessage(ServletContext context,
			String recipient, String name2, String value2, String name3,
			String value3) {
		PersistenceManager pm = PMF.get().getPersistenceManager();

		try {
			UserService userService = UserServiceFactory.getUserService();
			User user = userService.getCurrentUser();
			String sender = "nobody";
			if (user != null) {
				sender = user.getEmail();
			}

			// Send push message to phone
			C2DMessaging push = C2DMessaging.get(context);

			String collapseKey = "" + sender.hashCode();

			// delete will fail if the pm is different than the one used to
			// load the object - we must close the object when we're done

			List<DeviceInfo> registrations = null;
			registrations = DeviceInfo.getDeviceInfoForUser(recipient);
			log.info("sendMessage: got " + registrations.size()
					+ " registrations");

			// Deal with upgrades and multi-device:
			// If user has one device with an old version and few new ones -
			// the old registration will be deleted.
			if (registrations.size() > 1) {
				// Make sure there is no 'bare' registration
				// Keys are sorted - check the first
				DeviceInfo first = registrations.get(0);
				Key oldKey = first.getKey();
				if (oldKey.toString().indexOf("#") < 0) {
					// multiple devices, first is old-style.
					registrations.remove(0); // don't send to it
					pm.deletePersistent(first);
				}
			}

			for (DeviceInfo deviceInfo : registrations) {
				if (!"ac2dm".equals(deviceInfo.getType())) {
					continue; // user-specified device type
				}

				try {
					push.sendWithRetry(deviceInfo.getDeviceRegistrationID(),
							collapseKey, Config.C2DM_ACCOUNT_EXTRA, sender,
							name2, value2, name3, value3);
				} catch (IOException ex) {
				}
			}
		} catch (Exception e) {
		} finally {
			pm.close();
		}
	}

}
