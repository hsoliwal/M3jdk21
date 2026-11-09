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

import java.util.Locale;
import java.util.Objects;

/**
 * Unified construction-time façade over Synexia's fuzzy structure, algorithm and
 * provider-backed collection selectors.
 *
 * <p>This type owns no collection state and adds no hot-path wrapper. It only
 * delegates planning/creation to the existing concrete owners and exposes one
 * explainable plan shape across the three domains.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LeanCollectionFactory.java}
 * blob {@code a4a51ddd81ce89448a4893b03476a73786d4aad6} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (primitives DenseCollections chain after API_UNION U7/U8 (v6.3): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LeanCollectionFactory {

    public enum Domain {
        STRUCTURE,
        ALGORITHM,
        PROVIDER
    }

    public record Plan(
            Domain domain,
            String candidateId,
            String implementationType,
            String rationale,
            Object delegatePlan) {

        public Plan {
            Objects.requireNonNull(domain, "domain");
            candidateId = requireText(candidateId, "candidateId");
            implementationType = requireText(implementationType, "implementationType");
            rationale = requireText(rationale, "rationale");
            Objects.requireNonNull(delegatePlan, "delegatePlan");
        }

        private static String requireText(String value, String name) {
            Objects.requireNonNull(value, name);
            if (value.isBlank()) throw new IllegalArgumentException(name + " is blank");
            return value;
        }
    }

    private static final LeanCollectionFactory STANDARD =
            new LeanCollectionFactory(
                    new FuzzyStructureFactory(),
                    FuzzyAlgorithmFactory.standard(),
                    FuzzyProviderCollectionFactory.standard());

    private final FuzzyStructureFactory structures;
    private final FuzzyAlgorithmFactory algorithms;
    private final FuzzyProviderCollectionFactory providers;

    public LeanCollectionFactory(
            FuzzyStructureFactory structures,
            FuzzyAlgorithmFactory algorithms,
            FuzzyProviderCollectionFactory providers) {
        this.structures = Objects.requireNonNull(structures, "structures");
        this.algorithms = Objects.requireNonNull(algorithms, "algorithms");
        this.providers = Objects.requireNonNull(providers, "providers");
    }

    public static LeanCollectionFactory standard() {
        return STANDARD;
    }

    public FuzzyStructureFactory structures() {
        return structures;
    }

    public FuzzyAlgorithmFactory algorithms() {
        return algorithms;
    }

    public FuzzyProviderCollectionFactory providers() {
        return providers;
    }

    public Plan planStructure(String intent) {
        FuzzyStructureFactory.Plan plan = structures.plan(intent);
        String candidateId = "structure:"
                + plan.family().name().toLowerCase(Locale.ROOT)
                + ':'
                + plan.storage().name().toLowerCase(Locale.ROOT)
                + ':'
                + plan.concurrency().name().toLowerCase(Locale.ROOT);
        return new Plan(
                Domain.STRUCTURE,
                candidateId,
                plan.implementationType(),
                String.join("; ", plan.rationale()),
                plan);
    }

    public Plan planAlgorithm(AlgorithmRequirements requirements) {
        AlgorithmSelection selection =
                algorithms.select(Objects.requireNonNull(requirements, "requirements"));
        return new Plan(
                Domain.ALGORITHM,
                selection.candidateId(),
                selection.implementationType().getName(),
                selection.rationale(),
                selection);
    }

    public Plan planProvider(ProviderCollectionRequirements requirements) {
        FuzzyProviderCollectionFactory.Plan plan =
                providers.plan(Objects.requireNonNull(requirements, "requirements"));
        return new Plan(
                Domain.PROVIDER,
                plan.candidateId(),
                plan.implementationType().getName(),
                plan.rationale(),
                plan);
    }

    /** Returns the concrete structure selected by the existing fuzzy structure owner. */
    public Object createStructure(String intent) {
        return structures.create(intent);
    }

    public AlgorithmSelection selectAlgorithm(AlgorithmRequirements requirements) {
        return algorithms.select(Objects.requireNonNull(requirements, "requirements"));
    }

    public AlgorithmSelection selectAlgorithm(
            String candidateId,
            AlgorithmRequirements requirements) {
        return algorithms.selectPinned(
                Objects.requireNonNull(candidateId, "candidateId"),
                Objects.requireNonNull(requirements, "requirements"));
    }

    public Object createProviderRows(
            ProviderCollectionRequirements requirements,
            PrimitiveRowProvider provider) {
        return providers.createRows(
                Objects.requireNonNull(requirements, "requirements"),
                Objects.requireNonNull(provider, "provider"));
    }

    public Object createProviderList(
            ProviderCollectionRequirements requirements,
            PrimitiveRowProvider provider) {
        return providers.createList(
                Objects.requireNonNull(requirements, "requirements"),
                Objects.requireNonNull(provider, "provider"));
    }

    public ProviderBackedPrimitiveTree createOffsetTree(
            ProviderCollectionRequirements requirements,
            PagedPrimitiveTreeProvider provider) {
        return providers.createOffsetTree(
                Objects.requireNonNull(requirements, "requirements"),
                Objects.requireNonNull(provider, "provider"));
    }

    public BoundedProviderBackedPrimitiveTree createBoundedOffsetTree(
            ProviderCollectionRequirements requirements,
            PagedPrimitiveTreeProvider provider) {
        return providers.createBoundedOffsetTree(
                Objects.requireNonNull(requirements, "requirements"),
                Objects.requireNonNull(provider, "provider"));
    }

    public ProviderBackedPrimitiveLongTree createCursorTree(
            ProviderCollectionRequirements requirements,
            CursorPagedPrimitiveTreeProvider provider) {
        return providers.createCursorTree(
                Objects.requireNonNull(requirements, "requirements"),
                Objects.requireNonNull(provider, "provider"));
    }

    public BoundedProviderBackedPrimitiveLongTree createBoundedCursorTree(
            ProviderCollectionRequirements requirements,
            CursorPagedPrimitiveTreeProvider provider) {
        return providers.createBoundedCursorTree(
                Objects.requireNonNull(requirements, "requirements"),
                Objects.requireNonNull(provider, "provider"));
    }

    public VersionedProviderBackedPrimitiveLongTree createVersionedCursorTree(
            ProviderCollectionRequirements requirements,
            VersionedCursorPagedPrimitiveTreeProvider provider) {
        return providers.createVersionedCursorTree(
                Objects.requireNonNull(requirements, "requirements"),
                Objects.requireNonNull(provider, "provider"));
    }

    public BoundedVersionedProviderBackedPrimitiveLongTree createBoundedVersionedCursorTree(
            ProviderCollectionRequirements requirements,
            VersionedCursorPagedPrimitiveTreeProvider provider) {
        return providers.createBoundedVersionedCursorTree(
                Objects.requireNonNull(requirements, "requirements"),
                Objects.requireNonNull(provider, "provider"));
    }
}
