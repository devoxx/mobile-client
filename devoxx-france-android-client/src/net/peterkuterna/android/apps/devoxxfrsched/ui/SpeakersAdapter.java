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
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.Speakers;
import net.peterkuterna.android.apps.devoxxfrsched.util.ImageDownloader;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * {@link CursorAdapter} that renders a {@link SpeakersQuery}.
 */
class SpeakersAdapter extends CursorAdapter {

	private final ImageDownloader mImageDownloader;
	private final int mSpeakerResource;

	public SpeakersAdapter(Context context, Cursor cursor, int speakerResource) {
		super(context, cursor, 0);

		this.mSpeakerResource = speakerResource;
		this.mImageDownloader = new ImageDownloader(context);
	}

	protected void findAndCacheViews(View view) {
		SpeakerItemViews views = new SpeakerItemViews();
		views.imageView = (ImageView) view.findViewById(R.id.image);
		views.nameView = (TextView) view.findViewById(R.id.speaker);
		views.companyView = (TextView) view.findViewById(R.id.company);
		view.setTag(views);
	}

	/** {@inheritDoc} */
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View v = LayoutInflater.from(context).inflate(mSpeakerResource, parent,
				false);
		findAndCacheViews(v);
		return v;
	}

	/** {@inheritDoc} */
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		SpeakerItemViews views = (SpeakerItemViews) view.getTag();

		final String firstName = cursor
				.getString(SpeakersQuery.SPEAKER_FIRSTNAME);
		final String lastName = cursor
				.getString(SpeakersQuery.SPEAKER_LASTNAME);
		final String company = cursor.getString(SpeakersQuery.SPEAKER_COMPANY);
		final String imageUrl = cursor
				.getString(SpeakersQuery.SPEAKER_IMAGE_URL);

		final StringBuilder sb = new StringBuilder();
		if (!TextUtils.isEmpty(firstName)) {
			sb.append(firstName);
			sb.append(" ");
		}
		sb.append(lastName);

		views.nameView.setText(sb.toString());
		if (views.companyView != null) {
			views.companyView.setText(company);
		}
		mImageDownloader.download(imageUrl, views.imageView,
				R.drawable.speaker_image_empty);
	}

	public static final class SpeakerItemViews {
		ImageView imageView;
		TextView nameView;
		TextView companyView;
	}

	/**
	 * {@link Speakers} query parameters.
	 */
	public interface SpeakersQuery {
		int _TOKEN = 0x1;

		String[] PROJECTION = { BaseColumns._ID,
				CfpContract.Speakers.SPEAKER_ID,
				CfpContract.Speakers.SPEAKER_FIRSTNAME,
				CfpContract.Speakers.SPEAKER_LASTNAME,
				CfpContract.Speakers.SPEAKER_COMPANY,
				CfpContract.Speakers.SPEAKER_BIO,
				CfpContract.Speakers.SPEAKER_IMAGE_URL, };

		int _ID = 0;
		int SPEAKER_ID = 1;
		int SPEAKER_FIRSTNAME = 2;
		int SPEAKER_LASTNAME = 3;
		int SPEAKER_COMPANY = 4;
		int SPEAKER_BIO = 5;
		int SPEAKER_IMAGE_URL = 6;
	}

}