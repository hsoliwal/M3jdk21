/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Contract checks for opt-in M3 functional interfaces
 * @modules jdk.m3.function
 * @run main FunctionalInterfacesTest
 */

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import jdk.m3.function.*;

public class FunctionalInterfacesTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void functionalInterfacesHaveOneSam() throws Exception {
        for (String suffix : new String[] {
                "Consumer", "Function", "Predicate",
                "UnaryOperator", "BinaryOperator", "Supplier"
        }) {
            for (String prefix : new String[] {
                    "Boolean", "Byte", "Short", "Char", "Float"
            }) {
                if (prefix.equals("Boolean") && suffix.equals("Supplier")) {
                    continue; // The standard JDK already provides this shape.
                }
                verifySam("jdk.m3.function." + prefix + suffix);
            }
        }
        for (String simple : new String[] {
                "TriFunction", "TriConsumer", "TriPredicate", "TriOperator",
                "ThrowingFunction", "ThrowingConsumer",
                "ThrowingPredicate", "ThrowingSupplier",
                "ThrowingRunnable", "ThrowingBiFunction", "ThrowingBiConsumer",
                "ThrowingBiPredicate", "ThrowingUnaryOperator",
                "ThrowingBinaryOperator", "IndexedConsumer",
                "IndexedFunction", "IndexedPredicate"
        }) {
            verifySam("jdk.m3.function." + simple);
        }
        for (String prefix : new String[] {
                "Boolean", "Byte", "Short", "Char",
                "Float", "Int", "Long", "Double"
        }) {
            verifySam("jdk.m3.function.Obj" + prefix + "Function");
            verifySam("jdk.m3.function.Obj" + prefix + "Predicate");
            if (!prefix.equals("Int") && !prefix.equals("Long")
                    && !prefix.equals("Double")) {
                verifySam("jdk.m3.function.Obj" + prefix + "Consumer");
            }
        }
        for (String target : new String[] {
                "Byte", "Short", "Char", "Float"
        }) {
            verifySam("jdk.m3.function.To" + target + "Function");
            verifySam("jdk.m3.function.To" + target + "BiFunction");
        }
        for (String source : new String[] {
                "Byte", "Short", "Char", "Int", "Long", "Float", "Double"
        }) {
            for (String target : new String[] {
                    "Byte", "Short", "Char", "Int",
                    "Long", "Float", "Double"
            }) {
                if (source.equals(target)
                        || (isJdkCoreNumeric(source)
                                && isJdkCoreNumeric(target))) continue;
                verifySam("jdk.m3.function." + source + "To" + target
                        + "Function");
            }
        }
    }

    private static boolean isJdkCoreNumeric(String name) {
        return name.equals("Int") || name.equals("Long")
                || name.equals("Double");
    }

    private static void verifySam(String name) throws Exception {
        Class<?> type = Class.forName(name);
        check(type.isAnnotationPresent(FunctionalInterface.class), name + " annotation");
        long abstractMethods = 0;
        for (Method method : type.getMethods()) {
            if (Modifier.isAbstract(method.getModifiers())) {
                abstractMethods++;
            }
        }
        check(abstractMethods == 1, name + " has " + abstractMethods + " SAMs");
    }

    private static void triFunctions() {
        TriFunction<Integer, Integer, Integer, Integer> sum =
                (a, b, c) -> a + b + c;
        check(sum.andThen(x -> x * 2).apply(1, 2, 3) == 12, "tri function");
        TriOperator<String> join = (a, b, c) -> a + b + c;
        check(join.apply("a", "b", "c").equals("abc"), "tri operator");
        StringBuilder calls = new StringBuilder();
        TriConsumer<String, String, String> first =
                (a, b, c) -> calls.append(a).append(b).append(c);
        first.andThen((a, b, c) -> calls.append('!')).accept("a", "b", "c");
        check(calls.toString().equals("abc!"), "tri consumer ordering");
        TriPredicate<Integer, Integer, Integer> falseTest = (a, b, c) -> false;
        check(!falseTest.and((a, b, c) -> {
            throw new AssertionError("non-short-circuit and");
        }).test(1, 2, 3), "tri predicate and");
        check(falseTest.or((a, b, c) -> false).negate().test(1, 2, 3),
                "tri predicate or/negate");
        try {
            sum.andThen(null);
            throw new AssertionError("null mapping was accepted");
        } catch (NullPointerException expected) {
            // Immediate validation, like java.util.function.BiFunction.
        }
    }

    private static void checkedExceptions() throws Exception {
        IOException sentinel = new IOException("same object");
        ThrowingFunction<String, Integer, IOException> parse = input -> {
            if (input == null) throw sentinel;
            return Integer.parseInt(input);
        };
        check(parse.compose(Object::toString).andThen(x -> x + 1).apply(9) == 10,
                "throwing function composition");
        try {
            parse.apply(null);
            throw new AssertionError("missing checked failure");
        } catch (IOException actual) {
            check(actual == sentinel, "checked object was replaced");
        }
        StringBuilder effects = new StringBuilder();
        ThrowingConsumer<String, IOException> consumer = value -> {
            effects.append('a');
            throw sentinel;
        };
        try {
            consumer.andThen(value -> effects.append('b')).accept("value");
            throw new AssertionError("missing consumer failure");
        } catch (IOException actual) {
            check(actual == sentinel && effects.toString().equals("a"),
                    "consumer failure/order");
        }
        ThrowingPredicate<String, IOException> predicate = value -> false;
        check(!predicate.and(value -> {
            throw sentinel;
        }).test("x"), "throwing predicate short circuit");
        check(predicate.negate().test("x"), "throwing predicate negate");
        ThrowingSupplier<String, IOException> supplier = () -> {
            throw sentinel;
        };
        try {
            supplier.get();
            throw new AssertionError("missing supplier failure");
        } catch (IOException actual) {
            check(actual == sentinel, "supplier checked exception");
        }
        ThrowingRunnable<IOException> action = () -> {
            throw sentinel;
        };
        try {
            action.run();
            throw new AssertionError("missing runnable checked failure");
        } catch (IOException actual) {
            check(actual == sentinel, "runnable exception identity");
        }
        ThrowingBiFunction<String, Integer, String, IOException> biFunction =
                (prefix, number) -> {
                    if (prefix == null) throw sentinel;
                    return prefix + number;
                };
        check(biFunction.andThen(String::length).apply("abc", 12) == 5,
                "throwing bi-function composition");
        try {
            biFunction.apply(null, 12);
            throw new AssertionError("missing bi-function failure");
        } catch (IOException actual) {
            check(actual == sentinel, "bi-function exception identity");
        }
        StringBuilder biEffects = new StringBuilder();
        ThrowingBiConsumer<Integer, String, IOException> biConsumer =
                (index, value) -> {
                    biEffects.append(index).append(value);
                    throw sentinel;
                };
        try {
            biConsumer.andThen((index, value) -> biEffects.append('x'))
                    .accept(2, "z");
            throw new AssertionError("missing bi-consumer failure");
        } catch (IOException actual) {
            check(actual == sentinel && biEffects.toString().equals("2z"),
                    "bi-consumer exception and ordering");
        }
        ThrowingBiPredicate<Integer, String, IOException> biPredicate =
                (index, value) -> false;
        check(!biPredicate.and((index, value) -> {
            throw sentinel;
        }).test(0, "x"), "bi-predicate short circuit");
        check(biPredicate.negate().test(0, "x"), "bi-predicate negate");
        ThrowingUnaryOperator<String, IOException> unary = value -> value + "!";
        ThrowingBinaryOperator<String, IOException> binary = (a, b) -> a + b;
        check(unary.apply(binary.apply("a", "b")).equals("ab!"),
                "throwing operators");
    }

    private static void primitives() {
        BytePredicate negative = value -> value < 0;
        check(negative.test((byte) -128), "byte minimum");
        check(!negative.and(value -> {
            throw new AssertionError("byte predicate not short-circuited");
        }).test((byte) 1), "byte short circuit");
        ByteUnaryOperator id = ByteUnaryOperator.identity();
        check(id.compose(v -> (byte) (v + 1)).applyAsByte((byte) -128) == -127,
                "byte unary");
        ShortBinaryOperator plus = (a, b) -> (short) (a + b);
        check(plus.applyAsShort((short) 3, (short) 4) == 7, "short binary");
        CharFunction<String> display = value -> String.valueOf(value);
        check(display.andThen(String::length).apply('x') == 1, "char function");
        CharSupplier letter = () -> '\uD800';
        check(letter.getAsChar() == '\uD800', "unpaired surrogate preserved");
        FloatPredicate isNaN = Float::isNaN;
        check(isNaN.test(Float.NaN), "float NaN predicate");
        FloatUnaryOperator floatIdentity = FloatUnaryOperator.identity();
        check(Float.floatToRawIntBits(floatIdentity.applyAsFloat(-0.0f))
                == Float.floatToRawIntBits(-0.0f), "signed zero preserved");
        BooleanBinaryOperator xor = (a, b) -> a ^ b;
        check(xor.applyAsBoolean(true, false), "boolean binary");
        BooleanFunction<String> boolString = value -> Boolean.toString(value);
        check(Objects.equals(boolString.apply(true), "true"), "boolean function");
        StringBuilder order = new StringBuilder();
        FloatConsumer a = value -> order.append('a');
        a.andThen(value -> order.append('b')).accept(2.5f);
        check(order.toString().equals("ab"), "primitive consumer order");
    }

    private static void indexedAndMixedPrimitive() {
        IndexedFunction<String, String> indexedFunction =
                (index, value) -> index + ":" + value;
        check(indexedFunction.andThen(String::length).apply(12, "abc") == 6,
                "indexed function");
        StringBuilder indexedCalls = new StringBuilder();
        IndexedConsumer<String> first =
                (index, value) -> indexedCalls.append(index).append(value);
        first.andThen((index, value) -> indexedCalls.append('!'))
                .accept(3, "z");
        check(indexedCalls.toString().equals("3z!"),
                "indexed consumer ordering");
        IndexedPredicate<String> indexedPredicate = (index, value) -> false;
        check(!indexedPredicate.and((index, value) -> {
            throw new AssertionError("indexed predicate not short-circuited");
        }).test(0, "x"), "indexed predicate short circuit");
        check(indexedPredicate.negate().test(0, "x"),
                "indexed predicate negate");

        ObjIntFunction<String, String> referencePlusInt =
                (value, index) -> value + index;
        check(referencePlusInt.andThen(String::length).apply("ab", 12) == 4,
                "object + int function");
        ObjFloatPredicate<String> floatPredicate =
                (value, number) -> value.isEmpty() && Float.isNaN(number);
        check(floatPredicate.test("", Float.NaN), "object + float predicate");
        check(!floatPredicate.and((value, number) -> {
            throw new AssertionError("mixed predicate not short-circuited");
        }).test("x", Float.NaN), "mixed predicate short circuit");
        ObjCharConsumer<String> charConsumer =
                (value, character) -> indexedCalls.append(value).append(character);
        charConsumer.andThen((value, character) -> indexedCalls.append('!'))
                .accept("a", '\uD800');
        check(indexedCalls.toString().equals("3z!a\uD800!"),
                "mixed primitive char and consumer ordering");
        ObjBooleanFunction<String, String> booleanFunction =
                (value, flag) -> flag ? value : "";
        check(booleanFunction.apply("x", true).equals("x"),
                "object + boolean function");
    }

    private static void primitiveResultMappings() {
        ToByteFunction<String> bytes = value -> (byte) value.length();
        check(bytes.applyAsByte("abc") == 3, "to-byte function");
        ToShortFunction<String> shorts = value -> (short) value.length();
        check(shorts.applyAsShort("abcd") == 4, "to-short function");
        ToCharBiFunction<String, Integer> character =
                (value, index) -> value.charAt(index);
        check(character.applyAsChar("xyz", 1) == 'y',
                "to-char bi-function");
        ToFloatBiFunction<Integer, Integer> divide =
                (numerator, denominator) -> (float) numerator / denominator;
        check(divide.applyAsFloat(1, 4) == 0.25f, "to-float bi-function");

        ShortToLongFunction widen = value -> value;
        check(widen.applyAsLong(Short.MIN_VALUE) == Short.MIN_VALUE,
                "short-to-long widening");
        FloatToDoubleFunction precise = value -> value;
        check(Double.doubleToRawLongBits(precise.applyAsDouble(-0.0f))
                == Double.doubleToRawLongBits(-0.0d),
                "float-to-double signed zero");
        DoubleToByteFunction narrow = value -> (byte) value;
        check(narrow.applyAsByte(258.0d) == 2,
                "double-to-byte narrowing belongs to lambda");
        LongToFloatFunction longFloat = value -> value;
        check(Float.isFinite(longFloat.applyAsFloat(42L)),
                "long-to-float primitive result");
    }

    public static void main(String[] args) throws Exception {
        functionalInterfacesHaveOneSam();
        triFunctions();
        checkedExceptions();
        primitives();
        indexedAndMixedPrimitive();
        primitiveResultMappings();
    }
}
