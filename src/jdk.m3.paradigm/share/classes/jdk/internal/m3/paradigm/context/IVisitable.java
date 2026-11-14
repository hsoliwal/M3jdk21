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

import jdk.internal.m3.paradigm.annotation.ISpec;
import jdk.internal.m3.paradigm.annotation.Model;
import jdk.internal.m3.paradigm.annotation.Stability;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Contract for types that can accept visitors and produce streams.
 *
 * <p>From ContextOS: "Everything is visitable."
 *
 * <h2>Unix Model</h2>
 * <ul>
 *   <li>visit() - traverse and produce stream</li>
 *   <li>accept(fn) - double-dispatch</li>
 *   <li>pipe-compatible via stream output</li>
 * </ul>
 *
 * @param <Self> The self-type for type-safe dispatch
 * @since ContextOS 1.0
 */
@ISpec(
    value = "Contract for visiting with stream output - Unix pipe compatible",
    constraints = {
        "visit() produces stream of visited elements",
        "accept() passes self to visitor function",
        "Type-safe dispatch via Self-type"
    },
    invariants = {
        "visit() returns non-null stream",
        "accept(fn) returns fn.apply(self)"
    },
    examples = {
        "context.visit().forEach(System.out::println)",
        "context.accept(ctx -> ctx.id())"
    },
    model = Model.ModelType.HAIKU,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/IVisitable.java}
 * blob {@code 108371c609b49d836b1654fffd0c1bd1c58cbd2d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IVisitable<Self extends IVisitable<Self>> {

    /**
     * Visit and produce a stream of elements.
     *
     * @param <T> Element type
     * @return Stream of visited elements
     */
    @SuppressWarnings("unchecked")
    default <T> Stream<T> visit() {
        return Stream.of((T) this);
    }

    /**
     * Visit with a mapper function, producing stream.
     *
     * @param mapper Function to apply to each visited element
     * @param <T>    Input type
     * @param <R>    Result type
     * @return Stream of mapped results
     */
    @SuppressWarnings("unchecked")
    default <T, R> Stream<R> visit(Function<T, R> mapper) {
        return this.<T>visit().map(mapper);
    }

    /**
     * Accept a visitor function (double-dispatch).
     *
     * @param <R>     result type
     * @param visitor function to apply to this
     * @return visitor result
     */
    @SuppressWarnings("unchecked")
    default <R> R accept(Function<Self, R> visitor) {
        return visitor.apply((Self) this);
    }

    /**
     * Accept a void visitor (side-effect only).
     *
     * @param visitor consumer to apply to this
     */
    @SuppressWarnings("unchecked")
    default void acceptVoid(Consumer<Self> visitor) {
        visitor.accept((Self) this);
    }
}
