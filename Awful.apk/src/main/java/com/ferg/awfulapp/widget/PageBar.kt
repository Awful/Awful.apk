package com.ferg.awfulapp.widget

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.ColorInt
import com.ferg.awfulapp.R
import com.ferg.awfulapp.databinding.PageBarBinding
import com.ferg.awfulapp.util.AwfulUtils.isAtLeast
import java.util.Locale

/**
 * Created by baka kaba on 25/05/2016.
 * 
 * 
 * A navigation/refresh widget used for paged views.
 * 
 * 
 * Add a listener through [.setListener] to respond to user interactions.
 */
class PageBar : FrameLayout {
    lateinit var binding: PageBarBinding

    private var listener: PageBarCallbacks? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }


    private fun init() {
        binding = PageBarBinding.inflate(LayoutInflater.from(context), this, true)
        updatePagePosition(FIRST_PAGE, FIRST_PAGE)
        onRefreshClicked(binding.refresh)
        onRefreshClicked(binding.refreshAlt)
        onNavButtonClicked(binding.nextPage)
        onNavButtonClicked(binding.prevPage)
        onPageNumberClicked(binding.pageCountText)
        binding.pageBarContainer.addOnAttachStateChangeListener(object :
            OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                checkPadding()
            }

            override fun onViewDetachedFromWindow(view: View) {}
        })
    }

    fun checkPadding() {
        //check edge-to-edge
        if (isAtLeast(Build.VERSION_CODES.VANILLA_ICE_CREAM)) {
            val inset = binding.getRoot().rootWindowInsets
                .getInsets(WindowInsets.Type.systemGestures())
            // check navigation buttons
            if (inset.left <= 0) {
                val container: View = binding.pageBarContainer
                val buttonSpacing =
                    ((20 * resources.displayMetrics.density) + container.paddingBottom).toInt()
                container.setPadding(
                    container.paddingLeft,
                    container.paddingTop,
                    container.paddingRight,
                    buttonSpacing
                )
            }
        }
    }

    /**
     * Update the page bar to reflect the current position in a range of pages.
     * 
     * 
     * This will affect the layout of the navigation buttons, depending on where the current page is
     * in the range. Previous and next page buttons only appear when there's a page to go to, and
     * if only the previous page button is visible, the refresh button will move to the right side.
     * 
     * 
     * This widget does no page number validity checks, except for ignoring numbers below
     * [.FIRST_PAGE] when displaying the lastPage value.
     * 
     * @param currentPage the number of the current page
     * @param lastPage    the number of last page in the page range
     */
    fun updatePagePosition(currentPage: Int, lastPage: Int) {
        val type = if (currentPage == FIRST_PAGE) {
            if (currentPage == lastPage) PageType.SINGLE else PageType.FIRST_OF_MANY
        } else if (currentPage == lastPage) {
            PageType.LAST_OF_MANY
        } else {
            PageType.ONE_OF_MANY
        }
        // if currentPage is greater than lastPage, then lastPage isn't a meaningful page count (-1 is passed in when we don't have that data anyway)
        val hasPageCount = lastPage >= currentPage
        updateDisplay(currentPage, lastPage, type, hasPageCount)
    }


    private fun updateDisplay(
        currentPage: Int,
        lastPage: Int,
        pageType: PageType,
        hasPageCount: Boolean
    ) {
        val template = if (hasPageCount) "%d / %d" else "%d"
        binding.pageCountText.text = String.format(
            Locale.getDefault(),
            template,
            currentPage,
            lastPage
        )
        /*
            hide and show the appropriate icons for each state:
            - don't show the prev/next arrow on the first/last page
            - show the refresh icon on the side without an arrow
            - if both sides have an arrow (or neither does), show on the left side
            doing all the hiding before the showing should ensure clean transition animations, otherwise you can get stuff appearing on top of its replacement etc
        */
        when (pageType) {
            PageType.SINGLE -> {
                binding.prevPage.visibility = GONE
                binding.nextPage.visibility = GONE
                binding.refreshAlt.visibility = GONE
                binding.refresh.visibility = VISIBLE
            }

            PageType.FIRST_OF_MANY -> {
                binding.prevPage.visibility = GONE
                binding.refreshAlt.visibility = GONE
                binding.refresh.visibility = VISIBLE
                binding.nextPage.visibility = VISIBLE
            }

            PageType.ONE_OF_MANY -> {
                binding.refreshAlt.visibility = GONE
                binding.prevPage.visibility = VISIBLE
                binding.refresh.visibility = VISIBLE
                binding.nextPage.visibility = VISIBLE
            }

            PageType.LAST_OF_MANY -> {
                binding.nextPage.visibility = GONE
                binding.refresh.visibility = GONE
                binding.prevPage.visibility = VISIBLE
                binding.refreshAlt.visibility = VISIBLE
            }
        }
    }


    /**
     * Set a listener for callbacks when the user interacts with the bar.
     */
    fun setListener(listener: PageBarCallbacks?) {
        this.listener = listener
    }

    fun onRefreshClicked(button: ImageButton) {
        button.setOnClickListener {
            listener?.onRefreshClicked()
        }
    }

    fun onNavButtonClicked(button: ImageButton) {
        button.setOnClickListener {
            listener?.onPageNavigation(button.id == R.id.next_page)
        }
    }

    fun onPageNumberClicked(pageNumber: TextView) {
        pageNumber.setOnClickListener {
            listener?.onPageNumberClicked()
        }
    }

    // TODO: probably best to add a setter for the stuff that uses this
    val textView: View
        /**
         * Get a reference to the page text component on the page bar.
         */
        get() = binding.pageCountText

    fun setTextColour(@ColorInt textColour: Int) {
        binding.pageCountText.setTextColor(textColour)
    }

    private enum class PageType {
        SINGLE, FIRST_OF_MANY, LAST_OF_MANY, ONE_OF_MANY
    }

    interface PageBarCallbacks {
        /**
         * Called when the user clicks on the next or previous page buttons.
         * 
         * @param nextPage true for next page, false for previous
         */
        fun onPageNavigation(nextPage: Boolean)

        /**
         * Called when the user clicks on a refresh button.
         */
        fun onRefreshClicked()

        /**
         * Called when the user clicks on the page number display.
         */
        fun onPageNumberClicked()
    }

    companion object {
        const val FIRST_PAGE: Int = 1
    }
}
