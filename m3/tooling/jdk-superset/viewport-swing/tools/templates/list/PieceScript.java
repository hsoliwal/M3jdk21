/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */
// Template of gen_atom_tests.py / gen_atom_perf.py: the generated copy replaces this header.

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import sun.swing.list.M3RowPieces;

/**
 * Pattern ADD_REMOVE_GET: {@code M3RowPieces} (a piece table over the rows of the last layout) against a
 * {@code java.util.ArrayList} of the same rows, each entry the old row index or {@code NEW}. After every edit the
 * pieces expanded in order must equal the list, every piece must be non-empty and the lengths must add up to
 * {@code size()}.
 */
final class PieceScript {

    static final int MAX_ROWS = 40;
    static final int STEPS = 400;
    static final int MAX_RUN = 5;
    /** The piece lanes start with this many slots ({@code new int[8]}); more pieces grow them. */
    static final int INITIAL_SLOTS = 8;
    static final int GROWTH_ROWS = 64;

    enum Mix {
        INSERT(6, 1, 1, 0), REMOVE(1, 6, 1, 0), CHANGE(1, 1, 6, 0), RESET(2, 2, 2, 1), MIXED(3, 3, 3, 1);

        final int insert;
        final int remove;
        final int change;
        final int reset;

        Mix(int insert, int remove, int change, int reset) {
            this.insert = insert;
            this.remove = remove;
            this.change = change;
            this.reset = reset;
        }
    }

    final M3RowPieces pieces = new M3RowPieces();
    final List<Integer> oracle = new ArrayList<>();
    final Random rnd;
    int maxCount;

    PieceScript(String atom) {
        rnd = AtomSupport.random(atom);
    }

    void reset(int n) {
        pieces.reset(n);
        oracle.clear();
        for (int i = 0; i < n; i++) {
            oracle.add(i);
        }
        verify("reset " + n);
        assertEquals(n > 0 ? 1 : 0, pieces.count(), "reset leaves one piece of old rows, or none");
        if (n > 0) {
            assertEquals(0, pieces.start(0));
            assertEquals(n, pieces.length(0));
        }
    }

    void insert(int at, int k) {
        pieces.insert(at, k);
        for (int j = 0; j < k; j++) {
            oracle.add(at, M3RowPieces.NEW);
        }
        verify("insert " + at + " " + k);
    }

    void remove(int from, int to) {
        pieces.remove(from, to);
        oracle.subList(from, to + 1).clear();
        verify("remove " + from + ".." + to);
    }

    void change(int from, int to) {
        pieces.change(from, to);
        for (int j = from; j <= to; j++) {
            oracle.set(j, M3RowPieces.NEW);
        }
        verify("change " + from + ".." + to);
    }

    /** The expansion of the pieces, the size and the piece invariants. */
    void verify(String what) {
        assertEquals(oracle.size(), pieces.size(), what + ": size");
        List<Integer> rows = new ArrayList<>();
        for (int i = 0; i < pieces.count(); i++) {
            int len = pieces.length(i);
            int s = pieces.start(i);
            assertTrue(len > 0, what + ": piece " + i + " is empty");
            assertTrue(s == M3RowPieces.NEW || s >= 0, what + ": piece start " + s);
            for (int j = 0; j < len; j++) {
                rows.add(s == M3RowPieces.NEW ? M3RowPieces.NEW : s + j);
            }
        }
        assertEquals(oracle, rows, what);
        maxCount = Math.max(maxCount, pieces.count());
    }

    void run(Mix mix, int steps) {
        reset(rnd.nextInt(MAX_ROWS));
        int total = mix.insert + mix.remove + mix.change + mix.reset;
        for (int step = 0; step < steps; step++) {
            int op = rnd.nextInt(total);
            int n = pieces.size();
            if (op < mix.insert || n == 0) {
                insert(rnd.nextInt(n + 1), 1 + rnd.nextInt(MAX_RUN));
            } else if (op < mix.insert + mix.remove) {
                int from = rnd.nextInt(n);
                remove(from, Math.min(n - 1, from + rnd.nextInt(MAX_RUN)));
            } else if (op < mix.insert + mix.remove + mix.change) {
                int from = rnd.nextInt(n);
                change(from, Math.min(n - 1, from + rnd.nextInt(MAX_RUN)));
            } else {
                reset(rnd.nextInt(MAX_ROWS));
            }
        }
    }

    // ---- white-box checks of the private atoms ---------------------------------------------------------

    /** {@code split(row)}: the index of the piece that starts at {@code row}; the rows themselves do not change. */
    void splitContract() {
        MethodHandle split = AtomSupport.virtual(M3RowPieces.class, "split", int.class, int.class);
        for (int trial = 0; trial < STEPS; trial++) {
            run(Mix.MIXED, MAX_RUN);
            int n = pieces.size();
            int row = rnd.nextInt(n + 1);
            int countBefore = pieces.count();
            int i = (int) AtomSupport.call(split, pieces, row);
            verify("split " + row);
            int first = 0;
            for (int j = 0; j < i; j++) {
                first += pieces.length(j);
            }
            assertEquals(row, first, "piece " + i + " starts at row " + row);
            assertTrue(i <= pieces.count() && (i < pieces.count() || row == n));
            assertTrue(pieces.count() - countBefore <= 1, "split adds at most one piece");
        }
    }

    /** {@code open(i)}: one more slot at {@code i}; the pieces from {@code i} move up by one, the lanes grow. */
    void openContract() {
        MethodHandle open = AtomSupport.virtual(M3RowPieces.class, "open", void.class, int.class);
        VarHandle startLane = AtomSupport.field(M3RowPieces.class, "start", int[].class);
        for (int trial = 0; trial < STEPS; trial++) {
            M3RowPieces p = new M3RowPieces();
            p.reset(GROWTH_ROWS);
            int pieceCount = 1 + rnd.nextInt(INITIAL_SLOTS * 2);
            for (int k = 1; k < pieceCount && p.size() > 1; k++) {
                int from = rnd.nextInt(p.size() - 1);
                p.change(from, from);
            }
            int count = p.count();
            int[] starts = new int[count];
            int[] lengths = new int[count];
            for (int j = 0; j < count; j++) {
                starts[j] = p.start(j);
                lengths[j] = p.length(j);
            }
            int at = rnd.nextInt(count + 1);
            AtomSupport.call(open, p, at);
            assertEquals(count + 1, p.count(), "open adds one slot");
            assertTrue(((int[]) startLane.get(p)).length >= p.count(), "the lanes grew to hold every slot");
            for (int j = 0; j < count; j++) {
                int moved = j < at ? j : j + 1;
                assertEquals(starts[j], p.start(moved), "start of piece " + j);
                assertEquals(lengths[j], p.length(moved), "length of piece " + j);
            }
        }
    }

    // ---- per-atom checks ------------------------------------------------------------------------------

    static void check(String atom) {
        PieceScript s = new PieceScript(atom);
        switch (atom) {
            case "M3RowPieces" -> {
                M3RowPieces fresh = new M3RowPieces();
                assertEquals(0, fresh.size(), "a new table has no rows");
                assertEquals(0, fresh.count(), "and no pieces");
                s.run(Mix.MIXED, STEPS);
            }
            case "reset" -> s.run(Mix.RESET, STEPS);
            case "size", "count", "start", "length" -> s.run(Mix.MIXED, STEPS);
            case "insert" -> s.run(Mix.INSERT, STEPS);
            case "remove" -> s.run(Mix.REMOVE, STEPS);
            case "change" -> s.run(Mix.CHANGE, STEPS);
            case "split" -> s.splitContract();
            case "open" -> {
                s.openContract();
                s.run(Mix.CHANGE, STEPS);
                assertTrue(s.maxCount > INITIAL_SLOTS, "the script grew the piece lanes past their first capacity");
            }
            default -> fail("no check for M3RowPieces#" + atom);
        }
    }
}
