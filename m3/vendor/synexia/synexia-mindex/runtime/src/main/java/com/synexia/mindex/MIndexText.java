// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

/**
 * Common indexed-text surface.
 *
 * <p>{@link MIndexString} is the canonical single-pool-entry representation. Composite text keeps
 * several canonical entries plus metadata lanes without materializing their concatenation.</p>
 */
public sealed interface MIndexText extends CharSequence
        permits MIndexString, MIndexAnnotatedString, MIndexCompositeString {

    /** Canonical whole-string materialization; callers should defer this at hot boundaries. */
    MIndexString materialize();

    /** Number of canonical MIndexString segments backing this value. */
    int segmentCount();

    /** Canonical segment at the supplied ordinal. */
    MIndexString segment(int index);

    /**
     * Immutable metadata attached to one canonical segment.
     *
     * <p>Plain {@link MIndexString} values have no segment-side annotation. Annotated/composite
     * implementations override this without materializing the whole text.</p>
     */
    default MIndexStringMetadata segmentMetadata(int index) {
        java.util.Objects.checkIndex(index, segmentCount());
        return MIndexStringMetadata.NONE;
    }
}
