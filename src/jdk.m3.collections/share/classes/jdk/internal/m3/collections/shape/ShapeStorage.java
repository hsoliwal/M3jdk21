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
package jdk.internal.m3.collections.shape;

/**
 * Structural counts, not measured retained heap bytes. Diagnostics may allocate.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/ShapeStorage.java}
 * blob {@code c3df3d46600edf22104450604ce4474b0a97de6d} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public record ShapeStorage(long referenceSlots, long primitiveBytes, int leafBlocks,
                           int indexSlots) {
    public ShapeStorage {
        if (referenceSlots < 0 || primitiveBytes < 0 || leafBlocks < 0 || indexSlots < 0) {
            throw new IllegalArgumentException("negative storage count");
        }
    }
}
