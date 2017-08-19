package com.ferg.awfulapp.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ferg.awfulapp.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by baka kaba on 14/08/2017.
 * <p>
 * A widget that displays a status message and an optional activity spinner, with a frog icon in the
 * background.
 * <p>
 * This is meant for use on blank areas where you need to explain there's no content, the user needs
 * to do something (like selecting an item in a master-detail layout), or show the current status and
 * activity (e.g. content is being fetched).
 */
public class StatusFrog extends RelativeLayout {

    @BindView(R.id.status_message)
    TextView statusMessage;
    @BindView(R.id.status_progress_bar)
    ProgressBar progressBar;

    public StatusFrog(Context context) {
        super(context);
        init(context, null);
    }

    public StatusFrog(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public StatusFrog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public StatusFrog(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }


    private void init(Context context, @Nullable AttributeSet attrs) {
        View view = LayoutInflater.from(context).inflate(R.layout.status_frog, this, true);
        ButterKnife.bind(this, view);
        // handle any custom XML attributes
        if (attrs != null) {
            TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.StatusFrog, 0, 0);
            setStatusText(typedArray.getString(R.styleable.StatusFrog_status_message));
            showSpinner(typedArray.getBoolean(R.styleable.StatusFrog_show_spinner, false));
            typedArray.recycle();
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Update methods
    ///////////////////////////////////////////////////////////////////////////


    public StatusFrog setStatusText(@Nullable String text) {
        statusMessage.setText((text == null) ? "" : text);
        return this;
    }

    public StatusFrog setStatusText(@StringRes int resId) {
        return setStatusText(getContext().getString(resId));
    }

    /**
     * Display or hide the activity spinner.
     */
    public StatusFrog showSpinner(boolean show) {
        progressBar.setVisibility(show ? VISIBLE : INVISIBLE);
        return this;
    }
}
