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
import java.util.Iterator;

import net.peterkuterna.android.apps.devoxxsched.util.Lists;

public class ScheduleItem {

	String id;
	String title;
	String roomId;
	boolean containsStarred;
	long startMillis; // UTC milliseconds since the epoch
	long endMillis; // UTC milliseconds since the epoch
	int column;
	int maxColumns;
	int accentColor;

	/**
	 * Taken from the Calendar app in AOSP.
	 */
	protected static void computePositions(ArrayList<? extends ScheduleItem> blocksList) {
		ArrayList<ScheduleItem> activeList = Lists.newArrayList();
		ArrayList<ScheduleItem> groupList = Lists.newArrayList();

		long colMask = 0;
		int maxCols = 0;
		for (ScheduleItem event : blocksList) {
			long start = event.startMillis;

			// Remove the inactive events.
			// An event on the active list becomes inactive when its end time +
			// margin time is less
			// than or equal to the current event's start time. For more
			// information about
			// the margin time, see the comment in EVENT_OVERWRAP_MARGIN_TIME.
			Iterator<? extends ScheduleItem> iter = activeList.iterator();
			while (iter.hasNext()) {
				ScheduleItem active = iter.next();
				final long duration = active.endMillis - active.startMillis;
				if ((active.startMillis + duration) <= start) {
					colMask &= ~(1L << active.column);
					iter.remove();
				}
			}

			// If the active list is empty, then reset the max columns, clear
			// the column bit mask, and empty the groupList.
			if (activeList.isEmpty()) {
				for (ScheduleItem ev : groupList) {
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
		for (ScheduleItem ev : groupList) {
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
	
	public String getId() {
		return id;
	}

}
