package com.PopCorp.Purchases.Controllers;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayout;
import android.text.InputFilter;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.PopCorp.Purchases.Activities.MainActivity;
import com.PopCorp.Purchases.Data.Category;
import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.SD;
import com.PopCorp.Purchases.DataBase.DB;
import com.PopCorp.Purchases.Fragments.PreferencesFragment;
import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.MaterialDialog;

public class PreferencesController {

    private final AppCompatActivity context;
    private final SharedPreferences sPref;
    private final SharedPreferences.Editor editor;
    private final PreferencesFragment fragment;
    private final DB db;

    private ArrayList<String> currencies;
    private String selectedCurrency;

    private ArrayList<String> units;
    private String selectedUnit;

    private ArrayList<String> shopes;
    private int selectedColor = 0;

    public PreferencesController(AppCompatActivity context, PreferencesFragment fragment, SharedPreferences sPref, SharedPreferences.Editor editor) {
        this.context = context;
        this.sPref = sPref;
        this.editor = editor;
        this.fragment = fragment;
        db = ((MainActivity) context).getDB();
    }


    public void showDialogWithCities() {
        ArrayList<String> cities = new ArrayList<>();
        final ArrayList<String> citiesIds = new ArrayList<>();
        Cursor cursor = db.getAllData(DB.TABLE_CITIES);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                cities.add(cursor.getString(cursor.getColumnIndex(DB.KEY_CITY_NAME)));
                citiesIds.add(cursor.getString(cursor.getColumnIndex(DB.KEY_CITY_ID)));
                while (cursor.moveToNext()) {
                    cities.add(cursor.getString(cursor.getColumnIndex(DB.KEY_CITY_NAME)));
                    citiesIds.add(cursor.getString(cursor.getColumnIndex(DB.KEY_CITY_ID)));
                }
            }
            cursor.close();
        }
        Dialog dialog = new MaterialDialog.Builder(context)
                .title(R.string.prefs_region)
                .items(cities.toArray(new String[cities.size()]))
                .itemsCallbackSingleChoice(citiesIds.indexOf(sPref.getString(SD.PREFS_CITY, "1")), new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        editor.putString(SD.PREFS_CITY, citiesIds.get(which)).commit();
                        fragment.selectCity(text.toString());
                        return true;
                    }
                })
                .positiveText(R.string.dialog_select)
                .negativeText(R.string.dialog_cancel)
                .build();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    public void showDialogWithShopes() {
        shopes = getShopes();
        Dialog dialog = new MaterialDialog.Builder(context)
                .title(R.string.prefs_shops)
                .items(shopes.toArray(new String[shopes.size()]))
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        showDialogForEditingShop(text.toString());
                    }
                })
                .positiveText(R.string.dialog_add)
                .negativeText(R.string.dialog_cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        showDialogForNewShop();
                    }
                })
                .build();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void showDialogForNewShop() {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.dialog_input, null);
        final EditText input = (EditText) customView.findViewById(R.id.dialog_input_edittext);
        final TextInputLayout inputLayout = (TextInputLayout) customView.findViewById(R.id.dialog_input_layout);
        inputLayout.setHint(context.getString(R.string.string_shop_name));

        final Dialog dialog = new MaterialDialog.Builder(context)
                .title(R.string.string_new_shop)
                .positiveText(R.string.dialog_add)
                .negativeText(R.string.dialog_cancel)
                .autoDismiss(false)
                .customView(customView, false)
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        showDialogWithShopes();
                    }
                })
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        String newShop = input.getText().toString();
                        if (newShop.isEmpty()) {
                            inputLayout.setError(context.getString(R.string.notify_enter_name_of_shop));
                            return;
                        }
                        if (!shopes.contains(newShop)) {
                            shopes.add(newShop);
                            Set<String> setToPrefs = new LinkedHashSet<>(shopes);
                            editor.putStringSet(SD.PREFS_SHOPES, setToPrefs).commit();
                            dialog.dismiss();
                        } else {
                            inputLayout.setError(context.getString(R.string.notify_shop_exists));
                        }
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                        dialog.dismiss();
                    }
                })
                .build();
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String newShop = input.getText().toString();
                if (newShop.isEmpty()) {
                    inputLayout.setError(context.getString(R.string.notify_enter_name_of_shop));
                    return false;
                }
                if (!shopes.contains(newShop)) {
                    shopes.add(newShop);
                    Set<String> setToPrefs = new LinkedHashSet<>(shopes);
                    editor.putStringSet(SD.PREFS_SHOPES, setToPrefs).commit();
                    dialog.dismiss();
                } else {
                    inputLayout.setError(context.getString(R.string.notify_shop_exists));
                }
                return false;
            }
        });
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void showDialogForEditingShop(final String shop) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.dialog_input, null);
        final EditText input = (EditText) customView.findViewById(R.id.dialog_input_edittext);
        final TextInputLayout inputLayout = (TextInputLayout) customView.findViewById(R.id.dialog_input_layout);
        inputLayout.setHint(context.getString(R.string.string_shop_name));
        input.setText(shop);

        final Dialog dialog = new MaterialDialog.Builder(context)
                .title(R.string.string_editing_shop)
                .positiveText(R.string.dialog_save)
                .negativeText(R.string.dialog_remove)
                .neutralText(R.string.dialog_cancel)
                .autoDismiss(false)
                .customView(customView, false)
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        showDialogWithShopes();
                    }
                })
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        shopes.remove(shop);
                        Set<String> setToPrefs = new LinkedHashSet<>(shopes);
                        editor.putStringSet(SD.PREFS_SHOPES, setToPrefs).commit();
                        dialog.dismiss();
                    }

                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        String newShop = input.getText().toString();
                        if (newShop.isEmpty()) {
                            inputLayout.setError(context.getString(R.string.notify_enter_name_of_shop));
                            return;
                        }
                        if (!shop.equals(newShop)) {
                            if (shopes.contains(newShop)) {
                                inputLayout.setError(context.getString(R.string.notify_shop_exists));
                                return;
                            }
                            shopes.add(newShop);
                            shopes.remove(shop);
                            Set<String> setToPrefs = new LinkedHashSet<>(shopes);
                            editor.putStringSet(SD.PREFS_SHOPES, setToPrefs).commit();
                        }
                        dialog.dismiss();
                    }

                    @Override
                    public void onNeutral(MaterialDialog dialog) {
                        super.onNeutral(dialog);
                        dialog.dismiss();
                    }
                })
                .build();
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String newShop = input.getText().toString();
                if (newShop.isEmpty()) {
                    inputLayout.setError(context.getString(R.string.notify_enter_name_of_shop));
                    return false;
                }
                if (!shop.equals(newShop)) {
                    if (shopes.contains(newShop)) {
                        inputLayout.setError(context.getString(R.string.notify_shop_exists));
                        return false;
                    }
                    shopes.add(newShop);
                    shopes.remove(shop);
                    Set<String> setToPrefs = new LinkedHashSet<>(shopes);
                    editor.putStringSet(SD.PREFS_SHOPES, setToPrefs).commit();
                }
                dialog.dismiss();
                return false;
            }
        });
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    public void showDialogWithUnits() {
        units = getUnits();
        final String currentUnit = sPref.getString(SD.PREFS_DEF_EDIZM, context.getString(R.string.default_unit_one));
        selectedUnit = currentUnit;
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        builder.title(R.string.prefs_unit);
        builder.autoDismiss(false);
        builder.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog dialog) {
                if (selectedUnit.equals(units.get(0))) {
                    showDialogForAddUnit();
                    dialog.dismiss();
                    return;
                }
                if (!selectedUnit.equals(currentUnit)) {
                    editor.putString(SD.PREFS_DEF_EDIZM, selectedUnit).commit();
                    fragment.selectUnit(selectedUnit);
                }
                dialog.dismiss();
            }

            @Override
            public void onNegative(MaterialDialog dialog) {
                if (selectedUnit.equals(units.get(0)) || units.size() < 3 || selectedUnit.equals(currentUnit)) {
                    return;
                }
                units.remove(selectedUnit);
                units.remove(context.getString(R.string.string_add_unit));
                Set<String> setToPrefs = new LinkedHashSet<>(units);
                editor.putStringSet(SD.PREFS_EDIZMS, setToPrefs).commit();
                dialog.dismiss();
            }

            @Override
            public void onNeutral(MaterialDialog dialog) {
                super.onNeutral(dialog);
                dialog.dismiss();
            }
        });
        builder.items(units.toArray(new String[units.size()]));
        builder.alwaysCallSingleChoiceCallback();
        builder.itemsCallbackSingleChoice(units.indexOf(currentUnit), new MaterialDialog.ListCallbackSingleChoice() {
            @Override
            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                selectedUnit = units.get(which);
                return true;
            }
        });
        builder.positiveText(R.string.dialog_select);
        builder.neutralText(R.string.dialog_cancel);
        builder.negativeText(R.string.dialog_remove);
        Dialog dialog = builder.build();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void showDialogForAddUnit() {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.dialog_input, null);
        final EditText input = (EditText) customView.findViewById(R.id.dialog_input_edittext);
        final TextInputLayout inputLayout = (TextInputLayout) customView.findViewById(R.id.dialog_input_layout);
        inputLayout.setHint(context.getString(R.string.string_unit_name));

        final Dialog dialog = new MaterialDialog.Builder(context)
                .title(R.string.string_new_unit)
                .positiveText(R.string.dialog_add)
                .negativeText(R.string.dialog_cancel)
                .autoDismiss(false)
                .customView(customView, false)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        String newUnit = input.getText().toString();
                        if (newUnit.isEmpty()) {
                            inputLayout.setError(context.getString(R.string.notify_enter_name_of_unit));
                            return;
                        }
                        if (!units.contains(newUnit)) {
                            units.add(newUnit);
                            units.remove(context.getString(R.string.string_add_unit));
                            Set<String> setToPrefs = new LinkedHashSet<>(units);
                            editor.putStringSet(SD.PREFS_EDIZMS, setToPrefs).commit();
                            dialog.dismiss();
                        } else {
                            inputLayout.setError(context.getString(R.string.notify_unit_exists));
                        }
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                        dialog.dismiss();
                    }
                })
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        showDialogWithUnits();
                    }
                })
                .build();
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String newUnit = input.getText().toString();
                if (newUnit.isEmpty()) {
                    inputLayout.setError(context.getString(R.string.notify_enter_name_of_unit));
                    return false;
                }
                if (!units.contains(newUnit)) {
                    units.add(newUnit);
                    units.remove(context.getString(R.string.string_add_unit));
                    Set<String> setToPrefs = new LinkedHashSet<>(units);
                    editor.putStringSet(SD.PREFS_EDIZMS, setToPrefs).commit();
                    dialog.dismiss();
                } else {
                    inputLayout.setError(context.getString(R.string.notify_unit_exists));
                }
                return false;
            }
        });
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    public void showDialogWithCurrencies() {
        currencies = getCurrencies();
        final String currentCurrency = sPref.getString(SD.PREFS_DEF_CURRENCY, context.getString(R.string.default_one_currency));
        selectedCurrency = currentCurrency;
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        builder.title(R.string.prefs_currency);
        builder.autoDismiss(false);
        builder.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog dialog) {
                if (selectedCurrency.equals(currencies.get(0))) {
                    showDialogForAddCurrency();
                    dialog.dismiss();
                    return;
                }
                if (!selectedCurrency.equals(currentCurrency)) {
                    editor.putString(SD.PREFS_DEF_CURRENCY, selectedCurrency).commit();
                    fragment.selectCurrency(selectedCurrency);
                }
                dialog.dismiss();
            }

            @Override
            public void onNegative(MaterialDialog dialog) {
                if (selectedCurrency.equals(currencies.get(0)) || currencies.size() < 3 || selectedCurrency.equals(currentCurrency)) {
                    return;
                }
                currencies.remove(selectedCurrency);
                currencies.remove(context.getString(R.string.string_add_currency));
                Set<String> setToPrefs = new LinkedHashSet<>(currencies);
                editor.putStringSet(SD.PREFS_CURRENCYS, setToPrefs).commit();
                dialog.dismiss();
            }

            @Override
            public void onNeutral(MaterialDialog dialog) {
                super.onNeutral(dialog);
                dialog.dismiss();
            }
        });
        builder.items(currencies.toArray(new String[currencies.size()]));
        builder.alwaysCallSingleChoiceCallback();
        builder.itemsCallbackSingleChoice(currencies.indexOf(currentCurrency), new MaterialDialog.ListCallbackSingleChoice() {
            @Override
            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                selectedCurrency = currencies.get(which);
                return true;
            }
        });
        builder.positiveText(R.string.dialog_select);
        builder.neutralText(R.string.dialog_cancel);
        builder.negativeText(R.string.dialog_remove);
        Dialog dialog = builder.build();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void showDialogForAddCurrency() {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.dialog_input, null);
        final EditText input = (EditText) customView.findViewById(R.id.dialog_input_edittext);
        final TextInputLayout inputLayout = (TextInputLayout) customView.findViewById(R.id.dialog_input_layout);
        inputLayout.setHint(context.getString(R.string.string_currency_name));

        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        builder.title(R.string.string_new_currency);
        builder.positiveText(R.string.dialog_add);
        builder.autoDismiss(false);
        builder.negativeText(R.string.dialog_cancel);
        builder.dismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                showDialogWithCurrencies();
            }
        });
        builder.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onNegative(MaterialDialog dialog) {
                super.onNegative(dialog);
                dialog.dismiss();
            }

            @Override
            public void onPositive(MaterialDialog dialog) {
                super.onPositive(dialog);
                String newCurrency = input.getText().toString();
                if (newCurrency.isEmpty()) {
                    inputLayout.setError(context.getString(R.string.notify_enter_name_of_currency));
                    return;
                }
                if (!currencies.contains(newCurrency)) {
                    currencies.add(newCurrency);
                    currencies.remove(context.getString(R.string.string_add_currency));
                    Set<String> setToPrefs = new LinkedHashSet<>(currencies);
                    editor.putStringSet(SD.PREFS_CURRENCYS, setToPrefs).commit();
                    dialog.dismiss();
                } else {
                    inputLayout.setError(context.getString(R.string.notify_currency_exists));
                }
            }
        });
        builder.customView(customView, false);
        final Dialog dialog = builder.build();
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String newCurrency = input.getText().toString();
                if (newCurrency.isEmpty()) {
                    inputLayout.setError(context.getString(R.string.notify_enter_name_of_currency));
                    return false;
                }
                if (!currencies.contains(newCurrency)) {
                    currencies.add(newCurrency);
                    currencies.remove(context.getString(R.string.string_add_currency));
                    Set<String> setToPrefs = new LinkedHashSet<>(currencies);
                    editor.putStringSet(SD.PREFS_CURRENCYS, setToPrefs).commit();
                    dialog.dismiss();
                } else {
                    inputLayout.setError(context.getString(R.string.notify_currency_exists));
                }
                return false;
            }
        });
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private ArrayList<String> getCurrencies() {
        ArrayList<String> result = new ArrayList<>(sPref.getStringSet(SD.PREFS_CURRENCYS, new LinkedHashSet<String>()));
        result.add(0, context.getString(R.string.string_add_currency));
        return result;
    }

    private ArrayList<String> getUnits() {
        ArrayList<String> result = new ArrayList<>(sPref.getStringSet(SD.PREFS_EDIZMS, new LinkedHashSet<String>()));
        result.add(0, context.getString(R.string.string_add_unit));
        return result;
    }

    private ArrayList<String> getShopes() {
        return new ArrayList<>(sPref.getStringSet(SD.PREFS_SHOPES, new LinkedHashSet<String>()));
    }

    public void showDialogWithCategories() {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        final ArrayList<Category> categories = getCategories();
        ArrayList<String> names = new ArrayList<>();
        for (Category category : categories) {
            names.add(category.getName());
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.item_list_category, names) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(R.layout.item_list_category, parent, false);
                }
                view.findViewById(R.id.item_list_category_image).setBackgroundColor(categories.get(position).getColor());
                ((TextView) view.findViewById(R.id.item_list_category_name)).setText(categories.get(position).getName());
                return view;
            }
        };
        builder.adapter(adapter, new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                showDialogForCategoryChange(categories.get(which));
                dialog.cancel();
            }
        });
        builder.title(R.string.prefs_categories_of_products);
        builder.negativeText(R.string.dialog_cancel);
        builder.positiveText(R.string.dialog_add);
        builder.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog dialog) {
                showDialogForNewCategory();
            }
        });
        Dialog dialog = builder.build();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void showDialogForCategoryChange(final Category category) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_color_picker, null);
        builder.customView(layout, true);
        builder.title(R.string.dialog_title_editing_category);

        final EditText edtext = (EditText) layout.findViewById(R.id.dialog_color_picker_edittext);
        edtext.setText(category.getName());
        final TextInputLayout inputLayout = (TextInputLayout) layout.findViewById(R.id.dialog_color_picker_input_layout);
        final GridLayout list = (GridLayout) layout.findViewById(R.id.content_gridlayout_with_colors);
        final int[] mColors = initializeGridLayout(list, category.getColor());

        builder.neutralText(R.string.dialog_cancel);
        builder.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog dialog) {
                super.onPositive(dialog);
                if (edtext.getText().toString().length() > 0) {
                    category.updateInDb(db, edtext.getText().toString(), mColors[selectedColor]);
                    dialog.dismiss();
                } else {
                    inputLayout.setError(context.getString(R.string.notify_enter_name_of_category));
                }
            }

            @Override
            public void onNegative(MaterialDialog dialog) {
                super.onNegative(dialog);
                category.removeFromDb(db);
                dialog.dismiss();
            }

            @Override
            public void onNeutral(MaterialDialog dialog) {
                super.onNeutral(dialog);
                dialog.dismiss();
            }
        });
        builder.autoDismiss(false);
        builder.negativeText(R.string.dialog_remove);
        builder.positiveText(R.string.dialog_save);
        builder.dismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                showDialogWithCategories();
            }
        });
        final Dialog dialog = builder.build();
        edtext.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (edtext.getText().toString().length() > 0) {
                    category.updateInDb(db, edtext.getText().toString(), mColors[selectedColor]);
                    dialog.dismiss();
                } else {
                    inputLayout.setError(context.getString(R.string.notify_enter_name_of_category));
                }
                return false;
            }
        });
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private Drawable createSelector(int color) {
        ShapeDrawable coloredCircle = new ShapeDrawable(new OvalShape());
        coloredCircle.getPaint().setColor(color);
        ShapeDrawable darkerCircle = new ShapeDrawable(new OvalShape());
        darkerCircle.getPaint().setColor(shiftColor(color));

        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.addState(new int[]{-android.R.attr.state_pressed}, coloredCircle);
        stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, darkerCircle);
        return stateListDrawable;
    }

    public static int shiftColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.9f;
        return Color.HSVToColor(hsv);
    }

    private void showDialogForNewCategory() {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_color_picker, null);

        builder.customView(layout, true);
        builder.title(R.string.dialog_title_new_category);
        final EditText editForCategoryName = (EditText) layout.findViewById(R.id.dialog_color_picker_edittext);
        final TextInputLayout inputLayout = (TextInputLayout) layout.findViewById(R.id.dialog_color_picker_input_layout);
        final GridLayout list = (GridLayout) layout.findViewById(R.id.content_gridlayout_with_colors);

        final int[] mColors = initializeGridLayout(list, 0);
        builder.negativeText(R.string.dialog_cancel);
        builder.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog dialog) {
                super.onPositive(dialog);
                if (editForCategoryName.getText().toString().length() > 0) {
                    String name = editForCategoryName.getText().toString();
                    Category newCategory = new Category(-1, name, mColors[selectedColor]);
                    newCategory.putInDb(db);
                    dialog.dismiss();
                } else{
                    inputLayout.setError(context.getString(R.string.notify_enter_name_of_category));
                }
            }

            @Override
            public void onNegative(MaterialDialog dialog) {
                super.onNegative(dialog);
                dialog.dismiss();
            }
        });
        builder.autoDismiss(false);
        builder.positiveText(R.string.dialog_add);
        builder.dismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                showDialogWithCategories();
            }
        });

        final Dialog dialog = builder.build();
        editForCategoryName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (editForCategoryName.getText().toString().length() > 0) {
                    String name = editForCategoryName.getText().toString();
                    Category newCategory = new Category(-1, name, mColors[selectedColor]);
                    newCategory.putInDb(db);
                    dialog.dismiss();
                } else{
                    inputLayout.setError(context.getString(R.string.notify_enter_name_of_category));
                }
                return false;
            }
        });
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private int[] initializeGridLayout(final GridLayout list, int color) {
        selectedColor = 0;
        final TypedArray ta = context.getResources().obtainTypedArray(R.array.colors);
        final int[] mColors = new int[ta.length()];
        for (int i = 0; i < ta.length(); i++) {
            mColors[i] = ta.getColor(i, 0);
            if (mColors[i] == color) {
                selectedColor = i;
            }
        }
        ta.recycle();

        for (int i = 0; i < list.getChildCount(); i++) {
            FrameLayout child = (FrameLayout) list.getChildAt(i);
            child.setTag(i);
            child.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FrameLayout child = (FrameLayout) list.getChildAt(selectedColor);
                    child.getChildAt(0).setVisibility(View.GONE);

                    selectedColor = (Integer) v.getTag();
                    child = (FrameLayout) list.getChildAt(selectedColor);
                    child.getChildAt(0).setVisibility(View.VISIBLE);
                }
            });
            child.getChildAt(0).setVisibility(selectedColor == i ? View.VISIBLE : View.GONE);

            Drawable selector = createSelector(mColors[i]);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int[][] states = new int[][]{
                        new int[]{-android.R.attr.state_pressed},
                        new int[]{android.R.attr.state_pressed}
                };
                int[] colours = new int[]{
                        shiftColor(mColors[i]),
                        mColors[i]
                };
                ColorStateList rippleColors = new ColorStateList(states, colours);
                setBackgroundCompat(child, new RippleDrawable(rippleColors, selector, null));
            } else {
                setBackgroundCompat(child, selector);
            }
        }
        return mColors;
    }

    private void setBackgroundCompat(View view, Drawable d) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackground(d);
        } else {
            view.setBackgroundDrawable(d);
        }
    }

    private ArrayList<Category> getCategories() {
        ArrayList<Category> result = new ArrayList<>();
        Cursor cursor = db.getAllData(DB.TABLE_CATEGORIES);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                result.add(new Category(cursor));
                while (cursor.moveToNext()) {
                    result.add(new Category(cursor));
                }
            }
            cursor.close();
        }
        return result;
    }

    public String getCityForId(String id) {
        String result = "";
        Cursor cursor = db.getdata(DB.TABLE_CITIES, new String[]{DB.KEY_CITY_NAME}, DB.KEY_CITY_ID + "='" + id + "'", null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                result = cursor.getString(cursor.getColumnIndex(DB.KEY_CITY_NAME));
            }
            cursor.close();
        }
        return result;
    }

    public void showDialogForColor(final String key, String title) {
        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_theme_picker, null);
        builder.setView(layout);
        builder.setTitle(title);

        int color;
        final GridLayout list = (GridLayout) layout.findViewById(R.id.content_gridlayout_with_colors);
        if (key.equals(SD.PREFS_COLOR_PRIMARY)) {
            color = context.getResources().getColor(R.color.primary);
        } else {
            color = context.getResources().getColor(R.color.accent);
        }
        final int[] mColors = initializeGridLayout(list, sPref.getInt(key, color));

        builder.setNegativeButton(R.string.dialog_cancel, null);
        builder.setPositiveButton(R.string.string_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which1) {
                editor.putInt(key, mColors[selectedColor]).commit();
                if (key.equals(SD.PREFS_COLOR_ACCENT)) {
                    final TypedArray ta = context.getResources().obtainTypedArray(R.array.themes);
                    final int[] themes = new int[ta.length()];
                    for (int i = 0; i < ta.length(); i++) {
                        themes[i] = ta.getResourceId(i, 0);
                    }
                    ta.recycle();
                    int theme = themes[selectedColor];
                    editor.putInt(SD.PREFS_THEME, theme).commit();
                    ((MainActivity) context).setTheme();
                    fragment.refresh();
                } else {
                    final TypedArray arrayThemes = context.getResources().obtainTypedArray(R.array.dialog_themes);
                    final int[] themes = new int[arrayThemes.length()];
                    for (int i = 0; i < arrayThemes.length(); i++) {
                        themes[i] = arrayThemes.getResourceId(i, 0);
                    }
                    arrayThemes.recycle();
                    int dialogTheme = themes[selectedColor];
                    editor.putInt(SD.PREFS_DIALOG_THEME, dialogTheme).commit();

                    final TypedArray arrayHeaders = context.getResources().obtainTypedArray(R.array.headers);
                    final int[] headers = new int[arrayHeaders.length()];
                    for (int i = 0; i < arrayHeaders.length(); i++) {
                        headers[i] = arrayHeaders.getResourceId(i, 0);
                    }
                    arrayHeaders.recycle();
                    int header = headers[selectedColor];
                    editor.putInt(SD.PREFS_HEADER, header).commit();
                    ((MainActivity) context).updateTheme();
                }
            }
        });
        final Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    public void showDialogAbout() {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.dialog_about_application, null);

        final Dialog dialog = new MaterialDialog.Builder(context)
                .title(R.string.prefs_about)
                .positiveText(R.string.string_ok)
                .autoDismiss(false)
                .customView(customView, true)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        dialog.dismiss();
                    }
                })
                .build();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
}
