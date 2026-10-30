package com.ferg.awfulapp.thread

import android.content.res.Resources
import android.content.res.Resources.NotFoundException
import android.graphics.drawable.Drawable
import android.util.Log
import com.ferg.awfulapp.constants.Constants

/**
 * 
 * Created by baka kaba on 26/05/2015.
 * 
 * 
 * Defines a thread element as a collection of all possible items
 * 
 * 
 * Thread elements are attributes associated with a thread - ratings,
 * tags etc. This class lets you create a lookup table for an element
 * type, get IDs and categories for each, and assign drawables for
 * each element.
 * 
 * 
 * It's designed to be plugged into a static class that parses a URL,
 * determines a category and identifier token for the asset, and uses them
 * to search the collection of defined items. That class should also wrap
 * these methods to provide easy access to element attributes and resources
 */
internal class ElementCollection(description: String?) {
    private val TAG: String = Class::class.java.simpleName + (if (description == null) "" else ":$description")
    private val elements: MutableList<Element> = ArrayList()

    /**
     * This is the ID representing a missing or unidentified element.
     * You might want to make this available as a public constant in
     * your handler class, for 'is an element present' checks
     */
    /**
     * Create a new collection of a type of thread element.
     * @param description     Appended to logging tags
     */
    @JvmField
    val NULL_ELEMENT_ID: Int = add(-1, null, null)




    /**
     * Add an element to this collection.
     * See [Element] for more detail.
     * @param category          An ID used to divide the collection into categories of
     * items, e.g. secondary tags may belong to SA Mart,
     * Ask/Tell etc.
     * @param identifierToken   Used to identify this particular element - should be
     * unique within its category. This will generally be
     * something produced by the URL parser, e.g. a filename
     * @param drawableId        The ID of a drawable resource associated with this element
     * (may be null)
     * @return                  The added element's ID, used in the class's get* methods
     */
    fun add(category: Int, identifierToken: String?, drawableId: Int?): Int {
        val element = Element(category, identifierToken, drawableId)
        elements.add(element)
        return elements.indexOf(element)
    }


    /**
     * Find an element by category and identifier token.
     * @param categoryId        Specifies the category to check
     * @param identifierToken   The unique token encoded in the URL
     * @return                  The ID of a matching element in the collection, otherwise [.NULL_ELEMENT_ID]
     */
    fun findElement(categoryId: Int, identifierToken: String): Int {
        for (element in elements) {
            if (element.category == categoryId && identifierToken == element.identifierToken) {
                return elements.indexOf(element)
            }
        }
        if (Constants.DEBUG) {
            Log.w(
                TAG,
                String.format(
                    "No match for token (%s) for category (%d)!",
                    identifierToken,
                    categoryId
                )
            )
        }
        return NULL_ELEMENT_ID
    }


    /**
     * Get the category ID for a given element in a collection.
     * If the element cannot be found, the supplied default value is returned.
     * @param elementId     The ID of the element to look up
     * @param defaultValue  The value to return if the element is missing
     * @return              The category ID associated with this element
     */
    fun getType(elementId: Int, defaultValue: Int): Int {
        try {
            val element = elements[elementId]
            return element.category
        } catch (e: IndexOutOfBoundsException) {
            if (Constants.DEBUG) Log.w(
                TAG,
                "Can't get category for an unknown elementID: " + elementId
            )
            return defaultValue
        }
    }


    /**
     * Get the drawable associated with a given element ID.
     * @param elementId     The ID of the element to look up
     * @param resources
     * @return              Any associated drawable, otherwise null
     */
    fun getDrawable(elementId: Int, resources: Resources?): Drawable? {
        if (resources == null) {
            Log.w(TAG, "Null Resources object passed when getting drawable!")
            return null
        }
        try {
            val element = elements[elementId]
            if (element.drawableId != null) {
                return resources.getDrawable(element.drawableId)
            }
        } catch (e: NotFoundException) {
            if (Constants.DEBUG) Log.w(TAG, String.format("No drawable for ID: %d!", elementId))
        } catch (e: IndexOutOfBoundsException) {
            if (Constants.DEBUG) Log.w(TAG, "Can't get drawable for an unknown ID: " + elementId)
        }
        return null
    }


    /**
     * An object defining an element of a thread, e.g. a rating, a tag, etc.
     * Use these to build up a collection of known items in a handler, like
     * a list of all known ratings the site might throw at us.
     */
    private class Element(
        /**
         * A constant used to group elements into categories, such as
         * different rating types, which forum a secondary tag belongs to, etc.
         * The parser will probably need to determine this by the format of the
         * incoming URL - the path, the naming convention of the asset's filename, etc.
         * Together with the [.identifierToken] this forms a unique
         * reference to a particular element.
         */
        val category: Int,
        /**
         * A token used to identify a unique element within a category group.
         * This should be something the parser can produce from a URL, it can
         * be as simple as the filename of an image, like '5stars.gif'
         */
        val identifierToken: String?,
        /** The resource ID of a drawable for this element (may be null)  */
        val drawableId: Int?
    )
}
