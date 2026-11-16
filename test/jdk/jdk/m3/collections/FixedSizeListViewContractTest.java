/*
 * @test
 * @summary FixedSizeListView matches Arrays.asList on backing aliasing, callbacks and covariance
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main FixedSizeListViewContractTest
 */
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import jdk.internal.m3.collections.FixedSizeListView;

/** Exact JDK Arrays.asList differential and reflection test for M3 fixed-size views. */
public final class FixedSizeListViewContractTest {
    private static long assertions;
    private static void eq(Object a, Object b, String message) {
        assertions++;
        if (!Objects.equals(a, b)) throw new AssertionError(message + ": " + a + " != " + b);
    }
    private static void yes(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }
    private static Class<? extends Throwable> error(Runnable action) {
        try { action.run(); } catch (Throwable t) { return t.getClass(); }
        return null;
    }
    private static void testOne(Object[] input) {
        Object[] original = input.clone();
        Object[] expectedArray = input.clone();
        FixedSizeListView<Object> candidate = FixedSizeListView.of(input);
        List<Object> expected = Arrays.asList(expectedArray);
        eq(candidate.size(), expected.size(), "size");
        yes(candidate.asList() instanceof java.util.RandomAccess, "random access API");
        for (int i = 0; i < expected.size(); i++) {
            eq(candidate.get(i), expected.get(i), "get");
            eq(candidate.indexOf(expected.get(i)), expected.indexOf(expected.get(i)), "indexOf");
            eq(candidate.contains(expected.get(i)), expected.contains(expected.get(i)), "contains");
        }
        eq(candidate.indexOf(new Object()), expected.indexOf(new Object()), "missing");
        eq(candidate.indexOf(null), expected.indexOf(null), "null index");
        yes(Arrays.equals(candidate.toArray(), expected.toArray()), "snapshot content");
        Object[] snapshot = candidate.toArray();
        yes(snapshot != input, "snapshot distinct");
        for (int i = 0; i < input.length; i++) {
            Object value = i % 3 == 0 ? null : i % 3 == 1 ? "changed-" + i : Integer.valueOf(i);
            Object oldExpected = expected.set(i, value);
            Object oldActual = candidate.set(i, value);
            eq(oldActual, oldExpected, "set old");
            eq(input[i], expectedArray[i], "backing write-through");
            eq(candidate.asList().get(i), value, "facade sees owner set");
            if (i == 0) {
                candidate.asList().set(i, original[0]);
                expected.set(i, original[0]);
                eq(input[i], expectedArray[i], "facade write-through");
            }
        }
        yes(Arrays.equals(candidate.toArray(), expected.toArray()), "after set");
        eq(error(() -> candidate.add(null)), error(() -> expected.add(null)), "fixed-size add");
        eq(error(() -> candidate.asList().remove(0)),
                error(() -> expected.remove(0)), "fixed-size remove");
        eq(error(() -> candidate.get(-1)), error(() -> expected.get(-1)), "negative get exception");
        eq(error(() -> candidate.set(expected.size(), null)),
                error(() -> expected.set(expected.size(), null)), "set high exception");
        yes(Arrays.equals(snapshot, original), "detached snapshot");
    }
    private static void testOpaqueEquality() {
        class Query {
            int calls;
            @Override public boolean equals(Object value) {
                calls++;
                return value instanceof String s && s.equals("needle");
            }
            @Override public int hashCode() { return 1; }
        }
        Object[] source = {"other", "needle", "needle", null};
        Query q1 = new Query(), q2 = new Query();
        FixedSizeListView<Object> owner = FixedSizeListView.of(source);
        List<Object> expected = Arrays.asList(source);
        eq(owner.indexOf(q1), expected.indexOf(q2), "custom equality direction");
        eq(q1.calls, q2.calls, "custom equality callback count");
        yes(owner.contains(q1), "custom contains");
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void testCovariance() {
        String[] strings = {"first", "second"};
        FixedSizeListView<String> view = FixedSizeListView.of(strings);
        eq(view.set(0, "replacement"), "first", "typed write");
        eq(strings[0], "replacement", "typed backing");
        @SuppressWarnings("rawtypes") FixedSizeListView raw = view;
        eq(error(() -> raw.set(0, Integer.valueOf(3))), ArrayStoreException.class,
                "runtime array store check");
        eq(strings[0], "replacement", "no corrupt write");
        eq(error(() -> raw.asList().set(1, Integer.valueOf(3))),
                ArrayStoreException.class, "facade array store check");
        eq(error(() -> FixedSizeListView.of(null)), NullPointerException.class,
                "null backing refusal");
    }
    private static void testStorage() throws Exception {
        Field backing = FixedSizeListView.class.getDeclaredField("valueLane");
        Field masks = FixedSizeListView.class.getDeclaredField("stateMaskWords");
        eq(backing.getType(), Object[].class, "single array storage lane");
        eq(masks.getType(), long[].class, "primitive state mask lane");
        for (Field field : FixedSizeListView.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                yes(!java.util.Collection.class.isAssignableFrom(field.getType()),
                        "no retained collection wrapper: " + field.getName());
            }
        }
    }
    public static void main(String[] args) throws Exception {
        testOne(new Object[0]);
        testOne(new Object[]{null});
        testOne(new Object[]{"a", 1, "b", null, Long.valueOf(7L)});
        Random rnd = new Random(0x545dc01L);
        for (int n : new int[]{2, 3, 31, 64, 65, 127, 128, 1024}) {
            Object[] values = new Object[n];
            for (int i = 0; i < n; i++) {
                int k = rnd.nextInt(7);
                values[i] = k == 0 ? null : k % 2 == 0 ? Integer.valueOf(k) : "word-" + k;
            }
            testOne(values);
        }
        testOpaqueEquality();
        testCovariance();
        testStorage();
        FixedSizeListView<Object> empty = new FixedSizeListView<>();
        eq(empty.size(), 0, "empty constructor");
        eq(empty.asList(), List.of(), "empty facade");
        System.out.println("FixedSizeListViewContractTest PASS assertions=" + assertions);
    }
}
