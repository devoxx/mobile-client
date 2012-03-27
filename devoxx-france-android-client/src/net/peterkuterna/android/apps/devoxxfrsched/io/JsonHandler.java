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

package net.peterkuterna.android.apps.devoxxfrsched.io;

import java.io.IOException;
import java.util.ArrayList;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.os.RemoteException;

/**
 * Abstract class that handles reading and parsing an {@link JsonParser} into a
 * set of {@link ContentProviderOperation}. It catches recoverable network
 * exceptions and rethrows them as {@link HandlerException}. Any local
 * {@link ContentProvider} exceptions are considered unrecoverable.
 * <p>
 * This class is only designed to handle simple one-way synchronization.
 */
public abstract class JsonHandler extends BaseHandler {

	public JsonHandler(String authority) {
		super(authority);
	}

	/**
	 * Parse the given {@link JsonParser}, turning into a series of
	 * {@link ContentProviderOperation} that are immediately applied using the
	 * given {@link ContentResolver}.
	 */
	public void parseAndApply(JsonParser parser, ContentResolver resolver)
			throws HandlerException {
		try {
			final ArrayList<ContentProviderOperation> batch = parse(parser,
					resolver);
			resolver.applyBatch(mAuthority, batch);
		} catch (HandlerException e) {
			throw e;
		} catch (JsonParseException e) {
			throw new HandlerException("Problem parsing Json response", e);
		} catch (IOException e) {
			throw new HandlerException("Problem reading response", e);
		} catch (RemoteException e) {
			throw new RuntimeException("Problem applying batch operation", e);
		} catch (OperationApplicationException e) {
			throw new RuntimeException("Problem applying batch operation", e);
		}
	}

	/**
	 * Parse the given {@link JsonParser}, returning a set of
	 * {@link ContentProviderOperation} that will bring the
	 * {@link ContentProvider} into sync with the parsed data.
	 */
	public abstract ArrayList<ContentProviderOperation> parse(
			JsonParser parser, ContentResolver resolver)
			throws JsonParseException, IOException;

}
