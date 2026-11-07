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
import jdk.m3.collections.PrimitiveKind;

/**
 * Structured construction requirements for provider-backed lean collections.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/ProviderCollectionRequirements.java}
 * blob {@code 5c2feb93c81273726b4a5b7dc141c6ce8109f990} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3ProviderCollectionRequirements {
    public enum Purpose { ROWS, LIST, TREE }
    public enum IdWidth { AUTO, INT32, LONG64 }
    public enum Paging { NONE, OFFSET, CURSOR, VERSIONED_CURSOR }

    private final Purpose purpose;
    private final IdWidth idWidth;
    private final Paging paging;
    private final long maximumLogicalId;
    private final long logicalSize;
    private final int pageSize;
    private final int maxResidentRows;
    private final PrimitiveKind[] laneKinds;

    private M3ProviderCollectionRequirements(Builder builder) {
        purpose = builder.purpose;
        idWidth = builder.idWidth;
        paging = builder.paging;
        maximumLogicalId = builder.maximumLogicalId;
        logicalSize = builder.logicalSize;
        pageSize = builder.pageSize;
        maxResidentRows = builder.maxResidentRows;
        laneKinds = builder.laneKinds.clone();
        validate();
    }

    public static Builder builder(Purpose purpose, PrimitiveKind... laneKinds) {
        return new Builder(purpose, laneKinds);
    }

    public Purpose purpose() { return purpose; }
    public IdWidth idWidth() { return idWidth; }
    public Paging paging() { return paging; }
    public long maximumLogicalId() { return maximumLogicalId; }
    public long logicalSize() { return logicalSize; }
    public int pageSize() { return pageSize; }
    public int maxResidentRows() { return maxResidentRows; }
    public PrimitiveKind[] laneKinds() { return laneKinds.clone(); }
    PrimitiveKind[] rawLaneKinds() { return laneKinds; }

    public IdWidth resolvedIdWidth() {
        if (idWidth != IdWidth.AUTO) return idWidth;
        return effectiveMaximumLogicalId() < Integer.MAX_VALUE ? IdWidth.INT32 : IdWidth.LONG64;
    }

    int rootBitsForEightEightTwelve() {
        long maximum = effectiveMaximumLogicalId();
        long domain = Math.addExact(maximum, 1L);
        int totalBits = domain <= 1L ? 1 : 64 - Long.numberOfLeadingZeros(domain - 1L);
        int rootBits = Math.max(1, totalBits - 20);
        if (rootBits > 20) {
            throw new IllegalArgumentException(
                    "logical id domain exceeds 8/8/12 lazy-factory geometry");
        }
        return rootBits;
    }

    private long effectiveMaximumLogicalId() {
        if (purpose != Purpose.LIST || logicalSize == 0L) return maximumLogicalId;
        return Math.max(maximumLogicalId, logicalSize - 1L);
    }

    private void validate() {
        if (maximumLogicalId < 0L) throw new IllegalArgumentException("maximumLogicalId < 0");
        if (logicalSize < 0L) throw new IllegalArgumentException("logicalSize < 0");
        if (maxResidentRows < 0) throw new IllegalArgumentException("maxResidentRows < 0");
        if (laneKinds.length == 0) throw new IllegalArgumentException("at least one lane required");
        for (PrimitiveKind kind : laneKinds) Objects.requireNonNull(kind, "lane kind");

        switch (purpose) {
            case ROWS -> {
                if (paging != Paging.NONE) {
                    throw new IllegalArgumentException("ROWS require NONE paging");
                }
                if (pageSize != 0) {
                    throw new IllegalArgumentException("ROWS pageSize must be zero");
                }
                if (logicalSize != 0L) {
                    throw new IllegalArgumentException("ROWS logicalSize must be zero");
                }
            }
            case LIST -> {
                if (paging != Paging.NONE) {
                    throw new IllegalArgumentException("LIST requires NONE paging");
                }
                if (pageSize != 0) {
                    throw new IllegalArgumentException("LIST pageSize must be zero");
                }
                if (logicalSize > 0L && logicalSize - 1L > effectiveMaximumLogicalId()) {
                    throw new IllegalArgumentException("LIST logicalSize exceeds logical-id domain");
                }
            }
            case TREE -> {
                if (paging == Paging.NONE) {
                    throw new IllegalArgumentException("TREE requires paging");
                }
                if (pageSize <= 0) {
                    throw new IllegalArgumentException("TREE pageSize <= 0");
                }
                if (logicalSize != 0L) {
                    throw new IllegalArgumentException("TREE logicalSize must be zero");
                }
            }
        }
    }

    public static final class Builder {
        private final Purpose purpose;
        private final PrimitiveKind[] laneKinds;
        private IdWidth idWidth = IdWidth.AUTO;
        private Paging paging;
        private long maximumLogicalId = Integer.MAX_VALUE - 1L;
        private long logicalSize;
        private int pageSize;
        private int maxResidentRows;

        private Builder(Purpose purpose, PrimitiveKind... laneKinds) {
            this.purpose = Objects.requireNonNull(purpose, "purpose");
            Objects.requireNonNull(laneKinds, "laneKinds");
            this.laneKinds = laneKinds.clone();
            maximumLogicalId =
                    purpose == Purpose.LIST ? 0L : Integer.MAX_VALUE - 1L;
            paging = purpose == Purpose.TREE ? Paging.OFFSET : Paging.NONE;
            pageSize = purpose == Purpose.TREE ? 64 : 0;
        }

        public Builder idWidth(IdWidth value) {
            idWidth = Objects.requireNonNull(value, "idWidth");
            return this;
        }

        public Builder paging(Paging value) {
            paging = Objects.requireNonNull(value, "paging");
            return this;
        }

        public Builder maximumLogicalId(long value) {
            maximumLogicalId = value;
            return this;
        }

        /** Required logical element count for LIST; ignored/forbidden for ROWS/TREE. */
        public Builder logicalSize(long value) {
            logicalSize = value;
            return this;
        }

        public Builder pageSize(int value) {
            pageSize = value;
            return this;
        }

        public Builder maxResidentRows(int value) {
            maxResidentRows = value;
            return this;
        }

        public M3ProviderCollectionRequirements build() {
            return new M3ProviderCollectionRequirements(this);
        }
    }
}
