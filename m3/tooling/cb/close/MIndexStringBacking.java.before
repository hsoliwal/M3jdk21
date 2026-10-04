/*
 * Copyright (c) 2026, Contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.internal.mindex;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * Transport-neutral JDK contract for immutable canonical MIndex String backing.
 *
 * <p>The identifier is a logical MIndex coordinate, never a process address. Implementations may
 * use mapped files, VM-managed shared storage, FFM, or native memory while preserving the same
 * java.lang.String semantics.</p>
 */
public interface MIndexStringBacking extends AutoCloseable {
    int length(long id);

    int utf8Length(long id);

    int codePointCount(long id);

    int unpairedSurrogateCount(long id);

    int hashCode(long id);

    char charAt(long id, int index);

    CharBuffer utf16View(long id);

    ByteBuffer utf8View(long id);

    String materialize(long id);

    default boolean contentEquals(long id, CharSequence other) {
        if (other == null || length(id) != other.length()) {
            return false;
        }
        for (int index = 0; index < other.length(); index++) {
            if (charAt(id, index) != other.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    default int compare(long leftId, long rightId) {
        int leftLength = length(leftId);
        int rightLength = length(rightId);
        int common = Math.min(leftLength, rightLength);
        for (int index = 0; index < common; index++) {
            int result = Character.compare(charAt(leftId, index), charAt(rightId, index));
            if (result != 0) {
                return result;
            }
        }
        return Integer.compare(leftLength, rightLength);
    }
}
