/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public final class M3StringInvariant {
    static final Field STRING_M3 = field(String.class, "m3");
    static final Field STRING_VALUE = field(String.class, "value");
    static final Class<?> M3 = type("java.lang.M3String");
    static final Field M3_OWNER = field(M3, "owner");
    static final Field M3_VALUE = field(M3, "value");
    static final Method M3_FACTS = method(M3, "facts");
    static final Method M3_MATERIALIZE = method(M3, "materialize");

    static int checks;

    public static void main(String[] args) throws Exception {
        assertRepresentation();
        boolean expectMapped = Boolean.getBoolean("m3.expect.lexicon");

        String alpha1 = fresh("alpha");
        String alpha2 = fresh("alpha");
        Object m31 = body(alpha1);
        Object m32 = body(alpha2);
        Object alphaOwner = owner(m31);

        check(m31 != m32, "String wrappers own independent M3 coordinates");
        same(alphaOwner, owner(m32), "equal scalar values share canonical owner");
        check(atom(alphaOwner), "alpha is scalar owner");
        check(address(alphaOwner) != 0L, "scalar native/mapped address");
        check((payloadOwner(alphaOwner) != null) == expectMapped, "mapped/local owner selection");

        byte[] sentinel1 = (byte[]) STRING_VALUE.get(alpha1);
        byte[] sentinel2 = (byte[]) STRING_VALUE.get(alpha2);
        check(sentinel1.length == 0 && sentinel2.length == 0,
                "M3-backed String retains no byte payload");
        check(alpha1.equals(alpha2), "empty VM sentinel preserves equality");

        String right1 = fresh("gamma");
        String right2 = fresh("gamma");
        String joined1 = alpha1.concat(right1);
        String joined2 = alpha2.concat(right2);
        eq("alphagamma", joined1, "concat content");
        Object joinedM31 = body(joined1);
        Object joinedM32 = body(joined2);
        Object tupleOwner = owner(joinedM31);
        same(tupleOwner, owner(joinedM32), "equal compositions share canonical DAG owner");
        check(tuple(tupleOwner), "concat owner is tuple");
        assertNoArrayInstanceFields(tupleOwner.getClass());

        String slice = joined1.substring(2, 8);
        Object sliceM3 = body(slice);
        same(tupleOwner, owner(sliceM3), "substring reuses canonical parent owner");
        check(M3_VALUE.getLong(sliceM3) != M3_VALUE.getLong(joinedM31), "substring changes coordinate only");
        eq("phagam", slice, "substring content");

        Object facts1 = M3_FACTS.invoke(joinedM31);
        Object facts2 = M3_FACTS.invoke(joinedM32);
        same(facts1, facts2, "whole canonical owner shares internal precompute facts");
        assertNoArrayInstanceFields(facts1.getClass());

        String asciiTrim = fresh("  alpha  ");
        eq("alpha", asciiTrim.trim(), "M3 trim facts");
        eq("alpha", asciiTrim.strip(), "M3 ASCII strip facts");

        String unicodeStrip = fresh("\u2003\talpha\t\u2003");
        eq("alpha", unicodeStrip.strip(), "M3 Unicode strip facts");
        eq("\u2003\talpha\t\u2003", unicodeStrip.trim(), "legacy trim differs from Unicode strip");

        String allWhitespace = fresh(" \t\u2003");
        eq("", allWhitespace.strip(), "all Unicode whitespace strips empty");
        check(allWhitespace.isBlank(), "all Unicode whitespace is blank");

        String whitespaceTuple =
                fresh(" \u2003").concat(fresh("alpha")).concat(fresh("\u2003 "));
        eq("alpha", whitespaceTuple.strip(), "tuple-composed whitespace boundaries");
        check(!whitespaceTuple.isBlank(), "tuple-composed content is nonblank");

        Object whitespaceFacts = M3_FACTS.invoke(body(whitespaceTuple));
        same(whitespaceFacts, M3_FACTS.invoke(body(
                fresh(" \u2003").concat(fresh("alpha")).concat(fresh("\u2003 "))))),
                "equal whitespace tuple shares fixed facts");

        char[] chars = joined1.toCharArray();
        chars[0] = '!';
        eq("alphagamma", joined1, "JNI char shadow mutation isolation");

        byte[] bytes = (byte[]) M3_MATERIALIZE.invoke(joinedM31);
        if (bytes.length != 0) bytes[0] ^= 0x7f;
        eq("alphagamma", joined1, "JNI byte shadow mutation isolation");

        String repeated = joined1.repeat(5);
        eq("alphagamma".repeat(5), repeated, "repeat DAG content");
        check(tuple(owner(body(repeated))), "repeat remains canonical composition");

        String pa = fresh("a");
        String pb = fresh("b");
        String pc = fresh("c");
        String leftAssociated = pa.concat(pb).concat(pc);
        String rightAssociated = pa.concat(pb.concat(pc));
        same(
                owner(body(leftAssociated)),
                owner(body(rightAssociated)),
                "equal ordered composition ignores concat parenthesization");

        String chain = fresh("x");
        for (int index = 1; index < 4096; index++) {
            chain = chain.concat(fresh("x"));
        }
        Object chainOwner = owner(body(chain));
        check(tuple(chainOwner), "long concat chain remains tuple-backed");
        int height = field(chainOwner.getClass(), "height").getInt(chainOwner);
        check(height <= 20, "long concat chain stays balanced height=" + height);
        check(chain.length() == 4096, "balanced concat chain length");
        check(chain.charAt(0) == 'x' && chain.charAt(4095) == 'x',
                "balanced concat chain endpoint content");

        System.out.println("M3_STRING_INVARIANT_PASS checks=" + checks
                + " mapped=" + expectMapped
                + " m3Fields=2 retainedArrays=0 shadows=jni-only precompute=internal");
    }

    static String fresh(String value) {
        return new String(value.toCharArray());
    }

    static Object body(String value) throws Exception {
        value.length();
        Object body = STRING_M3.get(value);
        check(body != null, "String admitted to M3");
        return body;
    }

    static Object owner(Object m3) throws Exception {
        return M3_OWNER.get(m3);
    }

    static boolean atom(Object owner) {
        return owner.getClass().getName().equals("java.lang.M3StringAtom");
    }

    static boolean tuple(Object owner) {
        return owner.getClass().getName().equals("java.lang.M3StringTuple");
    }

    static long address(Object atom) throws Exception {
        return field(atom.getClass(), "address").getLong(atom);
    }

    static Object payloadOwner(Object atom) throws Exception {
        return field(atom.getClass(), "payloadOwner").get(atom);
    }

    static void assertRepresentation() {
        String[] instanceFields = Arrays.stream(M3.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(Field::getName).sorted().toArray(String[]::new);
        check(Arrays.equals(instanceFields, new String[] {"owner", "value"}),
                "M3String is exactly owner + coordinate");
        assertNoArrayInstanceFields(M3);
        assertNoArrayInstanceFields(type("java.lang.M3StringOwner"));
        assertNoArrayInstanceFields(type("java.lang.M3StringAtom"));
        assertNoArrayInstanceFields(type("java.lang.M3StringTuple"));
        assertNoArrayInstanceFields(type("java.lang.M3StringFacts"));
    }

    static void assertNoArrayInstanceFields(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && field.getType().isArray()) {
                throw new AssertionError(type.getName() + " retains array field " + field.getName());
            }
        }
        checks++;
    }

    static Class<?> type(String name) {
        try { return Class.forName(name); }
        catch (ClassNotFoundException e) { throw new ExceptionInInitializerError(e); }
    }

    static Field field(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static Method method(Class<?> type, String name) {
        try {
            Method method = type.getDeclaredMethod(name);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static void same(Object expected, Object actual, String label) {
        check(expected == actual, label);
    }

    static void eq(Object expected, Object actual, String label) {
        check(expected.equals(actual), label + " expected=" + expected + " actual=" + actual);
    }

    static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
