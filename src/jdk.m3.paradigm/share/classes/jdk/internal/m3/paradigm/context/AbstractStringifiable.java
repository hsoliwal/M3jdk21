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

import jdk.internal.m3.collections.PlaceholderLogger;

import java.util.stream.Stream;

/**
 * Template Method base for {@link IStringifiable}.
 *
 * <p>
 * Delegates string production to {@code doLines()} and {@code doStringify()}
 * with lifecycle hooks.
 *
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/AbstractStringifiable.java}
 * blob {@code da82c1b2ada8320d74311fe9b34a8c2a9f52d46d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractStringifiable implements IStringifiable {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AbstractStringifiable.class);

    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public AbstractStringifiable() {
    }


    // --- TEMPLATE METHODS ---

    @Override
    public Stream<String> lines() {
        onBeforeLines();
        Stream<String> result = doLines();
        onAfterLines(result);
        return result;
    }

    @Override
    public String stringify() {
        onBeforeStringify();
        String result = doStringify();
        onAfterStringify(result);
        return result;
    }

    // --- ABSTRACT doXxx() - SUBCLASS PROVIDES ---

    protected abstract Stream<String> doLines();

    protected abstract String doStringify();

    // --- CALLBACK HOOKS - onXxx() ---

    protected void onBeforeLines() {}

    protected void onAfterLines(Stream<String> result) {}

    protected void onBeforeStringify() {}

    protected void onAfterStringify(String result) {}
}
