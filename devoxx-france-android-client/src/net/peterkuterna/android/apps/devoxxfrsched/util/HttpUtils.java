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

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.zip.GZIPInputStream;

import net.peterkuterna.android.apps.devoxxfrsched.R;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.format.DateUtils;

/**
 * Class to deliver a {@link HttpClient} that accepts the multi-host SSL
 * certificate of Devoxx.
 */
public class HttpUtils {

	private static final int SECOND_IN_MILLIS = (int) DateUtils.SECOND_IN_MILLIS;

	private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	private static final String ENCODING_GZIP = "gzip";

	/**
	 * Generate and return a {@link HttpClient} configured for general use,
	 * including setting an application-specific user-agent string.
	 */
	public static HttpClient getHttpClient(Context context) {
		final DefaultHttpClient client = new DevoxxHttpClient(context);

		client.getParams().setParameter(CoreProtocolPNames.USER_AGENT,
				buildUserAgent(context));

		client.addRequestInterceptor(new HttpRequestInterceptor() {
			public void process(HttpRequest request, HttpContext context) {
				// Add header to accept gzip content
				if (!request.containsHeader(HEADER_ACCEPT_ENCODING)) {
					request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
				}
			}
		});

		client.addResponseInterceptor(new HttpResponseInterceptor() {
			public void process(HttpResponse response, HttpContext context) {
				// Inflate any responses compressed with gzip
				final HttpEntity entity = response.getEntity();
				final Header encoding = entity.getContentEncoding();
				if (encoding != null) {
					for (HeaderElement element : encoding.getElements()) {
						if (element.getName().equalsIgnoreCase(ENCODING_GZIP)) {
							response.setEntity(new InflatingEntity(response
									.getEntity()));
							break;
						}
					}
				}
			}
		});

		return client;
	}

	/**
	 * Build and return a user-agent string that can identify this application
	 * to remote servers. Contains the package name and version code.
	 */
	public static String buildUserAgent(Context context) {
		try {
			final PackageManager manager = context.getPackageManager();
			final PackageInfo info = manager.getPackageInfo(
					context.getPackageName(), 0);

			// Some APIs require "(gzip)" in the user-agent string.
			return info.packageName + "/" + info.versionName + " ("
					+ info.versionCode + ") (gzip)";
		} catch (NameNotFoundException e) {
			return null;
		}
	}

	/**
	 * Simple {@link HttpEntityWrapper} that inflates the wrapped
	 * {@link HttpEntity} by passing it through {@link GZIPInputStream}.
	 */
	private static class InflatingEntity extends HttpEntityWrapper {
		public InflatingEntity(HttpEntity wrapped) {
			super(wrapped);
		}

		@Override
		public InputStream getContent() throws IOException {
			return new GZIPInputStream(wrappedEntity.getContent());
		}

		@Override
		public long getContentLength() {
			return -1;
		}
	}

	private static class DevoxxHttpClient extends DefaultHttpClient {

		final Context mContext;

		public DevoxxHttpClient(Context mContext) {
			this.mContext = mContext;
		}

		@Override
		protected ClientConnectionManager createClientConnectionManager() {
			final HttpParams params = new BasicHttpParams();

			// Use generous timeouts for slow mobile networks
			HttpConnectionParams.setConnectionTimeout(params,
					180 * SECOND_IN_MILLIS);
			HttpConnectionParams.setSoTimeout(params, 180 * SECOND_IN_MILLIS);

			HttpConnectionParams.setSocketBufferSize(params, 8192);
			HttpProtocolParams.setUserAgent(params, buildUserAgent(mContext));

			final SchemeRegistry schemeReg = new SchemeRegistry();
			schemeReg.register(new Scheme("http", PlainSocketFactory
					.getSocketFactory(), 80));
			schemeReg.register(new Scheme("https", newSslSocketFactory(), 443));
			return new ThreadSafeClientConnManager(params, schemeReg);
		}

		private SSLSocketFactory newSslSocketFactory() {
			try {
				KeyStore trusted = KeyStore.getInstance("BKS");
				InputStream in = mContext.getResources().openRawResource(
						R.raw.keystore);
				try {
					trusted.load(in, "ez24get".toCharArray());
				} finally {
					in.close();
				}
				return new SSLSocketFactory(trusted);
			} catch (Exception e) {
				throw new AssertionError(e);
			}
		}

	}

}
