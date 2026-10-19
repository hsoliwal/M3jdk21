// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;
import java.util.Optional;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.type.MIndexClassMetadataIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/type/MIndexClassMetadataIndex.java}
 * blob {@code 217f2060cc3951e583907eb40e45671d7df5f0da} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code f91b02ad547b8a041cc9bd44dbf5d38859e466a1226aa4581a3e07a0cb85d25d}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.type.M3ClassMetadataIndex}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S13}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code member classSignature(int) (return type)}</li>
 *   <li>{@code member memberSignature(int) (return type)}</li>
 *   <li>{@code member memberAnnotationDefault(int) (return type)}</li>
 *   <li>{@code member memberThrowsAt(int, int) (return type)}</li>
 *   <li>{@code member permittedSubclassAt(int, int) (return type)}</li>
 *   <li>{@code member annotation(int) (return type)}</li>
 *   <li>{@code member annotationsOnClass(int) (return type)}</li>
 *   <li>{@code member annotationsOnMember(int) (return type)}</li>
 *   <li>{@code member annotationsByDescriptor(CharSequence) (return type)}</li>
 * </ul>
 */
public final class M3ClassMetadataIndex {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code classes} of type {@code MIndexClassPool}. */
    private int[] classesLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code signatures} of type {@code MIndexGenericSignaturePool}. */
    private int[] signaturesLane;

    /** primitive lane for source field {@code classSignatureHandles} of type {@code int[]}. */
    private int[] classSignatureHandles;

    /** primitive lane for source field {@code memberSignatureHandles} of type {@code int[]}. */
    private int[] memberSignatureHandles;

    /** primitive lane for source field {@code memberAnnotationDefaultHandles} of type {@code int[]}. */
    private int[] memberAnnotationDefaultHandles;

    /** primitive lane for source field {@code memberThrowsStarts} of type {@code int[]}. */
    private int[] memberThrowsStarts;

    /** primitive lane for source field {@code memberThrowsCounts} of type {@code int[]}. */
    private int[] memberThrowsCounts;

    /** primitive lane for source field {@code memberThrowsArena} of type {@code int[]}. */
    private int[] memberThrowsArena;

    /** primitive lane for source field {@code permittedStarts} of type {@code int[]}. */
    private int[] permittedStarts;

    /** primitive lane for source field {@code permittedCounts} of type {@code int[]}. */
    private int[] permittedCounts;

    /** primitive lane for source field {@code permittedArena} of type {@code int[]}. */
    private int[] permittedArena;

    /** primitive lane for source field {@code annotationOwners} of type {@code int[]}. */
    private int[] annotationOwners;

    /** primitive lane for source field {@code annotationMembers} of type {@code int[]}. */
    private int[] annotationMembers;

    /** primitive lane for source field {@code annotationDescriptorHandles} of type {@code int[]}. */
    private int[] annotationDescriptorHandles;

    /** primitive lane for source field {@code annotationValueHandles} of type {@code int[]}. */
    private int[] annotationValueHandles;

    /** primitive lane for source field {@code annotationVisibility} of type {@code byte[]}. */
    private byte[] annotationVisibility;

    /** primitive lane for source field {@code annotationTargetTypes} of type {@code short[]}. */
    private short[] annotationTargetTypes;

    /** primitive lane for source field {@code targetInfoStarts} of type {@code int[]}. */
    private int[] targetInfoStarts;

    /** primitive lane for source field {@code targetInfoCounts} of type {@code int[]}. */
    private int[] targetInfoCounts;

    /** primitive lane for source field {@code typePathStarts} of type {@code int[]}. */
    private int[] typePathStarts;

    /** primitive lane for source field {@code typePathCounts} of type {@code int[]}. */
    private int[] typePathCounts;

    /** primitive lane for source field {@code annotationExactHashes} of type {@code long[]}. */
    private long[] annotationExactHashes;

    /** primitive lane for source field {@code targetInfoArena} of type {@code int[]}. */
    private int[] targetInfoArena;

    /** primitive lane for source field {@code typePathArena} of type {@code int[]}. */
    private int[] typePathArena;

    /** primitive lane for source field {@code descriptorHashes} of type {@code long[]}. */
    private long[] descriptorHashes;

    /** primitive lane for source field {@code descriptorSlots} of type {@code int[]}. */
    private int[] descriptorSlots;

    /** primitive lane for source field {@code nextByDescriptor} of type {@code int[]}. */
    private int[] nextByDescriptor;

    private M3ClassMetadataIndex() {
        throw new M3NotYetReceived("M3ClassMetadataIndex#<init>/0");
    }

    public M3ClassPool classes() {
        throw new M3NotYetReceived("M3ClassMetadataIndex#classes/0");
    }

    public M3GenericSignaturePool signatures() {
        throw new M3NotYetReceived("M3ClassMetadataIndex#signatures/0");
    }

    public int annotationCount() {
        throw new M3NotYetReceived("M3ClassMetadataIndex#annotationCount/0");
    }

    public int memberThrowsCount(int memberIndex) {
        throw new M3NotYetReceived("M3ClassMetadataIndex#memberThrowsCount/1");
    }

    public int permittedSubclassCount(int classHandle) {
        throw new M3NotYetReceived("M3ClassMetadataIndex#permittedSubclassCount/1");
    }

    public long primitivePayloadBytes() {
        throw new M3NotYetReceived("M3ClassMetadataIndex#primitivePayloadBytes/0");
    }
}
