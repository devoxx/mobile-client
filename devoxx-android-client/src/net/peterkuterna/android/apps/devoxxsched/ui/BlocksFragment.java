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

import java.util.ArrayList;
import java.util.TimeZone;

import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Blocks;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.Block;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.BlockView;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.BlocksLayout;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.ObservableScrollView;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.ObservableScrollView.OnScrollListener;
import net.peterkuterna.android.apps.devoxxsched.util.AnalyticsUtils;
import net.peterkuterna.android.apps.devoxxsched.util.Lists;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ScrollView;

public class BlocksFragment extends ProgressFragment implements
		LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {

	private static final String TAG = "BlocksFragment";

	private OnScrollListener mOnScrollListener;
	private int mScrollY;
	private ViewGroup mRootView;

	/**
	 * Flags used with {@link android.text.format.DateUtils#formatDateRange}.
	 */
	private static final int TIME_FLAGS = DateUtils.FORMAT_SHOW_DATE
			| DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY;

	private static final int DISABLED_BLOCK_ALPHA = 100;

	private class Day {
		private ViewGroup rootView;
		private ObservableScrollView scrollView;
		private View nowView;
		private BlocksLayout blocksView;

		private int index = -1;
		private String label = null;
		private Uri blocksUri = null;
		private long timeStart = -1;
		private long timeEnd = -1;
	}

	private Day mDay;

	public static BlocksFragment newInstance(int id, long startMillis,
			int scrollY) {
		BlocksFragment f = new BlocksFragment();

		Bundle args = new Bundle();
		args.putInt("id", id);
		args.putLong("startMillis", startMillis);
		f.setArguments(args);

		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater,
			ViewGroup container, Bundle savedInstanceState) {
		mRootView = (ViewGroup) inflater.inflate(
				R.layout.fragment_blocks_content, null);

		final int position = getArguments().getInt("id");
		final long startMillis = getArguments().getLong("startMillis");
		setupDay(mRootView, position, startMillis);

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
		return R.id.blocks_scroll;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setContentShown(false);

		getLoaderManager().initLoader(getArguments().getInt("id"), null, this);
	}

	@Override
	public void onPause() {
		super.onPause();

		getActivity().unregisterReceiver(mReceiver);
		getActivity().getContentResolver().unregisterContentObserver(
				mSessionChangesObserver);
	}

	@Override
	public void onResume() {
		super.onResume();

		getLoaderManager().restartLoader(mDay.index, null, this);

		getActivity().getContentResolver()
				.registerContentObserver(CfpContract.Sessions.CONTENT_URI,
						true, mSessionChangesObserver);

		// Start listening for time updates to adjust "now" bar. TIME_TICK is
		// triggered once per minute, which is how we move the bar over time.
		final IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_TIME_TICK);
		filter.addAction(Intent.ACTION_TIME_CHANGED);
		filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		getActivity().registerReceiver(mReceiver, filter, null, new Handler());
	}

	private void setupDay(ViewGroup rootView, int position, long startMillis) {
		mDay = new Day();

		// Setup data
		mDay.index = position;
		mDay.timeStart = startMillis;
		mDay.timeEnd = startMillis + DateUtils.DAY_IN_MILLIS;
		mDay.blocksUri = CfpContract.Blocks.buildBlocksBetweenDirUri(
				mDay.timeStart, mDay.timeEnd);

		// Setup views
		mDay.rootView = rootView;

		mDay.scrollView = (ObservableScrollView) mDay.rootView
				.findViewById(R.id.blocks_scroll);

		mDay.blocksView = (BlocksLayout) mDay.rootView
				.findViewById(R.id.blocks);
		mDay.nowView = mDay.rootView.findViewById(R.id.blocks_now);

		mDay.blocksView.setDrawingCacheEnabled(true);
		mDay.blocksView.setAlwaysDrawnWithCacheEnabled(true);

		TimeZone.setDefault(UIUtils.CONFERENCE_TIME_ZONE);
		mDay.label = DateUtils.formatDateTime(getActivity(), startMillis,
				TIME_FLAGS);
	}

	/** {@inheritDoc} */
	public void onClick(View view) {
		if (view instanceof BlockView) {
			final String title = ((BlockView) view).getText().toString();
			AnalyticsUtils.getInstance(getActivity()).trackEvent("Schedule",
					"Session Click", title, 0);
			final String blockId = ((BlockView) view).getBlock().getBlockId();
			final Uri sessionsUri = CfpContract.Blocks
					.buildSessionsUri(blockId);
			final Intent intent = new Intent(Intent.ACTION_VIEW, sessionsUri);
			intent.putExtra(Intent.EXTRA_TITLE, title);
			((BaseActivity) getSupportActivity())
					.openActivityOrFragment(intent);
		}
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
				finalDay.blocksView.requestLayout();
			}
			return true;
		}

		return false;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), mDay.blocksUri,
				BlocksQuery.PROJECTION, null, null,
				CfpContract.Blocks.DEFAULT_SORT);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (!data.moveToFirst()) {
			return;
		}

		mDay.blocksView.removeAllBlocks();

		final ArrayList<Block> blocksList = Lists.newArrayList();
		Block.loadBlocks(data, blocksList);
		for (Block block : blocksList) {
			final BlockView blockView = new BlockView(getActivity(), block);

			if (block.getSessionsCount() > 0) {
				blockView.setOnClickListener(this);
			} else {
				blockView.setFocusable(false);
				blockView.setEnabled(false);
				LayerDrawable buttonDrawable = (LayerDrawable) blockView
						.getBackground();
				buttonDrawable.getDrawable(0).setAlpha(DISABLED_BLOCK_ALPHA);
				buttonDrawable.getDrawable(2).setAlpha(DISABLED_BLOCK_ALPHA);
			}

			mDay.blocksView.addBlock(blockView);
		}

		mDay.rootView.postInvalidate();

		if (getView() != null) {
			if (isResumed()) {
				setContentShown(true);
			} else {
				setContentShownNoAnimation(true);
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mDay.blocksView.removeAllBlocks();
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

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateNowView(false);
		}
	};

	private ContentObserver mSessionChangesObserver = new ContentObserver(
			new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			getLoaderManager().restartLoader(getArguments().getInt("id"), null,
					BlocksFragment.this);
		}
	};

	/**
	 * {@link Blocks} query parameters.
	 */
	public interface BlocksQuery {
		String[] PROJECTION = { BaseColumns._ID, CfpContract.Blocks.BLOCK_ID,
				CfpContract.Blocks.BLOCK_TITLE, CfpContract.Blocks.BLOCK_START,
				CfpContract.Blocks.BLOCK_END, CfpContract.Blocks.BLOCK_KIND,
				CfpContract.Blocks.BLOCK_TYPE, CfpContract.Blocks.BLOCK_CODE,
				CfpContract.Blocks.SESSIONS_COUNT,
				CfpContract.Blocks.CONTAINS_STARRED, };

		int _ID = 0;
		int BLOCK_ID = 1;
		int BLOCK_TITLE = 2;
		int BLOCK_START = 3;
		int BLOCK_END = 4;
		int BLOCK_KIND = 5;
		int BLOCK_TYPE = 6;
		int BLOCK_CODE = 7;
		int SESSIONS_COUNT = 8;
		int CONTAINS_STARRED = 9;
	}

}
