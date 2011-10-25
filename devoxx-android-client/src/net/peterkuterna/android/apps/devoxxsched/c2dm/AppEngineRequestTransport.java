/*
 * Copyright 2010 Google Inc.
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
package net.peterkuterna.android.apps.devoxxsched.c2dm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import net.peterkuterna.android.apps.devoxxsched.ui.SettingsActivity;
import net.peterkuterna.android.apps.devoxxsched.util.HttpUtils;
import net.peterkuterna.android.apps.devoxxsched.util.Prefs;
import net.peterkuterna.android.apps.devoxxsched.util.Prefs.DevoxxPrefs;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;

import com.google.web.bindery.requestfactory.shared.RequestTransport;
import com.google.web.bindery.requestfactory.shared.ServerFailure;

/**
 * An implementation of RequestTransport for use between an Android client and a
 * Google AppEngine server.
 */
public class AppEngineRequestTransport implements RequestTransport {

	private static final String TAG = "AppEngineRequestTransport";

	private static final int SECOND_IN_MILLIS = (int) DateUtils.SECOND_IN_MILLIS;

	static final String BASE_URL = "INSERT_YOUR_APP_ENGINE_HOST_HERE";
	private static final String AUTH_URL = BASE_URL + "/_ah/login";
	private static final String AUTH_TOKEN_TYPE = "ah";

	private final Context context;
	private final String urlPath;

	/**
	 * Constructs an AndroidRequestTransport instance.
	 * 
	 * @param uri
	 *            the URI for the RequestFactory service
	 * @param cookie
	 *            the ACSID or SACSID cookie used for authentication
	 */
	public AppEngineRequestTransport(Context context, String urlPath) {
		this.context = context;
		this.urlPath = urlPath;
	}

	public void send(String payload, TransportReceiver receiver) {
		send(payload, receiver, false);
	}

	public void send(String payload, TransportReceiver receiver,
			boolean newToken) {
		Throwable ex;
		try {
			final SharedPreferences prefs = Prefs.get(context);
			final String accountName = prefs.getString(
					DevoxxPrefs.ACCOUNT_NAME, "unknown");
			Account account = new Account(accountName, "com.google");
			String authToken = getAuthToken(context, account);

			if (newToken) { // invalidate the cached token
				AccountManager accountManager = AccountManager.get(context);
				accountManager.invalidateAuthToken(account.type, authToken);
				authToken = getAuthToken(context, account);
			}

			HttpClient client = new DefaultHttpClient();
			client.getParams().setParameter(CoreProtocolPNames.USER_AGENT,
					HttpUtils.buildUserAgent(context));
			String continueURL = BASE_URL;
			URI uri = new URI(AUTH_URL + "?continue="
					+ URLEncoder.encode(continueURL, "UTF-8") + "&auth="
					+ authToken);
			HttpGet method = new HttpGet(uri);
			final HttpParams getParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(getParams,
					20 * SECOND_IN_MILLIS);
			HttpConnectionParams.setSoTimeout(getParams, 20 * SECOND_IN_MILLIS);
			HttpClientParams.setRedirecting(getParams, false);
			method.setParams(getParams);

			HttpResponse res = client.execute(method);
			Header[] headers = res.getHeaders("Set-Cookie");
			if (!newToken
					&& (res.getStatusLine().getStatusCode() != 302 || headers.length == 0)) {
				send(payload, receiver, true);
			}

			String ascidCookie = null;
			for (Header header : headers) {
				if (header.getValue().indexOf("ACSID=") >= 0) {
					// let's parse it
					String value = header.getValue();
					String[] pairs = value.split(";");
					ascidCookie = pairs[0];
				}
			}

			// Make POST request
			uri = new URI(BASE_URL + urlPath);
			HttpPost post = new HttpPost();
			post.setHeader("Content-Type", "application/json;charset=UTF-8");
			post.setHeader("Cookie", ascidCookie);
			post.setURI(uri);
			post.setEntity(new StringEntity(payload, "UTF-8"));
			HttpResponse response = client.execute(post);
			if (200 == response.getStatusLine().getStatusCode()) {
				String contents = readStreamAsString(response.getEntity()
						.getContent());
				receiver.onTransportSuccess(contents);
			} else {
				receiver.onTransportFailure(new ServerFailure(response
						.getStatusLine().getReasonPhrase()));
			}
			return;
		} catch (UnsupportedEncodingException e) {
			ex = e;
		} catch (ClientProtocolException e) {
			ex = e;
		} catch (IOException e) {
			ex = e;
		} catch (URISyntaxException e) {
			ex = e;
		} catch (PendingAuthException e) {
			final Intent intent = new Intent(
					SettingsActivity.AUTH_PERMISSION_ACTION);
			intent.putExtra("AccountManagerBundle", e.getAccountManagerBundle());
			context.sendBroadcast(intent);
			return;
		} catch (Exception e) {
			ex = e;
		}
		receiver.onTransportFailure(new ServerFailure(ex.getMessage()));
	}

	/**
	 * Reads an entire input stream as a String. Closes the input stream.
	 */
	private String readStreamAsString(InputStream in) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
			byte[] buffer = new byte[1024];
			int count;
			do {
				count = in.read(buffer);
				if (count > 0) {
					out.write(buffer, 0, count);
				}
			} while (count >= 0);
			return out.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(
					"The JVM does not support the compiler's default encoding.",
					e);
		} catch (IOException e) {
			return null;
		} finally {
			try {
				in.close();
			} catch (IOException ignored) {
			}
		}
	}

	private String getAuthToken(Context context, Account account)
			throws PendingAuthException {
		String authToken = null;
		AccountManager accountManager = AccountManager.get(context);
		try {
			AccountManagerFuture<Bundle> future = accountManager.getAuthToken(
					account, AUTH_TOKEN_TYPE, false, null, null);
			Bundle bundle = future.getResult();
			authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
			if (authToken == null) {
				throw new PendingAuthException(bundle);
			}
		} catch (OperationCanceledException e) {
			Log.w(TAG, e.getMessage());
		} catch (AuthenticatorException e) {
			Log.w(TAG, e.getMessage());
		} catch (IOException e) {
			Log.w(TAG, e.getMessage());
		}
		return authToken;
	}

	public class PendingAuthException extends Exception {
		private static final long serialVersionUID = 1L;
		private final Bundle mAccountManagerBundle;

		public PendingAuthException(Bundle accountManagerBundle) {
			super();
			mAccountManagerBundle = accountManagerBundle;
		}

		public Bundle getAccountManagerBundle() {
			return mAccountManagerBundle;
		}
	}

}
