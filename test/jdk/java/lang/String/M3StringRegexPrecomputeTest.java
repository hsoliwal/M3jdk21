/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
/*
 * @test
 * @summary M3String regex precompute must preserve Pattern results and canonical range identity.
 * @modules java.base/java.lang:open
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringRegexPrecomputeTest required
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringRegexPrecomputeTest required
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage M3StringRegexPrecomputeTest off
 */
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class M3StringRegexPrecomputeTest {
    public static void main(String[] args) throws Exception {
        String[] patterns = {"a*", "a*b*", ".*", "a.*b", "c*a*b", "A*0*", "a*b.*a*",
                "a*".repeat(32), "a*".repeat(33), "", "a", "(?s).*", "[ab]*", "(a*)b\\1"};
        Random random = new Random(0x4d335245474558L);
        for (int trial = 0; trial < 3000; trial++) {
            StringBuilder out = new StringBuilder();
            for (int n = random.nextInt(80); n > 0; n--) {
                out.append("abA0\n\r\u0085\u2028\u2029\u00e9".charAt(random.nextInt(10)));
            }
            String text = new String(out.toString().toCharArray());
            int cut = text.length() / 2;
            String joined = text.substring(0, cut) + text.substring(cut);
            for (String p : patterns) {
                boolean expected = Pattern.compile(p).matcher(text).matches();
                if (text.matches(p) != expected || joined.matches(p) != expected) {
                    throw new AssertionError("Pattern parity trial " + trial + " regex " + p);
                }
            }
        }
        String high = new String(new char[]{'a', '\ud83d'});
        String low = new String(new char[]{'\ude00', 'b'});
        String splitPair = high + low;
        if (!splitPair.matches("a.b")) throw new AssertionError("surrogate seam fallback");
        for (String bad : new String[]{"*", "a**", "[", "(?", "\\"}) {
            try { "a".matches(bad); throw new AssertionError("invalid regex accepted"); }
            catch (PatternSyntaxException expected) { }
        }
        try { "a".matches(null); throw new AssertionError("null regex accepted"); }
        catch (NullPointerException expected) { }
        if (args[0].equals("required")) {
            Method m3 = String.class.getDeclaredMethod("m3"); m3.setAccessible(true);
            Class<?> helper = Class.forName("java.lang.M3StringRegexDp");
            Class<?> storage = Class.forName("java.lang.M3String");
            Method match = helper.getDeclaredMethod("tryMatch", storage, String.class);
            match.setAccessible(true);
            String fresh = new String("a".repeat(4096).toCharArray());
            Object value = m3.invoke(fresh);
            if (value == null) throw new AssertionError("M3 storage not activated; cannot qualify target");
            if (((Integer) match.invoke(null, value, "a*")) != 1) throw new AssertionError("M3 match");
            Field missing = helper.getDeclaredField("nativeUnavailable"); missing.setAccessible(true);
            if (missing.getBoolean(null)) throw new AssertionError("required libjava native route missing");
            // No derived program/cache/payload fields may leak onto the value representation.
            int fields = 0;
            for (Field f : storage.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) fields++;
            }
            if (fields != 2) throw new AssertionError("M3String owner+coordinate invariant");
        }
        System.out.println("M3_REGEX_PRODUCT_TEST_PASS|mode=" + args[0]);
    }
}
