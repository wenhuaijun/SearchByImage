package rikka.searchbyimage.ui.fragment;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import rikka.searchbyimage.BuildConfig;
import rikka.searchbyimage.R;
import rikka.searchbyimage.ui.UploadActivity;
import rikka.searchbyimage.utils.ClipBoardUtils;
import rikka.searchbyimage.utils.URLUtils;
import rikka.searchbyimage.widget.SettingsFragmentDividerItemDecoration;

/**
 * Created by Rikka on 2015/12/23.
 */
public class SettingsFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    Activity mActivity;

    PreferenceCategory mCategoryGoogle;
    PreferenceCategory mCategoryIqdb;
    SwitchPreference mSafeSearch;
    PreferenceScreen mScreen;
    EditTextPreference mCustomGoogleUri;
    PreferenceCategory mCategorySauceNAO;

    private int click = 0;
    private Runnable clearClickCount = new Runnable() {
        @Override
        public void run() {
            click = 0;
        }
    };

    private RecyclerView mList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mList = getListView();
        mList.addItemDecoration(new SettingsFragmentDividerItemDecoration(mActivity.getApplicationContext()));

        return view;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        boolean popup = getArguments().getBoolean("popup");

        if (popup) {
            if (!BuildConfig.hideOtherEngine)
                addPreferencesFromResource(R.xml.preferences_general_mini);

            addPreferencesFromResource(R.xml.preferences_search_settings);
        } else {
            addPreferencesFromResource(R.xml.preferences_usage);
            addPreferencesFromResource(BuildConfig.hideOtherEngine ? R.xml.preferences_general_gp : R.xml.preferences_general);
            addPreferencesFromResource(R.xml.preferences_search_settings);
            addPreferencesFromResource(R.xml.preferences_about);
        }


        mCategoryGoogle = (PreferenceCategory) findPreference("category_google");
        mCategoryIqdb = (PreferenceCategory) findPreference("category_iqdb");
        mCategorySauceNAO = (PreferenceCategory) findPreference("category_saucenao");

        mSafeSearch = (SwitchPreference) findPreference("safe_search_preference");
        mScreen = (PreferenceScreen) findPreference("screen");
        mCustomGoogleUri = (EditTextPreference) findPreference("google_region");

        setCustomGoogleUriHide();
        setSearchEngineHide();


        mActivity = getActivity();

        if (!popup)
        {
            Preference versionPref = findPreference("version");
            versionPref.setOnPreferenceClickListener(this);

            Preference githubPref = findPreference("open_source");
            githubPref.setOnPreferenceClickListener(this);

            Preference donatePref = findPreference("donate");
            donatePref.setOnPreferenceClickListener(this);

            try {
                versionPref.setSummary(mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), 0).versionName);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("search_engine_preference")) {
            setSearchEngineHide();
        }

        if (key.equals("google_region_preference")) {
            setCustomGoogleUriHide();
        }
    }

    private void setCustomGoogleUriHide() {
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();

        boolean customRedirect = sharedPreferences.getString("google_region_preference", "0").equals("2");

        if (customRedirect) {
            mCategoryGoogle.addPreference(mCustomGoogleUri);
        } else {
            mCategoryGoogle.removePreference(mCustomGoogleUri);
        }
    }

    private void setSearchEngineHide() {
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();

        int siteId = Integer.parseInt(sharedPreferences.getString("search_engine_preference", "0"));

        switch (siteId) {
            case UploadActivity.SITE_GOOGLE:
                mScreen.addPreference(mCategoryGoogle);
                mScreen.removePreference(mCategoryIqdb);
                mScreen.removePreference(mCategorySauceNAO);
                break;
            case UploadActivity.SITE_IQDB:
                mScreen.removePreference(mCategoryGoogle);
                mScreen.addPreference(mCategoryIqdb);
                mScreen.removePreference(mCategorySauceNAO);
                break;
            case UploadActivity.SITE_SAUCENAO:
                mScreen.removePreference(mCategoryGoogle);
                mScreen.removePreference(mCategoryIqdb);
                mScreen.addPreference(mCategorySauceNAO);
                break;
            case UploadActivity.SITE_BAIDU:
            case UploadActivity.SITE_TINEYE:
                mScreen.removePreference(mCategoryGoogle);
                mScreen.removePreference(mCategoryIqdb);
                mScreen.removePreference(mCategorySauceNAO);

                break;
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        switch (key) {
            case "version":
                getActivity().getWindow().getDecorView().removeCallbacks(clearClickCount);
                getActivity().getWindow().getDecorView().postDelayed(clearClickCount, 3000);

                click++;

                if (click == 5)
                    Toast.makeText(mActivity, "OAO", Toast.LENGTH_SHORT).show();
                else if (click == 10)
                    Toast.makeText(mActivity, "><", Toast.LENGTH_SHORT).show();
                else if (click == 15)
                    Toast.makeText(mActivity, "www", Toast.LENGTH_SHORT).show();
                else if (click == 25)
                    Toast.makeText(mActivity, "QAQ", Toast.LENGTH_SHORT).show();
                else if (click == 40) {
                    Toast.makeText(mActivity, "2333", Toast.LENGTH_SHORT).show();

                    click = -10;
                }

                break;

            case "open_source":
                URLUtils.Open("https://github.com/RikkaW/SearchByImage", mActivity);
                break;
            case "donate":
                ClipBoardUtils.putTextIntoClipboard(mActivity, "rikka@xing.moe");
                Toast.makeText(mActivity, "rikka@xing.moe" + " copied to clipboard.", Toast.LENGTH_SHORT).show();
                break;
        }

        return false;
    }
}