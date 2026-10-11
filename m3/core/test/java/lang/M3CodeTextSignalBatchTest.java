/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

/** Executable proof for bounded packed code-text signal rows. */
public final class M3CodeTextSignalBatchTest {
    private static int checks;

    private M3CodeTextSignalBatchTest() {}

    public static void main(String[] args) {
        char[] units = "a{;\\\\".toCharArray();
        long[] rows = M3CodeTextSignalBatch.analyze(
                units, new int[] {0, 1}, new int[] {1, 4}, M3CodeTextSignalBatch.Limits.DEFAULT);
        check(rows.length == 2);
        check((rows[1] & M3CodeTextFacts.HAS_BRACE) != 0);
        check((rows[1] & M3CodeTextFacts.HAS_SEMICOLON) != 0);
        check((rows[1] & M3CodeTextFacts.HAS_BACKSLASH) != 0);
        check(((rows[1] >>> 16) & 0xff) == 1);
        check(((rows[1] >>> 24) & 0xff) == 1);
        check(((rows[1] >>> 40) & 0xff) == 1);
        expectIllegalArgument(() -> M3CodeTextSignalBatch.analyze(
                units, new int[] {0}, new int[] {8}, M3CodeTextSignalBatch.Limits.DEFAULT));
        expectIllegalArgument(() -> M3CodeTextSignalBatch.analyze(
                units, new int[] {0}, new int[] {1, 2}, M3CodeTextSignalBatch.Limits.DEFAULT));
        expectIllegalArgument(() -> M3CodeTextSignalBatch.analyze(
                units, new int[] {-1}, new int[] {1}, M3CodeTextSignalBatch.Limits.DEFAULT));
        System.out.println("M3_CODE_TEXT_SIGNAL_BATCH_PASS checks=" + checks);
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
        if (!condition) throw new AssertionError("check " + checks);
    }
}
