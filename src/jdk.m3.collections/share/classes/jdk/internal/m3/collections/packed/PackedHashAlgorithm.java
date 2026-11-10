/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.packed;

/**
 * Primitive hash-table mechanics available without per-entry objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedHashAlgorithm.java}
 * blob {@code de488705ca9374d7e3f9b1a03f589f39422ecc89} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum PackedHashAlgorithm {
    /**
     * Sentinel-empty linear probing with backward-shift deletion.
     * Lowest retained bytes: no state byte per slot; key zero is stored separately.
     */
    COMPACT_LINEAR,

    /**
     * Existing Synexia linear probing with one byte of EMPTY/OCCUPIED/DELETED state.
     * Accepts all long keys directly and favors straightforward mutation semantics.
     */
    STATEFUL_LINEAR,

    /**
     * Triangular/quadratic probing with one byte state per slot.
     * Reduces primary clustering relative to linear probing.
     */
    QUADRATIC,

    /**
     * Robin-Hood probing with early-exit lookup and backward-shift deletion.
     * One occupancy byte per slot, no tombstones.
     */
    ROBIN_HOOD,

    /**
     * Swiss-table-style grouped probing with a 7-bit fingerprint/control byte.
     * Eight primitive slots are screened per group before full-key comparison.
     */
    SWISS_CONTROL
}
