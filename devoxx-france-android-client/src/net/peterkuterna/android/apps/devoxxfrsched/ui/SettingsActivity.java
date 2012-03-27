/*
 * Copyright 2010 Peter Kuterna
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
import net.peterkuterna.android.apps.devoxxfrsched.service.CfpSyncManager;
import net.peterkuterna.android.apps.devoxxfrsched.util.AnalyticsUtils;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

/**
 * {@link PreferenceActivity} to handle the application settings.
 */
public class SettingsActivity extends SherlockPreferenceActivity {

	private static final String TAG = "SettingsActivity";

	private static final long FIRST_TRIGGER = 30 * DateUtils.SECOND_IN_MILLIS;

	public static final String UPDATE_UI_ACTION = "net.peterkuterna.android.apps.devoxxfrsched.UPDATE_UI";
	public static final String AUTH_PERMISSION_ACTION = "net.peterkuterna.android.apps.devoxxfrsched.AUTH_PERMISSION";

	public static final String KEY_BACKGROUND_UPDATES = "background_updates";
	public static final String KEY_AUTO_UPDATE_WIFI_ONLY = "auto_update_wifi_only";
//	public static final String KEY_SYNC_GOOGLE_ACCOUNT = "sync_google_account";
//	public static final String KEY_DISCONNECT_GOOGLE_ACCOUNT = "disconnect_google_account";

	public static final String KEY_GOOGLE_PLUS = "google_plus";
	public static final String KEY_BLOG = "blog";
	public static final String KEY_TWITTER = "twitter";
	public static final String KEY_SOURCE_CODE = "source_code";

	public static final String KEY_DEVOXXFRSCHED_VERSION = "devoxxfrsched_version";

//	private State mState;

	private CheckBoxPreference mBackgroundSyncPref;
	private CheckBoxPreference mAutoUpdateWifiPref;
//	private ListPreference mAccountPref;
//	private Preference mDisconnectPref;

	private Preference mGooglePlusPref;
	private Preference mBlogPref;
	private Preference mTwitterPref;
	private Preference mSourceCodePref;

//	private ProgressDialog mProgressDialog;

	private boolean mBackgroundSync;

//	private boolean mPendingAuth = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		mBackgroundSync = prefs.getBoolean(KEY_BACKGROUND_UPDATES, true);
		mBackgroundSyncPref = (CheckBoxPreference) findPreference(KEY_BACKGROUND_UPDATES);
		mBackgroundSyncPref
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						boolean backgroundSync = ((Boolean) newValue)
								.booleanValue();
						mAutoUpdateWifiPref.setEnabled(backgroundSync);
						return true;
					}
				});

		mAutoUpdateWifiPref = (CheckBoxPreference) findPreference(KEY_AUTO_UPDATE_WIFI_ONLY);
		mAutoUpdateWifiPref.setEnabled(prefs.getBoolean(KEY_BACKGROUND_UPDATES,
				true));

//		mAccountPref = (ListPreference) findPreference(KEY_SYNC_GOOGLE_ACCOUNT);
//		mDisconnectPref = findPreference(KEY_DISCONNECT_GOOGLE_ACCOUNT);

//		if (mAccountPref != null) {
//			final ArrayList<String> accountsList = AccountUtils
//					.getGoogleAccounts(this);
//			final String[] accountsArray = new String[accountsList.size()];
//			accountsList.toArray(accountsArray);
//
//			mAccountPref.setEntries(accountsArray);
//			mAccountPref.setEntryValues(accountsArray);
//
//			mAccountPref
//					.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
//						@Override
//						public boolean onPreferenceChange(
//								Preference preference, Object newValue) {
//							if (newValue != null) {
//								register((String) newValue);
//							}
//							return true;
//						}
//					});
//
//			updateAccountPref();
//		}

//		if (mDisconnectPref != null) {
//			mDisconnectPref
//					.setOnPreferenceClickListener(new OnPreferenceClickListener() {
//						@Override
//						public boolean onPreferenceClick(Preference preference) {
//							unregister();
//							return true;
//						}
//					});
//		}

		mGooglePlusPref = findPreference(KEY_GOOGLE_PLUS);
		mGooglePlusPref
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						AnalyticsUtils.getInstance(SettingsActivity.this)
								.trackEvent("Settings", "Click", "Google+", 0);
						return false;
					}
				});
		mBlogPref = findPreference(KEY_BLOG);
		mBlogPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				AnalyticsUtils.getInstance(SettingsActivity.this).trackEvent(
						"Settings", "Click", "Blog", 0);
				return false;
			}
		});
		mTwitterPref = findPreference(KEY_TWITTER);
		mTwitterPref
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						AnalyticsUtils.getInstance(SettingsActivity.this)
								.trackEvent("Settings", "Click", "Twitter", 0);
						return false;
					}
				});
		mSourceCodePref = findPreference(KEY_SOURCE_CODE);
		mSourceCodePref
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						AnalyticsUtils.getInstance(SettingsActivity.this)
								.trackEvent("Settings", "Click", "Source Code",
										0);
						return false;
					}
				});

		findPreference(KEY_DEVOXXFRSCHED_VERSION).setTitle(formatVersion());

//		mState = (State) getLastNonConfigurationInstance();
//		if (mState != null) {
//			if (mState.mConnecting) {
//				mProgressDialog = ProgressDialog.show(SettingsActivity.this,
//						null, getString(R.string.settings_connecting_account),
//						true);
//			} else if (mState.mDisconnecting) {
//				mProgressDialog = ProgressDialog.show(SettingsActivity.this,
//						null,
//						getString(R.string.settings_disconnecting_account),
//						true);
//			}
//		} else {
//			mState = new State();
//		}

//		registerReceiver(mUpdateUIReceiver, new IntentFilter(UPDATE_UI_ACTION));
//		registerReceiver(mAuthPermissionReceiver, new IntentFilter(
//				AUTH_PERMISSION_ACTION));
	}

//	@Override
//	protected void onResume() {
//		super.onResume();
//		if (mPendingAuth) {
//			mPendingAuth = false;
//			String regId = C2DMessaging.getRegistrationId(this);
//			if (regId != null && !"".equals(regId)) {
//				DeviceRegistrar.registerOrUnregister(this, regId, true);
//			} else {
//				C2DMessaging.register(this, Config.C2DM_SENDER);
//			}
//		}
//	}

//	@Override
//	public Object onRetainNonConfigurationInstance() {
//		return mState;
//	}

//	@Override
//	public void onDestroy() {
//		unregisterReceiver(mUpdateUIReceiver);
//		unregisterReceiver(mAuthPermissionReceiver);
//		super.onDestroy();
//	}

	@Override
	public void finish() {
		final CfpSyncManager syncManager = new CfpSyncManager(this);
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		final boolean backgroundSync = prefs.getBoolean(KEY_BACKGROUND_UPDATES,
				true);

		if (mBackgroundSync != backgroundSync) {
			if (backgroundSync) {
				syncManager.setSyncAlarm(FIRST_TRIGGER);
			} else {
				syncManager.cancelSyncAlarm();
			}
		}

		super.finish();
	}

//	private void updateAccountPref() {
//		if (mAccountPref != null) {
//			final SharedPreferences prefs = Prefs.get(this);
//			final String deviceId = prefs.getString(
//					DevoxxPrefs.DEVICE_REGISTRATION_ID, null);
//			final String account = prefs.getString(DevoxxPrefs.ACCOUNT_NAME,
//					null);
//			mAccountPref.setValue(deviceId != null ? account : null);
//			mAccountPref.setSummary(deviceId != null ? account
//					: getString(R.string.settings_no_google_account));
//			mAccountPref.setEnabled(mAccountPref.getValue() == null);
//			mDisconnectPref.setEnabled(mAccountPref.getValue() != null);
//		}
//	}

//	private void register(String account) {
//		mState.mConnecting = true;
//		mProgressDialog = ProgressDialog.show(SettingsActivity.this, null,
//				getString(R.string.settings_connecting_account), true);
//
//		SharedPreferences prefs = Prefs.get(this);
//		SharedPreferences.Editor editor = prefs.edit();
//		editor.putString(DevoxxPrefs.ACCOUNT_NAME, account);
//		editor.commit();
//
//		C2DMessaging.register(this, Config.C2DM_SENDER);
//	}

//	private void unregister() {
//		mState.mDisconnecting = true;
//		mProgressDialog = ProgressDialog.show(SettingsActivity.this, null,
//				getString(R.string.settings_disconnecting_account), true);
//
//		C2DMessaging.unregister(this);
//	}

//	private final BroadcastReceiver mUpdateUIReceiver = new BroadcastReceiver() {
//		@Override
//		public void onReceive(Context context, Intent intent) {
//			final int status = intent.getIntExtra(DeviceRegistrar.STATUS_EXTRA,
//					DeviceRegistrar.ERROR_STATUS);
//			if (status == DeviceRegistrar.REGISTERED_STATUS) {
//				mState.mConnecting = false;
//				if (mProgressDialog != null) {
//					mProgressDialog.dismiss();
//					mProgressDialog = null;
//				}
//				updateAccountPref();
//				final Intent syncIntent = new Intent(SettingsActivity.this,
//						AppEngineSyncService.class);
//				syncIntent.putExtra(AppEngineSyncService.EXTRA_INITIAL_SYNC,
//						true);
//				startService(syncIntent);
//			} else if (status == DeviceRegistrar.UNREGISTERED_STATUS) {
//				mState.mDisconnecting = false;
//				if (mProgressDialog != null) {
//					mProgressDialog.dismiss();
//					mProgressDialog = null;
//				}
//				updateAccountPref();
//			} else {
//				mState.mConnecting = false;
//				mState.mDisconnecting = false;
//				if (mProgressDialog != null) {
//					mProgressDialog.dismiss();
//					mProgressDialog = null;
//				}
//				Toast.makeText(context, R.string.sync_error, Toast.LENGTH_SHORT)
//						.show();
//			}
//		}
//	};

//	private final BroadcastReceiver mAuthPermissionReceiver = new BroadcastReceiver() {
//		@Override
//		public void onReceive(Context context, Intent intent) {
//			Bundle extras = intent.getBundleExtra("AccountManagerBundle");
//			if (extras != null) {
//				Intent authIntent = (Intent) extras
//						.get(AccountManager.KEY_INTENT);
//				if (authIntent != null) {
//					mPendingAuth = true;
//					startActivity(authIntent);
//				}
//			}
//		}
//	};

	public String formatVersion() {
		String versionName = "";
		int versionCode = 0;
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(),
					PackageManager.GET_META_DATA).versionName;
			versionCode = getPackageManager().getPackageInfo(getPackageName(),
					PackageManager.GET_META_DATA).versionCode;
			return String.format(getString(R.string.settings_version_info),
					versionName, versionCode);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return versionName;
	}

	private static class State {

		public boolean mConnecting = false;
		public boolean mDisconnecting = false;

	}

}
