/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Every admitted functional shape accepts a typed lambda and preserves its positions.
 * @modules java.base/java.util.function.ext java.base/java.util.function.checked
 * @run main FunctionShapeLambdas12
 */

public class FunctionShapeLambdas12 {
    private static int checks;

    public static void main(String[] args) throws Throwable {
        case0();
        case1();
        case2();
        case3();
        case4();
        case5();
        case6();
        case7();
        case8();
        case9();
        System.out.println("PASS FunctionShapeLambdas12 checks=" + checks);
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError("position/result mismatch");
        }
        checks++;
    }

    private static void case0() throws Throwable {
        java.util.function.ext.ToByteFunction<String> callback = (value1) -> {
            check(value1 == "argument-one");
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte("argument-one") == (byte) -128);
    }

    private static void case1() throws Throwable {
        java.util.function.ext.ToCharBiFunction<String, String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar("argument-one", "argument-two") == '\uD800');
    }

    private static void case2() throws Throwable {
        java.util.function.ext.ToCharFunction<String> callback = (value1) -> {
            check(value1 == "argument-one");
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar("argument-one") == '\uD800');
    }

    private static void case3() throws Throwable {
        java.util.function.ext.ToFloatBiFunction<String, String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat("argument-one", "argument-two")) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case4() throws Throwable {
        java.util.function.ext.ToFloatFunction<String> callback = (value1) -> {
            check(value1 == "argument-one");
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat("argument-one")) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case5() throws Throwable {
        java.util.function.ext.ToShortBiFunction<String, String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort("argument-one", "argument-two") == (short) -32768);
    }

    private static void case6() throws Throwable {
        java.util.function.ext.ToShortFunction<String> callback = (value1) -> {
            check(value1 == "argument-one");
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort("argument-one") == (short) -32768);
    }

    private static void case7() throws Throwable {
        java.util.function.ext.TriConsumer<String, String, String> callback = (value1, value2,
                value3) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            checks++;
        };
        callback.accept("argument-one", "argument-two", "argument-one");
    }

    private static void case8() throws Throwable {
        java.util.function.ext.TriFunction<String, String, String, String> callback = (value1,
                value2, value3) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            checks++;
            return "argument-one";
        };
        check(callback.apply("argument-one", "argument-two", "argument-one") == "argument-one");
    }

    private static void case9() throws Throwable {
        java.util.function.ext.TriPredicate<String, String, String> callback = (value1, value2,
                value3) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            checks++;
            return true;
        };
        check(callback.test("argument-one", "argument-two", "argument-one") == true);
    }
}
