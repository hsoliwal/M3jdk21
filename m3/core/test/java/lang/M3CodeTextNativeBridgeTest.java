/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.Arrays;

/** Executable host-Java fallback and geometry proof for the optional code-text bridge. */
public final class M3CodeTextNativeBridgeTest {
    private static int checks;

    private M3CodeTextNativeBridgeTest() {}

    public static void main(String[] args) {
        System.clearProperty("m3.code.text.native.path");
        System.clearProperty("m3.code.text.native.required");
        System.setProperty("m3.code.text.native.verify", "true");

        char[] units = "class A { return x.matches(\"[a-z]+\"); }".toCharArray();
        int[] offsets = {0, 6, 12, units.length};
        int[] lengths = {units.length, 5, 18, 0};
        M3CodeTextSignalBatch.Limits limits = M3CodeTextSignalBatch.Limits.DEFAULT;

        System.setProperty("m3.code.text.native.required", "true");
        expectUnsatisfiedLink(() -> M3CodeTextNativeBridge.analyze(
                units, offsets, lengths, limits));
        System.clearProperty("m3.code.text.native.required");

        long[] expected = M3CodeTextSignalBatch.analyze(units, offsets, lengths, limits);
        long[] actual = M3CodeTextNativeBridge.analyze(units, offsets, lengths, limits);
        check(Arrays.equals(expected, actual));
        check(!M3CodeTextNativeBridge.isLoaded());

        expectIllegalArgument(() -> M3CodeTextNativeBridge.analyze(
                units, new int[] {0}, new int[] {}, limits));
        expectIllegalArgument(() -> M3CodeTextNativeBridge.analyze(
                units, new int[] {-1}, new int[] {1}, limits));
        expectIllegalArgument(() -> M3CodeTextNativeBridge.analyze(
                units, new int[] {units.length}, new int[] {1}, limits));

        M3CodeTextSignalBatch.Limits tiny = new M3CodeTextSignalBatch.Limits(1, 1, 1);
        expectIllegalArgument(() -> M3CodeTextNativeBridge.analyze(
                units, offsets, lengths, tiny));

        System.out.println("M3_CODE_TEXT_NATIVE_BRIDGE_FALLBACK_PASS checks=" + checks);
    }

    private static void expectUnsatisfiedLink(Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("expected UnsatisfiedLinkError");
        } catch (UnsatisfiedLinkError expected) {
            checks++;
        }
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
