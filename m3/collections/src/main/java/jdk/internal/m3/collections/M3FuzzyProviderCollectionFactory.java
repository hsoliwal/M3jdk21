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
public final class M3FuzzyProviderCollectionFactory {
    private static final M3FuzzyProviderCollectionFactory STANDARD =
            new M3FuzzyProviderCollectionFactory();

    public record Plan(
            String candidateId,
            Class<?> implementationType,
            M3ProviderCollectionRequirements.IdWidth resolvedIdWidth,
            String rationale) {
        public Plan {
            Objects.requireNonNull(candidateId, "candidateId");
            Objects.requireNonNull(implementationType, "implementationType");
            Objects.requireNonNull(resolvedIdWidth, "resolvedIdWidth");
            Objects.requireNonNull(rationale, "rationale");
        }
    }

    private M3FuzzyProviderCollectionFactory() {}

    public static M3FuzzyProviderCollectionFactory standard() {
        return STANDARD;
    }

    public Plan plan(M3ProviderCollectionRequirements requirements) {
        Objects.requireNonNull(requirements, "requirements");
        M3ProviderCollectionRequirements.IdWidth width = requirements.resolvedIdWidth();
        boolean bounded = requirements.maxResidentRows() > 0;

        return switch (requirements.purpose()) {
            case ROWS -> bounded
                    ? new Plan(
                            "bounded-provider-rows",
                            M3BoundedProviderBackedPrimitiveRows.class,
                            width,
                            "row provider + CLOCK bounded residency + sparse short lifecycle state")
                    : new Plan(
                            "provider-rows",
                            M3ProviderBackedPrimitiveRows.class,
                            width,
                            "row provider + lazy sparse SoA payload + sparse short lifecycle state");

            case LIST -> bounded
                    ? new Plan(
                            "bounded-provider-list",
                            M3BoundedProviderBackedPrimitiveList.class,
                            width,
                            "fixed logical list + CLOCK bounded payload residency + short lifecycle state")
                    : new Plan(
                            "provider-list",
                            M3ProviderBackedPrimitiveList.class,
                            width,
                            "fixed logical list + lazy provider materialization + short lifecycle state");

            case TREE -> switch (requirements.paging()) {
                case OFFSET -> {
                    if (width != M3ProviderCollectionRequirements.IdWidth.INT32) {
                        throw new IllegalArgumentException("OFFSET tree requires INT32 logical ids");
                    }
                    yield bounded
                            ? new Plan(
                                    "bounded-offset-provider-tree",
                                    M3BoundedProviderBackedPrimitiveTree.class,
                                    width,
                                    "offset paging + int relationship lanes + bounded payload CLOCK")
                            : new Plan(
                                    "offset-provider-tree",
                                    M3ProviderBackedPrimitiveTree.class,
                                    width,
                                    "parent-local offset paging with int-encoded relationship lanes");
                }
                case CURSOR -> {
                    if (width != M3ProviderCollectionRequirements.IdWidth.LONG64) {
                        throw new IllegalArgumentException("CURSOR tree requires LONG64 logical ids");
                    }
                    yield bounded
                            ? new Plan(
                                    "bounded-cursor-provider-long-tree",
                                    M3BoundedProviderBackedPrimitiveLongTree.class,
                                    width,
                                    "opaque parent-local cursors + long relationships + bounded payload CLOCK")
                            : new Plan(
                                    "cursor-provider-long-tree",
                                    M3ProviderBackedPrimitiveLongTree.class,
                                    width,
                                    "opaque parent-local cursors with long relationship lanes");
                }
                case VERSIONED_CURSOR -> {
                    if (width != M3ProviderCollectionRequirements.IdWidth.LONG64) {
                        throw new IllegalArgumentException(
                                "VERSIONED_CURSOR tree requires LONG64 logical ids");
                    }
                    yield bounded
                            ? new Plan(
                                    "bounded-versioned-cursor-provider-long-tree",
                                    M3BoundedVersionedProviderBackedPrimitiveLongTree.class,
                                    width,
                                    "version-pinned cursor paging + bounded payload CLOCK")
                            : new Plan(
                                    "versioned-cursor-provider-long-tree",
                                    M3VersionedProviderBackedPrimitiveLongTree.class,
                                    width,
                                    "opaque cursor paging pinned to one provider version per parent branch");
                }
                case NONE -> throw new IllegalArgumentException("TREE requires paging");
            };
        };
    }

    public Object createRows(
            M3ProviderCollectionRequirements requirements,
            M3PrimitiveRowProvider provider) {
        Plan plan = plan(requirements);
        if (requirements.purpose() != M3ProviderCollectionRequirements.Purpose.ROWS) {
            throw new IllegalArgumentException("requirements are not ROWS");
        }
        int rootBits = requirements.rootBitsForEightEightTwelve();
        return switch (plan.candidateId()) {
            case "bounded-provider-rows" -> new M3BoundedProviderBackedPrimitiveRows(
                    rootBits, 8, 12,
                    requirements.maxResidentRows(), provider, requirements.rawLaneKinds());
            case "provider-rows" -> new M3ProviderBackedPrimitiveRows(
                    rootBits, 8, 12, provider, requirements.rawLaneKinds());
            default -> throw new IllegalStateException("unexpected row plan " + plan.candidateId());
        };
    }

    /** Returns the selected concrete provider-backed list implementation. */
    public Object createList(
            M3ProviderCollectionRequirements requirements,
            M3PrimitiveRowProvider provider) {
        Plan plan = plan(requirements);
        if (requirements.purpose() != M3ProviderCollectionRequirements.Purpose.LIST) {
            throw new IllegalArgumentException("requirements are not LIST");
        }
        int rootBits = requirements.rootBitsForEightEightTwelve();
        return switch (plan.candidateId()) {
            case "bounded-provider-list" -> new M3BoundedProviderBackedPrimitiveList(
                    requirements.logicalSize(),
                    rootBits, 8, 12,
                    requirements.maxResidentRows(),
                    provider,
                    requirements.rawLaneKinds());
            case "provider-list" -> new M3ProviderBackedPrimitiveList(
                    requirements.logicalSize(),
                    rootBits, 8, 12,
                    provider,
                    requirements.rawLaneKinds());
            default -> throw new IllegalStateException("unexpected list plan " + plan.candidateId());
        };
    }

    public M3ProviderBackedPrimitiveTree createOffsetTree(
            M3ProviderCollectionRequirements requirements,
            M3PagedPrimitiveTreeProvider provider) {
        Plan plan = plan(requirements);
        if (!plan.candidateId().equals("offset-provider-tree")) {
            throw new IllegalArgumentException("requirements do not select unbounded offset tree");
        }
        return new M3ProviderBackedPrimitiveTree(
                requirements.rootBitsForEightEightTwelve(), 8, 12,
                provider, requirements.pageSize(), requirements.rawLaneKinds());
    }

    public M3BoundedProviderBackedPrimitiveTree createBoundedOffsetTree(
            M3ProviderCollectionRequirements requirements,
            M3PagedPrimitiveTreeProvider provider) {
        Plan plan = plan(requirements);
        if (!plan.candidateId().equals("bounded-offset-provider-tree")) {
            throw new IllegalArgumentException("requirements do not select bounded offset tree");
        }
        return new M3BoundedProviderBackedPrimitiveTree(
                requirements.rootBitsForEightEightTwelve(), 8, 12,
                requirements.maxResidentRows(),
                provider, requirements.pageSize(), requirements.rawLaneKinds());
    }

    public M3ProviderBackedPrimitiveLongTree createCursorTree(
            M3ProviderCollectionRequirements requirements,
            M3CursorPagedPrimitiveTreeProvider provider) {
        Plan plan = plan(requirements);
        if (!plan.candidateId().equals("cursor-provider-long-tree")) {
            throw new IllegalArgumentException("requirements do not select unbounded cursor long tree");
        }
        return new M3ProviderBackedPrimitiveLongTree(
                requirements.rootBitsForEightEightTwelve(), 8, 12,
                provider, requirements.pageSize(), requirements.rawLaneKinds());
    }

    public M3BoundedProviderBackedPrimitiveLongTree createBoundedCursorTree(
            M3ProviderCollectionRequirements requirements,
            M3CursorPagedPrimitiveTreeProvider provider) {
        Plan plan = plan(requirements);
        if (!plan.candidateId().equals("bounded-cursor-provider-long-tree")) {
            throw new IllegalArgumentException("requirements do not select bounded cursor long tree");
        }
        return new M3BoundedProviderBackedPrimitiveLongTree(
                requirements.rootBitsForEightEightTwelve(), 8, 12,
                requirements.maxResidentRows(),
                provider, requirements.pageSize(), requirements.rawLaneKinds());
    }

    public M3VersionedProviderBackedPrimitiveLongTree createVersionedCursorTree(
            M3ProviderCollectionRequirements requirements,
            M3VersionedCursorPagedPrimitiveTreeProvider provider) {
        Plan plan = plan(requirements);
        if (!plan.candidateId().equals("versioned-cursor-provider-long-tree")) {
            throw new IllegalArgumentException(
                    "requirements do not select unbounded versioned cursor tree");
        }
        return new M3VersionedProviderBackedPrimitiveLongTree(
                requirements.rootBitsForEightEightTwelve(), 8, 12,
                provider, requirements.pageSize(), requirements.rawLaneKinds());
    }

    public M3BoundedVersionedProviderBackedPrimitiveLongTree createBoundedVersionedCursorTree(
            M3ProviderCollectionRequirements requirements,
            M3VersionedCursorPagedPrimitiveTreeProvider provider) {
        Plan plan = plan(requirements);
        if (!plan.candidateId().equals("bounded-versioned-cursor-provider-long-tree")) {
            throw new IllegalArgumentException(
                    "requirements do not select bounded versioned cursor tree");
        }
        return new M3BoundedVersionedProviderBackedPrimitiveLongTree(
                requirements.rootBitsForEightEightTwelve(), 8, 12,
                requirements.maxResidentRows(),
                provider, requirements.pageSize(), requirements.rawLaneKinds());
    }
}
