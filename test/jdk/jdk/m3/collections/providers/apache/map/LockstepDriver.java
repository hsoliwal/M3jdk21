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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Drives {@code LinkedMap.asList()} (the system under test) and {@link ReferenceLinkedMapList}
 * in lockstep: twin maps changed by the same operations, or one map seen through both views.
 * An outcome is the value or the exception type, message, cause type and cause message, then
 * the {@link ReentrantKey} equals calls and the counting map's call deltas; every outcome and,
 * for twin maps, the keys and values after every operation must be equal.
 */
final class LockstepDriver {

    /** The side viewed through {@code LinkedMap.asList()}. */
    private final LockstepSide sut;

    /** The side viewed through the reference copy. */
    private final LockstepSide ref;

    /** Whether both sides view one map. */
    private final boolean shared;

    /** Where the driver is, for failure messages. */
    private String at = "";

    LockstepDriver(final Supplier<LinkedMap<Object, Object>> maps, final boolean shared) {
        final LinkedMap<Object, Object> map = maps.get();
        this.sut = new LockstepSide(map, LinkedMap::asList);
        this.ref = new LockstepSide(shared ? map : maps.get(), ReferenceLinkedMapList::new);
        this.shared = shared;
    }

    void at(final String where) {
        at = where;
    }

    boolean shared() {
        return shared;
    }

    int size() {
        return sut.map.size();
    }

    boolean has(final int slot) {
        assertEquals(ref.has(slot), sut.has(slot), at + "slot " + slot);
        return sut.has(slot);
    }

    /**
     * Runs one operation on both sides and asserts equal outcomes and, for twin maps, equal maps.
     *
     * @param what The operation, for failure messages
     * @param changes The changes an armed {@link ReentrantKey} may make
     * @param op The operation
     * @return The outcome of the system under test
     */
    List<Object> both(final String what, final int changes,
            final Function<LockstepSide, Object> op) {
        final List<Object> actual = run(sut, changes, op);
        final List<Object> expected = run(ref, changes, op);
        assertEquals(expected, actual, at + what);
        if (!shared) {
            assertEquals(new ArrayList<>(ref.map.keySet()), new ArrayList<>(sut.map.keySet()),
                    at + what + " keys");
            assertEquals(new ArrayList<>(ref.map.values()), new ArrayList<>(sut.map.values()),
                    at + what + " values");
        }
        return actual;
    }

    /**
     * Changes the map: once when shared, else both maps in lockstep.
     *
     * @param what The operation, for failure messages
     * @param op The operation
     * @return The outcome of the system under test
     */
    List<Object> map(final String what, final Function<LinkedMap<Object, Object>, Object> op) {
        if (shared) {
            return run(sut, 0, side -> op.apply(side.map));
        }
        return both(what, 0, side -> op.apply(side.map));
    }

    List<Object> open(final int slot, final String what,
            final Function<List<Object>, ListIterator<Object>> op) {
        return both(what, 0, side -> side.open(slot, op.apply(side.view())));
    }

    List<Object> iter(final int slot, final String what,
            final Function<ListIterator<Object>, Object> op) {
        has(slot);
        return both(what, 0, side -> op.apply(side.iterator(slot)));
    }

    private static List<Object> run(final LockstepSide side, final int changes,
            final Function<LockstepSide, Object> op) {
        final List<Integer> before = side.counts();
        ReentrantKey.arm(side.map, changes);
        final List<Object> outcome = capture(side, op);
        outcome.add(ReentrantKey.disarm());
        final List<Integer> after = side.counts();
        outcome.add(IntStream.range(0, after.size())
                .mapToObj(i -> after.get(i) - before.get(i)).toList());
        return outcome;
    }

    private static List<Object> capture(final LockstepSide side,
            final Function<LockstepSide, Object> op) {
        final List<Object> outcome = new ArrayList<>();
        try {
            outcome.add(op.apply(side));
            outcome.addAll(List.of("", "", "", ""));
        } catch (final RuntimeException e) {
            final Throwable cause = e.getCause();
            outcome.add("thrown");
            outcome.add(e.getClass());
            outcome.add(String.valueOf(e.getMessage()));
            outcome.add(cause == null ? "" : cause.getClass());
            outcome.add(cause == null ? "" : String.valueOf(cause.getMessage()));
        }
        return outcome;
    }
}
