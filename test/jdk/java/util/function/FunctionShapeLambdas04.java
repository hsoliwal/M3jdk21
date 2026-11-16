/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Every admitted functional shape accepts a typed lambda and preserves its positions.
 * @modules java.base/java.util.function.ext java.base/java.util.function.checked
 * @run main FunctionShapeLambdas04
 */

public class FunctionShapeLambdas04 {
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
        System.out.println("PASS FunctionShapeLambdas04 checks=" + checks);
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError("position/result mismatch");
        }
        checks++;
    }

    private static void case0() throws Throwable {
        java.util.function.checked.IntDoubleToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Integer.MIN_VALUE, Double.NEGATIVE_INFINITY) == Long.MIN_VALUE);
    }

    private static void case1() throws Throwable {
        java.util.function.checked.IntDoubleToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Integer.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Integer.MIN_VALUE, Double.NEGATIVE_INFINITY) == (short) -32768);
    }

    private static void case2() throws Throwable {
        java.util.function.checked.IntFloatConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
        };
        callback.accept(Integer.MIN_VALUE, Float.POSITIVE_INFINITY);
    }

    private static void case3() throws Throwable {
        java.util.function.checked.IntFloatFunction<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == Integer.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return "argument-one";
        };
        check(callback.apply(Integer.MIN_VALUE, Float.POSITIVE_INFINITY) == "argument-one");
    }

    private static void case4() throws Throwable {
        java.util.function.checked.IntFloatPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return true;
        };
        check(callback.test(Integer.MIN_VALUE, Float.POSITIVE_INFINITY) == true);
    }

    private static void case5() throws Throwable {
        java.util.function.checked.IntFloatToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Integer.MIN_VALUE, Float.POSITIVE_INFINITY) == (byte) -128);
    }

    private static void case6() throws Throwable {
        java.util.function.checked.IntFloatToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Integer.MIN_VALUE, Float.POSITIVE_INFINITY) == '\uD800');
    }

    private static void case7() throws Throwable {
        java.util.function.checked.IntFloatToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Integer.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Integer.MIN_VALUE,
                Float.POSITIVE_INFINITY)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case8() throws Throwable {
        java.util.function.checked.IntFloatToFloatFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Integer.MIN_VALUE,
                Float.POSITIVE_INFINITY)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case9() throws Throwable {
        java.util.function.checked.IntFloatToIntFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Integer.MIN_VALUE, Float.POSITIVE_INFINITY) == Integer.MIN_VALUE);
    }

    private static void case10() throws Throwable {
        java.util.function.checked.IntFloatToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Integer.MIN_VALUE, Float.POSITIVE_INFINITY) == Long.MIN_VALUE);
    }

    private static void case11() throws Throwable {
        java.util.function.checked.IntFloatToShortFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Integer.MIN_VALUE, Float.POSITIVE_INFINITY) == (short) -32768);
    }

    private static void case12() throws Throwable {
        java.util.function.checked.IntFunction<String, Exception> callback = (value1) -> {
            check(value1 == Integer.MIN_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply(Integer.MIN_VALUE) == "argument-one");
    }

    private static void case13() throws Throwable {
        java.util.function.checked.IntLongConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
        };
        callback.accept(Integer.MIN_VALUE, Long.MAX_VALUE);
    }

    private static void case14() throws Throwable {
        java.util.function.checked.IntLongFunction<String, Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply(Integer.MIN_VALUE, Long.MAX_VALUE) == "argument-one");
    }

    private static void case15() throws Throwable {
        java.util.function.checked.IntLongPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test(Integer.MIN_VALUE, Long.MAX_VALUE) == true);
    }

    private static void case16() throws Throwable {
        java.util.function.checked.IntLongToByteFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Integer.MIN_VALUE, Long.MAX_VALUE) == (byte) -128);
    }

    private static void case17() throws Throwable {
        java.util.function.checked.IntLongToCharFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Integer.MIN_VALUE, Long.MAX_VALUE) == '\uD800');
    }

    private static void case18() throws Throwable {
        java.util.function.checked.IntLongToDoubleFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Integer.MIN_VALUE, Long.MAX_VALUE))
                == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case19() throws Throwable {
        java.util.function.checked.IntLongToFloatFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Integer.MIN_VALUE, Long.MAX_VALUE)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case20() throws Throwable {
        java.util.function.checked.IntLongToIntFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Integer.MIN_VALUE, Long.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case21() throws Throwable {
        java.util.function.checked.IntLongToLongFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Integer.MIN_VALUE, Long.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case22() throws Throwable {
        java.util.function.checked.IntLongToShortFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Integer.MIN_VALUE, Long.MAX_VALUE) == (short) -32768);
    }

    private static void case23() throws Throwable {
        java.util.function.checked.IntObjConsumer<String, Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
        };
        callback.accept(Integer.MIN_VALUE, "argument-two");
    }

    private static void case24() throws Throwable {
        java.util.function.checked.IntObjFunction<String, String, Exception> callback = (value1,
                value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return "argument-one";
        };
        check(callback.apply(Integer.MIN_VALUE, "argument-two") == "argument-one");
    }

    private static void case25() throws Throwable {
        java.util.function.checked.IntObjPredicate<String, Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return true;
        };
        check(callback.test(Integer.MIN_VALUE, "argument-two") == true);
    }

    private static void case26() throws Throwable {
        java.util.function.checked.IntObjToByteFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Integer.MIN_VALUE, "argument-two") == (byte) -128);
    }

    private static void case27() throws Throwable {
        java.util.function.checked.IntObjToCharFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Integer.MIN_VALUE, "argument-two") == '\uD800');
    }

    private static void case28() throws Throwable {
        java.util.function.checked.IntObjToDoubleFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Integer.MIN_VALUE, "argument-two")) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case29() throws Throwable {
        java.util.function.checked.IntObjToFloatFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Integer.MIN_VALUE, "argument-two")) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case30() throws Throwable {
        java.util.function.checked.IntObjToIntFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Integer.MIN_VALUE, "argument-two") == Integer.MIN_VALUE);
    }

    private static void case31() throws Throwable {
        java.util.function.checked.IntObjToLongFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Integer.MIN_VALUE, "argument-two") == Long.MIN_VALUE);
    }

    private static void case32() throws Throwable {
        java.util.function.checked.IntObjToShortFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Integer.MIN_VALUE, "argument-two") == (short) -32768);
    }

    private static void case33() throws Throwable {
        java.util.function.checked.IntPredicate<Exception> callback = (value1) -> {
            check(value1 == Integer.MIN_VALUE);
            checks++;
            return true;
        };
        check(callback.test(Integer.MIN_VALUE) == true);
    }

    private static void case34() throws Throwable {
        java.util.function.checked.IntShortConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
        };
        callback.accept(Integer.MIN_VALUE, (short) 32767);
    }

    private static void case35() throws Throwable {
        java.util.function.checked.IntShortFunction<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return "argument-one";
        };
        check(callback.apply(Integer.MIN_VALUE, (short) 32767) == "argument-one");
    }

    private static void case36() throws Throwable {
        java.util.function.checked.IntShortPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return true;
        };
        check(callback.test(Integer.MIN_VALUE, (short) 32767) == true);
    }

    private static void case37() throws Throwable {
        java.util.function.checked.IntShortToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Integer.MIN_VALUE, (short) 32767) == (byte) -128);
    }

    private static void case38() throws Throwable {
        java.util.function.checked.IntShortToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Integer.MIN_VALUE, (short) 32767) == '\uD800');
    }

    private static void case39() throws Throwable {
        java.util.function.checked.IntShortToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Integer.MIN_VALUE, (short) 32767))
                == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case40() throws Throwable {
        java.util.function.checked.IntShortToFloatFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Integer.MIN_VALUE, (short) 32767)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case41() throws Throwable {
        java.util.function.checked.IntShortToIntFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Integer.MIN_VALUE, (short) 32767) == Integer.MIN_VALUE);
    }

    private static void case42() throws Throwable {
        java.util.function.checked.IntShortToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Integer.MIN_VALUE, (short) 32767) == Long.MIN_VALUE);
    }

    private static void case43() throws Throwable {
        java.util.function.checked.IntShortToShortFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Integer.MIN_VALUE, (short) 32767) == (short) -32768);
    }

    private static void case44() throws Throwable {
        java.util.function.checked.IntSupplier<Exception> callback = () -> {
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.getAsInt() == Integer.MIN_VALUE);
    }

    private static void case45() throws Throwable {
        java.util.function.checked.IntToByteFunction<Exception> callback = (value1) -> {
            check(value1 == Integer.MIN_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Integer.MIN_VALUE) == (byte) -128);
    }

    private static void case46() throws Throwable {
        java.util.function.checked.IntToCharFunction<Exception> callback = (value1) -> {
            check(value1 == Integer.MIN_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Integer.MIN_VALUE) == '\uD800');
    }

    private static void case47() throws Throwable {
        java.util.function.checked.IntToDoubleFunction<Exception> callback = (value1) -> {
            check(value1 == Integer.MIN_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Integer.MIN_VALUE)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case48() throws Throwable {
        java.util.function.checked.IntToFloatFunction<Exception> callback = (value1) -> {
            check(value1 == Integer.MIN_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Integer.MIN_VALUE)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case49() throws Throwable {
        java.util.function.checked.IntToLongFunction<Exception> callback = (value1) -> {
            check(value1 == Integer.MIN_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Integer.MIN_VALUE) == Long.MIN_VALUE);
    }

    private static void case50() throws Throwable {
        java.util.function.checked.IntToShortFunction<Exception> callback = (value1) -> {
            check(value1 == Integer.MIN_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Integer.MIN_VALUE) == (short) -32768);
    }

    private static void case51() throws Throwable {
        java.util.function.checked.IntUnaryOperator<Exception> callback = (value1) -> {
            check(value1 == Integer.MIN_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Integer.MIN_VALUE) == Integer.MIN_VALUE);
    }

    private static void case52() throws Throwable {
        java.util.function.checked.LongBiConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
        };
        callback.accept(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private static void case53() throws Throwable {
        java.util.function.checked.LongBiFunction<String, Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply(Long.MIN_VALUE, Long.MAX_VALUE) == "argument-one");
    }

    private static void case54() throws Throwable {
        java.util.function.checked.LongBiPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test(Long.MIN_VALUE, Long.MAX_VALUE) == true);
    }

    private static void case55() throws Throwable {
        java.util.function.checked.LongBiToByteFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Long.MIN_VALUE, Long.MAX_VALUE) == (byte) -128);
    }

    private static void case56() throws Throwable {
        java.util.function.checked.LongBiToCharFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Long.MIN_VALUE, Long.MAX_VALUE) == '\uD800');
    }

    private static void case57() throws Throwable {
        java.util.function.checked.LongBiToDoubleFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Long.MIN_VALUE, Long.MAX_VALUE)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case58() throws Throwable {
        java.util.function.checked.LongBiToFloatFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Long.MIN_VALUE, Long.MAX_VALUE)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case59() throws Throwable {
        java.util.function.checked.LongBiToIntFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Long.MIN_VALUE, Long.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case60() throws Throwable {
        java.util.function.checked.LongBiToShortFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Long.MIN_VALUE, Long.MAX_VALUE) == (short) -32768);
    }

    private static void case61() throws Throwable {
        java.util.function.checked.LongBinaryOperator<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Long.MIN_VALUE, Long.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case62() throws Throwable {
        java.util.function.checked.LongBooleanConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == false);
            checks++;
        };
        callback.accept(Long.MIN_VALUE, false);
    }

    private static void case63() throws Throwable {
        java.util.function.checked.LongBooleanFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == false);
            checks++;
            return "argument-one";
        };
        check(callback.apply(Long.MIN_VALUE, false) == "argument-one");
    }

    private static void case64() throws Throwable {
        java.util.function.checked.LongBooleanPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == false);
            checks++;
            return true;
        };
        check(callback.test(Long.MIN_VALUE, false) == true);
    }

    private static void case65() throws Throwable {
        java.util.function.checked.LongBooleanToByteFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == false);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Long.MIN_VALUE, false) == (byte) -128);
    }

    private static void case66() throws Throwable {
        java.util.function.checked.LongBooleanToCharFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == false);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Long.MIN_VALUE, false) == '\uD800');
    }

    private static void case67() throws Throwable {
        java.util.function.checked.LongBooleanToDoubleFunction<Exception> callback = (value1,
                value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == false);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Long.MIN_VALUE, false)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case68() throws Throwable {
        java.util.function.checked.LongBooleanToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == false);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Long.MIN_VALUE, false)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case69() throws Throwable {
        java.util.function.checked.LongBooleanToIntFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == false);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Long.MIN_VALUE, false) == Integer.MIN_VALUE);
    }

    private static void case70() throws Throwable {
        java.util.function.checked.LongBooleanToLongFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == false);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Long.MIN_VALUE, false) == Long.MIN_VALUE);
    }

    private static void case71() throws Throwable {
        java.util.function.checked.LongBooleanToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == false);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Long.MIN_VALUE, false) == (short) -32768);
    }

    private static void case72() throws Throwable {
        java.util.function.checked.LongByteConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
        };
        callback.accept(Long.MIN_VALUE, (byte) 127);
    }

    private static void case73() throws Throwable {
        java.util.function.checked.LongByteFunction<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return "argument-one";
        };
        check(callback.apply(Long.MIN_VALUE, (byte) 127) == "argument-one");
    }

    private static void case74() throws Throwable {
        java.util.function.checked.LongBytePredicate<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return true;
        };
        check(callback.test(Long.MIN_VALUE, (byte) 127) == true);
    }

    private static void case75() throws Throwable {
        java.util.function.checked.LongByteToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Long.MIN_VALUE, (byte) 127) == (byte) -128);
    }

    private static void case76() throws Throwable {
        java.util.function.checked.LongByteToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Long.MIN_VALUE, (byte) 127) == '\uD800');
    }

    private static void case77() throws Throwable {
        java.util.function.checked.LongByteToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Long.MIN_VALUE, (byte) 127)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case78() throws Throwable {
        java.util.function.checked.LongByteToFloatFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Long.MIN_VALUE, (byte) 127)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case79() throws Throwable {
        java.util.function.checked.LongByteToIntFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Long.MIN_VALUE, (byte) 127) == Integer.MIN_VALUE);
    }

    private static void case80() throws Throwable {
        java.util.function.checked.LongByteToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Long.MIN_VALUE, (byte) 127) == Long.MIN_VALUE);
    }

    private static void case81() throws Throwable {
        java.util.function.checked.LongByteToShortFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Long.MIN_VALUE, (byte) 127) == (short) -32768);
    }

    private static void case82() throws Throwable {
        java.util.function.checked.LongCharConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
        };
        callback.accept(Long.MIN_VALUE, '\uFFFF');
    }

    private static void case83() throws Throwable {
        java.util.function.checked.LongCharFunction<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return "argument-one";
        };
        check(callback.apply(Long.MIN_VALUE, '\uFFFF') == "argument-one");
    }

    private static void case84() throws Throwable {
        java.util.function.checked.LongCharPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return true;
        };
        check(callback.test(Long.MIN_VALUE, '\uFFFF') == true);
    }

    private static void case85() throws Throwable {
        java.util.function.checked.LongCharToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Long.MIN_VALUE, '\uFFFF') == (byte) -128);
    }

    private static void case86() throws Throwable {
        java.util.function.checked.LongCharToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Long.MIN_VALUE, '\uFFFF') == '\uD800');
    }

    private static void case87() throws Throwable {
        java.util.function.checked.LongCharToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Long.MIN_VALUE, '\uFFFF')) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case88() throws Throwable {
        java.util.function.checked.LongCharToFloatFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Long.MIN_VALUE, '\uFFFF')) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case89() throws Throwable {
        java.util.function.checked.LongCharToIntFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Long.MIN_VALUE, '\uFFFF') == Integer.MIN_VALUE);
    }

    private static void case90() throws Throwable {
        java.util.function.checked.LongCharToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Long.MIN_VALUE, '\uFFFF') == Long.MIN_VALUE);
    }

    private static void case91() throws Throwable {
        java.util.function.checked.LongCharToShortFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Long.MIN_VALUE, '\uFFFF') == (short) -32768);
    }

    private static void case92() throws Throwable {
        java.util.function.checked.LongComparator<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return 0;
        };
        check(callback.compare(Long.MIN_VALUE, Long.MAX_VALUE) == 0);
    }

    private static void case93() throws Throwable {
        java.util.function.checked.LongConsumer<Exception> callback = (value1) -> {
            check(value1 == Long.MIN_VALUE);
            checks++;
        };
        callback.accept(Long.MIN_VALUE);
    }

    private static void case94() throws Throwable {
        java.util.function.checked.LongDoubleConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
        };
        callback.accept(Long.MIN_VALUE, Double.NEGATIVE_INFINITY);
    }

    private static void case95() throws Throwable {
        java.util.function.checked.LongDoubleFunction<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return "argument-one";
        };
        check(callback.apply(Long.MIN_VALUE, Double.NEGATIVE_INFINITY) == "argument-one");
    }

    private static void case96() throws Throwable {
        java.util.function.checked.LongDoublePredicate<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return true;
        };
        check(callback.test(Long.MIN_VALUE, Double.NEGATIVE_INFINITY) == true);
    }

    private static void case97() throws Throwable {
        java.util.function.checked.LongDoubleToByteFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Long.MIN_VALUE, Double.NEGATIVE_INFINITY) == (byte) -128);
    }

    private static void case98() throws Throwable {
        java.util.function.checked.LongDoubleToCharFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Long.MIN_VALUE, Double.NEGATIVE_INFINITY) == '\uD800');
    }

    private static void case99() throws Throwable {
        java.util.function.checked.LongDoubleToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Long.MIN_VALUE,
                Double.NEGATIVE_INFINITY)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case100() throws Throwable {
        java.util.function.checked.LongDoubleToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Long.MIN_VALUE,
                Double.NEGATIVE_INFINITY)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case101() throws Throwable {
        java.util.function.checked.LongDoubleToIntFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Long.MIN_VALUE, Double.NEGATIVE_INFINITY) == Integer.MIN_VALUE);
    }

    private static void case102() throws Throwable {
        java.util.function.checked.LongDoubleToLongFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Long.MIN_VALUE, Double.NEGATIVE_INFINITY) == Long.MIN_VALUE);
    }

    private static void case103() throws Throwable {
        java.util.function.checked.LongDoubleToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Long.MIN_VALUE, Double.NEGATIVE_INFINITY) == (short) -32768);
    }

    private static void case104() throws Throwable {
        java.util.function.checked.LongFloatConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
        };
        callback.accept(Long.MIN_VALUE, Float.POSITIVE_INFINITY);
    }

    private static void case105() throws Throwable {
        java.util.function.checked.LongFloatFunction<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return "argument-one";
        };
        check(callback.apply(Long.MIN_VALUE, Float.POSITIVE_INFINITY) == "argument-one");
    }

    private static void case106() throws Throwable {
        java.util.function.checked.LongFloatPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return true;
        };
        check(callback.test(Long.MIN_VALUE, Float.POSITIVE_INFINITY) == true);
    }

    private static void case107() throws Throwable {
        java.util.function.checked.LongFloatToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Long.MIN_VALUE, Float.POSITIVE_INFINITY) == (byte) -128);
    }

    private static void case108() throws Throwable {
        java.util.function.checked.LongFloatToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Long.MIN_VALUE, Float.POSITIVE_INFINITY) == '\uD800');
    }

    private static void case109() throws Throwable {
        java.util.function.checked.LongFloatToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Long.MIN_VALUE,
                Float.POSITIVE_INFINITY)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case110() throws Throwable {
        java.util.function.checked.LongFloatToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Long.MIN_VALUE,
                Float.POSITIVE_INFINITY)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case111() throws Throwable {
        java.util.function.checked.LongFloatToIntFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Long.MIN_VALUE, Float.POSITIVE_INFINITY) == Integer.MIN_VALUE);
    }

    private static void case112() throws Throwable {
        java.util.function.checked.LongFloatToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Long.MIN_VALUE, Float.POSITIVE_INFINITY) == Long.MIN_VALUE);
    }

    private static void case113() throws Throwable {
        java.util.function.checked.LongFloatToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Long.MIN_VALUE, Float.POSITIVE_INFINITY) == (short) -32768);
    }

    private static void case114() throws Throwable {
        java.util.function.checked.LongFunction<String, Exception> callback = (value1) -> {
            check(value1 == Long.MIN_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply(Long.MIN_VALUE) == "argument-one");
    }

    private static void case115() throws Throwable {
        java.util.function.checked.LongIntConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
        };
        callback.accept(Long.MIN_VALUE, Integer.MAX_VALUE);
    }

    private static void case116() throws Throwable {
        java.util.function.checked.LongIntFunction<String, Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply(Long.MIN_VALUE, Integer.MAX_VALUE) == "argument-one");
    }

    private static void case117() throws Throwable {
        java.util.function.checked.LongIntPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test(Long.MIN_VALUE, Integer.MAX_VALUE) == true);
    }

    private static void case118() throws Throwable {
        java.util.function.checked.LongIntToByteFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Long.MIN_VALUE, Integer.MAX_VALUE) == (byte) -128);
    }

    private static void case119() throws Throwable {
        java.util.function.checked.LongIntToCharFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Long.MIN_VALUE, Integer.MAX_VALUE) == '\uD800');
    }

    private static void case120() throws Throwable {
        java.util.function.checked.LongIntToDoubleFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Long.MIN_VALUE, Integer.MAX_VALUE))
                == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case121() throws Throwable {
        java.util.function.checked.LongIntToFloatFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Long.MIN_VALUE, Integer.MAX_VALUE)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case122() throws Throwable {
        java.util.function.checked.LongIntToIntFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Long.MIN_VALUE, Integer.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case123() throws Throwable {
        java.util.function.checked.LongIntToLongFunction<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Long.MIN_VALUE, Integer.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case124() throws Throwable {
        java.util.function.checked.LongIntToShortFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Long.MIN_VALUE, Integer.MAX_VALUE) == (short) -32768);
    }

    private static void case125() throws Throwable {
        java.util.function.checked.LongObjConsumer<String, Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
        };
        callback.accept(Long.MIN_VALUE, "argument-two");
    }

    private static void case126() throws Throwable {
        java.util.function.checked.LongObjFunction<String, String, Exception> callback = (value1,
                value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return "argument-one";
        };
        check(callback.apply(Long.MIN_VALUE, "argument-two") == "argument-one");
    }

    private static void case127() throws Throwable {
        java.util.function.checked.LongObjPredicate<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return true;
        };
        check(callback.test(Long.MIN_VALUE, "argument-two") == true);
    }

    private static void case128() throws Throwable {
        java.util.function.checked.LongObjToByteFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Long.MIN_VALUE, "argument-two") == (byte) -128);
    }

    private static void case129() throws Throwable {
        java.util.function.checked.LongObjToCharFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Long.MIN_VALUE, "argument-two") == '\uD800');
    }

    private static void case130() throws Throwable {
        java.util.function.checked.LongObjToDoubleFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Long.MIN_VALUE, "argument-two")) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case131() throws Throwable {
        java.util.function.checked.LongObjToFloatFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Long.MIN_VALUE, "argument-two")) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case132() throws Throwable {
        java.util.function.checked.LongObjToIntFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Long.MIN_VALUE, "argument-two") == Integer.MIN_VALUE);
    }

    private static void case133() throws Throwable {
        java.util.function.checked.LongObjToLongFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Long.MIN_VALUE, "argument-two") == Long.MIN_VALUE);
    }

    private static void case134() throws Throwable {
        java.util.function.checked.LongObjToShortFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Long.MIN_VALUE, "argument-two") == (short) -32768);
    }

    private static void case135() throws Throwable {
        java.util.function.checked.LongPredicate<Exception> callback = (value1) -> {
            check(value1 == Long.MIN_VALUE);
            checks++;
            return true;
        };
        check(callback.test(Long.MIN_VALUE) == true);
    }

    private static void case136() throws Throwable {
        java.util.function.checked.LongShortConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
        };
        callback.accept(Long.MIN_VALUE, (short) 32767);
    }

    private static void case137() throws Throwable {
        java.util.function.checked.LongShortFunction<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return "argument-one";
        };
        check(callback.apply(Long.MIN_VALUE, (short) 32767) == "argument-one");
    }

    private static void case138() throws Throwable {
        java.util.function.checked.LongShortPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return true;
        };
        check(callback.test(Long.MIN_VALUE, (short) 32767) == true);
    }

    private static void case139() throws Throwable {
        java.util.function.checked.LongShortToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Long.MIN_VALUE, (short) 32767) == (byte) -128);
    }

    private static void case140() throws Throwable {
        java.util.function.checked.LongShortToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Long.MIN_VALUE, (short) 32767) == '\uD800');
    }

    private static void case141() throws Throwable {
        java.util.function.checked.LongShortToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Long.MIN_VALUE, (short) 32767)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case142() throws Throwable {
        java.util.function.checked.LongShortToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Long.MIN_VALUE, (short) 32767)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case143() throws Throwable {
        java.util.function.checked.LongShortToIntFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Long.MIN_VALUE, (short) 32767) == Integer.MIN_VALUE);
    }

    private static void case144() throws Throwable {
        java.util.function.checked.LongShortToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Long.MIN_VALUE, (short) 32767) == Long.MIN_VALUE);
    }

    private static void case145() throws Throwable {
        java.util.function.checked.LongShortToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Long.MIN_VALUE, (short) 32767) == (short) -32768);
    }

    private static void case146() throws Throwable {
        java.util.function.checked.LongSupplier<Exception> callback = () -> {
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.getAsLong() == Long.MIN_VALUE);
    }

    private static void case147() throws Throwable {
        java.util.function.checked.LongToByteFunction<Exception> callback = (value1) -> {
            check(value1 == Long.MIN_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Long.MIN_VALUE) == (byte) -128);
    }

    private static void case148() throws Throwable {
        java.util.function.checked.LongToCharFunction<Exception> callback = (value1) -> {
            check(value1 == Long.MIN_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Long.MIN_VALUE) == '\uD800');
    }

    private static void case149() throws Throwable {
        java.util.function.checked.LongToDoubleFunction<Exception> callback = (value1) -> {
            check(value1 == Long.MIN_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Long.MIN_VALUE)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case150() throws Throwable {
        java.util.function.checked.LongToFloatFunction<Exception> callback = (value1) -> {
            check(value1 == Long.MIN_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Long.MIN_VALUE)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case151() throws Throwable {
        java.util.function.checked.LongToIntFunction<Exception> callback = (value1) -> {
            check(value1 == Long.MIN_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Long.MIN_VALUE) == Integer.MIN_VALUE);
    }

    private static void case152() throws Throwable {
        java.util.function.checked.LongToShortFunction<Exception> callback = (value1) -> {
            check(value1 == Long.MIN_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Long.MIN_VALUE) == (short) -32768);
    }

    private static void case153() throws Throwable {
        java.util.function.checked.LongUnaryOperator<Exception> callback = (value1) -> {
            check(value1 == Long.MIN_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Long.MIN_VALUE) == Long.MIN_VALUE);
    }

    private static void case154() throws Throwable {
        java.util.function.checked.ObjBooleanConsumer<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == "argument-one");
            check(value2 == false);
            checks++;
        };
        callback.accept("argument-one", false);
    }

    private static void case155() throws Throwable {
        java.util.function.checked.ObjBooleanFunction<String, String, Exception> callback = (value1,
                value2) -> {
            check(value1 == "argument-one");
            check(value2 == false);
            checks++;
            return "argument-one";
        };
        check(callback.apply("argument-one", false) == "argument-one");
    }

    private static void case156() throws Throwable {
        java.util.function.checked.ObjBooleanPredicate<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == "argument-one");
            check(value2 == false);
            checks++;
            return true;
        };
        check(callback.test("argument-one", false) == true);
    }

    private static void case157() throws Throwable {
        java.util.function.checked.ObjBooleanToByteFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == "argument-one");
            check(value2 == false);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte("argument-one", false) == (byte) -128);
    }

    private static void case158() throws Throwable {
        java.util.function.checked.ObjBooleanToCharFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == "argument-one");
            check(value2 == false);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar("argument-one", false) == '\uD800');
    }

    private static void case159() throws Throwable {
        java.util.function.checked.ObjBooleanToDoubleFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == "argument-one");
            check(value2 == false);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble("argument-one", false)) == Double.doubleToRawLongBits(-0.0d));
    }
}
