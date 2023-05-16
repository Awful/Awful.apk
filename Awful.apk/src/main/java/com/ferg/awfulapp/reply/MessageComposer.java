package com.ferg.awfulapp.reply;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import androidx.fragment.app.Fragment;
import androidx.appcompat.view.SupportMenuInflater;
import androidx.appcompat.view.menu.MenuBuilder;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.ferg.awfulapp.EmotePicker;
import com.ferg.awfulapp.EmotePickerListener;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.reply.BasicTextInserter.BbCodeTag;
import com.ferg.awfulapp.util.AwfulUtils;
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder;
import com.github.rubensousa.bottomsheetbuilder.BottomSheetMenuDialog;

import static com.ferg.awfulapp.R.id.bbcode_bold;
import static com.ferg.awfulapp.R.id.bbcode_spoiler;
import static com.ferg.awfulapp.R.id.bbcode_underline;
import static com.ferg.awfulapp.R.id.emotes;

/**
 * Created by baka kaba on 07/11/2016.
 * <p>
 * A fragment holding an EditText, with functionality specific to composing messages and posts.
 * <p>
 * This is basically meant as a drop-in composer window, handling BBcode and smiley insertion
 * and exposing some methods to get and set the current contents. It has its own menu options,
 * so you need to make sure {@link #onCreateOptionsMenu(Menu, MenuInflater)} and
 * {@link #onOptionsItemSelected(MenuItem)} are called when appropriate.
 */

public class MessageComposer extends Fragment implements EmotePickerListener {

    private EditText messageBox;
    private BottomSheetMenuDialog bottomSheetMenuDialog;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.message_composer, container, true);
        messageBox = result.findViewById(R.id.message_edit_text);
        addBbCodeToSelectionMenu(messageBox);
        return result;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bottomSheetMenuDialog != null) {
            bottomSheetMenuDialog.dismiss();
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Menu handling
    ///////////////////////////////////////////////////////////////////////////


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.message_composer, menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // open formatting/insert menu
            case R.id.show_bbcode_menu:
                toggleBottomSheet();
                break;

            // formatting menu (stuff you mainly select and format)
            case bbcode_bold:
                insertWith(BbCodeTag.BOLD);
                break;
            case R.id.bbcode_italics:
                insertWith(BbCodeTag.ITALICS);
                break;
            case bbcode_underline:
                insertWith(BbCodeTag.UNDERLINE);
                break;
            case R.id.bbcode_strikeout:
                insertWith(BbCodeTag.STRIKEOUT);
                break;
            case bbcode_spoiler:
                insertWith(BbCodeTag.SPOILER);
                break;
            case R.id.bbcode_superscript:
                insertWith(BbCodeTag.SUPERSCRIPT);
                break;
            case R.id.bbcode_subscript:
                insertWith(BbCodeTag.SUBSCRIPT);
                break;
            case R.id.bbcode_fixed_width:
                insertWith(BbCodeTag.FIXED);
                break;

            // insert menu (emotes, images, block formatting, parameterised tags etc)
            case emotes:
                new EmotePicker().show(getChildFragmentManager(), "emotes");
                break;
            case R.id.bbcode_image:
                insertWith(ImageInserter::smartInsert);
                break;
            // removed imgur inserter due to change in Imgur TOS
            case R.id.bbcode_video:
                insertWith(VideoInserter::smartInsert);
                break;
            case R.id.bbcode_url:
                insertWith(UrlInserter::smartInsert);
                break;
            case R.id.bbcode_quote:
                insertWith(QuoteInserter::smartInsert);
                break;
            case R.id.bbcode_list:
                insertWith(ListInserter::smartInsert);
                break;
            case R.id.bbcode_code:
                insertWith(CodeInserter::smartInsert);
                break;
            case R.id.bbcode_pre:
                insertWith(BbCodeTag.PRE);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void insertWith(Inserter.Untagged inserter) {
        inserter.smartInsert(messageBox, getActivity());
    }

    private void insertWith(BbCodeTag bbCodeTag) {
        BasicTextInserter.smartInsert(messageBox, bbCodeTag, getActivity());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Callbacks
    ///////////////////////////////////////////////////////////////////////////


    public void onImageUploaded(@NonNull String url, boolean useThumbnail) {
        ImageInserter.insertWithoutDialog(messageBox, url, useThumbnail);
    }


    public void onHtml5VideoUploaded(@NonNull String url) {
        // embedding doesn't use [video] tags for some reason, needs to be a url with no link text
        UrlInserter.insertWithoutDialog(messageBox, url, null);
    }




    ///////////////////////////////////////////////////////////////////////////
    // Useful public functions
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Set the contents of the composer's EditText.
     *
     * @param messageText The text to set (empty if null)
     * @param selectAll   Select the contents, e.g. for easy deletion by the user
     */
    public void setText(@Nullable String messageText, boolean selectAll) {
        messageBox.setText(messageText == null ? "" : messageText);
        if (selectAll) {
            messageBox.setSelection(messageBox.length());
        }
    }


    /**
     * Get the text contents of the composer's EditText.
     */
    @NonNull
    public String getText() {
        return messageBox.getText().toString();
    }


    /**
     * Set the background colour of the EditText.
     */
    public void setBackgroundColor(@ColorInt int color) {
        messageBox.setBackgroundColor(color);
    }

    /**
     * Set the text colour of the EditText.
     */
    public void setTextColor(@ColorInt int color) {
        messageBox.setTextColor(color);
    }

    /**
     * Hide the keyboard, if currently active in the composer.
     */
    public void hideKeyboard() {
        if (getActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(messageBox.getApplicationWindowToken(), 0);
        }
    }


    /**
     * Insert a smiley
     *
     * @param emoteCode the smiley code to insert at the current selection point
     */
    public void onEmoteChosen(@NonNull String emoteCode) {
        int selectionStart = messageBox.getSelectionStart();
        messageBox.getEditableText().insert(selectionStart, emoteCode);
        messageBox.setSelection(selectionStart + emoteCode.length());
    }


    ///////////////////////////////////////////////////////////////////////////
    // Internal handling
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Adds the BBcode option to the text selection action menu.
     * <p>
     * This is mainly to fix the issue with the action menu replacing the action bar on earlier
     * versions of Android, meaning the BBcode option can't be pressed (and work on selected text).
     *
     * @param editText the textview to add the selection option to
     */
    private void addBbCodeToSelectionMenu(@NonNull final EditText editText) {
        ActionMode.Callback callback = new ActionMode.Callback() {

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                menu.add(Menu.NONE, R.id.show_bbcode_menu, Menu.NONE, getString(R.string.bbcode))
                        .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                        .setIcon(R.drawable.ic_bb);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == R.id.show_bbcode_menu) {
                    toggleBottomSheet();
                    return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {

            }
        };

        editText.setCustomSelectionActionModeCallback(callback);
        // add it to the insert menu too for consistency, why not
        if (AwfulUtils.isMarshmallow23()) {
            // noinspection AndroidLintNewApi
            editText.setCustomInsertionActionModeCallback(callback);
        }
    }


    /**
     * Display or hide the bottom sheet as appropriate.
     */
    private void toggleBottomSheet() {
        // if we already have a sheet, get rid of it
        if (bottomSheetMenuDialog != null) {
            bottomSheetMenuDialog.dismissWithAnimation();
            bottomSheetMenuDialog = null;
            return;
        }

        // Stupid hack to ensure the text selected when the options are shown is still selected
        // when an option is chosen. This is all because older versions use a Contextual Action Bar
        // for text selection, which a) deselects the text when you pick an option from it,
        // and b) covers the action bar so you can't use the menu item there that works fine
        final int[] selectionRange = !messageBox.hasSelection() ? null :
                new int[]{messageBox.getSelectionStart(), messageBox.getSelectionEnd()};

        // build a full menu to populate the sheet with
        Activity activity = getActivity();
        Menu sheetMenu = new MenuBuilder(activity);
        SupportMenuInflater inflater = new SupportMenuInflater(activity);
        inflater.inflate(R.menu.insert_into_message, sheetMenu);
        inflater.inflate(R.menu.format_message, sheetMenu);

        // need to apply themed background and text colours programmatically it seems
        TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]{
                R.attr.bottomSheetBackgroundColor,
                R.attr.bottomSheetItemTextColor});
        int backgroundColour = a.getResourceId(0, 0);
        int itemTextColour = a.getResourceId(1, 0);
        a.recycle();

        bottomSheetMenuDialog = new BottomSheetBuilder(activity)
                .setBackgroundColorResource(backgroundColour)
                .setItemTextColorResource(itemTextColour)
                .setMode(BottomSheetBuilder.MODE_GRID)
                .setMenu(sheetMenu)
                .setItemClickListener(item -> {
                    // restore any selection in the EditText before invoking the format/insert options
                    if (selectionRange != null) {
                        messageBox.setSelection(selectionRange[0], selectionRange[1]);
                    }
                    onOptionsItemSelected(item);
                })
                .createDialog();

        // drop the reference to an existing sheet when it goes away
        bottomSheetMenuDialog.setOnCancelListener(dialog -> bottomSheetMenuDialog = null);
        bottomSheetMenuDialog.setOnDismissListener(dialog -> bottomSheetMenuDialog = null);

        bottomSheetMenuDialog.show();
        // force the dialog to expand since peek/collapsed has some measurement issue in landscape
        bottomSheetMenuDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}
