// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.internal.mindex;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * RX7: immutable bounded ASCII DFA execution atom derived from a separately verified
 * Synexia MIndexRegexProgram v7 image. This is NOT a parser, image decoder, JDK
 * Matcher replacement, or an authority to decide whether an arbitrary Pattern matches.
 *
 * <p>The caller must prove correspondence between the supplied DFA and its exact
 * upstream v7 pattern/flags/source root before using this candidate. For now,
 * matchesVerified delegates final truth to the authoritative JDK Pattern.
 * All arrays are defensive snapshots; no String/M3String spelling is retained.
 */
public final class M3RX7 {
    private static final int ASCII = 128;
    private static final int MAX_STATES = 4096;
    private static final int MAX_TRANSITION_BYTES = 2 * 1024 * 1024;
    private static final int MAX_TEXT_UNITS = 1_000_000;
    private static final int MAX_PACKED_BYTES = 1 << 20;
    private static final int MAX_TOKEN_LENGTH = 4096;
    private static final long MAX_TOKEN_CELLS = 1_048_576;
    private static final long MAX_TOKEN_WORK = 32L << 20;
    private static final long MAX_TOKEN_INDEX_BYTES = 8L << 20;
    private static final long MIN_NATIVE_CELLS = 65_536;
    private static volatile boolean nativeTokenUnavailable;

    private final String programRoot;
    private final int start;
    private final short[] next;
    private final byte[] accepting;

    private M3RX7(String programRoot, int start, short[] next, byte[] accepting) {
        this.programRoot = programRoot;
        this.start = start;
        this.next = next;
        this.accepting = accepting;
    }

    /**
     * Adapt the exact primitive state lanes of an already-qualified v7
     * MIndexRegexProgram.AsciiDfaImage. The root is the 64-hex-character
     * program-image identity, not an identity of String text or owner ID.
     */
    public static M3RX7 fromAsciiDfa(
            String programRoot, int startState, short[] transitions, byte[] accepting) {
        Objects.requireNonNull(programRoot, "programRoot");
        Objects.requireNonNull(transitions, "transitions");
        Objects.requireNonNull(accepting, "accepting");
        if (!programRoot.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("invalid v7 source-image root");
        }
        int states = accepting.length;
        if (states < 1 || states > MAX_STATES || startState < 0 || startState >= states
                || (long) states * ASCII * Short.BYTES > MAX_TRANSITION_BYTES
                || transitions.length != (long) states * ASCII) {
            throw new IllegalArgumentException("invalid bounded ASCII DFA geometry");
        }
        for (byte flag : accepting) {
            if (flag != 0 && flag != 1) {
                throw new IllegalArgumentException("invalid accepting-state lane");
            }
        }
        for (short state : transitions) {
            if (state < -1 || state >= states) {
                throw new IllegalArgumentException("invalid DFA transition target");
            }
        }
        return new M3RX7(programRoot, startState, transitions.clone(), accepting.clone());
    }

    public String programRoot() { return programRoot; }
    public int stateCount() { return accepting.length; }
    public long primitivePayloadBytes() {
        return (long) next.length * Short.BYTES + accepting.length;
    }

    /**
     * Capture-free full-region ASCII Boolean candidate only: -1 means unsupported,
     * 0 means false and 1 means true. Reads immutable Java String UTF-16 units
     * directly, including M3-backed concatenation/slices; never flattens text.
     * The result is not JDK regex truth until independently verified.
     */
    public int matchesCandidate(CharSequence input) {
        Objects.requireNonNull(input, "input");
        int length = input.length();
        if (length > MAX_TEXT_UNITS) return -1;
        int state = start;
        for (int index = 0; index < length; index++) {
            char unit = input.charAt(index);
            if (unit >= ASCII) return -1;
            if (state >= 0) state = next[state * ASCII + unit];
        }
        return state >= 0 && accepting[state] != 0 ? 1 : 0;
    }

    /** Existing String entry retains its descriptor and M3-backed String behavior. */
    public int matchesCandidate(String input) {
        return matchesCandidate((CharSequence) input);
    }

    /**
     * Consume a range of each disjoint chunk without joining or copying chunks.
     * Offset/length arrays describe the selected UTF-16 slices; matching never
     * resets the state at a chunk boundary. The caller must not mutate inputs
     * concurrently with this operation.
     */
    public int matchesChunksCandidate(CharSequence[] chunks, int[] offsets, int[] lengths) {
        Objects.requireNonNull(chunks, "chunks");
        Objects.requireNonNull(offsets, "offsets");
        Objects.requireNonNull(lengths, "lengths");
        if (chunks.length != offsets.length || chunks.length != lengths.length) {
            throw new IllegalArgumentException("mismatched chunk coordinates");
        }
        long total = 0;
        for (int i = 0; i < chunks.length; i++) {
            CharSequence chunk = Objects.requireNonNull(chunks[i], "chunk");
            Objects.checkFromIndexSize(offsets[i], lengths[i], chunk.length());
            total += lengths[i];
        }
        if (total > MAX_TEXT_UNITS) return -1;
        int state = start;
        for (int i = 0; i < chunks.length; i++) {
            CharSequence chunk = chunks[i];
            int stop = offsets[i] + lengths[i];
            for (int at = offsets[i]; at < stop; at++) {
                char unit = chunk.charAt(at);
                if (unit >= ASCII) return -1;
                if (state >= 0) state = next[state * ASCII + unit];
            }
        }
        return state >= 0 && accepting[state] != 0 ? 1 : 0;
    }

    /**
     * Compose existing DFA transitions over packed ASCII token slices. This is
     * immutable primitive metadata, not token ownership or a second regex engine.
     * Each row is a DFA state, each column a token ID. The vocabulary is observed
     * only during construction and never retained. Null refuses an excessive
     * resource budget; malformed coordinates or non-ASCII tokens throw.
     *
     * <p>Only states that can still reach acceptance set allowed bits, matching
     * guided-decoding semantics. The same transitions support existing-token
     * streams; arbitrary BPE boundaries do not define regex boundaries.</p>
     */
    public TokenIndex prepareTokenIndex(byte[] packedAscii, int[] offsets, int[] lengths) {
        Objects.requireNonNull(packedAscii, "packedAscii");
        Objects.requireNonNull(offsets, "offsets");
        Objects.requireNonNull(lengths, "lengths");
        if (offsets.length != lengths.length) {
            throw new IllegalArgumentException("mismatched token coordinates");
        }
        if (packedAscii.length > MAX_PACKED_BYTES) return null;
        final int tokens = offsets.length;
        final int states = accepting.length;
        final long cells = (long) states * tokens;
        final int words = (int) (((long) tokens + 63) >>> 6);
        final long bytes = cells * Short.BYTES + (long) states * words * Long.BYTES;
        long tokenBytes = 0;
        for (int i = 0; i < tokens; i++) {
            Objects.checkFromIndexSize(offsets[i], lengths[i], packedAscii.length);
            if (lengths[i] == 0 || lengths[i] > MAX_TOKEN_LENGTH) {
                throw new IllegalArgumentException("invalid token length");
            }
            tokenBytes += lengths[i];
        }
        if (cells > MAX_TOKEN_CELLS || bytes > MAX_TOKEN_INDEX_BYTES
                || tokenBytes * states > MAX_TOKEN_WORK) return null;
        for (int i = 0; i < tokens; i++) {
            for (int p = offsets[i], stop = p + lengths[i]; p < stop; p++) {
                if (packedAscii[p] < 0) {
                    throw new IllegalArgumentException("non-ASCII token bytes are not admitted");
                }
            }
        }
        Runtime runtime = Runtime.getRuntime();
        long freeBudget = Math.max(0L,
                runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory()));
        if (bytes > freeBudget / 8) return null;
        try {
            short[] rows = new short[(int) cells];
            if (cells >= MIN_NATIVE_CELLS && !nativeTokenUnavailable) {
                try {
                    buildPacked0(next, packedAscii, offsets, lengths, rows);
                } catch (UnsatisfiedLinkError missing) {
                    nativeTokenUnavailable = true;
                    fillTokenRows(next, packedAscii, offsets, lengths, rows, states, tokens);
                }
            } else {
                fillTokenRows(next, packedAscii, offsets, lengths, rows, states, tokens);
            }
            byte[] useful = usefulStates();
            long[] allowed = new long[states * words];
            for (int state = 0; state < states; state++) {
                int base = state * tokens;
                int maskBase = state * words;
                for (int first = 0, word = 0; first < tokens; first += Long.SIZE, word++) {
                    long mask = 0L;
                    int width = Math.min(Long.SIZE, tokens - first);
                    for (int bit = 0; bit < width; bit++) {
                        int destination = rows[base + first + bit];
                        if (destination >= 0 && useful[destination] != 0) {
                            mask |= 1L << bit;
                        }
                    }
                    // One store per complete/partial word; high padding bits remain zero.
                    allowed[maskBase + word] = mask;
                }
            }
            return new TokenIndex(rows, allowed, accepting, start, states, tokens, words);
        } catch (OutOfMemoryError unavailable) {
            return null;
        }
    }

    private static void fillTokenRows(short[] dfa, byte[] packed, int[] offsets,
            int[] lengths, short[] rows, int states, int tokens) {
        for (int state = 0; state < states; state++) {
            int base = state * tokens;
            for (int token = 0; token < tokens; token++) {
                int cursor = state;
                for (int i = offsets[token], stop = i + lengths[token]; i < stop; i++) {
                    if (cursor < 0) break;
                    cursor = dfa[cursor * ASCII + packed[i]];
                }
                rows[base + token] = (short) cursor;
            }
        }
    }

    /** Reverse reachability over primitive coordinate lanes: no graph entry objects. */
    private byte[] usefulStates() {
        int states = accepting.length;
        int[] incoming = new int[states];
        int edges = 0;
        for (int i = 0; i < next.length; i++) {
            int target = next[i];
            if (target >= 0) { incoming[target]++; edges++; }
        }
        int[] offsets = new int[states + 1];
        for (int i = 0; i < states; i++) offsets[i + 1] = offsets[i] + incoming[i];
        int[] cursor = Arrays.copyOf(offsets, states);
        int[] previous = new int[edges];
        for (int i = 0; i < next.length; i++) {
            int target = next[i];
            if (target >= 0) previous[cursor[target]++] = i / ASCII;
        }
        byte[] useful = new byte[states];
        int[] queue = new int[states];
        int head = 0, tail = 0;
        for (int i = 0; i < states; i++) {
            if (accepting[i] != 0) { useful[i] = 1; queue[tail++] = i; }
        }
        while (head < tail) {
            int current = queue[head++];
            for (int i = offsets[current]; i < offsets[current + 1]; i++) {
                int source = previous[i];
                if (useful[source] == 0) { useful[source] = 1; queue[tail++] = source; }
            }
        }
        return useful;
    }

    // java.base's libjava owns the JNI kernel. Missing older binaries use exact Java rows.
    private static native void buildPacked0(short[] dfa, byte[] packed,
            int[] offsets, int[] lengths, short[] rows);

    /** Primitive token-transition index. Stores no tokens, strings or Entry objects. */
    public static final class TokenIndex {
        private final short[] rows;
        private final long[] allowed;
        private final byte[] accepting;
        private final int start;
        private final int states;
        private final int tokens;
        private final int words;

        private TokenIndex(short[] rows, long[] allowed, byte[] accepting, int start,
                int states, int tokens, int words) {
            this.rows = rows;
            this.allowed = allowed;
            this.accepting = accepting;
            this.start = start;
            this.states = states;
            this.tokens = tokens;
            this.words = words;
        }

        public int tokenCount() { return tokens; }
        public int initialState() { return start; }
        public boolean isFinal(int state) {
            return accepting[Objects.checkIndex(state, states)] != 0;
        }
        public int nextState(int state, int tokenId) {
            Objects.checkIndex(tokenId, tokens);
            if (state == -1) return -1;
            return rows[Objects.checkIndex(state, states) * tokens + tokenId];
        }
        public long allowedMaskWord(int state, int word) {
            Objects.checkIndex(state, states);
            return allowed[state * words + Objects.checkIndex(word, words)];
        }
        public int allowedMaskWords() { return words; }
        public long retainedPrimitiveBytes() {
            return (long) rows.length * Short.BYTES + (long) allowed.length * Long.BYTES;
        }
        public boolean matches(int[] tokenIds) {
            Objects.requireNonNull(tokenIds, "tokenIds");
            int state = start;
            for (int id : tokenIds) state = nextState(state, id);
            return state >= 0 && isFinal(state);
        }
    }

    /**
     * Authoritative compatibility oracle, deliberately not an optimization. Refuses
     * a disagreed candidate instead of suppressing a legitimate JDK match.
     */
    public boolean matchesVerified(String input, Pattern authoritativePattern) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(authoritativePattern, "authoritativePattern");
        boolean truth = authoritativePattern.matcher(input).matches();
        int candidate = matchesCandidate(input);
        if (candidate >= 0 && (candidate == 1) != truth) {
            throw new IllegalStateException("M3RX7 DFA disagrees with JDK Pattern");
        }
        return truth;
    }
}
