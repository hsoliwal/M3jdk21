/*
 * @test
 * @summary M3 String restricted-regex rolling DP preserves Pattern.matches semantics
 * @modules java.base/java.lang:open
 * @run main/othervm -Xmx128m -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage --add-opens=java.base/java.lang=ALL-UNNAMED M3StringRestrictedRegexDpTest
 */
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class M3StringRestrictedRegexDpTest {
    private M3StringRestrictedRegexDpTest() {}
    private static long comparisons;
    private static long admitted;
    private static Method tryMatches;
    private static Field m3Field;

    public static void main(String[] args) throws Exception {
        Class<?> owner = Class.forName("java.lang.M3String");
        Class<?> dp = Class.forName("java.lang.M3StringRegexDp");
        tryMatches = dp.getDeclaredMethod("tryMatches", owner, String.class);
        tryMatches.setAccessible(true);
        m3Field = String.class.getDeclaredField("m3");
        m3Field.setAccessible(true);
        examples();
        exclusions();
        randomCases();
        if (admitted < 1000) throw new AssertionError("matched M3-backed fast path was not exercised");
        System.out.println("M3_STRING_RESTRICTED_REGEX_DP_PASS|comparisons=" + comparisons
                + "|admitted=" + admitted);
    }

    private static String composite(String text) {
        int center = text.length() / 2;
        return String.join("", text.substring(0, center), text.substring(center));
    }

    private static void compare(String text, String pattern, Boolean expected) throws Exception {
        String s = composite(text);
        boolean jdk = Pattern.matches(pattern, new StringBuilder(s));
        if (expected != null && jdk != expected) {
            throw new AssertionError("incorrect fixture: " + text + " / " + pattern);
        }
        if (s.matches(pattern) != jdk) {
            throw new AssertionError("mismatch: " + text + " / " + pattern);
        }
        Object body = m3Field.get(s);
        if (body != null) {
            int status = (int) tryMatches.invoke(null, body, pattern);
            if (status >= 0) {
                admitted++;
                if ((status == 1) != jdk) {
                    throw new AssertionError("wrong DP: " + text + " / " + pattern);
                }
            }
        }
        comparisons++;
    }

    private static void examples() throws Exception {
        compare("", "a*", true);
        compare("aa", "a", false);
        compare("aa", "a*", true);
        compare("ab", ".*", true);
        compare("aab", "c*a*b", true);
        compare("mississippi", "mis*is*p*.", false);
        compare("ab", ".*c", false);
        compare("", "", true);
        compare("a", "", false);
        compare("", "a*b*c*", true);
        compare("abc", "a*b*c*", true);
        compare("abcc", "a*b*c*", true);
        compare("abcd", "a*b*c*", false);
    }

    private static void exclusions() throws Exception {
        for (String t : new String[] {"X", "a\nb", "a\r", "a\u2028b", "a\ud83d\ude42b", "\ud800", "a0b", "abc"}) {
            for (String p : new String[] {".*", "a*", "a.c", "", "[abc]*", "(?s).*", "a+", "^a.*b$"}) {
                compare(t, p, null);
            }
        }
        for (String malformed : new String[] {"*", "a**", "**", "a***", "(", "\\"}) {
            String value = composite("abc");
            try {
                value.matches(malformed);
                throw new AssertionError("missing PatternSyntaxException for " + malformed);
            } catch (PatternSyntaxException expected) {
                comparisons++;
            }
        }
    }

    private static void randomCases() throws Exception {
        Random rng = new Random(0x4d33524547455844L);
        for (int trial = 0; trial < 10_000; trial++) {
            StringBuilder pattern = new StringBuilder();
            for (int n = rng.nextInt(24); n > 0; n--) {
                pattern.append("abcz.".charAt(rng.nextInt(5)));
                if (rng.nextInt(4) == 0) pattern.append('*');
            }
            StringBuilder value = new StringBuilder();
            for (int n = rng.nextInt(55); n > 0; n--) {
                value.append("abcz".charAt(rng.nextInt(4)));
            }
            compare(value.toString(), pattern.toString(), null);
        }
    }
}
