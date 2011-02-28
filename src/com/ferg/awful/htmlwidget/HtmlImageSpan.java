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

import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

/**
 * An {@link ImageSpan} that stores HTML {@code <img>} {@code alt} and {@code
 * title} attributes.
 */
public class HtmlImageSpan extends ImageSpan {
    private final String mTitle;

    private final String mAlt;

    public HtmlImageSpan(Drawable d, String source, String title, String alt) {
        super(d, source);
        mTitle = title;
        mAlt = alt;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getAlt() {
        return mAlt;
    }
}
