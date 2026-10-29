<<<<<<< HEAD
/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Optional regex metadata obeys heap hysteresis, soft reclamation and canonical owner lifetime
 * @requires vm.gc.Serial
 * @run main/othervm -Xmx128m -Xcheck:jni --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringRegexPressureTest
 * @run main/othervm -Xmx128m -Xcheck:jni --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringRegexPressureTest
 * @run main/othervm -Xmx32m -XX:+UseSerialGC -XX:SoftRefLRUPolicyMSPerMB=0 -Xcheck:jni --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringRegexPressureTest pressure
 * @run main/othervm -Xmx32m -XX:+UseSerialGC -XX:SoftRefLRUPolicyMSPerMB=0 -Xcheck:jni --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringRegexPressureTest pressure
 */

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.regex.Pattern;

/**
 * The reserve-crossing allocation fixture uses Serial GC. G1 may reject a
 * 64 KiB artificial block while Runtime reports headroom above the advisory
 * reserve. Normal lifecycle/concurrency lanes retain the default collector.
 */
public final class M3StringRegexPressureTest {
    private static final String REGEX = ".*b*";
    private static final AtomicInteger CHECKS = new AtomicInteger();
    private static final Class<?> DP;
    private static final Class<?> M3;
    private static final Method HEADROOM;
    private static final Method HEAP_ALLOWS;
    private static final Method RELEASE;
    private static final Method TABLES;
    private static final Method MATCH;
    private static final Method OWNER;
    private static final Method COORDINATE;
    private static final Method START;
    private static final Method FACTS;
    private static final Field STORAGE;
    private static final Field CACHES;
    private static final Field SUSPENDED;
    private static volatile byte[][] artificialLoad;

    static {
        try {
            DP = Class.forName("java.lang.M3StringRegexDp");
            // Stage-1 images must fail here: no silent fallback to the strong cache.
            HEADROOM = method(DP, "hasHeapHeadroom", long.class, long.class,
                    long.class, boolean.class);
            HEAP_ALLOWS = method(DP, "heapAllowsPreparation");
            RELEASE = method(DP, "releaseCachedMetadata");
            TABLES = method(DP, "tables");
            M3 = Class.forName("java.lang.M3String");
            MATCH = method(DP, "tryMatch", M3, String.class);
            OWNER = method(M3, "owner");
            COORDINATE = method(M3, "coordinate");
            START = method(M3, "start");
            FACTS = method(M3, "facts");
            STORAGE = field(String.class, "m3");
            CACHES = field(DP, "caches");
            SUSPENDED = field(DP, "preparationSuspended");
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static Method method(Class<?> type, String name, Class<?>... arguments)
            throws ReflectiveOperationException {
        Method method = type.getDeclaredMethod(name, arguments);
        method.setAccessible(true);
        return method;
    }

    private static Field field(Class<?> type, String name) throws ReflectiveOperationException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Object invoke(Method method, Object... arguments) throws Exception {
        try {
            return method.invoke(null, arguments);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Error error) throw error;
            if (cause instanceof Exception exception) throw exception;
            throw failure;
        }
    }

    private static void check(boolean value, String label) {
        int count = CHECKS.incrementAndGet();
        if (!value) throw new AssertionError(label + " at check " + count);
    }

    private static boolean oracle(long maximum, long total, long free, boolean suspended) {
        BigInteger max = BigInteger.valueOf(maximum);
        if (max.signum() <= 0) return false;
        BigInteger committed = BigInteger.valueOf(total).max(BigInteger.ZERO).min(max);
        BigInteger unused = BigInteger.valueOf(free).max(BigInteger.ZERO).min(committed);
        BigInteger available = max.subtract(committed.subtract(unused));
        BigInteger floor = BigInteger.ONE.shiftLeft(suspended ? 20 : 19);
        BigInteger reserve = max.shiftRight(suspended ? 4 : 5).max(floor);
        return available.compareTo(reserve) >= 0;
    }

    private static void scalar(long maximum, long total, long free, boolean suspended)
            throws Exception {
        boolean actual = (boolean) invoke(HEADROOM, maximum, total, free, suspended);
        check(actual == oracle(maximum, total, free, suspended),
                "BigInteger heap-headroom oracle");
    }

    private static void scalarBoundaries() throws Exception {
        long[] values = {Long.MIN_VALUE, -1, 0, 1, 31, 32, (1L << 19) - 1,
                1L << 19, 1L << 20, 32L << 20, 128L << 20, Long.MAX_VALUE};
        for (long maximum : values) {
            for (long total : values) {
                for (long free : values) {
                    scalar(maximum, total, free, false);
                    scalar(maximum, total, free, true);
                }
            }
        }
        Random random = new Random(0x4d335052455353L);
        for (int trial = 0; trial < 10_000; trial++) {
            long maximum = random.nextLong();
            long total = random.nextLong();
            long free = random.nextLong();
            scalar(maximum, total, free, false);
            scalar(maximum, total, free, true);
        }
        for (long maximum : new long[] {32L << 20, 128L << 20, Long.MAX_VALUE}) {
            for (boolean suspended : new boolean[] {false, true}) {
                long reserve = Math.max(suspended ? 1L << 20 : 1L << 19,
                        maximum >>> (suspended ? 4 : 5));
                check(!(boolean) invoke(HEADROOM, maximum, maximum, reserve - 1, suspended),
                        "reserve minus one refuses");
                check((boolean) invoke(HEADROOM, maximum, maximum, reserve, suspended),
                        "exact reserve admits");
                check((boolean) invoke(HEADROOM, maximum, maximum, reserve + 1, suspended),
                        "reserve plus one admits");
            }
        }
        long maximum = 64L << 20;
        long middle = 3L << 20;
        check((boolean) invoke(HEADROOM, maximum, maximum, middle, false),
                "active preparation admits hysteresis middle");
        check(!(boolean) invoke(HEADROOM, maximum, maximum, middle, true),
                "suspended preparation waits at hysteresis middle");
    }

    private static String fixture() throws Exception {
        String text = new String(new char[] {'a', 'b', 'a', 'b'})
                .concat(new String(new char[] {'b', 'a', 'b', 'a'}));
        for (int level = 0; level < 6; level++) text = text.concat(text);
        Object storage = STORAGE.get(text);
        check(storage != null, "genuine char-array/concat M3 ingress");
        check(OWNER.invoke(storage).getClass().getName().equals("java.lang.M3StringTuple"),
                "actual canonical tuple owner");
        FACTS.invoke(storage);
        return text;
    }

    private static int direct(String text, String regex) throws Exception {
        Object storage = STORAGE.get(text);
        check(storage != null, "direct invocation requires actual String.m3 backing");
        return (int) invoke(MATCH, storage, regex);
    }

    private static void parity(String text) throws Exception {
        boolean expected = Pattern.compile(REGEX).matcher(new StringBuilder(text)).matches();
        check(expected, "independent alphabet oracle accepts fixture");
        check(direct(text, REGEX) == 1, "ordinary prepared decision remains definite");
        check(text.matches(REGEX) == expected, "public Pattern parity");
    }

    private static SoftReference<?> cacheRoot() throws Exception {
        Object root = CACHES.get(null);
        check(root instanceof SoftReference<?>, "VM-reclaimable SoftReference metadata root");
        return (SoftReference<?>) root;
    }

    private static void cacheBounds(Object cache) throws Exception {
        AtomicReferenceArray<?> plans = (AtomicReferenceArray<?>) field(cache.getClass(), "plans").get(cache);
        AtomicReferenceArray<?> effects = (AtomicReferenceArray<?>) field(cache.getClass(), "effects").get(cache);
        check(plans.length() == 64, "exact current 64 plan slots");
        check(effects.length() == 256, "exact current 256 effect slots");
        int populated = 0;
        for (int slot = 0; slot < effects.length(); slot++) {
            Object effect = effects.get(slot);
            if (effect != null) {
                check(field(effect.getClass(), "owner").get(effect) instanceof WeakReference<?>,
                        "effect key retains owner weakly");
                populated++;
            }
        }
        check(populated > 0, "actual effect entries exercised");
    }

    private static void inFlightAfterClear(String text, Object held) throws Exception {
        Object storage = STORAGE.get(text);
        Class<?> planType = Class.forName("java.lang.M3StringRegexDp$Plan");
        Object plan = invoke(method(DP, "prepare", String.class, held.getClass()), REGEX, held);
        long[] closure = (long[]) field(planType, "closure").get(plan);
        int[] atoms = (int[]) field(planType, "atoms").get(plan);
        Method evaluate = method(DP, "evaluate", Class.forName("java.lang.M3StringOwner"),
                int.class, int.class, planType, long.class, held.getClass(), int.class);
        long result = (long) invoke(evaluate, OWNER.invoke(storage), (int) START.invoke(storage),
                text.length(), plan, closure[0], held, 0);
        check(result != -1L && (result & (1L << atoms.length)) != 0,
                "in-flight strong cache remains usable after soft-root clear");
        Reference.reachabilityFence(held);
        Reference.reachabilityFence(text);
    }

    private static void lifecycleAndConcurrency() throws Exception {
        String text = fixture();
        Object storage = STORAGE.get(text);
        Object owner = OWNER.invoke(storage);
        long coordinate = (long) COORDINATE.invoke(storage);
        char[] expected = text.toCharArray();
        check((boolean) invoke(HEAP_ALLOWS), "normal bounded heap admits preparation");
        parity(text);
        SoftReference<?> old = cacheRoot();
        Object held = old.get();
        check(held != null, "strong in-flight cache captured");
        cacheBounds(held);
        invoke(RELEASE);
        check(old.get() == null, "explicit release clears soft metadata root");
        inFlightAfterClear(text, held);
        parity(text);
        Object rebuilt = cacheRoot().get();
        check(rebuilt != null && rebuilt != held, "reconstruction creates independent tables");
        cacheBounds(rebuilt);
        List<Callable<Void>> actions = new ArrayList<>();
        for (int worker = 0; worker < 8; worker++) {
            final int id = worker;
            actions.add(() -> {
                for (int round = 0; round < 128; round++) {
                    if (((id + round) & 7) == 0) invoke(RELEASE);
                    parity(text);
                }
                return null;
            });
        }
        try (var executor = Executors.newFixedThreadPool(8)) {
            for (var future : executor.invokeAll(actions)) future.get();
        }
        check(OWNER.invoke(STORAGE.get(text)) == owner, "clear/rebuild leaves canonical owner unchanged");
        check((long) COORDINATE.invoke(STORAGE.get(text)) == coordinate,
                "clear/rebuild leaves canonical coordinate unchanged");
        check(java.util.Arrays.equals(text.toCharArray(), expected),
                "clear/rebuild leaves canonical content unchanged");
        Reference.reachabilityFence(held);
        Reference.reachabilityFence(rebuilt);
        Reference.reachabilityFence(text);
    }

    private static long headroom() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
    }

    private static void actualPressure() throws Exception {
        String text = fixture();
        Object storage = STORAGE.get(text);
        Object owner = OWNER.invoke(storage);
        long coordinate = (long) COORDINATE.invoke(storage);
        char[] expected = text.toCharArray();
        for (int warmup = 0; warmup < 32; warmup++) parity(text);
        SoftReference<?> old = cacheRoot();
        Object held = old.get();
        check(held != null, "pressure fixture captures an in-flight cache");
        long maximum = Runtime.getRuntime().maxMemory();
        long reserve = Math.max(1L << 19, maximum >>> 5);
        byte[][] blocks = new byte[(int) (maximum / 65_536L) + 16][];
        artificialLoad = blocks;
        int used = 0;
        long observed;
        try {
            while (headroom() >= reserve && used < blocks.length) {
                blocks[used++] = new byte[65_536];
            }
            observed = headroom();
            check(observed < reserve, "artificial heap load crosses active reserve");
            check(!(boolean) invoke(HEAP_ALLOWS), "actual heap governor refuses below reserve");
            check(SUSPENDED.getBoolean(null), "actual pressure suspends preparation");
            check(old.get() == null, "pressure clears soft root despite strong in-flight hold");
            check(direct(text, REGEX) == -1, "pressure delegates ordinary DP with UNKNOWN");
            check(direct(text, "[a-z]+") == -1,
                    "pressure guard precedes already-prepared class facts");
            check(text.matches(REGEX), "public Pattern fallback remains exact under bounded pressure");
            check(OWNER.invoke(STORAGE.get(text)) == owner, "pressure preserves canonical owner");
            check((long) COORDINATE.invoke(STORAGE.get(text)) == coordinate,
                    "pressure preserves canonical coordinates");
        } finally {
            for (int index = 0; index < blocks.length; index++) blocks[index] = null;
            artificialLoad = null;
            // Only the test releases its artificial load; production never requests GC.
            System.gc();
        }
        boolean recovered = false;
        for (int attempt = 0; attempt < 20 && !recovered; attempt++) {
            recovered = (boolean) invoke(HEAP_ALLOWS);
            if (!recovered) {
                System.gc();
                Thread.sleep(10);
            }
        }
        check(recovered && !SUSPENDED.getBoolean(null), "headroom recovery resumes preparation");
        parity(text);
        Object rebuilt = cacheRoot().get();
        check(rebuilt != null && rebuilt != held, "pressure recovery reconstructs soft metadata");
        check(java.util.Arrays.equals(text.toCharArray(), expected), "pressure/recovery preserves content");
        inFlightAfterClear(text, held);
        System.out.println("M3_REGEX_PRESSURE_REFUSAL_PASS blocks=" + used
                + " headroom=" + observed + " reserve=" + reserve);
        Reference.reachabilityFence(held);
        Reference.reachabilityFence(text);
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length == 1 && arguments[0].equals("pressure")) {
            actualPressure();
        } else {
            check(arguments.length == 0, "normal test argument count");
            scalarBoundaries();
            lifecycleAndConcurrency();
        }
        System.out.println("M3_REGEX_PRESSURE_PASS checks=" + CHECKS.get());
    }
}
||||||| merged common ancestors
=======
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
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
