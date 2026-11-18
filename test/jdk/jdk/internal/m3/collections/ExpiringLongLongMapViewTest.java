/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary Passive TTL map uses packed 3-bit slot state and canonical primitive long indexing
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm --add-opens=jdk.m3.collections/jdk.internal.m3.collections=ALL-UNNAMED ExpiringLongLongMapViewTest
 */
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import jdk.internal.m3.collections.ExpiringLongLongMapView;

public final class ExpiringLongLongMapViewTest {
    private record Timed(long value, long deadline) { }

    private static long assertions;

    public static void main(String[] args) throws Exception {
        defaultNeverExpires();
        signedKeysAndZeroValues();
        deterministicTimeBounds();
        compactionAndMaskPacking();
        randomizedDifferential();
        System.out.println("ExpiringLongLongMapViewTest PASS " + assertions + " assertions");
    }

    private static void defaultNeverExpires() throws Exception {
        ExpiringLongLongMapView actual = new ExpiringLongLongMapView();
        check(actual.size() == 0, "default empty");
        check(actual.put(0, 0) == 0, "default put zero value");
        check(actual.containsKey(0), "zero value present");
        check(actual.getOrDefault(0, 17) == 0, "zero vs missing default");
        check(actual.getOrDefault(1, 17) == 17, "missing fallback");
        check(actual.put(0, 3) == 0, "replace zero");
        check(actual.put(Long.MIN_VALUE, Long.MAX_VALUE) == 0, "extreme mapping");
        check(actual.put(Long.MAX_VALUE, Long.MIN_VALUE) == 0, "reverse extremes");
        check(actual.size() == 3, "all immortal entries counted");
        check(actual.remove(0) == 3, "default remove value");
        check(actual.remove(0) == 0, "missing remove default");
        check(actual.size() == 2, "remove cardinality");
        actual.clear();
        check(actual.size() == 0, "clear cardinality");
        check(!actual.containsKey(Long.MAX_VALUE), "clear index membership");
        check(ExpiringLongLongMapView.scaffoldDescriptor().equals(
                "M3ExpiringLongLongMapView|EXPIRING_LONG_MAP|LONG|OCCUPIED,DELETED,EXPIRED|long[] keyLane;long[] valueLane;long[] deadlineLane"),
                "historical scaffold descriptor");
        verifyStorage(actual, new Oracle(-1, () -> 0));

        AtomicLong ticks = new AtomicLong(Long.MAX_VALUE);
        ExpiringLongLongMapView negative = new ExpiringLongLongMapView(-33, ticks::get);
        check(negative.put(15, 6) == 0, "negative TTL insertion");
        check(negative.getOrDefault(15, -1) == 6, "all negative TTL values immortal");
        check(negative.size() == 1, "negative TTL size");
        expect(NullPointerException.class,
                () -> new ExpiringLongLongMapView(5, null), "null clock rejected");
    }

    private static void signedKeysAndZeroValues() throws Exception {
        AtomicLong clock = new AtomicLong(1000);
        ExpiringLongLongMapView actual = new ExpiringLongLongMapView(10, clock::get);
        Oracle expected = new Oracle(10, clock::get);
        long[] keys = {Long.MIN_VALUE, Long.MAX_VALUE, -1, 0, 1, 13, -37};
        for (long key : keys) {
            check(actual.put(key, key == 0 ? 0 : Long.rotateLeft(key, 23)) ==
                    expected.put(key, key == 0 ? 0 : Long.rotateLeft(key, 23)),
                    "extreme put");
        }
        verifyStorage(actual, expected);
        check(actual.put(Long.MIN_VALUE, 71) == expected.put(Long.MIN_VALUE, 71),
                "replacing existing value");
        clock.addAndGet(9);
        check(actual.getOrDefault(0, -3) == expected.getOrDefault(0, -3),
                "unexpired zero-value mapping");
        clock.incrementAndGet();
        check(actual.containsKey(0) == expected.containsKey(0), "zero value expired");
        check(actual.containsKey(Long.MIN_VALUE) == expected.containsKey(Long.MIN_VALUE),
                "replacement refreshed deadline");
        check(actual.remove(-1) == expected.remove(-1), "expired remove returns zero");
        verifyStorage(actual, expected);
        check(actual.put(0, 0) == expected.put(0, 0), "reinsert expired zero");
        check(actual.containsKey(0) == expected.containsKey(0), "reinserted key present");
        verifyStorage(actual, expected);
    }

    private static void deterministicTimeBounds() throws Exception {
        AtomicLong clock = new AtomicLong(1);
        ExpiringLongLongMapView actual = new ExpiringLongLongMapView(0, clock::get);
        Oracle expected = new Oracle(0, clock::get);
        check(actual.put(1, 2) == expected.put(1, 2), "TTL zero insertion");
        check(actual.containsKey(1) == expected.containsKey(1), "TTL zero immediately expired");
        check(actual.size() == expected.size(), "TTL zero sweep");
        check(actual.put(1, 7) == expected.put(1, 7), "TTL zero reinsertion");
        check(actual.getOrDefault(1, -7) == expected.getOrDefault(1, -7),
                "TTL zero default");
        verifyStorage(actual, expected);

        clock.set(Long.MAX_VALUE - 8);
        actual = new ExpiringLongLongMapView(15, clock::get);
        expected = new Oracle(15, clock::get);
        check(actual.put(9, 17) == expected.put(9, 17), "deadline saturates high");
        clock.set(Long.MAX_VALUE - 1);
        check(actual.getOrDefault(9, -3) == expected.getOrDefault(9, -3),
                "saturated deadline not premature");
        clock.set(Long.MAX_VALUE);
        check(actual.getOrDefault(9, -3) == expected.getOrDefault(9, -3),
                "saturated deadline boundary expires");
        verifyStorage(actual, expected);

        clock.set(Long.MIN_VALUE + 4);
        actual = new ExpiringLongLongMapView(17, clock::get);
        expected = new Oracle(17, clock::get);
        check(actual.put(0, 0) == expected.put(0, 0), "negative clock epoch permitted");
        clock.addAndGet(16);
        check(actual.containsKey(0) == expected.containsKey(0),
                "negative epoch just before expiration");
        clock.incrementAndGet();
        check(actual.containsKey(0) == expected.containsKey(0),
                "negative epoch expiry boundary");
        verifyStorage(actual, expected);
    }

    private static void compactionAndMaskPacking() throws Exception {
        AtomicLong clock = new AtomicLong(100);
        ExpiringLongLongMapView actual = new ExpiringLongLongMapView(31, clock::get);
        Oracle expected = new Oracle(31, clock::get);
        for (int i = 0; i < 210; i++) {
            check(actual.put(i, i * 3L) == expected.put(i, i * 3L),
                    "multiword mask insertion");
        }
        verifyStorage(actual, expected);
        clock.set(130);
        for (int i = 0; i < 210; i += 3) {
            check(actual.put(i, i * 5L) == expected.put(i, i * 5L),
                    "replace selected keys refreshes deadlines");
        }
        clock.incrementAndGet();
        check(actual.getOrDefault(1, -1) == expected.getOrDefault(1, -1),
                "targeted expiry after first full mask word");
        check(laneSize(actual) == expected.entries.size() + 0,
                "targeted get removes only one expired row");
        check(actual.size() == expected.size(), "full sweep compacts expired rows");
        verifyStorage(actual, expected);
        for (int i = 0; i < 210; i += 3) {
            check(actual.remove(i) == expected.remove(i), "post-sweep remove");
        }
        verifyStorage(actual, expected);
        for (int i = 0; i < 500; i++) {
            check(actual.put(i * 17L, -i) == expected.put(i * 17L, -i),
                    "tombstone-free index reused after delete");
        }
        verifyStorage(actual, expected);
        actual.clear();
        expected.clear();
        verifyStorage(actual, expected);
    }

    private static void randomizedDifferential() throws Exception {
        long[] ttls = {-1, 0, 1, 4, 29, 101, 1_000_000};
        for (int seed = 0; seed < 42; seed++) {
            Random random = new Random(0x4D33455850495245L ^
                    (seed * 0x9E3779B97F4A7C15L));
            long ttl = ttls[seed % ttls.length];
            AtomicLong clock = new AtomicLong(1_000_000L);
            ExpiringLongLongMapView actual = new ExpiringLongLongMapView(ttl, clock::get);
            Oracle expected = new Oracle(ttl, clock::get);
            for (int step = 0; step < 12_000; step++) {
                if (random.nextInt(9) == 0) clock.addAndGet(random.nextInt(300));
                else clock.addAndGet(random.nextInt(5));
                long key = key(random);
                long value = random.nextLong();
                int op = random.nextInt(100);
                if (op < 42) {
                    check(actual.put(key, value) == expected.put(key, value),
                            "random put prior unexpired value");
                } else if (op < 61) {
                    check(actual.remove(key) == expected.remove(key),
                            "random targeted removal");
                } else if (op < 79) {
                    check(actual.getOrDefault(key, value) ==
                            expected.getOrDefault(key, value), "random targeted get");
                } else if (op < 92) {
                    check(actual.containsKey(key) == expected.containsKey(key),
                            "random targeted contains");
                } else if (op < 99) {
                    check(actual.size() == expected.size(), "random full expiry sweep");
                } else {
                    actual.clear();
                    expected.clear();
                }
                if ((step & 127) == 0) verifyStorage(actual, expected);
            }
            verifyStorage(actual, expected);
        }
    }

    private static long key(Random random) {
        return switch (random.nextInt(9)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0;
            case 3 -> -1;
            case 4 -> 1;
            default -> random.nextInt(200) - 100;
        };
    }

    private static int laneSize(ExpiringLongLongMapView actual) throws Exception {
        return (int) field(actual, "size");
    }

    private static void verifyStorage(ExpiringLongLongMapView actual, Oracle expected)
            throws Exception {
        check(actual.size() == expected.size(), "size purges all expired rows");
        long[] keys = (long[]) field(actual, "keyLane");
        long[] values = (long[]) field(actual, "valueLane");
        long[] deadlines = (long[]) field(actual, "deadlineLane");
        long[] words = (long[]) field(actual, "stateMaskWords");
        int size = laneSize(actual);
        check(keys.length == values.length && values.length == deadlines.length,
                "aligned owner lanes");
        check(words.length == (keys.length + 20L) / 21L, "three packed bits per slot");
        check(size == expected.entries.size(), "physical live row cardinality");
        Set<Long> seen = new HashSet<>();
        for (int row = 0; row < keys.length; row++) {
            int state = (int) ((words[row / 21] >>> ((row % 21) * 3)) & 7L);
            if (row < size) {
                check(state == 1, "all live slots marked OCCUPIED only");
                check(seen.add(keys[row]), "no duplicate indexed keys");
                Timed oracle = expected.entries.get(keys[row]);
                check(oracle != null, "only model keys retained");
                check(oracle.value == values[row], "parallel value lane");
                check(oracle.deadline == deadlines[row], "parallel deadline lane");
                check(actual.containsKey(keys[row]), "reusable indexed key lookup");
                check(actual.getOrDefault(keys[row], -17) == values[row],
                        "reusable indexed value lookup");
            } else {
                check(state == 0, "unused capacity has no state flags");
            }
        }
        check(seen.equals(expected.entries.keySet()), "entire live set reconstructed");
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static void expect(Class<? extends Throwable> type, Action action,
            String label) throws Exception {
        try { action.run(); }
        catch (Throwable failure) {
            check(type.isInstance(failure), label + " exception type " + failure);
            return;
        }
        check(false, label + " missing exception");
    }

    private interface Action { void run() throws Exception; }

    private static void check(boolean condition, String label) {
        assertions++;
        if (!condition) throw new AssertionError(label + " at assertion " + assertions);
    }

    private static final class Oracle {
        private final Map<Long, Timed> entries = new HashMap<>();
        private final long ttl;
        private final LongSupplier clock;

        Oracle(long ttl, LongSupplier clock) {
            this.ttl = ttl;
            this.clock = clock;
        }

        long getOrDefault(long key, long fallback) {
            Timed item = unexpired(key, now());
            return item == null ? fallback : item.value;
        }

        long put(long key, long value) {
            long now = now();
            Timed old = unexpired(key, now);
            long deadline;
            if (ttl < 0) deadline = Long.MAX_VALUE;
            else if (ttl > 0 && now > Long.MAX_VALUE - ttl) deadline = Long.MAX_VALUE;
            else deadline = now + ttl;
            entries.put(key, new Timed(value, deadline));
            return old == null ? 0 : old.value;
        }

        long remove(long key) {
            Timed old = unexpired(key, now());
            entries.remove(key);
            return old == null ? 0 : old.value;
        }

        boolean containsKey(long key) { return unexpired(key, now()) != null; }

        int size() {
            if (ttl >= 0) {
                long now = now();
                entries.entrySet().removeIf(e -> now >= e.getValue().deadline);
            }
            return entries.size();
        }

        void clear() { entries.clear(); }

        private Timed unexpired(long key, long now) {
            Timed old = entries.get(key);
            if (old != null && ttl >= 0 && now >= old.deadline) {
                entries.remove(key);
                return null;
            }
            return old;
        }

        private long now() { return ttl < 0 ? 0 : clock.getAsLong(); }
    }
}
