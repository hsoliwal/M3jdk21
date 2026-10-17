/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package jdk.internal.mindex;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Target-owned Boolean dot/star execution plan; not a Pattern parser or public JDK API.
 *
 * <p>Maps the prepared-state and fragment-relation responsibilities of Synexia
 * MIndexRegexProgram/MIndexRegexAtomAlgebra into the existing M3 String decision path.
 * Patterns admit only ASCII alphanumeric/dot atoms and optional stars. The legacy
 * summarize/advance entries reject surrogate input; summarizeUtf16 resolves code points.
 * No captures, search history, assertions, spelling payload or donor runtime is retained.
 * Immutable effects belong to this exact plan; the effect of A followed by B is B(A(state)).
 */
public final class M3RegexDpPlan {
    private final long[] literalMasks;
    private final long[] nullableBridges;
    private final long starred;
    private final long dots;
    private final long universe;
    private final long initial;
    private final long accepting;
    private final int states;
    private static volatile boolean nativeUnavailable;
    private static volatile boolean structuredUnavailable;
    private static volatile boolean repeatUnavailable;

    private M3RegexDpPlan(long[] literals, long dots, long starred, int tokens) {
        this.literalMasks = literals;
        this.dots = dots;
        this.starred = starred;
        states = tokens + 1;
        universe = states == 64 ? -1L : (1L << states) - 1;
        accepting = 1L << tokens;
        nullableBridges = new long[nullableLevels(starred)];
        long bridge = starred;
        for (int k = 0; k < nullableBridges.length; k++) {
            nullableBridges[k] = bridge;
            bridge &= bridge << (1 << k);
        }
        initial = close(1L);
    }

    /** Null means not admitted. The pattern is inspected but never retained. */
    public static M3RegexDpPlan prepare(String pattern) {
        Objects.requireNonNull(pattern, "pattern");
        int size = pattern.length();
        if (size < 2 || size > 128) return null;
        int tokens = 0, stars = 0;
        boolean previousStar = true;
        for (int i = 0; i < size; i++) {
            char c = pattern.charAt(i);
            if (c == '*') {
                if (previousStar) return null;
                stars++;
                previousStar = true;
            } else {
                if (!(c == '.' || c >= 'a' && c <= 'z'
                        || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9')) return null;
                if (++tokens > 63) return null;
                previousStar = false;
            }
        }
        if (stars == 0) return null;
        long[] literals = new long[62];
        long dot = 0, star = 0;
        int token = 0;
        for (int i = 0; i < size; i++) {
            char c = pattern.charAt(i);
            if (c == '*') star |= 1L << token;
            else if (c == '.') dot |= 1L << ++token;
            else literals[literalIndex(c)] |= 1L << ++token;
        }
        return new M3RegexDpPlan(literals, dot, star, tokens);
    }

    public int stateCount() { return states; }
    public long initialState() { return initial; }
    public boolean accepts(long frontier) { checkBits(frontier); return (frontier & accepting) != 0; }
    public boolean supports(char unit) { return !Character.isSurrogate(unit); }

    /** One allocation-free DP step, with up to six precomputed nullable-prefix propagation masks. */
    public long advance(long frontier, char unit) {
        checkBits(frontier);
        if (!supports(unit)) throw new IllegalArgumentException("surrogate input is not admitted");
        return advanceMatched(frontier, matchingMask(unit));
    }

    /** Pure pattern predicate: compute once per consumed unit, not once per row. */
    private long matchingMask(char unit) {
        int literal = literalIndex(unit);
        long matched = literal < 0 ? 0 : literalMasks[literal];
        return lineTerminator(unit) ? matched : matched | dots;
    }

    /** Internal callers already hold validated states and a supported unit. */
    private long advanceMatched(long frontier, long matched) {
        long consumed = ((frontier << 1) & matched & ~starred)
                | (frontier & matched & starred);
        return close(consumed);
    }

    /** Compact perfect index for exactly the admitted 62 literal code units. */
    private static int literalIndex(char unit) {
        if (unit >= 'a' && unit <= 'z') return unit - 'a';
        if (unit >= 'A' && unit <= 'Z') return unit - 'A' + 26;
        if (unit >= '0' && unit <= '9') return unit - '0' + 52;
        return -1;
    }

    /** Keep only the nonzero doubling bridges needed by this immutable program. */
    private static int nullableLevels(long stars) {
        int levels = 0;
        for (long bridge = stars; bridge != 0 && levels < 6; levels++) {
            bridge &= bridge << (1 << levels);
        }
        return levels;
    }

    private long close(long frontier) {
        for (int k = 0; k < nullableBridges.length; k++) {
            frontier |= (frontier << (1 << k)) & nullableBridges[k];
        }
        return frontier & universe;
    }

    private static boolean lineTerminator(char c) {
        return c == '\n' || c == '\r' || c == '\u0085' || c == '\u2028' || c == '\u2029';
    }

    /**
     * Build an all-entry-state effect without retaining input. Null means unsupported input.
     * Work is O(length * states), with at most six nullable-mask steps per entry transition.
     * Caller supplies stable text and a bounded length; a null checkpoint changes no thread state.
     */
    public Effect summarize(CharSequence text, Runnable checkpoint) {
        Objects.requireNonNull(text, "text");
        int length = text.length();
        long[] rows = identityRows();
        long previousMask = 0;
        boolean fixedPoint = false;
        boolean seekFixedPoint = length >= 2 * states;
        for (int i = 0; i < length; i++) {
            if ((i & 255) == 0) check(checkpoint);
            char c = text.charAt(i);
            if (!supports(c)) return null;
            long matched = matchingMask(c);
            if (!seekFixedPoint || i == 0 || matched != previousMask) {
                fixedPoint = false;
                for (int state = 0; state < states; state++) {
                    rows[state] = advanceMatched(rows[state], matched);
                }
            } else if (!fixedPoint) {
                fixedPoint = true;
                for (int state = 0; state < states; state++) {
                    long incoming = rows[state];
                    long next = advanceMatched(incoming, matched);
                    fixedPoint &= incoming == next;
                    rows[state] = next;
                }
            }
            previousMask = matched;
            // Reuse only a proven fixed point under the identical pattern predicate.
            // Keep reading even then: later surrogates, charAt failures and
            // checkpoint calls retain their original order and behavior.
        }
        check(checkpoint);
        return new Effect(rows, length);
    }

    /**
     * Code-point-correct summary for the same ASCII literal/dot/star program.
     * Unlike {@link #summarize}, this entry admits paired and unpaired surrogates.
     * A terminal high surrogate is deferred; a leading low has an optional skip relation.
     * Those two pieces of boundary metadata allow concatenation to resolve a split pair.
     * No character, source reference, text array or pointer is retained by the result.
     * Every UTF-16 unit is read once, in order; checkpoints occur at the same unit cadence.
     */
    public Effect summarizeUtf16(CharSequence text, Runnable checkpoint) {
        Objects.requireNonNull(text, "text");
        int length = text.length();
        long[] rows = identityRows();
        long[] withoutLeadingLow = null;
        boolean pendingHigh = false;
        for (int i = 0; i < length; i++) {
            if ((i & 255) == 0) check(checkpoint);
            char unit = text.charAt(i);
            if (i == 0 && Character.isLowSurrogate(unit)) {
                withoutLeadingLow = identityRows();
            }
            if (pendingHigh) {
                // Both a complete supplementary code point and a lone high surrogate
                // have the dot predicate in this program's strictly ASCII literal domain.
                advanceRows(rows, dots);
                if (withoutLeadingLow != null) advanceRows(withoutLeadingLow, dots);
                pendingHigh = false;
                if (Character.isLowSurrogate(unit)) continue;
            }
            if (Character.isHighSurrogate(unit)) {
                pendingHigh = true;
            } else {
                long matched = matchingMask(unit);
                advanceRows(rows, matched);
                if (withoutLeadingLow != null && i != 0) {
                    advanceRows(withoutLeadingLow, matched);
                }
            }
        }
        check(checkpoint);
        return new Effect(rows, length, withoutLeadingLow, pendingHigh);
    }

    private void advanceRows(long[] rows, long matched) {
        for (int state = 0; state < states; state++) {
            rows[state] = advanceMatched(rows[state], matched);
        }
    }

    /** Exact empty-fragment identity, not epsilon closure of arbitrary incoming states. */
    public Effect identity() { return new Effect(identityRows(), 0); }

    private long[] identityRows() {
        long[] rows = new long[states];
        for (int i = 0; i < states; i++) rows[i] = 1L << i;
        return rows;
    }

    /** Requested execution policy. JNI and CUDA are strict; PREFER_JNI may use Java. */
    public enum BatchBackend { JAVA, JNI, CUDA, PREFER_JNI }

    /** Execution actually performed. An empty batch has no backend execution. */
    public enum BatchExecution { NONE, JAVA, JNI, CUDA }

    /**
     * Admission limits for a derived batch. Primitive bytes bound the Java coordinates,
     * one invocation's result arrays, numeric staging image and simultaneous CPU/CUDA images.
     * Object headers, effect references and pre-existing immutable Effects are not counted.
     * Concurrent invocations each own their result arrays; this is not a process heap quota.
     * State visits conservatively bound preparation and all row evaluations in one call.
     */
    public record BatchLimits(int maxEffects, int maxRows, int maxTupleEffects,
                              long maxPrimitiveBytes, long maxStateVisits) {
        public BatchLimits {
            if (maxEffects < 0 || maxRows < 0 || maxTupleEffects < 0
                    || maxPrimitiveBytes < 0 || maxStateVisits < 0) {
                throw new IllegalArgumentException("negative prepared batch limit");
            }
        }

        public static BatchLimits defaults() {
            return new BatchLimits(65_536, 65_536, 1_048_576, 64L << 20, 64_000_000L);
        }
    }

    private static final int BATCH_ABI = 1;
    private static final int BATCH_ROWS_PER_CALL = 16_384;
    private static final int BATCH_EFFECTS_PER_CALL = 65_536;

    /**
     * Prepare full-match rows over existing Effects, without reading or retaining text.
     * Offsets are CSR boundaries: row r contains tupleEffects[offsets[r]..offsets[r+1]).
     * Empty rows and empty Effects are identities. Null means the candidate exceeds a
     * budget, never NO_MATCH. Malformed coordinates and foreign-plan Effects are errors.
     */
    public PreparedBatch prepareBatch(Effect[] effects, int[] tupleEffects, int[] offsets,
                                      BatchLimits limits, Runnable checkpoint) {
        return prepareBatch(null, effects, tupleEffects, offsets, null, limits, checkpoint);
    }

    /**
     * Prepare rows with a fixed prefix and suffix. Each answer is the complete-input
     * truth of prefix + row + suffix; this does not implement regex search or assertions.
     * Affixes remain open at their joins, including high/low surrogate pairs split there.
     */
    public PreparedBatch prepareBatch(Effect prefix, Effect[] effects, int[] tupleEffects,
                                      int[] offsets, Effect suffix, BatchLimits limits,
                                      Runnable checkpoint) {
        Objects.requireNonNull(effects, "effects");
        Objects.requireNonNull(tupleEffects, "tupleEffects");
        Objects.requireNonNull(offsets, "offsets");
        Objects.requireNonNull(limits, "limits");
        check(checkpoint);
        if (offsets.length == 0) throw new IllegalArgumentException("missing row boundary");
        int rowCount = offsets.length - 1;
        int affixes = (prefix == null ? 0 : 1) + (suffix == null ? 0 : 1);
        long effectCount = (long) effects.length + affixes;
        if (effectCount > limits.maxEffects || rowCount > limits.maxRows
                || tupleEffects.length > limits.maxTupleEffects) return null;
        long stride = 2L + 2L * states;
        long imageBytes = Math.addExact(Math.multiplyExact(effectCount, stride * Long.BYTES),
                Math.addExact((long) states * Long.BYTES,
                        ((long) tupleEffects.length + offsets.length) * Integer.BYTES));
        // One CPU host image, one CUDA host/device image, staging, and conservative spare
        // room for bounded workspaces. This bounds numeric allocations, not total JVM heap.
        long primitiveBytes = Math.addExact(Math.multiplyExact(5L, imageBytes),
                Math.addExact(((long) tupleEffects.length + offsets.length) * Integer.BYTES,
                        (long) rowCount * (Long.BYTES * 2L + 2L) + BATCH_ROWS_PER_CALL + 512L));
        long visits = Math.addExact(effectCount * 4L * states + 3L * states + 6L * states * states,
                Math.addExact((long) tupleEffects.length * 2L * states, rowCount));
        if (primitiveBytes > limits.maxPrimitiveBytes || visits > limits.maxStateVisits
                || effectCount * stride > Integer.MAX_VALUE) return null;

        // Snapshot coordinates before validation. No caller-owned mutable array is retained.
        Effect[] pinned = new Effect[(int) effectCount];
        System.arraycopy(effects, 0, pinned, 0, effects.length);
        int prefixIndex = prefix == null ? -1 : effects.length;
        int suffixIndex = suffix == null ? -1 : effects.length + (prefix == null ? 0 : 1);
        if (prefix != null) pinned[prefixIndex] = prefix;
        if (suffix != null) pinned[suffixIndex] = suffix;
        int[] tuples = tupleEffects.clone();
        int[] starts = offsets.clone();
        long[] lengths = new long[rowCount];
        for (int i = 0; i < pinned.length; i++) {
            if ((i & 255) == 0) check(checkpoint);
            requireEffect(pinned[i]);
        }
        if (starts[0] != 0 || starts[rowCount] != tuples.length) {
            throw new IllegalArgumentException("row boundaries must cover all tuple effects");
        }
        long affixLength = Math.addExact(prefix == null ? 0L : prefix.length,
                suffix == null ? 0L : suffix.length);
        for (int row = 0; row < rowCount; row++) {
            check(checkpoint);
            int start = starts[row], end = starts[row + 1];
            if (start < 0 || end < start || end > tuples.length) {
                throw new IllegalArgumentException("invalid prepared row boundary");
            }
            long length = affixLength;
            for (int at = start; at < end; at++) {
                if ((at & 255) == 0) check(checkpoint);
                int atom = tuples[at];
                if (atom < 0 || atom >= effects.length) {
                    throw new IllegalArgumentException("invalid prepared effect index");
                }
                length = Math.addExact(length, pinned[atom].length);
            }
            lengths[row] = length;
            if (end - start > BATCH_EFFECTS_PER_CALL) return null;
        }
        // Context-specific witnesses are private derived query state. Existing all-state
        // Effects stay immutable and retain every ordinary and boundary lane.
        long seed = prefix == null ? initial : applyRows(prefix.rows, initial);
        boolean seedHigh = prefix != null && prefix.trailingHigh;
        long suffixWithoutHigh = suffixPreimage(suffix, false);
        long suffixWithHigh = suffixPreimage(suffix, true);
        check(checkpoint);
        return new PreparedBatch(pinned, tuples, starts, lengths, prefixIndex, suffixIndex,
                primitiveBytes, imageBytes, seed, seedHigh, suffixWithoutHigh, suffixWithHigh);
    }

    /** Accepting preimage of one fixed suffix, including complete-input surrogate flush. */
    private long suffixPreimage(Effect suffix, boolean incomingHigh) {
        if (suffix == null || suffix.length == 0) {
            return incomingHigh ? dotPreimage(accepting) : accepting;
        }
        long targets = suffix.trailingHigh ? dotPreimage(accepting) : accepting;
        long[] relation = incomingHigh && suffix.withoutLeadingLow != null
                ? suffix.withoutLeadingLow : suffix.rows;
        long witness = preimage(relation, targets);
        return incomingHigh ? dotPreimage(witness) : witness;
    }

    /** Reverse existential observation scans all entry rows; it never changes a cached row. */
    private long preimage(long[] rows, long targets) {
        long witness = 0;
        for (int state = 0; state < states; state++) {
            if ((rows[state] & targets) != 0) witness |= 1L << state;
        }
        return witness;
    }

    private long dotPreimage(long targets) {
        long witness = 0;
        for (int state = 0; state < states; state++) {
            if ((advanceMatched(1L << state, dots) & targets) != 0) witness |= 1L << state;
        }
        return witness;
    }

    private void requireEffect(Effect effect) {
        Objects.requireNonNull(effect, "effect");
        if (effect.plan != this) throw new IllegalArgumentException("different regex plan");
    }

    /** Immutable complete-input answers, with exact UTF-16 lengths and an execution receipt. */
    public static final class BatchResult {
        private final byte[] answers;
        private final long[] lengths;
        private final BatchExecution execution;

        private BatchResult(byte[] answers, long[] lengths, BatchExecution execution) {
            this.answers = answers;
            this.lengths = lengths;
            this.execution = execution;
        }

        public int rowCount() { return answers.length; }
        public boolean matches(int row) { return answers[Objects.checkIndex(row, answers.length)] != 0; }
        public long utf16Length(int row) { return lengths[Objects.checkIndex(row, lengths.length)]; }
        public BatchExecution execution() { return execution; }
    }

    /**
     * Reusable, closeable numeric execution view of existing immutable Effects. It owns no
     * canonical text, parser or effect cache. Native tables are prepared lazily and retained
     * once per backend. Fixed affixes become one open seed and two boundary-aware suffix
     * acceptance preimages; row execution visits only its tuple fragments. Close waits for
     * active native chunks, releases both images and drops
     * the Java pins. Checkpoints always run outside lifecycle locks and can safely close it.
     */
    public final class PreparedBatch implements AutoCloseable {
        private final ReentrantReadWriteLock lifecycle = new ReentrantReadWriteLock(true);
        private final int rowCount;
        private final int prefixIndex, suffixIndex;
        private final long primitiveBytesBound, imageBytes;
        private final long seed, suffixWithoutHigh, suffixWithHigh;
        private final boolean seedHigh;
        private Effect[] effects;
        private int[] tuples, offsets;
        private long[] lengths;
        private long jniHandle, cudaHandle;
        private boolean closed;

        private PreparedBatch(Effect[] effects, int[] tuples, int[] offsets, long[] lengths,
                              int prefixIndex, int suffixIndex,
                              long primitiveBytesBound, long imageBytes, long seed, boolean seedHigh,
                              long suffixWithoutHigh, long suffixWithHigh) {
            this.effects = effects;
            this.tuples = tuples;
            this.offsets = offsets;
            this.lengths = lengths;
            this.rowCount = lengths.length;
            this.prefixIndex = prefixIndex;
            this.suffixIndex = suffixIndex;
            this.primitiveBytesBound = primitiveBytesBound;
            this.imageBytes = imageBytes;
            this.seed = seed;
            this.seedHigh = seedHigh;
            this.suffixWithoutHigh = suffixWithoutHigh;
            this.suffixWithHigh = suffixWithHigh;
        }

        public int rowCount() { return rowCount; }
        public long primitiveBytesBound() { return primitiveBytesBound; }
        public boolean isOpen() {
            lifecycle.readLock().lock();
            try { return !closed; }
            finally { lifecycle.readLock().unlock(); }
        }

        /**
         * Evaluate every row, with the same checkpoint boundaries for all backends.
         * JNI absence is eligible only for PREFER_JNI fallback. Runtime failures, invalid
         * geometry and cancellation propagate, and never become a definitive false answer.
         */
        public BatchResult matches(BatchBackend backend, Runnable checkpoint) {
            Objects.requireNonNull(backend, "backend");
            check(checkpoint);
            byte[] answers;
            long[] resultLengths;
            BatchExecution execution;
            lifecycle.writeLock().lock();
            try {
                ensureOpen();
                answers = new byte[rowCount];
                resultLengths = lengths.clone();
                execution = rowCount == 0 ? BatchExecution.NONE : chooseBackend(backend);
            } finally { lifecycle.writeLock().unlock(); }
            int start = 0;
            while (start < rowCount) {
                check(checkpoint);
                lifecycle.readLock().lock();
                try {
                    ensureOpen();
                    int end = chunkEnd(start);
                    if (execution == BatchExecution.JAVA) {
                        for (int row = start; row < end; row++) {
                            answers[row] = (byte) (matchRow(row) ? 1 : 0);
                        }
                    } else if (execution == BatchExecution.JNI) {
                        batchEvaluate0(jniHandle, start, end, answers, start);
                    } else {
                        batchCudaEvaluate0(cudaHandle, start, end, answers, start);
                    }
                    for (int row = start; row < end; row++) {
                        if (answers[row] != 0 && answers[row] != 1) {
                            throw new IllegalStateException("invalid prepared regex truth result");
                        }
                    }
                    start = end;
                } finally { lifecycle.readLock().unlock(); }
            }
            check(checkpoint);
            return new BatchResult(answers, resultLengths, execution);
        }

        private BatchExecution chooseBackend(BatchBackend backend) {
            if (backend == BatchBackend.JAVA) return BatchExecution.JAVA;
            boolean cuda = backend == BatchBackend.CUDA;
            if ((cuda ? cudaHandle : jniHandle) == 0) {
                try {
                    int abi = cuda ? batchCudaAbi0() : batchAbi0();
                    if (abi != BATCH_ABI) {
                        if (backend == BatchBackend.PREFER_JNI) return BatchExecution.JAVA;
                        throw new UnsupportedOperationException("prepared regex ABI mismatch");
                    }
                    long[] lanes = packedEffects();
                    long[] dotRows = identityRows();
                    advanceRows(dotRows, dots);
                    long handle = cuda
                            ? batchCudaCreate0(lanes, dotRows, states, initial, accepting,
                                    tuples, offsets, prefixIndex, suffixIndex, imageBytes)
                            : batchCreate0(lanes, dotRows, states, initial, accepting,
                                    tuples, offsets, prefixIndex, suffixIndex, imageBytes);
                    if (handle == 0) throw new IllegalStateException("prepared regex returned no handle");
                    if (cuda) cudaHandle = handle;
                    else jniHandle = handle;
                } catch (UnsatisfiedLinkError unavailable) {
                    if (backend == BatchBackend.PREFER_JNI) return BatchExecution.JAVA;
                    throw new UnsupportedOperationException("prepared regex " + backend + " unavailable", unavailable);
                }
            }
            return cuda ? BatchExecution.CUDA : BatchExecution.JNI;
        }

        private long[] packedEffects() {
            int stride = 2 + 2 * states;
            long[] lanes = new long[Math.multiplyExact(effects.length, stride)];
            for (int i = 0; i < effects.length; i++) {
                Effect effect = effects[i];
                int base = i * stride;
                lanes[base] = effect.length;
                lanes[base + 1] = (effect.withoutLeadingLow == null ? 0L : 1L)
                        | (effect.trailingHigh ? 2L : 0L);
                System.arraycopy(effect.rows, 0, lanes, base + 2, states);
                System.arraycopy(effect.withoutLeadingLow == null ? effect.rows : effect.withoutLeadingLow,
                        0, lanes, base + 2 + states, states);
            }
            return lanes;
        }

        private int chunkEnd(int start) {
            int end = start;
            int used = 0;
            while (end < rowCount && end - start < BATCH_ROWS_PER_CALL) {
                int next = offsets[end + 1] - offsets[end];
                if (end != start && used + next > BATCH_EFFECTS_PER_CALL) break;
                used += next;
                end++;
            }
            return end;
        }

        private boolean matchRow(int row) {
            long frontier = seed;
            boolean pendingHigh = seedHigh;
            int start = offsets[row], end = offsets[row + 1];
            for (int at = start; at < end; at++) {
                Effect effect = effects[tuples[at]];
                if (effect.length == 0) continue;
                long[] relation = effect.rows;
                if (pendingHigh) {
                    frontier = advanceMatched(frontier, dots);
                    if (effect.withoutLeadingLow != null) relation = effect.withoutLeadingLow;
                }
                frontier = applyRows(relation, frontier);
                pendingHigh = effect.trailingHigh;
            }
            return (frontier & (pendingHigh ? suffixWithHigh : suffixWithoutHigh)) != 0;
        }

        private void ensureOpen() {
            if (closed) throw new IllegalStateException("prepared regex batch is closed");
        }

        @Override public void close() {
            lifecycle.writeLock().lock();
            try {
                if (closed) return;
                closed = true;
                long cpu = jniHandle, gpu = cudaHandle;
                jniHandle = cudaHandle = 0;
                effects = null;
                tuples = offsets = null;
                lengths = null;
                try { if (cpu != 0) batchClose0(cpu); }
                finally { if (gpu != 0) batchCudaClose0(gpu); }
            } finally { lifecycle.writeLock().unlock(); }
        }
    }

    /**
     * Explicit internal attachment of the optional CUDA provider. The provider uses this
     * plan's numeric Effects and distinct JNI symbols; it does not replace libjava or add
     * another regex parser. No library is loaded implicitly by a String operation.
     */
    public static void loadCudaBatchProvider(String absolutePath) {
        System.load(Objects.requireNonNull(absolutePath, "absolutePath"));
        if (batchCudaAbi0() != BATCH_ABI) {
            throw new UnsupportedOperationException("prepared regex CUDA ABI mismatch");
        }
    }

    public final class Effect {
        private final M3RegexDpPlan plan = M3RegexDpPlan.this;
        private final long[] rows;
        private final long length;

        // Null means the fragment does not start with a low surrogate. Otherwise this
        // is the open relation after omitting that first unit. Rows omit a terminal high.
        private final long[] withoutLeadingLow;
        private final boolean trailingHigh;

        private Effect(long[] rows, long length) { this(rows, length, null, false); }
        private Effect(long[] rows, long length, long[] withoutLeadingLow, boolean trailingHigh) {
            this.rows = rows;
            this.length = length;
            this.withoutLeadingLow = withoutLeadingLow;
            this.trailingHigh = trailingHigh;
        }
        public long length() { return length; }
        public long apply(long frontier) {
            checkBits(frontier);
            long result = applyRows(rows, frontier);
            // Complete-input observation flushes a remaining lone high surrogate.
            return trailingHigh ? advanceMatched(result, dots) : result;
        }

        public Effect then(Effect right, Runnable checkpoint) {
            Objects.requireNonNull(right, "right");
            if (right.plan != plan) throw new IllegalArgumentException("different regex plan");
            long total = Math.addExact(length, right.length);
            check(checkpoint);
            Effect result;
            if (length == 0) {
                result = new Effect(right.rows, total, right.withoutLeadingLow, right.trailingHigh);
            } else if (right.length == 0) {
                result = new Effect(rows, total, withoutLeadingLow, trailingHigh);
            } else {
                long[] suffix = trailingHigh && right.withoutLeadingLow != null
                        ? right.withoutLeadingLow : right.rows;
                long[] joined = appendOpen(rows, suffix, trailingHigh);
                long[] skipped = withoutLeadingLow == null ? null
                        : appendOpen(withoutLeadingLow, suffix, trailingHigh);
                result = new Effect(joined, total, skipped, right.trailingHigh);
            }
            check(checkpoint);
            return result;
        }

        /** Insert the pending-high predicate before the suffix, without completing it. */
        private long[] appendOpen(long[] prefix, long[] suffix, boolean pendingHigh) {
            if (!pendingHigh) return compose(prefix, suffix);
            long[] flushed = prefix.clone();
            advanceRows(flushed, dots);
            return compose(flushed, suffix);
        }

        /** Boundary-aware powers reuse the same ordered composition; callbacks stay external. */
        private Effect repeatedUtf16(long count, long total, Runnable checkpoint) {
            Effect result = identity(), power = this;
            for (long left = count; left != 0; left >>>= 1) {
                check(checkpoint);
                if ((left & 1L) != 0) result = result.then(power, null);
                if (left > 1) power = power.then(power, null);
            }
            check(checkpoint);
            return new Effect(result.rows, total, result.withoutLeadingLow, result.trailingHigh);
        }

        /**
         * Exact power without expanded text. Boundary-free Java retains its checkpoint schedule and three
         * rotating rows. Eligible wide/null-checkpoint powers may use one bounded JNI call,
         * returning one row array. Missing repeat JNI leaves prior Java execution available.
         * Boundary-bearing effects compose in O(states^2 log(count + 1)) work and
         * O(states) live row payload, with O(states log(count + 1)) allocated row payload.
         */
        public Effect repeated(long count, Runnable checkpoint) {
            if (count < 0) throw new IllegalArgumentException("negative repeat count");
            long total = Math.multiplyExact(length, count);
            check(checkpoint);
            if (withoutLeadingLow != null || trailingHigh) {
                return repeatedUtf16(count, total, checkpoint);
            }
            // Preserve every supplied checkpoint on the original Java schedule.
            // A null checkpoint permits one bounded native call; it never owns text.
            if (checkpoint == null && count >= 256 && states >= 16
                    && starred == (universe & ~1L) && needsRowUnion(rows)
                    && !nativeUnavailable && !repeatUnavailable) {
                try {
                    return new Effect(repeat0(rows, count, starred == (universe & ~1L)), total);
                } catch (UnsatisfiedLinkError unavailable) {
                    // Older libjava may expose compose0 but not repeat0. Keep it usable.
                    repeatUnavailable = true;
                }
            }
            long[] result = identityRows(), power = rows.clone(), scratch = new long[states];
            for (long left = count; left != 0; left >>>= 1) {
                check(checkpoint);
                if ((left & 1) != 0) {
                    composeJava(result, power, scratch);
                    long[] swap = result; result = scratch; scratch = swap;
                }
                if (left > 1) {
                    composeJava(power, power, scratch);
                    long[] swap = power; power = scratch; scratch = swap;
                }
            }
            check(checkpoint);
            return new Effect(result, total);
        }
    }

    private long[] compose(long[] left, long[] right) {
        // Native absence is an optimization miss, never an answer of NO_MATCH.
        if (states >= 16 && needsRowUnion(left) && !nativeUnavailable) {
            if (starred == (universe & ~1L) && !structuredUnavailable) {
                try { return composeStructured0(left, right); }
                catch (UnsatisfiedLinkError unavailable) { structuredUnavailable = true; }
            }
            try { return compose0(left, right); }
            catch (UnsatisfiedLinkError unavailable) { nativeUnavailable = true; }
        }
        long[] result = new long[states];
        composeJava(left, right, result);
        return result;
    }

    /** Zero/singleton frontiers need direct row selection, not a JNI round trip. */
    private static boolean needsRowUnion(long[] rows) {
        for (long row : rows) if ((row & (row - 1)) != 0) return true;
        return false;
    }

    private static void composeJava(long[] left, long[] right, long[] result) {
        for (int i = 0; i < left.length; i++) result[i] = applyRows(right, left[i]);
    }

    private static long applyRows(long[] rows, long frontier) {
        long answer = 0;
        while (frontier != 0) {
            answer |= rows[Long.numberOfTrailingZeros(frontier)];
            frontier &= frontier - 1;
        }
        return answer;
    }

    private void checkBits(long bits) {
        if ((bits & ~universe) != 0) throw new IllegalArgumentException("foreign state bits");
    }
    private static void check(Runnable checkpoint) { if (checkpoint != null) checkpoint.run(); }

    // Package visibility permits exact JNI boundary qualification without exposing a JDK API.
    static native long[] compose0(long[] left, long[] right);
    static native long[] composeStructured0(long[] left, long[] right);
    static native long[] repeat0(long[] rows, long count, boolean structured);
    static native int batchAbi0();
    static native long batchCreate0(long[] lanes, long[] dotRows, int states,
            long initial, long accepting, int[] tuples, int[] offsets,
            int prefix, int suffix, long maxImageBytes);
    static native void batchEvaluate0(long handle, int start, int end, byte[] answers, int answerOffset);
    static native void batchClose0(long handle);
    static native int batchCudaAbi0();
    static native long batchCudaCreate0(long[] lanes, long[] dotRows, int states,
            long initial, long accepting, int[] tuples, int[] offsets,
            int prefix, int suffix, long maxImageBytes);
    static native void batchCudaEvaluate0(long handle, int start, int end, byte[] answers, int answerOffset);
    static native void batchCudaClose0(long handle);
}
