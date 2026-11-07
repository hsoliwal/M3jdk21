// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

/**
 * Optional, exact JNI byte-comparison proof lane. Callers explicitly load the native library;
 * Java-only class-image interning has no native dependency or hidden fallback.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-class-space-canonical-images-20261030/src/main/java/com/synexia/m3/classspace/M3ClassSpaceNative.java blob 1023caaed189; recipe m3/intern/recipe/class-space-20261107/relocate.py v1
public final class M3ClassSpaceNative {
    private M3ClassSpaceNative() {
    }

    /** Semantics match {@link java.util.Arrays#equals(byte[], byte[])}. */
    public static native boolean equalBytes(byte[] left, byte[] right);
}
