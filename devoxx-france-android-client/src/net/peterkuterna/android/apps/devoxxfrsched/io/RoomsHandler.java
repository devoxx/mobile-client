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

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

import java.io.IOException;
import java.util.ArrayList;

import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.Rooms;
import net.peterkuterna.android.apps.devoxxfrsched.util.Lists;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;

/**
 * Handle a local {@link XmlPullParser} that defines a set of {@link Rooms}
 * entries.
 */
public class RoomsHandler extends XmlHandler {

	public RoomsHandler() {
		super(CfpContract.CONTENT_AUTHORITY);
	}

	@Override
	public ArrayList<ContentProviderOperation> parse(XmlPullParser parser,
			ContentResolver resolver) throws XmlPullParserException,
			IOException {
		final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

		int type;
		while ((type = parser.next()) != END_DOCUMENT) {
			if (type == START_TAG && Tags.ROOM.equals(parser.getName())) {
				batch.add(parseRoom(parser));
			}
		}

		return batch;
	}

	private static ContentProviderOperation parseRoom(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		final int depth = parser.getDepth();
		final ContentProviderOperation.Builder builder = ContentProviderOperation
				.newInsert(Rooms.CONTENT_URI);

		String tag = null;
		int type;
		while (((type = parser.next()) != END_TAG || parser.getDepth() > depth)
				&& type != END_DOCUMENT) {
			if (type == START_TAG) {
				tag = parser.getName();
			} else if (type == END_TAG) {
				tag = null;
			} else if (type == TEXT) {
				final String text = parser.getText();
				if (Tags.ID.equals(tag)) {
				} else if (Tags.NAME.equals(tag)) {
					final String roomId = Rooms.generateRoomId(text);
					builder.withValue(Rooms.ROOM_ID, roomId);
					builder.withValue(Rooms.ROOM_NAME, text);
				} else if (Tags.CAPACITY.equals(tag)) {
					builder.withValue(Rooms.ROOM_CAPACITY, text);
				} else if (Tags.LEVEL.equals(tag)) {
					builder.withValue(Rooms.ROOM_LEVEL, text);
				} else if (Tags.LOCATIONNAME.equals(tag)) {
				}
			}
		}

		return builder.build();
	}

	interface Tags {
		String ROOM = "room";
		String ID = "id";
		String NAME = "name";
		String CAPACITY = "capacity";
		String LEVEL = "level";
		String LOCATIONNAME = "locationName";
	}

}
