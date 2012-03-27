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
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.Rooms;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.Speakers;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.SyncColumns;
import net.peterkuterna.android.apps.devoxxfrsched.util.Lists;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * Handle a local {@link JsonHandler} that defines a set of {@link Speakers}
 * entries.
 */
public class SpeakersHandler extends JsonHandler {

	public SpeakersHandler() {
		super(CfpContract.CONTENT_AUTHORITY);
	}

	/** {@inheritDoc} */
	@Override
	public ArrayList<ContentProviderOperation> parse(JsonParser parser,
			ContentResolver resolver) throws JsonParseException, IOException {
		final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

		batch.add(ContentProviderOperation.newUpdate(Speakers.CONTENT_URI)
				.withValue(Speakers.DELETED, CfpContract.MARK_AS_DELETED)
				.build());

		JsonToken token;
		while ((token = parser.nextToken()) != END_ARRAY) {
			if (token == START_OBJECT) {
				parseSpeaker(parser, batch, resolver);
			}
		}

		batch.add(ContentProviderOperation
				.newDelete(Speakers.CONTENT_URI)
				.withSelection(
						Speakers.DELETED + "=?",
						new String[] { String
								.valueOf(CfpContract.MARK_AS_DELETED) })
				.build());

		return batch;
	}

	/**
	 * Parse a given {@link Rooms} entry, building
	 * {@link ContentProviderOperation} to define it locally.
	 */
	private static void parseSpeaker(JsonParser parser,
			ArrayList<ContentProviderOperation> batch, ContentResolver resolver)
			throws JsonParseException, IOException {
		final ContentValues values = new ContentValues();
		String speakerId = null;

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
				if (Fields.ID.equals(fieldName)) {
					speakerId = Speakers.generateSpeakerId(value);
					values.put(Speakers.SPEAKER_ID, speakerId);
				}
			} else if (token == VALUE_STRING) {
				final String text = parser.getText();
				if (Fields.BIO.equals(fieldName)) {
					values.put(Speakers.SPEAKER_BIO, text);
				} else if (Fields.COMPANY.equals(fieldName)) {
					values.put(Speakers.SPEAKER_COMPANY, text);
				} else if (Fields.FIRSTNAME.equals(fieldName)) {
					values.put(Speakers.SPEAKER_FIRSTNAME, text);
				} else if (Fields.IMAGE.equals(fieldName)) {
					values.put(Speakers.SPEAKER_IMAGE_URL, text);
				} else if (Fields.LASTNAME.equals(fieldName)) {
					values.put(Speakers.SPEAKER_LASTNAME, text);
				}
			}
		}

		final Uri speakerUri = Speakers.buildSpeakerUri(speakerId);

		ContentProviderOperation.Builder speakerBuilder = null;
		final ContentValues oldValues = querySpeakerDetails(speakerUri,
				resolver);
		final long localUpdated = oldValues.getAsLong(SyncColumns.UPDATED);
		if (localUpdated == CfpContract.UPDATED_NEVER) {
			speakerBuilder = ContentProviderOperation
					.newInsert(Speakers.CONTENT_URI);
		} else {
			speakerBuilder = ContentProviderOperation.newUpdate(speakerUri);
		}

		values.put(SyncColumns.UPDATED, System.currentTimeMillis());
		values.put(SyncColumns.DELETED, CfpContract.NOT_DELETED);
		batch.add(speakerBuilder.withValues(values).build());
	}

	private static ContentValues querySpeakerDetails(Uri uri,
			ContentResolver resolver) {
		final ContentValues values = new ContentValues();
		final Cursor cursor = resolver.query(uri, SpeakersQuery.PROJECTION,
				null, null, null);
		try {
			if (cursor.moveToFirst()) {
				values.put(SyncColumns.UPDATED,
						cursor.getLong(SpeakersQuery.UPDATED));
			} else {
				values.put(SyncColumns.UPDATED, CfpContract.UPDATED_NEVER);
			}
		} finally {
			cursor.close();
		}
		return values;
	}

	/**
	 * {@link Speakers} query parameters.
	 */
	private interface SpeakersQuery {
		String[] PROJECTION = { SyncColumns.UPDATED };

		int UPDATED = 0;
	}

	private interface Fields {
		String BIO = "bio";
		String COMPANY = "company";
		String FIRSTNAME = "firstName";
		String ID = "id";
		String IMAGE = "imageURI";
		String LASTNAME = "lastName";
	}

}
