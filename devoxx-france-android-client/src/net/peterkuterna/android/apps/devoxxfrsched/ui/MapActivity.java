package net.peterkuterna.android.apps.devoxxfrsched.ui;

import net.peterkuterna.android.apps.devoxxfrsched.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class MapActivity extends BaseMultiPaneActivity {

	public static final String EXTRA_ROOM = "net.peterkuterna.android.apps.devoxxfrsched.extra.ROOM";
	public static final String EXTRA_LEVEL = "net.peterkuterna.android.apps.devoxxfrsched.extra.LEVEL";

	private static final String STATE_SELECTED_LEVEL = "selectedLevel";

	private int mLevel = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_map);

		final ActionBar bar = getSupportActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

		mLevel = getIntent().getIntExtra(EXTRA_LEVEL, 0);

		if (savedInstanceState != null) {
			mLevel = savedInstanceState.getInt(STATE_SELECTED_LEVEL, mLevel);
		}

		Bundle args1 = new Bundle();
		args1.putInt("imageId", R.drawable.marriot_level_ground);
		Bundle args2 = new Bundle();
		args2.putInt("imageId", R.drawable.marriot_level_basement);

		Tab levelGround = bar
				.newTab()
				.setTag("ground")
				.setText("Level 0")
				.setTabListener(
						new TabListener<MapFragment>(this, "ground",
								MapFragment.class, args1));
		Tab levelBasement = bar
				.newTab()
				.setTag("basement")
				.setText("Level -1")
				.setTabListener(
						new TabListener<MapFragment>(this, "basement",
								MapFragment.class, args2));
		bar.addTab(levelGround);
		bar.addTab(levelBasement);

		if (mLevel == -1) {
			bar.selectTab(levelBasement);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_SELECTED_LEVEL, "basement"
				.equals(getSupportActionBar().getSelectedTab().getTag()) ? -1
				: 0);
	}

	public static class TabListener<T extends Fragment> implements
			ActionBar.TabListener {
		private final SherlockFragmentActivity mActivity;
		private final String mTag;
		private final Class<T> mClass;
		private final Bundle mArgs;
		private Fragment mFragment;

		public TabListener(SherlockFragmentActivity activity, String tag,
				Class<T> clz) {
			this(activity, tag, clz, null);
		}

		public TabListener(SherlockFragmentActivity activity, String tag,
				Class<T> clz, Bundle args) {
			mActivity = activity;
			mTag = tag;
			mClass = clz;
			mArgs = args;

			// Check to see if we already have a fragment for this tab, probably
			// from a previously saved state. If so, deactivate it, because our
			// initial state is that a tab isn't shown.
			mFragment = mActivity.getSupportFragmentManager()
					.findFragmentByTag(mTag);
			if (mFragment != null && !mFragment.isDetached()) {
				FragmentTransaction ft = mActivity.getSupportFragmentManager()
						.beginTransaction();
				ft.detach(mFragment);
				ft.commit();
			}
		}

		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			FragmentTransaction fragmentTransaction = ft;
			if (fragmentTransaction == null) {
				fragmentTransaction = mActivity.getSupportFragmentManager()
						.beginTransaction();
			}
			if (mFragment == null) {
				mFragment = Fragment.instantiate(mActivity, mClass.getName(),
						mArgs);
				fragmentTransaction.add(R.id.root_container, mFragment, mTag);
			} else {
				fragmentTransaction.attach(mFragment);
			}
			if (ft == null) {
				fragmentTransaction.commit();
			}
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			FragmentTransaction fragmentTransaction = ft;
			if (fragmentTransaction == null) {
				fragmentTransaction = mActivity.getSupportFragmentManager()
						.beginTransaction();
			}
			if (mFragment != null) {
				fragmentTransaction.detach(mFragment);
			}
			if (ft == null) {
				fragmentTransaction.commit();
			}
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	}
}
