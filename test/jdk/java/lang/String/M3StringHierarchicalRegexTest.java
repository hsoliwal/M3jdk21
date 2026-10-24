/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Hierarchical regex effects preserve canonical M3String owner/range semantics
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/jdk.internal.mindex=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringHierarchicalRegexTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/jdk.internal.mindex=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringHierarchicalRegexTest
 */
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.regex.Pattern;

public final class M3StringHierarchicalRegexTest {
    public static void main(String[] args) throws Exception {
        Field backing = String.class.getDeclaredField("m3");
        backing.setAccessible(true);
        Class<?> m3 = Class.forName("java.lang.M3String");
        Method decision = Class.forName("java.lang.M3StringRegexDp")
                .getDeclaredMethod("tryMatch", m3, String.class);
        decision.setAccessible(true);
        Random random = new Random(0x4d334450L);
        String[] patterns = {"a*b*", "a.*b", "a*b*a*", ".*", "a*.*b*", "a*b*a*b*"};
        long checks = 0;
        for (int trial = 0; trial < 2048; trial++) {
            String block = String.join("", "aa", trial % 3 == 0 ? "b" : "a");
            String value = block.repeat(1 + random.nextInt(1000));
            int from = random.nextInt(value.length());
            String view = value.substring(from, from + random.nextInt(value.length() - from + 1));
            for (String regex : patterns) {
                boolean expected = Pattern.compile(regex).matcher(new StringBuilder(view)).matches();
                if (view.matches(regex) != expected) throw new AssertionError("String regex parity");
                Object source = backing.get(view);
                if (source != null) {
                    int actual = (Integer) decision.invoke(null, source, regex);
                    if (actual >= 0 && (actual == 1) != expected) throw new AssertionError("DP decision parity");
                }
                checks++;
            }
        }
        for (String unit : new String[]{"\n", "\r", "\u0085", "\u2028", "\u2029", "\u0100",
                "\ud800", "\udc00", "\ud800\udc00"}) {
            String value = String.join("", "a", unit, "b");
            boolean expected = Pattern.compile("a.*b").matcher(new StringBuilder(value)).matches();
            if (value.matches("a.*b") != expected) throw new AssertionError("seam/code-point fallback");
            checks++;
        }
        // Start with actual admitted storage so repeat creates the shared DAG being tested.
        String required = String.join("", new String(new char[]{'a', 'a'}).repeat(2048), "b");
        if (backing.get(required) == null) throw new AssertionError("required M3 path not exercised");
        // Exercise the exact receiver Access adapter on a real shared M3String DAG, not a fixture.
        Class<?> dp = Class.forName("java.lang.M3StringRegexDp");
        Field access = dp.getDeclaredField("RELATION_ACCESS");
        access.setAccessible(true);
        // Optional adapter allocation is lazy and independent of earlier random cohorts.
        Method accessFactory = dp.getDeclaredMethod("relationAccess");
        accessFactory.setAccessible(true);
        accessFactory.invoke(null);
        Class<?> relations = Class.forName("jdk.internal.mindex.M3DotStarRelations");
        Method evaluate = relations.getMethod("evaluate", Object.class,
                Class.forName("jdk.internal.mindex.M3DotStarRelations$Access"), int[].class,
                int.class, int.class, long.class,
                Class.forName("jdk.internal.mindex.M3DotStarRelations$Join"), Runnable.class);
        Object summary = evaluate.invoke(null, backing.get(required), access.get(null),
                new int[]{'a' | (1 << 16), 'b'}, 2, 128, (long) required.length() * 3, null, null);
        int actual = (Integer) summary.getClass().getMethod("decision").invoke(summary);
        long reads = (Long) summary.getClass().getMethod("codeUnitReads").invoke(summary);
        int reuse = (Integer) summary.getClass().getMethod("reusedSummaries").invoke(summary);
        if (actual != 1 || reuse == 0 || reads >= required.length()) {
            throw new AssertionError("required hierarchical reuse did not execute");
        }
        Thread.currentThread().interrupt();
        try {
            if (!required.matches("a*b") || !Thread.currentThread().isInterrupted()) {
                throw new AssertionError("String matching acquired cancellation semantics");
            }
        } finally { Thread.interrupted(); }
        System.out.println("M3_HIERARCHICAL_REGEX_TARGET checks=" + checks + " PASS");
    }
}
