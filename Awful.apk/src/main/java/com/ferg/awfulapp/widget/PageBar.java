package com.ferg.awfulapp.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.databinding.PageBarBinding;

import java.util.Locale;

/**
 * Created by baka kaba on 25/05/2016.
 * <p/>
 * A navigation/refresh widget used for paged views.
 * <p/>
 * Add a listener through {@link #setListener(PageBarCallbacks)} to respond to user interactions.
 */
public class PageBar extends FrameLayout {

    public static final int FIRST_PAGE = 1;
    PageBarBinding binding;

    private PageBarCallbacks listener = null;

    public PageBar(Context context) {
        super(context);
        init();
    }

    public PageBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PageBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PageBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }


    private void init() {
        binding = PageBarBinding.inflate(LayoutInflater.from(getContext()), this, true);
        updatePagePosition(FIRST_PAGE, FIRST_PAGE);
        onRefreshClicked(binding.refresh);
        onRefreshClicked(binding.refreshAlt);
        onNavButtonClicked(binding.nextPage);
        onNavButtonClicked(binding.prevPage);
        onPageNumberClicked(binding.pageCountText);
    }

    /**
     * Update the page bar to reflect the current position in a range of pages.
     * <p/>
     * This will affect the layout of the navigation buttons, depending on where the current page is
     * in the range. Previous and next page buttons only appear when there's a page to go to, and
     * if only the previous page button is visible, the refresh button will move to the right side.
     * <p/>
     * This widget does no page number validity checks, except for ignoring numbers below
     * {@link #FIRST_PAGE} when displaying the lastPage value.
     *
     * @param currentPage the number of the current page
     * @param lastPage    the number of last page in the page range
     */
    public void updatePagePosition(int currentPage, int lastPage) {
        PageType type;
        if (currentPage == FIRST_PAGE) {
            type = (currentPage == lastPage) ? PageType.SINGLE : PageType.FIRST_OF_MANY;
        } else if (currentPage == lastPage) {
            type = PageType.LAST_OF_MANY;
        } else {
            type = PageType.ONE_OF_MANY;
        }
        // if currentPage is greater than lastPage, then lastPage isn't a meaningful page count (-1 is passed in when we don't have that data anyway)
        boolean hasPageCount = lastPage >= currentPage;
        updateDisplay(currentPage, lastPage, type, hasPageCount);
    }


    private void updateDisplay(int currentPage, int lastPage, @NonNull PageType pageType, boolean hasPageCount) {
        String template = hasPageCount ? "%d / %d" : "%d";
        binding.pageCountText.setText(String.format(Locale.getDefault(), template, currentPage, lastPage));
        /*
            hide and show the appropriate icons for each state:
            - don't show the prev/next arrow on the first/last page
            - show the refresh icon on the side without an arrow
            - if both sides have an arrow (or neither does), show on the left side
            doing all the hiding before the showing should ensure clean transition animations, otherwise you can get stuff appearing on top of its replacement etc
        */
        switch (pageType) {
            case SINGLE:
                binding.prevPage.setVisibility(GONE);
                binding.nextPage.setVisibility(GONE);
                binding.refreshAlt.setVisibility(GONE);
                binding.refresh.setVisibility(VISIBLE);
                break;
            case FIRST_OF_MANY:
                binding.prevPage.setVisibility(GONE);
                binding.refreshAlt.setVisibility(GONE);
                binding.refresh.setVisibility(VISIBLE);
                binding.nextPage.setVisibility(VISIBLE);
                break;
            case ONE_OF_MANY:
                binding.refreshAlt.setVisibility(GONE);
                binding.prevPage.setVisibility(VISIBLE);
                binding.refresh.setVisibility(VISIBLE);
                binding.nextPage.setVisibility(VISIBLE);
                break;
            case LAST_OF_MANY:
                binding.nextPage.setVisibility(GONE);
                binding.refresh.setVisibility(GONE);
                binding.prevPage.setVisibility(VISIBLE);
                binding.refreshAlt.setVisibility(VISIBLE);
        }
    }


    /**
     * Set a listener for callbacks when the user interacts with the bar.
     */
    public void setListener(@Nullable PageBarCallbacks listener) {
        this.listener = listener;
    }

    public void onRefreshClicked(ImageButton button) {
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onRefreshClicked();
                }
            }
        });
    }

    public void onNavButtonClicked(ImageButton button) {
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onPageNavigation(button.getId() == R.id.next_page);
                }
            }
        });
    }

    public void onPageNumberClicked(TextView pageNumber) {
        pageNumber.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onPageNumberClicked();
                }
            }
        });

    }

    // TODO: probably best to add a setter for the stuff that uses this

    /**
     * Get a reference to the page text component on the page bar.
     */
    @NonNull
    public View getTextView() {
        return binding.pageCountText;
    }

    public void setTextColour(@ColorInt int textColour) {
        binding.pageCountText.setTextColor(textColour);
    }

    private enum PageType {SINGLE, FIRST_OF_MANY, LAST_OF_MANY, ONE_OF_MANY}

    public interface PageBarCallbacks {
        /**
         * Called when the user clicks on the next or previous page buttons.
         *
         * @param nextPage true for next page, false for previous
         */
        void onPageNavigation(boolean nextPage);

        /**
         * Called when the user clicks on a refresh button.
         */
        void onRefreshClicked();

        /**
         * Called when the user clicks on the page number display.
         */
        void onPageNumberClicked();
    }
}
