// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

/**
 * Bridges the module's native library property to the relocated posting oracles. The oracles keep their donor
 * property names ({@code matposting.native.path}, {@code matposting.requireNative}) as string literals; the module's
 * {@code native-linux} profile sets {@code m3.arrays.native.path} to {@code libm3_arrays.so}, which carries the
 * {@code Java_jdk_m3_arrays_M3IntPostingIndex_*} kernels since recipe posting-index-20261106 registered
 * {@code m3_posting_merge.c}. Without either property the oracles run their JAVA_ONLY lanes.
 */
//FLLDCIM: TFG - test-scope glue of the module template (M3*Test reads m3.arrays.native.path); recipe m3/arrays/recipe/posting-index-20261106
final class M3IntPostingNativePath {
    static final String ORACLE_PATH = "matposting.native.path";
    static final String ORACLE_REQUIRED = "matposting.requireNative";
    static final String MODULE_PATH = "m3.arrays.native.path";

    private M3IntPostingNativePath() {}

    /** Copies the module property into the oracle properties once; true when an oracle library path is set. */
    static synchronized boolean bridge() {
        String oracle = System.getProperty(ORACLE_PATH);
        if (oracle == null || oracle.isBlank()) {
            String module = System.getProperty(MODULE_PATH);
            if (module == null || module.isBlank()) return false;
            System.setProperty(ORACLE_PATH, module);
            System.setProperty(ORACLE_REQUIRED, "true");
        }
        return true;
    }
}
