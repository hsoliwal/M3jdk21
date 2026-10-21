// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
// M3 refinement 2026-10-21: checked coordinates, bound intake and composable row summaries.
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

    private M3RegexWordTransferRows(int states, int vocabulary, int[] next, long[] accept) {
        this.states = states;
        this.vocabulary = vocabulary;
        this.next = next;
        this.accept = accept;
    }

    /**
     * Builds the full vocab x Q table (image-time or lazy page fill). The DFA and spellings
     * must be stable during construction. A DFA transition must return a state in 0..Q-1.
     * UTF-16 units are exact, including NUL and unpaired surrogates; no folding is performed.
     */
    public static M3RegexWordTransferRows build(Dfa dfa, List<? extends CharSequence> words) {
        int q = Objects.requireNonNull(dfa, "dfa").stateCount();
        if (q <= 0) {
            throw new IllegalArgumentException("DFA state count must be positive");
        }
        int v = Objects.requireNonNull(words, "words").size();
        int[] next = new int[Math.multiplyExact(v, q)];
        long[] accept = M3BitLanes.lanes(next.length);
        for (int word = 0; word < v; word++) {
            CharSequence spelling = words.get(word);
            for (int state = 0; state < q; state++) {
                int current = state;
                for (int index = 0; index < spelling.length(); index++) {
                    current = dfa.next(current, spelling.charAt(index));
                    if (current < 0 || current >= q) {
                        throw new IllegalArgumentException("DFA transition out of range");
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

    /** Composes a half-open CWID range without copying the token lane. */
    public int run(int[] cwids, int from, int to, int state) {
        Objects.checkFromToIndex(from, to, cwids.length);
        Objects.checkIndex(state, states);
        int current = state;
        for (int token = from; token < to; token++) {
            current = next[row(cwids[token]) + current];
        }
        return current;
    }

    /** @return number of DFA states Q */
    public int stateCount() {
        return states;
    }

    /**
     * Precomputes a transfer for every starting state over all supplied CWIDs.
     * No spelling or DFA callback is visited after build. The input must remain stable
     * during this call; the returned summary retains neither the input nor any spelling.
     */
    public Summary summarize(int[] cwids) {
        return summarize(cwids, 0, cwids.length);
    }

    /**
     * Precomputes a half-open range in O(Q * (to-from)) work, retaining Q integers and
     * ceil(Q/64) acceptance lanes. Empty ranges are identity transfers, with no inside
     * acceptance; an initially accepting state is not itself a consumed-text match.
     */
    public Summary summarize(int[] cwids, int from, int to) {
        Objects.checkFromToIndex(from, to, cwids.length);
        int[] transition = identityTransitions();
        long[] acceptance = M3BitLanes.lanes(states);
        for (int token = from; token < to; token++) {
            int row = row(cwids[token]);
            for (int state = 0; state < states; state++) {
                int cell = row + transition[state];
                if (M3BitLanes.get(accept, cell)) {
                    M3BitLanes.set(acceptance, state);
                }
                transition[state] = next[cell];
            }
        }
        // Finish array initialization before the constructor's final-field publication.
        return new Summary(this, transition, acceptance);
    }

    private int[] identityTransitions() {
        int[] transition = new int[states];
        for (int state = 0; state < states; state++) {
            transition[state] = state;
        }
        return transition;
    }

    private Summary identity() {
        return new Summary(this, identityTransitions(), M3BitLanes.lanes(states));
    }

    private int row(int cwid) {
        // Check before subtract/multiply: flat-array bounds do not protect coordinates.
        if (cwid <= 0 || cwid > vocabulary) {
            throw new IndexOutOfBoundsException("CWID outside 1..V");
        }
        return (cwid - 1) * states;
    }

    private int cell(int cwid, int state) {
        Objects.checkIndex(state, states);
        return row(cwid) + state;
    }

    /**
     * Immutable primitive transfer function over this exact table owner. Summaries may
     * be shared by readers; arrays never escape. Retaining a summary retains its owner.
     * This summarizes supplied DFA transitions, not captures/backreferences or all of
     * java.util.regex. Acceptance means a state reached AFTER at least one UTF-16 unit.
     */
    public static final class Summary {
        private final M3RegexWordTransferRows owner;
        private final int[] next;
        private final long[] accept;

        private Summary(M3RegexWordTransferRows owner, int[] next, long[] accept) {
            this.owner = owner;
            this.next = next;
            this.accept = accept;
        }

        /** @return exit state for the supplied starting state */
        public int step(int state) {
            return next[Objects.checkIndex(state, next.length)];
        }

        /** @return whether an accepting state is reached after consuming a unit */
        public boolean acceptsInside(int state) {
            return M3BitLanes.get(accept, Objects.checkIndex(state, next.length));
        }

        /**
         * Returns this segment followed by right in O(Q) work, without spelling or
         * token reads. Rejects another owner even when its dimensions happen to match.
         */
        public Summary then(Summary right) {
            Objects.requireNonNull(right, "right");
            if (owner != right.owner) {
                throw new IllegalArgumentException("different transfer table owner");
            }
            int[] transition = new int[next.length];
            long[] acceptance = M3BitLanes.lanes(next.length);
            for (int state = 0; state < next.length; state++) {
                int middle = next[state];
                transition[state] = right.next[middle];
                if (M3BitLanes.get(accept, state) || M3BitLanes.get(right.accept, middle)) {
                    M3BitLanes.set(acceptance, state);
                }
            }
            return new Summary(owner, transition, acceptance);
        }

        /**
         * Repeats the segment by binary powering in O(Q * log(copies)) work and O(Q)
         * live auxiliary storage. No expanded word-id lane is created; zero is identity.
         */
        public Summary repeat(long copies) {
            if (copies < 0) {
                throw new IllegalArgumentException("negative repeat count");
            }
            if (copies == 1) {
                return this;
            }
            Summary result = owner.identity();
            Summary power = this;
            for (long count = copies; count != 0; count >>>= 1) {
                if ((count & 1) != 0) {
                    result = result.then(power);
                }
                if (count > 1) {
                    power = power.then(power);
                }
            }
            return result;
        }
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
     * Reads an image written by {@link #image}; refuses on magic, geometry or digest
     * mismatch. Legacy unbound intake: use the overload for caller-expected identity.
     * The bytes in [0, limit) must not change during intake, including through aliases
     * of a read-only buffer. Position, mark and byte order of the caller are untouched.
     */
    public static M3RegexWordTransferRows read(ByteBuffer image) throws M3LexiconFormatException {
        return readImage(image, 0, null);
    }

    /**
     * Reads only a sidecar for the caller's exact generation64 and 32-byte pattern key.
     * Digest and state validation still apply. These are caller-supplied identities,
     * not a signature, proof of the DFA, or globally collision-free lexicon identity.
     * Input bytes must remain immutable throughout this call, as for the legacy reader.
     */
    public static M3RegexWordTransferRows read(ByteBuffer image, long generation64, byte[] patternKey)
            throws M3LexiconFormatException {
        if (Objects.requireNonNull(patternKey, "patternKey").length != 32) {
            throw new IllegalArgumentException("pattern key must be a SHA-256");
        }
        return readImage(image, generation64, patternKey.clone());
    }

    private static M3RegexWordTransferRows readImage(ByteBuffer image, long generation64, byte[] patternKey)
            throws M3LexiconFormatException {
        ByteBuffer map = image.duplicate().order(ByteOrder.BIG_ENDIAN);
        M3LexiconFormat.checkHeader(map);
        M3LexiconFormatException.require(map.getLong(0) == M3LexiconFormat.M3RXW001.magic(), "magic");
        int q = map.getInt(16);
        int v = map.getInt(20);
        long cells = (long) q * v;
        // Bound before multiplying cell count by its width; malicious q*v can exceed long/4.
        M3LexiconFormatException.require(q > 0 && v >= 0 && cells <= (Integer.MAX_VALUE - HEADER) / 4,
                "invalid M3RXW001 cell count");
        long acceptAt = map.getLong(80);
        M3LexiconFormatException.require(map.getInt(24) == 4 && map.getLong(72) == HEADER
                && acceptAt == HEADER + 4 * cells && map.limit() == acceptAt + 8L * ((cells + 63) >>> 6),
                "invalid M3RXW001 geometry");
        if (patternKey != null) {
            byte[] storedKey = new byte[32];
            map.get(40, storedKey);
            M3LexiconFormatException.require(map.getLong(32) == generation64, "lexicon generation mismatch");
            M3LexiconFormatException.require(MessageDigest.isEqual(patternKey, storedKey), "pattern key mismatch");
        }
        byte[] expected = new byte[32];
        map.get(96, expected);
        M3LexiconFormatException.require(MessageDigest.isEqual(expected, digest(map)), "digest mismatch");
        int[] next = new int[(int) cells];
        long[] accept = M3BitLanes.lanes((int) cells);
        for (int cell = 0; cell < next.length; cell++) {
            next[cell] = map.getInt(HEADER + cell * 4);
            M3LexiconFormatException.require(next[cell] >= 0 && next[cell] < q, "state out of range");
        }
        for (int lane = 0; lane < accept.length; lane++) {
            accept[lane] = map.getLong((int) acceptAt + lane * 8);
        }
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
