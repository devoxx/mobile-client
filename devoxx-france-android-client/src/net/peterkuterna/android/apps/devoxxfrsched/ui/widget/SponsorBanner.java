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

package net.peterkuterna.android.apps.devoxxfrsched.ui.widget;

import net.peterkuterna.android.apps.devoxxfrsched.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.ViewAnimator;

public class SponsorBanner extends ViewAnimator {

	private static final String SPONSOR_ASSETS_ARRAY[] = {
			"premium_sponsor_xebia", "premium_sponsor_zenexity",
			"premium_sponsor_ibm", "premium_sponsor_google", };

	public SponsorBanner(Context context) {
		super(context);
		initBanner();
	}

	public SponsorBanner(Context context, AttributeSet attrs) {
		super(context, attrs);
		initBanner();
	}

	protected void initBanner() {
		setInAnimation(getContext(), android.R.anim.fade_in);
		setOutAnimation(getContext(), android.R.anim.fade_out);

		populate();
	}

	public void populate() {
		final int count = SPONSOR_ASSETS_ARRAY.length;
		final Context context = getContext();
		final LayoutInflater inflater = LayoutInflater.from(context);
		for (int i = 0; i < count; i++) {
			final ImageView child = (ImageView) inflater.inflate(
					R.layout.list_item_sponsor_image, this, false);
			final int id = getResources().getIdentifier(
					SPONSOR_ASSETS_ARRAY[i], "drawable",
					context.getPackageName());
			child.setImageResource(id);
			addView(child);
		}
	}

}
