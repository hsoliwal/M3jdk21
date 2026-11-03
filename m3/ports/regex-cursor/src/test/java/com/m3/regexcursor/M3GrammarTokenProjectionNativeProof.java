// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * JNI oracle reuse: invokes the EXISTING test-only C17 exact matcher from PR #2312.
 * Reflection exists only in the test harness; product projection is pure Java.
 * No second C engine, exported ABI, production native call or native pointer owner is created.
 */
final class M3GrammarTokenProjectionNativeProof {
    private M3GrammarTokenProjectionNativeProof() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("absolute test JNI library path required");
        System.load(args[0]);
        Method exactNative = M3StaticGrammarLexiconNativeProof.class.getDeclaredMethod(
                "exactNative", M3StaticGrammarLexicon.class, String.class, int.class, int.class);
        exactNative.setAccessible(true); // Reflective access to test-only private JNI entry, not product API.
        List<M3StaticGrammarLexicon.Literal> definitions = M3GrammarTokenProjectionProof.vocabulary();
        M3StaticGrammarLexicon grammar = M3StaticGrammarLexicon.compile("java", "21-spec-token-projection", definitions);
        String[] candidates = {"for", "forbid", "record", "recorded", "non-sealed", "non-sealedx",
                "₹", "😀", ">>", ">>>", "true", "\"for\"", "/*record*/", "a\ud800", "", "<"};
        Random rng = new Random(0xC7A21L);
        int checks = 0;
        for (int batch = 0; batch < 3_000; batch++) {
            int count = rng.nextInt(20);
            String[] words = new String[count];
            byte[] modes = new byte[count];
            for (int i = 0; i < count; i++) {
                words[i] = candidates[rng.nextInt(candidates.length)];
                modes[i] = (byte) (words[i].isEmpty() ? 0 : rng.nextInt(2));
            }
            M3GrammarTokenProjectionProof.Batch source = M3GrammarTokenProjectionProof.batch(words, modes);
            int[] javaLane = M3GrammarTokenProjection.project(grammar, source.source(),
                    source.starts(), source.ends(), source.modes());
            int[] nativeLane = new int[count];
            for (int i = 0; i < count; i++) {
                if (modes[i] == M3GrammarTokenProjection.FIXED_CANDIDATE) {
                    try {
                        int id = (int) exactNative.invoke(null, grammar, source.source(),
                                source.starts()[i], source.ends()[i]);
                        nativeLane[i] = Math.max(0, id);
                    } catch (InvocationTargetException failure) {
                        throw new AssertionError("native token " + i + " batch=" + batch, failure.getCause());
                    }
                }
            }
            if (!Arrays.equals(javaLane, nativeLane)) {
                throw new AssertionError("native projection mismatch at batch " + batch);
            }
            checks += count;
        }
        System.out.println("M3_GRAMMAR_TOKEN_PROJECTION_JNI_PASS token_checks=" + checks);
    }
}
