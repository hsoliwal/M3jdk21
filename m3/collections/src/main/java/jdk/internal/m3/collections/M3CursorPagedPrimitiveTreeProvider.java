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
 * Primitive row provider with parent-local opaque 64-bit child cursors.
 *
 * <p>No page object is returned. Child ids and the next cursor are written into caller-owned
 * reusable primitive buffers.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/CursorPagedPrimitiveTreeProvider.java}
 * blob {@code 58c574f6b5bb70d5e8071d683a01302aa9bdd3bb} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public interface M3CursorPagedPrimitiveTreeProvider extends M3PrimitiveRowProvider {

    /** Reserved next-cursor value meaning that the branch is complete. */
    long END_CURSOR = Long.MIN_VALUE;

    default M3PagedPrimitiveTreeProvider.ChildHint childHint(long nodeId) throws Exception {
        return M3PagedPrimitiveTreeProvider.ChildHint.UNKNOWN;
    }

    /**
     * Fill at most {@code limit} child ids into {@code childIds}.
     *
     * <p>The provider must overwrite {@code nextCursorOut[0]}. A value of
     * {@link #END_CURSOR} means complete. Otherwise the next cursor must differ from the supplied
     * cursor so repeated loading cannot loop forever.
     *
     * @return number of child ids written
     */
    int loadChildren(
            long parentId,
            long cursor,
            int limit,
            long[] childIds,
            long[] nextCursorOut) throws Exception;
}
