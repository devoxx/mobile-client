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

import java.util.List;

import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Rooms;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.MetropolisOverlay;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.RoomOverlay;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.RoomOverlay.OnTapListener;
import net.peterkuterna.android.apps.devoxxsched.util.ActivityHelper;
import net.peterkuterna.android.apps.devoxxsched.util.AnalyticsUtils;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class MapFragment extends Fragment implements
		LoaderManager.LoaderCallbacks<Cursor> {

	protected static final String TAG = "MapFragment";

	private static final String STATE_SELECTED_LEVEL = "selectedLevel";
	private static final String STATE_SELECTED_SATELLITE = "selectedSatellite";

	private static final GeoPoint METROPOLIS_GEOPOINT = new GeoPoint(
			(int) (51.246109 * 1e6), (int) (4.417276 * 1e6));

	public static final String EXTRA_ROOM = "net.peterkuterna.android.apps.devoxxsched.extra.ROOM";
	public static final String EXTRA_LEVEL = "net.peterkuterna.android.apps.devoxxsched.extra.LEVEL";

	private String mRoomId;

	private int mLevel = 0;
	private boolean mSatellite = false;

	private Button mLevel0;
	private Button mLevel1;

	private MapView mMapView;
	private MapController mMapController;

	private List<Overlay> mOverlays;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AnalyticsUtils.getInstance(getActivity()).trackPageView("/Map");

		final Intent intent = ActivityHelper
				.fragmentArgumentsToIntent(getArguments());
		mRoomId = intent.getStringExtra(EXTRA_ROOM);
		mLevel = intent.getIntExtra(EXTRA_LEVEL, 0);

		if (savedInstanceState != null) {
			mLevel = savedInstanceState.getInt(STATE_SELECTED_LEVEL, mLevel);
			mSatellite = savedInstanceState.getBoolean(
					STATE_SELECTED_SATELLITE, false);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_map,
				null);

		mMapView = (MapView) root.findViewById(R.id.mapview);
		mMapView.setBuiltInZoomControls(true);
		mMapView.setSatellite(mSatellite);
		mOverlays = mMapView.getOverlays();
		mOverlays.add(new MetropolisOverlay());

		mLevel0 = (Button) root.findViewById(R.id.level0);
		mLevel1 = (Button) root.findViewById(R.id.level1);
		mLevel0.setSelected(mLevel == 0);
		mLevel1.setSelected(mLevel == 1);
		mLevel0.setOnClickListener(mLevelClickListener);
		mLevel1.setOnClickListener(mLevelClickListener);

		Button satelliteBtn = (Button) root.findViewById(R.id.satellite);
		satelliteBtn.setSelected(mSatellite);
		satelliteBtn.setOnClickListener(mSatteliteClickListener);

		mMapController = mMapView.getController();
		mMapController.setCenter(METROPOLIS_GEOPOINT);
		mMapController.setZoom(19);

		return root;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onResume() {
		super.onResume();

		getActivity().getContentResolver().registerContentObserver(
				CfpContract.Rooms.CONTENT_URI, true, mRoomsChangesObserver);

		getLoaderManager().restartLoader(0, null, this);
	}

	@Override
	public void onPause() {
		super.onPause();

		getActivity().getContentResolver().unregisterContentObserver(
				mRoomsChangesObserver);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_SELECTED_LEVEL, mLevel);
		outState.putBoolean(STATE_SELECTED_SATELLITE, mMapView.isSatellite());
	}

	public void panLeft(float screenFraction) {
		final int longitudeSpan = mMapView.getLongitudeSpan();
		final GeoPoint center = mMapView.getMapCenter();
		final GeoPoint newCenter = new GeoPoint(center.getLatitudeE6(),
				center.getLongitudeE6()
						+ (int) (longitudeSpan * screenFraction));
		mMapController.animateTo(newCenter);
	}

	public void panTop(float screenFraction) {
		final int latitudeSpan = mMapView.getLatitudeSpan();
		final GeoPoint center = mMapView.getMapCenter();
		final GeoPoint newCenter = new GeoPoint(center.getLatitudeE6()
				+ (int) (latitudeSpan * screenFraction),
				center.getLongitudeE6());
		mMapController.animateTo(newCenter);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), Rooms.CONTENT_URI,
				RoomsQuery.PROJECTION, Rooms.ROOM_LEVEL + "=?",
				new String[] { String.valueOf(mLevel) },
				RoomsQuery.DEFAULT_SORT);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mOverlays.clear();

		mOverlays.add(new MetropolisOverlay());

		while (data.moveToNext()) {
			final String roomId = data.getString(RoomsQuery.ROOM_ID);
			final String roomName = data.getString(RoomsQuery.ROOM_NAME);
			final int selectable = data.getInt(RoomsQuery.ROOM_SELECTABLE);

			final String encodedPoly = data
					.getString(RoomsQuery.ROOM_ENCODED_POLY);
			if (!TextUtils.isEmpty(encodedPoly)) {
				final RoomOverlay overlay = new RoomOverlay(roomName,
						selectable == 1, encodedPoly, roomId.equals(mRoomId));
				overlay.setOnTapListener(new OnTapListener() {
					@Override
					public void onTap() {
						final Uri sessionsUri = Rooms
								.buildSessionsDirUri(roomId);
						AnalyticsUtils.getInstance(getActivity()).trackEvent(
								"Map", "Click", roomName, 0);
						final Intent intent = new Intent(Intent.ACTION_VIEW,
								sessionsUri);
						final String title = getResources().getString(
								R.string.title_sessions_in, roomName);
						intent.putExtra(Intent.EXTRA_TITLE, title);
						((BaseActivity) getSupportActivity())
								.openActivityOrFragment(intent);
					}
				});
				mOverlays.add(overlay);
			}
		}

		mMapView.postInvalidate();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mOverlays.clear();
	}

	private OnClickListener mLevelClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (mLevel0.isSelected() && mLevel1.equals(v)) {
				mLevel0.setSelected(false);
				mLevel1.setSelected(true);
				mLevel = 1;
				getLoaderManager().restartLoader(0, null, MapFragment.this);
			} else if (mLevel1.isSelected() && mLevel0.equals(v)) {
				mLevel0.setSelected(true);
				mLevel1.setSelected(false);
				mLevel = 0;
				getLoaderManager().restartLoader(0, null, MapFragment.this);
			}
		}
	};

	private OnClickListener mSatteliteClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Button button = (Button) v;
			mSatellite = !button.isSelected();
			button.setSelected(mSatellite);
			mMapView.setSatellite(mSatellite);
		}
	};

	private ContentObserver mRoomsChangesObserver = new ContentObserver(
			new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			if (getActivity() != null) {
				getLoaderManager().restartLoader(0, null, MapFragment.this);
			}
		}
	};

	/**
	 * {@link Rooms} query parameters.
	 */
	private interface RoomsQuery {
		String DEFAULT_SORT = CfpContract.Rooms.DEFAULT_SORT;

		String[] PROJECTION = { BaseColumns._ID, CfpContract.Rooms.ROOM_ID,
				CfpContract.Rooms.ROOM_NAME,
				CfpContract.Rooms.ROOM_ENCODED_POLY,
				CfpContract.Rooms.ROOM_SELECTABLE };

		int _ID = 0;
		int ROOM_ID = 1;
		int ROOM_NAME = 2;
		int ROOM_ENCODED_POLY = 3;
		int ROOM_SELECTABLE = 4;
	}

}
