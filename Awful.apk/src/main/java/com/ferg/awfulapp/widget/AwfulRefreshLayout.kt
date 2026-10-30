package com.ferg.awfulapp.widget

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.ferg.awfulapp.R
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.ColorProvider
import kotlin.math.abs
import androidx.core.view.isNotEmpty

/**
 * Replacement for SwipyRefreshLayout (com.github.orangegangsters:swipy)
 * This accepts a child view and will track overscroll on that. If the user has overscrolled to a
 * threshold calculated from a preference, it will trigger a callback onRefresh (top) or
 * onLoadMore(bottom). These can be set in the fragments or views that contain this layout.
 * If the callbacks are not set, the overscroll is ignored and the spinner is not shown.
 * finishedLoading() needs to be called in the callbacks stack somewhere to let the layout know to
 * stop spinning that frog.
 */

enum class OverscrollEdge {
    TOP,
    BOTTOM
}

class AwfulRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private companion object {
        private const val TAG = "AwfulRefreshLayout"
        const val SPINNER_SIZE_DP = 40f
        const val SPINNER_OFFSET_DP = 80f
        const val SPINNER_TRAVEL_DP = 120f
        const val SPINNER_FULL_ROTATION = 720f
        const val DRAG_RESISTANCE = 0.5f
        const val LOADING_ROTATION_DURATION_MS = 700L
        const val GESTURE_HIDE_DURATION_MS = 100L
        const val HIDE_DURATION_MS = 150L
    }

    // overwrite either of these to enable p2r or p2n
    var onRefresh: (() -> Unit)? = null
    var onLoadMore: (() -> Unit)? = null

    var isLoading: Boolean = false
        private set

    var spinnerDrawable: Int = R.drawable.frog_silhouette_spinner
        set(value) {
            field = value
            spinner.setImageDrawable(ContextCompat.getDrawable(context, value))
        }

    var spinnerTint: Int = ColorProvider.getARLActiveColor()
        set(value) {
            field = value
            applySpinnerTint(value)
        }

    var spinnerTintInert: Int = ColorProvider.getARLInertColor()
        set(value) {
            field = value
            if (activeEdge != null) {
                updateSpinner()
            }
        }

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val spinnerSize: Int
        get() = dp(SPINNER_SIZE_DP).toInt()

    private val spinnerOffset: Float
        get() = dp(SPINNER_OFFSET_DP)

    private val spinnerTravel: Float
        get() = dp(SPINNER_TRAVEL_DP)

    private val spinner = ImageView(context).apply {
        setImageResource(spinnerDrawable)
        scaleType = ImageView.ScaleType.FIT_CENTER
        alpha = 0f
        visibility = INVISIBLE
    }

    private var contentView: View? = null

    private var downY = 0f
    private var lastY = 0f

    private var activeEdge: OverscrollEdge? = null
    private var overscrollDistance = 0f

    private var loadingAnimator: ObjectAnimator? = null

    private val threshold: Float
        get() {
            return with(context.resources.displayMetrics) {
                heightPixels / density
            } * (AwfulPreferences.getInstance().p2rDistance ?: 0.5f)
        }


    override fun onFinishInflate() {
        super.onFinishInflate()

        if (isNotEmpty()) {
            contentView = getChildAt(0)
        }

        addView(
            spinner,
            LayoutParams(
                spinnerSize,
                spinnerSize,
                Gravity.CENTER_HORIZONTAL or Gravity.TOP
            )
        )

        applySpinnerTint(spinnerTint)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downY = event.y
                lastY = event.y
                activeEdge = null
                overscrollDistance = 0f
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                if (isLoading || activeEdge != null) {
                    return activeEdge != null
                }

                val totalDownY = event.y - downY

                if (abs(totalDownY) < touchSlop) {
                    return false
                }

                val edge = getOverscrollEdge(totalDownY) ?: return false

                beginGesture(edge, event.y)
                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                resetGesture()
            }
        }

        return activeEdge != null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val edge = activeEdge ?: return false

        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                updateOverscroll(event.y - lastY)
                updateSpinner()
                lastY = event.y
                return true
            }

            MotionEvent.ACTION_UP -> {
                finishGesture(edge)
                performClick()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                resetGesture()
                return true
            }
        }

        return true
    }

    // Fine I'll overwrite it, shut up shut up shut up
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun canChildScrollUp(): Boolean {
        return contentView?.canScrollVertically(-1) == true
    }

    private fun canChildScrollDown(): Boolean {
        return contentView?.canScrollVertically(1) == true
    }

    private fun getOverscrollEdge(dy: Float): OverscrollEdge? {
        return when {
            dy > 0f && onRefresh != null && !canChildScrollUp() -> OverscrollEdge.TOP
            dy < 0f && onLoadMore != null && !canChildScrollDown() -> OverscrollEdge.BOTTOM
            else -> null
        }
    }

    private fun beginGesture(edge: OverscrollEdge, y: Float) {
        activeEdge = edge
        overscrollDistance = 0f
        lastY = y
    }

    private fun finishGesture(edge: OverscrollEdge) {
        val listener = when (edge) {
            OverscrollEdge.TOP -> onRefresh
            OverscrollEdge.BOTTOM -> onLoadMore
        }

        if (overscrollDistance >= threshold && !isLoading && listener != null) {
            isLoading = true
            showLoadingSpinner()
            listener.invoke()
        }

        resetGesture()
    }

    private fun updateOverscroll(dy: Float) {
        val edge = activeEdge ?: return

        val direction = when (edge) {
            OverscrollEdge.TOP -> 1f
            OverscrollEdge.BOTTOM -> -1f
        }

        overscrollDistance = (overscrollDistance + (dy * direction * DRAG_RESISTANCE)).coerceAtLeast(0f)
    }

    private fun updateSpinner() {
        val edge = activeEdge ?: return

        val progress = (overscrollDistance / threshold).coerceIn(0f, 1f)
        val isActive = progress >= 1f

        spinner.visibility = VISIBLE
        // only start showing the spinner half-way in
        spinner.alpha = ((progress - 0.5f) * 2f).coerceIn(0f, 1f)
        spinner.rotation = progress * SPINNER_FULL_ROTATION

        applySpinnerTint(
            if (isActive) {
                spinnerTint
            } else {
                spinnerTintInert
            }
        )

        val distance = spinnerTravel * progress

        spinner.translationY = when (edge) {
            OverscrollEdge.TOP -> -spinnerOffset + distance
            OverscrollEdge.BOTTOM -> height - spinnerSize + spinnerOffset - distance
        }
    }

    fun finishedLoading() {
        isLoading = false
        hideLoadingSpinner()
    }

    private fun showLoadingSpinner() {
        spinner.animate().cancel()
        spinner.visibility = VISIBLE
        spinner.alpha = 1f
        startLoadingRotation()
    }

    private fun hideLoadingSpinner() {
        stopLoadingRotation()
        spinner.animate().cancel()
        spinner.animate()
            .alpha(0f)
            .setDuration(HIDE_DURATION_MS)
            .withEndAction {
                spinner.visibility = INVISIBLE
            }
            .start()
    }

    private fun startLoadingRotation() {
        loadingAnimator?.cancel()
        loadingAnimator = ObjectAnimator.ofFloat(
            spinner,
            ROTATION,
            spinner.rotation,
            spinner.rotation + 360f
        ).apply {
            duration = LOADING_ROTATION_DURATION_MS
            interpolator = LinearInterpolator()
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            start()
        }
    }

    private fun stopLoadingRotation() {
        loadingAnimator?.cancel()
        loadingAnimator = null
    }

    private fun applySpinnerTint(colorRes: Int) {
        ImageViewCompat.setImageTintList(
            spinner,
            ContextCompat.getColorStateList(context, colorRes)
        )
    }

    private fun resetGesture() {
        val edge = activeEdge
        activeEdge = null
        overscrollDistance = 0f
        downY = 0f
        lastY = 0f

        if (!isLoading && edge != null) {
            spinner
                .animate()
                .cancel()
            spinner
                .animate()
                .alpha(0f)
                .setDuration(GESTURE_HIDE_DURATION_MS)
                .withEndAction { spinner.visibility = INVISIBLE }
                .start()
        }
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }

    override fun onDetachedFromWindow() {
        stopLoadingRotation()
        spinner.animate().cancel()
        super.onDetachedFromWindow()
    }
}
