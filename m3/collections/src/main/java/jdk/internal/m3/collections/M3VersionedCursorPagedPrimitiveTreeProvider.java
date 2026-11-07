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
 * Primitive row provider whose child pages are addressable by both an opaque cursor and a stable
 * parent-specific version token.
 *
 * <p>The caller pins {@link #childVersion(long)} once per cached parent branch and passes that
 * exact token to every subsequent page request. This prevents one branch cache from silently
 * mixing pages from different mutable-backend versions.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/VersionedCursorPagedPrimitiveTreeProvider.java}
 * blob {@code 350b75da82fa80c82931fba031393a87eca46f5e} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public interface M3VersionedCursorPagedPrimitiveTreeProvider extends M3PrimitiveRowProvider {

    long END_CURSOR = M3CursorPagedPrimitiveTreeProvider.END_CURSOR;

    default M3PagedPrimitiveTreeProvider.ChildHint childHint(long nodeId) throws Exception {
        return M3PagedPrimitiveTreeProvider.ChildHint.UNKNOWN;
    }

    /** Stable version token for the next parent-branch cache lifetime. */
    long childVersion(long parentId) throws Exception;

    /**
     * Load children from exactly {@code versionToken}.
     *
     * <p>The provider must overwrite {@code nextCursorOut[0]}. END_CURSOR means complete.
     */
    int loadChildren(
            long parentId,
            long versionToken,
            long cursor,
            int limit,
            long[] childIds,
            long[] nextCursorOut) throws Exception;
}
