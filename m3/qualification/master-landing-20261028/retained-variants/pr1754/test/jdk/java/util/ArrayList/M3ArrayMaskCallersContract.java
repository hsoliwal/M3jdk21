/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * @test
 * @summary Original java.util ArrayList/Vector removal contracts with array-offset masks
 * @modules java.base/java.util:+open
 * @run main/othervm M3ArrayMaskCallersContract
 * @run main/othervm -Xint M3ArrayMaskCallersContract
 */
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public final class M3ArrayMaskCallersContract {
    private static long checks;

    private record Item(int value) {}

    public static void main(String[] args) throws Exception {
        int[] sizes = {0, 1, 2, 3, 31, 63, 64, 65, 127, 511, 512, 513, 1023, 1024, 2048};
        int[] pivots = {0, 1, 13, 511, 512};
        for (boolean vector : new boolean[]{false, true}) {
            for (int size : sizes) {
                for (int policy = 0; policy < 6; policy++) {
                    for (int pivot : pivots) {
                        checkRemoval(newList(vector, size + 8), size, policy, pivot);
                    }
                }
            }
            checkExceptions(newList(vector, 720));
            checkReentrantMutation(newList(vector, 720));
        }
        check(ArrayList.class.getClassLoader() == null);
        check(Vector.class.getClassLoader() == null);
        System.out.println("PASS M3JDK21 ArrayList+Vector mask-callers checks=" + checks);
    }

    private static List<Item> newList(boolean vector, int capacity) {
        return vector ? new Vector<>(capacity) : new ArrayList<>(capacity);
    }

    private static boolean removed(Item item, int policy, int pivot) {
        int number = item == null ? -1 : item.value();
        return switch (policy) {
            case 0 -> false;
            case 1 -> true;
            case 2 -> number == pivot;
            case 3 -> number >= pivot && number >= 0;
            case 4 -> item == null || number >= 0 && number % 7 == 0;
            default -> number >= 0 && (number * 31 + 13) % 17 < 5;
        };
    }

    private static void fill(List<Item> list, int count) {
        Item duplicate = new Item(-1);
        for (int i = 0; i < count; i++) {
            list.add(i % 19 == 0 ? null : i % 17 == 0 ? duplicate : new Item(i));
        }
    }

    private static void checkRemoval(List<Item> list, int length, int policy, int pivot) throws Exception {
        fill(list, length);
        Item[] before = list.toArray(new Item[0]);
        List<Item> retained = new ArrayList<>();
        for (Item item : before) {
            if (!removed(item, policy, pivot)) retained.add(item);
        }
        Iterator<Item> previous = list.iterator();
        int[] evaluations = {0};
        boolean changed = list.removeIf(item -> {
            evaluations[0]++;
            return removed(item, policy, pivot);
        });
        check(evaluations[0] == length);
        check(changed == (retained.size() != length));
        check(list.size() == retained.size());
        for (int index = 0; index < retained.size(); index++) {
            check(list.get(index) == retained.get(index));
        }
        Object[] owned = backingArray(list);
        for (int index = list.size(); index < length; index++) {
            check(owned[index] == null);
        }
        if (length > 0 && changed) {
            expect(ConcurrentModificationException.class, previous::next);
        } else if (length > 0) {
            check(previous.next() == before[0]);
        } else {
            check(!previous.hasNext());
        }
    }

    private static void checkExceptions(List<Item> list) throws Exception {
        fill(list, 640);
        Item[] original = list.toArray(new Item[0]);
        Iterator<Item> iterator = list.iterator();
        expect(NullPointerException.class, () -> list.removeIf(null));
        RuntimeException failure = new ArithmeticException("predicate rollback");
        int[] seen = {0};
        try {
            list.removeIf(item -> {
                if (++seen[0] == 320) throw failure;
                return removed(item, 4, 0);
            });
            throw new AssertionError("expected predicate exception");
        } catch (ArithmeticException observed) {
            check(observed == failure);
        }
        check(seen[0] == 320);
        check(list.size() == original.length);
        for (int i = 0; i < original.length; i++) check(list.get(i) == original[i]);
        check(iterator.next() == original[0]);
    }

    private static void checkReentrantMutation(List<Item> list) throws Exception {
        fill(list, 640);
        Item[] original = list.toArray(new Item[0]);
        Item appended = new Item(9999);
        int[] calls = {0};
        expect(ConcurrentModificationException.class, () -> list.removeIf(item -> {
            if (++calls[0] == 1) list.add(appended);
            return removed(item, 4, 0);
        }));
        check(list.size() == original.length + 1);
        for (int i = 0; i < original.length; i++) check(list.get(i) == original[i]);
        check(list.get(original.length) == appended);
    }

    private static Object[] backingArray(List<Item> list) throws Exception {
        Field field = (list instanceof Vector<?> ? Vector.class : ArrayList.class)
                .getDeclaredField("elementData");
        field.setAccessible(true);
        return (Object[]) field.get(list);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable observed) {
            check(type.isInstance(observed));
            return;
        }
        throw new AssertionError("missing " + type.getName());
    }

    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("M3 array-mask collection contract check " + checks);
    }
}
