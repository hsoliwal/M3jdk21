/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Every admitted functional shape accepts a typed lambda and preserves its positions.
 * @modules java.base/java.util.function.ext java.base/java.util.function.checked
 * @run main FunctionShapeLambdas03
 */

public class FunctionShapeLambdas03 {
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
        System.out.println("PASS FunctionShapeLambdas03 checks=" + checks);
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError("position/result mismatch");
        }
        checks++;
    }

    private static void case0() throws Throwable {
        java.util.function.checked.FloatBiToIntFunction<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(-0.0f, Float.POSITIVE_INFINITY) == Integer.MIN_VALUE);
    }

    private static void case1() throws Throwable {
        java.util.function.checked.FloatBiToLongFunction<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(-0.0f, Float.POSITIVE_INFINITY) == Long.MIN_VALUE);
    }

    private static void case2() throws Throwable {
        java.util.function.checked.FloatBiToShortFunction<Exception> callback = (value1, value2) ->
                {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(-0.0f, Float.POSITIVE_INFINITY) == (short) -32768);
    }

    private static void case3() throws Throwable {
        java.util.function.checked.FloatBinaryOperator<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(-0.0f, Float.POSITIVE_INFINITY)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case4() throws Throwable {
        java.util.function.checked.FloatBooleanConsumer<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == false);
            checks++;
        };
        callback.accept(-0.0f, false);
    }

    private static void case5() throws Throwable {
        java.util.function.checked.FloatBooleanFunction<String, Exception> callback = (value1,
                value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == false);
            checks++;
            return "argument-one";
        };
        check(callback.apply(-0.0f, false) == "argument-one");
    }

    private static void case6() throws Throwable {
        java.util.function.checked.FloatBooleanPredicate<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == false);
            checks++;
            return true;
        };
        check(callback.test(-0.0f, false) == true);
    }

    private static void case7() throws Throwable {
        java.util.function.checked.FloatBooleanToByteFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == false);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(-0.0f, false) == (byte) -128);
    }

    private static void case8() throws Throwable {
        java.util.function.checked.FloatBooleanToCharFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == false);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(-0.0f, false) == '\uD800');
    }

    private static void case9() throws Throwable {
        java.util.function.checked.FloatBooleanToDoubleFunction<Exception> callback = (value1,
                value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == false);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(-0.0f, false)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case10() throws Throwable {
        java.util.function.checked.FloatBooleanToFloatFunction<Exception> callback = (value1,
                value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == false);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(-0.0f, false)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case11() throws Throwable {
        java.util.function.checked.FloatBooleanToIntFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == false);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(-0.0f, false) == Integer.MIN_VALUE);
    }

    private static void case12() throws Throwable {
        java.util.function.checked.FloatBooleanToLongFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == false);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(-0.0f, false) == Long.MIN_VALUE);
    }

    private static void case13() throws Throwable {
        java.util.function.checked.FloatBooleanToShortFunction<Exception> callback = (value1,
                value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == false);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(-0.0f, false) == (short) -32768);
    }

    private static void case14() throws Throwable {
        java.util.function.checked.FloatByteConsumer<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == (byte) 127);
            checks++;
        };
        callback.accept(-0.0f, (byte) 127);
    }

    private static void case15() throws Throwable {
        java.util.function.checked.FloatByteFunction<String, Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == (byte) 127);
            checks++;
            return "argument-one";
        };
        check(callback.apply(-0.0f, (byte) 127) == "argument-one");
    }

    private static void case16() throws Throwable {
        java.util.function.checked.FloatBytePredicate<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == (byte) 127);
            checks++;
            return true;
        };
        check(callback.test(-0.0f, (byte) 127) == true);
    }

    private static void case17() throws Throwable {
        java.util.function.checked.FloatByteToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == (byte) 127);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(-0.0f, (byte) 127) == (byte) -128);
    }

    private static void case18() throws Throwable {
        java.util.function.checked.FloatByteToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == (byte) 127);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(-0.0f, (byte) 127) == '\uD800');
    }

    private static void case19() throws Throwable {
        java.util.function.checked.FloatByteToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == (byte) 127);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(-0.0f, (byte) 127)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case20() throws Throwable {
        java.util.function.checked.FloatByteToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == (byte) 127);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(-0.0f, (byte) 127)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case21() throws Throwable {
        java.util.function.checked.FloatByteToIntFunction<Exception> callback = (value1, value2) ->
                {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == (byte) 127);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(-0.0f, (byte) 127) == Integer.MIN_VALUE);
    }

    private static void case22() throws Throwable {
        java.util.function.checked.FloatByteToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == (byte) 127);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(-0.0f, (byte) 127) == Long.MIN_VALUE);
    }

    private static void case23() throws Throwable {
        java.util.function.checked.FloatByteToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == (byte) 127);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(-0.0f, (byte) 127) == (short) -32768);
    }

    private static void case24() throws Throwable {
        java.util.function.checked.FloatCharConsumer<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == '\uFFFF');
            checks++;
        };
        callback.accept(-0.0f, '\uFFFF');
    }

    private static void case25() throws Throwable {
        java.util.function.checked.FloatCharFunction<String, Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == '\uFFFF');
            checks++;
            return "argument-one";
        };
        check(callback.apply(-0.0f, '\uFFFF') == "argument-one");
    }

    private static void case26() throws Throwable {
        java.util.function.checked.FloatCharPredicate<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == '\uFFFF');
            checks++;
            return true;
        };
        check(callback.test(-0.0f, '\uFFFF') == true);
    }

    private static void case27() throws Throwable {
        java.util.function.checked.FloatCharToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == '\uFFFF');
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(-0.0f, '\uFFFF') == (byte) -128);
    }

    private static void case28() throws Throwable {
        java.util.function.checked.FloatCharToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == '\uFFFF');
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(-0.0f, '\uFFFF') == '\uD800');
    }

    private static void case29() throws Throwable {
        java.util.function.checked.FloatCharToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == '\uFFFF');
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(-0.0f, '\uFFFF')) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case30() throws Throwable {
        java.util.function.checked.FloatCharToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == '\uFFFF');
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(-0.0f, '\uFFFF')) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case31() throws Throwable {
        java.util.function.checked.FloatCharToIntFunction<Exception> callback = (value1, value2) ->
                {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == '\uFFFF');
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(-0.0f, '\uFFFF') == Integer.MIN_VALUE);
    }

    private static void case32() throws Throwable {
        java.util.function.checked.FloatCharToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == '\uFFFF');
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(-0.0f, '\uFFFF') == Long.MIN_VALUE);
    }

    private static void case33() throws Throwable {
        java.util.function.checked.FloatCharToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == '\uFFFF');
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(-0.0f, '\uFFFF') == (short) -32768);
    }

    private static void case34() throws Throwable {
        java.util.function.checked.FloatComparator<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return 0;
        };
        check(callback.compare(-0.0f, Float.POSITIVE_INFINITY) == 0);
    }

    private static void case35() throws Throwable {
        java.util.function.checked.FloatConsumer<Exception> callback = (value1) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            checks++;
        };
        callback.accept(-0.0f);
    }

    private static void case36() throws Throwable {
        java.util.function.checked.FloatDoubleConsumer<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
        };
        callback.accept(-0.0f, Double.NEGATIVE_INFINITY);
    }

    private static void case37() throws Throwable {
        java.util.function.checked.FloatDoubleFunction<String, Exception> callback = (value1,
                value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return "argument-one";
        };
        check(callback.apply(-0.0f, Double.NEGATIVE_INFINITY) == "argument-one");
    }

    private static void case38() throws Throwable {
        java.util.function.checked.FloatDoublePredicate<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return true;
        };
        check(callback.test(-0.0f, Double.NEGATIVE_INFINITY) == true);
    }

    private static void case39() throws Throwable {
        java.util.function.checked.FloatDoubleToByteFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(-0.0f, Double.NEGATIVE_INFINITY) == (byte) -128);
    }

    private static void case40() throws Throwable {
        java.util.function.checked.FloatDoubleToCharFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(-0.0f, Double.NEGATIVE_INFINITY) == '\uD800');
    }

    private static void case41() throws Throwable {
        java.util.function.checked.FloatDoubleToDoubleFunction<Exception> callback = (value1,
                value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(-0.0f, Double.NEGATIVE_INFINITY)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case42() throws Throwable {
        java.util.function.checked.FloatDoubleToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(-0.0f, Double.NEGATIVE_INFINITY)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case43() throws Throwable {
        java.util.function.checked.FloatDoubleToIntFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(-0.0f, Double.NEGATIVE_INFINITY) == Integer.MIN_VALUE);
    }

    private static void case44() throws Throwable {
        java.util.function.checked.FloatDoubleToLongFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(-0.0f, Double.NEGATIVE_INFINITY) == Long.MIN_VALUE);
    }

    private static void case45() throws Throwable {
        java.util.function.checked.FloatDoubleToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(-0.0f, Double.NEGATIVE_INFINITY) == (short) -32768);
    }

    private static void case46() throws Throwable {
        java.util.function.checked.FloatFunction<String, Exception> callback = (value1) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            checks++;
            return "argument-one";
        };
        check(callback.apply(-0.0f) == "argument-one");
    }

    private static void case47() throws Throwable {
        java.util.function.checked.FloatIntConsumer<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == Integer.MAX_VALUE);
            checks++;
        };
        callback.accept(-0.0f, Integer.MAX_VALUE);
    }

    private static void case48() throws Throwable {
        java.util.function.checked.FloatIntFunction<String, Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply(-0.0f, Integer.MAX_VALUE) == "argument-one");
    }

    private static void case49() throws Throwable {
        java.util.function.checked.FloatIntPredicate<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test(-0.0f, Integer.MAX_VALUE) == true);
    }

    private static void case50() throws Throwable {
        java.util.function.checked.FloatIntToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(-0.0f, Integer.MAX_VALUE) == (byte) -128);
    }

    private static void case51() throws Throwable {
        java.util.function.checked.FloatIntToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(-0.0f, Integer.MAX_VALUE) == '\uD800');
    }

    private static void case52() throws Throwable {
        java.util.function.checked.FloatIntToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(-0.0f, Integer.MAX_VALUE)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case53() throws Throwable {
        java.util.function.checked.FloatIntToFloatFunction<Exception> callback = (value1, value2) ->
                {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(-0.0f, Integer.MAX_VALUE)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case54() throws Throwable {
        java.util.function.checked.FloatIntToIntFunction<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(-0.0f, Integer.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case55() throws Throwable {
        java.util.function.checked.FloatIntToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(-0.0f, Integer.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case56() throws Throwable {
        java.util.function.checked.FloatIntToShortFunction<Exception> callback = (value1, value2) ->
                {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(-0.0f, Integer.MAX_VALUE) == (short) -32768);
    }

    private static void case57() throws Throwable {
        java.util.function.checked.FloatLongConsumer<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == Long.MAX_VALUE);
            checks++;
        };
        callback.accept(-0.0f, Long.MAX_VALUE);
    }

    private static void case58() throws Throwable {
        java.util.function.checked.FloatLongFunction<String, Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == Long.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply(-0.0f, Long.MAX_VALUE) == "argument-one");
    }

    private static void case59() throws Throwable {
        java.util.function.checked.FloatLongPredicate<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == Long.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test(-0.0f, Long.MAX_VALUE) == true);
    }

    private static void case60() throws Throwable {
        java.util.function.checked.FloatLongToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(-0.0f, Long.MAX_VALUE) == (byte) -128);
    }

    private static void case61() throws Throwable {
        java.util.function.checked.FloatLongToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == Long.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(-0.0f, Long.MAX_VALUE) == '\uD800');
    }

    private static void case62() throws Throwable {
        java.util.function.checked.FloatLongToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(-0.0f, Long.MAX_VALUE)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case63() throws Throwable {
        java.util.function.checked.FloatLongToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(-0.0f, Long.MAX_VALUE)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case64() throws Throwable {
        java.util.function.checked.FloatLongToIntFunction<Exception> callback = (value1, value2) ->
                {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(-0.0f, Long.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case65() throws Throwable {
        java.util.function.checked.FloatLongToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(-0.0f, Long.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case66() throws Throwable {
        java.util.function.checked.FloatLongToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(-0.0f, Long.MAX_VALUE) == (short) -32768);
    }

    private static void case67() throws Throwable {
        java.util.function.checked.FloatObjConsumer<String, Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == "argument-two");
            checks++;
        };
        callback.accept(-0.0f, "argument-two");
    }

    private static void case68() throws Throwable {
        java.util.function.checked.FloatObjFunction<String, String, Exception> callback = (value1,
                value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == "argument-two");
            checks++;
            return "argument-one";
        };
        check(callback.apply(-0.0f, "argument-two") == "argument-one");
    }

    private static void case69() throws Throwable {
        java.util.function.checked.FloatObjPredicate<String, Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == "argument-two");
            checks++;
            return true;
        };
        check(callback.test(-0.0f, "argument-two") == true);
    }

    private static void case70() throws Throwable {
        java.util.function.checked.FloatObjToByteFunction<String, Exception> callback = (value1,
                value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == "argument-two");
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(-0.0f, "argument-two") == (byte) -128);
    }

    private static void case71() throws Throwable {
        java.util.function.checked.FloatObjToCharFunction<String, Exception> callback = (value1,
                value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == "argument-two");
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(-0.0f, "argument-two") == '\uD800');
    }

    private static void case72() throws Throwable {
        java.util.function.checked.FloatObjToDoubleFunction<String, Exception> callback = (value1,
                value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == "argument-two");
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(-0.0f, "argument-two")) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case73() throws Throwable {
        java.util.function.checked.FloatObjToFloatFunction<String, Exception> callback = (value1,
                value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == "argument-two");
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(-0.0f, "argument-two")) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case74() throws Throwable {
        java.util.function.checked.FloatObjToIntFunction<String, Exception> callback = (value1,
                value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == "argument-two");
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(-0.0f, "argument-two") == Integer.MIN_VALUE);
    }

    private static void case75() throws Throwable {
        java.util.function.checked.FloatObjToLongFunction<String, Exception> callback = (value1,
                value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == "argument-two");
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(-0.0f, "argument-two") == Long.MIN_VALUE);
    }

    private static void case76() throws Throwable {
        java.util.function.checked.FloatObjToShortFunction<String, Exception> callback = (value1,
                value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == "argument-two");
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(-0.0f, "argument-two") == (short) -32768);
    }

    private static void case77() throws Throwable {
        java.util.function.checked.FloatPredicate<Exception> callback = (value1) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            checks++;
            return true;
        };
        check(callback.test(-0.0f) == true);
    }

    private static void case78() throws Throwable {
        java.util.function.checked.FloatShortConsumer<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == (short) 32767);
            checks++;
        };
        callback.accept(-0.0f, (short) 32767);
    }

    private static void case79() throws Throwable {
        java.util.function.checked.FloatShortFunction<String, Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == (short) 32767);
            checks++;
            return "argument-one";
        };
        check(callback.apply(-0.0f, (short) 32767) == "argument-one");
    }

    private static void case80() throws Throwable {
        java.util.function.checked.FloatShortPredicate<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == (short) 32767);
            checks++;
            return true;
        };
        check(callback.test(-0.0f, (short) 32767) == true);
    }

    private static void case81() throws Throwable {
        java.util.function.checked.FloatShortToByteFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == (short) 32767);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(-0.0f, (short) 32767) == (byte) -128);
    }

    private static void case82() throws Throwable {
        java.util.function.checked.FloatShortToCharFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == (short) 32767);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(-0.0f, (short) 32767) == '\uD800');
    }

    private static void case83() throws Throwable {
        java.util.function.checked.FloatShortToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == (short) 32767);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(-0.0f, (short) 32767)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case84() throws Throwable {
        java.util.function.checked.FloatShortToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == (short) 32767);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(-0.0f, (short) 32767)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case85() throws Throwable {
        java.util.function.checked.FloatShortToIntFunction<Exception> callback = (value1, value2) ->
                {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == (short) 32767);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(-0.0f, (short) 32767) == Integer.MIN_VALUE);
    }

    private static void case86() throws Throwable {
        java.util.function.checked.FloatShortToLongFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == (short) 32767);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(-0.0f, (short) 32767) == Long.MIN_VALUE);
    }

    private static void case87() throws Throwable {
        java.util.function.checked.FloatShortToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(value2 == (short) 32767);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(-0.0f, (short) 32767) == (short) -32768);
    }

    private static void case88() throws Throwable {
        java.util.function.checked.FloatSupplier<Exception> callback = () -> {
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.getAsFloat()) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case89() throws Throwable {
        java.util.function.checked.FloatToByteFunction<Exception> callback = (value1) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(-0.0f) == (byte) -128);
    }

    private static void case90() throws Throwable {
        java.util.function.checked.FloatToCharFunction<Exception> callback = (value1) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(-0.0f) == '\uD800');
    }

    private static void case91() throws Throwable {
        java.util.function.checked.FloatToDoubleFunction<Exception> callback = (value1) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(-0.0f)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case92() throws Throwable {
        java.util.function.checked.FloatToIntFunction<Exception> callback = (value1) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(-0.0f) == Integer.MIN_VALUE);
    }

    private static void case93() throws Throwable {
        java.util.function.checked.FloatToLongFunction<Exception> callback = (value1) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(-0.0f) == Long.MIN_VALUE);
    }

    private static void case94() throws Throwable {
        java.util.function.checked.FloatToShortFunction<Exception> callback = (value1) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(-0.0f) == (short) -32768);
    }

    private static void case95() throws Throwable {
        java.util.function.checked.FloatUnaryOperator<Exception> callback = (value1) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(-0.0f)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case96() throws Throwable {
        java.util.function.checked.Function<String, String, Exception> callback = (value1) -> {
            check(value1 == "argument-one");
            checks++;
            return "argument-one";
        };
        check(callback.apply("argument-one") == "argument-one");
    }

    private static void case97() throws Throwable {
        java.util.function.checked.Function10<String, String, String, String, String, String,
                String, String, String, String, String, Exception> callback = (value1, value2,
                value3, value4, value5, value6, value7, value8, value9, value10) -> {
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
            return "argument-one";
        };
        check(callback.apply("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two") == "argument-one");
    }

    private static void case98() throws Throwable {
        java.util.function.checked.Function11<String, String, String, String, String, String,
                String, String, String, String, String, String, Exception> callback = (value1,
                value2, value3, value4, value5, value6, value7, value8, value9, value10, value11) ->
                {
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
            return "argument-one";
        };
        check(callback.apply("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one") == "argument-one");
    }

    private static void case99() throws Throwable {
        java.util.function.checked.Function12<String, String, String, String, String, String,
                String, String, String, String, String, String, String, Exception> callback =
                (value1, value2, value3, value4, value5, value6, value7, value8, value9, value10,
                value11, value12) -> {
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
            return "argument-one";
        };
        check(callback.apply("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two") == "argument-one");
    }

    private static void case100() throws Throwable {
        java.util.function.checked.Function13<String, String, String, String, String, String,
                String, String, String, String, String, String, String, String, Exception> callback
                = (value1, value2, value3, value4, value5, value6, value7, value8, value9, value10,
                value11, value12, value13) -> {
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
            return "argument-one";
        };
        check(callback.apply("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one") == "argument-one");
    }

    private static void case101() throws Throwable {
        java.util.function.checked.Function14<String, String, String, String, String, String,
                String, String, String, String, String, String, String, String, String, Exception>
                callback = (value1, value2, value3, value4, value5, value6, value7, value8, value9,
                value10, value11, value12, value13, value14) -> {
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
            return "argument-one";
        };
        check(callback.apply("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two") == "argument-one");
    }

    private static void case102() throws Throwable {
        java.util.function.checked.Function15<String, String, String, String, String, String,
                String, String, String, String, String, String, String, String, String, String,
                Exception> callback = (value1, value2, value3, value4, value5, value6, value7,
                value8, value9, value10, value11, value12, value13, value14, value15) -> {
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
            return "argument-one";
        };
        check(callback.apply("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one") == "argument-one");
    }

    private static void case103() throws Throwable {
        java.util.function.checked.Function16<String, String, String, String, String, String,
                String, String, String, String, String, String, String, String, String, String,
                String, Exception> callback = (value1, value2, value3, value4, value5, value6,
                value7, value8, value9, value10, value11, value12, value13, value14, value15,
                value16) -> {
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
            return "argument-one";
        };
        check(callback.apply("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two") == "argument-one");
    }

    private static void case104() throws Throwable {
        java.util.function.checked.Function4<String, String, String, String, String, Exception>
                callback = (value1, value2, value3, value4) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            checks++;
            return "argument-one";
        };
        check(callback.apply("argument-one", "argument-two", "argument-one", "argument-two") == "argument-one");
    }

    private static void case105() throws Throwable {
        java.util.function.checked.Function5<String, String, String, String, String, String,
                Exception> callback = (value1, value2, value3, value4, value5) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            check(value5 == "argument-one");
            checks++;
            return "argument-one";
        };
        check(callback.apply("argument-one", "argument-two", "argument-one", "argument-two", "argument-one") == "argument-one");
    }

    private static void case106() throws Throwable {
        java.util.function.checked.Function6<String, String, String, String, String, String, String,
                Exception> callback = (value1, value2, value3, value4, value5, value6) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            check(value5 == "argument-one");
            check(value6 == "argument-two");
            checks++;
            return "argument-one";
        };
        check(callback.apply("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two") == "argument-one");
    }

    private static void case107() throws Throwable {
        java.util.function.checked.Function7<String, String, String, String, String, String, String,
                String, Exception> callback = (value1, value2, value3, value4, value5, value6,
                value7) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            check(value5 == "argument-one");
            check(value6 == "argument-two");
            check(value7 == "argument-one");
            checks++;
            return "argument-one";
        };
        check(callback.apply("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one") == "argument-one");
    }

    private static void case108() throws Throwable {
        java.util.function.checked.Function8<String, String, String, String, String, String, String,
                String, String, Exception> callback = (value1, value2, value3, value4, value5,
                value6, value7, value8) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            check(value5 == "argument-one");
            check(value6 == "argument-two");
            check(value7 == "argument-one");
            check(value8 == "argument-two");
            checks++;
            return "argument-one";
        };
        check(callback.apply("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two") == "argument-one");
    }

    private static void case109() throws Throwable {
        java.util.function.checked.Function9<String, String, String, String, String, String, String,
                String, String, String, Exception> callback = (value1, value2, value3, value4,
                value5, value6, value7, value8, value9) -> {
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
            return "argument-one";
        };
        check(callback.apply("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one") == "argument-one");
    }

    private static void case110() throws Throwable {
        java.util.function.checked.IntBiConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
        };
        callback.accept(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private static void case111() throws Throwable {
        java.util.function.checked.IntBiFunction<String, Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply(Integer.MIN_VALUE, Integer.MAX_VALUE) == "argument-one");
    }

    private static void case112() throws Throwable {
        java.util.function.checked.IntBiPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test(Integer.MIN_VALUE, Integer.MAX_VALUE) == true);
    }

    private static void case113() throws Throwable {
        java.util.function.checked.IntBiToByteFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Integer.MIN_VALUE, Integer.MAX_VALUE) == (byte) -128);
    }

    private static void case114() throws Throwable {
        java.util.function.checked.IntBiToCharFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Integer.MIN_VALUE, Integer.MAX_VALUE) == '\uD800');
    }

    private static void case115() throws Throwable {
        java.util.function.checked.IntBiToDoubleFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Integer.MIN_VALUE,
                Integer.MAX_VALUE)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case116() throws Throwable {
        java.util.function.checked.IntBiToFloatFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Integer.MIN_VALUE, Integer.MAX_VALUE))
                == Float.floatToRawIntBits(-0.0f));
    }

    private static void case117() throws Throwable {
        java.util.function.checked.IntBiToLongFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Integer.MIN_VALUE, Integer.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case118() throws Throwable {
        java.util.function.checked.IntBiToShortFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Integer.MIN_VALUE, Integer.MAX_VALUE) == (short) -32768);
    }

    private static void case119() throws Throwable {
        java.util.function.checked.IntBinaryOperator<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Integer.MIN_VALUE, Integer.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case120() throws Throwable {
        java.util.function.checked.IntBooleanConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == false);
            checks++;
        };
        callback.accept(Integer.MIN_VALUE, false);
    }

    private static void case121() throws Throwable {
        java.util.function.checked.IntBooleanFunction<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == false);
            checks++;
            return "argument-one";
        };
        check(callback.apply(Integer.MIN_VALUE, false) == "argument-one");
    }

    private static void case122() throws Throwable {
        java.util.function.checked.IntBooleanPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == false);
            checks++;
            return true;
        };
        check(callback.test(Integer.MIN_VALUE, false) == true);
    }

    private static void case123() throws Throwable {
        java.util.function.checked.IntBooleanToByteFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == false);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Integer.MIN_VALUE, false) == (byte) -128);
    }

    private static void case124() throws Throwable {
        java.util.function.checked.IntBooleanToCharFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == false);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Integer.MIN_VALUE, false) == '\uD800');
    }

    private static void case125() throws Throwable {
        java.util.function.checked.IntBooleanToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == false);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Integer.MIN_VALUE, false)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case126() throws Throwable {
        java.util.function.checked.IntBooleanToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == false);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Integer.MIN_VALUE, false)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case127() throws Throwable {
        java.util.function.checked.IntBooleanToIntFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == false);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Integer.MIN_VALUE, false) == Integer.MIN_VALUE);
    }

    private static void case128() throws Throwable {
        java.util.function.checked.IntBooleanToLongFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == false);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Integer.MIN_VALUE, false) == Long.MIN_VALUE);
    }

    private static void case129() throws Throwable {
        java.util.function.checked.IntBooleanToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == false);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Integer.MIN_VALUE, false) == (short) -32768);
    }

    private static void case130() throws Throwable {
        java.util.function.checked.IntByteConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
        };
        callback.accept(Integer.MIN_VALUE, (byte) 127);
    }

    private static void case131() throws Throwable {
        java.util.function.checked.IntByteFunction<String, Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return "argument-one";
        };
        check(callback.apply(Integer.MIN_VALUE, (byte) 127) == "argument-one");
    }

    private static void case132() throws Throwable {
        java.util.function.checked.IntBytePredicate<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return true;
        };
        check(callback.test(Integer.MIN_VALUE, (byte) 127) == true);
    }

    private static void case133() throws Throwable {
        java.util.function.checked.IntByteToByteFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Integer.MIN_VALUE, (byte) 127) == (byte) -128);
    }

    private static void case134() throws Throwable {
        java.util.function.checked.IntByteToCharFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Integer.MIN_VALUE, (byte) 127) == '\uD800');
    }

    private static void case135() throws Throwable {
        java.util.function.checked.IntByteToDoubleFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Integer.MIN_VALUE, (byte) 127)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case136() throws Throwable {
        java.util.function.checked.IntByteToFloatFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Integer.MIN_VALUE, (byte) 127)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case137() throws Throwable {
        java.util.function.checked.IntByteToIntFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Integer.MIN_VALUE, (byte) 127) == Integer.MIN_VALUE);
    }

    private static void case138() throws Throwable {
        java.util.function.checked.IntByteToLongFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Integer.MIN_VALUE, (byte) 127) == Long.MIN_VALUE);
    }

    private static void case139() throws Throwable {
        java.util.function.checked.IntByteToShortFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Integer.MIN_VALUE, (byte) 127) == (short) -32768);
    }

    private static void case140() throws Throwable {
        java.util.function.checked.IntCharConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
        };
        callback.accept(Integer.MIN_VALUE, '\uFFFF');
    }

    private static void case141() throws Throwable {
        java.util.function.checked.IntCharFunction<String, Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return "argument-one";
        };
        check(callback.apply(Integer.MIN_VALUE, '\uFFFF') == "argument-one");
    }

    private static void case142() throws Throwable {
        java.util.function.checked.IntCharPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return true;
        };
        check(callback.test(Integer.MIN_VALUE, '\uFFFF') == true);
    }

    private static void case143() throws Throwable {
        java.util.function.checked.IntCharToByteFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Integer.MIN_VALUE, '\uFFFF') == (byte) -128);
    }

    private static void case144() throws Throwable {
        java.util.function.checked.IntCharToCharFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Integer.MIN_VALUE, '\uFFFF') == '\uD800');
    }

    private static void case145() throws Throwable {
        java.util.function.checked.IntCharToDoubleFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Integer.MIN_VALUE, '\uFFFF')) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case146() throws Throwable {
        java.util.function.checked.IntCharToFloatFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Integer.MIN_VALUE, '\uFFFF')) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case147() throws Throwable {
        java.util.function.checked.IntCharToIntFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Integer.MIN_VALUE, '\uFFFF') == Integer.MIN_VALUE);
    }

    private static void case148() throws Throwable {
        java.util.function.checked.IntCharToLongFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Integer.MIN_VALUE, '\uFFFF') == Long.MIN_VALUE);
    }

    private static void case149() throws Throwable {
        java.util.function.checked.IntCharToShortFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Integer.MIN_VALUE, '\uFFFF') == (short) -32768);
    }

    private static void case150() throws Throwable {
        java.util.function.checked.IntComparator<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return 0;
        };
        check(callback.compare(Integer.MIN_VALUE, Integer.MAX_VALUE) == 0);
    }

    private static void case151() throws Throwable {
        java.util.function.checked.IntConsumer<Exception> callback = (value1) -> {
            check(value1 == Integer.MIN_VALUE);
            checks++;
        };
        callback.accept(Integer.MIN_VALUE);
    }

    private static void case152() throws Throwable {
        java.util.function.checked.IntDoubleConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
        };
        callback.accept(Integer.MIN_VALUE, Double.NEGATIVE_INFINITY);
    }

    private static void case153() throws Throwable {
        java.util.function.checked.IntDoubleFunction<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == Integer.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return "argument-one";
        };
        check(callback.apply(Integer.MIN_VALUE, Double.NEGATIVE_INFINITY) == "argument-one");
    }

    private static void case154() throws Throwable {
        java.util.function.checked.IntDoublePredicate<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return true;
        };
        check(callback.test(Integer.MIN_VALUE, Double.NEGATIVE_INFINITY) == true);
    }

    private static void case155() throws Throwable {
        java.util.function.checked.IntDoubleToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Integer.MIN_VALUE, Double.NEGATIVE_INFINITY) == (byte) -128);
    }

    private static void case156() throws Throwable {
        java.util.function.checked.IntDoubleToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Integer.MIN_VALUE, Double.NEGATIVE_INFINITY) == '\uD800');
    }

    private static void case157() throws Throwable {
        java.util.function.checked.IntDoubleToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Integer.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Integer.MIN_VALUE,
                Double.NEGATIVE_INFINITY)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case158() throws Throwable {
        java.util.function.checked.IntDoubleToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Integer.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Integer.MIN_VALUE,
                Double.NEGATIVE_INFINITY)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case159() throws Throwable {
        java.util.function.checked.IntDoubleToIntFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Integer.MIN_VALUE, Double.NEGATIVE_INFINITY) ==
                Integer.MIN_VALUE);
    }
}
