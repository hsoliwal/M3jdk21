/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2.
 */

/*
 * @test
 * @summary Canonical M3 literal regex replacement, fallback semantics, and in-memory Java source edits
 * @library /test/lib
 * @modules java.base/java.lang:open jdk.compiler jdk.management
 * @build jdk.test.lib.compiler.InMemoryJavaCompiler jdk.test.lib.ByteCodeLoader
 * @run main/othervm -Xmx128m -XX:ActiveProcessorCount=2 --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringRegexReplacementTest
 */

import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jdk.test.lib.ByteCodeLoader;
import jdk.test.lib.compiler.InMemoryJavaCompiler;

public class M3StringRegexReplacementTest {
    private static Field stringM3;
    private static Field owner;
    private static Field coordinate;
    private static Method literalGuard;
    private static Method prepare;
    private static AtomicReferenceArray<?> cache;
    private static int checks;
    private static int compiledProjects;

    public static void main(String[] args) throws Exception {
        try {
            requireCurrentReceiver();
        } catch (Exception | AssertionError failure) {
            throw new AssertionError("MATCHED_M3_RUNTIME_REQUIRED: current String.m3/M3String owner+value"
                    + " receiver with UseM3StringStorage enabled", failure);
        }
        preparedPlanReuse();
        canonicalComposition();
        literalMatrix();
        asciiLocaleMapping();
        fallbackAndExceptions();
        preparedBoundRefusals();
        compilerProjects();
        System.out.println("M3_STRING_REGEX_REPLACEMENT_PASS|checks=" + checks
                + "|compiledProjects=" + compiledProjects);
    }

    private static void requireCurrentReceiver() throws Exception {
        HotSpotDiagnosticMXBean vm = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        check("true".equals(vm.getVMOption("UseM3StringStorage").getValue()),
                "matched receiver must enable UseM3StringStorage");
        Class<?> m3 = Class.forName("java.lang.M3String");
        stringM3 = field(String.class, "m3");
        owner = field(m3, "owner");
        coordinate = field(m3, "value");
        check(stringM3.getType() == m3, "String must own the current M3String descriptor");
        check(owner.getType().getName().equals("java.lang.M3StringOwner"), "canonical owner type");
        check(coordinate.getType() == long.class, "packed long coordinate");
        int instanceFields = 0;
        for (Field declared : m3.getDeclaredFields()) {
            if (!Modifier.isStatic(declared.getModifiers())) {
                instanceFields++;
                check(declared.getName().equals("owner") || declared.getName().equals("value"),
                        "M3String must retain only owner and coordinate");
            }
        }
        check(instanceFields == 2, "current two-field M3String representation");
        literalGuard = m3.getDeclaredMethod("isLiteralRegexReplacement", String.class, String.class);
        literalGuard.setAccessible(true);
        Class<?> precompute = Class.forName("java.lang.M3StringSearchPrecompute");
        prepare = precompute.getDeclaredMethod("prepare", m3);
        prepare.setAccessible(true);
        cache = (AtomicReferenceArray<?>) field(precompute, "CACHE").get(null);
        check(cache.length() == 256, "existing bounded prepared-plan cache");
        body(fresh("receiver admission"));
    }

    private static void preparedPlanReuse() throws Exception {
        String regex = fresh("m3WarmRegexPlanToken");
        String source = fresh("left-m3WarmRegex").concat(fresh("PlanToken-middle-"))
                .concat(regex).concat(fresh("-tail"));
        Object pattern = body(regex);
        check(cachedPlan(pattern) == null, "cold route witness must start without a prepared plan");
        String first = source.replaceFirst(regex, "Q");
        Object plan = cachedPlan(pattern);
        check(plan != null, "actual replaceFirst must prepare the existing search plan");
        String all = source.replaceAll(regex, "Q");
        check(cachedPlan(pattern) == plan, "actual replaceAll must reuse the same owner/range plan");
        check(prepare.invoke(null, pattern) == plan, "ordinary prepare must return the route's plan");
        check(field(plan.getClass(), "patternLength").getInt(plan) == regex.length(), "plan length");
        check(((int[]) field(plan.getClass(), "prefix").get(plan)).length == regex.length(),
                "bounded forward metadata");
        check(((int[]) field(plan.getClass(), "reversePrefix").get(plan)).length == regex.length(),
                "bounded reverse metadata");
        check(((int[]) field(plan.getClass(), "skip256").get(plan)).length == 256,
                "bounded skip metadata");
        equal(oracle(source, regex, "Q", true), first, "warm first content");
        equal(oracle(source, regex, "Q", false), all, "warm all content");
    }

    private static Object cachedPlan(Object pattern) throws Exception {
        Object expectedOwner = owner.get(pattern);
        long expectedCoordinate = coordinate.getLong(pattern);
        for (int index = 0; index < cache.length(); index++) {
            Object entry = cache.get(index);
            if (entry != null
                    && ((Reference<?>) field(entry.getClass(), "owner").get(entry)).get() == expectedOwner
                    && field(entry.getClass(), "coordinate").getLong(entry) == expectedCoordinate) {
                return field(entry.getClass(), "plan").get(entry);
            }
        }
        return null;
    }

    private static void canonicalComposition() throws Exception {
        String source = fresh("x").concat(fresh("ab")).concat(fresh("c"))
                .concat(fresh("yab")).concat(fresh("c")).concat(fresh("z"));
        check(tuple(source), "source must cross canonical atom seams");
        String first = source.replaceFirst("abc", "Q");
        String expectedFirst = source.substring(0, 1).concat(fresh("Q")).concat(source.substring(4));
        sameCoordinate(expectedFirst, first, "first preserves prefix/replacement/suffix coordinates");
        String all = source.replaceAll("abc", "Q");
        String expectedAll = source.substring(0, 1).concat(fresh("Q"))
                .concat(source.substring(4, 5)).concat(fresh("Q")).concat(source.substring(8));
        sameCoordinate(expectedAll, all, "all preserves each surviving source range");
        check(tuple(first) && tuple(all), "replacement results remain canonical compositions");
        String fullMatch = fresh("abc");
        String unchangedSpelling = fullMatch.replaceFirst("abc", "abc");
        check(unchangedSpelling != fullMatch, "matched replacement produces a wrapper even for equal spelling");
        sameCoordinate(fullMatch, unchangedSpelling, "equal-spelling match can reuse canonical storage");
    }

    private static void literalMatrix() throws Exception {
        String joined = fresh("a").concat(fresh("bcab")).concat(fresh("caaaaa"))
                .concat(fresh("\u03b2abc")).concat(fresh("\ud83d"))
                .concat(fresh("\ude42abc"));
        String framed = fresh("<<").concat(joined).concat(fresh(">>"));
        String sliced = framed.substring(2, framed.length() - 2);
        check(tuple(joined) && tuple(sliced), "joined and sliced receivers retain composition owners");
        check(owner.get(body(framed)) == owner.get(body(sliced)), "slice preserves its canonical owner");
        String[] receivers = {joined, sliced, fresh("aaaaa"), fresh("")};
        String[] regexes = {"abc", "aa", "a", "\u03b2abc", "missing"};
        String[] replacements = {"Q", "", "XYZ", "\u03b2", "\ud83d\ude42"};
        for (String source : receivers) {
            if (!source.isEmpty()) body(source);
            for (String regex : regexes) {
                for (String replacement : replacements) {
                    check(eligible(regex, replacement), "BMP literal and literal replacement must be admitted");
                    sameOutcome(source, regex, replacement, true, "literal first");
                    sameOutcome(source, regex, replacement, false, "literal all");
                }
            }
            check(source.replaceFirst("missing", "Q") == source, "first no-match identity");
            check(source.replaceAll("missing", "Q") == source, "all no-match identity");
        }
        equal("bba", fresh("aaaaa").replaceAll("aa", "b"), "non-overlapping all matches");
        equal("baaa", fresh("aaaaa").replaceFirst("aa", "b"), "first consumes one non-overlapping match");
    }

    private static void asciiLocaleMapping() throws Exception {
        String mixed = fresh("MiXeD");
        equal("mixed", mixed.toLowerCase(Locale.ROOT), "ASCII lower case uses canonical M3 mapping");
        equal("MIXED", mixed.toUpperCase(Locale.ROOT), "ASCII upper case uses canonical M3 mapping");

        String turkishInput = fresh("I");
        Locale turkish = Locale.forLanguageTag("tr");
        equal("\u0131", turkishInput.toLowerCase(turkish),
                "Turkish lower case remains locale-sensitive");
        equal("I", turkishInput.toUpperCase(turkish),
                "Turkish upper case remains locale-sensitive");
        check(tuple(mixed) && tuple(turkishInput),
                "case discriminator receivers remain canonical M3 compositions");
    }

    private static void fallbackAndExceptions() throws Exception {
        String source = fresh("abc").concat(fresh("\ud83d")).concat(fresh("\ude42abc"))
                .concat(fresh("\ud83dX\ude42"));
        String[] regexes = {"", ".", "a+", "(abc)", "\\Qabc\\E",
                "\ud83d", "\ude42", "\ud83d\ude42"};
        for (String regex : regexes) {
            check(!eligible(regex, "Q"), "syntax, empty, and surrogate patterns must stay with Matcher");
            for (String replacement : new String[] {"Q", "", "$0", "\\$"}) {
                sameOutcome(source, regex, replacement, true, "fallback first");
                sameOutcome(source, regex, replacement, false, "fallback all");
            }
        }
        for (String replacement : new String[] {"$", "\\", "$9"}) {
            check(!eligible("abc", replacement), "replacement syntax must stay with Matcher");
            for (String regex : new String[] {"abc", "missing"}) {
                sameOutcome(source, regex, replacement, true, "replacement syntax first");
                sameOutcome(source, regex, replacement, false, "replacement syntax all");
            }
        }
        for (String regex : new String[] {null, "[", "(", "abc", "missing", ""}) {
            check(!eligible(regex, null), "null replacement is never fast-path eligible");
            sameOutcome(source, regex, null, true, "null/invalid ordering first");
            sameOutcome(source, regex, null, false, "null/invalid ordering all");
        }
        sameOutcome(source, null, "Q", true, "null regex first");
        sameOutcome(source, null, "Q", false, "null regex all");
    }

    private static void preparedBoundRefusals() throws Exception {
        check(prepare.invoke(null, body(fresh("x"))) == null, "one-unit pattern uses existing direct search");
        String large = fresh("x".repeat(8193));
        check(prepare.invoke(null, body(large)) == null, "existing 8192-unit plan ceiling is preserved");
        String source = large.concat(fresh("Z"));
        equal("QZ", source.replaceFirst(large, "Q"), "first exact fallback above plan ceiling");
        equal("QZ", source.replaceAll(large, "Q"), "all exact fallback above plan ceiling");
    }

    private static void compilerProjects() throws Exception {
        int caseIndex = 0;
        for (String replacement : new String[] {"EDIT", ""}) {
            for (boolean first : new boolean[] {true, false}) {
                String name = "M3EditedDummy" + caseIndex++;
                String raw = "public class " + name + " {\n"
                        + "  // TOKEN in a comment\n"
                        + "  public static String value() {\n"
                        + "    String word = \"TOKEN\";\n"
                        + "    String block = \"\"\"\n"
                        + "        TOKEN in text\n"
                        + "        \"\"\";\n"
                        + "    return word + \":\" + block.strip();\n"
                        + "  }\n}\n";
                int seam = raw.indexOf("TOKEN") + 2;
                String source = fresh(raw.substring(0, seam)).concat(fresh(raw.substring(seam)));
                check(tuple(source), "Java source target crosses an atom seam");
                String expectedSource = oracle(source, "TOKEN", replacement, first);
                String actualSource = replace(source, "TOKEN", replacement, first);
                equal(expectedSource, actualSource, "code/comment/text-block source edit");
                String expectedValue = compileValue(name, expectedSource);
                String actualValue = compileValue(name, actualSource);
                equal(expectedValue, actualValue, "compiler-observed edited program behavior");
                equal(first ? "TOKEN:TOKEN in text" : replacement + ":" + (replacement + " in text").strip(),
                        actualValue, "independent observable program value");
            }
        }
        check(compiledProjects == 8, "finite four-case, two-oracle compiler corpus");
    }

    private static String compileValue(String name, String source) throws Exception {
        byte[] bytes = InMemoryJavaCompiler.compile(name, source,
                "--release", "21", "-proc:none", "-Xlint:all", "-Werror");
        Class<?> project = ByteCodeLoader.load(name, bytes);
        Method value = project.getDeclaredMethod("value");
        check(value.getReturnType() == String.class && Modifier.isPublic(value.getModifiers())
                        && Modifier.isStatic(value.getModifiers()), "dummy public API remains unchanged");
        compiledProjects++;
        return (String) value.invoke(null);
    }

    private static void sameOutcome(String source, String regex, String replacement,
                                    boolean first, String label) throws Exception {
        String expected = null;
        RuntimeException expectedFailure = null;
        try {
            expected = oracle(source, regex, replacement, first);
        } catch (RuntimeException failure) {
            expectedFailure = failure;
        }
        String actual = null;
        RuntimeException actualFailure = null;
        try {
            actual = replace(source, regex, replacement, first);
        } catch (RuntimeException failure) {
            actualFailure = failure;
        }
        if (expectedFailure == null) {
            check(actualFailure == null, label + " unexpected exception=" + actualFailure);
            equal(expected, actual, label);
            if (stringM3.get(source) != null && eligible(regex, replacement)) body(actual);
        } else {
            check(actualFailure != null && actualFailure.getClass() == expectedFailure.getClass(),
                    label + " expected exception=" + expectedFailure + " actual=" + actualFailure);
            check(Objects.equals(expectedFailure.getMessage(), actualFailure.getMessage()),
                    label + " exception diagnostic/order changed");
        }
    }

    private static String oracle(String source, String regex, String replacement, boolean first) {
        Matcher matcher = Pattern.compile(regex).matcher(source);
        return first ? matcher.replaceFirst(replacement) : matcher.replaceAll(replacement);
    }

    private static String replace(String source, String regex, String replacement, boolean first) {
        return first ? source.replaceFirst(regex, replacement) : source.replaceAll(regex, replacement);
    }

    private static boolean eligible(String regex, String replacement) throws Exception {
        return (Boolean) literalGuard.invoke(null, regex, replacement);
    }

    private static String fresh(String value) {
        return new String(value.toCharArray());
    }

    private static Object body(String value) throws Exception {
        value.length();
        Object body = stringM3.get(value);
        check(body != null, "String must be admitted to current M3 storage");
        return body;
    }

    private static boolean tuple(String value) throws Exception {
        return owner.get(body(value)).getClass().getName().equals("java.lang.M3StringTuple");
    }

    private static void sameCoordinate(String expected, String actual, String label) throws Exception {
        equal(expected, actual, label);
        Object left = body(expected);
        Object right = body(actual);
        check(owner.get(left) == owner.get(right) && coordinate.getLong(left) == coordinate.getLong(right), label);
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field result = type.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static void equal(String expected, String actual, String label) {
        check(Arrays.equals(expected.toCharArray(), actual.toCharArray()), label);
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
