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

package net.peterkuterna.android.apps.devoxxfrsched.ui;

import net.peterkuterna.android.apps.devoxxfrsched.R;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.Tracks;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.provider.BaseColumns;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class TracksAdapter extends CursorAdapter {

	private static final int ALL_ITEM_ID = Integer.MAX_VALUE;

	private Context mContext;
	private boolean mHasAllItem;
	private int mPositionDisplacement;

	public TracksAdapter(Context context, Cursor c) {
		super(context, c, 0);

		mContext = context;
	}

	public void setHasAllItem(boolean hasAllItem) {
		mHasAllItem = hasAllItem;
		mPositionDisplacement = mHasAllItem ? 1 : 0;
	}

	@Override
	public int getCount() {
		int count = super.getCount();
		return count > 0 ? count + mPositionDisplacement : count;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		if (mHasAllItem && position == 0) {
			if (convertView == null) {
				convertView = LayoutInflater.from(mContext).inflate(
						R.layout.list_item_track, parent, false);
			}

			// Custom binding for the first item
			((TextView) convertView.findViewById(android.R.id.text1))
					.setText("("
							+ mContext.getResources().getString(
									R.string.all_sessions_title) + ")");
			convertView.findViewById(android.R.id.icon1).setVisibility(
					View.INVISIBLE);

			return convertView;
		}

		View v = super.getDropDownView(position - mPositionDisplacement,
				convertView, parent);
		v.findViewById(android.R.id.icon1).setVisibility(View.VISIBLE);
		return v;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (mHasAllItem && position == 0) {
			if (convertView == null) {
				convertView = LayoutInflater.from(mContext).inflate(
						R.layout.list_item_track, parent, false);
			}

			// Custom binding for the first item
			((TextView) convertView.findViewById(android.R.id.text1))
					.setText("("
							+ mContext.getResources().getString(
									R.string.all_sessions_title) + ")");
			convertView.findViewById(android.R.id.icon1).setVisibility(
					View.INVISIBLE);

			return convertView;
		}

		return super.getView(position - mPositionDisplacement, convertView,
				parent);
	}

	@Override
	public Object getItem(int position) {
		if (mHasAllItem && position == 0) {
			return null;
		}
		return super.getItem(position - mPositionDisplacement);
	}

	@Override
	public long getItemId(int position) {
		if (mHasAllItem && position == 0) {
			return ALL_ITEM_ID;
		}
		return super.getItemId(position - mPositionDisplacement);
	}

	@Override
	public boolean isEnabled(int position) {
		if (mHasAllItem && position == 0) {
			return true;
		}
		return super.isEnabled(position - mPositionDisplacement);
	}

	@Override
	public int getViewTypeCount() {
		// Add an item type for the "All" view.
		return super.getViewTypeCount() + 1;
	}

	@Override
	public int getItemViewType(int position) {
		if (mHasAllItem && position == 0) {
			return getViewTypeCount() - 1;
		}
		return super.getItemViewType(position - mPositionDisplacement);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return LayoutInflater.from(context).inflate(R.layout.list_item_track,
				parent, false);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		final TextView textView = (TextView) view
				.findViewById(android.R.id.text1);
		textView.setText(cursor.getString(TracksQuery.TRACK_NAME));

		// Assign track color to visible block
		final ImageView iconView = (ImageView) view
				.findViewById(android.R.id.icon1);
		iconView.setImageDrawable(new ColorDrawable(cursor
				.getInt(TracksQuery.TRACK_COLOR)));
	}

	/**
	 * {@link Tracks} query parameters.
	 */
	public interface TracksQuery {
		int _TOKEN = 0x1;

		String[] PROJECTION = { BaseColumns._ID, CfpContract.Tracks.TRACK_ID,
				CfpContract.Tracks.TRACK_NAME,
				CfpContract.Tracks.TRACK_DESCRIPTION,
				CfpContract.Tracks.TRACK_COLOR, };

		String[] PROJECTION_WITH_SESSIONS_COUNT = { BaseColumns._ID,
				CfpContract.Tracks.TRACK_ID, CfpContract.Tracks.TRACK_NAME,
				CfpContract.Tracks.TRACK_DESCRIPTION,
				CfpContract.Tracks.TRACK_COLOR,
				CfpContract.Tracks.SESSIONS_COUNT, };

		int _ID = 0;
		int TRACK_ID = 1;
		int TRACK_NAME = 2;
		int TRACK_ABSTRACT = 3;
		int TRACK_COLOR = 4;
	}

}
