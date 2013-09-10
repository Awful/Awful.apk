/*-
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ferg.awful.htmlwidget;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;

/**
 * A {@link ClickableSpan} that launches an {@link Intent} with zero or more
 * fallback {@link Intent Intents}.
 */
class IntentsSpan extends ClickableSpan {

    private static final String TAG = "IntentsSpan";

    private final Intent[] mIntents;

    public IntentsSpan(Intent... intents) {
        if (intents == null) {
            throw new NullPointerException();
        }
        if (intents.length < 1) {
            throw new IllegalArgumentException();
        }
        mIntents = intents;
    }

    @Override
    public void onClick(View widget) {
        for (Intent intent : mIntents) {
            try {
                Context context = widget.getContext();
                context.startActivity(intent);
                return;
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "Activity not found", e);
                continue;
            }
        }
    }
}
