package com.ferg.awfulapp.announcements;

import android.os.Bundle;
import android.view.MenuItem;

import com.ferg.awfulapp.AwfulActivity;
import com.ferg.awfulapp.R;

import butterknife.ButterKnife;

/**
 * Created by baka kaba on 24/01/2017.
 * <p>
 * Basic activity that displays announcements.
 * Doesn't need to exist to be honest, someone make a reusable container activity!
 */

public class AnnouncementsActivity extends AwfulActivity {

    // TODO: 05/02/2017 this is a toolbar and a fragment, and the fragment is just a webview - refactor that layout into something reusable!

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.announcements_activity);
        ButterKnife.bind(this);
        setSupportActionBar(ButterKnife.findById(this, R.id.toolbar));
        setActionBar();
        setActionbarTitle(getString(R.string.announcements), null);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
