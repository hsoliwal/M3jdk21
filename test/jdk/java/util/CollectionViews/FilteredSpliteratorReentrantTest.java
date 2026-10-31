/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
/*
 * @test
 * @summary M3JDK21 filtered CollectionViews spliterator preserves enclosing callback state
 * @run main FilteredSpliteratorReentrantTest
 */
import java.util.Collection;
import java.util.CollectionViews;
import java.util.List;
import java.util.Spliterator;

public class FilteredSpliteratorReentrantTest {
    private static void check(boolean value, String why) {
        if (!value) throw new AssertionError(why);
    }

    private static Spliterator<Integer> selected(List<Integer> values, boolean oddOnly) {
        Collection<Integer> source = values;
        return CollectionViews.filtered(source,
                value -> !oddOnly || (value & 1) == 1).spliterator();
    }

    private static void nestedExhaustionMustNotEraseOuterDelivery() {
        Spliterator<Integer> cursor = selected(List.of(7), false);
        int[] seen = { 0 };
        boolean advanced = cursor.tryAdvance(value -> {
            seen[0]++;
            check(value == 7, "unexpected outer element");
            check(!cursor.tryAdvance(inner -> {
                throw new AssertionError("nested cursor should be exhausted");
            }), "nested empty cursor must report false");
        });
        check(advanced, "outer cursor delivered element but returned false");
        check(seen[0] == 1, "outer consumer invoked more than once");
        check(!cursor.tryAdvance(ignored -> {
            throw new AssertionError("unexpected final element");
        }), "outer exhausted cursor must report false");
    }

    private static void rejectedNestedValuesDoNotEraseDelivery() {
        Spliterator<Integer> cursor = selected(List.of(2, 3, 4), true);
        int[] seen = { -1 };
        check(cursor.tryAdvance(value -> {
            seen[0] = value;
            check(!cursor.tryAdvance(unexpected -> {
                throw new AssertionError("nested predicate admitted even value");
            }), "all remaining values should be rejected");
        }), "filtered outer callback did not preserve delivery");
        check(seen[0] == 3, "wrong selected value");
    }

    private static void nestedSuccessRetainsEncounterOrder() {
        Spliterator<Integer> cursor = selected(List.of(1, 2), false);
        int[] observed = new int[2];
        check(cursor.tryAdvance(value -> {
            observed[0] = value;
            check(cursor.tryAdvance(nested -> observed[1] = nested),
                    "nested accepted value missing");
        }), "outer value missing");
        check(observed[0] == 1 && observed[1] == 2, "encounter order changed");
        check(!cursor.tryAdvance(unexpected -> {
            throw new AssertionError("unexpected extra value");
        }), "cursor should be exhausted");
    }

    public static void main(String[] args) {
        nestedExhaustionMustNotEraseOuterDelivery();
        rejectedNestedValuesDoNotEraseDelivery();
        nestedSuccessRetainsEncounterOrder();
        System.out.println("PASS FilteredSpliteratorReentrantTest");
    }
}
