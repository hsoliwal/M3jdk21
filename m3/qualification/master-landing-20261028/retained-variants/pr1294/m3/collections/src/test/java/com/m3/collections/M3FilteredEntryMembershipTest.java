// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.Test;

/** Backing membership remains authoritative for live read-only sorted entry views. */
final class M3FilteredEntryMembershipTest {
    @Test void comparatorAliasesUseTheStoredKeyInBothFactories() {
        BigDecimal stored = new BigDecimal("1.0"), alias = new BigDecimal("1.00");
        for (boolean navigable : new boolean[]{false, true}) {
            NavigableMap<BigDecimal, String> owner = new TreeMap<>();
            owner.put(stored, "v");
            List<BigDecimal> calls = new ArrayList<>();
            SortedMap<BigDecimal, String> selected = view(owner, (key, value) -> {
                calls.add(key);
                return key.scale() == 1;
            }, navigable);
            Map.Entry<BigDecimal, String> probe = Map.entry(alias, "v");
            assertTrue(owner.entrySet().contains(probe));
            assertTrue(selected.entrySet().contains(probe));
            assertEquals(1, calls.size());
            assertSame(stored, calls.getFirst());
            calls.clear();
            assertTrue(selected.sequencedEntrySet().contains(probe));
            assertEquals(1, calls.size());
            assertSame(stored, calls.getFirst());
        }
    }

    @Test void absentWrongValueAndNonEntryProbesDoNotDemandThePredicate() {
        for (boolean navigable : new boolean[]{false, true}) {
            NavigableMap<BigDecimal, String> owner = new TreeMap<>();
            owner.put(new BigDecimal("1.0"), "v");
            SortedMap<BigDecimal, String> selected = view(owner,
                    (key, value) -> { throw new AssertionError("predicate not demanded"); },
                    navigable);
            assertFalse(selected.entrySet().contains(Map.entry(new BigDecimal("1.00"), "wrong")));
            assertFalse(selected.entrySet().contains(Map.entry(new BigDecimal("2.00"), "v")));
            assertFalse(selected.entrySet().contains("not an entry"));
            assertFalse(selected.entrySet().contains(null));
        }
    }

    @Test void nullValuesKeepPresenceSeparateFromVisibility() {
        for (boolean navigable : new boolean[]{false, true}) {
            BigDecimal stored = new BigDecimal("1.0"), alias = new BigDecimal("1.00");
            NavigableMap<BigDecimal, String> owner = new TreeMap<>();
            owner.put(stored, null);
            SortedMap<BigDecimal, String> visible = view(owner,
                    (key, value) -> key == stored && value == null, navigable);
            assertTrue(visible.entrySet().contains(new SimpleImmutableEntry<>(alias, null)));
            assertFalse(visible.entrySet().contains(new SimpleImmutableEntry<>(new BigDecimal("2"), null)));
            SortedMap<BigDecimal, String> hidden = view(owner, (key, value) -> false, navigable);
            assertFalse(hidden.entrySet().contains(new SimpleImmutableEntry<>(alias, null)));
        }
    }

    @Test void backingNullAndTypeFailuresPrecedePredicateDemand() {
        for (boolean navigable : new boolean[]{false, true}) {
            NavigableMap<BigDecimal, String> owner = new TreeMap<>();
            owner.put(BigDecimal.ONE, "v");
            SortedMap<BigDecimal, String> selected = view(owner,
                    (key, value) -> { throw new AssertionError("predicate not demanded"); },
                    navigable);
            assertThrows(NullPointerException.class,
                    () -> selected.entrySet().contains(new SimpleImmutableEntry<>(null, "v")));
            assertThrows(ClassCastException.class,
                    () -> selected.entrySet().contains(Map.entry("wrong key type", "v")));
        }
    }

    @Test void nullFriendlyBackingKeysRetainTheirMembership() {
        for (boolean navigable : new boolean[]{false, true}) {
            NavigableMap<BigDecimal, String> owner = new TreeMap<>(
                    Comparator.nullsFirst(Comparator.naturalOrder()));
            owner.put(null, "v");
            SortedMap<BigDecimal, String> selected =
                    view(owner, (key, value) -> key == null, navigable);
            assertTrue(selected.entrySet().contains(new SimpleImmutableEntry<>(null, "v")));
            assertFalse(selected.entrySet().contains(new SimpleImmutableEntry<>(null, "wrong")));
        }
    }

    @Test void comparatorAndPredicateThrowablesKeepTheirIdentity() {
        BigDecimal stored = new BigDecimal("1.0"), alias = new BigDecimal("1.00");
        RuntimeException comparison = new RuntimeException("comparison");
        Comparator<BigDecimal> comparator = (left, right) -> {
            if (left == alias) throw comparison;
            return left.compareTo(right);
        };
        NavigableMap<BigDecimal, String> owner = new TreeMap<>(comparator);
        owner.put(stored, "v");
        for (boolean navigable : new boolean[]{false, true}) {
            SortedMap<BigDecimal, String> selected = view(owner,
                    (key, value) -> { throw new AssertionError("predicate not demanded"); },
                    navigable);
            assertSame(comparison, assertThrows(RuntimeException.class,
                    () -> selected.entrySet().contains(Map.entry(alias, "v"))));
        }
        owner = new TreeMap<>();
        owner.put(stored, "v");
        RuntimeException predicate = new RuntimeException("predicate");
        for (boolean navigable : new boolean[]{false, true}) {
            SortedMap<BigDecimal, String> selected = view(owner,
                    (key, value) -> { assertSame(stored, key); throw predicate; },
                    navigable);
            assertSame(predicate, assertThrows(RuntimeException.class,
                    () -> selected.entrySet().contains(Map.entry(alias, "v"))));
        }
    }

    @Test void storedValueOwnsTheBackingEqualityComparison() {
        List<Value> calls = new ArrayList<>();
        Value stored = new Value("v", calls), probe = new Value("v", calls);
        NavigableMap<BigDecimal, Value> owner = new TreeMap<>();
        owner.put(BigDecimal.ONE, stored);
        SortedMap<BigDecimal, Value> selected = M3FilteredViews.filterEntries(
                (SortedMap<BigDecimal, Value>) owner, (key, value) -> true);
        assertTrue(selected.entrySet().contains(Map.entry(BigDecimal.ONE, probe)));
        assertEquals(1, calls.size());
        assertSame(stored, calls.getFirst());
    }

    @Test void rangeDescendingAndReversedViewsStayLiveAndReadOnly() {
        BigDecimal stored = new BigDecimal("1.0"), alias = new BigDecimal("1.00");
        NavigableMap<BigDecimal, String> owner = new TreeMap<>();
        owner.put(BigDecimal.ZERO, "hidden");
        owner.put(stored, "v");
        owner.put(new BigDecimal("2.0"), "tail");
        NavigableMap<BigDecimal, String> selected =
                M3FilteredViews.filterEntries(owner, (key, value) -> key.signum() > 0);
        List<SortedMap<BigDecimal, String>> variants = List.of(
                selected, selected.descendingMap(),
                selected.subMap(BigDecimal.ONE, true, new BigDecimal("3"), false),
                view(owner, (key, value) -> key.signum() > 0, false).reversed());
        for (SortedMap<BigDecimal, String> variant : variants) {
            assertTrue(variant.entrySet().contains(Map.entry(alias, "v")));
            assertThrows(UnsupportedOperationException.class,
                    () -> variant.entrySet().remove(Map.entry(alias, "v")));
            assertThrows(UnsupportedOperationException.class,
                    () -> variant.entrySet().iterator().next().setValue("changed"));
        }
        owner.put(stored, "updated");
        for (SortedMap<BigDecimal, String> variant : variants) {
            assertFalse(variant.entrySet().contains(Map.entry(alias, "v")));
            assertTrue(variant.entrySet().contains(Map.entry(alias, "updated")));
        }
        owner.remove(stored);
        for (SortedMap<BigDecimal, String> variant : variants)
            assertFalse(variant.entrySet().contains(Map.entry(alias, "updated")));
    }

    private static <V> SortedMap<BigDecimal, V> view(
            NavigableMap<BigDecimal, V> owner,
            BiPredicate<? super BigDecimal, ? super V> predicate, boolean navigable) {
        return navigable ? M3FilteredViews.filterEntries(owner, predicate)
                : M3FilteredViews.filterEntries((SortedMap<BigDecimal, V>) owner, predicate);
    }

    private static final class Value {
        private final String value;
        private final List<Value> calls;
        Value(String value, List<Value> calls) { this.value = value; this.calls = calls; }
        @Override public boolean equals(Object other) {
            calls.add(this);
            return other instanceof Value that && value.equals(that.value);
        }
        @Override public int hashCode() { return value.hashCode(); }
    }
}
