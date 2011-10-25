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

package net.peterkuterna.appengine.apps.devoxxsched.client;

import net.peterkuterna.appengine.apps.devoxxsched.shared.RegistrationInfoProxy;

import com.google.web.bindery.requestfactory.shared.InstanceRequest;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.ServiceName;

public interface RegistrationRequestFactory extends RequestFactory {

	@ServiceName("net.peterkuterna.appengine.apps.devoxxsched.server.RegistrationInfo")
	public interface RegistrationInfoRequest extends RequestContext {
		/**
		 * Register a device for C2DM messages.
		 */
		InstanceRequest<RegistrationInfoProxy, Void> register();

		/**
		 * Unregister a device for C2DM messages.
		 */
		InstanceRequest<RegistrationInfoProxy, Void> unregister();
	}

	RegistrationInfoRequest registrationInfoRequest();

}
