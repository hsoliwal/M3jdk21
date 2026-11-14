/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Verify request-confined regex Matcher reuse against a fresh-Matcher oracle
 * @run main/othervm -Xcheck:jni M3RegexWorkspaceContractTest
 * @run main/othervm -Xint -Xcheck:jni M3RegexWorkspaceContractTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Xcheck:jni M3RegexWorkspaceContractTest
 */

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3RegexWorkspaceContractTest {
    private static final String[] PATTERNS = {
            "", "^", "$", "\\bword\\b", "(a)(b)?\\1", "(?<=abc)tail",
            "(?=a)a", "(?s)(.*?)(z)?", "(?i:alpha)", "a|aa", "[\\s\\S]",
            "\\p{L}+", "(?:ab)*", "a+?b", "^foo$", "(?m)^bar$",
            "(?<one>a)(?<two>b)?"
    };
    private static final String[] SUBJECTS = {
            "", "a", "aa", "ab", "aba", "abab", "abc", "abctail",
            "word boundary", "aab", "alpha", "ALPHA", "foo", "foo\nbar",
            "bar", "z", "😀", "a😀b", "\uD800", "\uDC00", "λ"
    };

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void same(Matcher reused, Matcher fresh, int operation) {
        boolean left = switch (operation) {
            case 0 -> reused.find();
            case 1 -> reused.matches();
            default -> reused.lookingAt();
        };
        boolean right = switch (operation) {
            case 0 -> fresh.find();
            case 1 -> fresh.matches();
            default -> fresh.lookingAt();
        };
        check(left == right, "match boolean");
        check(reused.hitEnd() == fresh.hitEnd(), "hitEnd");
        check(reused.requireEnd() == fresh.requireEnd(), "requireEnd");
        check(reused.regionStart() == fresh.regionStart(), "regionStart");
        check(reused.regionEnd() == fresh.regionEnd(), "regionEnd");
        if (left) {
            check(reused.groupCount() == fresh.groupCount(), "groupCount");
            MatchResult snapshot = reused.toMatchResult();
            for (int i = 0; i <= reused.groupCount(); i++) {
                check(reused.start(i) == fresh.start(i), "capture start");
                check(reused.end(i) == fresh.end(i), "capture end");
                check(java.util.Objects.equals(reused.group(i), fresh.group(i)), "capture value");
                check(snapshot.start(i) == reused.start(i), "snapshot capture");
            }
        }
    }
    private static int differential() {
        int comparisons = 0;
        for (String regex : PATTERNS) {
            Pattern compiled = Pattern.compile(regex);
            Matcher reused = compiled.matcher("");
            for (String text : SUBJECTS) {
                // Construct a distinct logical String each time to exercise M3 String owners.
                String composite = new StringBuilder().append(text, 0, text.length() / 2)
                        .append(text, text.length() / 2, text.length()).toString();
                for (int mode = 0; mode < 3; mode++) {
                    Matcher fresh = compiled.matcher(new StringBuilder(composite));
                    same(reused.reset(composite), fresh, mode);
                    comparisons++;
                }
                Matcher withRegion = compiled.matcher(new StringBuilder(composite));
                int from = Math.min(1, composite.length());
                reused.region(from, composite.length());
                withRegion.region(from, composite.length());
                same(reused, withRegion, 0);
                reused.useTransparentBounds(true).useAnchoringBounds(false);
                withRegion.useTransparentBounds(true).useAnchoringBounds(false);
                same(reused.reset(composite), withRegion.reset(), 2);
                // Reset() intentionally does not restore boundary flags.
                reused.useTransparentBounds(false).useAnchoringBounds(true);
                comparisons += 2;
            }
        }
        return comparisons;
    }
    public static void main(String[] args) throws Exception {
        int checks = differential();
        try (var workers = Executors.newFixedThreadPool(4)) {
            List<Callable<Integer>> tasks = new ArrayList<>();
            for (int i = 0; i < 8; i++) tasks.add(M3RegexWorkspaceContractTest::differential);
            for (var future : workers.invokeAll(tasks)) checks += future.get();
        }
        System.out.println("M3_REGEX_WORKSPACE_CONTRACT_PASS\tchecks=" + checks);
    }
}
