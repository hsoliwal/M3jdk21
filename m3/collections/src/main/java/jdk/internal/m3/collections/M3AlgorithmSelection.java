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
 * Explainable deterministic result of algorithm-kernel selection.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/AlgorithmSelection.java}
 * blob {@code 847392aa0152fe751abdf22356687153b8ccc0b1} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3AlgorithmSelection {

    public record Alternative(
            String candidateId,
            Class<?> implementationType,
            double score,
            long estimatedBytes,
            String rationale) {
    }

    private final M3AlgorithmRequirements requirements;
    private final Alternative best;
    private final List<Alternative> alternatives;

    M3AlgorithmSelection(
            M3AlgorithmRequirements requirements,
            Alternative best,
            List<Alternative> alternatives) {
        this.requirements = Objects.requireNonNull(requirements, "requirements");
        this.best = Objects.requireNonNull(best, "best");
        this.alternatives = List.copyOf(alternatives);
    }

    public M3AlgorithmRequirements requirements() { return requirements; }
    public String candidateId() { return best.candidateId(); }
    public Class<?> implementationType() { return best.implementationType(); }
    public double score() { return best.score(); }
    public long estimatedBytes() { return best.estimatedBytes(); }
    public String rationale() { return best.rationale(); }
    public List<Alternative> alternatives() { return alternatives; }
}
