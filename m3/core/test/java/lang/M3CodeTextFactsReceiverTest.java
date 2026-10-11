/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.Arrays;

/** Executable proof for the typed 11-field precomputed facts receiver. */
public final class M3CodeTextFactsReceiverTest {
    private static int checks;

    private M3CodeTextFactsReceiverTest() {}

    public static void main(String[] args) {
        long[] minHash = new long[16];
        for (int index = 0; index < minHash.length; index++) {
            minHash[index] = 100L + index;
        }
        M3CodeTextFacts facts = M3CodeTextFacts.fromPrecomputed(
                12, 10, 0x203, 0x0102030405060708L, 0x1112131415161718L,
                0x2122232425262728L, 0x3132333435363738L, 4, 9, 7, minHash);
        check(facts.utf16Length == 12);
        check(facts.codePointCount == 10);
        check(facts.textFlags == 0x203);
        check(facts.contentHash64 == 0x0102030405060708L);
        check(facts.presence64 == 0x1112131415161718L);
        check(facts.simHash64 == 0x2122232425262728L);
        check(facts.lexicalPacked == 0x3132333435363738L);
        check(facts.javaKeywordHits == 4);
        check(facts.codeScore == 9);
        check(facts.regexScore == 7);
        long[] copied = facts.minHash();
        check(Arrays.equals(copied, minHash));
        copied[0] = -1L;
        check(facts.minHash()[0] == 100L);
        minHash[1] = -2L;
        check(facts.minHash()[1] == 101L);

        expectIllegalArgument(() -> M3CodeTextFacts.fromPrecomputed(
                -1, 0, 0, 0L, 0L, 0L, 0L, 0, 0, 0, new long[16]));
        expectIllegalArgument(() -> M3CodeTextFacts.fromPrecomputed(
                2, 3, 0, 0L, 0L, 0L, 0L, 0, 0, 0, new long[16]));
        expectIllegalArgument(() -> M3CodeTextFacts.fromPrecomputed(
                2, 1, 0, 0L, 0L, 0L, 0L, 0, 0, 0, new long[15]));
        expectIllegalArgument(() -> M3CodeTextFacts.fromPrecomputed(
                2, 1, 0, 0L, 0L, 0L, 0L, -1, 0, 0, new long[16]));

        System.out.println("M3_CODE_TEXT_FACTS_RECEIVER_PASS checks=" + checks);
    }

    private static void expectIllegalArgument(Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }

    private static void check(boolean condition) {
        checks++;
        if (!condition) {
            throw new AssertionError("check " + checks);
        }
    }
}
