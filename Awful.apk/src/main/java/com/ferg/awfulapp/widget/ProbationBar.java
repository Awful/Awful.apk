package com.ferg.awfulapp.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.databinding.ProbationBarBinding;

import java.text.DateFormat;
import java.util.Date;


/**
 * Created by baka kaba on 25/05/2016.
 * <p/>
 * Probation bar widget, for dropping into UI that needs it.
 * <p/>
 * Set the probation time with {@link #setProbation(Long)} to show or hide the widget, and set
 * a click listener with {@link #setListener(Callbacks)} to handle user interaction.
 */
public class ProbationBar extends LinearLayout {

    ProbationBarBinding binding;

    @Nullable
    private Callbacks listener = null;

    public ProbationBar(Context context) {
        super(context);
        init();
    }

    public ProbationBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProbationBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ProbationBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        binding = ProbationBarBinding.inflate(LayoutInflater.from(getContext()));
        binding.goToLC.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onProbationButtonClicked();
                }
            }
        });
    }


    /**
     * Set the listener for user interaction callbacks, i.e. clicking the Leper Colony button
     *
     * @param listener An optional callback listener
     */
    public void setListener(@Nullable Callbacks listener) {
        this.listener = listener;
    }


    /**
     * Display the probation notice for the provided deadline, or hide the probation bar.
     *
     * @param probationTime the probation expiry timestamp, or null to hide the bar
     */
    public void setProbation(@Nullable Long probationTime) {
        if (probationTime == null) {
            this.setVisibility(View.GONE);
            return;
        }
        this.setVisibility(VISIBLE);
        String probeEnd = DateFormat.getDateTimeInstance().format(new Date(probationTime));
        binding.probationMessage.setText(String.format(binding.getRoot().getResources().getString(R.string.probation_message), probeEnd));
    }

    public interface Callbacks {
        /**
         * Called when the 'go to leper colony' button is clicked on the probation bar
         */
        void onProbationButtonClicked();
    }
}
