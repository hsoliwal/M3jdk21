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

import java.util.List;
import java.util.function.Function;

/**
 * Functional interface for data transformation operations.
 *
 * <p>
 * Provides a standard contract for transforming input data to output, with
 * default methods for chaining and batch operations.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 *
 * Transformer<String, Integer> parser = Integer::parseInt;
 * var result = parser.andThen(x -> x * 2).transform("5"); // 10
 *
 * List<Integer> results = parser.transformAll(List.of("1", "2", "3"));
 * }</pre>
 *
 * @param <T>
 *            input type
 * @param <R>
 *            output type
 * @since ContextOS 1.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/Transformer.java}
 * blob {@code 5d47daf44af1d96bebaf5bd1b47578ec031abdbd} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@FunctionalInterface
public interface Transformer<T, R> {

    /**
     * Transforms the input to output.
     *
     * @param input
     *            the input to transform
     * @return the transformed output
     */
    R transform(T input);

    // --- Default Methods - Chainable Operations ---

    /**
     * Chains another transformer after this one.
     *
     * @param after
     *            transformer to apply after this one
     * @param <V>
     *            final output type
     * @return chained transformer
     */
    default <V> Transformer<T, V> andThen(Transformer<R, V> after) {
        return input -> after.transform(this.transform(input));
    }

    /**
     * Composes another transformer before this one.
     *
     * @param before
     *            transformer to apply before this one
     * @param <V>
     *            initial input type
     * @return composed transformer
     */
    default <V> Transformer<V, R> compose(Transformer<V, T> before) {
        return input -> this.transform(before.transform(input));
    }

    /**
     * Transforms a list of inputs.
     *
     * @param inputs
     *            list of inputs
     * @return list of transformed outputs
     */
    default List<R> transformAll(List<T> inputs) {
        return inputs.stream().map(this::transform).toList();
    }

    // --- Static Factory Methods ---

    /**
     * Creates an identity transformer that returns input unchanged.
     *
     * @param <T>
     *            type
     * @return identity transformer
     */
    static <T> Transformer<T, T> identity() {
        return input -> input;
    }

    /**
     * Creates a transformer from a Function.
     *
     * @param function
     *            the function to wrap
     * @param <T>
     *            input type
     * @param <R>
     *            output type
     * @return transformer wrapping the function
     */
    static <T, R> Transformer<T, R> from(Function<T, R> function) {
        return function::apply;
    }

    /**
     * Creates a constant transformer that always returns the same value.
     *
     * @param value
     *            constant value to return
     * @param <T>
     *            input type (ignored)
     * @param <R>
     *            output type
     * @return constant transformer
     */
    static <T, R> Transformer<T, R> constant(R value) {
        return input -> value;
    }
}
