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

import jdk.internal.m3.paradigm.annotation.IFieldSpec;
import java.util.Objects;
import java.util.function.Function;

/**
 * TriFunction - Functional interface for three-argument functions.
 *
 * <h2>Extends Java's Functional Interface Family</h2>
 *
 * <pre>
 * Java provides:
 *   Function&lt;T, R&gt;         - 1 arg
 *   BiFunction&lt;T, U, R&gt;    - 2 args
 *
 * IOP adds:
 *   TriFunction&lt;A, B, C, R&gt; - 3 args
 *
 * For more args, use records/tuples as input.
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 *
 * // Three-source computed field
 * class AddressComputer implements TriFunction<String, String, String, String> {
 *
 * 	public String apply(String street, String city, String country) {
 * 		return street + ", " + city + ", " + country;
 * 	}
 * }
 *
 * @IFieldSpec(triFunction = AddressComputer.class, sources = {"street", "city", "country"})
 * private String fullAddress;
 * }</pre>
 *
 * @param <A>
 *            type of first argument
 * @param <B>
 *            type of second argument
 * @param <C>
 *            type of third argument
 * @param <R>
 *            type of result
 *
 * @since IOP 1.0 - Functional Extensions
 * @m3.pattern gof:strategy
 * @m3.algorithm Dispatch three ordered arguments to the implementing function
 * @m3.dataStructures Three generic input references and one generic result
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/TriFunction.java}
 * blob {@code 7ae562e6ba2d521b3c15dccb0801554e572f7c55} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@FunctionalInterface
public interface TriFunction<A, B, C, R> {

    /**
     * Apply this function to the given arguments.
     *
     * @param a
     *            first argument
     * @param b
     *            second argument
     * @param c
     *            third argument
     * @return result
     * @m3.pattern gof:strategy
     * @m3.algorithm The implementation supplies evaluation for the three arguments
     * @m3.dataStructures Three caller supplied references and the returned result
     */
    R apply(A a, B b, C c);

    /**
     * Returns a composed function that first applies this function, then applies
     * the after function to the result.
     *
     * @param <V>
     *            type of output of the after function
     * @param after
     *            function to apply after this function
     * @return composed function
     * @m3.pattern gof:strategy
     * @m3.algorithm Require a nonnull continuation then apply this function before the continuation
     * @m3.dataStructures Captured function and continuation references in a composed lambda
     */
    default <V> TriFunction<A, B, C, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (a, b, c) -> after.apply(apply(a, b, c));
    }
}
