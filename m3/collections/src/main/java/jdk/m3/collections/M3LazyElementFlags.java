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
 * One 16-bit provider/lazy lifecycle word per touched element.
 *
 * <p>Lineage: absorbed from Synexia {@code synexia-primitives/src/main/java/com/synexia/primitives/LazyElementFlags.java}
 * blob {@code 59205c31449431e806b172ae377dc2cf0ad3a236} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_SYNEXIA_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3LazyElementFlags {
    public static final short DISCOVERED = 1 << 0;
    public static final short LOADING = 1 << 1;
    public static final short MATERIALIZED = 1 << 2;
    public static final short FAILED = 1 << 3;
    public static final short DIRTY = 1 << 4;
    public static final short PINNED = 1 << 5;
    public static final short INVALIDATED = 1 << 6;
    public static final short CHILDREN_KNOWN = 1 << 7;
    public static final short HAS_CHILDREN = 1 << 8;
    public static final short CHILDREN_LOADING = 1 << 9;
    public static final short CHILDREN_PARTIAL = 1 << 10;
    public static final short CHILDREN_COMPLETE = 1 << 11;

    public static final short APPLICATION_0 = 1 << 12;
    public static final short APPLICATION_1 = 1 << 13;
    public static final short APPLICATION_2 = 1 << 14;
    public static final short APPLICATION_3 = (short) (1 << 15);
    public static final short APPLICATION_MASK =
            (short) (APPLICATION_0 | APPLICATION_1 | APPLICATION_2 | APPLICATION_3);

    static final short CHILD_STATE_MASK =
            (short) (CHILDREN_KNOWN | HAS_CHILDREN | CHILDREN_LOADING
                    | CHILDREN_PARTIAL | CHILDREN_COMPLETE);
    static final short LOAD_PRESERVE_MASK =
            (short) (DIRTY | PINNED | CHILD_STATE_MASK | APPLICATION_MASK);

    private M3LazyElementFlags() {}

    public static boolean has(short flags, short bit) {
        return (flags & bit) != 0;
    }

    public static short add(short flags, short bits) {
        return (short) (flags | bits);
    }

    public static short remove(short flags, short bits) {
        return (short) (flags & ~bits);
    }
}
