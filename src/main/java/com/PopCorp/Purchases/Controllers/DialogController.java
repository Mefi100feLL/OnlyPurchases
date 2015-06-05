package com.PopCorp.Purchases.Controllers;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.PopCorp.Purchases.Data.List;
import com.PopCorp.Purchases.DataBase.DB;
import com.PopCorp.Purchases.Loaders.LoaderItemsFromSMS;
import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.SD;
import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.MaterialDialog;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class DialogController {

    public interface CallbackForEditingList extends LoaderItemsFromSMS.CallbackForLoadingSMS {
        void onListEdited(List list, String name, String currency);
        void addNewList(String name, String currency);
    }

    public static void showDialogForNewList(final Context activity, SharedPreferences sPref, final CallbackForEditingList callback) {
        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(activity);
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_new_list, null);

        final EditText edittextForName = (EditText) layout.findViewById(R.id.dialog_new_list_edittext_name);
        final TextInputLayout inputLayout = (TextInputLayout) layout.findViewById(R.id.dialog_new_list_input_layout);

        final Spinner spinnerForCurrency = (Spinner) layout.findViewById(R.id.dialog_new_list_spinner_currency);
        Set<String> currencys = sPref.getStringSet(SD.PREFS_CURRENCYS, new LinkedHashSet<String>());
        if (currencys == null) {
            currencys = new LinkedHashSet<>();
        }
        ArrayAdapter<String> adapterForSpinnerCurrency = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, currencys.toArray(new String[currencys.size()]));
        adapterForSpinnerCurrency.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerForCurrency.setAdapter(adapterForSpinnerCurrency);
        spinnerForCurrency.setSelection(adapterForSpinnerCurrency.getPosition(sPref.getString(SD.PREFS_DEF_CURRENCY, activity.getString(R.string.default_one_currency))));

        builder.setTitle(R.string.dialog_title_new_list);
        builder.autoDismiss(false);
        builder.setView(layout);
        builder.setPositiveButton(activity.getResources().getString(R.string.dialog_create), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (checkNameAndCreateNewList(edittextForName.getText().toString(), (String) spinnerForCurrency.getSelectedItem(), callback)) {
                    dialog.dismiss();
                } else{
                    inputLayout.setError(activity.getString(R.string.notify_no_entered_name_of_list));
                }
            }
        });
        builder.setNegativeButton(activity.getResources().getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        final Dialog dialog = builder.create();

        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();

        edittextForName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(android.widget.TextView v, int actionId, KeyEvent event) {
                if (checkNameAndCreateNewList(edittextForName.getText().toString(), (String) spinnerForCurrency.getSelectedItem(), callback)) {
                    dialog.dismiss();
                } else{
                    inputLayout.setError(activity.getString(R.string.notify_no_entered_name_of_list));
                }
                return true;
            }
        });
    }

    private static boolean checkNameAndCreateNewList(String name, String currency, CallbackForEditingList callback) {
        if (name.isEmpty()) {
            return false;
        }
        callback.addNewList(name, currency);
        return true;
    }

    public static void showDialogWithSMS(Context context, final ArrayList<HashMap<String, String>> mapsSMS, final LoaderItemsFromSMS.CallbackForLoadingSMS controller) {
        final ArrayList<HashMap<String, String>> result = new ArrayList<>();
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        builder.title(R.string.dialog_select_sms);
        String[] smsArray = new String[mapsSMS.size()];
        for (int i = 0; i < mapsSMS.size(); i++) {
            smsArray[i] = LoaderItemsFromSMS.getContactDisplayNameByNumber(context, mapsSMS.get(i).get(SD.SMS_KEY_ADDRESS)) + "\n" + mapsSMS.get(i).get(SD.SMS_KEY_DATE);
        }
        builder.alwaysCallMultiChoiceCallback();
        builder.items(smsArray);
        builder.itemsCallbackMultiChoice(new Integer[]{}, new MaterialDialog.ListCallbackMultiChoice() {
            @Override
            public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {
                result.clear();
                for (int i : which) {
                    if (!result.contains(mapsSMS.get(i))) {
                        result.add(mapsSMS.get(i));
                    }
                }
                return true;
            }
        });
        builder.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog dialog) {
                super.onPositive(dialog);
                controller.loadFromSelectedSMS(result);
            }
        });
        builder.positiveText(R.string.string_ok);
        builder.negativeText(R.string.dialog_cancel);
        Dialog dialog = builder.build();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    public static void showDialogForEditingList(final Context context, final List list, SharedPreferences sPref, final CallbackForEditingList callback) {
        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_new_list, null);
        final EditText edittextForName = (EditText) layout.findViewById(R.id.dialog_new_list_edittext_name);
        final TextInputLayout inputLayout = (TextInputLayout) layout.findViewById(R.id.dialog_new_list_input_layout);
        edittextForName.setText(list.getName());

        final Spinner spinnerForCurrency = (Spinner) layout.findViewById(R.id.dialog_new_list_spinner_currency);
        Set<String> currencys = sPref.getStringSet(SD.PREFS_CURRENCYS, new LinkedHashSet<String>());
        if (currencys == null) {
            currencys = new LinkedHashSet<>();
        }
        ArrayAdapter<String> adapterForSpinnerCurrency = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, currencys.toArray(new String[currencys.size()]));
        adapterForSpinnerCurrency.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerForCurrency.setAdapter(adapterForSpinnerCurrency);
        if (currencys.contains(list.getCurrency())) {
            spinnerForCurrency.setSelection(adapterForSpinnerCurrency.getPosition(list.getCurrency()));
        } else {
            spinnerForCurrency.setSelection(adapterForSpinnerCurrency.getPosition(sPref.getString(SD.PREFS_DEF_CURRENCY, context.getString(R.string.prefs_default_currency))));
        }

        builder.setTitle(R.string.dialog_title_edit_list);
        builder.setView(layout);
        builder.autoDismiss(false);
        builder.setPositiveButton(context.getResources().getString(R.string.dialog_change), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (checkNameAndChangeList(list, edittextForName.getText().toString(), (String) spinnerForCurrency.getSelectedItem(), callback)) {
                    dialog.dismiss();
                } else{
                    inputLayout.setError(context.getString(R.string.notify_no_entered_name_of_list));
                }
            }
        });
        builder.setNegativeButton(context.getResources().getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        final Dialog dialog = builder.create();

        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();

        edittextForName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(android.widget.TextView v, int actionId, KeyEvent event) {
                if (checkNameAndChangeList(list, edittextForName.getText().toString(), (String) spinnerForCurrency.getSelectedItem(), callback)) {
                    dialog.dismiss();
                } else{
                    inputLayout.setError(context.getString(R.string.notify_no_entered_name_of_list));
                }
                return true;
            }
        });
    }

    private static boolean checkNameAndChangeList(List list, String name, String currency, CallbackForEditingList callback) {
        if (name.isEmpty()) {
            return false;
        }
        callback.onListEdited(list, name, currency);
        return true;
    }

    public static void showDialogForSendingList(final Context context, final List list, LoaderItemsFromSMS.CallbackForLoadingSMS controller) {
        if (list.getItems().size() == 0) {
            controller.showToast(R.string.notify_list_is_empty);
            return;
        }
        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(context);
        builder.setTitle(R.string.dialog_title_send_list);
        builder.setItems(context.getResources().getStringArray(R.array.types_of_sending_list), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                list.send(context, List.TYPES_OF_SENDING_LIST[which]);
            }
        });
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    public static void showDialogForAlarm(final DB db, final AppCompatActivity activity, final List list) {
        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(activity);
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_set_alarm, null);

        final Calendar date = Calendar.getInstance();

        final Button buttonDate = (Button) layout.findViewById(R.id.dialog_alert_date);
        final Button buttonTime = (Button) layout.findViewById(R.id.dialog_alert_time);
        buttonTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT > 20) {
                    showMaterialTimePickerDialog(activity, date, buttonTime);
                } else {
                    showTimePickerDialog(activity, date, buttonTime);
                }
            }
        });
        buttonDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT > 20) {
                    showMaterialDatePickerDialog(activity, date, buttonDate);
                } else {
                    showDatePickerDialog(activity, date, buttonDate);
                }
            }
        });

        if (!list.getAlarm().isEmpty()) {
            SimpleDateFormat formatter = new SimpleDateFormat(List.FORMAT_FOR_DATE_ALARM, new Locale("ru"));
            try {
                date.setTime(formatter.parse(list.getAlarm()));
            } catch (ParseException ignored) {
            }

            builder.setNeutralButton(R.string.dialog_alarm_remove, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    list.cancelAlarm(db, activity);
                }
            });
        }
        buttonTime.setText(new SimpleDateFormat(SD.TIME_FORMAT_FOR_ALARM, new Locale("ru")).format(date.getTime()));
        buttonDate.setText(new SimpleDateFormat(SD.DATE_FORMAT_FOR_ALARM, new Locale("ru")).format(date.getTime()));

        builder.setTitle(R.string.dialog_title_alarm);
        builder.setView(layout);
        builder.setPositiveButton(R.string.dialog_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                list.setAlarm(db, activity, date.getTime());
            }
        });
        builder.setNegativeButton(R.string.dialog_cancel, null);

        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private static void showDatePickerDialog(AppCompatActivity activity, final Calendar date, final Button buttonDate) {
        Calendar now = Calendar.getInstance();
        DatePickerDialog.OnDateSetListener dateListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
                date.set(year, monthOfYear, dayOfMonth);
                buttonDate.setText(new SimpleDateFormat(SD.DATE_FORMAT_FOR_ALARM, new Locale("ru")).format(date.getTime()));
            }
        };
        DatePickerDialog dpd = DatePickerDialog.newInstance(
                dateListener,
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );
        dpd.show(activity.getFragmentManager(), "Datepickerdialog");
    }

    private static void showMaterialDatePickerDialog(Context activity, final Calendar date, final Button buttonDate) {
        Calendar now = Calendar.getInstance();
        android.app.DatePickerDialog.OnDateSetListener dateListener = new android.app.DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                date.set(year, monthOfYear, dayOfMonth);
                buttonDate.setText(new SimpleDateFormat(SD.DATE_FORMAT_FOR_ALARM, new Locale("ru")).format(date.getTime()));
            }
        };
        SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(activity);
        int theme = sPref.getInt(SD.PREFS_DIALOG_THEME, R.style.TealThemeDialog);
        android.app.DatePickerDialog dpd = new android.app.DatePickerDialog(activity,
                theme,
                dateListener,
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );
        dpd.show();
    }

    private static void showTimePickerDialog(AppCompatActivity activity, final Calendar date, final Button buttonTime) {
        TimePickerDialog.OnTimeSetListener timeListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
                date.set(Calendar.HOUR_OF_DAY, hourOfDay);
                date.set(Calendar.MINUTE, minute);
                date.set(Calendar.SECOND, 0);
                buttonTime.setText(new SimpleDateFormat(SD.TIME_FORMAT_FOR_ALARM, new Locale("ru")).format(date.getTime()));
            }
        };
        TimePickerDialog tpd = TimePickerDialog.newInstance(
                timeListener,
                date.get(Calendar.HOUR_OF_DAY),
                date.get(Calendar.MINUTE),
                true
        );
        tpd.setThemeDark(false);
        tpd.show(activity.getFragmentManager(), "Timepickerdialog");
    }

    private static void showMaterialTimePickerDialog(Context activity, final Calendar date, final Button buttonTime) {
        SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(activity);
        int theme = sPref.getInt(SD.PREFS_DIALOG_THEME, R.style.TealThemeDialog);
        android.app.TimePickerDialog tpd = new android.app.TimePickerDialog(activity, theme, new android.app.TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                date.set(Calendar.HOUR_OF_DAY, hourOfDay);
                date.set(Calendar.MINUTE, minute);
                date.set(Calendar.SECOND, 0);
                buttonTime.setText(new SimpleDateFormat(SD.TIME_FORMAT_FOR_ALARM, new Locale("ru")).format(date.getTime()));
            }
        }, date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE), true);
        tpd.show();
    }

}
