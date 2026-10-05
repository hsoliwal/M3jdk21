// SPDX-License-Identifier: Apache-2.0
package com.synexia.common.collections.jdk;

import com.synexia.common.lazy.event.LazyEventBits;
import com.synexia.common.lazy.event.LazyEventGranularity;
import com.synexia.common.lazy.event.LazyEventPolicy;
import com.synexia.common.lazy.event.LazyEventSink;
import com.synexia.common.progress.SystemProgress;
import com.synexia.common.utils.Progress;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Actual Java 21 AVL owner, provider, views, eager oracle and storage checks. No stubs. */
public final class PackedLazyTreeMapSelfTest {
    private PackedLazyTreeMapSelfTest() { }

    public static void main(String[] args) throws Exception {
        PackedLazyValueStateSelfTest.main(args);
        admissionAndNavigation(); sharedViewsAndEntries(); failureAndReentrancy();
        progressAndCancellation(); observability(); canonicalKeys(); eagerAndLazyDifferential(); storage();
        System.out.println(
                "PACKED_LAZY_TREE_OK eager/lazy AVL differential=20000 views/entries/flags/progress/events");
    }

    private static void admissionAndNavigation() {
        int[] calls = {0};
        var map = new PackedTreeMap<Integer, Integer>(null, (key, progress) -> { calls[0]++; return key * 3; });
        equal(null, map.get(7)); equal(0, map.size()); equal(0, calls[0]);
        equal(LazyValueProvider.ABSENT, map.valueFlags(7));
        for (int i = 0; i < 128; i++) { require(map.defer(i), "new key admission"); }
        require(!map.defer(7), "existing key unchanged");
        equal(128, map.size()); equal(0, map.firstKey()); equal(127, map.lastKey());
        equal(6, map.lowerKey(7)); equal(7, map.floorKey(7)); equal(8, map.higherKey(7));
        equal(7, map.ceilingKey(7)); require(map.containsKey(7), "deferred membership");
        equal(128, new ArrayList<>(map.navigableKeySet()).size()); equal(0, calls[0]);
        equal(LazyValueProvider.UNLOADED, map.valueFlags(7));
        Iterator<Integer> keys = map.navigableKeySet().iterator();
        equal(21, map.touch(7)); equal(21, map.get(7)); equal(1, calls[0]);
        require(map.isValueLoaded(7), "loaded flag"); equal(0, keys.next());
        map.verifyInvariants();
        map.clear(); equal(1, calls[0]); map.verifyInvariants();
        map.defer(7); equal(21, map.get(7)); equal(2, calls[0]);

        int[] nullCalls = {0};
        var nulls = new PackedTreeMap<Integer, Integer>(null, (key, progress) -> { nullCalls[0]++; return null; });
        nulls.defer(1); equal(null, nulls.get(1)); equal(null, nulls.getOrDefault(1, 9));
        require(nulls.containsKey(1) && nulls.isValueLoaded(1), "loaded null is present");
        equal(1, nullCalls[0]); equal(9, nulls.getOrDefault(2, 9));
        equal(null, nulls.putIfAbsent(1, 8)); equal(8, nulls.get(1)); equal(1, nullCalls[0]);
        nulls.put(2, null); require(nulls.isValueLoaded(2), "explicit null loaded");
        equal(null, nulls.get(2)); equal(1, nullCalls[0]); nulls.verifyInvariants();
        failure(IllegalStateException.class, () -> new PackedTreeMap<Integer, Integer>().defer(1));
        failure(NullPointerException.class, () -> map.defer(null));
        failure(NullPointerException.class, () -> new PackedTreeMap<Integer, Integer>(null, null));
    }

    private static void sharedViewsAndEntries() {
        int[] calls = {0}; Object identity = new Object();
        var map = new PackedTreeMap<Integer, Object>(null, (key, progress) -> { calls[0]++; return identity; });
        for (int i = 0; i < 12; i++) { map.defer(i); }
        var view = map.subMap(3, true, 8, false).descendingMap();
        equal(List.of(7, 6, 5, 4, 3), new ArrayList<>(view.navigableKeySet()));
        equal(LazyValueProvider.ABSENT, view.valueFlags(8)); equal(null, view.touch(8));
        failure(IllegalArgumentException.class, () -> view.defer(9)); equal(0, calls[0]);
        Map.Entry<Integer, Object> live = view.entrySet().iterator().next();
        equal(7, live.getKey()); equal(0, calls[0]);
        same(identity, live.getValue()); same(identity, map.get(7)); equal(1, calls[0]);
        require(view.isValueLoaded(7), "shared flags");
        Map.Entry<Integer, Object> snapshot = view.floorEntry(6);
        same(identity, snapshot.getValue()); equal(2, calls[0]);
        Object replaced = new Object(); same(identity, view.put(6, replaced));
        same(identity, snapshot.getValue()); same(replaced, map.get(6));
        Map.Entry<Integer, Object> fresh = view.entrySet().iterator().next();
        require(live != fresh, "entries are fresh caller projections");
        Object explicit = new Object(); same(identity, live.setValue(explicit)); same(explicit, map.get(7));
        Progress progress = Progress.of(5); view.preload(progress);
        equal(5, calls[0]); require(progress.isDone(), "bounded preload complete");
        equal(5, progress.getWorkedSoFar()); require(!map.isValueLoaded(2) && !map.isValueLoaded(8), "outside stays lazy");
        view.clear(); equal(5, calls[0]); equal(7, map.size()); map.verifyInvariants();
        for (int i = 3; i < 8; i++) { map.defer(i); }
        require(!map.isValueLoaded(6), "free-slot flags reset");
        same(identity, map.get(6)); equal(6, calls[0]); map.verifyInvariants();
    }

    private static void failureAndReentrancy() {
        RuntimeException original = new IllegalArgumentException("storage failed"); int[] attempts = {0};
        var map = new PackedTreeMap<Integer, Integer>(null, (key, progress) -> {
            if (++attempts[0] == 1) { throw original; } return key * 3;
        });
        map.defer(2); same(original, failure(RuntimeException.class, () -> map.put(2, 9)));
        require(map.containsKey(2), "failed replacement keeps key"); equal(LazyValueProvider.FAILED, map.valueFlags(2));
        equal(6, map.put(2, 9)); equal(9, map.get(2)); equal(2, attempts[0]); map.verifyInvariants();

        AtomicReference<PackedTreeMap<Integer, Integer>> owner = new AtomicReference<>();
        var cycle = new PackedTreeMap<Integer, Integer>(null, (key, progress) -> owner.get().get(3 - key));
        owner.set(cycle); cycle.defer(1); cycle.defer(2);
        failure(IllegalStateException.class, () -> cycle.touch(1));
        equal(LazyValueProvider.FAILED, cycle.valueFlags(1)); equal(LazyValueProvider.FAILED, cycle.valueFlags(2));
        cycle.verifyInvariants(); cycle.clear(); require(cycle.isEmpty(), "cycle guard released");
        var nested = new PackedTreeMap<Integer, Integer>(null,
                (key, progress) -> key == 1 ? owner.get().get(2) + 1 : 20);
        owner.set(nested); nested.defer(1); nested.defer(2); equal(21, nested.touch(1)); nested.verifyInvariants();

        List<Consumer<PackedTreeMap<Integer, Integer>>> mutations = List.of(
                m -> m.put(3, 30), m -> m.put(2, 20), m -> m.remove(2),
                PackedTreeMap::clear, m -> m.descendingMap().clear(),
                m -> m.headMap(3, true).defer(3), m -> m.pollFirstEntry(),
                m -> m.navigableKeySet().pollLast(), m -> m.entrySet().iterator().next().setValue(4));
        for (Consumer<PackedTreeMap<Integer, Integer>> mutation : mutations) {
            var guarded = new PackedTreeMap<Integer, Integer>(null, (key, progress) -> {
                mutation.accept(owner.get()); return 10;
            });
            owner.set(guarded); guarded.defer(1); guarded.put(2, 22);
            failure(IllegalStateException.class, () -> guarded.touch(1));
            equal(List.of(1, 2), new ArrayList<>(guarded.keySet())); equal(22, guarded.get(2));
            equal(LazyValueProvider.FAILED, guarded.valueFlags(1)); guarded.verifyInvariants();
            guarded.clear(); require(guarded.isEmpty(), "provider guard released after failure");
        }
        var removal = new PackedTreeMap<Integer, Integer>(null, (key, progress) -> { throw original; });
        removal.defer(1); same(original, failure(RuntimeException.class, () -> removal.remove(1)));
        require(removal.containsKey(1), "failed removal preserves mapping");
        removal.clear(); require(removal.isEmpty(), "clear does not resolve values");
    }

    private static void progressAndCancellation() {
        int[] calls = {0};
        var map = new PackedTreeMap<Integer, Integer>(null, (key, progress) -> { calls[0]++; return key; });
        for (int i = 0; i < 5; i++) { map.defer(i); }
        Progress partial = Progress.of(5, p -> { if (p.getWorkedSoFar() == 2) { p.cancel(); } });
        failure(CancellationException.class, () -> map.preload(partial));
        equal(2, calls[0]); equal(2, partial.getWorkedSoFar()); require(!partial.isDone(), "cancel not done");
        require(map.isValueLoaded(0) && map.isValueLoaded(1) && !map.isValueLoaded(2), "partial canonical progress");
        map.preload(null); equal(5, calls[0]); map.verifyInvariants();

        var late = new PackedTreeMap<Integer, Integer>(null, (key, progress) -> { progress.cancel(); return 9; });
        late.defer(1); failure(CancellationException.class, () -> late.touch(1, Progress.of(1)));
        equal(LazyValueProvider.FAILED, late.valueFlags(1)); require(!late.isValueLoaded(1), "late cancel not committed");
        equal(9, late.touch(1, Progress.NULL)); late.verifyInvariants();

        var changed = new PackedTreeMap<Integer, Integer>(null, (key, progress) -> key); changed.defer(1);
        Progress interfering = Progress.of(1, p -> { if (p.getWorkedSoFar() == 1) { changed.clear(); } });
        failure(ConcurrentModificationException.class, () -> changed.preload(interfering));
        require(!interfering.isDone(), "interfering traversal not done"); changed.verifyInvariants();
    }

    private static void observability() {
        List<Long> events = new ArrayList<>();
        AtomicReference<Progress> seenProgress = new AtomicReference<>();
        LazyValueProvider<Integer, Integer> provider = new LazyValueProvider<>() {
            @Override public Integer load(Integer key, Progress progress) {
                seenProgress.set(progress);
                return key * 2;
            }
            @Override public LazyEventSink<Progress> lazyEventSink() {
                return (eventBit, slot, oldState, newState, operation, progress, failure) ->
                        events.add(eventBit);
            }
        };
        var map = new PackedTreeMap<Integer, Integer>(null, provider);
        map.defer(1);
        LazyEventPolicy touchPolicy = LazyEventPolicy.of(
                LazyEventBits.BEFORE_TOUCH | LazyEventBits.CACHE_MISS
                        | LazyEventBits.STATE_CHANGED | LazyEventBits.AFTER_LOAD,
                LazyEventGranularity.STATE_TRANSITION);
        long systemBefore = SystemProgress.get().state().worked();
        equal(2, map.touch(1, touchPolicy, null));
        require(SystemProgress.get().state().worked() >= systemBefore + 1L,
                "touch uses shared system progress");
        require(events.contains(LazyEventBits.BEFORE_TOUCH)
                        && events.contains(LazyEventBits.CACHE_MISS)
                        && events.contains(LazyEventBits.STATE_CHANGED)
                        && events.contains(LazyEventBits.AFTER_LOAD),
                "touch policy selects events");

        map.defer(2);
        Progress explicit = Progress.of(1);
        equal(4, map.touch(2, touchPolicy, explicit));
        same(explicit, seenProgress.get());

        map.defer(3); map.defer(4); events.clear();
        LazyEventPolicy rangePolicy = LazyEventPolicy.of(
                LazyEventBits.RANGE_BEGIN | LazyEventBits.RANGE_END | LazyEventBits.PROGRESS,
                LazyEventGranularity.ELEMENT);
        map.preload(rangePolicy, null);
        require(events.contains(LazyEventBits.RANGE_BEGIN)
                        && events.contains(LazyEventBits.RANGE_END)
                        && events.contains(LazyEventBits.PROGRESS),
                "preload range/progress events");
        map.verifyInvariants();
    }

    private static void canonicalKeys() {
        String key = new String("Key"); AtomicReference<String> seen = new AtomicReference<>();
        var map = new PackedTreeMap<String, String>(String.CASE_INSENSITIVE_ORDER,
                (stored, progress) -> { seen.set(stored); return stored; });
        map.defer(key); same(key, map.touch("KEY")); same(key, seen.get());
        require(!map.defer("key"), "comparator-equivalent key not replaced"); map.verifyInvariants();
        var nullable = new PackedTreeMap<String, String>(java.util.Comparator.nullsFirst(String::compareTo),
                (stored, progress) -> stored == null ? "null-key" : stored);
        nullable.defer(null); equal("null-key", nullable.get(null)); nullable.verifyInvariants();
    }

    private static void eagerAndLazyDifferential() {
        for (boolean lazy : List.of(false, true)) {
            var actual = lazy ? new PackedTreeMap<Integer, Integer>(null,
                    (key, progress) -> key % 5 == 0 ? null : key * 3) : new PackedTreeMap<Integer, Integer>();
            var oracle = new TreeMap<Integer, Integer>(); Random random = new Random(20261003);
            for (int i = 0; i < 10000; i++) {
                int key = random.nextInt(200), operation = random.nextInt(10);
                Integer value = i % 7 == 0 ? null : i;
                switch (operation) {
                    case 0 -> {
                        if (lazy) {
                            boolean absent = !oracle.containsKey(key); equal(absent, actual.defer(key));
                            if (absent) { oracle.put(key, key % 5 == 0 ? null : key * 3); }
                        } else { equal(oracle.put(key, value), actual.put(key, value)); }
                    }
                    case 1 -> equal(oracle.put(key, value), actual.put(key, value));
                    case 2 -> equal(oracle.remove(key), actual.remove(key));
                    case 3 -> equal(oracle.get(key), actual.get(key));
                    case 4 -> equal(oracle.putIfAbsent(key, value), actual.putIfAbsent(key, value));
                    case 5 -> equal(oracle.computeIfAbsent(key, k -> k % 3 == 0 ? null : k + 1),
                            actual.computeIfAbsent(key, k -> k % 3 == 0 ? null : k + 1));
                    case 6 -> equal(oracle.floorEntry(key), actual.floorEntry(key));
                    case 7 -> equal(oracle.pollFirstEntry(), actual.pollFirstEntry());
                    case 8 -> {
                        if (i % 41 == 0) { oracle.subMap(20, true, 80, false).clear(); actual.subMap(20, true, 80, false).clear(); }
                        else { equal(oracle.remove(key, value), actual.remove(key, value)); }
                    }
                    default -> equal(oracle.replace(key, value), actual.replace(key, value));
                }
                equal(oracle.size(), actual.size()); actual.verifyInvariants();
                equal(new ArrayList<>(oracle.descendingKeySet()), new ArrayList<>(actual.descendingKeySet()));
                if (i % 83 == 0) {
                    require(actual.equals(oracle) && oracle.equals(actual), "map equality oracle");
                    equal(oracle.hashCode(), actual.hashCode());
                    equal(new ArrayList<>(oracle.subMap(20, true, 80, false).entrySet()),
                            new ArrayList<>(actual.subMap(20, true, 80, false).entrySet()));
                }
            }
            equal(oracle, actual); actual.verifyInvariants();
        }
    }

    private static void storage() throws Exception {
        Object eager = field(new PackedTreeMap<Integer, Integer>(), "tree");
        require(field(eager, "lazy") == null, "eager tree retains no lazy lane owner");
        require(((Object[]) field(eager, "keys")).length == 0, "shared empty payload arrays");
        require(field(eager, "height") instanceof byte[], "byte AVL height preserved");
        var map = new PackedTreeMap<Integer, Integer>(null, (key, progress) -> key);
        Object tree = field(map, "tree"); Object state = field(tree, "lazy");
        require(((short[]) field(state, "flags")).length == 0, "empty lazy flags");
        for (int i = 0; i < 4096; i++) { map.defer(i); }
        int capacity = ((Object[]) field(tree, "keys")).length;
        equal(capacity, ((short[]) field(state, "flags")).length);
        require(field(tree, "left") instanceof int[] && field(tree, "right") instanceof int[]
                && field(tree, "parent") instanceof int[], "columnar integer topology");
        require(Arrays.stream((Object[]) field(tree, "values")).allMatch(Objects::isNull), "no eager payloads");
        same(tree, field(map.subMap(10, true, 30, false).descendingMap(), "tree"));
        map.verifyInvariants(); map.clear(); map.verifyInvariants();
        require(Arrays.stream((Object[]) field(tree, "keys")).allMatch(Objects::isNull), "key references cleared");
        for (short flag : (short[]) field(state, "flags")) { equal(LazyValueProvider.UNLOADED, flag); }
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }
    private static void require(boolean valid, String message) { if (!valid) { throw new AssertionError(message); } }
    private static void equal(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) { throw new AssertionError("expected " + expected + " but got " + actual); }
    }
    private static void same(Object expected, Object actual) { require(expected == actual, "same object identity"); }
    private static <T extends Throwable> T failure(Class<T> type, Runnable action) {
        try { action.run(); }
        catch (Throwable caught) {
            if (type.isInstance(caught)) { return type.cast(caught); }
            throw new AssertionError("unexpected failure", caught);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }
}
