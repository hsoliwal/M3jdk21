// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.intern;

/**
 * A deeply immutable composite that can intern itself into a shared InternShapes space;
 * equal values return equal handles in the same space; the interface is internal and not exported.
 */
public interface Internable {
    /**
     * Interns this object into a shared InternShapes space.
     * @param shapes The InternShapes space to intern into.
     * @param loaderScope Resolution scope of record and enum types met inside this value.
     * @return The interned handle.
     */
    long internInto(InternShapes shapes, long loaderScope);
}
