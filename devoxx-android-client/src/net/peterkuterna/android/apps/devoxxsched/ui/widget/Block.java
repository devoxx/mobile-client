/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (C) 2011 Peter Kuterna
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
import java.util.Iterator;

import net.peterkuterna.android.apps.devoxxsched.ui.BlocksFragment;
import net.peterkuterna.android.apps.devoxxsched.util.Lists;
import net.peterkuterna.android.apps.devoxxsched.util.Maps;
import net.peterkuterna.android.apps.devoxxsched.util.ParserUtils;
import android.database.Cursor;

public class Block {

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

	String blockId;
	String title;
	boolean containsStarred;
	int sessionsCount;
	long startMillis; // UTC milliseconds since the epoch
	long endMillis; // UTC milliseconds since the epoch
	int column;
	int maxColumns;
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

	public static void loadBlocks(Cursor data, ArrayList<Block> blocksList) {
		if (data == null) {
			return;
		}

		do {
			final String code = data
					.getString(BlocksFragment.BlocksQuery.BLOCK_CODE);
			final String kind = data
					.getString(BlocksFragment.BlocksQuery.BLOCK_KIND);
			final String type = data
					.getString(BlocksFragment.BlocksQuery.BLOCK_TYPE);

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
					.getString(BlocksFragment.BlocksQuery.BLOCK_ID);
			final String title = data
					.getString(BlocksFragment.BlocksQuery.BLOCK_TITLE);
			final long start = data
					.getLong(BlocksFragment.BlocksQuery.BLOCK_START);
			final long end = data.getLong(BlocksFragment.BlocksQuery.BLOCK_END);
			final boolean containsStarred = data
					.getInt(BlocksFragment.BlocksQuery.CONTAINS_STARRED) != 0;
			final int sessionsCount = data
					.getInt(BlocksFragment.BlocksQuery.SESSIONS_COUNT);

			final Block block = new Block();
			block.blockId = blockId;
			block.title = title;
			block.startMillis = start;
			block.endMillis = end;
			block.containsStarred = containsStarred;
			block.sessionsCount = sessionsCount;
			block.type = columnType.intValue();

			blocksList.add(block);

		} while (data.moveToNext());

		computePositions(blocksList);
	}

	/**
	 * Taken from the Calendar app in AOSP.
	 */
	private static void computePositions(ArrayList<Block> blocksList) {
		ArrayList<Block> activeList = Lists.newArrayList();
		ArrayList<Block> groupList = Lists.newArrayList();

		long colMask = 0;
		int maxCols = 0;
		for (Block event : blocksList) {
			long start = event.startMillis;

			// Remove the inactive events.
			// An event on the active list becomes inactive when its end time +
			// margin time is less
			// than or equal to the current event's start time. For more
			// information about
			// the margin time, see the comment in EVENT_OVERWRAP_MARGIN_TIME.
			Iterator<Block> iter = activeList.iterator();
			while (iter.hasNext()) {
				Block active = iter.next();
				final long duration = active.endMillis - active.startMillis;
				if ((active.startMillis + duration) <= start) {
					colMask &= ~(1L << active.column);
					iter.remove();
				}
			}

			// If the active list is empty, then reset the max columns, clear
			// the column bit mask, and empty the groupList.
			if (activeList.isEmpty()) {
				for (Block ev : groupList) {
					ev.maxColumns = maxCols;
				}
				maxCols = 0;
				colMask = 0;
				groupList.clear();
			}

			// Find the first empty column. Empty columns are represented by
			// zero bits in the column mask "colMask".
			int col = findFirstZeroBit(colMask);
			if (col == 64)
				col = 63;
			colMask |= (1L << col);
			event.column = col;
			activeList.add(event);
			groupList.add(event);
			int len = activeList.size();
			if (maxCols < len)
				maxCols = len;
		}
		for (Block ev : groupList) {
			ev.maxColumns = maxCols;
		}
	}

	public static int findFirstZeroBit(long val) {
		for (int ii = 0; ii < 64; ++ii) {
			if ((val & (1L << ii)) == 0)
				return ii;
		}
		return 64;
	}

	public String getBlockId() {
		return blockId;
	}

	public int getSessionsCount() {
		return sessionsCount;
	}

}
