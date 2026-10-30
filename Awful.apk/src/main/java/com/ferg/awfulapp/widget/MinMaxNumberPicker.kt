package com.ferg.awfulapp.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import com.ferg.awfulapp.R
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Created by baka kaba on 22/05/2016.
 * 
 * 
 * Creates a NumberPicker alert dialog, with min/max buttons.
 */
/**
 * Get a MinMaxNumberPicker, which can be displayed with [.show]
 *
 * @param context        A context to associate with the AlertDialog
 * @param minValue       The minimum value that can be selected
 * @param maxValue       The maximum value that can be selected
 * @param initialValue   The initial value - this will be bound to min/max if necessary
 * @param title          The dialog's title, if required
 * @param listener A callback for when the user selects a dialog option
 */
open class MinMaxNumberPicker(
    context: Context,
    private val minValue: Int,
    private val maxValue: Int,
    private val initialValue: Int,
    title: String?,
    private val listener: ResultListener
) {
    private val title: String = title ?: ""

    private val builder: AlertDialog.Builder = AlertDialog.Builder(context)

    init {
        setUpPicker()
    }


    /**
     * Show the page picker dialog, and handle user input and navigation.
     */
    private fun setUpPicker() {
        val inflater = LayoutInflater.from(builder.context)
        @SuppressLint("InflateParams") val pickerView =
            inflater.inflate(R.layout.number_picker, null)
        val picker = pickerView.findViewById<NumberPicker>(R.id.pagePicker)
        val minButton = pickerView.findViewById<Button>(R.id.min)
        val maxButton = pickerView.findViewById<Button>(R.id.max)

        val minMaxClickListener: View.OnClickListener =
            View.OnClickListener { v -> picker.value = if (v === minButton) minValue else maxValue }

        picker.minValue = minValue
        picker.maxValue = maxValue
        // make sure the initial value is within the min/max bounds
        val boundedInitialValue = max(minValue, min(initialValue, maxValue))
        picker.value = boundedInitialValue

        minButton.setOnClickListener(minMaxClickListener)
        maxButton.setOnClickListener(minMaxClickListener)
        minButton.text = String.format(Locale.getDefault(), "%d", minValue)
        maxButton.text = String.format(Locale.getDefault(), "%d", maxValue)


        val dialogButtonClickListener: DialogInterface.OnClickListener =
            DialogInterface.OnClickListener { _, aWhich ->
                // clearing focus will read any value entered with the keyboard, in case
                // the user hasn't hit the keyboard's confirm button. This relies on
                // NumberPicker's input validation to ignore bad values
                picker.clearFocus()
                listener.onButtonPressed(aWhich, picker.value)
            }

        builder.setTitle(title)
            .setView(pickerView)
            .setPositiveButton(R.string.alert_ok, dialogButtonClickListener)
            .setNegativeButton(R.string.cancel, dialogButtonClickListener)
    }


    /**
     * Display the picker.
     */
    fun show() {
        builder.show()
    }


    interface ResultListener {
        /**
         * Called when the user clicks the picker's positive or negative button.
         * 
         * @param button      The ID of the button, e.g. [DialogInterface.BUTTON_POSITIVE]
         * @param resultValue The value set on the picker
         */
        fun onButtonPressed(button: Int, resultValue: Int)
    }
}
