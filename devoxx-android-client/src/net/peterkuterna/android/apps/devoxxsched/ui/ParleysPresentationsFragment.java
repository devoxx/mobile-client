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
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.ParleysPresentations;
import net.peterkuterna.android.apps.devoxxsched.util.ActivityHelper;
import net.peterkuterna.android.apps.devoxxsched.util.AnalyticsUtils;
import net.peterkuterna.android.apps.devoxxsched.util.ImageDownloader;
import net.peterkuterna.android.apps.devoxxsched.util.ParleysUriUtils;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ParleysPresentationsFragment extends ListFragment {

	private CursorAdapter mAdapter;
	private Uri mPresentationUri;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setEmptyText(getString(R.string.empty_presentations));

		reloadFromArguments(getArguments());

		setListShown(false);
	}

	public void reloadFromArguments(Bundle arguments) {
		// Load new arguments
		final Intent intent = ActivityHelper
				.fragmentArgumentsToIntent(arguments);
		mPresentationUri = intent.getData();
		if (mPresentationUri == null) {
			return;
		}

		mAdapter = new ParleysPresentationsAdapter(getActivity(), null);

		setListAdapter(mAdapter);

		getLoaderManager().initLoader(ParleysPresentationsQuery._TOKEN, null,
				mPresentationsLoaderCallback);
	}

	@Override
	public void onResume() {
		super.onResume();

		getActivity().getContentResolver().registerContentObserver(
				CfpContract.ParleysPresentations.CONTENT_URI, true,
				mPresentationChangesObserver);

		if (mPresentationUri != null) {
			getLoaderManager().restartLoader(ParleysPresentationsQuery._TOKEN,
					null, mPresentationsLoaderCallback);
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		getActivity().getContentResolver().unregisterContentObserver(
				mPresentationChangesObserver);
	}

	/** {@inheritDoc} */
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		final Cursor cursor = (Cursor) mAdapter.getItem(position);
		final String presId = cursor
				.getString(ParleysPresentationsQuery.PRESENTATION_ID);
		final String title = cursor.getString(ParleysPresentationsQuery.TITLE);
		AnalyticsUtils.getInstance(getActivity()).trackEvent("Parleys.com",
				"Click", title, 0);
		final Intent intent = new Intent(Intent.ACTION_VIEW,
				ParleysUriUtils.buildParleysViewUri(presId));
		((BaseActivity) getSupportActivity()).openActivityOrFragment(intent);
	}

	private LoaderManager.LoaderCallbacks<Cursor> mPresentationsLoaderCallback = new LoaderCallbacks<Cursor>() {

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			return new CursorLoader(getActivity(), mPresentationUri,
					ParleysPresentationsQuery.PROJECTION, null, null,
					CfpContract.ParleysPresentations.DEFAULT_SORT);
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

	};

	private ContentObserver mPresentationChangesObserver = new ContentObserver(
			new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			if (getActivity() != null) {
				if (mPresentationUri != null) {
					getLoaderManager().restartLoader(
							ParleysPresentationsQuery._TOKEN, null,
							mPresentationsLoaderCallback);
				}
			}
		}
	};

	/**
	 * {@link CursorAdapter} that renders a {@link ParleysPresentationsQuery}.
	 */
	private class ParleysPresentationsAdapter extends CursorAdapter {

		private final ImageDownloader mImageDownloader;

		public ParleysPresentationsAdapter(Context context, Cursor cursor) {
			super(context, cursor, 0);

			mImageDownloader = new ImageDownloader(context);
		}

		/** {@inheritDoc} */
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return LayoutInflater.from(context).inflate(
					R.layout.list_item_presentation, parent, false);
		}

		/** {@inheritDoc} */
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final TextView titleView = (TextView) view.findViewById(R.id.title);
			final TextView subtitleView = (TextView) view
					.findViewById(R.id.summary);
			final ImageView imageView = (ImageView) view
					.findViewById(R.id.image);

			final String thumbnailUrl = cursor
					.getString(ParleysPresentationsQuery.THUMBNAIL);

			titleView
					.setText(cursor.getString(ParleysPresentationsQuery.TITLE));
			subtitleView.setText(cursor
					.getString(ParleysPresentationsQuery.SUMMARY));
			mImageDownloader.download(thumbnailUrl, imageView,
					R.drawable.parleys_image_empty);
		}
	}

	/**
	 * {@link ParleysPresentations} query parameters.
	 */
	private interface ParleysPresentationsQuery {
		int _TOKEN = 0x1;

		String[] PROJECTION = { BaseColumns._ID,
				CfpContract.ParleysPresentations.PRESENTATION_ID,
				CfpContract.ParleysPresentations.PRESENTATION_TITLE,
				CfpContract.ParleysPresentations.PRESENTATION_SUMMARY,
				CfpContract.ParleysPresentations.PRESENTATION_THUMBNAIL, };

		int _ID = 0;
		int PRESENTATION_ID = 1;
		int TITLE = 2;
		int SUMMARY = 3;
		int THUMBNAIL = 4;
	}

}
