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
package jdk.m3.collections;

/**
 * Primitive row loader plus parent-specific paged child discovery.
 *
 * <p>Child IDs are written into caller-owned reusable storage and must not be retained.
 *
 * <p>Lineage: absorbed from Synexia {@code synexia-primitives/src/main/java/com/synexia/primitives/PagedPrimitiveTreeProvider.java}
 * blob {@code 383e86bf0b4fd741533137948c7fb47370292a50} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_SYNEXIA_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public interface M3PagedPrimitiveTreeProvider extends M3PrimitiveRowProvider {

    /** Pseudo-parent id used by the default root paging bridge. */
    long ROOT_PARENT_ID = -1L;

    enum ChildHint {
        UNKNOWN,
        NONE,
        SOME
    }

    default ChildHint childHint(long nodeId) throws Exception {
        return ChildHint.UNKNOWN;
    }

    /** Optional root-level hint for forest paging. */
    default ChildHint rootHint() throws Exception {
        return ChildHint.UNKNOWN;
    }

    /**
     * Fill at most {@code limit} root IDs into {@code rootIds}.
     *
     * <p>The default delegates to {@link #loadChildren(long, int, int, long[])} with
     * {@link #ROOT_PARENT_ID}, so a provider may implement one paging path for both roots
     * and ordinary parents.
     */
    default long loadRoots(
            int offset,
            int limit,
            long[] rootIds) throws Exception {
        return loadChildren(ROOT_PARENT_ID, offset, limit, rootIds);
    }

    /**
     * Fill at most {@code limit} child IDs into {@code childIds}.
     *
     * @return packed {@link #pageResult(int, int)}; nextOffset -1 means complete
     */
    long loadChildren(
            long parentId,
            int offset,
            int limit,
            long[] childIds) throws Exception;

    static long pageResult(int count, int nextOffset) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        if (nextOffset < -1) throw new IllegalArgumentException("nextOffset < -1");
        return ((long) nextOffset << 32) | (count & 0xffff_ffffL);
    }

    static int pageCount(long result) {
        return (int) result;
    }

    static int nextOffset(long result) {
        return (int) (result >> 32);
    }
}
