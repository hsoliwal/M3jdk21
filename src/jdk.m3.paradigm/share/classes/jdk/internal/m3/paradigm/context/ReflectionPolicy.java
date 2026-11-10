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
 * Fail-closed limits and observation policy for Java-object to context-tree reflection.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextReflectionPolicy.java}
 * blob {@code 2062de9fc40ec1c6a923ca5c5cd0454918407c80} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record ReflectionPolicy(
        int maxDepth,
        int maxNodes,
        int maxCollectionElements,
        int maxToStringChars,
        boolean includeFields,
        boolean includeMethods,
        boolean includeConstructors,
        boolean includeAnnotations,
        boolean includeStaticFields,
        boolean includeSynthetic,
        boolean sampleToString) {

    public ReflectionPolicy {
        if (maxDepth < 0
                || maxNodes <= 0
                || maxCollectionElements < 0
                || maxToStringChars < 0) {
            throw new IllegalArgumentException("invalid reflection mapping limits");
        }
    }

    public static ReflectionPolicy safeDefaults() {
        return new ReflectionPolicy(
                12,
                10_000,
                1_024,
                4_096,
                true,
                true,
                true,
                true,
                false,
                false,
                false);
    }

    public ReflectionPolicy withToStringSampling(int maxChars) {
        if (maxChars < 0) {
            throw new IllegalArgumentException("maxChars");
        }
        return new ReflectionPolicy(
                maxDepth,
                maxNodes,
                maxCollectionElements,
                maxChars,
                includeFields,
                includeMethods,
                includeConstructors,
                includeAnnotations,
                includeStaticFields,
                includeSynthetic,
                true);
    }
}
