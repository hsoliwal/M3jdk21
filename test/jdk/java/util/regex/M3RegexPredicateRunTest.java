/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
/*
 * @test
 * @summary Wide regex predicate planes preserve identity, range and epsilon boundaries
 * @modules java.base/java.util.regex:open
 * @run main/othervm M3RegexPredicateRunTest
 */
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.regex.Pattern;

public class M3RegexPredicateRunTest {
    private static long checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static Object read(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
    private static void planes(Pattern pattern) throws Exception {
        Object plan = read(pattern, "m3BooleanPlan");
        check(plan != null, "plan absent");
        Object[] atoms = (Object[]) read(plan, "atoms");
        boolean[] repeated = (boolean[]) read(plan, "repeated");
        boolean[] empty = (boolean[]) read(plan, "empty");
        long[] masks = (long[]) read(plan, "ascii");
        int words = (atoms.length >>> 6) + 1;
        check(masks != null && masks.length == 137 * words, "wide geometry");
        Method is = Class.forName("java.util.regex.Pattern$CharPredicate").getDeclaredMethod("is", int.class);
        is.setAccessible(true);
        for (int unit = 0; unit < 128; unit++) {
            for (int state = 0; state < words * 64; state++) {
                boolean expected = state > 0 && state <= atoms.length && (boolean) is.invoke(atoms[state - 1], unit);
                check(((masks[unit * words + (state >>> 6)] >>> (state & 63)) & 1L) == (expected ? 1L : 0L),
                        "character plane state=" + state + " unit=" + unit);
            }
        }
        for (int state = 0; state < words * 64; state++) {
            boolean star = state > 0 && state <= atoms.length && repeated[state - 1];
            boolean nullable = state < empty.length && empty[state];
            check(((masks[136 * words + (state >>> 6)] >>> (state & 63)) & 1L) == (star ? 1L : 0L), "repeat plane");
            check(((masks[134 * words + (state >>> 6)] >>> (state & 63)) & 1L) == (nullable ? 1L : 0L), "empty plane");
            check(((masks[135 * words + (state >>> 6)] >>> (state & 63)) & 1L) == (state == atoms.length ? 1L : 0L), "accept plane");
            for (int level = 0, span = 1; level < 6; level++, span <<= 1) {
                boolean expected = (state & 63) >= span && state <= atoms.length;
                for (int back = 0; expected && back < span; back++) expected = repeated[state - 1 - back];
                check(((masks[(128 + level) * words + (state >>> 6)] >>> (state & 63)) & 1L) == (expected ? 1L : 0L), "epsilon plane");
            }
        }
        for (String input : new String[] {"", "0", "123", "abc", " ", "\n", "\u00e9", "\u0661", "\ud800", "\udc00", "\ud83d\ude00", "12\ud83d\ude00"}) {
            check(pattern.asMatchPredicate().test(input) == pattern.matcher(new StringBuilder(input)).matches(), "differential " + pattern);
        }
    }
    public static void main(String[] args) throws Exception {
        for (int count : new int[] {64, 65, 127, 128, 129, 191, 192, 255, 256}) {
            for (String atom : new String[] {"\\d*", "\\w*", "\\s*", ".*", "a*", "[ab]*"}) {
                planes(Pattern.compile(atom.repeat(count)));
            }
            // Same lambda implementation class with different captured literal values.
            planes(Pattern.compile("a*b*".repeat(count / 2) + (count % 2 == 1 ? "a*" : "")));
            // Shared digit predicate, mixed mandatory and repeated states at word seams.
            planes(Pattern.compile("\\d*".repeat(count - 2) + "\\d\\d*", Pattern.UNICODE_CHARACTER_CLASS));
        }
        Random random = new Random(0x4d3352554eL);
        String[] atoms = {"\\d*", "\\w*", "\\s*", "a*", "b*", ".*", "\\d", "[ab]*"};
        for (int sample = 0; sample < 24; sample++) {
            StringBuilder expression = new StringBuilder("\\d*");
            for (int i = 1, size = 64 + random.nextInt(193); i < size; i++) expression.append(atoms[random.nextInt(atoms.length)]);
            planes(Pattern.compile(expression.toString(), sample % 2 == 0 ? Pattern.DOTALL : Pattern.UNICODE_CHARACTER_CLASS));
        }
        System.out.println("PREDICATE_PLAN_PASS checks=" + checks);
    }
}
