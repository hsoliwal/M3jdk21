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
package jdk.m3.collections.eclipse.impl.map.mutable.primitive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import jdk.m3.collections.eclipse.api.iterator.IntIterator;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.IntArrayList;

/**
 * Twin-state comparison for the values-lane differential: every instance field of a map (slot
 * lanes with raw float/double bits, occupiedWithData, occupiedWithSentinels, sentinelValues,
 * copyKeysOnWrite, subclass logs), lane-array identity relative to frozen key sets, and the
 * observable order of toString, keysView, intIterator and the values view.
 */
//FLLDCIM: LEETCODIFY — twin-state oracle: lanes, counters, sentinels, aliasing, order
final class ValuesLaneState {

    private static final ClassValue<Field[]> FIELDS = new ClassValue<>() {
        @Override
        protected Field[] computeValue(Class<?> type) {
            List<Field> fields = new ArrayList<>();
            for (Class<?> c = type; c != Object.class; c = c.getSuperclass()) {
                Arrays.stream(c.getDeclaredFields())
                        .filter(f -> !Modifier.isStatic(f.getModifiers()))
                        .forEach(fields::add);
            }
            fields.forEach(f -> f.setAccessible(true));
            return fields.toArray(new Field[0]);
        }
    };

    private ValuesLaneState() {
    }

    /** Asserts that every instance field of p equals the same field of t, bit for bit. */
    static void assertSameLanes(Object p, Object t, String at) {
        for (Field field : FIELDS.get(p.getClass())) {
            Object a = read(field, p);
            Object b = read(field, t);
            assertTrue(sameValue(a, b), () -> at + " field " + field.getName()
                    + "\n patched=" + describe(a) + "\n reference=" + describe(b));
        }
    }

    /** Asserts the same lane-array identity relative to each frozen key set, and its contents. */
    static void assertSameAliasing(Object p, Object t, List<Object> frozenP, List<Object> frozenT,
            boolean contents, String at) {
        assertEquals(frozenT.size(), frozenP.size(), at + " frozen count");
        for (int i = 0; i < frozenP.size(); i++) {
            assertEquals(aliasMask(t, frozenT.get(i)), aliasMask(p, frozenP.get(i)),
                    at + " frozen alias " + i);
            if (contents) {
                assertEquals(frozenT.get(i).toString(), frozenP.get(i).toString(),
                        at + " frozen contents " + i);
            }
        }
    }

    /** Asserts the same observable order of the map, its keys, its iterator and its values. */
    static void assertSameOrder(AbstractMutableIntValuesMap p, AbstractMutableIntValuesMap t,
            ValuesLaneOwners owner, String at) {
        assertEquals(t.toString(), p.toString(), at + " toString");
        assertEquals(owner.keys.apply(t), owner.keys.apply(p), at + " keysView");
        assertEquals(iterated(t), iterated(p), at + " intIterator");
        assertEquals(t.values().makeString(), p.values().makeString(), at + " values view");
    }

    static IntArrayList iterated(AbstractMutableIntValuesMap map) {
        IntArrayList seen = new IntArrayList();
        IntIterator iterator = map.intIterator();
        while (iterator.hasNext()) {
            seen.add(iterator.next());
        }
        return seen;
    }

    private static long aliasMask(Object map, Object frozen) {
        long mask = 0L;
        int bit = 0;
        for (Field lane : FIELDS.get(map.getClass())) {
            for (Field held : FIELDS.get(frozen.getClass())) {
                if (lane.getType().isArray() && held.getType().isArray()) {
                    mask |= (read(lane, map) == read(held, frozen) ? 1L : 0L) << bit++;
                }
            }
        }
        return mask;
    }

    private static Object read(Field field, Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException e) {
            throw new AssertionError(field.getName(), e);
        }
    }

    private static boolean sameValue(Object a, Object b) {
        return switch (a) {
            case int[] x when b instanceof int[] y -> Arrays.equals(x, y);
            case long[] x when b instanceof long[] y -> Arrays.equals(x, y);
            case byte[] x when b instanceof byte[] y -> Arrays.equals(x, y);
            case short[] x when b instanceof short[] y -> Arrays.equals(x, y);
            case char[] x when b instanceof char[] y -> Arrays.equals(x, y);
            case float[] x when b instanceof float[] y -> Arrays.equals(rawBits(x), rawBits(y));
            case double[] x when b instanceof double[] y -> Arrays.equals(rawBits(x), rawBits(y));
            case null, default -> describe(a).equals(describe(b));
        };
    }

    private static int[] rawBits(float[] lane) {
        int[] bits = new int[lane.length];
        Arrays.setAll(bits, i -> Float.floatToRawIntBits(lane[i]));
        return bits;
    }

    private static long[] rawBits(double[] lane) {
        long[] bits = new long[lane.length];
        Arrays.setAll(bits, i -> Double.doubleToRawLongBits(lane[i]));
        return bits;
    }

    private static String describe(Object value) {
        if (value instanceof AbstractMutableIntValuesMap.SentinelValues s) {
            return "zero=" + s.containsZeroKey + "/" + s.zeroValue
                    + " one=" + s.containsOneKey + "/" + s.oneValue;
        }
        if (value instanceof Float f) {
            return "f" + Float.floatToRawIntBits(f);
        }
        if (value instanceof Double d) {
            return "d" + Double.doubleToRawLongBits(d);
        }
        return value != null && value.getClass().isArray() ? arrayText(value)
                : Objects.toString(value);
    }

    private static String arrayText(Object array) {
        return switch (array) {
            case int[] a -> Arrays.toString(a);
            case long[] a -> Arrays.toString(a);
            case byte[] a -> Arrays.toString(a);
            case short[] a -> Arrays.toString(a);
            case char[] a -> Arrays.toString(new String(a).chars().toArray());
            case float[] a -> Arrays.toString(rawBits(a));
            case double[] a -> Arrays.toString(rawBits(a));
            default -> array.getClass().getName();
        };
    }
}
