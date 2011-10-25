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
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Blocks;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Rooms;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Sessions;
import net.peterkuterna.android.apps.devoxxsched.util.Lists;
import net.peterkuterna.android.apps.devoxxsched.util.ParserUtils;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

/**
 * Handle a local {@link JsonHandler} that defines a set of {@link Sessions} and
 * {@link Blocks} entries.
 */
public class ScheduleHandler extends JsonHandler {

	public ScheduleHandler() {
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
				parseSchedule(parser, batch, resolver);
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
	private static void parseSchedule(JsonParser parser,
			ArrayList<ContentProviderOperation> batch, ContentResolver resolver)
			throws JsonParseException, IOException {
		String sessionId = null;
		long startTime = 0;
		long endTime = 0;
		String kind = null;
		String code = null;
		String type = null;
		String roomId = null;
		// ArrayList<String> speakerIds = new ArrayList<String>();

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
			} else if (token == VALUE_STRING) {
				final String text = parser.getText();
				if (Fields.Schedule.FROMTIME.equals(fieldName)) {
					startTime = ParserUtils.parseDevoxxTime(text);
				} else if (Fields.Schedule.TOTIME.equals(fieldName)) {
					endTime = ParserUtils.parseDevoxxTime(text);
				} else if (Fields.Schedule.KIND.equals(fieldName)) {
					kind = text;
				} else if (Fields.Schedule.CODE.equals(fieldName)) {
					code = text;
				} else if (Fields.Schedule.TYPE.equals(fieldName)) {
					type = text;
				} else if (Fields.Schedule.PRESENTATIONURI.equals(fieldName)) {
					sessionId = Uri.parse(text).getLastPathSegment();
				} else if (Fields.Schedule.ROOM.equals(fieldName)) {
					roomId = Rooms.generateRoomId(text);
				}
			}
		}

		final String blockId = findOrCreateBlock(kind, code, type, startTime,
				endTime, batch, resolver);

		if (sessionId != null) {
			final Uri sessionUri = Sessions.buildSessionUri(sessionId);

			ContentProviderOperation.Builder builder = ContentProviderOperation
					.newUpdate(sessionUri);
			builder.withValue(Sessions.BLOCK_ID, blockId);
			builder.withValue(Sessions.DELETED, CfpContract.NOT_DELETED);
			builder.withValue(Sessions.ROOM_ID, roomId);
			batch.add(builder.build());
		}
	}

	/**
	 * Return a {@link Blocks#BLOCK_ID} matching the requested arguments,
	 * inserting a new {@link Blocks} entry as a
	 * {@link ContentProviderOperation} when none already exists.
	 */
	private static String findOrCreateBlock(String kind, String code,
			String type, long startTime, long endTime,
			ArrayList<ContentProviderOperation> batch, ContentResolver resolver) {
		final String blockId = Blocks.generateBlockId(
				ParserUtils.isTalk(code) ? type : kind, startTime, endTime);

		if (!isBlockExisting(blockId, resolver)) {
			final String title = ParserUtils.isTalk(code) ? type : code;
			final ContentProviderOperation.Builder builder = ContentProviderOperation
					.newInsert(Blocks.CONTENT_URI);
			builder.withValue(Blocks.BLOCK_ID, blockId);
			builder.withValue(Blocks.BLOCK_TITLE, title);
			builder.withValue(Blocks.BLOCK_START, startTime);
			builder.withValue(Blocks.BLOCK_END, endTime);
			builder.withValue(Blocks.BLOCK_KIND, kind);
			builder.withValue(Blocks.BLOCK_TYPE, type);
			builder.withValue(Blocks.BLOCK_CODE, code);
			batch.add(builder.build());
		}

		return blockId;
	}

	private static boolean isBlockExisting(String blockId,
			ContentResolver resolver) {
		final Uri uri = Blocks.buildBlockUri(blockId);
		final Cursor cursor = resolver.query(uri, BlocksQuery.PROJECTION, null,
				null, null);
		try {
			return cursor.moveToFirst();
		} finally {
			cursor.close();
		}
	}

	/**
	 * {@link Blocks} query parameters.
	 */
	private interface BlocksQuery {
		String[] PROJECTION = { Blocks.BLOCK_ID };

		int BLOCK_ID = 0;
	}

	private interface Fields {

		interface Schedule {
			String CODE = "code";
			String FROMTIME = "fromTime";
			String ID = "id";
			String KIND = "kind";
			String NOTE = "note";
			String PARTNERSLOT = "partnerSlot";
			String PRESENTATIONURI = "presentationUri";
			String ROOM = "room";
			String SPEAKER = "speaker";
			String SPEAKERURI = "speakerUri";
			String SPEAKERS = "speakers";
			String TITLE = "title";
			String TOTIME = "toTime";
			String TYPE = "type";
		}

		interface Speakers {
			String SPEAKER = "speaker";
			String SPEAKERURI = "speakerUri";
		}

	}

}
