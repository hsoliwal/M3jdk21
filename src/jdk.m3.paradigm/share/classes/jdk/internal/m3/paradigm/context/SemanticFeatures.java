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

import java.util.List;
import java.util.Objects;

/**
 * Immutable candidate evidence used for fast sorting/indexing before heavier inference.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextSemanticFeatures.java}
 * blob {@code e3aa726b2b12af750ee8ee34d5b4be134d9e153c} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record SemanticFeatures(
        String normalized,
        List<String> tokens,
        List<String> stems,
        String sortKey,
        long fingerprint)
        implements Immutable {
    public SemanticFeatures {
        normalized = Objects.requireNonNull(normalized, "normalized");
        tokens = List.copyOf(Objects.requireNonNull(tokens, "tokens"));
        stems = List.copyOf(Objects.requireNonNull(stems, "stems"));
        sortKey = Objects.requireNonNull(sortKey, "sortKey");
    }
}
