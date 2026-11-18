// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary M3 bounded decimal128 and IEEE binary128 const facts preserve exact literals
 * @modules jdk.m3.paradigm/jdk.internal.m3.paradigm.numeric
 * @run main/othervm Const128FactTest
 */
import jdk.internal.m3.paradigm.numeric.M3Const128;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Executable Java 21 diagnostic proof of the bounded internal M3JDK const128 fact evaluator. */
public final class Const128FactTest {
    private static int checks;

    private Const128FactTest() { }

    public static void main(String[] args) {
        var decimal = M3Const128.compileDeclaration("const decimal128 amount = 125.50;");
        equal(M3Const128.Type.DECIMAL128, decimal.type());
        equal("amount", decimal.name());
        equal("125.50", decimal.sourceLiteral());
        equal("D128:+12550E-2", decimal.encodedValue());
        equal(64, decimal.sourceSha256().length());
        equal(decimal, M3Const128.compileDeclaration("const decimal128 amount = 125.50;"));
        check(!decimal.sourceSha256().equals(M3Const128.compileDeclaration(
                " const decimal128 amount = 125.50;").sourceSha256()));
        check(decimal.tsv().startsWith("M3_CONST128_V1\tDECIMAL128\tamount\t"));

        var binary = M3Const128.compileDeclaration("const float128 amount = 125.50;");
        equal(M3Const128.Type.FLOAT128, binary.type());
        equal("F128:4005f600000000000000000000000000", binary.encodedValue());
        equal("F128:3fff0000000000000000000000000000", encodedFloat("1"));
        equal("F128:40000000000000000000000000000000", encodedFloat("2"));
        equal("F128:3ffb999999999999999999999999999a", encodedFloat("0.1"));
        equal("F128:00000000000000000000000000000000", encodedFloat("0"));
        equal("F128:80000000000000000000000000000000", encodedFloat("-0.0"));
        equal("D128:-0E-2", encodedDecimal("-0.00"));
        equal("D128:+0E-2", encodedDecimal("0.00"));
        equal("D128:+1E6111", encodedDecimal("1e6111"));
        equal("D128:+1234567890123456789012345678901234E0",
                encodedDecimal("1234567890123456789012345678901234"));
        equal("F128:00000000000000000000000000000000", encodedFloat("1e-20000"));

        // Exactly halfway between 1 and the next binary128 value must round to even.
        BigDecimal halfUlp = BigDecimal.ONE.divide(
                new BigDecimal(BigInteger.ONE.shiftLeft(113)));
        equal("F128:3fff0000000000000000000000000000",
                encodedFloat(BigDecimal.ONE.add(halfUlp).toPlainString()));
        equal("F128:3fff0000000000000000000000000002",
                encodedFloat(BigDecimal.ONE.add(halfUlp.multiply(BigDecimal.valueOf(3)))
                        .toPlainString()));

        List<String> invalid = new ArrayList<>(List.of(
                "const String amount = 125.50;",
                "const u32 amount = 125.50;",
                "final float128 amount = 125.50;",
                "const float128 amount = getValue();",
                "const float128 amount = 1 + 2;",
                "const float128 amount = NaN;",
                "const float128 amount = 12.50f;",
                "const float128 amount = 1e;",
                "const float128 amount = 1e+;",
                "const float128 amount = .;",
                "const float128 amount = 1e20001;",
                "const float128 amount = 1e6000;",
                "const decimal128 amount = 1e6112;",
                "const decimal128 amount = 1e-6177;",
                "const decimal128 amount = 12345678901234567890123456789012345;",
                "const float128 amount = 1; const float128 other = 2;",
                "\"const float128 amount = 125.50;\"",
                "// const float128 amount = 125.50;",
                "/*const float128 amount = 125.50;*/",
                "const float128 amount = 125.50; // tail",
                "const float128 amount = 125.50 + /*fake*/ 1;",
                "const float128 amount = 1_000;",
                "const float128 8bad = 1;",
                "const float128 amount = 1"));
        for (String source : invalid) refuse(source);
        refuse(null);
        refuse("const float128 amount = " + "1".repeat(16_385) + ";");
        randomizedDecimalQuantum();
        exactBinaryPowers();
        System.out.println("M3_CONST128_PASS checks=" + checks);
    }

    /** Retain exact coefficient, source quantum, and explicit negative sign. */
    private static void randomizedDecimalQuantum() {
        Random random = new Random(0x4D3344313238L);
        for (int iteration = 0; iteration < 500; iteration++) {
            int length = random.nextInt(34) + 1;
            StringBuilder literal = new StringBuilder();
            literal.append(random.nextInt(9) + 1);
            while (literal.length() < length) literal.append(random.nextInt(10));
            if (random.nextBoolean()) literal.insert(random.nextInt(length + 1), '.');
            if (random.nextBoolean()) literal.insert(0, '-');
            literal.append('E').append(random.nextInt(241) - 120);
            BigDecimal exact = new BigDecimal(literal.toString());
            String expected = "D128:" + (literal.charAt(0) == '-' ? "-" : "+")
                    + exact.unscaledValue().abs() + "E" + (-exact.scale());
            equal(expected, encodedDecimal(literal.toString()));
        }
    }

    /** Exact powers of two must have zero fraction bits and the proper exponent. */
    private static void exactBinaryPowers() {
        for (int exponent = -200; exponent <= 200; exponent++) {
            BigDecimal exact = exponent >= 0
                    ? new BigDecimal(BigInteger.ONE.shiftLeft(exponent))
                    : BigDecimal.ONE.divide(new BigDecimal(BigInteger.ONE.shiftLeft(-exponent)));
            BigInteger encoded = new BigInteger(encodedFloat(exact.toPlainString())
                    .substring("F128:".length()), 16);
            BigInteger expected = BigInteger.valueOf(exponent + 16_383).shiftLeft(112);
            equal(expected, encoded);
        }
    }

    private static String encodedFloat(String literal) {
        return M3Const128.compileDeclaration("const float128 result = " + literal + ";")
                .encodedValue();
    }

    private static String encodedDecimal(String literal) {
        return M3Const128.compileDeclaration("const decimal128 result = " + literal + ";")
                .encodedValue();
    }

    private static void refuse(String declaration) {
        boolean rejected = false;
        try {
            M3Const128.compileDeclaration(declaration);
        } catch (IllegalArgumentException | NullPointerException expected) {
            rejected = true;
        }
        check(rejected);
    }

    private static void equal(Object expected, Object actual) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }

    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("M3 const128 check " + checks);
    }
}
