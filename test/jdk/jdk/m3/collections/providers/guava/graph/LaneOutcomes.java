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
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Outcome capture and comparison for lockstep runs: a call yields its result or the runtime
 * exception it threw; results compare by identity, pairs by their identical ends, connections by
 * kind and identical node, exceptions by class and message.
 */
final class LaneOutcomes {

    static final int HAS_NEXT = 0;
    static final int NEXT = 1;
    static final int REMOVE = 2;

    private LaneOutcomes() {
    }

    static Object outcome(Supplier<?> call) {
        try {
            return call.get();
        } catch (RuntimeException e) {
            return e;
        }
    }

    /** The comparison of {@link #assertSameOutcome} as a predicate, without building messages. */
    static boolean sameOutcome(Object expected, Object actual) {
        if (expected instanceof RuntimeException e) {
            return actual != null && e.getClass() == actual.getClass()
                    && Objects.equals(e.getMessage(), ((RuntimeException) actual).getMessage());
        }
        if (expected instanceof EndpointPair<?> pair) {
            return actual instanceof EndpointPair<?> other && pair.isOrdered() == other.isOrdered()
                    && pair.nodeU() == other.nodeU() && pair.nodeV() == other.nodeV();
        }
        if (LaneProbe.isConnection(expected)) {
            return LaneProbe.kind(expected).equals(LaneProbe.kind(actual))
                    && LaneProbe.node(expected) == LaneProbe.node(actual);
        }
        return expected == actual;
    }

    static void assertSameOutcome(Object expected, Object actual, String at) {
        if (sameOutcome(expected, actual)) {
            return;
        }
        if (expected instanceof RuntimeException e) {
            assertEquals(e.getClass(), actual == null ? null : actual.getClass(), at + " throws");
            assertEquals(e.getMessage(), ((RuntimeException) actual).getMessage(), at + " message");
        } else if (expected instanceof EndpointPair<?> pair) {
            assertSamePair(pair, actual, at);
        } else if (LaneProbe.isConnection(expected)) {
            assertEquals(LaneProbe.kind(expected), LaneProbe.kind(actual), at + " kind");
            assertSame(LaneProbe.node(expected), LaneProbe.node(actual), at + " node");
        } else {
            assertSame(expected, actual, at);
        }
    }

    private static void assertSamePair(EndpointPair<?> expected, Object actual, String at) {
        EndpointPair<?> pair = assertInstanceOf(EndpointPair.class, actual, at + " pair");
        assertEquals(expected.isOrdered(), pair.isOrdered(), at + " ordered");
        assertSame(expected.nodeU(), pair.nodeU(), at + " nodeU");
        assertSame(expected.nodeV(), pair.nodeV(), at + " nodeV");
    }

    /** Drains both iterators and compares length and every element. */
    static void assertSameSequence(Iterator<?> expected, Iterator<?> actual, String at) {
        List<Object> left = drain(expected);
        List<Object> right = drain(actual);
        if (left.size() == right.size() && IntStream.range(0, left.size())
                .allMatch(i -> sameOutcome(left.get(i), right.get(i)))) {
            return;
        }
        assertEquals(left.size(), right.size(), at + " length");
        IntStream.range(0, left.size())
                .forEach(i -> assertSameOutcome(left.get(i), right.get(i), at + " at " + i));
    }

    static List<Object> drain(Iterator<?> iterator) {
        List<Object> items = new ArrayList<>();
        iterator.forEachRemaining(items::add);
        return items;
    }

    /** One step on a suspended pair; after a throw, both are asked hasNext() once more. */
    static Object step(Iterator<?> expected, Iterator<?> actual, int action, String at) {
        Object left = outcome(() -> act(expected, action));
        Object right = outcome(() -> act(actual, action));
        assertSameOutcome(left, right, at);
        if (left instanceof RuntimeException) {
            assertSameOutcome(outcome(expected::hasNext), outcome(actual::hasNext), at + " after");
        }
        return right;
    }

    private static Object act(Iterator<?> iterator, int action) {
        return switch (action) {
            case HAS_NEXT -> iterator.hasNext();
            case NEXT -> iterator.next();
            default -> remove(iterator);
        };
    }

    private static Object remove(Iterator<?> iterator) {
        iterator.remove();
        return null;
    }
}
