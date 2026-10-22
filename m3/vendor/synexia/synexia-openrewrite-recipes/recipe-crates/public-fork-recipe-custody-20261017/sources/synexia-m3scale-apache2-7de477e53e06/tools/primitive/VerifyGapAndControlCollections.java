/* SPDX-License-Identifier: Apache-2.0 */
import com.synexia.m3scale.primitive.*;
import java.util.*;

public final class VerifyGapAndControlCollections {
    private static long state = 0xd1b54a32d192ed03L;
    private VerifyGapAndControlCollections() { }

    public static void main(String[] args) {
        verifyControlSet();
        verifyControlLongLongMap();
        verifyControlLongByteMap();
        verifyGapSet();
        verifyGapLongLongMap();
        verifyGapLongByteMap();
        verifyBoundedTopBottom();
        verifyFilteredSet();
        System.out.println("gap-control-collections: PASS");
    }

    private static void verifyControlSet() {
        LongControlHashSet actual = new LongControlHashSet(1);
        HashSet<Long> expected = new HashSet<>();
        for (int i = 0; i < 100_000; i++) {
            long key = nextLong();
            if ((i & 3) != 0) {
                check(actual.add(key) == expected.add(key), "control set add");
            } else {
                check(actual.remove(key) == expected.remove(key), "control set remove");
            }
        }
        check(actual.size() == expected.size(), "control set size");
        expected.forEach(v -> check(actual.contains(v), "control set final"));
        check(actual.estimatedPayloadBytes() > 0L, "control set payload");
    }

    private static void verifyControlLongLongMap() {
        LongLongControlHashMap actual = new LongLongControlHashMap(1);
        HashMap<Long, Long> expected = new HashMap<>();
        actual.put(0L, Long.MIN_VALUE);
        expected.put(0L, Long.MIN_VALUE);
        actual.put(-1L, Long.MAX_VALUE);
        expected.put(-1L, Long.MAX_VALUE);

        for (int i = 0; i < 80_000; i++) {
            long key = nextLong();
            long value = nextLong();
            if ((i & 7) != 0) {
                boolean inserted = actual.put(key, value);
                Long old = expected.put(key, value);
                check(inserted == (old == null), "control map put");
            } else {
                check(actual.remove(key) == (expected.remove(key) != null), "control map remove");
            }
        }

        check(actual.size() == expected.size(), "control map size");
        expected.forEach((k, v) -> check(actual.getOrDefault(k, ~v) == v, "control map final"));
        check(actual.getOrDefault(0L, 7L) == Long.MIN_VALUE, "zero key");
        check(actual.getOrDefault(-1L, 7L) == Long.MAX_VALUE, "negative key");
    }

    private static void verifyControlLongByteMap() {
        LongByteControlHashMap actual = new LongByteControlHashMap(1);
        HashMap<Long, Integer> expected = new HashMap<>();
        for (int i = 0; i < 50_000; i++) {
            long key = nextLong();
            int value = i & 0xff;
            actual.putUnsigned(key, value);
            expected.put(key, value);
        }
        expected.forEach((k, v) ->
                check(actual.getUnsignedOrDefault(k, -1) == v, "control byte map"));
    }

    private static void verifyGapSet() {
        LongGapSortedSet actual = new LongGapSortedSet(1);
        TreeSet<Long> expected = new TreeSet<>();

        for (int i = 0; i < 30_000; i++) {
            long value = nextLong() & 0x1ffffL;
            if ((i & 3) != 0) {
                check(actual.add(value) == expected.add(value), "gap set add");
            } else {
                check(actual.remove(value) == expected.remove(value), "gap set remove");
            }
        }

        long[] batch = new long[10_000];
        for (int i = 0; i < batch.length; i++) batch[i] = nextLong() & 0x3ffffL;
        Arrays.sort(batch);
        for (long value : batch) expected.add(value);
        actual.addAllSorted(batch, 0, batch.length);

        check(actual.size() == expected.size(), "gap set size");
        int i = 0;
        for (long value : expected) {
            check(actual.get(i++) == value, "gap set order");
        }
        check(Arrays.equals(actual.toArray(), expected.stream().mapToLong(Long::longValue).toArray()),
                "gap set array");
    }

    private static void verifyGapLongLongMap() {
        LongLongGapSortedMap actual = new LongLongGapSortedMap(1);
        TreeMap<Long, Long> expected = new TreeMap<>();

        for (int i = 0; i < 20_000; i++) {
            long key = nextLong() & 0xffffL;
            long value = nextLong();
            actual.put(key, value);
            expected.put(key, value);
        }

        long[] keys = new long[10_000];
        long[] values = new long[keys.length];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = nextLong() & 0x1ffffL;
            values[i] = nextLong();
        }
        sortPairs(keys, values);
        for (int i = 0; i < keys.length; i++) expected.put(keys[i], values[i]);
        actual.putAllSorted(keys, values, 0, keys.length);

        check(actual.size() == expected.size(), "gap map size");
        int index = 0;
        for (var entry : expected.entrySet()) {
            check(actual.keyAt(index) == entry.getKey(), "gap map key order");
            check(actual.valueAt(index) == entry.getValue(), "gap map value");
            index++;
        }
    }

    private static void verifyGapLongByteMap() {
        LongByteGapSortedMap actual = new LongByteGapSortedMap(1);
        TreeMap<Long, Integer> expected = new TreeMap<>();
        for (int i = 0; i < 20_000; i++) {
            long key = nextLong() & 0xffffL;
            int value = i & 0xff;
            actual.putUnsigned(key, value);
            expected.put(key, value);
        }
        check(actual.size() == expected.size(), "gap byte map size");
        int index = 0;
        for (var entry : expected.entrySet()) {
            check(actual.keyAt(index) == entry.getKey(), "gap byte map key");
            check(actual.valueUnsignedAt(index) == entry.getValue(), "gap byte map value");
            index++;
        }
    }

    private static void verifyBoundedTopBottom() {
        BoundedLongSortedSet highest =
                new BoundedLongSortedSet(100, BoundedLongSortedSet.KEEP_HIGHEST);
        BoundedLongSortedSet lowest =
                new BoundedLongSortedSet(100, BoundedLongSortedSet.KEEP_LOWEST);

        TreeSet<Long> all = new TreeSet<>();
        for (int i = 0; i < 50_000; i++) {
            long value = nextLong() & 0xfffffL;
            all.add(value);
            highest.offer(value);
            lowest.offer(value);
        }

        long[] expectedLow = all.stream().limit(100).mapToLong(Long::longValue).toArray();
        long[] expectedHigh = all.descendingSet().stream().limit(100).mapToLong(Long::longValue)
                .sorted().toArray();

        check(Arrays.equals(lowest.toArray(), expectedLow), "bounded lowest");
        check(Arrays.equals(highest.toArray(), expectedHigh), "bounded highest");
        check(highest.filteredByBoundaryCount() > 0L, "highest prefilter");
        check(lowest.filteredByBoundaryCount() > 0L, "lowest prefilter");
    }

    private static void verifyFilteredSet() {
        FilteredLongGapSortedSet evens =
                new FilteredLongGapSortedSet(value -> (value & 1L) == 0L);
        for (long i = 0; i < 10_000; i++) evens.add(i);
        check(evens.size() == 5_000, "filtered size");
        check(evens.rejectedCount() == 5_000L, "filtered rejects");
        for (int i = 0; i < evens.size(); i++) check((evens.get(i) & 1L) == 0L, "filtered entry");
    }

    private static void sortPairs(long[] keys, long[] values) {
        LongLongGapSortedMap sorter = new LongLongGapSortedMap(keys.length);
        for (int i = 0; i < keys.length; i++) sorter.put(keys[i], values[i]);
        int n = sorter.size();
        long[] uniqueKeys = new long[n];
        long[] uniqueValues = new long[n];
        for (int i = 0; i < n; i++) {
            uniqueKeys[i] = sorter.keyAt(i);
            uniqueValues[i] = sorter.valueAt(i);
        }

        // Expand the unique sorted pairs back into the original arrays.
        // The verifier deliberately passes only the populated prefix.
        System.arraycopy(uniqueKeys, 0, keys, 0, n);
        System.arraycopy(uniqueValues, 0, values, 0, n);
        if (n < keys.length) {
            Arrays.fill(keys, n, keys.length, Long.MAX_VALUE);
            Arrays.fill(values, n, values.length, uniqueValues.length == 0 ? 0L : uniqueValues[n - 1]);
        }
    }

    private static long nextLong() {
        state ^= state >>> 12;
        state ^= state << 25;
        state ^= state >>> 27;
        return state * 0x2545f4914f6cdd1dL;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
