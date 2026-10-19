// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.structure.MIndexRepresentationMetadata}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexRepresentationMetadata.java}
 * blob {@code 85d8bbb9f1db7cf27a8de290456c1fdfa830b231} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 764febf3aa45a92d1572a378bb57203c20a1d5829b50a1d44ec1504654243261}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.structure.M3RepresentationMetadata}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S14}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code record component span erased to Object (source type MIndexSourceSpan)}</li>
 *   <li>{@code record component fieldOrder erased to Object (source type MIndexString)}</li>
 *   <li>{@code record component quotingStyle erased to Object (source type MIndexString)}</li>
 *   <li>{@code record component numericSpelling erased to Object (source type MIndexString)}</li>
 *   <li>{@code record component tag erased to Object (source type MIndexString)}</li>
 *   <li>{@code record component trivia erased to Object (source type MIndexString)}</li>
 *   <li>{@code member source(MIndexSourceFormat, MIndexSourceSpan) (parameter span)}</li>
 * </ul>
 */
public record M3RepresentationMetadata(M3SourceFormat format, Object span, int occurrenceOrdinal, Object fieldOrder, Object quotingStyle, Object numericSpelling, Object tag, Object trivia, byte[] rawSource) {
    public byte[] rawSource() {
        throw new M3NotYetReceived("M3RepresentationMetadata#rawSource/0");
    }

    public M3RepresentationMetadata withRawSource(byte[] source) {
        throw new M3NotYetReceived("M3RepresentationMetadata#withRawSource/1");
    }
}
