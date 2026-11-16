/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Every admitted functional shape accepts a typed lambda and preserves its positions.
 * @modules java.base/java.util.function.ext java.base/java.util.function.checked
 * @run main FunctionShapeLambdas02
 */

public class FunctionShapeLambdas02 {
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
        System.out.println("PASS FunctionShapeLambdas02 checks=" + checks);
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError("position/result mismatch");
        }
        checks++;
    }

    private static void case0() throws Throwable {
        java.util.function.checked.CharLongPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == Long.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test('\uD800', Long.MAX_VALUE) == true);
    }

    private static void case1() throws Throwable {
        java.util.function.checked.CharLongToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte('\uD800', Long.MAX_VALUE) == (byte) -128);
    }

    private static void case2() throws Throwable {
        java.util.function.checked.CharLongToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(value2 == Long.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar('\uD800', Long.MAX_VALUE) == '\uD800');
    }

    private static void case3() throws Throwable {
        java.util.function.checked.CharLongToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble('\uD800', Long.MAX_VALUE)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case4() throws Throwable {
        java.util.function.checked.CharLongToFloatFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat('\uD800', Long.MAX_VALUE)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case5() throws Throwable {
        java.util.function.checked.CharLongToIntFunction<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt('\uD800', Long.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case6() throws Throwable {
        java.util.function.checked.CharLongToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong('\uD800', Long.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case7() throws Throwable {
        java.util.function.checked.CharLongToShortFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort('\uD800', Long.MAX_VALUE) == (short) -32768);
    }

    private static void case8() throws Throwable {
        java.util.function.checked.CharObjConsumer<String, Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(value2 == "argument-two");
            checks++;
        };
        callback.accept('\uD800', "argument-two");
    }

    private static void case9() throws Throwable {
        java.util.function.checked.CharObjFunction<String, String, Exception> callback = (value1,
                value2) -> {
            check(value1 == '\uD800');
            check(value2 == "argument-two");
            checks++;
            return "argument-one";
        };
        check(callback.apply('\uD800', "argument-two") == "argument-one");
    }

    private static void case10() throws Throwable {
        java.util.function.checked.CharObjPredicate<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(value2 == "argument-two");
            checks++;
            return true;
        };
        check(callback.test('\uD800', "argument-two") == true);
    }

    private static void case11() throws Throwable {
        java.util.function.checked.CharObjToByteFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == '\uD800');
            check(value2 == "argument-two");
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte('\uD800', "argument-two") == (byte) -128);
    }

    private static void case12() throws Throwable {
        java.util.function.checked.CharObjToCharFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == '\uD800');
            check(value2 == "argument-two");
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar('\uD800', "argument-two") == '\uD800');
    }

    private static void case13() throws Throwable {
        java.util.function.checked.CharObjToDoubleFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == '\uD800');
            check(value2 == "argument-two");
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble('\uD800', "argument-two")) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case14() throws Throwable {
        java.util.function.checked.CharObjToFloatFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == '\uD800');
            check(value2 == "argument-two");
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat('\uD800', "argument-two")) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case15() throws Throwable {
        java.util.function.checked.CharObjToIntFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == '\uD800');
            check(value2 == "argument-two");
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt('\uD800', "argument-two") == Integer.MIN_VALUE);
    }

    private static void case16() throws Throwable {
        java.util.function.checked.CharObjToLongFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == '\uD800');
            check(value2 == "argument-two");
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong('\uD800', "argument-two") == Long.MIN_VALUE);
    }

    private static void case17() throws Throwable {
        java.util.function.checked.CharObjToShortFunction<String, Exception> callback = (value1,
                value2) -> {
            check(value1 == '\uD800');
            check(value2 == "argument-two");
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort('\uD800', "argument-two") == (short) -32768);
    }

    private static void case18() throws Throwable {
        java.util.function.checked.CharPredicate<Exception> callback = (value1) -> {
            check(value1 == '\uD800');
            checks++;
            return true;
        };
        check(callback.test('\uD800') == true);
    }

    private static void case19() throws Throwable {
        java.util.function.checked.CharShortConsumer<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == (short) 32767);
            checks++;
        };
        callback.accept('\uD800', (short) 32767);
    }

    private static void case20() throws Throwable {
        java.util.function.checked.CharShortFunction<String, Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(value2 == (short) 32767);
            checks++;
            return "argument-one";
        };
        check(callback.apply('\uD800', (short) 32767) == "argument-one");
    }

    private static void case21() throws Throwable {
        java.util.function.checked.CharShortPredicate<Exception> callback = (value1, value2) -> {
            check(value1 == '\uD800');
            check(value2 == (short) 32767);
            checks++;
            return true;
        };
        check(callback.test('\uD800', (short) 32767) == true);
    }

    private static void case22() throws Throwable {
        java.util.function.checked.CharShortToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(value2 == (short) 32767);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte('\uD800', (short) 32767) == (byte) -128);
    }

    private static void case23() throws Throwable {
        java.util.function.checked.CharShortToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(value2 == (short) 32767);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar('\uD800', (short) 32767) == '\uD800');
    }

    private static void case24() throws Throwable {
        java.util.function.checked.CharShortToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(value2 == (short) 32767);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble('\uD800', (short) 32767)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case25() throws Throwable {
        java.util.function.checked.CharShortToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(value2 == (short) 32767);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat('\uD800', (short) 32767)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case26() throws Throwable {
        java.util.function.checked.CharShortToIntFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(value2 == (short) 32767);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt('\uD800', (short) 32767) == Integer.MIN_VALUE);
    }

    private static void case27() throws Throwable {
        java.util.function.checked.CharShortToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(value1 == '\uD800');
            check(value2 == (short) 32767);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong('\uD800', (short) 32767) == Long.MIN_VALUE);
    }

    private static void case28() throws Throwable {
        java.util.function.checked.CharShortToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(value1 == '\uD800');
            check(value2 == (short) 32767);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort('\uD800', (short) 32767) == (short) -32768);
    }

    private static void case29() throws Throwable {
        java.util.function.checked.CharSupplier<Exception> callback = () -> {
            checks++;
            return '\uD800';
        };
        check(callback.getAsChar() == '\uD800');
    }

    private static void case30() throws Throwable {
        java.util.function.checked.CharToByteFunction<Exception> callback = (value1) -> {
            check(value1 == '\uD800');
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte('\uD800') == (byte) -128);
    }

    private static void case31() throws Throwable {
        java.util.function.checked.CharToDoubleFunction<Exception> callback = (value1) -> {
            check(value1 == '\uD800');
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble('\uD800')) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case32() throws Throwable {
        java.util.function.checked.CharToFloatFunction<Exception> callback = (value1) -> {
            check(value1 == '\uD800');
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat('\uD800')) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case33() throws Throwable {
        java.util.function.checked.CharToIntFunction<Exception> callback = (value1) -> {
            check(value1 == '\uD800');
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt('\uD800') == Integer.MIN_VALUE);
    }

    private static void case34() throws Throwable {
        java.util.function.checked.CharToLongFunction<Exception> callback = (value1) -> {
            check(value1 == '\uD800');
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong('\uD800') == Long.MIN_VALUE);
    }

    private static void case35() throws Throwable {
        java.util.function.checked.CharToShortFunction<Exception> callback = (value1) -> {
            check(value1 == '\uD800');
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort('\uD800') == (short) -32768);
    }

    private static void case36() throws Throwable {
        java.util.function.checked.CharUnaryOperator<Exception> callback = (value1) -> {
            check(value1 == '\uD800');
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar('\uD800') == '\uD800');
    }

    private static void case37() throws Throwable {
        java.util.function.checked.Comparator<String, Exception> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            checks++;
            return 0;
        };
        check(callback.compare("argument-one", "argument-two") == 0);
    }

    private static void case38() throws Throwable {
        java.util.function.checked.Consumer<String, Exception> callback = (value1) -> {
            check(value1 == "argument-one");
            checks++;
        };
        callback.accept("argument-one");
    }

    private static void case39() throws Throwable {
        java.util.function.checked.Consumer10<String, String, String, String, String, String,
                String, String, String, String, Exception> callback = (value1, value2, value3,
                value4, value5, value6, value7, value8, value9, value10) -> {
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
        };
        callback.accept("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two");
    }

    private static void case40() throws Throwable {
        java.util.function.checked.Consumer11<String, String, String, String, String, String,
                String, String, String, String, String, Exception> callback = (value1, value2,
                value3, value4, value5, value6, value7, value8, value9, value10, value11) -> {
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
        };
        callback.accept("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one");
    }

    private static void case41() throws Throwable {
        java.util.function.checked.Consumer12<String, String, String, String, String, String,
                String, String, String, String, String, String, Exception> callback = (value1,
                value2, value3, value4, value5, value6, value7, value8, value9, value10, value11,
                value12) -> {
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
        };
        callback.accept("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two");
    }

    private static void case42() throws Throwable {
        java.util.function.checked.Consumer13<String, String, String, String, String, String,
                String, String, String, String, String, String, String, Exception> callback =
                (value1, value2, value3, value4, value5, value6, value7, value8, value9, value10,
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
        };
        callback.accept("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one");
    }

    private static void case43() throws Throwable {
        java.util.function.checked.Consumer14<String, String, String, String, String, String,
                String, String, String, String, String, String, String, String, Exception> callback
                = (value1, value2, value3, value4, value5, value6, value7, value8, value9, value10,
                value11, value12, value13, value14) -> {
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
        };
        callback.accept("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two");
    }

    private static void case44() throws Throwable {
        java.util.function.checked.Consumer15<String, String, String, String, String, String,
                String, String, String, String, String, String, String, String, String, Exception>
                callback = (value1, value2, value3, value4, value5, value6, value7, value8, value9,
                value10, value11, value12, value13, value14, value15) -> {
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
        };
        callback.accept("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one");
    }

    private static void case45() throws Throwable {
        java.util.function.checked.Consumer16<String, String, String, String, String, String,
                String, String, String, String, String, String, String, String, String, String,
                Exception> callback = (value1, value2, value3, value4, value5, value6, value7,
                value8, value9, value10, value11, value12, value13, value14, value15, value16) -> {
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
        };
        callback.accept("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two");
    }

    private static void case46() throws Throwable {
        java.util.function.checked.Consumer4<String, String, String, String, Exception> callback =
                (value1, value2, value3, value4) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            checks++;
        };
        callback.accept("argument-one", "argument-two", "argument-one", "argument-two");
    }

    private static void case47() throws Throwable {
        java.util.function.checked.Consumer5<String, String, String, String, String, Exception>
                callback = (value1, value2, value3, value4, value5) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            check(value5 == "argument-one");
            checks++;
        };
        callback.accept("argument-one", "argument-two", "argument-one", "argument-two", "argument-one");
    }

    private static void case48() throws Throwable {
        java.util.function.checked.Consumer6<String, String, String, String, String, String,
                Exception> callback = (value1, value2, value3, value4, value5, value6) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            check(value5 == "argument-one");
            check(value6 == "argument-two");
            checks++;
        };
        callback.accept("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two");
    }

    private static void case49() throws Throwable {
        java.util.function.checked.Consumer7<String, String, String, String, String, String, String,
                Exception> callback = (value1, value2, value3, value4, value5, value6, value7) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            check(value5 == "argument-one");
            check(value6 == "argument-two");
            check(value7 == "argument-one");
            checks++;
        };
        callback.accept("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one");
    }

    private static void case50() throws Throwable {
        java.util.function.checked.Consumer8<String, String, String, String, String, String, String,
                String, Exception> callback = (value1, value2, value3, value4, value5, value6,
                value7, value8) -> {
            check(value1 == "argument-one");
            check(value2 == "argument-two");
            check(value3 == "argument-one");
            check(value4 == "argument-two");
            check(value5 == "argument-one");
            check(value6 == "argument-two");
            check(value7 == "argument-one");
            check(value8 == "argument-two");
            checks++;
        };
        callback.accept("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two");
    }

    private static void case51() throws Throwable {
        java.util.function.checked.Consumer9<String, String, String, String, String, String, String,
                String, String, Exception> callback = (value1, value2, value3, value4, value5,
                value6, value7, value8, value9) -> {
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
        };
        callback.accept("argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one", "argument-two", "argument-one");
    }

    private static void case52() throws Throwable {
        java.util.function.checked.DoubleBiConsumer<Exception> callback = (value1, value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
        };
        callback.accept(-0.0d, Double.NEGATIVE_INFINITY);
    }

    private static void case53() throws Throwable {
        java.util.function.checked.DoubleBiFunction<String, Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return "argument-one";
        };
        check(callback.apply(-0.0d, Double.NEGATIVE_INFINITY) == "argument-one");
    }

    private static void case54() throws Throwable {
        java.util.function.checked.DoubleBiPredicate<Exception> callback = (value1, value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return true;
        };
        check(callback.test(-0.0d, Double.NEGATIVE_INFINITY) == true);
    }

    private static void case55() throws Throwable {
        java.util.function.checked.DoubleBiToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(-0.0d, Double.NEGATIVE_INFINITY) == (byte) -128);
    }

    private static void case56() throws Throwable {
        java.util.function.checked.DoubleBiToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(-0.0d, Double.NEGATIVE_INFINITY) == '\uD800');
    }

    private static void case57() throws Throwable {
        java.util.function.checked.DoubleBiToFloatFunction<Exception> callback = (value1, value2) ->
                {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(-0.0d, Double.NEGATIVE_INFINITY)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case58() throws Throwable {
        java.util.function.checked.DoubleBiToIntFunction<Exception> callback = (value1, value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(-0.0d, Double.NEGATIVE_INFINITY) == Integer.MIN_VALUE);
    }

    private static void case59() throws Throwable {
        java.util.function.checked.DoubleBiToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(-0.0d, Double.NEGATIVE_INFINITY) == Long.MIN_VALUE);
    }

    private static void case60() throws Throwable {
        java.util.function.checked.DoubleBiToShortFunction<Exception> callback = (value1, value2) ->
                {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(-0.0d, Double.NEGATIVE_INFINITY) == (short) -32768);
    }

    private static void case61() throws Throwable {
        java.util.function.checked.DoubleBinaryOperator<Exception> callback = (value1, value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(-0.0d, Double.NEGATIVE_INFINITY)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case62() throws Throwable {
        java.util.function.checked.DoubleBooleanConsumer<Exception> callback = (value1, value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == false);
            checks++;
        };
        callback.accept(-0.0d, false);
    }

    private static void case63() throws Throwable {
        java.util.function.checked.DoubleBooleanFunction<String, Exception> callback = (value1,
                value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == false);
            checks++;
            return "argument-one";
        };
        check(callback.apply(-0.0d, false) == "argument-one");
    }

    private static void case64() throws Throwable {
        java.util.function.checked.DoubleBooleanPredicate<Exception> callback = (value1, value2) ->
                {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == false);
            checks++;
            return true;
        };
        check(callback.test(-0.0d, false) == true);
    }

    private static void case65() throws Throwable {
        java.util.function.checked.DoubleBooleanToByteFunction<Exception> callback = (value1,
                value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == false);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(-0.0d, false) == (byte) -128);
    }

    private static void case66() throws Throwable {
        java.util.function.checked.DoubleBooleanToCharFunction<Exception> callback = (value1,
                value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == false);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(-0.0d, false) == '\uD800');
    }

    private static void case67() throws Throwable {
        java.util.function.checked.DoubleBooleanToDoubleFunction<Exception> callback = (value1,
                value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == false);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(-0.0d, false)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case68() throws Throwable {
        java.util.function.checked.DoubleBooleanToFloatFunction<Exception> callback = (value1,
                value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == false);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(-0.0d, false)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case69() throws Throwable {
        java.util.function.checked.DoubleBooleanToIntFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == false);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(-0.0d, false) == Integer.MIN_VALUE);
    }

    private static void case70() throws Throwable {
        java.util.function.checked.DoubleBooleanToLongFunction<Exception> callback = (value1,
                value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == false);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(-0.0d, false) == Long.MIN_VALUE);
    }

    private static void case71() throws Throwable {
        java.util.function.checked.DoubleBooleanToShortFunction<Exception> callback = (value1,
                value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == false);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(-0.0d, false) == (short) -32768);
    }

    private static void case72() throws Throwable {
        java.util.function.checked.DoubleByteConsumer<Exception> callback = (value1, value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == (byte) 127);
            checks++;
        };
        callback.accept(-0.0d, (byte) 127);
    }

    private static void case73() throws Throwable {
        java.util.function.checked.DoubleByteFunction<String, Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == (byte) 127);
            checks++;
            return "argument-one";
        };
        check(callback.apply(-0.0d, (byte) 127) == "argument-one");
    }

    private static void case74() throws Throwable {
        java.util.function.checked.DoubleBytePredicate<Exception> callback = (value1, value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == (byte) 127);
            checks++;
            return true;
        };
        check(callback.test(-0.0d, (byte) 127) == true);
    }

    private static void case75() throws Throwable {
        java.util.function.checked.DoubleByteToByteFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == (byte) 127);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(-0.0d, (byte) 127) == (byte) -128);
    }

    private static void case76() throws Throwable {
        java.util.function.checked.DoubleByteToCharFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == (byte) 127);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(-0.0d, (byte) 127) == '\uD800');
    }

    private static void case77() throws Throwable {
        java.util.function.checked.DoubleByteToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == (byte) 127);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(-0.0d, (byte) 127)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case78() throws Throwable {
        java.util.function.checked.DoubleByteToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == (byte) 127);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(-0.0d, (byte) 127)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case79() throws Throwable {
        java.util.function.checked.DoubleByteToIntFunction<Exception> callback = (value1, value2) ->
                {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == (byte) 127);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(-0.0d, (byte) 127) == Integer.MIN_VALUE);
    }

    private static void case80() throws Throwable {
        java.util.function.checked.DoubleByteToLongFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == (byte) 127);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(-0.0d, (byte) 127) == Long.MIN_VALUE);
    }

    private static void case81() throws Throwable {
        java.util.function.checked.DoubleByteToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == (byte) 127);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(-0.0d, (byte) 127) == (short) -32768);
    }

    private static void case82() throws Throwable {
        java.util.function.checked.DoubleCharConsumer<Exception> callback = (value1, value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == '\uFFFF');
            checks++;
        };
        callback.accept(-0.0d, '\uFFFF');
    }

    private static void case83() throws Throwable {
        java.util.function.checked.DoubleCharFunction<String, Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == '\uFFFF');
            checks++;
            return "argument-one";
        };
        check(callback.apply(-0.0d, '\uFFFF') == "argument-one");
    }

    private static void case84() throws Throwable {
        java.util.function.checked.DoubleCharPredicate<Exception> callback = (value1, value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == '\uFFFF');
            checks++;
            return true;
        };
        check(callback.test(-0.0d, '\uFFFF') == true);
    }

    private static void case85() throws Throwable {
        java.util.function.checked.DoubleCharToByteFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == '\uFFFF');
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(-0.0d, '\uFFFF') == (byte) -128);
    }

    private static void case86() throws Throwable {
        java.util.function.checked.DoubleCharToCharFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == '\uFFFF');
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(-0.0d, '\uFFFF') == '\uD800');
    }

    private static void case87() throws Throwable {
        java.util.function.checked.DoubleCharToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == '\uFFFF');
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(-0.0d, '\uFFFF')) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case88() throws Throwable {
        java.util.function.checked.DoubleCharToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == '\uFFFF');
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(-0.0d, '\uFFFF')) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case89() throws Throwable {
        java.util.function.checked.DoubleCharToIntFunction<Exception> callback = (value1, value2) ->
                {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == '\uFFFF');
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(-0.0d, '\uFFFF') == Integer.MIN_VALUE);
    }

    private static void case90() throws Throwable {
        java.util.function.checked.DoubleCharToLongFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == '\uFFFF');
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(-0.0d, '\uFFFF') == Long.MIN_VALUE);
    }

    private static void case91() throws Throwable {
        java.util.function.checked.DoubleCharToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == '\uFFFF');
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(-0.0d, '\uFFFF') == (short) -32768);
    }

    private static void case92() throws Throwable {
        java.util.function.checked.DoubleComparator<Exception> callback = (value1, value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return 0;
        };
        check(callback.compare(-0.0d, Double.NEGATIVE_INFINITY) == 0);
    }

    private static void case93() throws Throwable {
        java.util.function.checked.DoubleConsumer<Exception> callback = (value1) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            checks++;
        };
        callback.accept(-0.0d);
    }

    private static void case94() throws Throwable {
        java.util.function.checked.DoubleFloatConsumer<Exception> callback = (value1, value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
        };
        callback.accept(-0.0d, Float.POSITIVE_INFINITY);
    }

    private static void case95() throws Throwable {
        java.util.function.checked.DoubleFloatFunction<String, Exception> callback = (value1,
                value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return "argument-one";
        };
        check(callback.apply(-0.0d, Float.POSITIVE_INFINITY) == "argument-one");
    }

    private static void case96() throws Throwable {
        java.util.function.checked.DoubleFloatPredicate<Exception> callback = (value1, value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return true;
        };
        check(callback.test(-0.0d, Float.POSITIVE_INFINITY) == true);
    }

    private static void case97() throws Throwable {
        java.util.function.checked.DoubleFloatToByteFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(-0.0d, Float.POSITIVE_INFINITY) == (byte) -128);
    }

    private static void case98() throws Throwable {
        java.util.function.checked.DoubleFloatToCharFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(-0.0d, Float.POSITIVE_INFINITY) == '\uD800');
    }

    private static void case99() throws Throwable {
        java.util.function.checked.DoubleFloatToDoubleFunction<Exception> callback = (value1,
                value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(-0.0d, Float.POSITIVE_INFINITY)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case100() throws Throwable {
        java.util.function.checked.DoubleFloatToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(-0.0d, Float.POSITIVE_INFINITY)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case101() throws Throwable {
        java.util.function.checked.DoubleFloatToIntFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(-0.0d, Float.POSITIVE_INFINITY) == Integer.MIN_VALUE);
    }

    private static void case102() throws Throwable {
        java.util.function.checked.DoubleFloatToLongFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(-0.0d, Float.POSITIVE_INFINITY) == Long.MIN_VALUE);
    }

    private static void case103() throws Throwable {
        java.util.function.checked.DoubleFloatToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(-0.0d, Float.POSITIVE_INFINITY) == (short) -32768);
    }

    private static void case104() throws Throwable {
        java.util.function.checked.DoubleFunction<String, Exception> callback = (value1) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            checks++;
            return "argument-one";
        };
        check(callback.apply(-0.0d) == "argument-one");
    }

    private static void case105() throws Throwable {
        java.util.function.checked.DoubleIntConsumer<Exception> callback = (value1, value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == Integer.MAX_VALUE);
            checks++;
        };
        callback.accept(-0.0d, Integer.MAX_VALUE);
    }

    private static void case106() throws Throwable {
        java.util.function.checked.DoubleIntFunction<String, Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply(-0.0d, Integer.MAX_VALUE) == "argument-one");
    }

    private static void case107() throws Throwable {
        java.util.function.checked.DoubleIntPredicate<Exception> callback = (value1, value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test(-0.0d, Integer.MAX_VALUE) == true);
    }

    private static void case108() throws Throwable {
        java.util.function.checked.DoubleIntToByteFunction<Exception> callback = (value1, value2) ->
                {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(-0.0d, Integer.MAX_VALUE) == (byte) -128);
    }

    private static void case109() throws Throwable {
        java.util.function.checked.DoubleIntToCharFunction<Exception> callback = (value1, value2) ->
                {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(-0.0d, Integer.MAX_VALUE) == '\uD800');
    }

    private static void case110() throws Throwable {
        java.util.function.checked.DoubleIntToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(-0.0d, Integer.MAX_VALUE)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case111() throws Throwable {
        java.util.function.checked.DoubleIntToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(-0.0d, Integer.MAX_VALUE)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case112() throws Throwable {
        java.util.function.checked.DoubleIntToIntFunction<Exception> callback = (value1, value2) ->
                {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(-0.0d, Integer.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case113() throws Throwable {
        java.util.function.checked.DoubleIntToLongFunction<Exception> callback = (value1, value2) ->
                {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(-0.0d, Integer.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case114() throws Throwable {
        java.util.function.checked.DoubleIntToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(-0.0d, Integer.MAX_VALUE) == (short) -32768);
    }

    private static void case115() throws Throwable {
        java.util.function.checked.DoubleLongConsumer<Exception> callback = (value1, value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == Long.MAX_VALUE);
            checks++;
        };
        callback.accept(-0.0d, Long.MAX_VALUE);
    }

    private static void case116() throws Throwable {
        java.util.function.checked.DoubleLongFunction<String, Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == Long.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply(-0.0d, Long.MAX_VALUE) == "argument-one");
    }

    private static void case117() throws Throwable {
        java.util.function.checked.DoubleLongPredicate<Exception> callback = (value1, value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == Long.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test(-0.0d, Long.MAX_VALUE) == true);
    }

    private static void case118() throws Throwable {
        java.util.function.checked.DoubleLongToByteFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(-0.0d, Long.MAX_VALUE) == (byte) -128);
    }

    private static void case119() throws Throwable {
        java.util.function.checked.DoubleLongToCharFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == Long.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(-0.0d, Long.MAX_VALUE) == '\uD800');
    }

    private static void case120() throws Throwable {
        java.util.function.checked.DoubleLongToDoubleFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(-0.0d, Long.MAX_VALUE)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case121() throws Throwable {
        java.util.function.checked.DoubleLongToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(-0.0d, Long.MAX_VALUE)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case122() throws Throwable {
        java.util.function.checked.DoubleLongToIntFunction<Exception> callback = (value1, value2) ->
                {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(-0.0d, Long.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case123() throws Throwable {
        java.util.function.checked.DoubleLongToLongFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(-0.0d, Long.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case124() throws Throwable {
        java.util.function.checked.DoubleLongToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(-0.0d, Long.MAX_VALUE) == (short) -32768);
    }

    private static void case125() throws Throwable {
        java.util.function.checked.DoubleObjConsumer<String, Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == "argument-two");
            checks++;
        };
        callback.accept(-0.0d, "argument-two");
    }

    private static void case126() throws Throwable {
        java.util.function.checked.DoubleObjFunction<String, String, Exception> callback = (value1,
                value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == "argument-two");
            checks++;
            return "argument-one";
        };
        check(callback.apply(-0.0d, "argument-two") == "argument-one");
    }

    private static void case127() throws Throwable {
        java.util.function.checked.DoubleObjPredicate<String, Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == "argument-two");
            checks++;
            return true;
        };
        check(callback.test(-0.0d, "argument-two") == true);
    }

    private static void case128() throws Throwable {
        java.util.function.checked.DoubleObjToByteFunction<String, Exception> callback = (value1,
                value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == "argument-two");
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(-0.0d, "argument-two") == (byte) -128);
    }

    private static void case129() throws Throwable {
        java.util.function.checked.DoubleObjToCharFunction<String, Exception> callback = (value1,
                value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == "argument-two");
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(-0.0d, "argument-two") == '\uD800');
    }

    private static void case130() throws Throwable {
        java.util.function.checked.DoubleObjToDoubleFunction<String, Exception> callback = (value1,
                value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == "argument-two");
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(-0.0d, "argument-two")) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case131() throws Throwable {
        java.util.function.checked.DoubleObjToFloatFunction<String, Exception> callback = (value1,
                value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == "argument-two");
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(-0.0d, "argument-two")) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case132() throws Throwable {
        java.util.function.checked.DoubleObjToIntFunction<String, Exception> callback = (value1,
                value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == "argument-two");
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(-0.0d, "argument-two") == Integer.MIN_VALUE);
    }

    private static void case133() throws Throwable {
        java.util.function.checked.DoubleObjToLongFunction<String, Exception> callback = (value1,
                value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == "argument-two");
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(-0.0d, "argument-two") == Long.MIN_VALUE);
    }

    private static void case134() throws Throwable {
        java.util.function.checked.DoubleObjToShortFunction<String, Exception> callback = (value1,
                value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == "argument-two");
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(-0.0d, "argument-two") == (short) -32768);
    }

    private static void case135() throws Throwable {
        java.util.function.checked.DoublePredicate<Exception> callback = (value1) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            checks++;
            return true;
        };
        check(callback.test(-0.0d) == true);
    }

    private static void case136() throws Throwable {
        java.util.function.checked.DoubleShortConsumer<Exception> callback = (value1, value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == (short) 32767);
            checks++;
        };
        callback.accept(-0.0d, (short) 32767);
    }

    private static void case137() throws Throwable {
        java.util.function.checked.DoubleShortFunction<String, Exception> callback = (value1,
                value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == (short) 32767);
            checks++;
            return "argument-one";
        };
        check(callback.apply(-0.0d, (short) 32767) == "argument-one");
    }

    private static void case138() throws Throwable {
        java.util.function.checked.DoubleShortPredicate<Exception> callback = (value1, value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == (short) 32767);
            checks++;
            return true;
        };
        check(callback.test(-0.0d, (short) 32767) == true);
    }

    private static void case139() throws Throwable {
        java.util.function.checked.DoubleShortToByteFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == (short) 32767);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(-0.0d, (short) 32767) == (byte) -128);
    }

    private static void case140() throws Throwable {
        java.util.function.checked.DoubleShortToCharFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == (short) 32767);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(-0.0d, (short) 32767) == '\uD800');
    }

    private static void case141() throws Throwable {
        java.util.function.checked.DoubleShortToDoubleFunction<Exception> callback = (value1,
                value2) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == (short) 32767);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(-0.0d, (short) 32767)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case142() throws Throwable {
        java.util.function.checked.DoubleShortToFloatFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == (short) 32767);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(-0.0d, (short) 32767)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case143() throws Throwable {
        java.util.function.checked.DoubleShortToIntFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == (short) 32767);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(-0.0d, (short) 32767) == Integer.MIN_VALUE);
    }

    private static void case144() throws Throwable {
        java.util.function.checked.DoubleShortToLongFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == (short) 32767);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(-0.0d, (short) 32767) == Long.MIN_VALUE);
    }

    private static void case145() throws Throwable {
        java.util.function.checked.DoubleShortToShortFunction<Exception> callback = (value1, value2)
                -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            check(value2 == (short) 32767);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(-0.0d, (short) 32767) == (short) -32768);
    }

    private static void case146() throws Throwable {
        java.util.function.checked.DoubleSupplier<Exception> callback = () -> {
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.getAsDouble()) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case147() throws Throwable {
        java.util.function.checked.DoubleToByteFunction<Exception> callback = (value1) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(-0.0d) == (byte) -128);
    }

    private static void case148() throws Throwable {
        java.util.function.checked.DoubleToCharFunction<Exception> callback = (value1) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(-0.0d) == '\uD800');
    }

    private static void case149() throws Throwable {
        java.util.function.checked.DoubleToFloatFunction<Exception> callback = (value1) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(-0.0d)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case150() throws Throwable {
        java.util.function.checked.DoubleToIntFunction<Exception> callback = (value1) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(-0.0d) == Integer.MIN_VALUE);
    }

    private static void case151() throws Throwable {
        java.util.function.checked.DoubleToLongFunction<Exception> callback = (value1) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(-0.0d) == Long.MIN_VALUE);
    }

    private static void case152() throws Throwable {
        java.util.function.checked.DoubleToShortFunction<Exception> callback = (value1) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(-0.0d) == (short) -32768);
    }

    private static void case153() throws Throwable {
        java.util.function.checked.DoubleUnaryOperator<Exception> callback = (value1) -> {
            check(Double.doubleToRawLongBits(value1) == Double.doubleToRawLongBits(-0.0d));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(-0.0d)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case154() throws Throwable {
        java.util.function.checked.FloatBiConsumer<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
        };
        callback.accept(-0.0f, Float.POSITIVE_INFINITY);
    }

    private static void case155() throws Throwable {
        java.util.function.checked.FloatBiFunction<String, Exception> callback = (value1, value2) ->
                {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return "argument-one";
        };
        check(callback.apply(-0.0f, Float.POSITIVE_INFINITY) == "argument-one");
    }

    private static void case156() throws Throwable {
        java.util.function.checked.FloatBiPredicate<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return true;
        };
        check(callback.test(-0.0f, Float.POSITIVE_INFINITY) == true);
    }

    private static void case157() throws Throwable {
        java.util.function.checked.FloatBiToByteFunction<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(-0.0f, Float.POSITIVE_INFINITY) == (byte) -128);
    }

    private static void case158() throws Throwable {
        java.util.function.checked.FloatBiToCharFunction<Exception> callback = (value1, value2) -> {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(-0.0f, Float.POSITIVE_INFINITY) == '\uD800');
    }

    private static void case159() throws Throwable {
        java.util.function.checked.FloatBiToDoubleFunction<Exception> callback = (value1, value2) ->
                {
            check(Float.floatToRawIntBits(value1) == Float.floatToRawIntBits(-0.0f));
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(-0.0f, Float.POSITIVE_INFINITY)) ==
                Double.doubleToRawLongBits(-0.0d));
    }
}
