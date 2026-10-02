/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
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
 */

package java.lang;

import jdk.internal.util.M3StringFacts;

/**
 * Immutable segmented backing for M3JDK {@link String} values.
 *
 * <p>The segments are ordinary immutable leaf Strings. Each segment contributes
 * a character range described by {@code offsets} and cumulative {@code ends}.
 * Joined Strings therefore share existing String payload arrays instead of
 * copying their contents. This class never exposes the segment arrays or the
 * mutable byte arrays owned by leaf Strings.</p>
 *
 * <p>This is deliberately a representation helper, not a public String API.
 * Unsupported String operations may ask for a cached contiguous materialization;
 * structural operations can remain segment-native.</p>
 */
final class M3StringStorage {
    private static final int MAX_SEGMENTS = 256;
    private static final int LARGE_LEAF = 4096;
    private static final int MAX_RETENTION_RATIO = 8;
    private static final int MAX_CACHED_MATERIALIZED_BYTES = 64 * 1024;

    /*
     * These field names and types are part of the M3JDK VM/JDK private
     * contract. HotSpot resolves their offsets in java_lang_M3StringStorage.
     */
    private final String[] segments;
    private final int[] offsets;
    private final int[] ends;
    private final int length;
    private final byte coder;

    /*
     * A compatibility materialization is created only for operations that
     * still require contiguous Compact-String storage. It is immutable after
     * publication and never returned to application code.
     */
    private volatile byte[] materialized;
    private volatile long bitSignal64;
    private volatile int characterFlags = -1;

    private M3StringStorage(String[] segments, int[] offsets, int[] ends,
                            int length, byte coder) {
        this.segments = segments;
        this.offsets = offsets;
        this.ends = ends;
        this.length = length;
        this.coder = coder;
    }

    static M3StringStorage join(String first, String second) {
        String[] parts = { first, second };
        return join(parts);
    }

    static M3StringStorage join(String[] parts) {
        int segmentCount = 0;
        int totalLength = 0;
        byte resultCoder = String.LATIN1;

        for (String part : parts) {
            if (part == null) {
                throw new NullPointerException();
            }
            int partLength = part.length();
            if (partLength == 0) {
                continue;
            }
            if (Integer.MAX_VALUE - totalLength < partLength) {
                throw new OutOfMemoryError("Required length exceeds implementation limit");
            }
            totalLength += partLength;
            resultCoder |= part.coder();

            M3StringStorage storage = part.m3Storage();
            int additional = storage == null ? 1 : storage.segments.length;
            if (segmentCount > MAX_SEGMENTS - additional) {
                return null;
            }
            segmentCount += additional;
        }

        if (segmentCount == 0) {
            return null;
        }

        String[] resultSegments = new String[segmentCount];
        int[] resultOffsets = new int[segmentCount];
        int[] resultEnds = new int[segmentCount];

        int segment = 0;
        int end = 0;
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            M3StringStorage storage = part.m3Storage();
            if (storage == null) {
                resultSegments[segment] = part;
                resultOffsets[segment] = 0;
                end += part.length();
                resultEnds[segment] = end;
                segment++;
            } else {
                int previous = 0;
                for (int index = 0; index < storage.segments.length; index++) {
                    int count = storage.ends[index] - previous;
                    resultSegments[segment] = storage.segments[index];
                    resultOffsets[segment] = storage.offsets[index];
                    end += count;
                    resultEnds[segment] = end;
                    segment++;
                    previous = storage.ends[index];
                }
            }
        }

        return new M3StringStorage(
                resultSegments, resultOffsets, resultEnds, totalLength, resultCoder);
    }

    static M3StringStorage sliceOf(String source, int beginIndex, int endIndex) {
        int sliceLength = endIndex - beginIndex;
        if (sliceLength == 0) {
            return null;
        }
        M3StringStorage storage = source.m3Storage();
        if (storage != null) {
            return storage.slice(beginIndex, endIndex);
        }
        if (!mayRetain(source.length(), sliceLength)) {
            return null;
        }
        return new M3StringStorage(
                new String[] { source },
                new int[] { beginIndex },
                new int[] { sliceLength },
                sliceLength,
                source.coder());
    }

    int length() {
        return length;
    }

    byte coder() {
        return coder;
    }

    long bitSignal64() {
        long signal = bitSignal64;
        if (signal != 0L) {
            return signal;
        }
        for (int index = 0; index < length; index++) {
            signal = M3StringFacts.addSignal(signal, charAt(index));
        }
        bitSignal64 = signal;
        return signal;
    }

    int characterFlags() {
        int flags = characterFlags;
        if (flags >= 0) {
            return flags;
        }
        flags = M3StringFacts.initialFlags(length);
        for (int index = 0; index < length; index++) {
            flags = M3StringFacts.addFlags(flags, charAt(index));
        }
        characterFlags = flags;
        return flags;
    }

    int segmentCount() {
        return segments.length;
    }

    char charAt(int index) {
        String.checkIndex(index, length);
        int segment = segmentAt(index);
        int previous = segment == 0 ? 0 : ends[segment - 1];
        return segments[segment].charAt(offsets[segment] + index - previous);
    }

    int hashCodeValue() {
        int hash = 0;
        int previous = 0;
        for (int segment = 0; segment < segments.length; segment++) {
            String source = segments[segment];
            int sourceIndex = offsets[segment];
            int count = ends[segment] - previous;
            for (int index = 0; index < count; index++) {
                hash = 31 * hash + source.charAt(sourceIndex + index);
            }
            previous = ends[segment];
        }
        return hash;
    }

    boolean contentEquals(String other) {
        if (other.length() != length) {
            return false;
        }
        int logical = 0;
        int previous = 0;
        for (int segment = 0; segment < segments.length; segment++) {
            String source = segments[segment];
            int sourceIndex = offsets[segment];
            int count = ends[segment] - previous;
            for (int index = 0; index < count; index++) {
                if (source.charAt(sourceIndex + index) != other.charAt(logical++)) {
                    return false;
                }
            }
            previous = ends[segment];
        }
        return true;
    }

    void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        String.checkBoundsBeginEnd(srcBegin, srcEnd, length);
        int remaining = srcEnd - srcBegin;
        if (remaining == 0) {
            return;
        }

        int logical = srcBegin;
        int target = dstBegin;
        while (remaining > 0) {
            int segment = segmentAt(logical);
            int previous = segment == 0 ? 0 : ends[segment - 1];
            int inSegment = logical - previous;
            int available = ends[segment] - logical;
            int count = Math.min(remaining, available);
            int sourceBegin = offsets[segment] + inSegment;
            segments[segment].getChars(
                    sourceBegin, sourceBegin + count, dst, target);
            logical += count;
            target += count;
            remaining -= count;
        }
    }

    void getBytes(byte[] dst, int srcPos, int dstBegin, byte dstCoder, int count) {
        int remaining = count;
        int logical = srcPos;
        int target = dstBegin;
        while (remaining > 0) {
            int segment = segmentAt(logical);
            int previous = segment == 0 ? 0 : ends[segment - 1];
            int inSegment = logical - previous;
            int available = ends[segment] - logical;
            int take = Math.min(remaining, available);
            int sourceBegin = offsets[segment] + inSegment;
            segments[segment].getBytes(dst, sourceBegin, target, dstCoder, take);
            logical += take;
            target += take;
            remaining -= take;
        }
    }

    byte[] materialize() {
        byte[] cached = materialized;
        if (cached != null) {
            return cached;
        }

        int byteLength = length << coder;
        if ((byteLength >> coder) != length) {
            throw new OutOfMemoryError("Required length exceeds implementation limit");
        }

        byte[] created = new byte[byteLength];
        getBytes(created, 0, 0, coder, length);
        if (byteLength <= MAX_CACHED_MATERIALIZED_BYTES) {
            materialized = created;
        }
        return created;
    }

    M3StringStorage slice(int beginIndex, int endIndex) {
        int newLength = endIndex - beginIndex;
        if (beginIndex == 0 && endIndex == length) {
            return this;
        }
        if (newLength == 0) {
            return null;
        }

        int first = segmentAt(beginIndex);
        int last = segmentAt(endIndex - 1);
        int count = last - first + 1;
        String[] newSegments = new String[count];
        int[] newOffsets = new int[count];
        int[] newEnds = new int[count];

        int target = 0;
        int cumulative = 0;
        for (int segment = first; segment <= last; segment++) {
            int segmentStart = segment == 0 ? 0 : ends[segment - 1];
            int segmentEnd = ends[segment];
            int logicalStart = Math.max(beginIndex, segmentStart);
            int logicalEnd = Math.min(endIndex, segmentEnd);
            int take = logicalEnd - logicalStart;
            String leaf = segments[segment];
            if (!mayRetain(leaf.length(), take)) {
                return null;
            }
            newSegments[target] = leaf;
            newOffsets[target] = offsets[segment] + logicalStart - segmentStart;
            cumulative += take;
            newEnds[target] = cumulative;
            target++;
        }

        return new M3StringStorage(newSegments, newOffsets, newEnds, newLength, coder);
    }

    private int segmentAt(int logicalIndex) {
        int low = 0;
        int high = ends.length - 1;
        int key = logicalIndex + 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int end = ends[mid];
            if (end < key) {
                low = mid + 1;
            } else if (mid > 0 && ends[mid - 1] >= key) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        throw new InternalError("Invalid M3 String segment index");
    }

    private static boolean mayRetain(int retainedLength, int usedLength) {
        return retainedLength <= LARGE_LEAF
                || usedLength >= (retainedLength + MAX_RETENTION_RATIO - 1)
                        / MAX_RETENTION_RATIO;
    }
}
