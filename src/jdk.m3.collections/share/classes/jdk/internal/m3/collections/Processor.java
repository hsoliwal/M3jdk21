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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Functional interface for processing operations with validation.
 *
 * <p>
 * Extends basic transformation with validation, filtering, and error handling
 * capabilities.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 *
 * Processor<String, Integer> parser = Integer::parseInt;
 *
 * // With validation
 * int result = parser.processValidated(str, s -> s.matches("\\d+"));
 *
 * // Safe processing
 * int safe = parser.processSafe("invalid", 0); // returns 0
 *
 * // Nullable
 * Optional<Integer> opt = parser.processNullable(nullableStr);
 * }</pre>
 *
 * @param <T>
 *            input type
 * @param <R>
 *            output type
 * @since ContextOS 1.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/Processor.java}
 * blob {@code 05ce3375f7ea06c16417ebef6c44f07877aea59f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@FunctionalInterface
public interface Processor<T, R> {

    /**
     * Processes the input and returns the result.
     *
     * @param input
     *            the input to process
     * @return the processed result
     * @throws ProcessingException
     *             if processing fails
     */
    R process(T input);

    // --- Default Methods - Validation & Filtering ---

    /**
     * Processes input with validation.
     *
     * @param input
     *            the input to process
     * @param validator
     *            predicate to validate input
     * @return processed result
     * @throws IllegalArgumentException
     *             if validation fails
     */
    default R processValidated(T input, Predicate<T> validator) {
        if (!validator.test(input)) {
            throw new IllegalArgumentException("Input validation failed");
        }
        return process(input);
    }

    /**
     * Processes input, returning Optional.empty() on null input.
     *
     * @param input
     *            the input to process (may be : null)
     * @return Optional containing result, or empty if input was null
     */
    default Optional<R> processNullable(T input) {
        return Optional.ofNullable(input).map(this::process);
    }

    /**
     * Processes input with fallback on exception.
     *
     * @param input
     *            the input to process
     * @param fallback
     *            value to return on exception
     * @return processed result or fallback
     */
    default R processSafe(T input, R fallback) {
        try {
            return process(input);
        } catch (Exception e) {
            return fallback;
        }
    }

    /**
     * Processes a list of inputs, filtering nulls.
     *
     * @param inputs
     *            list of inputs
     * @return list of processed non-null results
     */
    default List<R> processAll(List<T> inputs) {
        return inputs.stream()
                .filter(Objects::nonNull)
                .map(this::process)
                .filter(Objects::nonNull)
                .toList();
    }

    // --- Default Methods - Chaining ---

    /**
     * Chains another processor after this one.
     *
     * @param after
     *            processor to apply after
     * @param <V>
     *            final output type
     * @return chained processor
     */
    default <V> Processor<T, V> andThen(Processor<R, V> after) {
        return input -> after.process(this.process(input));
    }

    // --- Static Factory Methods ---

    /**
     * Creates an identity processor.
     *
     * @param <T>
     *            type
     * @return identity processor
     */
    static <T> Processor<T, T> identity() {
        return input -> input;
    }

    /**
     * Creates a processor that always throws.
     *
     * @param message
     *            error message
     * @param <T>
     *            input type
     * @param <R>
     *            output type
     * @return failing processor
     */
    static <T, R> Processor<T, R> failing(String message) {
        return input -> {
            throw new ProcessingException(message);
        };
    }

    /** Exception for processing failures. */
    class ProcessingException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        /**
         * Creates a ProcessingException with a message.
         *
         * @param message
         *            the error message
         */
        public ProcessingException(String message) {
            super(message);
        }

        /**
         * Creates a ProcessingException with a message and cause.
         *
         * @param message
         *            the error message
         * @param cause
         *            the underlying cause
         */
        public ProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
