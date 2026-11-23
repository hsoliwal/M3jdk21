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
package jdk.m3.collections.eclipse.impl.map.sorted.mutable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.TreeMap;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.map.MapIterable;
import jdk.m3.collections.eclipse.api.map.MutableMapIterable;
import jdk.m3.collections.eclipse.api.ordered.OrderedIterable;
import jdk.m3.collections.eclipse.impl.map.ordered.mutable.OrderedHashMap;
import jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastKeys.Key;
import jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastKeys.Val;

/** Pre-patch reference paths, outcome capture, probes and lockstep lanes for getLast. */
final class GetLastOracle {

    /** Stands for Optional.empty() when outcomes are compared by identity. */
    private static final Object NO_VALUE = new Object();

    private GetLastOracle() {
    }

    /** A returned value, or the class and message of what was thrown. */
    record Outcome(Object value, Class<?> thrown, String message) {
    }

    /** One owner with its wrappers, its valuesView counter and its java.util model. */
    record Lane(MutableMapIterable<Key, Val> map, OrderedIterable<Val> ordered,
            List<Supplier<OrderedIterable<Val>>> wrappers, SequencedMap<Key, Val> model,
            Supplier<Map<Key, Val>> source, IntSupplier views) {
    }

    /** TreeSortedMap that counts valuesView calls, the pre-patch getLast entry point. */
    @SuppressWarnings("serial")
    static final class ProbeTreeSortedMap extends TreeSortedMap<Key, Val> {
        private static final long serialVersionUID = 1L;
        int views;

        ProbeTreeSortedMap(Comparator<? super Key> order) {
            super(order);
        }

        @Override
        public RichIterable<Val> valuesView() {
            this.views++;
            return super.valuesView();
        }
    }

    /** OrderedHashMap that counts valuesView calls, the pre-patch getLast entry point. */
    @SuppressWarnings("serial")
    static final class ProbeOrderedHashMap extends OrderedHashMap<Key, Val> {
        private static final long serialVersionUID = 1L;
        int views;

        ProbeOrderedHashMap(int capacity) {
            super(capacity);
        }

        ProbeOrderedHashMap() {
            super();
        }

        @Override
        public RichIterable<Val> valuesView() {
            this.views++;
            return super.valuesView();
        }
    }

    static Lane treeLane(Comparator<? super Key> order) {
        ProbeTreeSortedMap map = new ProbeTreeSortedMap(order);
        List<Supplier<OrderedIterable<Val>>> wrappers =
                List.of(map::asUnmodifiable, map::asSynchronized);
        return new Lane(map, map, wrappers, new TreeMap<>(order),
                () -> new TreeMap<>(order), () -> map.views);
    }

    static Lane orderedLane(ProbeOrderedHashMap map) {
        List<Supplier<OrderedIterable<Val>>> wrappers =
                List.of(map::asUnmodifiable, map::asSynchronized);
        return new Lane(map, map, wrappers, new LinkedHashMap<>(), LinkedHashMap::new,
                () -> map.views);
    }

    /** Captures the value or the thrown class and message of one call. */
    static Outcome run(Supplier<?> action) {
        try {
            return new Outcome(action.get(), null, null);
        } catch (Throwable failure) {
            return new Outcome(null, failure.getClass(), failure.getMessage());
        }
    }

    /** Verbatim pre-patch body of AbstractMapIterable.getLast for both owners. */
    @SuppressWarnings("deprecation")
    static <V> V prePatchGetLast(MapIterable<?, V> map) {
        return map.valuesView().getLast();
    }

    /** Verbatim OrderedIterable.getLastOptional default over the pre-patch getLast. */
    static <V> Optional<V> prePatchGetLastOptional(MapIterable<?, V> map) {
        if (map.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(prePatchGetLast(map));
    }

    private static Object unwrap(Object value) {
        return value instanceof Optional<?> optional
                ? optional.isPresent() ? optional.get() : NO_VALUE : value;
    }

    static void assertSameOutcome(Outcome expected, Outcome actual, String at) {
        assertEquals(expected.thrown(), actual.thrown(), at + " thrown");
        assertEquals(expected.message(), actual.message(), at + " message");
        assertSame(unwrap(expected.value()), unwrap(actual.value()), at + " identity");
        assertEquals(expected.value(), actual.value(), at + " equals");
    }

    /** Patched getLast/getLastOptional, then each wrapper's pair; no callback. */
    static List<Outcome> observe(Lane lane, String at, boolean fast) {
        int views = lane.views().getAsInt();
        long calls = GetLastKeys.callbacks;
        List<Outcome> outcomes = new ArrayList<>();
        outcomes.add(run(lane.ordered()::getLast));
        outcomes.add(run(lane.ordered()::getLastOptional));
        lane.wrappers().forEach(wrapper -> {
            outcomes.add(run(() -> wrapper.get().getLast()));
            outcomes.add(run(() -> wrapper.get().getLastOptional()));
        });
        assertTrue(!fast || views == lane.views().getAsInt(), at + " valuesView calls");
        assertEquals(calls, GetLastKeys.callbacks, at + " callbacks");
        return outcomes;
    }

    /** Fast-path comparison: patched outcomes equal the pre-patch ones without valuesView. */
    static Outcome compareToReference(Lane lane, String at) {
        return compareToReference(lane, at, true);
    }

    /** Every patched outcome equals the pre-patch one; the reference makes no callback. */
    static Outcome compareToReference(Lane lane, String at, boolean fast) {
        List<Outcome> outcomes = observe(lane, at, fast);
        long calls = GetLastKeys.callbacks;
        Outcome last = run(() -> prePatchGetLast(lane.map()));
        Outcome optional = run(() -> prePatchGetLastOptional(lane.map()));
        assertEquals(calls, GetLastKeys.callbacks, at + " reference callbacks");
        IntStream.range(0, outcomes.size()).forEach(i -> assertSameOutcome(
                i % 2 == 0 ? last : optional, outcomes.get(i), at + " #" + i));
        return outcomes.get(0);
    }

    /** Full lockstep check: reference, model, no mutation, equal to the model. */
    static void check(Lane lane, String at) {
        String before = snapshot(lane.map());
        Outcome last = compareToReference(lane, at);
        assertTrue(last.thrown() == null, at + " threw " + last.thrown());
        assertSame(modelLast(lane.model()), last.value(), at + " model");
        assertEquals(before, snapshot(lane.map()), at + " unchanged");
        assertEquals(lane.model(), lane.map(), at + " model equality");
        assertEquals(lane.model().keySet().stream().toList(), lane.map().keysView().toList(),
                at + " key order");
    }

    /** A plain map (no probe): getLast and getLastOptional equal the pre-patch paths. */
    static <V> void assertLastMatches(MapIterable<?, V> map, OrderedIterable<V> same, String at) {
        assertSameOutcome(run(() -> prePatchGetLast(map)), run(same::getLast), at + " getLast");
        assertSameOutcome(run(() -> prePatchGetLastOptional(map)), run(same::getLastOptional),
                at + " getLastOptional");
    }

    static String snapshot(MutableMapIterable<?, ?> map) {
        return map.size() + "|" + map + "|" + map.hashCode() + "|" + map.keysView().toList();
    }

    static <V> V modelLast(SequencedMap<?, V> model) {
        return model.isEmpty() ? null : model.lastEntry().getValue();
    }
}
