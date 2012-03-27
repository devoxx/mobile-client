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

import java.net.URLEncoder;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import org.codehaus.jackson.JsonParser;
import org.xmlpull.v1.XmlPullParser;

import android.content.ContentProvider;
import android.net.Uri;
import android.text.format.Time;

/**
 * Various utility methods used by {@link JsonParser} and {@link XmlPullParser}
 * implementations.
 */
public class ParserUtils {

	/** Used to sanitize a string to be {@link Uri} safe. */
	private static final Pattern sSanitizePattern = Pattern
			.compile("[^a-z0-9-_]");
	private static final Pattern sParenPattern = Pattern.compile("\\(.*?\\)");

	private static final Pattern sDevoxxNotAllowPattern1 = Pattern
			.compile("[\\?]");
	private static final Pattern sDevoxxNotAllowPattern2 = Pattern
			.compile("[/]");

	private static Time sTime = new Time();

	private static SimpleDateFormat twitterDateFormat = new SimpleDateFormat(
			"EEE',' dd MMM yyyy HH:mm:ss Z", Locale.US);

	private static String BASE_DEVOXX_WEB_URL = "https://www.devoxx.fr/display/FR12/Accueil/";

	/**
	 * Sanitize the given string to be {@link Uri} safe for building
	 * {@link ContentProvider} paths.
	 */
	public static String sanitizeId(String input) {
		return sanitizeId(input, false);
	}

	/**
	 * Sanitize the given string to be {@link Uri} safe for building
	 * {@link ContentProvider} paths.
	 */
	public static String sanitizeId(String input, boolean stripParen) {
		if (input == null)
			return null;
		if (stripParen) {
			// Strip out all parenthetical statements when requested.
			input = sParenPattern.matcher(input).replaceAll("");
		}
		return sSanitizePattern.matcher(input.toLowerCase()).replaceAll("");
	}

	/**
	 * Parse the given string as a RFC 3339 timestamp, returning the value as
	 * milliseconds since the epoch.
	 */
	public static long parseTime(String time) {
		sTime.parse3339(time);
		return sTime.toMillis(false);
	}

	public static long parseDevoxxTime(String time) {
		parseTime(time.replace(' ', 'T') + "00+02:00");
		return sTime.toMillis(false);
	}

	public static long parseTwitterSearchTime(String time) {
		try {
			Date date = twitterDateFormat.parse(time, new ParsePosition(0));
			return date.getTime();
		} catch (IllegalArgumentException e) {
		}
		return 0;
	}

	public static boolean isTalk(String kind) {
		return ("talk".equalsIgnoreCase(kind) || "keynote".equalsIgnoreCase(kind));
	}

	public static boolean isKeynote(String kind) {
		return "keynote".equalsIgnoreCase(kind);
	}

	public static String buildDevoxxWebUrl(String text) {
		text = sDevoxxNotAllowPattern1.matcher(text).replaceAll("");
		text = sDevoxxNotAllowPattern2.matcher(text).replaceAll(" ");
		return BASE_DEVOXX_WEB_URL + URLEncoder.encode(text);
	}

}
