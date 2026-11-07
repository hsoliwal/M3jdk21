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

import jdk.m3.collections.M3PrimitiveKind;

/**
 * Construction-time factory for provider-backed lean collection families.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/ProviderCollectionFactory.java}
 * blob {@code 4e6fdf58c2ec38786666ef9d0b661547b4bd5d3c} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3ProviderCollectionFactory {
    private static final M3ProviderCollectionFactory STANDARD = new M3ProviderCollectionFactory();

    private M3ProviderCollectionFactory() {}

    public static M3ProviderCollectionFactory standard() {
        return STANDARD;
    }

    public M3ProviderBackedPrimitiveRows rows(
            M3PrimitiveRowProvider provider,
            M3PrimitiveKind... laneKinds) {
        return new M3ProviderBackedPrimitiveRows(provider, laneKinds);
    }

    public M3ProviderBackedPrimitiveList list(
            long logicalSize,
            M3PrimitiveRowProvider provider,
            M3PrimitiveKind... laneKinds) {
        return new M3ProviderBackedPrimitiveList(logicalSize, provider, laneKinds);
    }

    public M3ProviderBackedPrimitiveList list(
            long logicalSize,
            int rootBits,
            int directoryBits,
            int leafBits,
            M3PrimitiveRowProvider provider,
            M3PrimitiveKind... laneKinds) {
        return new M3ProviderBackedPrimitiveList(
                logicalSize,
                rootBits,
                directoryBits,
                leafBits,
                provider,
                laneKinds);
    }

    public M3ProviderBackedPrimitiveRows rows(
            int rootBits,
            int directoryBits,
            int leafBits,
            M3PrimitiveRowProvider provider,
            M3PrimitiveKind... laneKinds) {
        return new M3ProviderBackedPrimitiveRows(
                rootBits, directoryBits, leafBits, provider, laneKinds);
    }

    public M3ProviderBackedPrimitiveTree tree(
            M3PagedPrimitiveTreeProvider provider,
            int pageSize,
            M3PrimitiveKind... laneKinds) {
        return new M3ProviderBackedPrimitiveTree(provider, pageSize, laneKinds);
    }

    public M3ProviderBackedPrimitiveTree tree(
            int rootBits,
            int directoryBits,
            int leafBits,
            M3PagedPrimitiveTreeProvider provider,
            int pageSize,
            M3PrimitiveKind... laneKinds) {
        return new M3ProviderBackedPrimitiveTree(
                rootBits, directoryBits, leafBits, provider, pageSize, laneKinds);
    }

    public M3ProviderBackedPrimitiveLongTree longTree(
            M3CursorPagedPrimitiveTreeProvider provider,
            int pageSize,
            M3PrimitiveKind... laneKinds) {
        return new M3ProviderBackedPrimitiveLongTree(provider, pageSize, laneKinds);
    }

    public M3ProviderBackedPrimitiveLongTree longTree(
            int rootBits,
            int directoryBits,
            int leafBits,
            M3CursorPagedPrimitiveTreeProvider provider,
            int pageSize,
            M3PrimitiveKind... laneKinds) {
        return new M3ProviderBackedPrimitiveLongTree(
                rootBits, directoryBits, leafBits, provider, pageSize, laneKinds);
    }
    public M3BoundedProviderBackedPrimitiveRows boundedRows(
            int maximumResident,
            M3PrimitiveRowProvider provider,
            M3PrimitiveKind... laneKinds) {
        return new M3BoundedProviderBackedPrimitiveRows(
                maximumResident, provider, laneKinds);
    }

    public M3BoundedProviderBackedPrimitiveList boundedList(
            long logicalSize,
            int maximumResident,
            M3PrimitiveRowProvider provider,
            M3PrimitiveKind... laneKinds) {
        return new M3BoundedProviderBackedPrimitiveList(
                logicalSize, maximumResident, provider, laneKinds);
    }

    public M3BoundedProviderBackedPrimitiveTree boundedTree(
            int maximumResident,
            M3PagedPrimitiveTreeProvider provider,
            int pageSize,
            M3PrimitiveKind... laneKinds) {
        return new M3BoundedProviderBackedPrimitiveTree(
                maximumResident, provider, pageSize, laneKinds);
    }

    public M3BoundedProviderBackedPrimitiveLongTree boundedLongTree(
            int maximumResident,
            M3CursorPagedPrimitiveTreeProvider provider,
            int pageSize,
            M3PrimitiveKind... laneKinds) {
        return new M3BoundedProviderBackedPrimitiveLongTree(
                maximumResident, provider, pageSize, laneKinds);
    }

    public M3VersionedProviderBackedPrimitiveLongTree versionedLongTree(
            M3VersionedCursorPagedPrimitiveTreeProvider provider,
            int pageSize,
            M3PrimitiveKind... laneKinds) {
        return new M3VersionedProviderBackedPrimitiveLongTree(
                provider, pageSize, laneKinds);
    }

    public M3BoundedVersionedProviderBackedPrimitiveLongTree boundedVersionedLongTree(
            int maximumResident,
            M3VersionedCursorPagedPrimitiveTreeProvider provider,
            int pageSize,
            M3PrimitiveKind... laneKinds) {
        return new M3BoundedVersionedProviderBackedPrimitiveLongTree(
                maximumResident, provider, pageSize, laneKinds);
    }

}
