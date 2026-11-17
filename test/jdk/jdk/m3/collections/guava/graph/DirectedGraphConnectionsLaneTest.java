/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections.guava.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * The slot lane with EMPTY tombstones and a hashed index behind a stable mutable
 * DirectedGraphConnections behaves exactly as the ArrayList it replaces: lockstep runs against
 * the verbatim pre-patch copy compare results, exceptions, views, iteration order and fail-fast
 * iterators; lane scripts pin the ArrayList iterator contract; reflection checks the lane
 * invariants; a public-API model covers removeNode, and a hub probe reports the speed-up.
 */
//FLLDCIM: LEETCODIFY — differential proof for the DirectedGraphConnections slot lane
class DirectedGraphConnectionsLaneTest {

    private static final Logger LOG = System.getLogger(DirectedGraphConnectionsLaneTest.class
            .getName());
    private static final int SEEDS = Integer.getInteger("lane.seeds", 500);
    private static final int OPS = Integer.getInteger("lane.ops", 2_000);
    private static final int LANE_SEEDS = 200;
    private static final int GRAPH_SEEDS = 60;
    private static final int GRAPH_OPS = 1_000;
    private static final int COPY_EVERY = 250;
    private static final int MIN_UNIVERSE = 2;
    private static final int SMALL_UNIVERSE = 10;
    private static final int LARGE_UNIVERSE = 24;
    private static final int LARGE_EVERY = 4;
    private static final int MIN_PHASE = 20;
    private static final int PHASE_SPAN = 180;
    private static final int UNIVERSE = 12;
    private static final int RETAINED = 64;
    private static final int GC_ROUNDS = 50;
    private static final long GC_PAUSE_MILLIS = 10;
    private static final int HUB_DEGREE = Integer.getInteger("lane.hubDegree", 200_000);
    private static final Object VALUE = new Object();

    @Test
    void randomStableOpsMatchReference() {
        IntStream.range(0, SEEDS).forEach(seed -> runConnections(seed, true));
    }

    @Test
    void randomUnorderedOpsMatchReference() {
        IntStream.range(0, SEEDS).forEach(seed -> runConnections(seed, false));
    }

    private static void runConnections(int seed, boolean stable) {
        Random random = new Random(seed);
        LaneTwin twin = LaneTwin.mutable(stable, universe(random, seed), random.nextInt());
        int phase = MIN_PHASE + random.nextInt(PHASE_SPAN);
        IntStream.range(0, OPS).forEach(op -> {
            String at = (stable ? "stable" : "unordered") + " seed " + seed + " op " + op;
            twin.randomOp(random, op / phase % 2 == 0, at);
            twin.touchIterators(random, at);
        });
    }

    private static int universe(Random random, int seed) {
        return MIN_UNIVERSE
                + random.nextInt(seed % LARGE_EVERY == 0 ? LARGE_UNIVERSE : SMALL_UNIVERSE);
    }

    @Test
    void laneMatchesArrayListInLockstep() {
        IntStream.range(0, LANE_SEEDS).forEach(seed -> {
            Random random = new Random(seed);
            LaneLockstep lockstep = new LaneLockstep(universe(random, seed));
            int phase = MIN_PHASE + random.nextInt(PHASE_SPAN);
            IntStream.range(0, OPS).forEach(op -> lockstep.randomStep(random, op / phase % 2 == 0));
        });
    }

    @Test
    void exhaustedNextThrowsNoSuchElement() {
        assertEquals("T i p0 NoSuchElementException", script("+p0 i n n"));
    }

    @Test
    void exhaustedThenAddedThrowsCme() {
        assertEquals("T T i p0 p1 F T T ConcurrentModificationException",
                script("+p0 +p1 i n n h +s0 h n"));
    }

    @Test
    void removeBehindCursorEndsIteration() {
        assertEquals("T T T i p0 p1 T F ConcurrentModificationException",
                script("+p0 +p1 +p2 i n n -p0 h n"));
    }

    @Test
    void removeTwoBehindThrowsCme() {
        assertEquals("T T T i p0 p1 T T T ConcurrentModificationException",
                script("+p0 +p1 +p2 i n n -p0 -p1 h n"));
    }

    @Test
    void removeAheadThrowsCme() {
        assertEquals("T T T T i p0 T T ConcurrentModificationException",
                script("+p0 +p1 +p2 +p3 i n -p3 h n"));
    }

    @Test
    void compactionMidIterationThrowsCme() {
        assertEquals("T T T T T T i p0 T T T T T ConcurrentModificationException",
                script("+p0 +p1 +p2 +p3 +p4 +p5 i n -p3 -p4 -p5 -p1 h n"));
    }

    @Test
    void failedRemoveKeepsIterating() {
        assertEquals("T T i p0 F F F T s1 F", script("+p0 +s1 i n -s0 -p1 -p9 h n h"));
    }

    private static String script(String script) {
        return new LaneLockstep(UNIVERSE).run(script);
    }

    @Test
    void nonStructuralChangesKeepIterating() {
        LaneTwin twin = LaneTwin.mutable(true, UNIVERSE, 0);
        LaneKey[] k = keys();
        twin.apply(c -> LaneTwin.addPredecessor(c, k[1], VALUE), "p1");
        twin.apply(c -> c.addSuccessor(k[2], VALUE), "s2");
        twin.apply(c -> LaneTwin.addPredecessor(c, k[3], VALUE), "p3");
        Iterator<?>[] pair = twin.open(LaneTwin.INCIDENT);
        LaneOutcomes.step(pair[0], pair[1], LaneOutcomes.NEXT, "first");
        applyNonStructural(twin, k);
        assertRestMatches(pair);
    }

    /** Re-add a predecessor, replace a successor value, remove two non-members. */
    private static void applyNonStructural(LaneTwin twin, LaneKey[] k) {
        LaneKey[] again = keys();
        Object replacement = new Object();
        twin.apply(c -> LaneTwin.addPredecessor(c, again[1], VALUE), "re-add p1");
        twin.apply(c -> c.addSuccessor(again[2], replacement), "replace s2 value");
        twin.apply(c -> c.removeSuccessor(k[5]), "remove absent s5");
        twin.apply(c -> LaneTwin.removePredecessor(c, k[2]), "remove absent p2");
    }

    private static void assertRestMatches(Iterator<?>[] pair) {
        List<Object> expected = LaneOutcomes.drain(pair[0]);
        List<Object> actual = LaneOutcomes.drain(pair[1]);
        assertEquals(2, actual.size(), "rest of the iteration");
        LaneOutcomes.assertSameSequence(expected.iterator(), actual.iterator(), "rest");
    }

    @Test
    void wrapperCompactionThrowsCme() {
        LaneTwin twin = LaneTwin.mutable(true, UNIVERSE, 0);
        LaneKey[] k = keys();
        IntStream.rangeClosed(1, 6).forEach(id ->
                twin.apply(c -> LaneTwin.addPredecessor(c, k[id], VALUE), "add " + id));
        Iterator<?>[] pair = twin.open(LaneTwin.PREDECESSORS);
        LaneOutcomes.step(pair[0], pair[1], LaneOutcomes.NEXT, "first");
        IntStream.of(4, 5, 6, 2).forEach(id ->
                twin.apply(c -> LaneTwin.removePredecessor(c, k[id]), "remove " + id));
        Object outcome = LaneOutcomes.step(pair[0], pair[1], LaneOutcomes.HAS_NEXT, "hasNext");
        assertInstanceOf(ConcurrentModificationException.class, outcome, "fail-fast");
    }

    private static LaneKey key(int id) {
        return new LaneKey(id, -id);
    }

    /** One instance per id, shared by both sides of a twin. */
    private static LaneKey[] keys() {
        return IntStream.range(0, UNIVERSE).mapToObj(DirectedGraphConnectionsLaneTest::key)
                .toArray(LaneKey[]::new);
    }

    @Test
    void removedNodesAreReclaimable() {
        assertReclaimable(DirectedGraphConnectionsReference.of(ElementOrder.stable()));
        assertReclaimable(DirectedGraphConnections.of(ElementOrder.stable()));
    }

    private static void assertReclaimable(GraphConnections<LaneKey, Object> connections) {
        List<WeakReference<LaneKey>> refs = IntStream.rangeClosed(1, RETAINED)
                .mapToObj(id -> attach(connections, id)).toList();
        IntStream.rangeClosed(1, RETAINED).filter(id -> id % 2 == 0)
                .forEach(id -> detach(connections, id));
        IntStream.rangeClosed(1, RETAINED).filter(id -> id % 2 != 0)
                .forEach(id -> detach(connections, id));
        assertTrue(connections.adjacentNodes().isEmpty(), "all removed");
        assertTrue(IntStream.range(0, GC_ROUNDS).anyMatch(round -> cleared(refs)),
                "removed nodes reclaimed by " + connections.getClass().getSimpleName());
        Reference.reachabilityFence(connections);
    }

    private static WeakReference<LaneKey> attach(GraphConnections<LaneKey, Object> c, int id) {
        LaneKey node = new LaneKey(id, id);
        c.addPredecessor(node, VALUE);
        c.addSuccessor(node, VALUE);
        return new WeakReference<>(node);
    }

    private static void detach(GraphConnections<LaneKey, Object> c, int id) {
        c.removeSuccessor(key(id));
        c.removePredecessor(key(id));
    }

    private static boolean cleared(List<WeakReference<LaneKey>> refs) {
        System.gc();
        if (refs.stream().allMatch(ref -> ref.refersTo(null))) {
            return true;
        }
        try {
            Thread.sleep(GC_PAUSE_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    @Test
    void graphApiMatchesModel() {
        IntStream.range(0, GRAPH_SEEDS).forEach(seed -> {
            Random random = new Random(seed);
            GraphTwin twin = new GraphTwin(MIN_UNIVERSE + random.nextInt(SMALL_UNIVERSE));
            IntStream.range(0, GRAPH_OPS).forEach(op -> {
                String at = "graph seed " + seed + " op " + op;
                twin.randomOp(random, at);
                if (op % COPY_EVERY == 0) {
                    twin.compareImmutableCopy(at);
                }
            });
        });
    }

    @Test
    void immutableConnectionsMatchReference() {
        IntStream.range(0, SEEDS).forEach(seed -> {
            Random random = new Random(seed);
            LaneKey self = new LaneKey(0, seed);
            Map<Integer, Object> values = new HashMap<>();
            LaneTwin.immutable(self, LaneTwin.randomEdges(random, self, UNIVERSE),
                    node -> values.computeIfAbsent(node.id, id -> new Object()), UNIVERSE)
                    .compareViews("immutable seed " + seed);
        });
    }

    @Test
    void hubRemovalScalesLinearly() {
        MutableValueGraph<Integer, Object> graph = ValueGraphBuilder.directed()
                .incidentEdgeOrder(ElementOrder.<Integer>stable()).build();
        Integer hub = -1;
        IntStream.range(0, HUB_DEGREE).forEach(leaf -> {
            graph.putEdgeValue(hub, leaf, VALUE);
            graph.putEdgeValue(leaf, hub, VALUE);
        });
        long start = System.nanoTime();
        assertTrue(graph.removeNode(hub), "hub removed");
        long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        LOG.log(Level.INFO, "removeNode(hub) with {0} successors and predecessors: {1} ms",
                HUB_DEGREE, millis);
        assertEquals(HUB_DEGREE, graph.nodes().size(), "leaves kept");
        assertEquals(0, graph.edges().size(), "no edges left");
    }
}
