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

import net.peterkuterna.android.apps.devoxxsched.R;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.util.TypedValue;
import android.widget.Button;

/**
 * Custom view that represents a {@link ScheduleItem} instance, including its
 * title and time span that it occupies. Usually organized automatically by
 * {@link ScheduleItemsLayout} to match up against a {@link TimeRulerView}
 * instance.
 */
public class ScheduleItemView extends Button {

	private ScheduleItem mItem;

	public ScheduleItemView(Context context, ScheduleItem item) {
		super(context);

		mItem = item;

		setText(item.title);

		// TODO: turn into color state list with layers?
		int textColor = Color.WHITE;
		int accentColor = item.accentColor;

		LayerDrawable buttonDrawable = (LayerDrawable) context.getResources()
				.getDrawable(R.drawable.btn_block);
		buttonDrawable.getDrawable(0).setColorFilter(accentColor,
				PorterDuff.Mode.SRC_ATOP);
		buttonDrawable.getDrawable(1).setAlpha(item.containsStarred ? 255 : 0);
		buttonDrawable.getDrawable(2).setAlpha(
				"room3".equals(item.roomId) ? 255 : 0);
		buttonDrawable.getDrawable(3).setAlpha(
				"room4".equals(item.roomId) ? 255 : 0);
		buttonDrawable.getDrawable(4).setAlpha(
				"room5".equals(item.roomId) ? 255 : 0);
		buttonDrawable.getDrawable(5).setAlpha(
				"room6".equals(item.roomId) ? 255 : 0);
		buttonDrawable.getDrawable(6).setAlpha(
				"room7".equals(item.roomId) ? 255 : 0);
		buttonDrawable.getDrawable(7).setAlpha(
				"room8".equals(item.roomId) ? 255 : 0);
		buttonDrawable.getDrawable(8).setAlpha(
				"room9".equals(item.roomId) ? 255 : 0);
		buttonDrawable.getDrawable(9).setAlpha(
				"bof1".equals(item.roomId) ? 255 : 0);
		buttonDrawable.getDrawable(10).setAlpha(
				"bof2".equals(item.roomId) ? 255 : 0);

		setTextColor(textColor);
		setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources()
				.getDimensionPixelSize(R.dimen.text_size_small));

		setBackgroundDrawable(buttonDrawable);
	}

	public ScheduleItem getScheduleItem() {
		return mItem;
	}

}
