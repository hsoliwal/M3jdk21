// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.util.Arrays;
import java.util.Objects;

/**
 * Compile-once exact UTF-16 Levenshtein plan.
 *
 * <p>Patterns up to 63 UTF-16 units use a precomputed single-word Myers verifier. Longer patterns
 * use reusable two-row dynamic programming. Threshold queries use a banded exact path and may be
 * preceded by {@link M3TextSignals#editLowerBound(M3TextSignals)}.</p>
 */
public final class M3EditDistancePlan implements CharSequence {
    private static final int MAX_PATTERN_UNITS = 1_048_576;
    private static final int MAX_SOURCE_UNITS = 1_048_576;
    private static final long MAX_DP_CELLS = 64_000_000L;
    private static final int MYERS_MAX_UNITS = 63;

    private final char[] pattern;
    private final char[] myersUnits;
    private final long[] myersMasks;
    private final long myersHighBit;
    private final long myersDomainMask;

    private M3EditDistancePlan(
            char[] pattern,
            char[] myersUnits,
            long[] myersMasks,
            long myersHighBit,
            long myersDomainMask) {
        this.pattern = pattern;
        this.myersUnits = myersUnits;
        this.myersMasks = myersMasks;
        this.myersHighBit = myersHighBit;
        this.myersDomainMask = myersDomainMask;
    }

    public static M3EditDistancePlan compile(CharSequence pattern) {
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.length() > MAX_PATTERN_UNITS) {
            throw new IllegalArgumentException("distance pattern budget exceeded");
        }
        char[] snapshot = new char[pattern.length()];
        for (int index = 0; index < snapshot.length; index++) {
            snapshot[index] = pattern.charAt(index);
        }

        if (snapshot.length == 0 || snapshot.length > MYERS_MAX_UNITS) {
            return new M3EditDistancePlan(snapshot, null, null, 0L, 0L);
        }

        char[] units = snapshot.clone();
        Arrays.sort(units);
        int unique = 1;
        for (int index = 1; index < units.length; index++) {
            if (units[index] != units[unique - 1]) units[unique++] = units[index];
        }
        units = Arrays.copyOf(units, unique);
        long[] masks = new long[unique];
        for (int index = 0; index < snapshot.length; index++) {
            int lane = Arrays.binarySearch(units, snapshot[index]);
            masks[lane] |= 1L << index;
        }
        long highBit = 1L << (snapshot.length - 1);
        long domainMask = (1L << snapshot.length) - 1L;
        return new M3EditDistancePlan(snapshot, units, masks, highBit, domainMask);
    }

    public int patternLength() {
        return pattern.length;
    }

    public boolean usesMyers() {
        return myersUnits != null;
    }

    public long primitivePayloadBytes() {
        long patternBytes = Character.BYTES * (long) pattern.length;
        long masks = myersMasks == null ? 0L : Long.BYTES * (long) myersMasks.length;
        long units = myersUnits == null ? 0L : Character.BYTES * (long) myersUnits.length;
        return Math.addExact(patternBytes, Math.addExact(masks, units));
    }

    public Workspace workspace() {
        return new Workspace(this);
    }

    public int distance(CharSequence value) {
        return distance(value, workspace(), M3Progress.none());
    }

    public int distance(CharSequence value, Workspace workspace, M3Progress monitor) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(workspace, "workspace").require(this);
        M3Progress progress = monitor == null ? M3Progress.none() : monitor;
        ensureWorkload(value.length());
        if (pattern.length == 0) return value.length();
        if (usesMyers()) return myersDistance(value, progress);
        requireFullDpBudget(value.length());
        return dynamicDistance(value, workspace, progress);
    }

    public boolean within(CharSequence value, int maxDistance) {
        return within(value, workspace(), maxDistance, M3Progress.none());
    }

    public boolean within(
            CharSequence value,
            Workspace workspace,
            int maxDistance,
            M3Progress monitor) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(workspace, "workspace").require(this);
        if (maxDistance < 0) throw new IllegalArgumentException("negative maxDistance");
        M3Progress progress = monitor == null ? M3Progress.none() : monitor;
        ensureWorkload(value.length());
        if (Math.abs((long) value.length() - pattern.length) > maxDistance) return false;
        if (pattern.length == 0) return value.length() <= maxDistance;

        M3TextSignals patternSignals = workspace.patternSignals;
        M3TextSignals valueSignals = M3TextSignals.compile(value);
        if (patternSignals.editLowerBound(valueSignals) > maxDistance) return false;

        if (usesMyers() && maxDistance >= Math.min(pattern.length, value.length())) {
            return myersDistance(value, progress) <= maxDistance;
        }
        requireBandedBudget(value.length(), maxDistance);
        return bandedWithin(value, workspace, maxDistance, progress);
    }

    private int myersDistance(CharSequence value, M3Progress progress) {
        long positive = myersDomainMask;
        long negative = 0L;
        int score = pattern.length;
        progress.begin("M3 Myers edit distance", value.length());
        try {
            for (int index = 0; index < value.length(); index++) {
                if ((index & 1023) == 0) progress.checkCanceled();
                long equal = mask(value.charAt(index));
                long xv = equal | negative;
                long xh = (((equal & positive) + positive) ^ positive) | equal;
                long ph = negative | ~(xh | positive);
                long mh = positive & xh;

                if ((ph & myersHighBit) != 0L) score++;
                if ((mh & myersHighBit) != 0L) score--;

                ph = ((ph << 1) | 1L) & myersDomainMask;
                mh = (mh << 1) & myersDomainMask;
                positive = (mh | ~(xv | ph)) & myersDomainMask;
                negative = (ph & xv) & myersDomainMask;
                progress.worked(1);
            }
            progress.checkCanceled();
            return score;
        } finally {
            progress.done();
        }
    }

    private long mask(char unit) {
        int index = Arrays.binarySearch(myersUnits, unit);
        return index < 0 ? 0L : myersMasks[index];
    }

    private int dynamicDistance(
            CharSequence value,
            Workspace workspace,
            M3Progress progress) {
        int[] previous = workspace.previous;
        int[] current = workspace.current;
        for (int column = 0; column <= pattern.length; column++) previous[column] = column;

        progress.begin("M3 dynamic edit distance", value.length());
        try {
            for (int row = 1; row <= value.length(); row++) {
                if ((row & 1023) == 0) progress.checkCanceled();
                current[0] = row;
                char unit = value.charAt(row - 1);
                for (int column = 1; column <= pattern.length; column++) {
                    current[column] =
                            Math.min(
                                    Math.min(current[column - 1] + 1, previous[column] + 1),
                                    previous[column - 1]
                                            + (pattern[column - 1] == unit ? 0 : 1));
                }
                int[] swap = previous;
                previous = current;
                current = swap;
                progress.worked(1);
            }
            int result = previous[pattern.length];
            if (previous != workspace.previous) {
                System.arraycopy(previous, 0, workspace.previous, 0, previous.length);
            }
            progress.checkCanceled();
            return result;
        } finally {
            progress.done();
        }
    }

    private boolean bandedWithin(
            CharSequence value,
            Workspace workspace,
            int maxDistance,
            M3Progress progress) {
        int band = Math.min(maxDistance, Math.max(value.length(), pattern.length));
        int infinity = band + 1;
        int[] previous = workspace.previous;
        int[] current = workspace.current;
        Arrays.fill(previous, 0, pattern.length + 1, infinity);
        int last = Math.min(pattern.length, band);
        for (int column = 0; column <= last; column++) previous[column] = column;

        progress.begin("M3 banded edit distance", value.length());
        try {
            for (int row = 1; row <= value.length(); row++) {
                if ((row & 1023) == 0) progress.checkCanceled();
                Arrays.fill(current, 0, pattern.length + 1, infinity);
                current[0] = row;
                int low = Math.max(1, row - band);
                int high = Math.min(pattern.length, row + band);
                int rowMinimum = infinity;
                for (int column = low; column <= high; column++) {
                    current[column] =
                            Math.min(
                                    Math.min(current[column - 1] + 1, previous[column] + 1),
                                    previous[column - 1]
                                            + (pattern[column - 1] == value.charAt(row - 1) ? 0 : 1));
                    rowMinimum = Math.min(rowMinimum, current[column]);
                }
                int[] swap = previous;
                previous = current;
                current = swap;
                progress.worked(1);
                if (rowMinimum > maxDistance) return false;
            }
            progress.checkCanceled();
            return previous[pattern.length] <= maxDistance;
        } finally {
            progress.done();
        }
    }

    private void ensureWorkload(int sourceLength) {
        if (sourceLength < 0 || sourceLength > MAX_SOURCE_UNITS) {
            throw new IllegalArgumentException("distance source budget exceeded");
        }
    }

    private void requireFullDpBudget(int sourceLength) {
        long cells = Math.multiplyExact((long) sourceLength, Math.max(1, pattern.length));
        if (cells > MAX_DP_CELLS) {
            throw new IllegalArgumentException("distance DP work budget exceeded");
        }
    }

    private void requireBandedBudget(int sourceLength, int maxDistance) {
        long width = Math.min(
                pattern.length,
                Math.addExact(Math.multiplyExact((long) maxDistance, 2L), 1L));
        long cells = Math.multiplyExact((long) sourceLength, Math.max(1L, width));
        if (cells > MAX_DP_CELLS) {
            throw new IllegalArgumentException("distance banded work budget exceeded");
        }
    }

    @Override
    public int length() {
        return pattern.length;
    }

    @Override
    public char charAt(int index) {
        return pattern[Objects.checkIndex(index, pattern.length)];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        Objects.checkFromToIndex(start, end, pattern.length);
        return new String(pattern, start, end - start);
    }

    @Override
    public String toString() {
        return new String(pattern);
    }

    public static final class Workspace {
        private final M3EditDistancePlan owner;
        private final int[] previous;
        private final int[] current;
        private final M3TextSignals patternSignals;

        private Workspace(M3EditDistancePlan owner) {
            this.owner = owner;
            this.previous = new int[owner.pattern.length + 1];
            this.current = new int[owner.pattern.length + 1];
            this.patternSignals = M3TextSignals.compile(owner);
        }

        public int[] previousRowCopy() {
            return previous.clone();
        }

        private void require(M3EditDistancePlan expected) {
            if (owner != expected) {
                throw new IllegalArgumentException("distance workspace belongs to another plan");
            }
        }
    }
}
