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
import java.util.HashMap;

import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.ui.BlockScheduleItemsFragment;
import net.peterkuterna.android.apps.devoxxsched.util.Maps;
import net.peterkuterna.android.apps.devoxxsched.util.ParserUtils;
import android.content.Context;
import android.database.Cursor;

public class BlockScheduleItem extends ScheduleItem {

	public static final String BLOCK_KIND_REGISTRATION = "Registration";
	public static final String BLOCK_KIND_BREAK = "Break";
	public static final String BLOCK_KIND_COFFEE_BREAK = "Coffee Break";
	public static final String BLOCK_KIND_LUNCH = "Lunch";
	public static final String BLOCK_KIND_BREAKFAST = "Breakfast";
	public static final String BLOCK_KIND_KEYNOTE = "Keynote";
	public static final String BLOCK_KIND_TALK = "Talk";

	public static final String BLOCK_TYPE_UNIVERSITY = "University";
	public static final String BLOCK_TYPE_CONFERENCE = "Conference";
	public static final String BLOCK_TYPE_QUICKIE = "Quickie";
	public static final String BLOCK_TYPE_TOOLS_IN_ACTION = "Tools in Action";
	public static final String BLOCK_TYPE_BOF = "BOF";
	public static final String BLOCK_TYPE_HANDS_ON_LAB = "Hands-on Labs";

	public static final int COLOR_REGISTRATION = 0;
	public static final int COLOR_BREAK = 1;
	public static final int COLOR_KEYNOTE = 2;
	public static final int COLOR_UNIVERSITY = 3;
	public static final int COLOR_CONFERENCE = 4;
	public static final int COLOR_TOOLS_IN_ACTION = 5;
	public static final int COLOR_QUICKIE = 6;
	public static final int COLOR_HANDS_ON_LAB = 7;
	public static final int COLOR_BOF = 8;

	private static final HashMap<String, Integer> sKindMap = buildKindMap();
	private static final HashMap<String, Integer> sTypeMap = buildTypeMap();

	int sessionsCount;
	String sessionId; // the single session id in case we only have one session
	int type;

	private static HashMap<String, Integer> buildKindMap() {
		final HashMap<String, Integer> map = Maps.newHashMap();
		map.put(BLOCK_KIND_REGISTRATION, COLOR_REGISTRATION);
		map.put(BLOCK_KIND_BREAK, COLOR_BREAK);
		map.put(BLOCK_KIND_COFFEE_BREAK, COLOR_BREAK);
		map.put(BLOCK_KIND_BREAKFAST, COLOR_BREAK);
		map.put(BLOCK_KIND_LUNCH, COLOR_BREAK);
		map.put(BLOCK_KIND_KEYNOTE, COLOR_KEYNOTE);
		return map;
	}

	private static HashMap<String, Integer> buildTypeMap() {
		final HashMap<String, Integer> map = Maps.newHashMap();
		map.put(BLOCK_TYPE_UNIVERSITY, COLOR_UNIVERSITY);
		map.put(BLOCK_TYPE_CONFERENCE, COLOR_CONFERENCE);
		map.put(BLOCK_TYPE_TOOLS_IN_ACTION, COLOR_TOOLS_IN_ACTION);
		map.put(BLOCK_TYPE_QUICKIE, COLOR_QUICKIE);
		map.put(BLOCK_TYPE_HANDS_ON_LAB, COLOR_HANDS_ON_LAB);
		map.put(BLOCK_TYPE_BOF, COLOR_BOF);
		return map;
	}

	public static void loadBlocks(Context context, Cursor data,
			ArrayList<BlockScheduleItem> blocksList) {
		if (data == null) {
			return;
		}

		do {
			final String code = data
					.getString(BlockScheduleItemsFragment.BlocksQuery.BLOCK_CODE);
			final String kind = data
					.getString(BlockScheduleItemsFragment.BlocksQuery.BLOCK_KIND);
			final String type = data
					.getString(BlockScheduleItemsFragment.BlocksQuery.BLOCK_TYPE);

			Integer columnType = null;
			if (ParserUtils.isTalk(code)) {
				columnType = sTypeMap.get(type);
			} else {
				columnType = sKindMap.get(kind);
			}

			// TODO: place random blocks at bottom of entire layout
			if (columnType == null) {
				continue;
			}

			final String blockId = data
					.getString(BlockScheduleItemsFragment.BlocksQuery.BLOCK_ID);
			final String title = data
					.getString(BlockScheduleItemsFragment.BlocksQuery.BLOCK_TITLE);
			final long start = data
					.getLong(BlockScheduleItemsFragment.BlocksQuery.BLOCK_START);
			final long end = data.getLong(BlockScheduleItemsFragment.BlocksQuery.BLOCK_END);
			final boolean containsStarred = data
					.getInt(BlockScheduleItemsFragment.BlocksQuery.CONTAINS_STARRED) != 0;
			final int sessionsCount = data
					.getInt(BlockScheduleItemsFragment.BlocksQuery.SESSIONS_COUNT);

			final BlockScheduleItem block = new BlockScheduleItem();
			block.id = blockId;
			block.title = title;
			block.roomId = null;
			block.startMillis = start;
			block.endMillis = end;
			block.containsStarred = containsStarred;
			block.sessionsCount = sessionsCount;
			block.type = columnType.intValue();
			block.accentColor = getAccentColor(context, block);

			blocksList.add(block);

		} while (data.moveToNext());

		computePositions(blocksList);
	}

	private static int getAccentColor(Context context, BlockScheduleItem block) {
		int accentColor = -1;
		if (context != null) {
			switch (block.type) {
			case BlockScheduleItem.COLOR_REGISTRATION:
				accentColor = context.getResources().getColor(
						R.color.block_registration);
				break;
			case BlockScheduleItem.COLOR_BREAK:
				accentColor = context.getResources().getColor(
						R.color.block_break);
				break;
			case BlockScheduleItem.COLOR_KEYNOTE:
				accentColor = context.getResources().getColor(
						R.color.block_keynote);
				break;
			case BlockScheduleItem.COLOR_UNIVERSITY:
				accentColor = context.getResources().getColor(
						R.color.block_university);
				break;
			case BlockScheduleItem.COLOR_CONFERENCE:
				accentColor = context.getResources().getColor(
						R.color.block_conference);
				break;
			case BlockScheduleItem.COLOR_TOOLS_IN_ACTION:
				accentColor = context.getResources().getColor(
						R.color.block_tools_in_action);
				break;
			case BlockScheduleItem.COLOR_QUICKIE:
				accentColor = context.getResources().getColor(
						R.color.block_quickie);
				break;
			case BlockScheduleItem.COLOR_HANDS_ON_LAB:
				accentColor = context.getResources().getColor(
						R.color.block_hands_on_lab);
				break;
			case BlockScheduleItem.COLOR_BOF:
				accentColor = context.getResources()
						.getColor(R.color.block_bof);
				break;
			}
		}
		return accentColor;
	}

	public static void updateSessionId(BlockScheduleItem block, String sessionId) {
		block.sessionId = sessionId;
	}

	public int getSessionsCount() {
		return sessionsCount;
	}

}
