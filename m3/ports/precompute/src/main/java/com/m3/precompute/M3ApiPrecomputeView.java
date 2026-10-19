// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.precompute.MIndexApiPrecomputeView}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexApiPrecomputeView.java}
 * blob {@code 744bca405912889fd99b297f00bd4c101608a1e3} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code ba6794e5808ea2ffc4a830c7fe435149b0727a8ec2b43afaca113a548c685c35}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.precompute.M3ApiPrecomputeView}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S20}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: contract only; implementations keep data in growable primitive
 * lanes and per-slot state bitmasks, never in stored entry or node objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code nested types not received: ResultKind}</li>
 *   <li>{@code member kind(int) (return type)}</li>
 * </ul>
 */
public interface M3ApiPrecomputeView {
    byte FLAG_HAS_MORE = 1;

    int rowCount();

    int maxArgumentCount();

    long contentFingerprint();

    int find(long apiId, long[] arguments, int offset, int length);

    byte flags(int row);

    long scalar(int row);

    long auxiliary(int row);

    int payloadLength(int row);

    long payloadAt(int row, int index);

    long retainedPrimitiveBytes();

    default boolean booleanResult(int row) {
        throw new M3NotYetReceived("M3ApiPrecomputeView#booleanResult/1");
    }

    default long longResult(int row) {
        throw new M3NotYetReceived("M3ApiPrecomputeView#longResult/1");
    }

    default int bitmapBitCount(int row) {
        throw new M3NotYetReceived("M3ApiPrecomputeView#bitmapBitCount/1");
    }

    default boolean bitmapContains(int row, int bitIndex) {
        throw new M3NotYetReceived("M3ApiPrecomputeView#bitmapContains/2");
    }

    default boolean pageHasMore(int row) {
        throw new M3NotYetReceived("M3ApiPrecomputeView#pageHasMore/1");
    }

    default long pageNextCursor(int row) {
        throw new M3NotYetReceived("M3ApiPrecomputeView#pageNextCursor/1");
    }

    default long pageTotalCount(int row) {
        throw new M3NotYetReceived("M3ApiPrecomputeView#pageTotalCount/1");
    }

    default int copyPayload(int row, long[] target, int targetOffset) {
        throw new M3NotYetReceived("M3ApiPrecomputeView#copyPayload/3");
    }
}
