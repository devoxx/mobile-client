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
import net.peterkuterna.android.apps.devoxxsched.c2dm.Util;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Sessions;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Tracks;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.ScrollableTabs;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.ScrollableTabsAdapter;
import net.peterkuterna.android.apps.devoxxsched.util.ActivityHelper;
import net.peterkuterna.android.apps.devoxxsched.util.AnalyticsUtils;
import net.peterkuterna.android.apps.devoxxsched.util.FractionalTouchDelegate;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler;
import net.peterkuterna.android.apps.devoxxsched.util.Prefs;
import net.peterkuterna.android.apps.devoxxsched.util.Prefs.DevoxxPrefs;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import net.peterkuterna.appengine.apps.devoxxsched.shared.DevoxxRequest;
import net.peterkuterna.appengine.apps.devoxxsched.shared.DevoxxRequestFactory;
import net.peterkuterna.appengine.apps.devoxxsched.shared.SessionProxy;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A fragment that shows detail information for a session. Summary, notes,
 * links, parleys and parallel sessions are shown in a (@link {@link ViewPager}.
 */
public class SessionDetailFragment extends Fragment implements
		CompoundButton.OnCheckedChangeListener {

	private static final String TAG = "SessionDetailFragment";

	public static final String CONFERENCE_HASHTAG = "#devoxx";

	public static final String EXTRA_TRACK_COLOR = "net.peterkuterna.android.apps.devoxxsched.extra.TRACK_COLOR";

	private static final int[] TAB_TITLES = { R.string.session_summary,
			R.string.session_notes, R.string.session_links,
			R.string.session_parallel };
	private static final int[] TAB_TITLES_HONEYCOMB_TABLET = {
			R.string.session_summary, R.string.session_notes,
			R.string.session_links, R.string.session_parleys,
			R.string.session_parallel };

	private String mTrackName = null;
	private String mHashtag = null;
	private int mTrackColor = -1;

	private Uri mSessionUri;
	private String mSessionId;

	private String mTitleString;
	private String mRoomId;
	private int mLevel;
	private String mUrl;

	private ViewGroup mRootView;
	private ScrollableTabs mTabs;
	private ViewPager mViewPager;
	private TextView mTitle;
	private TextView mSubtitle;
	private CompoundButton mStarred;

	private Handler mHandler = new Handler();
	private NotifyingAsyncQueryHandler mQueryHandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = ActivityHelper
				.fragmentArgumentsToIntent(getArguments());
		mSessionUri = intent.getData();

		if (mSessionUri == null) {
			return;
		}

		mSessionId = Sessions.getSessionId(mSessionUri);

		mTrackName = intent.getStringExtra(Intent.EXTRA_TITLE);
		mTrackColor = intent.getIntExtra(EXTRA_TRACK_COLOR, -1);

		if (TextUtils.isEmpty(mTrackName) || mTrackColor == -1) {
			final Uri trackUri = Sessions.buildTracksDirUri(mSessionId);
			Bundle args = new Bundle();
			args.putParcelable("uri", trackUri);
			getLoaderManager().initLoader(TracksQuery._TOKEN, args,
					mTrackLoaderCallback);
		} else {
			updateTrackData();
		}

		setHasOptionsMenu(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (mSessionUri == null) {
			return;
		}

		final SessionDetailPagerAdapter adapter = new SessionDetailPagerAdapter(
				getActivity(), getFragmentManager());
		mViewPager.setAdapter(adapter);
		mTabs.setAdapter(adapter);
		mViewPager.setOnPageChangeListener(mTabs);

		adapter.notifyDataSetChanged();

		mQueryHandler = new NotifyingAsyncQueryHandler(getActivity()
				.getContentResolver(), null);
	}

	@Override
	public void onResume() {
		super.onResume();

		getLoaderManager().restartLoader(SessionsQuery._TOKEN, null,
				mSessionDetailCallback);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		mRootView = (ViewGroup) inflater.inflate(
				R.layout.fragment_session_detail, null);

		mTabs = (ScrollableTabs) mRootView
				.findViewById(R.id.viewpagerheader_session_detail);
		mViewPager = (ViewPager) mRootView
				.findViewById(R.id.viewpager_session_detail);

		mViewPager.setOffscreenPageLimit(2);
		mViewPager.setPageMargin(getResources().getDimensionPixelSize(
				R.dimen.viewpager_page_margin));
		mViewPager.setPageMarginDrawable(R.drawable.viewpager_margin);

		mTitle = (TextView) mRootView.findViewById(R.id.session_title);
		mSubtitle = (TextView) mRootView.findViewById(R.id.session_subtitle);
		mStarred = (CompoundButton) mRootView.findViewById(R.id.star_button);

		mStarred.setFocusable(true);
		mStarred.setClickable(true);

		// Larger target triggers star toggle
		final View starParent = mRootView.findViewById(R.id.header_session);
		FractionalTouchDelegate.setupDelegate(starParent, mStarred, new RectF(
				0.6f, 0f, 1f, 0.8f));

		return mRootView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.session_detail_menu_items, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_map: {
			final Intent intent = new Intent(getActivity()
					.getApplicationContext(), MapActivity.class);
			intent.putExtra(MapFragment.EXTRA_ROOM, mRoomId);
			intent.putExtra(MapFragment.EXTRA_LEVEL, mLevel);
			startActivity(intent);
			return true;
		}
		case R.id.menu_share: {
			final String shareString = getString(R.string.share_template,
					mTitleString, getHashtagsString(), mUrl);
			final Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT, shareString);
			startActivity(Intent.createChooser(intent,
					getText(R.string.title_share)));
			return true;
		}
		}
		return super.onOptionsItemSelected(item);
	}

	private String getHashtagsString() {
		if (!TextUtils.isEmpty(mHashtag)) {
			return CONFERENCE_HASHTAG + " #" + mHashtag;
		} else {
			return CONFERENCE_HASHTAG;
		}
	}

	private void updateTrackData() {
		if (!UIUtils.isHoneycombTablet(getActivity())) {
			UIUtils.setActionBarData(getSupportActivity(), mTrackName,
					mTrackColor);
		}
	}

	/**
	 * Handle toggling of starred checkbox.
	 */
	public void onCheckedChanged(CompoundButton buttonView,
			final boolean isChecked) {
		final ContentValues values = new ContentValues();
		values.put(CfpContract.Sessions.SESSION_STARRED, isChecked ? 1 : 0);
		values.put(CfpContract.Sessions.SESSION_OPERATION_PENDING, 0);
		mQueryHandler.startUpdate(mSessionUri, values);

		SharedPreferences prefs = Prefs.get(getActivity());
		String deviceRegistrationID = prefs.getString(
				DevoxxPrefs.DEVICE_REGISTRATION_ID, null);
		if (deviceRegistrationID != null) {
			new AsyncTask<Void, Void, Boolean>() {

				@Override
				protected void onPreExecute() {
					final ContentValues values = new ContentValues();
					values.put(CfpContract.Sessions.SESSION_OPERATION_PENDING,
							1);
					mQueryHandler.startUpdate(mSessionUri, values);
				}

				@Override
				protected void onPostExecute(Boolean result) {
					if (Boolean.TRUE.equals(result)) {
						final ContentValues values = new ContentValues();
						values.put(
								CfpContract.Sessions.SESSION_OPERATION_PENDING,
								0);
						mQueryHandler.startUpdate(mSessionUri, values);
					}
				}

				@Override
				protected Boolean doInBackground(Void... arg0) {
					try {
						DevoxxRequestFactory factory = (DevoxxRequestFactory) Util
								.getRequestFactory(getActivity(),
										DevoxxRequestFactory.class);
						DevoxxRequest request = factory.devoxxRequest();

						SessionProxy session = request
								.create(SessionProxy.class);
						session.setSessionId(mSessionId);

						if (isChecked) {
							request.star(session).fire();
						} else {
							request.unstar(session).fire();
						}
					} catch (Exception e) {
						return Boolean.FALSE;
					}

					return Boolean.TRUE;
				}

			}.execute();
		}

		// Because change listener is set to null during initialization, these
		// won't fire on
		// pageview.
		AnalyticsUtils.getInstance(getActivity()).trackEvent("Session",
				isChecked ? "Starred" : "Unstarred", mTitleString, 0);
	}

	private LoaderManager.LoaderCallbacks<Cursor> mSessionDetailCallback = new LoaderCallbacks<Cursor>() {

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			return new CursorLoader(getActivity(), mSessionUri,
					SessionsQuery.PROJECTION, null, null, null);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			if (!data.moveToFirst()) {
				return;
			}

			// Format time block this session occupies
			final long blockStart = data.getLong(SessionsQuery.BLOCK_START);
			final long blockEnd = data.getLong(SessionsQuery.BLOCK_END);
			final String roomName = data.getString(SessionsQuery.ROOM_NAME);
			mRoomId = data.getString(SessionsQuery.ROOM_ID);
			mLevel = data.getInt(SessionsQuery.ROOM_LEVEL);
			final String subtitle = UIUtils.formatSessionSubtitle(blockStart,
					blockEnd, roomName, getActivity());
			final int starred = data.getInt(SessionsQuery.STARRED);

			mTitleString = data.getString(SessionsQuery.TITLE);
			mUrl = data.getString(SessionsQuery.URL);
			mHashtag = data.getString(SessionsQuery.TRACK_HASHTAG);

			AnalyticsUtils.getInstance(getActivity()).trackPageView(
					"/Sessions/" + mTitleString);

			mHandler.post(new Runnable() {
				@Override
				public void run() {
					mTitle.setText(mTitleString);
					mSubtitle.setText(subtitle);

					// Unregister around setting checked state to avoid
					// triggering listener since change isn't user generated.
					mStarred.setOnCheckedChangeListener(null);
					mStarred.setChecked(starred != 0);
					mStarred.setOnCheckedChangeListener(SessionDetailFragment.this);
				}
			});
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
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
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
		}

	};

	private class SessionDetailPagerAdapter extends FragmentStatePagerAdapter
			implements ScrollableTabsAdapter {

		private final Context mContext;

		public SessionDetailPagerAdapter(Context context, FragmentManager fm) {
			super(fm);

			this.mContext = context;
		}

		@Override
		public TextView getTab(final int position, LinearLayout root) {
			final TextView indicator = (TextView) getActivity()
					.getLayoutInflater().inflate(R.layout.tab_indicator, null,
							false);
			indicator
					.setText(UIUtils.isHoneycomb() ? TAB_TITLES_HONEYCOMB_TABLET[position]
							: TAB_TITLES[position]);
			indicator.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					mViewPager.setCurrentItem(position);
				}
			});
			return indicator;
		}

		@Override
		public Fragment getItem(int position) {
			if (position == 0) {
				return SessionSummaryFragment.newInstance(mSessionUri);
			} else if (position == 1) {
				return SessionNotesFragment.newInstance(mSessionUri);
			} else if (position == 2) {
				return SessionLinksFragment.newInstance(mSessionUri);
			} else if (position == 3 && UIUtils.isHoneycombTablet(mContext)) {
				ParleysPresentationsFragment f = new ParleysPresentationsFragment();
				final String sessionId = Sessions.getSessionId(mSessionUri);
				final Intent intent = new Intent(Intent.ACTION_VIEW,
						Sessions.buildParleysDirUri(sessionId));
				f.setArguments(ActivityHelper.intentToFragmentArguments(intent));
				return f;
			} else if ((position == 3)
					|| (position == 4 && UIUtils.isHoneycomb())) {
				SessionsFragment f = new SessionsFragment();
				final String sessionId = Sessions.getSessionId(mSessionUri);
				final Intent intent = new Intent(Intent.ACTION_VIEW,
						Sessions.buildSessionsParallelDirUri(sessionId));
				f.setArguments(ActivityHelper.intentToFragmentArguments(intent));
				return f;
			}
			return null;
		}

		@Override
		public int getCount() {
			return UIUtils.isHoneycomb() ? 5 : 4;
		}

		@Override
		public void finishUpdate(View container) {
			try {
				super.finishUpdate(container);
			} catch (IllegalStateException e) {
				Log.e(TAG, "executePendingTransactions is already executing");
			}
		}

	}

	/**
	 * {@link Sessions} query parameters.
	 */
	private interface SessionsQuery {

		int _TOKEN = 0x1;

		String[] PROJECTION = { CfpContract.Blocks.BLOCK_START,
				CfpContract.Blocks.BLOCK_END,
				CfpContract.Sessions.SESSION_TITLE,
				CfpContract.Sessions.SESSION_STARRED,
				CfpContract.Sessions.SESSION_URL, CfpContract.Rooms.ROOM_ID,
				CfpContract.Rooms.ROOM_NAME, CfpContract.Rooms.ROOM_LEVEL,
				CfpContract.Tracks.TRACK_HASHTAG };

		int BLOCK_START = 0;
		int BLOCK_END = 1;
		int TITLE = 2;
		int STARRED = 3;
		int URL = 4;
		int ROOM_ID = 5;
		int ROOM_NAME = 6;
		int ROOM_LEVEL = 7;
		int TRACK_HASHTAG = 8;

	}

	/**
	 * {@link Tracks} query parameters.
	 */
	private interface TracksQuery {
		int _TOKEN = 0x2;

		String[] PROJECTION = { CfpContract.Tracks.TRACK_NAME,
				CfpContract.Tracks.TRACK_COLOR };

		int TRACK_NAME = 0;
		int TRACK_COLOR = 1;
	}

}
