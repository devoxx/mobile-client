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
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Sessions;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.ObservableScrollView;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.ScheduleItem;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.ScheduleItemView;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.ScheduleItemsLayout;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.SessionScheduleItem;
import net.peterkuterna.android.apps.devoxxsched.util.AnalyticsUtils;
import net.peterkuterna.android.apps.devoxxsched.util.Lists;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;

public class SessionScheduleItemsFragment extends ScheduleItemsFragment
		implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {

	private static final String TAG = "SessionScheduleItemsFragment";

	public static SessionScheduleItemsFragment newInstance(long currentTime) {
		SessionScheduleItemsFragment f = new SessionScheduleItemsFragment();

		Bundle args = new Bundle();
		args.putLong("currentTime", currentTime);
		f.setArguments(args);

		return f;
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
	}

	protected void setupDay(ViewGroup rootView) {
		final long currentTime = getArguments().getLong("currentTime");

		mDay = new Day();

		// Setup data
		for (int i = UIUtils.NUMBER_DAYS; i > 0; i--) {
			if (currentTime >= UIUtils.START_DAYS_IN_MILLIS[i - 1]) {
				mDay.index = (i - 1);
				break;
			}
		}
		mDay.timeStart = UIUtils.START_DAYS_IN_MILLIS[mDay.index];
		mDay.timeEnd = mDay.timeStart + DateUtils.DAY_IN_MILLIS;
		mDay.loaderUri = CfpContract.Sessions.buildSessionsBetweenDirUri(
				mDay.timeStart, mDay.timeEnd);

		// Setup views
		mDay.rootView = rootView;

		mDay.scrollView = (ObservableScrollView) mDay.rootView
				.findViewById(R.id.schedule_items_scroll);

		mDay.scheduleItemsView = (ScheduleItemsLayout) mDay.rootView
				.findViewById(R.id.schedule_items);
		mDay.nowView = mDay.rootView.findViewById(R.id.schedule_items_now);

		// mDay.blocksView.setDrawingCacheEnabled(true);
		// mDay.blocksView.setAlwaysDrawnWithCacheEnabled(true);

		TimeZone.setDefault(UIUtils.CONFERENCE_TIME_ZONE);
	}

	/** {@inheritDoc} */
	public void onClick(View view) {
		if (view instanceof ScheduleItemView) {
			final ScheduleItemView itemView = (ScheduleItemView) view;
			final ScheduleItem item = itemView.getScheduleItem();
			final String title = itemView.getText().toString();
			AnalyticsUtils.getInstance(getActivity()).trackEvent("Schedule",
					"Session Click", title, 0);
			final String sessionId = item.getId();
			final Uri sessionsUri = CfpContract.Sessions
					.buildSessionUri(sessionId);
			final Intent intent = new Intent(Intent.ACTION_VIEW, sessionsUri);
			intent.putExtra(Intent.EXTRA_TITLE, title);
			((BaseActivity) getSupportActivity())
					.openActivityOrFragment(intent);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), mDay.loaderUri,
				SessionsQuery.PROJECTION, Sessions.SESSION_STARRED + "=1",
				null, CfpContract.Sessions.DEFAULT_SORT);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (data.moveToFirst()) {
			mDay.scheduleItemsView.removeAllBlocks();

			final ArrayList<SessionScheduleItem> sessionsList = Lists
					.newArrayList();
			SessionScheduleItem.loadSessions(data, sessionsList);
			for (SessionScheduleItem session : sessionsList) {
				final ScheduleItemView view = new ScheduleItemView(
						getActivity(), session);

				view.setOnClickListener(this);

				mDay.scheduleItemsView.addBlock(view);
			}
		}

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
		mDay.scheduleItemsView.removeAllBlocks();
	}

	private ContentObserver mSessionChangesObserver = new ContentObserver(
			new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			if (getActivity() != null) {
				getLoaderManager().restartLoader(getArguments().getInt("id"), null,
						SessionScheduleItemsFragment.this);
			}
		}
	};

	/**
	 * {@link Sessions} query parameters.
	 */
	public interface SessionsQuery {
		String[] PROJECTION = { BaseColumns._ID,
				CfpContract.Sessions.SESSION_ID,
				CfpContract.Sessions.SESSION_TITLE,
				CfpContract.Blocks.BLOCK_START, CfpContract.Blocks.BLOCK_END,
				CfpContract.Rooms.ROOM_ID,
				CfpContract.Tracks.TRACK_COLOR, };

		int _ID = 0;
		int SESSION_ID = 1;
		int SESSION_TITLE = 2;
		int BLOCK_START = 3;
		int BLOCK_END = 4;
		int ROOM_ID = 5;
		int TRACK_COLOR = 6;
	}

}
