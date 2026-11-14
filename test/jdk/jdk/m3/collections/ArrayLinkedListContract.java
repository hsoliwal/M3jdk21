/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Random;
import jdk.m3.collections.apache.M3NodeCachingLinkedList;

/** Differential behavior and physical-slot ownership proof for the Apache arrayified owner. */
@SuppressWarnings("deprecation")
public final class ArrayLinkedListContract {
    private static long checks;

    private static void require(boolean condition, String what) {
        checks++;
        if (!condition) {
            throw new AssertionError(what + " after " + checks + " assertions");
        }
    }

    private static Object value(Random random) {
        return switch (random.nextInt(9)) {
            case 0 -> null;
            case 1 -> "same";
            case 2 -> 0;
            case 3 -> -1;
            default -> random.nextInt(101) - 50;
        };
    }

    private static void equal(List<?> expected, List<?> actual) {
        require(Objects.equals(expected, actual), "contents " + expected + " != " + actual);
        require(expected.size() == actual.size(), "size");
        require(expected.hashCode() == actual.hashCode(), "hashCode");
        require(Arrays.equals(expected.toArray(), actual.toArray()), "array");
        require(Arrays.equals(expected.toArray(new Object[0]), actual.toArray(new Object[0])), "typed array");
        require(expected.toString().equals(actual.toString()), "toString");
        if (!expected.isEmpty()) {
            require(Objects.equals(expected.getFirst(), actual.getFirst()), "first");
            require(Objects.equals(expected.getLast(), actual.getLast()), "last");
        }
    }

    private static byte[] serialize(Object value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        return bytes.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static void roundTrip(M3NodeCachingLinkedList<Object> actual,
            LinkedList<Object> expected) throws Exception {
        byte[] bytes = serialize(actual);
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            Object object = in.readObject();
            require(object instanceof M3NodeCachingLinkedList, "serialized type");
            equal(expected, (M3NodeCachingLinkedList<Object>) object);
        }
    }

    private static void iteratorMutation(LinkedList<Object> expected,
            M3NodeCachingLinkedList<Object> actual, Random random) {
        int index = random.nextInt(expected.size() + 1);
        ListIterator<Object> e = expected.listIterator(index);
        ListIterator<Object> a = actual.listIterator(index);
        int n = random.nextInt(8);
        for (int i = 0; i < n; i++) {
            int op = random.nextInt(6);
            require(e.nextIndex() == a.nextIndex(), "nextIndex");
            require(e.previousIndex() == a.previousIndex(), "previousIndex");
            if (op == 0 && e.hasNext()) {
                require(Objects.equals(e.next(), a.next()), "iterator next");
            } else if (op == 1 && e.hasPrevious()) {
                require(Objects.equals(e.previous(), a.previous()), "iterator previous");
            } else if (op == 2) {
                Object v = value(random);
                e.add(v);
                a.add(v);
            } else if (op == 3) {
                Object v = value(random);
                String err1 = status(() -> e.set(v));
                String err2 = status(() -> a.set(v));
                require(err1.equals(err2), "iterator set exception " + err1 + " vs " + err2);
            } else if (op == 4) {
                String err1 = status(e::remove);
                String err2 = status(a::remove);
                require(err1.equals(err2), "iterator remove exception");
            } else {
                require(e.hasNext() == a.hasNext(), "iterator hasNext");
                require(e.hasPrevious() == a.hasPrevious(), "iterator hasPrevious");
            }
        }
        equal(expected, actual);
    }

    private static String status(Runnable op) {
        try {
            op.run();
            return "success";
        } catch (RuntimeException error) {
            return error.getClass().getName();
        }
    }

    private static void storageProof(M3NodeCachingLinkedList<?> actual) throws Exception {
        boolean objectLane = false;
        boolean prevLane = false;
        boolean nextLane = false;
        boolean bitmap = false;
        for (Field field : M3NodeCachingLinkedList.class.getDeclaredFields()) {
            require(!field.getType().getSimpleName().contains("Node"), "node-owned field");
            require(!field.getType().getSimpleName().contains("Entry"), "entry-owned field");
            if (field.getName().equals("elements")) objectLane = field.getType() == Object[].class;
            if (field.getName().equals("previous")) prevLane = field.getType() == int[].class;
            if (field.getName().equals("next")) nextLane = field.getType() == int[].class;
            if (field.getName().equals("markMask")) bitmap = field.getType() == long[].class;
        }
        require(objectLane && prevLane && nextLane && bitmap, "array lanes");
        Field elements = M3NodeCachingLinkedList.class.getDeclaredField("elements");
        elements.setAccessible(true);
        Object[] lane = (Object[]) elements.get(actual);
        int liveCount = 0;
        for (Object item : lane) {
            if (item != null) liveCount++;
        }
        // Nullable elements do not count as live references. All slots are reclaimed on clear.
        actual.clear();
        for (Object item : (Object[]) elements.get(actual)) {
            require(item == null, "retained stale reference after clear");
        }
    }

    public static void main(String[] args) throws Exception {
        Random random = new Random(0x4d3341525241594cL);
        LinkedList<Object> expected = new LinkedList<>();
        M3NodeCachingLinkedList<Object> actual = new M3NodeCachingLinkedList<>();
        require(actual.isEmpty(), "initial empty");
        require(actual.reversed().isEmpty(), "initial reversed");
        for (int step = 0; step < 60_000; step++) {
            int op = random.nextInt(17);
            Object item = value(random);
            switch (op) {
                case 0 -> {
                    expected.add(item);
                    actual.add(item);
                }
                case 1 -> {
                    expected.addFirst(item);
                    actual.addFirst(item);
                }
                case 2 -> {
                    expected.addLast(item);
                    actual.addLast(item);
                }
                case 3 -> {
                    int index = random.nextInt(expected.size() + 1);
                    expected.add(index, item);
                    actual.add(index, item);
                }
                case 4 -> {
                    if (!expected.isEmpty()) {
                        int index = random.nextInt(expected.size());
                        require(Objects.equals(expected.remove(index), actual.remove(index)), "removeAt");
                    }
                }
                case 5 -> {
                    if (!expected.isEmpty()) {
                        int index = random.nextInt(expected.size());
                        require(Objects.equals(expected.set(index, item), actual.set(index, item)), "set");
                    }
                }
                case 6 -> require(expected.remove(item) == actual.remove(item), "remove value");
                case 7 -> {
                    if (!expected.isEmpty()) {
                        require(Objects.equals(expected.removeFirst(), actual.removeFirst()), "removeFirst");
                    }
                }
                case 8 -> {
                    if (!expected.isEmpty()) {
                        require(Objects.equals(expected.removeLast(), actual.removeLast()), "removeLast");
                    }
                }
                case 9 -> {
                    int index = random.nextInt(expected.size() + 1);
                    Collection<Object> incoming = Arrays.asList(value(random), null, value(random));
                    expected.addAll(index, incoming);
                    actual.addAll(index, incoming);
                }
                case 10 -> {
                    if (expected.size() < 70) {
                        expected.addAll(expected);
                        actual.addAll(actual);
                    }
                }
                case 11 -> {
                    if (!expected.isEmpty()) {
                        iteratorMutation(expected, actual, random);
                    }
                }
                case 12 -> {
                    if (expected.size() > 0) {
                        int from = random.nextInt(expected.size());
                        int to = from + random.nextInt(expected.size() - from + 1);
                        require(expected.subList(from, to).equals(actual.subList(from, to)), "subList");
                        if (to > from && random.nextBoolean()) {
                            expected.subList(from, to).clear();
                            actual.subList(from, to).clear();
                        }
                    }
                }
                case 13 -> {
                    if (expected.size() > 0 && random.nextBoolean()) {
                        expected.reversed().addFirst(item);
                        actual.reversed().addFirst(item);
                    }
                }
                case 14 -> {
                    if (expected.size() > 0 && random.nextBoolean()) {
                        require(Objects.equals(expected.reversed().removeFirst(), actual.reversed().removeFirst()),
                                "reverse removal");
                    }
                }
                case 15 -> {
                    if (random.nextInt(5) == 0) {
                        expected.clear();
                        actual.clear();
                    } else {
                        int filter = random.nextInt(10);
                        expected.removeIf(e -> e instanceof Integer n && Math.floorMod(n, 10) == filter);
                        actual.removeIf(e -> e instanceof Integer n && Math.floorMod(n, 10) == filter);
                    }
                }
                default -> {
                    require(expected.contains(item) == actual.contains(item), "contains");
                    require(expected.indexOf(item) == actual.indexOf(item), "indexOf");
                    require(expected.lastIndexOf(item) == actual.lastIndexOf(item), "lastIndexOf");
                }
            }
            equal(expected, actual);
            if (step % 401 == 0) {
                require(expected.reversed().equals(actual.reversed()), "reversed sequence");
                roundTrip(actual, expected);
            }
        }
        storageProof(actual);
        System.out.println("PASS checks=" + checks + " steps=60000 array-lanes=Object[],int[],int[],long[]");
    }
}
