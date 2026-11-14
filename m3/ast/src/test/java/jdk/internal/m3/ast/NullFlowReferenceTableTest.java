// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Hand-derived reference tables for the relocated null-flow atoms (recipe m3/ast/recipe/null-flow-20261107, census
 * L048): the 3 x 3 join table of the two-bit lattice at word boundaries, the reachability and assumption tables,
 * the monotone merge table, a four-node loop with a guarded exit whose fixed point is written out by hand, and the
 * identity (not equality) contract of the symbol table. The donor probes are seeded oracles; this table is the
 * independent reference model a reviewer can check line by line.
 */
//FLLDCIM: RELOCATE - M3JDK-authored reference model of recipe m3/ast/recipe/null-flow-20261107 (census L048); donor oracles are the three *Probe classes
final class NullFlowReferenceTableTest {
    private static final int U = NullFlowFacts.UNKNOWN;
    private static final int N = NullFlowFacts.NULL;
    private static final int X = NullFlowFacts.NONNULL;
    /** join(a, b) by hand: equal facts survive, any disagreement forgets. Rows a = U, N, X; columns b = U, N, X. */
    private static final int[][] JOIN = {{U, U, U}, {U, N, U}, {U, U, X}};
    /** Slots on both sides of the 32-fact word boundaries of a 70-variable frame. */
    private static final int[] SLOTS = {0, 1, 30, 31, 32, 33, 63, 64, 65, 69};

    @Test void joinTableAtWordBoundaries() {
        for (int a = U; a <= X; a++) {
            for (int b = U; b <= X; b++) {
                NullFlowFacts left = new NullFlowFacts(70);
                NullFlowFacts right = new NullFlowFacts(70);
                for (int slot : SLOTS) {
                    left.set(slot, a);
                    right.set(slot, b);
                }
                NullFlowFacts joined = new NullFlowFacts(70);
                joined.joinFrom(left, right);
                assertTrue(joined.reachable());
                for (int slot : SLOTS) assertEquals(JOIN[a][b], joined.state(slot), "join(" + a + "," + b + ") slot " + slot);
                for (int slot = 0; slot < 70; slot++) {
                    boolean touched = java.util.Arrays.binarySearch(SLOTS, slot) >= 0;
                    if (!touched) assertEquals(U, joined.state(slot), "untouched slot " + slot);
                }
                // the join is symmetric
                NullFlowFacts mirrored = new NullFlowFacts(70);
                mirrored.joinFrom(right, left);
                assertEquals(joined, mirrored);
            }
        }
    }

    @Test void reachabilityTable() {
        // (left reachable, right reachable) -> result reachable; an unreachable side is the identity of the join
        NullFlowFacts live = new NullFlowFacts(3);
        live.set(1, N);
        NullFlowFacts dead = new NullFlowFacts(3);
        dead.makeUnreachable();
        NullFlowFacts result = new NullFlowFacts(3);
        result.joinFrom(live, live);
        assertTrue(result.reachable());
        assertEquals(live, result);
        result.joinFrom(live, dead);
        assertTrue(result.reachable());
        assertEquals(live, result);
        result.joinFrom(dead, live);
        assertTrue(result.reachable());
        assertEquals(live, result);
        result.joinFrom(dead, dead);
        assertFalse(result.reachable());
        assertThrows(IllegalStateException.class, () -> result.state(1));
    }

    @Test void assumptionTable() {
        // prior x required -> (accepted, state after, frame reachable)
        int[][] rows = {
            // prior, required, accepted, state, reachable
            {U, N, 1, N, 1},
            {U, X, 1, X, 1},
            {N, N, 1, N, 1},
            {X, X, 1, X, 1},
            {N, X, 0, -1, 0},
            {X, N, 0, -1, 0},
        };
        for (int[] row : rows) {
            NullFlowFacts frame = new NullFlowFacts(40);
            frame.set(33, row[0]);
            boolean accepted = frame.assume(33, row[1]);
            assertEquals(row[2] == 1, accepted, "accepted prior=" + row[0] + " required=" + row[1]);
            assertEquals(row[4] == 1, frame.reachable(), "reachable prior=" + row[0] + " required=" + row[1]);
            if (row[4] == 1) assertEquals(row[3], frame.state(33));
        }
        NullFlowFacts frame = new NullFlowFacts(1);
        assertThrows(IllegalArgumentException.class, () -> frame.assume(0, U));
        frame.makeUnreachable();
        assertFalse(frame.assume(0, N));
    }

    @Test void mergeTable() {
        // accumulator x incoming -> (accumulator after, changed); merge is the join with change detection
        int[][] rows = {
            // acc, incoming, after, changed
            {U, U, U, 0}, {U, N, U, 0}, {U, X, U, 0},
            {N, U, U, 1}, {N, N, N, 0}, {N, X, U, 1},
            {X, U, U, 1}, {X, N, U, 1}, {X, X, X, 0},
        };
        for (int[] row : rows) {
            NullFlowFacts acc = new NullFlowFacts(64);
            acc.set(63, row[0]);
            NullFlowFacts incoming = new NullFlowFacts(64);
            incoming.set(63, row[1]);
            boolean changed = acc.mergeFrom(incoming);
            assertEquals(row[3] == 1, changed, "changed acc=" + row[0] + " in=" + row[1]);
            assertEquals(row[2], acc.state(63), "after acc=" + row[0] + " in=" + row[1]);
        }
        // an unreachable accumulator takes the incoming frame; an unreachable incoming frame changes nothing
        NullFlowFacts acc = new NullFlowFacts(2);
        acc.makeUnreachable();
        NullFlowFacts incoming = new NullFlowFacts(2);
        incoming.set(0, X);
        assertTrue(acc.mergeFrom(incoming));
        assertEquals(incoming, acc);
        NullFlowFacts dead = new NullFlowFacts(2);
        dead.makeUnreachable();
        assertFalse(acc.mergeFrom(dead));
        assertEquals(incoming, acc);
    }

    @Test void loopWithGuardedExitsByHand() {
        // nodes: 0 entry, 1 loop head, 2 loop body, 3 exit guarded by v0 != null, 4 exit guarded by v0 == null
        // edges: 0->1 SET_NONNULL v0; 1->2 IDENTITY; 2->1 FORGET v0; 1->3 REQUIRE_NONNULL v0; 1->4 REQUIRE_NULL v0
        // by hand: in[1] = join(NONNULL from 0, UNKNOWN from 2) = UNKNOWN; in[2] = in[1] = UNKNOWN;
        // in[3]: REQUIRE_NONNULL on UNKNOWN -> NONNULL (and on NONNULL -> NONNULL) = NONNULL;
        // in[4]: REQUIRE_NULL on NONNULL is a contradiction (unreachable candidate), on UNKNOWN -> NULL = NULL.
        NullFlowWorklist graph = new NullFlowWorklist(5, 1,
                new int[] {0, 1, 2, 1, 1}, new int[] {1, 2, 1, 3, 4},
                new int[] {0, -1, 0, 0, 0},
                new byte[] {NullFlowWorklist.SET_NONNULL, NullFlowWorklist.IDENTITY, NullFlowWorklist.FORGET,
                            NullFlowWorklist.REQUIRE_NONNULL, NullFlowWorklist.REQUIRE_NULL});
        NullFlowFacts entry = new NullFlowFacts(1);
        NullFlowFacts[] in = graph.solve(0, entry);
        int[] expected = {U, U, U, X, N};
        for (int node = 0; node < 5; node++) {
            assertTrue(in[node].reachable(), "node " + node);
            assertEquals(expected[node], in[node].state(0), "node " + node);
        }
        assertEquals(new NullFlowFacts(1), entry);
        // the same fixed point with the edges in reverse order
        NullFlowWorklist reversed = new NullFlowWorklist(5, 1,
                new int[] {1, 1, 2, 1, 0}, new int[] {4, 3, 1, 2, 1},
                new int[] {0, 0, 0, -1, 0},
                new byte[] {NullFlowWorklist.REQUIRE_NULL, NullFlowWorklist.REQUIRE_NONNULL,
                            NullFlowWorklist.FORGET, NullFlowWorklist.IDENTITY, NullFlowWorklist.SET_NONNULL});
        NullFlowFacts[] again = reversed.solve(0, entry);
        for (int node = 0; node < 5; node++) assertEquals(in[node], again[node], "node " + node);
        // an unreachable entry reaches nothing
        NullFlowFacts deadEntry = new NullFlowFacts(1);
        deadEntry.makeUnreachable();
        for (NullFlowFacts frame : graph.solve(0, deadEntry)) assertFalse(frame.reachable());
    }

    @Test void symbolIdentityNotEquality() {
        NullFlowVariableIds ids = new NullFlowVariableIds("0123456789abcdef".repeat(4), "table#0");
        String first = new String("x");
        String second = new String("x");
        assertEquals(first, second);
        assertEquals(0, ids.intern(first));
        assertEquals(1, ids.intern(second));
        assertEquals(0, ids.intern(first));
        assertEquals(-1, ids.findId(new String("x")));
        assertEquals(2, ids.variableCount());
        assertThrows(IllegalStateException.class, ids::newFacts);
        ids.freeze();
        assertThrows(IllegalStateException.class, () -> ids.intern(new Object()));
        assertEquals(2, ids.newFacts().variableCount());
        assertNotEquals(ids.findId(first), ids.findId(second));
    }
}
