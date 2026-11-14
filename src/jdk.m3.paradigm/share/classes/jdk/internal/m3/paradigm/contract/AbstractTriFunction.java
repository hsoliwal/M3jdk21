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
package jdk.internal.m3.paradigm.contract;

import jdk.internal.m3.collections.PlaceholderLogger;

/**
 * Template Method base for {@link TriFunction}.
 *
 * <p>
 * Delegates function application to {@code doApply()} with lifecycle hooks.
 *
 * @param <A>
 *            type of first argument
 * @param <B>
 *            type of second argument
 * @param <C>
 *            type of third argument
 * @param <R>
 *            type of result
 * @since 1.0.0
 * @m3.pattern gof:template-method
 * @m3.algorithm Sequence before hook then subclass evaluation then after hook on normal return
 * @m3.dataStructures Three generic arguments and the evaluation result
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AbstractTriFunction.java}
 * blob {@code 03a797bf748d03c7f3110059c546cefbad26bf4c} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractTriFunction<A, B, C, R> implements TriFunction<A, B, C, R> {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AbstractTriFunction.class);

    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public AbstractTriFunction() {
    }


    // --- TEMPLATE METHODS ---

    /**
     * Runs hooks in order and returns the subclass result without catching failures.
     * @m3.pattern gof:template-method
     * @m3.algorithm Invoke before then doApply then after exactly once on normal completion
     * @m3.dataStructures Input references and one local result reference
     */
    @Override
    public final R apply(A a, B b, C c) {
        onBeforeApply(a, b, c);
        R result = doApply(a, b, c);
        onAfterApply(a, b, c, result);
        return result;
    }

    // --- ABSTRACT doXxx() - SUBCLASS PROVIDES ---

    /**
     * Supplies the evaluation step used by the final application template.
     * @m3.pattern gof:template-method
     * @m3.algorithm Subclass defined evaluation dispatched once from apply
     * @m3.dataStructures Three input references and the returned result
     */
    protected abstract R doApply(A a, B b, C c);

    // --- CALLBACK HOOKS - onXxx() ---

    /**
     * Provides an empty overridable hook before subclass evaluation.
     * @m3.pattern gof:template-method
     * @m3.algorithm Return immediately unless overridden by a subclass
     * @m3.dataStructures Three argument references without retained local storage
     */
    protected void onBeforeApply(A a, B b, C c) {}

    /**
     * Provides an empty overridable hook after successful subclass evaluation.
     * @m3.pattern gof:template-method
     * @m3.algorithm Return immediately unless overridden by a subclass
     * @m3.dataStructures Three input references and the computed result reference
     */
    protected void onAfterApply(A a, B b, C c, R result) {}
}
