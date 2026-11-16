/*
 * @test
 * @summary GrowthListView matches Apache Commons GrowthList indexed null-padding and mutation
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main GrowthListViewContractTest
 */
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import jdk.internal.m3.collections.GrowthListView;

public class GrowthListViewContractTest {
    private static long assertions;

    private static void check(boolean result, String description) {
        assertions++;
        if (!result) throw new AssertionError(description);
    }

    private static void equal(Object actual, Object expected, String description) {
        check(Objects.equals(actual, expected), description + ": " + actual + " != " + expected);
    }

    private static Class<? extends Throwable> failure(Runnable call) {
        try { call.run(); } catch (Throwable t) { return t.getClass(); }
        return null;
    }

    private static void same(GrowthListView<Object> actual, List<Object> expected) {
        equal(actual.size(), expected.size(), "logical size");
        Object[] snapshot = actual.toArray();
        check(Arrays.equals(snapshot, expected.toArray()), "snapshot");
        for (int i = 0; i < expected.size(); i++) {
            check(actual.get(i) == expected.get(i), "reference identity " + i);
        }
        if (expected.size() > 0) {
            check(snapshot != actual.toArray(), "fresh snapshot");
        }
    }

    /** Reference semantics: Apache Commons GrowthList decorates an ArrayList. */
    private static Object donorSet(ArrayList<Object> owner, int index, Object element) {
        int size = owner.size();
        if (index >= size) owner.addAll(Collections.nCopies(index - size + 1, null));
        return owner.set(index, element);
    }

    private static void donorAdd(ArrayList<Object> owner, int index, Object element) {
        int size = owner.size();
        if (index > size) owner.addAll(Collections.nCopies(index - size, null));
        owner.add(index, element);
    }

    private static void boundaries() {
        GrowthListView<Object> actual = new GrowthListView<>();
        ArrayList<Object> expected = new ArrayList<>();
        equal(GrowthListView.scaffoldDescriptor(), "M3GrowthListView|GROWTH_LIST|OBJECT|GROWTH|Object[] valueLane", "descriptor");
        same(actual, expected);
        equal(failure(() -> actual.get(-1)), failure(() -> expected.get(-1)), "negative get");
        equal(failure(() -> actual.set(-1, "x")), failure(() -> donorSet(expected, -1, "x")), "negative set");
        equal(failure(() -> actual.add(-1, "x")), failure(() -> donorAdd(expected, -1, "x")), "negative add");
        same(actual, expected);
        Object value = new Object();
        equal(actual.set(4, value), donorSet(expected, 4, value), "set into gap");
        same(actual, expected);
        Object next = new Object();
        actual.add(9, next);
        donorAdd(expected, 9, next);
        same(actual, expected);
        equal(actual.get(9), next, "gap-end value");
        equal(actual.set(9, value), donorSet(expected, 9, value), "overwrite");
        actual.add(4, next);
        donorAdd(expected, 4, next);
        same(actual, expected);
        check(actual.add(null), "append null");
        expected.add(null);
        same(actual, expected);
        equal(failure(() -> actual.get(actual.size())), failure(() -> expected.get(expected.size())), "get at end");
        actual.clear();
        expected.clear();
        same(actual, expected);
        actual.add("after-clear");
        expected.add("after-clear");
        same(actual, expected);
    }

    private static void randomized() {
        for (int seed = 0; seed < 80; seed++) {
            GrowthListView<Object> actual = new GrowthListView<>();
            ArrayList<Object> expected = new ArrayList<>();
            Random random = new Random(0x6137dL + seed * 991L);
            for (int step = 0; step < 350; step++) {
                int kind = random.nextInt(6);
                Object element = random.nextInt(7) == 0 ? null : new Object();
                int index = kind == 4 ? -1 : random.nextInt(Math.min(150, expected.size() + 18));
                switch (kind) {
                    case 0 -> {
                        equal(actual.add(element), expected.add(element), "append result");
                    }
                    case 1, 4 -> {
                        Class<? extends Throwable> f1 = failure(() -> actual.add(index, element));
                        Class<? extends Throwable> f2 = failure(() -> donorAdd(expected, index, element));
                        equal(f1, f2, "add(index) failure");
                    }
                    case 2, 3 -> {
                        final Object[] a = {null}, b = {null};
                        Class<? extends Throwable> f1 = failure(() -> a[0] = actual.set(index, element));
                        Class<? extends Throwable> f2 = failure(() -> b[0] = donorSet(expected, index, element));
                        equal(f1, f2, "set(index) failure");
                        if (f1 == null) check(a[0] == b[0], "set previous identity");
                    }
                    default -> {
                        if (random.nextInt(20) == 0) {
                            actual.clear(); expected.clear();
                        } else if (!expected.isEmpty()) {
                            int j = random.nextInt(expected.size());
                            check(actual.get(j) == expected.get(j), "get previous identity");
                        }
                    }
                }
                same(actual, expected);
            }
        }
    }

    private static void ownership() throws Exception {
        Class<?> type = GrowthListView.class;
        equal(type.getDeclaredField("valueLane").getType(), Object[].class, "single Object[] owner");
        equal(type.getDeclaredField("stateMaskWords").getType(), long[].class, "primitive mask");
        for (Field f : type.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                check(!java.util.Collection.class.isAssignableFrom(f.getType()), "no retained per-entry collection");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        boundaries();
        randomized();
        ownership();
        System.out.println("GrowthListViewContractTest PASS assertions=" + assertions);
    }
}
