// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.sortable;

import com.m3.collections.M3SignalOrder;
import com.m3.collections.M3SignalOrderNative;
import java.util.Arrays;
import java.util.Random;

/**
 * Existing JNI radix owner receives only primitive projected keys and source row IDs.
 * Generated compiler metadata and domain predicates never enter C.
 */
public final class M3CompilerFactsJniProof {
    private M3CompilerFactsJniProof() { }

    public static void main(String[] args) {
        if (!M3SignalOrderNative.available()) {
            throw new AssertionError("existing M3SignalOrder JNI peer not loaded");
        }
        Random random = new Random(0x4d33464143544a4eL);
        int checks = 0;
        for (int run = 0; run < 512; run++) {
            int count = 1 + random.nextInt(255);
            double[] primitiveScores = new double[count];
            for (int row = 0; row < count; row++) {
                int[] features = new int[M3CompilerFacts.COUNT];
                features[M3CompilerFacts.FIELDS] = random.nextInt(512);
                features[M3CompilerFacts.METHODS] = random.nextInt(64);
                var facts = new M3CompilerFacts(features, new long[]{
                        (1L << M3CompilerFacts.FIELDS) | (1L << M3CompilerFacts.METHODS)});
                double[] policy = new double[M3CompilerFacts.COUNT];
                policy[M3CompilerFacts.FIELDS] = 2.0;
                policy[M3CompilerFacts.METHODS] = 0.5;
                primitiveScores[row] = facts.project(policy);
                if (!Double.isFinite(primitiveScores[row])) {
                    throw new AssertionError("source-time typed projection not finite");
                }
            }
            int[] javaRows = M3SignalOrder.ascending(primitiveScores).rows();
            int[] nativeRows = M3SignalOrder.filtered(
                    primitiveScores, new byte[count], 0, 0, true,
                    M3SignalOrder.Backend.NATIVE_REQUIRED).rows();
            if (!Arrays.equals(javaRows, nativeRows)) {
                throw new AssertionError("JNI signal-order mismatch run=" + run);
            }
            checks++;
        }
        System.out.println("PASS M3CompilerFactsJniProof runs=" + checks
                + " existing JNI ABI; no callback/object transferred");
    }
}
