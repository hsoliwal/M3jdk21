// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

/**
 * Bridges the module's native library property to the relocated bit matrix owner and its oracles. {@link M3BitMatrix}
 * reads {@code m3.matmatrix.native.path} once, when the class initializes, and loads that library explicitly (no
 * fallback, no library search); the oracle {@link M3BitMatrixContract} reads {@code m3.bitmatrix.requireNative}. The
 * module's {@code native-linux} profile sets {@code m3.arrays.native.path} to {@code libm3_arrays.so}, which carries
 * the {@code Java_jdk_m3_arrays_M3BitMatrix_*} kernel since recipe bitmatrix-20261106 registered
 * {@code m3_bit_matrix.c}. The bridge must run before anything touches {@code M3BitMatrix}; without either property
 * the oracles run their Java lanes and the native-required tile oracle is skipped.
 */
//FLLDCIM: TFG - test-scope glue from the module template M3IntPostingNativePath (recipe posting-index-20261106); recipe m3/arrays/recipe/bitmatrix-20261106
final class BitMatrixNativePath {
    static final String OWNER_PATH = "m3.matmatrix.native.path";
    static final String ORACLE_REQUIRED = "m3.bitmatrix.requireNative";
    static final String MODULE_PATH = "m3.arrays.native.path";

    private BitMatrixNativePath() {}

    /** Copies the module property into the owner and oracle properties once; true when an owner library path is set. */
    static synchronized boolean bridge() {
        String owner = System.getProperty(OWNER_PATH);
        if (owner == null || owner.isBlank()) {
            String module = System.getProperty(MODULE_PATH);
            if (module == null || module.isBlank()) return false;
            System.setProperty(OWNER_PATH, module);
            System.setProperty(ORACLE_REQUIRED, "true");
        }
        return true;
    }
}
