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
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Sessions;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Tracks;
import net.peterkuterna.android.apps.devoxxsched.util.ActivityHelper;
import net.peterkuterna.android.apps.devoxxsched.util.AnalyticsUtils;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.Spannable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

public class SessionsFragment extends ListFragment {

	private static final String STATE_CHECKED_POSITION = "checkedPosition";

	public static final String EXTRA_TRACK = "net.peterkuterna.android.apps.devoxxsched.extra.TRACK";
	public static final String EXTRA_TRACK_COLOR = "net.peterkuterna.android.apps.devoxxsched.extra.TRACK_COLOR";

	private CursorAdapter mAdapter;
	private int mCheckedPosition = -1;
	private Uri mSessionsUri;
	private long mNewSessionTimestamp = -1;
	private String mTrackName = null;
	private int mTrackColor = -1;

	private Handler mHandler = new Handler();
	private Handler mMessageQueueHandler = new Handler();

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setEmptyText(getString(R.string.empty_sessions));

		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		reloadFromArguments(getArguments(), false);

		setListShown(false);

		if (savedInstanceState != null) {
			mCheckedPosition = savedInstanceState.getInt(
					STATE_CHECKED_POSITION, -1);
		}
	}

	public void reloadFromArguments(Bundle arguments, boolean reset) {
		mCheckedPosition = -1;

		// Load new arguments
		final Intent intent = ActivityHelper
				.fragmentArgumentsToIntent(arguments);
		mSessionsUri = intent.getData();
		if (mSessionsUri == null) {
			return;
		}

		mNewSessionTimestamp = intent.getLongExtra(
				NewSessionsActivity.EXTRA_NEW_SESSION_TIMESTAMP, -1);

		final int sessionsQueryId;
		if (!CfpContract.Sessions.isSearchUri(mSessionsUri)) {
			mAdapter = new SessionsAdapter(getActivity(), null);
			sessionsQueryId = SessionsQuery._TOKEN;
		} else {
			mAdapter = new SearchAdapter(getActivity(), null);
			sessionsQueryId = SearchQuery._TOKEN;
		}

		setListAdapter(mAdapter);

		if (!reset) {
			getLoaderManager().initLoader(sessionsQueryId, null,
					mSessionsLoaderCallback);
		} else {
			getLoaderManager().restartLoader(sessionsQueryId, null,
					mSessionsLoaderCallback);
		}

		mTrackName = intent.getStringExtra(Intent.EXTRA_TITLE);
		mTrackColor = intent.getIntExtra(EXTRA_TRACK_COLOR, -1);
		final Uri trackUri = intent
				.getParcelableExtra(SessionsFragment.EXTRA_TRACK);
		if (!TextUtils.isEmpty(mTrackName)) {
			AnalyticsUtils.getInstance(getActivity()).trackPageView(
					"/Sessions/" + mTrackName);
			updateTrackData();
		}
		if (TextUtils.isEmpty(mTrackName) && mTrackColor == -1
				&& trackUri != null) {
			Bundle args = new Bundle();
			args.putParcelable("uri", trackUri);
			getLoaderManager().initLoader(TracksQuery._TOKEN, args,
					mTrackLoaderCallback);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		mMessageQueueHandler.post(mRefreshSessionsRunnable);

		getActivity().getContentResolver()
				.registerContentObserver(CfpContract.Sessions.CONTENT_URI,
						true, mSessionChangesObserver);

		if (mSessionsUri != null) {
			if (!CfpContract.Sessions.isSearchUri(mSessionsUri)) {
				getLoaderManager().restartLoader(SessionsQuery._TOKEN, null,
						mSessionsLoaderCallback);
			} else {
				getLoaderManager().restartLoader(SearchQuery._TOKEN, null,
						mSessionsLoaderCallback);
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		mMessageQueueHandler.removeCallbacks(mRefreshSessionsRunnable);
		getActivity().getContentResolver().unregisterContentObserver(
				mSessionChangesObserver);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_CHECKED_POSITION, mCheckedPosition);
	}

	/** {@inheritDoc} */
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// Launch viewer for specific session, passing along any track knowledge
		// that should influence the title-bar.
		final Cursor cursor = (Cursor) mAdapter.getItem(position);
		final String sessionId = cursor.getString(SessionsQuery.SESSION_ID);
		final String title = cursor.getString(SessionsQuery.SESSION_TITLE);
		AnalyticsUtils.getInstance(getActivity()).trackEvent("Sessions",
				"Click", title, 0);
		final Uri sessionUri = CfpContract.Sessions.buildSessionUri(sessionId);
		final Intent intent = new Intent(Intent.ACTION_VIEW, sessionUri);
		intent.putExtra(Intent.EXTRA_TITLE, mTrackName);
		intent.putExtra(SessionDetailFragment.EXTRA_TRACK_COLOR, mTrackColor);
		((BaseActivity) getSupportActivity()).openActivityOrFragment(intent);
		getListView().setItemChecked(position, true);
		mCheckedPosition = position;
	}

	public void clearCheckedPosition() {
		if (mCheckedPosition >= 0) {
			if (getView() != null) {
				getListView().setItemChecked(mCheckedPosition, false);
			}
			mCheckedPosition = -1;
		}
	}

	private void updateTrackData() {
		if (!UIUtils.isHoneycombTablet(getActivity())) {
			UIUtils.setActionBarData(getSupportActivity(), mTrackName,
					mTrackColor);
		}
	}

	private LoaderManager.LoaderCallbacks<Cursor> mSessionsLoaderCallback = new LoaderCallbacks<Cursor>() {

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			if (id == SessionsQuery._TOKEN) {
				final String selection = mNewSessionTimestamp != -1 ? (Sessions.SESSION_NEW_TIMESTAMP + "=?")
						: null;
				final String[] selectionArgs = mNewSessionTimestamp != -1 ? (new String[] { String
						.valueOf(mNewSessionTimestamp) }) : null;
				return new CursorLoader(getActivity(), mSessionsUri,
						SessionsQuery.PROJECTION, selection, selectionArgs,
						CfpContract.Sessions.DEFAULT_SORT);
			} else if (id == SearchQuery._TOKEN) {
				return new CursorLoader(getActivity(), mSessionsUri,
						SearchQuery.PROJECTION, null, null,
						CfpContract.Sessions.DEFAULT_SORT);
			}
			return null;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			mAdapter.swapCursor(data);

			if (getView() != null) {
				if (isResumed()) {
					setListShown(true);
				} else {
					setListShownNoAnimation(true);
				}
			}

			if (mCheckedPosition >= 0 && getView() != null) {
				getListView().setItemChecked(mCheckedPosition, true);
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			mAdapter.swapCursor(null);
		}

	};

	private LoaderManager.LoaderCallbacks<Cursor> mTrackLoaderCallback = new LoaderCallbacks<Cursor>() {

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			final Uri uri = args.getParcelable("uri");
			return new CursorLoader(getActivity(), uri, TracksQuery.PROJECTION,
					null, null, null);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			if (!data.moveToFirst()) {
				return;
			}

			mTrackName = data.getString(TracksQuery.TRACK_NAME);
			mTrackColor = data.getInt(TracksQuery.TRACK_COLOR);

			mHandler.post(new Runnable() {
				@Override
				public void run() {
					updateTrackData();
				}
			});

			AnalyticsUtils.getInstance(getActivity()).trackPageView(
					"/Sessions/" + mTrackName);
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
		}

	};

	private ContentObserver mSessionChangesObserver = new ContentObserver(
			new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			if (mSessionsUri != null) {
				if (CfpContract.Sessions.isSearchUri(mSessionsUri)) {
					getLoaderManager().restartLoader(SearchQuery._TOKEN, null,
							mSessionsLoaderCallback);
				} else if (!CfpContract.Sessions.isNewSessionsUri(mSessionsUri)) {
					getLoaderManager().restartLoader(SessionsQuery._TOKEN,
							null, mSessionsLoaderCallback);
				}
			}
		}
	};

	private Runnable mRefreshSessionsRunnable = new Runnable() {
		public void run() {
			if (mAdapter != null) {
				// This is used to refresh session title colors.
				mAdapter.notifyDataSetChanged();
			}

			// Check again on the next quarter hour, with some padding to
			// account for network
			// time differences.
			long nextQuarterHour = (SystemClock.uptimeMillis() / 900000 + 1) * 900000 + 5000;
			mMessageQueueHandler.postAtTime(mRefreshSessionsRunnable,
					nextQuarterHour);
		}
	};

	/**
	 * {@link CursorAdapter} that renders a {@link SessionsQuery}.
	 */
	private class SessionsAdapter extends CursorAdapter {

		public SessionsAdapter(Context context, Cursor cursor) {
			super(context, cursor, 0);
		}

		/** {@inheritDoc} */
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return LayoutInflater.from(context).inflate(
					R.layout.list_item_session, parent, false);
		}

		/** {@inheritDoc} */
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final TextView titleView = (TextView) view
					.findViewById(R.id.session_title);
			final TextView subtitleView = (TextView) view
					.findViewById(R.id.session_subtitle);

			titleView.setText(cursor.getString(SessionsQuery.SESSION_TITLE));

			// Format time block this session occupies
			final long blockStart = cursor.getLong(SessionsQuery.BLOCK_START);
			final long blockEnd = cursor.getLong(SessionsQuery.BLOCK_END);
			final String roomName = cursor.getString(SessionsQuery.ROOM_NAME);
			final String subtitle = UIUtils.formatSessionSubtitle(blockStart,
					blockEnd, roomName, context);

			subtitleView.setText(subtitle);

			final boolean starred = cursor
					.getInt(SessionsQuery.SESSION_STARRED) != 0;
			view.findViewById(R.id.star_button).setVisibility(
					starred ? View.VISIBLE : View.INVISIBLE);

			// Possibly indicate that the session has occurred in the past.
			UIUtils.setSessionTitleColor(blockStart, blockEnd, titleView,
					subtitleView);
		}
	}

	/**
	 * {@link CursorAdapter} that renders a {@link SearchQuery}.
	 */
	private class SearchAdapter extends CursorAdapter {
		public SearchAdapter(Context context, Cursor cursor) {
			super(context, cursor, 0);
		}

		/** {@inheritDoc} */
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return LayoutInflater.from(context).inflate(
					R.layout.list_item_session, parent, false);
		}

		/** {@inheritDoc} */
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			((TextView) view.findViewById(R.id.session_title)).setText(cursor
					.getString(SearchQuery.TITLE));

			final String snippet = cursor.getString(SearchQuery.SEARCH_SNIPPET);

			final Spannable styledSnippet = UIUtils.buildStyledSnippet(snippet);
			((TextView) view.findViewById(R.id.session_subtitle))
					.setText(styledSnippet);

			final boolean starred = cursor.getInt(SearchQuery.STARRED) != 0;
			view.findViewById(R.id.star_button).setVisibility(
					starred ? View.VISIBLE : View.INVISIBLE);
		}
	}

	/**
	 * {@link Sessions} query parameters.
	 */
	private interface SessionsQuery {
		int _TOKEN = 0x1;

		String[] PROJECTION = { BaseColumns._ID,
				CfpContract.Sessions.SESSION_ID,
				CfpContract.Sessions.SESSION_TITLE,
				CfpContract.Sessions.SESSION_STARRED,
				CfpContract.Blocks.BLOCK_START, CfpContract.Blocks.BLOCK_END,
				CfpContract.Rooms.ROOM_NAME, };

		int _ID = 0;
		int SESSION_ID = 1;
		int SESSION_TITLE = 2;
		int SESSION_STARRED = 3;
		int BLOCK_START = 4;
		int BLOCK_END = 5;
		int ROOM_NAME = 6;
	}

	/**
	 * {@link Tracks} query parameters.
	 */
	private interface TracksQuery {
		int _TOKEN = 0x2;

		String[] PROJECTION = { CfpContract.Tracks.TRACK_NAME,
				CfpContract.Tracks.TRACK_COLOR, };

		int TRACK_NAME = 0;
		int TRACK_COLOR = 1;
	}

	/**
	 * {@link Sessions} search query parameters.
	 */
	private interface SearchQuery {
		int _TOKEN = 0x3;

		String[] PROJECTION = { BaseColumns._ID,
				CfpContract.Sessions.SESSION_ID,
				CfpContract.Sessions.SESSION_TITLE,
				CfpContract.Sessions.SEARCH_SNIPPET,
				CfpContract.Sessions.SESSION_STARRED, };

		int _ID = 0;
		int SESSION_ID = 1;
		int TITLE = 2;
		int SEARCH_SNIPPET = 3;
		int STARRED = 4;
	}

}
