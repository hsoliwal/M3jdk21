/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
/*
 * @test
 * @summary Reuse exact nullable Unicode frontiers without rescanning every atom
 * @modules java.base/java.util.regex:open
 *          jdk.management/com.sun.management
 * @run main/othervm -Xint M3PatternNullablePrefixTest
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import com.sun.management.ThreadMXBean;

/** The semantic oracle is ordinary Matcher on a separate mutable carrier. */
public final class M3PatternNullablePrefixTest {
    private static final Class<?> PLAN_TYPE;
    private static final Field PLAN;
    private static final MethodHandle MATCHES;
    private static final MethodHandle ROLLING;
    private static long checks;
    private static volatile long sink;

    static {
        try {
            PLAN_TYPE = Class.forName("java.util.regex.M3PatternQuery$BooleanPlan");
            PLAN = field(Pattern.class, "m3BooleanPlan");
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(PLAN_TYPE, MethodHandles.lookup());
            MethodType type = MethodType.methodType(int.class, String.class);
            MethodType erased = MethodType.methodType(int.class, Object.class, String.class);
            MATCHES = lookup.findVirtual(PLAN_TYPE, "matches", type).asType(erased);
            ROLLING = lookup.findVirtual(PLAN_TYPE, "matchesRolling", type).asType(erased);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private M3PatternNullablePrefixTest() { }

    private static Field field(Class<?> type, String name) throws ReflectiveOperationException {
        Field value = type.getDeclaredField(name);
        value.setAccessible(true);
        return value;
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void pair(Pattern pattern, String input) throws Throwable {
        Object plan = PLAN.get(pattern);
        require(plan != null, "admitted plan");
        boolean expected = pattern.matcher(new StringBuilder(input)).matches();
        require(pattern.asMatchPredicate().test(input) == expected, "public result: " + pattern);
        int wanted = expected ? 1 : 0;
        require((int) MATCHES.invokeExact(plan, input) == wanted, "prefix state result: " + pattern);
        require((int) ROLLING.invokeExact(plan, input) == wanted, "rolling result: " + pattern);
    }

    private static void boundaries() throws Throwable {
        String letter = "\ud801\udc00";
        String digit = "\ud835\udfce";
        List<String> inputs = List.of("", "abc", "123", "!", "\u00e9", "\ud800", "\udc00",
                letter, digit, letter + digit, letter + "123", letter + "!", digit + letter,
                "abc" + letter + digit, "123" + letter, letter + "X" + digit,
                letter + "\ud800\udc00", letter + "\ud800", "\ud800" + letter);
        for (String regex : List.of("\\p{L}*", "\\p{L}*\\p{N}*", "\\p{L}*[0-9]*",
                "\\p{L}*[0-9]*.*", ".*[a-z]*", "[a-z]*\\p{L}*",
                "\\p{L}*X\\p{N}*", "\\p{L}*X")) {
            Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
            for (String input : inputs) pair(pattern, input);
        }
        for (int atoms : new int[] {1, 63, 64, 256}) {
            Pattern pattern = Pattern.compile(".*".repeat(atoms), Pattern.DOTALL);
            for (String input : inputs) pair(pattern, input);
            pair(pattern, "a".repeat(8193) + letter.repeat(33));
        }
        // Verify the intended compiled-node boundaries, independently of a parser assumption.
        Object bmp = PLAN.get(Pattern.compile("[a-z]*\\p{L}*"));
        Object general = PLAN.get(Pattern.compile("\\p{L}*\\p{N}*"));
        Object nonnullable = PLAN.get(Pattern.compile("\\p{L}*X"));
        require(!((boolean[]) field(PLAN_TYPE, "codePoint").get(bmp))[0], "BMP first node");
        require(((boolean[]) field(PLAN_TYPE, "codePoint").get(general))[0], "general first node");
        boolean[] empty = (boolean[]) field(PLAN_TYPE, "empty").get(nonnullable);
        require(!empty[empty.length - 1], "nonnullable suffix");
    }

    /** A BMP-interface predicate can accept a full code point even in a BMP node. */
    private static void bmpNodeMustKeepUnitWidth() throws Throwable {
        Class<?> node = Class.forName("java.util.regex.Pattern$Node");
        Class<?> predicate = Class.forName("java.util.regex.Pattern$BmpCharPredicate");
        Class<?> property = Class.forName("java.util.regex.Pattern$BmpCharProperty");
        Class<?> greedy = Class.forName("java.util.regex.Pattern$BmpCharPropertyGreedy");
        Constructor<?> propertyConstructor = property.getDeclaredConstructor(predicate);
        Constructor<?> greedyConstructor = greedy.getDeclaredConstructor(property, int.class);
        propertyConstructor.setAccessible(true);
        greedyConstructor.setAccessible(true);
        Object first = greedyConstructor.newInstance(propertyConstructor.newInstance(
                proxy(predicate, cp -> cp == 0x10000)), 0);
        Object second = greedyConstructor.newInstance(propertyConstructor.newInstance(
                proxy(predicate, cp -> false)), 0);
        Field next = field(node, "next");
        next.set(first, second);
        next.set(second, field(Pattern.class, "lastAccept").get(null));
        Method derive = Class.forName("java.util.regex.M3PatternQuery").getDeclaredMethod("deriveBoolean", node);
        derive.setAccessible(true);
        Object plan = derive.invoke(null, first);
        Pattern oracle = Pattern.compile("a*");
        field(Pattern.class, "matchRoot").set(oracle, first);
        String input = "\ud800\udc00";
        require(!oracle.matcher(new StringBuilder(input)).matches(), "actual BMP node rejects pair");
        require((int) MATCHES.invokeExact(plan, input) == 0, "BMP_NODE_WIDTH_CHANGED");
        require((int) ROLLING.invokeExact(plan, input) == 0, "BMP_ROLLING_WIDTH_CHANGED");
    }

    private static Object proxy(Class<?> type, IntPredicate predicate) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (object, method, args) -> {
            if (method.getName().equals("is")) return predicate.test((Integer) args[0]);
            throw new UnsupportedOperationException(method.toString());
        });
    }

    private static long allocated(ThreadMXBean bean, Predicate<String> predicate, String input, int calls) {
        long thread = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(thread);
        long result = 0;
        for (int i = 0; i < calls; i++) result += predicate.test(input) ? 1 : 0;
        long bytes = bean.getThreadAllocatedBytes(thread) - before;
        sink = result;
        require(result == calls, "resource result");
        return bytes;
    }

    private static void allocationWitness() {
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        require(bean.isThreadAllocatedMemorySupported(), "allocation counter supported");
        bean.setThreadAllocatedMemoryEnabled(true);
        Predicate<String> predicate = Pattern.compile(".*".repeat(256), Pattern.DOTALL).asMatchPredicate();
        String bmp = "\u00e9".repeat(16);
        String pairs = "\ud801\udc00".repeat(16);
        for (int i = 0; i < 128; i++) {
            sink = predicate.test(bmp) ? 1 : 0;
            sink = predicate.test(pairs) ? 1 : 0;
        }
        allocated(bean, predicate, bmp, 16);
        allocated(bean, predicate, pairs, 16);
        int calls = 1024;
        long bmpBytes = allocated(bean, predicate, bmp, calls);
        long pairBytes = allocated(bean, predicate, pairs, calls);
        System.out.println("NULLABLE_PREFIX_ALLOCATION bmp_bytes=" + bmpBytes
                + " pair_bytes=" + pairBytes + " calls=" + calls + " tolerance=1024");
        require(pairBytes <= bmpBytes + 1024,
                "NULLABLE_PREFIX_REUSE_MISSING: an all-live prefix still allocates an intermediate row");
    }

    public static void main(String[] arguments) throws Throwable {
        boundaries();
        bmpNodeMustKeepUnitWidth();
        System.out.println("NULLABLE_PREFIX_SEMANTICS_PASS checks=" + checks);
        allocationWitness();
        System.out.println("M3PatternNullablePrefixTest PASS checks=" + checks + " sink=" + sink);
    }
}
