/*
 * Copyright 2011 Google Inc.
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
import java.io.InputStream;

import net.peterkuterna.android.apps.devoxxfrsched.io.BaseHandler.HandlerException;
import net.peterkuterna.android.apps.devoxxfrsched.util.JsonHandlerUtils;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;

import android.content.ContentResolver;

/**
 * Executes an {@link HttpUriRequest} and passes the result as an
 * {@link JsonParser} to the given {@link JsonHandler}.
 */
public class RemoteExecutor {
	private final HttpClient mHttpClient;
	private final ContentResolver mResolver;

	public RemoteExecutor(HttpClient httpClient, ContentResolver resolver) {
		mHttpClient = httpClient;
		mResolver = resolver;
	}

	/**
	 * Execute a {@link HttpGet} request, passing a valid response through
	 * {@link JsonHandler#parseAndApply(JsonParser, ContentResolver)}.
	 */
	public void executeGet(String url, JsonHandler handler)
			throws HandlerException {
		final HttpUriRequest request = new HttpGet(url);
		execute(request, handler);
	}

	/**
	 * Execute this {@link HttpUriRequest}, passing a valid response through
	 * {@link JsonHandler#parseAndApply(JsonParser, ContentResolver)}.
	 */
	public void execute(HttpUriRequest request, JsonHandler handler)
			throws HandlerException {
		try {
			final HttpResponse resp = mHttpClient.execute(request);
			final int status = resp.getStatusLine().getStatusCode();
			if (status != HttpStatus.SC_OK) {
				throw new HandlerException("Unexpected server response "
						+ resp.getStatusLine() + " for "
						+ request.getRequestLine());
			}

			final InputStream input = resp.getEntity().getContent();
			try {
				final JsonParser parser = JsonHandlerUtils.newJsonParser(input);
				handler.parseAndApply(parser, mResolver);
			} catch (JsonParseException e) {
				throw new HandlerException("Malformed response for "
						+ request.getRequestLine(), e);
			} finally {
				if (input != null)
					input.close();
			}
		} catch (HandlerException e) {
			throw e;
		} catch (IOException e) {
			throw new HandlerException("Problem reading remote response for "
					+ request.getRequestLine(), e);
		}
	}
}
