/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Every admitted functional shape accepts a typed lambda and preserves its positions.
 * @modules java.base/java.util.function.ext java.base/java.util.function.checked
 * @run main FunctionShapeLambdas11
 */

public class FunctionShapeLambdas11 {
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
        case10();
        case11();
        case12();
        case13();
        case14();
        case15();
        case16();
        case17();
        case18();
        case19();
        case20();
        case21();
        case22();
        case23();
        case24();
        case25();
        case26();
        case27();
        case28();
        case29();
        case30();
        case31();
        case32();
        case33();
        case34();
        case35();
        case36();
        case37();
        case38();
        case39();
        case40();
        case41();
        case42();
        case43();
        case44();
        case45();
        case46();
        case47();
        case48();
        case49();
        case50();
        case51();
        case52();
        case53();
        case54();
        case55();
        case56();
        case57();
        case58();
        case59();
        case60();
        case61();
        case62();
        case63();
        case64();
        case65();
        case66();
        case67();
        case68();
        case69();
        case70();
        case71();
        case72();
        case73();
        case74();
        case75();
        case76();
        case77();
        case78();
        case79();
        case80();
        case81();
        case82();
        case83();
        case84();
        case85();
        case86();
        case87();
        case88();
        case89();
        case90();
        case91();
        case92();
        case93();
        case94();
        case95();
        case96();
        case97();
        case98();
        case99();
        case100();
        case101();
        case102();
        case103();
        case104();
        case105();
        case106();
        case107();
        case108();
        case109();
        case110();
        case111();
        case112();
        case113();
        case114();
        case115();
        case116();
        case117();
        case118();
        case119();
        case120();
        case121();
        case122();
        case123();
        case124();
        case125();
        case126();
        case127();
        case128();
        case129();
        case130();
        case131();
        case132();
        case133();
        case134();
        case135();
        case136();
        case137();
        case138();
        case139();
        case140();
        case141();
        case142();
        case143();
        case144();
        case145();
        case146();
        case147();
        case148();
        case149();
        case150();
        case151();
        case152();
        case153();
        case154();
        case155();
        case156();
        case157();
        case158();
        case159();
        System.out.println("PASS FunctionShapeLambdas11 checks=" + checks);
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError("position/result mismatch");
        }
        checks++;
    }

    private static void case0() throws Throwable {
        java.util.function.ext.ObjDoubleToCharFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar("argument-one", Double.NEGATIVE_INFINITY) == '\uD800');
    }

    private static void case1() throws Throwable {
        java.util.function.ext.ObjDoubleToDoubleFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble("argument-one", Double.NEGATIVE_INFINITY)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case2() throws Throwable {
        java.util.function.ext.ObjDoubleToFloatFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat("argument-one", Double.NEGATIVE_INFINITY)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case3() throws Throwable {
        java.util.function.ext.ObjDoubleToIntFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt("argument-one", Double.NEGATIVE_INFINITY) == Integer.MIN_VALUE);
    }

    private static void case4() throws Throwable {
        java.util.function.ext.ObjDoubleToLongFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong("argument-one", Double.NEGATIVE_INFINITY) == Long.MIN_VALUE);
    }

    private static void case5() throws Throwable {
        java.util.function.ext.ObjDoubleToShortFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort("argument-one", Double.NEGATIVE_INFINITY) == (short) -32768);
    }

    private static void case6() throws Throwable {
        java.util.function.ext.ObjFloatConsumer<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
        };
        callback.accept("argument-one", Float.POSITIVE_INFINITY);
    }

    private static void case7() throws Throwable {
        java.util.function.ext.ObjFloatFunction<String, String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return "argument-one";
        };
        check(callback.apply("argument-one", Float.POSITIVE_INFINITY) == "argument-one");
    }

    private static void case8() throws Throwable {
        java.util.function.ext.ObjFloatPredicate<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return true;
        };
        check(callback.test("argument-one", Float.POSITIVE_INFINITY) == true);
    }

    private static void case9() throws Throwable {
        java.util.function.ext.ObjFloatToByteFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte("argument-one", Float.POSITIVE_INFINITY) == (byte) -128);
    }

    private static void case10() throws Throwable {
        java.util.function.ext.ObjFloatToCharFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar("argument-one", Float.POSITIVE_INFINITY) == '\uD800');
    }

    private static void case11() throws Throwable {
        java.util.function.ext.ObjFloatToDoubleFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble("argument-one", Float.POSITIVE_INFINITY)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case12() throws Throwable {
        java.util.function.ext.ObjFloatToFloatFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat("argument-one", Float.POSITIVE_INFINITY)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case13() throws Throwable {
        java.util.function.ext.ObjFloatToIntFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt("argument-one", Float.POSITIVE_INFINITY) == Integer.MIN_VALUE);
    }

    private static void case14() throws Throwable {
        java.util.function.ext.ObjFloatToLongFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong("argument-one", Float.POSITIVE_INFINITY) == Long.MIN_VALUE);
    }

    private static void case15() throws Throwable {
        java.util.function.ext.ObjFloatToShortFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort("argument-one", Float.POSITIVE_INFINITY) == (short) -32768);
    }

    private static void case16() throws Throwable {
        java.util.function.ext.ObjIntFunction<String, String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply("argument-one", Integer.MAX_VALUE) == "argument-one");
    }

    private static void case17() throws Throwable {
        java.util.function.ext.ObjIntPredicate<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test("argument-one", Integer.MAX_VALUE) == true);
    }

    private static void case18() throws Throwable {
        java.util.function.ext.ObjIntToByteFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte("argument-one", Integer.MAX_VALUE) == (byte) -128);
    }

    private static void case19() throws Throwable {
        java.util.function.ext.ObjIntToCharFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar("argument-one", Integer.MAX_VALUE) == '\uD800');
    }

    private static void case20() throws Throwable {
        java.util.function.ext.ObjIntToDoubleFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble("argument-one", Integer.MAX_VALUE)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case21() throws Throwable {
        java.util.function.ext.ObjIntToFloatFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat("argument-one", Integer.MAX_VALUE)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case22() throws Throwable {
        java.util.function.ext.ObjIntToIntFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt("argument-one", Integer.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case23() throws Throwable {
        java.util.function.ext.ObjIntToLongFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong("argument-one", Integer.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case24() throws Throwable {
        java.util.function.ext.ObjIntToShortFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort("argument-one", Integer.MAX_VALUE) == (short) -32768);
    }

    private static void case25() throws Throwable {
        java.util.function.ext.ObjLongFunction<String, String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == Long.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply("argument-one", Long.MAX_VALUE) == "argument-one");
    }

    private static void case26() throws Throwable {
        java.util.function.ext.ObjLongPredicate<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == Long.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test("argument-one", Long.MAX_VALUE) == true);
    }

    private static void case27() throws Throwable {
        java.util.function.ext.ObjLongToByteFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte("argument-one", Long.MAX_VALUE) == (byte) -128);
    }

    private static void case28() throws Throwable {
        java.util.function.ext.ObjLongToCharFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == Long.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar("argument-one", Long.MAX_VALUE) == '\uD800');
    }

    private static void case29() throws Throwable {
        java.util.function.ext.ObjLongToDoubleFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble("argument-one", Long.MAX_VALUE)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case30() throws Throwable {
        java.util.function.ext.ObjLongToFloatFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat("argument-one", Long.MAX_VALUE)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case31() throws Throwable {
        java.util.function.ext.ObjLongToIntFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt("argument-one", Long.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case32() throws Throwable {
        java.util.function.ext.ObjLongToLongFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong("argument-one", Long.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case33() throws Throwable {
        java.util.function.ext.ObjLongToShortFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort("argument-one", Long.MAX_VALUE) == (short) -32768);
    }

    private static void case34() throws Throwable {
        java.util.function.ext.ObjShortConsumer<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == (short) 32767);
            checks++;
        };
        callback.accept("argument-one", (short) 32767);
    }

    private static void case35() throws Throwable {
        java.util.function.ext.ObjShortFunction<String, String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == (short) 32767);
            checks++;
            return "argument-one";
        };
        check(callback.apply("argument-one", (short) 32767) == "argument-one");
    }

    private static void case36() throws Throwable {
        java.util.function.ext.ObjShortPredicate<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == (short) 32767);
            checks++;
            return true;
        };
        check(callback.test("argument-one", (short) 32767) == true);
    }

    private static void case37() throws Throwable {
        java.util.function.ext.ObjShortToByteFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == (short) 32767);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte("argument-one", (short) 32767) == (byte) -128);
    }

    private static void case38() throws Throwable {
        java.util.function.ext.ObjShortToCharFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == (short) 32767);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar("argument-one", (short) 32767) == '\uD800');
    }

    private static void case39() throws Throwable {
        java.util.function.ext.ObjShortToDoubleFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == (short) 32767);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble("argument-one", (short) 32767)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case40() throws Throwable {
        java.util.function.ext.ObjShortToFloatFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == (short) 32767);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat("argument-one", (short) 32767)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case41() throws Throwable {
        java.util.function.ext.ObjShortToIntFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == (short) 32767);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt("argument-one", (short) 32767) == Integer.MIN_VALUE);
    }

    private static void case42() throws Throwable {
        java.util.function.ext.ObjShortToLongFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == (short) 32767);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong("argument-one", (short) 32767) == Long.MIN_VALUE);
    }

    private static void case43() throws Throwable {
        java.util.function.ext.ObjShortToShortFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == (short) 32767);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort("argument-one", (short) 32767) == (short) -32768);
    }

    private static void case44() throws Throwable {
        java.util.function.ext.Predicate10<String, String, String, String, String, String, String,
                String, String, String> callback = (value1, value2, value3, value4, value5, value6,
                value7, value8, value9, value10) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            check(value5 == "argument-one");
            check(value6 == "argument-two");
            check(value7 == "argument-one");
            check(value8 == "argument-two");
            check(value9 == "argument-one");
            check(value10 == "argument-two");
            checks++;
            return true;
        };
        check(callback.test("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two") == true);
    }

    private static void case45() throws Throwable {
        java.util.function.ext.Predicate11<String, String, String, String, String, String, String,
                String, String, String, String> callback = (value1, value2, value3, value4, value5,
                value6, value7, value8, value9, value10, value11) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            check(value5 == "argument-one");
            check(value6 == "argument-two");
            check(value7 == "argument-one");
            check(value8 == "argument-two");
            check(value9 == "argument-one");
            check(value10 == "argument-two");
            check(value11 == "argument-one");
            checks++;
            return true;
        };
        check(callback.test("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one") == true);
    }

    private static void case46() throws Throwable {
        java.util.function.ext.Predicate12<String, String, String, String, String, String, String,
                String, String, String, String, String> callback = (value1, value2, value3, value4,
                value5, value6, value7, value8, value9, value10, value11, value12) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            check(value5 == "argument-one");
            check(value6 == "argument-two");
            check(value7 == "argument-one");
            check(value8 == "argument-two");
            check(value9 == "argument-one");
            check(value10 == "argument-two");
            check(value11 == "argument-one");
            check(value12 == "argument-two");
            checks++;
            return true;
        };
        check(callback.test("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two") == true);
    }

    private static void case47() throws Throwable {
        java.util.function.ext.Predicate13<String, String, String, String, String, String, String,
                String, String, String, String, String, String> callback = (value1, value2, value3,
                value4, value5, value6, value7, value8, value9, value10, value11, value12, value13)
                -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            check(value5 == "argument-one");
            check(value6 == "argument-two");
            check(value7 == "argument-one");
            check(value8 == "argument-two");
            check(value9 == "argument-one");
            check(value10 == "argument-two");
            check(value11 == "argument-one");
            check(value12 == "argument-two");
            check(value13 == "argument-one");
            checks++;
            return true;
        };
        check(callback.test("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one") == true);
    }

    private static void case48() throws Throwable {
        java.util.function.ext.Predicate14<String, String, String, String, String, String, String,
                String, String, String, String, String, String, String> callback = (value1, value2,
                value3, value4, value5, value6, value7, value8, value9, value10, value11, value12,
                value13, value14) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            check(value5 == "argument-one");
            check(value6 == "argument-two");
            check(value7 == "argument-one");
            check(value8 == "argument-two");
            check(value9 == "argument-one");
            check(value10 == "argument-two");
            check(value11 == "argument-one");
            check(value12 == "argument-two");
            check(value13 == "argument-one");
            check(value14 == "argument-two");
            checks++;
            return true;
        };
        check(callback.test("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two") == true);
    }

    private static void case49() throws Throwable {
        java.util.function.ext.Predicate15<String, String, String, String, String, String, String,
                String, String, String, String, String, String, String, String> callback = (value1,
                value2, value3, value4, value5, value6, value7, value8, value9, value10, value11,
                value12, value13, value14, value15) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            check(value5 == "argument-one");
            check(value6 == "argument-two");
            check(value7 == "argument-one");
            check(value8 == "argument-two");
            check(value9 == "argument-one");
            check(value10 == "argument-two");
            check(value11 == "argument-one");
            check(value12 == "argument-two");
            check(value13 == "argument-one");
            check(value14 == "argument-two");
            check(value15 == "argument-one");
            checks++;
            return true;
        };
        check(callback.test("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one") == true);
    }

    private static void case50() throws Throwable {
        java.util.function.ext.Predicate16<String, String, String, String, String, String, String,
                String, String, String, String, String, String, String, String, String> callback =
                (value1, value2, value3, value4, value5, value6, value7, value8, value9, value10,
                value11, value12, value13, value14, value15, value16) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            check(value5 == "argument-one");
            check(value6 == "argument-two");
            check(value7 == "argument-one");
            check(value8 == "argument-two");
            check(value9 == "argument-one");
            check(value10 == "argument-two");
            check(value11 == "argument-one");
            check(value12 == "argument-two");
            check(value13 == "argument-one");
            check(value14 == "argument-two");
            check(value15 == "argument-one");
            check(value16 == "argument-two");
            checks++;
            return true;
        };
        check(callback.test("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two") == true);
    }

    private static void case51() throws Throwable {
        java.util.function.ext.Predicate4<String, String, String, String> callback = (value1,
                value2, value3, value4) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            checks++;
            return true;
        };
        check(callback.test("argument-one", "argument-two", "argument-one", "argument-two") == true);
    }

    private static void case52() throws Throwable {
        java.util.function.ext.Predicate5<String, String, String, String, String> callback =
                (value1, value2, value3, value4, value5) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            check(value5 == "argument-one");
            checks++;
            return true;
        };
        check(callback.test("argument-one", "argument-two", "argument-one", "argument-two", "argument-one") == true);
    }

    private static void case53() throws Throwable {
        java.util.function.ext.Predicate6<String, String, String, String, String, String> callback =
                (value1, value2, value3, value4, value5, value6) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            check(value5 == "argument-one");
            check(value6 == "argument-two");
            checks++;
            return true;
        };
        check(callback.test("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two") == true);
    }

    private static void case54() throws Throwable {
        java.util.function.ext.Predicate7<String, String, String, String, String, String, String>
                callback = (value1, value2, value3, value4, value5, value6, value7) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            check(value5 == "argument-one");
            check(value6 == "argument-two");
            check(value7 == "argument-one");
            checks++;
            return true;
        };
        check(callback.test("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one") == true);
    }

    private static void case55() throws Throwable {
        java.util.function.ext.Predicate8<String, String, String, String, String, String, String,
                String> callback = (value1, value2, value3, value4, value5, value6, value7, value8)
                -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            check(value5 == "argument-one");
            check(value6 == "argument-two");
            check(value7 == "argument-one");
            check(value8 == "argument-two");
            checks++;
            return true;
        };
        check(callback.test("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two") == true);
    }

    private static void case56() throws Throwable {
        java.util.function.ext.Predicate9<String, String, String, String, String, String, String,
                String, String> callback = (value1, value2, value3, value4, value5, value6, value7,
                value8, value9) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            check(value5 == "argument-one");
            check(value6 == "argument-two");
            check(value7 == "argument-one");
            check(value8 == "argument-two");
            check(value9 == "argument-one");
            checks++;
            return true;
        };
        check(callback.test("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one") == true);
    }

    private static void case57() throws Throwable {
        java.util.function.ext.ShortBiConsumer callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == (short) 32767);
            checks++;
        };
        callback.accept((short) -32768, (short) 32767);
    }

    private static void case58() throws Throwable {
        java.util.function.ext.ShortBiFunction<String> callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == (short) 32767);
            checks++;
            return "argument-one";
        };
        check(callback.apply((short) -32768, (short) 32767) == "argument-one");
    }

    private static void case59() throws Throwable {
        java.util.function.ext.ShortBiPredicate callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == (short) 32767);
            checks++;
            return true;
        };
        check(callback.test((short) -32768, (short) 32767) == true);
    }

    private static void case60() throws Throwable {
        java.util.function.ext.ShortBiToByteFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == (short) 32767);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((short) -32768, (short) 32767) == (byte) -128);
    }

    private static void case61() throws Throwable {
        java.util.function.ext.ShortBiToCharFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == (short) 32767);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((short) -32768, (short) 32767) == '\uD800');
    }

    private static void case62() throws Throwable {
        java.util.function.ext.ShortBiToDoubleFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == (short) 32767);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((short) -32768, (short) 32767)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case63() throws Throwable {
        java.util.function.ext.ShortBiToFloatFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == (short) 32767);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((short) -32768, (short) 32767)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case64() throws Throwable {
        java.util.function.ext.ShortBiToIntFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == (short) 32767);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((short) -32768, (short) 32767) == Integer.MIN_VALUE);
    }

    private static void case65() throws Throwable {
        java.util.function.ext.ShortBiToLongFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == (short) 32767);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((short) -32768, (short) 32767) == Long.MIN_VALUE);
    }

    private static void case66() throws Throwable {
        java.util.function.ext.ShortBinaryOperator callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == (short) 32767);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((short) -32768, (short) 32767) == (short) -32768);
    }

    private static void case67() throws Throwable {
        java.util.function.ext.ShortBooleanConsumer callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == false);
            checks++;
        };
        callback.accept((short) -32768, false);
    }

    private static void case68() throws Throwable {
        java.util.function.ext.ShortBooleanFunction<String> callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == false);
            checks++;
            return "argument-one";
        };
        check(callback.apply((short) -32768, false) == "argument-one");
    }

    private static void case69() throws Throwable {
        java.util.function.ext.ShortBooleanPredicate callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == false);
            checks++;
            return true;
        };
        check(callback.test((short) -32768, false) == true);
    }

    private static void case70() throws Throwable {
        java.util.function.ext.ShortBooleanToByteFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == false);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((short) -32768, false) == (byte) -128);
    }

    private static void case71() throws Throwable {
        java.util.function.ext.ShortBooleanToCharFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == false);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((short) -32768, false) == '\uD800');
    }

    private static void case72() throws Throwable {
        java.util.function.ext.ShortBooleanToDoubleFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == false);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((short) -32768, false)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case73() throws Throwable {
        java.util.function.ext.ShortBooleanToFloatFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == false);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((short) -32768, false)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case74() throws Throwable {
        java.util.function.ext.ShortBooleanToIntFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == false);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((short) -32768, false) == Integer.MIN_VALUE);
    }

    private static void case75() throws Throwable {
        java.util.function.ext.ShortBooleanToLongFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == false);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((short) -32768, false) == Long.MIN_VALUE);
    }

    private static void case76() throws Throwable {
        java.util.function.ext.ShortBooleanToShortFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == false);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((short) -32768, false) == (short) -32768);
    }

    private static void case77() throws Throwable {
        java.util.function.ext.ShortByteConsumer callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == (byte) 127);
            checks++;
        };
        callback.accept((short) -32768, (byte) 127);
    }

    private static void case78() throws Throwable {
        java.util.function.ext.ShortByteFunction<String> callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == (byte) 127);
            checks++;
            return "argument-one";
        };
        check(callback.apply((short) -32768, (byte) 127) == "argument-one");
    }

    private static void case79() throws Throwable {
        java.util.function.ext.ShortBytePredicate callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == (byte) 127);
            checks++;
            return true;
        };
        check(callback.test((short) -32768, (byte) 127) == true);
    }

    private static void case80() throws Throwable {
        java.util.function.ext.ShortByteToByteFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == (byte) 127);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((short) -32768, (byte) 127) == (byte) -128);
    }

    private static void case81() throws Throwable {
        java.util.function.ext.ShortByteToCharFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == (byte) 127);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((short) -32768, (byte) 127) == '\uD800');
    }

    private static void case82() throws Throwable {
        java.util.function.ext.ShortByteToDoubleFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == (byte) 127);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((short) -32768, (byte) 127)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case83() throws Throwable {
        java.util.function.ext.ShortByteToFloatFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == (byte) 127);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((short) -32768, (byte) 127)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case84() throws Throwable {
        java.util.function.ext.ShortByteToIntFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == (byte) 127);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((short) -32768, (byte) 127) == Integer.MIN_VALUE);
    }

    private static void case85() throws Throwable {
        java.util.function.ext.ShortByteToLongFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == (byte) 127);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((short) -32768, (byte) 127) == Long.MIN_VALUE);
    }

    private static void case86() throws Throwable {
        java.util.function.ext.ShortByteToShortFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == (byte) 127);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((short) -32768, (byte) 127) == (short) -32768);
    }

    private static void case87() throws Throwable {
        java.util.function.ext.ShortCharConsumer callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == '\uFFFF');
            checks++;
        };
        callback.accept((short) -32768, '\uFFFF');
    }

    private static void case88() throws Throwable {
        java.util.function.ext.ShortCharFunction<String> callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == '\uFFFF');
            checks++;
            return "argument-one";
        };
        check(callback.apply((short) -32768, '\uFFFF') == "argument-one");
    }

    private static void case89() throws Throwable {
        java.util.function.ext.ShortCharPredicate callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == '\uFFFF');
            checks++;
            return true;
        };
        check(callback.test((short) -32768, '\uFFFF') == true);
    }

    private static void case90() throws Throwable {
        java.util.function.ext.ShortCharToByteFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == '\uFFFF');
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((short) -32768, '\uFFFF') == (byte) -128);
    }

    private static void case91() throws Throwable {
        java.util.function.ext.ShortCharToCharFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == '\uFFFF');
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((short) -32768, '\uFFFF') == '\uD800');
    }

    private static void case92() throws Throwable {
        java.util.function.ext.ShortCharToDoubleFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == '\uFFFF');
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((short) -32768, '\uFFFF')) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case93() throws Throwable {
        java.util.function.ext.ShortCharToFloatFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == '\uFFFF');
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((short) -32768, '\uFFFF')) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case94() throws Throwable {
        java.util.function.ext.ShortCharToIntFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == '\uFFFF');
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((short) -32768, '\uFFFF') == Integer.MIN_VALUE);
    }

    private static void case95() throws Throwable {
        java.util.function.ext.ShortCharToLongFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == '\uFFFF');
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((short) -32768, '\uFFFF') == Long.MIN_VALUE);
    }

    private static void case96() throws Throwable {
        java.util.function.ext.ShortCharToShortFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == '\uFFFF');
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((short) -32768, '\uFFFF') == (short) -32768);
    }

    private static void case97() throws Throwable {
        java.util.function.ext.ShortComparator callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == (short) 32767);
            checks++;
            return 0;
        };
        check(callback.compare((short) -32768, (short) 32767) == 0);
    }

    private static void case98() throws Throwable {
        java.util.function.ext.ShortConsumer callback = (value1) -> {
            check(value1 == (short) -32768);
            checks++;
        };
        callback.accept((short) -32768);
    }

    private static void case99() throws Throwable {
        java.util.function.ext.ShortDoubleConsumer callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
        };
        callback.accept((short) -32768, Double.NEGATIVE_INFINITY);
    }

    private static void case100() throws Throwable {
        java.util.function.ext.ShortDoubleFunction<String> callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return "argument-one";
        };
        check(callback.apply((short) -32768, Double.NEGATIVE_INFINITY) == "argument-one");
    }

    private static void case101() throws Throwable {
        java.util.function.ext.ShortDoublePredicate callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return true;
        };
        check(callback.test((short) -32768, Double.NEGATIVE_INFINITY) == true);
    }

    private static void case102() throws Throwable {
        java.util.function.ext.ShortDoubleToByteFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((short) -32768, Double.NEGATIVE_INFINITY) == (byte) -128);
    }

    private static void case103() throws Throwable {
        java.util.function.ext.ShortDoubleToCharFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((short) -32768, Double.NEGATIVE_INFINITY) == '\uD800');
    }

    private static void case104() throws Throwable {
        java.util.function.ext.ShortDoubleToDoubleFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((short) -32768,
                Double.NEGATIVE_INFINITY)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case105() throws Throwable {
        java.util.function.ext.ShortDoubleToFloatFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((short) -32768,
                Double.NEGATIVE_INFINITY)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case106() throws Throwable {
        java.util.function.ext.ShortDoubleToIntFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((short) -32768, Double.NEGATIVE_INFINITY) == Integer.MIN_VALUE);
    }

    private static void case107() throws Throwable {
        java.util.function.ext.ShortDoubleToLongFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((short) -32768, Double.NEGATIVE_INFINITY) == Long.MIN_VALUE);
    }

    private static void case108() throws Throwable {
        java.util.function.ext.ShortDoubleToShortFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((short) -32768, Double.NEGATIVE_INFINITY) == (short) -32768);
    }

    private static void case109() throws Throwable {
        java.util.function.ext.ShortFloatConsumer callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
        };
        callback.accept((short) -32768, Float.POSITIVE_INFINITY);
    }

    private static void case110() throws Throwable {
        java.util.function.ext.ShortFloatFunction<String> callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return "argument-one";
        };
        check(callback.apply((short) -32768, Float.POSITIVE_INFINITY) == "argument-one");
    }

    private static void case111() throws Throwable {
        java.util.function.ext.ShortFloatPredicate callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return true;
        };
        check(callback.test((short) -32768, Float.POSITIVE_INFINITY) == true);
    }

    private static void case112() throws Throwable {
        java.util.function.ext.ShortFloatToByteFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((short) -32768, Float.POSITIVE_INFINITY) == (byte) -128);
    }

    private static void case113() throws Throwable {
        java.util.function.ext.ShortFloatToCharFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((short) -32768, Float.POSITIVE_INFINITY) == '\uD800');
    }

    private static void case114() throws Throwable {
        java.util.function.ext.ShortFloatToDoubleFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((short) -32768,
                Float.POSITIVE_INFINITY)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case115() throws Throwable {
        java.util.function.ext.ShortFloatToFloatFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((short) -32768,
                Float.POSITIVE_INFINITY)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case116() throws Throwable {
        java.util.function.ext.ShortFloatToIntFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((short) -32768, Float.POSITIVE_INFINITY) == Integer.MIN_VALUE);
    }

    private static void case117() throws Throwable {
        java.util.function.ext.ShortFloatToLongFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((short) -32768, Float.POSITIVE_INFINITY) == Long.MIN_VALUE);
    }

    private static void case118() throws Throwable {
        java.util.function.ext.ShortFloatToShortFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((short) -32768, Float.POSITIVE_INFINITY) == (short) -32768);
    }

    private static void case119() throws Throwable {
        java.util.function.ext.ShortFunction<String> callback = (value1) -> {
            check(value1 == (short) -32768);
            checks++;
            return "argument-one";
        };
        check(callback.apply((short) -32768) == "argument-one");
    }

    private static void case120() throws Throwable {
        java.util.function.ext.ShortIntConsumer callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Integer.MAX_VALUE);
            checks++;
        };
        callback.accept((short) -32768, Integer.MAX_VALUE);
    }

    private static void case121() throws Throwable {
        java.util.function.ext.ShortIntFunction<String> callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply((short) -32768, Integer.MAX_VALUE) == "argument-one");
    }

    private static void case122() throws Throwable {
        java.util.function.ext.ShortIntPredicate callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test((short) -32768, Integer.MAX_VALUE) == true);
    }

    private static void case123() throws Throwable {
        java.util.function.ext.ShortIntToByteFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((short) -32768, Integer.MAX_VALUE) == (byte) -128);
    }

    private static void case124() throws Throwable {
        java.util.function.ext.ShortIntToCharFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((short) -32768, Integer.MAX_VALUE) == '\uD800');
    }

    private static void case125() throws Throwable {
        java.util.function.ext.ShortIntToDoubleFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((short) -32768, Integer.MAX_VALUE))
                == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case126() throws Throwable {
        java.util.function.ext.ShortIntToFloatFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((short) -32768, Integer.MAX_VALUE)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case127() throws Throwable {
        java.util.function.ext.ShortIntToIntFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((short) -32768, Integer.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case128() throws Throwable {
        java.util.function.ext.ShortIntToLongFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((short) -32768, Integer.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case129() throws Throwable {
        java.util.function.ext.ShortIntToShortFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((short) -32768, Integer.MAX_VALUE) == (short) -32768);
    }

    private static void case130() throws Throwable {
        java.util.function.ext.ShortLongConsumer callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Long.MAX_VALUE);
            checks++;
        };
        callback.accept((short) -32768, Long.MAX_VALUE);
    }

    private static void case131() throws Throwable {
        java.util.function.ext.ShortLongFunction<String> callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply((short) -32768, Long.MAX_VALUE) == "argument-one");
    }

    private static void case132() throws Throwable {
        java.util.function.ext.ShortLongPredicate callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test((short) -32768, Long.MAX_VALUE) == true);
    }

    private static void case133() throws Throwable {
        java.util.function.ext.ShortLongToByteFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((short) -32768, Long.MAX_VALUE) == (byte) -128);
    }

    private static void case134() throws Throwable {
        java.util.function.ext.ShortLongToCharFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((short) -32768, Long.MAX_VALUE) == '\uD800');
    }

    private static void case135() throws Throwable {
        java.util.function.ext.ShortLongToDoubleFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((short) -32768, Long.MAX_VALUE)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case136() throws Throwable {
        java.util.function.ext.ShortLongToFloatFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((short) -32768, Long.MAX_VALUE)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case137() throws Throwable {
        java.util.function.ext.ShortLongToIntFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((short) -32768, Long.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case138() throws Throwable {
        java.util.function.ext.ShortLongToLongFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((short) -32768, Long.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case139() throws Throwable {
        java.util.function.ext.ShortLongToShortFunction callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((short) -32768, Long.MAX_VALUE) == (short) -32768);
    }

    private static void case140() throws Throwable {
        java.util.function.ext.ShortObjConsumer<String> callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == "argument-two");
            checks++;
        };
        callback.accept((short) -32768, "argument-two");
    }

    private static void case141() throws Throwable {
        java.util.function.ext.ShortObjFunction<String, String> callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == "argument-two");
            checks++;
            return "argument-one";
        };
        check(callback.apply((short) -32768, "argument-two") == "argument-one");
    }

    private static void case142() throws Throwable {
        java.util.function.ext.ShortObjPredicate<String> callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == "argument-two");
            checks++;
            return true;
        };
        check(callback.test((short) -32768, "argument-two") == true);
    }

    private static void case143() throws Throwable {
        java.util.function.ext.ShortObjToByteFunction<String> callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == "argument-two");
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((short) -32768, "argument-two") == (byte) -128);
    }

    private static void case144() throws Throwable {
        java.util.function.ext.ShortObjToCharFunction<String> callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == "argument-two");
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((short) -32768, "argument-two") == '\uD800');
    }

    private static void case145() throws Throwable {
        java.util.function.ext.ShortObjToDoubleFunction<String> callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == "argument-two");
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((short) -32768, "argument-two")) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case146() throws Throwable {
        java.util.function.ext.ShortObjToFloatFunction<String> callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == "argument-two");
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((short) -32768, "argument-two")) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case147() throws Throwable {
        java.util.function.ext.ShortObjToIntFunction<String> callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == "argument-two");
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((short) -32768, "argument-two") == Integer.MIN_VALUE);
    }

    private static void case148() throws Throwable {
        java.util.function.ext.ShortObjToLongFunction<String> callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == "argument-two");
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((short) -32768, "argument-two") == Long.MIN_VALUE);
    }

    private static void case149() throws Throwable {
        java.util.function.ext.ShortObjToShortFunction<String> callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == "argument-two");
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((short) -32768, "argument-two") == (short) -32768);
    }

    private static void case150() throws Throwable {
        java.util.function.ext.ShortPredicate callback = (value1) -> {
            check(value1 == (short) -32768);
            checks++;
            return true;
        };
        check(callback.test((short) -32768) == true);
    }

    private static void case151() throws Throwable {
        java.util.function.ext.ShortSupplier callback = () -> {
            checks++;
            return (short) -32768;
        };
        check(callback.getAsShort() == (short) -32768);
    }

    private static void case152() throws Throwable {
        java.util.function.ext.ShortToByteFunction callback = (value1) -> {
            check(value1 == (short) -32768);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((short) -32768) == (byte) -128);
    }

    private static void case153() throws Throwable {
        java.util.function.ext.ShortToCharFunction callback = (value1) -> {
            check(value1 == (short) -32768);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((short) -32768) == '\uD800');
    }

    private static void case154() throws Throwable {
        java.util.function.ext.ShortToDoubleFunction callback = (value1) -> {
            check(value1 == (short) -32768);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((short) -32768)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case155() throws Throwable {
        java.util.function.ext.ShortToFloatFunction callback = (value1) -> {
            check(value1 == (short) -32768);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((short) -32768)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case156() throws Throwable {
        java.util.function.ext.ShortToIntFunction callback = (value1) -> {
            check(value1 == (short) -32768);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((short) -32768) == Integer.MIN_VALUE);
    }

    private static void case157() throws Throwable {
        java.util.function.ext.ShortToLongFunction callback = (value1) -> {
            check(value1 == (short) -32768);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((short) -32768) == Long.MIN_VALUE);
    }

    private static void case158() throws Throwable {
        java.util.function.ext.ShortUnaryOperator callback = (value1) -> {
            check(value1 == (short) -32768);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((short) -32768) == (short) -32768);
    }

    private static void case159() throws Throwable {
        java.util.function.ext.ToByteBiFunction<String, String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte("argument-one", "argument-two") == (byte) -128);
    }
}
