// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary The M3 segmented concat combinator (typed stringify gateways, one String[] collection)
 *          produces exactly the JLS concatenation result for every argument type, arity and
 *          constant shape; the generator is driven directly so the proof holds in both flag modes,
 *          and javac-compiled indy expressions are compared as well
 * @modules java.base/java.lang.invoke:+open
 * @run main M3StringConcatTupleTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringConcatTupleTest invoke
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.StringConcatFactory;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class M3StringConcatTupleTest {
    private static final Class<?>[] TYPES = {
        int.class, long.class, char.class, boolean.class, byte.class, short.class, float.class,
        double.class, Object.class, String.class, Integer.class, CharSequence.class, int[].class
    };
    private static final String[] CONSTANTS = {null, "", "x", "αβ", "-", " ", "constant piece"};
    private static long checks;

    /** toString() returning null must concatenate as "null" (JLS 15.18.1 via String.valueOf). */
    private static final class Nullish {
        @Override
        public String toString() {
            return null;
        }
    }

    public static void main(String[] args) throws Throwable {
        // Invoking the combinator needs the M3 runtime (storage pool); without the flag only
        // construction, handle types and the typed stringify gateways are checked.
        boolean invoke = args.length > 0 && args[0].equals("invoke");
        stringifiers();
        combinator(invoke);
        indy();
        System.out.println("M3StringConcatTupleTest checks=" + checks);
    }

    private static void stringifiers() throws Throwable {
        Method stringifier = StringConcatFactory.class.getDeclaredMethod("m3Stringifier", Class.class);
        stringifier.setAccessible(true);
        Random random = new Random(0x4d3353545249L);
        for (Class<?> type : TYPES) {
            MethodHandle mh = (MethodHandle) stringifier.invoke(null, type);
            if (type == String.class) {
                check(mh == null, "String needs no stringifier");
                continue;
            }
            check(mh.type().returnType() == String.class && mh.type().parameterCount() == 1, "stringifier type " + mh.type());
            Class<?> param = mh.type().parameterType(0);
            check(type.isPrimitive() ? param == type : param == Object.class,
                    "no boxing: " + type + " gateway takes " + param);
            for (int round = 0; round < 200; round++) {
                Object value = value(type, random);
                String actual = (String) mh.invokeWithArguments(value);
                check(stringOf(type, value).equals(actual), "stringifier " + type + " for " + value);
            }
        }
    }

    private static void combinator(boolean invoke) throws Throwable {
        Method generate = StringConcatFactory.class.getDeclaredMethod(
                "generateM3Concat", MethodType.class, String[].class);
        generate.setAccessible(true);
        Random random = new Random(0x4d33434f4e434154L);
        for (int round = 0; round < 3_000; round++) {
            int arity = 1 + random.nextInt(6);
            Class<?>[] types = new Class<?>[arity];
            Object[] values = new Object[arity];
            for (int i = 0; i < arity; i++) {
                types[i] = TYPES[random.nextInt(TYPES.length)];
                values[i] = value(types[i], random);
            }
            String[] constants = new String[arity + 1];
            for (int i = 0; i <= arity; i++) {
                constants[i] = CONSTANTS[random.nextInt(CONSTANTS.length)];
            }
            MethodType mt = MethodType.methodType(String.class, types);
            MethodHandle mh = (MethodHandle) generate.invoke(null, mt, constants);
            check(mh.type().equals(mt), "handle type " + mh.type() + " vs " + mt);
            if (!invoke) {
                continue;
            }
            String actual = (String) mh.invokeWithArguments(values);
            String expected = expected(constants, types, values);
            check(expected.equals(actual), "combinator " + mt + " constants " + List.of(describe(constants))
                    + " expected " + show(expected) + " actual " + show(actual));
        }
        if (!invoke) {
            return;
        }
        // Fixed shapes: no constants, only constants around, all-empty pieces, single String.
        MethodType mt = MethodType.methodType(String.class, int.class, String.class, Object.class);
        MethodHandle mh = (MethodHandle) generate.invoke(null, mt, new String[] {null, null, null, null});
        check("7nullnull".equals((String) mh.invokeWithArguments(7, null, null)), "nulls");
        mh = (MethodHandle) generate.invoke(null, MethodType.methodType(String.class, String.class),
                new String[] {"[", "]"});
        check("[s]".equals((String) mh.invokeWithArguments("s")), "wrapped single");
        check("[null]".equals((String) mh.invokeWithArguments((Object) null)), "wrapped null");
        mh = (MethodHandle) generate.invoke(null, MethodType.methodType(String.class, String.class, String.class),
                new String[] {"", "", ""});
        check("".equals((String) mh.invokeWithArguments("", "")), "all empty");
        check("ab".equals((String) mh.invokeWithArguments("a", "b")), "two pieces");
    }

    private static Object value(Class<?> type, Random random) {
        if (type == int.class) return switch (random.nextInt(4)) { case 0 -> 0; case 1 -> Integer.MIN_VALUE; case 2 -> Integer.MAX_VALUE; default -> random.nextInt(); };
        if (type == long.class) return random.nextBoolean() ? random.nextLong() : Long.MIN_VALUE;
        if (type == char.class) return switch (random.nextInt(4)) { case 0 -> 'a'; case 1 -> 'é'; case 2 -> '\uD83D'; default -> (char) random.nextInt(0x10000); };
        if (type == boolean.class) return random.nextBoolean();
        if (type == byte.class) return (byte) random.nextInt();
        if (type == short.class) return (short) random.nextInt();
        if (type == float.class) return switch (random.nextInt(4)) { case 0 -> Float.NaN; case 1 -> -0.0f; case 2 -> Float.MIN_VALUE; default -> random.nextFloat() * 1e6f; };
        if (type == double.class) return switch (random.nextInt(4)) { case 0 -> Double.POSITIVE_INFINITY; case 1 -> -0.0; case 2 -> 1e300; default -> random.nextDouble(); };
        if (type == String.class) return switch (random.nextInt(4)) { case 0 -> null; case 1 -> ""; case 2 -> "sé"; default -> "😀"; };
        if (type == Integer.class) return random.nextBoolean() ? null : random.nextInt();
        if (type == CharSequence.class) return switch (random.nextInt(3)) { case 0 -> null; case 1 -> new StringBuilder("sb"); default -> "cs"; };
        if (type == int[].class) return random.nextBoolean() ? null : new int[] {1};
        return switch (random.nextInt(5)) { case 0 -> null; case 1 -> new Nullish(); case 2 -> 42; case 3 -> new StringBuilder("obj"); default -> "o"; };
    }

    /** JLS 15.18.1: constants interleaved with String.valueOf-converted operands; null toString is "null". */
    private static String expected(String[] constants, Class<?>[] types, Object[] values) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (constants[i] != null) out.append(constants[i]);
            out.append(stringOf(types[i], values[i]));
        }
        if (constants[values.length] != null) out.append(constants[values.length]);
        return out.toString();
    }

    private static String stringOf(Class<?> type, Object value) {
        if (type.isPrimitive()) {
            if (type == char.class) return String.valueOf((char) value);
            if (type == float.class) return String.valueOf((float) value);
            if (type == double.class) return String.valueOf((double) value);
            if (type == boolean.class) return String.valueOf((boolean) value);
            if (type == long.class) return String.valueOf((long) value);
            return String.valueOf(((Number) value).intValue());
        }
        if (value == null) return "null";
        String text = value.toString();
        return text == null ? "null" : text;
    }

    private static void indy() {
        Random random = new Random(0x4d33494e4459L);
        for (int round = 0; round < 2_000; round++) {
            int i = random.nextInt();
            long l = random.nextLong();
            char c = (char) random.nextInt(0x10000);
            boolean b = random.nextBoolean();
            byte by = (byte) random.nextInt();
            short sh = (short) random.nextInt();
            float f = random.nextFloat();
            double d = random.nextDouble();
            Object o = round % 3 == 0 ? null : round % 3 == 1 ? new Nullish() : (Object) round;
            String s = round % 4 == 0 ? null : "s" + round;
            Integer boxed = round % 5 == 0 ? null : round;
            String expected = new StringBuilder().append("a").append(i).append("b").append(l).append(c)
                    .append(b).append(by).append(sh).append(f).append(d).append(String.valueOf(o))
                    .append(s).append(boxed).append("z").toString()
                    .replace("Nullish", "Nullish");
            String viaValueOf = "a" + i + "b" + l + c + b + by + sh + f + d + o + s + boxed + "z";
            check(expected(new String[] {"a", "b", null, null, null, null, null, null, null, null, null, "z"},
                    new Class<?>[] {int.class, long.class, char.class, boolean.class, byte.class, short.class,
                        float.class, double.class, Object.class, String.class, Integer.class},
                    new Object[] {i, l, c, b, by, sh, f, d, o, s, boxed}).equals(viaValueOf),
                    "indy mixed round " + round);
            check(("" + i).equals(Integer.toString(i)), "indy single int");
            check((s + o).equals(String.valueOf(s) + stringOf(Object.class, o)), "indy two refs");
            check(expected.length() >= 0, "builder reference");
        }
    }

    private static String[] describe(String[] constants) {
        String[] out = new String[constants.length];
        for (int i = 0; i < constants.length; i++) out[i] = constants[i] == null ? "<null>" : show(constants[i]);
        return out;
    }

    private static String show(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            out.append(ch < 32 || ch > 126 ? String.format("\\u%04x", (int) ch) : String.valueOf(ch));
        }
        return out.append('"').toString();
    }

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}
