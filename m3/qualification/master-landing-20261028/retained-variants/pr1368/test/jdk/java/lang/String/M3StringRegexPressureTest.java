/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Real canonical regex metadata clears under pressure and reconstructs without semantic changes
 * @run main/othervm -Xmx16m --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Xcheck:jni M3StringRegexPressureTest
 * @run main/othervm -Xmx16m --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings -Xcheck:jni M3StringRegexPressureTest
 */

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.regex.Pattern;

public final class M3StringRegexPressureTest {
    private static long checks;
    private static Class<?> dp;
    private static Field caches;

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label + " #" + checks);
    }

    private static Reference<?> root() throws Exception {
        Object root = caches.get(null);
        check(root instanceof SoftReference<?>, "VM-reclaimable metadata root exists");
        return (Reference<?>) root;
    }

    // Independent arbitrary-precision arithmetic checks normalization and overflow behavior.
    private static boolean oracle(long maximum, long total, long free, boolean suspended) {
        if (maximum <= 0) return false;
        BigInteger max = BigInteger.valueOf(maximum);
        BigInteger committed = BigInteger.valueOf(total).min(max).max(BigInteger.ZERO);
        BigInteger unused = BigInteger.valueOf(free).min(committed).max(BigInteger.ZERO);
        BigInteger available = max.subtract(committed.subtract(unused));
        BigInteger reserve = max.divide(BigInteger.valueOf(suspended ? 16 : 32))
                .max(BigInteger.valueOf(suspended ? 1L << 20 : 1L << 19));
        return available.compareTo(reserve) >= 0;
    }

    private static void policy() throws Exception {
        Method policy = dp.getDeclaredMethod("hasHeapHeadroom",
                long.class, long.class, long.class, boolean.class);
        policy.setAccessible(true);
        long[] edges = {Long.MIN_VALUE, -1, 0, 1, (1L << 19) - 1,
                1L << 19, 1L << 20, 1L << 24, Long.MAX_VALUE};
        for (long max : edges) for (long total : edges) for (long free : edges) {
            for (boolean suspended : new boolean[]{false, true}) {
                check((Boolean) policy.invoke(null, max, total, free, suspended)
                        == oracle(max, total, free, suspended), "headroom boundary parity");
            }
        }
        Random random = new Random(339211);
        for (int i = 0; i < 10_000; i++) {
            long max = random.nextLong() & Long.MAX_VALUE;
            long total = random.nextLong();
            long free = random.nextLong();
            boolean suspended = random.nextBoolean();
            check((Boolean) policy.invoke(null, max, total, free, suspended)
                    == oracle(max, total, free, suspended), "headroom randomized parity");
        }
        check((Boolean) policy.invoke(null, 16L << 20, 16L << 20, 800L << 10, false),
                "active lower threshold");
        check(!(Boolean) policy.invoke(null, 16L << 20, 16L << 20, 800L << 10, true),
                "suspended higher threshold");
    }

    private static void bounded() throws Exception {
        Object table = root().get();
        check(table != null, "metadata prepared");
        Field plans = table.getClass().getDeclaredField("plans");
        Field effects = table.getClass().getDeclaredField("effects");
        plans.setAccessible(true);
        effects.setAccessible(true);
        check(((AtomicReferenceArray<?>) plans.get(table)).length() == 64, "bounded plans");
        check(((AtomicReferenceArray<?>) effects.get(table)).length() == 128, "bounded effects");
        Reference.reachabilityFence(table);
    }

    private static void parity(String text, String regex) {
        boolean expected = Pattern.compile(regex).matcher(new StringBuilder(text)).matches();
        check(text.matches(regex) == expected, "JDK Pattern parity");
    }

    private static void reclaim(String text, String regex) throws Exception {
        parity(text, regex);
        bounded();
        Reference<?> reference = root();
        check(!reference.refersTo(null), "live cache before manual clear");
        reference.clear();
        check(reference.refersTo(null), "manual clear");
        parity(text, regex);
        check(!root().refersTo(null), "reconstructed cache");
        reference = root();
        boolean exhausted = false;
        boolean cleared = false;
        byte[][] pressure = new byte[128][]; // 32 MiB exceeds this process's 16 MiB heap.
        try {
            for (int i = 0; i < pressure.length; i++) pressure[i] = new byte[256 << 10];
        } catch (OutOfMemoryError expected) {
            exhausted = true;
            cleared = reference.refersTo(null);
        } finally {
            for (int i = 0; i < pressure.length; i++) pressure[i] = null;
        }
        check(exhausted, "real heap pressure reached");
        check(cleared, "VM reclaimed derived metadata before allocation OOME");
        // Recovery requests are test-only. Production does not force GC or sleep.
        System.gc();
        parity(text, regex);
        check(!root().refersTo(null), "reconstruction after pressure");
        System.out.println("REAL_OWNER_VM_SOFT_RECLAIM_BEFORE_OOME=" + cleared);
    }

    private static void concurrent(String text, String regex) throws Exception {
        try (var pool = Executors.newFixedThreadPool(4)) {
            ArrayList<Future<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < 512; i++) {
                if (i % 31 == 0) root().clear();
                final String subject = switch (i % 4) {
                    case 0 -> text;
                    case 1 -> text.concat("b");
                    case 2 -> text.concat("\n");
                    default -> text.substring(17, 1017);
                };
                tasks.add(pool.submit(() -> subject.matches(regex)
                        == Pattern.compile(regex).matcher(new StringBuilder(subject)).matches()));
            }
            for (Future<Boolean> task : tasks) check(task.get(), "concurrent clear parity");
        }
    }

    public static void main(String[] args) throws Exception {
        String text = new String(new char[]{'a', 'a', 'a', 'a'});
        for (int i = 0; i < 9; i++) text = text.concat(text);
        String regex = new String(new char[]{'a', '*', 'b', '*'});
        Field canonical = String.class.getDeclaredField("m3");
        canonical.setAccessible(true);
        check(canonical.get(text) != null, "actual canonical String owner");
        dp = Class.forName("java.lang.M3StringRegexDp");
        try {
            caches = dp.getDeclaredField("caches");
        } catch (NoSuchFieldException lostPressureCustody) {
            throw new AssertionError("missing pressure-aware soft metadata root", lostPressureCustody);
        }
        caches.setAccessible(true);
        policy();
        reclaim(text, regex);
        concurrent(text, regex);
        for (String suffix : new String[]{"b", "\n", "\u0085", "\ud83d", "\ude00"}) {
            parity(text.concat(suffix), regex);
        }
        System.out.println("M3_STRING_REGEX_PRESSURE_PASS checks=" + checks
                + " concurrency=512 owner_scope=ACTUAL_M3STRING");
    }
}
