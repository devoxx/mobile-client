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

package net.peterkuterna.android.apps.devoxxsched.ui;

import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.ObservableScrollView;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.ObservableScrollView.OnScrollListener;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.ScheduleItemsLayout;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ScrollView;

public abstract class ScheduleItemsFragment extends ProgressFragment {

	private static final String TAG = "ScheduleItemsFragment";

	private ViewGroup mRootView;

	/**
	 * Flags used with {@link android.text.format.DateUtils#formatDateRange}.
	 */
	protected static final int TIME_FLAGS = DateUtils.FORMAT_SHOW_DATE
			| DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY;

	protected static final int DISABLED_BLOCK_ALPHA = 100;

	private OnScrollListener mOnScrollListener;
	private int mScrollY;

	protected class Day {
		ViewGroup rootView;
		ObservableScrollView scrollView;
		View nowView;
		ScheduleItemsLayout scheduleItemsView;

		int index = -1;
		String label = null;
		Uri loaderUri = null;
		long timeStart = -1;
		long timeEnd = -1;
	}

	protected Day mDay;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater,
			ViewGroup container, Bundle savedInstanceState) {
		mRootView = (ViewGroup) inflater.inflate(
				R.layout.fragment_schedule_items_content, null);

		setupDay(mRootView);

		if (mOnScrollListener != null) {
			mDay.scrollView.setOnScrollListener(mOnScrollListener);
		}

		mDay.scrollView.getViewTreeObserver().addOnGlobalLayoutListener(
				mOnGlobalLayoutListener);

		return mRootView;
	}

	private final OnGlobalLayoutListener mOnGlobalLayoutListener = new OnGlobalLayoutListener() {
		@Override
		public void onGlobalLayout() {
			if (mDay.scrollView.isShown()) {
				mDay.scrollView.getViewTreeObserver()
						.removeGlobalOnLayoutListener(mOnGlobalLayoutListener);

				if (getActivity() != null) {
					getActivity().runOnUiThread(new Runnable() {
						public void run() {
							updateNowView(true);
							if (mScrollY != -1) {
								mDay.scrollView.scrollTo(0, mScrollY);
							}
						}
					});
				}
			}
		}
	};

	@Override
	int getContentResourceId() {
		return R.id.schedule_items_scroll;
	}

	@Override
	public void onPause() {
		super.onPause();

		getActivity().unregisterReceiver(mReceiver);
	}

	@Override
	public void onResume() {
		super.onResume();

		// Start listening for time updates to adjust "now" bar. TIME_TICK is
		// triggered once per minute, which is how we move the bar over time.
		final IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_TIME_TICK);
		filter.addAction(Intent.ACTION_TIME_CHANGED);
		filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		getActivity().registerReceiver(mReceiver, filter, null, new Handler());
	}

	/**
	 * Update position and visibility of "now" view.
	 */
	private boolean updateNowView(boolean forceScroll) {
		final long now = UIUtils.getCurrentTime(getActivity());

		Day nowDay = null; // effectively Day corresponding to today
		if (now >= mDay.timeStart && now <= mDay.timeEnd) {
			nowDay = mDay;
			mDay.nowView.setVisibility(View.VISIBLE);
		} else {
			mDay.nowView.setVisibility(View.GONE);
		}

		if (nowDay != null && forceScroll) {
			final Day finalDay = nowDay;
			if (finalDay.scrollView.isShown()) {
				final int offset = finalDay.scrollView.getHeight() / 2;
				finalDay.nowView.post(new Runnable() {
					@Override
					public void run() {
						finalDay.nowView.requestRectangleOnScreen(new Rect(0,
								offset, 0, offset), true);
					}
				});
				finalDay.scheduleItemsView.requestLayout();
			}
			return true;
		}

		return false;
	}

	public void setOnScrollListener(OnScrollListener listener) {
		mOnScrollListener = listener;
		if (mDay != null && mDay.scrollView != null) {
			mDay.scrollView.setOnScrollListener(mOnScrollListener);
		}
	}

	public void setScrollY(int scrollY) {
		mScrollY = scrollY;
		if (mDay != null && mDay.scrollView != null) {
			mDay.scrollView.scrollTo(0, scrollY);
		}
	}

	public ScrollView getScrollView() {
		if (mDay != null) {
			return mDay.scrollView;
		}
		return null;
	}

	protected abstract void setupDay(ViewGroup rootView);

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateNowView(false);
		}
	};

}
