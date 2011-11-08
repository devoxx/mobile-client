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

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.repackaged.com.google.common.collect.Lists;
import com.google.web.bindery.requestfactory.server.RequestFactoryServlet;

public class DevoxxService {

	static DataStore db = new DataStore();

	public static Session star(Session session) {
		return star(session, true);
	}

	public static Session star(Session session, boolean clearCache) {
		if (clearCache) {
			final String email = DataStore.getUserEmail();
			if (email != null) {
				final MemcacheService syncCache = MemcacheServiceFactory
						.getMemcacheService();
				syncCache.delete(email);
			}
		}

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
		unstar(session, true);
	}

	public static void unstar(Session session, boolean clearCache) {
		if (clearCache) {
			final String email = DataStore.getUserEmail();
			if (email != null) {
				final MemcacheService syncCache = MemcacheServiceFactory
						.getMemcacheService();
				syncCache.delete(email);
			}
		}

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
		final String email = DataStore.getUserEmail();
		final MemcacheService syncCache = MemcacheServiceFactory
				.getMemcacheService();
		boolean sessionsToStar = toStar != null && toStar.size() > 0;
		boolean sessionsToUnstar = toUnstar != null && toUnstar.size() > 0;
		boolean clearCache = sessionsToStar || sessionsToUnstar;
		if (clearCache) {
			if (email != null) {
				syncCache.delete(email);
			}
		}

		for (Session session : toStar) {
			star(session, false);
		}
		for (Session session : toUnstar) {
			unstar(session, false);
		}

		List<Session> result = Lists.newArrayList();
		List<Session> resultFromCache = (List<Session>) syncCache.get(email);
		if (resultFromCache == null) {
			List<Session> resultFromDb = db.findAll();
			result.addAll(resultFromDb);
			syncCache.put(email, result);
		} else {
			result.addAll(resultFromCache);
		}
		return result;
	}

}
