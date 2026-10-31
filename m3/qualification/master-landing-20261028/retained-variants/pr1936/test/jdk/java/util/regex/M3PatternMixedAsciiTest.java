/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
/*
 * @test
 * @summary Reuse immutable regex ASCII masks after exact Unicode rolling transitions
 * @run main/othervm --add-opens java.base/java.util.regex=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED M3PatternMixedAsciiTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage --add-opens java.base/java.util.regex=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED M3PatternMixedAsciiTest m3
 */

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.function.IntPredicate;

public final class M3PatternMixedAsciiTest {
    private static final Field PLAN = field(Pattern.class, "m3BooleanPlan");
    private static final Class<?> PLAN_CLASS = type("java.util.regex.M3PatternQuery$BooleanPlan");
    private static final Class<?> PREDICATE = type("java.util.regex.Pattern$CharPredicate");
    private static final Class<?> PREDICATE_ARRAY = Array.newInstance(PREDICATE, 0).getClass();
    private static final Field ATOMS = field(PLAN_CLASS, "atoms");
    private static final Field ASCII = field(PLAN_CLASS, "ascii");
    private static final Field REPEATED = field(PLAN_CLASS, "repeated");
    private static final Field CODE_POINT = field(PLAN_CLASS, "codePoint");
    private static final Field EMPTY = field(PLAN_CLASS, "empty");
    private static final Method IS = method(PREDICATE, "is", int.class);
    private static final Method MATCHES = method(PLAN_CLASS, "matches", String.class);
    private static final Field STORAGE = field(String.class, "m3");
    private static long checks;
    private static long pairs;
    private static long tuples;
    private static long splitPairTuples;
    private static long observedAsciiCalls;

    private static Class<?> type(String name) {
        try { return Class.forName(name); }
        catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
    }
    private static Field field(Class<?> owner, String name) {
        try { Field value = owner.getDeclaredField(name); value.setAccessible(true); return value; }
        catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
    }
    private static Method method(Class<?> owner, String name, Class<?>... args) {
        try { Method value = owner.getDeclaredMethod(name, args); value.setAccessible(true); return value; }
        catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
    }
    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
    private static int invoke(Object plan, String text) throws Exception {
        try { return (int) MATCHES.invoke(plan, text); }
        catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof Error cause) throw cause;
            if (failure.getCause() instanceof RuntimeException cause) throw cause;
            throw failure;
        }
    }

    /** Delegates the actual JDK predicate; masks are prepared before attaching this observer. */
    private static final class Calls {
        long ascii;
        long unicode;
        boolean failPreparation;
        Object observe(Object original) {
            return Proxy.newProxyInstance(PREDICATE.getClassLoader(), new Class<?>[] {PREDICATE},
                    (proxy, method, arguments) -> {
                        if (!method.getName().equals("is")) throw new UnsupportedOperationException(method.toString());
                        if (failPreparation) {
                            failPreparation = false;
                            throw new OutOfMemoryError("optional ASCII preparation witness");
                        }
                        int cp = (int) arguments[0];
                        if (cp < 128) ascii++; else unicode++;
                        try { return IS.invoke(original, arguments); }
                        catch (InvocationTargetException failure) { throw failure.getCause(); }
                    });
        }
        void attach(Object plan) throws Exception {
            Object[] atoms = (Object[]) ATOMS.get(plan);
            for (int i = 0; i < atoms.length; i++) atoms[i] = observe(atoms[i]);
            ascii = unicode = 0;
        }
    }

    // Removing the rolling-to-mask handback must fail this test with identical semantic results.
    private static void reuse() throws Exception {
        for (int count : new int[] {63, 64, 127, 128, 255, 256}) {
            Pattern pattern = Pattern.compile("é*" + "a*".repeat(count - 2) + "b");
            Object plan = PLAN.get(pattern);
            check(plan != null, "missing admitted plan " + count);
            long[] masks = (long[]) ASCII.get(plan);
            check(masks != null, "missing prepared masks " + count);
            long[] before = masks.clone();
            Calls calls = new Calls();
            calls.attach(plan);
            String text = "é".concat("a".repeat(4096)).concat("b");
            check(pattern.asMatchPredicate().test(text), "mixed positive " + count);
            observedAsciiCalls += calls.ascii;
            check(calls.unicode > 0, "Unicode rolling transition was not observed " + count);
            check(calls.ascii == 0, "ASCII_MASK_REUSE_MISSING atoms=" + count + " suffixPredicateCalls=" + calls.ascii);
            check(ASCII.get(plan) == masks && Arrays.equals(before, masks), "mask mutation " + count);
            // A known negative oracle avoids hostile recursive Matcher backtracking.
            check(!pattern.asMatchPredicate().test("é" + "a".repeat(30000) + "c"), "hostile negative " + count);
        }
        System.out.println("REUSE_PASS suffixAsciiPredicateCalls=" + observedAsciiCalls);
    }

    private static String composite(String text) throws Exception {
        int cut = text.length() / 2;
        String result = new String(text.substring(0, cut).toCharArray())
                .concat(new String(text.substring(cut).toCharArray()));
        Object body = STORAGE.get(result);
        if (body != null) {
            Method owner = method(body.getClass(), "owner");
            if (owner.invoke(body).getClass().getName().equals("java.lang.M3StringTuple")) tuples++;
        }
        return result;
    }

    private static void pair(Pattern pattern, String text) throws Exception {
        boolean expected = pattern.matcher(new StringBuilder(text)).matches();
        String flat = new String(text.toCharArray());
        String joined = composite(text);
        check(pattern.asMatchPredicate().test(text) == expected, "original structure mismatch " + pattern);
        check(pattern.asMatchPredicate().test(flat) == expected, "flat mismatch " + pattern);
        check(pattern.asMatchPredicate().test(joined) == expected, "composite mismatch " + pattern);
        Object plan = PLAN.get(pattern);
        if (plan != null) check(invoke(plan, joined) == (expected ? 1 : 0), "plan mismatch " + pattern);
        if ((pairs & 63) == 0 && pattern.flags() == 0) {
            check(Pattern.matches(pattern.pattern(), joined) == expected, "static mismatch");
            check(joined.matches(pattern.pattern()) == expected, "String mismatch");
        }
        pairs++;
    }

    private static void differential() throws Exception {
        Random random = new Random(0x4d334d49584544L);
        String[] atoms = {"a", "b", "a*", "b*", ".", ".*", "é*", "[ab]*", "[^x]*", "\\p{L}*", "\\p{Nd}*", "\\uD800*", "\\uDC00*"};
        String[] units = {"", "a", "b", "x", "é", "一", "\n", "\r", "\u0085", "\u2028", "\ud800", "\udc00", "\ud801\udc00", "\ud83d\ude00"};
        int[] flags = {0, Pattern.DOTALL, Pattern.UNIX_LINES, Pattern.UNICODE_CHARACTER_CLASS,
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE};
        for (int sample = 0; sample < 12000; sample++) {
            StringBuilder regex = new StringBuilder();
            for (int i = 0, n = 1 + random.nextInt(6); i < n; i++) regex.append(atoms[random.nextInt(atoms.length)]);
            StringBuilder text = new StringBuilder();
            for (int i = 0, n = random.nextInt(6); i < n; i++) text.append(units[random.nextInt(units.length)]);
            pair(Pattern.compile(regex.toString(), flags[random.nextInt(flags.length)]), text.toString());
        }
        // Safe bounded ASCII runs reach handback in scalar and wide cohorts and cross epsilon words.
        for (int count : new int[] {1, 2, 31, 32, 62, 63, 64, 65, 127, 128, 255, 256}) {
            Pattern pattern = Pattern.compile(count == 1 ? ".*" : ".*".repeat(count - 1) + "b", Pattern.DOTALL);
            for (String middle : List.of("é", "\ud800", "\udc00", "\ud801\udc00", "\udc00\ud800")) {
                for (String text : List.of(middle + "a".repeat(48) + "b",
                        "a".repeat(24) + middle + "a".repeat(48) + "b",
                        "a".repeat(48) + middle + "b")) pair(pattern, text);
            }
        }
        for (int unit = 0; unit < 128; unit++) pair(Pattern.compile("é*.*b", Pattern.DOTALL), "é" + ((char) unit) + "a".repeat(40) + "b");
        String splitPair = new String(new char[] {'\ud801'}).concat(new String(new char[] {'\udc00'}));
        Object pairBody = STORAGE.get(splitPair);
        if (pairBody != null && method(pairBody.getClass(), "owner").invoke(pairBody)
                .getClass().getName().equals("java.lang.M3StringTuple")) splitPairTuples++;
        String split = "a".repeat(32).concat(splitPair).concat("a".repeat(48)).concat("b");
        pair(Pattern.compile(".*b", Pattern.DOTALL), split);
        pair(Pattern.compile("a*\\p{L}*b"), "a".repeat(32) + "\ud801\udc00" + "a".repeat(48) + "b");
        // A dead frontier may become ASCII again; it must remain dead without skipping pair logic.
        pair(Pattern.compile("a*b"), "é" + "a".repeat(64) + "b");
        pair(Pattern.compile("a*b"), "é\ud800\udc00" + "a".repeat(64) + "b");
        pair(Pattern.compile("é*" + "a*".repeat(62) + "b"), "é".repeat(96) + "a".repeat(128) + "b");
        Pattern islands = Pattern.compile(".*b", Pattern.DOTALL);
        check(islands.asMatchPredicate().test(("é" + "a".repeat(8)).repeat(20000) + "b"), "short-island stack safety");
        check(islands.asMatchPredicate().test(("é" + "a".repeat(32)).repeat(20000) + "b"), "long-island stack safety");
    }

    // Real JDK node methods remain the oracle when BMP predicates accept surrogate units.
    private static void syntheticWidths() throws Exception {
        Class<?> nodeClass = type("java.util.regex.Pattern$Node");
        Class<?> queryClass = type("java.util.regex.M3PatternQuery");
        Field root = field(Pattern.class, "matchRoot");
        Field next = field(nodeClass, "next");
        Method derive = method(queryClass, "deriveBoolean", nodeClass);
        for (boolean firstBmp : new boolean[] {false, true}) {
            for (boolean secondBmp : new boolean[] {false, true}) {
                for (boolean firstRepeat : new boolean[] {false, true}) {
                    for (boolean secondRepeat : new boolean[] {false, true}) {
                        for (IntPredicate firstTest : List.<IntPredicate>of(cp -> true,
                                cp -> cp >= 0xd800 && cp <= 0xdbff)) {
                            Object first = syntheticAtom(firstBmp, firstRepeat, firstTest);
                            Object second = syntheticAtom(secondBmp, secondRepeat,
                                    cp -> cp >= 0xdc00 && cp <= 0xdfff);
                            Pattern oracle = Pattern.compile("a*".repeat(62) + "b");
                            next.set(first, second);
                            next.set(second, root.get(oracle));
                            root.set(oracle, first);
                            Object plan = derive.invoke(null, first);
                            check(plan != null, "synthetic wide plan missing");
                            for (String prefix : List.of("", "\ud800", "\udc00", "\ud800\udc00",
                                    "\ud800\ud800\udc00", "\ud800\udc00\udc00")) {
                                String text = prefix + "a".repeat(48) + "b";
                                boolean expected = oracle.matcher(new StringBuilder(text)).matches();
                                check(invoke(plan, composite(text)) == (expected ? 1 : 0),
                                        "synthetic width mismatch " + firstBmp + "/" + secondBmp
                                                + "/" + firstRepeat + "/" + secondRepeat);
                            }
                        }
                    }
                }
            }
        }
    }

    private static Object syntheticAtom(boolean bmp, boolean repeat, IntPredicate test) throws Exception {
        Class<?> predicateClass = type("java.util.regex.Pattern$BmpCharPredicate");
        Object predicate = Proxy.newProxyInstance(predicateClass.getClassLoader(), new Class<?>[] {predicateClass},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("is")) return test.test((int) arguments[0]);
                    throw new UnsupportedOperationException(method.toString());
                });
        Class<?> propertyClass = type("java.util.regex.Pattern$" + (bmp ? "BmpCharProperty" : "CharProperty"));
        Constructor<?> propertyConstructor = propertyClass.getDeclaredConstructor(bmp ? predicateClass : PREDICATE);
        propertyConstructor.setAccessible(true);
        Object property = propertyConstructor.newInstance(predicate);
        if (!repeat) return property;
        Constructor<?> greedy = type("java.util.regex.Pattern$" + (bmp ? "BmpCharPropertyGreedy" : "CharPropertyGreedy"))
                .getDeclaredConstructor(propertyClass, int.class);
        greedy.setAccessible(true);
        return greedy.newInstance(property, 0);
    }

    /** Real optional mask preparation catches the injected OOM; no production failure hook. */
    private static void absenceAndOom() throws Exception {
        Constructor<?> constructor = PLAN_CLASS.getDeclaredConstructor(PREDICATE_ARRAY,
                boolean[].class, boolean[].class, boolean[].class);
        constructor.setAccessible(true);
        for (int count : new int[] {2, 63, 64, 128, 256}) {
            Pattern pattern = Pattern.compile("é*" + "a*".repeat(count - 2) + "b");
            Object original = PLAN.get(pattern);
            Object[] atoms = ((Object[]) ATOMS.get(original)).clone();
            Calls calls = new Calls();
            for (int i = 0; i < atoms.length; i++) atoms[i] = calls.observe(atoms[i]);
            calls.failPreparation = true;
            Object noMasks = constructor.newInstance(atoms, REPEATED.get(original), CODE_POINT.get(original), EMPTY.get(original));
            check(ASCII.get(noMasks) == null, "optional OOM did not refuse mask preparation " + count);
            calls.ascii = calls.unicode = 0;
            String text = "é" + (count == 2 ? "é" : "a").repeat(96) + "b";
            check(invoke(noMasks, text) == 1, "null-mask positive " + count);
            check(calls.ascii > 0, "null masks unexpectedly used masked execution " + count);
            check(invoke(noMasks, "é\ud800\udc00" + "a".repeat(32) + "b") == 0, "null-mask surrogate negative " + count);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("semantic-only")) {
            differential(); syntheticWidths(); absenceAndOom();
        } else {
            reuse(); differential(); syntheticWidths(); absenceAndOom();
            if (args.length > 0 && args[0].equals("m3")) {
                check(tuples >= 1000, "missing actual M3 composite coverage " + tuples);
                check(splitPairTuples > 0, "missing actual split-owner surrogate tuple");
            }
        }
        System.out.println("MIXED_ASCII_PASS pairs=" + pairs + " checks=" + checks + " tupleObservations=" + tuples + " splitPairTuples=" + splitPairTuples);
    }
}
