/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only
 */
/*
 * @test
 * @summary Verify bounded weak-identity two-way factor metadata and exact search semantics
 * @modules java.base/java.lang:+open
 * @run main M3FlatFactorizationCacheTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3FlatFactorizationCacheTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3FlatFactorizationCacheTest
 */
import java.lang.ref.Reference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReferenceArray;

/** Cache behavior plus an independent UTF-16 search oracle. */
public class M3FlatFactorizationCacheTest {
    private static final int SLOTS = 64;
    private static final long BUDGET = 256L * (8192L * 16 + 256L * 4)
            + 64L * 32768 * 8 + 2L * SLOTS * Long.BYTES;
    private static long checks;
    private static ThreadLocal<Boolean> admitting;
    private static Field value, coder, m3;
    private static Method cached, fresh, twoWay, intern, budget;
    private static Constructor<String> canonical;
    private static AtomicReferenceArray<?> entries;

    private static void bind() throws Exception {
        Class<?> owner = Class.forName("java.lang.M3StringSearchPrecompute");
        Class<?> storage = Class.forName("java.lang.M3String");
        Class<?> lane = Class.forName("java.lang.M3StringBuilderLane");
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Field field = storage.getDeclaredField("ADMITTING");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Boolean> state = (ThreadLocal<Boolean>) field.get(null);
        admitting = state;
        value = String.class.getDeclaredField("value"); value.setAccessible(true);
        coder = String.class.getDeclaredField("coder"); coder.setAccessible(true);
        m3 = String.class.getDeclaredField("m3"); m3.setAccessible(true);
        cached = owner.getDeclaredMethod("flatFactorization", CharSequence.class,
                byte[].class, byte.class, int.class, boolean.class);
        fresh = owner.getDeclaredMethod("factorization", CharSequence.class,
                byte[].class, byte.class, int.class, boolean.class);
        twoWay = owner.getDeclaredMethod("twoWay", byte[].class, byte.class, int.class,
                lane, CharSequence.class, int.class, boolean.class);
        intern = pool.getDeclaredMethod("internChars", char[].class, int.class, int.class);
        budget = owner.getDeclaredMethod("maximumRetainedPrimitiveBytes");
        for (Method method : new Method[] {cached, fresh, twoWay, intern, budget}) method.setAccessible(true);
        canonical = String.class.getDeclaredConstructor(storage); canonical.setAccessible(true);
        Field cache = Class.forName(owner.getName() + "$FlatFactors").getDeclaredField("ENTRIES");
        cache.setAccessible(true);
        entries = (AtomicReferenceArray<?>) cache.get(null);
    }

    private static String flat(char[] units) throws Exception {
        Boolean previous = admitting.get();
        admitting.set(Boolean.TRUE);
        try {
            String text = new String(units);
            check(m3.get(text) == null, "physically flat input");
            return text;
        } finally {
            if (previous == null) admitting.remove();
            else admitting.set(previous);
        }
    }

    private static int slot(String text, boolean reverse) {
        return (System.identityHashCode(text) & (SLOTS - 1)) + (reverse ? SLOTS : 0);
    }

    private static long factor(Method method, String text, boolean reverse) throws Exception {
        return (long) method.invoke(null, text, value.get(text), coder.getByte(text), text.length(), reverse);
    }

    private static void validate(String text, boolean reverse) throws Exception {
        long expected = factor(fresh, text, reverse);
        check(factor(cached, text, reverse) == expected, "cache miss or exact hit parity");
        Reference<?> entry = (Reference<?>) entries.get(slot(text, reverse));
        check(entry != null && entry.get() == text, "exact weak key");
        check(factor(cached, text, reverse) == expected, "repeat parity");
        check(entries.get(slot(text, reverse)) == entry, "repeat must reuse metadata");
    }

    private static int reference(char[] hay, CharSequence needle, int first, boolean reverse) {
        for (int pos = first; pos <= hay.length - needle.length(); pos++) {
            int i = 0;
            while (i < needle.length() && hay[reverse ? hay.length - 1 - pos - i : pos + i]
                    == needle.charAt(reverse ? needle.length() - 1 - i : i)) i++;
            if (i == needle.length()) return pos;
        }
        return -1;
    }

    private static void compare(String hay, char[] units, CharSequence needle) throws Exception {
        for (boolean reverse : new boolean[] {false, true}) {
            for (int first = 0; first <= units.length - needle.length(); first++) {
                int expected = reference(units, needle, first, reverse);
                int actual = (int) twoWay.invoke(null, value.get(hay), coder.getByte(hay), units.length,
                        null, needle, first, reverse);
                check(actual == expected, "independent search parity");
            }
        }
    }

    private static void race(String first, String second) throws Exception {
        String[] keys = {first, second};
        long[][] expected = new long[2][2];
        for (int key = 0; key < keys.length; key++) {
            for (int direction = 0; direction < 2; direction++) {
                expected[key][direction] = factor(fresh, keys[key], direction != 0);
            }
        }
        try (var workers = Executors.newFixedThreadPool(4)) {
            java.util.List<Future<Integer>> tasks = new java.util.ArrayList<>();
            for (int worker = 0; worker < 4; worker++) {
                int id = worker;
                tasks.add(workers.submit(() -> {
                    int count = 0;
                    for (int i = 0; i < 128; i++) {
                        int key = (i + id) & 1;
                        int direction = (i >>> 1) & 1;
                        if (factor(cached, keys[key], direction != 0) != expected[key][direction]) {
                            throw new AssertionError("concurrent collision returned foreign metadata");
                        }
                        count++;
                    }
                    return count;
                }));
            }
            for (Future<Integer> task : tasks) {
                int count = task.get();
                check(count == 128, "concurrent task completed");
                checks += count;
            }
        }
    }

    private static void check(boolean condition, String detail) {
        checks++;
        if (!condition) throw new AssertionError("check " + checks + ": " + detail);
    }

    public static void main(String[] args) throws Exception {
        bind();
        check(entries != null && entries.length() == 2 * SLOTS, "fixed cardinality");
        check((long) budget.invoke(null) == BUDGET, "primitive budget includes packed entries");
        String a = flat("abababababababababababababababab".toCharArray());
        validate(a, false); validate(a, true);
        Object forward = entries.get(slot(a, false));
        Object reverse = entries.get(slot(a, true));
        check(forward != reverse, "directions have independent entries");
        ((Reference<?>) reverse).clear();
        validate(a, true);
        check(entries.get(slot(a, true)) != reverse, "cleared reference rebuilt");
        check(entries.get(slot(a, false)) == forward, "reverse rebuild preserved forward");
        Random random = new Random(0x464143544f52534cL);
        String[] seen = new String[SLOTS];
        boolean collision = false;
        String firstCollision = null, secondCollision = null;
        for (int trial = 0; trial <= SLOTS; trial++) {
            char[] chars = new char[32];
            for (int i = 0; i < chars.length; i++) chars[i] = (char) random.nextInt(0x10000);
            String text = flat(chars);
            int index = slot(text, false);
            if (seen[index] != null) {
                validate(seen[index], false);
                validate(text, false);
                validate(seen[index], false);
                if (!collision) { firstCollision = seen[index]; secondCollision = text; }
                collision = true;
            }
            seen[index] = text;
        }
        check(collision, "pigeonhole identity collision");
        race(firstCollision, secondCollision);
        for (int length : new int[] {15, 8193}) {
            String bypass = flat(new char[length]);
            Object old = entries.get(slot(bypass, true));
            check(factor(cached, bypass, true) == factor(fresh, bypass, true), "length bypass parity");
            check(entries.get(slot(bypass, true)) == old, "length bypass does not cache");
        }
        char[] hay = "abababababababababababababababababababababababababababababababab".toCharArray();
        String flatHay = flat(hay);
        StringBuilder mutable = new StringBuilder(a);
        compare(flatHay, hay, mutable);
        mutable.setCharAt(0, 'c');
        compare(flatHay, hay, mutable);
        char[] units = a.toCharArray();
        Object descriptor = intern.invoke(null, units, 0, units.length);
        check(descriptor != null, "canonical pool admission");
        String backed = canonical.newInstance(descriptor);
        check(m3.get(backed) == descriptor, "canonical String fixture");
        Object prior = entries.get(slot(backed, true));
        compare(flatHay, hay, backed);
        check(entries.get(slot(backed, true)) == prior, "canonical String bypass");
        for (int trial = 0; trial < 64; trial++) {
            char[] source = new char[48];
            for (int i = 0; i < source.length; i++) source[i] = (char) (random.nextInt(4) + (trial % 2 == 0 ? 0x60 : 0xd800));
            char[] needle = java.util.Arrays.copyOfRange(source, trial % 16, trial % 16 + 16);
            if (trial % 3 == 0) needle[3] ^= 1;
            compare(flat(source), source, flat(needle));
        }
        System.out.println("M3_FLAT_FACTORIZATION_CACHE_PASS checks=" + checks);
    }
}
