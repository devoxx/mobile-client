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
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Speakers;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Tags;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.ExpandableTextView;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.ExpandableTextView.OnFirstDrawListener;
import net.peterkuterna.android.apps.devoxxsched.util.AnalyticsUtils;
import net.peterkuterna.android.apps.devoxxsched.util.ImageDownloader;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class SessionSummaryFragment extends ProgressFragment {

	private ViewGroup mRootView;
	private ExpandableTextView mAbstract;
	private TextView mAbstractMore;
	private TextView mExperience;

	private ImageDownloader mImageDownloader;

	private boolean mHasSummaryContent = false;
	private boolean mTagsLoaded = false;
	private boolean mSessionAbstractLoaded = false;
	private boolean mSessionSpeakersLoaded = false;

	public static SessionSummaryFragment newInstance(String sessionId) {
		SessionSummaryFragment f = new SessionSummaryFragment();
		Bundle args = new Bundle();
		args.putString("sessionId", sessionId);
		f.setArguments(args);
		return f;
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater,
			ViewGroup container, Bundle savedInstanceState) {
		mRootView = (ViewGroup) inflater.inflate(
				R.layout.fragment_session_summary, null);

		mAbstract = (ExpandableTextView) mRootView
				.findViewById(R.id.session_abstract);
		mAbstractMore = (TextView) mRootView
				.findViewById(R.id.session_abstract_more);
		mExperience = (TextView) mRootView
				.findViewById(R.id.session_experience);

		return mRootView;
	}

	@Override
	int getContentResourceId() {
		return R.id.summary_content;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mImageDownloader = new ImageDownloader(getActivity());

		setEmptyText(getString(R.string.empty_session_detail));
		setContentShown(false);

		getLoaderManager().initLoader(TagsQuery._TOKEN, null,
				mSessionTagsCallback);
		getLoaderManager().initLoader(SessionsQuery._TOKEN, null,
				mSessionAbstractCallback);
		getLoaderManager().initLoader(SpeakersQuery._TOKEN, null,
				mSessionSpeakersCallback);
	}

	@Override
	public void onResume() {
		super.onResume();

		getLoaderManager().restartLoader(TagsQuery._TOKEN, null,
				mSessionTagsCallback);
		getLoaderManager().restartLoader(SessionsQuery._TOKEN, null,
				mSessionAbstractCallback);
		getLoaderManager().restartLoader(SpeakersQuery._TOKEN, null,
				mSessionSpeakersCallback);
	}

	private synchronized void isAllLoaded(boolean animate) {
		final boolean allLoaded = (mTagsLoaded && mSessionAbstractLoaded && mSessionSpeakersLoaded);
		if (allLoaded) {
			showEmptyText(!mHasSummaryContent);
			mRootView.setVisibility(mHasSummaryContent ? View.VISIBLE
					: View.GONE);

			if (animate) {
				setContentShown(true);
			} else {
				setContentShownNoAnimation(true);
			}
		}
	}

	private LoaderManager.LoaderCallbacks<Cursor> mSessionTagsCallback = new LoaderCallbacks<Cursor>() {

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			final String sessionId = getArguments().getString("sessionId");
			final Uri tagsUri = CfpContract.Sessions.buildTagsDirUri(sessionId);
			return new CursorLoader(getActivity(), tagsUri,
					TagsQuery.PROJECTION, null, null, Tags.DEFAULT_SORT);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			final ViewGroup tagsGroup = (ViewGroup) mRootView
					.findViewById(R.id.tags_container);
			tagsGroup.removeAllViews();
			final LayoutInflater inflater = getActivity().getLayoutInflater();

			while (data.moveToNext()) {
				final String tagId = data.getString(TagsQuery.TAG_ID);
				final String tagName = data.getString(TagsQuery.TAG_NAME);

				final View tagView = inflater.inflate(R.layout.tag, tagsGroup,
						false);
				final TextView tagNameView = (TextView) tagView
						.findViewById(R.id.tag_name);

				tagNameView.setText(tagName);
				tagNameView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						final Uri sessionsUri = Tags.buildSessionsDirUri(tagId);
						final CharSequence title = getString(
								R.string.title_sessions_for, tagName);
						AnalyticsUtils.getInstance(getActivity()).trackEvent(
								"Session", "Keyword Click", tagName, 0);
						final Intent intent = new Intent(Intent.ACTION_VIEW,
								sessionsUri);
						intent.putExtra(Intent.EXTRA_TITLE, title);
						((BaseActivity) getSupportActivity())
								.openActivityOrFragment(intent);
					}
				});

				tagsGroup.addView(tagView);

				mHasSummaryContent = true;
			}

			mTagsLoaded = true;
			if (getView() != null) {
				if (isResumed()) {
					isAllLoaded(true);
				} else {
					isAllLoaded(false);
				}
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
		}

	};

	private LoaderManager.LoaderCallbacks<Cursor> mSessionAbstractCallback = new LoaderCallbacks<Cursor>() {

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			final String sessionId = getArguments().getString("sessionId");
			final Uri uri = Sessions.buildSessionUri(sessionId);
			return new CursorLoader(getActivity(), uri,
					SessionsQuery.PROJECTION, null, null, null);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			if (!data.moveToFirst()) {
				return;
			}

			mHasSummaryContent = true;

			final String sessionSummary = data.getString(SessionsQuery.SUMMARY);
			UIUtils.setTextMaybeHtml(mAbstract, sessionSummary);

			mAbstract.setOnFirstDrawListener(new OnFirstDrawListener() {
				@Override
				public void onFirstDraw(View v) {
					final ExpandableTextView etv = (ExpandableTextView) v;
					if (etv.getLineCount() > 5) {
						final int padding = (int) (getResources().getDimension(
								R.dimen.more_less_btn_padding) + 0.5f);
						final int padding_arrow = (int) (getResources()
								.getDimension(
										R.dimen.more_less_btn_padding_arrow) + 0.5f);
						mAbstractMore.setText(R.string.session_more);
						mAbstractMore
								.setBackgroundResource(R.drawable.btn_more);
						mAbstractMore.setPadding(padding, padding,
								padding_arrow, padding);
						mAbstractMore.setVisibility(View.VISIBLE);
						mAbstractMore.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								mAbstract.toggleExpand();
								((TextView) v).setText(mAbstract.isExpanded() ? R.string.session_less
										: R.string.session_more);
								((TextView) v).setBackgroundResource(mAbstract
										.isExpanded() ? R.drawable.btn_less
										: R.drawable.btn_more);
								if (mAbstract.isExpanded()) {
									mAbstractMore.setPadding(padding_arrow,
											padding, padding, padding);
								} else {
									mAbstractMore.setPadding(padding, padding,
											padding_arrow, padding);
								}
							}
						});
					}
				}
			});

			final View requirementsBlock = mRootView
					.findViewById(R.id.session_experience_block);
			final String experience = data.getString(SessionsQuery.EXPERIENCE);
			if (!TextUtils.isEmpty(experience)) {
				UIUtils.setTextMaybeHtml(mExperience, experience);
				requirementsBlock.setVisibility(View.VISIBLE);
			} else {
				requirementsBlock.setVisibility(View.GONE);
			}

			mSessionAbstractLoaded = true;
			if (getView() != null) {
				if (isResumed()) {
					isAllLoaded(true);
				} else {
					isAllLoaded(false);
				}
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
		}

	};

	private LoaderManager.LoaderCallbacks<Cursor> mSessionSpeakersCallback = new LoaderCallbacks<Cursor>() {

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			final String sessionId = getArguments().getString("sessionId");
			final Uri speakersUri = CfpContract.Sessions
					.buildSpeakersDirUri(sessionId);
			return new CursorLoader(getActivity(), speakersUri,
					SpeakersQuery.PROJECTION, null, null, null);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			final ViewGroup speakersGroup = (ViewGroup) mRootView
					.findViewById(R.id.session_speakers_block);
			speakersGroup.removeAllViews();
			final LayoutInflater inflater = getActivity().getLayoutInflater();

			while (data.moveToNext()) {
				final String speakerFirstName = data
						.getString(SpeakersQuery.SPEAKER_FIRSTNAME);
				final String speakerLastName = data
						.getString(SpeakersQuery.SPEAKER_LASTNAME);
				final String speakerName = speakerFirstName + " "
						+ speakerLastName;
				if (TextUtils.isEmpty(speakerName)) {
					continue;
				}

				final String speakerImageUrl = data
						.getString(SpeakersQuery.SPEAKER_IMAGE_URL);
				final String speakerCompany = data
						.getString(SpeakersQuery.SPEAKER_COMPANY);
				final String speakerBio = data
						.getString(SpeakersQuery.SPEAKER_BIO);

				String speakerHeader = speakerName;
				if (!TextUtils.isEmpty(speakerCompany)) {
					speakerHeader += ", " + speakerCompany;
				}

				final View speakerView = inflater.inflate(
						R.layout.speaker_detail, speakersGroup, false);
				final TextView speakerHeaderView = (TextView) speakerView
						.findViewById(R.id.speaker_header);
				final ImageView speakerImgView = (ImageView) speakerView
						.findViewById(R.id.speaker_image);
				final ExpandableTextView speakerBioView = (ExpandableTextView) speakerView
						.findViewById(R.id.speaker_bio);
				final TextView speakerMore = (TextView) speakerView
						.findViewById(R.id.speaker_more);

				if (!TextUtils.isEmpty(speakerImageUrl)) {
					if (mImageDownloader != null) {
						mImageDownloader.download(speakerImageUrl,
								speakerImgView, R.drawable.speaker_image_empty);
					}
				}

				speakerHeaderView.setText(speakerHeader);
				UIUtils.setTextMaybeHtml(speakerBioView, speakerBio);

				speakerBioView
						.setOnFirstDrawListener(new OnFirstDrawListener() {
							@Override
							public void onFirstDraw(View v) {
								final ExpandableTextView etv = (ExpandableTextView) v;
								if (etv.getLineCount() > 5) {
									final int padding = (int) (getResources()
											.getDimension(
													R.dimen.more_less_btn_padding) + 0.5f);
									final int padding_arrow = (int) (getResources()
											.getDimension(
													R.dimen.more_less_btn_padding_arrow) + 0.5f);
									speakerMore.setText(R.string.session_more);
									speakerMore
											.setBackgroundResource(R.drawable.btn_more);
									speakerMore.setPadding(padding, padding,
											padding_arrow, padding);
									speakerMore.setVisibility(View.VISIBLE);
									speakerMore
											.setOnClickListener(new OnClickListener() {
												@Override
												public void onClick(View v) {
													speakerBioView
															.toggleExpand();
													((TextView) v)
															.setText(speakerBioView
																	.isExpanded() ? R.string.session_less
																	: R.string.session_more);
													((TextView) v)
															.setBackgroundResource(speakerBioView
																	.isExpanded() ? R.drawable.btn_less
																	: R.drawable.btn_more);
													if (speakerBioView
															.isExpanded()) {
														speakerMore.setPadding(
																padding_arrow,
																padding,
																padding,
																padding);
													} else {
														speakerMore.setPadding(
																padding,
																padding,
																padding_arrow,
																padding);
													}
												}
											});
								}
							}
						});

				speakersGroup.addView(speakerView);

				mHasSummaryContent = true;
			}

			mSessionSpeakersLoaded = true;
			if (getView() != null) {
				if (isResumed()) {
					isAllLoaded(true);
				} else {
					isAllLoaded(false);
				}
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
		}

	};

	/**
	 * {@link Tags} query parameters.
	 */
	private interface TagsQuery {

		int _TOKEN = 0x0;

		String[] PROJECTION = { CfpContract.Tags.TAG_ID,
				CfpContract.Tags.TAG_NAME, };

		int TAG_ID = 0;
		int TAG_NAME = 1;

	}

	/**
	 * {@link Sessions} query parameters.
	 */
	private interface SessionsQuery {
		int _TOKEN = 0x1;

		String[] PROJECTION = { CfpContract.Sessions.SESSION_SUMMARY,
				CfpContract.Sessions.SESSION_EXPERIENCE, };

		int SUMMARY = 0;
		int EXPERIENCE = 1;
	}

	/**
	 * {@link Speakers} query parameters.
	 */
	private interface SpeakersQuery {
		int _TOKEN = 0x2;

		String[] PROJECTION = { CfpContract.Speakers.SPEAKER_FIRSTNAME,
				CfpContract.Speakers.SPEAKER_LASTNAME,
				CfpContract.Speakers.SPEAKER_IMAGE_URL,
				CfpContract.Speakers.SPEAKER_COMPANY,
				CfpContract.Speakers.SPEAKER_BIO, };

		int SPEAKER_FIRSTNAME = 0;
		int SPEAKER_LASTNAME = 1;
		int SPEAKER_IMAGE_URL = 2;
		int SPEAKER_COMPANY = 3;
		int SPEAKER_BIO = 4;
	}

}
