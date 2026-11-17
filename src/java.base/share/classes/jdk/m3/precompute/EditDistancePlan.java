// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.IntUnaryOperator;

/**
 * Compile-once exact UTF-16 Levenshtein plan.
 *
 * <p>Patterns up to 63 UTF-16 units use a precomputed single-word Myers verifier. Longer patterns
 * use reusable two-row dynamic programming. Threshold queries use a banded exact path and may be
 * preceded by {@link M3TextSignals#editLowerBound(M3TextSignals)}.</p>
 *
 * <p>{@link #frontier()} admits an immutable incremental checkpoint (exact DP row over a consumed
 * prefix) that appends UTF-16 units under {@link FrontierLimits}. The legacy code-unit flag of
 * {@link #compile(CharSequence, boolean)} remains observable; both flag values retain UTF-16-unit
 * behaviour. Threshold answers are exact within the admitted workload.</p>
 */
public final class EditDistancePlan implements CharSequence {
    private static final int MAX_PATTERN_UNITS = 1_048_576;
    private static final int MAX_SOURCE_UNITS = 1_048_576;
    private static final long MAX_DP_CELLS = 64_000_000L;
    private static final int MYERS_MAX_UNITS = 63;

    private final char[] pattern;
    private final char[] myersUnits;
    private final long[] myersMasks;
    private final long myersHighBit;
    private final long myersDomainMask;
    //FLLDCIM: MRL - census3 P3-010, donor MIndexStringEditDistancePlan blob 8302a1524f20: legacy code-unit flag lane
    private final boolean useUnicodeCodeUnits;

    private EditDistancePlan(
            char[] pattern,
            char[] myersUnits,
            long[] myersMasks,
            long myersHighBit,
            long myersDomainMask,
            boolean utf16CodeUnits) {
        this.pattern = pattern;
        this.myersUnits = myersUnits;
        this.myersMasks = myersMasks;
        this.myersHighBit = myersHighBit;
        this.myersDomainMask = myersDomainMask;
        this.useUnicodeCodeUnits = utf16CodeUnits;
    }

    public static EditDistancePlan compile(CharSequence pattern) {
        return compile(pattern, true);
    }

    //FLLDCIM: MRL - census3 P3-010, donor MIndexStringEditDistancePlan blob 8302a1524f20: compile with the legacy code-unit flag
    /** Compiles {@code pattern}; the flag is retained and observable, both values use UTF-16 units. */
    public static EditDistancePlan compile(CharSequence pattern, boolean utf16CodeUnits) {
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.length() > MAX_PATTERN_UNITS) {
            throw new IllegalArgumentException("distance pattern budget exceeded");
        }
        char[] snapshot = new char[pattern.length()];
        for (int index = 0; index < snapshot.length; index++) {
            snapshot[index] = pattern.charAt(index);
        }

        if (snapshot.length == 0 || snapshot.length > MYERS_MAX_UNITS) {
            return new EditDistancePlan(snapshot, null, null, 0L, 0L, utf16CodeUnits);
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
        return new EditDistancePlan(snapshot, units, masks, highBit, domainMask, utf16CodeUnits);
    }

    public int patternLength() {
        return pattern.length;
    }

    public boolean usesMyers() {
        return myersUnits != null;
    }

    //FLLDCIM: MRL - census3 P3-010, donor MIndexStringEditDistancePlan blob 8302a1524f20: flag accessor
    public boolean usesUtf16CodeUnits() {
        return useUnicodeCodeUnits;
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
        return distance(value, workspace(), Progress.none());
    }

    public int distance(CharSequence value, Workspace workspace, Progress monitor) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(workspace, "workspace").require(this);
        Progress progress = monitor == null ? Progress.none() : monitor;
        ensureWorkload(value.length());
        if (pattern.length == 0) return value.length();
        if (usesMyers()) return myersDistance(value, progress);
        requireFullDpBudget(value.length());
        return dynamicDistance(value, workspace, progress);
    }

    public boolean within(CharSequence value, int maxDistance) {
        return within(value, workspace(), maxDistance, Progress.none());
    }

    public boolean within(
            CharSequence value,
            Workspace workspace,
            int maxDistance,
            Progress monitor) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(workspace, "workspace").require(this);
        if (maxDistance < 0) throw new IllegalArgumentException("negative maxDistance");
        Progress progress = monitor == null ? Progress.none() : monitor;
        ensureWorkload(value.length());
        if (Math.abs((long) value.length() - pattern.length) > maxDistance) return false;
        if (pattern.length == 0) return value.length() <= maxDistance;

        TextSignals patternSignals = workspace.patternSignals;
        TextSignals valueSignals = TextSignals.compile(value);
        if (patternSignals.editLowerBound(valueSignals) > maxDistance) return false;

        if (usesMyers() && maxDistance >= Math.min(pattern.length, value.length())) {
            return myersDistance(value, progress) <= maxDistance;
        }
        requireBandedBudget(value.length(), maxDistance);
        return bandedWithin(value, workspace, maxDistance, progress);
    }

    //FLLDCIM: MRL - census3 P3-010, donor MIndexStringEditDistancePlan blob 8302a1524f20: frontier admission
    public Frontier frontier() {
        return frontier(FrontierLimits.DEFAULT, null);
    }

    /**
     * Admits an immutable empty-prefix checkpoint owned by this exact compiled plan. A null monitor
     * is a no-op; thread interruption is honored. The caller owns begin/done and input lifetimes.
     */
    public Frontier frontier(FrontierLimits limits, Progress monitor) {
        Objects.requireNonNull(limits, "limits");
        frontierCheck(monitor);
        long bytes = Integer.BYTES * ((long) pattern.length + 1L);
        if (bytes > limits.maxAdditionalPrimitiveBytes()) {
            throw new IllegalArgumentException("frontier workspace budget exceeded");
        }
        int[] row = new int[pattern.length + 1];
        for (int j = 0; j < row.length; j++) {
            if ((j & 1023) == 0) frontierCheck(monitor);
            row[j] = j;
        }
        frontierCheck(monitor);
        return new Frontier(this, limits, row, 0);
    }

    private int myersDistance(CharSequence value, Progress progress) {
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
            Progress progress) {
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
            Progress progress) {
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

    //FLLDCIM: MRL - census3 P3-010, donor MIndexStringEditDistancePlan blob 8302a1524f20: pure exact row atom of the frontier
    private static void advanceRow(
            char[] query, int[] previous, int[] current, char unit, int consumed, Progress monitor) {
        current[0] = consumed;
        int diagonal = previous[0];
        int left = consumed;
        for (int j = 1; j <= query.length; j++) {
            if ((j & 1023) == 0) frontierCheck(monitor);
            int above = previous[j];
            int score = Math.min(Math.min(left + 1, above + 1), diagonal + (query[j - 1] == unit ? 0 : 1));
            current[j] = score;
            diagonal = above;
            left = score;
        }
    }

    //FLLDCIM: MRL - census3 P3-010, donor MIndexStringEditDistancePlan blob 8302a1524f20: cancellation point (interrupt or monitor)
    private static void frontierCheck(Progress monitor) {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("distance frontier interrupted");
        }
        if (monitor != null) monitor.checkCanceled();
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("distance frontier interrupted");
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

    //FLLDCIM: MRL - census3 P3-010, donor MIndexStringEditDistancePlan blob 8302a1524f20: frontier limits
    /** Per-branch logical work and per-append added Java primitive storage admission. */
    public record FrontierLimits(int maxInputUnits, long maxCellUpdates, long maxAdditionalPrimitiveBytes) {
        public static final FrontierLimits DEFAULT = new FrontierLimits(1_048_576, 100_000_000L, 64L << 20);

        public FrontierLimits {
            if (maxInputUnits < 0 || maxInputUnits == Integer.MAX_VALUE
                    || maxCellUpdates < 0 || maxAdditionalPrimitiveBytes < 0) {
                throw new IllegalArgumentException("invalid frontier limits");
            }
        }
    }

    //FLLDCIM: MRL - census3 P3-010, donor MIndexStringEditDistancePlan blob 8302a1524f20: immutable incremental checkpoint (appendDirect/FrontierBackend held: indexstring JNI)
    /**
     * Exact immutable DP boundary: {@code row[j] = distance(pattern[0,j), consumed prefix)}.
     *
     * <p>The checkpoint retains only its plan, limits, unit count and numeric row. Appends may run
     * concurrently from one checkpoint, with independent monitors and stable inputs. A failed append
     * never mutates the checkpoint or publishes its partially computed successor. Keep or discard
     * older checkpoints explicitly: no hidden or global cache is introduced.</p>
     *
     * <p>Each appended UTF-16 unit costs {@code max(1, patternLength)} logical cell updates. The work
     * budget applies to the complete branch, so partitioning cannot evade it. Initialization and row
     * copies are reported by storage accounting, not added to that recurrence count. Additional
     * primitive storage is two int rows per nonempty append, excluding the plan, existing
     * checkpoints and object headers. This is not a reusable text-to-text transfer matrix or a
     * scalar-distance composition rule.</p>
     */
    public static final class Frontier {
        private final EditDistancePlan owner;
        private final FrontierLimits limits;
        private final int[] row;
        private final int inputUnits;

        private Frontier(EditDistancePlan owner, FrontierLimits limits, int[] row, int inputUnits) {
            this.owner = owner;
            this.limits = limits;
            this.row = row;
            this.inputUnits = inputUnits;
        }

        public EditDistancePlan plan() {
            return owner;
        }

        public FrontierLimits limits() {
            return limits;
        }

        public int inputUnits() {
            return inputUnits;
        }

        public long cellUpdates() {
            return (long) inputUnits * Math.max(1, owner.pattern.length);
        }

        public long primitivePayloadBytes() {
            return Integer.BYTES * (long) row.length;
        }

        public int distance() {
            return row[owner.pattern.length];
        }

        public int[] rowCopy() {
            return row.clone();
        }

        /**
         * Exact minimum final distance over all possible suffixes of {@code remainingUnits}.
         *
         * <p>This is a lower bound for a particular unseen suffix, never proof that it matches. It
         * performs one existing-row lookup (or length arithmetic), with no scan, allocation, source
         * access or new cache. It does not admit append resources; append limits still apply. The
         * long result also describes logical completions larger than this frontier can admit.</p>
         *
         * <p>Let m be the pattern length and r the suffix length. Every completion crosses this
         * boundary at a pattern prefix j, costing at least {@code row[j] + abs(m-j-r)}. Adjacent exact
         * row values differ by at most one, so the minimum is {@code row[m-r]} for r &lt;= m, otherwise
         * {@code inputUnits+r-m}. Choosing the final r pattern units (or padding when r &gt; m) attains
         * it. This rule is specific to the unit-cost UTF-16 insertion/deletion/substitution
         * contract; it is not a weighted, transposition, grapheme or normalization bound.</p>
         *
         * @throws IllegalArgumentException if remainingUnits is negative
         */
        public long minimumPossibleDistance(int remainingUnits) {
            if (remainingUnits < 0) throw new IllegalArgumentException("negative remaining units");
            int width = owner.pattern.length;
            return remainingUnits <= width ? row[width - remainingUnits]
                    : (long) inputUnits + remainingUnits - width;
        }

        public boolean within(int maximum) {
            if (maximum < 0) throw new IllegalArgumentException("negative maximum distance");
            return distance() <= maximum;
        }

        public Frontier append(CharSequence value) {
            return append(value, null);
        }

        /** Input must remain stable only for this synchronous call; it is never retained. */
        public Frontier append(CharSequence value, Progress monitor) {
            Objects.requireNonNull(value, "value");
            frontierCheck(monitor);
            return append(value, 0, value.length(), monitor);
        }

        /** Appends exactly [begin,end) without substring creation, normalization or case folding. */
        public Frontier append(CharSequence value, int begin, int end, Progress monitor) {
            Objects.requireNonNull(value, "value");
            frontierCheck(monitor);
            Objects.checkFromToIndex(begin, end, value.length());
            frontierCheck(monitor);
            return extend(end - begin, index -> value.charAt(begin + index), monitor);
        }

        private int admittedLength(int count) {
            long total = (long) inputUnits + count;
            if (total > limits.maxInputUnits()) {
                throw new IllegalArgumentException("frontier input budget exceeded");
            }
            long cells = total * Math.max(1, owner.pattern.length);
            if (cells > limits.maxCellUpdates()) {
                throw new IllegalArgumentException("frontier cell budget exceeded");
            }
            long bytes = count == 0 ? 0L : 2L * Integer.BYTES * row.length;
            if (bytes > limits.maxAdditionalPrimitiveBytes()) {
                throw new IllegalArgumentException("frontier workspace budget exceeded");
            }
            return (int) total;
        }

        private Frontier extend(int count, IntUnaryOperator unitAt, Progress monitor) {
            int total = admittedLength(count);
            frontierCheck(monitor);
            if (count == 0) return this;
            int[] previous = row.clone();
            int[] current = new int[row.length];
            int reported = 0;
            for (int index = 0; index < count; index++) {
                frontierCheck(monitor);
                char unit = (char) unitAt.applyAsInt(index);
                frontierCheck(monitor);
                advanceRow(owner.pattern, previous, current, unit, inputUnits + index + 1, monitor);
                int[] swap = previous;
                previous = current;
                current = swap;
                if ((index & 1023) == 1023 || index == count - 1) {
                    int delta = index + 1 - reported;
                    if (monitor != null) monitor.worked(delta);
                    reported = index + 1;
                    frontierCheck(monitor);
                }
            }
            frontierCheck(monitor);
            return new Frontier(owner, limits, previous, total);
        }
    }

    public static final class Workspace {
        private final EditDistancePlan owner;
        private final int[] previous;
        private final int[] current;
        private final TextSignals patternSignals;

        private Workspace(EditDistancePlan owner) {
            this.owner = owner;
            this.previous = new int[owner.pattern.length + 1];
            this.current = new int[owner.pattern.length + 1];
            this.patternSignals = TextSignals.compile(owner);
        }

        public int[] previousRowCopy() {
            return previous.clone();
        }

        private void require(EditDistancePlan expected) {
            if (owner != expected) {
                throw new IllegalArgumentException("distance workspace belongs to another plan");
            }
        }
    }
}
