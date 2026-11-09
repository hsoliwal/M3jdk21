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
package jdk.internal.m3.collections.shape;

import java.util.List;
import java.util.Objects;

/**
 * One admitted score or rejected candidate with deterministic reasons.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/CandidateDecision.java}
 * blob {@code 8ec84906c0a69348147a6bddda30f81890d58e4b} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public record CandidateDecision(String id, boolean admitted, long score, List<String> reasons) {
    public CandidateDecision {
        Objects.requireNonNull(id, "id");
        reasons = List.copyOf(reasons);
        if (admitted != reasons.isEmpty() || (admitted ? score < 0 : score != -1)) {
            throw new IllegalArgumentException("inconsistent admission decision");
        }
    }
}
