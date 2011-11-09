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
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class SessionLinksFragment extends ProgressFragment implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private ViewGroup mRootView;
	private String mTitleString;

	public static SessionLinksFragment newInstance(String sessionId) {
		SessionLinksFragment f = new SessionLinksFragment();
		Bundle args = new Bundle();
		args.putString("sessionId", sessionId);
		f.setArguments(args);
		return f;
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater,
			ViewGroup container, Bundle savedInstanceState) {
		mRootView = (ViewGroup) inflater.inflate(
				R.layout.fragment_session_links, null);
		return mRootView;
	}

	@Override
	int getContentResourceId() {
		return R.id.links_scroller;
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
	}

	private void updateLinksTab(Cursor cursor) {
		ViewGroup container = (ViewGroup) mRootView
				.findViewById(R.id.links_container);

		// Remove all views but the 'empty' view
		int childCount = container.getChildCount();
		if (childCount > 1) {
			container.removeViews(1, childCount - 1);
		}

		LayoutInflater inflater = getLayoutInflater(null);

		boolean hasLinks = false;
		for (int i = 0; i < SessionsQuery.LINKS_INDICES.length; i++) {
			final String url = cursor.getString(SessionsQuery.LINKS_INDICES[i]);
			if (!TextUtils.isEmpty(url)) {
				hasLinks = true;
				ViewGroup linkContainer = (ViewGroup) inflater.inflate(
						R.layout.list_item_session_link, container, false);
				((TextView) linkContainer.findViewById(R.id.link_text))
						.setText(SessionsQuery.LINKS_TITLES[i]);
				final int linkTitleIndex = i;
				linkContainer.setOnClickListener(new View.OnClickListener() {
					public void onClick(View view) {
						fireLinkEvent(SessionsQuery.LINKS_TITLES[linkTitleIndex]);
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri
								.parse(url));
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
						startActivity(intent);

					}
				});

				container.addView(linkContainer);

				// Create separator
				View separatorView = new ImageView(getActivity());
				separatorView.setLayoutParams(new ViewGroup.LayoutParams(
						ViewGroup.LayoutParams.FILL_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT));
				separatorView
						.setBackgroundResource(android.R.drawable.divider_horizontal_bright);
				container.addView(separatorView);
			}
		}

		container.findViewById(R.id.empty_links).setVisibility(
				hasLinks ? View.GONE : View.VISIBLE);
	}

	/*
	 * Event structure: Category -> "Session Details" Action -> Link Text Label
	 * -> Session's Title Value -> 0.
	 */
	public void fireLinkEvent(int actionId) {
		AnalyticsUtils.getInstance(getActivity()).trackEvent("Link Details",
				getActivity().getString(actionId), mTitleString, 0);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		final String sessionId = getArguments().getString("sessionId");
		final Uri uri = Sessions.buildSessionUri(sessionId);
		return new CursorLoader(getActivity(), uri, SessionsQuery.PROJECTION,
				null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (!data.moveToFirst()) {
			return;
		}

		mTitleString = data.getString(SessionsQuery.TITLE);

		updateLinksTab(data);

		if (getView() != null) {
			if (isResumed()) {
				setContentShown(true);
			} else {
				setContentShownNoAnimation(true);
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	/**
	 * {@link Sessions} query parameters.
	 */
	private interface SessionsQuery {

		String[] PROJECTION = { CfpContract.Sessions.SESSION_TITLE,
				CfpContract.Sessions.SESSION_URL, };

		int TITLE = 0;
		int URL = 1;

		int[] LINKS_INDICES = { URL, };

		int[] LINKS_TITLES = { R.string.session_link_main, };

	}

}
