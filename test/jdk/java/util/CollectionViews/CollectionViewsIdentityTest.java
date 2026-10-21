/*
 * @test
 * @summary Actual java.base CollectionViews occurrence and owner mutation contract
 * @run main CollectionViewsIdentityTest
 */
// Modified 2026: execute the existing behavioral oracle against the java.base receiver.
/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
import java.util.CollectionViews;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/** Behavioral oracle for backing-owned identity and equals membership. */
public final class CollectionViewsIdentityTest {
    private static int checks;
    private CollectionViewsIdentityTest() { }

    private static final class Key {
        final String text;
        final boolean visible;
        Key(String text, boolean visible) { this.text = text; this.visible = visible; }
        @Override public boolean equals(Object other) {
            return other instanceof Key key && text.equals(key.text);
        }
        @Override public int hashCode() { return text.hashCode(); }
    }

    private static Set<Key> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private static void check(boolean expected, boolean actual, String label) {
        checks++;
        if (expected != actual) throw new AssertionError(label + ": expected " + expected + " but got " + actual);
    }

    private static void same(Object expected, Object actual, String label) {
        checks++;
        if (expected != actual) throw new AssertionError(label);
    }

    private static void foreignIdentityProbe() {
        Key stored = new Key("same", true);
        Key alias = new Key("same", true);
        Set<Key> owner = identitySet();
        owner.add(stored);
        Set<Key> view = CollectionViews.filtered(owner, key -> key.visible);
        check(false, view.contains(alias), "foreign identity alias is absent");
        check(false, view.remove(alias), "foreign identity alias cannot remove a stored reference");
        check(true, owner.contains(stored), "foreign removal preserves exact stored reference");
        Collection<Key> erased = owner;
        Collection<Key> erasedView = CollectionViews.filtered(erased, key -> key.visible);
        check(false, erasedView.contains(alias), "Collection overload preserves runtime identity Set");
        check(false, erasedView.remove(alias), "Collection overload removal preserves identity membership");
    }

    private static void exactHiddenIdentityProbe() {
        Key hidden = new Key("same", false);
        Key selected = new Key("same", true);
        Set<Key> owner = identitySet();
        owner.add(hidden);
        owner.add(selected);
        Set<Key> view = CollectionViews.filtered(owner, key -> key.visible);
        check(false, view.contains(hidden), "hidden exact reference cannot borrow visible equal sibling");
        check(false, view.remove(hidden), "hidden exact removal cannot delete visible equal sibling");
        check(true, owner.contains(hidden), "hidden exact reference remains owned");
        check(true, owner.contains(selected), "visible equal sibling remains owned");
        check(true, view.contains(selected), "visible exact reference is present");
        check(true, view.remove(selected), "visible exact reference is removable");
        check(true, owner.contains(hidden), "exact visible removal preserves hidden equal sibling");
        check(false, owner.contains(selected), "exact visible reference removed");
    }

    private static void nullIdentityProbe() {
        Set<Key> owner = identitySet();
        Key selected = new Key("selected", true);
        owner.add(null); owner.add(selected);
        Set<Key> nonNull = CollectionViews.filtered(owner, key -> key != null);
        check(false, nonNull.contains(null), "hidden exact null is absent");
        check(false, nonNull.remove(null), "hidden exact null cannot be removed");
        check(true, owner.contains(null), "hidden null stays owned");
        Set<Key> onlyNull = CollectionViews.filtered(owner, key -> key == null);
        check(true, onlyNull.contains(null), "visible null is present");
        check(true, onlyNull.remove(null), "visible null is removed");
        check(false, owner.contains(null), "null removed from identity owner");
        check(true, owner.contains(selected), "null removal preserves other reference");
    }

    private static void ordinaryEqualsOwners() {
        for (Set<Key> owner : List.<Set<Key>>of(new HashSet<>(), new CopyOnWriteArraySet<>())) {
            Key stored = new Key("same", true);
            Key hiddenAlias = new Key("same", false);
            owner.add(stored);
            Set<Key> view = CollectionViews.filtered(owner, key -> key.visible);
            check(true, view.contains(hiddenAlias), "equals owner tests stored representative");
            check(true, view.remove(hiddenAlias), "equals alias removes admitted stored representative");
            check(true, owner.isEmpty(), "equals owner changed through alias");
            owner.add(stored);
            check(true, view.remove(stored), "exact equals reference uses iterator or owner fallback");
            Key hidden = new Key("hidden", false);
            owner.add(hidden);
            check(false, view.contains(new Key("hidden", true)), "visible alias cannot expose hidden stored key");
            check(false, view.remove(new Key("hidden", true)), "visible alias cannot remove hidden stored key");
            check(true, owner.contains(hidden), "hidden equals representative stays owned");
        }
    }

    private static void nestedFiltersAndWritableIterators() {
        Key hidden = new Key("same", false);
        Key selected = new Key("same", true);
        Set<Key> owner = identitySet(); owner.add(hidden); owner.add(selected);
        Set<Key> first = CollectionViews.filtered(owner, key -> key != null);
        Set<Key> nested = CollectionViews.filtered(first, key -> key.visible);
        check(false, nested.contains(hidden), "nested filters retain exact hidden identity");
        check(false, nested.remove(hidden), "nested removal preserves visible sibling");
        check(true, nested.remove(selected), "nested exact visible removal writes through");
        check(true, owner.contains(hidden), "nested removal leaves hidden owner reference");

        // A valid owner may support physical iterator deletion but reject direct removal.
        List<Key> rows = new ArrayList<>(); rows.add(selected);
        Set<Key> iteratorWritable = new AbstractSet<>() {
            @Override public Iterator<Key> iterator() { return rows.iterator(); }
            @Override public int size() { return rows.size(); }
            @Override public boolean remove(Object value) { throw new UnsupportedOperationException("direct remove"); }
        };
        check(true, CollectionViews.filtered(iteratorWritable, key -> true).remove(selected),
                "exact removal preserves writable iterator behavior");
        check(true, rows.isEmpty(), "iterator-writable owner changed");
    }

    private static void exceptionIdentity() {
        Key selected = new Key("same", true);
        RuntimeException predicateFailure = new IllegalStateException("predicate marker");
        Set<Key> owner = identitySet(); owner.add(selected);
        Set<Key> throwing = CollectionViews.filtered(owner, key -> { throw predicateFailure; });
        try { throwing.contains(selected); throw new AssertionError("contains must propagate predicate failure"); }
        catch (RuntimeException actual) { same(predicateFailure, actual, "contains preserves predicate exception identity"); }
        try { throwing.remove(selected); throw new AssertionError("remove must propagate predicate failure"); }
        catch (RuntimeException actual) { same(predicateFailure, actual, "remove preserves predicate exception identity"); }
        check(true, owner.contains(selected), "predicate exception precedes mutation");

        RuntimeException ownerFailure = new IllegalArgumentException("membership marker");
        int[] callbacks = {0};
        Set<Key> failedOwner = new AbstractSet<>() {
            @Override public Iterator<Key> iterator() { return Collections.singleton(selected).iterator(); }
            @Override public int size() { return 1; }
            @Override public boolean contains(Object value) { throw ownerFailure; }
        };
        Set<Key> failedView = CollectionViews.filtered(failedOwner, key -> { callbacks[0]++; return true; });
        try { failedView.contains(selected); throw new AssertionError("owner contains must remain authoritative"); }
        catch (RuntimeException actual) { same(ownerFailure, actual, "contains preserves owner failure identity"); }
        try { failedView.remove(selected); throw new AssertionError("owner membership gates removal"); }
        catch (RuntimeException actual) { same(ownerFailure, actual, "remove preserves owner failure identity"); }
        check(true, callbacks[0] == 0, "owner membership failure precedes predicate callbacks");
    }

    public static void main(String[] args) {
        checks = 0;
        foreignIdentityProbe();
        exactHiddenIdentityProbe();
        nullIdentityProbe();
        ordinaryEqualsOwners();
        nestedFiltersAndWritableIterators();
        exceptionIdentity();
        System.out.println("CollectionViewsIdentityTest: " + checks + " checks passed");
    }
}
