// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Reuses the existing M3 UTF-16 C11/JNI signal ABI: no second JNI library or raw pointer.
 * Native selection is explicitly opt-in; its results must equal the Java reference.
 */
final class M3CpuToolNativeSignals {
    private static final String NATIVE_PROPERTY = "m3.cpu.tools.native.signals";

    private M3CpuToolNativeSignals() {}

    static byte[] pack(byte[] utf8) {
        char[] characters = decode(utf8);
        int[] offsets = {0};
        int[] lengths = {characters.length};
        M3CodeTextSignalBatch.Limits limits = M3CodeTextSignalBatch.Limits.DEFAULT;
        long[] javaFacts = M3CodeTextSignalBatchJava.INSTANCE.analyze(
                characters, offsets, lengths, limits, M3Progress.none());
        if (Boolean.getBoolean(NATIVE_PROPERTY)) {
            long[] nativeFacts = M3CodeTextSignalBatchNative.loadRequired().analyze(
                    characters, offsets, lengths, limits, M3Progress.none());
            if (!Arrays.equals(javaFacts, nativeFacts)) {
                throw new IllegalStateException("M3 JNI signals disagree with Java oracle");
            }
        }
        return ByteBuffer.allocate(Long.BYTES).putLong(javaFacts[0]).array();
    }

    private static char[] decode(byte[] utf8) {
        try {
            var chars = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(utf8));
            char[] copy = new char[chars.remaining()];
            chars.get(copy);
            return copy;
        } catch (CharacterCodingException invalid) {
            throw new IllegalArgumentException("invalid input UTF-8", invalid);
        }
    }
}
