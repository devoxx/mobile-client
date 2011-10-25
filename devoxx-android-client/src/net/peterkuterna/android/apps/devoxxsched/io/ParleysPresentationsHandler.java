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
import static org.codehaus.jackson.JsonToken.VALUE_NUMBER_INT;
import static org.codehaus.jackson.JsonToken.VALUE_STRING;

import java.io.IOException;
import java.util.ArrayList;

import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.ParleysPresentations;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Tags;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpDatabase.ParleysPresentationsTags;
import net.peterkuterna.android.apps.devoxxsched.util.Lists;
import net.peterkuterna.android.apps.devoxxsched.util.ParserUtils;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.net.Uri;

/**
 * Handle a local {@link JsonParser} that defines a set of
 * {@link ParleysPresentations} entries.
 */
public class ParleysPresentationsHandler extends JsonHandler {

	public ParleysPresentationsHandler() {
		super(CfpContract.CONTENT_AUTHORITY);
	}

	@Override
	public ArrayList<ContentProviderOperation> parse(JsonParser parser,
			ContentResolver resolver) throws JsonParseException, IOException {
		final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

		JsonToken token;
		while ((token = parser.nextToken()) != END_ARRAY) {
			if (token == START_OBJECT) {
				parsePresentation(parser, batch, resolver);
			}
		}

		return batch;
	}

	private static void parsePresentation(JsonParser parser,
			ArrayList<ContentProviderOperation> batch, ContentResolver resolver)
			throws JsonParseException, IOException {
		final ContentProviderOperation.Builder operation = ContentProviderOperation
				.newInsert(ParleysPresentations.CONTENT_URI);
		final ArrayList<String> tags = Lists.newArrayList();
		String presentationId = null;
		int totalViews = 0;

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
			} else if (token == VALUE_NUMBER_INT) {
				final int value = parser.getIntValue();
				if (Fields.Presentations.ID.equals(fieldName) && (depth == 0)) {
					presentationId = ParleysPresentations
							.generateParleysId(value);
					operation.withValue(ParleysPresentations.PRESENTATION_ID,
							presentationId);
				} else if (Fields.Presentations.TOTAL_VIEWS.equals(fieldName)) {
					totalViews = value;
				}
			} else if (token == VALUE_STRING) {
				final String text = parser.getText();
				if (Fields.Presentations.TITLE.equals(fieldName)) {
					operation.withValue(
							ParleysPresentations.PRESENTATION_TITLE, text);
				} else if (Fields.Presentations.SUMMARY.equals(fieldName)) {
					operation.withValue(
							ParleysPresentations.PRESENTATION_SUMMARY, text);
				} else if (Fields.Presentations.THUMBNAIL.equals(fieldName)) {
					operation.withValue(
							ParleysPresentations.PRESENTATION_THUMBNAIL, text);
				} else if (depth == 2 && Fields.Keywords.NAME.equals(fieldName)) {
					tags.add(text.toLowerCase());
				}
			}
		}

		if (totalViews > 0) {
			batch.add(operation.build());

			final Uri parleysTagsUri = ParleysPresentations
					.buildTagsDirUri(presentationId);

			for (String tag : tags) {
				final String tagId = ParserUtils.sanitizeId(tag);
				batch.add(ContentProviderOperation.newInsert(Tags.CONTENT_URI)
						.withValue(Tags.TAG_ID, tagId)
						.withValue(Tags.TAG_NAME, tag).build());
				batch.add(ContentProviderOperation
						.newInsert(parleysTagsUri)
						.withValue(ParleysPresentationsTags.PRESENTATION_ID,
								presentationId)
						.withValue(ParleysPresentationsTags.TAG_ID, tagId)
						.build());
			}
		}
	}

	private interface Fields {

		interface Presentations {
			String MP3_URL = "MP3URL";
			String DURATION = "duration";
			String ID = "id";
			String IS_FREE = "isFree";
			String KEYWORDS = "keywords";
			String SPEAKERS = "speakers";
			String SUMMARY = "summary";
			String THUMBNAIL = "thumbnail";
			String TITLE = "title";
			String TOTAL_COMMENTS = "totalComments";
			String TOTAL_DOWNLOADS = "totalDownloads";
			String TOTAL_VIEWS = "totalViews";
			String TOTAL_VOTES = "totalVotes";
			String TOTAL_VOTES_VALUE = "totalVotesValue";
			String TYPE = "type";
		}

		interface Keywords {
			String NAME = "name";
		}
	}

}
