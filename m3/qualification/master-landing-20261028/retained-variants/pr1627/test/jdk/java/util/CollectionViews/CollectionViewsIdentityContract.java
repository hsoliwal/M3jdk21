// Modified 2026 by Hitesh Soliwal and contributors: exercise public receiving owner identity views and wrappers.
/*
 * @test
 * @summary Identity and equals membership retain the authoritative backing owner
 * @run main CollectionViewsIdentityContract
 */
/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
import java.util.CollectionViews;

import java.util.AbstractMap;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/** Behavioral oracle for backing-owned identity and equals membership. */
public final class CollectionViewsIdentityContract {
    private static int checks;
    private CollectionViewsIdentityContract() { }

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

    private static void equalInt(int expected, int actual, String label) {
        checks++;
        if (expected != actual) throw new AssertionError(label + ": expected " + expected + " but got " + actual);
    }

    private static void unsupported(Runnable action, String label) {
        try { action.run(); throw new AssertionError(label + ": mutation was accepted"); }
        catch (UnsupportedOperationException expected) { checks++; }
    }

    private static boolean readOnly(int wrapper) {
        return wrapper == 1 || wrapper == 4 || wrapper == 6;
    }

    private static Set<Key> wrapSet(Set<Key> source, int wrapper) {
        return switch (wrapper) {
            case 0 -> source;
            case 1 -> Collections.unmodifiableSet(source);
            case 2 -> Collections.checkedSet(source, Key.class);
            case 3 -> Collections.synchronizedSet(source);
            case 4 -> Collections.unmodifiableSet(Collections.synchronizedSet(
                    Collections.checkedSet(source, Key.class)));
            case 5 -> Collections.checkedSet(CollectionViews.filtered(source, key -> true), Key.class);
            case 6 -> Collections.unmodifiableSet(Collections.synchronizedSet(Collections.checkedSet(
                    CollectionViews.filtered(source, key -> true), Key.class)));
            default -> throw new AssertionError(wrapper);
        };
    }

    private static Collection<Key> wrapValues(Collection<Key> source, int wrapper) {
        return switch (wrapper) {
            case 0 -> source;
            case 1 -> Collections.unmodifiableCollection(source);
            case 2 -> Collections.checkedCollection(source, Key.class);
            case 3 -> Collections.synchronizedCollection(source);
            case 4 -> Collections.unmodifiableCollection(Collections.synchronizedCollection(
                    Collections.checkedCollection(source, Key.class)));
            case 5 -> Collections.checkedCollection(CollectionViews.filtered(source, key -> true), Key.class);
            case 6 -> Collections.unmodifiableCollection(Collections.synchronizedCollection(Collections.checkedCollection(
                    CollectionViews.filtered(source, key -> true), Key.class)));
            default -> throw new AssertionError(wrapper);
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Set<Map.Entry<Key, Key>> wrapEntries(IdentityHashMap<Key, Key> source, int wrapper) {
        Class<Map.Entry<Key, Key>> entryType = (Class) Map.Entry.class;
        return switch (wrapper) {
            case 0 -> source.entrySet();
            case 1 -> Collections.unmodifiableMap(source).entrySet();
            case 2 -> Collections.checkedMap(source, Key.class, Key.class).entrySet();
            case 3 -> Collections.synchronizedMap(source).entrySet();
            case 4 -> Collections.unmodifiableMap(Collections.checkedMap(
                    Collections.synchronizedMap(source), Key.class, Key.class)).entrySet();
            case 5 -> Collections.checkedSet(CollectionViews.filtered(
                    Collections.checkedMap(source, Key.class, Key.class).entrySet(), entry -> true), entryType);
            case 6 -> Collections.unmodifiableSet(Collections.synchronizedSet(Collections.checkedSet(
                    CollectionViews.filtered(source.entrySet(), entry -> true), entryType)));
            default -> throw new AssertionError(wrapper);
        };
    }

    private static void coherentSet(Set<?> expected, Set<?> actual, String label) {
        check(true, expected.equals(actual), label + " owner-to-view equality");
        check(true, actual.equals(expected), label + " view-to-owner equality");
        equalInt(expected.hashCode(), actual.hashCode(), label + " equal sets have equal hashes");
        Set<Set<?>> directory = new HashSet<>();
        directory.add(expected);
        check(true, directory.contains(actual), label + " hash directory finds equal view");
    }

    private static void identityHashes() {
        for (int ownerKind = 0; ownerKind < 2; ownerKind++) {
            for (int wrapper = 0; wrapper < 7; wrapper++) {
                Key hidden = new Key("same", false);
                Key selected = new Key("same", true);
                Set<Key> owner;
                if (ownerKind == 0) {
                    owner = identitySet(); owner.add(hidden); owner.add(selected); owner.add(null);
                } else {
                    IdentityHashMap<Key, Boolean> map = new IdentityHashMap<>();
                    map.put(hidden, Boolean.TRUE); map.put(selected, Boolean.TRUE); map.put(null, Boolean.TRUE);
                    owner = map.keySet();
                }
                String label = "identity hash owner=" + ownerKind + " wrapper=" + wrapper;
                Set<Key> wrapped = wrapSet(owner, wrapper);
                Set<Key> all = CollectionViews.filtered(wrapped, key -> true);
                coherentSet(owner, all, label + " all");
                Set<Key> selectedOwner = identitySet(); selectedOwner.add(selected); selectedOwner.add(null);
                Set<Key> partial = CollectionViews.filtered(all, key -> key == null || key.visible);
                coherentSet(selectedOwner, partial, label + " partial");
                check(false, partial.contains(hidden), label + " hidden reference stays absent");
                check(false, partial.contains(new Key("same", true)), label + " foreign equals-equal reference stays absent");
                check(false, partial.remove(hidden), label + " hidden removal does not borrow sibling");
                check(true, owner.contains(hidden), label + " hidden reference remains owned");
                if (readOnly(wrapper)) {
                    unsupported(() -> partial.remove(selected), label + " read-only exact removal");
                    check(true, owner.contains(selected), label + " read-only owner preserved");
                } else {
                    check(true, partial.remove(selected), label + " exact removal");
                    check(false, owner.contains(selected), label + " exact selected reference removed");
                }
            }
        }
        Object identityOnly = new Object() {
            @Override public boolean equals(Object other) { throw new AssertionError("identity owner called equals"); }
            @Override public int hashCode() { throw new AssertionError("identity owner called value hashCode"); }
        };
        Set<Object> owner = Collections.newSetFromMap(new IdentityHashMap<>());
        owner.add(identityOnly);
        Set<Object> view = CollectionViews.filtered(owner, value -> true);
        equalInt(System.identityHashCode(identityOnly), view.hashCode(), "identity hash does not call element.hashCode");
        check(false, view.contains(new Object()), "identity membership does not call element.equals");
    }

    private static void identityValues() {
        for (int wrapper = 0; wrapper < 7; wrapper++) {
            Key hidden = new Key("same", false);
            Key selected = new Key("same", true);
            Key foreign = new Key("same", true);
            IdentityHashMap<Object, Key> map = new IdentityHashMap<>();
            map.put(new Object(), hidden); map.put(new Object(), selected);
            map.put(new Object(), selected); map.put(new Object(), null);
            int[] calls = {0};
            Collection<Key> source = wrapValues(map.values(), wrapper);
            Collection<Key> filtered = CollectionViews.filtered(source, key -> {
                calls[0]++; return key == null || key.visible;
            });
            Collection<Key> view = CollectionViews.filtered(filtered, key -> true);
            String label = "identity values wrapper=" + wrapper;
            equalInt(0, calls[0], label + " construction is metadata only");
            check(false, view.contains(foreign), label + " foreign probe absent");
            check(false, view.remove(foreign), label + " foreign removal refused");
            equalInt(0, calls[0], label + " absent identity probes do not invoke predicate");
            check(false, view.contains(hidden), label + " hidden exact probe absent");
            check(false, view.remove(hidden), label + " hidden exact removal refused");
            check(true, map.containsValue(hidden), label + " hidden mapping retained");
            check(true, view.contains(selected), label + " exact selected probe present");
            check(true, view.contains(null), label + " null value present");
            equalInt(3, view.size(), label + " selected duplicate references and null retained");
            if (readOnly(wrapper)) {
                unsupported(() -> view.remove(selected), label + " wrapped iterator/direct remove ownership");
                unsupported(() -> view.remove(null), label + " read-only null removal");
                equalInt(4, map.size(), label + " read-only map remains unchanged");
            } else {
                check(true, view.remove(selected), label + " one selected occurrence removed");
                equalInt(3, map.size(), label + " only one mapping removed");
                check(true, map.containsValue(selected), label + " duplicate selected occurrence remains");
                check(true, view.remove(null), label + " selected null removed");
                equalInt(2, map.size(), label + " null removes only its mapping");
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void checkedEntryRejectsWrongType(Map.Entry<Key, Key> entry, String label) {
        try { ((Map.Entry) entry).setValue("wrong type"); throw new AssertionError(label); }
        catch (ClassCastException expected) { checks++; }
    }

    private static void identityEntries() {
        for (int wrapper = 0; wrapper < 7; wrapper++) {
            Key hiddenKey = new Key("key", false), selectedKey = new Key("key", true);
            Key hiddenValue = new Key("value", false), selectedValue = new Key("value", true);
            IdentityHashMap<Key, Key> map = new IdentityHashMap<>();
            map.put(hiddenKey, hiddenValue); map.put(selectedKey, selectedValue); map.put(null, null);
            Set<Map.Entry<Key, Key>> source = wrapEntries(map, wrapper);
            int[] calls = {0};
            Set<Map.Entry<Key, Key>> first = CollectionViews.filtered(source, entry -> {
                calls[0]++; return entry.getValue() == null || entry.getValue().visible;
            });
            Set<Map.Entry<Key, Key>> view = CollectionViews.filtered(first, entry -> true);
            String label = "identity entries wrapper=" + wrapper;
            equalInt(0, calls[0], label + " construction does not inspect entries");
            Map.Entry<Key, Key> hidden = new AbstractMap.SimpleImmutableEntry<>(hiddenKey, hiddenValue);
            Map.Entry<Key, Key> selected = new AbstractMap.SimpleImmutableEntry<>(selectedKey, selectedValue);
            check(false, view.contains(new AbstractMap.SimpleImmutableEntry<>(new Key("key", true), selectedValue)),
                    label + " foreign equals-equal key absent");
            check(false, view.contains(new AbstractMap.SimpleImmutableEntry<>(selectedKey, new Key("value", true))),
                    label + " foreign equals-equal value absent");
            check(false, view.remove(new AbstractMap.SimpleImmutableEntry<>(new Key("key", true), new Key("value", true))),
                    label + " foreign entry cannot remove mapping");
            check(false, view.contains(null), label + " null is not an entry");
            check(false, view.contains("entry"), label + " non-entry probe absent");
            equalInt(0, calls[0], label + " absent entries do not evaluate visibility");
            check(false, view.contains(hidden), label + " hidden mapping cannot borrow visible equals-equal entry");
            check(false, view.remove(hidden), label + " hidden entry removal refused");
            same(hiddenValue, map.get(hiddenKey), label + " hidden mapping retained");
            same(selectedValue, map.get(selectedKey), label + " visible mapping retained");
            check(true, view.contains(selected), label + " ordinary entry with exact key/value references accepted");
            check(true, view.contains(new AbstractMap.SimpleImmutableEntry<>(null, null)), label + " null mapping accepted");
            IdentityHashMap<Key, Key> expected = new IdentityHashMap<>();
            expected.put(selectedKey, selectedValue); expected.put(null, null);
            coherentSet(expected.entrySet(), view, label + " partial identity entry set");
            coherentSet(map.entrySet(), CollectionViews.filtered(source, entry -> true), label + " all entry set");
            Map.Entry<Key, Key> projected = null;
            for (Map.Entry<Key, Key> entry : view) if (entry.getKey() == selectedKey) projected = entry;
            check(true, projected != null, label + " selected projection exists");
            final Map.Entry<Key, Key> selectedProjection = projected;
            if (readOnly(wrapper)) {
                // A plain unmodifiableSet does not make Map.Entry.setValue read-only.
                if (wrapper == 1 || wrapper == 4) {
                    unsupported(() -> selectedProjection.setValue(hiddenValue), label + " map entry projection remains read-only");
                }
                unsupported(() -> view.remove(selected), label + " read-only entry removal");
                equalInt(3, map.size(), label + " read-only map retained");
            } else {
                if (wrapper == 2 || wrapper == 5) {
                    checkedEntryRejectsWrongType(projected, label + " checked entry type policy retained");
                    same(selectedValue, map.get(selectedKey), label + " rejected type preserves mapping");
                }
                check(true, view.remove(selected), label + " exact identity mapping removed");
                check(false, map.containsKey(selectedKey), label + " selected key removed");
                same(hiddenValue, map.get(hiddenKey), label + " removal preserves hidden equal key");
            }
        }
    }

    private static void bulkOperandMembership() {
        Key hidden = new Key("same", false), selected = new Key("same", true), foreign = new Key("same", true);
        IdentityHashMap<Object, Key> values = new IdentityHashMap<>();
        values.put(new Object(), hidden); values.put(new Object(), selected);
        Collection<Key> valueView = CollectionViews.filtered(values.values(), key -> key.visible);
        check(true, valueView.removeAll(List.of(foreign)), "values removeAll uses ordinary operand.contains");
        equalInt(1, values.size(), "bulk values removal preserves hidden mapping");
        check(true, values.containsValue(hidden), "hidden equal value remains owned");
        values.put(new Object(), selected); values.put(new Object(), new Key("other", true));
        check(true, valueView.retainAll(List.of(foreign)), "values retainAll uses ordinary operand.contains");
        equalInt(2, values.size(), "retention keeps selected equals-equal value and hidden mapping");
        check(true, values.containsValue(selected), "operand membership retains exact selected value");

        Set<Key> keys = identitySet(); keys.add(hidden); keys.add(selected);
        Set<Key> keyView = CollectionViews.filtered(keys, key -> key.visible);
        check(true, keyView.removeAll(List.of(foreign)), "identity set removeAll uses operand membership");
        check(true, keys.contains(hidden), "identity set bulk removal preserves hidden reference");
        check(false, keys.contains(selected), "identity set bulk removes matching visible reference");

        IdentityHashMap<Key, Key> entries = new IdentityHashMap<>();
        entries.put(hidden, hidden); entries.put(selected, selected);
        Set<Map.Entry<Key, Key>> entryView = CollectionViews.filtered(entries.entrySet(), entry -> entry.getKey().visible);
        Map.Entry<Key, Key> foreignEntry = new AbstractMap.SimpleImmutableEntry<>(foreign, foreign);
        check(false, entryView.removeAll(List.of(foreignEntry)),
                "entry operand retains its own contains equality direction");
        int[] operandCalls = {0};
        Collection<Map.Entry<Key, Key>> operand = new AbstractCollection<>() {
            @Override public Iterator<Map.Entry<Key, Key>> iterator() {
                throw new AssertionError("bulk membership must call the operand's contains");
            }
            @Override public int size() { return 1; }
            @Override public boolean contains(Object candidate) {
                operandCalls[0]++; return foreignEntry.equals(candidate);
            }
        };
        check(true, entryView.removeAll(operand), "identity entry removeAll uses operand-owned membership");
        equalInt(1, operandCalls[0], "operand membership sees only the visible stored entry");
        check(true, entries.containsKey(hidden), "bulk entry removal preserves hidden mapping");
        check(false, entries.containsKey(selected), "bulk entry removal removes selected mapping");
    }

    private static void wrappedFilterCallbackStructure() {
        Key selected = new Key("selected", true);
        IdentityHashMap<Object, Key> map = new IdentityHashMap<>();
        map.put(new Object(), selected);
        int[] callbacks = {0, 0};
        Collection<Key> inner = CollectionViews.filtered(map.values(), key -> {
            callbacks[0]++; return true;
        });
        Collection<Key> view = CollectionViews.filtered(Collections.checkedCollection(inner, Key.class), key -> {
            callbacks[1]++; return true;
        });
        equalInt(0, callbacks[0], "metadata inspection does not run inner filter");
        equalInt(0, callbacks[1], "metadata inspection does not run outer filter");
        check(true, view.contains(selected), "wrapped filtered values contain selected reference");
        equalInt(1, callbacks[0], "contains preserves one inner traversal predicate callback");
        equalInt(1, callbacks[1], "contains preserves one outer visibility callback");
    }

    private static void legacy() {
        foreignIdentityProbe();
        exactHiddenIdentityProbe();
        nullIdentityProbe();
        ordinaryEqualsOwners();
        nestedFiltersAndWritableIterators();
        exceptionIdentity();
    }

    public static void main(String[] args) {
        checks = 0;
        String mode = args.length == 0 ? "all" : args[0];
        switch (mode) {
            case "legacy" -> legacy();
            case "hashes" -> identityHashes();
            case "values" -> identityValues();
            case "entries" -> identityEntries();
            case "bulk" -> bulkOperandMembership();
            case "callbacks" -> wrappedFilterCallbackStructure();
            case "all" -> {
                legacy(); identityHashes(); identityValues(); identityEntries(); bulkOperandMembership();
                wrappedFilterCallbackStructure();
            }
            default -> throw new IllegalArgumentException(mode);
        }
        System.out.println("CollectionViewsIdentityContract mode=" + mode + ": " + checks + " checks passed");
    }
}
