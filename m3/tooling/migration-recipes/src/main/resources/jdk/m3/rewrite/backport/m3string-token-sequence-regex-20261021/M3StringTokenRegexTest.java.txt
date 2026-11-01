/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary M3String token-sequence regex over canonical leaf/range tokens with JNI candidate parity
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringTokenRegexTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

public class M3StringTokenRegexTest {
    private static final Field STRING_M3;
    private static final Class<?> M3;
    private static final Class<?> REGEX;
    private static final Class<?> BUILDER;
    private static final Class<?> QUANTIFIER;
    private static final Method BUILDER_FACTORY;
    private static final Method EXACT;
    private static final Method EXACT_QUANTIFIED;
    private static final Method EXACT_SEQUENCE;
    private static final Method ANY;
    private static final Method ANY_QUANTIFIED;
    private static final Method BUILD;
    private static final Method MATCHES;
    private static final Method LOOKING_AT;
    private static final Method FIND;
    private static final Method NATIVE_MATCHES;
    private static long checks;

    static {
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);
            M3 = Class.forName("java.lang.M3String");
            REGEX = Class.forName("java.lang.M3StringTokenRegex");
            BUILDER = Class.forName("java.lang.M3StringTokenRegex$Builder");
            QUANTIFIER = Class.forName("java.lang.M3StringTokenRegex$Quantifier");
            BUILDER_FACTORY = declared(REGEX, "builder");
            EXACT = declared(BUILDER, "exact", M3);
            EXACT_QUANTIFIED = declared(BUILDER, "exact", M3, QUANTIFIER);
            EXACT_SEQUENCE = declared(BUILDER, "exactSequence", M3);
            ANY = declared(BUILDER, "any");
            ANY_QUANTIFIED = declared(BUILDER, "any", QUANTIFIER);
            BUILD = declared(BUILDER, "build");
            MATCHES = declared(REGEX, "matches", M3);
            LOOKING_AT = declared(REGEX, "lookingAt", M3);
            FIND = declared(REGEX, "find", M3);
            NATIVE_MATCHES =
                    declared(
                            REGEX,
                            "matches0",
                            long[].class,
                            long[].class,
                            byte[].class,
                            byte[].class);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        exactSequence();
        anyAndQuantifiers();
        partialLeafExactness();
        findAndLookingAt();
        nativeFingerprintKernel();
        largeSourceExercisesNativeLane();
        System.out.println("M3_STRING_TOKEN_REGEX_PASS checks=" + checks);
    }

    private static void exactSequence() throws Exception {
        String alpha = fresh("alpha");
        String beta = fresh("beta");
        Object pattern = build(builderExactSequence(alpha.concat(beta)));

        check(matches(pattern, alpha.concat(beta)), "exact sequence");
        check(!matches(pattern, beta.concat(alpha)), "ordered tokens");
        check(!matches(pattern, alpha), "missing token");
    }

    private static void anyAndQuantifiers() throws Exception {
        String alpha = fresh("alpha");
        String beta = fresh("beta");
        String gamma = fresh("gamma");

        Object builder = builder();
        EXACT.invoke(builder, m3(alpha));
        ANY.invoke(builder);
        EXACT.invoke(builder, m3(gamma));
        Object pattern = build(builder);
        check(matches(pattern, alpha.concat(beta).concat(gamma)), "ANY one token");
        check(!matches(pattern, alpha.concat(gamma)), "ANY requires one token");

        Object starBuilder = builder();
        EXACT.invoke(starBuilder, m3(alpha));
        ANY_QUANTIFIED.invoke(starBuilder, quantifier("ZERO_OR_MORE"));
        EXACT.invoke(starBuilder, m3(gamma));
        Object star = build(starBuilder);
        check(matches(star, alpha.concat(gamma)), "ANY* zero");
        check(matches(star, alpha.concat(beta).concat(beta).concat(gamma)), "ANY* many");

        Object optionalBuilder = builder();
        EXACT.invoke(optionalBuilder, m3(alpha));
        EXACT_QUANTIFIED.invoke(optionalBuilder, m3(beta), quantifier("OPTIONAL"));
        EXACT.invoke(optionalBuilder, m3(gamma));
        Object optional = build(optionalBuilder);
        check(matches(optional, alpha.concat(gamma)), "optional absent");
        check(matches(optional, alpha.concat(beta).concat(gamma)), "optional present");

        Object plusBuilder = builder();
        EXACT.invoke(plusBuilder, m3(alpha));
        ANY_QUANTIFIED.invoke(plusBuilder, quantifier("ONE_OR_MORE"));
        EXACT.invoke(plusBuilder, m3(gamma));
        Object plus = build(plusBuilder);
        check(!matches(plus, alpha.concat(gamma)), "ANY+ requires one");
        check(matches(plus, alpha.concat(beta).concat(gamma)), "ANY+ one");
    }

    private static void partialLeafExactness() throws Exception {
        String whole = fresh("__alpha__");
        String slice = whole.substring(2, whole.length() - 2);
        String alpha = fresh("alpha");

        Object builder = builder();
        EXACT.invoke(builder, m3(alpha));
        Object pattern = build(builder);
        check(matches(pattern, slice), "partial leaf equals independent canonical token by text");

        Object exactSliceBuilder = builder();
        EXACT.invoke(exactSliceBuilder, m3(slice));
        Object slicePattern = build(exactSliceBuilder);
        check(matches(slicePattern, alpha), "partial pattern token exactness is symmetric");
    }

    private static void findAndLookingAt() throws Exception {
        String alpha = fresh("alpha");
        String beta = fresh("beta");
        String prefix = fresh("prefix");
        String suffix = fresh("suffix");
        Object pattern = build(builderExactSequence(alpha.concat(beta)));

        check(lookingAt(pattern, alpha.concat(beta).concat(suffix)), "lookingAt token prefix");
        check(!lookingAt(pattern, prefix.concat(alpha).concat(beta)), "lookingAt refuses later match");
        check(find(pattern, prefix.concat(alpha).concat(beta).concat(suffix)), "find token sequence");
        check(!find(pattern, prefix.concat(beta).concat(alpha).concat(suffix)), "find respects order");
    }

    private static void nativeFingerprintKernel() throws Exception {
        long alpha = fingerprint("alpha");
        long beta = fingerprint("beta");
        long gamma = fingerprint("gamma");

        int result =
                (int)
                        NATIVE_MATCHES.invoke(
                                null,
                                new long[] {alpha, beta, gamma},
                                new long[] {alpha, 0L, gamma},
                                new byte[] {0, 1, 0},
                                new byte[] {0, 0, 0});
        check(result == 1, "native ANY candidate");

        int no =
                (int)
                        NATIVE_MATCHES.invoke(
                                null,
                                new long[] {alpha, beta},
                                new long[] {alpha, gamma},
                                new byte[] {0, 0},
                                new byte[] {0, 0});
        check(no == 0, "native exact rejection");

        int invalid =
                (int)
                        NATIVE_MATCHES.invoke(
                                null,
                                new long[] {alpha},
                                new long[] {alpha},
                                new byte[] {9},
                                new byte[] {0});
        check(invalid == -1, "native invalid geometry");
    }

    private static void largeSourceExercisesNativeLane() throws Exception {
        String token = fresh("x");
        String end = fresh("end");
        String source = fresh("");
        for (int index = 0; index < 96; index++) {
            source = source.concat(token);
        }
        source = source.concat(end);

        Object builder = builder();
        ANY_QUANTIFIED.invoke(builder, quantifier("ZERO_OR_MORE"));
        EXACT.invoke(builder, m3(end));
        Object pattern = build(builder);
        check(matches(pattern, source), "large token source native-candidate lane");
    }

    private static Object builderExactSequence(String sequence) throws Exception {
        Object builder = builder();
        EXACT_SEQUENCE.invoke(builder, m3(sequence));
        return builder;
    }

    private static Object builder() throws Exception {
        return BUILDER_FACTORY.invoke(null);
    }

    private static Object build(Object builder) throws Exception {
        return BUILD.invoke(builder);
    }

    private static boolean matches(Object regex, String value) throws Exception {
        return (boolean) MATCHES.invoke(regex, m3(value));
    }

    private static boolean lookingAt(Object regex, String value) throws Exception {
        return (boolean) LOOKING_AT.invoke(regex, m3(value));
    }

    private static boolean find(Object regex, String value) throws Exception {
        return (boolean) FIND.invoke(regex, m3(value));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object quantifier(String name) {
        return Enum.valueOf((Class) QUANTIFIER, name);
    }

    private static Object m3(String value) throws Exception {
        value.length();
        Object backing = STRING_M3.get(value);
        if (backing == null) throw new AssertionError("String is not M3-backed: " + value);
        return backing;
    }

    private static long fingerprint(String value) {
        return ((long) value.hashCode() << 32) | Integer.toUnsignedLong(value.length());
    }

    private static String fresh(String value) {
        return new String(value.toCharArray());
    }

    private static Method declared(Class<?> owner, String name, Class<?>... parameters)
            throws ReflectiveOperationException {
        Method method = owner.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
