package com.PopCorp.Purchases.Fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.PopCorp.Purchases.Activities.MainActivity;
import com.PopCorp.Purchases.BuildConfig;
import com.PopCorp.Purchases.Controllers.PreferencesController;
import com.PopCorp.Purchases.PurchasesApplication;
import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.SD;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;

public class PreferencesFragment extends PreferenceFragment {

    public static final String TAG = PreferencesFragment.class.getSimpleName();

    private AppCompatActivity context;
    private SharedPreferences sPref;
    private PreferencesController controller;
    private ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
        context = (AppCompatActivity) getActivity();
        sPref = PreferenceManager.getDefaultSharedPreferences(context);
        controller = new PreferencesController(context, this, sPref, sPref.edit());

        initializePrefs();
        if (getPreferenceScreen() != null) {
            ArrayList<Preference> preferences = getAllPreferenceScreen(getPreferenceScreen(), new ArrayList<Preference>());
            for (Preference preference : preferences) {
                preferenceToMaterialPreference(preference);
            }
        }
    }

    private ArrayList<Preference> getAllPreferenceScreen(Preference p, ArrayList<Preference> list) {
        if (p instanceof PreferenceCategory || p instanceof PreferenceScreen) {
            PreferenceGroup pGroup = (PreferenceGroup) p;
            int pCount = pGroup.getPreferenceCount();
            list.add(p);
            for (int i = 0; i < pCount; i++) {
                getAllPreferenceScreen(pGroup.getPreference(i), list);
            }
        }
        return list;
    }

    private void preferenceToMaterialPreference(Preference preference) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (preference instanceof PreferenceScreen && preference.getLayoutResource()
                    != R.layout.mp_preference_material) {
                preference.setLayoutResource(R.layout.mp_preference_material);
            } else if (preference instanceof PreferenceCategory && preference.getLayoutResource() != R.layout.mp_preference_category) {
                preference.setLayoutResource(R.layout.mp_preference_category);

                PreferenceCategory category = (PreferenceCategory) preference;
                for (int j = 0; j < category.getPreferenceCount(); j++) {
                    Preference basicPreference = category.getPreference(j);
                    if (!(basicPreference instanceof PreferenceCategory || basicPreference instanceof PreferenceScreen)) {
                        if (basicPreference.getLayoutResource() != R.layout.mp_preference_material_widget) {
                            basicPreference.setLayoutResource(R.layout.mp_preference_material_widget);
                        }
                    }
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        if (rootView != null) {
            listView = (ListView) rootView.findViewById(android.R.id.list);
            if (Build.VERSION.SDK_INT < 21) {
                listView.setSelector(R.drawable.selector_for_normal_list);
                listView.setPadding(listView.getPaddingLeft(), (int) context.getResources().getDimension(R.dimen.listview_padding_top), listView.getPaddingRight(), (int) context.getResources().getDimension(R.dimen.listview_padding_bottom));
                listView.setClipToPadding(false);
                listView.setFooterDividersEnabled(false);
            }
        }
        if (!BuildConfig.DEBUG) {
            Tracker t = ((PurchasesApplication) getActivity().getApplication()).getTracker(PurchasesApplication.TrackerName.APP_TRACKER);
            t.setScreenName(this.getClass().getSimpleName());
            t.send(new HitBuilders.AppViewBuilder().build());
        }
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        listView.setKeepScreenOn(sPref.getBoolean(SD.PREFS_DISPLAY_NO_OFF, true));
    }

    private void initializePrefs() {
        final Preference prefSortListItem = findPreference(SD.PREFS_SORT_LIST_ITEM);
        if (prefSortListItem != null) {
            prefSortListItem.setSummary(context.getString(R.string.prefs_default_sort) + " " + sPref.getString(SD.PREFS_SORT_LIST_ITEM, context.getString(R.string.prefs_default_sort_listitem_one)));
            prefSortListItem.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    prefSortListItem.setSummary(context.getString(R.string.prefs_default_sort) + " " + newValue);
                    return true;
                }
            });
        }

        final Preference prefFontSize = findPreference(SD.PREFS_LIST_ITEM_FONT_SIZE);
        if (prefFontSize != null) {
            prefFontSize.setSummary(context.getString(R.string.prefs_text_size_summary) + " " + sPref.getString(SD.PREFS_LIST_ITEM_FONT_SIZE, "14"));

            prefFontSize.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    prefFontSize.setSummary(context.getString(R.string.prefs_text_size_summary) + " " + newValue);
                    return true;
                }
            });
        }

        final Preference prefFontSizeSmall = findPreference(SD.PREFS_LIST_ITEM_FONT_SIZE_SMALL);
        if (prefFontSizeSmall != null) {
            prefFontSizeSmall.setSummary(context.getString(R.string.prefs_text_size_summary_small) + " " + sPref.getString(SD.PREFS_LIST_ITEM_FONT_SIZE_SMALL, "12"));
            prefFontSizeSmall.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    prefFontSizeSmall.setSummary(context.getString(R.string.prefs_text_size_summary_small) + " " + newValue);
                    return true;
                }
            });
        }

        final Preference prefCategories = findPreference(SD.PREFS_CATEGORIES);
        if (prefCategories != null) {
            prefCategories.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    controller.showDialogWithCategories();
                    return true;
                }
            });
        }

        Preference prefCurrency = findPreference(SD.PREFS_CURRENCY);
        if (prefCurrency != null) {
            prefCurrency.setSummary(context.getString(R.string.prefs_default_currency) + " " + sPref.getString(SD.PREFS_DEF_CURRENCY, context.getString(R.string.default_one_currency)));
            prefCurrency.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    controller.showDialogWithCurrencies();
                    return true;
                }
            });
        }

        Preference prefUnit = findPreference(SD.PREFS_UNIT);
        if (prefUnit != null) {
            prefUnit.setSummary(context.getString(R.string.prefs_default_unit) + " " + sPref.getString(SD.PREFS_DEF_EDIZM, context.getString(R.string.default_unit_one)));
            prefUnit.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    controller.showDialogWithUnits();
                    return true;
                }
            });
        }

        final Preference prefSortList = findPreference(SD.PREFS_SORT_LISTS);
        if (prefSortList != null) {
            prefSortList.setSummary(context.getString(R.string.prefs_default_sort) + " " + sPref.getString(SD.PREFS_SORT_LISTS, context.getString(R.string.prefs_default_sort_listitem_one)));
            prefSortList.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    prefSortList.setSummary(context.getString(R.string.prefs_default_sort) + " " + newValue);
                    return true;
                }
            });
        }

        Preference shopes = findPreference(SD.PREFS_SHOPES);
        if (shopes != null) {
            shopes.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    controller.showDialogWithShopes();
                    return true;
                }
            });
        }

        Preference prefCity = findPreference(SD.PREFS_CITY);
        if (prefCity != null) {
            prefCity.setSummary(controller.getCityForId(sPref.getString(SD.PREFS_CITY, "1")));
            prefCity.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    controller.showDialogWithCities();
                    return true;
                }
            });
        }

        Preference prefDisplayNoOff = findPreference(SD.PREFS_DISPLAY_NO_OFF);
        if (prefDisplayNoOff != null) {
            prefDisplayNoOff.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean value = (boolean) newValue;
                    listView.setKeepScreenOn(value);
                    return true;
                }
            });
        }
        OnPreferenceClickListener listener = new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                controller.showDialogForColor(preference.getKey(), preference.getTitle().toString());
                return true;
            }
        };
        Preference prefColorPrimary = findPreference(SD.PREFS_COLOR_PRIMARY);
        if (prefColorPrimary != null) {
            prefColorPrimary.setOnPreferenceClickListener(listener);
        }
        Preference prefColorAccent = findPreference(SD.PREFS_COLOR_ACCENT);
        if (prefColorAccent != null) {
            prefColorAccent.setOnPreferenceClickListener(listener);
        }

        Preference about = findPreference(SD.PREFS_ABOUT);
        if (about != null) {
            about.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    controller.showDialogAbout();
                    return true;
                }
            });
        }
    }

    public void selectCurrency(String selectedCurrency) {
        Preference prefCurrency = findPreference(SD.PREFS_CURRENCY);
        if (prefCurrency != null) {
            prefCurrency.setSummary(context.getString(R.string.prefs_default_currency) + " " + selectedCurrency);
        }
    }

    public void selectUnit(String selectedUnit) {
        Preference prefUnit = findPreference(SD.PREFS_UNIT);
        if (prefUnit != null) {
            prefUnit.setSummary(context.getString(R.string.prefs_default_unit) + " " + selectedUnit);
        }
    }

    public void selectCity(String selectedCity) {
        Preference prefCity = findPreference(SD.PREFS_CITY);
        if (prefCity != null) {
            prefCity.setSummary(selectedCity);
        }
    }


    public void refresh() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(MainActivity.TO_SETTINGS, true);
            getActivity().finish();
            Intent intent = getActivity().getIntent();
            intent.putExtras(bundle);
            getActivity().startActivity(intent);
        } else{
            getFragmentManager()
                .beginTransaction()
                .detach(this)
                .attach(this)
                .commit();
        }
    }
}
