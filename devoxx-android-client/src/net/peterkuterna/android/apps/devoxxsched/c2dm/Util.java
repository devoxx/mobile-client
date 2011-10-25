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
package net.peterkuterna.android.apps.devoxxsched.c2dm;

import android.content.Context;

import com.google.web.bindery.event.shared.SimpleEventBus;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.vm.RequestFactorySource;

/**
 * Utility methods for getting the base URL for client-server communication and
 * retrieving shared preferences.
 */
public class Util {

	private static final String TAG = "Util";

	/**
	 * URL suffix for the RequestFactory servlet.
	 */
	public static final String RF_METHOD = "/gwtRequest";

	/**
	 * Creates and returns an initialized {@link RequestFactory} of the given
	 * type.
	 */
	public static <T extends RequestFactory> T getRequestFactory(
			Context context, Class<T> factoryClass) {
		T requestFactory = RequestFactorySource.create(factoryClass);

		requestFactory.initialize(new SimpleEventBus(),
				new AppEngineRequestTransport(context, RF_METHOD));

		return requestFactory;
	}

}
