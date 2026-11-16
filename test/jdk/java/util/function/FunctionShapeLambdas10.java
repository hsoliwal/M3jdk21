/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Every admitted functional shape accepts a typed lambda and preserves its positions.
 * @modules java.base/java.util.function.ext java.base/java.util.function.checked
 * @run main FunctionShapeLambdas10
 */

public class FunctionShapeLambdas10 {
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
        System.out.println("PASS FunctionShapeLambdas10 checks=" + checks);
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError("position/result mismatch");
        }
        checks++;
    }

    private static void case0() throws Throwable {
        java.util.function.ext.IntLongFunction<String> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply(Integer.MIN_VALUE, Long.MAX_VALUE) == "argument-one");
    }

    private static void case1() throws Throwable {
        java.util.function.ext.IntLongPredicate callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test(Integer.MIN_VALUE, Long.MAX_VALUE) == true);
    }

    private static void case2() throws Throwable {
        java.util.function.ext.IntLongToByteFunction callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Integer.MIN_VALUE, Long.MAX_VALUE) == (byte) -128);
    }

    private static void case3() throws Throwable {
        java.util.function.ext.IntLongToCharFunction callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Integer.MIN_VALUE, Long.MAX_VALUE) == '\uD800');
    }

    private static void case4() throws Throwable {
        java.util.function.ext.IntLongToDoubleFunction callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Integer.MIN_VALUE, Long.MAX_VALUE))
                == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case5() throws Throwable {
        java.util.function.ext.IntLongToFloatFunction callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Integer.MIN_VALUE, Long.MAX_VALUE)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case6() throws Throwable {
        java.util.function.ext.IntLongToIntFunction callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Integer.MIN_VALUE, Long.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case7() throws Throwable {
        java.util.function.ext.IntLongToLongFunction callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Integer.MIN_VALUE, Long.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case8() throws Throwable {
        java.util.function.ext.IntLongToShortFunction callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Integer.MIN_VALUE, Long.MAX_VALUE) == (short) -32768);
    }

    private static void case9() throws Throwable {
        java.util.function.ext.IntObjConsumer<String> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
        };
        callback.accept(Integer.MIN_VALUE, "argument-two");
    }

    private static void case10() throws Throwable {
        java.util.function.ext.IntObjFunction<String, String> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return "argument-one";
        };
        check(callback.apply(Integer.MIN_VALUE, "argument-two") == "argument-one");
    }

    private static void case11() throws Throwable {
        java.util.function.ext.IntObjPredicate<String> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return true;
        };
        check(callback.test(Integer.MIN_VALUE, "argument-two") == true);
    }

    private static void case12() throws Throwable {
        java.util.function.ext.IntObjToByteFunction<String> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Integer.MIN_VALUE, "argument-two") == (byte) -128);
    }

    private static void case13() throws Throwable {
        java.util.function.ext.IntObjToCharFunction<String> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Integer.MIN_VALUE, "argument-two") == '\uD800');
    }

    private static void case14() throws Throwable {
        java.util.function.ext.IntObjToDoubleFunction<String> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Integer.MIN_VALUE, "argument-two")) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case15() throws Throwable {
        java.util.function.ext.IntObjToFloatFunction<String> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Integer.MIN_VALUE, "argument-two")) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case16() throws Throwable {
        java.util.function.ext.IntObjToIntFunction<String> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Integer.MIN_VALUE, "argument-two") == Integer.MIN_VALUE);
    }

    private static void case17() throws Throwable {
        java.util.function.ext.IntObjToLongFunction<String> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Integer.MIN_VALUE, "argument-two") == Long.MIN_VALUE);
    }

    private static void case18() throws Throwable {
        java.util.function.ext.IntObjToShortFunction<String> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Integer.MIN_VALUE, "argument-two") == (short) -32768);
    }

    private static void case19() throws Throwable {
        java.util.function.ext.IntShortConsumer callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
        };
        callback.accept(Integer.MIN_VALUE, (short) 32767);
    }

    private static void case20() throws Throwable {
        java.util.function.ext.IntShortFunction<String> callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return "argument-one";
        };
        check(callback.apply(Integer.MIN_VALUE, (short) 32767) == "argument-one");
    }

    private static void case21() throws Throwable {
        java.util.function.ext.IntShortPredicate callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return true;
        };
        check(callback.test(Integer.MIN_VALUE, (short) 32767) == true);
    }

    private static void case22() throws Throwable {
        java.util.function.ext.IntShortToByteFunction callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Integer.MIN_VALUE, (short) 32767) == (byte) -128);
    }

    private static void case23() throws Throwable {
        java.util.function.ext.IntShortToCharFunction callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Integer.MIN_VALUE, (short) 32767) == '\uD800');
    }

    private static void case24() throws Throwable {
        java.util.function.ext.IntShortToDoubleFunction callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Integer.MIN_VALUE, (short) 32767))
                == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case25() throws Throwable {
        java.util.function.ext.IntShortToFloatFunction callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Integer.MIN_VALUE, (short) 32767)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case26() throws Throwable {
        java.util.function.ext.IntShortToIntFunction callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Integer.MIN_VALUE, (short) 32767) == Integer.MIN_VALUE);
    }

    private static void case27() throws Throwable {
        java.util.function.ext.IntShortToLongFunction callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Integer.MIN_VALUE, (short) 32767) == Long.MIN_VALUE);
    }

    private static void case28() throws Throwable {
        java.util.function.ext.IntShortToShortFunction callback = (value1, value2) -> {
            check(value1 == Integer.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Integer.MIN_VALUE, (short) 32767) == (short) -32768);
    }

    private static void case29() throws Throwable {
        java.util.function.ext.IntToByteFunction callback = (value1) -> {
            check(value1 == Integer.MIN_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Integer.MIN_VALUE) == (byte) -128);
    }

    private static void case30() throws Throwable {
        java.util.function.ext.IntToCharFunction callback = (value1) -> {
            check(value1 == Integer.MIN_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Integer.MIN_VALUE) == '\uD800');
    }

    private static void case31() throws Throwable {
        java.util.function.ext.IntToFloatFunction callback = (value1) -> {
            check(value1 == Integer.MIN_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Integer.MIN_VALUE)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case32() throws Throwable {
        java.util.function.ext.IntToShortFunction callback = (value1) -> {
            check(value1 == Integer.MIN_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Integer.MIN_VALUE) == (short) -32768);
    }

    private static void case33() throws Throwable {
        java.util.function.ext.LongBiConsumer callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
        };
        callback.accept(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private static void case34() throws Throwable {
        java.util.function.ext.LongBiFunction<String> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply(Long.MIN_VALUE, Long.MAX_VALUE) == "argument-one");
    }

    private static void case35() throws Throwable {
        java.util.function.ext.LongBiPredicate callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test(Long.MIN_VALUE, Long.MAX_VALUE) == true);
    }

    private static void case36() throws Throwable {
        java.util.function.ext.LongBiToByteFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Long.MIN_VALUE, Long.MAX_VALUE) == (byte) -128);
    }

    private static void case37() throws Throwable {
        java.util.function.ext.LongBiToCharFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Long.MIN_VALUE, Long.MAX_VALUE) == '\uD800');
    }

    private static void case38() throws Throwable {
        java.util.function.ext.LongBiToDoubleFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Long.MIN_VALUE, Long.MAX_VALUE)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case39() throws Throwable {
        java.util.function.ext.LongBiToFloatFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Long.MIN_VALUE, Long.MAX_VALUE)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case40() throws Throwable {
        java.util.function.ext.LongBiToIntFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Long.MIN_VALUE, Long.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case41() throws Throwable {
        java.util.function.ext.LongBiToShortFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Long.MIN_VALUE, Long.MAX_VALUE) == (short) -32768);
    }

    private static void case42() throws Throwable {
        java.util.function.ext.LongBooleanConsumer callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == false);
            checks++;
        };
        callback.accept(Long.MIN_VALUE, false);
    }

    private static void case43() throws Throwable {
        java.util.function.ext.LongBooleanFunction<String> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == false);
            checks++;
            return "argument-one";
        };
        check(callback.apply(Long.MIN_VALUE, false) == "argument-one");
    }

    private static void case44() throws Throwable {
        java.util.function.ext.LongBooleanPredicate callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == false);
            checks++;
            return true;
        };
        check(callback.test(Long.MIN_VALUE, false) == true);
    }

    private static void case45() throws Throwable {
        java.util.function.ext.LongBooleanToByteFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == false);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Long.MIN_VALUE, false) == (byte) -128);
    }

    private static void case46() throws Throwable {
        java.util.function.ext.LongBooleanToCharFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == false);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Long.MIN_VALUE, false) == '\uD800');
    }

    private static void case47() throws Throwable {
        java.util.function.ext.LongBooleanToDoubleFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == false);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Long.MIN_VALUE, false)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case48() throws Throwable {
        java.util.function.ext.LongBooleanToFloatFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == false);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Long.MIN_VALUE, false)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case49() throws Throwable {
        java.util.function.ext.LongBooleanToIntFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == false);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Long.MIN_VALUE, false) == Integer.MIN_VALUE);
    }

    private static void case50() throws Throwable {
        java.util.function.ext.LongBooleanToLongFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == false);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Long.MIN_VALUE, false) == Long.MIN_VALUE);
    }

    private static void case51() throws Throwable {
        java.util.function.ext.LongBooleanToShortFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == false);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Long.MIN_VALUE, false) == (short) -32768);
    }

    private static void case52() throws Throwable {
        java.util.function.ext.LongByteConsumer callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
        };
        callback.accept(Long.MIN_VALUE, (byte) 127);
    }

    private static void case53() throws Throwable {
        java.util.function.ext.LongByteFunction<String> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return "argument-one";
        };
        check(callback.apply(Long.MIN_VALUE, (byte) 127) == "argument-one");
    }

    private static void case54() throws Throwable {
        java.util.function.ext.LongBytePredicate callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return true;
        };
        check(callback.test(Long.MIN_VALUE, (byte) 127) == true);
    }

    private static void case55() throws Throwable {
        java.util.function.ext.LongByteToByteFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Long.MIN_VALUE, (byte) 127) == (byte) -128);
    }

    private static void case56() throws Throwable {
        java.util.function.ext.LongByteToCharFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Long.MIN_VALUE, (byte) 127) == '\uD800');
    }

    private static void case57() throws Throwable {
        java.util.function.ext.LongByteToDoubleFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Long.MIN_VALUE, (byte) 127)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case58() throws Throwable {
        java.util.function.ext.LongByteToFloatFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Long.MIN_VALUE, (byte) 127)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case59() throws Throwable {
        java.util.function.ext.LongByteToIntFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Long.MIN_VALUE, (byte) 127) == Integer.MIN_VALUE);
    }

    private static void case60() throws Throwable {
        java.util.function.ext.LongByteToLongFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Long.MIN_VALUE, (byte) 127) == Long.MIN_VALUE);
    }

    private static void case61() throws Throwable {
        java.util.function.ext.LongByteToShortFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (byte) 127);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Long.MIN_VALUE, (byte) 127) == (short) -32768);
    }

    private static void case62() throws Throwable {
        java.util.function.ext.LongCharConsumer callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
        };
        callback.accept(Long.MIN_VALUE, '\uFFFF');
    }

    private static void case63() throws Throwable {
        java.util.function.ext.LongCharFunction<String> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return "argument-one";
        };
        check(callback.apply(Long.MIN_VALUE, '\uFFFF') == "argument-one");
    }

    private static void case64() throws Throwable {
        java.util.function.ext.LongCharPredicate callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return true;
        };
        check(callback.test(Long.MIN_VALUE, '\uFFFF') == true);
    }

    private static void case65() throws Throwable {
        java.util.function.ext.LongCharToByteFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Long.MIN_VALUE, '\uFFFF') == (byte) -128);
    }

    private static void case66() throws Throwable {
        java.util.function.ext.LongCharToCharFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Long.MIN_VALUE, '\uFFFF') == '\uD800');
    }

    private static void case67() throws Throwable {
        java.util.function.ext.LongCharToDoubleFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Long.MIN_VALUE, '\uFFFF')) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case68() throws Throwable {
        java.util.function.ext.LongCharToFloatFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Long.MIN_VALUE, '\uFFFF')) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case69() throws Throwable {
        java.util.function.ext.LongCharToIntFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Long.MIN_VALUE, '\uFFFF') == Integer.MIN_VALUE);
    }

    private static void case70() throws Throwable {
        java.util.function.ext.LongCharToLongFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Long.MIN_VALUE, '\uFFFF') == Long.MIN_VALUE);
    }

    private static void case71() throws Throwable {
        java.util.function.ext.LongCharToShortFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == '\uFFFF');
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Long.MIN_VALUE, '\uFFFF') == (short) -32768);
    }

    private static void case72() throws Throwable {
        java.util.function.ext.LongComparator callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Long.MAX_VALUE);
            checks++;
            return 0;
        };
        check(callback.compare(Long.MIN_VALUE, Long.MAX_VALUE) == 0);
    }

    private static void case73() throws Throwable {
        java.util.function.ext.LongDoubleConsumer callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
        };
        callback.accept(Long.MIN_VALUE, Double.NEGATIVE_INFINITY);
    }

    private static void case74() throws Throwable {
        java.util.function.ext.LongDoubleFunction<String> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return "argument-one";
        };
        check(callback.apply(Long.MIN_VALUE, Double.NEGATIVE_INFINITY) == "argument-one");
    }

    private static void case75() throws Throwable {
        java.util.function.ext.LongDoublePredicate callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return true;
        };
        check(callback.test(Long.MIN_VALUE, Double.NEGATIVE_INFINITY) == true);
    }

    private static void case76() throws Throwable {
        java.util.function.ext.LongDoubleToByteFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Long.MIN_VALUE, Double.NEGATIVE_INFINITY) == (byte) -128);
    }

    private static void case77() throws Throwable {
        java.util.function.ext.LongDoubleToCharFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Long.MIN_VALUE, Double.NEGATIVE_INFINITY) == '\uD800');
    }

    private static void case78() throws Throwable {
        java.util.function.ext.LongDoubleToDoubleFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Long.MIN_VALUE,
                Double.NEGATIVE_INFINITY)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case79() throws Throwable {
        java.util.function.ext.LongDoubleToFloatFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Long.MIN_VALUE,
                Double.NEGATIVE_INFINITY)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case80() throws Throwable {
        java.util.function.ext.LongDoubleToIntFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Long.MIN_VALUE, Double.NEGATIVE_INFINITY) == Integer.MIN_VALUE);
    }

    private static void case81() throws Throwable {
        java.util.function.ext.LongDoubleToLongFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Long.MIN_VALUE, Double.NEGATIVE_INFINITY) == Long.MIN_VALUE);
    }

    private static void case82() throws Throwable {
        java.util.function.ext.LongDoubleToShortFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Long.MIN_VALUE, Double.NEGATIVE_INFINITY) == (short) -32768);
    }

    private static void case83() throws Throwable {
        java.util.function.ext.LongFloatConsumer callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
        };
        callback.accept(Long.MIN_VALUE, Float.POSITIVE_INFINITY);
    }

    private static void case84() throws Throwable {
        java.util.function.ext.LongFloatFunction<String> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return "argument-one";
        };
        check(callback.apply(Long.MIN_VALUE, Float.POSITIVE_INFINITY) == "argument-one");
    }

    private static void case85() throws Throwable {
        java.util.function.ext.LongFloatPredicate callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return true;
        };
        check(callback.test(Long.MIN_VALUE, Float.POSITIVE_INFINITY) == true);
    }

    private static void case86() throws Throwable {
        java.util.function.ext.LongFloatToByteFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Long.MIN_VALUE, Float.POSITIVE_INFINITY) == (byte) -128);
    }

    private static void case87() throws Throwable {
        java.util.function.ext.LongFloatToCharFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Long.MIN_VALUE, Float.POSITIVE_INFINITY) == '\uD800');
    }

    private static void case88() throws Throwable {
        java.util.function.ext.LongFloatToDoubleFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Long.MIN_VALUE,
                Float.POSITIVE_INFINITY)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case89() throws Throwable {
        java.util.function.ext.LongFloatToFloatFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Long.MIN_VALUE,
                Float.POSITIVE_INFINITY)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case90() throws Throwable {
        java.util.function.ext.LongFloatToIntFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Long.MIN_VALUE, Float.POSITIVE_INFINITY) == Integer.MIN_VALUE);
    }

    private static void case91() throws Throwable {
        java.util.function.ext.LongFloatToLongFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Long.MIN_VALUE, Float.POSITIVE_INFINITY) == Long.MIN_VALUE);
    }

    private static void case92() throws Throwable {
        java.util.function.ext.LongFloatToShortFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(Float.floatToRawIntBits(value2) ==
                    Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Long.MIN_VALUE, Float.POSITIVE_INFINITY) == (short) -32768);
    }

    private static void case93() throws Throwable {
        java.util.function.ext.LongIntConsumer callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
        };
        callback.accept(Long.MIN_VALUE, Integer.MAX_VALUE);
    }

    private static void case94() throws Throwable {
        java.util.function.ext.LongIntFunction<String> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return "argument-one";
        };
        check(callback.apply(Long.MIN_VALUE, Integer.MAX_VALUE) == "argument-one");
    }

    private static void case95() throws Throwable {
        java.util.function.ext.LongIntPredicate callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return true;
        };
        check(callback.test(Long.MIN_VALUE, Integer.MAX_VALUE) == true);
    }

    private static void case96() throws Throwable {
        java.util.function.ext.LongIntToByteFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Long.MIN_VALUE, Integer.MAX_VALUE) == (byte) -128);
    }

    private static void case97() throws Throwable {
        java.util.function.ext.LongIntToCharFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Long.MIN_VALUE, Integer.MAX_VALUE) == '\uD800');
    }

    private static void case98() throws Throwable {
        java.util.function.ext.LongIntToDoubleFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Long.MIN_VALUE, Integer.MAX_VALUE))
                == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case99() throws Throwable {
        java.util.function.ext.LongIntToFloatFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Long.MIN_VALUE, Integer.MAX_VALUE)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case100() throws Throwable {
        java.util.function.ext.LongIntToIntFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Long.MIN_VALUE, Integer.MAX_VALUE) == Integer.MIN_VALUE);
    }

    private static void case101() throws Throwable {
        java.util.function.ext.LongIntToLongFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Long.MIN_VALUE, Integer.MAX_VALUE) == Long.MIN_VALUE);
    }

    private static void case102() throws Throwable {
        java.util.function.ext.LongIntToShortFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == Integer.MAX_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Long.MIN_VALUE, Integer.MAX_VALUE) == (short) -32768);
    }

    private static void case103() throws Throwable {
        java.util.function.ext.LongObjConsumer<String> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
        };
        callback.accept(Long.MIN_VALUE, "argument-two");
    }

    private static void case104() throws Throwable {
        java.util.function.ext.LongObjFunction<String, String> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return "argument-one";
        };
        check(callback.apply(Long.MIN_VALUE, "argument-two") == "argument-one");
    }

    private static void case105() throws Throwable {
        java.util.function.ext.LongObjPredicate<String> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return true;
        };
        check(callback.test(Long.MIN_VALUE, "argument-two") == true);
    }

    private static void case106() throws Throwable {
        java.util.function.ext.LongObjToByteFunction<String> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Long.MIN_VALUE, "argument-two") == (byte) -128);
    }

    private static void case107() throws Throwable {
        java.util.function.ext.LongObjToCharFunction<String> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Long.MIN_VALUE, "argument-two") == '\uD800');
    }

    private static void case108() throws Throwable {
        java.util.function.ext.LongObjToDoubleFunction<String> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Long.MIN_VALUE, "argument-two")) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case109() throws Throwable {
        java.util.function.ext.LongObjToFloatFunction<String> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Long.MIN_VALUE, "argument-two")) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case110() throws Throwable {
        java.util.function.ext.LongObjToIntFunction<String> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Long.MIN_VALUE, "argument-two") == Integer.MIN_VALUE);
    }

    private static void case111() throws Throwable {
        java.util.function.ext.LongObjToLongFunction<String> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Long.MIN_VALUE, "argument-two") == Long.MIN_VALUE);
    }

    private static void case112() throws Throwable {
        java.util.function.ext.LongObjToShortFunction<String> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == "argument-two");
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Long.MIN_VALUE, "argument-two") == (short) -32768);
    }

    private static void case113() throws Throwable {
        java.util.function.ext.LongShortConsumer callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
        };
        callback.accept(Long.MIN_VALUE, (short) 32767);
    }

    private static void case114() throws Throwable {
        java.util.function.ext.LongShortFunction<String> callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return "argument-one";
        };
        check(callback.apply(Long.MIN_VALUE, (short) 32767) == "argument-one");
    }

    private static void case115() throws Throwable {
        java.util.function.ext.LongShortPredicate callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return true;
        };
        check(callback.test(Long.MIN_VALUE, (short) 32767) == true);
    }

    private static void case116() throws Throwable {
        java.util.function.ext.LongShortToByteFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Long.MIN_VALUE, (short) 32767) == (byte) -128);
    }

    private static void case117() throws Throwable {
        java.util.function.ext.LongShortToCharFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Long.MIN_VALUE, (short) 32767) == '\uD800');
    }

    private static void case118() throws Throwable {
        java.util.function.ext.LongShortToDoubleFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble(Long.MIN_VALUE, (short) 32767)) ==
                Double.doubleToRawLongBits(-0.0d));
    }

    private static void case119() throws Throwable {
        java.util.function.ext.LongShortToFloatFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Long.MIN_VALUE, (short) 32767)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case120() throws Throwable {
        java.util.function.ext.LongShortToIntFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt(Long.MIN_VALUE, (short) 32767) == Integer.MIN_VALUE);
    }

    private static void case121() throws Throwable {
        java.util.function.ext.LongShortToLongFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong(Long.MIN_VALUE, (short) 32767) == Long.MIN_VALUE);
    }

    private static void case122() throws Throwable {
        java.util.function.ext.LongShortToShortFunction callback = (value1, value2) -> {
            check(value1 == Long.MIN_VALUE);
            check(value2 == (short) 32767);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Long.MIN_VALUE, (short) 32767) == (short) -32768);
    }

    private static void case123() throws Throwable {
        java.util.function.ext.LongToByteFunction callback = (value1) -> {
            check(value1 == Long.MIN_VALUE);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte(Long.MIN_VALUE) == (byte) -128);
    }

    private static void case124() throws Throwable {
        java.util.function.ext.LongToCharFunction callback = (value1) -> {
            check(value1 == Long.MIN_VALUE);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar(Long.MIN_VALUE) == '\uD800');
    }

    private static void case125() throws Throwable {
        java.util.function.ext.LongToFloatFunction callback = (value1) -> {
            check(value1 == Long.MIN_VALUE);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat(Long.MIN_VALUE)) ==
                Float.floatToRawIntBits(-0.0f));
    }

    private static void case126() throws Throwable {
        java.util.function.ext.LongToShortFunction callback = (value1) -> {
            check(value1 == Long.MIN_VALUE);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort(Long.MIN_VALUE) == (short) -32768);
    }

    private static void case127() throws Throwable {
        java.util.function.ext.ObjBooleanConsumer<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == false);
            checks++;
        };
        callback.accept("argument-one", false);
    }

    private static void case128() throws Throwable {
        java.util.function.ext.ObjBooleanFunction<String, String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == false);
            checks++;
            return "argument-one";
        };
        check(callback.apply("argument-one", false) == "argument-one");
    }

    private static void case129() throws Throwable {
        java.util.function.ext.ObjBooleanPredicate<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == false);
            checks++;
            return true;
        };
        check(callback.test("argument-one", false) == true);
    }

    private static void case130() throws Throwable {
        java.util.function.ext.ObjBooleanToByteFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == false);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte("argument-one", false) == (byte) -128);
    }

    private static void case131() throws Throwable {
        java.util.function.ext.ObjBooleanToCharFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == false);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar("argument-one", false) == '\uD800');
    }

    private static void case132() throws Throwable {
        java.util.function.ext.ObjBooleanToDoubleFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == false);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble("argument-one", false)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case133() throws Throwable {
        java.util.function.ext.ObjBooleanToFloatFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == false);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat("argument-one", false)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case134() throws Throwable {
        java.util.function.ext.ObjBooleanToIntFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == false);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt("argument-one", false) == Integer.MIN_VALUE);
    }

    private static void case135() throws Throwable {
        java.util.function.ext.ObjBooleanToLongFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == false);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong("argument-one", false) == Long.MIN_VALUE);
    }

    private static void case136() throws Throwable {
        java.util.function.ext.ObjBooleanToShortFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == false);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort("argument-one", false) == (short) -32768);
    }

    private static void case137() throws Throwable {
        java.util.function.ext.ObjByteConsumer<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == (byte) 127);
            checks++;
        };
        callback.accept("argument-one", (byte) 127);
    }

    private static void case138() throws Throwable {
        java.util.function.ext.ObjByteFunction<String, String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == (byte) 127);
            checks++;
            return "argument-one";
        };
        check(callback.apply("argument-one", (byte) 127) == "argument-one");
    }

    private static void case139() throws Throwable {
        java.util.function.ext.ObjBytePredicate<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == (byte) 127);
            checks++;
            return true;
        };
        check(callback.test("argument-one", (byte) 127) == true);
    }

    private static void case140() throws Throwable {
        java.util.function.ext.ObjByteToByteFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == (byte) 127);
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte("argument-one", (byte) 127) == (byte) -128);
    }

    private static void case141() throws Throwable {
        java.util.function.ext.ObjByteToCharFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == (byte) 127);
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar("argument-one", (byte) 127) == '\uD800');
    }

    private static void case142() throws Throwable {
        java.util.function.ext.ObjByteToDoubleFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == (byte) 127);
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble("argument-one", (byte) 127)) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case143() throws Throwable {
        java.util.function.ext.ObjByteToFloatFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == (byte) 127);
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat("argument-one", (byte) 127)) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case144() throws Throwable {
        java.util.function.ext.ObjByteToIntFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == (byte) 127);
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt("argument-one", (byte) 127) == Integer.MIN_VALUE);
    }

    private static void case145() throws Throwable {
        java.util.function.ext.ObjByteToLongFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == (byte) 127);
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong("argument-one", (byte) 127) == Long.MIN_VALUE);
    }

    private static void case146() throws Throwable {
        java.util.function.ext.ObjByteToShortFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == (byte) 127);
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort("argument-one", (byte) 127) == (short) -32768);
    }

    private static void case147() throws Throwable {
        java.util.function.ext.ObjCharConsumer<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == '\uFFFF');
            checks++;
        };
        callback.accept("argument-one", '\uFFFF');
    }

    private static void case148() throws Throwable {
        java.util.function.ext.ObjCharFunction<String, String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == '\uFFFF');
            checks++;
            return "argument-one";
        };
        check(callback.apply("argument-one", '\uFFFF') == "argument-one");
    }

    private static void case149() throws Throwable {
        java.util.function.ext.ObjCharPredicate<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == '\uFFFF');
            checks++;
            return true;
        };
        check(callback.test("argument-one", '\uFFFF') == true);
    }

    private static void case150() throws Throwable {
        java.util.function.ext.ObjCharToByteFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == '\uFFFF');
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte("argument-one", '\uFFFF') == (byte) -128);
    }

    private static void case151() throws Throwable {
        java.util.function.ext.ObjCharToCharFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == '\uFFFF');
            checks++;
            return '\uD800';
        };
        check(callback.applyAsChar("argument-one", '\uFFFF') == '\uD800');
    }

    private static void case152() throws Throwable {
        java.util.function.ext.ObjCharToDoubleFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == '\uFFFF');
            checks++;
            return -0.0d;
        };
        check(Double.doubleToRawLongBits(callback.applyAsDouble("argument-one", '\uFFFF')) == Double.doubleToRawLongBits(-0.0d));
    }

    private static void case153() throws Throwable {
        java.util.function.ext.ObjCharToFloatFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == '\uFFFF');
            checks++;
            return -0.0f;
        };
        check(Float.floatToRawIntBits(callback.applyAsFloat("argument-one", '\uFFFF')) == Float.floatToRawIntBits(-0.0f));
    }

    private static void case154() throws Throwable {
        java.util.function.ext.ObjCharToIntFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == '\uFFFF');
            checks++;
            return Integer.MIN_VALUE;
        };
        check(callback.applyAsInt("argument-one", '\uFFFF') == Integer.MIN_VALUE);
    }

    private static void case155() throws Throwable {
        java.util.function.ext.ObjCharToLongFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == '\uFFFF');
            checks++;
            return Long.MIN_VALUE;
        };
        check(callback.applyAsLong("argument-one", '\uFFFF') == Long.MIN_VALUE);
    }

    private static void case156() throws Throwable {
        java.util.function.ext.ObjCharToShortFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(value2 == '\uFFFF');
            checks++;
            return (short) -32768;
        };
        check(callback.applyAsShort("argument-one", '\uFFFF') == (short) -32768);
    }

    private static void case157() throws Throwable {
        java.util.function.ext.ObjDoubleFunction<String, String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return "argument-one";
        };
        check(callback.apply("argument-one", Double.NEGATIVE_INFINITY) == "argument-one");
    }

    private static void case158() throws Throwable {
        java.util.function.ext.ObjDoublePredicate<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return true;
        };
        check(callback.test("argument-one", Double.NEGATIVE_INFINITY) == true);
    }

    private static void case159() throws Throwable {
        java.util.function.ext.ObjDoubleToByteFunction<String> callback = (value1, value2) -> {
            check(value1 == "argument-one");
            check(Double.doubleToRawLongBits(value2) ==
                    Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            checks++;
            return (byte) -128;
        };
        check(callback.applyAsByte("argument-one", Double.NEGATIVE_INFINITY) == (byte) -128);
    }
}
