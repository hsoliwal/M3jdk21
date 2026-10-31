/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
/*
 * @test
 * @summary Preserve compiled-node Unicode consumption in the M3 Boolean full-match plan
 * @modules java.base/java.util.regex:open
 * @run main/othervm M3PatternUnicodeTest candidate
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.function.IntPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Uses ordinary Matcher on a separate StringBuilder as the semantic oracle. */
public final class M3PatternUnicodeTest {
    private static final Field PLAN;
    private static final Field ROOT;
    private static final Field NEXT;
    private static final Field ASCII;
    private static final Method MATCHES;
    private static final Method ROLLING;
    private static final Method DERIVE;
    private static final Object ACCEPT;
    private static final Class<?> PREDICATE;
    private static final Class<?> BMP_PREDICATE;
    private static final Constructor<?> PROPERTY;
    private static final Constructor<?> BMP_PROPERTY;
    private static final Constructor<?> GREEDY;
    private static final Constructor<?> BMP_GREEDY;
    private static final MessageDigest DIGEST;
    private static long checks;
    private static long pairs;
    private static long decisions;
    private static long refusals;
    private static long flagCombinations;
    private static boolean candidate;

    static {
        try {
            Class<?> node = Class.forName("java.util.regex.Pattern$Node");
            Class<?> query = Class.forName("java.util.regex.M3PatternQuery");
            Class<?> plan = Class.forName("java.util.regex.M3PatternQuery$BooleanPlan");
            Class<?> property = Class.forName("java.util.regex.Pattern$CharProperty");
            Class<?> bmpProperty = Class.forName("java.util.regex.Pattern$BmpCharProperty");
            PREDICATE = Class.forName("java.util.regex.Pattern$CharPredicate");
            BMP_PREDICATE = Class.forName("java.util.regex.Pattern$BmpCharPredicate");
            PLAN = field(Pattern.class, "m3BooleanPlan");
            ROOT = field(Pattern.class, "matchRoot");
            NEXT = field(node, "next");
            ASCII = field(plan, "ascii");
            ACCEPT = field(Pattern.class, "lastAccept").get(null);
            MATCHES = method(plan, "matches", String.class);
            ROLLING = method(plan, "matchesRolling", String.class);
            DERIVE = method(query, "deriveBoolean", node);
            PROPERTY = constructor(property, PREDICATE);
            BMP_PROPERTY = constructor(bmpProperty, BMP_PREDICATE);
            GREEDY = constructor(Class.forName("java.util.regex.Pattern$CharPropertyGreedy"), property, int.class);
            BMP_GREEDY = constructor(Class.forName("java.util.regex.Pattern$BmpCharPropertyGreedy"), bmpProperty, int.class);
            DIGEST = MessageDigest.getInstance("SHA-256");
        } catch (ReflectiveOperationException | java.security.NoSuchAlgorithmException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
        Field result = owner.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static Method method(Class<?> owner, String name, Class<?>... arguments) throws ReflectiveOperationException {
        Method result = owner.getDeclaredMethod(name, arguments);
        result.setAccessible(true);
        return result;
    }

    private static Constructor<?> constructor(Class<?> owner, Class<?>... arguments) throws ReflectiveOperationException {
        Constructor<?> result = owner.getDeclaredConstructor(arguments);
        result.setAccessible(true);
        return result;
    }

    private static int invoke(Method method, Object plan, String input) throws Exception {
        try {
            return (Integer) method.invoke(plan, input);
        } catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof RuntimeException cause) throw cause;
            if (failure.getCause() instanceof Error cause) throw cause;
            throw failure;
        }
    }

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    private static boolean hasSurrogate(String input) {
        for (int i = 0; i < input.length(); i++) {
            if (Character.isSurrogate(input.charAt(i))) return true;
        }
        return false;
    }

    private static String escape(String input) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            result.append(String.format("%04x", (int) input.charAt(i)));
        }
        return result.toString();
    }

    private static void pair(Pattern pattern, String input, boolean rolling) throws Exception {
        boolean expected = pattern.matcher(new StringBuilder(input)).matches();
        checks++;
        if (pattern.asMatchPredicate().test(input) != expected) {
            throw new AssertionError("PUBLIC_MISMATCH pattern=" + pattern + " flags=" + pattern.flags()
                    + " input=" + escape(input));
        }
        Object plan = PLAN.get(pattern);
        if (plan != null) {
            int actual = invoke(MATCHES, plan, input);
            int wanted = !candidate && hasSurrogate(input) ? -1 : expected ? 1 : 0;
            checks++;
            if (actual != wanted) {
                throw new AssertionError("PLAN_MISMATCH pattern=" + pattern + " flags=" + pattern.flags()
                        + " input=" + escape(input) + " expected=" + wanted + " actual=" + actual);
            }
            if (actual >= 0) decisions++; else refusals++;
            if (rolling) {
                checks++;
                if (invoke(ROLLING, plan, input) != wanted) {
                    throw new AssertionError("ROLLING_MISMATCH pattern=" + pattern + " input=" + escape(input));
                }
            }
        }
        if (pattern.flags() == 0 && (pairs & 255) == 0) {
            check(Pattern.matches(pattern.pattern(), input) == expected, "STATIC_MISMATCH");
            check(input.matches(pattern.pattern()) == expected, "STRING_MISMATCH");
        }
        DIGEST.update((byte) (expected ? 1 : 0));
        pairs++;
    }

    private static String[] inputs() {
        return new String[] {"", "a", "ab", "123", " ", "\n", "\r\n", "\u0085", "\u2028", "\u2029",
                "\u00e9", "e\u0301", "\u03a9", "\u0627", "\u0915", "\u4e00", "\u200c", "\u200d",
                "\uff11", "\ud800", "\udc00", "\ud800\ud800", "\udc00\udc00", "\udc00\ud800",
                "\ud800\udc00", "\ud801\udc00", "\ud801\udc28", "\ud835\udfce", "\ud840\udc00",
                "\ud83d\ude00", "\udbff\udfff", "a\ud801\udc00b", "\u00e9\ud801\udc28a",
                "a\ud800b\udc00", "\ud800\ud800\udc00\udc00", "\ud83c\uddfa\ud83c\uddf8",
                "\ud83d\udc69\u200d\ud83d\udcbb", "\u0000", "\uffff"};
    }

    private static void witness() throws Exception {
        Pattern pattern = Pattern.compile("\\p{sc=Deseret}*");
        String text = "\ud801\udc00";
        check(pattern.matcher(new StringBuilder(text)).matches(), "ORACLE_WITNESS");
        Object plan = PLAN.get(pattern);
        check(plan != null, "WITNESS_PLAN_MISSING");
        int actual = invoke(MATCHES, plan, text);
        check(actual == 1, "MISSING_SUPPLEMENTARY_DECISION expected=1 actual=" + actual);
        System.out.println("SUPPLEMENTARY_WITNESS_PASS");
    }

    private static void flagsAndProperties() throws Exception {
        String[] expressions = {".*", "a*.*", "\\p{L}*", "\\p{sc=Deseret}*", "\\p{Nd}*", "\\w*",
                "\\s*", "\\d*", "[a-z]*", "\\x{10400}*", "\\p{Cs}*", "\\P{Cs}*"};
        int[] bits = {Pattern.UNIX_LINES, Pattern.CASE_INSENSITIVE, Pattern.COMMENTS, Pattern.MULTILINE,
                Pattern.LITERAL, Pattern.DOTALL, Pattern.UNICODE_CASE, Pattern.CANON_EQ, Pattern.UNICODE_CHARACTER_CLASS};
        String[] values = inputs();
        for (int combination = 0; combination < 1 << bits.length; combination++) {
            int flags = 0;
            for (int bit = 0; bit < bits.length; bit++) if ((combination & (1 << bit)) != 0) flags |= bits[bit];
            for (String expression : expressions) {
                Pattern pattern = Pattern.compile(expression, flags);
                if ((flags & Pattern.CANON_EQ) != 0) check(PLAN.get(pattern) == null, "CANON_EQ_ADMISSION");
                for (String value : values) pair(pattern, value, false);
            }
            flagCombinations++;
        }
        String[] more = {"\\p{IsHan}*", "\\p{InEmoticons}*", "[\\p{L}&&[^\\p{sc=Latin}]]*",
                "\\p{M}*", "\\p{N}*", "\\p{javaJavaIdentifierStart}*\\p{javaJavaIdentifierPart}*",
                "\\p{javaUnicodeIdentifierStart}*\\p{javaUnicodeIdentifierPart}*", "\\p{IsExtended_Pictographic}*",
                "[\\uD800-\\uDFFF]*", "\\uD800*\\uDC00*", "\\h*\\v*", "[^x]*", "\\p{ASCII}*",
                "(?U)\\w*", "(?iu)\\x{10400}*", "(?s).*", "(?d).*", "(?U)\\p{Lower}*",
                "a*(?U)\\w*", "(?U)\\w*(?-U)\\d*", "\\Qpublic class X {\\E.*",
                "\\p{L}*.\\p{N}*", ".\\p{Cs}*.", "\\uD800*.*\\uDC00*"};
        for (String expression : more) {
            Pattern pattern = Pattern.compile(expression);
            for (String value : values) pair(pattern, value, true);
        }
    }

    private static void allUnitsAndPairSeams() throws Exception {
        Pattern[] patterns = {Pattern.compile(".*", Pattern.DOTALL), Pattern.compile("\\p{Cs}*"),
                Pattern.compile("\\p{L}*"), Pattern.compile("[a-z]*")};
        for (int unit = 0; unit <= Character.MAX_VALUE; unit++) {
            String value = Character.toString((char) unit);
            for (Pattern pattern : patterns) pair(pattern, value, false);
        }
        int[] low = {0xdc00, 0xdc01, 0xdc28, 0xddff, 0xde00, 0xdffe, 0xdfff};
        for (int high = 0xd800; high <= 0xdbff; high++) {
            for (int trailing : low) {
                String value = new String(new char[] {(char) high, (char) trailing});
                for (Pattern pattern : patterns) pair(pattern, value, true);
                pair(Pattern.compile("a*.*", Pattern.DOTALL), "aaa" + value + "\ud800", true);
            }
        }
        int[] high = {0xd800, 0xd801, 0xd835, 0xd83d, 0xd840, 0xdbfe, 0xdbff};
        for (int trailing = 0xdc00; trailing <= 0xdfff; trailing++) {
            for (int leading : high) {
                pair(patterns[2], new String(new char[] {(char) leading, (char) trailing}), true);
            }
        }
    }

    private static void scriptRepresentatives() throws Exception {
        int[] bmp = new int[Character.UnicodeScript.values().length];
        int[] supplementary = bmp.clone();
        Arrays.fill(bmp, -1);
        Arrays.fill(supplementary, -1);
        for (int cp = 0; cp <= Character.MAX_CODE_POINT; cp++) {
            int ordinal = Character.UnicodeScript.of(cp).ordinal();
            int[] target = cp <= Character.MAX_VALUE ? bmp : supplementary;
            if (target[ordinal] < 0) target[ordinal] = cp;
        }
        int scripts = 0;
        for (Character.UnicodeScript script : Character.UnicodeScript.values()) {
            Pattern pattern = Pattern.compile("\\p{sc=" + script.name() + "}*");
            for (int cp : new int[] {bmp[script.ordinal()], supplementary[script.ordinal()]}) {
                if (cp >= 0) {
                    pair(pattern, Character.toString(cp), true);
                    pair(pattern, "a" + Character.toString(cp), true);
                }
            }
            scripts++;
        }
        System.out.println("SCRIPT_REPRESENTATIVES scripts=" + scripts + " exhaustiveCodePointClassification=true");
    }

    private static void largePlans() throws Exception {
        for (int count : new int[] {1, 2, 31, 32, 33, 62, 63, 64, 65, 127, 128, 129, 255, 256}) {
            Pattern pattern = Pattern.compile(".*".repeat(count), Pattern.DOTALL);
            Object plan = PLAN.get(pattern);
            check(plan != null, "PLAN_BUDGET " + count);
            long[] masks = ((long[]) ASCII.get(plan)).clone();
            for (String value : inputs()) {
                pair(pattern, value, true);
                pair(pattern, "a".repeat(79) + value + "z", true);
            }
            check(Arrays.equals(masks, (long[]) ASCII.get(plan)), "MUTATED_ASCII_MASKS");
            if (count > 1) {
                Pattern boundary = Pattern.compile("a*".repeat(count - 1) + "\\p{L}*");
                for (String value : List.of("", "a\ud801\udc00", "\ud801\udc00a", "\u00e9\ud801\udc28", "\ud83d\ude00", "\ud800")) {
                    pair(boundary, value, true);
                }
            }
        }
        check(PLAN.get(Pattern.compile(".*".repeat(257))) == null, "PLAN_BOUND_CHANGED");
        Pattern longInput = Pattern.compile("a*\\p{L}*\\p{Nd}*");
        pair(longInput, "a".repeat(8193) + "\ud801\udc00".repeat(8193) + "\ud835\udfce".repeat(100), true);
        pair(longInput, "\ud801\udc00".repeat(8193) + "\ud800", true);
    }

    private static Object predicate(boolean bmp, IntPredicate test) {
        Class<?> type = bmp ? BMP_PREDICATE : PREDICATE;
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            if (method.getName().equals("is")) return test.test((Integer) args[0]);
            throw new UnsupportedOperationException(method.toString());
        });
    }

    private static Object atom(boolean bmpNode, boolean bmpPredicate, boolean repeat, IntPredicate test) throws Exception {
        Object property = (bmpNode ? BMP_PROPERTY : PROPERTY).newInstance(predicate(bmpPredicate, test));
        return repeat ? (bmpNode ? BMP_GREEDY : GREEDY).newInstance(property, 0) : property;
    }

    /** Tests real JDK node match methods even when a BMP predicate can accept a surrogate. */
    private static void syntheticNodeConsumption() throws Exception {
        IntPredicate high = cp -> cp >= 0xd800 && cp <= 0xdbff;
        IntPredicate low = cp -> cp >= 0xdc00 && cp <= 0xdfff;
        IntPredicate any = cp -> true;
        for (boolean firstBmp : new boolean[] {false, true}) {
            for (boolean secondBmp : new boolean[] {false, true}) {
                for (boolean firstRepeat : new boolean[] {false, true}) {
                    for (boolean secondRepeat : new boolean[] {false, true}) {
                        if (!firstRepeat && !secondRepeat) continue;
                        for (IntPredicate firstTest : List.of(high, any)) {
                            Object first = atom(firstBmp, true, firstRepeat, firstTest);
                            Object second = atom(secondBmp, true, secondRepeat, low);
                            NEXT.set(first, second);
                            NEXT.set(second, ACCEPT);
                            Object plan = DERIVE.invoke(null, first);
                            check(plan != null, "SYNTHETIC_PLAN_MISSING");
                            Pattern oracle = Pattern.compile("a*");
                            ROOT.set(oracle, first);
                            for (String input : List.of("", "\ud800", "\udc00", "\ud800\udc00",
                                    "\ud800\ud800\udc00", "\ud800\udc00\udc00", "\ud800\udc00\ud800\udc00")) {
                                boolean expected = oracle.matcher(new StringBuilder(input)).matches();
                                int actual = invoke(MATCHES, plan, input);
                                int wanted = !candidate && hasSurrogate(input) ? -1 : expected ? 1 : 0;
                                check(actual == wanted, "NODE_CONSUMPTION_MISMATCH firstBmp=" + firstBmp
                                        + " secondBmp=" + secondBmp + " firstRepeat=" + firstRepeat
                                        + " secondRepeat=" + secondRepeat + " input=" + escape(input)
                                        + " expected=" + wanted + " actual=" + actual);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void generatedAndCodePayloads() throws Exception {
        Random random = new Random(0x5531364350L);
        String[] atoms = {"a", "a*", ".", ".*", "\\p{L}*", "\\p{Nd}*", "[a-z]*", "[^x]*",
                "\\p{Cs}*", "\\uD800*", "\\uDC00*", "\\x{10400}*", "\\w*", "\\p{M}*"};
        String[] values = inputs();
        for (int sample = 0; sample < 4096; sample++) {
            StringBuilder expression = new StringBuilder();
            for (int i = 0, n = 1 + random.nextInt(6); i < n; i++) expression.append(atoms[random.nextInt(atoms.length)]);
            StringBuilder input = new StringBuilder();
            for (int i = 0, n = random.nextInt(5); i < n; i++) input.append(values[random.nextInt(values.length)]);
            int flags = random.nextBoolean() ? Pattern.UNICODE_CHARACTER_CLASS | Pattern.DOTALL : 0;
            pair(Pattern.compile(expression.toString(), flags), input.toString(), true);
        }
        String[] code = {
                "class \ud801\udc00 { String regex = \"\\\\p{L}*\"; /* (?U)\\w* */ }",
                "# Python: (?s).*\n\u03bb = 'class X { \\uD800 }'\n",
                "const \u53d8\u91cf = /\\p{Script=Han}*/u; // \ud83d\ude00\n",
                "fn main() { let s = r#\"(?U)\\w*\"#; /* \u0627 */ }",
                "\"\"\"\nnew String(new char[]{'\\uD800', '\\uDC00'})\n\"\"\"",
                "SELECT '\ud800' /* class Fake { } */ FROM t WHERE x REGEXP '.*';"
        };
        for (String payload : code) {
            for (String expression : List.of(".*", "\\p{L}*", "[^\\r\\n]*", "(?s).*", "(?U)\\w*", "\\Qclass\\E.*")) {
                pair(Pattern.compile(expression), payload, true);
            }
        }
    }

    private static final class TraceSequence implements CharSequence {
        private final StringBuilder value;
        private final StringBuilder trace = new StringBuilder();
        private final boolean mutate;
        private int reads;
        TraceSequence(String value, boolean mutate) { this.value = new StringBuilder(value); this.mutate = mutate; }
        @Override public int length() { trace.append('L'); return value.length(); }
        @Override public char charAt(int index) {
            trace.append('C').append(index).append(';');
            if (mutate && ++reads == 2 && value.length() > 1) value.setCharAt(1, 'x');
            return value.charAt(index);
        }
        @Override public CharSequence subSequence(int start, int end) {
            trace.append('S').append(start).append(',').append(end).append(';');
            return value.subSequence(start, end);
        }
        @Override public String toString() { trace.append('T'); return value.toString(); }
    }

    private static void publicContracts() throws Exception {
        for (String expression : List.of("a*", ".*", "\\p{L}*", "\\p{Cs}*")) {
            for (boolean mutate : new boolean[] {false, true}) {
                TraceSequence expected = new TraceSequence("a\ud801\udc00b", mutate);
                TraceSequence actual = new TraceSequence("a\ud801\udc00b", mutate);
                boolean wanted = Pattern.compile(expression).matcher(expected).matches();
                check(Pattern.matches(expression, actual) == wanted, "CUSTOM_SEQUENCE_RESULT");
                check(expected.trace.toString().equals(actual.trace.toString()), "CUSTOM_SEQUENCE_READS");
            }
        }
        for (String expression : List.of("a+", "a?", "a*?", "a*+", "(a*)", "a|b", "(?=a)a*",
                "(a)\\1", "a*$", "^a*", "\\R*", "\\X*", "\\b.*", "\\b{g}.*")) {
            Pattern pattern = Pattern.compile(expression);
            check(PLAN.get(pattern) == null, "ADMISSION_WIDENED " + expression);
            for (String input : inputs()) pair(pattern, input, false);
        }
        Pattern p = Pattern.compile("\\p{L}*.*", Pattern.UNICODE_CHARACTER_CLASS);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(p); }
        Pattern restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (Pattern) in.readObject();
        }
        check(PLAN.get(restored) == null, "DESERIALIZED_EAGER_PLAN");
        for (String input : inputs()) pair(restored, input, true);
        for (String expression : List.of("[", "a{3,2}", "\\p{NoSuchProperty}", "(?q).*")) {
            try { Pattern.matches(expression, null); throw new AssertionError("SYNTAX_PRECEDENCE"); }
            catch (PatternSyntaxException expected) { checks++; }
        }
        try { Pattern.matches(".*", null); throw new AssertionError("NULL_SEQUENCE"); }
        catch (NullPointerException expected) { checks++; }
        try { p.asMatchPredicate().test(null); throw new AssertionError("NULL_PREDICATE"); }
        catch (NullPointerException expected) { checks++; }
        for (boolean transparent : new boolean[] {false, true}) {
            for (boolean anchoring : new boolean[] {false, true}) {
                Matcher m = Pattern.compile(".").matcher("a\ud801\udc00b");
                m.region(1, 2).useTransparentBounds(transparent).useAnchoringBounds(anchoring);
                check(!m.matches(), "MATCHER_REGION_HIGH_HALF");
                check(m.hitEnd(), "MATCHER_REGION_HIT_END");
            }
        }
    }

    private static void concurrency() throws Exception {
        Pattern pattern = Pattern.compile("\\p{L}*\\p{Nd}*", Pattern.UNICODE_CHARACTER_CLASS);
        Object plan = PLAN.get(pattern);
        long[] before = ((long[]) ASCII.get(plan)).clone();
        String[] values = inputs();
        List<Callable<Long>> jobs = new ArrayList<>();
        for (int worker = 0; worker < 4; worker++) {
            final int seed = worker;
            jobs.add(() -> {
                Random random = new Random(seed);
                long comparisons = 0;
                for (int i = 0; i < 1024; i++) {
                    String value = values[random.nextInt(values.length)];
                    boolean expected = pattern.matcher(new StringBuilder(value)).matches();
                    if (pattern.asMatchPredicate().test(value) != expected) throw new AssertionError("CONCURRENT_PUBLIC");
                    int wanted = !candidate && hasSurrogate(value) ? -1 : expected ? 1 : 0;
                    if (invoke(MATCHES, plan, value) != wanted) throw new AssertionError("CONCURRENT_PLAN");
                    comparisons++;
                }
                return comparisons;
            });
        }
        long comparisons = 0;
        try (var executor = Executors.newFixedThreadPool(4)) {
            for (var result : executor.invokeAll(jobs)) comparisons += result.get();
        }
        check(comparisons == 4096, "CONCURRENT_COVERAGE");
        check(PLAN.get(pattern) == plan, "PLAN_IDENTITY_CHANGED");
        check(Arrays.equals(before, (long[]) ASCII.get(plan)), "CONCURRENT_MASK_MUTATION");
        System.out.println("CONCURRENT_COMPARISONS=" + comparisons);
    }

    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "candidate" : args[0];
        candidate = !mode.equals("baseline");
        if (mode.equals("witness")) { witness(); return; }
        if (candidate) witness();
        flagsAndProperties();
        allUnitsAndPairSeams();
        scriptRepresentatives();
        largePlans();
        syntheticNodeConsumption();
        generatedAndCodePayloads();
        publicContracts();
        concurrency();
        System.out.println("UNICODE_PASS mode=" + mode + " pairs=" + pairs + " checks=" + checks
                + " decisions=" + decisions + " refusals=" + refusals + " flagCombinations=" + flagCombinations
                + " semanticDigest=" + HexFormat.of().formatHex(DIGEST.digest()));
    }
}
