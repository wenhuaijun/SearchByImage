package rikka.searchbyimage.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import rikka.materialpreference.DropDownPreference;
import rikka.materialpreference.EditTextPreference;
import rikka.materialpreference.ListPreference;
import rikka.materialpreference.Preference;
import rikka.materialpreference.PreferenceCategory;
import rikka.materialpreference.PreferenceFragment;
import rikka.materialpreference.PreferenceScreen;
import rikka.materialpreference.SwitchPreference;
import rikka.searchbyimage.BuildConfig;
import rikka.searchbyimage.R;
import rikka.searchbyimage.staticdata.CustomEngine;
import rikka.searchbyimage.ui.EditSitesActivity;
import rikka.searchbyimage.utils.ArrayUtils;
import rikka.searchbyimage.utils.ClipBoardUtils;
import rikka.searchbyimage.utils.CustomTabsHelper;
import rikka.searchbyimage.utils.URLUtils;

import static rikka.searchbyimage.staticdata.EngineId.SITE_ASCII2D;
import static rikka.searchbyimage.staticdata.EngineId.SITE_BAIDU;
import static rikka.searchbyimage.staticdata.EngineId.SITE_GOOGLE;
import static rikka.searchbyimage.staticdata.EngineId.SITE_IQDB;
import static rikka.searchbyimage.staticdata.EngineId.SITE_SAUCENAO;
import static rikka.searchbyimage.staticdata.EngineId.SITE_TINEYE;
import static rikka.searchbyimage.support.GetDeviceInfo.getAppInfo;

/**
 * Created by Rikka on 2015/12/23.
 */
public class SettingsFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    Activity mActivity;

    PreferenceCategory mCategoryGoogle;
    PreferenceCategory mCategoryIqdb;
    PreferenceCategory mCategoryBaidu;
    PreferenceCategory mCategoryAdvance;

    SwitchPreference mSafeSearch;
    PreferenceScreen mScreen;
    EditTextPreference mCustomGoogleUri;
    PreferenceCategory mCategorySauceNAO;
    ListPreference mSearchEngine;

    Preference mNotice;

    List<CustomEngine> mData;

    private int click = 0;
    private Runnable clearClickCount = new Runnable() {
        @Override
        public void run() {
            click = 0;
        }
    };

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        mActivity = getActivity();

        boolean popup = getArguments().getBoolean("popup");

        if (popup) {
            addPreferencesFromResource(R.xml.preferences_general_mini);

            addPreferencesFromResource(R.xml.preferences_search_settings);
        } else {
            addPreferencesFromResource(R.xml.preferences_usage);
            addPreferencesFromResource(R.xml.preferences_general);
            addPreferencesFromResource(R.xml.preferences_search_settings);
            addPreferencesFromResource(R.xml.preferences_about);
        }


        mCategoryGoogle = (PreferenceCategory) findPreference("category_google");
        mCategoryIqdb = (PreferenceCategory) findPreference("category_iqdb");
        mCategorySauceNAO = (PreferenceCategory) findPreference("category_saucenao");
        mCategoryBaidu = (PreferenceCategory) findPreference("category_baidu");
        mCategoryAdvance = (PreferenceCategory) findPreference("category_advance");

        mData = CustomEngine.getList(mActivity);
        mSearchEngine = (ListPreference) findPreference("search_engine_preference");

        mSafeSearch = (SwitchPreference) findPreference("safe_search_preference");
        mScreen = (PreferenceScreen) findPreference("screen");
        mCustomGoogleUri = (EditTextPreference) findPreference("google_region");
        mNotice = findPreference("preference_notice");


        setCustomGoogleUriHide();

        if (!CustomTabsHelper.getIsChromeInstalled(mActivity)) {
            ListPreference showResultInPreference = (ListPreference) findPreference("show_result_in");
            CharSequence[] entries = ArrayUtils.remove(showResultInPreference.getEntries(), CharSequence.class, 1);
            CharSequence[] entryValues = ArrayUtils.remove(showResultInPreference.getEntryValues(), CharSequence.class, 1);
            showResultInPreference.setEntries(entries);
            showResultInPreference.setEntryValues(entryValues);

            if (showResultInPreference.getValue() == null || showResultInPreference.getValue().equals("2")) {
                showResultInPreference.setValue("0");
            }
        }

        if (!popup) {
            Preference versionPref = findPreference("version");
            versionPref.setOnPreferenceClickListener(this);

            Preference githubPref = findPreference("open_source");
            githubPref.setOnPreferenceClickListener(this);

            findPreference("contact").setOnPreferenceClickListener(this);

            Preference advancePref = findPreference("advance");
            if (advancePref != null) {
                advancePref.setOnPreferenceClickListener(this);
            }

            Preference donatePref = findPreference("donate");
            if (BuildConfig.hideOtherEngine) {
                ((PreferenceCategory) findPreference("about")).removePreference(donatePref);
            } else {
                donatePref.setOnPreferenceClickListener(this);
            }

            try {
                versionPref.setSummary(mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), 0).versionName);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    @Override
    public DividerDecoration onCreateItemDecoration() {
        return new CategoryDivideDividerDecoration();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

        addCustomEngines();
    }

    private void addCustomEngines() {
        List<CharSequence> entries = new ArrayList<>();
        List<CharSequence> entryValues = new ArrayList<>();

        String value = null;
        for (CustomEngine item : mData) {
            if (item.getEnabled() == 1) {
                entries.add(item.getName());
                entryValues.add(Integer.toString(item.getId()));

                if (value == null) {
                    value = Integer.toString(item.getId());
                }
            }
        }

        value = getPreferenceManager().getSharedPreferences().getString("search_engine_id", value);

        mSearchEngine.setEntries(entries.toArray(new CharSequence[entries.size()]));
        mSearchEngine.setEntryValues(entryValues.toArray(new CharSequence[entries.size()]));

        mSearchEngine.setValue(value);

        setSearchEngineHide(Integer.parseInt(value));
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case "google_region_preference":
                setCustomGoogleUriHide();
                break;
            case "search_engine_preference":
                String value = sharedPreferences.getString(key, "0");
                getPreferenceManager().getSharedPreferences()
                        .edit()
                        .putString("search_engine_id", value)
                        .commit();

                setSearchEngineHide(Integer.parseInt(value));
                break;
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

    private void setSearchEngineHide(int siteId) {
        switch (siteId) {
            case SITE_GOOGLE:
                mScreen.addPreference(mCategoryGoogle);
                mScreen.removePreference(mCategoryIqdb);
                mScreen.removePreference(mCategorySauceNAO);
                mScreen.removePreference(mCategoryBaidu);
                break;
            case SITE_IQDB:
                mScreen.removePreference(mCategoryGoogle);
                mScreen.addPreference(mCategoryIqdb);
                mScreen.removePreference(mCategorySauceNAO);
                mScreen.removePreference(mCategoryBaidu);
                break;
            case SITE_SAUCENAO:
                mScreen.removePreference(mCategoryGoogle);
                mScreen.removePreference(mCategoryIqdb);
                mScreen.addPreference(mCategorySauceNAO);
                mScreen.removePreference(mCategoryBaidu);
                break;
            case SITE_BAIDU:
                mScreen.removePreference(mCategoryGoogle);
                mScreen.removePreference(mCategoryIqdb);
                mScreen.removePreference(mCategorySauceNAO);
                mScreen.addPreference(mCategoryBaidu);
                break;
            case SITE_ASCII2D:
            case SITE_TINEYE:
            default:
                mScreen.removePreference(mCategoryGoogle);
                mScreen.removePreference(mCategoryIqdb);
                mScreen.removePreference(mCategorySauceNAO);
                mScreen.removePreference(mCategoryBaidu);
                break;
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        switch (key) {
            case "advance":
                startActivity(new Intent(mActivity, EditSitesActivity.class));
                break;
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
                Toast.makeText(mActivity, String.format(getString(R.string.copy_to_clipboard), "rikka@xing.moe"), Toast.LENGTH_SHORT).show();

                break;
            case "contact":
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "rikka@xing.moe", null));
                intent.putExtra(Intent.EXTRA_CC, new String[]{"xmu.miffy+imageSearchFeedback@gmail.com"});
                intent.putExtra(Intent.EXTRA_SUBJECT, "SearchByImage Feedback");
                intent.putExtra(Intent.EXTRA_TEXT, getAppInfo(mActivity).toString());
                startActivity(Intent.createChooser(intent, "Send feedback by Email"));
                break;
        }

        return false;
    }
}