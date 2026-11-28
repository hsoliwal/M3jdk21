/*
 * @test
 * @summary Array-owned expiring object map preserves passive TTL and primitive SlotIndex invariants
 * @modules jdk.m3.collections/jdk.internal.m3.collections:+open
 * @run main ExpiringMapArrayContractTest
 */
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import jdk.internal.m3.collections.ExpiringMapView;

public class ExpiringMapArrayContractTest {
    private static long checks;

    private static void ok(boolean condition, String what) {
        checks++;
        if (!condition) throw new AssertionError(what);
    }

    private static void equal(Object actual, Object expected, String what) {
        ok(Objects.equals(actual, expected), what + ": actual=" + actual + " expected=" + expected);
    }

    private static Class<? extends Throwable> failure(Runnable action) {
        try { action.run(); } catch (Throwable problem) { return problem.getClass(); }
        return null;
    }

    private static final class Entry {
        final Object value;
        final long expiry;
        Entry(Object value, long expiry) { this.value = value; this.expiry = expiry; }
    }

    private static final class Oracle {
        final HashMap<Object, Entry> entries = new HashMap<>();
        final long ttl;
        final AtomicLong clock;
        Oracle(long ttl, AtomicLong clock) { this.ttl = ttl; this.clock = clock; }
        private boolean expired(Entry row) {
            return row != null && row.expiry >= 0 && clock.get() >= row.expiry;
        }
        private long deadline() {
            long now = clock.get();
            return ttl < 0 || (ttl > 0 && now > Long.MAX_VALUE - ttl) ? -1 : now + ttl;
        }
        Object get(Object key) {
            Entry row = entries.get(key);
            if (expired(row)) { entries.remove(key); return null; }
            return row == null ? null : row.value;
        }
        boolean contains(Object key) {
            Entry row = entries.get(key);
            if (expired(row)) { entries.remove(key); return false; }
            return entries.containsKey(key);
        }
        Object put(Object key, Object value) {
            Entry old = entries.get(key);
            if (expired(old)) { entries.remove(key); old = null; }
            entries.put(key, new Entry(value, deadline()));
            return old == null ? null : old.value;
        }
        Object remove(Object key) {
            Entry old = entries.remove(key);
            return old == null ? null : old.value;
        }
        int size() {
            entries.entrySet().removeIf(entry -> expired(entry.getValue()));
            return entries.size();
        }
        boolean isEmpty() { return size() == 0; }
        void clear() { entries.clear(); }
    }

    private static final class CollidingKey {
        final int id;
        CollidingKey(int id) { this.id = id; }
        @Override public int hashCode() { return 11; }
        @Override public boolean equals(Object other) {
            return other instanceof CollidingKey key && key.id == id;
        }
    }

    private static void validate(ExpiringMapView<Object, Object> actual,
                                 Oracle expected, Object[] keys, String label) {
        equal(actual.size(), expected.size(), label + " size after sweep");
        equal(actual.isEmpty(), expected.isEmpty(), label + " empty after sweep");
        for (Object key : keys) {
            equal(actual.containsKey(key), expected.contains(key), label + " contains");
            ok(actual.get(key) == expected.get(key), label + " value identity");
        }
    }

    private static final Field SIZE;
    private static final Field KEYS;
    private static final Field VALUES;
    private static final Field DEADLINES;
    private static final Field HASHES;
    private static final Field INDEX;
    private static final Field MASKS;
    static {
        try {
            Class<?> owner = ExpiringMapView.class;
            SIZE = owner.getDeclaredField("size");
            KEYS = owner.getDeclaredField("keyLane");
            VALUES = owner.getDeclaredField("valueLane");
            DEADLINES = owner.getDeclaredField("deadlineLane");
            HASHES = owner.getDeclaredField("hashLane");
            INDEX = owner.getDeclaredField("hashIndexLane");
            MASKS = owner.getDeclaredField("stateMaskWords");
            for (Field field : new Field[]{SIZE, KEYS, VALUES, DEADLINES, HASHES, INDEX, MASKS})
                field.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static void physicalInvariants(ExpiringMapView<?, ?> map)
            throws ReflectiveOperationException {
        int size = SIZE.getInt(map);
        Object[] keys = (Object[]) KEYS.get(map);
        Object[] values = (Object[]) VALUES.get(map);
        long[] deadlines = (long[]) DEADLINES.get(map);
        int[] hashes = (int[]) HASHES.get(map);
        int[] index = (int[]) INDEX.get(map);
        long[] masks = (long[]) MASKS.get(map);
        ok(size >= 0 && size <= keys.length, "physical size/capacity");
        equal(values.length, keys.length, "parallel value capacity");
        equal(deadlines.length, keys.length, "parallel deadline capacity");
        equal(hashes.length, keys.length, "parallel cached hash capacity");
        boolean[] seen = new boolean[size];
        int indexed = 0;
        for (int encoded : index) {
            if (encoded == 0) continue;
            ok(encoded > 0 && encoded <= size, "index ordinal in bounds");
            ok(!seen[encoded - 1], "duplicate indexed ordinal");
            seen[encoded - 1] = true;
            indexed++;
        }
        equal(indexed, size, "one index entry per row");
        for (int i = 0; i < size; i++) {
            ok(seen[i], "row is indexed");
            int bits = (int) ((masks[i / 21] >>> (3 * (i % 21))) & 7);
            equal(bits, 1, "row occupancy mask");
            equal(hashes[i], keys[i] == null ? 0 : keys[i].hashCode(), "cached hash");
        }
        for (int i = size; i < keys.length; i++) {
            ok(keys[i] == null, "no retained key past logical size");
            ok(values[i] == null, "no retained value past logical size");
            equal(hashes[i], 0, "no stale hash past size");
            equal(deadlines[i], 0L, "no stale deadline past size");
        }
    }

    private static void randomized() throws ReflectiveOperationException {
        Object[] keys = new Object[52];
        keys[0] = null;
        keys[1] = "foo";
        keys[2] = Integer.valueOf(0);
        keys[3] = Long.valueOf(0);
        for (int i = 4; i < keys.length; i++) keys[i] = new CollidingKey(i);
        long[] ttls = {-1, 0, 1, 4, 50};
        for (long ttl : ttls) {
            for (int seed = 0; seed < 24; seed++) {
                Random rnd = new Random(0xcafed001L + seed * 37 + ttl * 191);
                AtomicLong clock = new AtomicLong(50);
                ExpiringMapView<Object, Object> actual = new ExpiringMapView<>(ttl, clock::get);
                Oracle expected = new Oracle(ttl, clock);
                for (int step = 0; step < 850; step++) {
                    if (rnd.nextInt(4) == 0) clock.addAndGet(rnd.nextInt(5));
                    Object key = keys[rnd.nextInt(keys.length)];
                    Object value = rnd.nextInt(11) == 0 ? null : new Object();
                    int operation = rnd.nextInt(9);
                    switch (operation) {
                        case 0, 1 -> ok(actual.put(key, value) == expected.put(key, value), "put previous identity");
                        case 2 -> ok(actual.get(key) == expected.get(key), "get identity");
                        case 3 -> equal(actual.containsKey(key), expected.contains(key), "containsKey");
                        case 4 -> ok(actual.remove(key) == expected.remove(key), "remove previous identity");
                        case 5 -> equal(actual.size(), expected.size(), "size sweep");
                        case 6 -> equal(actual.isEmpty(), expected.isEmpty(), "empty sweep");
                        case 7 -> { actual.clear(); expected.clear(); }
                        default -> ok(actual.get(key) == expected.get(key), "repeat get");
                    }
                    if (step % 17 == 0) validate(actual, expected, keys, "ttl=" + ttl);
                    if (step % 29 == 0) physicalInvariants(actual);
                }
                validate(actual, expected, keys, "final ttl=" + ttl);
                physicalInvariants(actual);
            }
        }
    }

    private static void keyClustering() throws ReflectiveOperationException {
        AtomicLong clock = new AtomicLong(100);
        ExpiringMapView<CollidingKey, Object> map = new ExpiringMapView<>(500L, clock::get);
        Object[] values = new Object[1500];
        for (int i = 0; i < values.length; i++) {
            values[i] = new Object();
            ok(map.put(new CollidingKey(i), values[i]) == null, "insert in collision run");
        }
        physicalInvariants(map);
        for (int i = 0; i < values.length; i += 3) {
            ok(map.remove(new CollidingKey(i)) == values[i], "delete from collision run");
        }
        physicalInvariants(map);
        for (int i = 0; i < values.length; i++) {
            ok(map.get(new CollidingKey(i)) == (i % 3 == 0 ? null : values[i]),
                    "collision-chain lookup after delete/relocate");
        }
        clock.set(600);
        equal(map.size(), 0, "passive expiry across 1000 collision entries");
        physicalInvariants(map);
        equal(map.size(), 0, "repeated empty size");
    }

    private static void clockAndEdgeCases() throws ReflectiveOperationException {
        AtomicLong time = new AtomicLong(10);
        ExpiringMapView<Object, Object> map = new ExpiringMapView<>(5, time::get);
        Object first = new Object(), second = new Object();
        equal(map.put("first", first), null, "put missing");
        time.set(13);
        equal(map.put("first", second), first, "refresh prior before expiry");
        time.set(15);
        ok(map.get("first") == second, "old TTL not used after replacement");
        time.set(18);
        equal(map.get("first"), null, "boundary now >= deadline");
        equal(map.size(), 0, "expired value physically removed");
        map.put("soon", first);
        time.set(100);
        ok(map.remove("soon") == first, "Commons remove returns stale unswept value");
        equal(map.size(), 0, "remove cleared raw entry");
        ExpiringMapView<Object, Object> zero = new ExpiringMapView<>(0, time::get);
        zero.put("zero", first);
        equal(zero.get("zero"), null, "zero TTL immediate read expiry");
        zero.put("zero", first);
        ok(zero.remove("zero") == first, "remove does not pre-expire zero-TTL record");
        ExpiringMapView<Object, Object> infinite = new ExpiringMapView<>();
        infinite.put(null, null);
        infinite.put("a", first);
        equal(infinite.get(null), null, "explicit null value");
        ok(infinite.containsKey(null), "explicit null key presence");
        equal(infinite.size(), 2, "default constructor never expires");
        AtomicLong nearMax = new AtomicLong(Long.MAX_VALUE - 4);
        ExpiringMapView<Object, Object> overflow = new ExpiringMapView<>(100L, nearMax::get);
        overflow.put("permanent", first);
        nearMax.set(Long.MAX_VALUE);
        ok(overflow.get("permanent") == first, "deadline overflow elects never-expire");
        physicalInvariants(overflow);
        final int[] count = {0};
        LongSupplier counted = () -> { count[0]++; return 500L; };
        ExpiringMapView<Object, Object> countedMap = new ExpiringMapView<>(30L, counted);
        countedMap.put("a", first); equal(count[0], 1, "clock sample on put");
        countedMap.get("a"); equal(count[0], 2, "clock sample on get");
        countedMap.containsKey("a"); equal(count[0], 3, "clock sample on contains");
        countedMap.size(); equal(count[0], 4, "clock sample on size");
        countedMap.isEmpty(); equal(count[0], 5, "clock sample on isEmpty");
        countedMap.remove("a"); equal(count[0], 5, "remove does not ask clock");
        countedMap.clear(); equal(count[0], 5, "clear does not ask clock");
    }

    private static void policyDeadlinesAndExceptions() throws ReflectiveOperationException {
        AtomicLong now = new AtomicLong(100L);
        final int[] calls = {0};
        ExpiringMapView.ExpirationPolicy<String, String> policy = (key, value) -> {
            calls[0]++;
            if ("never".equals(key)) return -1L;
            if ("reject".equals(key)) throw new IllegalArgumentException("refused");
            return now.get() + ("short".equals(key) ? 5L : 100L);
        };
        ExpiringMapView<String, String> map = new ExpiringMapView<>(policy, now::get);
        equal(map.put("never", "eternal"), null, "custom never deadline");
        equal(map.put("short", "quick"), null, "custom short deadline");
        equal(map.put("long", "slow"), null, "custom long deadline");
        equal(calls[0], 3, "policy called exactly once per put");
        now.set(105L);
        equal(map.get("short"), null, "custom deadline expires exactly at time");
        equal(map.get("never"), "eternal", "custom negative deadline never expires");
        equal(map.get("long"), "slow", "long deadline still live");
        equal(map.size(), 2, "after targeted custom expiration");
        equal(failure(() -> map.put("reject", "invalid")), IllegalArgumentException.class,
                "policy error propagated");
        ok(!map.containsKey("reject"), "policy failure not inserted");
        equal(map.size(), 2, "policy exception has not changed storage");
        equal(map.put("long", "new slow"), "slow", "refresh custom deadline");
        equal(calls[0], 5, "replacement policy called once");
        now.set(201L);
        equal(map.get("long"), "new slow", "replacement deadline refreshed");
        now.set(206L);
        equal(map.get("long"), null, "replacement expires on refreshed deadline");
        equal(map.size(), 1, "negative-expiry row remains");
        physicalInvariants(map);

        final java.util.concurrent.atomic.AtomicReference<ExpiringMapView<String, String>> holder =
                new java.util.concurrent.atomic.AtomicReference<>();
        ExpiringMapView.ExpirationPolicy<String, String> mutatingPolicy = (key, value) -> {
            if ("outer".equals(key)) holder.get().put("nested", "record");
            return -1;
        };
        holder.set(new ExpiringMapView<>(mutatingPolicy, now::get));
        equal(failure(() -> holder.get().put("outer", "not committed")),
                java.util.ConcurrentModificationException.class,
                "reentrant deadline callback fails closed");
        ok(holder.get().containsKey("nested"), "nested mutation visible");
        ok(!holder.get().containsKey("outer"), "outer insertion refused");
    }

    private static void referenceAndCallbackSafety() {
        ExpiringMapView<Object, Object> map = new ExpiringMapView<>();
        map.put("start", new Object());
        class ReentrantHash {
            @Override public int hashCode() {
                map.put("nested", "record");
                return 7;
            }
        }
        equal(failure(() -> map.put(new ReentrantHash(), "value")),
                java.util.ConcurrentModificationException.class, "reentrant key hash failure");
        ok(map.containsKey("nested"), "reentrant nested mutation stays visible");
        equal(map.size(), 2, "reentrant outer mutation refused");
    }

    private static void ownership() throws ReflectiveOperationException {
        Class<?> owner = ExpiringMapView.class;
        equal(KEYS.getType(), Object[].class, "key lane type");
        equal(VALUES.getType(), Object[].class, "value lane type");
        equal(DEADLINES.getType(), long[].class, "deadline lane type");
        equal(HASHES.getType(), int[].class, "cached hash lane type");
        equal(INDEX.getType(), int[].class, "slot index type");
        equal(MASKS.getType(), long[].class, "state mask type");
        for (Field field : owner.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                ok(!Map.class.isAssignableFrom(field.getType()), "no retained map or entry owner: " + field);
                ok(!java.util.Collection.class.isAssignableFrom(field.getType()), "no collection wrapper: " + field);
            }
        }
        equal(ExpiringMapView.scaffoldDescriptor(),
                "M3ExpiringMapView|EXPIRING_MAP|OBJECT|OCCUPIED,DELETED,EXPIRED|Object[] keyLane;Object[] valueLane;long[] deadlineLane;int[] hashIndexLane",
                "original descriptor preserved");
    }

    public static void main(String[] args) throws Exception {
        ownership();
        clockAndEdgeCases();
        randomized();
        keyClustering();
        policyDeadlinesAndExceptions();
        referenceAndCallbackSafety();
        System.out.println("ExpiringMapArrayContractTest PASS checks=" + checks);
    }
}
