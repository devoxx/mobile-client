/*
 * Copyright 2011 Google Inc.
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

package net.peterkuterna.android.apps.devoxxfrsched.ui.tablet;

import net.peterkuterna.android.apps.devoxxfrsched.R;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract;
import net.peterkuterna.android.apps.devoxxfrsched.ui.BaseActivity;
import net.peterkuterna.android.apps.devoxxfrsched.ui.SessionsFragment;
import net.peterkuterna.android.apps.devoxxfrsched.ui.TracksAdapter;
import net.peterkuterna.android.apps.devoxxfrsched.util.ActivityHelper;
import net.peterkuterna.android.apps.devoxxfrsched.util.UIUtils;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * A tablet-specific fragment that is a giant {@link android.widget.Spinner}
 * -like widget. It shows a {@link ListPopupWindow} containing a list of tracks,
 * using {@link TracksAdapter}.
 * 
 * Requires API level 11 or later since {@link ListPopupWindow} is API level
 * 11+.
 */
public class TracksDropdownFragment extends Fragment implements
		LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener,
		AdapterView.OnItemSelectedListener, PopupWindow.OnDismissListener {

	private boolean mAutoloadTarget = true;
	private Cursor mCursor;
	private TracksAdapter mAdapter;
	private Uri mTracksUri;
	private String mNextType;

	private ListPopupWindow mListPopupWindow;
	private ViewGroup mRootView;
	private TextView mTitle;
	private TextView mAbstract;

	private Handler mHandler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAdapter = new TracksAdapter(getActivity(), null);
		mAdapter.setHasAllItem(true);

		if (savedInstanceState != null) {
			// Prevent auto-load behavior on orientation change.
			mAutoloadTarget = false;
		}
	}

	public void reloadFromArguments(Bundle arguments) {
		if (mListPopupWindow != null) {
			mListPopupWindow.setAdapter(mAdapter);
		}

		getLoaderManager().initLoader(TracksAdapter.TracksQuery._TOKEN,
				arguments, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mRootView = (ViewGroup) inflater.inflate(
				R.layout.fragment_tracks_dropdown, null);

		mTitle = (TextView) mRootView.findViewById(R.id.track_title);
		mAbstract = (TextView) mRootView.findViewById(R.id.track_abstract);

		mRootView.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				mListPopupWindow = new ListPopupWindow(getActivity());
				mListPopupWindow.setAdapter(mAdapter);
				mListPopupWindow.setModal(true);
				mListPopupWindow.setContentWidth(400);
				mListPopupWindow.setAnchorView(mRootView);
				mListPopupWindow
						.setOnItemClickListener(TracksDropdownFragment.this);
				mListPopupWindow.show();
				mListPopupWindow
						.setOnDismissListener(TracksDropdownFragment.this);
			}
		});

		return mRootView;
	}

	/** {@inheritDoc} */
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		selectTrack(position);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		selectTrack(position);
	}

	@SuppressLint("NewApi") protected void selectTrack(int position) {
		final Cursor cursor = (Cursor) mAdapter.getItem(position);
		loadTrack(cursor, true);

		if (cursor != null) {
			UIUtils.setLastUsedTrackID(getActivity(),
					cursor.getString(TracksAdapter.TracksQuery.TRACK_ID));
		} else {
			UIUtils.setLastUsedTrackID(getActivity(),
					CfpContract.Tracks.ALL_TRACK_ID);
		}

		if (mListPopupWindow != null) {
			mListPopupWindow.dismiss();
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}

	public void loadTrack(Cursor cursor, boolean loadTargetFragment) {
		final Resources res = getResources();
		final String trackId;
		final int trackColor;

		if (cursor != null) {
			trackColor = cursor.getInt(TracksAdapter.TracksQuery.TRACK_COLOR);
			trackId = cursor.getString(TracksAdapter.TracksQuery.TRACK_ID);
			if (mTitle != null) {
				mTitle.setText(cursor
						.getString(TracksAdapter.TracksQuery.TRACK_NAME));
			}
			if (mAbstract != null) {
				mAbstract.setText(cursor
						.getString(TracksAdapter.TracksQuery.TRACK_ABSTRACT));
			}
		} else {
			trackColor = res.getColor(R.color.all_track_color);
			trackId = CfpContract.Tracks.ALL_TRACK_ID;
			if (mTitle != null) {
				mTitle.setText(R.string.all_sessions_title);
			}
			if (mAbstract != null) {
				mAbstract.setText(R.string.all_sessions_subtitle);
			}
		}

		boolean isDark = UIUtils.isColorDark(trackColor);
		mRootView.setBackgroundColor(trackColor);

		if (isDark) {
			if (mTitle != null) {
				mTitle.setTextColor(res.getColor(R.color.body_text_1_inverse));
			}
			if (mAbstract != null) {
				mAbstract.setTextColor(res
						.getColor(R.color.body_text_2_inverse));
			}
			mRootView.findViewById(R.id.track_dropdown_arrow)
					.setBackgroundResource(
							R.drawable.track_dropdown_arrow_light);
		} else {
			if (mTitle != null) {
				mTitle.setTextColor(res.getColor(R.color.body_text_1));
			}
			if (mAbstract != null) {
				mAbstract.setTextColor(res.getColor(R.color.body_text_2));
			}
			mRootView
					.findViewById(R.id.track_dropdown_arrow)
					.setBackgroundResource(R.drawable.track_dropdown_arrow_dark);
		}

		if (loadTargetFragment) {
			final Intent intent = new Intent(Intent.ACTION_VIEW);
			final Uri trackUri = CfpContract.Tracks.buildTrackUri(trackId);
			intent.putExtra(SessionsFragment.EXTRA_TRACK, trackUri);

			if (cursor == null) {
				intent.setData(CfpContract.Sessions.CONTENT_URI);
			} else {
				intent.setData(CfpContract.Tracks.buildSessionsUri(trackId));
			}

			((BaseActivity) getActivity())
					.openActivityOrFragment(intent);
		}
	}

	public void onDismiss() {
		mListPopupWindow = null;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		final Intent intent = ActivityHelper.fragmentArgumentsToIntent(args);
		final Uri tracksUri = intent.getData();
		if (tracksUri == null) {
			return null;
		}

		final String[] projection = TracksAdapter.TracksQuery.PROJECTION_WITH_SESSIONS_COUNT;
		return new CursorLoader(getActivity(), tracksUri, projection, null,
				null, CfpContract.Tracks.DEFAULT_SORT);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {
		if (getActivity() != null) {
			// If there was a last-opened track, load it. Otherwise load the
			// first
			// track.
			data.moveToFirst();
			String lastTrackID = UIUtils.getLastUsedTrackID(getActivity());
			if (lastTrackID != null) {
				while (!data.isAfterLast()) {
					if (lastTrackID.equals(data
							.getString(TracksAdapter.TracksQuery.TRACK_ID))) {
						break;
					}
					data.moveToNext();
				}

				if (data.isAfterLast()) {
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							loadTrack(null, mAutoloadTarget);
						}
					});
				} else {
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							loadTrack(data, mAutoloadTarget);
						}
					});
				}
			} else {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						loadTrack(null, mAutoloadTarget);
					}
				});
			}
		}

		mAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

}
