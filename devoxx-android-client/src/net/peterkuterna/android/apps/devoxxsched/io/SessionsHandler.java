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
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Rooms;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.SessionTypes;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Sessions;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Speakers;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.SyncColumns;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Tags;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Tracks;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpDatabase.SessionsSpeakers;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpDatabase.SessionsTags;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpDatabase.Tables;
import net.peterkuterna.android.apps.devoxxsched.util.Lists;
import net.peterkuterna.android.apps.devoxxsched.util.ParserUtils;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * Handle a local {@link JsonParser} that defines a set of {@link Sessions},
 * {@link Speakers} and {@link Tags} entries.
 */
public class SessionsHandler extends JsonHandler {

	public SessionsHandler() {
		super(CfpContract.CONTENT_AUTHORITY);
	}

	/** {@inheritDoc} */
	@Override
	public ArrayList<ContentProviderOperation> parse(JsonParser parser,
			ContentResolver resolver) throws JsonParseException, IOException {
		final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

		batch.add(ContentProviderOperation.newUpdate(Sessions.CONTENT_URI)
				.withValue(Sessions.DELETED, CfpContract.MARK_AS_DELETED)
				.build());

		JsonToken token;
		while ((token = parser.nextToken()) != END_ARRAY) {
			if (token == START_OBJECT) {
				parseSession(parser, batch, resolver);
			}
		}

		batch.add(ContentProviderOperation
				.newDelete(Sessions.CONTENT_URI)
				.withSelection(
						Sessions.DELETED + "=?",
						new String[] { String
								.valueOf(CfpContract.MARK_AS_DELETED) })
				.build());

		return batch;
	}

	/**
	 * Parse a given {@link Rooms} entry, building
	 * {@link ContentProviderOperation} to define it locally.
	 */
	private static void parseSession(JsonParser parser,
			ArrayList<ContentProviderOperation> batch, ContentResolver resolver)
			throws JsonParseException, IOException {
		final ContentValues values = new ContentValues();
		final ArrayList<String> speakerIds = Lists.newArrayList();
		final ArrayList<String> tags = Lists.newArrayList();
		String sessionId = null;

		int depth = 0;
		String fieldName = null;
		StringBuilder keywords = new StringBuilder();
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
				if (Fields.Sessions.ID.equals(fieldName)) {
					sessionId = Sessions.generateSessionId(value);
					values.put(Sessions.SESSION_ID, sessionId);
				}
			} else if (token == VALUE_STRING) {
				final String text = parser.getText();
				if (Fields.Sessions.EXPERIENCE.equals(fieldName)) {
					values.put(Sessions.SESSION_EXPERIENCE, text);
				} else if (Fields.Sessions.SUMMARY.equals(fieldName)) {
					values.put(Sessions.SESSION_SUMMARY, text);
				} else if (Fields.Sessions.TITLE.equals(fieldName)) {
					values.put(Sessions.SESSION_TITLE, text);
					values.put(Sessions.SESSION_URL,
							ParserUtils.buildDevoxxWebUrl(text));
				} else if (Fields.Sessions.TRACK.equals(fieldName)) {
					values.put(Sessions.TRACK_ID, Tracks.generateTrackId(text));
				} else if (Fields.Sessions.TYPE.equals(fieldName)) {
					values.put(Sessions.SESSION_TYPE_ID,
							SessionTypes.generateSessionTypeId(text));
				} else if (depth == 2
						&& Fields.Speakers.SPEAKERURI.equals(fieldName)) {
					speakerIds.add(Uri.parse(text).getLastPathSegment());
				} else if (depth == 2 && Fields.Tags.NAME.equals(fieldName)) {
					if (keywords.length() > 0) {
						keywords.append(", ");
					}
					keywords.append(text);
					tags.add(text.toLowerCase());
				}
			}
		}

		final Uri sessionUri = Sessions.buildSessionUri(sessionId);

		ContentProviderOperation.Builder sessionBuilder = null;
		final ContentValues oldValues = querySessionDetails(sessionUri,
				resolver);
		final long localUpdated = oldValues.getAsLong(SyncColumns.UPDATED);
		if (localUpdated == CfpContract.UPDATED_NEVER) {
			sessionBuilder = ContentProviderOperation
					.newInsert(Sessions.CONTENT_URI);
			values.put(Sessions.SESSION_NEW, 1);
		} else {
			sessionBuilder = ContentProviderOperation.newUpdate(sessionUri);
			values.put(Sessions.SESSION_NEW, 0);
		}

		if (oldValues.containsKey(Sessions.SESSION_STARRED)) {
			values.put(Sessions.SESSION_STARRED,
					oldValues.getAsInteger(Sessions.SESSION_STARRED));
		}
		values.put(Sessions.SESSION_KEYWORDS, keywords.toString());
		values.put(SyncColumns.UPDATED, System.currentTimeMillis());
		values.put(SyncColumns.DELETED, CfpContract.NOT_DELETED);
		batch.add(sessionBuilder.withValues(values).build());

		final Uri sessionSpeakersUri = Sessions.buildSpeakersDirUri(sessionId);
		batch.add(ContentProviderOperation.newDelete(sessionSpeakersUri)
				.build());

		for (String speakerId : speakerIds) {
			batch.add(ContentProviderOperation.newInsert(sessionSpeakersUri)
					.withValue(SessionsSpeakers.SESSION_ID, sessionId)
					.withValue(SessionsSpeakers.SPEAKER_ID, speakerId).build());
		}

		final Uri sessionTagsUri = Sessions.buildTagsDirUri(sessionId);
		batch.add(ContentProviderOperation.newDelete(sessionTagsUri).build());

		for (String tag : tags) {
			final String tagId = ParserUtils.sanitizeId(tag);
			batch.add(ContentProviderOperation.newInsert(Tags.CONTENT_URI)
					.withValue(Tags.TAG_ID, tagId)
					.withValue(Tags.TAG_NAME, tag).build());
			batch.add(ContentProviderOperation.newInsert(sessionTagsUri)
					.withValue(SessionsTags.SESSION_ID, sessionId)
					.withValue(SessionsTags.TAG_ID, tagId).build());
		}
	}

	private static ContentValues querySessionDetails(Uri uri,
			ContentResolver resolver) {
		final ContentValues values = new ContentValues();
		final Cursor cursor = resolver.query(uri, SessionsQuery.PROJECTION,
				null, null, null);
		try {
			if (cursor.moveToFirst()) {
				values.put(SyncColumns.UPDATED,
						cursor.getLong(SessionsQuery.UPDATED));
				values.put(Sessions.SESSION_STARRED,
						cursor.getInt(SessionsQuery.STARRED));
			} else {
				values.put(SyncColumns.UPDATED, CfpContract.UPDATED_NEVER);
			}
		} finally {
			cursor.close();
		}
		return values;
	}

	/**
	 * {@link Sessions} query parameters.
	 */
	private interface SessionsQuery {
		String[] PROJECTION = { Tables.SESSIONS + "." + SyncColumns.UPDATED,
				Sessions.SESSION_STARRED, };

		int UPDATED = 0;
		int STARRED = 1;
	}

	private interface Fields {

		interface Sessions {
			String EXPERIENCE = "experience";
			String ID = "id";
			String SPEAKER = "speaker";
			String SPEAKERS = "speakers";
			String SUMMARY = "summary";
			String TAGS = "tags";
			String TITLE = "title";
			String TRACK = "track";
			String TYPE = "type";
		}

		interface Speakers {
			String SPEAKERURI = "speakerUri";
		}

		interface Tags {
			String NAME = "name";
		}
	}

}
