package com.ferg.awfulapp.forums;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.bignerdranch.expandablerecyclerview.Adapter.ExpandableRecyclerAdapter;
import com.bignerdranch.expandablerecyclerview.Model.ParentListItem;
import com.bignerdranch.expandablerecyclerview.ViewHolder.ChildViewHolder;
import com.bignerdranch.expandablerecyclerview.ViewHolder.ParentViewHolder;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.databinding.ForumIndexItemBinding;
import com.ferg.awfulapp.databinding.ForumIndexSubforumItemBinding;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.ColorProvider;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.ferg.awfulapp.forums.Forum.SECTION;

/**
 * Created by baka kaba on 13/04/2016.
 * <p/>
 * A RecyclerView adapter for displaying expandable two-level lists of forums.
 */
public class ForumListAdapter extends ExpandableRecyclerAdapter<ForumListAdapter.TopLevelForumHolder, ForumListAdapter.SubforumHolder> {

    private final AwfulPreferences awfulPrefs;
    @NonNull
    private final EventListener eventListener;
    @NonNull
    private final LayoutInflater inflater;
    /**
     * interpolator for any animations a view holder wants to do
     */
    @NonNull
    private final Interpolator interpolator;


    private ForumListAdapter(@NonNull Context context,
                             @NonNull List<TopLevelForum> topLevelForums,
                             @NonNull EventListener listener,
                             @Nullable AwfulPreferences awfulPreferences) {
        super(topLevelForums);
        eventListener = listener;
        awfulPrefs = awfulPreferences;
        inflater = LayoutInflater.from(context);
        interpolator = new FastOutSlowInInterpolator();
    }

    /**
     * Returns a configured adapter.
     * <p/>
     * Takes a list of Forums which will form the main list.
     * Any of those which has items in {@link Forum#subforums} will be expandable,
     * and the subforums will be shown as an inner list. Any subforums of those items
     * will be ignored. Use {@link com.ferg.awfulapp.forums.ForumStructure.ListBuilder} etc.
     * to flatten the forums hierarchy into two levels.
     *
     * @param context          Used for layout inflation
     * @param forums           A list of Forums to display
     * @param listener         Gets callbacks for clicks etc
     * @param awfulPreferences used to check for user options
     * @return an adapter containing the provided forums
     */
    public static ForumListAdapter getInstance(@NonNull Context context,
                                               @NonNull List<Forum> forums,
                                               @NonNull EventListener listener,
                                               @Nullable AwfulPreferences awfulPreferences) {
        List<TopLevelForum> topLevelForums = new ArrayList<>();
        ForumListAdapter adapter = new ForumListAdapter(context, topLevelForums, listener, awfulPreferences);
        // this is a stupid hack so we can supply the constructor with a list of objects we
        // can't even create without an instance... it's better than pulling TopLevelForum out
        // into a separate file at least
        adapter.addToTopLevelForums(forums, topLevelForums);
        adapter.notifyParentItemRangeInserted(0, topLevelForums.size());
        return adapter;
    }

    /**
     * Create TopLevelForums from a list of Forums, adding them to a supplied list.
     *
     * @param forums         The forums to add
     * @param topLevelForums The list to add to
     */
    private void addToTopLevelForums(@NonNull List<Forum> forums,
                                     @NonNull List<TopLevelForum> topLevelForums) {
        for (Forum forum : forums) {
            topLevelForums.add(new TopLevelForum(forum));
        }
    }

    /**
     * Update the contents of the data set with a new list of forums.
     *
     * @param forums The new list to display
     *               (see {@link #getInstance(Context, List, EventListener, AwfulPreferences)} for the list format)
     */
    public void updateForumList(@NonNull List<Forum> forums) {
        @SuppressWarnings("unchecked")
        List<TopLevelForum> itemList = (List<TopLevelForum>) getParentItemList();

        // we can't just reassign the dataset variable, we have to mess with the contents instead
        int oldSize = itemList.size();
        if (oldSize > 0) {
            notifyParentItemRangeRemoved(0, oldSize);
        }
        itemList.clear();
        addToTopLevelForums(forums, itemList);
        int newSize = forums.size();
        if (newSize > 0) {
            notifyParentItemRangeInserted(0, newSize);
        }
    }

    private void setText(@NonNull Forum forum,
                         @NonNull TextView title,
                         @NonNull TextView subtitle,
                         @Nullable TextView sectionTitle) {
        title.setText(forum.title);
        subtitle.setText(forum.subtitle);
        if (sectionTitle != null) {
            sectionTitle.setText(forum.title);
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // List items!
    ///////////////////////////////////////////////////////////////////////////

    private void handleSubtitles(@NonNull Forum forum, @NonNull TextView subtitleView) {
        // we remove the subtitle if it's not there (or it's disabled) so that the title gets vertically centred
        boolean subtitlesEnabled = false;
        if (awfulPrefs != null) {
            subtitlesEnabled = awfulPrefs.forumIndexShowSubtitles;
        }
        subtitleView.setVisibility(!forum.subtitle.isEmpty() && subtitlesEnabled ? VISIBLE : GONE);
    }

    /**
     * Rotate the dropdown button to the up or down position.
     *
     * @param dropdown  The view to rotate
     * @param down      True to rotate to the down state (default rotation)
     * @param immediate Set rotation immediately, false will animate
     */
    private void rotateDropdown(@NonNull ImageView dropdown, boolean down, boolean immediate) {
        final int DOWN_ROTATION = 0;
        final int UP_ROTATION = -540;
        dropdown.animate()
                .setDuration(immediate ? 0 : 400)
                .rotation(down ? DOWN_ROTATION : UP_ROTATION)
                .setInterpolator(interpolator);
    }

    /**
     * Apply colour theming
     *
     * @param mainView The main item layout, has its background set
     */
    private void setThemeColours(View mainView, TextView title, TextView subtitle) {
        mainView.setBackgroundColor(ColorProvider.BACKGROUND.getColor());
        title.setTextColor(ColorProvider.PRIMARY_TEXT.getColor());
        subtitle.setTextColor(ColorProvider.ALT_TEXT.getColor());
    }

    @Override
    public TopLevelForumHolder onCreateParentViewHolder(ViewGroup parentViewGroup) {
        View view = inflater.inflate(R.layout.forum_index_item, parentViewGroup, false);
        return new TopLevelForumHolder(view);
    }

    @Override
    public SubforumHolder onCreateChildViewHolder(ViewGroup childViewGroup) {
        View view = inflater.inflate(R.layout.forum_index_subforum_item, childViewGroup, false);
        return new SubforumHolder(view);
    }

    @Override
    public void onBindParentViewHolder(TopLevelForumHolder parentViewHolder, int position, ParentListItem parentListItem) {
        parentViewHolder.bind((TopLevelForum) parentListItem);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Internal adapter wiring
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onBindChildViewHolder(SubforumHolder childViewHolder, int position, Object childListItem) {
        childViewHolder.bind((Forum) childListItem);
    }


    public interface EventListener {
        void onForumClicked(@NonNull Forum forum);

        void onContextMenuCreated(@NonNull Forum forum, @NonNull Menu contextMenu);
    }

    private static class TopLevelForum implements ParentListItem {

        final Forum forum;


        TopLevelForum(Forum forum) {
            this.forum = forum;
        }


        @Override
        public List<?> getChildItemList() {
            return forum.subforums;
        }


        @Override
        public boolean isInitiallyExpanded() {
            return false;
        }
    }

    class TopLevelForumHolder extends ParentViewHolder {

        // list item sections - overall view, left column (tags etc), right column (details)
        private final View itemView;
        private ForumIndexItemBinding binding;


        private Forum forum;
        private boolean hasSubforums;


        TopLevelForumHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            binding = ForumIndexItemBinding.bind(itemView);

            binding.forumDetails.setOnCreateContextMenuListener((contextMenu, view, contextMenuInfo) -> eventListener.onContextMenuCreated(forum, contextMenu));
        }


        void bind(final TopLevelForum forumItem) {
            forum = forumItem.forum;
            hasSubforums = !forumItem.getChildItemList().isEmpty();

            /* section items hide everything but the section title,
               other forum types hide the section title and show the other components.
               Think of of them as two alternative layouts in the same Layout file */
            binding.tagAndDropdownArrow.setVisibility(forum.isType(SECTION) ? GONE : VISIBLE);
            binding.forumDetails.setVisibility(forum.isType(SECTION) ? GONE : VISIBLE);
            binding.sectionTitle.setVisibility(forum.isType(SECTION) ? VISIBLE : GONE);

            // hide the list divider for section titles and expanded parent forums
            boolean hideDivider = forum.isType(SECTION) || forumItem.isInitiallyExpanded();
            binding.listDivider.setVisibility(hideDivider ? INVISIBLE : VISIBLE);

            // sectionTitle is basically a differently formatted version of the title
            setText(forum, binding.forumTitle, binding.forumSubtitle, binding.sectionTitle);
            setThemeColours(itemView, binding.forumTitle, binding.forumSubtitle);
            handleSubtitles(forum, binding.forumSubtitle);

            binding.forumFavouriteMarker.setVisibility(forum.isFavourite() ? VISIBLE : GONE);

            /* the left section (potentially) has a tag and a dropdown button, anything missing
               is set to GONE so whatever's there gets vertically centred, and the space remains */

            // if there's a forum tag then display it, otherwise remove it
            boolean hasForumTag = forum.getTagUrl() != null;
            if (hasForumTag) {
                TagProvider.setSquareForumTag(binding.forumTag, forum);
                binding.forumTag.setVisibility(View.VISIBLE);
            } else {
                binding.forumTag.setVisibility(View.GONE);
            }

            // if this item has subforums, show the dropdown and make it work, otherwise remove it
            if (hasSubforums) {
                rotateDropdown(binding.subforumsExpandArrow, !isExpanded(), true);
                binding.subforumsExpandArrow.setVisibility(VISIBLE);
            } else {
                binding.subforumsExpandArrow.setVisibility(GONE);
            }
            binding.tagAndDropdownArrow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (hasSubforums) {
                        if (isExpanded()) {
                            collapseView();
                        } else {
                            expandView();
                        }
                    }
                }
            });

            binding.forumDetails.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    eventListener.onForumClicked(forum);
                }
            });
        }


        @Override
        public boolean shouldItemViewClickToggleExpansion() {
            return false;
        }


        @Override
        public void onExpansionToggled(boolean closing) {
            super.onExpansionToggled(closing);
            rotateDropdown(binding.subforumsExpandArrow, closing, false);
            binding.listDivider.setVisibility(closing ? VISIBLE : INVISIBLE);
        }
    }

    class SubforumHolder extends ChildViewHolder {

        Forum forum;
        ForumIndexSubforumItemBinding binding;



        SubforumHolder(View itemView) {
            super(itemView);
            binding = ForumIndexSubforumItemBinding.bind(itemView);
            binding.forumDetails.setOnCreateContextMenuListener((contextMenu, view, contextMenuInfo) -> eventListener.onContextMenuCreated(forum, contextMenu));
        }


        void bind(final Forum forumItem) {
            forum = forumItem;
            setText(forum, binding.forumTitle, binding.forumSubtitle, null);
            setThemeColours(binding.getRoot(), binding.forumTitle, binding.forumSubtitle);
            handleSubtitles(forum, binding.forumSubtitle);
            binding.forumFavouriteMarker.setVisibility(forum.isFavourite() ? VISIBLE : GONE);
            binding.forumDetails.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    eventListener.onForumClicked(forum);
                }
            });
        }
    }

}
