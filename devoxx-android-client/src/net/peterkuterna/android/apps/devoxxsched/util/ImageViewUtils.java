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

package net.peterkuterna.android.apps.devoxxsched.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.widget.ImageView;

public class ImageViewUtils {

	/**
	 * Sets a new {@link Bitmap} on an {@link ImageView} with a fade.
	 * 
	 * @param iv
	 * @param bitmap
	 */
	public static void setBitmapWithFade(ImageView iv, Bitmap bitmap) {
		Resources resources = iv.getResources();
		BitmapDrawable bitmapDrawable = new BitmapDrawable(resources, bitmap);
		setDrawableWithFade(iv, bitmapDrawable);
	}

	/**
	 * Sets a new {@link Drawable} on an {@link ImageView} with a fade.
	 * 
	 * @param iv
	 * @param bitmap
	 */
	public static void setDrawableWithFade(ImageView iv, Drawable drawable) {
		Drawable currentDrawable = iv.getDrawable();
		if (currentDrawable != null) {
			Drawable[] arrayDrawable = new Drawable[2];
			arrayDrawable[0] = currentDrawable;
			arrayDrawable[1] = drawable;
			TransitionDrawable transitionDrawable = new TransitionDrawable(
					arrayDrawable);
			transitionDrawable.setCrossFadeEnabled(true);
			iv.setImageDrawable(transitionDrawable);
			transitionDrawable.startTransition(250);
		} else {
			iv.setImageDrawable(drawable);
		}
	}

}
