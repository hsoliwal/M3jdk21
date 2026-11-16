/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only
 */

/*
 * @test
 * @summary Copy bounded List windows to caller-owned arrays without copying the entire list
 * @run main/othervm ListRangeWindowCopy
 */

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ListRangeWindowCopy {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wrong exception: " + failure, failure);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }

    private static void exercise(List<Integer> list, String label) {
        Object sentinel = new Object();
        Object[] window = new Object[25];
        for (int from = 0; from < list.size(); from += 37) {
            Arrays.fill(window, sentinel);
            int length = Math.min(17, list.size() - from);
            list.copyRangeToArray(from, length, window, 3);
            for (int i = 0; i < length; i++) {
                check(window[3 + i].equals(list.get(from + i)), label + " element " + i);
            }
            check(window[0] == sentinel && window[1] == sentinel
                    && window[2] == sentinel, label + " prefix touched");
            for (int i = 3 + length; i < window.length; i++) {
                check(window[i] == sentinel, label + " suffix touched " + i);
            }
        }
        Arrays.fill(window, sentinel);
        list.copyRangeToArray(list.size(), 0, window, window.length);
        check(Arrays.stream(window).allMatch(value -> value == sentinel),
                label + " empty window touched array");

        Object[] unchanged = new Object[] {sentinel, sentinel, sentinel};
        expect(IndexOutOfBoundsException.class,
                () -> list.copyRangeToArray(-1, 1, unchanged, 0));
        expect(IndexOutOfBoundsException.class,
                () -> list.copyRangeToArray(0, -1, unchanged, 0));
        expect(IndexOutOfBoundsException.class,
                () -> list.copyRangeToArray(list.size(), 1, unchanged, 0));
        expect(IndexOutOfBoundsException.class,
                () -> list.copyRangeToArray(Integer.MAX_VALUE, 2, unchanged, 0));
        expect(IndexOutOfBoundsException.class,
                () -> list.copyRangeToArray(0, 1, unchanged, -1));
        expect(IndexOutOfBoundsException.class,
                () -> list.copyRangeToArray(0, 1, unchanged, unchanged.length));
        expect(IndexOutOfBoundsException.class,
                () -> list.copyRangeToArray(0, 2, unchanged, unchanged.length - 1));
        expect(NullPointerException.class,
                () -> list.copyRangeToArray(0, 0, null, 0));
        check(Arrays.stream(unchanged).allMatch(value -> value == sentinel),
                label + " invalid argument mutated destination");
    }

    private static final class CountingLinkedList<E> extends LinkedList<E> {
        private static final long serialVersionUID = 1L;
        int iteratorStarts;

        @Override
        public java.util.ListIterator<E> listIterator(int index) {
            iteratorStarts++;
            return super.listIterator(index);
        }
    }

    public static void main(String[] args) {
        ArrayList<Integer> array = new ArrayList<>();
        CountingLinkedList<Integer> linked = new CountingLinkedList<>();
        for (int i = 0; i < 10_000; i++) {
            array.add(i);
            linked.add(i);
        }
        exercise(array, "ArrayList bulk path");
        exercise(linked, "sequential List default");
        check(linked.iteratorStarts > 0, "sequential List failed to use listIterator");
        check(array.size() == 10_000 && linked.size() == 10_000,
                "copy changed original list size");

        exercise(Collections.unmodifiableList(array), "unmodifiable wrapper");
        exercise(array.subList(123, 444), "subList window");
        exercise(List.copyOf(array.subList(0, 200)), "immutable list");

        List<String> nulls = Arrays.asList(null, "second", null);
        Object[] nullable = new Object[] {"before", "unset", "unset", "after"};
        nulls.copyRangeToArray(0, 2, nullable, 1);
        check(nullable[0].equals("before") && nullable[1] == null
                && nullable[2].equals("second") && nullable[3].equals("after"),
                "null propagation or unrelated slot changed");

        List<Object> mixed = new ArrayList<>(List.of("valid", 11));
        expect(ArrayStoreException.class,
                () -> mixed.copyRangeToArray(0, 2, new String[2], 0));
        List<Object> sequentialMixed = new LinkedList<>(mixed);
        expect(ArrayStoreException.class,
                () -> sequentialMixed.copyRangeToArray(0, 2, new String[2], 0));

        // A user-provided AbstractList compiled with no new override also gets the default.
        List<Integer> anonymous = new AbstractList<>() {
            @Override public Integer get(int index) { return index; }
            @Override public int size() { return 4; }
        };
        Object[] extracted = new Object[2];
        anonymous.copyRangeToArray(1, 2, extracted, 0);
        check(extracted[0].equals(1) && extracted[1].equals(2),
                "old AbstractList receiver");
    }
}
