/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */

/* @test
 * @summary M3 AST reuses canonical M3 String facts without changing String semantics
 * @modules jdk.compiler/com.sun.tools.javac.m3
 *          java.base/jdk.internal.access
 * @run main/othervm M3ASTTextFactsTest fallback
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3ASTTextFactsTest m3
 */

import com.sun.tools.javac.m3.M3ASTTextFacts;

public class M3ASTTextFactsTest {
    private static int checks;

    public static void main(String[] args) {
        boolean expectM3 = args.length == 1 && args[0].equals("m3");
        String source = new String("αbeta Gamma42\nreason".toCharArray());

        checkRange(source, 1, 5, "beta", expectM3);
        checkRange(source, 6, 13, "Gamma42", expectM3);
        checkRange(source, 14, source.length(), "reason", expectM3);

        M3ASTTextFacts.Facts whole = M3ASTTextFacts.of(source);
        check(whole.utf16Length() == source.length(), "whole length");
        check(whole.javaHash() == source.hashCode(), "whole Java hash");
        check(whole.codePointCount() == source.codePointCount(0, source.length()),
                "whole code points");
        check(whole.m3Backed() == expectM3, "whole M3 ownership expectation");
        check(whole.signalsAvailable() == expectM3, "whole signal expectation");

        if (expectM3) {
            check(whole.ownerStructuralHash64() != 0L, "M3 owner structural hash");
            check(whole.coordinate() != 0L, "M3 coordinate");
            rejectMissingSignal(whole.bitSignal64(), whole::mayContainUnits, "unit signal");
            rejectMissingSignal(whole.bigramSignal64(), whole::mayContainBigrams, "bigram signal");
            rejectMissingSignal(whole.trigramSignal64(), whole::mayContainTrigrams, "trigram signal");
        } else {
            check(whole.mayContainUnits(~0L), "fallback cannot false-negative unit signal");
            check(whole.mayContainBigrams(~0L), "fallback cannot false-negative bigram signal");
            check(whole.mayContainTrigrams(~0L), "fallback cannot false-negative trigram signal");
        }

        System.out.println("M3_AST_STRING_FACTS_PASS mode=" + args[0] + " checks=" + checks);
    }

    private static void checkRange(
            String source, int begin, int end, String expected, boolean expectM3) {
        M3ASTTextFacts.Facts facts = M3ASTTextFacts.of(source, begin, end);
        String range = source.substring(begin, end);

        check(range.equals(expected), "fixture range");
        check(facts.utf16Length() == range.length(), "range length");
        check(facts.javaHash() == range.hashCode(), "range Java hash");
        check(facts.codePointCount() == range.codePointCount(0, range.length()),
                "range code points");
        check(facts.ascii() == range.chars().allMatch(value -> value <= 0x7f), "range ASCII");
        check(facts.latin1() == range.chars().allMatch(value -> value <= 0xff), "range Latin-1");
        check(facts.m3Backed() == expectM3, "range M3 ownership");
        check(facts.signalsAvailable() == expectM3, "range signals");

        int upper = range.toUpperCase(java.util.Locale.ROOT).hashCode();
        int lower = range.toLowerCase(java.util.Locale.ROOT).hashCode();
        if (facts.ascii()) {
            check(facts.asciiUpperHash() == upper, "ASCII upper hash");
            check(facts.asciiLowerHash() == lower, "ASCII lower hash");
        }
    }

    private static void rejectMissingSignal(
            long available,
            java.util.function.LongPredicate predicate,
            String label) {
        check(predicate.test(available), label + " accepts its own bits");
        long missing = Long.lowestOneBit(~available);
        if (missing != 0L) {
            check(!predicate.test(missing), label + " rejects impossible bit");
        }
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
