/*
 * Copyright 2011 Peter Kuterna
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package net.peterkuterna.appengine.apps.devoxxsched.util;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.IOUtils;

public class Md5Calculator {

	private String requestUri;

	public Md5Calculator(final String requestUri) {
		this.requestUri = requestUri;
	}

	public String calculateMd5() {
		final byte[] response = getResponse(requestUri);
		if (response != null) {
			try {
				MessageDigest mdEnc = MessageDigest.getInstance("MD5");
				mdEnc.update(response);
				return new BigInteger(1, mdEnc.digest()).toString(16);
			} catch (NoSuchAlgorithmException e) {
			}
		}
		return null;
	}

	private byte[] getResponse(final String requestUri) {
		try {
			URL url = new URL(requestUri);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(120 * 1000);
			connection
					.setRequestProperty("Cache-Control", "no-cache,max-age=0");
			connection.setRequestProperty("Pragma", "no-cache");
			InputStream response = connection.getInputStream();
			if (connection.getResponseCode() == 200) {
				return IOUtils.toByteArray(response);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

}