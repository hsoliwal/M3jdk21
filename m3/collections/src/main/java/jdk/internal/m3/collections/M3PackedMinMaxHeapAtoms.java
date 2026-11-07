/*******************************************************************************
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
 * Min-max-specific geometry; ordinary parent/child geometry stays in M3PackedHeapAtoms.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedMinMaxHeapAtoms.java}
 * blob {@code 888f76b7abcc18fc5dd6333ef15600977d620f71} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
final class M3PackedMinMaxHeapAtoms {
    private M3PackedMinMaxHeapAtoms() { }

    /** Requires a nonnegative valid heap index; root depth zero is a minimum level. */
    static boolean minLevel(int index) {
        return ((31 - Integer.numberOfLeadingZeros(index + 1)) & 1) == 0;
    }

    /** Requires index >= 3. */
    static int grandparent(int index) { return (index - 3) >>> 2; }
}
