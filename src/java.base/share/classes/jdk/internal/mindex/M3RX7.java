// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.internal.mindex;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;
import jdk.internal.access.JavaLangAccess;
import jdk.internal.access.SharedSecrets;

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
    private static final int MAX_M3_TOKEN_SEQUENCE = 65_536;
    private static final int MAX_M3_TOKEN_VOCABULARY = 4096;
    private static final long MAX_TOKEN_CELLS = 1_048_576;
    private static final long MAX_TOKEN_WORK = 32L << 20;
    private static final long MAX_TOKEN_INDEX_BYTES = 8L << 20;
    private static final long MIN_NATIVE_CELLS = 65_536;
    private static final int MAX_LEXICAL_STATES = 256;
    private static final int MAX_LEXICAL_EDGES = 4096;
    private static final long MAX_LEXICAL_WORK = 32L << 20;
    private static volatile boolean nativeTokenUnavailable;
    private static final int TOKEN_NATIVE_PAGE = 4096;
    private static volatile boolean nativeAdvanceUnavailable;

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
                for (int token = 0; token < tokens; token++) {
                    int destination = rows[base + token];
                    if (destination >= 0 && useful[destination] != 0) {
                        allowed[maskBase + (token >>> 6)] |= 1L << (token & 63);
                    }
                }
            }
            return new TokenIndex(rows, allowed, accepting, start, states, tokens, words);
        } catch (OutOfMemoryError unavailable) {
            return null;
        }
    }

    /**
     * Receive an already-compiled token-symbol DFA into the existing index owner.
     * A column is one complete lexical symbol, not the spelling of a storage
     * piece, BPE token, or UTF-16 character. Overlapping token predicates must be
     * classified into disjoint symbols by the compiler before this call.
     *
     * <p>Both roots are lowercase SHA-256 identities. The alphabet root must bind
     * token classification, dictionary identity/version and context policy. The
     * caller remains responsible for proving the supplied automaton's semantics.
     * No lexer, parser, captures, or automatic comment filtering is introduced.
     * Arrays are snapshotted. Null means the bounded resource budget was refused;
     * malformed inputs throw. Do not mutate inputs during construction.</p>
     */
    public static TokenIndex fromTokenDfa(String programRoot, String alphabetRoot,
            int startState, int tokenCount, short[] transitions, byte[] finalStates) {
        requireRoot(programRoot);
        requireRoot(alphabetRoot);
        Objects.requireNonNull(transitions, "transitions");
        Objects.requireNonNull(finalStates, "finalStates");
        int states = finalStates.length;
        long cells = (long) states * tokenCount;
        if (states < 1 || states > MAX_STATES || tokenCount < 0
                || startState < 0 || startState >= states || cells != transitions.length) {
            throw new IllegalArgumentException("invalid token-symbol DFA geometry");
        }
        int words = (int) (((long) tokenCount + 63) >>> 6);
        // Include reverse edges and per-state scratch, not just retained rows.
        long peak = cells * (Short.BYTES + Integer.BYTES)
                + (long) states * words * Long.BYTES + 32L * states + Integer.BYTES;
        if (cells > MAX_TOKEN_CELLS || peak > MAX_TOKEN_INDEX_BYTES) return null;
        Runtime runtime = Runtime.getRuntime();
        long available = Math.max(0L,
                runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory()));
        if (peak > available / 8) return null;
        try {
            short[] rows = transitions.clone();
            byte[] flags = finalStates.clone();
            for (short destination : rows) {
                if (destination < -1 || destination >= states) {
                    throw new IllegalArgumentException("invalid token-symbol destination");
                }
            }
            for (byte flag : flags) {
                if (flag != 0 && flag != 1) {
                    throw new IllegalArgumentException("invalid token-symbol final flag");
                }
            }
            byte[] useful = usefulStates(rows, flags, tokenCount);
            long[] allowed = new long[states * words];
            for (int state = 0; state < states; state++) {
                for (int token = 0; token < tokenCount; token++) {
                    int destination = rows[state * tokenCount + token];
                    if (destination >= 0 && useful[destination] != 0) {
                        allowed[state * words + (token >>> 6)] |= 1L << (token & 63);
                    }
                }
            }
            return new TokenIndex(rows, allowed, flags, startState, states,
                    tokenCount, words, programRoot, alphabetRoot);
        } catch (OutOfMemoryError unavailable) {
            return null;
        }
    }

    private static void requireRoot(String root) {
        Objects.requireNonNull(root, "root");
        if (root.length() != 64) throw new IllegalArgumentException("invalid source root");
        for (int i = 0; i < root.length(); i++) {
            char c = root.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
                throw new IllegalArgumentException("invalid source root");
            }
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
        return usefulStates(next, accepting, ASCII);
    }

    private static byte[] usefulStates(short[] next, byte[] accepting, int columns) {
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
            if (target >= 0) previous[cursor[target]++] = i / columns;
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

    private static native int advanceTokens0(short[] rows, int tokens, int state,
            int[] tokenIds, int offset, int length);

    /** Primitive token-transition index. No spelling payload or per-token objects. */
    public static final class TokenIndex {
        private final short[] rows;
        private final long[] allowed;
        private final byte[] accepting;
        private final int start;
        private final int states;
        private final int tokens;
        private final int words;
        private final String programRoot;
        private final String alphabetRoot;

        private TokenIndex(short[] rows, long[] allowed, byte[] accepting, int start,
                int states, int tokens, int words) {
            this(rows, allowed, accepting, start, states, tokens, words, null, null);
        }

        private TokenIndex(short[] rows, long[] allowed, byte[] accepting, int start,
                int states, int tokens, int words, String programRoot, String alphabetRoot) {
            this.programRoot = programRoot;
            this.alphabetRoot = alphabetRoot;
            this.rows = rows;
            this.allowed = allowed;
            this.accepting = accepting;
            this.start = start;
            this.states = states;
            this.tokens = tokens;
            this.words = words;
        }

        public int tokenCount() { return tokens; }
        public int stateCount() { return states; }
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
        /** Program/alphabet provenance; null for the older spelling-based index. */
        public String programRoot() { return programRoot; }
        public String alphabetRoot() { return alphabetRoot; }

        /**
         * Consume an int-array page without resetting the frontier. Return -1 for
         * the dead state. An empty page preserves even the dead state. Every ID
         * is checked, including IDs after a dead transition; no page is retained.
         * Callers must not mutate the input during a call. Source UTF-16 spans,
         * long stream positions and lexer continuation state belong to the caller.
         */
        public int advance(int state, int[] tokenIds, int offset, int length) {
            Objects.requireNonNull(tokenIds, "tokenIds");
            Objects.checkFromIndexSize(offset, length, tokenIds.length);
            if (state != -1) Objects.checkIndex(state, states);
            int initialForRetry = state;
            int end = offset + length;
            for (int i = offset; i < end; i++) Objects.checkIndex(tokenIds[i], tokens);
            if (length >= TOKEN_NATIVE_PAGE && !nativeAdvanceUnavailable) {
                try {
                    for (int at = offset; at < end; at += Math.min(TOKEN_NATIVE_PAGE, end - at)) {
                        state = advanceTokens0(rows, tokens, state, tokenIds,
                                at, Math.min(TOKEN_NATIVE_PAGE, end - at));
                    }
                    return state;
                } catch (UnsatisfiedLinkError missing) {
                    nativeAdvanceUnavailable = true;
                } catch (OutOfMemoryError unavailable) {
                    // Optional JNI metadata copies must not prevent exact Java execution.
                }
                // A failing later JNI page must not replay the prefix from its new state.
                return advanceJava(initialForRetry, tokenIds, offset, length);
            }
            return advanceJava(state, tokenIds, offset, length);
        }

        /** Reject mixing IDs from independently versioned token alphabets. */
        public int advance(String expectedAlphabetRoot, int state,
                int[] tokenIds, int offset, int length) {
            Objects.requireNonNull(expectedAlphabetRoot, "expectedAlphabetRoot");
            if (!expectedAlphabetRoot.equals(alphabetRoot)) {
                throw new IllegalArgumentException("token alphabet identity mismatch");
            }
            return advance(state, tokenIds, offset, length);
        }

        private int advanceJava(int state, int[] tokenIds, int offset, int length) {
            for (int i = offset, end = offset + length; i < end; i++) {
                state = nextState(state, tokenIds[i]);
            }
            return state;
        }

        /**
         * Prepare exact acceptance reachability through this vocabulary's token edges.
         * Existing allowedMaskWord keeps its character-reachability contract. This
         * separate optional filter excludes prefixes that cannot finish using the
         * admitted token vocabulary, including incomplete vocabularies. No text is retained.
         * Null means the optional preparation could not obtain its resource budget.
         */
        public VocabularyFilter prepareVocabularyFilter() {
            // Include worst-case reverse-CSR edges, offsets, cursors and queue;
            // admission covers temporary array payload as well as retained masks.
            long bytes = (long) allowed.length * Long.BYTES
                    + (long) rows.length * Integer.BYTES
                    + (3L * states + 1) * Integer.BYTES + states;
            Runtime runtime = Runtime.getRuntime();
            long freeBudget = Math.max(0L,
                    runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory()));
            if (bytes > freeBudget / 8) return null;
            try {
                byte[] useful = M3RX7Transitions.useful(rows, states, tokens, accepting);
                return new VocabularyFilter(useful,
                        M3RX7Transitions.masks(rows, states, tokens, useful), tokens, words);
            } catch (OutOfMemoryError unavailable) {
                return null;
            }
        }

        /** All-entry-state effect of the whole token sequence, without retaining its IDs. */
        public Transfer transfer(int[] tokenIds) {
            Objects.requireNonNull(tokenIds, "tokenIds");
            return transfer(tokenIds, 0, tokenIds.length);
        }

        /**
         * All-entry-state effect of a selected token-ID range. IDs are validated even
         * behind dead transitions. Null refuses more than 32 Mi state/token steps or
         * unavailable allocation; source IDs and vocabulary spelling are never retained.
         */
        public Transfer transfer(int[] tokenIds, int offset, int length) {
            try {
                short[] effect = M3RX7Transitions.tokens(rows, states, tokens,
                        tokenIds, offset, length);
                return effect == null ? null : new Transfer(accepting, start, effect, length);
            } catch (OutOfMemoryError unavailable) {
                return null;
            }
        }
        /**
         * TimSort-style adaptive execution over natural runs of identical canonical token IDs.
         * Cold or short runs stay on the direct transition path. A sufficiently long repeated run
         * is promoted to an all-entry-state transfer and exponentiated without expanding the run.
         * Every ID is validated before execution, so dead-state shortcuts never hide invalid input.
         */
        public int advanceAdaptive(int state, int[] tokenIds, int offset, int length) {
            Objects.requireNonNull(tokenIds, "tokenIds");
            Objects.checkFromIndexSize(offset, length, tokenIds.length);
            if (state != -1) Objects.checkIndex(state, states);
            int end = offset + length;
            for (int index = offset; index < end; index++) {
                Objects.checkIndex(tokenIds[index], tokens);
            }
            for (int index = offset; index < end && state >= 0; ) {
                int token = tokenIds[index];
                int runEnd = index + 1;
                while (runEnd < end && tokenIds[runEnd] == token) runEnd++;
                int runLength = runEnd - index;
                int levels = Integer.SIZE - Integer.numberOfLeadingZeros(runLength);
                long transferWork = (long) states * (levels + 1L);
                if (runLength >= 32 && runLength > transferWork) {
                    short[] single = new short[states];
                    for (int source = 0; source < states; source++) {
                        single[source] = rows[source * tokens + token];
                    }
                    short[] repeated = M3RX7Transitions.repeat(single, runLength);
                    state = repeated[state];
                } else {
                    for (int at = index; at < runEnd && state >= 0; at++) {
                        state = rows[state * tokens + token];
                    }
                }
                index = runEnd;
            }
            return state;
        }

        public boolean matches(int[] tokenIds) {
            Objects.requireNonNull(tokenIds, "tokenIds");
            int state = advance(start, tokenIds, 0, tokenIds.length);
            return state >= 0 && isFinal(state);
        }

        /**
         * Prepare exact acceptance reachability through this vocabulary's token edges.
         * Existing allowedMaskWord keeps its character-reachability contract. This
         * separate optional filter excludes prefixes that cannot finish using the
         * admitted token vocabulary, including incomplete vocabularies. No text is retained.
         * Null means the optional preparation could not obtain its resource budget.
         */
        public VocabularyFilter prepareVocabularyFilter() {
            // Include worst-case reverse-CSR edges, offsets, cursors and queue;
            // admission covers temporary array payload as well as retained masks.
            long bytes = (long) allowed.length * Long.BYTES
                    + (long) rows.length * Integer.BYTES
                    + (3L * states + 1) * Integer.BYTES + states;
            Runtime runtime = Runtime.getRuntime();
            long freeBudget = Math.max(0L,
                    runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory()));
            if (bytes > freeBudget / 8) return null;
            try {
                byte[] useful = M3RX7Transitions.useful(rows, states, tokens, accepting);
                return new VocabularyFilter(useful,
                        M3RX7Transitions.masks(rows, states, tokens, useful), tokens, words);
            } catch (OutOfMemoryError unavailable) {
                return null;
            }
        }

        /** All-entry-state effect of the whole token sequence, without retaining its IDs. */
        public Transfer transfer(int[] tokenIds) {
            Objects.requireNonNull(tokenIds, "tokenIds");
            return transfer(tokenIds, 0, tokenIds.length);
        }

        /**
         * All-entry-state effect of a selected token-ID range. IDs are validated even
         * behind dead transitions. Null refuses more than 32 Mi state/token steps or
         * unavailable allocation; source IDs and vocabulary spelling are never retained.
         */
        public Transfer transfer(int[] tokenIds, int offset, int length) {
            try {
                short[] effect = M3RX7Transitions.tokens(rows, states, tokens,
                        tokenIds, offset, length);
                return effect == null ? null : new Transfer(accepting, start, effect, length);
            } catch (OutOfMemoryError unavailable) {
                return null;
            }
        }
    }

    /** Immutable vocabulary-conditioned masks, distinct from character-reachable masks. */
    public static final class VocabularyFilter {
        private final byte[] useful;
        private final long[] allowed;
        private final int tokens;
        private final int words;

        private VocabularyFilter(byte[] useful, long[] allowed, int tokens, int words) {
            this.useful = useful;
            this.allowed = allowed;
            this.tokens = tokens;
            this.words = words;
        }

        public int tokenCount() { return tokens; }
        public int allowedMaskWords() { return words; }
        public boolean canReachFinal(int state) {
            if (state == -1) return false;
            return useful[Objects.checkIndex(state, useful.length)] != 0;
        }
        public long allowedMaskWord(int state, int word) {
            Objects.checkIndex(state, useful.length);
            return allowed[state * words + Objects.checkIndex(word, words)];
        }
        public boolean allows(int state, int tokenId) {
            Objects.checkIndex(tokenId, tokens);
            if (state == -1) return false;
            return (allowedMaskWord(state, tokenId >>> 6) & (1L << (tokenId & 63))) != 0;
        }
        public long retainedPrimitiveBytes() {
            return useful.length + (long) allowed.length * Long.BYTES;
        }
    }

    /**
     * Immutable all-entry-state effect, tied to one exact RX7 owner's acceptance
     * snapshot. The private snapshot is unique to each fromAsciiDfa result, so it
     * supplies owner identity without retaining that owner's original DFA table.
     * Composition and repetition operate on state rows, never on expanded tokens.
     */
    public static final class Transfer {
        private final byte[] accepting;
        private final int start;
        private final short[] rows;
        private final long length;

        private Transfer(byte[] accepting, int start, short[] rows, long length) {
            this.accepting = accepting;
            this.start = start;
            this.rows = rows;
            this.length = length;
        }

        public long length() { return length; }
        /** Array payload bytes for effect rows and shared immutable acceptance flags. */
        public long retainedPrimitiveBytes() {
            return (long) rows.length * Short.BYTES + accepting.length;
        }
        public int nextState(int state) {
            if (state == -1) return -1;
            return rows[Objects.checkIndex(state, rows.length)];
        }
        public boolean matches() {
            int state = rows[start];
            return state >= 0 && accepting[state] != 0;
        }

        /** Consume this effect, then right; equal root strings do not imply owner identity. */
        public Transfer then(Transfer right) {
            Objects.requireNonNull(right, "right");
            if (right.accepting != accepting || right.start != start) {
                throw new IllegalArgumentException("different RX7 owners");
            }
            long total = Math.addExact(length, right.length);
            return new Transfer(accepting, start,
                    M3RX7Transitions.compose(rows, right.rows), total);
        }

        /** Nonnegative repetition, including the exact empty-sequence identity at zero. */
        public Transfer repeated(long count) {
            if (count < 0) throw new IllegalArgumentException("negative repeat count");
            long total = Math.multiplyExact(length, count);
            return new Transfer(accepting, start, M3RX7Transitions.repeat(rows, count), total);
        }
    }

    /**
     * Receive a compiler-produced lexical-token NFA into this existing runtime owner.
     * State offsets describe contiguous edge ranges; each edge carries a target and
     * a conjunction of kind, spelling-ID, required-flag and forbidden-flag predicates.
     * Kind -1 and spelling ID -1 are wildcards. Kind -2 denotes an epsilon edge and
     * must carry no other predicate. Epsilon edges provide alternation and repetition
     * through the supplied topology; this method does not parse text or regex syntax.
     *
     * <p>Spelling IDs are exact caller-established identities, not hashes. The compiler
     * and caller must use the same canonical dictionary namespace and generation for
     * this plan and its input columns. The program root identifies that qualified
     * program/context; its format check is not proof of provenance or equivalence.
     * Input columns and source spellings are never retained. No v7 wire opcode or
     * public Pattern/Matcher contract is reinterpreted.</p>
     *
     * @throws IllegalArgumentException for malformed or over-budget topology
     */
    public static LexicalPlan fromLexicalNfa(String programRoot, int startState,
            int[] stateOffsets, int[] targets, int[] kinds, long[] spellingIds,
            int[] requiredFlags, int[] forbiddenFlags, byte[] accepting) {
        Objects.requireNonNull(programRoot, "programRoot");
        Objects.requireNonNull(stateOffsets, "stateOffsets");
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(kinds, "kinds");
        Objects.requireNonNull(spellingIds, "spellingIds");
        Objects.requireNonNull(requiredFlags, "requiredFlags");
        Objects.requireNonNull(forbiddenFlags, "forbiddenFlags");
        Objects.requireNonNull(accepting, "accepting");
        if (!programRoot.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("invalid lexical program root");
        }
        int states = accepting.length;
        int edges = targets.length;
        if (states < 1 || states > MAX_LEXICAL_STATES
                || startState < 0 || startState >= states || edges > MAX_LEXICAL_EDGES
                || stateOffsets.length != states + 1 || kinds.length != edges
                || spellingIds.length != edges || requiredFlags.length != edges
                || forbiddenFlags.length != edges) {
            throw new IllegalArgumentException("invalid lexical NFA geometry");
        }
        // Snapshot first, then validate the exact arrays that will be executed.
        return new LexicalPlan(programRoot, startState, stateOffsets.clone(),
                targets.clone(), kinds.clone(), spellingIds.clone(), requiredFlags.clone(),
                forbiddenFlags.clone(), accepting.clone());
    }

    /**
     * Bounded predicate NFA over pretokenized primitive columns, with no token objects.
     * All input arrays must remain stable during a call. This is an exact lexical
     * automaton in its declared domain, not an assertion about Java regex semantics.
     */
    public static final class LexicalPlan {
        private static final int EPSILON = -2;
        private static final int ANY = -1;
        private final String programRoot;
        private final int start;
        private final int states;
        private final int words;
        private final int[] offsets;
        private final int[] targets;
        private final int[] kinds;
        private final long[] spellingIds;
        private final int[] requiredFlags;
        private final int[] forbiddenFlags;
        private final byte[] accepting;
        private final long[] closures;
        private final long[] acceptanceOrigins;

        private LexicalPlan(String programRoot, int start, int[] offsets,
                int[] targets, int[] kinds, long[] spellingIds, int[] requiredFlags,
                int[] forbiddenFlags, byte[] accepting) {
            this.programRoot = programRoot;
            this.start = start;
            this.states = accepting.length;
            this.words = (states + 63) >>> 6;
            this.offsets = offsets;
            this.targets = targets;
            this.kinds = kinds;
            this.spellingIds = spellingIds;
            this.requiredFlags = requiredFlags;
            this.forbiddenFlags = forbiddenFlags;
            this.accepting = accepting;
            validateTopology();
            this.closures = epsilonClosures();
            this.acceptanceOrigins = acceptanceOrigins();
        }

        private void validateTopology() {
            if (offsets[0] != 0 || offsets[states] != targets.length) {
                throw new IllegalArgumentException("invalid lexical edge coordinates");
            }
            for (int state = 0; state < states; state++) {
                if (offsets[state] < 0 || offsets[state] > offsets[state + 1]
                        || offsets[state + 1] > targets.length
                        || accepting[state] < 0 || accepting[state] > 1) {
                    throw new IllegalArgumentException("invalid lexical state lane");
                }
            }
            for (int edge = 0; edge < targets.length; edge++) {
                if (targets[edge] < 0 || targets[edge] >= states || kinds[edge] < EPSILON
                        || spellingIds[edge] < ANY
                        || (requiredFlags[edge] & forbiddenFlags[edge]) != 0
                        || (kinds[edge] == EPSILON && (spellingIds[edge] != ANY
                            || requiredFlags[edge] != 0 || forbiddenFlags[edge] != 0))) {
                    throw new IllegalArgumentException("invalid lexical edge predicate");
                }
            }
        }

        private long[] epsilonClosures() {
            long[] rows = new long[states * words];
            int[] queue = new int[states];
            for (int state = 0; state < states; state++) {
                int base = state * words;
                rows[base + (state >>> 6)] |= 1L << (state & 63);
                int head = 0;
                int tail = 1;
                queue[0] = state;
                while (head < tail) {
                    int current = queue[head++];
                    for (int edge = offsets[current]; edge < offsets[current + 1]; edge++) {
                        if (kinds[edge] != EPSILON) continue;
                        int target = targets[edge];
                        int word = base + (target >>> 6);
                        long bit = 1L << (target & 63);
                        if ((rows[word] & bit) == 0) {
                            rows[word] |= bit;
                            queue[tail++] = target;
                        }
                    }
                }
            }
            return rows;
        }

        public String programRoot() { return programRoot; }
        public int stateCount() { return states; }
        public int edgeCount() { return targets.length; }

        /** Primitive plan payload; excludes array headers, this object and the root string. */
        public long retainedPrimitiveBytes() {
            return (long) offsets.length * Integer.BYTES
                    + (long) targets.length * (4 * Integer.BYTES + Long.BYTES)
                    + accepting.length + ((long) closures.length
                        + acceptanceOrigins.length) * Long.BYTES;
        }

        /** Fresh caller-owned epsilon-closed frontier for incremental token partitions. */
        public long[] initialStates() {
            return Arrays.copyOfRange(closures, start * words, (start + 1) * words);
        }

        private void validateFrontier(long[] frontier) {
            Objects.requireNonNull(frontier, "frontier");
            if (frontier.length != words || ((states & 63) != 0
                    && (frontier[words - 1] >>> (states & 63)) != 0)) {
                throw new IllegalArgumentException("invalid lexical frontier");
            }
        }

        /** Prepare accepting origins once, including non-epsilon-closed frontiers. */
        private long[] acceptanceOrigins() {
            long[] origins = new long[words];
            long[] finalStates = new long[words];
            for (int state = 0; state < states; state++) {
                if (accepting[state] != 0) {
                    finalStates[state >>> 6] |= 1L << (state & 63);
                }
            }
            for (int state = 0; state < states; state++) {
                int base = state * words;
                for (int word = 0; word < words; word++) {
                    if ((closures[base + word] & finalStates[word]) != 0) {
                        origins[state >>> 6] |= 1L << (state & 63);
                        break;
                    }
                }
            }
            return origins;
        }

        /** Acceptance of the current frontier, including epsilon-reachable final states. */
        public boolean accepts(long[] frontier) {
            validateFrontier(frontier);
            for (int word = 0; word < words; word++) {
                if ((frontier[word] & acceptanceOrigins[word]) != 0) return true;
            }
            return false;
        }

        private void validateInput(int[] tokenKinds, long[] tokenSpellingIds,
                int[] tokenFlags, int from, int to, boolean finding) {
            Objects.requireNonNull(tokenKinds, "tokenKinds");
            Objects.requireNonNull(tokenSpellingIds, "tokenSpellingIds");
            Objects.requireNonNull(tokenFlags, "tokenFlags");
            if (tokenKinds.length != tokenSpellingIds.length
                    || tokenKinds.length != tokenFlags.length) {
                throw new IllegalArgumentException("mismatched lexical token columns");
            }
            Objects.checkFromToIndex(from, to, tokenKinds.length);
            long length = to - from;
            long perToken = (long) targets.length * (finding ? states : words) + states;
            if (length > MAX_TEXT_UNITS || length * perToken > MAX_LEXICAL_WORK) {
                throw new IllegalArgumentException("lexical execution budget exceeded");
            }
            for (int token = from; token < to; token++) {
                if (tokenKinds[token] < 0 || tokenSpellingIds[token] < 0) {
                    throw new IllegalArgumentException("negative lexical token identity");
                }
            }
        }

        private boolean matchesEdge(int edge, int kind, long spellingId, int flags) {
            return kinds[edge] != EPSILON && (kinds[edge] == ANY || kinds[edge] == kind)
                    && (spellingIds[edge] == ANY || spellingIds[edge] == spellingId)
                    && (flags & requiredFlags[edge]) == requiredFlags[edge]
                    && (flags & forbiddenFlags[edge]) == 0;
        }

        private void unionClosure(long[] destination, int target) {
            int base = target * words;
            for (int word = 0; word < words; word++) {
                destination[word] |= closures[base + word];
            }
        }

        private void step(long[] frontier, long[] next, int kind, long spellingId, int flags) {
            Arrays.fill(next, 0L);
            for (int word = 0; word < words; word++) {
                long bits = frontier[word];
                while (bits != 0) {
                    int state = (word << 6) + Long.numberOfTrailingZeros(bits);
                    for (int edge = offsets[state]; edge < offsets[state + 1]; edge++) {
                        if (matchesEdge(edge, kind, spellingId, flags)) {
                            unionClosure(next, targets[edge]);
                        }
                    }
                    bits &= bits - 1;
                }
            }
        }

        /**
         * Advance a caller-owned frontier through one token partition. The output is
         * published only after validation and successful execution. No token column is
         * retained or modified; aliasing the spelling-ID column as the frontier is refused.
         * Frontiers are program-local: callers must not mix different plans' state numbers.
         */
        public void advance(long[] frontier, int[] tokenKinds, long[] tokenSpellingIds,
                int[] tokenFlags, int from, int to) {
            validateFrontier(frontier);
            validateInput(tokenKinds, tokenSpellingIds, tokenFlags, from, to, false);
            if (frontier == tokenSpellingIds) {
                throw new IllegalArgumentException("frontier aliases token identities");
            }
            long[] current = new long[words];
            for (int word = 0; word < words; word++) {
                long bits = frontier[word];
                while (bits != 0) {
                    unionClosure(current, (word << 6) + Long.numberOfTrailingZeros(bits));
                    bits &= bits - 1;
                }
            }
            long[] next = new long[words];
            for (int token = from; token < to; token++) {
                step(current, next, tokenKinds[token], tokenSpellingIds[token], tokenFlags[token]);
                long[] swap = current;
                current = next;
                next = swap;
            }
            System.arraycopy(current, 0, frontier, 0, words);
        }

        /** Exact full-region match of the selected lexical tokens. */
        public boolean matches(int[] tokenKinds, long[] tokenSpellingIds,
                int[] tokenFlags, int from, int to) {
            long[] frontier = initialStates();
            advance(frontier, tokenKinds, tokenSpellingIds, tokenFlags, from, to);
            return accepts(frontier);
        }

        /** Exact full-sequence match; token boundaries are semantically significant. */
        public boolean matches(int[] tokenKinds, long[] tokenSpellingIds, int[] tokenFlags) {
            Objects.requireNonNull(tokenKinds, "tokenKinds");
            return matches(tokenKinds, tokenSpellingIds, tokenFlags, 0, tokenKinds.length);
        }

        private void seedStarts(int[] starts, int target, int origin) {
            int base = target * words;
            for (int word = 0; word < words; word++) {
                long bits = closures[base + word];
                while (bits != 0) {
                    int state = (word << 6) + Long.numberOfTrailingZeros(bits);
                    if (origin < starts[state]) starts[state] = origin;
                    bits &= bits - 1;
                }
            }
        }

        private void stepStarts(int[] current, int[] next, int kind, long spellingId, int flags) {
            Arrays.fill(next, Integer.MAX_VALUE);
            for (int state = 0; state < states; state++) {
                if (current[state] == Integer.MAX_VALUE) continue;
                for (int edge = offsets[state]; edge < offsets[state + 1]; edge++) {
                    if (matchesEdge(edge, kind, spellingId, flags)) {
                        seedStarts(next, targets[edge], current[state]);
                    }
                }
            }
        }

        /**
         * Find the leftmost-longest lexical match in [from,to), returning packed token
         * start/end coordinates, or -1 when absent. Empty matches are admitted and a
         * longer match at that same start wins. Alternatives have no capture/priority
         * semantics; this explicit policy does not emulate Matcher.find().
         *
         * <p>Only the earliest origin for each NFA state is needed: equal states have
         * the same future language. Two primitive origin arrays bound working storage
         * independently of the number of possible matching starts.</p>
         */
        public long find(int[] tokenKinds, long[] tokenSpellingIds,
                int[] tokenFlags, int from, int to) {
            validateInput(tokenKinds, tokenSpellingIds, tokenFlags, from, to, true);
            int[] current = new int[states];
            int[] next = new int[states];
            Arrays.fill(current, Integer.MAX_VALUE);
            int first = Integer.MAX_VALUE;
            int last = -1;
            for (int position = from; ; position++) {
                seedStarts(current, start, position);
                for (int state = 0; state < states; state++) {
                    if (accepting[state] != 0 && current[state] != Integer.MAX_VALUE
                            && current[state] <= first) {
                        first = current[state];
                        last = position;
                    }
                }
                if (position == to) break;
                stepStarts(current, next, tokenKinds[position], tokenSpellingIds[position],
                        tokenFlags[position]);
                int[] swap = current;
                current = next;
                next = swap;
            }
            return last < 0 ? -1L : packRange(first, last);
        }

        public long find(int[] tokenKinds, long[] tokenSpellingIds, int[] tokenFlags) {
            Objects.requireNonNull(tokenKinds, "tokenKinds");
            return find(tokenKinds, tokenSpellingIds, tokenFlags, 0, tokenKinds.length);
        }
    }

    private static long packRange(int from, int to) {
        return ((long) from << 32) | Integer.toUnsignedLong(to);
    }

    /** Decode a nonnegative packed half-open token or UTF-16 range. */
    public static int rangeStart(long range) {
        int from = (int) (range >>> 32);
        int to = (int) range;
        if (range < 0 || to < from) throw new IllegalArgumentException("invalid packed range");
        return from;
    }

    /** Decode a nonnegative packed half-open token or UTF-16 range. */
    public static int rangeEnd(long range) {
        rangeStart(range);
        return (int) range;
    }

    /**
     * Map a token interval to original UTF-16 coordinates without reading or joining
     * text. Coordinates must be ordered, nonoverlapping and within sourceLength; gaps
     * may contain trivia. A nonempty result includes gaps between its first/last tokens.
     * An empty result uses the next token's offset, or sourceLength at sequence end.
     * The absent sentinel -1 passes through after coordinate validation.
     */
    public static long utf16Range(long tokenRange, int[] offsets, int[] lengths,
            int sourceLength) {
        Objects.requireNonNull(offsets, "offsets");
        Objects.requireNonNull(lengths, "lengths");
        if (offsets.length != lengths.length || sourceLength < 0
                || offsets.length > MAX_TEXT_UNITS) {
            throw new IllegalArgumentException("invalid token source geometry");
        }
        int previousEnd = 0;
        for (int token = 0; token < offsets.length; token++) {
            Objects.checkFromIndexSize(offsets[token], lengths[token], sourceLength);
            if (offsets[token] < previousEnd) {
                throw new IllegalArgumentException("overlapping or unordered token spans");
            }
            previousEnd = offsets[token] + lengths[token];
        }
        if (tokenRange == -1L) return -1L;
        int first = rangeStart(tokenRange);
        int last = rangeEnd(tokenRange);
        Objects.checkFromToIndex(first, last, offsets.length);
        int start = first == offsets.length ? sourceLength : offsets[first];
        int end = first == last ? start : offsets[last - 1] + lengths[last - 1];
        return packRange(start, end);
    }

    /** Immutable vocabulary-conditioned masks, distinct from character-reachable masks. */
    public static final class VocabularyFilter {
        private final byte[] useful;
        private final long[] allowed;
        private final int tokens;
        private final int words;

        private VocabularyFilter(byte[] useful, long[] allowed, int tokens, int words) {
            this.useful = useful;
            this.allowed = allowed;
            this.tokens = tokens;
            this.words = words;
        }

        public int tokenCount() { return tokens; }
        public int allowedMaskWords() { return words; }
        public boolean canReachFinal(int state) {
            if (state == -1) return false;
            return useful[Objects.checkIndex(state, useful.length)] != 0;
        }
        public long allowedMaskWord(int state, int word) {
            Objects.checkIndex(state, useful.length);
            return allowed[state * words + Objects.checkIndex(word, words)];
        }
        public boolean allows(int state, int tokenId) {
            Objects.checkIndex(tokenId, tokens);
            if (state == -1) return false;
            return (allowedMaskWord(state, tokenId >>> 6) & (1L << (tokenId & 63))) != 0;
        }
        public long retainedPrimitiveBytes() {
            return useful.length + (long) allowed.length * Long.BYTES;
        }
    }

    /**
     * Immutable all-entry-state effect, tied to one exact RX7 owner's acceptance
     * snapshot. The private snapshot is unique to each fromAsciiDfa result, so it
     * supplies owner identity without retaining that owner's original DFA table.
     * Composition and repetition operate on state rows, never on expanded tokens.
     */
    public static final class Transfer {
        private final byte[] accepting;
        private final int start;
        private final short[] rows;
        private final long length;

        private Transfer(byte[] accepting, int start, short[] rows, long length) {
            this.accepting = accepting;
            this.start = start;
            this.rows = rows;
            this.length = length;
        }

        public long length() { return length; }
        /** Array payload bytes for effect rows and shared immutable acceptance flags. */
        public long retainedPrimitiveBytes() {
            return (long) rows.length * Short.BYTES + accepting.length;
        }
        public int nextState(int state) {
            if (state == -1) return -1;
            return rows[Objects.checkIndex(state, rows.length)];
        }
        public boolean matches() {
            int state = rows[start];
            return state >= 0 && accepting[state] != 0;
        }

        /** Consume this effect, then right; equal root strings do not imply owner identity. */
        public Transfer then(Transfer right) {
            Objects.requireNonNull(right, "right");
            if (right.accepting != accepting || right.start != start) {
                throw new IllegalArgumentException("different RX7 owners");
            }
            long total = Math.addExact(length, right.length);
            return new Transfer(accepting, start,
                    M3RX7Transitions.compose(rows, right.rows), total);
        }

        /** Nonnegative repetition, including the exact empty-sequence identity at zero. */
        public Transfer repeated(long count) {
            if (count < 0) throw new IllegalArgumentException("negative repeat count");
            long total = Math.multiplyExact(length, count);
            return new Transfer(accepting, start, M3RX7Transitions.repeat(rows, count), total);
        }
    }

    /**
     * Prepare exact DFA state-transfer rows for the canonical scalar ranges of one M3-backed
     * ASCII String. The String owner DAG is observed through JavaLangAccess as bounded primitive
     * lanes; no whole-String flattening or retained String/token object is introduced.
     *
     * <p>Null means the String is not M3-backed, is not ASCII, or the bounded transition/image
     * budget refuses the candidate. The authoritative JDK regex engine remains the fallback.</p>
     */
    public M3TokenPlan prepareM3TokenPlan(String input) {
        Objects.requireNonNull(input, "input");
        JavaLangAccess.M3StringTokenSnapshot snapshot =
                SharedSecrets.getJavaLangAccess().m3StringAsciiTokens(
                        input,
                        MAX_M3_TOKEN_SEQUENCE,
                        MAX_M3_TOKEN_VOCABULARY,
                        MAX_TOKEN_LENGTH,
                        MAX_PACKED_BYTES);
        if (snapshot == null) return null;
        byte[] packed = snapshot.packedAscii();
        int[] offsets = snapshot.offsets();
        int[] lengths = snapshot.lengths();
        int[] sequence = snapshot.sequence();
        TokenIndex index = prepareTokenIndex(packed, offsets, lengths);
        return index == null ? null : new M3TokenPlan(index, sequence);
    }

    /** Returns -1 when the bounded M3 token lane is unavailable, otherwise exact DFA 0/1. */
    public int matchesM3TokensCandidate(String input) {
        M3TokenPlan plan = prepareM3TokenPlan(Objects.requireNonNull(input, "input"));
        return plan == null ? -1 : plan.matches() ? 1 : 0;
    }

    /**
     * Compatibility oracle for the M3 token lane. A candidate disagreement is a hard invariant
     * failure; otherwise the JDK Pattern remains the returned truth.
     */
    public boolean matchesM3TokensVerified(String input, Pattern authoritativePattern) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(authoritativePattern, "authoritativePattern");
        boolean truth = authoritativePattern.matcher(input).matches();
        int candidate = matchesM3TokensCandidate(input);
        if (candidate >= 0 && (candidate == 1) != truth) {
            throw new IllegalStateException("M3RX7 token DFA disagrees with JDK Pattern");
        }
        return truth;
    }

    /** Pattern-specific primitive state-transfer image plus one M3 leaf-token sequence. */
    public static final class M3TokenPlan {
        private final TokenIndex index;
        private final int[] sequence;

        private M3TokenPlan(TokenIndex index, int[] sequence) {
            this.index = Objects.requireNonNull(index, "index");
            this.sequence = Objects.requireNonNull(sequence, "sequence").clone();
            for (int token : this.sequence) Objects.checkIndex(token, index.tokenCount());
        }

        public int tokenSequenceLength() { return sequence.length; }
        public int vocabularySize() { return index.tokenCount(); }
        public long retainedPrimitiveBytes() {
            return index.retainedPrimitiveBytes() + (long) sequence.length * Integer.BYTES;
        }
        public boolean matches() {
            int state = index.advanceAdaptive(index.initialState(), sequence, 0, sequence.length);
            return state >= 0 && index.isFinal(state);
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
