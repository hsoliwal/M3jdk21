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
package jdk.m3.collections.apache.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import jdk.m3.collections.apache.iterators.UnmodifiableListIterator;
import org.junit.jupiter.api.Test;

/**
 * LinkedMap.asList()'s list iterator walks the entry links for a plain LinkedMap: in lockstep
 * with a verbatim copy of the old list view on {@code AbstractList}'s iterator, equal values,
 * exceptions with messages and causes, cursors, maps and reentrant equals calls under random
 * map changes; subclasses keep the old iterator with the same calls; a full pass is linear.
 */
//FLLDCIM: LEETCODIFY — lockstep proof for LinkedMap.LinkedMapList.listIterator link walk
class LinkedMapListIteratorLockstepTest {

    /** Random scripts per scenario. */
    private static final int SEEDS = 800;

    /** Steps per script. */
    private static final int STEPS = 400;

    /** Keys of the complexity smoke. */
    private static final int BIG = 300_000;

    /** The delegate field of the unmodifiable wrapper. */
    private static final String DELEGATE = "iterator";

    private static void scripts(final int seeds, final Supplier<LinkedMap<Object, Object>> maps,
            final boolean shared, final boolean reentrant) {
        IntStream.range(0, seeds).forEach(seed -> {
            final LockstepDriver d = new LockstepDriver(maps, shared);
            final Random rnd = new Random(seed);
            IntStream.range(0, STEPS).forEach(step -> {
                d.at("seed " + seed + " step " + step + ": ");
                LockstepOps.step(d, rnd, reentrant);
            });
        });
    }

    @SuppressWarnings("serial")
    private static LinkedMap<Object, Object> anonymous() {
        return new LinkedMap<>() { };
    }

    @Test
    void twinMapsMatchReference() {
        scripts(SEEDS, LinkedMap::new, false, false);
    }

    @Test
    void reentrantEqualsMatchesReference() {
        scripts(SEEDS, LinkedMap::new, false, true);
    }

    @Test
    void sharedMapViewsAgree() {
        scripts(SEEDS / 2, LinkedMap::new, true, false);
    }

    @Test
    void countingSubclassKeepsCalls() {
        scripts(SEEDS / 4, CountingLinkedMap::new, false, true);
    }

    @Test
    void anonymousSubclassKeepsPath() {
        scripts(SEEDS / 4, LinkedMapListIteratorLockstepTest::anonymous, false, true);
    }

    @Test
    void shiftedSubclassKeepsGet() {
        scripts(SEEDS / 4, ShiftedLinkedMap::new, false, false);
        final ShiftedLinkedMap map = new ShiftedLinkedMap();
        IntStream.range(0, 3).forEach(i -> map.put(i, i));
        final List<Object> keys = new ArrayList<>();
        map.asList().listIterator().forEachRemaining(keys::add);
        assertEquals(List.of(1, 2, 0), keys);
        assertEquals(1, map.asList().listIterator(1).previous());
        assertEquals(0, map.asList().listIterator(0).previous());
    }

    @Test
    void routeUsesLinkWalk() throws ReflectiveOperationException {
        final Field field = UnmodifiableListIterator.class.getDeclaredField(DELEGATE);
        field.setAccessible(true);
        final LinkedMap<Object, Object> plain = new LinkedMap<>();
        plain.put(1, 1);
        assertEquals("LinkWalkIterator", delegate(field, plain.asList().listIterator()));
        assertEquals("LinkWalkIterator", delegate(field, plain.asList().listIterator(1)));
        assertEquals("ListItr", delegate(field, new CountingLinkedMap().asList().listIterator()));
        assertEquals("ListItr", delegate(field, anonymous().asList().listIterator(0)));
        assertEquals("ListItr", delegate(field, new ShiftedLinkedMap().asList().listIterator()));
    }

    private static String delegate(final Field field, final ListIterator<Object> it)
            throws ReflectiveOperationException {
        assertEquals(UnmodifiableListIterator.class, it.getClass());
        final Object inner = field.get(it);
        assertFalse(inner instanceof UnmodifiableListIterator, "wrapped once");
        return inner.getClass().getSimpleName();
    }

    @Test
    void traversalIsLinear() {
        final LinkedMap<Object, Object> map = new LinkedMap<>();
        IntStream.range(0, BIG).forEach(i -> map.put(i, i));
        final LinkedMap<Object, Object> copy = new LinkedMap<>(map);
        final long forward = LongStream.range(0, BIG).reduce(0, (h, i) -> h * 31 + i);
        final long backward = LongStream.range(0, BIG).reduce(0, (h, i) -> h * 31 + BIG - 1 - i);
        assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> traverse(map, copy, forward, backward));
    }

    private static void traverse(final LinkedMap<Object, Object> map,
            final LinkedMap<Object, Object> copy, final long forward, final long backward) {
        final ListIterator<Object> up = map.asList().listIterator();
        assertEquals(forward, LongStream.range(0, BIG)
                .reduce(0, (h, i) -> h * 31 + (Integer) up.next()));
        assertFalse(up.hasNext());
        final ListIterator<Object> down = map.asList().listIterator(BIG);
        assertEquals(backward, LongStream.range(0, BIG)
                .reduce(0, (h, i) -> h * 31 + (Integer) down.previous()));
        assertFalse(down.hasPrevious());
        assertTrue(map.asList().equals(copy.asList()));
    }

    private static void assertNoSuch(final List<Object> outcome, final String cause) {
        assertEquals(NoSuchElementException.class, outcome.get(1), outcome::toString);
        assertEquals(IndexOutOfBoundsException.class, outcome.get(3), outcome::toString);
        assertEquals(cause, outcome.get(4), outcome::toString);
    }

    private static Object next(final LockstepDriver d) {
        return d.iter(0, "next", ListIterator::next).get(0);
    }

    private static Object previous(final LockstepDriver d) {
        return d.iter(0, "previous", ListIterator::previous).get(0);
    }

    @Test
    void emptyAndSingleEdges() {
        final LockstepDriver d = new LockstepDriver(LinkedMap::new, false);
        d.open(0, "listIterator(0)", v -> v.listIterator(0));
        assertNoSuch(d.iter(0, "next", ListIterator::next), "Index 0 is invalid for size 0");
        assertNoSuch(d.iter(0, "previous", ListIterator::previous), "Index -1 is less than zero");
        final List<Object> bad = d.open(1, "listIterator(1)", v -> v.listIterator(1));
        assertEquals(List.of(IndexOutOfBoundsException.class, "Index: 1, Size: 0"),
                bad.subList(1, 3));
        d.map("put a", m -> m.put("a", 1));
        assertEquals("a", next(d));
        assertNoSuch(d.iter(0, "next", ListIterator::next), "Index 1 is invalid for size 1");
        assertEquals("a", previous(d));
        assertNoSuch(d.iter(0, "previous", ListIterator::previous), "Index -1 is less than zero");
        assertEquals(List.of(UnsupportedOperationException.class, "set() is not supported"),
                set(d).subList(1, 3));
    }

    private static List<Object> set(final LockstepDriver d) {
        return d.iter(0, "set", it -> {
            it.set("b");
            return "set";
        });
    }

    @Test
    void cursorAtSizeSeesAppend() {
        final LockstepDriver d = new LockstepDriver(LinkedMap::new, false);
        IntStream.range(0, 3).forEach(i -> d.map("put " + i, m -> m.put(i, i)));
        d.open(0, "listIterator(3)", v -> v.listIterator(3));
        assertEquals(false, d.iter(0, "hasNext", ListIterator::hasNext).get(0));
        d.map("put 9", m -> m.put(9, 9));
        assertEquals(true, d.iter(0, "hasNext", ListIterator::hasNext).get(0));
        assertEquals(9, next(d));
        assertEquals(9, previous(d));
        assertEquals(2, previous(d));
        d.map("remove 2", m -> m.remove((Object) 2));
        assertEquals(9, next(d));
        d.map("remove 0", m -> m.remove((Object) 0));
        assertNoSuch(d.iter(0, "next", ListIterator::next), "Index 3 is invalid for size 2");
    }

    @Test
    void clearUnderLiveIterator() {
        final LockstepDriver d = new LockstepDriver(LinkedMap::new, false);
        IntStream.range(0, 5).forEach(i -> d.map("put " + i, m -> m.put(i, i)));
        d.open(0, "listIterator(2)", v -> v.listIterator(2));
        assertEquals(2, next(d));
        d.map("clear", m -> {
            m.clear();
            return "clear";
        });
        assertEquals(true, d.iter(0, "hasNext", ListIterator::hasNext).get(0));
        assertNoSuch(d.iter(0, "next", ListIterator::next), "Index 3 is invalid for size 0");
        assertNoSuch(d.iter(0, "previous", ListIterator::previous),
                "Index 2 is invalid for size 0");
        IntStream.range(10, 16).forEach(i -> d.map("put " + i, m -> m.put(i, i)));
        assertEquals(13, next(d));
        assertEquals(13, previous(d));
    }

    @Test
    void nullKeyThroughSentinel() {
        final LockstepDriver d = new LockstepDriver(LinkedMap::new, false);
        d.map("put null", m -> m.put(null, "n"));
        d.map("put 1", m -> m.put(1, 1));
        d.open(0, "listIterator()", List::listIterator);
        assertEquals(null, next(d));
        assertEquals(1, next(d));
        assertEquals(1, previous(d));
        assertEquals(null, previous(d));
        assertNoSuch(d.iter(0, "previous", ListIterator::previous), "Index -1 is less than zero");
        assertEquals(0, d.iter(0, "nextIndex", ListIterator::nextIndex).get(0));
    }
}
