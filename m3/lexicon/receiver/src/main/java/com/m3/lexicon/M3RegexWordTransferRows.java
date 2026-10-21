// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

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

    /** Builds the full vocab x Q table (image-time or lazy page fill). */
    public static M3RegexWordTransferRows build(Dfa dfa, List<? extends CharSequence> words) {
        int q = dfa.stateCount();
        int v = words.size();
        int[] next = new int[Math.multiplyExact(v, q)];
        long[] accept = M3BitLanes.lanes(next.length);
        for (int word = 0; word < v; word++) {
            CharSequence spelling = words.get(word);
            for (int state = 0; state < q; state++) {
                int current = state;
                for (int index = 0; index < spelling.length(); index++) {
                    current = dfa.next(current, spelling.charAt(index));
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
        return next[(cwid - 1) * states + state];
    }

    /** @return true when an accepting state is reached inside word {@code cwid} from {@code state} */
    public boolean acceptsInside(int cwid, int state) {
        return M3BitLanes.get(accept, (cwid - 1) * states + state);
    }

    /** Composes rows over a CWID sequence (every CWID must be in 1..V). */
    public int run(int[] cwids, int state) {
        int current = state;
        for (int cwid : cwids) {
            current = step(cwid, current);
        }
        return current;
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

    /** Reads an image written by {@link #image}; refuses on magic, geometry or digest mismatch. */
    public static M3RegexWordTransferRows read(ByteBuffer image) throws M3LexiconFormatException {
        ByteBuffer map = image.duplicate().order(ByteOrder.BIG_ENDIAN);
        M3LexiconFormat.checkHeader(map);
        M3LexiconFormatException.require(map.getLong(0) == M3LexiconFormat.M3RXW001.magic(), "magic");
        int q = map.getInt(16);
        int v = map.getInt(20);
        long cells = (long) q * v;
        long acceptAt = map.getLong(80);
        M3LexiconFormatException.require(q > 0 && v >= 0 && map.getInt(24) == 4 && map.getLong(72) == HEADER
                && acceptAt == HEADER + 4 * cells && map.limit() == acceptAt + 8L * ((cells + 63) >>> 6),
                "invalid M3RXW001 geometry");
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
