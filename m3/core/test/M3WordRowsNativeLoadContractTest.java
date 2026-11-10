/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.M3WordRowsNative;

import java.nio.file.Path;

/**
 * Explicit JNI opt-in contract of the word-row bulk reader: nothing is loaded implicitly, a null
 * library is refused, and once the verified library is bound further explicit loads are no-ops that
 * never rebind another file. The bulk parity itself is proved by M3WordRowsContractTest with the
 * same library.
 *
 * <p>Usage: {@code <m3_wordrows_jni library>}.
 */
public final class M3WordRowsNativeLoadContractTest {
    private static long checks;

    private M3WordRowsNativeLoadContractTest() {}

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " check=" + checks);
    }

    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("m3_wordrows_jni library");
        Path library = Path.of(args[0]);
        check(!M3WordRowsNative.available(), "JNI is never an implicit runtime dependency");
        try {
            M3WordRowsNative.load(null);
            throw new IllegalStateException("null library accepted");
        } catch (NullPointerException expected) {
            check(
                    "library".equals(expected.getMessage()),
                    "null library refusal names the argument");
        }
        check(!M3WordRowsNative.available(), "a refused load binds nothing");
        M3WordRowsNative.load(library);
        check(M3WordRowsNative.available(), "explicit load binds the verified library");
        M3WordRowsNative.load(library);
        M3WordRowsNative.load(library.resolveSibling("never-loaded-" + library.getFileName()));
        check(M3WordRowsNative.available(), "repeated explicit loads are no-ops once bound");
        System.out.println("M3_WORDROWS_NATIVE_LOAD_PASS checks=" + checks);
    }
}
