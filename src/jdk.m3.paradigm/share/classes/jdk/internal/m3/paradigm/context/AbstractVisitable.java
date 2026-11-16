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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Template Method base for {@link IVisitable}.
 *
 * <p>
 * Delegates visitation to {@code doVisit()} and {@code doAccept()} with
 * lifecycle hooks.
 *
 * @param <Self>
 *            self type for double dispatch
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/AbstractVisitable.java}
 * blob {@code d77880b9c863466539dedfa5919e3461603f3f05} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractVisitable<Self extends AbstractVisitable<Self>>
        implements IVisitable<Self> {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AbstractVisitable.class);

    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public AbstractVisitable() {
    }


    // --- TEMPLATE METHODS ---

    @Override
    public <T> Stream<T> visit() {
        onBeforeVisit();
        Stream<T> result = doVisit();
        onAfterVisit(result);
        return result;
    }

    @Override
    public <T, R> Stream<R> visit(Function<T, R> mapper) {
        onBeforeVisitMapped();
        Stream<R> result = doVisitMapped(mapper);
        onAfterVisitMapped(result);
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R accept(Function<Self, R> visitor) {
        onBeforeAccept();
        R result = visitor.apply((Self) this);
        onAfterAccept(result);
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void acceptVoid(Consumer<Self> visitor) {
        onBeforeAccept();
        visitor.accept((Self) this);
        onAfterAcceptVoid();
    }

    // --- ABSTRACT doXxx() - SUBCLASS PROVIDES ---

    protected abstract <T> Stream<T> doVisit();

    protected abstract <T, R> Stream<R> doVisitMapped(Function<T, R> mapper);

    // --- CALLBACK HOOKS - onXxx() ---

    protected void onBeforeVisit() {}

    protected <T> void onAfterVisit(Stream<T> result) {}

    protected void onBeforeVisitMapped() {}

    protected <R> void onAfterVisitMapped(Stream<R> result) {}

    protected void onBeforeAccept() {}

    protected <R> void onAfterAccept(R result) {}

    protected void onAfterAcceptVoid() {}
}
