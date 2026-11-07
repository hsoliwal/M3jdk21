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
 * Immutable primitive-coordinate statement: subject, predicate, object and explicit context.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatStatement.java}
 * blob {@code 0cd52259c9cfa778e69cb113d38db88de804d309} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public record M3MatStatement(long subject, int predicate, long object, long context) {
    public M3MatStatement {
        if (predicate < 0) {
            throw new IllegalArgumentException("predicate must be non-negative");
        }
    }

    public M3MatStatement reverse(int newPredicate) {
        return new M3MatStatement(object, newPredicate, subject, context);
    }

    public long hash64() {
        long hash = 0x4d415453544d5431L;
        hash = mix(hash, subject);
        hash = mix(hash, Integer.toUnsignedLong(predicate));
        hash = mix(hash, object);
        hash = mix(hash, context);
        return avalanche(hash);
    }

    private static long mix(long hash, long value) {
        return Long.rotateLeft(hash ^ value, 27) * 0x9e3779b185ebca87L
                + 0xc2b2ae3d27d4eb4fL;
    }

    private static long avalanche(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        return value ^ (value >>> 33);
    }
}
