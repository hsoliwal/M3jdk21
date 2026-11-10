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

import java.util.Objects;

/**
 * Hard resource and search bounds. Fuzzy ranking cannot override these gates.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/cognition/ContextBudget.java}
 * blob {@code d16132dcb813ec2aa3ed1fe59c3f5dfbbe1bc84f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record Budget(
        long maxCostUnits,
        long maxLatencyMicros,
        long maxMemoryBytes,
        long maxWorkUnits,
        int maxDepth,
        int maxNodes,
        int maxAlternatives,
        SkillEffect maxEffect) {

    public Budget {
        positive(maxCostUnits, "maxCostUnits");
        positive(maxLatencyMicros, "maxLatencyMicros");
        positive(maxMemoryBytes, "maxMemoryBytes");
        positive(maxWorkUnits, "maxWorkUnits");
        if (maxDepth < 0 || maxDepth > 64) throw new IllegalArgumentException("maxDepth must be 0..64");
        if (maxNodes < 1 || maxNodes > 10_000) throw new IllegalArgumentException("maxNodes must be 1..10000");
        if (maxAlternatives < 1 || maxAlternatives > 64) {
            throw new IllegalArgumentException("maxAlternatives must be 1..64");
        }
        maxEffect = Objects.requireNonNull(maxEffect, "maxEffect");
    }

    public Budget share(int permille) {
        if (permille < 1 || permille > 1_000) {
            throw new IllegalArgumentException("budget share must be 1..1000 permille");
        }
        return new Budget(
                scaled(maxCostUnits, permille),
                scaled(maxLatencyMicros, permille),
                scaled(maxMemoryBytes, permille),
                scaled(maxWorkUnits, permille),
                maxDepth,
                maxNodes,
                maxAlternatives,
                maxEffect);
    }

    public String root() {
        return SemanticHash.sha256("COP-CONTEXT-BUDGET/1",
                Long.toString(maxCostUnits), Long.toString(maxLatencyMicros),
                Long.toString(maxMemoryBytes), Long.toString(maxWorkUnits),
                Integer.toString(maxDepth), Integer.toString(maxNodes),
                Integer.toString(maxAlternatives), maxEffect.name());
    }

    private static long scaled(long value, int permille) {
        long quotient = value / 1_000L;
        long remainder = value % 1_000L;
        return Math.max(1L, quotient * permille + (remainder * permille) / 1_000L);
    }

    private static void positive(long value, String name) {
        if (value < 1) throw new IllegalArgumentException(name + " must be positive");
    }
}
