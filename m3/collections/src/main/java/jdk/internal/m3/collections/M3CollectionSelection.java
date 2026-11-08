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

import java.util.List;
import java.util.Objects;

/**
 * Reproducible decision receipt. No timing, worker IDs, or filesystem paths enter its digest.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/CollectionSelection.java}
 * blob {@code 01d0a94b1bb54b932317536b5b10aa6ed209f96d} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 2 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public record M3CollectionSelection(String policy, M3ShapesCollectionRequest request, String registryDigest,
                                  String selectedId, List<M3CandidateDecision> decisions,
                                  String canonicalText, String digest) {
    public M3CollectionSelection {
        Objects.requireNonNull(policy, "policy"); Objects.requireNonNull(request, "request");
        Objects.requireNonNull(registryDigest, "registryDigest"); Objects.requireNonNull(selectedId, "selectedId");
        decisions = List.copyOf(decisions);
        Objects.requireNonNull(canonicalText, "canonicalText"); Objects.requireNonNull(digest, "digest");
    }
}
