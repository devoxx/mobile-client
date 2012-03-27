/*
 * Copyright 2011 Google Inc.
 * Copyright 2011 Peter Kuterna.
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

package net.peterkuterna.android.apps.devoxxfrsched.io;

import java.io.IOException;

public abstract class BaseHandler {

	protected final String mAuthority;

	public BaseHandler(String authority) {
		mAuthority = authority;
	}

	/**
	 * General {@link IOException} that indicates a problem occured while
	 * parsing.
	 */
	public static class HandlerException extends IOException {
		public HandlerException(String message) {
			super(message);
		}

		public HandlerException(String message, Throwable cause) {
			super(message);
			initCause(cause);
		}

		@Override
		public String toString() {
			if (getCause() != null) {
				return getLocalizedMessage() + ": " + getCause();
			} else {
				return getLocalizedMessage();
			}
		}
	}

}
