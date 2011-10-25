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

package net.peterkuterna.appengine.apps.devoxxsched.server;

import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import com.google.android.c2dm.server.PMF;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

public class DataStore {

	/**
	 * Remove this object from the data store.
	 */
	public void delete(Long id) {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Session item = pm.getObjectById(Session.class, id);
			pm.deletePersistent(item);
		} finally {
			pm.close();
		}
	}

	/**
	 * Find a {@link Session} by id.
	 * 
	 * @param id
	 *            the {@link Session} id
	 * @return the associated {@link Session}, or null if not found
	 */
	@SuppressWarnings("unchecked")
	public Session find(Long id) {
		if (id == null) {
			return null;
		}

		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Query query = pm.newQuery("select from " + Session.class.getName()
					+ " where id==" + id.toString() + " && emailAddress=='"
					+ getUserEmail() + "'");
			List<Session> list = (List<Session>) query.execute();
			return list.size() == 0 ? null : list.get(0);
		} catch (RuntimeException e) {
			System.out.println(e);
			throw e;
		} finally {
			pm.close();
		}
	}

	public Session find(String sessionId) {
		if (sessionId == null) {
			return null;
		}

		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Query query = pm.newQuery("select from " + Session.class.getName()
					+ " where sessionId=='" + sessionId
					+ "' && emailAddress=='" + getUserEmail() + "'");
			List<Session> list = (List<Session>) query.execute();
			return list.size() == 0 ? null : list.get(0);
		} catch (RuntimeException e) {
			System.out.println(e);
			throw e;
		} finally {
			pm.close();
		}
	}

	@SuppressWarnings("unchecked")
	public List<Session> findAll() {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Query query = pm.newQuery("select from " + Session.class.getName()
					+ " where emailAddress=='" + getUserEmail() + "'");
			List<Session> list = (List<Session>) query.execute();
			if (list.size() == 0) {
				// Workaround for this issue:
				// http://code.google.com/p/datanucleus-appengine/issues/detail?id=24
				list.size();
			}

			return list;
		} catch (RuntimeException e) {
			System.out.println(e);
			throw e;
		} finally {
			pm.close();
		}
	}

	/**
	 * Persist this object in the datastore.
	 */
	public Session update(Session session) {
		// set the user id (not sure this is where we should be doing this)
		session.setUserId(getUserId());
		session.setEmailAddress(getUserEmail());
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			pm.makePersistent(session);
			return session;
		} finally {
			pm.close();
		}
	}

	public static String getUserId() {
		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();
		return user.getUserId();
	}

	public static String getUserEmail() {
		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();
		return user.getEmail();
	}

}
