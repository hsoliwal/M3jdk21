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
 * @summary The ArraySizingStrategy factories equal their libraries' growth arithmetic for every
 *          current length incl. 0 (ArraysSupport.newLength, Eclipse FastList, Guava
 *          ImmutableCollection.Builder, Brownies CapacityHelper, HPPC's formula); ArrayList,
 *          FastList, ImmutableList.Builder, GapList, ObjectArrayList and ObjectArrayDeque grow
 *          by them; overflow is refused; a lambda is a strategy; the enum constants are
 *          serializable singletons with their own accounting
 * @modules java.base/java.util:+open
 *          java.base/jdk.internal.util
 *          jdk.m3.collections/jdk.m3.collections.hppc:+open
 *          jdk.m3.collections/jdk.m3.collections.brownies:+open
 *          jdk.m3.collections/jdk.m3.collections.guava.collect:+open
 *          jdk.m3.collections/jdk.m3.collections.eclipse.impl.list.mutable:+open
 * @run main ArraySizingStrategySupersetTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.IntBinaryOperator;
import java.util.function.IntSupplier;
import jdk.internal.util.ArraysSupport;
import jdk.m3.collections.brownies.GapList;
import jdk.m3.collections.brownies.helper.CapacityHelper;
import jdk.m3.collections.eclipse.impl.list.mutable.FastList;
import jdk.m3.collections.guava.collect.ImmutableList;
import jdk.m3.collections.hppc.ArraySizingStrategy;
import jdk.m3.collections.hppc.BoundedProportionalArraySizingStrategy;
import jdk.m3.collections.hppc.BufferAllocationException;
import jdk.m3.collections.hppc.ObjectArrayDeque;
import jdk.m3.collections.hppc.ObjectArrayList;

public class ArraySizingStrategySupersetTest {

    static long checks;

    static void same(Object expected, Object actual, String what) {
        checks++;
        if (!Objects.equals(String.valueOf(expected), String.valueOf(actual))) {
            throw new AssertionError(what + ": expected " + expected + " actual " + actual);
        }
    }

    /** A value, or "refused" when an array of that length cannot be made. */
    static String outcome(IntSupplier s) {
        try {
            return "value " + s.getAsInt();
        } catch (BufferAllocationException | OutOfMemoryError e) {
            return "refused";
        } catch (RuntimeException e) {
            return "threw " + e.getClass().getSimpleName();
        }
    }

    /** The simple name of what s throws, or "nothing". */
    static String thrown(IntSupplier s) {
        try {
            s.getAsInt();
            return "nothing";
        } catch (RuntimeException | Error e) {
            return e.getClass().getSimpleName();
        }
    }

    static final Method EXPANDED_CAPACITY;
    static final Method SIZE_PLUS_FIFTY_PERCENT;
    static final Field FAST_LIST_ITEMS;
    static final Field ARRAY_LIST_ELEMENTS;
    static final Field GAP_LIST_VALUES;
    static final Field BUILDER_CONTENTS;
    static final int NUM_BYTES_OBJECT_REF;

    static {
        try {
            Class<?> builder = Class.forName(
                    "jdk.m3.collections.guava.collect.ImmutableCollection$Builder");
            EXPANDED_CAPACITY =
                    builder.getDeclaredMethod("expandedCapacity", int.class, int.class);
            EXPANDED_CAPACITY.setAccessible(true);
            SIZE_PLUS_FIFTY_PERCENT =
                    FastList.class.getDeclaredMethod("sizePlusFiftyPercent", int.class);
            SIZE_PLUS_FIFTY_PERCENT.setAccessible(true);
            FAST_LIST_ITEMS = FastList.class.getDeclaredField("items");
            FAST_LIST_ITEMS.setAccessible(true);
            ARRAY_LIST_ELEMENTS = ArrayList.class.getDeclaredField("elementData");
            ARRAY_LIST_ELEMENTS.setAccessible(true);
            GAP_LIST_VALUES = GapList.class.getDeclaredField("values");
            GAP_LIST_VALUES.setAccessible(true);
            BUILDER_CONTENTS = ImmutableList.Builder.class.getDeclaredField("contents");
            BUILDER_CONTENTS.setAccessible(true);
            Field ref = Class.forName("jdk.m3.collections.hppc.RamUsageEstimator")
                    .getDeclaredField("NUM_BYTES_OBJECT_REF");
            ref.setAccessible(true);
            NUM_BYTES_OBJECT_REF = ref.getInt(null);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static int invoke(Method m, Object target, Object... args) {
        try {
            return (Integer) m.invoke(target, args);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException r) {
                throw r;
            }
            throw new AssertionError(e);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    static int length(Field array, Object owner) {
        try {
            return ((Object[]) array.get(owner)).length;
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /** Receiver of FastList.sizePlusFiftyPercent, which reads no state of it. */
    static final FastList<Object> FAST_LIST = new FastList<>(0);

    static final String[] NAMES =
            {"hppc", "arrayList", "fastList", "immutableCollectionBuilder", "gapList"};

    static ArraySizingStrategy[] factories() {
        return new ArraySizingStrategy[] {ArraySizingStrategy.hppc(),
                ArraySizingStrategy.arrayList(), ArraySizingStrategy.fastList(),
                ArraySizingStrategy.immutableCollectionBuilder(), ArraySizingStrategy.gapList()};
    }

    static final ArraySizingStrategy[] FACTORIES = factories();

    /** Each library's own arithmetic for (current length, needed length), in factory order. */
    static final IntBinaryOperator[] ORACLES = {
            ArraySizingStrategySupersetTest::hppcFormula,
            (current, needed) -> ArraysSupport.newLength(current, needed - current, current >> 1),
            (current, needed) ->
                    Math.max(invoke(SIZE_PLUS_FIFTY_PERCENT, FAST_LIST, current), needed),
            (current, needed) -> invoke(EXPANDED_CAPACITY, null, current, needed),
            (current, needed) ->
                    CapacityHelper.calculateCapacity(needed, current, GapList.DEFAULT_CAPACITY)};

    /**
     * BoundedProportionalArraySizingStrategy.grow with its documented defaults written out:
     * growth by half the length in float, at least 10, at most Integer.MAX_VALUE - 32, which is
     * also the largest length; above it the request is refused.
     */
    static int hppcFormula(int current, int needed) {
        int maxLength = Integer.MAX_VALUE - 32;
        long growBy = Math.min(Math.max((long) (current * 0.5f), 10), maxLength);
        long length = Math.max(needed, Math.min(maxLength, current + growBy));
        if (length > maxLength) {
            throw new BufferAllocationException("Java array size exceeded");
        }
        return (int) length;
    }

    public static void main(String[] args) throws Exception {
        Random random = new Random(17);
        long[] pairs = new long[4];
        for (int i = 0; i < RANDOM + EDGES.length * EDGES.length; i++) {
            boolean edge = i >= RANDOM;
            int current = edge ? EDGES[(i - RANDOM) / EDGES.length] : bound(random);
            int needed = edge ? EDGES[(i - RANDOM) % EDGES.length] : needed(random, current);
            pairs[(edge ? 2 : 0) + (needed > current ? 0 : 1)]++;
            pair(random, current, needed);
        }
        overflow();
        realLists();
        defaultConstructors();
        deques();
        constants();
        lambda();
        System.out.println("ArraySizingStrategySupersetTest: " + checks + " checks passed; "
                + "(current, needed) pairs with needed > current / not: random " + pairs[0]
                + " / " + pairs[1] + ", edge " + pairs[2] + " / " + pairs[3]);
    }

    static final int RANDOM = 300_000;

    /** Each factory against its library at one (current, needed) pair, split at random. */
    static void pair(Random random, int current, int needed) {
        for (int f = 0; f < FACTORIES.length; f++) {
            int elements = (int) Math.min(needed, (long) (random.nextDouble() * (needed + 1L)));
            int additions = needed - elements;
            ArraySizingStrategy s = FACTORIES[f];
            IntBinaryOperator oracle = ORACLES[f];
            same(outcome(() -> oracle.applyAsInt(current, needed)),
                    outcome(() -> s.grow(current, elements, additions)),
                    NAMES[f] + " " + current + "/" + elements + "/" + additions);
        }
    }

    /**
     * Small lengths, the lengths where current + current / 2 crosses Integer.MAX_VALUE - 8, and
     * the largest ones.
     */
    static final int[] EDGES = edges();

    static int[] edges() {
        List<Integer> edges = new ArrayList<>(List.of(0, 1, 2, 3, 4, 9, 10, 11));
        for (int c = 1_431_655_755; c <= 1_431_655_772; c++) {
            edges.add(c);
        }
        for (int c = Integer.MAX_VALUE - 12; c < Integer.MAX_VALUE; c++) {
            edges.add(c);
        }
        edges.add(Integer.MAX_VALUE);
        return edges.stream().mapToInt(Integer::intValue).toArray();
    }

    /** A needed length above the current one: anywhere up to the largest, or a little above it. */
    static int needed(Random random, int current) {
        if (current == Integer.MAX_VALUE) {
            return current;
        }
        if (random.nextInt(4) == 0) {
            return (int) Math.min(Integer.MAX_VALUE, (long) current + 1 + random.nextInt(64));
        }
        return current + 1
                + (int) (random.nextDouble() * ((long) Integer.MAX_VALUE - current - 1));
    }

    /** A current length: small, near a power of two, or near the largest array lengths. */
    static int bound(Random random) {
        return switch (random.nextInt(4)) {
            case 0 -> random.nextInt(100);
            case 1 -> (1 << random.nextInt(31)) - 1 + random.nextInt(3);
            case 2 -> Integer.MAX_VALUE - random.nextInt(40);
            default -> random.nextInt(Integer.MAX_VALUE);
        };
    }

    /** Every factory throws BufferAllocationException when elements + additions overflow. */
    static void overflow() {
        for (int f = 0; f < FACTORIES.length; f++) {
            ArraySizingStrategy s = FACTORIES[f];
            String name = "overflow " + NAMES[f];
            same("BufferAllocationException", thrown(() -> s.grow(100, Integer.MAX_VALUE, 1)),
                    name);
            same("BufferAllocationException", thrown(() -> s.grow(0, 1, Integer.MAX_VALUE)),
                    name + " from 0");
            same("BufferAllocationException",
                    thrown(() -> s.grow(Integer.MAX_VALUE, Integer.MAX_VALUE, 1)),
                    name + " at the largest length");
            same("BufferAllocationException", thrown(() -> s.grow(Integer.MAX_VALUE - 8,
                    Integer.MAX_VALUE - 8, Integer.MAX_VALUE)), name + " far");
        }
    }

    static final int ADDS = 5_000;

    /** The next length by s when a full buffer needs one more slot: s.grow(length, size, 1). */
    static IntBinaryOperator byStrategy(ArraySizingStrategy s) {
        return (length, needed) -> s.grow(length, needed - 1, 1);
    }

    /**
     * Adds elements one by one from size {@code from} up to ADDS. Before an add that needs more
     * than the expected length (with {@code slack} free slots kept), the model grows it; after
     * every add the container's length must equal it.
     */
    static void follow(String what, int from, int slack, int length, IntBinaryOperator model,
            Runnable add, IntSupplier actual) {
        int expected = length;
        for (int size = from; size < ADDS; size++) {
            int needed = size + 1 + slack;
            if (needed > expected) {
                expected = model.applyAsInt(expected, needed);
            }
            add.run();
            same(expected, actual.getAsInt(), what + " at size " + (size + 1));
        }
    }

    /** Containers created with an explicit capacity grow by their library's factory. */
    static void realLists() {
        for (int c : new int[] {0, 1, 2, 7, 10, 33}) {
            ArrayList<Object> arrayList = new ArrayList<>(c);
            follow("new ArrayList<>(" + c + ")", 0, 0, c,
                    byStrategy(ArraySizingStrategy.arrayList()), () -> arrayList.add(""),
                    () -> length(ARRAY_LIST_ELEMENTS, arrayList));
            FastList<Object> fastList = new FastList<>(c);
            follow("new FastList<>(" + c + ")", 0, 0, c,
                    byStrategy(ArraySizingStrategy.fastList()), () -> fastList.add(""),
                    () -> length(FAST_LIST_ITEMS, fastList));
            ImmutableList.Builder<Object> builder = ImmutableList.builderWithExpectedSize(c);
            follow("ImmutableList.builderWithExpectedSize(" + c + ")", 0, 0, c,
                    byStrategy(ArraySizingStrategy.immutableCollectionBuilder()),
                    () -> builder.add(""), () -> length(BUILDER_CONTENTS, builder));
            GapList<Object> gapList = new GapList<>(c);
            follow("new GapList<>(" + c + ")", 0, 0, c,
                    byStrategy(ArraySizingStrategy.gapList()), () -> gapList.add(""),
                    () -> length(GAP_LIST_VALUES, gapList));
            Object[] libraries = {null, arrayList, fastList, builder, gapList};
            Field[] arrays =
                    {null, ARRAY_LIST_ELEMENTS, FAST_LIST_ITEMS, BUILDER_CONTENTS, GAP_LIST_VALUES};
            for (int f = 0; f < FACTORIES.length; f++) {
                ObjectArrayList<Object> list = new ObjectArrayList<>(c, FACTORIES[f]);
                String what = "ObjectArrayList(" + c + ", " + NAMES[f] + "())";
                follow(what, 0, 0, c, ORACLES[f], () -> list.add(""), () -> list.buffer.length);
                if (libraries[f] != null) {
                    same(length(arrays[f], libraries[f]), list.buffer.length,
                            what + " ends as long as its library's container");
                }
            }
        }
    }

    /** The documented exceptions: default constructors, and empty buffers. */
    static void defaultConstructors() {
        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add("");
        same(10, length(ARRAY_LIST_ELEMENTS, arrayList), "new ArrayList<>() first add");
        follow("new ArrayList<>()", 1, 0, 10, byStrategy(ArraySizingStrategy.arrayList()),
                () -> arrayList.add(""), () -> length(ARRAY_LIST_ELEMENTS, arrayList));
        FastList<Object> fastList = new FastList<>();
        fastList.add("");
        same(10, length(FAST_LIST_ITEMS, fastList), "new FastList<>() first add");
        follow("new FastList<>()", 1, 0, 10, byStrategy(ArraySizingStrategy.fastList()),
                () -> fastList.add(""), () -> length(FAST_LIST_ITEMS, fastList));
        ImmutableList.Builder<Object> builder = ImmutableList.builder();
        same(4, length(BUILDER_CONTENTS, builder), "ImmutableList.builder() allocates 4");
        follow("ImmutableList.builder()", 0, 0, 4,
                byStrategy(ArraySizingStrategy.immutableCollectionBuilder()),
                () -> builder.add(""), () -> length(BUILDER_CONTENTS, builder));
        for (GapList<Object> gapList : List.of(new GapList<>(), new GapList<>(0))) {
            same(0, length(GAP_LIST_VALUES, gapList), "GapList starts empty");
            gapList.add("");
            same(10, length(GAP_LIST_VALUES, gapList), "GapList first add");
            follow("GapList from 10", 1, 0, 10, byStrategy(ArraySizingStrategy.gapList()),
                    () -> gapList.add(""), () -> length(GAP_LIST_VALUES, gapList));
        }
        ArrayList<Object> zero = new ArrayList<>(0);
        zero.add("");
        same(1, length(ARRAY_LIST_ELEMENTS, zero), "new ArrayList<>(0) first add");
        FastList<Object> fastZero = new FastList<>(0);
        fastZero.add("");
        same(1, length(FAST_LIST_ITEMS, fastZero), "new FastList<>(0) first add");
        ImmutableList.Builder<Object> builderZero = ImmutableList.builderWithExpectedSize(0);
        builderZero.add("");
        same(1, length(BUILDER_CONTENTS, builderZero), "builderWithExpectedSize(0) first add");
        for (int f = 0; f < FACTORIES.length; f++) {
            same(new int[] {10, 1, 1, 1, 10}[f], FACTORIES[f].grow(0, 0, 1),
                    NAMES[f] + " from an empty buffer");
        }
    }

    /** ObjectArrayDeque keeps one slot free: it asks each factory for elementsCount + 1 more. */
    static void deques() {
        for (int f = 0; f < FACTORIES.length; f++) {
            ArraySizingStrategy s = FACTORIES[f];
            for (int c : new int[] {0, 1, 7, 33}) {
                ObjectArrayDeque<Object> deque = new ObjectArrayDeque<>(c, s);
                String what = "ObjectArrayDeque(" + c + ", " + NAMES[f] + "())";
                int start = ORACLES[f].applyAsInt(0, c + 1);
                same(start, deque.buffer.length, what + " starts");
                follow(what, 0, 1, start, ORACLES[f], () -> deque.addLast(""),
                        () -> deque.buffer.length);
                same(ADDS, deque.size(), what + " size");
            }
            String name = "ObjectArrayDeque " + NAMES[f];
            same("BufferAllocationException",
                    thrown(() -> new ObjectArrayDeque<>(Integer.MAX_VALUE, s).size()),
                    name + " created for Integer.MAX_VALUE elements");
            ObjectArrayDeque<Object> five = new ObjectArrayDeque<>(5, s);
            for (int i = 0; i < 5; i++) {
                five.addLast("");
            }
            int length = five.buffer.length;
            same("BufferAllocationException", thrown(() -> {
                five.ensureCapacity(Integer.MAX_VALUE);
                return 0;
            }), name + " with 5 elements ensuring Integer.MAX_VALUE");
            same(length, five.buffer.length, name + " keeps its buffer");
            same(5, five.size(), name + " keeps its elements");
        }
    }

    /** hppc() is HPPC's instance; the others are distinct enum singletons. */
    static void constants() throws Exception {
        same(true, ArraySizingStrategy.class.isAnnotationPresent(FunctionalInterface.class),
                "@FunctionalInterface");
        same(true, FACTORIES[0] == BoundedProportionalArraySizingStrategy.DEFAULT_INSTANCE,
                "hppc() is the default instance");
        Set<ArraySizingStrategy> distinct = Collections.newSetFromMap(new IdentityHashMap<>());
        ArraySizingStrategy[] again = factories();
        for (int f = 0; f < FACTORIES.length; f++) {
            distinct.add(FACTORIES[f]);
            same(true, again[f] == FACTORIES[f], NAMES[f] + "() returns one instance");
        }
        same(FACTORIES.length, distinct.size(), "distinct strategies");
        for (int f = 1; f < FACTORIES.length; f++) {
            ArraySizingStrategy s = FACTORIES[f];
            same("ArraySizingStrategy." + NAMES[f] + "()", s.toString(), "toString");
            same(true, s instanceof Enum<?>, NAMES[f] + " is an enum constant");
            same(true, roundTrip(s) == s, NAMES[f] + " deserializes to itself");
            same(instanceBytes(s), s.ramBytesAllocated(), NAMES[f] + " ramBytesAllocated");
            same(s.ramBytesAllocated(), s.ramBytesUsed(), NAMES[f] + " ramBytesUsed");
        }
    }

    static Object roundTrip(Object o) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(o);
        }
        try (ObjectInputStream in =
                new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return in.readObject();
        }
    }

    /** The object header and the instance fields of o's class and its superclasses. */
    static long instanceBytes(Object o) {
        long bytes = header();
        for (Class<?> k = o.getClass(); k != null; k = k.getSuperclass()) {
            for (Field field : k.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    bytes += fieldBytes(field.getType());
                }
            }
        }
        return bytes;
    }

    static int fieldBytes(Class<?> type) {
        if (!type.isPrimitive()) {
            return NUM_BYTES_OBJECT_REF;
        }
        if (type == long.class || type == double.class) {
            return Long.BYTES;
        }
        if (type == int.class || type == float.class) {
            return Integer.BYTES;
        }
        return type == short.class || type == char.class ? Short.BYTES : Byte.BYTES;
    }

    /** The object header, from BoundedProportionalArraySizingStrategy's own accounting. */
    static long header() {
        return BoundedProportionalArraySizingStrategy.DEFAULT_INSTANCE.ramBytesAllocated()
                - Integer.BYTES * 2 - Float.BYTES;
    }

    /** A lambda is a strategy; the Accountable defaults count its object header. */
    static void lambda() {
        ArraySizingStrategy exact = (current, elements, additions) -> elements + additions;
        same(5, exact.grow(0, 2, 3), "lambda");
        same(header(), exact.ramBytesAllocated(), "lambda ramBytesAllocated");
        same(header(), exact.ramBytesUsed(), "lambda ramBytesUsed");
        ObjectArrayList<Object> list = new ObjectArrayList<>(0, exact);
        follow("ObjectArrayList with a lambda", 0, 0, 0, byStrategy(exact),
                () -> list.add(""), () -> list.buffer.length);
    }
}
