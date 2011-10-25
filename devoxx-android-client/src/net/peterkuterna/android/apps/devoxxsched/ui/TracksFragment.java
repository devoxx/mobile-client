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
import net.peterkuterna.android.apps.devoxxsched.util.ActivityHelper;
import net.peterkuterna.android.apps.devoxxsched.util.AnalyticsUtils;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;

public class TracksFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private TracksAdapter mAdapter;
	private Uri mTracksUri;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AnalyticsUtils.getInstance(getActivity()).trackPageView("/Tracks");

		final Intent intent = ActivityHelper
				.fragmentArgumentsToIntent(getArguments());
		mTracksUri = intent.getData();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setEmptyText(getString(R.string.empty_tracks));

		mAdapter = new TracksAdapter(getActivity(), null);
		mAdapter.setHasAllItem(true);
		setListAdapter(mAdapter);

		setListShown(false);

		getLoaderManager().initLoader(TracksAdapter.TracksQuery._TOKEN, null,
				this);
	}

	@Override
	public void onResume() {
		super.onResume();

		getLoaderManager().restartLoader(TracksAdapter.TracksQuery._TOKEN,
				null, this);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		final Cursor cursor = (Cursor) mAdapter.getItem(position);
		final String trackId;
		final String trackName;
		final int trackColor;

		if (cursor != null) {
			trackId = cursor.getString(TracksAdapter.TracksQuery.TRACK_ID);
			trackName = cursor.getString(TracksAdapter.TracksQuery.TRACK_NAME);
			trackColor = cursor.getInt(TracksAdapter.TracksQuery.TRACK_COLOR);
		} else {
			trackId = CfpContract.Tracks.ALL_TRACK_ID;
			trackName = null;
			trackColor = -1;
		}

		AnalyticsUtils.getInstance(getActivity()).trackEvent("Tracks", "Click",
				trackName != null ? trackName : "All Sessions", 0);

		final Intent intent = new Intent(Intent.ACTION_VIEW);
		final Uri trackUri = CfpContract.Tracks.buildTrackUri(trackId);
		intent.putExtra(SessionsFragment.EXTRA_TRACK, trackUri);
		intent.putExtra(Intent.EXTRA_TITLE, trackName);
		intent.putExtra(SessionsFragment.EXTRA_TRACK_COLOR, trackColor);

		if (cursor == null) {
			intent.setData(CfpContract.Sessions.CONTENT_URI);
		} else {
			intent.setData(CfpContract.Tracks.buildSessionsUri(trackId));
		}

		((BaseActivity) getSupportActivity()).openActivityOrFragment(intent);

		getListView().setItemChecked(position, true);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		final String[] projection = TracksAdapter.TracksQuery.PROJECTION_WITH_SESSIONS_COUNT;
		return new CursorLoader(getActivity(), mTracksUri, projection, null,
				null, CfpContract.Tracks.DEFAULT_SORT);
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
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

}
