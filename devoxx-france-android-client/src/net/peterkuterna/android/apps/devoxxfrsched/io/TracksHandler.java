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
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.Tracks;
import net.peterkuterna.android.apps.devoxxfrsched.util.Lists;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.graphics.Color;

/**
 * Handle a local {@link XmlPullParser} that defines a set of {@link Tracks}
 * entries.
 */
public class TracksHandler extends XmlHandler {

	public TracksHandler() {
		super(CfpContract.CONTENT_AUTHORITY);
	}

	@Override
	public ArrayList<ContentProviderOperation> parse(XmlPullParser parser,
			ContentResolver resolver) throws XmlPullParserException,
			IOException {
		final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

		int type;
		while ((type = parser.next()) != END_DOCUMENT) {
			if (type == START_TAG && Tags.TRACK.equals(parser.getName())) {
				batch.add(parseTrack(parser));
			}
		}

		return batch;
	}

	private static ContentProviderOperation parseTrack(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		final int depth = parser.getDepth();
		final ContentProviderOperation.Builder builder = ContentProviderOperation
				.newInsert(Tracks.CONTENT_URI);

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
					final String trackId = Tracks.generateTrackId(text);
					builder.withValue(Tracks.TRACK_ID, trackId);
					builder.withValue(Tracks.TRACK_NAME, text);
				} else if (Tags.COLOR.equals(tag)) {
					final int color = Color.parseColor(text);
					builder.withValue(Tracks.TRACK_COLOR, color);
				} else if (Tags.DESCRIPTION.equals(tag)) {
					builder.withValue(Tracks.TRACK_DESCRIPTION, text);
				} else if (Tags.HASHTAG.equals(tag)) {
					builder.withValue(Tracks.TRACK_HASHTAG, text);
				}
			}
		}

		return builder.build();
	}

	interface Tags {
		String TRACK = "track";
		String ID = "id";
		String NAME = "name";
		String COLOR = "color";
		String DESCRIPTION = "description";
		String HASHTAG = "hashtag";
	}

}
