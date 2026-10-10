/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.Arrays;

/** Executable proof for the target-side loaded JNI bridge. */
public final class M3CodeTextNativeBridgeNativeTest {
    private static int checks;

    private M3CodeTextNativeBridgeNativeTest() {}

    public static void main(String[] args) {
        String path = System.getProperty("m3.code.text.native.path");
        check(path != null && !path.isBlank());

        System.setProperty("m3.code.text.native.required", "true");
        System.setProperty("m3.code.text.native.verify", "true");

        char[] units = "class A { return x.matches(\"[a-z]+\"); }".toCharArray();
        int[] offsets = {0, 6, 12, units.length};
        int[] lengths = {units.length, 5, 18, 0};
        M3CodeTextSignalBatch.Limits limits = M3CodeTextSignalBatch.Limits.DEFAULT;
        long[] expected = M3CodeTextSignalBatch.analyze(units, offsets, lengths, limits);
        long[] actual = M3CodeTextNativeBridge.analyze(units, offsets, lengths, limits);

        check(M3CodeTextNativeBridge.isLoaded());
        check(Arrays.equals(expected, actual));
        System.out.println("M3_CODE_TEXT_NATIVE_BRIDGE_NATIVE_PASS loaded=true checks=" + checks);
    }

    private static void check(boolean condition) {
        checks++;
        if (!condition) {
            throw new AssertionError("check " + checks);
        }
    }
}
