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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import jdk.m3.collections.guava.collect.ImmutableList;

/**
 * The patched {@link DirectedGraphConnections} driven in lockstep with the verbatim pre-patch
 * {@link DirectedGraphConnectionsReference}: same calls, same node and value instances, then the
 * outcome, every view, the raw connection lane and the lane invariants are compared.
 */
final class LaneTwin {

    static final int PREDECESSORS = 0;
    static final int SUCCESSORS = 1;
    static final int ADJACENT = 2;
    static final int INCIDENT = 3;
    static final int RAW = 4;

    private static final int MAX_SUSPENDED = 6;
    private static final int PERCENT = 100;
    private static final int NULL_ODDS = 60;
    private static final int GROW_ADDS = 80;
    private static final int SHRINK_ADDS = 15;
    private static final int VALUE_SHARE = 5;
    private static final int ITERATOR_ROLLS = 4;
    private static final int OPEN_ROLL = 0;
    private static final int LAST_STEP_ROLL = 2;
    private static final int WRAPPER_ACTIONS = 3;
    private static final int RAW_ACTIONS = 2;
    private static final int PAIR = 2;
    private static final int EDGE_ROLLS = 4;
    private static final int IN_EDGE = 1;
    private static final int OUT_EDGE = 2;
    private static final String FOREIGN = "foreign";

    private enum Mode { UNORDERED, STABLE, IMMUTABLE }

    private final GraphConnections<LaneKey, Object> reference;
    private final GraphConnections<LaneKey, Object> patched;
    private final LaneKey self;
    private final int universe;
    private final Mode mode;
    private final List<Iterator<?>[]> suspended = new ArrayList<>();
    private int expectedModCount;

    private LaneTwin(GraphConnections<LaneKey, Object> reference,
            GraphConnections<LaneKey, Object> patched, LaneKey self, int universe, Mode mode) {
        this.reference = reference;
        this.patched = patched;
        this.self = self;
        this.universe = universe;
        this.mode = mode;
    }

    /** Mutable connections with a stable or an unordered incident edge order. */
    static LaneTwin mutable(boolean stable, int universe, int tag) {
        ElementOrder<LaneKey> order = stable ? ElementOrder.stable() : ElementOrder.unordered();
        return new LaneTwin(DirectedGraphConnectionsReference.of(order),
                DirectedGraphConnections.of(order), new LaneKey(0, tag), universe,
                stable ? Mode.STABLE : Mode.UNORDERED);
    }

    /** Immutable connections built by ofImmutable from the same edges and value function. */
    static LaneTwin immutable(LaneKey self, List<EndpointPair<LaneKey>> edges,
            jdk.m3.collections.guava.base.Function<LaneKey, Object> values, int universe) {
        return new LaneTwin(DirectedGraphConnectionsReference.ofImmutable(self, edges, values),
                DirectedGraphConnections.ofImmutable(self, edges, values), self, universe,
                Mode.IMMUTABLE);
    }

    /** Shuffled incident edges of {@code self}: per other node none, in, out or both; a loop. */
    static List<EndpointPair<LaneKey>> randomEdges(Random random, LaneKey self, int universe) {
        List<EndpointPair<LaneKey>> edges = new ArrayList<>();
        IntStream.range(1, universe).forEach(id -> addEdges(edges, self,
                new LaneKey(id, random.nextInt()), random.nextInt(EDGE_ROLLS)));
        if (random.nextBoolean()) {
            edges.add(EndpointPair.ordered(self, self));
        }
        Collections.shuffle(edges, random);
        return edges;
    }

    private static void addEdges(List<EndpointPair<LaneKey>> edges, LaneKey self, LaneKey other,
            int roll) {
        if ((roll & IN_EDGE) != 0) {
            edges.add(EndpointPair.ordered(other, self));
        }
        if ((roll & OUT_EDGE) != 0) {
            edges.add(EndpointPair.ordered(self, other));
        }
    }

    /** Applies one operation to both sides, then compares outcome, views and lane. */
    Object apply(Function<GraphConnections<LaneKey, Object>, Object> op, String at) {
        int before = rawSize();
        Object expected = LaneOutcomes.outcome(() -> op.apply(reference));
        Object actual = LaneOutcomes.outcome(() -> op.apply(patched));
        LaneOutcomes.assertSameOutcome(expected, actual, at + " result");
        expectedModCount += Math.abs(rawSize() - before);
        compareViews(at);
        return actual;
    }

    /** One weighted random operation; growing phases favour adds, shrinking ones removes. */
    void randomOp(Random random, boolean growing, String at) {
        LaneKey node = random.nextInt(NULL_ODDS) == 0
                ? null : new LaneKey(random.nextInt(universe), random.nextInt());
        Object value = random.nextInt(NULL_ODDS) == 0 ? null : new Object();
        int roll = random.nextInt(PERCENT);
        if (roll < (growing ? GROW_ADDS : SHRINK_ADDS)) {
            apply(random.nextBoolean() ? c -> addPredecessor(c, node, value)
                    : c -> c.addSuccessor(node, value), at + " add " + node);
        } else if (roll < PERCENT - VALUE_SHARE) {
            apply(random.nextBoolean() ? c -> removePredecessor(c, node)
                    : c -> c.removeSuccessor(node), at + " remove " + node);
        } else {
            apply(c -> c.value(node), at + " value " + node);
        }
    }

    static Object addPredecessor(GraphConnections<LaneKey, Object> c, LaneKey node, Object v) {
        c.addPredecessor(node, v);
        return null;
    }

    static Object removePredecessor(GraphConnections<LaneKey, Object> c, LaneKey node) {
        c.removePredecessor(node);
        return null;
    }

    /** Between operations: open a suspended iterator pair, step one, or leave them alone. */
    void touchIterators(Random random, String at) {
        int roll = random.nextInt(ITERATOR_ROLLS);
        if (roll == OPEN_ROLL) {
            if (suspended.size() == MAX_SUSPENDED) {
                suspended.remove(random.nextInt(MAX_SUSPENDED));
            }
            suspended.add(open(random.nextInt(mode == Mode.STABLE ? RAW + 1 : RAW)));
        } else if (roll <= LAST_STEP_ROLL && !suspended.isEmpty()) {
            Iterator<?>[] pair = suspended.get(random.nextInt(suspended.size()));
            int actions = pair.length > PAIR ? RAW_ACTIONS : WRAPPER_ACTIONS;
            LaneOutcomes.step(pair[0], pair[1], random.nextInt(actions), at + " iterator");
        }
    }

    /** A reference/patched iterator pair of one kind; a raw lane pair has a third marker slot. */
    Iterator<?>[] open(int kind) {
        Iterator<?> expected = iterator(reference, kind);
        Iterator<?> actual = iterator(patched, kind);
        return kind == RAW ? new Iterator<?>[] {expected, actual, null}
                : new Iterator<?>[] {expected, actual};
    }

    private Iterator<?> iterator(GraphConnections<LaneKey, Object> side, int kind) {
        return switch (kind) {
            case PREDECESSORS -> side.predecessors().iterator();
            case SUCCESSORS -> side.successors().iterator();
            case ADJACENT -> side.adjacentNodes().iterator();
            case INCIDENT -> side.incidentEdgeIterator(self);
            default -> ((Collection<?>) LaneProbe.ordered(side)).iterator();
        };
    }

    void compareViews(String at) {
        compareSet(reference.predecessors(), patched.predecessors(), at + " predecessors");
        compareSet(reference.successors(), patched.successors(), at + " successors");
        compareSet(reference.adjacentNodes(), patched.adjacentNodes(), at + " adjacent");
        LaneOutcomes.assertSameSequence(reference.incidentEdgeIterator(self),
                patched.incidentEdgeIterator(self), at + " incident");
        switch (mode) {
            case UNORDERED -> assertNull(LaneProbe.ordered(patched), at + " I9 no lane");
            case STABLE -> compareLane(at);
            case IMMUTABLE -> compareImmutable(at);
        }
    }

    private void compareLane(String at) {
        Collection<?> expected = (Collection<?>) LaneProbe.ordered(reference);
        Collection<?> lane = (Collection<?>) LaneProbe.ordered(patched);
        LaneOutcomes.assertSameSequence(expected.iterator(), lane.iterator(), at + " I1 lane");
        assertEquals(expected.size(), lane.size(), at + " I2 lane size");
        LaneProbe.check(lane, at);
        assertEquals(expectedModCount, LaneProbe.modCount(lane), at + " I6 modCount");
    }

    private void compareImmutable(String at) {
        Collection<?> expected = (Collection<?>) LaneProbe.ordered(reference);
        Object actual = LaneProbe.ordered(patched);
        assertInstanceOf(ImmutableList.class, actual, at + " I9 immutable list");
        LaneOutcomes.assertSameSequence(expected.iterator(), ((Collection<?>) actual).iterator(),
                at + " immutable lane");
    }

    private void compareSet(Set<LaneKey> expected, Set<LaneKey> actual, String at) {
        LaneOutcomes.assertSameSequence(expected.iterator(), actual.iterator(), at);
        assertEquals(expected.size(), actual.size(), at + " size");
        assertEquals(expected.isEmpty(), actual.isEmpty(), at + " isEmpty");
        assertEquals(expected.toString(), actual.toString(), at + " toString");
        assertEquals(expected.hashCode(), actual.hashCode(), at + " hashCode");
        assertTrue(actual.equals(expected) && expected.equals(actual), at + " equals");
        assertEquals(containsMask(expected), containsMask(actual), at + " contains");
    }

    private String containsMask(Set<LaneKey> set) {
        return IntStream.rangeClosed(0, universe)
                .mapToObj(id -> set.contains(new LaneKey(id, -1)) ? "1" : "0")
                .collect(Collectors.joining()) + set.contains(FOREIGN);
    }

    private int rawSize() {
        return mode == Mode.STABLE ? ((Collection<?>) LaneProbe.ordered(reference)).size() : 0;
    }
}
