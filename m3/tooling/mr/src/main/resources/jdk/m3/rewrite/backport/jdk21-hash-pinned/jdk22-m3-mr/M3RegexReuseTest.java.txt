/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Reuse immutable input facts without changing Matcher state or mutable-input semantics
 * @modules java.base/java.util.regex:open
 *          jdk.management
 * @run main/othervm -Xcheck:jni M3RegexReuseTest
 * @run main/othervm -Xint -Xcheck:jni M3RegexReuseTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Xcheck:jni M3RegexReuseTest
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
        String text = "abc".repeat(100);
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
