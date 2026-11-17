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
 * @summary Eclipse FastList and Brownies GapList grow by an ArraySizingStrategy given at
 *          construction: with fastList() and gapList() exactly as without one (contents and
 *          backing-array length after every operation), with arrayList() and a lambda by their
 *          arithmetic; a strategy that returns too little is refused; a list with a strategy is
 *          not serializable
 * @modules jdk.m3.collections/jdk.m3.collections.eclipse.impl.list.mutable:+open
 *          jdk.m3.collections/jdk.m3.collections.brownies:+open
 *          jdk.m3.collections/jdk.m3.collections.hppc
 * @run main ArraySizingStrategyAdoptionTest
 */

import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import jdk.m3.collections.brownies.GapList;
import jdk.m3.collections.eclipse.impl.list.mutable.FastList;
import jdk.m3.collections.hppc.ArraySizingStrategy;

public class ArraySizingStrategyAdoptionTest {

    static long checks;
    static final Field FAST_LIST_ITEMS = field(FastList.class, "items");
    static final Field GAP_LIST_VALUES = field(GapList.class, "values");

    static void same(Object expected, Object actual, String what) {
        checks++;
        if (!Objects.equals(String.valueOf(expected), String.valueOf(actual))) {
            throw new AssertionError(what + ": expected " + expected + " actual " + actual);
        }
    }

    static Field field(Class<?> type, String name) {
        try {
            Field f = type.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    static int capacity(Object list) {
        try {
            Field f = list instanceof FastList ? FAST_LIST_ITEMS : GAP_LIST_VALUES;
            return ((Object[]) f.get(list)).length;
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static void main(String[] args) throws Exception {
        for (long seed = 1; seed <= 40; seed++) {
            int start = (int) (seed % 5) * 3;
            fastLists(seed, start);
            gapLists(seed, start);
        }
        refusals();
        System.out.println("ArraySizingStrategyAdoptionTest: " + checks + " checks passed");
    }

    /**
     * A plain FastList, one with fastList() (must equal the plain one in contents and capacity),
     * one with arrayList() and one with a lambda (capacity by their arithmetic at each growth).
     */
    static void fastLists(long seed, int start) {
        Random random = new Random(seed);
        FastList<Integer> plain = new FastList<>(start);
        FastList<Integer> same = new FastList<>(start, ArraySizingStrategy.fastList());
        ArraySizingStrategy exact = (current, elements, additions) -> elements + additions;
        List<ArraySizingStrategy> strategies = List.of(ArraySizingStrategy.arrayList(), exact);
        List<FastList<Integer>> others = new ArrayList<>();
        for (ArraySizingStrategy s : strategies) {
            others.add(new FastList<>(start, s));
        }
        for (int step = 0; step < 3_000; step++) {
            String at = "FastList seed " + seed + " step " + step;
            int what = random.nextInt(10);
            int value = random.nextInt(1_000);
            int k = random.nextInt(12);
            int index = plain.isEmpty() ? 0 : random.nextInt(plain.size() + 1);
            List<Integer> batch = new ArrayList<>();
            for (int i = 0; i < k; i++) {
                batch.add(value + i);
            }
            int min = plain.size() + random.nextInt(40);
            boolean clear = what == 9 && random.nextInt(50) == 0;
            List<FastList<Integer>> all = new ArrayList<>(List.of(plain, same));
            all.addAll(others);
            int[] before = new int[others.size()];
            int[] sizes = new int[others.size()];
            for (int i = 0; i < others.size(); i++) {
                before[i] = capacity(others.get(i));
                sizes[i] = others.get(i).size();
            }
            int needed = switch (what) {
                case 0, 1, 2 -> 1;
                case 3 -> 1;
                case 4 -> k;
                case 5 -> k;
                case 6 -> k;
                case 7 -> min - plain.size();
                default -> 0;
            };
            for (FastList<Integer> list : all) {
                switch (what) {
                    case 0, 1, 2 -> list.add(value);
                    case 3 -> list.add(index, value);
                    case 4 -> list.addAll(batch);
                    case 5 -> list.addAll(index, FastList.newList(batch));
                    case 6 -> list.addAll(index, new ArrayList<>(batch));
                    case 7 -> list.ensureCapacity(min);
                    case 8 -> {
                        if (!list.isEmpty()) {
                            list.remove(list.size() / 2);
                        }
                    }
                    default -> {
                        if (clear) {
                            list.clear();
                        }
                    }
                }
            }
            same(plain, same, at + " contents with fastList()");
            same(capacity(plain), capacity(same), at + " capacity with fastList()");
            for (int i = 0; i < others.size(); i++) {
                FastList<Integer> list = others.get(i);
                same(plain, list, at + " contents with " + strategies.get(i));
                int now = capacity(list);
                if (now != before[i] && what != 9) {
                    same(strategies.get(i).grow(before[i], sizes[i], needed), now,
                            at + " growth with " + strategies.get(i));
                }
            }
        }
    }

    /** As fastLists, for GapList and gapList(). */
    static void gapLists(long seed, int start) {
        Random random = new Random(seed * 31);
        GapList<Integer> plain = new GapList<>(start);
        GapList<Integer> same = new GapList<>(start, ArraySizingStrategy.gapList());
        GapList<Integer> other = new GapList<>(start, ArraySizingStrategy.arrayList());
        for (int step = 0; step < 3_000; step++) {
            String at = "GapList seed " + seed + " step " + step;
            int what = random.nextInt(8);
            int value = random.nextInt(1_000);
            int k = random.nextInt(12);
            int index = plain.isEmpty() ? 0 : random.nextInt(plain.size() + 1);
            List<Integer> batch = new ArrayList<>();
            for (int i = 0; i < k; i++) {
                batch.add(value + i);
            }
            int min = plain.size() + random.nextInt(40);
            int before = capacity(other);
            int size = other.size();
            for (GapList<Integer> list : List.of(plain, same, other)) {
                switch (what) {
                    case 0, 1 -> list.add(value);
                    case 2 -> list.add(0, value);
                    case 3 -> list.add(index, value);
                    case 4 -> list.addAll(batch);
                    case 5 -> list.ensureCapacity(min);
                    case 6 -> {
                        if (!list.isEmpty()) {
                            list.remove(0);
                        }
                    }
                    default -> {
                        if (!list.isEmpty()) {
                            list.remove(list.size() - 1);
                        }
                    }
                }
            }
            same(plain, same, at + " contents with gapList()");
            same(capacity(plain), capacity(same), at + " capacity with gapList()");
            same(plain, other, at + " contents with arrayList()");
            int now = capacity(other);
            int needed = what == 4 ? k : what == 5 ? min - size : 1;
            if (now != before) {
                same(ArraySizingStrategy.arrayList().grow(before, size, needed), now,
                        at + " growth with arrayList()");
            }
        }
    }

    /** A strategy returning too little is refused; a list with a strategy is not serializable. */
    static void refusals() throws Exception {
        ArraySizingStrategy tooLittle = (current, elements, additions) -> current;
        FastList<Integer> fast = new FastList<>(0, tooLittle);
        same("IllegalStateException", thrown(() -> fast.add(1)), "FastList with a short strategy");
        same(0, fast.size(), "FastList unchanged after the refusal");
        GapList<Integer> gap = new GapList<>(0, tooLittle);
        same("IllegalStateException", thrown(() -> gap.add(1)), "GapList with a short strategy");
        same(0, gap.size(), "GapList unchanged after the refusal");
        same("NullPointerException", thrown(() -> new FastList<Integer>(0, null)),
                "FastList null");
        same("NullPointerException", thrown(() -> new GapList<Integer>(0, null)), "GapList null");
        same("IllegalArgumentException",
                thrown(() -> new FastList<Integer>(-1, ArraySizingStrategy.fastList())),
                "FastList negative capacity");
        same("written", serialize(new FastList<>(List.of(1, 2))), "plain FastList serializes");
        same("written", serialize(new GapList<>(List.of(1, 2))), "plain GapList serializes");
        FastList<Integer> fastWith = new FastList<>(4, ArraySizingStrategy.fastList());
        fastWith.add(1);
        same("NotSerializableException", serialize(fastWith), "FastList with a strategy");
        GapList<Integer> gapWith = new GapList<>(4, ArraySizingStrategy.gapList());
        gapWith.add(1);
        same("NotSerializableException", serialize(gapWith), "GapList with a strategy");
    }

    static String thrown(Runnable action) {
        try {
            action.run();
            return "nothing";
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName();
        }
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
