// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
//
// PLACEHOLDER_FORMAT_DIFFERENTIAL v1 (2026-11-09): compares jdk.internal.m3.collections.PlaceholderLogger.format with
// the reference {} formatter, org.slf4j.helpers.MessageFormatter.arrayFormat (slf4j-api 2.0.17 on the classpath; a
// qualification-time oracle only, never an M3 dependency). Fixed edge cases plus seeded random patterns. Prints the
// number of cases and every mismatch; exit 1 on any mismatch.
//
// java -cp <PlaceholderLogger classes>;<slf4j-api.jar>;. PlaceholderFormatDiff <seed> <random cases>
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import jdk.internal.m3.collections.PlaceholderLogger;
import org.slf4j.helpers.MessageFormatter;

public final class PlaceholderFormatDiff {
    public static void main(String[] args) {
        long seed = Long.parseLong(args[0]);
        int randomCases = Integer.parseInt(args[1]);
        List<Object[]> cases = new ArrayList<>();
        Object[] cyclic = new Object[2];
        cyclic[0] = "x";
        cyclic[1] = cyclic;
        Object bad = new Object() { @Override public String toString() { throw new IllegalStateException("boom"); } };
        String[] patterns = {"", "plain", "{}", "{} {}", "a{}b{}c", "\\{}", "\\\\{}", "x \\{} y {}", "{}{}{}", "{",
                "}", "{ }", "trailing {", "{}}", "{{}}", "a\\\\\\{}b {}", null};
        Object[][] argSets = {{}, {1}, {1, 2}, {1, 2, 3}, {null}, {null, null}, {new int[] {1, 2}},
                {new Object[] {"a", new long[] {3L}}}, {cyclic}, {bad}, {new IllegalArgumentException("e")},
                {1, new IllegalStateException("t")}, {"s", 2.5, 'c', true}, {new double[0], new char[] {'q'}}};
        for (String p : patterns) for (Object[] a : argSets) cases.add(new Object[] {p, a});
        Random random = new Random(seed);
        String[] pieces = {"{}", "\\{}", "\\\\{}", "a", " ", "{", "}", "\\", "xyz"};
        for (int i = 0; i < randomCases; i++) {
            StringBuilder pattern = new StringBuilder();
            int n = random.nextInt(8);
            for (int k = 0; k < n; k++) pattern.append(pieces[random.nextInt(pieces.length)]);
            Object[] a = new Object[random.nextInt(5)];
            for (int k = 0; k < a.length; k++) a[k] = random.nextInt(4) == 0 ? null : (Object) random.nextInt(100);
            if (a.length > 0 && random.nextInt(5) == 0) a[a.length - 1] = new RuntimeException("r" + i);
            cases.add(new Object[] {pattern.toString(), a});
        }
        int mismatches = 0;
        for (Object[] c : cases) {
            String pattern = (String) c[0];
            Object[] a = (Object[]) c[1];
            String expected = MessageFormatter.arrayFormat(pattern, a).getMessage();
            Throwable expectedThrowable = MessageFormatter.arrayFormat(pattern, a).getThrowable();
            String actual = PlaceholderLogger.format(pattern, a);
            Throwable actualThrowable = PlaceholderLogger.throwableOf(a);
            if (!java.util.Objects.equals(expected, actual) || expectedThrowable != actualThrowable) {
                mismatches++;
                System.out.println("MISMATCH pattern=" + pattern + " args=" + java.util.Arrays.deepToString(a)
                        + " expected=" + expected + " actual=" + actual
                        + " throwable " + expectedThrowable + " vs " + actualThrowable);
            }
        }
        System.out.println("PLACEHOLDER_FORMAT_DIFFERENTIAL seed " + seed + " cases " + cases.size()
                + " mismatches " + mismatches);
        System.exit(mismatches == 0 ? 0 : 1);
    }
}
