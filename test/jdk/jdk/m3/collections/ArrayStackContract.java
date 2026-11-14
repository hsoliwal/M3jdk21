/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.EmptyStackException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import jdk.m3.collections.apache.M3ArrayStack;

/** Deterministic, independent JDK ArrayList and stack-protocol differential. */
@SuppressWarnings("deprecation")
public final class ArrayStackContract {
    private static long checks;

    private ArrayStackContract() {
    }

    private static void check(boolean condition, String context) {
        checks++;
        if (!condition) {
            throw new AssertionError(context + " check=" + checks);
        }
    }

    private static Object token(Random random) {
        int choice = random.nextInt(10);
        return choice == 0 ? null : choice < 5 ? Integer.valueOf(random.nextInt(11) - 5)
                : "value-" + random.nextInt(13);
    }

    private static int searchFromTop(List<Object> values, Object wanted) {
        for (int i = values.size() - 1; i >= 0; i--) {
            if (wanted == null ? values.get(i) == null : wanted.equals(values.get(i))) {
                return values.size() - i;
            }
        }
        return -1;
    }

    private static void assertEquivalent(List<Object> expected, M3ArrayStack<Object> actual) {
        check(expected.equals(actual), "List.equals");
        check(actual.equals(expected), "symmetric equals");
        check(actual.hashCode() == expected.hashCode(), "hashCode");
        check(Arrays.equals(expected.toArray(), actual.toArray()), "toArray");
        check(actual.size() == expected.size(), "size");
        check(actual.empty() == expected.isEmpty(), "empty");
        check(actual.toString().equals(expected.toString()), "toString");
    }

    private static void roundTrip(M3ArrayStack<Object> actual, List<Object> expected)
            throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(actual);
        }
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            Object copy = in.readObject();
            check(copy instanceof M3ArrayStack, "serialized type");
            check(expected.equals(copy), "serialized sequence");
            check(copy != actual, "detached owner");
        }
    }

    private static void laneProof(M3ArrayStack<?> actual) throws Exception {
        int payloadFields = 0;
        for (Field field : M3ArrayStack.class.getDeclaredFields()) {
            if (field.getName().equals("elements")) {
                check(field.getType() == Object[].class, "Object[] owner");
                payloadFields++;
            }
            check(!field.getType().getSimpleName().contains("Node"), "no Node field");
            check(!field.getType().getSimpleName().contains("Entry"), "no Entry field");
        }
        check(payloadFields == 1, "single payload owner");
        Field elements = M3ArrayStack.class.getDeclaredField("elements");
        elements.setAccessible(true);
        actual.clear();
        for (Object value : (Object[]) elements.get(actual)) {
            check(value == null, "cleared capacity should drop payload reference");
        }
    }

    private static void checkedExceptions(M3ArrayStack<Object> stack) {
        stack.clear();
        try {
            stack.peek();
            throw new AssertionError("peek accepted empty");
        } catch (EmptyStackException expected) {
            checks++;
        }
        try {
            stack.pop();
            throw new AssertionError("pop accepted empty");
        } catch (EmptyStackException expected) {
            checks++;
        }
        try {
            stack.peek(0);
            throw new AssertionError("peek(0) accepted empty");
        } catch (EmptyStackException expected) {
            checks++;
        }
        try {
            stack.peek(-1);
            throw new AssertionError("peek(-1) accepted empty");
        } catch (IndexOutOfBoundsException expected) {
            checks++;
        }
    }

    public static void main(String[] args) throws Exception {
        Random random = new Random(0x4d33415252415953L);
        ArrayList<Object> expected = new ArrayList<>();
        M3ArrayStack<Object> actual = new M3ArrayStack<>();
        for (int step = 0; step < 60_000; step++) {
            if (expected.size() > 500) {
                expected.clear();
                actual.clear();
            }
            Object value = token(random);
            int size = expected.size();
            int index = size == 0 ? 0 : random.nextInt(size + 1);
            switch (random.nextInt(17)) {
                case 0, 1, 2 -> {
                    check(actual.push(value) == value, "push identity");
                    expected.add(value);
                }
                case 3 -> {
                    actual.add(index, value);
                    expected.add(index, value);
                }
                case 4 -> {
                    if (size > 0) {
                        int at = random.nextInt(size);
                        check(Objects.equals(actual.remove(at), expected.remove(at)), "remove");
                    }
                }
                case 5 -> {
                    if (size > 0) {
                        int at = random.nextInt(size);
                        check(Objects.equals(actual.set(at, value),
                                expected.set(at, value)), "set");
                    }
                }
                case 6 -> {
                    if (size > 0) {
                        check(Objects.equals(actual.pop(), expected.remove(size - 1)), "pop");
                    }
                }
                case 7 -> {
                    List<Object> incoming = Arrays.asList(value, null, token(random));
                    check(actual.addAll(index, incoming) == expected.addAll(index, incoming),
                            "addAll(index)");
                }
                case 8 -> {
                    Object wanted = token(random);
                    check(actual.remove(wanted) == expected.remove(wanted), "remove(value)");
                }
                case 9 -> {
                    check(actual.removeIf(item -> item != null && (item.hashCode() & 7) == 0)
                            == expected.removeIf(item -> item != null && (item.hashCode() & 7) == 0),
                            "removeIf");
                }
                case 10 -> {
                    Set<Object> selected = new HashSet<>(Arrays.asList(value, null, token(random)));
                    check(actual.retainAll(selected) == expected.retainAll(selected), "retainAll");
                }
                case 11 -> {
                    if (size > 0) {
                        int at = random.nextInt(size);
                        ListIterator<Object> a = actual.listIterator(at);
                        ListIterator<Object> e = expected.listIterator(at);
                        check(Objects.equals(a.next(), e.next()), "ListIterator next");
                        if (random.nextBoolean()) {
                            a.set(value);
                            e.set(value);
                        } else {
                            a.remove();
                            e.remove();
                        }
                    }
                }
                case 12 -> {
                    if (size > 0) {
                        int at = random.nextInt(size);
                        check(Objects.equals(actual.peek(size - at - 1), expected.get(at)),
                                "peek(depth)");
                        check(actual.search(value) == searchFromTop(expected, value), "search");
                    }
                }
                case 13 -> {
                    if (size > 0) {
                        List<Object> reversed = actual.reversed();
                        List<Object> reference = expected.reversed();
                        if (random.nextBoolean()) {
                            check(Objects.equals(reversed.remove(0), reference.remove(0)),
                                    "reversed remove");
                        } else {
                            reversed.add(0, value);
                            reference.add(0, value);
                        }
                    }
                }
                case 14 -> {
                    actual.ensureCapacity(size + 10);
                    actual.trimToSize();
                }
                case 15 -> {
                    Object copy = actual.clone();
                    check(copy instanceof M3ArrayStack, "clone type");
                    check(expected.equals(copy), "clone content");
                    check(copy != actual, "clone detached");
                }
                default -> {
                    check(actual.search(value) == searchFromTop(expected, value), "search");
                    if (size > 0) {
                        check(Objects.equals(actual.peek(), expected.get(size - 1)),
                                "peek");
                    }
                }
            }
            assertEquivalent(expected, actual);
            if (step % 400 == 0) {
                roundTrip(actual, expected);
            }
        }

        // Same source collection as the target: snapshot before shifting.
        List<Object> input = new ArrayList<>(expected);
        expected.addAll(0, input);
        actual.addAll(0, actual);
        assertEquivalent(expected, actual);

        // Iterator captured before a structural mutation must be invalid.
        Iterator<Object> before = actual.iterator();
        actual.add(null);
        try {
            before.next();
            throw new AssertionError("structural mutation missed by iterator");
        } catch (ConcurrentModificationException expectedException) {
            checks++;
        }
        checkedExceptions(actual);
        laneProof(actual);
        System.out.printf("PASS checks=%d steps=60000 array-lanes=Object[]%n", checks);
    }
}
