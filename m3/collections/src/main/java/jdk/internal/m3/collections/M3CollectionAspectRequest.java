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
 * Compact construction request retaining semantic and residency aspects in one primitive word.
 *
 * <p>The existing {@link M3ShapesCollectionRequest} remains the provider-facing compatibility request while
 * selector migration is in progress. Policy bits remain attached here and are never silently
 * discarded.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/CollectionAspectRequest.java}
 * blob {@code 9432ffccef48c8d5336ba9ea9b8ac21a7657af1d} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 2 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public record M3CollectionAspectRequest(
        int aspects,
        int maximumSize,
        M3CollectionPreferences preferred) {

    public M3CollectionAspectRequest {
        M3CollectionAspectMask.requireShapeRequirementWord(aspects);
        Objects.requireNonNull(preferred, "preferred");
        if (maximumSize < 0 || maximumSize > (1 << 28)) {
            throw new IllegalArgumentException("maximumSize outside 0..2^28");
        }
        if (preferred.expectedSize() > maximumSize) {
            throw new IllegalArgumentException("expectedSize exceeds maximumSize");
        }
    }

    /** Balanced constructor requested by the compact collection API. */
    public M3CollectionAspectRequest(int aspects, int expectedSize, int maximumSize) {
        this(aspects, maximumSize, M3CollectionPreferences.balanced(expectedSize));
    }

    /** Default maximum matches the current shape-addressing domain. */
    public static M3CollectionAspectRequest of(int aspects, int expectedSize) {
        return new M3CollectionAspectRequest(aspects, expectedSize, 1 << 28);
    }

    public boolean has(int aspect) {
        return M3CollectionAspectMask.has(aspects, aspect);
    }

    /** Current semantic fields consumable by existing shape providers. */
    public int semanticAspects() {
        return aspects & M3CollectionAspectMask.SEMANTIC_MASK;
    }

    /** Storage/residency/API policy that must not be silently lost during provider migration. */
    public int policyAspects() {
        return aspects & ~M3CollectionAspectMask.SEMANTIC_MASK;
    }

    /** Explicit projection into the existing shape selector. */
    public M3ShapesCollectionRequest semanticRequest() {
        return new M3ShapesCollectionRequest(
                new M3CollectionRequirements(semanticAspects(), maximumSize),
                preferred);
    }

    public boolean pageable() {
        return has(M3CollectionAspectMask.PAGEABLE);
    }

    public boolean sqliteCold() {
        return has(M3CollectionAspectMask.SQLITE_COLD);
    }

    public boolean pressureAdaptive() {
        return has(M3CollectionAspectMask.PRESSURE_ADAPTIVE);
    }
}
