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

package net.peterkuterna.android.apps.devoxxfrsched.util;

import java.util.List;
import java.util.TimeZone;

import net.peterkuterna.android.apps.devoxxfrsched.R;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.Blocks;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.Rooms;
import net.peterkuterna.android.apps.devoxxfrsched.ui.BaseActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class UIUtils {

	/**
	 * Time zone to use when formatting all session times. To always use the
	 * phone local time, use {@link TimeZone#getDefault()}.
	 */
	public static TimeZone CONFERENCE_TIME_ZONE = TimeZone
			.getTimeZone("Europe/Brussels");

	public static final long CONFERENCE_START_MILLIS = ParserUtils
			.parseTime("2012-04-18T08:00:00.000+01:00");
	public static final long CONFERENCE_END_MILLIS = ParserUtils
			.parseTime("2012-04-20T18:50:00.000+01:00");

	public static final int NUMBER_DAYS = 3;
	public static final long[] START_DAYS_IN_MILLIS = {
			ParserUtils.parseTime("2012-04-18T00:00:00.000+01:00"),
			ParserUtils.parseTime("2012-04-19T00:00:00.000+01:00"),
			ParserUtils.parseTime("2012-04-20T00:00:00.000+01:00") };

	private static final int DAY_FLAGS = DateUtils.FORMAT_SHOW_WEEKDAY
			| DateUtils.FORMAT_ABBREV_WEEKDAY;
	private static final int TIME_FLAGS = DateUtils.FORMAT_SHOW_TIME;

	private static StyleSpan sBoldSpan = new StyleSpan(Typeface.BOLD);

	/**
	 * Format and return the given {@link Blocks} and {@link Rooms} values using
	 * {@link #CONFERENCE_TIME_ZONE}.
	 */
	public static String formatSessionSubtitle(long blockStart, long blockEnd,
			String roomName, Context context) {
		TimeZone.setDefault(CONFERENCE_TIME_ZONE);

		final CharSequence dayString = DateUtils.formatDateRange(context,
				blockStart, blockEnd, DAY_FLAGS);

		final CharSequence timeString = DateUtils.formatDateRange(context,
				blockStart, blockEnd, TIME_FLAGS);

		return context.getString(R.string.session_subtitle, dayString,
				timeString, roomName);
	}

	/**
	 * Formats the name of speaker in returning a string with the first and last
	 * name.
	 * 
	 * @param firstName
	 * @param lastName
	 * @return
	 */
	public static String formatSpeakerName(String firstName, String lastName) {
		final StringBuilder sb = new StringBuilder();
		if (!TextUtils.isEmpty(firstName)) {
			sb.append(firstName);
			sb.append(" ");
		}
		sb.append(lastName);
		return sb.toString();
	}

	/**
	 * Populate the given {@link TextView} with the requested text, formatting
	 * through {@link Html#fromHtml(String)} when applicable. Also sets
	 * {@link TextView#setMovementMethod} so inline links are handled.
	 */
	public static void setTextMaybeHtml(TextView view, String text) {
		if (TextUtils.isEmpty(text)) {
			view.setText("");
			return;
		}
		if (text.contains("<") && text.contains(">")) {
			view.setText(Html.fromHtml(text));
			view.setMovementMethod(LinkMovementMethod.getInstance());
		} else {
			view.setText(text);
		}
	}

	/**
	 * Sets the color of a session title.
	 * 
	 * @param blockStart
	 * @param blockEnd
	 * @param title
	 * @param subtitle
	 */
	public static void setSessionTitleColor(long blockStart, long blockEnd,
			TextView title, TextView subtitle) {
		long currentTimeMillis = System.currentTimeMillis();
		int colorId = R.color.body_text_1;
		int subColorId = R.color.body_text_2;

		if (currentTimeMillis > blockEnd
				&& currentTimeMillis < CONFERENCE_END_MILLIS) {
			colorId = subColorId = R.color.body_text_disabled;
		}

		final Resources res = title.getResources();
		title.setTextColor(res.getColor(colorId));
		subtitle.setTextColor(res.getColor(subColorId));
	}

	/**
	 * Set title and color of action bar.
	 * 
	 * @param activity
	 * @param title
	 * @param color
	 */
	public static void setActionBarData(SherlockFragmentActivity activity, String title,
			int color) {
		if (activity != null) {
			final ActivityHelper activityHelper = ((BaseActivity) activity)
					.getActivityHelper();
			activity.setTitle(title);
			if (color != -1) {
				activityHelper.setActionBarColor(color);
			}
		}
	}

	/**
	 * Given a snippet string with matching segments surrounded by curly braces,
	 * turn those areas into bold spans, removing the curly braces.
	 */
	public static Spannable buildStyledSnippet(String snippet) {
		final SpannableStringBuilder builder = new SpannableStringBuilder(
				snippet);

		// Walk through string, inserting bold snippet spans
		int startIndex = -1, endIndex = -1, delta = 0;
		while ((startIndex = snippet.indexOf('{', endIndex)) != -1) {
			endIndex = snippet.indexOf('}', startIndex);

			// Remove braces from both sides
			builder.delete(startIndex - delta, startIndex - delta + 1);
			builder.delete(endIndex - delta - 1, endIndex - delta);

			// Insert bold style
			builder.setSpan(sBoldSpan, startIndex - delta,
					endIndex - delta - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			delta += 2;
		}

		return builder;
	}

	public static String getLastUsedTrackID(Context context) {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(context);
		return sp.getString("last_track_id", null);
	}

	public static void setLastUsedTrackID(Context context, String trackID) {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(context);
		sp.edit().putString("last_track_id", trackID).commit();
	}

	private static final int BRIGHTNESS_THRESHOLD = 130;

	/**
	 * Calculate whether a color is light or dark, based on a commonly known
	 * brightness formula.
	 * 
	 * @see {@literal http://en.wikipedia.org/wiki/HSV_color_space%23Lightness}
	 */
	public static boolean isColorDark(int color) {
		return ((30 * Color.red(color) + 59 * Color.green(color) + 11 * Color
				.blue(color)) / 100) <= BRIGHTNESS_THRESHOLD;
	}

	public static boolean isFroyo() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
	}

	public static boolean isHoneycomb() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}

	public static boolean isHoneycombTablet(Context context) {
		// Can use static final constants like HONEYCOMB, declared in later
		// versions
		// of the OS since they are inlined at compile time. This is guaranteed
		// behavior.
		return isHoneycomb()
				&& (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	public static long getCurrentTime(final Context context) {
		// SharedPreferences prefs = context.getSharedPreferences("mock_data",
		// 0);
		// prefs.edit().commit();
		// return prefs.getLong("mock_current_time",
		// System.currentTimeMillis());
		// return START_DAYS_IN_MILLIS[3]+(DateUtils.HOUR_IN_MILLIS * 16);
		 return System.currentTimeMillis();
	}

	public static Drawable getIconForIntent(final Context context, Intent i) {
		PackageManager pm = context.getPackageManager();
		List<ResolveInfo> infos = pm.queryIntentActivities(i,
				PackageManager.MATCH_DEFAULT_ONLY);
		if (infos.size() > 0) {
			return infos.get(0).loadIcon(pm);
		}
		return null;
	}

}
