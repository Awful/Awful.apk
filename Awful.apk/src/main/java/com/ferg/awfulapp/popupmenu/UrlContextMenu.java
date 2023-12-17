package com.ferg.awfulapp.popupmenu;

import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.ThreadDisplayFragment;
import com.ferg.awfulapp.provider.ColorProvider;

import java.util.ArrayList;
import java.util.List;


import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.ferg.awfulapp.popupmenu.UrlContextMenu.UrlMenuAction.COPY_LINK_URL;
import static com.ferg.awfulapp.popupmenu.UrlContextMenu.UrlMenuAction.DISPLAY_IMAGE;
import static com.ferg.awfulapp.popupmenu.UrlContextMenu.UrlMenuAction.DOWNLOAD_IMAGE;
import static com.ferg.awfulapp.popupmenu.UrlContextMenu.UrlMenuAction.OPEN_URL;
import static com.ferg.awfulapp.popupmenu.UrlContextMenu.UrlMenuAction.PLAY_GIF;
import static com.ferg.awfulapp.popupmenu.UrlContextMenu.UrlMenuAction.SHARE_URL;
import static com.ferg.awfulapp.popupmenu.UrlContextMenu.UrlMenuAction.SHOW_INLINE;

/**
 * Created by baka kaba on 22/05/2017.
 * <p>
 * A popup context menu for when a URL is clicked.
 */
public class UrlContextMenu extends BasePopupMenu<UrlContextMenu.UrlMenuAction> {

    {
        layoutResId = R.layout.select_url_action_dialog;
    }

    private static final String TAG = UrlContextMenu.class.getSimpleName();

    public static final String ARG_URL = "url";
    public static final String ARG_IS_IMAGE = "isImage";
    public static final String ARG_IS_GIF = "isGif";
    public static final String ARG_SUBHEADING_TEXT = "subheadingText";

    private String url;
    private boolean isImage;
    private boolean isGif;

    TextView titleText;

    TextView subheading = null;
    @Nullable
    private String subheadingText = null;

    /**
     * Get a context menu for a link.
     *
     * @param url        the link's URL
     * @param isImage    true if this link represents an image
     * @param isGif      true if this link represents a GIF
     * @param subheading an optional subheading - passing null hides the view, see {@link #setSubheading(String)}
     * @return the configured menu, ready to show
     */
    public static UrlContextMenu newInstance(@NonNull String url, boolean isImage, boolean isGif, @Nullable String subheading) {
        Bundle args = new Bundle();
        UrlContextMenu fragment = new UrlContextMenu();

        args.putString(ARG_URL, url);
        args.putBoolean(ARG_IS_IMAGE, isImage);
        args.putBoolean(ARG_IS_GIF, isGif);
        args.putString(ARG_SUBHEADING_TEXT, subheading);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    void init(@NonNull Bundle args) {
        url = args.getString(ARG_URL);
        isImage = args.getBoolean(ARG_IS_IMAGE);
        isGif = args.getBoolean(ARG_IS_GIF);
        subheadingText = args.getString(ARG_SUBHEADING_TEXT);
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        titleText = view.findViewById(R.id.actionTitle);
        subheading = view.findViewById(R.id.title_subheading);
        // tiny title text for long URLs - need to reapply the colour set in the xml
        titleText.setTextAppearance(getContext(), androidx.appcompat.R.style.TextAppearance_AppCompat_Small);
        titleText.setTextColor(ColorProvider.ACTION_BAR_TEXT.getColor());

        setSubheading(subheadingText);
        return view;
    }


    @NonNull
    @Override
    List<UrlMenuAction> generateMenuItems() {
        List<UrlMenuAction> awfulActions = new ArrayList<>();

        if (isImage) {
            awfulActions.add(DISPLAY_IMAGE);
            awfulActions.add(isGif ? PLAY_GIF : SHOW_INLINE);
            awfulActions.add(DOWNLOAD_IMAGE);
        }
        awfulActions.add(OPEN_URL);
        awfulActions.add(COPY_LINK_URL);
        awfulActions.add(SHARE_URL);

        return awfulActions;
    }


    @Override
    void onActionClicked(@NonNull UrlMenuAction action) {
        ThreadDisplayFragment parent = (ThreadDisplayFragment) getTargetFragment();
        if (parent == null) {
            Log.w(TAG, "onActionClicked: can't get target ThreadDisplayFragment");
            return;
        }
        switch (action) {
            case DOWNLOAD_IMAGE:
                parent.enqueueDownload(Uri.parse(url));
                break;
            case PLAY_GIF:
            case SHOW_INLINE:
                parent.showImageInline(url);
                break;
            case COPY_LINK_URL:
                parent.copyToClipboard(url);
                break;
            case OPEN_URL:
                parent.startUrlIntent(url);
                break;
            case SHARE_URL:
                startActivity(parent.createShareIntent(url));
                break;
            case DISPLAY_IMAGE:
                parent.displayImage(url);
                break;
        }
    }


    @NonNull
    @Override
    public String getTitle() {
        return url;
    }


    /**
     * Set the contents of the subheading, showing or hiding the view as necessary.
     * <p>
     * A null value for <b>text</b> hides the view entirely, but an empty string will show it.
     */
    public void setSubheading(@Nullable String text) {
        this.subheadingText = text;
        if (subheading == null) {
            // layout isn't inflated yet, just store the text for when it is
            return;
        }

        // null text means we're hiding the subheading
        if (subheadingText == null) {
            subheading.setText("");
            subheading.setVisibility(GONE);
            return;
        }

        // we have some text (empty string counts) so show the view
        if (subheading.getVisibility() != VISIBLE) {
            // basic set-and-show for hidden subheader and old Androids that can't animate properly
            subheading.setText(subheadingText);
            subheading.setVisibility(VISIBLE);
        } else {
            subheading.animate().alpha(0f).withEndAction(() -> {
                        subheading.setText(subheadingText);
                        subheading.animate().alpha(1f);
                    }
            );
        }
    }


    enum UrlMenuAction implements AwfulAction {
        DISPLAY_IMAGE(R.drawable.ic_photo_dark, "Display Image"),
        PLAY_GIF(R.drawable.ic_movie_dark, "Play .gif"),
        SHOW_INLINE(R.drawable.ic_area_close_dark, "Show Image inline"),
        DOWNLOAD_IMAGE(R.drawable.ic_file_download_dark, "Download Image"),
        OPEN_URL(R.drawable.ic_open_in_app_dark, "Open URL"),
        COPY_LINK_URL(R.drawable.ic_insert_link_dark, "Copy URL"),
        SHARE_URL(R.drawable.ic_share_dark, "Share URL");

        private final int iconId;
        @NonNull
        private final String menuText;

        UrlMenuAction(@DrawableRes int iconId, @NonNull String menuText) {
            this.iconId = iconId;
            this.menuText = menuText;
        }

        @Override
        @NonNull
        public String getMenuLabel() {
            return menuText;
        }

        @Override
        public int getIconId() {
            return iconId;
        }
    }
}
