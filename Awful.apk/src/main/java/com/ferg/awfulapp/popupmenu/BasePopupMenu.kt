package com.ferg.awfulapp.popupmenu

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ferg.awfulapp.AwfulDialogFragment
import com.ferg.awfulapp.R
import com.ferg.awfulapp.databinding.ActionItemBinding
import com.ferg.awfulapp.provider.ColorProvider

/**
 * Created by baka kaba on 22/05/2017.
 * 
 * 
 * A menu dialog that displays a list of actions.
 * 
 * 
 * Subclass this and implement the methods that provide a list of actions, and handle when the user
 * clicks one of them. I recommend following the approach in [UrlContextMenu], where you define
 * the menu items as an enum, add whichever items you need, and switch on the enum cases to handle
 * the user's selection. Just pass the enum as the type parameter for the class.
 */
abstract class BasePopupMenu<T : AwfulAction?> internal constructor() : AwfulDialogFragment() {
    /**
     * Can be used to set a callback that is called when an action is clicked.
     */
    interface OnActionClickedListener<T : AwfulAction?> {
        /**
         * Called when an action is clicked.
         * This method is called after [BasePopupMenu.onActionClicked] has been called.
         * @param action    the action that was clicked
         */
        fun onActionClicked(action: T?)
    }

    @JvmField
    var layoutResId: Int = R.layout.select_url_action_dialog

    private var menuItems: MutableList<T?>? = null

    private var onActionClickedListener: OnActionClickedListener<T?>? = null

    fun setOnActionClickedListener(listener: OnActionClickedListener<T?>?) {
        this.onActionClickedListener = listener
    }

    init {
        this.setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            init(requireArguments())
        }
        menuItems = generateMenuItems()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val result = inflater.inflate(layoutResId, container, false)

        val actionTitle = result.findViewById<TextView>(R.id.actionTitle)
        actionTitle.movementMethod = ScrollingMovementMethod()
        actionTitle.text = getTitle()

        val actionsView = result.findViewById<RecyclerView>(R.id.post_actions)
        actionsView.setAdapter(ActionHolderAdapter())
        actionsView.setLayoutManager(LinearLayoutManager(context))

        dialog?.setCanceledOnTouchOutside(true)
        awfulActivity.setPreferredFont(result)
        return result
    }


    /**
     * Called during onCreate, passing in the arguments set with [Fragment.setArguments].
     * 
     * 
     * Fragments can be recreated, losing all their state, so set the arguments when creating a new
     * fragment instance, and unpack them and build your state here.
     */
    abstract fun init(args: Bundle)


    /**
     * Generate the list of menu items to display.
     * 
     * 
     * This is where you should define the items you need to include, in the order they should be displayed.
     * 
     * @return the list of menu items, in order
     */
    abstract fun generateMenuItems(): MutableList<T?>

    /**
     * Called when the user selects one of your menu items.
     * 
     * The dialog is dismissed after this method is called - don't dismiss it yourself!
     */
    abstract fun onActionClicked(action: T)

    /**
     * Get the text to display for a given action.
     * 
     * 
     * The default implementation just defers to the action's own [AwfulAction.getMenuLabel] method.
     * You can override this if you need to manipulate the text, e.g. if you've defined a format string
     * for a particular item, and you need to pass in the specific parameters for this menu instance.
     * 
     * @param action the action item being displayed
     */
    open fun getMenuLabel(action: T): String {
        return action?.menuLabel ?: ""
    }


    internal class ActionHolder(view: View) : RecyclerView.ViewHolder(view) {
        var binding: ActionItemBinding = ActionItemBinding.bind(view)
    }

    private inner class ActionHolderAdapter : RecyclerView.Adapter<ActionHolder?>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.action_item, parent, false)
            awfulActivity.setPreferredFont(view)
            return ActionHolder(view)
        }

        override fun onBindViewHolder(holder: ActionHolder, position: Int) {
            val action = menuItems!![position]
            action?.let {
                holder.binding.actionTitle.text = getMenuLabel(action)
                holder.binding.actionTitle.setTextColor(ColorProvider.PRIMARY_TEXT.color)
                holder.binding.actionTag.setImageResource(action.iconId)
                holder.itemView.setOnClickListener { _: View? ->
                    onActionClicked(action)
                    onActionClickedListener?.onActionClicked(action)
                    // Sometimes this happens after onSaveInstanceState is called, which throws an Exception if we don't allow state loss
                    dismissAllowingStateLoss()
                }
            }
        }

        override fun getItemCount(): Int {
            return menuItems?.size ?: 0
        }
    }
}
