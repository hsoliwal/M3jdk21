/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary Stable-ID sorted long list preserves dynamic offsets, rank/select and bit-mask contracts
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm SortedIndexedLongListPackedLiveTest
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import jdk.internal.m3.collections.SortedIndexedLongList;

public final class SortedIndexedLongListPackedLiveTest {
    private static long checks;

    public static void main(String[] args) {
        for (int seed = 0; seed < 64; seed++) randomTrace(seed);
        growthPromotionAndTombstones();
        invalidArguments();
        System.out.println("SortedIndexedLongListPackedLiveTest PASS: " + checks + " assertions");
    }

    private static void randomTrace(int seed) {
        Random random = new Random(0x1EAF70A5L + seed * 31337L);
        SortedIndexedLongList actual = new SortedIndexedLongList(seed % 5);
        ArrayList<Entry> reference = new ArrayList<>();
        long expectedVersion = 0;
        for (int step = 0; step < 3600; step++) {
            int action = random.nextInt(1000);
            int live = countLive(reference);
            if (action < 470 && live < 620) {
                long value = randomLong(random);
                long key = randomKey(random);
                int id = actual.add(value, key);
                check(id == reference.size(), "stable append id");
                reference.add(new Entry(value, key));
                expectedVersion++;
            } else if (action < 670 && live > 0) {
                int id = chooseLive(reference, random);
                check(actual.removeId(id), "remove existing");
                Entry entry = reference.get(id);
                entry.live = false;
                expectedVersion++;
                check(!actual.removeId(id), "id cannot be removed twice");
            } else if (action < 800 && live > 0) {
                int id = chooseLive(reference, random);
                long value = randomLong(random);
                actual.setValue(id, value);
                reference.get(id).value = value;
                expectedVersion++;
            } else if (action < 925 && live > 0) {
                int id = chooseLive(reference, random);
                Entry entry = reference.get(id);
                long key = random.nextInt(5) == 0 ? entry.key : randomKey(random);
                actual.setSortKey(id, key);
                if (entry.key != key) expectedVersion++;
                entry.key = key;
            } else if (action < 930 && live > 0) {
                actual.clear();
                for (Entry entry : reference) entry.live = false;
                expectedVersion++;
            } else {
                check(!actual.containsId(-1), "no negative stable ID");
                check(!actual.containsId(reference.size()), "no future stable ID");
            }
            check(actual.version() == expectedVersion, "version");
            check(actual.issuedIds() == reference.size(), "permanent ID inventory");
            check(actual.size() == countLive(reference), "live count");
            check(actual.isEmpty() == (liveAfter(reference) == 0), "isEmpty");
            if (step % 127 == 0) verifySnapshot(actual, reference);
        }
        verifySnapshot(actual, reference);
    }

    private static void growthPromotionAndTombstones() {
        SortedIndexedLongList list = new SortedIndexedLongList(1);
        ArrayList<Entry> reference = new ArrayList<>();
        check(!list.treeIndexed(), "flat initially");
        for (int i = 0; i < 8193; i++) {
            long value = ((long) i << 32) ^ (i * 0x9E3779B9L);
            long key = i % 29 - 14;
            check(list.add(value, key) == i, "growth stable ID");
            reference.add(new Entry(value, key));
            if (i == SortedIndexedLongList.FLAT_LIMIT - 1)
                check(!list.treeIndexed(), "flat threshold exact");
            if (i == SortedIndexedLongList.FLAT_LIMIT)
                check(list.treeIndexed(), "tree promotion threshold");
        }
        check(list.capacity() >= 8193, "capacity widened");
        verifySnapshot(list, reference);
        for (int i = 0; i < 8193; i += 3) {
            check(list.removeId(i), "tombstone");
            reference.get(i).live = false;
        }
        for (int i = 1; i < 8193; i += 7) {
            if (!reference.get(i).live) continue;
            long key = i % 5;
            list.setSortKey(i, key);
            reference.get(i).key = key;
        }
        verifySnapshot(list, reference);
        int issuedBeforeClear = list.issuedIds();
        list.clear();
        for (Entry entry : reference) entry.live = false;
        check(list.issuedIds() == issuedBeforeClear, "clear never reissues IDs");
        check(list.treeIndexed(), "clear retains promoted index mode");
        verifySnapshot(list, reference);
        for (int i = 0; i < 4097; i++) {
            int id = list.add(-i, -(i % 31));
            check(id == reference.size(), "new permanent ID");
            reference.add(new Entry(-i, -(i % 31)));
        }
        verifySnapshot(list, reference);
        check(list.reservedPrimitiveBytes() >= (long) list.capacity() * 2 * Long.BYTES,
                "reserved bytes cover payload lanes");
    }

    private static void invalidArguments() {
        expect(IllegalArgumentException.class, () -> new SortedIndexedLongList(-1));
        SortedIndexedLongList list = new SortedIndexedLongList(0);
        expect(NoSuchElementException.class, list::firstId);
        expect(IndexOutOfBoundsException.class, () -> list.sortedIdAt(0));
        expect(IndexOutOfBoundsException.class, () -> list.valueAtId(0));
        expect(IndexOutOfBoundsException.class, () -> list.rankOfId(-1));
        expect(IndexOutOfBoundsException.class, () -> list.setValue(0, 0));
        expect(IndexOutOfBoundsException.class, () -> list.setSortKey(0, 0));
        int id = list.add(Long.MIN_VALUE, Long.MAX_VALUE);
        check(id == 0, "first ID zero");
        expect(IndexOutOfBoundsException.class, () -> list.fillSortedIds(-1, new int[1], 0, 1));
        expect(IndexOutOfBoundsException.class, () -> list.fillSortedIds(2, new int[1], 0, 1));
        expect(IndexOutOfBoundsException.class, () -> list.fillSortedIds(0, new int[1], 1, 1));
        check(list.removeId(0), "remove first ID");
        expect(IndexOutOfBoundsException.class, () -> list.keyAtId(0));
        expect(IndexOutOfBoundsException.class, () -> list.setValue(0, 5L));
        check(!list.removeId(-1) && !list.removeId(1), "absent id removal false");
    }

    private static void verifySnapshot(SortedIndexedLongList actual, List<Entry> reference) {
        Integer[] sorted = sortedIds(reference);
        check(actual.issuedIds() == reference.size(), "snapshot issued");
        check(actual.size() == sorted.length, "snapshot size");
        check(actual.isEmpty() == (sorted.length == 0), "snapshot empty");
        for (int id = 0; id < reference.size(); id++) {
            Entry entry = reference.get(id);
            check(actual.containsId(id) == entry.live, "snapshot live bit");
            if (entry.live) {
                check(actual.valueAtId(id) == entry.value, "snapshot value");
                check(actual.keyAtId(id) == entry.key, "snapshot sortKey");
            }
        }
        if (sorted.length == 0) {
            expect(NoSuchElementException.class, actual::firstId);
        } else {
            check(actual.firstId() == sorted[0], "first sorted");
        }
        int[] window = new int[sorted.length + 7];
        Arrays.fill(window, -37);
        check(actual.fillSortedIds(0, window, 3, sorted.length) == sorted.length,
                "viewport filled count");
        check(window[0] == -37 && window[1] == -37 && window[2] == -37,
                "viewport prefix untouched");
        check(window[sorted.length + 3] == -37, "viewport suffix untouched");
        for (int ordinal = 0; ordinal < sorted.length; ordinal++) {
            int id = sorted[ordinal];
            check(window[ordinal + 3] == id, "viewport rank");
            check(actual.sortedIdAt(ordinal) == id, "select");
            check(actual.rankOfId(id) == ordinal, "rank");
            check(actual.sortedValueAt(ordinal) == reference.get(id).value,
                    "sorted payload");
        }
        int mid = sorted.length / 2;
        int n = Math.min(7, sorted.length - mid);
        int[] page = new int[n + 2];
        check(actual.fillSortedIds(mid, page, 1, n) == n, "small viewport");
        for (int i = 0; i < n; i++) check(page[i + 1] == sorted[mid + i], "small viewport id");
        check(actual.fillSortedIds(sorted.length, page, 0, page.length) == 0,
                "past final viewport empty");
        long[] probes = {Long.MIN_VALUE, -17, -2, -1, 0, 1, 2, 17, Long.MAX_VALUE};
        for (long key : probes) verifyBounds(actual, sorted, reference, key);
        for (int i = 0; i < Math.min(23, sorted.length); i++) {
            long key = reference.get(sorted[(i * 137) % sorted.length]).key;
            verifyBounds(actual, sorted, reference, key);
        }
    }

    private static void verifyBounds(SortedIndexedLongList actual, Integer[] sorted,
                                     List<Entry> reference, long key) {
        int lo = 0, hi = 0;
        for (int id : sorted) {
            if (reference.get(id).key < key) lo++;
            if (reference.get(id).key <= key) hi++;
        }
        check(actual.lowerBound(key) == lo, "signed lower bound");
        check(actual.upperBound(key) == hi, "signed upper bound");
        check(actual.countOfKey(key) == hi - lo, "key count");
    }

    private static Integer[] sortedIds(List<Entry> entries) {
        ArrayList<Integer> ids = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) if (entries.get(i).live) ids.add(i);
        ids.sort(Comparator.<Integer>comparingLong(i -> entries.get(i).key)
                .thenComparingInt(i -> i));
        return ids.toArray(Integer[]::new);
    }

    private static int countLive(List<Entry> entries) {
        int result = 0;
        for (Entry entry : entries) if (entry.live) result++;
        return result;
    }

    private static int liveAfter(List<Entry> entries) { return countLive(entries); }

    private static int chooseLive(List<Entry> entries, Random random) {
        int ordinal = random.nextInt(countLive(entries));
        for (int id = 0; id < entries.size(); id++) {
            if (!entries.get(id).live) continue;
            if (ordinal-- == 0) return id;
        }
        throw new AssertionError("bad reference live count");
    }

    private static long randomLong(Random random) {
        return switch (random.nextInt(7)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            case 3 -> -1L;
            default -> random.nextLong();
        };
    }

    private static long randomKey(Random random) {
        return switch (random.nextInt(8)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            case 3 -> -1L;
            default -> random.nextInt(101) - 50;
        };
    }

    private static void check(boolean success, String reason) {
        checks++;
        if (!success) throw new AssertionError(reason + " (check " + checks + ")");
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError("expected " + type + " got " + thrown, thrown);
        }
        throw new AssertionError("expected " + type);
    }

    private static final class Entry {
        long value;
        long key;
        boolean live = true;
        Entry(long value, long key) { this.value = value; this.key = key; }
    }
}
