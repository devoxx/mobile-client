/*
 * Copyright 2011 Google Inc.
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

import java.io.InputStream;

import net.peterkuterna.android.apps.devoxxfrsched.io.XmlHandler;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * Various utility methods used by {@link XmlHandler} implementations.
 */
public class XmlHandlerUtils {

	private static XmlPullParserFactory sFactory;

	/**
	 * Build and return a new {@link XmlPullParser} with the given
	 * {@link InputStream} assigned to it.
	 */
	public static XmlPullParser newPullParser(InputStream input)
			throws XmlPullParserException {
		if (sFactory == null) {
			sFactory = XmlPullParserFactory.newInstance();
		}
		final XmlPullParser parser = sFactory.newPullParser();
		parser.setInput(input, null);
		return parser;
	}

}
