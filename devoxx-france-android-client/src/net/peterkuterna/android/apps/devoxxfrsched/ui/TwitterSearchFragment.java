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

package net.peterkuterna.android.apps.devoxxfrsched.ui;

import net.peterkuterna.android.apps.devoxxfrsched.R;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.Tweets;
import net.peterkuterna.android.apps.devoxxfrsched.service.AbstractSyncService;
import net.peterkuterna.android.apps.devoxxfrsched.service.TwitterSearchService;
import net.peterkuterna.android.apps.devoxxfrsched.ui.widget.PullToRefreshListView.OnRefreshListener;
import net.peterkuterna.android.apps.devoxxfrsched.util.AnalyticsUtils;
import net.peterkuterna.android.apps.devoxxfrsched.util.DetachableResultReceiver;
import net.peterkuterna.android.apps.devoxxfrsched.util.ImageDownloader;
import net.peterkuterna.android.apps.devoxxfrsched.util.TwitterApiUriUtils;
import android.app.Activity;
import android.content.Context;
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
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class TwitterSearchFragment extends PullToRefreshListFragment implements
		DetachableResultReceiver.Receiver,
		LoaderManager.LoaderCallbacks<Cursor> {

	protected static final String TAG = "TwitterSearchFragment";

	private TwitterSearchAdapter mAdapter;
	private State mState;
	private Handler mHandler = new Handler();

	public static TwitterSearchFragment newInstance() {
		return new TwitterSearchFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AnalyticsUtils.getInstance(getActivity()).trackPageView(
				"/TwitterSearch");
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		setRetainInstance(true);

		if (mState == null) {
			mState = new State();
		}

		mState.mReceiver.setReceiver(this);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		getListView().setOnRefreshListener(mOnRefreshListener);

		if (mState.mSyncing) {
			getListView().prepareForRefresh();
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getListView().prepareForRefresh();
		if (!mState.mSyncing) {
			triggerRefresh();
		}

		setEmptyText(getString(R.string.empty_twitter_results));

		mAdapter = new TwitterSearchAdapter(getActivity(), null);
		setListAdapter(mAdapter);

		setListShown(false);

		getLoaderManager().initLoader(TwitterSearchQuery._TOKEN, null, this);
	}

	@Override
	public void onResume() {
		super.onResume();

		mHandler.postDelayed(mUpdateTimeRunnable,
				60 * DateUtils.SECOND_IN_MILLIS);
		mHandler.postDelayed(mSyncRunnable, 5 * 60 * DateUtils.SECOND_IN_MILLIS);

		getLoaderManager().restartLoader(TwitterSearchQuery._TOKEN, null, this);

		getActivity().getContentResolver().registerContentObserver(
				Tweets.CONTENT_URI, true, mTweetsChangesObserver);
	}

	@Override
	public void onPause() {
		super.onPause();

		mHandler.removeCallbacks(mSyncRunnable);
		mHandler.removeCallbacks(mUpdateTimeRunnable);

		getActivity().getContentResolver().unregisterContentObserver(
				mTweetsChangesObserver);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		final Cursor cursor = (Cursor) mAdapter
				.getItem(position > 0 ? position - 1 : position);
		final String user = cursor.getString(TwitterSearchQuery.TWEET_USER);
		final String tweetId = cursor.getString(TwitterSearchQuery.TWEET_ID);
		AnalyticsUtils.getInstance(getActivity()).trackEvent("Twitter Search",
				"Click", user, 0);
		final Uri uri = TwitterApiUriUtils.buildTwitterTweetUri(user, tweetId);
		final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		((BaseActivity) getActivity()).openActivityOrFragment(intent);
	}

	private void triggerRefresh() {
		final Intent intent = new Intent(Intent.ACTION_SYNC, null,
				getActivity(), TwitterSearchService.class);
		intent.putExtra(AbstractSyncService.EXTRA_STATUS_RECEIVER,
				mState.mReceiver);
		getActivity().startService(intent);
	}

	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
		switch (resultCode) {
		case AbstractSyncService.STATUS_RUNNING: {
			mState.mSyncing = true;
			if (getView() != null) {
				getListView().prepareForRefresh();
			}
			break;
		}
		case AbstractSyncService.STATUS_FINISHED: {
			mState.mSyncing = false;
			if (getView() != null) {
				getListView().onRefreshComplete();
			}
			break;
		}
		case AbstractSyncService.STATUS_ERROR: {
			mState.mSyncing = false;
			if (getView() != null) {
				getListView().onRefreshComplete();
			}
			break;
		}
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), Tweets.CONTENT_URI,
				TwitterSearchQuery.PROJECTION, null, null, Tweets.DEFAULT_SORT);
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

	private OnRefreshListener mOnRefreshListener = new OnRefreshListener() {
		@Override
		public void onRefresh() {
			triggerRefresh();
		}
	};

	private ContentObserver mTweetsChangesObserver = new ContentObserver(
			new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			if (getActivity() != null) {
				getLoaderManager().restartLoader(TwitterSearchQuery._TOKEN,
						null, TwitterSearchFragment.this);
			}
		}
	};

	public static final class TweetItemViews {
		ImageView userImageView;
		TextView createdAtView;
		TextView userView;
		TextView textView;
	}

	private class TwitterSearchAdapter extends CursorAdapter {

		private ImageDownloader mImageDownloader;

		private long mCurrentTime = 0;

		public TwitterSearchAdapter(Context context, Cursor c) {
			super(context, c, 0);

			mImageDownloader = new ImageDownloader(context);
		}

		protected void findAndCacheViews(View view) {
			TweetItemViews views = new TweetItemViews();
			views.userImageView = (ImageView) view
					.findViewById(R.id.user_image);
			views.createdAtView = (TextView) view.findViewById(R.id.created_at);
			views.userView = (TextView) view.findViewById(R.id.user);
			views.textView = (TextView) view.findViewById(R.id.tweet);
			view.setTag(views);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View v = LayoutInflater.from(context).inflate(
					R.layout.list_item_twitter, parent, false);
			findAndCacheViews(v);
			return v;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TweetItemViews views = (TweetItemViews) view.getTag();

			final String imageUri = cursor
					.getString(TwitterSearchQuery.TWEET_IMAGE_URI);
			final String user = cursor.getString(TwitterSearchQuery.TWEET_USER);
			final String tweet = cursor
					.getString(TwitterSearchQuery.TWEET_TEXT);
			final long createdAt = cursor
					.getLong(TwitterSearchQuery.TWEET_CREATED_AT);
			final CharSequence createdAtRelative = DateUtils
					.getRelativeTimeSpanString(
							createdAt,
							createdAt > mCurrentTime ? createdAt : mCurrentTime,
							DateUtils.MINUTE_IN_MILLIS,
							DateUtils.FORMAT_ABBREV_ALL);

			views.createdAtView.setText(createdAtRelative + " ");
			views.userView.setText(user);
			views.textView.setText(tweet);

			mImageDownloader.download(imageUri, views.userImageView,
					R.drawable.speaker_image_empty);
		}

		@Override
		public void notifyDataSetChanged() {
			mCurrentTime = System.currentTimeMillis();

			super.notifyDataSetChanged();
		}

	}

	private Runnable mUpdateTimeRunnable = new Runnable() {
		public void run() {
			if (mAdapter != null) {
				mAdapter.notifyDataSetChanged();
			}

			mHandler.postDelayed(mUpdateTimeRunnable,
					60 * DateUtils.SECOND_IN_MILLIS);
		}
	};

	private Runnable mSyncRunnable = new Runnable() {
		public void run() {
			final Intent intent = new Intent(Intent.ACTION_SYNC, null,
					getActivity(), TwitterSearchService.class);
			getActivity().startService(intent);

			mHandler.postDelayed(mSyncRunnable,
					5 * 60 * DateUtils.SECOND_IN_MILLIS);
		}
	};

	private static class State {
		public DetachableResultReceiver mReceiver;
		public boolean mSyncing = false;

		private State() {
			mReceiver = new DetachableResultReceiver(new Handler());
		}

	}

	/**
	 * {@link Tweets} query parameters.
	 */
	public interface TwitterSearchQuery {
		int _TOKEN = 0x1;

		String[] PROJECTION = { BaseColumns._ID, CfpContract.Tweets.TWEET_ID,
				CfpContract.Tweets.TWEET_RESULT_TYPE,
				CfpContract.Tweets.TWEET_TEXT, CfpContract.Tweets.TWEET_USER,
				CfpContract.Tweets.TWEET_CREATED_AT,
				CfpContract.Tweets.TWEET_IMAGE_URI, };

		int _ID = 0;
		int TWEET_ID = 1;
		int TWEET_RESULT_TYPE = 2;
		int TWEET_TEXT = 3;
		int TWEET_USER = 4;
		int TWEET_CREATED_AT = 5;
		int TWEET_IMAGE_URI = 6;
	}

}
