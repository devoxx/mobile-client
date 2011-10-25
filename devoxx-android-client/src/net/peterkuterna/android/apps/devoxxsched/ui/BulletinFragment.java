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
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.News;
import net.peterkuterna.android.apps.devoxxsched.util.AnalyticsUtils;
import net.peterkuterna.android.apps.devoxxsched.util.NotifierManager;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class BulletinFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<Cursor> {

	protected static final String TAG = "BulletinFragment";

	private NotifyingAsyncQueryHandler mHandler;
	private NotifierManager mNotifierManager;
	private BulletinAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AnalyticsUtils.getInstance(getActivity()).trackPageView("/Bulletin");
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mHandler = new NotifyingAsyncQueryHandler(getActivity()
				.getContentResolver(), null);
		mNotifierManager = new NotifierManager(getActivity());

		setEmptyText(getString(R.string.empty_bulletin_results));

		mAdapter = new BulletinAdapter(getActivity(), null);
		setListAdapter(mAdapter);

		setListShown(false);

		getLoaderManager().initLoader(NewsQuery._TOKEN, null, this);
	}

	@Override
	public void onResume() {
		super.onResume();

		clearNewNewsItems();

		getLoaderManager().restartLoader(NewsQuery._TOKEN, null, this);

		getActivity().getContentResolver().registerContentObserver(
				News.CONTENT_URI, true, mNewsChangesObserver);
	}

	@Override
	public void onPause() {
		super.onPause();

		getActivity().getContentResolver().unregisterContentObserver(
				mNewsChangesObserver);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), News.CONTENT_URI,
				NewsQuery.PROJECTION, null, null, News.DEFAULT_SORT);
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

	private void clearNewNewsItems() {
		final Uri uri = News.CONTENT_NEW_URI;
		final ContentValues values = new ContentValues();
		values.put(News.NEWS_NEW, 0);
		mHandler.startUpdate(uri, values);
		mNotifierManager.clearNewNewsNotification();
	}

	private ContentObserver mNewsChangesObserver = new ContentObserver(
			new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			getLoaderManager().restartLoader(NewsQuery._TOKEN, null,
					BulletinFragment.this);
		}
	};

	public static final class NewsViews {
		TextView dateView;
		TextView textView;
	}

	private class BulletinAdapter extends CursorAdapter {

		public BulletinAdapter(Context context, Cursor c) {
			super(context, c, 0);
		}

		protected void findAndCacheViews(View view) {
			NewsViews views = new NewsViews();
			views.dateView = (TextView) view.findViewById(android.R.id.text1);
			views.textView = (TextView) view.findViewById(android.R.id.text2);
			view.setTag(views);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View v = LayoutInflater.from(context).inflate(
					R.layout.list_item_bulletin, parent, false);
			findAndCacheViews(v);
			return v;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			NewsViews views = (NewsViews) view.getTag();

			final String date = cursor.getString(NewsQuery.NEWS_DATE);
			final String text = cursor.getString(NewsQuery.NEWS_TEXT);

			views.dateView.setText(date);
			views.textView.setText(text);
		}

	}

	/**
	 * {@link News} query parameters.
	 */
	public interface NewsQuery {
		int _TOKEN = 0x1;

		String[] PROJECTION = { BaseColumns._ID, CfpContract.News.NEWS_DATE,
				CfpContract.News.NEWS_TEXT, };

		int _ID = 0;
		int NEWS_DATE = 1;
		int NEWS_TEXT = 2;
	}

}
