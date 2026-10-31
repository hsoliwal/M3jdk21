// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.tools.cpu;

import java.util.Objects;

/** Exact source-coordinate merge of non-overlapping, half-open UTF-16 range edits.
 * A cross-side overlapping range or boundary insertion refuses with conflict coordinates.
 * This is not Git diff3, a Unicode-grapheme merge, or fuzzy hunk matching.
 */
public final class M3RangePatchMerge {
    private M3RangePatchMerge() { }
    private static final int MAX_EDITS = 100_000;
    private static final int MAX_OUTPUT_UNITS = 16 * 1024 * 1024;
    public record Result(String merged, int leftConflict, int rightConflict) {
        public boolean clean() { return leftConflict == -1 && rightConflict == -1; }
    }

    public static Result merge(CharSequence base,
            int[] leftStart, int[] leftEnd, CharSequence[] leftValue,
            int[] rightStart, int[] rightEnd, CharSequence[] rightValue) {
        Objects.requireNonNull(base, "base");
        validate(base.length(), leftStart, leftEnd, leftValue);
        validate(base.length(), rightStart, rightEnd, rightValue);
        if ((long)leftStart.length + rightStart.length > MAX_EDITS)
            throw new IllegalArgumentException("edit budget");
        int left = 0, right = 0, position = 0, prevOwner = -1, prevStart = -1, prevEnd = -1;
        int prevIndex = -1;
        StringBuilder result = new StringBuilder(base.length());
        while (left < leftStart.length || right < rightStart.length) {
            boolean fromLeft = right == rightStart.length || (left < leftStart.length
                    && leftStart[left] <= rightStart[right]);
            int owner = fromLeft ? 0 : 1, index = fromLeft ? left++ : right++;
            int from = fromLeft ? leftStart[index] : rightStart[index];
            int end = fromLeft ? leftEnd[index] : rightEnd[index];
            CharSequence replacement = fromLeft ? leftValue[index] : rightValue[index];
            if (prevOwner != -1 && prevOwner != owner
                    && (from < prevEnd || (from == prevEnd && (from == end || prevStart == prevEnd)))) {
                return new Result(null, prevOwner == 0 ? prevIndex : index,
                        prevOwner == 1 ? prevIndex : index);
            }
            if (from < position) throw new IllegalStateException("invalid prevalidated edit order");
            appendBounded(result, base, position, from);
            appendBounded(result, replacement, 0, replacement.length());
            position = end;
            prevOwner = owner; prevStart = from; prevEnd = end; prevIndex = index;
        }
        appendBounded(result, base, position, base.length());
        return new Result(result.toString(), -1, -1);
    }

    private static void validate(int sourceLength, int[] starts, int[] ends,
                                 CharSequence[] replacements) {
        Objects.requireNonNull(starts); Objects.requireNonNull(ends);
        Objects.requireNonNull(replacements);
        if (starts.length != ends.length || starts.length != replacements.length
                || starts.length > MAX_EDITS || sourceLength > MAX_OUTPUT_UNITS)
            throw new IllegalArgumentException("edit geometry");
        for (int i = 0; i < starts.length; i++) {
            if (starts[i] < 0 || ends[i] < starts[i] || ends[i] > sourceLength
                    || replacements[i] == null || replacements[i].length() > MAX_OUTPUT_UNITS)
                throw new IllegalArgumentException("range bounds");
            if (i > 0 && (starts[i] < ends[i - 1]
                    || (starts[i] == starts[i - 1] && starts[i - 1] == ends[i - 1])))
                throw new IllegalArgumentException("overlapping same-side edits");
        }
    }
    private static void appendBounded(StringBuilder result, CharSequence text, int start, int end) {
        if (end - start > MAX_OUTPUT_UNITS - result.length())
            throw new IllegalArgumentException("output size budget");
        result.append(text, start, end);
    }
}
