/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Every admitted functional shape accepts a typed lambda and preserves its positions.
 * @modules java.base/java.util.function.ext java.base/java.util.function.checked
 * @run main FunctionShapeLambdas06
 */

public class FunctionShapeLambdas06 {
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
        System.out.println("PASS FunctionShapeLambdas06 checks=" + checks);
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError("position/result mismatch");
        }
        checks++;
    }

    private static void case0() throws Throwable {
        java.util.function.checked.ShortIntToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (short) -32768);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((short) -32768, Integer.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case1() throws Throwable {
        java.util.function.checked.ShortIntToShortFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (short) -32768);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((short) -32768, Integer.MAX_VALUE) == (short) -32768);
    }

    private static void case2() throws Throwable {
        java.util.function.checked.ShortLongConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Long.MAX_VALUE);
            checks++;
        };
        callback.accept((short) -32768, Long.MAX_VALUE);
    }

    private static void case3() throws Throwable {
        java.util.function.checked.ShortLongFunction<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == (short) -32768);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply((short) -32768, Long.MAX_VALUE) == "argument-one");
    }

    private static void case4() throws Throwable {
        java.util.function.checked.ShortLongPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == (short) -32768);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test((short) -32768, Long.MAX_VALUE) == true);
    }

    private static void case5() throws Throwable {
        java.util.function.checked.ShortLongToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (short) -32768);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((short) -32768, Long.MAX_VALUE) == (byte) -128);
    }

    private static void case6() throws Throwable {
        java.util.function.checked.ShortLongToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (short) -32768);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((short) -32768, Long.MAX_VALUE) == '\uD800');
    }

    private static void case7() throws Throwable {
        java.util.function.checked.ShortLongToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (short) -32768);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((short) -32768, Long.MAX_VALUE)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case8() throws Throwable {
        java.util.function.checked.ShortLongToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (short) -32768);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((short) -32768, Long.MAX_VALUE)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case9() throws Throwable {
        java.util.function.checked.ShortLongToIntFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (short) -32768);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((short) -32768, Long.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case10() throws Throwable {
        java.util.function.checked.ShortLongToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == (short) -32768);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((short) -32768, Long.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case11() throws Throwable {
        java.util.function.checked.ShortLongToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == (short) -32768);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((short) -32768, Long.MAX_VALUE) == (short) -32768);
    }

    private static void case12() throws Throwable {
        java.util.function.checked.ShortObjConsumer<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == (short) -32768);
            check(value2 == "argument-two");
            checks++;
        };
        callback.accept((short) -32768, "argument-two");
    }

    private static void case13() throws Throwable {
        java.util.function.checked.ShortObjFunction<String, String, Exception> callback = (value1,
                value2) -> {
            check(value1 == (short) -32768);
            check(value2 == "argument-two");
            checks++;
            return "argument-one";
        };
        check(callback.apply((short) -32768, "argument-two") == "argument-one");
    }

    private static void case14() throws Throwable {
        java.util.function.checked.ShortObjPredicate<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == (short) -32768);
            check(value2 == "argument-two");
            checks++;
            return true;
        };
        check(callback.test((short) -32768, "argument-two") == true);
    }

    private static void case15() throws Throwable {
        java.util.function.checked.ShortObjToByteFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == (short) -32768);
            check(value2 == "argument-two");
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((short) -32768, "argument-two") == (byte) -128);
    }

    private static void case16() throws Throwable {
        java.util.function.checked.ShortObjToCharFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == (short) -32768);
            check(value2 == "argument-two");
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((short) -32768, "argument-two") == '\uD800');
    }

    private static void case17() throws Throwable {
        java.util.function.checked.ShortObjToDoubleFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == (short) -32768);
            check(value2 == "argument-two");
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((short) -32768, "argument-two")) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case18() throws Throwable {
        java.util.function.checked.ShortObjToFloatFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == (short) -32768);
            check(value2 == "argument-two");
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((short) -32768, "argument-two")) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case19() throws Throwable {
        java.util.function.checked.ShortObjToIntFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == (short) -32768);
            check(value2 == "argument-two");
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((short) -32768, "argument-two") == Integer.MIN_VALUE);
    }

    private static void case20() throws Throwable {
        java.util.function.checked.ShortObjToLongFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == (short) -32768);
            check(value2 == "argument-two");
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((short) -32768, "argument-two") == Long.MIN_VALUE);
    }

    private static void case21() throws Throwable {
        java.util.function.checked.ShortObjToShortFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == (short) -32768);
            check(value2 == "argument-two");
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((short) -32768, "argument-two") == (short) -32768);
    }

    private static void case22() throws Throwable {
        java.util.function.checked.ShortPredicate<Exception> callback = (value1) -> {
            check(value1 == (short) -32768);
            checks++;
            return true;
        };
        check(callback.test((short) -32768) == true);
    }

    private static void case23() throws Throwable {
        java.util.function.checked.ShortSupplier<Exception> callback = () -> {
            checks++;
            return (short) -32768;
        };
        check(callback.getAsShort() == (short) -32768);
    }

    private static void case24() throws Throwable {
        java.util.function.checked.ShortToByteFunction<Exception> callback = (value1) -> {
            check(value1 == (short) -32768);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte((short) -32768) == (byte) -128);
    }

    private static void case25() throws Throwable {
        java.util.function.checked.ShortToCharFunction<Exception> callback = (value1) -> {
            check(value1 == (short) -32768);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((short) -32768) == '\uD800');
    }

    private static void case26() throws Throwable {
        java.util.function.checked.ShortToDoubleFunction<Exception> callback = (value1) -> {
            check(value1 == (short) -32768);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((short) -32768)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case27() throws Throwable {
        java.util.function.checked.ShortToFloatFunction<Exception> callback = (value1) -> {
            check(value1 == (short) -32768);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((short) -32768)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case28() throws Throwable {
        java.util.function.checked.ShortToIntFunction<Exception> callback = (value1) -> {
            check(value1 == (short) -32768);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((short) -32768) == Integer.MIN_VALUE);
    }

    private static void case29() throws Throwable {
        java.util.function.checked.ShortToLongFunction<Exception> callback = (value1) -> {
            check(value1 == (short) -32768);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong((short) -32768) == Long.MIN_VALUE);
    }

    private static void case30() throws Throwable {
        java.util.function.checked.ShortUnaryOperator<Exception> callback = (value1) -> {
            check(value1 == (short) -32768);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort((short) -32768) == (short) -32768);
    }

    private static void case31() throws Throwable {
        java.util.function.checked.Supplier<String, Exception> callback = () -> {
            checks++;
            return "argument-one";
        };
        check(callback.get() == "argument-one");
    }

    private static void case32() throws Throwable {
        java.util.function.checked.ToByteBiFunction<String, String, Exception> callback = (value1,
                value2) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte("argument-one", "argument-two") == (byte) -128);
    }

    private static void case33() throws Throwable {
        java.util.function.checked.ToByteFunction<String, Exception> callback = (value1) -> {
            check(value1 == "argument-one");
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte("argument-one") == (byte) -128);
    }

    private static void case34() throws Throwable {
        java.util.function.checked.ToCharBiFunction<String, String, Exception> callback = (value1,
                value2) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar("argument-one", "argument-two") == '\uD800');
    }

    private static void case35() throws Throwable {
        java.util.function.checked.ToCharFunction<String, Exception> callback = (value1) -> {
            check(value1 == "argument-one");
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar("argument-one") == '\uD800');
    }

    private static void case36() throws Throwable {
        java.util.function.checked.ToDoubleBiFunction<String, String, Exception> callback = (value1,
                value2) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble("argument-one", "argument-two")) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case37() throws Throwable {
        java.util.function.checked.ToDoubleFunction<String, Exception> callback = (value1) -> {
            check(value1 == "argument-one");
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble("argument-one")) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case38() throws Throwable {
        java.util.function.checked.ToFloatBiFunction<String, String, Exception> callback = (value1,
                value2) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat("argument-one", "argument-two")) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case39() throws Throwable {
        java.util.function.checked.ToFloatFunction<String, Exception> callback = (value1) -> {
            check(value1 == "argument-one");
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat("argument-one")) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case40() throws Throwable {
        java.util.function.checked.ToIntBiFunction<String, String, Exception> callback = (value1,
                value2) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt("argument-one", "argument-two") == Integer.MIN_VALUE);
    }

    private static void case41() throws Throwable {
        java.util.function.checked.ToIntFunction<String, Exception> callback = (value1) -> {
            check(value1 == "argument-one");
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt("argument-one") == Integer.MIN_VALUE);
    }

    private static void case42() throws Throwable {
        java.util.function.checked.ToLongBiFunction<String, String, Exception> callback = (value1,
                value2) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong("argument-one", "argument-two") == Long.MIN_VALUE);
    }

    private static void case43() throws Throwable {
        java.util.function.checked.ToLongFunction<String, Exception> callback = (value1) -> {
            check(value1 == "argument-one");
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong("argument-one") == Long.MIN_VALUE);
    }

    private static void case44() throws Throwable {
        java.util.function.checked.ToShortBiFunction<String, String, Exception> callback = (value1,
                value2) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort("argument-one", "argument-two") == (short) -32768);
    }

    private static void case45() throws Throwable {
        java.util.function.checked.ToShortFunction<String, Exception> callback = (value1) -> {
            check(value1 == "argument-one");
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort("argument-one") == (short) -32768);
    }

    private static void case46() throws Throwable {
        java.util.function.checked.TriConsumer<String, String, String, Exception> callback =
                (value1, value2, value3) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            checks++;
        };
        callback.accept("argument-one", "argument-two", "argument-one");
    }

    private static void case47() throws Throwable {
        java.util.function.checked.TriFunction<String, String, String, String, Exception> callback =
                (value1, value2, value3) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            checks++;
            return "argument-one";
        };
        check(callback.apply("argument-one", "argument-two", "argument-one") == "argument-one");
    }

    private static void case48() throws Throwable {
        java.util.function.checked.TriPredicate<String, String, String, Exception> callback =
                (value1, value2, value3) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            checks++;
            return true;
        };
        check(callback.test("argument-one", "argument-two", "argument-one") == true);
    }

    private static void case49() throws Throwable {
        java.util.function.checked.UnaryOperator<String, Exception> callback = (value1) -> {
            check(value1 == "argument-one");
            checks++;
            return "argument-one";
        };
        check(callback.apply("argument-one") == "argument-one");
    }

    private static void case50() throws Throwable {
        java.util.function.ext.BooleanBiConsumer callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == false);
            checks++;
        };
        callback.accept(true, false);
    }

    private static void case51() throws Throwable {
        java.util.function.ext.BooleanBiFunction<String> callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == false);
            checks++;
            return "argument-one";
        };
        check(callback.apply(true, false) == "argument-one");
    }

    private static void case52() throws Throwable {
        java.util.function.ext.BooleanBiPredicate callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == false);
            checks++;
            return true;
        };
        check(callback.test(true, false) == true);
    }

    private static void case53() throws Throwable {
        java.util.function.ext.BooleanBiToByteFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == false);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(true, false) == (byte) -128);
    }

    private static void case54() throws Throwable {
        java.util.function.ext.BooleanBiToCharFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == false);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(true, false) == '\uD800');
    }

    private static void case55() throws Throwable {
        java.util.function.ext.BooleanBiToDoubleFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == false);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(true, false)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case56() throws Throwable {
        java.util.function.ext.BooleanBiToFloatFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == false);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(true, false)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case57() throws Throwable {
        java.util.function.ext.BooleanBiToIntFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == false);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(true, false) == Integer.MIN_VALUE);
    }

    private static void case58() throws Throwable {
        java.util.function.ext.BooleanBiToLongFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == false);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(true, false) == Long.MIN_VALUE);
    }

    private static void case59() throws Throwable {
        java.util.function.ext.BooleanBiToShortFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == false);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(true, false) == (short) -32768);
    }

    private static void case60() throws Throwable {
        java.util.function.ext.BooleanBinaryOperator callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == false);
            checks++;
            return true;
        };
        check(callback.applyAsBoolean(true, false) == true);
    }

    private static void case61() throws Throwable {
        java.util.function.ext.BooleanByteConsumer callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == (byte) 127);
            checks++;
        };
        callback.accept(true, (byte) 127);
    }

    private static void case62() throws Throwable {
        java.util.function.ext.BooleanByteFunction<String> callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == (byte) 127);
            checks++;
            return "argument-one";
        };
        check(callback.apply(true, (byte) 127) == "argument-one");
    }

    private static void case63() throws Throwable {
        java.util.function.ext.BooleanBytePredicate callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == (byte) 127);
            checks++;
            return true;
        };
        check(callback.test(true, (byte) 127) == true);
    }

    private static void case64() throws Throwable {
        java.util.function.ext.BooleanByteToByteFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == (byte) 127);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(true, (byte) 127) == (byte) -128);
    }

    private static void case65() throws Throwable {
        java.util.function.ext.BooleanByteToCharFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == (byte) 127);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(true, (byte) 127) == '\uD800');
    }

    private static void case66() throws Throwable {
        java.util.function.ext.BooleanByteToDoubleFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == (byte) 127);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(true, (byte) 127)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case67() throws Throwable {
        java.util.function.ext.BooleanByteToFloatFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == (byte) 127);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(true, (byte) 127)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case68() throws Throwable {
        java.util.function.ext.BooleanByteToIntFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == (byte) 127);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(true, (byte) 127) == Integer.MIN_VALUE);
    }

    private static void case69() throws Throwable {
        java.util.function.ext.BooleanByteToLongFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == (byte) 127);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(true, (byte) 127) == Long.MIN_VALUE);
    }

    private static void case70() throws Throwable {
        java.util.function.ext.BooleanByteToShortFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == (byte) 127);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(true, (byte) 127) == (short) -32768);
    }

    private static void case71() throws Throwable {
        java.util.function.ext.BooleanCharConsumer callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == '\uFFFF');
            checks++;
        };
        callback.accept(true, '\uFFFF');
    }

    private static void case72() throws Throwable {
        java.util.function.ext.BooleanCharFunction<String> callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == '\uFFFF');
            checks++;
            return "argument-one";
        };
        check(callback.apply(true, '\uFFFF') == "argument-one");
    }

    private static void case73() throws Throwable {
        java.util.function.ext.BooleanCharPredicate callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == '\uFFFF');
            checks++;
            return true;
        };
        check(callback.test(true, '\uFFFF') == true);
    }

    private static void case74() throws Throwable {
        java.util.function.ext.BooleanCharToByteFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == '\uFFFF');
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(true, '\uFFFF') == (byte) -128);
    }

    private static void case75() throws Throwable {
        java.util.function.ext.BooleanCharToCharFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == '\uFFFF');
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(true, '\uFFFF') == '\uD800');
    }

    private static void case76() throws Throwable {
        java.util.function.ext.BooleanCharToDoubleFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == '\uFFFF');
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(true, '\uFFFF')) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case77() throws Throwable {
        java.util.function.ext.BooleanCharToFloatFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == '\uFFFF');
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(true, '\uFFFF')) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case78() throws Throwable {
        java.util.function.ext.BooleanCharToIntFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == '\uFFFF');
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(true, '\uFFFF') == Integer.MIN_VALUE);
    }

    private static void case79() throws Throwable {
        java.util.function.ext.BooleanCharToLongFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == '\uFFFF');
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(true, '\uFFFF') == Long.MIN_VALUE);
    }

    private static void case80() throws Throwable {
        java.util.function.ext.BooleanCharToShortFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == '\uFFFF');
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(true, '\uFFFF') == (short) -32768);
    }

    private static void case81() throws Throwable {
        java.util.function.ext.BooleanComparator callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == false);
            checks++;
            return 0;
        };
        check(callback.compare(true, false) == 0);
    }

    private static void case82() throws Throwable {
        java.util.function.ext.BooleanConsumer callback = (value1) -> {
            check(value1 == true);
            checks++;
        };
        callback.accept(true);
    }

    private static void case83() throws Throwable {
        java.util.function.ext.BooleanDoubleConsumer callback = (value1, value2) -> {
            check(value1 == true);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
        };
        callback.accept(true, Double.NEGATIVE_INFINITY);
    }

    private static void case84() throws Throwable {
        java.util.function.ext.BooleanDoubleFunction<String> callback = (value1, value2) -> {
            check(value1 == true);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return "argument-one";
        };
        check(callback.apply(true, Double.NEGATIVE_INFINITY) == "argument-one");
    }

    private static void case85() throws Throwable {
        java.util.function.ext.BooleanDoublePredicate callback = (value1, value2) -> {
            check(value1 == true);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return true;
        };
        check(callback.test(true, Double.NEGATIVE_INFINITY) == true);
    }

    private static void case86() throws Throwable {
        java.util.function.ext.BooleanDoubleToByteFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(true, Double.NEGATIVE_INFINITY) == (byte) -128);
    }

    private static void case87() throws Throwable {
        java.util.function.ext.BooleanDoubleToCharFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(true, Double.NEGATIVE_INFINITY) == '\uD800');
    }

    private static void case88() throws Throwable {
        java.util.function.ext.BooleanDoubleToDoubleFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(true, Double.NEGATIVE_INFINITY)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case89() throws Throwable {
        java.util.function.ext.BooleanDoubleToFloatFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(true, Double.NEGATIVE_INFINITY)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case90() throws Throwable {
        java.util.function.ext.BooleanDoubleToIntFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(true, Double.NEGATIVE_INFINITY) == Integer.MIN_VALUE);
    }

    private static void case91() throws Throwable {
        java.util.function.ext.BooleanDoubleToLongFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(true, Double.NEGATIVE_INFINITY) == Long.MIN_VALUE);
    }

    private static void case92() throws Throwable {
        java.util.function.ext.BooleanDoubleToShortFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(true, Double.NEGATIVE_INFINITY) == (short) -32768);
    }

    private static void case93() throws Throwable {
        java.util.function.ext.BooleanFloatConsumer callback = (value1, value2) -> {
            check(value1 == true);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
        };
        callback.accept(true, Float.POSITIVE_INFINITY);
    }

    private static void case94() throws Throwable {
        java.util.function.ext.BooleanFloatFunction<String> callback = (value1, value2) -> {
            check(value1 == true);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return "argument-one";
        };
        check(callback.apply(true, Float.POSITIVE_INFINITY) == "argument-one");
    }

    private static void case95() throws Throwable {
        java.util.function.ext.BooleanFloatPredicate callback = (value1, value2) -> {
            check(value1 == true);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return true;
        };
        check(callback.test(true, Float.POSITIVE_INFINITY) == true);
    }

    private static void case96() throws Throwable {
        java.util.function.ext.BooleanFloatToByteFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(true, Float.POSITIVE_INFINITY) == (byte) -128);
    }

    private static void case97() throws Throwable {
        java.util.function.ext.BooleanFloatToCharFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(true, Float.POSITIVE_INFINITY) == '\uD800');
    }

    private static void case98() throws Throwable {
        java.util.function.ext.BooleanFloatToDoubleFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(true, Float.POSITIVE_INFINITY)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case99() throws Throwable {
        java.util.function.ext.BooleanFloatToFloatFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(true, Float.POSITIVE_INFINITY)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case100() throws Throwable {
        java.util.function.ext.BooleanFloatToIntFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(true, Float.POSITIVE_INFINITY) == Integer.MIN_VALUE);
    }

    private static void case101() throws Throwable {
        java.util.function.ext.BooleanFloatToLongFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(true, Float.POSITIVE_INFINITY) == Long.MIN_VALUE);
    }

    private static void case102() throws Throwable {
        java.util.function.ext.BooleanFloatToShortFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(true, Float.POSITIVE_INFINITY) == (short) -32768);
    }

    private static void case103() throws Throwable {
        java.util.function.ext.BooleanFunction<String> callback = (value1) -> {
            check(value1 == true);
            checks++;
            return "argument-one";
        };
        check(callback.apply(true) == "argument-one");
    }

    private static void case104() throws Throwable {
        java.util.function.ext.BooleanIntConsumer callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == Integer.MAX_VALUE);
            checks++;
        };
        callback.accept(true, Integer.MAX_VALUE);
    }

    private static void case105() throws Throwable {
        java.util.function.ext.BooleanIntFunction<String> callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply(true, Integer.MAX_VALUE) == "argument-one");
    }

    private static void case106() throws Throwable {
        java.util.function.ext.BooleanIntPredicate callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test(true, Integer.MAX_VALUE) == true);
    }

    private static void case107() throws Throwable {
        java.util.function.ext.BooleanIntToByteFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(true, Integer.MAX_VALUE) == (byte) -128);
    }

    private static void case108() throws Throwable {
        java.util.function.ext.BooleanIntToCharFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(true, Integer.MAX_VALUE) == '\uD800');
    }

    private static void case109() throws Throwable {
        java.util.function.ext.BooleanIntToDoubleFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(true, Integer.MAX_VALUE)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case110() throws Throwable {
        java.util.function.ext.BooleanIntToFloatFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(true, Integer.MAX_VALUE)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case111() throws Throwable {
        java.util.function.ext.BooleanIntToIntFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(true, Integer.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case112() throws Throwable {
        java.util.function.ext.BooleanIntToLongFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(true, Integer.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case113() throws Throwable {
        java.util.function.ext.BooleanIntToShortFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(true, Integer.MAX_VALUE) == (short) -32768);
    }

    private static void case114() throws Throwable {
        java.util.function.ext.BooleanLongConsumer callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == Long.MAX_VALUE);
            checks++;
        };
        callback.accept(true, Long.MAX_VALUE);
    }

    private static void case115() throws Throwable {
        java.util.function.ext.BooleanLongFunction<String> callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply(true, Long.MAX_VALUE) == "argument-one");
    }

    private static void case116() throws Throwable {
        java.util.function.ext.BooleanLongPredicate callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test(true, Long.MAX_VALUE) == true);
    }

    private static void case117() throws Throwable {
        java.util.function.ext.BooleanLongToByteFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(true, Long.MAX_VALUE) == (byte) -128);
    }

    private static void case118() throws Throwable {
        java.util.function.ext.BooleanLongToCharFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(true, Long.MAX_VALUE) == '\uD800');
    }

    private static void case119() throws Throwable {
        java.util.function.ext.BooleanLongToDoubleFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(true, Long.MAX_VALUE)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case120() throws Throwable {
        java.util.function.ext.BooleanLongToFloatFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(true, Long.MAX_VALUE)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case121() throws Throwable {
        java.util.function.ext.BooleanLongToIntFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(true, Long.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case122() throws Throwable {
        java.util.function.ext.BooleanLongToLongFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(true, Long.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case123() throws Throwable {
        java.util.function.ext.BooleanLongToShortFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(true, Long.MAX_VALUE) == (short) -32768);
    }

    private static void case124() throws Throwable {
        java.util.function.ext.BooleanObjConsumer<String> callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == "argument-two");
            checks++;
        };
        callback.accept(true, "argument-two");
    }

    private static void case125() throws Throwable {
        java.util.function.ext.BooleanObjFunction<String, String> callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == "argument-two");
            checks++;
            return "argument-one";
        };
        check(callback.apply(true, "argument-two") == "argument-one");
    }

    private static void case126() throws Throwable {
        java.util.function.ext.BooleanObjPredicate<String> callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == "argument-two");
            checks++;
            return true;
        };
        check(callback.test(true, "argument-two") == true);
    }

    private static void case127() throws Throwable {
        java.util.function.ext.BooleanObjToByteFunction<String> callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == "argument-two");
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(true, "argument-two") == (byte) -128);
    }

    private static void case128() throws Throwable {
        java.util.function.ext.BooleanObjToCharFunction<String> callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == "argument-two");
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(true, "argument-two") == '\uD800');
    }

    private static void case129() throws Throwable {
        java.util.function.ext.BooleanObjToDoubleFunction<String> callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == "argument-two");
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(true, "argument-two")) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case130() throws Throwable {
        java.util.function.ext.BooleanObjToFloatFunction<String> callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == "argument-two");
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(true, "argument-two")) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case131() throws Throwable {
        java.util.function.ext.BooleanObjToIntFunction<String> callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == "argument-two");
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(true, "argument-two") == Integer.MIN_VALUE);
    }

    private static void case132() throws Throwable {
        java.util.function.ext.BooleanObjToLongFunction<String> callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == "argument-two");
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(true, "argument-two") == Long.MIN_VALUE);
    }

    private static void case133() throws Throwable {
        java.util.function.ext.BooleanObjToShortFunction<String> callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == "argument-two");
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(true, "argument-two") == (short) -32768);
    }

    private static void case134() throws Throwable {
        java.util.function.ext.BooleanPredicate callback = (value1) -> {
            check(value1 == true);
            checks++;
            return true;
        };
        check(callback.test(true) == true);
    }

    private static void case135() throws Throwable {
        java.util.function.ext.BooleanShortConsumer callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == (short) 32767);
            checks++;
        };
        callback.accept(true, (short) 32767);
    }

    private static void case136() throws Throwable {
        java.util.function.ext.BooleanShortFunction<String> callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == (short) 32767);
            checks++;
            return "argument-one";
        };
        check(callback.apply(true, (short) 32767) == "argument-one");
    }

    private static void case137() throws Throwable {
        java.util.function.ext.BooleanShortPredicate callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == (short) 32767);
            checks++;
            return true;
        };
        check(callback.test(true, (short) 32767) == true);
    }

    private static void case138() throws Throwable {
        java.util.function.ext.BooleanShortToByteFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == (short) 32767);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(true, (short) 32767) == (byte) -128);
    }

    private static void case139() throws Throwable {
        java.util.function.ext.BooleanShortToCharFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == (short) 32767);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(true, (short) 32767) == '\uD800');
    }

    private static void case140() throws Throwable {
        java.util.function.ext.BooleanShortToDoubleFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == (short) 32767);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(true, (short) 32767)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case141() throws Throwable {
        java.util.function.ext.BooleanShortToFloatFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == (short) 32767);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(true, (short) 32767)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case142() throws Throwable {
        java.util.function.ext.BooleanShortToIntFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == (short) 32767);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(true, (short) 32767) == Integer.MIN_VALUE);
    }

    private static void case143() throws Throwable {
        java.util.function.ext.BooleanShortToLongFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == (short) 32767);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(true, (short) 32767) == Long.MIN_VALUE);
    }

    private static void case144() throws Throwable {
        java.util.function.ext.BooleanShortToShortFunction callback = (value1, value2) -> {
            check(value1 == true);
            check(value2 == (short) 32767);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(true, (short) 32767) == (short) -32768);
    }

    private static void case145() throws Throwable {
        java.util.function.ext.BooleanToByteFunction callback = (value1) -> {
            check(value1 == true);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(true) == (byte) -128);
    }

    private static void case146() throws Throwable {
        java.util.function.ext.BooleanToCharFunction callback = (value1) -> {
            check(value1 == true);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(true) == '\uD800');
    }

    private static void case147() throws Throwable {
        java.util.function.ext.BooleanToDoubleFunction callback = (value1) -> {
            check(value1 == true);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(true)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case148() throws Throwable {
        java.util.function.ext.BooleanToFloatFunction callback = (value1) -> {
            check(value1 == true);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(true)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case149() throws Throwable {
        java.util.function.ext.BooleanToIntFunction callback = (value1) -> {
            check(value1 == true);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(true) == Integer.MIN_VALUE);
    }

    private static void case150() throws Throwable {
        java.util.function.ext.BooleanToLongFunction callback = (value1) -> {
            check(value1 == true);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(true) == Long.MIN_VALUE);
    }

    private static void case151() throws Throwable {
        java.util.function.ext.BooleanToShortFunction callback = (value1) -> {
            check(value1 == true);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(true) == (short) -32768);
    }

    private static void case152() throws Throwable {
        java.util.function.ext.BooleanUnaryOperator callback = (value1) -> {
            check(value1 == true);
            checks++;
            return true;
        };
        check(callback.applyAsBoolean(true) == true);
    }

    private static void case153() throws Throwable {
        java.util.function.ext.ByteBiConsumer callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == (byte) 127);
            checks++;
        };
        callback.accept((byte) -128, (byte) 127);
    }

    private static void case154() throws Throwable {
        java.util.function.ext.ByteBiFunction<String> callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == (byte) 127);
            checks++;
            return "argument-one";
        };
        check(callback.apply((byte) -128, (byte) 127) == "argument-one");
    }

    private static void case155() throws Throwable {
        java.util.function.ext.ByteBiPredicate callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == (byte) 127);
            checks++;
            return true;
        };
        check(callback.test((byte) -128, (byte) 127) == true);
    }

    private static void case156() throws Throwable {
        java.util.function.ext.ByteBiToCharFunction callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == (byte) 127);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar((byte) -128, (byte) 127) == '\uD800');
    }

    private static void case157() throws Throwable {
        java.util.function.ext.ByteBiToDoubleFunction callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == (byte) 127);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble((byte) -128, (byte) 127)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case158() throws Throwable {
        java.util.function.ext.ByteBiToFloatFunction callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == (byte) 127);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat((byte) -128, (byte) 127)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case159() throws Throwable {
        java.util.function.ext.ByteBiToIntFunction callback = (value1, value2) -> {
            check(value1 == (byte) -128);
            check(value2 == (byte) 127);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt((byte) -128, (byte) 127) == Integer.MIN_VALUE);
    }
}
