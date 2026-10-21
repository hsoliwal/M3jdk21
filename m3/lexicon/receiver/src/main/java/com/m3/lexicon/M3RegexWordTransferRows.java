// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
// M3 2026-10-21: owner-scoped phrase transfers and explicit image/coordinate admission.
package com.m3.lexicon;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;

/**
 * Word-id regex transfer rows (sidecar M3RXW001; REGEX_PRECOMPUTE_DESIGN.md §2e). For a DFA whose state
 * numbering is owned by the automaton leaf (R6-AUTOMATON), row {@code next[(cwid-1)*Q + q]} is the state
 * after the word's UTF-16 units from q, and accept bit {@code (cwid-1)*Q + q} records that an accepting
 * state is reached inside the word. Matching a word-tokenized String is one table read per token; any
 * split of the token sequence composes to the same state because rows are functions {@code Q -> Q}.
 */
public final class M3RegexWordTransferRows {
    /** Deterministic automaton over UTF-16 units. */
    public interface Dfa {
        /** @return number of states Q */
        int stateCount();

        /** @return next state for one UTF-16 unit */
        int next(int state, char unit);

        /** @return true when the state is accepting */
        boolean accepting(int state);
    }

    /** Header bytes of the M3RXW001 image. */
    public static final int HEADER = 128;

    private final int states;
    private final int vocabulary;
    private final int[] next;
    private final long[] accept;
    // Shared by summaries without retaining this vocabulary table or its payload arrays.
    private final Object summaryDomain = new Object();

    private M3RegexWordTransferRows(int states, int vocabulary, int[] next, long[] accept) {
        this.states = states;
        this.vocabulary = vocabulary;
        this.next = next;
        this.accept = accept;
    }

    /**
     * Builds the full vocab x Q table (image-time or lazy page fill). Inputs and DFA behavior
     * must remain stable during construction. Retained arrays contain no spelling references.
     */
    public static M3RegexWordTransferRows build(Dfa dfa, List<? extends CharSequence> words) {
        Objects.requireNonNull(dfa, "dfa");
        Objects.requireNonNull(words, "words");
        int q = dfa.stateCount();
        if (q <= 0) {
            throw new IllegalArgumentException("DFA state count must be positive");
        }
        int v = words.size();
        int[] next = new int[Math.multiplyExact(v, q)];
        long[] accept = M3BitLanes.lanes(next.length);
        for (int word = 0; word < v; word++) {
            CharSequence spelling = words.get(word);
            for (int state = 0; state < q; state++) {
                int current = state;
                for (int index = 0; index < spelling.length(); index++) {
                    current = dfa.next(current, spelling.charAt(index));
                    if (current < 0 || current >= q) {
                        throw new IllegalArgumentException("DFA transition outside state range");
                    }
                    if (dfa.accepting(current)) {
                        M3BitLanes.set(accept, word * q + state);
                    }
                }
                next[word * q + state] = current;
            }
        }
        return new M3RegexWordTransferRows(q, v, next, accept);
    }

    /** @return state after word {@code cwid} from {@code state} */
    public int step(int cwid, int state) {
        return next[cell(cwid, state)];
    }

    /** @return true when an accepting state is reached inside word {@code cwid} from {@code state} */
    public boolean acceptsInside(int cwid, int state) {
        return M3BitLanes.get(accept, cell(cwid, state));
    }

    /** Composes rows over a CWID sequence (every CWID must be in 1..V). */
    public int run(int[] cwids, int state) {
        return run(cwids, 0, cwids.length, state);
    }

    /** Runs one range, without copying the caller's lane or inspecting units outside that range. */
    public int run(int[] cwids, int offset, int length, int state) {
        Objects.checkFromIndexSize(offset, length, cwids.length);
        Objects.checkIndex(state, states);
        int current = state;
        for (int index = offset; index < offset + length; index++) {
            current = next[wordRow(cwids[index]) * states + current];
        }
        return current;
    }

    /** @return number of automaton states Q */
    public int stateCount() {
        return states;
    }

    /** Precomputes the whole CWID lane for every possible entry state. */
    public TransferSummary summarize(int[] cwids) {
        return summarize(cwids, 0, cwids.length);
    }

    /**
     * Precomputes a range using only the existing word rows, without rescanning spellings or
     * retaining the caller's lane. Costs O(length * Q) time and O(Q) space. Caller must keep the
     * lane stable during this call. Empty ranges are identity with no accepting visit.
     */
    public TransferSummary summarize(int[] cwids, int offset, int length) {
        Objects.checkFromIndexSize(offset, length, cwids.length);
        for (int index = offset; index < offset + length; index++) {
            wordRow(cwids[index]);
        }
        int[] exits = new int[states];
        long[] seen = M3BitLanes.lanes(states);
        for (int state = 0; state < states; state++) {
            exits[state] = state;
        }
        for (int index = offset; index < offset + length; index++) {
            int row = (cwids[index] - 1) * states;
            for (int state = 0; state < states; state++) {
                int at = row + exits[state];
                if (M3BitLanes.get(accept, at)) {
                    M3BitLanes.set(seen, state);
                }
                exits[state] = next[at];
            }
        }
        return new TransferSummary(summaryDomain, exits, seen);
    }

    /**
     * Immutable transfer function for a phrase/subtree. Arrays are private and do not escape.
     * This is an accepting-visit summary, not capture, match-offset or zero-width-match data.
     * It retains only O(Q) state and the owner's identity token, not the vocabulary table.
     */
    public static final class TransferSummary {
        private final Object domain;
        private final int[] exits;
        private final long[] seen;

        private TransferSummary(Object domain, int[] exits, long[] seen) {
            this.domain = domain;
            this.exits = exits;
            this.seen = seen;
        }

        /** @return number of entry states */
        public int stateCount() {
            return exits.length;
        }

        /** @return the exit state for this entry state */
        public int step(int state) {
            return exits[Objects.checkIndex(state, exits.length)];
        }

        /** @return whether an accepting state was visited after consuming at least one unit */
        public boolean acceptsInside(int state) {
            return M3BitLanes.get(seen, Objects.checkIndex(state, exits.length));
        }

        /**
         * Applies this summary then right, in O(Q) time/space, without text or token reads.
         * Associative, not commutative. Distinct table owners are deliberately incompatible,
         * including separate reads of identical images; equal state counts are not identity.
         */
        public TransferSummary then(TransferSummary right) {
            Objects.requireNonNull(right, "right");
            if (domain != right.domain) {
                throw new IllegalArgumentException("transfer summaries have different owners");
            }
            int[] composed = new int[exits.length];
            long[] accepted = M3BitLanes.lanes(exits.length);
            for (int state = 0; state < exits.length; state++) {
                int middle = exits[state];
                composed[state] = right.exits[middle];
                if (M3BitLanes.get(seen, state) || M3BitLanes.get(right.seen, middle)) {
                    M3BitLanes.set(accepted, state);
                }
            }
            return new TransferSummary(domain, composed, accepted);
        }
    }

    private int cell(int cwid, int state) {
        Objects.checkIndex(state, states);
        return wordRow(cwid) * states + state;
    }

    private int wordRow(int cwid) {
        if (cwid < 1 || cwid > vocabulary) {
            throw new IndexOutOfBoundsException("CWID outside vocabulary");
        }
        return cwid - 1;
    }

    /** @return vocabulary size V */
    public int vocabulary() {
        return vocabulary;
    }

    /** Serializes the M3RXW001 image: 4-byte cells, accept bitset, SHA-256 at 96..127. */
    public byte[] image(long generation64, byte[] patternKey) {
        if (patternKey.length != 32) {
            throw new IllegalArgumentException("pattern key must be a SHA-256");
        }
        long transfer = HEADER;
        long acceptAt = transfer + 4L * next.length;
        ByteBuffer out = ByteBuffer.allocate(Math.toIntExact(acceptAt + 8L * accept.length))
                .order(ByteOrder.BIG_ENDIAN);
        out.putLong(0, M3LexiconFormat.M3RXW001.magic()).putInt(8, 1).putInt(12, 0).putInt(16, states)
                .putInt(20, vocabulary).putInt(24, 4).putInt(28, 4096).putLong(32, generation64)
                .put(40, patternKey).putLong(72, transfer).putLong(80, acceptAt).putLong(88, 0);
        for (int cell = 0; cell < next.length; cell++) {
            out.putInt((int) transfer + cell * 4, next[cell]);
        }
        for (int lane = 0; lane < accept.length; lane++) {
            out.putLong((int) acceptAt + lane * 8, accept[lane]);
        }
        out.put(96, digest(out));
        return out.array();
    }

    /**
     * Integrity-only read of an M3RXW001 v1 image; no expected lexicon/pattern is asserted.
     * The image starts at buffer index zero (position is ignored). Keep its bytes stable
     * during admission; the result copies only table lanes and does not retain the buffer.
     */
    public static M3RegexWordTransferRows read(ByteBuffer image) throws M3LexiconFormatException {
        return read(image, 0, null, false);
    }

    /**
     * Reads only when the full stored 64-bit generation and all 32 pattern-key bytes match.
     * These identifiers must come from the caller's trusted manifest. The generation is not
     * a full lexicon SHA-256, and a caller-supplied pattern key is not proof of DFA semantics.
     * Buffer ownership is the same as {@link #read(ByteBuffer)}.
     */
    public static M3RegexWordTransferRows read(ByteBuffer image, long expectedGeneration64,
            byte[] expectedPatternKey) throws M3LexiconFormatException {
        Objects.requireNonNull(expectedPatternKey, "expectedPatternKey");
        if (expectedPatternKey.length != 32) {
            throw new IllegalArgumentException("pattern key must be a SHA-256");
        }
        return read(image, expectedGeneration64, expectedPatternKey.clone(), true);
    }

    private static M3RegexWordTransferRows read(ByteBuffer image, long generation,
            byte[] patternKey, boolean bound) throws M3LexiconFormatException {
        ByteBuffer map = image.duplicate().order(ByteOrder.BIG_ENDIAN);
        M3LexiconFormat.checkHeader(map);
        M3LexiconFormatException.require(map.getLong(0) == M3LexiconFormat.M3RXW001.magic(), "magic");
        int q = map.getInt(16);
        int v = map.getInt(20);
        long cells = (long) q * v;
        long acceptAt = map.getLong(80);
        // Bound cells by the actual int-sized buffer before multiplying or narrowing.
        M3LexiconFormatException.require(q > 0 && v >= 0 && cells <= (map.limit() - HEADER) / 4L
                && map.getInt(24) == 4 && map.getLong(72) == HEADER
                && acceptAt == HEADER + 4 * cells && map.limit() == acceptAt + 8L * ((cells + 63) >>> 6),
                "invalid M3RXW001 geometry");
        M3LexiconFormatException.require(map.getInt(12) == 0 && map.getInt(28) == 4096
                && map.getLong(88) == 0, "unsupported M3RXW001 metadata");
        byte[] expected = new byte[32];
        map.get(96, expected);
        M3LexiconFormatException.require(MessageDigest.isEqual(expected, digest(map)), "digest mismatch");
        if (bound) {
            byte[] storedPattern = new byte[32];
            map.get(40, storedPattern);
            M3LexiconFormatException.require(map.getLong(32) == generation
                    && MessageDigest.isEqual(storedPattern, patternKey), "lexicon/pattern binding mismatch");
        }
        int[] next = new int[(int) cells];
        long[] accept = M3BitLanes.lanes((int) cells);
        for (int cell = 0; cell < next.length; cell++) {
            next[cell] = map.getInt(HEADER + cell * 4);
            M3LexiconFormatException.require(next[cell] >= 0 && next[cell] < q, "state out of range");
        }
        for (int lane = 0; lane < accept.length; lane++) {
            accept[lane] = map.getLong((int) acceptAt + lane * 8);
        }
        int used = next.length & 63;
        M3LexiconFormatException.require(used == 0 || (accept[accept.length - 1] >>> used) == 0,
                "nonzero acceptance padding");
        return new M3RegexWordTransferRows(q, v, next, accept);
    }

    private static byte[] digest(ByteBuffer image) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(image.duplicate().position(0).limit(96));
            sha.update(image.duplicate().position(HEADER).limit(image.limit()));
            return sha.digest();
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
