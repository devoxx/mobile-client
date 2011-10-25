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

import java.util.TimeZone;

import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Blocks;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.text.format.DateUtils;
import android.widget.Button;

/**
 * Custom view that represents a {@link Blocks#BLOCK_ID} instance, including its
 * title and time span that it occupies. Usually organized automatically by
 * {@link BlocksLayout} to match up against a {@link TimeRulerView} instance.
 */
public class BlockView extends Button {
	private static final int TIME_STRING_FLAGS = DateUtils.FORMAT_SHOW_DATE
			| DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY
			| DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_TIME;

	private final Block mBlock;

	public BlockView(Context context, Block block) {
		super(context);

		mBlock = block;

		setText(mBlock.title);

		// TODO: turn into color state list with layers?
		int textColor = Color.WHITE;
		int accentColor = -1;
		switch (mBlock.type) {
		case Block.COLOR_REGISTRATION:
			accentColor = getResources().getColor(R.color.block_registration);
			break;
		case Block.COLOR_BREAK:
			accentColor = getResources().getColor(R.color.block_break);
			break;
		case Block.COLOR_KEYNOTE:
			accentColor = getResources().getColor(R.color.block_keynote);
			break;
		case Block.COLOR_UNIVERSITY:
			accentColor = getResources().getColor(R.color.block_university);
			break;
		case Block.COLOR_CONFERENCE:
			accentColor = getResources().getColor(R.color.block_conference);
			break;
		case Block.COLOR_TOOLS_IN_ACTION:
			accentColor = getResources()
					.getColor(R.color.block_tools_in_action);
			break;
		case Block.COLOR_QUICKIE:
			accentColor = getResources().getColor(R.color.block_quickie);
			break;
		case Block.COLOR_HANDS_ON_LAB:
			accentColor = getResources().getColor(R.color.block_hands_on_lab);
			break;
		case Block.COLOR_BOF:
			accentColor = getResources().getColor(R.color.block_bof);
			break;
		}

		LayerDrawable buttonDrawable = (LayerDrawable) context.getResources()
				.getDrawable(R.drawable.btn_block);
		buttonDrawable.getDrawable(0).setColorFilter(accentColor,
				PorterDuff.Mode.SRC_ATOP);
		buttonDrawable.getDrawable(1)
				.setAlpha(mBlock.containsStarred ? 255 : 0);

		setTextColor(textColor);
		setBackgroundDrawable(buttonDrawable);
	}

	public Block getBlock() {
		return mBlock;
	}

	public String getBlockTimeString() {
		TimeZone.setDefault(UIUtils.CONFERENCE_TIME_ZONE);
		return DateUtils.formatDateTime(getContext(), mBlock.startMillis,
				TIME_STRING_FLAGS);
	}

}
