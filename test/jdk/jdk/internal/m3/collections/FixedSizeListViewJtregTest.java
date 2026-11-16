/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Fixed-size M3 object array view matches Java 21 Arrays.asList
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm FixedSizeListViewJtregTest
 */
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import jdk.internal.m3.collections.FixedSizeListView;

/** Source-pinned, primitive-free array-view compatibility oracle. */
public final class FixedSizeListViewJtregTest {
    private static int checks;
    private static final Object[] VALUES = {
        null, "one", "two", Integer.valueOf(-1), Long.valueOf(Long.MAX_VALUE), new Object()
    };

    private interface Operation { Object call(); }

    private static Object attempt(Operation operation) {
        try { return operation.call(); }
        catch (RuntimeException failure) { return failure.getClass(); }
    }

    private static void check(boolean ok, String context) {
        checks++;
        if (!ok) throw new AssertionError(context);
    }

    private static void same(Object actual, Object reference, String context) {
        if (actual instanceof Object[] x && reference instanceof Object[] y) {
            check(x.getClass() == y.getClass(), context + " array type");
            check(Arrays.equals(x, y), context + " array elements");
        } else {
            check(Objects.equals(actual, reference), context + " mismatch: " + actual + " / " + reference);
        }
    }

    private static void edges() {
        var empty = new FixedSizeListView<Object>();
        check(empty.size() == 0, "empty constructor");
        check(empty.toArray().getClass() == Object[].class, "empty Object[]");
        same(attempt(() -> empty.add("x")),
                attempt(() -> Arrays.asList(new Object[0]).add("x")), "empty append");
        same(attempt(() -> FixedSizeListView.of(null)),
                attempt(() -> Arrays.asList((Object[]) null)), "null backing");
        String[] data = {"a", "b"};
        Object[] covariant = data;
        var view = FixedSizeListView.of(covariant);
        Object[] copy = view.toArray();
        check(copy != data && copy.getClass() == Object[].class, "detached Object[]");
        data[0] = "changed";
        check("changed".equals(view.get(0)), "live caller array");
        check("a".equals(copy[0]), "detached copy");
        same(attempt(() -> view.set(0, new Object())),
                attempt(() -> Arrays.asList(covariant).set(0, new Object())), "covariant store");
        same(attempt(() -> view.get(-1)),
                attempt(() -> Arrays.asList(covariant).get(-1)), "negative get");
        same(attempt(() -> view.set(3, null)),
                attempt(() -> Arrays.asList(covariant).set(3, null)), "upper set");
        List<Object> facade = view.asList();
        facade.set(1, "facade");
        check("facade".equals(data[1]), "facade writes original");
        same(attempt(() -> facade.add("x")),
                attempt(() -> Arrays.asList(covariant).add("x")), "facade append");
        Object asym = new Object() {
            @Override public boolean equals(Object value) { return "changed".equals(value); }
            @Override public int hashCode() { return 31; }
        };
        check(view.indexOf(asym) == 0, "search-argument equals direction");
    }

    private static void randomDifferential() {
        SplittableRandom rng = new SplittableRandom(0x4D33464958454453L);
        for (int round = 0; round < 32; round++) {
            int n = rng.nextInt(25);
            Object[] actual = new Object[n], expected = new Object[n];
            for (int i = 0; i < n; i++) actual[i] = expected[i] = VALUES[rng.nextInt(VALUES.length)];
            var view = FixedSizeListView.of(actual);
            List<Object> jdk = Arrays.asList(expected);
            List<Object> facade = view.asList();
            for (int k = 0; k < 10_000; k++) {
                int op = rng.nextInt(10), index = rng.nextInt(-2, n + 3);
                Object value = VALUES[rng.nextInt(VALUES.length)];
                String context = "round " + round + " op " + k + " choice " + op;
                switch (op) {
                    case 0 -> same(attempt(() -> view.get(index)),
                            attempt(() -> jdk.get(index)), context);
                    case 1 -> same(attempt(() -> view.set(index, value)),
                            attempt(() -> jdk.set(index, value)), context);
                    case 2 -> same(view.indexOf(value), jdk.indexOf(value), context);
                    case 3 -> same(view.contains(value), jdk.contains(value), context);
                    case 4 -> same(view.toArray(), jdk.toArray(), context);
                    case 5 -> same(attempt(() -> facade.set(index, value)),
                            attempt(() -> jdk.set(index, value)), context);
                    case 6 -> same(view.size(), jdk.size(), context);
                    case 7 -> same(attempt(() -> view.add(value)),
                            attempt(() -> jdk.add(value)), context);
                    case 8 -> {
                        if (n != 0) actual[0] = expected[0] = value;
                        same(view.getClass().getName().contains("FixedSizeListView"), true, context);
                    }
                    default -> same(attempt(() -> facade.add(value)),
                            attempt(() -> jdk.add(value)), context);
                }
                check(Arrays.equals(actual, expected), context + " backing mismatch");
            }
        }
    }

    public static void main(String[] args) {
        edges();
        randomDifferential();
        System.out.println("FIXEDSIZE_JTREG_PASS operations=320000 checks=" + checks);
    }
}
