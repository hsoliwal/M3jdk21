/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary M3 indexed object list: order-statistic offsets, recycled slots, packed masks, fail-fast iterator
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm --add-opens=jdk.m3.collections/jdk.internal.m3.collections=ALL-UNNAMED IndexedListOrderLaneTest
 */
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;
import jdk.internal.m3.collections.IndexedList;

public final class IndexedListOrderLaneTest {
    private static long assertions;

    private record Outcome(Object value, Class<? extends RuntimeException> exception) {}

    public static void main(String[] args) throws Exception {
        emptyAndEdgeCases();
        randomizedDifferential();
        iteratorContracts();
        recycleAndReferenceRelease();
        System.out.println("IndexedListOrderLaneTest PASS " + assertions + " assertions");
    }

    private static void emptyAndEdgeCases() throws Exception {
        IndexedList<String> list = new IndexedList<>();
        check(list.size() == 0, "initial cardinality");
        check(list.indexOf(null) == -1, "missing null");
        check(list.indexOf("x") == -1, "missing object");
        check(IndexedList.scaffoldDescriptor().equals(
                "M3IndexedList|INDEXED_LIST|OBJECT|OCCUPIED,DELETED,BALANCE|Object[] valueLane;int[] leftLane;int[] rightLane;int[] sizeLane"),
                "descriptor compatibility");
        expect(IndexOutOfBoundsException.class, () -> list.get(0));
        expect(IndexOutOfBoundsException.class, () -> list.get(-1));
        expect(IndexOutOfBoundsException.class, () -> list.add(-1, "no"));
        expect(IndexOutOfBoundsException.class, () -> list.add(1, "no"));
        expect(IndexOutOfBoundsException.class, () -> list.remove(0));
        expect(IndexOutOfBoundsException.class, () -> list.set(0, "no"));
        expect(IndexOutOfBoundsException.class, () -> list.listIterator(-1));
        expect(IndexOutOfBoundsException.class, () -> list.listIterator(1));
        ListIterator<String> empty = list.listIterator(0);
        check(!empty.hasNext() && !empty.hasPrevious(), "empty iterator");
        expect(NoSuchElementException.class, empty::next);
        expect(NoSuchElementException.class, empty::previous);
        expect(IllegalStateException.class, empty::remove);
        expect(IllegalStateException.class, () -> empty.set("x"));
        list.add(null);
        list.add("a");
        list.add(1, "b");
        check(list.indexOf(null) == 0 && list.indexOf("b") == 1, "nullable insertion");
        check(list.set(0, "b") == null, "previous null");
        check(list.indexOf("b") == 0, "first of duplicate");
        check("b".equals(list.remove(1)), "remove duplicate");
        check(list.size() == 2, "size after remove");
        list.clear();
        check(list.size() == 0, "clear");
        verifyStorage(list);
    }

    private static void randomizedDifferential() throws Exception {
        for (int seed = 0; seed < 32; seed++) {
            Random random = new Random(0x6D334152524159L ^ ((long) seed << 33));
            IndexedList<Object> actual = new IndexedList<>();
            ArrayList<Object> expected = new ArrayList<>();
            for (int step = 0; step < 8_000; step++) {
                int action = random.nextInt(100);
                int size = expected.size();
                Object value = randomValue(random);
                if (action < 38) {
                    int index = random.nextInt(size + 1);
                    actual.add(index, value);
                    expected.add(index, value);
                } else if (action < 49) {
                    check(actual.add(value) == expected.add(value), "append result");
                } else if (action < 63 && size > 0) {
                    int index = random.nextInt(size);
                    check(Objects.equals(actual.remove(index), expected.remove(index)), "removed value");
                } else if (action < 75 && size > 0) {
                    int index = random.nextInt(size);
                    check(Objects.equals(actual.set(index, value), expected.set(index, value)), "replaced value");
                } else if (action < 83 && size > 0) {
                    int index = random.nextInt(size);
                    check(Objects.equals(actual.get(index), expected.get(index)), "retrieved value");
                } else if (action < 91) {
                    check(actual.indexOf(value) == expected.indexOf(value), "indexed equality search");
                } else if (action < 94) {
                    actual.clear();
                    expected.clear();
                } else {
                    iteratorRound(random, actual, expected);
                }
                check(actual.size() == expected.size(), "rank cardinality");
                if (!expected.isEmpty()) {
                    int sample = random.nextInt(expected.size());
                    check(Objects.equals(actual.get(sample), expected.get(sample)), "sample order");
                }
                if ((step & 127) == 0) {
                    compareAll(actual, expected);
                    verifyStorage(actual);
                }
            }
            compareAll(actual, expected);
            verifyStorage(actual);
        }
    }

    private static void iteratorContracts() throws Exception {
        IndexedList<Integer> actual = new IndexedList<>();
        ArrayList<Integer> expected = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            actual.add(i);
            expected.add(i);
        }
        Random random = new Random(0xA11CE55);
        for (int i = 0; i < 3_000; i++) {
            iteratorRound(random, actual, expected);
            compareAll(actual, expected);
        }
        ListIterator<Integer> stale = actual.listIterator(0);
        actual.add(0, 700);
        expect(ConcurrentModificationException.class, stale::next);
        expect(ConcurrentModificationException.class, () -> stale.add(9));
        expected.add(0, 700);
        compareAll(actual, expected);
        verifyStorage(actual);
    }

    private static <T> void iteratorRound(Random r, IndexedList<T> actual, ArrayList<T> expected) {
        int index = r.nextInt(expected.size() + 1);
        ListIterator<T> first = actual.listIterator(index);
        ListIterator<T> second = expected.listIterator(index);
        for (int action = 0; action < 25; action++) {
            check(first.hasNext() == second.hasNext(), "iterator hasNext");
            check(first.hasPrevious() == second.hasPrevious(), "iterator hasPrevious");
            check(first.nextIndex() == second.nextIndex(), "iterator nextIndex");
            check(first.previousIndex() == second.previousIndex(), "iterator previousIndex");
            int choice = r.nextInt(7);
            if (choice == 0) compare(() -> first.next(), () -> second.next(), "next");
            else if (choice == 1) compare(() -> first.previous(), () -> second.previous(), "previous");
            else if (choice == 2) compare(() -> { first.remove(); return null; },
                    () -> { second.remove(); return null; }, "iterator remove");
            else if (choice == 3) {
                @SuppressWarnings("unchecked")
                T value = (T) (r.nextBoolean() ? null : Integer.valueOf(r.nextInt(31) - 15));
                compare(() -> { first.set(value); return null; },
                        () -> { second.set(value); return null; }, "iterator set");
            } else if (choice == 4) {
                @SuppressWarnings("unchecked")
                T value = (T) (r.nextBoolean() ? null : Integer.valueOf(r.nextInt(31) - 15));
                compare(() -> { first.add(value); return null; },
                        () -> { second.add(value); return null; }, "iterator add");
            } else if (choice == 5) {
                check(first.hasNext() == second.hasNext(), "double hasNext");
            } else {
                check(first.hasPrevious() == second.hasPrevious(), "double hasPrevious");
            }
        }
        check(actual.size() == expected.size(), "iterator cardinality");
    }

    private static Object randomValue(Random r) {
        return switch (r.nextInt(9)) {
            case 0, 1 -> null;
            case 2 -> "";
            case 3 -> "same";
            case 4 -> Integer.valueOf(r.nextInt(11) - 5);
            case 5 -> Long.valueOf(r.nextLong());
            default -> "slot-" + r.nextInt(301);
        };
    }

    private static void recycleAndReferenceRelease() throws Exception {
        IndexedList<Object> list = new IndexedList<>();
        ArrayList<Object> model = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            int index = (i * 17) % (i + 1);
            Object value = "item-" + i;
            list.add(index, value);
            model.add(index, value);
        }
        compareAll(list, model);
        verifyStorage(list);
        int allocatedAtPeak = (int) field(list, "allocated");
        for (int i = 0; i < 9_000; i++) {
            int rank = i % list.size();
            check(Objects.equals(list.remove(rank), model.remove(rank)), "bulk removal");
        }
        verifyStorage(list);
        for (int i = 0; i < 8_000; i++) {
            Object value = "reused-" + i;
            list.add(value);
            model.add(value);
        }
        check(((Integer) field(list, "allocated")) == allocatedAtPeak, "recycled offsets");
        compareAll(list, model);
        verifyStorage(list);
        list.clear();
        check(((Integer) field(list, "allocated")) == 0, "reset high water");
        verifyStorage(list);
    }

    private static void compareAll(IndexedList<?> actual, ArrayList<?> expected) {
        check(actual.size() == expected.size(), "full size");
        for (int i = 0; i < expected.size(); i++) {
            check(Objects.equals(actual.get(i), expected.get(i)), "full indexed content");
        }
        ListIterator<?> a = actual.listIterator(actual.size());
        ListIterator<?> b = expected.listIterator(expected.size());
        while (b.hasPrevious()) check(Objects.equals(a.previous(), b.previous()), "reverse walk");
        check(!a.hasPrevious(), "reverse complete");
    }

    private static void verifyStorage(IndexedList<?> list) throws Exception {
        Object[] values = (Object[]) field(list, "valueLane");
        int[] next = (int[]) field(list, "nextLane");
        int[] previous = (int[]) field(list, "previousLane");
        long[] state = (long[]) field(list, "stateMaskWords");
        int high = (int) field(list, "allocated");
        int first = (int) field(list, "firstSlot");
        int last = (int) field(list, "lastSlot");
        int free = (int) field(list, "freeHead");
        check(values.length == next.length && next.length == previous.length, "aligned slot lanes");
        check(state.length == (values.length == 0 ? 0 : ((values.length - 1 + 31) >>> 5)),
                "32 two-bit entries per word");
        boolean[] visited = new boolean[high + 1];
        int previousSeen = 0;
        int live = 0;
        for (int slot = first; slot != 0; slot = next[slot]) {
            check(slot > 0 && slot <= high && !visited[slot], "live offset bounds/uniqueness");
            visited[slot] = true;
            check(previous[slot] == previousSeen, "doubly linked inverse");
            check(stateOf(state, slot) == 1L, "packed live state");
            check(Objects.equals(list.get(live), values[slot]), "rank to stable offset");
            previousSeen = slot;
            live++;
        }
        check(live == list.size() && previousSeen == last, "live chain cardinality");
        int freed = 0;
        for (int slot = free; slot != 0; slot = previous[slot]) {
            check(slot > 0 && slot <= high && !visited[slot], "free offset bounds/uniqueness");
            visited[slot] = true;
            check(stateOf(state, slot) == 2L, "packed deleted state");
            check(values[slot] == null, "no retained deleted reference");
            freed++;
        }
        check(live + freed == high, "all allocated offsets accounted");
        for (int slot = high + 1; slot < values.length; slot++) {
            check(values[slot] == null && stateOf(state, slot) == 0L, "unused capacity empty");
        }
    }

    private static long stateOf(long[] words, int slot) {
        int index = slot - 1;
        return (words[index >>> 5] >>> ((index & 31) << 1)) & 3L;
    }

    private static Object field(Object owner, String name) throws Exception {
        Field f = owner.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(owner);
    }

    private static Outcome outcome(Callable<?> action) {
        try {
            return new Outcome(action.call(), null);
        } catch (RuntimeException exception) {
            return new Outcome(null, exception.getClass());
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static void compare(Callable<?> actual, Callable<?> expected, String context) {
        check(Objects.equals(outcome(actual), outcome(expected)), context);
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable operation) {
        try {
            operation.run();
        } catch (RuntimeException failure) {
            check(type.isInstance(failure), "expected " + type + " but got " + failure);
            return;
        }
        throw new AssertionError("expected " + type);
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message + " at assertion " + assertions);
    }
}
