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

import java.util.Objects;

/**
 * Structured construction-time selector for provider-backed lean collections.
 *
 * <p>Provider semantics are hard constraints. "Fuzzy" selection is limited to AUTO id width and
 * bounded-versus-unbounded payload residency. Unsupported semantic combinations fail closed.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/FuzzyProviderCollectionFactory.java}
 * blob {@code 6bb0670156d13655910e6ed26c31e69ff042311b} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class FuzzyProviderCollectionFactory {
    private static final FuzzyProviderCollectionFactory STANDARD =
            new FuzzyProviderCollectionFactory();

    public record Plan(
            String candidateId,
            Class<?> implementationType,
            ProviderCollectionRequirements.IdWidth resolvedIdWidth,
            String rationale) {
        public Plan {
            Objects.requireNonNull(candidateId, "candidateId");
            Objects.requireNonNull(implementationType, "implementationType");
            Objects.requireNonNull(resolvedIdWidth, "resolvedIdWidth");
            Objects.requireNonNull(rationale, "rationale");
        }
    }

    private FuzzyProviderCollectionFactory() {}

    public static FuzzyProviderCollectionFactory standard() {
        return STANDARD;
    }

    public Plan plan(ProviderCollectionRequirements requirements) {
        Objects.requireNonNull(requirements, "requirements");
        ProviderCollectionRequirements.IdWidth width = requirements.resolvedIdWidth();
        boolean bounded = requirements.maxResidentRows() > 0;

        return switch (requirements.purpose()) {
            case ROWS -> bounded
                    ? new Plan(
                            "bounded-provider-rows",
                            BoundedProviderBackedPrimitiveRows.class,
                            width,
                            "row provider + CLOCK bounded residency + sparse short lifecycle state")
                    : new Plan(
                            "provider-rows",
                            ProviderBackedPrimitiveRows.class,
                            width,
                            "row provider + lazy sparse SoA payload + sparse short lifecycle state");

            case LIST -> bounded
                    ? new Plan(
                            "bounded-provider-list",
                            BoundedProviderBackedPrimitiveList.class,
                            width,
                            "fixed logical list + CLOCK bounded payload residency + short lifecycle state")
                    : new Plan(
                            "provider-list",
                            ProviderBackedPrimitiveList.class,
                            width,
                            "fixed logical list + lazy provider materialization + short lifecycle state");

            case TREE -> switch (requirements.paging()) {
                case OFFSET -> {
                    if (width != ProviderCollectionRequirements.IdWidth.INT32) {
                        throw new IllegalArgumentException("OFFSET tree requires INT32 logical ids");
                    }
                    yield bounded
                            ? new Plan(
                                    "bounded-offset-provider-tree",
                                    BoundedProviderBackedPrimitiveTree.class,
                                    width,
                                    "offset paging + int relationship lanes + bounded payload CLOCK")
                            : new Plan(
                                    "offset-provider-tree",
                                    ProviderBackedPrimitiveTree.class,
                                    width,
                                    "parent-local offset paging with int-encoded relationship lanes");
                }
                case CURSOR -> {
                    if (width != ProviderCollectionRequirements.IdWidth.LONG64) {
                        throw new IllegalArgumentException("CURSOR tree requires LONG64 logical ids");
                    }
                    yield bounded
                            ? new Plan(
                                    "bounded-cursor-provider-long-tree",
                                    BoundedProviderBackedPrimitiveLongTree.class,
                                    width,
                                    "opaque parent-local cursors + long relationships + bounded payload CLOCK")
                            : new Plan(
                                    "cursor-provider-long-tree",
                                    ProviderBackedPrimitiveLongTree.class,
                                    width,
                                    "opaque parent-local cursors with long relationship lanes");
                }
                case VERSIONED_CURSOR -> {
                    if (width != ProviderCollectionRequirements.IdWidth.LONG64) {
                        throw new IllegalArgumentException(
                                "VERSIONED_CURSOR tree requires LONG64 logical ids");
                    }
                    yield bounded
                            ? new Plan(
                                    "bounded-versioned-cursor-provider-long-tree",
                                    BoundedVersionedProviderBackedPrimitiveLongTree.class,
                                    width,
                                    "version-pinned cursor paging + bounded payload CLOCK")
                            : new Plan(
                                    "versioned-cursor-provider-long-tree",
                                    VersionedProviderBackedPrimitiveLongTree.class,
                                    width,
                                    "opaque cursor paging pinned to one provider version per parent branch");
                }
                case NONE -> throw new IllegalArgumentException("TREE requires paging");
            };
        };
    }

    public Object createRows(
            ProviderCollectionRequirements requirements,
            PrimitiveRowProvider provider) {
        Plan plan = plan(requirements);
        if (requirements.purpose() != ProviderCollectionRequirements.Purpose.ROWS) {
            throw new IllegalArgumentException("requirements are not ROWS");
        }
        int rootBits = requirements.rootBitsForEightEightTwelve();
        return switch (plan.candidateId()) {
            case "bounded-provider-rows" -> new BoundedProviderBackedPrimitiveRows(
                    rootBits, 8, 12,
                    requirements.maxResidentRows(), provider, requirements.rawLaneKinds());
            case "provider-rows" -> new ProviderBackedPrimitiveRows(
                    rootBits, 8, 12, provider, requirements.rawLaneKinds());
            default -> throw new IllegalStateException("unexpected row plan " + plan.candidateId());
        };
    }

    /** Returns the selected concrete provider-backed list implementation. */
    public Object createList(
            ProviderCollectionRequirements requirements,
            PrimitiveRowProvider provider) {
        Plan plan = plan(requirements);
        if (requirements.purpose() != ProviderCollectionRequirements.Purpose.LIST) {
            throw new IllegalArgumentException("requirements are not LIST");
        }
        int rootBits = requirements.rootBitsForEightEightTwelve();
        return switch (plan.candidateId()) {
            case "bounded-provider-list" -> new BoundedProviderBackedPrimitiveList(
                    requirements.logicalSize(),
                    rootBits, 8, 12,
                    requirements.maxResidentRows(),
                    provider,
                    requirements.rawLaneKinds());
            case "provider-list" -> new ProviderBackedPrimitiveList(
                    requirements.logicalSize(),
                    rootBits, 8, 12,
                    provider,
                    requirements.rawLaneKinds());
            default -> throw new IllegalStateException("unexpected list plan " + plan.candidateId());
        };
    }

    public ProviderBackedPrimitiveTree createOffsetTree(
            ProviderCollectionRequirements requirements,
            PagedPrimitiveTreeProvider provider) {
        Plan plan = plan(requirements);
        if (!plan.candidateId().equals("offset-provider-tree")) {
            throw new IllegalArgumentException("requirements do not select unbounded offset tree");
        }
        return new ProviderBackedPrimitiveTree(
                requirements.rootBitsForEightEightTwelve(), 8, 12,
                provider, requirements.pageSize(), requirements.rawLaneKinds());
    }

    public BoundedProviderBackedPrimitiveTree createBoundedOffsetTree(
            ProviderCollectionRequirements requirements,
            PagedPrimitiveTreeProvider provider) {
        Plan plan = plan(requirements);
        if (!plan.candidateId().equals("bounded-offset-provider-tree")) {
            throw new IllegalArgumentException("requirements do not select bounded offset tree");
        }
        return new BoundedProviderBackedPrimitiveTree(
                requirements.rootBitsForEightEightTwelve(), 8, 12,
                requirements.maxResidentRows(),
                provider, requirements.pageSize(), requirements.rawLaneKinds());
    }

    public ProviderBackedPrimitiveLongTree createCursorTree(
            ProviderCollectionRequirements requirements,
            CursorPagedPrimitiveTreeProvider provider) {
        Plan plan = plan(requirements);
        if (!plan.candidateId().equals("cursor-provider-long-tree")) {
            throw new IllegalArgumentException("requirements do not select unbounded cursor long tree");
        }
        return new ProviderBackedPrimitiveLongTree(
                requirements.rootBitsForEightEightTwelve(), 8, 12,
                provider, requirements.pageSize(), requirements.rawLaneKinds());
    }

    public BoundedProviderBackedPrimitiveLongTree createBoundedCursorTree(
            ProviderCollectionRequirements requirements,
            CursorPagedPrimitiveTreeProvider provider) {
        Plan plan = plan(requirements);
        if (!plan.candidateId().equals("bounded-cursor-provider-long-tree")) {
            throw new IllegalArgumentException("requirements do not select bounded cursor long tree");
        }
        return new BoundedProviderBackedPrimitiveLongTree(
                requirements.rootBitsForEightEightTwelve(), 8, 12,
                requirements.maxResidentRows(),
                provider, requirements.pageSize(), requirements.rawLaneKinds());
    }

    public VersionedProviderBackedPrimitiveLongTree createVersionedCursorTree(
            ProviderCollectionRequirements requirements,
            VersionedCursorPagedPrimitiveTreeProvider provider) {
        Plan plan = plan(requirements);
        if (!plan.candidateId().equals("versioned-cursor-provider-long-tree")) {
            throw new IllegalArgumentException(
                    "requirements do not select unbounded versioned cursor tree");
        }
        return new VersionedProviderBackedPrimitiveLongTree(
                requirements.rootBitsForEightEightTwelve(), 8, 12,
                provider, requirements.pageSize(), requirements.rawLaneKinds());
    }

    public BoundedVersionedProviderBackedPrimitiveLongTree createBoundedVersionedCursorTree(
            ProviderCollectionRequirements requirements,
            VersionedCursorPagedPrimitiveTreeProvider provider) {
        Plan plan = plan(requirements);
        if (!plan.candidateId().equals("bounded-versioned-cursor-provider-long-tree")) {
            throw new IllegalArgumentException(
                    "requirements do not select bounded versioned cursor tree");
        }
        return new BoundedVersionedProviderBackedPrimitiveLongTree(
                requirements.rootBitsForEightEightTwelve(), 8, 12,
                requirements.maxResidentRows(),
                provider, requirements.pageSize(), requirements.rawLaneKinds());
    }
}
