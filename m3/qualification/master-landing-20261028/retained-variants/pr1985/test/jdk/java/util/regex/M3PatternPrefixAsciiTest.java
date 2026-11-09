/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
/*
 * @test
 * @summary Reuse prepared ASCII masks for qualified tails beyond the initial Unicode lookahead
 * @run main/othervm --add-opens java.base/java.util.regex=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED M3PatternPrefixAsciiTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage --add-opens java.base/java.util.regex=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED M3PatternPrefixAsciiTest m3
 */
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.regex.Pattern;

public final class M3PatternPrefixAsciiTest {
    private static long checks;
    private static long pairs;
    private static long reused;
    private static long asciiCalls;
    private static long unicodeCalls;
    private static long tuples;
    private static long splitPairTuples;

    private static Field field(Class<?> owner, String name) throws Exception {
        Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private static void check(boolean value, String detail) {
        checks++;
        if (!value) throw new AssertionError(detail);
    }

    private static String joined(String text, int split) throws Exception {
        String value = new String(text.substring(0, split).toCharArray())
                .concat(new String(text.substring(split).toCharArray()));
        Object storage = field(String.class, "m3").get(value);
        if (storage != null) {
            Method owner = storage.getClass().getDeclaredMethod("owner");
            owner.setAccessible(true);
            if (owner.invoke(storage).getClass().getName().equals("java.lang.M3StringTuple")) {
                tuples++;
                if (split > 0 && split < text.length()
                        && Character.isHighSurrogate(text.charAt(split - 1))
                        && Character.isLowSurrogate(text.charAt(split))) splitPairTuples++;
            }
        }
        return value;
    }

    private static void compare(int count, String regex, int flags, String text,
                                boolean expected, boolean reuse, int split) throws Exception {
        Pattern p = Pattern.compile(regex, flags);
        Object plan = field(Pattern.class, "m3BooleanPlan").get(p);
        check(plan != null, "missing admitted plan");
        Class<?> owner = plan.getClass();
        Object[] atoms = (Object[]) field(owner, "atoms").get(plan);
        check(atoms.length == count, "exact atom count " + count);
        long[] masks = (long[]) field(owner, "ascii").get(plan);
        check(masks != null, "prepared masks");
        long[] before = masks.clone();
        Class<?> predicate = atoms.getClass().componentType();
        Method is = predicate.getDeclaredMethod("is", int.class);
        is.setAccessible(true);
        for (int i = 0; i < atoms.length; i++) {
            Object original = atoms[i];
            atoms[i] = Proxy.newProxyInstance(predicate.getClassLoader(), new Class<?>[] {predicate},
                    (proxy, method, arguments) -> {
                        if (!method.getName().equals("is"))
                            throw new UnsupportedOperationException(method.toString());
                        if ((int) arguments[0] < 128) asciiCalls++; else unicodeCalls++;
                        try { return is.invoke(original, arguments); }
                        catch (InvocationTargetException failure) { throw failure.getCause(); }
                    });
        }
        Method rolling = owner.getDeclaredMethod("matchesRolling", String.class);
        rolling.setAccessible(true);
        for (String input : new String[] {new String(text.toCharArray()), joined(text, split)}) {
            asciiCalls = unicodeCalls = 0;
            check((int) rolling.invoke(plan, input) == (expected ? 1 : 0), "frozen rolling oracle");
            long rollingAscii = asciiCalls;
            long rollingUnicode = unicodeCalls;
            asciiCalls = unicodeCalls = 0;
            check(p.asMatchPredicate().test(input) == expected, "public match parity");
            check(unicodeCalls == rollingUnicode, "Unicode predicate demand");
            if (reuse) {
                check(asciiCalls == 0, "LATE_ASCII_REUSE_MISSING atoms=" + count
                        + " length=" + input.length() + " asciiPredicateCalls=" + asciiCalls);
                reused++;
            } else {
                check(asciiCalls == rollingAscii, "original rolling demand control");
            }
            check(field(owner, "ascii").get(plan) == masks && Arrays.equals(before, masks),
                    "immutable mask reuse");
            pairs++;
        }
    }

    public static void main(String[] args) throws Exception {
        for (int count : new int[] {8, 63, 64, 65, 128, 256}) {
            String literal = "é*" + "a*".repeat(count - 2) + "b";
            for (int prefix : new int[] {64, 65, 512}) {
                for (int tail : new int[] {7, 8, 9}) {
                    for (char last : new char[] {'b', 'c'}) {
                        String text = "é".repeat(prefix) + "a".repeat(tail - 1) + last;
                        compare(count, literal, 0, text, last == 'b', tail >= 8, prefix);
                    }
                }
            }
            String dots = ".*".repeat(count - 1) + "b";
            for (String seam : new String[] {"\ud801\udc00", "\ud801", "\udc00", "\udc00\ud801"}) {
                String prefix = "é".repeat(70) + seam;
                String text = prefix + "a".repeat(8) + "b";
                compare(count, dots, Pattern.DOTALL, text, true, true,
                        seam.length() == 2 ? 71 : prefix.length());
            }
            compare(count, dots, Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS,
                    "é".repeat(70) + "a".repeat(8) + "b", true, true, 70);
            // Interior islands and Unicode endings retain the original bounded selector.
            compare(count, literal, 0, "é".repeat(70) + "a".repeat(64) + "b" + "é",
                    false, false, 70);
            compare(count, literal, 0, "é".repeat(70), false, false, 35);
            compare(count, literal, 0, "é".repeat(70) + "a".repeat(3) + "b",
                    true, false, 70);
        }
        if (args.length != 0 && args[0].equals("m3")) {
            check(tuples > 0, "actual M3 tuples");
            check(splitPairTuples > 0, "actual pair split across M3 tuple seam");
        }
        System.out.printf("PREFIX_ASCII_PASS checks=%d pairs=%d reused=%d tuples=%d splitPairTuples=%d%n",
                checks, pairs, reused, tuples, splitPairTuples);
    }
}
