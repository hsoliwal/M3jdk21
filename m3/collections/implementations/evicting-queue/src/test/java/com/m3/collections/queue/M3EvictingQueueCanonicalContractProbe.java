// SPDX-License-Identifier: Apache-2.0
package com.m3.collections.queue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.SplittableRandom;

/**
 * Differential proof for the historical binary/serialized owner and the
 * first-party capability owner. No Guava runtime is present or required.
 */
public final class M3EvictingQueueCanonicalContractProbe {
    private static final String LEGACY = "com.m3.collections.guava.M3EvictingQueue";
    private static final String CANONICAL = "com.m3.collections.queue.M3EvictingQueue";

    private M3EvictingQueueCanonicalContractProbe() { }

    private record Evidence(String transcript, List<String> methods, byte[] wire) { }

    public static void main(String[] ignored) throws Exception {
        Class<?> legacy = Class.forName(LEGACY);
        Class<?> canonical = Class.forName(CANONICAL);
        require(legacy != canonical, "old and new Java binary identities must remain distinct");
        require(Modifier.isFinal(legacy.getModifiers()), "legacy final ABI changed");
        require(Modifier.isFinal(canonical.getModifiers()), "canonical owner must remain final");
        require(ObjectStreamClass.lookup(legacy).getSerialVersionUID() == 0L,
                "legacy serialization ID changed");
        require(ObjectStreamClass.lookup(canonical).getSerialVersionUID() == 0L,
                "canonical serialization ID changed");

        Evidence before = examine(legacy);
        Evidence after = examine(canonical);
        require(before.transcript().equals(after.transcript()), "queue behavior changed");
        require(before.methods().equals(after.methods()), "normalized public methods changed");
        require(LEGACY.length() == CANONICAL.length(), "descriptor normalization needs equal width");
        String wire = new String(before.wire(), StandardCharsets.ISO_8859_1);
        require(wire.indexOf(LEGACY) >= 0 && wire.indexOf(LEGACY) == wire.lastIndexOf(LEGACY),
                "legacy descriptor must be unique");
        byte[] normalized = wire.replace(LEGACY, CANONICAL)
                .getBytes(StandardCharsets.ISO_8859_1);
        require(Arrays.equals(normalized, after.wire()),
                "serialized bytes differ beyond the deliberately new class descriptor");
        require(!new String(after.wire(), StandardCharsets.ISO_8859_1).contains(LEGACY),
                "canonical stream unexpectedly contains legacy Java class name");

        long steps = runSeededQueueModel(legacy) + runSeededQueueModel(canonical);
        verifyBoundedInitialStorage();
        verifySelfReferenceRendering();
        verifyPhysicalGrowthCeiling();
        verifyPhysicalGrowthArithmetic();
        System.out.println("M3_EVICTING_CANONICAL_NAME_PASS normalized_wire_sha256="
                + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(normalized))
                + " randomized_model_steps=" + steps);
    }

    /** JDK AbstractCollection direct self-reference contract, including wire round-trip. */
    private static void verifySelfReferenceRendering() throws Exception {
        M3EvictingQueue<Object> actual = M3EvictingQueue.create(3);
        ArrayDeque<Object> expected = new ArrayDeque<>();
        actual.add("start");
        expected.addLast("start");
        actual.add(actual);
        expected.addLast(expected);
        actual.add("end");
        expected.addLast("end");
        require(actual.toString().equals(expected.toString()),
                "canonical direct self-reference rendering differs from JDK Collection");
        require(actual.toString().equals("[start, (this Collection), end]"),
                "canonical direct self-reference must not recurse");
        Queue<?> restored = (Queue<?>) deserialize(serialize(actual));
        require(restored.getClass() == M3EvictingQueue.class,
                "serialized self-referential owner identity changed");
        require(restored.toArray()[1] == restored,
                "self-referential member was not restored as the queue itself");
        require(restored.toString().equals(expected.toString()),
                "self-reference render after readObject differs from JDK Collection");
        actual.add("tail");
        expected.removeFirst();
        expected.addLast("tail");
        require(actual.toString().equals(expected.toString()),
                "direct self-reference rendering after eviction differs from JDK Collection");
        System.out.println("M3_EVICTING_SELF_REFERENCE_RENDER_PASS");
    }

    /** Reserve no more than logical capacity plus one physical sentinel slot. */
    private static void verifyPhysicalGrowthCeiling() throws Exception {
        var storage = M3EvictingQueue.class.getDeclaredField("elements");
        storage.setAccessible(true);
        for (int limit : new int[] {16, 17, 18, 31, 32, 33, 63, 64, 65, 127, 128, 129}) {
            M3EvictingQueue<Integer> actual = M3EvictingQueue.create(limit);
            ArrayDeque<Integer> expected = new ArrayDeque<>();
            for (int index = 0; index < limit * 4; index++) {
                actual.add(index);
                if (expected.size() == limit) expected.removeFirst();
                expected.addLast(index);
                Object[] ring = (Object[]) storage.get(actual);
                require(ring.length <= limit + 1,
                        "ring exceeded logical max plus sentinel: limit=" + limit);
                require(Arrays.equals(actual.toArray(), expected.toArray()),
                        "bounded growth FIFO differed: limit=" + limit + " step=" + index);
                if (index % 7 == 3) {
                    require(actual.poll().equals(expected.poll()),
                            "bounded growth poll differed: limit=" + limit + " step=" + index);
                }
            }
            while (!expected.isEmpty()) {
                require(actual.remove().equals(expected.removeFirst()),
                        "bounded growth drain differed: limit=" + limit);
            }
            require(actual.isEmpty(), "bounded growth queue was not fully drained: limit=" + limit);
        }
        System.out.println("M3_EVICTING_PHYSICAL_GROWTH_CEILING_PASS");
    }


    /** Check the real private growth calculation, without allocating capacity-sized arrays. */
    private static void verifyPhysicalGrowthArithmetic() throws Exception {
        Method growth = M3EvictingQueue.class.getDeclaredMethod("expandedLength", int.class);
        growth.setAccessible(true);
        var storage = M3EvictingQueue.class.getDeclaredField("elements");
        storage.setAccessible(true);
        SplittableRandom random = new SplittableRandom(0x72696e675f333031L);
        long expansions = 0;
        for (int capacity = 0; capacity < 4096; capacity++) {
            expansions += verifyGrowthSequence(growth, storage, capacity);
        }
        for (int sample = 0; sample < 16_384; sample++) {
            expansions += verifyGrowthSequence(growth, storage, random.nextInt(Integer.MAX_VALUE));
        }
        int[] extremes = {Integer.MAX_VALUE, Integer.MAX_VALUE - 1,
                Integer.MAX_VALUE - 8, Integer.MAX_VALUE - 9,
                Integer.MAX_VALUE - 10, 1_000_000_000,
                127, 128, 129, 15, 16, 17, 31, 63, 64, 65};
        for (int capacity : extremes) {
            expansions += verifyGrowthSequence(growth, storage, capacity);
        }
        require(expansions > 700_000L, "growth oracle did not exercise enough expansions");
        System.out.println("M3_EVICTING_GROWTH_ARITHMETIC_PASS configurations=20496 expansions="
                + expansions);
    }

    private static long verifyGrowthSequence(Method growth,
            java.lang.reflect.Field storage, int limit) throws Exception {
        M3EvictingQueue<Integer> owner = M3EvictingQueue.create(limit);
        int physical = limit >= 15 ? 16 : limit + 1;
        require(((Object[]) storage.get(owner)).length == physical,
                "initial physical slots differed from constructor for limit=" + limit);
        // A Java Object[] length is int: the mathematical MAX_VALUE+1 is not allocatable.
        long feasible = Math.min((long) limit + 1, (long) Integer.MAX_VALUE);
        int steps = 0;
        while (physical < feasible) {
            int next = (Integer) growth.invoke(owner, physical);
            require(next > physical && next <= feasible,
                    "invalid M3 ring expansion: limit=" + limit
                            + " old=" + physical + " next=" + next);
            physical = next;
            require(++steps <= 100, "nonconvergent M3 ring growth for limit=" + limit);
        }
        return steps;
    }

    /** Exercise bounded empty-capacity storage, physical ring growth, wrap and wire replay. */
    private static void verifyBoundedInitialStorage() throws Exception {
        final int limit = 1_000_000_000;
        M3EvictingQueue<Integer> actual = M3EvictingQueue.create(limit);
        var storage = M3EvictingQueue.class.getDeclaredField("elements");
        storage.setAccessible(true);
        require(((Object[]) storage.get(actual)).length == 16,
                "large logical limit allocated a large backing array");
        ArrayDeque<Integer> expected = new ArrayDeque<>();
        for (int index = 0; index < 257; index++) {
            actual.add(index);
            expected.addLast(index);
        }
        for (int index = 0; index < 113; index++) {
            require(actual.remove().equals(expected.removeFirst()), "poll after growth differed");
        }
        for (int index = 257; index < 521; index++) {
            actual.add(index);
            expected.addLast(index);
        }
        require(Arrays.equals(actual.toArray(), expected.toArray()),
                "ring ordering after bounded growth and wrap differed");
        require(actual.remainingCapacity() == limit - expected.size(),
                "large logical limit was changed by physical growth");
        @SuppressWarnings("unchecked")
        Queue<Integer> recovered = (Queue<Integer>) deserialize(serialize(actual));
        require(recovered.getClass() == M3EvictingQueue.class,
                "bounded queue serialization changed binary identity");
        require(Arrays.equals(recovered.toArray(), expected.toArray()),
                "bounded queue serialized payload differed");
        // The wire image uses count+1 slots, not the new 16-slot constructor.
        // Force repeated growth after readObject() and verify FIFO preservation.
        for (int index = 521; index < 2048; index++) {
            recovered.add(index);
            expected.addLast(index);
        }
        require(Arrays.equals(recovered.toArray(), expected.toArray()),
                "serialized ring growth changed FIFO order");
        while (!expected.isEmpty()) {
            require(recovered.remove().equals(expected.removeFirst()),
                    "serialized ring growth/drain differed");
        }
        require(recovered.isEmpty(), "serialized ring did not drain to empty");
        System.out.println("M3_EVICTING_SERIALIZED_GROWTH_PASS");
        System.out.println("M3_EVICTING_LAZY_INITIAL_STORAGE_PASS");
    }

    /**
     * Independent JDK deque oracle: no implementation reflection, no reuse of M3
     * ring offsets, masks, or removal algorithms in the expected-state model.
     * Fixed seeds make a failure repeatable on Java 21 and across platforms.
     */
    private static long runSeededQueueModel(Class<?> owner) throws Exception {
        int[] capacities = {0, 1, 2, 3, 7, 15, 16, 17, 31, 63, 64, 65};
        long[] seeds = {0x63716c2d6d3321L, 0x6c656761637921L, 0x7fffffffffffffffL};
        long steps = 0;
        for (int capacity : capacities) {
            for (long seed : seeds) {
                SplittableRandom random = new SplittableRandom(seed);
                Queue<String> actual = create(owner, capacity);
                ArrayDeque<String> expected = new ArrayDeque<>();
                for (int step = 0; step < 15_000; step++) {
                    int operation = random.nextInt(16);
                    String value = "v" + random.nextInt(19);
                    switch (operation) {
                        case 0 -> {
                            require(actual.add(value), "add returned false");
                            appendBounded(expected, value, capacity);
                        }
                        case 1 -> {
                            require(actual.offer(value), "offer returned false");
                            appendBounded(expected, value, capacity);
                        }
                        case 2 -> require(java.util.Objects.equals(actual.poll(), expected.poll()),
                                "poll differed");
                        case 3 -> require(java.util.Objects.equals(actual.peek(), expected.peek()),
                                "peek differed");
                        case 4 -> require(actual.contains(value) == expected.contains(value),
                                "contains differed");
                        case 5 -> require(actual.remove(value) == expected.remove(value),
                                "remove(Object) differed");
                        case 6 -> {
                            actual.clear();
                            expected.clear();
                        }
                        case 7 -> {
                            ArrayList<String> batch = new ArrayList<>();
                            for (int index = random.nextInt(0, capacity + 5); index > 0; index--) {
                                batch.add("b" + random.nextInt(11));
                            }
                            boolean changed = actual.addAll(batch);
                            for (String item : batch) appendBounded(expected, item, capacity);
                            require(changed == (!batch.isEmpty() && capacity > 0),
                                    "addAll changed contract differed");
                        }
                        case 8 -> {
                            boolean a = actual.removeIf(item -> item.endsWith("3") || item.endsWith("7"));
                            boolean b = expected.removeIf(item -> item.endsWith("3") || item.endsWith("7"));
                            require(a == b, "removeIf differed");
                        }
                        case 9 -> {
                            List<String> keep = List.of(value, "b2", "v0");
                            require(actual.retainAll(keep) == expected.retainAll(keep),
                                    "retainAll differed");
                        }
                        case 10 -> {
                            List<String> drop = List.of(value, "b2", "v0");
                            require(actual.removeAll(drop) == expected.removeAll(drop),
                                    "removeAll differed");
                        }
                        case 11 -> {
                            if (!expected.isEmpty()) {
                                int index = random.nextInt(expected.size());
                                Iterator<String> a = actual.iterator();
                                Iterator<String> b = expected.iterator();
                                for (int position = 0; position <= index; position++) {
                                    require(a.next().equals(b.next()), "iterator traversal differed");
                                }
                                a.remove();
                                b.remove();
                            }
                        }
                        case 12 -> {
                            Iterator<String> a = actual.iterator();
                            Iterator<String> b = expected.iterator();
                            int index = 0;
                            while (b.hasNext()) {
                                require(a.hasNext(), "M3 iterator ended early");
                                require(a.next().equals(b.next()), "iterator element differed");
                                if ((index++ & 1) == 0) {
                                    a.remove();
                                    b.remove();
                                }
                            }
                            require(!a.hasNext(), "M3 iterator ended late");
                        }
                        case 13 -> {
                            Iterator<String> a = actual.iterator();
                            Iterator<String> b = expected.iterator();
                            if (b.hasNext() && random.nextBoolean()) {
                                require(a.next().equals(b.next()), "prefix traversal differed");
                            }
                            ArrayList<String> left = new ArrayList<>();
                            ArrayList<String> right = new ArrayList<>();
                            a.forEachRemaining(left::add);
                            b.forEachRemaining(right::add);
                            require(left.equals(right), "iterator.forEachRemaining differed");
                        }
                        case 14 -> {
                            String[] a = new String[actual.size() + 2];
                            String[] b = new String[expected.size() + 2];
                            Arrays.fill(a, "sentinel");
                            Arrays.fill(b, "sentinel");
                            require(Arrays.equals(actual.toArray(a), expected.toArray(b)),
                                    "oversized typed toArray differed");
                        }
                        case 15 -> {
                            if (expected.isEmpty()) {
                                try {
                                    actual.element();
                                    throw new AssertionError("element on empty queue succeeded");
                                } catch (NoSuchElementException correct) {
                                    // JDK Queue's empty-element exception contract.
                                }
                            } else {
                                require(actual.element().equals(expected.element()),
                                        "element differed");
                                if (random.nextBoolean()) {
                                    require(actual.remove().equals(expected.remove()),
                                            "remove() differed");
                                }
                            }
                        }
                        default -> throw new AssertionError("unknown model operation");
                    }
                    assertModeledState(owner, capacity, seed, step, operation, actual, expected);
                    if ((step & 2047) == 2047) {
                        Queue<?> recovered = (Queue<?>) deserialize(serialize(actual));
                        require(recovered.getClass() == owner, "randomized serialization identity differed");
                        require(Arrays.equals(recovered.toArray(), expected.toArray()),
                                "randomized serialization contents differed");
                    }
                    steps++;
                }
            }
        }
        return steps;
    }

    private static void appendBounded(ArrayDeque<String> deque, String value, int capacity) {
        if (capacity == 0) return;
        if (deque.size() == capacity) deque.removeFirst();
        deque.addLast(value);
    }

    private static void assertModeledState(Class<?> owner, int capacity, long seed,
            int step, int operation, Queue<String> actual, ArrayDeque<String> expected)
            throws Exception {
        Object[] a = actual.toArray();
        Object[] b = expected.toArray();
        if (!Arrays.equals(a, b)) {
            throw new AssertionError(owner.getSimpleName() + " cap=" + capacity
                    + " seed=" + seed + " step=" + step + " operation=" + operation
                    + " actual=" + Arrays.toString(a) + " oracle=" + Arrays.toString(b));
        }
        require(actual.size() == expected.size(), "size differed");
        require(actual.isEmpty() == expected.isEmpty(), "isEmpty differed");
        require(actual.toString().equals(expected.toString()), "toString differed");
        int remaining = (Integer) owner.getMethod("remainingCapacity").invoke(actual);
        require(remaining == capacity - expected.size(), "remainingCapacity differed");
    }

    private static Evidence examine(Class<?> owner) throws Exception {
        StringBuilder observed = new StringBuilder();
        Queue<String> queue = create(owner, 4);
        require(queue.addAll(List.of("a", "b", "c", "d", "e", "f")), "batch insertion refused");
        observed.append(queue).append('|').append(Arrays.toString(queue.toArray())).append('|');
        String[] extra = new String[6];
        Arrays.fill(extra, "sentinel");
        observed.append(Arrays.toString(queue.toArray(extra))).append('|');

        Iterator<String> cursor = queue.iterator();
        observed.append(cursor.next()).append('|');
        cursor.remove();
        observed.append(queue).append('|');
        observed.append(queue.removeIf("e"::equals)).append('|');
        observed.append(queue.retainAll(List.of("d", "f"))).append('|');
        observed.append(queue).append('|');
        observed.append(owner.getMethod("remainingCapacity").invoke(queue)).append('|');

        queue.add("g");
        queue.add("h");
        observed.append(queue).append('|');
        observed.append(queue.removeAll(List.of("d", "g"))).append('|');
        observed.append(queue).append('|');
        observed.append(queue.contains("h")).append('/').append(queue.contains(null)).append('|');

        Queue<String> zero = create(owner, 0);
        observed.append(zero.add("discard")).append('/').append(zero.size()).append('|');
        try { zero.add(null); throw new AssertionError("null was accepted"); }
        catch (NullPointerException expected) { observed.append("null-rejected|"); }
        try { zero.remove(); throw new AssertionError("empty remove succeeded"); }
        catch (NoSuchElementException expected) { observed.append("empty-rejected|"); }
        try { create(owner, -1); throw new AssertionError("negative capacity accepted"); }
        catch (java.lang.reflect.InvocationTargetException expected) {
            require(expected.getCause() instanceof IllegalArgumentException,
                    "wrong capacity exception type");
            observed.append("capacity-rejected|");
        }

        byte[] image = serialize(queue);
        Object recovered = deserialize(image);
        require(recovered.getClass() == owner, "serialization class identity changed");
        require(Arrays.equals(((Queue<?>) recovered).toArray(), queue.toArray()),
                "serialized queue ordering changed");
        observed.append(Arrays.toString(((Queue<?>) recovered).toArray())).append('|');

        List<String> methods = Arrays.stream(owner.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(Method::toGenericString)
                .map(signature -> signature.replace(owner.getName(), "<queue-owner>"))
                .sorted().toList();
        require(!methods.isEmpty(), "no public queue surface");
        return new Evidence(observed.toString(), methods, image);
    }

    @SuppressWarnings("unchecked")
    private static Queue<String> create(Class<?> owner, int capacity) throws Exception {
        return (Queue<String>) owner.getMethod("create", int.class).invoke(null, capacity);
    }

    private static byte[] serialize(Object value) throws Exception {
        try (var bytes = new ByteArrayOutputStream();
                var output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
            output.flush();
            return bytes.toByteArray();
        }
    }

    private static Object deserialize(byte[] image) throws Exception {
        try (var input = new ObjectInputStream(new ByteArrayInputStream(image))) {
            return input.readObject();
        }
    }

    private static void require(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }
}
