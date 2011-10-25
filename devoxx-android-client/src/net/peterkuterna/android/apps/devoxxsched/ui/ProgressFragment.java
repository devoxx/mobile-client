/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2011 Peter Kuterna
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

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public abstract class ProgressFragment extends Fragment {

	static final int INTERNAL_EMPTY_ID = 0x00ff0001;
	static final int INTERNAL_PROGRESS_CONTAINER_ID = 0x00ff0002;
	static final int INTERNAL_CONTENT_CONTAINER_ID = 0x00ff0003;

	private View mProgressContainer;
	private View mContentContainer;
	private View mContent;
	private TextView mStandardEmptyView;
	private CharSequence mEmptyText;
	private boolean mContentShown;

	abstract View onCreateContentView(LayoutInflater inflater,
			ViewGroup container, Bundle savedInstanceState);

	abstract int getContentResourceId();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final Context context = getActivity();

		FrameLayout root = new FrameLayout(context);

		LinearLayout pframe = new LinearLayout(context);
		pframe.setId(INTERNAL_PROGRESS_CONTAINER_ID);
		pframe.setOrientation(LinearLayout.VERTICAL);
		pframe.setVisibility(View.GONE);
		pframe.setGravity(Gravity.CENTER);

		ProgressBar progress = new ProgressBar(context, null,
				android.R.attr.progressBarStyleLarge);
		pframe.addView(progress, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));

		root.addView(pframe, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.FILL_PARENT));

		FrameLayout cframe = new FrameLayout(context);
		cframe.setId(INTERNAL_CONTENT_CONTAINER_ID);

		TextView tv = new TextView(context);
		tv.setId(INTERNAL_EMPTY_ID);
		tv.setGravity(Gravity.CENTER);
		cframe.addView(tv, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.FILL_PARENT));

		View contentView = onCreateContentView(inflater, cframe,
				savedInstanceState);
		cframe.addView(contentView, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.FILL_PARENT));

		root.addView(cframe, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.FILL_PARENT));

		root.setLayoutParams(new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.FILL_PARENT));

		return root;
	}

	private void ensureContent() {
		if (mContent != null) {
			return;
		}
		View root = getView();
		if (root == null) {
			throw new IllegalStateException("Content view not yet created");
		}

		mStandardEmptyView = (TextView) root.findViewById(INTERNAL_EMPTY_ID);
		mStandardEmptyView.setVisibility(View.GONE);
		mProgressContainer = root.findViewById(INTERNAL_PROGRESS_CONTAINER_ID);
		mContentContainer = root.findViewById(INTERNAL_CONTENT_CONTAINER_ID);

		mContent = mContentContainer.findViewById(getContentResourceId());
		if (mContent == null) {
			throw new IllegalStateException("Content view not created");
		}

		if (mEmptyText != null) {
			mStandardEmptyView.setText(mEmptyText);
		}

		mContentShown = true;
	}

	public void setEmptyText(CharSequence text) {
		ensureContent();
		if (mStandardEmptyView == null) {
			throw new IllegalStateException(
					"Can't be used with a custom content view");
		}
		mEmptyText = text;
		mStandardEmptyView.setText(text);
	}

	public void showEmptyText(boolean show) {
		ensureContent();
		if (mStandardEmptyView == null) {
			throw new IllegalStateException(
					"Can't be used with a custom content view");
		}
		mStandardEmptyView.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	public void setContentShown(boolean shown) {
		setContentShown(shown, true);
	}

	public void setContentShownNoAnimation(boolean shown) {
		setContentShown(shown, false);
	}

	private void setContentShown(boolean shown, boolean animate) {
		ensureContent();
		if (mProgressContainer == null) {
			throw new IllegalStateException(
					"Can't be used with a custom content view");
		}
		if (mContentShown == shown) {
			return;
		}
		mContentShown = shown;
		if (shown) {
			if (animate) {
				mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
						getActivity(), android.R.anim.fade_out));
				mContentContainer.startAnimation(AnimationUtils.loadAnimation(
						getActivity(), android.R.anim.fade_in));
			} else {
				mProgressContainer.clearAnimation();
				mContentContainer.clearAnimation();
			}
			mProgressContainer.setVisibility(View.GONE);
			mContentContainer.setVisibility(View.VISIBLE);
		} else {
			if (animate) {
				mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
						getActivity(), android.R.anim.fade_in));
				mContentContainer.startAnimation(AnimationUtils.loadAnimation(
						getActivity(), android.R.anim.fade_out));
			} else {
				mProgressContainer.clearAnimation();
				mContentContainer.clearAnimation();
			}
			mProgressContainer.setVisibility(View.VISIBLE);
			mContentContainer.setVisibility(View.GONE);
		}
	}

}
