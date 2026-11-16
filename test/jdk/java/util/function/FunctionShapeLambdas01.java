/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Every admitted functional shape accepts a typed lambda and preserves its positions.
 * @modules java.base/java.util.function.ext java.base/java.util.function.checked
 * @run main FunctionShapeLambdas01
 */

public class FunctionShapeLambdas01 {
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
        System.out.println("PASS FunctionShapeLambdas01 checks=" + checks);
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError("position/result mismatch");
        }
        checks++;
    }

    private static void case0() throws Throwable {
        java.util.function.checked.ByteBiToLongFunction<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == (byte) 127);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((byte) -128, (byte) 127) == Long.MIN_VALUE);
    }

    private static void case1() throws Throwable {
        java.util.function.checked.ByteBiToShortFunction<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == (byte) 127);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((byte) -128, (byte) 127) == (short) -32768);
    }

    private static void case2() throws Throwable {
        java.util.function.checked.ByteBinaryOperator<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == (byte) 127);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((byte) -128, (byte) 127) == (byte) -128);
    }

    private static void case3() throws Throwable {
        java.util.function.checked.ByteBooleanConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == false);
            checks++;
        };
        callback.accept((byte) -128, false);
    }

    private static void case4() throws Throwable {
        java.util.function.checked.ByteBooleanFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == (byte) -128);
            check(value2 == false);
            checks++;
            return "argument-one";
        };
        check(callback.apply((byte) -128, false) == "argument-one");
    }

    private static void case5() throws Throwable {
        java.util.function.checked.ByteBooleanPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == false);
            checks++;
            return true;
        };
        check(callback.test((byte) -128, false) == true);
    }

    private static void case6() throws Throwable {
        java.util.function.checked.ByteBooleanToByteFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(value2 == false);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((byte) -128, false) == (byte) -128);
    }

    private static void case7() throws Throwable {
        java.util.function.checked.ByteBooleanToCharFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(value2 == false);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((byte) -128, false) == '\uD800');
    }

    private static void case8() throws Throwable {
        java.util.function.checked.ByteBooleanToDoubleFunction<Exception> callback = (value1,
                value2) -> {
            check(value1 == (byte) -128);
            check(value2 == false);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((byte) -128, false)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case9() throws Throwable {
        java.util.function.checked.ByteBooleanToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(value2 == false);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((byte) -128, false)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case10() throws Throwable {
        java.util.function.checked.ByteBooleanToIntFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(value2 == false);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((byte) -128, false) == Integer.MIN_VALUE);
    }

    private static void case11() throws Throwable {
        java.util.function.checked.ByteBooleanToLongFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(value2 == false);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((byte) -128, false) == Long.MIN_VALUE);
    }

    private static void case12() throws Throwable {
        java.util.function.checked.ByteBooleanToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(value2 == false);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((byte) -128, false) == (short) -32768);
    }

    private static void case13() throws Throwable {
        java.util.function.checked.ByteCharConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == '\uFFFF');
            checks++;
        };
        callback.accept((byte) -128, '\uFFFF');
    }

    private static void case14() throws Throwable {
        java.util.function.checked.ByteCharFunction<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(value2 == '\uFFFF');
            checks++;
            return "argument-one";
        };
        check(callback.apply((byte) -128, '\uFFFF') == "argument-one");
    }

    private static void case15() throws Throwable {
        java.util.function.checked.ByteCharPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == '\uFFFF');
            checks++;
            return true;
        };
        check(callback.test((byte) -128, '\uFFFF') == true);
    }

    private static void case16() throws Throwable {
        java.util.function.checked.ByteCharToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(value2 == '\uFFFF');
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((byte) -128, '\uFFFF') == (byte) -128);
    }

    private static void case17() throws Throwable {
        java.util.function.checked.ByteCharToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(value2 == '\uFFFF');
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((byte) -128, '\uFFFF') == '\uD800');
    }

    private static void case18() throws Throwable {
        java.util.function.checked.ByteCharToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(value2 == '\uFFFF');
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((byte) -128, '\uFFFF')) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case19() throws Throwable {
        java.util.function.checked.ByteCharToFloatFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(value2 == '\uFFFF');
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((byte) -128, '\uFFFF')) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case20() throws Throwable {
        java.util.function.checked.ByteCharToIntFunction<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == '\uFFFF');
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((byte) -128, '\uFFFF') == Integer.MIN_VALUE);
    }

    private static void case21() throws Throwable {
        java.util.function.checked.ByteCharToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(value2 == '\uFFFF');
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((byte) -128, '\uFFFF') == Long.MIN_VALUE);
    }

    private static void case22() throws Throwable {
        java.util.function.checked.ByteCharToShortFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(value2 == '\uFFFF');
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((byte) -128, '\uFFFF') == (short) -32768);
    }

    private static void case23() throws Throwable {
        java.util.function.checked.ByteComparator<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == (byte) 127);
            checks++;
            return 0;
        };
        check(callback.compare((byte) -128, (byte) 127) == 0);
    }

    private static void case24() throws Throwable {
        java.util.function.checked.ByteConsumer<Exception> callback = (value1) -> {
            check(value1 == (byte) -128);
            checks++;
        };
        callback.accept((byte) -128);
    }

    private static void case25() throws Throwable {
        java.util.function.checked.ByteDoubleConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
        };
        callback.accept((byte) -128, Double.NEGATIVE_INFINITY);
    }

    private static void case26() throws Throwable {
        java.util.function.checked.ByteDoubleFunction<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return "argument-one";
        };
        check(callback.apply((byte) -128, Double.NEGATIVE_INFINITY) == "argument-one");
    }

    private static void case27() throws Throwable {
        java.util.function.checked.ByteDoublePredicate<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return true;
        };
        check(callback.test((byte) -128, Double.NEGATIVE_INFINITY) == true);
    }

    private static void case28() throws Throwable {
        java.util.function.checked.ByteDoubleToByteFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((byte) -128, Double.NEGATIVE_INFINITY) == (byte) -128);
    }

    private static void case29() throws Throwable {
        java.util.function.checked.ByteDoubleToCharFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((byte) -128, Double.NEGATIVE_INFINITY) == '\uD800');
    }

    private static void case30() throws Throwable {
        java.util.function.checked.ByteDoubleToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((byte) -128,
                Double.NEGATIVE_INFINITY)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case31() throws Throwable {
        java.util.function.checked.ByteDoubleToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((byte) -128, Double.NEGATIVE_INFINITY))
                == Float.floatToRawIntBits(-0.0f));
    }

    private static void case32() throws Throwable {
        java.util.function.checked.ByteDoubleToIntFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((byte) -128, Double.NEGATIVE_INFINITY) == Integer.MIN_VALUE);
    }

    private static void case33() throws Throwable {
        java.util.function.checked.ByteDoubleToLongFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((byte) -128, Double.NEGATIVE_INFINITY) == Long.MIN_VALUE);
    }

    private static void case34() throws Throwable {
        java.util.function.checked.ByteDoubleToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((byte) -128, Double.NEGATIVE_INFINITY) == (short) -32768);
    }

    private static void case35() throws Throwable {
        java.util.function.checked.ByteFloatConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
        };
        callback.accept((byte) -128, Float.POSITIVE_INFINITY);
    }

    private static void case36() throws Throwable {
        java.util.function.checked.ByteFloatFunction<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return "argument-one";
        };
        check(callback.apply((byte) -128, Float.POSITIVE_INFINITY) == "argument-one");
    }

    private static void case37() throws Throwable {
        java.util.function.checked.ByteFloatPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return true;
        };
        check(callback.test((byte) -128, Float.POSITIVE_INFINITY) == true);
    }

    private static void case38() throws Throwable {
        java.util.function.checked.ByteFloatToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((byte) -128, Float.POSITIVE_INFINITY) == (byte) -128);
    }

    private static void case39() throws Throwable {
        java.util.function.checked.ByteFloatToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((byte) -128, Float.POSITIVE_INFINITY) == '\uD800');
    }

    private static void case40() throws Throwable {
        java.util.function.checked.ByteFloatToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((byte) -128,
                Float.POSITIVE_INFINITY)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case41() throws Throwable {
        java.util.function.checked.ByteFloatToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((byte) -128, Float.POSITIVE_INFINITY))
                == Float.floatToRawIntBits(-0.0f));
    }

    private static void case42() throws Throwable {
        java.util.function.checked.ByteFloatToIntFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((byte) -128, Float.POSITIVE_INFINITY) == Integer.MIN_VALUE);
    }

    private static void case43() throws Throwable {
        java.util.function.checked.ByteFloatToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((byte) -128, Float.POSITIVE_INFINITY) == Long.MIN_VALUE);
    }

    private static void case44() throws Throwable {
        java.util.function.checked.ByteFloatToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((byte) -128, Float.POSITIVE_INFINITY) == (short) -32768);
    }

    private static void case45() throws Throwable {
        java.util.function.checked.ByteFunction<String, Exception> callback = (value1) -> {
            check(value1 == (byte) -128);
            checks++;
            return "argument-one";
        };
        check(callback.apply((byte) -128) == "argument-one");
    }

    private static void case46() throws Throwable {
        java.util.function.checked.ByteIntConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == Integer.MAX_VALUE);
            checks++;
        };
        callback.accept((byte) -128, Integer.MAX_VALUE);
    }

    private static void case47() throws Throwable {
        java.util.function.checked.ByteIntFunction<String, Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply((byte) -128, Integer.MAX_VALUE) == "argument-one");
    }

    private static void case48() throws Throwable {
        java.util.function.checked.ByteIntPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test((byte) -128, Integer.MAX_VALUE) == true);
    }

    private static void case49() throws Throwable {
        java.util.function.checked.ByteIntToByteFunction<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((byte) -128, Integer.MAX_VALUE) == (byte) -128);
    }

    private static void case50() throws Throwable {
        java.util.function.checked.ByteIntToCharFunction<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((byte) -128, Integer.MAX_VALUE) == '\uD800');
    }

    private static void case51() throws Throwable {
        java.util.function.checked.ByteIntToDoubleFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((byte) -128, Integer.MAX_VALUE)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case52() throws Throwable {
        java.util.function.checked.ByteIntToFloatFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((byte) -128, Integer.MAX_VALUE)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case53() throws Throwable {
        java.util.function.checked.ByteIntToIntFunction<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((byte) -128, Integer.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case54() throws Throwable {
        java.util.function.checked.ByteIntToLongFunction<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((byte) -128, Integer.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case55() throws Throwable {
        java.util.function.checked.ByteIntToShortFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((byte) -128, Integer.MAX_VALUE) == (short) -32768);
    }

    private static void case56() throws Throwable {
        java.util.function.checked.ByteLongConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == Long.MAX_VALUE);
            checks++;
        };
        callback.accept((byte) -128, Long.MAX_VALUE);
    }

    private static void case57() throws Throwable {
        java.util.function.checked.ByteLongFunction<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply((byte) -128, Long.MAX_VALUE) == "argument-one");
    }

    private static void case58() throws Throwable {
        java.util.function.checked.ByteLongPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test((byte) -128, Long.MAX_VALUE) == true);
    }

    private static void case59() throws Throwable {
        java.util.function.checked.ByteLongToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((byte) -128, Long.MAX_VALUE) == (byte) -128);
    }

    private static void case60() throws Throwable {
        java.util.function.checked.ByteLongToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((byte) -128, Long.MAX_VALUE) == '\uD800');
    }

    private static void case61() throws Throwable {
        java.util.function.checked.ByteLongToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((byte) -128, Long.MAX_VALUE)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case62() throws Throwable {
        java.util.function.checked.ByteLongToFloatFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((byte) -128, Long.MAX_VALUE)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case63() throws Throwable {
        java.util.function.checked.ByteLongToIntFunction<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((byte) -128, Long.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case64() throws Throwable {
        java.util.function.checked.ByteLongToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((byte) -128, Long.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case65() throws Throwable {
        java.util.function.checked.ByteLongToShortFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((byte) -128, Long.MAX_VALUE) == (short) -32768);
    }

    private static void case66() throws Throwable {
        java.util.function.checked.ByteObjConsumer<String, Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(value2 == "argument-two");
            checks++;
        };
        callback.accept((byte) -128, "argument-two");
    }

    private static void case67() throws Throwable {
        java.util.function.checked.ByteObjFunction<String, String, Exception> callback = (value1,
                value2) -> {
            check(value1 == (byte) -128);
            check(value2 == "argument-two");
            checks++;
            return "argument-one";
        };
        check(callback.apply((byte) -128, "argument-two") == "argument-one");
    }

    private static void case68() throws Throwable {
        java.util.function.checked.ByteObjPredicate<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(value2 == "argument-two");
            checks++;
            return true;
        };
        check(callback.test((byte) -128, "argument-two") == true);
    }

    private static void case69() throws Throwable {
        java.util.function.checked.ByteObjToByteFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == (byte) -128);
            check(value2 == "argument-two");
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((byte) -128, "argument-two") == (byte) -128);
    }

    private static void case70() throws Throwable {
        java.util.function.checked.ByteObjToCharFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == (byte) -128);
            check(value2 == "argument-two");
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((byte) -128, "argument-two") == '\uD800');
    }

    private static void case71() throws Throwable {
        java.util.function.checked.ByteObjToDoubleFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == (byte) -128);
            check(value2 == "argument-two");
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((byte) -128, "argument-two")) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case72() throws Throwable {
        java.util.function.checked.ByteObjToFloatFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == (byte) -128);
            check(value2 == "argument-two");
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((byte) -128, "argument-two")) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case73() throws Throwable {
        java.util.function.checked.ByteObjToIntFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == (byte) -128);
            check(value2 == "argument-two");
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((byte) -128, "argument-two") == Integer.MIN_VALUE);
    }

    private static void case74() throws Throwable {
        java.util.function.checked.ByteObjToLongFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == (byte) -128);
            check(value2 == "argument-two");
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((byte) -128, "argument-two") == Long.MIN_VALUE);
    }

    private static void case75() throws Throwable {
        java.util.function.checked.ByteObjToShortFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == (byte) -128);
            check(value2 == "argument-two");
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((byte) -128, "argument-two") == (short) -32768);
    }

    private static void case76() throws Throwable {
        java.util.function.checked.BytePredicate<Exception> callback = (value1) -> {
            check(value1 == (byte) -128);
            checks++;
            return true;
        };
        check(callback.test((byte) -128) == true);
    }

    private static void case77() throws Throwable {
        java.util.function.checked.ByteShortConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == (short) 32767);
            checks++;
        };
        callback.accept((byte) -128, (short) 32767);
    }

    private static void case78() throws Throwable {
        java.util.function.checked.ByteShortFunction<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(value2 == (short) 32767);
            checks++;
            return "argument-one";
        };
        check(callback.apply((byte) -128, (short) 32767) == "argument-one");
    }

    private static void case79() throws Throwable {
        java.util.function.checked.ByteShortPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == (short) 32767);
            checks++;
            return true;
        };
        check(callback.test((byte) -128, (short) 32767) == true);
    }

    private static void case80() throws Throwable {
        java.util.function.checked.ByteShortToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(value2 == (short) 32767);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((byte) -128, (short) 32767) == (byte) -128);
    }

    private static void case81() throws Throwable {
        java.util.function.checked.ByteShortToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(value2 == (short) 32767);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((byte) -128, (short) 32767) == '\uD800');
    }

    private static void case82() throws Throwable {
        java.util.function.checked.ByteShortToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(value2 == (short) 32767);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((byte) -128, (short) 32767)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case83() throws Throwable {
        java.util.function.checked.ByteShortToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(value2 == (short) 32767);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((byte) -128, (short) 32767)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case84() throws Throwable {
        java.util.function.checked.ByteShortToIntFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(value2 == (short) 32767);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((byte) -128, (short) 32767) == Integer.MIN_VALUE);
    }

    private static void case85() throws Throwable {
        java.util.function.checked.ByteShortToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (byte) -128);
            check(value2 == (short) 32767);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((byte) -128, (short) 32767) == Long.MIN_VALUE);
    }

    private static void case86() throws Throwable {
        java.util.function.checked.ByteShortToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (byte) -128);
            check(value2 == (short) 32767);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((byte) -128, (short) 32767) == (short) -32768);
    }

    private static void case87() throws Throwable {
        java.util.function.checked.ByteSupplier<Exception> callback = () -> {
            checks++;
            return (byte) -128;
        };
        check(callback.getAsByte() == (byte) -128);
    }

    private static void case88() throws Throwable {
        java.util.function.checked.ByteToCharFunction<Exception> callback = (value1) -> {
            check(value1 == (byte) -128);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((byte) -128) == '\uD800');
    }

    private static void case89() throws Throwable {
        java.util.function.checked.ByteToDoubleFunction<Exception> callback = (value1) -> {
            check(value1 == (byte) -128);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((byte) -128)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case90() throws Throwable {
        java.util.function.checked.ByteToFloatFunction<Exception> callback = (value1) -> {
            check(value1 == (byte) -128);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((byte) -128)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case91() throws Throwable {
        java.util.function.checked.ByteToIntFunction<Exception> callback = (value1) -> {
            check(value1 == (byte) -128);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((byte) -128) == Integer.MIN_VALUE);
    }

    private static void case92() throws Throwable {
        java.util.function.checked.ByteToLongFunction<Exception> callback = (value1) -> {
            check(value1 == (byte) -128);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((byte) -128) == Long.MIN_VALUE);
    }

    private static void case93() throws Throwable {
        java.util.function.checked.ByteToShortFunction<Exception> callback = (value1) -> {
            check(value1 == (byte) -128);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((byte) -128) == (short) -32768);
    }

    private static void case94() throws Throwable {
        java.util.function.checked.ByteUnaryOperator<Exception> callback = (value1) -> {
            check(value1 == (byte) -128);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((byte) -128) == (byte) -128);
    }

    private static void case95() throws Throwable {
        java.util.function.checked.CharBiConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == '\uFFFF');
            checks++;
        };
        callback.accept('\uD800', '\uFFFF');
    }

    private static void case96() throws Throwable {
        java.util.function.checked.CharBiFunction<String, Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(value2 == '\uFFFF');
            checks++;
            return "argument-one";
        };
        check(callback.apply('\uD800', '\uFFFF') == "argument-one");
    }

    private static void case97() throws Throwable {
        java.util.function.checked.CharBiPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == '\uFFFF');
            checks++;
            return true;
        };
        check(callback.test('\uD800', '\uFFFF') == true);
    }

    private static void case98() throws Throwable {
        java.util.function.checked.CharBiToByteFunction<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == '\uFFFF');
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte('\uD800', '\uFFFF') == (byte) -128);
    }

    private static void case99() throws Throwable {
        java.util.function.checked.CharBiToDoubleFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(value2 == '\uFFFF');
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble('\uD800', '\uFFFF')) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case100() throws Throwable {
        java.util.function.checked.CharBiToFloatFunction<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == '\uFFFF');
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat('\uD800', '\uFFFF')) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case101() throws Throwable {
        java.util.function.checked.CharBiToIntFunction<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == '\uFFFF');
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt('\uD800', '\uFFFF') == Integer.MIN_VALUE);
    }

    private static void case102() throws Throwable {
        java.util.function.checked.CharBiToLongFunction<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == '\uFFFF');
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong('\uD800', '\uFFFF') == Long.MIN_VALUE);
    }

    private static void case103() throws Throwable {
        java.util.function.checked.CharBiToShortFunction<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == '\uFFFF');
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort('\uD800', '\uFFFF') == (short) -32768);
    }

    private static void case104() throws Throwable {
        java.util.function.checked.CharBinaryOperator<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == '\uFFFF');
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar('\uD800', '\uFFFF') == '\uD800');
    }

    private static void case105() throws Throwable {
        java.util.function.checked.CharBooleanConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == false);
            checks++;
        };
        callback.accept('\uD800', false);
    }

    private static void case106() throws Throwable {
        java.util.function.checked.CharBooleanFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == '\uD800');
            check(value2 == false);
            checks++;
            return "argument-one";
        };
        check(callback.apply('\uD800', false) == "argument-one");
    }

    private static void case107() throws Throwable {
        java.util.function.checked.CharBooleanPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == false);
            checks++;
            return true;
        };
        check(callback.test('\uD800', false) == true);
    }

    private static void case108() throws Throwable {
        java.util.function.checked.CharBooleanToByteFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(value2 == false);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte('\uD800', false) == (byte) -128);
    }

    private static void case109() throws Throwable {
        java.util.function.checked.CharBooleanToCharFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(value2 == false);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar('\uD800', false) == '\uD800');
    }

    private static void case110() throws Throwable {
        java.util.function.checked.CharBooleanToDoubleFunction<Exception> callback = (value1,
                value2) -> {
            check(value1 == '\uD800');
            check(value2 == false);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble('\uD800', false)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case111() throws Throwable {
        java.util.function.checked.CharBooleanToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(value2 == false);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat('\uD800', false)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case112() throws Throwable {
        java.util.function.checked.CharBooleanToIntFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(value2 == false);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt('\uD800', false) == Integer.MIN_VALUE);
    }

    private static void case113() throws Throwable {
        java.util.function.checked.CharBooleanToLongFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(value2 == false);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong('\uD800', false) == Long.MIN_VALUE);
    }

    private static void case114() throws Throwable {
        java.util.function.checked.CharBooleanToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(value2 == false);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort('\uD800', false) == (short) -32768);
    }

    private static void case115() throws Throwable {
        java.util.function.checked.CharByteConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == (byte) 127);
            checks++;
        };
        callback.accept('\uD800', (byte) 127);
    }

    private static void case116() throws Throwable {
        java.util.function.checked.CharByteFunction<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(value2 == (byte) 127);
            checks++;
            return "argument-one";
        };
        check(callback.apply('\uD800', (byte) 127) == "argument-one");
    }

    private static void case117() throws Throwable {
        java.util.function.checked.CharBytePredicate<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == (byte) 127);
            checks++;
            return true;
        };
        check(callback.test('\uD800', (byte) 127) == true);
    }

    private static void case118() throws Throwable {
        java.util.function.checked.CharByteToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(value2 == (byte) 127);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte('\uD800', (byte) 127) == (byte) -128);
    }

    private static void case119() throws Throwable {
        java.util.function.checked.CharByteToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(value2 == (byte) 127);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar('\uD800', (byte) 127) == '\uD800');
    }

    private static void case120() throws Throwable {
        java.util.function.checked.CharByteToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(value2 == (byte) 127);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble('\uD800', (byte) 127)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case121() throws Throwable {
        java.util.function.checked.CharByteToFloatFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(value2 == (byte) 127);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat('\uD800', (byte) 127)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case122() throws Throwable {
        java.util.function.checked.CharByteToIntFunction<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == (byte) 127);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt('\uD800', (byte) 127) == Integer.MIN_VALUE);
    }

    private static void case123() throws Throwable {
        java.util.function.checked.CharByteToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(value2 == (byte) 127);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong('\uD800', (byte) 127) == Long.MIN_VALUE);
    }

    private static void case124() throws Throwable {
        java.util.function.checked.CharByteToShortFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(value2 == (byte) 127);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort('\uD800', (byte) 127) == (short) -32768);
    }

    private static void case125() throws Throwable {
        java.util.function.checked.CharComparator<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == '\uFFFF');
            checks++;
            return 0;
        };
        check(callback.compare('\uD800', '\uFFFF') == 0);
    }

    private static void case126() throws Throwable {
        java.util.function.checked.CharConsumer<Exception> callback = (value1) -> {
            check(value1 == '\uD800');
            checks++;
        };
        callback.accept('\uD800');
    }

    private static void case127() throws Throwable {
        java.util.function.checked.CharDoubleConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
        };
        callback.accept('\uD800', Double.NEGATIVE_INFINITY);
    }

    private static void case128() throws Throwable {
        java.util.function.checked.CharDoubleFunction<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return "argument-one";
        };
        check(callback.apply('\uD800', Double.NEGATIVE_INFINITY) == "argument-one");
    }

    private static void case129() throws Throwable {
        java.util.function.checked.CharDoublePredicate<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return true;
        };
        check(callback.test('\uD800', Double.NEGATIVE_INFINITY) == true);
    }

    private static void case130() throws Throwable {
        java.util.function.checked.CharDoubleToByteFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte('\uD800', Double.NEGATIVE_INFINITY) == (byte) -128);
    }

    private static void case131() throws Throwable {
        java.util.function.checked.CharDoubleToCharFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar('\uD800', Double.NEGATIVE_INFINITY) == '\uD800');
    }

    private static void case132() throws Throwable {
        java.util.function.checked.CharDoubleToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble('\uD800', Double.NEGATIVE_INFINITY)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case133() throws Throwable {
        java.util.function.checked.CharDoubleToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat('\uD800', Double.NEGATIVE_INFINITY)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case134() throws Throwable {
        java.util.function.checked.CharDoubleToIntFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt('\uD800', Double.NEGATIVE_INFINITY) == Integer.MIN_VALUE);
    }

    private static void case135() throws Throwable {
        java.util.function.checked.CharDoubleToLongFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong('\uD800', Double.NEGATIVE_INFINITY) == Long.MIN_VALUE);
    }

    private static void case136() throws Throwable {
        java.util.function.checked.CharDoubleToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort('\uD800', Double.NEGATIVE_INFINITY) == (short) -32768);
    }

    private static void case137() throws Throwable {
        java.util.function.checked.CharFloatConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
        };
        callback.accept('\uD800', Float.POSITIVE_INFINITY);
    }

    private static void case138() throws Throwable {
        java.util.function.checked.CharFloatFunction<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return "argument-one";
        };
        check(callback.apply('\uD800', Float.POSITIVE_INFINITY) == "argument-one");
    }

    private static void case139() throws Throwable {
        java.util.function.checked.CharFloatPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return true;
        };
        check(callback.test('\uD800', Float.POSITIVE_INFINITY) == true);
    }

    private static void case140() throws Throwable {
        java.util.function.checked.CharFloatToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte('\uD800', Float.POSITIVE_INFINITY) == (byte) -128);
    }

    private static void case141() throws Throwable {
        java.util.function.checked.CharFloatToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar('\uD800', Float.POSITIVE_INFINITY) == '\uD800');
    }

    private static void case142() throws Throwable {
        java.util.function.checked.CharFloatToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble('\uD800', Float.POSITIVE_INFINITY)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case143() throws Throwable {
        java.util.function.checked.CharFloatToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat('\uD800', Float.POSITIVE_INFINITY)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case144() throws Throwable {
        java.util.function.checked.CharFloatToIntFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt('\uD800', Float.POSITIVE_INFINITY) == Integer.MIN_VALUE);
    }

    private static void case145() throws Throwable {
        java.util.function.checked.CharFloatToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong('\uD800', Float.POSITIVE_INFINITY) == Long.MIN_VALUE);
    }

    private static void case146() throws Throwable {
        java.util.function.checked.CharFloatToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort('\uD800', Float.POSITIVE_INFINITY) == (short) -32768);
    }

    private static void case147() throws Throwable {
        java.util.function.checked.CharFunction<String, Exception> callback = (value1) -> {
            check(value1 == '\uD800');
            checks++;
            return "argument-one";
        };
        check(callback.apply('\uD800') == "argument-one");
    }

    private static void case148() throws Throwable {
        java.util.function.checked.CharIntConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == Integer.MAX_VALUE);
            checks++;
        };
        callback.accept('\uD800', Integer.MAX_VALUE);
    }

    private static void case149() throws Throwable {
        java.util.function.checked.CharIntFunction<String, Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply('\uD800', Integer.MAX_VALUE) == "argument-one");
    }

    private static void case150() throws Throwable {
        java.util.function.checked.CharIntPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test('\uD800', Integer.MAX_VALUE) == true);
    }

    private static void case151() throws Throwable {
        java.util.function.checked.CharIntToByteFunction<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte('\uD800', Integer.MAX_VALUE) == (byte) -128);
    }

    private static void case152() throws Throwable {
        java.util.function.checked.CharIntToCharFunction<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar('\uD800', Integer.MAX_VALUE) == '\uD800');
    }

    private static void case153() throws Throwable {
        java.util.function.checked.CharIntToDoubleFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble('\uD800', Integer.MAX_VALUE)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case154() throws Throwable {
        java.util.function.checked.CharIntToFloatFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat('\uD800', Integer.MAX_VALUE)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case155() throws Throwable {
        java.util.function.checked.CharIntToIntFunction<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt('\uD800', Integer.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case156() throws Throwable {
        java.util.function.checked.CharIntToLongFunction<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong('\uD800', Integer.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case157() throws Throwable {
        java.util.function.checked.CharIntToShortFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort('\uD800', Integer.MAX_VALUE) == (short) -32768);
    }

    private static void case158() throws Throwable {
        java.util.function.checked.CharLongConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == Long.MAX_VALUE);
            checks++;
        };
        callback.accept('\uD800', Long.MAX_VALUE);
    }

    private static void case159() throws Throwable {
        java.util.function.checked.CharLongFunction<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(value2 == Long.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply('\uD800', Long.MAX_VALUE) == "argument-one");
    }
}
