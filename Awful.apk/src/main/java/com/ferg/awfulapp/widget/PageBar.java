package com.ferg.awfulapp.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.ferg.awfulapp.R;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by baka kaba on 25/05/2016.
 * <p/>
 * A navigation/refresh widget used for paged views.
 * <p/>
 * Add a listener through {@link #setListener(PageBarCallbacks)} to respond to user interactions.
 */
public class PageBar extends FrameLayout {

    public static final int FIRST_PAGE = 1;
    @BindView(R.id.page_count_text)
    TextView mPageCountText;
    @BindView(R.id.next_page)
    ImageButton nextPageButton;
    @BindView(R.id.prev_page)
    ImageButton prevPageButton;
    @BindView(R.id.refresh)
    ImageButton refreshButton;
    @BindView(R.id.refresh_alt)
    ImageButton altRefreshButton;

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
        View pageBar = LayoutInflater.from(getContext()).inflate(R.layout.page_bar, this, true);
        ButterKnife.bind(pageBar);
        updatePagePosition(FIRST_PAGE, FIRST_PAGE);
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
            /*
                Possible states and their button layouts (left side / right side):
                    single page -         refresh /
                    first of many -       refresh / next
                    middle of many - prev refresh / next
                    last of many -   prev         / refresh2

             */
        String text = String.format(Locale.getDefault(), "Page %d%s", currentPage, lastPage >= FIRST_PAGE ? "/" + lastPage : "");
//        String text = String.format(Locale.getDefault(), getResources().getString(R.string.page_bar_text), currentPage, lastPage >= FIRST_PAGE ? "/" + lastPage : "");
        mPageCountText.setText(text);
        boolean isFirstPage = currentPage == FIRST_PAGE;
        boolean isLastPage = currentPage == lastPage;

        int prevPageVisibility = isFirstPage ? GONE : VISIBLE;
        int nextPageVisibility = isLastPage ? GONE : VISIBLE;
        prevPageButton.setVisibility(prevPageVisibility);
        nextPageButton.setVisibility(nextPageVisibility);

        // move (show the other) refresh icon if the next page button is missing,
        // and the prev page button is present (basically filling the space)
        boolean showAltRefresh = nextPageVisibility != VISIBLE && prevPageVisibility == VISIBLE;
        refreshButton.setVisibility(showAltRefresh ? GONE : VISIBLE);
        altRefreshButton.setVisibility(showAltRefresh ? VISIBLE : GONE);
    }


    /**
     * Set a listener for callbacks when the user interacts with the bar.
     */
    public void setListener(@Nullable PageBarCallbacks listener) {
        this.listener = listener;
    }

    @OnClick({R.id.refresh, R.id.refresh_alt})
    public void onRefreshClicked() {
        if (listener != null) {
            listener.onRefreshClicked();
        }
    }

    @OnClick({R.id.next_page, R.id.prev_page})
    public void onNavButtonClicked(View view) {
        if (listener != null) {
            listener.onPageNavigation(view.getId() == R.id.next_page);
        }
    }


    @OnClick({R.id.page_count_text})
    public void onPageNumberClicked() {
        if (listener != null) {
            listener.onPageNumberClicked();
        }
    }

    // TODO: probably best to add a setter for the stuff that uses this

    /**
     * Get a reference to the page text component on the page bar.
     */
    @NonNull
    public View getTextView() {
        return mPageCountText;
    }

    public void setTextColour(@ColorInt int textColour) {
        mPageCountText.setTextColor(textColour);
    }

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
