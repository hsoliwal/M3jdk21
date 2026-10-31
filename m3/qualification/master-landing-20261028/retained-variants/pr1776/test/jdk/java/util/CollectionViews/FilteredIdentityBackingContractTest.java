/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
/*
 * @test
 * @summary Filtered generic Collection retains backing-owner identity and exception semantics
 * @run main FilteredIdentityBackingContractTest
 */
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.CollectionViews;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * A generic IdentityHashMap.values() is a Collection, NOT a Set or List.
 * Its contains/remove use reference identity even when values compare equal.
 */
public final class FilteredIdentityBackingContractTest {
    private static long checks;

    private record Item(int id, boolean selected) {
        @Override public boolean equals(Object other) {
            return other instanceof Item item && id == item.id;
        }
        @Override public int hashCode() { return id; }
    }

    private static void check(boolean ok, String category) {
        checks++;
        if (!ok) throw new AssertionError(category + " check=" + checks);
    }

    private static void identityOwner() {
        Random random = new Random(0x4d3349444f574e4cL);
        for (int trial = 0; trial < 2400; trial++) {
            int id = random.nextInt(100);
            Item hidden = new Item(id, false);
            Item visible = new Item(id, true);
            Item equalButAbsent = new Item(id, true);
            IdentityHashMap<Object, Item> owner = new IdentityHashMap<>();
            owner.put(new Object(), hidden);
            owner.put(new Object(), visible);
            Collection<Item> values = owner.values();
            check(values.contains(hidden), "owner must contain hidden identity");
            check(values.contains(visible), "owner must contain visible identity");
            check(!values.contains(equalButAbsent), "owner must refuse equals-only probe");
            Collection<Item> filtered = CollectionViews.filtered(values, Item::selected);
            check(!filtered.contains(hidden), "hidden exact identity is not visible");
            check(!filtered.remove(hidden), "hidden exact identity must not remove visible equal item");
            check(owner.containsValue(hidden), "hidden owner reference preserved");
            check(owner.containsValue(visible), "visible owner reference preserved");
            check(!filtered.contains(equalButAbsent), "foreign equals probe is absent");
            check(!filtered.remove(equalButAbsent), "foreign equals probe cannot mutate owner");
            check(filtered.contains(visible), "visible exact identity admitted");
            check(filtered.remove(visible), "visible exact identity removed");
            check(!owner.containsValue(visible), "selected owner reference removed");
            check(owner.containsValue(hidden), "hidden owner reference still present");
        }
    }

    private static void ownerExceptionsBeforePredicate() {
        Item only = new Item(1, true);
        IllegalArgumentException exception = new IllegalArgumentException("owner veto");
        Collection<Item> owner = new AbstractCollection<>() {
            @Override public int size() { return 1; }
            @Override public Iterator<Item> iterator() { return List.of(only).iterator(); }
            @Override public boolean contains(Object candidate) {
                if (candidate == null) throw exception;
                if (!(candidate instanceof Item)) throw new ClassCastException("owner type");
                return candidate == only;
            }
        };
        AtomicInteger calls = new AtomicInteger();
        Predicate<Item> predicate = item -> {
            calls.incrementAndGet();
            return item.selected();
        };
        Collection<Item> filtered = CollectionViews.filtered(owner, predicate);
        try {
            filtered.contains(null);
            throw new AssertionError("missing owner veto");
        } catch (IllegalArgumentException expected) {
            check(expected == exception, "exact backing-owner exception identity");
        }
        try {
            filtered.remove(null);
            throw new AssertionError("missing owner remove veto");
        } catch (IllegalArgumentException expected) {
            check(expected == exception, "exact backing-owner removal exception identity");
        }
        try {
            filtered.contains("wrong class");
            throw new AssertionError("missing class-domain refusal");
        } catch (ClassCastException expected) {
            check(Objects.equals(expected.getMessage(), "owner type"), "owner type contract");
        }
        check(calls.get() == 0, "predicate must not run before owner admits probe");
        check(filtered.contains(only), "admitted identity must remain visible");
        check(calls.get() == 1, "one predicate evaluation for admitted probe");
    }

    public static void main(String[] ignored) {
        identityOwner();
        ownerExceptionsBeforePredicate();
        System.out.println("PASS FilteredIdentityBackingContractTest checks=" + checks);
    }
}
