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

import java.util.Objects;

/**
 * Structured workload facts consumed by the deterministic MAT collection factory.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatCollectionRequirements.java}
 * blob {@code 14572752e4f3da6bbdce8c1655d4308e62d45547} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public record MatCollectionRequirements(
        Shape shape,
        AccessPattern accessPattern,
        MutationModel mutationModel,
        MatCollectionPolicy policy,
        boolean rankSelect,
        long expectedSize,
        long expectedDomainSpan) {

    public enum Shape { VECTOR, SET, MAP, POSTING_INDEX, RANGE_SET, RANGE_MAP, GRAPH }
    public enum AccessPattern { ITERATION_HEAVY, LOOKUP_HEAVY, RANGE_HEAVY, MIXED }
    public enum MutationModel { BUILD_THEN_FREEZE, MUTABLE_BOUNDARY }

    public MatCollectionRequirements {
        shape = Objects.requireNonNull(shape, "shape");
        accessPattern = Objects.requireNonNull(accessPattern, "accessPattern");
        mutationModel = Objects.requireNonNull(mutationModel, "mutationModel");
        policy = Objects.requireNonNull(policy, "policy");
        if (expectedSize < 0 || expectedDomainSpan < 0) {
            throw new IllegalArgumentException("negative expected metric");
        }
    }

    public static MatCollectionRequirements defaults(Shape shape) {
        return new MatCollectionRequirements(
                shape,
                AccessPattern.MIXED,
                MutationModel.BUILD_THEN_FREEZE,
                MatCollectionPolicy.BALANCED,
                false,
                0,
                0);
    }

    public String canonical() {
        return "shape=" + shape
                + ";access=" + accessPattern
                + ";mutation=" + mutationModel
                + ";policy=" + policy
                + ";rankSelect=" + rankSelect
                + ";expectedSize=" + expectedSize
                + ";expectedDomainSpan=" + expectedDomainSpan;
    }
}
