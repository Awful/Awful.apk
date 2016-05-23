package com.ferg.awfulapp.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import com.ferg.awfulapp.R;

import java.util.Locale;

/**
 * Created by baka kaba on 22/05/2016.
 * <p/>
 * Creates a NumberPicker alert dialog, with min/max buttons.
 */
public class MinMaxNumberPicker {

    private final int minValue;
    private final int maxValue;
    private final int initialValue;
    private final String title;

    private final ResultListener listener;
    private final AlertDialog.Builder builder;

    /**
     * Get a MinMaxNumberPicker, which can be displayed with {@link #show()}
     *
     * @param context        A context to associate with the AlertDialog
     * @param minValue       The minimum value that can be selected
     * @param maxValue       The maximum value that can be selected
     * @param initialValue   The initial value - this will be bound to min/max if necessary
     * @param title          The dialog's title, if required
     * @param resultListener A callback for when the user selects a dialog option
     */
    public MinMaxNumberPicker(Context context, int minValue, int maxValue, int initialValue, @Nullable String title, @NonNull ResultListener resultListener) {
        builder = new AlertDialog.Builder(context);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.initialValue = initialValue;
        this.title = (title == null) ? "" : title;
        listener = resultListener;
        setUpPicker();
    }


    /**
     * Show the page picker dialog, and handle user input and navigation.
     */
    private void setUpPicker() {
        LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        @SuppressLint("InflateParams") View pickerView = inflater.inflate(R.layout.number_picker, null);
        final NumberPicker picker = (NumberPicker) pickerView.findViewById(R.id.pagePicker);
        final Button minButton = (Button) pickerView.findViewById(R.id.min);
        final Button maxButton = (Button) pickerView.findViewById(R.id.max);

        View.OnClickListener minMaxClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                picker.setValue(v == minButton ? minValue : maxValue);
            }
        };

        picker.setMinValue(minValue);
        picker.setMaxValue(maxValue);
        // make sure the initial value is within the min/max bounds
        int boundedInitialValue = Math.max(minValue, Math.min(initialValue, maxValue));
        picker.setValue(boundedInitialValue);

        minButton.setOnClickListener(minMaxClickListener);
        maxButton.setOnClickListener(minMaxClickListener);
        minButton.setText(String.format(Locale.getDefault(), "%d", minValue));
        maxButton.setText(String.format(Locale.getDefault(), "%d", maxValue));


        DialogInterface.OnClickListener dialogButtonClickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface aDialog, int aWhich) {
                // clearing focus will read any value entered with the keyboard, in case
                // the user hasn't hit the keyboard's confirm button. This relies on
                // NumberPicker's input validation to ignore bad values
                picker.clearFocus();
                listener.onButtonPressed(aWhich, picker.getValue());
            }
        };

        builder.setTitle(title)
                .setView(pickerView)
                .setPositiveButton(R.string.alert_ok, dialogButtonClickListener)
                .setNegativeButton(R.string.cancel, dialogButtonClickListener);
    }


    /**
     * Display the picker.
     */
    public void show() {
        builder.show();
    }


    public interface ResultListener {

        /**
         * Called when the user clicks the picker's positive or negative button.
         *
         * @param button      The ID of the button, e.g. {@link DialogInterface#BUTTON_POSITIVE}
         * @param resultValue The value set on the picker
         */
        void onButtonPressed(int button, int resultValue);

    }

}
