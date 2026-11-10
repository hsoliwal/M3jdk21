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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Service-provider contract for deterministic or model-backed semantic first passes.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextSemanticProvider.java}
 * blob {@code 18083846feeda095532b9d0e59a21073b3f383b0} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface SemanticProvider {
    String name();

    default int priority() {
        return 0;
    }

    SemanticFeatures analyze(String text);

    default int distancePpm(
            SemanticFeatures left,
            SemanticFeatures right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Set<String> a = new HashSet<>(left.stems());
        Set<String> b = new HashSet<>(right.stems());
        if (a.isEmpty() && b.isEmpty()) return 0;

        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        a.retainAll(b);
        long similarity = 1_000_000L * a.size() / Math.max(1, union.size());
        return (int) (1_000_000L - similarity);
    }
}
