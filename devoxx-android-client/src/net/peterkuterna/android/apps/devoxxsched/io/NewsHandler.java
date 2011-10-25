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

package net.peterkuterna.android.apps.devoxxsched.io;

import static org.codehaus.jackson.JsonToken.END_ARRAY;
import static org.codehaus.jackson.JsonToken.END_OBJECT;
import static org.codehaus.jackson.JsonToken.FIELD_NAME;
import static org.codehaus.jackson.JsonToken.START_ARRAY;
import static org.codehaus.jackson.JsonToken.START_OBJECT;
import static org.codehaus.jackson.JsonToken.VALUE_STRING;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.News;
import net.peterkuterna.android.apps.devoxxsched.util.Lists;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentResolver;

/**
 * Handle a local {@link JsonParser} that defines a set of {@link News} entries.
 */
public class NewsHandler extends JsonHandler {

	public NewsHandler() {
		super(CfpContract.CONTENT_AUTHORITY);
	}

	@Override
	public ArrayList<ContentProviderOperation> parse(JsonParser parser,
			ContentResolver resolver) throws JsonParseException, IOException {
		final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

		JsonToken token;
		int depth = 0;
		while ((token = parser.nextToken()) != END_OBJECT || depth > 1) {
			if (depth == 2 && token == START_OBJECT) {
				parseMessage(parser, batch, resolver);
			} else if (token == START_OBJECT || token == START_ARRAY) {
				depth++;
			} else if (token == END_OBJECT || token == END_ARRAY) {
				depth--;
			}
		}

		Collections.reverse(batch);

		return batch;
	}

	/**
	 * Parse a given {@link News} entry, building
	 * {@link ContentProviderOperation} to define it locally.
	 */
	private static void parseMessage(JsonParser parser,
			ArrayList<ContentProviderOperation> batch, ContentResolver resolver)
			throws JsonParseException, IOException {
		Builder builder = ContentProviderOperation.newInsert(News.CONTENT_URI);

		builder.withValue(News.NEWS_NEW, 1);

		String fieldName = null;
		JsonToken token;
		while ((token = parser.nextToken()) != END_OBJECT) {
			if (token == FIELD_NAME) {
				fieldName = parser.getCurrentName();
			} else if (token == VALUE_STRING) {
				final String text = parser.getText();
				if (Fields.DATE.equals(fieldName)) {
					final String id = News.generateNewsId(text);
					builder.withValue(News.NEWS_ID, id);
					builder.withValue(News.NEWS_DATE, text);
				} else if (Fields.MESSAGE.equals(fieldName)) {
					builder.withValue(News.NEWS_TEXT, text);
				}
			}
		}

		batch.add(builder.build());
	}

	private interface Fields {
		String DATE = "date";
		String MESSAGE = "message";
	}

}
