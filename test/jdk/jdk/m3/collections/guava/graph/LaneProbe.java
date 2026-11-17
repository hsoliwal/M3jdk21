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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Reflective white-box access to the private connection lane of {@link DirectedGraphConnections}
 * and to the private connection classes of both it and {@link DirectedGraphConnectionsReference}.
 */
final class LaneProbe {

    private static final String OWNER = "jdk.m3.collections.guava.graph.DirectedGraphConnections";
    private static final String REFERENCE = OWNER + "Reference";
    private static final Class<?> LANE = type(OWNER + "$ConnectionLane");
    private static final Class<?> CONNECTION = type(OWNER + "$NodeConnection");
    private static final Class<?> REF_CONNECTION = type(REFERENCE + "$NodeConnection");
    private static final Field ORDERED = field(type(OWNER), "orderedNodeConnections");
    private static final Field REF_ORDERED = field(type(REFERENCE), "orderedNodeConnections");
    private static final Field NODE = field(CONNECTION, "node");
    private static final Field REF_NODE = field(REF_CONNECTION, "node");
    private static final Field SLOT = field(CONNECTION, "slot");
    private static final Field SLOTS = field(LANE, "slots");
    private static final Field END = field(LANE, "end");
    private static final Field LIVE = field(LANE, "live");
    private static final Field MOD_COUNT = field(LANE, "modCount");
    private static final Field INDEX = field(LANE, "index");
    private static final Object EMPTY = get(field(LANE, "EMPTY"), null);
    private static final Constructor<?> PRED = constructor(OWNER + "$NodeConnection$Pred");
    private static final Constructor<?> SUCC = constructor(OWNER + "$NodeConnection$Succ");

    private LaneProbe() {
    }

    /** The {@code orderedNodeConnections} field of either implementation. */
    static Object ordered(Object connections) {
        return get(connections instanceof DirectedGraphConnections ? ORDERED : REF_ORDERED,
                connections);
    }

    static boolean isConnection(Object value) {
        return CONNECTION.isInstance(value) || REF_CONNECTION.isInstance(value);
    }

    /** "Pred" or "Succ" for a connection; the class name for anything else. */
    static String kind(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }

    static Object node(Object connection) {
        return get(CONNECTION.isInstance(connection) ? NODE : REF_NODE, connection);
    }

    /** A new connection of the patched class. */
    static Object connection(boolean successor, Object node) {
        try {
            return (successor ? SUCC : PRED).newInstance(node);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    static int modCount(Object lane) {
        return (int) get(MOD_COUNT, lane);
    }

    /** Asserts I2 to I5 and that no slot holds null. */
    static void check(Object lane, String at) {
        Object[] slots = (Object[]) get(SLOTS, lane);
        int end = (int) get(END, lane);
        int live = (int) get(LIVE, lane);
        Map<?, ?> index = (Map<?, ?>) get(INDEX, lane);
        int[] used = IntStream.range(0, end).filter(i -> slots[i] != EMPTY).toArray();
        assertEquals(live, used.length, at + " I2 live slots");
        assertEquals(live, index.size(), at + " I2 index size");
        Arrays.stream(used).forEach(i -> checkSlot(slots[i], i, index, at));
        assertTrue(end - live <= live, at + " I4 tombstones <= live");
        assertTrue(IntStream.range(end, slots.length).allMatch(i -> slots[i] == EMPTY),
                at + " I5 tail EMPTY");
        assertTrue(Arrays.stream(slots).noneMatch(Objects::isNull), at + " no null slot");
    }

    private static void checkSlot(Object connection, int slot, Map<?, ?> index, String at) {
        assertEquals(slot, (int) get(SLOT, connection), at + " I3 slot " + slot);
        assertSame(connection, index.get(connection), at + " I3 index " + slot);
    }

    private static Class<?> type(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private static Field field(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    private static Object get(Field field, Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static Constructor<?> constructor(String name) {
        try {
            Constructor<?> constructor = type(name).getDeclaredConstructor(Object.class);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }
}
