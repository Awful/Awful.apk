package com.ferg.awfulapp.popupmenu;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.provider.ColorProvider;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by baka kaba on 22/05/2017.
 * <p>
 * A menu dialog that displays a list of actions.
 * <p>
 * Subclass this and implement the methods that provide a list of actions, and handle when the user
 * clicks one of them. I recommend following the approach in {@link UrlContextMenu}, where you define
 * the menu items as an enum, add whichever items you need, and switch on the enum cases to handle
 * the user's selection. Just pass the enum as the type parameter for the class.
 */
public abstract class BasePopupMenu<T extends AwfulAction> extends DialogFragment {

    /**
     * Can be used to set a callback that is called when an action is clicked.
     */
    public interface OnActionClickedListener<T extends AwfulAction> {
        /**
         * Called when an action is clicked.
         * This method is called after {@link BasePopupMenu#onActionClicked} has been called.
         * @param action    the action that was clicked
         */
        void onActionClicked(T action);
    }

    int layoutResId = R.layout.select_url_action_dialog;

    private List<T> menuItems = null;

    private OnActionClickedListener<T> onActionClickedListener = null;

    public void setOnActionClickedListener(OnActionClickedListener<T> listener) {
        this.onActionClickedListener = listener;
    }

    BasePopupMenu() {
        this.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            init(getArguments());
        }
        menuItems = generateMenuItems();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View result = inflater.inflate(layoutResId, container, false);
        ButterKnife.bind(this, result);

        TextView actionTitle = result.findViewById(R.id.actionTitle);
        actionTitle.setMovementMethod(new ScrollingMovementMethod());
        actionTitle.setText(getTitle());

        RecyclerView actionsView = result.findViewById(R.id.post_actions);
        actionsView.setAdapter(new ActionHolderAdapter());
        actionsView.setLayoutManager(new LinearLayoutManager(getContext()));

        getDialog().setCanceledOnTouchOutside(true);
        return result;
    }


    /**
     * Called during onCreate, passing in the arguments set with {@link Fragment#setArguments(Bundle)}.
     * <p>
     * Fragments can be recreated, losing all their state, so set the arguments when creating a new
     * fragment instance, and unpack them and build your state here.
     */
    abstract void init(@NonNull Bundle args);


    /**
     * Generate the list of menu items to display.
     * <p>
     * This is where you should define the items you need to include, in the order they should be displayed.
     *
     * @return the list of menu items, in order
     */
    @NonNull
    abstract List<T> generateMenuItems();

    /**
     * The title to display on the dialog.
     */
    @NonNull
    abstract String getTitle();

    /**
     * Called when the user selects one of your menu items.
     *
     * The dialog is dismissed after this method is called - don't dismiss it yourself!
     */
    abstract void onActionClicked(@NonNull T action);

    /**
     * Get the text to display for a given action.
     * <p>
     * The default implementation just defers to the action's own {@link AwfulAction#getMenuLabel()} method.
     * You can override this if you need to manipulate the text, e.g. if you've defined a format string
     * for a particular item, and you need to pass in the specific parameters for this menu instance.
     *
     * @param action the action item being displayed
     */
    @NonNull
    String getMenuLabel(@NonNull T action) {
        return action.getMenuLabel();
    }


    class ActionHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.actionTag)
        ImageView actionTag;
        @BindView(R.id.actionTitle)
        TextView actionText;

        ActionHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    private class ActionHolderAdapter extends RecyclerView.Adapter<ActionHolder> {

        @NonNull
        @Override
        public ActionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.action_item, parent, false);
            return new ActionHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ActionHolder holder, final int position) {
            final T action = menuItems.get(position);
            holder.actionText.setText(getMenuLabel(action));
            holder.actionText.setTextColor(ColorProvider.PRIMARY_TEXT.getColor());
            holder.actionTag.setImageResource(action.getIconId());
            holder.itemView.setOnClickListener(v -> {
                onActionClicked(action);
                if (onActionClickedListener != null) {
                    onActionClickedListener.onActionClicked(action);
                }
                // Sometimes this happens after onSaveInstanceState is called, which throws an Exception if we don't allow state loss
                dismissAllowingStateLoss();
            });
        }

        @Override
        public int getItemCount() {
            return menuItems == null ? 0 : menuItems.size();
        }

    }


}
