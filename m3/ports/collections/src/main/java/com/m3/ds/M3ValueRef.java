// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.structure.MIndexValueRef}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexValueRef.java}
 * blob {@code 4930482378e1cc7c67d75384cc7f93cafd4183df} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code a69489c7a30025b4a26215155a703ae71a4b820c01d2ffe40983e5e3452fd3f9}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.structure.M3ValueRef}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S15}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code source supertypes not received: implements Comparable<M3ValueRef>}</li>
 *   <li>{@code constant SPACE_BOOLEAN (long)}</li>
 *   <li>{@code constant SPACE_INT8 (long)}</li>
 *   <li>{@code constant SPACE_INT16 (long)}</li>
 *   <li>{@code constant SPACE_CHAR16 (long)}</li>
 *   <li>{@code constant SPACE_INT32 (long)}</li>
 *   <li>{@code constant SPACE_INT64 (long)}</li>
 *   <li>{@code constant SPACE_FLOAT32_RAW (long)}</li>
 *   <li>{@code constant SPACE_FLOAT64_RAW (long)}</li>
 *   <li>{@code member indexedString(long, MatIndexString) (parameter value)}</li>
 *   <li>{@code member mat(long, MatRef<?>) (parameter value)}</li>
 * </ul>
 */
public record M3ValueRef(long spaceId, long valueId) {
    public static M3ValueRef bool(boolean value) {
        throw new M3NotYetReceived("M3ValueRef#bool/1");
    }

    public static M3ValueRef int8(byte value) {
        throw new M3NotYetReceived("M3ValueRef#int8/1");
    }

    public static M3ValueRef int16(short value) {
        throw new M3NotYetReceived("M3ValueRef#int16/1");
    }

    public static M3ValueRef char16(char value) {
        throw new M3NotYetReceived("M3ValueRef#char16/1");
    }

    public static M3ValueRef int32(int value) {
        throw new M3NotYetReceived("M3ValueRef#int32/1");
    }

    public static M3ValueRef int64(long value) {
        throw new M3NotYetReceived("M3ValueRef#int64/1");
    }

    public static M3ValueRef float32(float value) {
        throw new M3NotYetReceived("M3ValueRef#float32/1");
    }

    public static M3ValueRef float64(double value) {
        throw new M3NotYetReceived("M3ValueRef#float64/1");
    }

    public int compareTo(M3ValueRef other) {
        throw new M3NotYetReceived("M3ValueRef#compareTo/1");
    }
}
