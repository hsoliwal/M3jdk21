/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Reuse immutable input facts without changing Matcher state or mutable-input semantics
 * @modules java.base/java.util.regex:open
 *          java.base/java.lang:open
 *          java.base/jdk.internal.access
 *          jdk.management
 * @run main/othervm -Xcheck:jni M3RegexReuseTest
 * @run main/othervm -Xint -Xcheck:jni M3RegexReuseTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Xcheck:jni M3RegexReuseTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage -Xcheck:jni M3RegexReuseTest flat
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Xcheck:jni M3RegexReuseTest prepared
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings -Xcheck:jni M3RegexReuseTest prepared
 */

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3RegexReuseTest {
    private static long checks;
    private static long cells;
    private static final MessageDigest DIGEST = digest();

    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "all" : args[0];
        if (mode.equals("cost")) { cost(); return; }
        if (mode.equals("prepared")) { prepared(); return; }
        if (mode.equals("flat")) { flat(); return; }
        if (!mode.equals("reuse")) semantic();
        if (!mode.equals("semantic")) reuse();
    }

    private static void semantic() {
        List<String> texts = new ArrayList<>();
        enumerate(texts, "", 3);
        texts.addAll(List.of("return (a+b)*31;", "Pattern.compile(\"a+b?\")", "/* x */\nclass X {}",
                "\ud83d\ude42", "x\ud83dy", "\ude42x", "\u0085\u2028\u2029", "abc".repeat(100),
                "abc".repeat(10_922) + "XY", "abc".repeat(10_922) + "XYZ"));
        List<Pattern> patterns = new ArrayList<>();
        for (String literal : List.of("", "a", "ab", "abc", "acb", "\ud83d\ude42x", "b\na", "return (a+b)*31;")) {
            for (int flags : new int[] {Pattern.LITERAL, Pattern.LITERAL | Pattern.CASE_INSENSITIVE,
                    Pattern.LITERAL | Pattern.UNICODE_CASE | Pattern.CANON_EQ}) {
                patterns.add(Pattern.compile(literal, flags));
            }
        }
        for (String regex : List.of("a*", "(?<x>a)?(b)", "^.*$", "(?<=a)b", "(a)\\1", "\\R")) {
            patterns.add(Pattern.compile(regex));
        }
        check((long) texts.size() * patterns.size() <= 13_000, "frozen Cartesian budget");
        for (String text : texts) {
            for (Pattern pattern : patterns) {
                cells++;
                Matcher left = pattern.matcher(text);
                Matcher right = pattern.matcher(new StringBuilder(text));
                for (int i = 0; i < 3; i++) same(left.find(), right.find(), left, right);
                left.reset(); right.reset();
                same(left.find(), right.find(), left, right);
                same(left.find(0), right.find(0), left, right);
                int end = Math.min(4, text.length());
                for (int i = 0; i < 2; i++) {
                    left.region(0, end); right.region(0, end);
                    same(left.find(), right.find(), left, right);
                }
                int from = text.length() / 2;
                left.region(from, text.length()); right.region(from, text.length());
                left.useTransparentBounds(true).useAnchoringBounds(false);
                right.useTransparentBounds(true).useAnchoringBounds(false);
                same(left.find(), right.find(), left, right);
                same(left.matches(), right.matches(), left, right);
                same(left.lookingAt(), right.lookingAt(), left, right);
                Pattern other = Pattern.compile("abc", Pattern.LITERAL);
                left.usePattern(other); right.usePattern(other);
                same(left.find(), right.find(), left, right);
                left.reset(text); right.reset(new StringBuilder(text));
                same(left.find(), right.find(), left, right);
                left.reset(new String(text)); right.reset(new StringBuilder(text));
                same(left.find(), right.find(), left, right);
                String replacement = "abcXYZ";
                left.reset(replacement); right.reset(new StringBuilder(replacement));
                same(left.find(), right.find(), left, right);
                left.reset("acb"); right.reset(new StringBuilder("acb"));
                same(left.find(), right.find(), left, right);
                left.reset("abc"); right.reset(new StringBuilder("abc"));
                same(left.find(), right.find(), left, right);
                StringBuilder mutable = new StringBuilder("acb");
                left.reset(mutable); right.reset(new StringBuilder("acb"));
                same(left.find(), right.find(), left, right);
                mutable.replace(0, 3, "abc");
                left.reset(mutable); right.reset(new StringBuilder("abc"));
                same(left.find(), right.find(), left, right);
                check(left.replaceAll("[$0]").equals(right.replaceAll("[$0]")), "replacement");
            }
        }
        System.out.println("MR_SEMANTIC\tcells=" + cells + "\tchecks=" + checks
                + "\tsha256=" + HexFormat.of().formatHex(DIGEST.digest()));
    }

    private static void same(boolean left, boolean right, Matcher candidate, Matcher oracle) {
        check(left == right, "result");
        check(candidate.hitEnd() == oracle.hitEnd(), "hitEnd");
        check(candidate.requireEnd() == oracle.requireEnd(), "requireEnd");
        check(candidate.hasMatch() == oracle.hasMatch(), "hasMatch");
        check(candidate.regionStart() == oracle.regionStart(), "regionStart");
        check(candidate.regionEnd() == oracle.regionEnd(), "regionEnd");
        frame(Boolean.toString(left));
        frame(candidate.hitEnd() + ":" + candidate.requireEnd());
        if (left) {
            check(candidate.groupCount() == oracle.groupCount(), "groupCount");
            for (int group = 0; group <= candidate.groupCount(); group++) {
                check(candidate.start(group) == oracle.start(group), "start");
                check(candidate.end(group) == oracle.end(group), "end");
                check(java.util.Objects.equals(candidate.group(group), oracle.group(group)), "group");
                frame(candidate.start(group) + ":" + candidate.end(group));
                frame(candidate.group(group));
            }
            var snapshot = candidate.toMatchResult();
            check(snapshot.start() == candidate.start(), "snapshot");
        } else {
            check(failure(candidate::start).equals(failure(oracle::start)), "failed start exception");
        }
    }

    private static void reuse() throws Exception {
        Field facts = Matcher.class.getDeclaredField("m3TqFacts");
        facts.setAccessible(true);
        // A distinct whole-owner UTF-16 fixture cannot inherit semantic()'s prepared facts.
        char[] coldUnits = new char[305];
        for (int i = 0; i < 303; i++) coldUnits[i] = "abc".charAt(i % 3);
        coldUnits[303] = '\u0a17';
        coldUnits[304] = '\u4b91';
        String text = new String(coldUnits);
        Matcher matcher = Pattern.compile("acb", Pattern.LITERAL).matcher(text);
        check(!matcher.find(), "cold rejection");
        Object cold = facts.get(matcher);
        check(cold != null, "cold facts");
        matcher.reset();
        check(facts.get(matcher) == cold, "plain reset retains exact immutable facts");
        check(!matcher.find(), "warm rejection");
        check(facts.get(matcher) == cold, "warm search skips precompute");
        matcher.reset(text);
        check(facts.get(matcher) == cold, "same input reset retains facts");
        matcher.region(0, text.length());
        check(!matcher.find() && facts.get(matcher) == cold, "same region retains facts");
        matcher.usePattern(Pattern.compile("abc", Pattern.LITERAL));
        check(matcher.find() && facts.get(matcher) == cold, "facts independent of literal query");
        check(matcher.find(0) && facts.get(matcher) == cold, "find(start) reuse");
        matcher.region(1, 10);
        matcher.find();
        Object range = facts.get(matcher);
        check(range != cold && range != null, "changed region recomputes");
        matcher.region(1, 10); matcher.find();
        check(facts.get(matcher) == range, "repeated region reuses");
        check(!failure(() -> matcher.region(-1, 10)).isEmpty(), "bad region rejected");
        check(facts.get(matcher) == range, "bad region leaves cache alone");
        matcher.reset(new String(text));
        check(facts.get(matcher) == null, "equal different input invalidates without another text owner");
        matcher.find();
        Object replacement = facts.get(matcher);
        check(replacement != range, "replacement computed");
        StringBuilder mutable = new StringBuilder("acb");
        matcher.reset(mutable);
        check(facts.get(matcher) == null && !matcher.find(), "mutable input bypass");
        mutable.replace(0, 3, "abc");
        matcher.reset(mutable);
        check(matcher.find() && facts.get(matcher) == null, "same mutable input observes changes");
        matcher.reset(text); matcher.find();
        check(facts.get(matcher) != null, "reseed");
        check(failure(() -> matcher.reset(null)).equals("java.lang.NullPointerException"), "null reset contract");
        check(facts.get(matcher) == null, "failed new-input reset releases old facts");
        matcher.reset("abc".repeat(10_923)); matcher.find();
        check(facts.get(matcher) == null, "oversize region bypass");
        System.out.println("MR_REUSE\tchecks=" + checks);
    }


    private static void flat() {
        var access = jdk.internal.access.SharedSecrets.getJavaLangAccess();
        check(access != null, "JavaLangAccess initialized");
        check(access.stringM3TrigramFacts(null) == null, "null facts");
        for (String value : List.of("", "abc", new String(new char[] {'x', '\u0100'}))) {
            check(access.stringM3TrigramFacts(value) == null, "disabled M3 has no prepared facts");
        }
        System.out.println("MR_FLAT_FACTS\tchecks=" + checks);
    }

    private static void prepared() throws Exception {
        var access = jdk.internal.access.SharedSecrets.getJavaLangAccess();
        check(access != null, "JavaLangAccess initialized");
        check(access.stringM3TrigramFacts(null) == null, "null has no prepared facts");
        Field storageField = String.class.getDeclaredField("m3");
        storageField.setAccessible(true);
        Field cache = Matcher.class.getDeclaredField("m3TqFacts");
        cache.setAccessible(true);
        Field cacheFrom = Matcher.class.getDeclaredField("m3TqFactsFrom");
        Field cacheTo = Matcher.class.getDeclaredField("m3TqFactsTo");
        cacheFrom.setAccessible(true);
        cacheTo.setAccessible(true);

        char[] units = new char[305];
        for (int i = 0; i < 303; i++) units[i] = "abc".charAt(i % 3);
        units[303] = '\u3191';
        units[304] = '\u7101';
        String cold = new String(units);
        Object storage = storageField.get(cold);
        check(storage != null, "prepared mode requires M3-backed String");
        var peek = storage.getClass().getDeclaredMethod("factsIfPrepared");
        var prepare = storage.getClass().getDeclaredMethod("facts");
        peek.setAccessible(true);
        prepare.setAccessible(true);
        check(peek.invoke(storage) == null, "fresh facts are cold");
        check(access.stringM3TrigramFacts(cold) == null, "lookup does not prepare");
        check(access.stringM3TrigramFacts(cold) == null, "repeated lookup does not prepare");
        check(peek.invoke(storage) == null, "cold lookup leaves source unchanged");

        Pattern missing = Pattern.compile("acb", Pattern.LITERAL);
        Matcher prior = missing.matcher(cold);
        boolean coldResult = prior.find();
        Object originalCache = cache.get(prior);
        check(originalCache != null, "cold source retains regional preparation");
        check(peek.invoke(storage) == null, "regional preparation does not prepare String facts");
        Matcher priorOracle = missing.matcher(new StringBuilder(cold));
        same(coldResult, priorOracle.find(), prior, priorOracle);
        Object preparedFacts = prepare.invoke(storage);
        var predicate = access.stringM3TrigramFacts(cold);
        check(predicate == preparedFacts, "accessor reuses exact immutable facts");
        check(access.stringM3TrigramFacts(cold) == predicate, "stable prepared identity");
        check(!predicate.test(packed("acb", 0)), "negative fixture precondition");
        check(predicate.test(-1L) && predicate.test(1L << 48), "unknown keys are conservative");

        prior.reset();
        priorOracle.reset();
        same(prior.find(), priorOracle.find(), prior, priorOracle);
        check(cache.get(prior) == originalCache, "prepared String does not replace a warm cache");
        int savedFrom = cacheFrom.getInt(prior), savedTo = cacheTo.getInt(prior);
        prior.region(1, 100);
        priorOracle.region(1, 100);
        same(prior.find(), priorOracle.find(), prior, priorOracle);
        check(cache.get(prior) == originalCache, "global negative preserves old regional cache");
        check(cacheFrom.getInt(prior) == savedFrom && cacheTo.getInt(prior) == savedTo,
                "negative does not stamp old cache as a new region");

        Matcher absent = missing.matcher(cold), absentOracle = missing.matcher(new StringBuilder(cold));
        same(absent.find(), absentOracle.find(), absent, absentOracle);
        check(cache.get(absent) == null, "prepared negative skips regional preparation");
        absent.region(2, 50);
        absentOracle.region(2, 50);
        same(absent.find(), absentOracle.find(), absent, absentOracle);
        check(cache.get(absent) == null, "regional negative stays metadata-only");
        check(peek.invoke(storage) == preparedFacts, "negative keeps source facts identity");

        String outside = new String("abcXYZdef".toCharArray());
        prepareFacts(storageField, outside);
        check(access.stringM3TrigramFacts(outside).test(packed("XYZ", 0)),
                "global-positive fixture precondition");
        Matcher bounded = Pattern.compile("XYZ", Pattern.LITERAL).matcher(outside).region(0, 3);
        Matcher boundedOracle = Pattern.compile("XYZ", Pattern.LITERAL)
                .matcher(new StringBuilder(outside)).region(0, 3);
        same(bounded.find(), boundedOracle.find(), bounded, boundedOracle);
        check(cache.get(bounded) != null, "global positive still verifies isolated region");

        String slice = outside.substring(0, 3);
        Object sliceFacts = prepareFacts(storageField, slice);
        var slicePredicate = access.stringM3TrigramFacts(slice);
        check(slicePredicate == sliceFacts, "range lookup reuses range facts");
        check(!slicePredicate.test(packed("XYZ", 0)), "range excludes parent-only trigram");
        Matcher sliced = Pattern.compile("XYZ", Pattern.LITERAL).matcher(slice);
        Matcher slicedOracle = Pattern.compile("XYZ", Pattern.LITERAL).matcher(new StringBuilder(slice));
        same(sliced.find(), slicedOracle.find(), sliced, slicedOracle);
        check(cache.get(sliced) == null, "prepared range negative skips regional preparation");

        String collision = new String("aco".toCharArray());
        prepareFacts(storageField, collision);
        check(access.stringM3TrigramFacts(collision).test(packed("adv", 0)),
                "probabilistic collision fixture precondition");
        Matcher collided = Pattern.compile("adv", Pattern.LITERAL).matcher(collision);
        Matcher collidedOracle = Pattern.compile("adv", Pattern.LITERAL).matcher(new StringBuilder(collision));
        same(collided.find(), collidedOracle.find(), collided, collidedOracle);
        check(cache.get(collided) != null, "signal collision retains exact regional verification");

        String captured = new String("xxabcdefyy".toCharArray());
        prepareFacts(storageField, captured);
        Pattern capturePattern = Pattern.compile("ab(cd)ef");
        Matcher capture = capturePattern.matcher(captured);
        Matcher captureOracle = capturePattern.matcher(new StringBuilder(captured));
        same(capture.find(), captureOracle.find(), capture, captureOracle);
        check(cache.get(capture) != null, "captured query retains regional verification");
        capture.reset();
        captureOracle.reset();
        same(capture.find(1), captureOracle.find(1), capture, captureOracle);
        capture.useTransparentBounds(true).useAnchoringBounds(false).region(2, 8);
        captureOracle.useTransparentBounds(true).useAnchoringBounds(false).region(2, 8);
        same(capture.find(), captureOracle.find(), capture, captureOracle);

        List<String> unicode = new ArrayList<>();
        unicode.add(new String(new char[] {0, '\u00ff', '\u0100', '\ud83d', '\ude42', '\ud800', 'x', '\udc00'}));
        String high = new String(new char[] {'a', '\ud83d'});
        String low = new String(new char[] {'\ude42', 'z'});
        String joined = String.join("", high, low);
        unicode.add(joined);
        unicode.add(joined.substring(1, 3));
        unicode.add(joined.substring(0, 2));
        unicode.add(joined.substring(2));
        for (String value : unicode) {
            Object facts = prepareFacts(storageField, value);
            var grams = access.stringM3TrigramFacts(value);
            check(grams == facts, "Unicode facts reused directly");
            for (int i = 0; i + 2 < value.length(); i++) {
                check(grams.test(packed(value, i)), "present UTF-16 triple is never rejected");
            }
            Pattern literal = Pattern.compile(value, Pattern.LITERAL);
            Matcher left = literal.matcher(value), right = literal.matcher(new StringBuilder(value));
            same(left.find(), right.find(), left, right);
            same(left.find(), right.find(), left, right);
        }
        System.out.println("MR_PREPARED\tchecks=" + checks);
    }

    private static Object prepareFacts(Field storageField, String text) throws Exception {
        Object storage = storageField.get(text);
        check(storage != null, "prepared fixture has M3 storage");
        var method = storage.getClass().getDeclaredMethod("facts");
        method.setAccessible(true);
        return method.invoke(storage);
    }

    private static long packed(String value, int from) {
        return ((long) value.charAt(from) << 32)
                | ((long) value.charAt(from + 1) << 16) | value.charAt(from + 2);
    }

    private static void cost() throws Exception {
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new AssertionError("allocation measurement unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        for (int length : new int[] {126, 6_144}) {
            String text = "abc".repeat(length / 3);
            Matcher matcher = Pattern.compile("acb", Pattern.LITERAL).matcher(text);
            long begin = System.nanoTime();
            check(!matcher.find(), "cold cost result");
            long cold = System.nanoTime() - begin;
            for (int i = 0; i < 200; i++) { matcher.reset(); check(!matcher.find(), "warmup"); }
            for (int pass = 0; pass < 4; pass++) {
                long bytes = bean.getThreadAllocatedBytes(thread);
                long start = System.nanoTime();
                for (int i = 0; i < 256; i++) { matcher.reset(); check(!matcher.find(), "warm cost result"); }
                long elapsed = System.nanoTime() - start;
                long allocated = bean.getThreadAllocatedBytes(thread) - bytes;
                System.out.println("MR_COST\tunits=" + length + "\tpass=" + pass + "\tcoldNs=" + cold
                        + "\tqueries=256\tbytes=" + allocated + "\tns=" + elapsed);
            }
        }
    }

    private static void enumerate(List<String> values, String prefix, int remaining) {
        values.add(prefix);
        if (remaining == 0) return;
        for (String token : List.of("a", "b", "c", "\n", "é", "\ud83d", "\ude42")) {
            enumerate(values, prefix + token, remaining - 1);
        }
    }

    private interface Action { Object run(); }
    private static String failure(Action action) {
        try { action.run(); return ""; } catch (RuntimeException expected) { return expected.getClass().getName(); }
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " at cell " + cells);
    }
    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static void frame(String value) {
        if (value == null) { DIGEST.update((byte) 0); return; }
        DIGEST.update((byte) 1);
        int size = value.length();
        for (int i = 24; i >= 0; i -= 8) DIGEST.update((byte) (size >>> i));
        for (int i = 0; i < size; i++) {
            DIGEST.update((byte) (value.charAt(i) >>> 8));
            DIGEST.update((byte) value.charAt(i));
        }
    }
}
