// Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Prepared containment preserves set truth across dense runs and adaptive gallop boundaries
 * @modules java.base/jdk.internal.mindex:+open
 * @run main M3TQAdaptiveContainmentTest
 */
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import jdk.internal.mindex.M3TQ;

public final class M3TQAdaptiveContainmentTest {
    private static final Constructor<M3TQ.Facts> FACTORY;
    private static long checks;
    static {
        try {
            FACTORY = M3TQ.Facts.class.getDeclaredConstructor(int.class, int.class, int.class, long[].class);
            FACTORY.setAccessible(true);
        } catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
    }
    public static void main(String[] args) throws Exception {
        M3TQ.Facts[] subsets = new M3TQ.Facts[256];
        for (int bits = 0; bits < subsets.length; bits++) {
            long[] keys = new long[Integer.bitCount(bits)];
            int at = 0;
            for (int bit = 0; bit < 8; bit++) if ((bits & (1 << bit)) != 0) keys[at++] = bit * 3L;
            subsets[bits] = facts(keys);
        }
        for (int source = 0; source < 256; source++) {
            for (int required = 0; required < 256; required++) {
                check(subsets[source].containsAll(subsets[required]) == ((source & required) == required), "finite set truth");
            }
        }
        long[] source = range(32768, 0, 1);
        pair(source, source.clone(), "dense equal distinct objects");
        pair(source, range(16384, 0, 2), "dense interleaved");
        pair(source, range(1024, 31744, 1), "large skipped prefix at sparse/dense crossover");
        pair(source, range(4096, 0, 8), "unproductive gallop boundary");
        pair(source, range(1024, 31745, 1), "absent high key after long skip");
        pair(range(32768, 1, 2), range(1024, 0, 1), "absent low key");
        long[] clustered = new long[2048];
        for (int group = 0; group < 32; group++) {
            for (int member = 0; member < 64; member++) clustered[group * 64 + member] = group * 1024L + 960 + member;
        }
        pair(source, clustered, "repeated productive gaps");
        clustered[clustered.length - 1] = 32768;
        pair(source, clustered, "failed final gallop");
        for (int length : new int[] {6, 7, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65, 127, 128, 129, 1023}) {
            long[] values = range(length, 0, 2);
            for (long key : new long[] {0, 1, 2L * (length - 1), 2L * length - 1, 0xffffffffffffL}) {
                pair(values, new long[] {key}, "probe boundary");
            }
            int count = Math.max(1, length / 3);
            pair(values, Arrays.copyOfRange(values, length - count, length), "power-boundary dense tail");
        }
        pair(new long[] {0, 1, 0xffffffffL, 0x100000000L, 0xfffffffffffeL, 0xffffffffffffL},
                new long[] {0, 0xffffffffffffL}, "valid 48-bit extrema");
        pair(range(128, 0xffffffffffffL - 127, 1), range(8, 0xffffffffffffL - 7, 1),
                "dense gallop at maximum valid 48-bit key");
        // Synthetic private-constructor domain only: proves signed comparison/index arithmetic,
        // not a claim that public UTF-16 preparation emits out-of-domain keys.
        long[] signed = range(64, Long.MIN_VALUE, 1);
        signed[61] = 0; signed[62] = 0xffffffffffffL; signed[63] = Long.MAX_VALUE;
        pair(signed, new long[] {Long.MIN_VALUE + 59, Long.MIN_VALUE + 60, 0, 0xffffffffffffL, Long.MAX_VALUE},
                "dense signed-long lower bound");
        pair(signed, new long[] {Long.MIN_VALUE + 59, Long.MIN_VALUE + 60, 0, 1, Long.MAX_VALUE},
                "dense signed-long missing interior");

        Random random = new Random(0x54494d534f52544cL);
        for (int trial = 0; trial < 6000; trial++) {
            int size = trial % 23 == 0 ? 4096 : random.nextInt(257);
            long[] values = new long[size];
            for (int i = 0; i < size; i++) values[i] = random.nextInt(65536);
            values = Arrays.stream(values).distinct().sorted().toArray();
            int count = trial % 3 == 0 ? random.nextInt(4) : random.nextInt(257);
            long[] required = new long[count];
            for (int i = 0; i < count; i++) {
                required[i] = values.length != 0 && (trial % 2 == 0 || random.nextBoolean())
                        ? values[random.nextInt(values.length)] : random.nextInt(65536);
            }
            required = Arrays.stream(required).distinct().sorted().toArray();
            pair(values, required, "seeded spectrum");
        }
        M3TQ.Facts empty = facts(new long[0]);
        check(empty.containsAll(empty), "empty identity");
        try { empty.containsAll(null); throw new AssertionError("null accepted"); }
        catch (NullPointerException expected) { check("required".equals(expected.getMessage()), "null exception order/message"); }

        Fence text = new Fence("abc-bcd-\u0000\uD800x\uDC00-\uFFFF".repeat(100));
        M3TQ.Facts prepared = M3TQ.precompute(text, text.length());
        check(text.reads == text.length(), "single UTF-16 preparation scan");
        long[] poisoned = prepared.keys();
        Arrays.fill(poisoned, Long.MIN_VALUE);
        text.closed = true;
        M3TQ.Facts required = M3TQ.precompute("abcd", 4);
        for (int repeat = 0; repeat < 1000; repeat++) check(prepared.containsAll(required), "necessary-condition false positive retained");
        check(text.reads == text.length(), "zero warm payload reads");
        check(!"abc-bcd".contains("abcd"), "facts do not prove exact substring");
        System.out.println("TQ_ADAPTIVE_CONTAINMENT_PASS checks=" + checks + " warmPayloadReads=0");
    }
    private static void pair(long[] source, long[] required, String label) throws Exception {
        Set<Long> truth = new HashSet<>();
        for (long value : source) truth.add(value);
        boolean expected = true;
        for (long value : required) expected &= truth.contains(value);
        M3TQ.Facts a = facts(source), b = facts(required);
        check(a.containsAll(b) == expected, label);
        check(a.containsAll(a), label + " identity");
        check(Arrays.equals(source, a.keys()), label + " unchanged source keys");
        check(Arrays.equals(required, b.keys()), label + " unchanged required keys");
    }
    private static M3TQ.Facts facts(long[] keys) throws Exception {
        return FACTORY.newInstance(keys.length + 2, 0, 0, keys.clone());
    }
    private static long[] range(int count, long first, long stride) {
        long[] values = new long[count];
        for (int i = 0; i < count; i++) values[i] = first + i * stride;
        return values;
    }
    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
    private static final class Fence implements CharSequence {
        private final String text;
        private int reads;
        private boolean closed;
        Fence(String text) { this.text = text; }
        public int length() { return text.length(); }
        public char charAt(int at) {
            if (closed) throw new AssertionError("warm payload read");
            reads++;
            return text.charAt(at);
        }
        public CharSequence subSequence(int from, int to) { throw new AssertionError("slice materialization"); }
    }
}
