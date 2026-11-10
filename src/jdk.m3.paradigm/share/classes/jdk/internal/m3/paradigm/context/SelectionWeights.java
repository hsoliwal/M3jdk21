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

/**
 * Integer-only weights for deterministic skill ranking.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/cognition/SelectionWeights.java}
 * blob {@code ca36b42630c57dbae1dac9a2974fa7750e4c10ce} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record SelectionWeights(
        int semantic,
        int efficiency,
        int cost,
        int taskSize,
        int latency,
        int memory,
        int determinism) {

    public SelectionWeights {
        check(semantic, "semantic");
        check(efficiency, "efficiency");
        check(cost, "cost");
        check(taskSize, "taskSize");
        check(latency, "latency");
        check(memory, "memory");
        check(determinism, "determinism");
        if (semantic + efficiency + cost + taskSize + latency + memory + determinism == 0) {
            throw new IllegalArgumentException("at least one selection weight is required");
        }
    }

    public static SelectionWeights balanced() {
        return new SelectionWeights(300, 150, 150, 100, 100, 100, 100);
    }

    public int total() {
        return semantic + efficiency + cost + taskSize + latency + memory + determinism;
    }

    public String root() {
        return SemanticHash.sha256("COP-SELECTION-WEIGHTS/1",
                Integer.toString(semantic), Integer.toString(efficiency), Integer.toString(cost),
                Integer.toString(taskSize), Integer.toString(latency), Integer.toString(memory),
                Integer.toString(determinism));
    }

    private static void check(int value, String name) {
        if (value < 0 || value > 1_000) throw new IllegalArgumentException(name + " must be 0..1000");
    }
}
