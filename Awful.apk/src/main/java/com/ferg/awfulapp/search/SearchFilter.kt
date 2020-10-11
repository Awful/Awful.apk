package com.ferg.awfulapp.search

import android.os.Parcel
import android.os.Parcelable
import androidx.appcompat.app.AlertDialog
import android.view.View
import android.widget.EditText
import com.ferg.awfulapp.R

/**
 * Created by baka kaba on 13/06/2018.
 *
 * Represents a filter for use in forums searches.
 *
 * This holds a type (reflecting the different search keywords available on the site) and the actual
 * data being searched for / filtered on - text, IDs etc.
 */
class SearchFilter(val type: FilterType, val param: String) : Parcelable {

    /**
     * Describes the various types of filters available in the forums search.
     *
     * [filterTemplate] is used to produce the output required by the query string.
     * [label] is a short name for the filter, and [description] is an optional longer version used
     * in the hint text for input dialogs.
     */
    enum class FilterType(val filterTemplate: String, val label: String, val description: String? = null) {

        PostText("%s", "Text in posts"),
        UserId("userid:%s", "User ID"),
        Username("username:\"%s\"", "Username"),
        Quoting("quoting:\"%s\"", "User being quoted"),
        Before("before:\"%s\"", "Earlier than"),
        After("since:\"%s\"", "Later than"),
        ThreadId("threadid:%s", "Thread ID"),
        InTitle("intitle:\"%s\"", "Thread title", "Text in thread title");


        /**
         * Show a popup dialog allowing the user to add data to filter on.
         * Sets the result on the provided #SearchFragment.
         */
        fun showDialog(searchFragment: SearchFragment) {
            searchFragment.activity?.run {
                val layout = layoutInflater.inflate(R.layout.insert_text_dialog, null)
                val textField = layout.findViewById<View>(R.id.text_field) as EditText
                textField.hint = description ?: label
                AlertDialog.Builder(this)
                        .setTitle("Add search filter")
                        .setView(layout)
                        .setPositiveButton("Add filter", { _, _ ->
                            searchFragment.addFilter(SearchFilter(this@FilterType, textField.text.toString()))
                        })
                        .show()
            }
        }
    }

    override fun toString(): String = type.filterTemplate.format(param)


    //
    // Parcelable boilerplate - maybe use @Parcelize from Kotlin Extensions?
    //

    constructor(parcel: Parcel) : this(
            FilterType.values()[parcel.readInt()],
            parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(type.ordinal)
        parcel.writeString(param)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<SearchFilter> {
            override fun createFromParcel(parcel: Parcel): SearchFilter = SearchFilter(parcel)
            override fun newArray(size: Int): Array<SearchFilter?> = arrayOfNulls(size)
        }
    }

}