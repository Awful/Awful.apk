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

package com.ferg.awfulapp.htmlwidget;

import android.support.annotation.NonNull;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A {@link FilterInputStream} that blocks until the requested number of bytes
 * have been read/skipped, or the end of the stream is reached.
 * <p>
 * This filter can be used as a work-around for <a
 * href="http://code.google.com/p/android/issues/detail?id=6066">Issue
 * #6066</a>.
 */
class BlockingFilterInputStream extends FilterInputStream {

    public BlockingFilterInputStream(InputStream input) {
        super(input);
    }

    @Override
    public int read(@NonNull byte[] buffer, int offset, int count) throws IOException {
        int total = 0;
        while (total < count) {
            int read = super.read(buffer, offset + total, count - total);
            if (read == -1) {
                return (total != 0) ? total : -1;
            }
            total += read;
        }
        return total;
    }

    @Override
    public int read(@NonNull byte[] buffer) throws IOException {
        int total = 0;
        while (total < buffer.length) {
            int offset = total;
            int count = buffer.length - total;
            int read = super.read(buffer, offset, count);
            if (read == -1) {
                return (total != 0) ? total : -1;
            }
            total += read;
        }
        return total;
    }

    @Override
    public long skip(long count) throws IOException {
        long total = 0L;
        while (total < count) {
            long skipped = super.skip(count - total);
            if (skipped == 0L) {
                int b = super.read();
                if (b < 0) {
                    break;
                } else {
                    skipped += 1;
                }
            }
            total += skipped;
        }
        return total;
    }
}
