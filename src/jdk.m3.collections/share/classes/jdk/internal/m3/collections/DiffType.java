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
package jdk.internal.m3.collections;

/**
 * Diff Type - delta classification for convergence.
 *
 * <h2>SPECIFICATION</h2>
 *
 * <h3>Classification Rules</h3>
 *
 * <pre>{@code
 * UNCHANGED  → similarity > 0.85, no event generated
 * MODIFIED   → similarity ∈ (0, 0.85], generates ConsolidateEvent
 * NEW        → no match found, generates AddEvent
 * VANISHED   → in memory but not seen this pass, may generate DropEvent
 * }</pre>
 *
 * @author Hitesh Soliwal (hsoliwal@gmail.com)
 * @see ConvergenceMatcher
 * @see ConvergenceDiff
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/convergence/DiffType.java}
 * blob {@code b484194a9c28a1aac9bf159e3e37fabbea88a5af} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum DiffType {

    /**
     * Chunk unchanged from memory (similarity > 0.85).
     *
     * <p>
     * Reinforced by survival - no event needed.
     * </p>
     */
    UNCHANGED,

    /**
     * Chunk modified from memory (0 < similarity <= 0.85).
     *
     * <p>
     * Generates ConsolidateEvent to update chunk.
     * </p>
     */
    MODIFIED,

    /**
     * New chunk not in memory (no match found).
     *
     * <p>
     * Generates AddEvent to insert chunk.
     * </p>
     */
    NEW,

    /**
     * Chunk in memory but not seen this pass.
     *
     * <p>
     * Subject to decay. May generate DropEvent if below threshold.
     * </p>
     */
    VANISHED
}
