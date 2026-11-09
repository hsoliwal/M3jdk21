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
package jdk.internal.m3.collections.shape;

/**
 * Version-1 fuzzy ratings in 0..1000, explicitly policy judgments rather than benchmark data.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/CandidateProfile.java}
 * blob {@code de62a6708eab4c9cfc14731b62e27085eb9938a2} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public record CandidateProfile(SizeBand sizeBand, int lookup, int mutation,
                               int traversal, int memory, int growth) {
    /** Cardinality membership curves evaluated from the expected size. */
    public enum SizeBand { SMALL, GENERAL, LARGE }
    public CandidateProfile {
        if (sizeBand == null) { throw new NullPointerException("sizeBand"); }
        new CollectionPreferences(0, lookup, mutation, traversal, memory, growth);
    }
    long score(CollectionPreferences p) {
        long n = p.expectedSize();
        long membership = switch (sizeBand) {
            case SMALL -> Math.max(0, 1000 - n * 1000 / 64);
            case GENERAL -> Math.min(1000, 250 + n * 750 / 64);
            case LARGE -> Math.min(1000, n * 1000 / 4096);
        };
        return membership * 2000 + (long) lookup * p.lookup() + (long) mutation * p.mutation()
                + (long) traversal * p.traversal() + (long) memory * p.memory()
                + (long) growth * p.growth();
    }
}
