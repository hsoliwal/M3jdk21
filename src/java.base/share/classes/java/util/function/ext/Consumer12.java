/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

package java.util.function.ext;

/**
 * A consumer with the shape {@code (reference, reference, reference, reference, reference,
 * reference, reference, reference, reference, reference, reference, reference) to void}.
 *
 * <p>Primitive positions are primitive in the functional method. Reference arguments and results
 * may be null unless the implementation imposes a restriction. Invocations are caller-driven; this
 * interface implies no scheduling, caching, serialization, purity, resource ownership, or
 * thread-safety guarantee.
 *
 * This is a functional interface whose functional method is {@code accept}.
 *
 * @param <T1> an input type
 * @param <T2> an input type
 * @param <T3> an input type
 * @param <T4> an input type
 * @param <T5> an input type
 * @param <T6> an input type
 * @param <T7> an input type
 * @param <T8> an input type
 * @param <T9> an input type
 * @param <U1> an input type
 * @param <U2> an input type
 * @param <U3> an input type
 * @since M3JDK 21
 */
@FunctionalInterface
public interface Consumer12<T1, T2, T3, T4, T5, T6, T7, T8, T9, U1, U2, U3> {
    /**
     * Performs this operation on the supplied arguments.
     *
     * @param value1 the argument at position 1
     * @param value2 the argument at position 2
     * @param value3 the argument at position 3
     * @param value4 the argument at position 4
     * @param value5 the argument at position 5
     * @param value6 the argument at position 6
     * @param value7 the argument at position 7
     * @param value8 the argument at position 8
     * @param value9 the argument at position 9
     * @param value10 the argument at position 10
     * @param value11 the argument at position 11
     * @param value12 the argument at position 12
     */
    void accept(
            T1 value1,
            T2 value2,
            T3 value3,
            T4 value4,
            T5 value5,
            T6 value6,
            T7 value7,
            T8 value8,
            T9 value9,
            U1 value10,
            U2 value11,
            U3 value12);

    /**
     * Returns an operation that invokes this operation, then {@code after}.
     * <p>No operation runs until the returned operation is invoked. If this operation throws,
     * {@code after} is not invoked. The original throwable is relayed.
     *
     * @param after the operation to invoke second
     * @return the composed operation
     * @throws NullPointerException if {@code after} is null
     */
    default Consumer12<T1, T2, T3, T4, T5, T6, T7, T8, T9, U1, U2, U3> andThen(
            Consumer12<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ?
                    super T7, ? super T8, ? super T9, ? super U1, ? super U2, ? super U3> after) {
        java.util.Objects.requireNonNull(after);
        return (value1, value2, value3, value4, value5, value6, value7, value8, value9, value10,
                value11, value12) -> {
            accept(value1, value2, value3, value4, value5, value6, value7, value8, value9, value10,
                    value11, value12);
            after.accept(value1, value2, value3, value4, value5, value6, value7, value8, value9,
                    value10, value11, value12);
        };
    }
}
