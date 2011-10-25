/*
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

import android.content.Context;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ScrollableTabs extends HorizontalScrollView implements
		OnPageChangeListener {

	private LinearLayout mHost;
	private ScrollableTabsAdapter mAdapter;
	private int mCurrentTab = -1;

	public ScrollableTabs(Context context) {
		this(context, null);
	}

	public ScrollableTabs(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ScrollableTabs(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		init();
	}

	private void init() {
		mHost = new LinearLayout(getContext());
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT);
		addView(mHost, layoutParams);

		setHorizontalScrollBarEnabled(false);
		setFillViewport(true);
	}

	public void setAdapter(ScrollableTabsAdapter adapter) {
		if (mAdapter != null) {
			// TODO: data set observer
		}

		mAdapter = adapter;

		// clean up our childs
		mHost.removeAllViews();

		if (mAdapter != null) {
			final int count = mAdapter.getCount();

			// add the child text views
			for (int i = 0; i < count; i++) {
				mHost.addView(mAdapter.getTab(i, mHost), new LayoutParams(
						ViewGroup.LayoutParams.WRAP_CONTENT,
						ViewGroup.LayoutParams.MATCH_PARENT));
			}

			if (count > 0) {
				focusTab(0);
				// mCurrentTab = 0;
			}

			requestLayout();
			invalidate();
		}
	}

	public void focusTab(int index) {
		final int tabCount = mHost.getChildCount();

		if (index < 0 || index > tabCount - 1) {
			return;
		}

		if (mCurrentTab != index) {
			final TextView tvOld = (TextView) mHost.getChildAt(mCurrentTab);
			if (tvOld != null) {
				tvOld.setSelected(false);
			}

			mCurrentTab = index;

			final TextView tvNew = (TextView) mHost.getChildAt(index);
			if (tvNew != null) {
				int right = tvNew.getRight();
				int left = tvNew.getLeft() + right;
				int width = getWidth();
				int i = (left - width) / 2;
				tvNew.setSelected(true);
				smoothScrollTo(i, 0);
			}
		}
	}

	@Override
	public void onPageScrollStateChanged(int state) {
	}

	@Override
	public void onPageScrolled(int position, float positionOffset,
			int positionOffsetPixels) {
	}

	@Override
	public void onPageSelected(int position) {
		focusTab(position);
	}

}
