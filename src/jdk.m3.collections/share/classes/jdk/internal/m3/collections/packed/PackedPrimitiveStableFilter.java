/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.packed;

import java.util.Objects;

/**
 * Stable caller-mask compaction for eight primitive types; exact preflight, no retained state.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedPrimitiveStableFilter.java}
 * blob {@code c7efb73327a005be7461fc203f571b840b9b3c46} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedPrimitiveStableFilter {
    private PackedPrimitiveStableFilter() { }

    /**
     * Stable compaction using bits relative to from. Tail bits outside the range are ignored.
     * Same-array output is permitted only at or before from. Other aliases are rejected.
     * Exact cardinality/capacity are checked BEFORE writes; source and mask must be stable.
     */
    public static int compact(boolean[] source, int from, int to, long[] decisions,
            boolean[] output, int outputFrom) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(decisions, "decisions");
        Objects.checkFromToIndex(from, to, source.length);
        Objects.checkFromIndexSize(outputFrom, 0, output.length);
        if ((Object) source == decisions || (Object) output == decisions ||
                (source == output && outputFrom > from)) {
            throw new IllegalArgumentException("unsafe alias");
        }
        int count = PackedAdmissionMask.count(decisions, to - from);
        Objects.checkFromIndexSize(outputFrom, count, output.length);
        if (count == to - from) {
            System.arraycopy(source, from, output, outputFrom, count);
            return count;
        }
        int write = outputFrom;
        if (count > (to - from) / 16) {
            for (int i = from; i < to; i++) {
                if (PackedAdmissionMask.selected(decisions, i - from)) { output[write++] = source[i]; }
            }
            return count;
        }
        for (int bit = PackedAdmissionMask.nextSelected(decisions, 0, to - from); bit < to - from;
                bit = PackedAdmissionMask.nextSelected(decisions, bit + 1, to - from)) {
            output[write++] = source[from + bit];
        }
        return count;
    }

    /**
     * Stable compaction using bits relative to from. Tail bits outside the range are ignored.
     * Same-array output is permitted only at or before from. Other aliases are rejected.
     * Exact cardinality/capacity are checked BEFORE writes; source and mask must be stable.
     */
    public static int compact(byte[] source, int from, int to, long[] decisions,
            byte[] output, int outputFrom) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(decisions, "decisions");
        Objects.checkFromToIndex(from, to, source.length);
        Objects.checkFromIndexSize(outputFrom, 0, output.length);
        if ((Object) source == decisions || (Object) output == decisions ||
                (source == output && outputFrom > from)) {
            throw new IllegalArgumentException("unsafe alias");
        }
        int count = PackedAdmissionMask.count(decisions, to - from);
        Objects.checkFromIndexSize(outputFrom, count, output.length);
        if (count == to - from) {
            System.arraycopy(source, from, output, outputFrom, count);
            return count;
        }
        int write = outputFrom;
        if (count > (to - from) / 16) {
            for (int i = from; i < to; i++) {
                if (PackedAdmissionMask.selected(decisions, i - from)) { output[write++] = source[i]; }
            }
            return count;
        }
        for (int bit = PackedAdmissionMask.nextSelected(decisions, 0, to - from); bit < to - from;
                bit = PackedAdmissionMask.nextSelected(decisions, bit + 1, to - from)) {
            output[write++] = source[from + bit];
        }
        return count;
    }

    /**
     * Stable compaction using bits relative to from. Tail bits outside the range are ignored.
     * Same-array output is permitted only at or before from. Other aliases are rejected.
     * Exact cardinality/capacity are checked BEFORE writes; source and mask must be stable.
     */
    public static int compact(short[] source, int from, int to, long[] decisions,
            short[] output, int outputFrom) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(decisions, "decisions");
        Objects.checkFromToIndex(from, to, source.length);
        Objects.checkFromIndexSize(outputFrom, 0, output.length);
        if ((Object) source == decisions || (Object) output == decisions ||
                (source == output && outputFrom > from)) {
            throw new IllegalArgumentException("unsafe alias");
        }
        int count = PackedAdmissionMask.count(decisions, to - from);
        Objects.checkFromIndexSize(outputFrom, count, output.length);
        if (count == to - from) {
            System.arraycopy(source, from, output, outputFrom, count);
            return count;
        }
        int write = outputFrom;
        if (count > (to - from) / 16) {
            for (int i = from; i < to; i++) {
                if (PackedAdmissionMask.selected(decisions, i - from)) { output[write++] = source[i]; }
            }
            return count;
        }
        for (int bit = PackedAdmissionMask.nextSelected(decisions, 0, to - from); bit < to - from;
                bit = PackedAdmissionMask.nextSelected(decisions, bit + 1, to - from)) {
            output[write++] = source[from + bit];
        }
        return count;
    }

    /**
     * Stable compaction using bits relative to from. Tail bits outside the range are ignored.
     * Same-array output is permitted only at or before from. Other aliases are rejected.
     * Exact cardinality/capacity are checked BEFORE writes; source and mask must be stable.
     */
    public static int compact(char[] source, int from, int to, long[] decisions,
            char[] output, int outputFrom) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(decisions, "decisions");
        Objects.checkFromToIndex(from, to, source.length);
        Objects.checkFromIndexSize(outputFrom, 0, output.length);
        if ((Object) source == decisions || (Object) output == decisions ||
                (source == output && outputFrom > from)) {
            throw new IllegalArgumentException("unsafe alias");
        }
        int count = PackedAdmissionMask.count(decisions, to - from);
        Objects.checkFromIndexSize(outputFrom, count, output.length);
        if (count == to - from) {
            System.arraycopy(source, from, output, outputFrom, count);
            return count;
        }
        int write = outputFrom;
        if (count > (to - from) / 16) {
            for (int i = from; i < to; i++) {
                if (PackedAdmissionMask.selected(decisions, i - from)) { output[write++] = source[i]; }
            }
            return count;
        }
        for (int bit = PackedAdmissionMask.nextSelected(decisions, 0, to - from); bit < to - from;
                bit = PackedAdmissionMask.nextSelected(decisions, bit + 1, to - from)) {
            output[write++] = source[from + bit];
        }
        return count;
    }

    /**
     * Stable compaction using bits relative to from. Tail bits outside the range are ignored.
     * Same-array output is permitted only at or before from. Other aliases are rejected.
     * Exact cardinality/capacity are checked BEFORE writes; source and mask must be stable.
     */
    public static int compact(int[] source, int from, int to, long[] decisions,
            int[] output, int outputFrom) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(decisions, "decisions");
        Objects.checkFromToIndex(from, to, source.length);
        Objects.checkFromIndexSize(outputFrom, 0, output.length);
        if ((Object) source == decisions || (Object) output == decisions ||
                (source == output && outputFrom > from)) {
            throw new IllegalArgumentException("unsafe alias");
        }
        int count = PackedAdmissionMask.count(decisions, to - from);
        Objects.checkFromIndexSize(outputFrom, count, output.length);
        if (count == to - from) {
            System.arraycopy(source, from, output, outputFrom, count);
            return count;
        }
        int write = outputFrom;
        if (count > (to - from) / 16) {
            for (int i = from; i < to; i++) {
                if (PackedAdmissionMask.selected(decisions, i - from)) { output[write++] = source[i]; }
            }
            return count;
        }
        for (int bit = PackedAdmissionMask.nextSelected(decisions, 0, to - from); bit < to - from;
                bit = PackedAdmissionMask.nextSelected(decisions, bit + 1, to - from)) {
            output[write++] = source[from + bit];
        }
        return count;
    }

    /**
     * Stable compaction using bits relative to from. Tail bits outside the range are ignored.
     * Same-array output is permitted only at or before from. Other aliases are rejected.
     * Exact cardinality/capacity are checked BEFORE writes; source and mask must be stable.
     */
    public static int compact(long[] source, int from, int to, long[] decisions,
            long[] output, int outputFrom) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(decisions, "decisions");
        Objects.checkFromToIndex(from, to, source.length);
        Objects.checkFromIndexSize(outputFrom, 0, output.length);
        if ((Object) source == decisions || (Object) output == decisions ||
                (source == output && outputFrom > from)) {
            throw new IllegalArgumentException("unsafe alias");
        }
        int count = PackedAdmissionMask.count(decisions, to - from);
        Objects.checkFromIndexSize(outputFrom, count, output.length);
        if (count == to - from) {
            System.arraycopy(source, from, output, outputFrom, count);
            return count;
        }
        int write = outputFrom;
        if (count > (to - from) / 16) {
            for (int i = from; i < to; i++) {
                if (PackedAdmissionMask.selected(decisions, i - from)) { output[write++] = source[i]; }
            }
            return count;
        }
        for (int bit = PackedAdmissionMask.nextSelected(decisions, 0, to - from); bit < to - from;
                bit = PackedAdmissionMask.nextSelected(decisions, bit + 1, to - from)) {
            output[write++] = source[from + bit];
        }
        return count;
    }

    /**
     * Stable compaction using bits relative to from. Tail bits outside the range are ignored.
     * Same-array output is permitted only at or before from. Other aliases are rejected.
     * Exact cardinality/capacity are checked BEFORE writes; source and mask must be stable.
     */
    public static int compact(float[] source, int from, int to, long[] decisions,
            float[] output, int outputFrom) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(decisions, "decisions");
        Objects.checkFromToIndex(from, to, source.length);
        Objects.checkFromIndexSize(outputFrom, 0, output.length);
        if ((Object) source == decisions || (Object) output == decisions ||
                (source == output && outputFrom > from)) {
            throw new IllegalArgumentException("unsafe alias");
        }
        int count = PackedAdmissionMask.count(decisions, to - from);
        Objects.checkFromIndexSize(outputFrom, count, output.length);
        if (count == to - from) {
            System.arraycopy(source, from, output, outputFrom, count);
            return count;
        }
        int write = outputFrom;
        if (count > (to - from) / 16) {
            for (int i = from; i < to; i++) {
                if (PackedAdmissionMask.selected(decisions, i - from)) { output[write++] = source[i]; }
            }
            return count;
        }
        for (int bit = PackedAdmissionMask.nextSelected(decisions, 0, to - from); bit < to - from;
                bit = PackedAdmissionMask.nextSelected(decisions, bit + 1, to - from)) {
            output[write++] = source[from + bit];
        }
        return count;
    }

    /**
     * Stable compaction using bits relative to from. Tail bits outside the range are ignored.
     * Same-array output is permitted only at or before from. Other aliases are rejected.
     * Exact cardinality/capacity are checked BEFORE writes; source and mask must be stable.
     */
    public static int compact(double[] source, int from, int to, long[] decisions,
            double[] output, int outputFrom) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(decisions, "decisions");
        Objects.checkFromToIndex(from, to, source.length);
        Objects.checkFromIndexSize(outputFrom, 0, output.length);
        if ((Object) source == decisions || (Object) output == decisions ||
                (source == output && outputFrom > from)) {
            throw new IllegalArgumentException("unsafe alias");
        }
        int count = PackedAdmissionMask.count(decisions, to - from);
        Objects.checkFromIndexSize(outputFrom, count, output.length);
        if (count == to - from) {
            System.arraycopy(source, from, output, outputFrom, count);
            return count;
        }
        int write = outputFrom;
        if (count > (to - from) / 16) {
            for (int i = from; i < to; i++) {
                if (PackedAdmissionMask.selected(decisions, i - from)) { output[write++] = source[i]; }
            }
            return count;
        }
        for (int bit = PackedAdmissionMask.nextSelected(decisions, 0, to - from); bit < to - from;
                bit = PackedAdmissionMask.nextSelected(decisions, bit + 1, to - from)) {
            output[write++] = source[from + bit];
        }
        return count;
    }
}
