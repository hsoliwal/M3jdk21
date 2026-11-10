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

import jdk.m3.collections.PrimitiveKind;

/**
 * Construction-time factory for provider-backed lean collection families.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/ProviderCollectionFactory.java}
 * blob {@code 4e6fdf58c2ec38786666ef9d0b661547b4bd5d3c} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class ProviderCollectionFactory {
    private static final ProviderCollectionFactory STANDARD = new ProviderCollectionFactory();

    private ProviderCollectionFactory() {}

    public static ProviderCollectionFactory standard() {
        return STANDARD;
    }

    public ProviderBackedPrimitiveRows rows(
            PrimitiveRowProvider provider,
            PrimitiveKind... laneKinds) {
        return new ProviderBackedPrimitiveRows(provider, laneKinds);
    }

    public ProviderBackedPrimitiveList list(
            long logicalSize,
            PrimitiveRowProvider provider,
            PrimitiveKind... laneKinds) {
        return new ProviderBackedPrimitiveList(logicalSize, provider, laneKinds);
    }

    public ProviderBackedPrimitiveList list(
            long logicalSize,
            int rootBits,
            int directoryBits,
            int leafBits,
            PrimitiveRowProvider provider,
            PrimitiveKind... laneKinds) {
        return new ProviderBackedPrimitiveList(
                logicalSize,
                rootBits,
                directoryBits,
                leafBits,
                provider,
                laneKinds);
    }

    public ProviderBackedPrimitiveRows rows(
            int rootBits,
            int directoryBits,
            int leafBits,
            PrimitiveRowProvider provider,
            PrimitiveKind... laneKinds) {
        return new ProviderBackedPrimitiveRows(
                rootBits, directoryBits, leafBits, provider, laneKinds);
    }

    public ProviderBackedPrimitiveTree tree(
            PagedPrimitiveTreeProvider provider,
            int pageSize,
            PrimitiveKind... laneKinds) {
        return new ProviderBackedPrimitiveTree(provider, pageSize, laneKinds);
    }

    public ProviderBackedPrimitiveTree tree(
            int rootBits,
            int directoryBits,
            int leafBits,
            PagedPrimitiveTreeProvider provider,
            int pageSize,
            PrimitiveKind... laneKinds) {
        return new ProviderBackedPrimitiveTree(
                rootBits, directoryBits, leafBits, provider, pageSize, laneKinds);
    }

    public ProviderBackedPrimitiveLongTree longTree(
            CursorPagedPrimitiveTreeProvider provider,
            int pageSize,
            PrimitiveKind... laneKinds) {
        return new ProviderBackedPrimitiveLongTree(provider, pageSize, laneKinds);
    }

    public ProviderBackedPrimitiveLongTree longTree(
            int rootBits,
            int directoryBits,
            int leafBits,
            CursorPagedPrimitiveTreeProvider provider,
            int pageSize,
            PrimitiveKind... laneKinds) {
        return new ProviderBackedPrimitiveLongTree(
                rootBits, directoryBits, leafBits, provider, pageSize, laneKinds);
    }
    public BoundedProviderBackedPrimitiveRows boundedRows(
            int maximumResident,
            PrimitiveRowProvider provider,
            PrimitiveKind... laneKinds) {
        return new BoundedProviderBackedPrimitiveRows(
                maximumResident, provider, laneKinds);
    }

    public BoundedProviderBackedPrimitiveList boundedList(
            long logicalSize,
            int maximumResident,
            PrimitiveRowProvider provider,
            PrimitiveKind... laneKinds) {
        return new BoundedProviderBackedPrimitiveList(
                logicalSize, maximumResident, provider, laneKinds);
    }

    public BoundedProviderBackedPrimitiveTree boundedTree(
            int maximumResident,
            PagedPrimitiveTreeProvider provider,
            int pageSize,
            PrimitiveKind... laneKinds) {
        return new BoundedProviderBackedPrimitiveTree(
                maximumResident, provider, pageSize, laneKinds);
    }

    public BoundedProviderBackedPrimitiveLongTree boundedLongTree(
            int maximumResident,
            CursorPagedPrimitiveTreeProvider provider,
            int pageSize,
            PrimitiveKind... laneKinds) {
        return new BoundedProviderBackedPrimitiveLongTree(
                maximumResident, provider, pageSize, laneKinds);
    }

    public VersionedProviderBackedPrimitiveLongTree versionedLongTree(
            VersionedCursorPagedPrimitiveTreeProvider provider,
            int pageSize,
            PrimitiveKind... laneKinds) {
        return new VersionedProviderBackedPrimitiveLongTree(
                provider, pageSize, laneKinds);
    }

    public BoundedVersionedProviderBackedPrimitiveLongTree boundedVersionedLongTree(
            int maximumResident,
            VersionedCursorPagedPrimitiveTreeProvider provider,
            int pageSize,
            PrimitiveKind... laneKinds) {
        return new BoundedVersionedProviderBackedPrimitiveLongTree(
                maximumResident, provider, pageSize, laneKinds);
    }

}
