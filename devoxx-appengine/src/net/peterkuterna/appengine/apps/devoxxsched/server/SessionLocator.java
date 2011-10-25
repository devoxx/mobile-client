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

import com.google.web.bindery.requestfactory.shared.Locator;

public class SessionLocator extends Locator<Session, Void> {

	@Override
	public Session create(Class<? extends Session> clazz) {
		return new Session();
	}

	@Override
	public Session find(Class<? extends Session> clazz, Void id) {
		return create(clazz);
	}

	@Override
	public Class<Session> getDomainType() {
		return Session.class;
	}

	@Override
	public Void getId(Session domainObject) {
		return null;
	}

	@Override
	public Class<Void> getIdType() {
		return Void.class;
	}

	@Override
	public Object getVersion(Session domainObject) {
		return null;
	}

}
