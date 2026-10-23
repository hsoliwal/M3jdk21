/*
 * @test
 * @summary Compiled trigram Boolean short-circuiting is a sound Matcher.find absence gate
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm --add-opens java.base/java.util.regex=ALL-UNNAMED M3RegexShortCircuitTest
 */

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jdk.internal.mindex.M3TQ;

public final class M3RegexShortCircuitTest {
    private static final Field QUERY;
    private static final Field CACHED_QUERY;
    private static long comparisons;

    static {
        try {
            QUERY = Pattern.class.getDeclaredField("m3Tq");
            QUERY.setAccessible(true);
            CACHED_QUERY = Matcher.class.getDeclaredField("m3TqScanQuery");
            CACHED_QUERY.setAccessible(true);
        } catch (ReflectiveOperationException problem) {
            throw new ExceptionInInitializerError(problem);
        }
    }

    private static void sameResult(Pattern gatedPattern, Pattern original, String text,
                                   int from, int to) throws ReflectiveOperationException {
        Matcher gated = gatedPattern.matcher(text).region(from, to);
        Matcher baseline = original.matcher(text).region(from, to);
        for (int i = 0; i < 3; i++) {
            boolean candidate = gated.find();
            boolean expected = baseline.find();
            if (candidate != expected || gated.hitEnd() != baseline.hitEnd()
                    || gated.requireEnd() != baseline.requireEnd()) {
                throw new AssertionError("find/state diverged for " + gatedPattern
                        + " at " + from + ":" + to + " on " + printable(text));
            }
            if (candidate) {
                if (gated.start() != baseline.start() || gated.end() != baseline.end()
                        || gated.groupCount() != baseline.groupCount()) {
                    throw new AssertionError("group offsets diverged");
                }
                for (int g = 0; g <= gated.groupCount(); g++) {
                    if (gated.start(g) != baseline.start(g)
                            || gated.end(g) != baseline.end(g)
                            || !java.util.Objects.equals(gated.group(g), baseline.group(g))) {
                        throw new AssertionError("group " + g + " diverged");
                    }
                }
            }
            comparisons++;
        }
        gated.reset();
        baseline.reset();
        if (gated.matches() != baseline.matches()
                || gated.lookingAt() != baseline.lookingAt()) {
            throw new AssertionError("anchored operation diverged");
        }
        comparisons += 2;
    }

    private static String printable(String text) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            builder.append(String.format("%04x", (int) text.charAt(i)));
        }
        return builder.toString();
    }

    public static void main(String[] args) throws Exception {
        List<String> patterns = List.of("abc|xyz", "abc.*xyz", "(abc|xyz)qqq", "abc",
                "(abc)z{1,2}", "(?i)abc", "^abc$", "\\babc\\b", "a*", "\\Qabc\\E");
        char[] units = { 'a', 'b', 'c', 'x', 'y', 'z', 'q', '\0', '\ud800', '\udc00', ' ' };
        Random random = new Random(0x4D335451L);
        int admitted = 0;
        for (String regex : patterns) {
            Pattern gated = Pattern.compile(regex);
            Pattern baseline = Pattern.compile(regex);
            QUERY.set(baseline, null); // exact same compiled JDK engine, prefilter disabled
            M3TQ compiled = (M3TQ) QUERY.get(gated);
            if (compiled != null && compiled.hasScanPlan()) admitted++;
            for (int sample = 0; sample < 500; sample++) {
                int len = random.nextInt(40);
                StringBuilder builder = new StringBuilder(len);
                for (int u = 0; u < len; u++) {
                    builder.append(units[random.nextInt(units.length)]);
                }
                String text = builder.toString();
                sameResult(gated, baseline, text, 0, text.length());
                int from = random.nextInt(len + 1);
                int to = from + random.nextInt(len - from + 1);
                sameResult(gated, baseline, text, from, to);
            }
        }
        if (admitted < 2) throw new AssertionError("streaming plan not installed");

        Pattern first = Pattern.compile("abc|xyz");
        Pattern second = Pattern.compile("qqq|zzz");
        Matcher reused = first.matcher("abc........qqq");
        if (!reused.find()) throw new AssertionError("first pattern");
        if (CACHED_QUERY.get(reused) != QUERY.get(first)) {
            throw new AssertionError("compiled scan cache not populated");
        }
        reused.usePattern(second).reset();
        if (!reused.find() || !"qqq".equals(reused.group())) {
            throw new AssertionError("cache leaked through Matcher.usePattern");
        }
        reused.reset("no candidate here");
        if (reused.find()) throw new AssertionError("new subject leaked old cache");
        System.out.println("PASS product JDK matcher comparisons=" + comparisons);
    }
}
