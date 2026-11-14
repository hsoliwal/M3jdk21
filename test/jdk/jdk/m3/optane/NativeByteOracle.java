// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;
/** Independent C17 JNI differential oracle, not a production PMEMSDK export. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/synexia-openrewrite-recipes/recipe-crates/m3-pmem-gram-index-20261102/proof/stubs/com/synexia/m3/pmem/NativeByteOracle.java blob 556bf41251e0; recipe m3/optane/recipe/pmem-gram-index-20261106/relocate.py v3
final class NativeByteOracle {
    static { System.loadLibrary("m3gramoracle"); }
    private NativeByteOracle() {}
    static native long firstHit(byte[] haystack, byte[] pattern);
}
