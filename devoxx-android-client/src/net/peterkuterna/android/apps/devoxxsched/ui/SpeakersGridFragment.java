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

package net.peterkuterna.android.apps.devoxxsched.ui;

import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Speakers;
import net.peterkuterna.android.apps.devoxxsched.util.ActivityHelper;
import net.peterkuterna.android.apps.devoxxsched.util.AnalyticsUtils;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.widget.GridView;

public class SpeakersGridFragment extends GridFragment {

	private static final String STATE_CHECKED_POSITION = "checkedPosition";

	private CursorAdapter mAdapter;
	private int mCheckedPosition = -1;
	private Uri mSpeakersUri;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AnalyticsUtils.getInstance(getActivity()).trackPageView("/Speakers");
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		final GridView gridView = getGridView();
		float sf = getResources().getDisplayMetrics().density;
		final int padding = (int) (5.0 * sf);
		final int spacing = (int) (20.0 * sf);

		gridView.setNumColumns(GridView.AUTO_FIT);
		gridView.setColumnWidth((int) (120 * sf));
		gridView.setStretchMode(GridView.STRETCH_SPACING);
		gridView.setVerticalFadingEdgeEnabled(true);
		gridView.setPadding(padding, padding, padding, padding);
		gridView.setHorizontalSpacing(spacing);
		gridView.setVerticalSpacing(spacing);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setEmptyText(getString(R.string.empty_speakers));

		if (UIUtils.isHoneycomb()) {
			getGridView().setChoiceMode(GridView.CHOICE_MODE_SINGLE);
		}

		reloadFromArguments(getArguments());

		setGridShown(false);

		if (savedInstanceState != null) {
			mCheckedPosition = savedInstanceState.getInt(
					STATE_CHECKED_POSITION, -1);
		}

	}

	public void reloadFromArguments(Bundle arguments) {
		mCheckedPosition = -1;

		// Load new arguments
		final Intent intent = ActivityHelper
				.fragmentArgumentsToIntent(arguments);
		mSpeakersUri = intent.getData();
		if (mSpeakersUri == null) {
			return;
		}

		mAdapter = new SpeakersAdapter(getActivity(), null,
				R.layout.grid_item_speaker);
		getGridView().setAdapter(mAdapter);

		getLoaderManager().initLoader(SpeakersAdapter.SpeakersQuery._TOKEN,
				null, mSpeakersLoaderCallback);
	}

	@Override
	public void onResume() {
		super.onResume();

		getActivity().getContentResolver()
				.registerContentObserver(CfpContract.Speakers.CONTENT_URI,
						true, mSpeakerChangesObserver);
		if (mSpeakersUri != null) {
			getLoaderManager().restartLoader(
					SpeakersAdapter.SpeakersQuery._TOKEN, null,
					mSpeakersLoaderCallback);
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		getActivity().getContentResolver().unregisterContentObserver(
				mSpeakerChangesObserver);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_CHECKED_POSITION, mCheckedPosition);
	}

	@Override
	public void onGridItemClick(GridView gv, View v, int position, long id) {
		final Cursor cursor = (Cursor) mAdapter.getItem(position);
		final String speakerId = cursor
				.getString(SpeakersAdapter.SpeakersQuery.SPEAKER_ID);
		final String firstName = cursor
				.getString(SpeakersAdapter.SpeakersQuery.SPEAKER_FIRSTNAME);
		final String lastName = cursor
				.getString(SpeakersAdapter.SpeakersQuery.SPEAKER_LASTNAME);
		final String speakerName = UIUtils.formatSpeakerName(firstName,
				lastName);
		final String title = getResources().getString(
				R.string.title_sessions_of, speakerName);
		AnalyticsUtils.getInstance(getActivity()).trackEvent("Speakers Grid",
				"Click", speakerName, 0);
		final Uri sessionsUri = CfpContract.Speakers
				.buildSessionsDirUri(speakerId);
		final Intent intent = new Intent(Intent.ACTION_VIEW, sessionsUri);
		intent.putExtra(Intent.EXTRA_TITLE, title);
		((AbstractActivity) getActivity()).openActivityOrFragment(intent);
		if (UIUtils.isHoneycomb()) {
			getGridView().setItemChecked(position, true);
			mCheckedPosition = position;
		}
	}

	public void clearCheckedPosition() {
		if (mCheckedPosition >= 0 && UIUtils.isHoneycomb()) {
			getGridView().setItemChecked(mCheckedPosition, false);
			mCheckedPosition = -1;
		}
	}

	private LoaderManager.LoaderCallbacks<Cursor> mSpeakersLoaderCallback = new LoaderCallbacks<Cursor>() {

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			return new CursorLoader(getActivity(), mSpeakersUri,
					SpeakersAdapter.SpeakersQuery.PROJECTION, null, null,
					CfpContract.Speakers.DEFAULT_SORT);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			mAdapter.swapCursor(data);

			if (isResumed()) {
				setGridShown(true);
			} else {
				setGridShownNoAnimation(true);
			}

			if (mCheckedPosition >= 0 && getView() != null
					&& UIUtils.isHoneycomb()) {
				getGridView().setItemChecked(mCheckedPosition, true);
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			mAdapter.swapCursor(null);
		}

	};

	private ContentObserver mSpeakerChangesObserver = new ContentObserver(
			new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			if (mSpeakersUri != null) {
				getLoaderManager().restartLoader(
						SpeakersAdapter.SpeakersQuery._TOKEN, null,
						mSpeakersLoaderCallback);
			}
		}
	};

	/**
	 * {@link Speakers} query parameters.
	 */
	private interface SpeakersQuery {
		int _TOKEN = 0x1;

		String[] PROJECTION = { BaseColumns._ID,
				CfpContract.Speakers.SPEAKER_ID,
				CfpContract.Speakers.SPEAKER_FIRSTNAME,
				CfpContract.Speakers.SPEAKER_LASTNAME,
				CfpContract.Speakers.SPEAKER_COMPANY,
				CfpContract.Speakers.SPEAKER_BIO,
				CfpContract.Speakers.SPEAKER_IMAGE_URL, };

		int _ID = 0;
		int SPEAKER_ID = 1;
		int SPEAKER_FIRSTNAME = 2;
		int SPEAKER_LASTNAME = 3;
		int SPEAKER_COMPANY = 4;
		int SPEAKER_BIO = 5;
		int SPEAKER_IMAGE_URL = 6;
	}

}
