// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import java.nio.ByteBuffer;

/** Optional checked-JNI witness of read-only direct-buffer byte geometry. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-class-space-canonical-images-20261030/src/main/java/com/synexia/m3/classspace/M3ClassSpaceArchiveNative.java blob 93e8667d99a9; recipe m3/intern/recipe/class-space-20261107/relocate.py v1
final class ClassSpaceArchiveNative {
    private ClassSpaceArchiveNative() {
    }

    static native long fnv64Region(ByteBuffer direct, int offset, int length);
}
