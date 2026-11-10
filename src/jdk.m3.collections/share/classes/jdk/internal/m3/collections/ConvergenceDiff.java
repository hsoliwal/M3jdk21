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
 * Convergence Diff - delta detection result.
 *
 * <p>
 * Represents the difference between an incoming chunk and the best matching
 * chunk in memory.
 * </p>
 *
 * <h2>SPECIFICATION</h2>
 *
 * <h3>Fields</h3>
 * <ul>
 * <li><b>type</b> - Classification (UNCHANGED/MODIFIED/NEW/VANISHED)</li>
 * <li><b>current</b> - The incoming chunk being processed</li>
 * <li><b>previous</b> - The matched chunk from memory (null for NEW)</li>
 * </ul>
 *
 * @author Hitesh Soliwal (hsoliwal@gmail.com)
 * @param type
 *            the diff type
 * @param current
 *            the incoming chunk
 * @param previous
 *            the matched chunk (null for NEW)
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/convergence/ConvergenceDiff.java}
 * blob {@code 252a1b63d59b45dcccb59ac590ed552f19215a3e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record ConvergenceDiff(DiffType type, ConvergenceChunk current, ConvergenceChunk previous) {

    /**
     * Creates a diff for a new chunk.
     *
     * @param chunk
     *            the new chunk
     * @return NEW diff
     */
    public static ConvergenceDiff newChunk(ConvergenceChunk chunk) {
        return new ConvergenceDiff(DiffType.NEW, chunk, null);
    }

    /**
     * Creates a diff for an unchanged chunk.
     *
     * @param current
     *            the current chunk
     * @param previous
     *            the matched chunk
     * @return UNCHANGED diff
     */
    public static ConvergenceDiff unchanged(ConvergenceChunk current, ConvergenceChunk previous) {
        return new ConvergenceDiff(DiffType.UNCHANGED, current, previous);
    }

    /**
     * Creates a diff for a modified chunk.
     *
     * @param current
     *            the current chunk
     * @param previous
     *            the matched chunk
     * @return MODIFIED diff
     */
    public static ConvergenceDiff modified(ConvergenceChunk current, ConvergenceChunk previous) {
        return new ConvergenceDiff(DiffType.MODIFIED, current, previous);
    }

    /**
     * Checks if this diff represents a new chunk.
     *
     * @return true if type == NEW
     */
    public boolean isNew() {
        return type == DiffType.NEW;
    }

    /**
     * Checks if this diff requires an event.
     *
     * <p>
     * UNCHANGED diffs don't require events (survival reinforcement).
     * </p>
     *
     * @return true if event should be generated
     */
    public boolean requiresEvent() {
        return type != DiffType.UNCHANGED;
    }
}
