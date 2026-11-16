/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

/*
 * @test
 * @summary Canonical String casing preserves JDK Unicode/locale rules and bounded output owners
 * @modules java.base/java.lang:open
 * @run main/othervm -XX:+UnlockExperimentalVMOptions
 *   -XX:+UseM3StringStorage M3StringUnicodeCaseTest
 * @run main/othervm -Xint -XX:+UnlockExperimentalVMOptions
 *   -XX:+UseM3StringStorage -XX:-CompactStrings M3StringUnicodeCaseTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions
 *   -XX:+UseM3StringStorage -Dm3.string.pool.maxBytes=0 M3StringUnicodeCaseTest refusal
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;

public class M3StringUnicodeCaseTest {
    private static final Class<?> TYPE;
    private static final Class<?> TUPLE;
    private static final Method ADMIT;
    private static final Method CONCAT;
    private static final Method UNIT;
    private static final Method SLICE;
    private static final Constructor<String> WRAP;
    private static final Field STORAGE;
    private static final Field OWNER;
    private static final Field OWNER_LENGTH;
    private static final Field LEFT;
    private static final Field RIGHT;
    static {
        try {
            TYPE = Class.forName("java.lang.M3String");
            TUPLE = Class.forName("java.lang.M3StringTuple");
            Class<?> pool = Class.forName("java.lang.M3StringPool");
            ADMIT = method(TYPE, "admit", char[].class, int.class, int.class);
            CONCAT = method(pool, "concat", TYPE, TYPE);
            UNIT = method(pool, "internUnit", char.class);
            SLICE = method(TYPE, "slice", int.class, int.class);
            WRAP = String.class.getDeclaredConstructor(TYPE);
            WRAP.setAccessible(true);
            STORAGE = field(String.class, "m3");
            OWNER = field(TYPE, "owner");
            OWNER_LENGTH = field(Class.forName("java.lang.M3StringOwner"), "length");
            LEFT = field(TUPLE, "left");
            RIGHT = field(TUPLE, "right");
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        boolean refusal = args.length != 0;
        literal("Stra\u00dfe", "und", true, "STRASSE");
        literal("\u0130", "und", false, "i\u0307");
        literal("I\u0307 I I\u0323\u0307 I\u0301\u0307", "tr", false,
                "i \u0131 i\u0323 \u0131\u0301\u0307");
        literal("i \u0131", "tr", true, "\u0130 I");
        literal("I\u0307 I", "az", false, "i \u0131");
        literal("i\u0307 j\u0323\u0307", "lt", true, "I J\u0323");
        literal("I\u0301 \u00cc\u00cd\u0128", "lt", false,
                "i\u0307\u0301 i\u0307\u0300i\u0307\u0301i\u0307\u0303");
        literal("\u039f\u03a3 \u039f\u03a3\u0391 \u03a3", "und", false,
                "\u03bf\u03c2 \u03bf\u03c3\u03b1 \u03c3");
        literal("\ufb03\u0149\u0390", "und", true, "FFI\u02bcN\u0399\u0308\u0301");
        literal("\ud801\udc00 \ud801\udc28 \ud800x\udc00", "und", false,
                "\ud801\udc28 \ud801\udc28 \ud800x\udc00");
        String seam = wrap(CONCAT.invoke(null, canonical("a\ud801"), canonical("\udc28\u00df")));
        sameText(seam.toUpperCase(Locale.ROOT), "A\ud801\udc00SS");
        String range = wrap(SLICE.invoke(canonical("!\ud801\udc00\ud800X\udc00!"), 1, 7));
        sameText(range.toLowerCase(Locale.ROOT), "\ud801\udc28\ud800x\udc00!");
        String stable = wrap(canonical("\u1d62\u0307"));
        check(stable.toUpperCase(Locale.forLanguageTag("lt")) == stable,
                "Lithuanian conditional-only mapping must preserve first-change identity");
        String uncased = wrap(canonical("\u2603\ud800\udc00\udc00"));
        check(uncased.toLowerCase(Locale.ROOT) != uncased,
                "surrogate-triggered unchanged lower mapping returns a fresh String");
        check(uncased.toUpperCase(Locale.ROOT) != uncased,
                "surrogate-triggered unchanged upper mapping returns a fresh String");
        String emoji = wrap(canonical("\ud83d\ude00"));
        check(emoji.toLowerCase(Locale.ROOT) != emoji && emoji.toUpperCase(Locale.ROOT) != emoji,
                "unchanged supplementary emoji preserves JDK reference identity");
        String isolated = wrap(canonical("\ud800x\udc00"));
        check(isolated.toLowerCase(Locale.ROOT) != isolated,
                "unchanged isolated surrogate preserves JDK reference identity");
        String bmpStable = wrap(canonical("\u2603"));
        check(bmpStable.toLowerCase(Locale.ROOT) == bmpStable,
                "unchanged BMP mapping returns original String");
        try {
            stable.toLowerCase(null);
            throw new AssertionError("null locale accepted");
        } catch (NullPointerException expected) { }
        try {
            stable.toUpperCase(null);
            throw new AssertionError("null locale accepted");
        } catch (NullPointerException expected) { }
        String dense = wrap(canonical("\u00e4\u00df\ud801\udc28x".repeat(refusal ? 1000 : 20000)));
        String upper = dense.toUpperCase(Locale.ROOT);
        sameText(upper, "\u00c4SS\ud801\udc00X".repeat(refusal ? 1000 : 20000));
        int owners = checkChunkOwners(STORAGE.get(upper), refusal ? 1 : 1024);
        if (!refusal) {
            check(owners <= 2 * ((upper.length() + 1023) / 1024) + 1,
                    "dense mapping creates one owner chunk per bounded block");
            String alternating = wrap(canonical("x\u00c4".repeat(20000)));
            String lower = alternating.toLowerCase(Locale.ROOT);
            sameText(lower, "x\u00e4".repeat(20000));
            checkChunkOwners(STORAGE.get(lower), 1024);
            // A long unchanged range must remain a view of the original canonical owner.
            Object snow = canonical("\u2603".repeat(10000));
            Object source = CONCAT.invoke(null, canonical("a"), snow);
            source = CONCAT.invoke(null, source, canonical("b"));
            String shared = wrap(source).toUpperCase(Locale.ROOT);
            check(containsOwner(STORAGE.get(shared), OWNER.get(snow)),
                    "long unchanged source owner must survive case conversion");
        }
        System.out.println("M3StringUnicodeCaseTest PASS refusal=" + refusal);
    }

    private static void literal(String input, String tag, boolean upper, String expected)
            throws Exception {
        String source = wrap(canonical(input));
        String result = upper ? source.toUpperCase(Locale.forLanguageTag(tag))
                              : source.toLowerCase(Locale.forLanguageTag(tag));
        sameText(result, expected);
        check(STORAGE.get(result) != null, "mapped String retains canonical storage");
    }

    private static Object canonical(String text) throws Exception {
        char[] chars = text.toCharArray();
        Object admitted = ADMIT.invoke(null, chars, 0, chars.length);
        if (admitted != null) return admitted;
        // Exercise the real guaranteed single-unit lane when the scalar budget is zero.
        ArrayList<Object> values = new ArrayList<>(chars.length);
        for (char unit : chars) values.add(UNIT.invoke(null, unit));
        while (values.size() > 1) {
            ArrayList<Object> next = new ArrayList<>((values.size() + 1) / 2);
            for (int i = 0; i < values.size(); i += 2) {
                next.add(i + 1 == values.size() ? values.get(i)
                        : CONCAT.invoke(null, values.get(i), values.get(i + 1)));
            }
            values = next;
        }
        return values.getFirst();
    }

    private static String wrap(Object value) throws Exception { return WRAP.newInstance(value); }

    private static void sameText(String actual, String expected) {
        check(actual.length() == expected.length(), "mapped UTF-16 length");
        for (int i = 0; i < expected.length(); i++)
            check(actual.charAt(i) == expected.charAt(i), "mapped UTF-16 unit " + i);
    }

    private static int checkChunkOwners(Object value, int max) throws Exception {
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        visit(value, seen, null, max);
        return seen.size();
    }

    private static boolean containsOwner(Object value, Object wanted) throws Exception {
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        return visit(value, seen, wanted, Integer.MAX_VALUE);
    }

    private static boolean visit(Object value, Set<Object> seen, Object wanted, int max)
            throws Exception {
        Object owner = OWNER.get(value);
        if (owner == wanted) return true;
        if (!seen.add(owner)) return false;
        if (TUPLE.isInstance(owner)) {
            boolean left = visit(LEFT.get(owner), seen, wanted, max);
            boolean right = visit(RIGHT.get(owner), seen, wanted, max);
            return left || right;
        }
        check((int) OWNER_LENGTH.get(owner) <= max,
                "case result retained an oversized scalar owner");
        return false;
    }

    private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name); field.setAccessible(true); return field;
    }

    private static Method method(Class<?> owner, String name, Class<?>... parameters)
            throws ReflectiveOperationException {
        Method method = owner.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
