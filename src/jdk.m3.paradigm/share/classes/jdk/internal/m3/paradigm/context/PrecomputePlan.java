/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic fuzzy selection result over one frozen precompute catalog generation.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/precompute/PrecomputePlan.java}
 * blob {@code e2c255dd87fdebfb99d74a73b2610127e327777f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record PrecomputePlan(
        String requestId,
        String catalogRoot,
        int ordinal,
        String providerId,
        String bindingId,
        double score,
        long estimatedRetainedBytes,
        long estimatedBuildMicros,
        String planRoot,
        List<Alternative> alternatives) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public record Alternative(
            int ordinal,
            String providerId,
            String bindingId,
            double score,
            long estimatedRetainedBytes,
            long estimatedBuildMicros) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        public Alternative {
            if (ordinal < 0) throw new IllegalArgumentException("ordinal < 0");
            providerId = Objects.requireNonNull(providerId, "providerId");
            bindingId = Objects.requireNonNull(bindingId, "bindingId");
            if (!Double.isFinite(score) || score < 0.0 || score > 1.0) {
                throw new IllegalArgumentException("score outside [0,1]");
            }
            if (estimatedRetainedBytes < 0L) {
                throw new IllegalArgumentException("estimatedRetainedBytes < 0");
            }
            if (estimatedBuildMicros < 0L) {
                throw new IllegalArgumentException("estimatedBuildMicros < 0");
            }
        }
    }

    public PrecomputePlan {
        requestId = Objects.requireNonNull(requestId, "requestId");
        PrecomputeHashes.requireSha256(catalogRoot, "catalogRoot");
        if (ordinal < 0) throw new IllegalArgumentException("ordinal < 0");
        providerId = Objects.requireNonNull(providerId, "providerId");
        bindingId = Objects.requireNonNull(bindingId, "bindingId");
        if (!Double.isFinite(score) || score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("score outside [0,1]");
        }
        if (estimatedRetainedBytes < 0L) {
            throw new IllegalArgumentException("estimatedRetainedBytes < 0");
        }
        if (estimatedBuildMicros < 0L) {
            throw new IllegalArgumentException("estimatedBuildMicros < 0");
        }
        PrecomputeHashes.requireSha256(planRoot, "planRoot");
        alternatives = List.copyOf(Objects.requireNonNull(alternatives, "alternatives"));
        if (alternatives.isEmpty()) throw new IllegalArgumentException("alternatives empty");
    }
}
