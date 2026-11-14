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
package jdk.internal.m3.collections.mat;

/**
 * Mechanical relation contracts used by the bounded logic kernel.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatPredicateFlags.java}
 * blob {@code 77bb7c26894601f45ee955952950b21a7fad6935} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatPredicateFlags {
    public static final int SYMMETRIC = 1 << 0;
    public static final int TRANSITIVE = 1 << 1;
    public static final int REFLEXIVE = 1 << 2;

    public static final int KNOWN_MASK = SYMMETRIC | TRANSITIVE | REFLEXIVE;

    private MatPredicateFlags() { }

    public static int requireKnown(int flags) {
        if ((flags & ~KNOWN_MASK) != 0) {
            throw new IllegalArgumentException("Unknown predicate flag bits");
        }
        return flags;
    }
}
