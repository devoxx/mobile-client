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

import static org.codehaus.jackson.JsonToken.END_ARRAY;
import static org.codehaus.jackson.JsonToken.END_OBJECT;
import static org.codehaus.jackson.JsonToken.FIELD_NAME;
import static org.codehaus.jackson.JsonToken.START_ARRAY;
import static org.codehaus.jackson.JsonToken.START_OBJECT;
import static org.codehaus.jackson.JsonToken.VALUE_NUMBER_INT;
import static org.codehaus.jackson.JsonToken.VALUE_STRING;

import java.io.IOException;
import java.util.ArrayList;

import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.News;
import net.peterkuterna.android.apps.devoxxfrsched.util.Lists;
import net.peterkuterna.android.apps.devoxxfrsched.util.ParserUtils;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.text.Html;

/**
 * Handle a local {@link JsonParser} that defines a set of {@link News} entries.
 */
public class NewsHandler extends JsonHandler {

	private final boolean noNotifications;

	public NewsHandler(boolean noNotifications) {
		super(CfpContract.CONTENT_AUTHORITY);

		this.noNotifications = noNotifications;
	}

	@Override
	public ArrayList<ContentProviderOperation> parse(JsonParser parser,
			ContentResolver resolver) throws JsonParseException, IOException {
		final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

		JsonToken token;
		while ((token = parser.nextToken()) != null) {
			if (token == JsonToken.FIELD_NAME
					&& "results".equals(parser.getText())) {
				while ((token = parser.nextToken()) != END_ARRAY) {
					if (token == START_OBJECT) {
						parseNewsResult(parser, batch, resolver,
								noNotifications);
					}
				}
			}
		}

		return batch;
	}

	private static void parseNewsResult(JsonParser parser,
			ArrayList<ContentProviderOperation> batch,
			ContentResolver resolver, boolean noNotifications)
			throws JsonParseException, IOException {
		ContentProviderOperation.Builder builder = ContentProviderOperation
				.newInsert(News.CONTENT_URI);

		int depth = 0;
		String fieldName = null;
		JsonToken token;
		while ((token = parser.nextToken()) != END_OBJECT || depth > 0) {
			if (token == START_OBJECT || token == START_ARRAY) {
				depth++;
			} else if (token == END_OBJECT || token == END_ARRAY) {
				depth--;
			} else if (token == FIELD_NAME) {
				fieldName = parser.getCurrentName();
			} else if (token == VALUE_STRING) {
				final String text = parser.getText();
				if (Fields.CREATED_AT.equals(fieldName)) {
					builder.withValue(News.NEWS_CREATED_AT,
							ParserUtils.parseTwitterSearchTime(text));
				} else if (Fields.TEXT.equals(fieldName)) {
					builder.withValue(News.NEWS_TEXT, Html.fromHtml(text)
							.toString());
				} else if (Fields.RESULT_TYPE.equals(fieldName)) {
					builder.withValue(News.NEWS_RESULT_TYPE,
							"popular".equals(text) ? 0 : 1);
				}
			} else if (token == VALUE_NUMBER_INT) {
				final long value = parser.getLongValue();
				if (Fields.ID.equals(fieldName)) {
					builder.withValue(News.NEWS_ID, value);
				} else if (Fields.TO_USER_ID.equals(fieldName)) {
					return;
				}
			}
		}

		if (!noNotifications) {
			builder.withValue(News.NEWS_NEW, 1);
		}

		batch.add(builder.build());
	}

	interface Fields {
		String ID = "id";
		String TO_USER_ID = "to_user_id";
		String TEXT = "text";
		String CREATED_AT = "created_at";
		String RESULT_TYPE = "result_type";
	}

}
