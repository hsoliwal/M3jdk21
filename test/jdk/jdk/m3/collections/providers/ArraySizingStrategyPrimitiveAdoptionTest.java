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
 *******************************************************************************/

/*
 * @test
 * @summary The Eclipse primitive array lists (Byte..Short) and the Brownies primitive gap lists
 *          (Boolean..Short) grow by an ArraySizingStrategy given at construction: with
 *          fastList() and gapList() exactly as without one (contents and backing-array length
 *          after every operation), with arrayList() and a lambda by their arithmetic; a strategy
 *          that returns too little is refused before the list changes (also FastList's insertion
 *          at an index); a list with a strategy is not serializable
 * @modules jdk.m3.collections/jdk.m3.collections.eclipse.impl.list.mutable.primitive:+open
 *          jdk.m3.collections/jdk.m3.collections.eclipse.impl.list.mutable
 *          jdk.m3.collections/jdk.m3.collections.brownies.primitive:+open
 *          jdk.m3.collections/jdk.m3.collections.hppc
 * @run main ArraySizingStrategyPrimitiveAdoptionTest
 */

import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;
import jdk.m3.collections.eclipse.impl.list.mutable.FastList;
import jdk.m3.collections.hppc.ArraySizingStrategy;

public class ArraySizingStrategyPrimitiveAdoptionTest {

    static long checks;
    static final String ECLIPSE = "jdk.m3.collections.eclipse.impl.list.mutable.primitive.";
    static final String BROWNIES = "jdk.m3.collections.brownies.primitive.";
    static final ArraySizingStrategy EXACT = (current, elements, additions) -> elements + additions;
    static final ArraySizingStrategy TOO_LITTLE = (current, elements, additions) -> current;

    static void same(Object expected, Object actual, String what) {
        checks++;
        if (!Objects.deepEquals(expected, actual)) {
            throw new AssertionError(
                    what + ": expected " + show(expected) + " actual " + show(actual));
        }
    }

    static String show(Object o) {
        if (o == null || !o.getClass().isArray()) {
            return String.valueOf(o);
        }
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < Array.getLength(o); i++) {
            b.append(i == 0 ? "" : ", ").append(Array.get(o, i));
        }
        return b.append(']').toString();
    }

    /** One primitive list class, seen through reflection. */
    static final class Kind {
        final String name;
        final Class<?> element;
        final boolean eclipse;
        final Field array;
        final Constructor<?> plain;
        final Constructor<?> sized;
        final Method add, addAt, addAll, addAllAt, ensure, removeAt, clear, toArray;

        Kind(String simple, Class<?> element, boolean eclipse) throws ReflectiveOperationException {
            Class<?> type = Class.forName((eclipse ? ECLIPSE : BROWNIES) + simple);
            Class<?> arrayType = element.arrayType();
            this.name = simple;
            this.element = element;
            this.eclipse = eclipse;
            this.array = type.getDeclaredField(eclipse ? "items" : "values");
            this.array.setAccessible(true);
            this.plain = type.getConstructor(int.class);
            this.sized = type.getConstructor(int.class, ArraySizingStrategy.class);
            this.add = type.getMethod("add", element);
            this.addAt = type.getMethod(eclipse ? "addAtIndex" : "add", int.class, element);
            this.addAll = type.getMethod(eclipse ? "addAll" : "addArray", arrayType);
            this.addAllAt = type.getMethod(
                    eclipse ? "addAllAtIndex" : "addArray", int.class, arrayType);
            this.ensure = type.getMethod("ensureCapacity", int.class);
            this.removeAt = type.getMethod(eclipse ? "removeAtIndex" : "remove", int.class);
            this.clear = type.getMethod("clear");
            this.toArray = type.getMethod("toArray");
        }

        Object value(int v) {
            if (element == boolean.class) {
                return v % 2 == 0;
            }
            if (element == byte.class) {
                return (byte) v;
            }
            if (element == char.class) {
                return (char) v;
            }
            if (element == short.class) {
                return (short) v;
            }
            if (element == long.class) {
                return (long) v;
            }
            if (element == float.class) {
                return (float) v;
            }
            return element == double.class ? (Object) (double) v : (Object) v;
        }

        Object values(int first, int count) {
            Object a = Array.newInstance(element, count);
            for (int i = 0; i < count; i++) {
                Array.set(a, i, value(first + i));
            }
            return a;
        }

        Object create(int capacity, ArraySizingStrategy sizing) throws Exception {
            return unwrap(() -> sizing == null
                    ? plain.newInstance(capacity)
                    : sized.newInstance(capacity, sizing));
        }

        int capacity(Object list) throws IllegalAccessException {
            return Array.getLength(array.get(list));
        }

        Object contents(Object list) throws Exception {
            return call(list, toArray);
        }

        int size(Object list) throws Exception {
            return Array.getLength(contents(list));
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static Object call(Object target, Method m, Object... args) throws Exception {
        return unwrap(() -> m.invoke(target, args));
    }

    static Object unwrap(Callable<Object> action) throws Exception {
        try {
            return action.call();
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException r) {
                throw r;
            }
            throw e;
        }
    }

    static String thrown(Callable<Object> action) throws Exception {
        try {
            action.call();
            return "nothing";
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName();
        }
    }

    static List<Kind> kinds() throws ReflectiveOperationException {
        List<Kind> kinds = new ArrayList<>();
        Class<?>[] elements = {byte.class, char.class, double.class, float.class, int.class,
                long.class, short.class};
        String[] names = {"Byte", "Char", "Double", "Float", "Int", "Long", "Short"};
        for (int i = 0; i < names.length; i++) {
            kinds.add(new Kind(names[i] + "ArrayList", elements[i], true));
        }
        kinds.add(new Kind("BooleanGapList", boolean.class, false));
        for (int i = 0; i < names.length; i++) {
            kinds.add(new Kind(names[i] + "GapList", elements[i], false));
        }
        return kinds;
    }

    public static void main(String[] args) throws Exception {
        List<Kind> kinds = kinds();
        for (Kind kind : kinds) {
            for (long seed = 1; seed <= 20; seed++) {
                run(kind, seed, (int) (seed % 5) * 3);
            }
            refusals(kind);
        }
        fastListInsertionRefusal();
        System.out.println("ArraySizingStrategyPrimitiveAdoptionTest: " + kinds.size()
                + " classes, " + checks + " checks passed");
    }

    /**
     * A plain list, one with the class's own policy as a strategy (must equal the plain one in
     * contents and capacity after every operation), one with arrayList() and one with a lambda
     * (each growth equal to the strategy's arithmetic).
     */
    static void run(Kind kind, long seed, int start) throws Exception {
        Random random = new Random(seed * 1_000_003L + kind.name.hashCode());
        Object plain = kind.create(start, null);
        ArraySizingStrategy own = kind.eclipse
                ? ArraySizingStrategy.fastList()
                : ArraySizingStrategy.gapList();
        Object same = kind.create(start, own);
        List<ArraySizingStrategy> strategies = List.of(ArraySizingStrategy.arrayList(), EXACT);
        List<Object> others = new ArrayList<>();
        for (ArraySizingStrategy s : strategies) {
            others.add(kind.create(start, s));
        }
        List<Object> all = new ArrayList<>(List.of(plain, same));
        all.addAll(others);
        for (int step = 0; step < 2_000; step++) {
            String at = kind + " seed " + seed + " step " + step;
            int size = kind.size(plain);
            int what = random.nextInt(10);
            int value = random.nextInt(1_000);
            int k = random.nextInt(12);
            int index = size == 0 ? 0 : random.nextInt(size + 1);
            int min = size + random.nextInt(40);
            int needed = switch (what) {
                case 0, 1, 2, 3 -> 1;
                case 4, 5 -> k;
                case 6 -> min - size;
                default -> 0;
            };
            int[] before = new int[others.size()];
            for (int i = 0; i < others.size(); i++) {
                before[i] = kind.capacity(others.get(i));
            }
            for (Object list : all) {
                switch (what) {
                    case 0, 1, 2 -> call(list, kind.add, kind.value(value));
                    case 3 -> call(list, kind.addAt, index, kind.value(value));
                    case 4 -> call(list, kind.addAll, kind.values(value, k));
                    case 5 -> call(list, kind.addAllAt, index, kind.values(value, k));
                    case 6 -> call(list, kind.ensure, min);
                    case 7, 8 -> {
                        if (size > 0) {
                            call(list, kind.removeAt, what == 7 ? size / 2 : size - 1);
                        }
                    }
                    default -> {
                        if (size > 400) {
                            call(list, kind.clear);
                        }
                    }
                }
            }
            Object expected = kind.contents(plain);
            same(expected, kind.contents(same), at + " contents with " + own);
            same(kind.capacity(plain), kind.capacity(same), at + " capacity with " + own);
            for (int i = 0; i < others.size(); i++) {
                Object list = others.get(i);
                same(expected, kind.contents(list), at + " contents with " + strategies.get(i));
                int now = kind.capacity(list);
                if (needed > 0 && now != before[i]) {
                    same(strategies.get(i).grow(before[i], size, needed), now,
                            at + " growth with " + strategies.get(i));
                }
            }
        }
    }

    /**
     * Every insertion through a strategy that returns too little throws IllegalStateException and
     * leaves the list unchanged; null strategy; negative capacity as without a strategy;
     * serialization refused with a strategy, plain lists still serialize.
     */
    static void refusals(Kind kind) throws Exception {
        Object list = kind.create(2, TOO_LITTLE);
        call(list, kind.add, kind.value(1));
        call(list, kind.add, kind.value(2));
        Object contents = kind.contents(list);
        List<Callable<Object>> insertions = List.of(
                () -> call(list, kind.add, kind.value(3)),
                () -> call(list, kind.addAt, 1, kind.value(3)),
                () -> call(list, kind.addAt, 0, kind.value(3)),
                () -> call(list, kind.addAll, kind.values(3, 3)),
                () -> call(list, kind.addAllAt, 1, kind.values(3, 3)),
                () -> call(list, kind.ensure, 10));
        for (int i = 0; i < insertions.size(); i++) {
            same("IllegalStateException", thrown(insertions.get(i)),
                    kind + " short strategy, insertion " + i);
            same(contents, kind.contents(list), kind + " unchanged after refusal " + i);
            same(2, kind.capacity(list), kind + " capacity unchanged after refusal " + i);
        }
        same("NullPointerException", thrown(() -> unwrap(() -> kind.sized.newInstance(0, null))),
                kind + " null strategy");
        same(thrown(() -> kind.create(-1, null)),
                thrown(() -> kind.create(-1, ArraySizingStrategy.arrayList())),
                kind + " negative capacity as without a strategy");
        Object plain = kind.create(4, null);
        call(plain, kind.add, kind.value(1));
        same("written", serialize(plain), kind + " plain list serializes");
        Object sized = kind.create(4, ArraySizingStrategy.arrayList());
        call(sized, kind.add, kind.value(1));
        same("NotSerializableException", serialize(sized), kind + " with a strategy");
    }

    /** FastList's insertion at an index asks the strategy before the list changes. */
    static void fastListInsertionRefusal() throws Exception {
        FastList<Integer> list = new FastList<>(2, TOO_LITTLE);
        list.add(1);
        list.add(2);
        same("IllegalStateException", thrown(() -> {
            list.add(1, 3);
            return null;
        }), "FastList short strategy at an index");
        same("[1, 2]", list.toString(), "FastList unchanged after the refusal at an index");
        same(2, list.size(), "FastList size after the refusal at an index");
    }

    static String serialize(Object list) throws Exception {
        try (ObjectOutputStream out = new ObjectOutputStream(new ByteArrayOutputStream())) {
            out.writeObject(list);
            return "written";
        } catch (NotSerializableException e) {
            return "NotSerializableException";
        }
    }
}
