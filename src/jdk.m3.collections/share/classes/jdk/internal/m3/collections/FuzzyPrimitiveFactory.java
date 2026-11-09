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
package jdk.internal.m3.collections;

import java.util.Objects;

/**
 * Unified entry point for intent-driven primitive data structures and scored algorithms.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/FuzzyPrimitiveFactory.java}
 * blob {@code c01da4b8cd6440aa23913f8846764ebec5b152ee} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (primitives DenseCollections chain after API_UNION U7/U8 (v6.3): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class FuzzyPrimitiveFactory {
    private static final FuzzyPrimitiveFactory STANDARD =
            new FuzzyPrimitiveFactory(new FuzzyStructureFactory(), FuzzyAlgorithmFactory.standard());

    private final FuzzyStructureFactory structures;
    private final FuzzyAlgorithmFactory algorithms;

    public FuzzyPrimitiveFactory(
            FuzzyStructureFactory structures,
            FuzzyAlgorithmFactory algorithms) {
        this.structures = Objects.requireNonNull(structures, "structures");
        this.algorithms = Objects.requireNonNull(algorithms, "algorithms");
    }

    public static FuzzyPrimitiveFactory standard() {
        return STANDARD;
    }

    public FuzzyStructureFactory structures() {
        return structures;
    }

    public FuzzyAlgorithmFactory algorithms() {
        return algorithms;
    }

    /**
     * Provider-backed lazy collection family. Kept separate from ordinary structure
     * selection because a provider callback is a hard semantic requirement.
     */
    public ProviderCollectionFactory providers() {
        return ProviderCollectionFactory.standard();
    }

    public FuzzyStructureFactory.Plan planStructure(String intent) {
        return structures.plan(intent);
    }

    public Object createStructure(String intent) {
        return structures.create(intent);
    }

    public AlgorithmSelection selectAlgorithm(AlgorithmRequirements requirements) {
        return algorithms.select(requirements);
    }

    public AlgorithmSelection selectAlgorithm(
            String candidateId, AlgorithmRequirements requirements) {
        return algorithms.selectPinned(candidateId, requirements);
    }
}
