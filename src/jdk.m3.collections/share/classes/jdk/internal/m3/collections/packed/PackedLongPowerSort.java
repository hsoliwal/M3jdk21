/* *****************************************************************************
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

import java.util.Arrays;

/**
 * Package-local natural-run merge atom for long batch owners.
 *
 * <p>A bounded sample decides whether enough existing order is present to justify run discovery.
 * Random-looking input is refused without mutation so the caller can retain its JDK sort fallback.
 * Accepted input discovers complete monotonic runs, reverses strict descending runs, schedules
 * adjacent merges by a Powersort-style node depth, and reuses the existing long galloping bound to
 * trim already-ordered merge prefixes/suffixes. No fact survives the call.</p>
 *
 * <p>This owner sorts only. Set-style duplicate collapse remains in {@link PackedLongArrayOps},
 * preserving one authority for that contract.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongPowerSort.java}
 * blob {@code 81d6011df1dae5c4dc86aa28524d6bfba4ab525f} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
final class PackedLongPowerSort {
    private static final int SAMPLE_LIMIT = 256;
    private static final int MIN_SAMPLE = 64;
    private static final int MAX_SAMPLE_RUNS = 16;
    private static final int MIN_AVERAGE_RUN = 16;

    private PackedLongPowerSort() { }

    /** Sort in-place when the bounded sample proves useful natural-run structure. */
    static boolean sortIfStructured(long[] values, int size) {
        if (!structuredCandidate(values, size)) {
            return false;
        }
        Workspace workspace = new Workspace();
        int start = 0;
        int length = prepareRun(values, 0, size);
        workspace.push(0, length);
        start += length;
        while (start < size) {
            int nextLength = prepareRun(values, start, size);
            int power = nodePower(
                    workspace.topStart(), workspace.topLength(), nextLength, size);
            while (workspace.count > 1 && workspace.powerBeforeTop() > power) {
                workspace.mergeTop(values);
            }
            workspace.setTopPower(power);
            workspace.push(start, nextLength);
            start += nextLength;
        }
        while (workspace.count > 1) {
            workspace.mergeTop(values);
        }
        return true;
    }

    /**
     * Bounded cost hint only. False guarantees no mutation; true never substitutes for sorting.
     * The complete run structure is rediscovered only after this admission succeeds.
     */
    static boolean structuredCandidate(long[] values, int size) {
        if (size < MIN_SAMPLE) {
            return false;
        }
        int limit = Math.min(size, SAMPLE_LIMIT);
        int start = 0;
        int runs = 0;
        while (start < limit && runs < MAX_SAMPLE_RUNS) {
            start = runEnd(values, start, limit);
            runs++;
        }
        if (runs == 0 || runs == MAX_SAMPLE_RUNS && start < limit) {
            return false;
        }
        return start / runs >= MIN_AVERAGE_RUN;
    }

    /**
     * Depth of the boundary between two adjacent runs in the conceptual balanced merge tree.
     * Long arithmetic prevents the doubled midpoint coordinates from overflowing int.
     */
    static int nodePower(
            int start, int leftLength, int rightLength, int totalLength) {
        long total = totalLength;
        long leftMidpointTwice = 2L * start + leftLength;
        long rightMidpointTwice =
                leftMidpointTwice + leftLength + rightLength;
        int power = 0;
        for (;;) {
            power++;
            if (leftMidpointTwice >= total) {
                leftMidpointTwice -= total;
                rightMidpointTwice -= total;
            } else if (rightMidpointTwice >= total) {
                return power;
            }
            leftMidpointTwice <<= 1;
            rightMidpointTwice <<= 1;
        }
    }

    private static int prepareRun(long[] values, int start, int end) {
        int stop = runEnd(values, start, end);
        if (stop - start > 1 && values[start] > values[start + 1]) {
            reverse(values, start, stop);
        }
        return stop - start;
    }

    private static int runEnd(long[] values, int start, int end) {
        int next = start + 1;
        if (next >= end) {
            return end;
        }
        boolean descending = values[next] < values[start];
        next++;
        if (descending) {
            while (next < end && values[next] < values[next - 1]) {
                next++;
            }
        } else {
            while (next < end && values[next] >= values[next - 1]) {
                next++;
            }
        }
        return next;
    }

    private static void reverse(long[] values, int from, int to) {
        for (int left = from, right = to - 1;
                left < right;
                left++, right--) {
            long value = values[left];
            values[left] = values[right];
            values[right] = value;
        }
    }

    private static final class Workspace {
        private int[] starts = new int[16];
        private int[] lengths = new int[16];
        private int[] powers = new int[16];
        private long[] scratch = new long[0];
        private int count;

        void push(int start, int length) {
            ensureRuns(count + 1);
            starts[count] = start;
            lengths[count] = length;
            powers[count] = 0;
            count++;
        }

        int topStart() {
            return starts[count - 1];
        }

        int topLength() {
            return lengths[count - 1];
        }

        int powerBeforeTop() {
            return powers[count - 2];
        }

        void setTopPower(int power) {
            powers[count - 1] = power;
        }

        void mergeTop(long[] values) {
            int left = count - 2;
            int start = starts[left];
            int middle = start + lengths[left];
            int end = middle + lengths[left + 1];
            merge(values, start, middle, end);
            lengths[left] += lengths[left + 1];
            count--;
        }

        private void merge(
                long[] values, int start, int middle, int end) {
            if (values[middle - 1] <= values[middle]) {
                return;
            }
            int trimmedStart = PackedPrimitiveArrayOps.gallopBound(
                    values, start, middle, values[middle], true);
            int trimmedEnd = PackedPrimitiveArrayOps.gallopBound(
                    values, middle, end, values[middle - 1], false);
            if (trimmedStart >= middle || trimmedEnd <= middle) {
                return;
            }
            int leftLength = middle - trimmedStart;
            int rightLength = trimmedEnd - middle;
            ensureScratch(Math.min(leftLength, rightLength));
            if (leftLength <= rightLength) {
                mergeLow(
                        values, trimmedStart, middle, trimmedEnd, leftLength);
            } else {
                mergeHigh(
                        values, trimmedStart, middle, trimmedEnd, rightLength);
            }
        }

        private void mergeLow(
                long[] values,
                int start,
                int middle,
                int end,
                int leftLength) {
            System.arraycopy(values, start, scratch, 0, leftLength);
            int left = 0;
            int right = middle;
            int write = start;
            while (left < leftLength && right < end) {
                if (scratch[left] <= values[right]) {
                    values[write++] = scratch[left++];
                } else {
                    values[write++] = values[right++];
                }
            }
            if (left < leftLength) {
                System.arraycopy(
                        scratch, left, values, write, leftLength - left);
            }
        }

        private void mergeHigh(
                long[] values,
                int start,
                int middle,
                int end,
                int rightLength) {
            System.arraycopy(values, middle, scratch, 0, rightLength);
            int left = middle - 1;
            int right = rightLength - 1;
            int write = end - 1;
            while (left >= start && right >= 0) {
                if (values[left] > scratch[right]) {
                    values[write--] = values[left--];
                } else {
                    values[write--] = scratch[right--];
                }
            }
            if (right >= 0) {
                System.arraycopy(scratch, 0, values, start, right + 1);
            }
        }

        private void ensureRuns(int required) {
            if (required <= starts.length) {
                return;
            }
            int next = starts.length << 1;
            starts = Arrays.copyOf(starts, next);
            lengths = Arrays.copyOf(lengths, next);
            powers = Arrays.copyOf(powers, next);
        }

        private void ensureScratch(int required) {
            if (required > scratch.length) {
                scratch = new long[required];
            }
        }
    }
}
