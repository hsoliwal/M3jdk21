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

import java.util.Comparator;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Exactly one process-wide semantic first-pass provider.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextSemantic.java}
 * blob {@code 1e84750343b263f21c9337daf38c0b5115c519c9} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class Semantic {
    private static final AtomicReference<SemanticProvider> GLOBAL =
            new AtomicReference<>(discover());

    private Semantic() {
    }

    public static SemanticProvider provider() {
        return GLOBAL.get();
    }

    public static SemanticProvider install(SemanticProvider provider) {
        return GLOBAL.getAndSet(Objects.requireNonNull(provider, "provider"));
    }

    public static SemanticProvider reload() {
        SemanticProvider discovered = discover();
        GLOBAL.set(discovered);
        return discovered;
    }

    public static SemanticFeatures analyze(String text) {
        return ContextImmutablePool.global().intern(provider().analyze(text));
    }

    public static int distancePpm(String left, String right) {
        SemanticProvider provider = provider();
        return provider.distancePpm(provider.analyze(left), provider.analyze(right));
    }

    private static SemanticProvider discover() {
        return ServiceLoader.load(SemanticProvider.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .max(Comparator.comparingInt(SemanticProvider::priority)
                        .thenComparing(SemanticProvider::name))
                .orElse(LexicalSemanticProvider.INSTANCE);
    }
}
