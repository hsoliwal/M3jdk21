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
package jdk.internal.m3.collections.mat;

/**
 * Stable schema-level capability bits shared with the source-time MAT processor.
 * Bits express declared eligibility, not proof of safe allocation, distance or eviction.
 * None is stored per collection entry.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatCapabilityFlags.java}
 * blob {@code 93e722dcab087c147850cfc809bc2d21f3d45bae} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatCapabilityFlags {
    public static final int SCHEMA_VERSION = 1;
    public static final long SORTABLE = 1L << 0;
    public static final long CACHEABLE = 1L << 1;
    public static final long GC_COUNTABLE = 1L << 2;
    public static final long INDEXABLE = 1L << 3;
    public static final long DISTANCE_AWARE = 1L << 4;
    public static final long POOLABLE = 1L << 5;
    public static final long INTERNABLE = 1L << 6;
    public static final long LAZY = 1L << 7;
    public static final long DEFERRED = 1L << 8;
    public static final long SPILLABLE = 1L << 9;
    public static final long RECREATABLE = 1L << 10;
    public static final long COMPRESSIBLE = 1L << 11;
    public static final long PREFETCHABLE = 1L << 12;
    public static final long OBSERVABLE = 1L << 13;
    public static final long VIEWPORT_AWARE = 1L << 14;
    // Extension IDs are permanent. Do not renumber published capabilities.
    public static final long LISTABLE = 1L << 15;
    public static final long COLLECTABLE = 1L << 16;
    public static final long MAPPABLE = 1L << 17;
    public static final long COMPARABLE_CAPABILITY = 1L << 18;
    public static final long REUSABLE = 1L << 19;
    public static final long DURABLE = 1L << 20;
    public static final long ALL = (1L << 21) - 1L;

    private MatCapabilityFlags() {}

    public static boolean has(long declaredMask, long flag) {
        return (declaredMask & flag) == flag && flag != 0L;
    }

    /** Only positive, recognized masks may enter runtime planning. */
    public static long checked(long declaredMask) {
        if ((declaredMask & ~ALL) != 0L) {
            throw new IllegalArgumentException("unknown capability bits");
        }
        return declaredMask;
    }
}
