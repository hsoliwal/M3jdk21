/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Monitored collection operations preserve wrapper capabilities, callback order and owner locks
 * @run main/othervm MonitoredWrapperContracts behavior
 * @run main/othervm MonitoredWrapperContracts owner
 * @run main/othervm MonitoredWrapperContracts mutex
 */

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ProgressMonitor;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MonitoredWrapperContracts {
    private static void equal(Object actual, Object expected) {
        if (!expected.equals(actual)) throw new AssertionError("expected <" + expected + ">, got <" + actual + ">");
    }

    private static <T extends Throwable> T fails(Class<T> type, Runnable call) {
        try {
            call.run();
        } catch (Throwable ex) {
            if (type.isInstance(ex)) return type.cast(ex);
            throw new AssertionError("expected " + type.getName(), ex);
        }
        throw new AssertionError("missing " + type.getName());
    }

    // C = cancellation boundary; W = one successfully completed operation.
    private static final class Trace implements ProgressMonitor {
        final StringBuilder events = new StringBuilder();
        final CancellationException stop = new CancellationException("stop");
        final Object mutex;
        final int stopAt;
        int checks;
        Trace() { this(null, 0); }
        Trace(Object mutex, int stopAt) { this.mutex = mutex; this.stopAt = stopAt; }
        void event(String name) {
            if (mutex != null && !Thread.holdsLock(mutex)) throw new AssertionError("owner lock absent at " + name);
            events.append(name);
        }
        public void checkCancelled() {
            event("C");
            if (++checks == stopAt) throw stop;
        }
        public void worked(long units) { equal(units, 1L); event("W"); }
        public void begin(String name, long total) { throw new AssertionError("borrowed begin"); }
        public void done() { throw new AssertionError("borrowed done"); }
        public void cancel() { throw new AssertionError("borrowed cancel"); }
        void expect(String expected) { equal(events.toString(), expected); }
    }

    private static void readonly() {
        var backing = new ArrayList<>(List.of("a", "b"));
        Collection<String> c = Collections.unmodifiableCollection(backing);
        Trace visit = new Trace();
        c.forEachMonitored(visit::event, visit);
        visit.expect("CaWCbWCC");
        Trace missing = new Trace();
        equal(c.containsAllMonitored(List.of("b", "missing", "ignored"), missing), false);
        missing.expect("CWCWC");
        Trace empty = new Trace();
        equal(c.addAllMonitored(List.of(), empty), false);
        empty.expect("C");
        Trace add = new Trace();
        fails(UnsupportedOperationException.class, () -> c.addAllMonitored(List.of("x"), add));
        add.expect("C");
        Trace remove = new Trace();
        fails(UnsupportedOperationException.class, () -> c.removeIfMonitored(v -> {
            throw new AssertionError("predicate on unmodifiable receiver");
        }, remove));
        remove.expect("C");
        Trace cancelled = new Trace(null, 1);
        equal(fails(CancellationException.class, () -> c.removeIfMonitored(v -> false, cancelled)), cancelled.stop);
        Trace invalid = new Trace(null, 1);
        fails(NullPointerException.class, () -> c.removeIfMonitored(null, invalid));
        fails(NullPointerException.class, () -> c.addAllMonitored(null, invalid));
        fails(NullPointerException.class, () -> c.containsAllMonitored(null, invalid));
        fails(NullPointerException.class, () -> c.forEachMonitored(null, invalid));
        invalid.expect("");
        IllegalArgumentException signal = new IllegalArgumentException();
        Trace callback = new Trace();
        equal(fails(IllegalArgumentException.class, () -> c.forEachMonitored(v -> {
            callback.event(v);
            if (v.equals("b")) throw signal;
        }, callback)), signal);
        callback.expect("CaWCb");
        equal(backing, List.of("a", "b"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"}) // Deliberate erased call into the checked wrapper.
    private static void checkedAndLifo() {
        var backing = new ArrayList<String>();
        Collection c = Collections.checkedCollection(backing, String.class);
        Trace insert = new Trace();
        fails(ClassCastException.class, () -> c.addAllMonitored(List.of("a", 2, "b"), insert));
        insert.expect("CWC");
        equal(backing, List.of("a"));
        Trace stop = new Trace(null, 2);
        equal(fails(CancellationException.class, () -> c.addAllMonitored(List.of("b", "c"), stop)), stop.stop);
        stop.expect("CWC");
        equal(backing, List.of("a", "b"));

        Collection<String> checked = Collections.checkedCollection(backing, String.class);
        Trace remove = new Trace();
        equal(checked.removeIfMonitored(v -> { remove.event(v); return v.equals("a"); }, remove), true);
        remove.expect("CCaWCbWC");
        equal(backing, List.of("b"));
        var set = Collections.checkedSet(new LinkedHashSet<>(List.of("a")), String.class);
        Trace duplicate = new Trace();
        equal(set.addAllMonitored(List.of("a"), duplicate), false);
        duplicate.expect("CWC"); // A successful add call earns credit even when the set doesn't grow.
        var deque = new ArrayDeque<>(List.of("z"));
        var lifo = Collections.asLifoQueue(deque);
        Trace push = new Trace();
        equal(lifo.addAllMonitored(List.of("a", "b"), push), true);
        push.expect("CWCWC");
        equal(List.copyOf(deque), List.of("b", "a", "z"));
        Trace walk = new Trace();
        lifo.forEachMonitored(walk::event, walk);
        walk.expect("CbWCaWCzWCC");
    }

    // Use the interface default views and their reversals. The backing key set
    // rejects insertion and records exactly which method and exception reached its caller.
    private static final class TestMap extends AbstractMap<Integer, String> implements SequencedMap<Integer, String> {
        private final SequencedMap<Integer, String> data;
        final List<String> calls = new ArrayList<>();
        final UnsupportedOperationException noAdd = new UnsupportedOperationException("backing key set");
        int plainAdds;
        TestMap(SequencedMap<Integer, String> data) { this.data = data; }
        public Set<Entry<Integer, String>> entrySet() { return data.entrySet(); }
        public SequencedMap<Integer, String> reversed() { return new TestMap(data.reversed()); }
        public Set<Integer> keySet() {
            return new AbstractSet<>() {
                public int size() { return data.size(); }
                public Iterator<Integer> iterator() { return data.keySet().iterator(); }
                public boolean add(Integer k) { plainAdds++; throw noAdd; }
                public boolean addAll(Collection<? extends Integer> keys) { plainAdds++; throw noAdd; }
                public boolean containsAllMonitored(Collection<?> keys, ProgressMonitor monitor) {
                    calls.add("contains");
                    return super.containsAllMonitored(keys, monitor);
                }
                public boolean removeIfMonitored(Predicate<? super Integer> remove, ProgressMonitor monitor) {
                    calls.add("remove");
                    return super.removeIfMonitored(remove, monitor);
                }
                public void forEachMonitored(Consumer<? super Integer> action, ProgressMonitor monitor) {
                    calls.add("visit");
                    super.forEachMonitored(action, monitor);
                }
                public boolean addAllMonitored(Collection<? extends Integer> keys, ProgressMonitor monitor) {
                    calls.add("add");
                    return super.addAllMonitored(keys, monitor);
                }
            };
        }
    }

    private static <T> void view(Collection<T> c, T first, T second, T absent) {
        Trace visit = new Trace();
        var seen = new ArrayList<T>();
        c.forEachMonitored(v -> { seen.add(v); visit.event("V"); }, visit);
        equal(seen, List.of(first, second));
        visit.expect("CVWCVWCC");
        Trace search = new Trace();
        equal(c.containsAllMonitored(List.of(second, absent, first), search), false);
        search.expect("CWCWC");
        Trace remove = new Trace();
        equal(c.removeIfMonitored(v -> { remove.event("P"); return false; }, remove), false);
        remove.expect("CCPWCPWC");
        Trace empty = new Trace();
        equal(c.addAllMonitored(List.of(), empty), false);
        empty.expect("C");
        Trace add = new Trace();
        fails(UnsupportedOperationException.class, () -> c.addAllMonitored(List.of(absent), add));
        add.expect("C");
        Trace stop = new Trace(null, 1);
        equal(fails(CancellationException.class, () -> c.addAllMonitored(List.of(absent), stop)), stop.stop);
        stop.expect("C");
    }

    private static void sequenced() {
        var seed = new LinkedHashMap<Integer, String>();
        seed.put(1, "a");
        seed.put(2, "b");
        var custom = new TestMap(seed);
        var a = Map.entry(1, "a");
        var b = Map.entry(2, "b");
        var z = Map.entry(9, "z");
        view(custom.sequencedKeySet(), 1, 2, 9);
        view(custom.sequencedValues().reversed(), "b", "a", "z");
        view(custom.reversed().sequencedEntrySet(), b, a, z);
        equal(seed, Map.of(1, "a", 2, "b"));
        Trace remove = new Trace();
        equal(custom.reversed().sequencedValues().removeIfMonitored("a"::equals, remove), true);
        equal(seed, Map.of(2, "b"));
        seed.putFirst(1, "a");

        var unmodifiable = Collections.unmodifiableSequencedMap(seed);
        for (Collection<Map.Entry<Integer, String>> entries : List.of(unmodifiable.entrySet(),
                unmodifiable.sequencedEntrySet(), unmodifiable.sequencedEntrySet().reversed(),
                unmodifiable.reversed().sequencedEntrySet())) {
            Trace credit = new Trace();
            entries.forEachMonitored(e -> fails(UnsupportedOperationException.class, () -> e.setValue("x")), credit);
            credit.expect("CWCWCC");
        }
        equal(seed, Map.of(1, "a", 2, "b"));
    }

    private static void ownerDispatch() {
        var custom = new TestMap(new LinkedHashMap<>(Map.of(1, "a")));
        var keys = custom.sequencedKeySet();
        equal(keys.containsAllMonitored(List.of(1), new Trace()), true);
        keys.forEachMonitored(v -> equal(v, 1), new Trace());
        equal(keys.removeIfMonitored(v -> false, new Trace()), false);
        // Only ordinary add/addAll fail locally. All four monitored operations
        // dispatch to the corresponding backing method, even on failure.
        fails(UnsupportedOperationException.class, () -> keys.add(2));
        fails(UnsupportedOperationException.class, () -> keys.addAll(List.of()));
        equal(custom.plainAdds, 0);
        Trace refusal = new Trace();
        equal(fails(UnsupportedOperationException.class, () -> keys.addAllMonitored(List.of(2), refusal)), custom.noAdd);
        refusal.expect("C");
        equal(custom.plainAdds, 1);
        Trace empty = new Trace();
        equal(keys.addAllMonitored(List.of(), empty), false);
        empty.expect("C");
        Trace stopped = new Trace(null, 1);
        equal(fails(CancellationException.class, () -> keys.addAllMonitored(List.of(2), stopped)), stopped.stop);
        stopped.expect("C");
        Trace invalid = new Trace();
        fails(NullPointerException.class, () -> keys.addAllMonitored(null, invalid));
        invalid.expect("");
        equal(custom.plainAdds, 1);
        equal(custom.calls, List.of("contains", "visit", "remove", "add", "add", "add", "add"));
        equal(List.copyOf(keys), List.of(1));
    }

    private static <T> Collection<T> input(T member, Object lock) {
        return new AbstractCollection<>() {
            public int size() { return 1; }
            public Iterator<T> iterator() {
                if (!Thread.holdsLock(lock)) throw new AssertionError("owner lock absent at input.iterator");
                return List.of(member).iterator();
            }
        };
    }

    private static <T> void mutex(Object lock, Collection<T> view, T member, boolean canAdd) {
        Trace visit = new Trace(lock, 0);
        view.forEachMonitored(v -> {
            visit.event("V");
            synchronized (lock) { equal(view.contains(v), true); }
        }, visit);
        visit.expect("CVWCC");
        Trace search = new Trace(lock, 0);
        equal(view.containsAllMonitored(input(member, lock), search), true);
        search.expect("CWC");
        Trace removal = new Trace(lock, 0);
        equal(view.removeIfMonitored(v -> { removal.event("P"); return false; }, removal), false);
        removal.expect("CCPWC");
        Trace add = new Trace(lock, 0);
        if (canAdd) {
            equal(view.addAllMonitored(input(member, lock), add), true);
            add.expect("CWC");
        } else {
            fails(UnsupportedOperationException.class, () -> view.addAllMonitored(input(member, lock), add));
            add.expect("C");
        }
        RuntimeException signal = new RuntimeException();
        equal(fails(RuntimeException.class, () -> view.forEachMonitored(v -> { throw signal; }, new Trace(lock, 0))), signal);
        equal(Thread.holdsLock(lock), false);
    }

    private static void synchronizedOwners() {
        var c = Collections.synchronizedCollection(new ArrayList<>(List.of("a")));
        mutex(c, c, "a", true);
        var map = Collections.synchronizedMap(new LinkedHashMap<>(Map.of(1, "a")));
        mutex(map, map.keySet(), 1, false);
        mutex(map, map.values(), "a", false);
        mutex(map, map.entrySet(), Map.entry(1, "a"), false);
    }

    public static void main(String... args) {
        switch (args[0]) {
            case "behavior" -> { readonly(); checkedAndLifo(); sequenced(); }
            case "owner" -> ownerDispatch();
            case "mutex" -> synchronizedOwners();
            default -> throw new AssertionError(args[0]);
        }
        System.out.println("PASS " + args[0]);
    }
}
