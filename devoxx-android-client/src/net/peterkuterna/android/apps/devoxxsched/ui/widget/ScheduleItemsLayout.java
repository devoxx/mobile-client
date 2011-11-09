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
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Custom layout that contains and organizes a {@link TimeRulerView} and several
 * instances of {@link ScheduleItemView}. Also positions current "now" divider using
 * {@link R.id#schedule_items_now} view when applicable.
 */
public class ScheduleItemsLayout extends ViewGroup {

	private TimeRulerView mRulerView;
	private View mNowView;

	public ScheduleItemsLayout(Context context) {
		this(context, null);
	}

	public ScheduleItemsLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private void ensureChildren() {
		mRulerView = (TimeRulerView) findViewById(R.id.schedule_items_ruler);
		if (mRulerView == null) {
			throw new IllegalStateException(
					"Must include a R.id.schedule_items_rules view.");
		}
		// mRulerView.setDrawingCacheEnabled(true);

		mNowView = findViewById(R.id.schedule_items_now);
		if (mNowView == null) {
			throw new IllegalStateException(
					"Must include a R.id.schedule_items_now view.");
		}
		// mNowView.setDrawingCacheEnabled(true);
	}

	/**
	 * Remove any {@link BlockView} instances, leaving only
	 * {@link TimeRulerView} remaining.
	 */
	public void removeAllBlocks() {
		ensureChildren();
		removeAllViews();
		addView(mRulerView);
		addView(mNowView);
	}

	public void addBlock(ScheduleItemView scheduleItemView) {
		// blockView.setDrawingCacheEnabled(true);
		addView(scheduleItemView, 1);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		ensureChildren();

		mRulerView.measure(widthMeasureSpec, heightMeasureSpec);
		mNowView.measure(widthMeasureSpec, heightMeasureSpec);

		final int width = mRulerView.getMeasuredWidth();
		final int height = mRulerView.getMeasuredHeight();

		setMeasuredDimension(resolveSize(width, widthMeasureSpec),
				resolveSize(height, heightMeasureSpec));
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		ensureChildren();

		final TimeRulerView rulerView = mRulerView;
		final int headerWidth = rulerView.getHeaderWidth();

		rulerView.layout(0, 0, getWidth(), getHeight());

		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() == GONE)
				continue;

			if (child instanceof ScheduleItemView) {
				final ScheduleItemView view = (ScheduleItemView) child;
				final ScheduleItem item = view.getScheduleItem();
				final int columnWidth = (getWidth() - headerWidth)
						/ item.maxColumns;
				final int top = rulerView
						.getTimeVerticalOffset(item.startMillis);
				final int bottom = rulerView
						.getTimeVerticalOffset(item.endMillis);
				final int left = headerWidth + (item.column * columnWidth);
				final int right = left + columnWidth;
				view.layout(left, top, right, bottom);
			}
		}

		// Align now view to match current time
		final View nowView = mNowView;
		final long now = UIUtils.getCurrentTime(getContext());

		final int top = rulerView.getTimeVerticalOffset(now);
		final int bottom = top + nowView.getMeasuredHeight();
		final int left = 0;
		final int right = getWidth();

		nowView.layout(left, top, right, bottom);
	}
}
