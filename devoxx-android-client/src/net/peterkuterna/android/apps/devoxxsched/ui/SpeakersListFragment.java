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
import net.peterkuterna.android.apps.devoxxsched.util.ImageDownloader;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class SpeakersListFragment extends ListFragment {

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
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setEmptyText(getString(R.string.empty_speakers));

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
		mSpeakersUri = intent.getData();
		if (mSpeakersUri == null) {
			return;
		}

		final int speakersQueryId;
		if (!CfpContract.Speakers.isSearchUri(mSpeakersUri)) {
			mAdapter = new SpeakersAdapter(getActivity(), null,
					R.layout.list_item_speaker);
			speakersQueryId = SpeakersAdapter.SpeakersQuery._TOKEN;
		} else {
			mAdapter = new SearchAdapter(getActivity(), null);
			speakersQueryId = SearchQuery._TOKEN;
		}

		setListAdapter(mAdapter);

		if (!reset) {
			getLoaderManager().initLoader(speakersQueryId, null,
					mSpeakersLoaderCallback);
		} else {
			getLoaderManager().restartLoader(speakersQueryId, null,
					mSpeakersLoaderCallback);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		getActivity().getContentResolver()
				.registerContentObserver(CfpContract.Speakers.CONTENT_URI,
						true, mSpeakerChangesObserver);

		if (mSpeakersUri != null) {
			if (!CfpContract.Speakers.isSearchUri(mSpeakersUri)) {
				getLoaderManager().restartLoader(SpeakersQuery._TOKEN, null,
						mSpeakersLoaderCallback);
			} else {
				getLoaderManager().restartLoader(SearchQuery._TOKEN, null,
						mSpeakersLoaderCallback);
			}
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

	/** {@inheritDoc} */
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
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
		AnalyticsUtils.getInstance(getActivity()).trackEvent("Speakers List",
				"Click", speakerName, 0);
		final Uri sessionsUri = CfpContract.Speakers
				.buildSessionsDirUri(speakerId);
		final Intent intent = new Intent(Intent.ACTION_VIEW, sessionsUri);
		intent.putExtra(Intent.EXTRA_TITLE, title);
		((AbstractActivity) getActivity()).openActivityOrFragment(intent);
		getListView().setItemChecked(position, true);
		mCheckedPosition = position;
	}

	public void clearCheckedPosition() {
		if (mCheckedPosition >= 0) {
			getListView().setItemChecked(mCheckedPosition, false);
			mCheckedPosition = -1;
		}
	}

	private LoaderManager.LoaderCallbacks<Cursor> mSpeakersLoaderCallback = new LoaderCallbacks<Cursor>() {

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			if (id == SpeakersAdapter.SpeakersQuery._TOKEN) {
				return new CursorLoader(getActivity(), mSpeakersUri,
						SpeakersAdapter.SpeakersQuery.PROJECTION, null, null,
						CfpContract.Speakers.DEFAULT_SORT);
			} else if (id == SearchQuery._TOKEN) {
				return new CursorLoader(getActivity(), mSpeakersUri,
						SearchQuery.PROJECTION, null, null,
						CfpContract.Speakers.DEFAULT_SORT);
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

	private ContentObserver mSpeakerChangesObserver = new ContentObserver(
			new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			if (mSpeakersUri != null) {
				if (!CfpContract.Speakers.isSearchUri(mSpeakersUri)) {
					getLoaderManager().restartLoader(
							SpeakersAdapter.SpeakersQuery._TOKEN, null,
							mSpeakersLoaderCallback);
				} else {
					getLoaderManager().restartLoader(SearchQuery._TOKEN, null,
							mSpeakersLoaderCallback);
				}
			}
		}
	};

	/**
	 * {@link CursorAdapter} that renders a {@link SearchQuery}.
	 */
	private class SearchAdapter extends CursorAdapter {

		private final ImageDownloader mImageDownloader;

		public SearchAdapter(Context context, Cursor cursor) {
			super(context, cursor, 0);

			this.mImageDownloader = new ImageDownloader(context);
		}

		/** {@inheritDoc} */
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return LayoutInflater.from(context).inflate(
					R.layout.list_item_speaker, parent, false);
		}

		/** {@inheritDoc} */
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final String firstName = cursor.getString(SearchQuery.FIRSTNAME);
			final String lastName = cursor.getString(SearchQuery.LASTNAME);
			final String speakerName = UIUtils.formatSpeakerName(firstName,
					lastName);
			final String imageUrl = cursor.getString(SearchQuery.IMAGE_URL);

			((TextView) view.findViewById(R.id.speaker)).setText(speakerName);

			final String snippet = cursor.getString(SearchQuery.SEARCH_SNIPPET);

			final Spannable styledSnippet = UIUtils.buildStyledSnippet(snippet);
			((TextView) view.findViewById(R.id.company)).setText(styledSnippet);

			mImageDownloader.download(imageUrl,
					(ImageView) view.findViewById(R.id.image),
					R.drawable.speaker_image_empty);
		}
	}

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

	/**
	 * {@link Speakers} search query parameters.
	 */
	private interface SearchQuery {
		int _TOKEN = 0x3;

		String[] PROJECTION = { BaseColumns._ID,
				CfpContract.Speakers.SPEAKER_ID,
				CfpContract.Speakers.SPEAKER_FIRSTNAME,
				CfpContract.Speakers.SPEAKER_LASTNAME,
				CfpContract.Speakers.SPEAKER_IMAGE_URL,
				CfpContract.Speakers.SEARCH_SNIPPET, };

		int _ID = 0;
		int SPEAKER_ID = 1;
		int FIRSTNAME = 2;
		int LASTNAME = 3;
		int IMAGE_URL = 4;
		int SEARCH_SNIPPET = 5;
	}

}
