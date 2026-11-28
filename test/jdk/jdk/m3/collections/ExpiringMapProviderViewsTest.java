/*
 * @test
 * @summary Array-owned passive expiring Map backed views and standard Map compatibility
 * @modules jdk.m3.collections/jdk.internal.m3.collections:+open
 * @run main ExpiringMapProviderViewsTest
 */
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import jdk.internal.m3.collections.ExpiringMapView;

public final class ExpiringMapProviderViewsTest {
    private static long checks;

    private static void require(boolean predicate, String reason) {
        checks++;
        if (!predicate) throw new AssertionError(reason);
    }

    private static void equal(Object observed, Object expected, String reason) {
        require(Objects.equals(observed, expected),
                reason + " observed=" + observed + " expected=" + expected);
    }

    private static Class<? extends Throwable> fails(Runnable run) {
        try { run.run(); return null; }
        catch (Throwable error) { return error.getClass(); }
    }

    private static final class Key {
        private final int id;
        Key(int id) { this.id = id; }
        @Override public int hashCode() { return 13; }
        @Override public boolean equals(Object other) {
            return other instanceof Key key && key.id == id;
        }
        @Override public String toString() { return "key:" + id; }
    }

    private static final class Ref {
        final Object value;
        final long deadline;
        Ref(Object value, long deadline) { this.value = value; this.deadline = deadline; }
    }

    private static final class Oracle {
        final Map<Object, Ref> state = new HashMap<>();
        final AtomicLong clock;
        final long ttl;
        Oracle(long ttl, AtomicLong clock) { this.ttl = ttl; this.clock = clock; }

        private long due() {
            long now = clock.get();
            if (ttl < 0 || (ttl > 0 && now > Long.MAX_VALUE - ttl)) return -1;
            return now + ttl;
        }

        private boolean expired(Ref ref) {
            return ref != null && ref.deadline >= 0 && clock.get() >= ref.deadline;
        }

        Object get(Object key) {
            Ref ref = state.get(key);
            if (expired(ref)) { state.remove(key); return null; }
            return ref == null ? null : ref.value;
        }

        boolean containsKey(Object key) {
            Ref ref = state.get(key);
            if (expired(ref)) { state.remove(key); return false; }
            return state.containsKey(key);
        }

        Object put(Object key, Object value) {
            Ref ref = state.get(key);
            if (expired(ref)) { state.remove(key); ref = null; }
            state.put(key, new Ref(value, due()));
            return ref == null ? null : ref.value;
        }

        Object remove(Object key) {
            Ref ref = state.remove(key);
            return ref == null ? null : ref.value;
        }

        void sweep() { state.entrySet().removeIf(e -> expired(e.getValue())); }

        Map<Object,Object> snapshot() {
            sweep();
            Map<Object,Object> result = new HashMap<>();
            for (var e : state.entrySet()) result.put(e.getKey(), e.getValue().value);
            return result;
        }

        void updateValueWithoutChangingTTL(Object key, Object value) {
            Ref old = state.get(key);
            if (old == null) throw new AssertionError("Missing oracle mapping");
            state.put(key, new Ref(value, old.deadline));
        }
    }

    private static void checkMap(ExpiringMapView<Object,Object> map, Oracle oracle) {
        Map<Object,Object> expected = oracle.snapshot();
        equal(map.size(), expected.size(), "size");
        equal(map.isEmpty(), expected.isEmpty(), "isEmpty");
        equal(map, expected, "Map.equals");
        equal(expected, map, "reverse Map.equals");
        equal(map.hashCode(), expected.hashCode(), "Map.hashCode");
        equal(map.keySet(), expected.keySet(), "live keySet");
        require(map.entrySet().equals(expected.entrySet()), "entrySet.equals");
        require(map.entrySet().hashCode() == expected.entrySet().hashCode(), "entrySet hash");
        require(map.values().size() == expected.size(), "values size");
        require(map.toString().startsWith("{") && map.toString().endsWith("}"), "map toString");
        for (var e : expected.entrySet()) {
            require(map.containsKey(e.getKey()), "present mapping");
            require(map.get(e.getKey()) == e.getValue(), "mapping value reference identity");
            require(map.keySet().contains(e.getKey()), "keySet contains");
            require(map.values().contains(e.getValue()), "values contains");
            require(map.entrySet().contains(new AbstractMap.SimpleEntry<>(
                    e.getKey(), e.getValue())), "entrySet contains");
        }
        for (var entry : map.entrySet()) {
            require(expected.containsKey(entry.getKey()), "no ghost entry");
            require(entry.getValue() == expected.get(entry.getKey()), "projected entry live value");
            equal(entry.hashCode(),
                    Objects.hashCode(entry.getKey()) ^ Objects.hashCode(entry.getValue()),
                    "projected entry hash");
        }
    }

    private static void deterministic() {
        AtomicLong clock = new AtomicLong(100);
        ExpiringMapView<Object,Object> map = new ExpiringMapView<>(12, clock::get);
        Map<Object,Object> asMap = map;
        Oracle oracle = new Oracle(12, clock);
        Object a = new Object(), b = new Object();
        require(asMap.put(null, null) == oracle.put(null, null), "null mapping");
        require(asMap.put("a", a) == oracle.put("a", a), "first object");
        require(asMap.put("b", b) == oracle.put("b", b), "second object");
        require(asMap.put(new Key(1), 1) == oracle.put(new Key(1), 1), "hash collision key");
        checkMap(map, oracle);
        require(asMap.keySet().remove(null), "remove present null value by key");
        oracle.containsKey(null); oracle.remove(null);
        checkMap(map, oracle);

        Iterator<Map.Entry<Object,Object>> entries = map.entrySet().iterator();
        require(entries.hasNext(), "entry iterator not empty");
        var entry = entries.next();
        Object prior = entry.getValue();
        Object replacement = new Object();
        require(entry.setValue(replacement) == prior, "entry.setValue old");
        oracle.updateValueWithoutChangingTTL(entry.getKey(), replacement);
        checkMap(map, oracle);

        Iterator<Object> values = map.values().iterator();
        require(values.hasNext(), "values iterator available");
        Object removed = values.next();
        // Determine the key from the current value's unique identity.
        Object removedKey = null;
        for (var e : oracle.snapshot().entrySet()) {
            if (e.getValue() == removed) { removedKey = e.getKey(); break; }
        }
        require(removedKey != null, "unique non-null removed value");
        values.remove();
        oracle.remove(removedKey);
        equal(fails(values::remove), IllegalStateException.class, "iterator double remove");
        checkMap(map, oracle);

        Iterator<Object> keys = map.keySet().iterator();
        require(keys.hasNext(), "keys iterator exists");
        Object toRemove = keys.next();
        keys.remove();
        oracle.remove(toRemove);
        checkMap(map, oracle);

        var liveKeys = map.keySet();
        var liveValues = map.values();
        var liveEntries = map.entrySet();
        Object extra = new Object();
        map.put("new", extra);
        oracle.put("new", extra);
        require(liveKeys.contains("new"), "previous keySet sees new mapping");
        require(liveValues.contains(extra), "previous values sees new mapping");
        require(liveEntries.contains(new AbstractMap.SimpleEntry<>("new", extra)),
                "previous entrySet sees new mapping");

        Iterator<Object> stale = liveKeys.iterator();
        map.put("structural", "x");
        oracle.put("structural", "x");
        equal(fails(stale::next), ConcurrentModificationException.class,
                "iterator structural fail-fast");
        checkMap(map, oracle);

        clock.set(112);
        equal(map.size(), oracle.snapshot().size(), "TTL boundary sweep");
        require(map.keySet().isEmpty(), "expired key projection empty");
        require(map.values().isEmpty(), "expired values projection empty");
        require(map.entrySet().isEmpty(), "expired entry projection empty");
        equal(map, Map.of(), "empty Map.equals");

        ExpiringMapView<Object,Object> zero = new ExpiringMapView<>(0, clock::get);
        zero.put("short", "value");
        require(zero.keySet().isEmpty(), "zero TTL excluded from keySet");
        require(zero.entrySet().isEmpty(), "zero TTL excluded from entrySet");
        require(zero.values().isEmpty(), "zero TTL excluded from values");

        ExpiringMapView<Object,Object> infinite = new ExpiringMapView<>();
        Map<Object,Object> standard = new HashMap<>();
        standard.put(null, null);
        standard.put("A", 1);
        standard.put("B", 2);
        infinite.putAll(standard);
        equal(infinite, standard, "putAll Map parity");
        require(infinite.values().remove(1), "values().remove live mapping");
        standard.remove("A");
        equal(infinite, standard, "values remove result");
        require(infinite.entrySet().remove(new AbstractMap.SimpleEntry<>("B", 2)),
                "entrySet remove");
        standard.remove("B");
        equal(infinite, standard, "entrySet remove result");
        require(infinite.keySet().remove(null), "keySet remove null mapping");
        standard.remove(null);
        equal(infinite, standard, "empty after projected removals");
        require(infinite.entrySet().isEmpty(), "entrySet empty");

        ExpiringMapView<Object,Object> self = new ExpiringMapView<>();
        self.put("one", new Object());
        self.put("two", new Object());
        Map<Object,Object> snapshot = new HashMap<>(self);
        self.putAll(self);
        equal(self, snapshot, "putAll(self) preserves entries and cursor validity");
        self.put("self", self);
        require(self.toString().contains("(this Map)"), "self reference toString");
        self.clear();
    }

    private static void randomized() {
        Object[] keys = new Object[45];
        keys[0] = null;
        keys[1] = "a";
        keys[2] = Integer.valueOf(1);
        for (int i = 3; i < keys.length; i++) keys[i] = new Key(i);
        for (int seed = 0; seed < 32; seed++) {
            Random random = new Random(0x7872aL + seed);
            AtomicLong clock = new AtomicLong(100);
            long ttl = seed % 7 == 0 ? -1 : seed % 7 == 1 ? 0 : 4 + seed % 12;
            ExpiringMapView<Object,Object> actual = new ExpiringMapView<>(ttl, clock::get);
            Oracle oracle = new Oracle(ttl, clock);
            for (int step = 0; step < 320; step++) {
                if (random.nextInt(5) == 0) clock.addAndGet(random.nextInt(5));
                Object key = keys[random.nextInt(keys.length)];
                Object value = random.nextInt(8) == 0 ? null : new Object();
                int op = random.nextInt(9);
                if (op <= 1) {
                    require(actual.put(key, value) == oracle.put(key,value),
                            "random put previous identity");
                } else if (op == 2) {
                    require(actual.get(key) == oracle.get(key), "random get identity");
                } else if (op == 3) {
                    equal(actual.containsKey(key), oracle.containsKey(key), "random contains");
                } else if (op == 4) {
                    require(actual.remove(key) == oracle.remove(key), "random remove identity");
                } else if (op == 5) {
                    Map<Object,Object> update = new HashMap<>();
                    update.put(key, value);
                    actual.putAll(update);
                    oracle.put(key,value);
                } else if (op == 6) {
                    boolean expectedRemoved = oracle.containsKey(key);
                    if (expectedRemoved) oracle.remove(key);
                    equal(actual.keySet().remove(key), expectedRemoved, "random keySet remove");
                } else if (op == 7) {
                    actual.clear(); oracle.state.clear();
                } else {
                    equal(actual.containsValue(value), oracle.snapshot().containsValue(value),
                            "random containsValue");
                }
                if (step % 23 == 0) checkMap(actual, oracle);
            }
            checkMap(actual, oracle);
        }
    }

    private static void ownership() {
        for (Field field : ExpiringMapView.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            require(!Map.class.isAssignableFrom(field.getType()),
                    "no stored secondary Map owner");
            require(!Collection.class.isAssignableFrom(field.getType()),
                    "no stored entry/collection wrapper");
            require(!java.util.Map.Entry.class.isAssignableFrom(field.getType()),
                    "no stored Entry object");
        }
    }

    public static void main(String[] args) {
        deterministic();
        randomized();
        ownership();
        System.out.println("ExpiringMapProviderViewsTest PASS assertions=" + checks);
    }
}
