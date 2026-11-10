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

import java.util.function.Supplier;

/**
 * Proceed - Layer Chain Continuation (COP Pattern).
 *
 * <p>Proceed represents the continuation in a layer chain, allowing
 * layers to execute the next layer in the stack. This is the core
 * COP pattern from ContextL/ContextR/ContextJS.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // In a layer's around advice:
 * Proceed<Integer> proceed = Proceed.from(() -> next.apply(value));
 * Integer result = proceed.invoke();
 *
 * // Constant proceed (skip chain)
 * Proceed<String> skip = Proceed.constant("cached");
 * }</pre>
 *
 * @param <T> result type
 * @since COP Integration - ContextR/ContextL/ContextJS patterns
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/layer/Proceed.java}
 * blob {@code 56edd3c257361998525d8ca9e730f42b31c46eff} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@FunctionalInterface
public interface Proceed<T> {

    /**
     * Invoke the continuation.
     *
     * @return result of the continuation
     */
    T invoke();

    /**
     * Create a proceed from a supplier.
     *
     * @param supplier the supplier
     * @param <T>      result type
     * @return proceed
     */
    static <T> Proceed<T> from(Supplier<T> supplier) {
        return supplier::get;
    }

    /**
     * Create a proceed that returns a constant value.
     *
     * @param value the constant
     * @param <T>   result type
     * @return proceed
     */
    static <T> Proceed<T> constant(T value) {
        return () -> value;
    }

    /**
     * Create a proceed that throws an exception.
     *
     * @param exception the exception to throw
     * @param <T>       result type
     * @return proceed
     */
    static <T> Proceed<T> failing(RuntimeException exception) {
        return () -> {
            throw exception;
        };
    }

    /**
     * Map the result of this proceed.
     *
     * @param mapper the mapping function
     * @param <R>    new result type
     * @return new proceed
     */
    default <R> Proceed<R> map(java.util.function.Function<T, R> mapper) {
        return () -> mapper.apply(invoke());
    }

    /**
     * Flat-map the result of this proceed.
     *
     * @param mapper the mapping function
     * @param <R>    new result type
     * @return new proceed
     */
    default <R> Proceed<R> flatMap(java.util.function.Function<T, Proceed<R>> mapper) {
        return () -> mapper.apply(invoke()).invoke();
    }
}
