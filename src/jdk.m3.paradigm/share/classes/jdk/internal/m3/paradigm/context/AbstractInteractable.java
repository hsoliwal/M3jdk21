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
 * Template Method base for {@link IInteractable}.
 *
 * <p>
 * Delegates interaction to {@code doInteract()} with lifecycle hooks.
 *
 * @param <Self>
 *            self type for fluent API
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/AbstractInteractable.java}
 * blob {@code ac307977856bb68ac310b6216d1c78b4ec5f50d0} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractInteractable<Self extends AbstractInteractable<Self>>
        implements IInteractable<Self> {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AbstractInteractable.class);

    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public AbstractInteractable() {
    }


    // --- TEMPLATE METHODS ---

    @Override
    public <T> Stream<T> interact() {
        onBeforeInteract();
        Stream<T> result = doInteract();
        onAfterInteract(result);
        return result;
    }

    @Override
    public <T, O> Stream<T> interact(O other) {
        onBeforeInteractWith(other);
        Stream<T> result = doInteractWith(other);
        onAfterInteractWith(other, result);
        return result;
    }

    // --- ABSTRACT doXxx() - SUBCLASS PROVIDES ---

    protected abstract <T> Stream<T> doInteract();

    protected abstract <T, O> Stream<T> doInteractWith(O other);

    // --- CALLBACK HOOKS - onXxx() ---

    protected void onBeforeInteract() {}

    protected <T> void onAfterInteract(Stream<T> result) {}

    protected <O> void onBeforeInteractWith(O other) {}

    protected <T, O> void onAfterInteractWith(O other, Stream<T> result) {}
}
