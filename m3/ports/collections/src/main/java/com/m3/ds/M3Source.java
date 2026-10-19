// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexSource}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexSource.java}
 * blob {@code c7d60feb61461cfe1a101fcb432302135bc87e90} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code b8fd79aed296423be1572735861cfeda628035cfe3e35fe61e35780cf2f108e3}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3Source}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S10}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: contract only; implementations keep data in growable primitive
 * lanes and per-slot state bitmasks, never in stored entry or node objects.
 *
 * <p>Co-donors with the same proposed name (BLOB_DIFF, not merged, review required):
 * <ul>
 *   <li>{@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexSource.java(at)2591aff7aae9}</li>
 * </ul>
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code source supertypes not received: extends AutoCloseable}</li>
 * </ul>
 */
public interface M3Source {
    long length();

    byte byteAt(long offset) throws IOException;

    void read(long offset, byte[] target, int targetOffset, int length) throws IOException;

    ByteBuffer slice(long offset, int length) throws IOException;

    default ByteBuffer chunk(long offset, int maxLength) throws IOException {
        throw new M3NotYetReceived("M3Source#chunk/2");
    }

    String sha256() throws IOException;

    void verifyUnchanged() throws IOException;

    void close() throws IOException;
}
