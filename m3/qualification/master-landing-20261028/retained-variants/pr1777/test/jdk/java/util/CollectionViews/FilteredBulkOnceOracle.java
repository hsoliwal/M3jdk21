// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary M3JDK21 CollectionViews validates, copies and forwards input once
 * @run main FilteredBulkOnceOracle
 */
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
public final class FilteredBulkOnceOracle {
    private static int checks;
    private static void check(boolean ok) {
        checks++;
        if (!ok) throw new AssertionError("check=" + checks);
    }
    private static <E> Collection<E> once(List<E> payload) {
        return new AbstractCollection<>() {
            private boolean consumed;
            @Override public int size() { return payload.size(); }
            @Override public Iterator<E> iterator() {
                if (consumed) throw new IllegalStateException("input traversed twice");
                consumed = true;
                return payload.iterator();
            }
        };
    }
    public static void main(String[] ignored) {
        ArrayList<String> source = new ArrayList<>(List.of("first"));
        AtomicInteger selected = new AtomicInteger();
        Collection<String> filtered = java.util.CollectionViews.filtered(
                source, x -> { selected.incrementAndGet(); return x != null; });
        String a = new String("same"), b = new String("same");
        check(filtered.addAll(once(List.of(a, b))));
        check(selected.get() == 2);
        check(source.size() == 3 && source.get(1) == a && source.get(2) == b);
        try {
            filtered.addAll(once(java.util.Arrays.asList("okay", null, "later")));
            throw new AssertionError("expected rejection");
        } catch (IllegalArgumentException expected) { checks++; }
        check(source.size() == 3);
        check(!filtered.addAll(once(List.of())));
        check(source.size() == 3);
        System.out.println("FilteredBulkOnceOracle PASS checks=" + checks);
    }
}
