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
import java.util.function.Function;

/**
 * Streams resource scopes into one antichain without temporary per-scope containers.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/PrefixResourceReducer.java}
 * blob {@code c5d22b5e477b467c49454b155902aa43d5236fb4} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PrefixResourceReducer {
    private M3PrefixResourceReducer() { }
    /** Returns admissions, including parent replacements; extractor failure preserves prior admissions. */
    public static <E> int addResources(M3ShapePrefixSet<E> target, Iterable<? extends E> resources,
                                      Function<? super E, String> path) {
        Objects.requireNonNull(target, "target"); Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(path, "path"); int admitted = 0;
        for (E resource : resources) { if (target.add(path.apply(resource), resource)) { admitted++; } }
        return admitted;
    }
}
