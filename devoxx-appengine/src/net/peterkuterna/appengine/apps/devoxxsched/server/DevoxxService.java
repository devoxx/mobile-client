/*
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

package net.peterkuterna.appengine.apps.devoxxsched.server;

import java.util.List;

import net.peterkuterna.appengine.apps.devoxxsched.c2dm.C2DMMessage;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.web.bindery.requestfactory.server.RequestFactoryServlet;

public class DevoxxService {

	static DataStore db = new DataStore();

	public static Session star(Session session) {
		final String sessionId = session.getSessionId();
		Session sessionDb = db.find(sessionId);
		if (sessionDb == null) {
			session.setEmailAddress(DataStore.getUserEmail());
			session = db.update(session);
		}
		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();
		C2DMMessage.enqueueStarSessionMessage(RequestFactoryServlet
				.getThreadLocalRequest().getSession().getServletContext(),
				user.getEmail(), sessionId);
		return session;
	}

	public static void unstar(Session session) {
		final String sessionId = session.getSessionId();
		Session sessionDb = db.find(sessionId);
		if (sessionDb != null) {
			db.delete(sessionDb.getId());
			UserService userService = UserServiceFactory.getUserService();
			User user = userService.getCurrentUser();
			C2DMMessage.enqueueUnstarSessionMessage(RequestFactoryServlet
					.getThreadLocalRequest().getSession().getServletContext(),
					user.getEmail(), session.getSessionId());
		}
	}

	public static List<Session> sync(List<Session> toStar,
			List<Session> toUnstar) {
		for (Session session : toStar) {
			star(session);
		}
		for (Session session : toUnstar) {
			unstar(session);
		}
		return db.findAll();
	}

}
