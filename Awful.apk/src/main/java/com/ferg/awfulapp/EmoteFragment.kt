package com.ferg.awfulapp

import android.database.Cursor
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.*
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v4.view.ViewPager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.volley.VolleyError
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.Keys
import com.ferg.awfulapp.provider.AwfulProvider
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.service.AwfulCursorAdapter
import com.ferg.awfulapp.task.AwfulRequest
import com.ferg.awfulapp.task.EmoteRequest
import com.ferg.awfulapp.thread.AwfulEmote

/**
 * Created by baka kaba on 28/12/2017.
 *
 * A rework of the original EmoteFragment.java
 *
 * This version adds an extra tabbed page which displays the user's most recent choices, and breaks
 * out the shared functionality into a Fragment class the pages can inherit from. The fragments don't
 * hold any state (including references to the MessageComposer that probably launched it) so they
 * should behave if they need to be recreated.
 *
 * Later it would be good to add another tab for favourites (and add that fav/unfav functionality to all the views)
 */

/**
 * Singleton handling the user's current recent emotes, that loads and saves state as required.
 *
 * If favourites are added they should probably go in here too, it's pretty similar functionality!
 */
private object EmoteHistory {
    private const val MAX_RECENT_EMOTES = 30
    const val SEPARATOR = " "
    private val recentList: MutableList<String> by lazy {
        AwfulPreferences.getInstance().getPreference(Keys.RECENT_EMOTES, "")!!.split(SEPARATOR).toMutableList()
    }

    /**
     * Get the list of recent emote codes, as a [SEPARATOR] separated string.
     */
    fun getRecent(): String = recentList.joinToString(SEPARATOR)

    /**
     * Add an emote code to the list of recent emotes, removing the oldest if the list has grown too large.
     *
     * If the code is already in the list, it's set as the newest. This doesn't track frequency of use,
     * it just works on a last-in-last-out basis.
     */
    fun addRecent(emoteCode: String) {
        // add the code to the end of the list, removing any existing copy, and trim the list down
        with(recentList) {
            remove(emoteCode)
            plus(emoteCode).takeLast(MAX_RECENT_EMOTES).let { newRecent -> clear(); addAll(newRecent) }
        }
        AwfulPreferences.getInstance().setPreference(Keys.RECENT_EMOTES, getRecent())
    }
}


/**
 * A dialog that lets the user view and choose emotes, including a searchable list and their most recent choices.
 *
 * This must be created by a parent fragment that implements [EmotePickerListener]
 */
class EmotePicker : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.emote_picker_container_fragment, container, false)
    }

    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)
        dialog.setTitle(R.string.reply_emotes)
        val viewPager by bind<ViewPager>(R.id.emote_view_pager)
        val tabLayout by bind<TabLayout>(R.id.tab_layout)
        with(EmotePagerAdapter(childFragmentManager)) {
            viewPager!!.adapter = this
            tabLayout!!.setupWithViewPager(viewPager)
            pages.forEachIndexed { i, page -> tabLayout.getTabAt(i)!!.setIcon(page.iconResId).contentDescription = page.title }
        }
    }

    fun onEmoteChosen(emoteCode: String) {
        Toast.makeText(activity, emoteCode, Toast.LENGTH_SHORT).show()
        (parentFragment as EmotePickerListener).onEmoteChosen(emoteCode)
        EmoteHistory.addRecent(emoteCode)
        dismiss()
    }
}


private class EmotePagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
    data class Page(val fragmentGetter: () -> Fragment, val title: String, val iconResId: Int)

    val pages = arrayOf(
            Page({ EmoteFragment() }, "Search", R.drawable.ic_search_dark),
            Page({ EmoteHistoryFragment() }, "Recent", R.drawable.ic_history_dark_24dp)
    )
    override fun getCount() = pages.size
    override fun getItem(position: Int) = pages[position].fragmentGetter()
    // uncomment this to get titles on tabs
//    override fun getPageTitle(position: Int) = pages[position].title
}

/**
 * A fragment that displays all the site emotes, with a live search box to filter them.
 */
class EmoteFragment : EmoteGridFragment() {

    override val layoutId = R.layout.emote_picker_main_fragment
    private val emoteGrid: GridView? by bind(R.id.emote_grid)
    private val filterText: EditText? by bind(R.id.filter_text)

    override fun getTitle() = "Emotes"

    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)
        emoteGrid?.setUpEmoteGrid()

        // set up the delete button to clear the filter field - which triggers its text change callback
        bind<ImageButton>(R.id.delete_button).value?.setOnClickListener { filterText!!.setText("") }

        filterText?.apply {
            setTextColor(ColorProvider.PRIMARY_TEXT.color)
            addTextChangedListener(object : TextWatcher {
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable) {
                    emoteLoader.currentFilter = (filterText!!.text.toString().trim())
                }
            })
        }
    }
}


/**
 * A fragment that displays the user's most recently used emotes.
 */
class EmoteHistoryFragment : EmoteGridFragment() {

    override val layoutId = R.layout.emote_picker_history_fragment
    private val emoteGrid: GridView? by bind(R.id.emote_grid)

    override fun getTitle() = "Emote history"

    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)
        emoteGrid?.setUpEmoteGrid()
        with(emoteLoader) {
            filterExactCode = true
            currentFilter = EmoteHistory.getRecent().let { recent ->
                // a blank filter list will show all emotes, so we need a term that shouldn't match anything...
                if (recent.isBlank()) "match_nothing_thanks" else recent
            }
        }
    }
}


/**
 * Class with shared code for the fragment pages, mostly handling the emote grid and lifecycle stuff.
 */
abstract class EmoteGridFragment : AwfulFragment() {

    abstract protected val layoutId: Int

    private val cursorAdapter by lazy { AwfulCursorAdapter(activity as AwfulActivity, null, null) }
    val emoteLoader by lazy { EmoteDataCallback(cursorAdapter) }
    // TODO can go in the loader callbacks?
    var loadFailed = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(layoutId, container, false)


    /**
     * Initialises a GridView as an EmoteGrid with all the basic settings - call this from your subclass!
     */
    fun GridView.setUpEmoteGrid() {
        adapter = cursorAdapter
        setBackgroundColor(ColorProvider.BACKGROUND.color)
        setOnItemClickListener { _, view, _, _ ->
            val emoteCode = view.findViewById<TextView>(R.id.emote_text).text.toString().trim()
            (parentFragment as EmotePicker).onEmoteChosen(emoteCode)
        }
    }

    override fun onStart() {
        super.onStart()
        restartLoader()
    }

    override fun onStop() {
        super.onStop()
        loaderManager.destroyLoader(Constants.EMOTE_LOADER_ID)
    }

    private fun restartLoader() = restartLoader(Constants.EMOTE_LOADER_ID, null, emoteLoader)

    fun syncEmotes() {
        activity?.let {
            queueRequest(EmoteRequest(activity).build(this, object : AwfulRequest.AwfulResultCallback<Void> {
                override fun success(result: Void?) {
                    loadFailed = false
                    awfulActivity?.let { restartLoader() }
                }

                override fun failure(error: VolleyError?) {
                    loadFailed = true
                }
            }))
        }
    }

    inner class EmoteDataCallback(
            private val adapter: AwfulCursorAdapter
    ) : LoaderManager.LoaderCallbacks<Cursor> {

        /** when true the filter will only match emote codes exactly, otherwise it searches within codes and the emotes' title subtexts */
        var filterExactCode = false
        var currentFilter: String? = null
            get() = field.let { if (it != null && it.isNotBlank()) it else null }
            set(value) {
                field = value; restartLoader()
            }


        /**
         * Generate an SQL query selection string and its argument list from a set of search terms, according to the type of filtering we're doing.
         */
        private fun List<String>.asSelectionAndArgs(): Pair<String, Array<String>> {
            return if (filterExactCode) {
                // searching for the specific code strings, used to select specific emotes - look in TEXT only, match exactly
                val selection = Array(size) { "${AwfulEmote.TEXT}=?" }.joinToString(separator = " OR ")
                Pair(selection, this.toTypedArray())
            } else {
                // general search (used for the search view) - search for each term twice, in emote titles and also the subtext (image title attribute)
                val searchColumns = listOf(AwfulEmote.TEXT, AwfulEmote.SUBTEXT)
                val selection = flatMap { searchColumns }.joinToString(separator = " OR ") { "$it  LIKE '%' || ? || '%'" }
                Pair(selection, flatMap { term -> List(searchColumns.size) { term } }.toTypedArray())
            }

        }


        override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
            Log.v(TAG, "Creating emote cursor")
            return currentFilter.let { filterText ->
                // break the filter text into multiple keywords, and create a query that matches any of them
                val selectionAndArgs = filterText?.split(EmoteHistory.SEPARATOR)?.filterNot(String::isBlank)?.asSelectionAndArgs() ?: Pair(null, null)
                CursorLoader(activity!!,
                        AwfulEmote.CONTENT_URI,
                        AwfulProvider.EmoteProjection,
                        selectionAndArgs.first,
                        selectionAndArgs.second,
                        AwfulEmote.INDEX)
            }
        }

        override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
            adapter.swapCursor(data)
            if (data.count < 5 && currentFilter == null && !loadFailed) {
                syncEmotes()
            }
        }

        override fun onLoaderReset(loader: Loader<Cursor>) {
            adapter.swapCursor(null)
        }
    }

}

/**
 * Lets you late-bind views as vals, so long as you only access them after the view has been created
 */
private fun <T : View> Fragment.bind(resId: Int): Lazy<T?> = lazy(LazyThreadSafetyMode.NONE) { view!!.findViewById<T>(resId) }

interface EmotePickerListener {
    fun onEmoteChosen(emoteCode: String)
}
