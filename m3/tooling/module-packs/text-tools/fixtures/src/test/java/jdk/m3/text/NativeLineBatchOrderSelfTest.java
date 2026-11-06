// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from synexia-unix4j-m3 NativeLineBatchOrderSelfTest blob c923e4871ddb9badbf1d2c19bae8f51b73fbe3ea by PROGRESS_MONITOR_TEST_MAP v1.
package jdk.m3.text;

import java.util.concurrent.ProgressMonitor;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;

/** Independent Java object-sort oracle against primitive packed/direct JNI ordering. */
public final class NativeLineBatchOrderSelfTest {
    private static final long SEED = 0x4d334f5244455231L;
    private static int checks;
    private static final String[] UNICODE = {"", "a", "aa", "a\u0000b", "é", "中",
            "😀", "\ue000", "\uffff", "e\u0301", "z", "\n", "\r"};

    private NativeLineBatchOrderSelfTest() { }

    private static void equal(int[] actual, int[] expected) {
        checks++;
        if (!Arrays.equals(actual, expected))
            throw new AssertionError("order mismatch actual=" + Arrays.toString(actual)
                    + " expected=" + Arrays.toString(expected));
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void refuses(Class<? extends RuntimeException> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (RuntimeException error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("wrong refusal", error);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }

    private static int[] oracle(byte[][] rows, int start, int end, boolean reverse, boolean distinct) {
        Integer[] active = new Integer[end - start];
        for (int i = start; i < end; i++) active[i - start] = i;
        Comparator<Integer> comparison = (a, b) -> Arrays.compareUnsigned(rows[a], rows[b]);
        Arrays.sort(active, reverse ? comparison.reversed() : comparison);
        List<Integer> expected = new ArrayList<>();
        Integer previous = null;
        for (int index : active) {
            if (!distinct || previous == null || !Arrays.equals(rows[previous], rows[index]))
                expected.add(index);
            previous = index;
        }
        return expected.stream().mapToInt(Integer::intValue).toArray();
    }

    private static void fixture(byte[][] rows, int first, int end) {
        int[] offsets = new int[rows.length + 1];
        for (int i = 0; i < rows.length; i++) offsets[i + 1] = offsets[i] + rows[i].length;
        byte[] packed = new byte[offsets[rows.length]];
        for (int i = 0; i < rows.length; i++) System.arraycopy(rows[i], 0, packed, offsets[i], rows[i].length);
        NativeLineBatch heap = NativeLineBatch.fromPacked(packed, offsets);
        ByteBuffer direct = ByteBuffer.allocateDirect(packed.length + 11);
        direct.position(5); direct.put(packed); direct.limit(5 + packed.length); direct.position(5);
        NativeLineBatch mapped = NativeLineBatch.fromReadOnlyM3Bytes(direct.asReadOnlyBuffer(), offsets, direct);
        // Heap admission must snapshot both byte content and coordinate lanes.
        Arrays.fill(packed, (byte)0x5a);
        Arrays.fill(offsets, -1);
        heap = heap.tail(rows.length - first).head(end - first);
        mapped = mapped.tail(rows.length - first).head(end - first);
        List<Integer> selection = heap.selectedIndices();
        for (boolean reverse : new boolean[]{false, true}) {
            for (boolean distinct : new boolean[]{false, true}) {
                int[] expected = oracle(rows, first, end, reverse, distinct);
                int[] actual = heap.stableOrder(reverse, distinct, Long.MAX_VALUE);
                equal(actual, expected);
                equal(mapped.stableOrder(reverse, distinct, Long.MAX_VALUE), expected);
                check(heap.selectedIndices().equals(selection), "ordering mutated selection");
                if (actual.length != 0) actual[0] = -1;
                equal(heap.stableOrder(reverse, distinct, Long.MAX_VALUE), expected);
            }
        }
        check(direct.position() == 5 && direct.limit() == 5 + packed.length, "direct cursor changed");
    }

    private static byte[][] utf8(String... rows) {
        return Arrays.stream(rows).map(s -> s.getBytes(StandardCharsets.UTF_8)).toArray(byte[][]::new);
    }

    private static void budgetsAndCancellation() {
        NativeLineBatch two = NativeLineBatch.fromUtf8Lines(List.of("aaa", "aab"));
        equal(two.stableOrder(false, false, 4), new int[]{0, 1});
        refuses(IllegalArgumentException.class, () -> two.stableOrder(false, false, 3));
        refuses(IllegalArgumentException.class, () -> two.stableOrder(false, false, -1));
        equal(NativeLineBatch.fromUtf8Lines(List.of()).stableOrder(false, true, 0), new int[0]);
        equal(NativeLineBatch.fromUtf8Lines(List.of("a")).stableOrder(false, true, 0), new int[]{0});
        NativeLineBatch empty = NativeLineBatch.fromUtf8Lines(List.of("", ""));
        equal(empty.stableOrder(false, false, 1), new int[]{0, 1});
        equal(empty.stableOrder(false, true, 2), new int[]{0});
        refuses(IllegalArgumentException.class, () -> empty.stableOrder(false, true, 1));
        Probe canceled = new Probe(1);
        refuses(CancellationException.class, () -> two.stableOrder(false, false, 100, canceled));
        check(canceled.worked == 0, "canceled call credited progress");
        String longPrefix = "a".repeat(100_000);
        NativeLineBatch longRows = NativeLineBatch.fromUtf8Lines(List.of(longPrefix + "b", longPrefix + "c"));
        Probe duringPrefix = new Probe(4);
        refuses(CancellationException.class,
                () -> longRows.stableOrder(false, false, Long.MAX_VALUE, duringPrefix));
        check(duringPrefix.polls == 4 && duringPrefix.worked == 0,
                "cancellation did not interrupt equal-prefix comparison");
        equal(longRows.stableOrder(false, false, Long.MAX_VALUE), new int[]{0, 1});
        Probe progress = new Probe(Integer.MAX_VALUE);
        equal(two.stableOrder(true, false, 100, progress), new int[]{1, 0});
        check(progress.worked == 2, "row progress not credited");
        check(!progress.done, "caller-owned monitor completed by kernel");
    }

    public static void main(String[] args) {
        checks = 0;
        fixture(new byte[0][], 0, 0);
        fixture(utf8("", "", "a", "a", "😀", "\ue000", "z", "a"), 0, 8);
        fixture(utf8("z", "b", "a", "b", "a", ""), 1, 5);
        fixture(new byte[][]{{(byte)255}, {(byte)128}, {(byte)0}, {(byte)127}, {}, {(byte)255}}, 0, 6);
        Random random = new Random(SEED);
        for (int iteration = 0; iteration < 1500; iteration++) {
            int count = random.nextInt(132);
            byte[][] rows = new byte[count][];
            for (int i = 0; i < count; i++) {
                if ((iteration & 1) == 0) {
                    String value = UNICODE[random.nextInt(UNICODE.length)]
                            + UNICODE[random.nextInt(UNICODE.length)];
                    rows[i] = value.getBytes(StandardCharsets.UTF_8);
                } else {
                    rows[i] = new byte[random.nextInt(32)];
                    random.nextBytes(rows[i]);
                }
                if (i != 0 && random.nextInt(4) == 0) rows[i] = rows[random.nextInt(i)].clone();
            }
            int first = random.nextInt(count + 1), end = first + random.nextInt(count - first + 1);
            fixture(rows, first, end);
        }
        budgetsAndCancellation();
        System.out.println("M3_NATIVE_ORDER_PASS checks=" + checks + " seed=" + Long.toHexString(SEED));
    }

    private static final class Probe implements ProgressMonitor {
        private final int cancelAt;
        private int polls;
        private long worked;
        private boolean canceled;
        private boolean done;
        Probe(int cancelAt) { this.cancelAt = cancelAt; }
        @Override public void begin(String name, long total) { }
        @Override public void done() { done = true; }
        @Override public void worked(long work) { worked += work; }
        @Override public boolean isCancelled() { return canceled || ++polls >= cancelAt; }
        @Override public void cancel() { canceled = true; }
    }
}
