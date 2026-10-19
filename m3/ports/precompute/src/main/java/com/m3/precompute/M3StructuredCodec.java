// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.util.Optional;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.structure.MIndexStructuredCodec}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexStructuredCodec.java}
 * blob {@code c35ff204b5ca383454259e850aef739f65aa23fd} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code e2e1bc391f572166e33dab1a9c4554f94ce57125fbd43154daa245760e41f22a}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.structure.M3StructuredCodec}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S25}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: contract only; implementations keep data in growable primitive
 * lanes and per-slot state bitmasks, never in stored entry or node objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code member format() (return type)}</li>
 *   <li>{@code member decode(byte[], IndexResolver, int, boolean) (return type)}</li>
 *   <li>{@code member encode(MIndexStructure, boolean) (parameter structure)}</li>
 *   <li>{@code member exactRootSource(MIndexStructure) (parameter structure)}</li>
 *   <li>{@code member freezeSource(MIndexStructureBuilder, int, byte[], boolean, MIndexSourceSpan) (return type)}</li>
 * </ul>
 */
public interface M3StructuredCodec {
}
