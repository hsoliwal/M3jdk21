/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Canonical M3 regex plans, composed effects, ranges, fallback and bounded ownership
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringRegexEffectsTest enabled
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringRegexEffectsTest enabled
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage M3StringRegexEffectsTest disabled
 */
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class M3StringRegexEffectsTest {
    private static long checks;
    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }
    private static void parity(String source, String expression) {
        boolean expected = Pattern.compile(expression).matcher(new StringBuilder(source)).matches();
        check(source.matches(expression) == expected, "String regex parity");
    }
    public static void main(String[] args) throws Exception {
        String atom = new String(new char[]{'a','a','a','a'});
        String text = atom;
        for (int n = 0; n < 9; n++) text = text.concat(text);
        String regex = String.join("", "a*", "b*");
        for (int i = 0; i < 20; i++) parity(text, regex);
        parity(text.concat("b"), regex);
        parity(text.substring(17, 1017), regex);
        parity(String.join("", "a", "\u03b2"), "a.*");
        for (String line : new String[]{"\n", "\r", "\r\n", "\u0085", "\u2028", "\u2029"}) {
            parity(text.substring(0, 128).concat(line), ".*");
        }
        parity(String.join("", "a", "\ud83d", "\ude00", "b"), "a.*b");
        parity(String.join("", "a", "\ud83d", "b"), "a.*b");
        parity(text, "a*".repeat(63));
        parity(text, "a".repeat(64) + "a*"); // preserve >64-state rolling-row fallback
        parity("", regex);
        parity(text, "a+");
        parity(text, "(a*)");
        parity(text, "(?s).*");
        for (String invalid : new String[]{"a**", "*a"}) {
            try { text.matches(invalid); throw new AssertionError("expected syntax error"); }
            catch (PatternSyntaxException expected) { checks++; }
        }
        if (args[0].equals("enabled")) {
            Class<?> m3 = Class.forName("java.lang.M3String");
            long fields = Arrays.stream(m3.getDeclaredFields())
                    .filter(f -> !java.lang.reflect.Modifier.isStatic(f.getModifiers())).count();
            check(fields == 2, "owner+coordinate representation retained");
            Field backing = String.class.getDeclaredField("m3"); backing.setAccessible(true);
            check(backing.get(text) != null, "test requires real canonical M3 storage");
            Class<?> dp = Class.forName("java.lang.M3StringRegexDp");
            Field c = dp.getDeclaredField("caches"); c.setAccessible(true);
            Object root = c.get(null);
            if (root != null) { // optional metadata may have been refused under pressure
                check(root instanceof java.lang.ref.SoftReference<?>, "VM-reclaimable metadata root");
                Object cache = ((java.lang.ref.Reference<?>) root).get();
                if (cache != null) {
                    Field plans = cache.getClass().getDeclaredField("plans"); plans.setAccessible(true);
                    Field effects = cache.getClass().getDeclaredField("effects"); effects.setAccessible(true);
                    check(((AtomicReferenceArray<?>) plans.get(cache)).length() == 64, "bounded plan slots");
                    check(((AtomicReferenceArray<?>) effects.get(cache)).length() == 128, "bounded effect slots");
                }
                ((java.lang.ref.Reference<?>) root).clear();
            }
            parity(text, regex); // refusal/eviction/reconstruction preserves the answer
        }
        System.out.println("M3_STRING_REGEX_EFFECTS_PASS checks=" + checks);
    }
}
