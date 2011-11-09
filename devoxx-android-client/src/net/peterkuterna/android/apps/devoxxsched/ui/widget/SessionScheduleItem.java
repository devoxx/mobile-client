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

package net.peterkuterna.android.apps.devoxxsched.ui.widget;

import java.util.ArrayList;

import net.peterkuterna.android.apps.devoxxsched.ui.SessionScheduleItemsFragment;
import android.database.Cursor;

public class SessionScheduleItem extends ScheduleItem {

	public static void loadSessions(Cursor data,
			ArrayList<SessionScheduleItem> sessionsList) {
		if (data == null) {
			return;
		}

		do {
			final String sessionId = data
					.getString(SessionScheduleItemsFragment.SessionsQuery.SESSION_ID);
			final String title = data
					.getString(SessionScheduleItemsFragment.SessionsQuery.SESSION_TITLE);
			final long start = data
					.getLong(SessionScheduleItemsFragment.SessionsQuery.BLOCK_START);
			final long end = data
					.getLong(SessionScheduleItemsFragment.SessionsQuery.BLOCK_END);
			final int trackColor = data
					.getInt(SessionScheduleItemsFragment.SessionsQuery.TRACK_COLOR);
			final String roomId = data
					.getString(SessionScheduleItemsFragment.SessionsQuery.ROOM_ID);

			final SessionScheduleItem session = new SessionScheduleItem();
			session.id = sessionId;
			session.title = title;
			session.roomId = roomId;
			session.startMillis = start;
			session.endMillis = end;
			session.containsStarred = false;
			session.accentColor = trackColor;

			sessionsList.add(session);

		} while (data.moveToNext());

		computePositions(sessionsList);
	}

}
