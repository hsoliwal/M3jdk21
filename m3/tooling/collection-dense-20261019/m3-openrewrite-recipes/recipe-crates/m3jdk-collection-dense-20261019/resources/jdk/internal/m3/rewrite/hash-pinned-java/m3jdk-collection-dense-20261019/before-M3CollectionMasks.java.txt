/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. This file is subject to the
 * "Classpath" exception as provided in the LICENSE file that accompanied
 * this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package java.util;

/**
 * Physical index extraction for collection-owned selection masks.
 *
 * <p>A mask word describes 64 physical positions, least-significant bit first.
 * The caller must keep the mask unchanged during a call. The returned array
 * is detached, ascending, and contains precisely the set positions in the
 * requested half-open range. This helper neither retains an input nor owns
 * collection state, predicates, comparators, or a persistent selection cache.
 *
 * <p>The Java path remains the production default. The internal diagnostic
 * startup property {@code jdk.m3.collectionMasks.native=true} can request the
 * target libjava path; an unavailable symbol leaves this consumer on Java.
 * Explicit native qualification methods still fail when that symbol is absent.
 * This helper never loads an external library.
 */
final class M3CollectionMasks {
    private M3CollectionMasks() {
    }

    static int[] selected(long[] words, int fromInclusive, int toExclusive) {
        if (NativeSelection.ENABLED && !NativeSelection.unavailable) {
            try {
                return selectedNative(words, fromInclusive, toExclusive);
            } catch (UnsatisfiedLinkError absentTargetSymbol) {
                NativeSelection.unavailable = true;
            }
        }
        return selectedJava(words, fromInclusive, toExclusive);
    }

    private static final class NativeSelection {
        // Saved startup configuration avoids new SecurityManager property
        // permissions in an existing collection operation. Initialization is
        // deferred until a collection actually requests index extraction.
        static final boolean ENABLED = Boolean.parseBoolean(
                jdk.internal.misc.VM.getSavedProperty("jdk.m3.collectionMasks.native"));
        static volatile boolean unavailable;
    }

    /**
     * Two word scans, one exact-sized result, and no per-position objects.
     * Any detected count drift between scans fails instead of returning an
     * incompletely filled result. Such detection is best effort: it does not
     * turn an unsynchronized mutable mask into a snapshot.
     */
    static int[] selectedJava(long[] words, int fromInclusive, int toExclusive) {
        checkRange(words, fromInclusive, toExclusive);
        if (fromInclusive == toExclusive) {
            return new int[0];
        }
        int firstWord = fromInclusive >>> 6;
        int lastWord = (toExclusive - 1) >>> 6;
        int count = 0;
        for (int index = firstWord; index <= lastWord; index++) {
            count += Long.bitCount(windowWord(words, index, firstWord, lastWord,
                    fromInclusive, toExclusive));
        }
        int[] result = new int[count];
        int written = 0;
        for (int index = firstWord; index <= lastWord; index++) {
            long word = windowWord(words, index, firstWord, lastWord,
                    fromInclusive, toExclusive);
            while (word != 0L) {
                if (written == count) {
                    throw changedMask();
                }
                result[written++] = (index << 6) + Long.numberOfTrailingZeros(word);
                word &= word - 1L;
            }
        }
        if (written != count) {
            throw changedMask();
        }
        return result;
    }

    /** Explicit native qualification request; validation precedes native use. */
    static int[] selectedNative(long[] words, int fromInclusive, int toExclusive) {
        checkRange(words, fromInclusive, toExclusive);
        return selected0(words, fromInclusive, toExclusive);
    }

    /* The native entry also validates inputs; tests exercise that boundary. */
    static native int[] selected0(long[] words, int fromInclusive, int toExclusive);

    private static void checkRange(long[] words, int fromInclusive, int toExclusive) {
        Objects.requireNonNull(words, "words");
        // A long[] can describe more than Integer.MAX_VALUE bits. Do not narrow
        // the capacity or overflow it before checking the int index window.
        long bitCapacity = ((long) words.length) << 6;
        if (fromInclusive < 0 || toExclusive < fromInclusive
                || ((long) toExclusive) > bitCapacity) {
            throw new IndexOutOfBoundsException("selection range [" + fromInclusive
                    + ", " + toExclusive + ") for " + bitCapacity + " bits");
        }
    }

    private static long windowWord(long[] words, int index, int firstWord,
                                   int lastWord, int fromInclusive, int toExclusive) {
        long word = words[index];
        if (index == firstWord) {
            word &= -1L << (fromInclusive & 63);
        }
        if (index == lastWord) {
            word &= -1L >>> (63 - ((toExclusive - 1) & 63));
        }
        return word;
    }

    private static ConcurrentModificationException changedMask() {
        return new ConcurrentModificationException("selection mask changed during extraction");
    }
}
