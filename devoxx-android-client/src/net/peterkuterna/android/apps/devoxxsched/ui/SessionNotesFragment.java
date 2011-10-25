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
import net.peterkuterna.android.apps.devoxxsched.util.AnalyticsUtils;
import net.peterkuterna.android.apps.devoxxsched.util.CatchNotesHelper;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class SessionNotesFragment extends ProgressFragment implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private ViewGroup mRootView;
	private String mTitleString;
	private String mHashtag;

	private Handler mHandler = new Handler();

	public static SessionNotesFragment newInstance(Uri sessionUri) {
		SessionNotesFragment f = new SessionNotesFragment();
		Bundle args = new Bundle();
		args.putParcelable("uri", sessionUri);
		f.setArguments(args);
		return f;
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater,
			ViewGroup container, Bundle savedInstanceState) {
		mRootView = (ViewGroup) inflater.inflate(
				R.layout.fragment_session_notes, null);
		return mRootView;
	}

	@Override
	int getContentResourceId() {
		return R.id.notes_scroll;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setContentShown(false);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onResume() {
		super.onResume();

		getLoaderManager().restartLoader(0, null, this);

		final IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
		filter.addDataScheme("package");
		getActivity().registerReceiver(mPackageChangesReceiver, filter);
	}

	@Override
	public void onPause() {
		super.onPause();

		getActivity().unregisterReceiver(mPackageChangesReceiver);
	}

	private void updateNotesTab() {
		final CatchNotesHelper helper = new CatchNotesHelper(getActivity());
		final boolean notesInstalled = helper
				.isNotesInstalledAndMinimumVersion();

		final Intent marketIntent = helper.notesMarketIntent();
		final Intent newIntent = helper.createNoteIntent(getString(
				R.string.note_template, mTitleString, getHashtagsString()));

		final Intent viewIntent = helper.viewNotesIntent(getHashtagsString());

		// Set icons and click listeners
		((ImageView) mRootView.findViewById(R.id.notes_catch_market_icon))
				.setImageDrawable(UIUtils.getIconForIntent(getActivity(),
						marketIntent));
		((ImageView) mRootView.findViewById(R.id.notes_catch_new_icon))
				.setImageDrawable(UIUtils.getIconForIntent(getActivity(),
						newIntent));
		((ImageView) mRootView.findViewById(R.id.notes_catch_view_icon))
				.setImageDrawable(UIUtils.getIconForIntent(getActivity(),
						viewIntent));

		// Set click listeners
		mRootView.findViewById(R.id.notes_catch_market_link)
				.setOnClickListener(new View.OnClickListener() {
					public void onClick(View view) {
						startActivity(marketIntent);
						fireNotesEvent(R.string.notes_catch_market_title);
					}
				});

		mRootView.findViewById(R.id.notes_catch_new_link).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
						startActivity(newIntent);
						fireNotesEvent(R.string.notes_catch_new_title);
					}
				});

		mRootView.findViewById(R.id.notes_catch_view_link).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
						startActivity(viewIntent);
						fireNotesEvent(R.string.notes_catch_view_title);
					}
				});

		// Show/hide elements
		mRootView.findViewById(R.id.notes_catch_market_link).setVisibility(
				notesInstalled ? View.GONE : View.VISIBLE);
		mRootView.findViewById(R.id.notes_catch_market_separator)
				.setVisibility(notesInstalled ? View.GONE : View.VISIBLE);

		mRootView.findViewById(R.id.notes_catch_new_link).setVisibility(
				!notesInstalled ? View.GONE : View.VISIBLE);
		mRootView.findViewById(R.id.notes_catch_new_separator).setVisibility(
				!notesInstalled ? View.GONE : View.VISIBLE);

		mRootView.findViewById(R.id.notes_catch_view_link).setVisibility(
				!notesInstalled ? View.GONE : View.VISIBLE);
		mRootView.findViewById(R.id.notes_catch_view_separator).setVisibility(
				!notesInstalled ? View.GONE : View.VISIBLE);
	}

	/*
	 * Event structure: Category -> "Session Details" Action -> "Create Note",
	 * "View Note", etc Label -> Session's Title Value -> 0.
	 */
	public void fireNotesEvent(int actionId) {
		AnalyticsUtils.getInstance(getActivity()).trackEvent("Session Details",
				getActivity().getString(actionId), mTitleString, 0);
	}

	private String getHashtagsString() {
		if (!TextUtils.isEmpty(mHashtag)) {
			return SessionDetailFragment.CONFERENCE_HASHTAG + " #" + mHashtag;
		} else {
			return SessionDetailFragment.CONFERENCE_HASHTAG;
		}
	}

	private BroadcastReceiver mPackageChangesReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateNotesTab();
		}
	};

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		final Uri uri = getArguments().getParcelable("uri");
		return new CursorLoader(getActivity(), uri, SessionsQuery.PROJECTION,
				null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (!data.moveToFirst()) {
			return;
		}

		mTitleString = data.getString(SessionsQuery.TITLE);
		mHashtag = data.getString(SessionsQuery.HASHTAG);

		mHandler.post(new Runnable() {
			@Override
			public void run() {
				updateNotesTab();

				if (getView() != null) {
					if (isResumed()) {
						setContentShown(true);
					} else {
						setContentShownNoAnimation(true);
					}
				}
			}
		});
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	/**
	 * {@link Sessions} query parameters.
	 */
	private interface SessionsQuery {
		String[] PROJECTION = { CfpContract.Sessions.SESSION_TITLE,
				CfpContract.Tracks.TRACK_HASHTAG };

		int TITLE = 0;
		int HASHTAG = 1;
	}

}
