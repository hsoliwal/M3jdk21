/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

package java.util.function.ext;

/**
 * A function with the shape {@code (reference, boolean) to float}.
 *
 * <p>Primitive positions are primitive in the functional method. Reference arguments and results
 * may be null unless the implementation imposes a restriction. Invocations are caller-driven; this
 * interface implies no scheduling, caching, serialization, purity, resource ownership, or
 * thread-safety guarantee.
 *
 * This is a functional interface whose functional method is {@code applyAsFloat}.
 *
 * @param <T> an input type
 * @since M3JDK 21
 */
@FunctionalInterface
public interface ObjBooleanToFloatFunction<T> {
    /**
     * Applies this function to the supplied arguments.
     *
     * @param value1 the argument at position 1
     * @param value2 the argument at position 2
     * @return the result
     */
    float applyAsFloat(T value1, boolean value2);
}
