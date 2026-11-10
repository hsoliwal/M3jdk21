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

import java.util.stream.Stream;

/**
 * Contract for types that can interact - stream-in, stream-out.
 *
 * <p>From ContextOS: "Everything interacts through streams."
 *
 * <h2>Interaction Model</h2>
 * <ul>
 *   <li>interact() - produce stream from self</li>
 *   <li>interact(other) - interact with another, produce stream</li>
 *   <li>pipe(next) - chain interactions: a | b</li>
 *   <li>connect(other) - bidirectional: a &lt;-&gt; b</li>
 * </ul>
 *
 * @param <Self> The self-type for fluent returns
 * @since ContextOS 1.0
 */
@ISpec(
    value = "Contract for stream-based interaction between contexts",
    constraints = {
        "Interactions always return non-null streams",
        "Streams are lazy and composable",
        "Pipes chain: output of A becomes input of B"
    },
    invariants = {
        "interact() never returns null",
        "pipe() never returns null"
    },
    examples = {
        "context.interact()",
        "context.interact(other)",
        "a.pipe(b).interact()"
    },
    model = Model.ModelType.HAIKU,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/IInteractable.java}
 * blob {@code ca020fea56d126d779e4bb78032ee3d194ec35aa} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IInteractable<Self> {

    /**
     * Interact and produce a stream from self.
     *
     * @param <T> Element type
     * @return Stream of elements
     */
    default <T> Stream<T> interact() {
        return Stream.empty();
    }

    /**
     * Interact with another entity.
     *
     * @param other The entity to interact with
     * @param <T>   Element type
     * @param <O>   Other entity type
     * @return Stream of interaction results
     */
    default <T, O> Stream<T> interact(O other) {
        return Stream.empty();
    }

    /**
     * Pipe to another interactable: a | b
     *
     * <p>Output of this becomes input to next.
     *
     * @param next Next stage
     * @param <N>  Type of next
     * @return Combined pipeline
     */
    default <N extends IInteractable<N>> IInteractable<?> pipe(N next) {
        IInteractable<Self> self = this;
        return new IInteractable<>() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Stream<T> interact() {
                return self.interact()
                        .flatMap(e -> (Stream<T>) next.interact(e));
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T, O> Stream<T> interact(O other) {
                return self.interact(other)
                        .flatMap(e -> (Stream<T>) next.interact(e));
            }
        };
    }

    /**
     * Connect bidirectionally: a &lt;-&gt; b
     *
     * @param other The other interactable
     * @param <O>   Type of other
     * @return Bidirectional connection
     */
    default <O extends IInteractable<O>> Connection<Self, O> connect(O other) {
        return new Connection<>(this, other);
    }

    /** Bidirectional connection between two interactables. */
    record Connection<A, B>(IInteractable<A> left, IInteractable<B> right) {

        /** Flow left → right. */
        @SuppressWarnings("unchecked")
        public <T> Stream<T> leftToRight() {
            return left.interact()
                    .flatMap(e -> (Stream<T>) right.interact(e));
        }

        /** Flow right → left. */
        @SuppressWarnings("unchecked")
        public <T> Stream<T> rightToLeft() {
            return right.interact()
                    .flatMap(e -> (Stream<T>) left.interact(e));
        }

        /** Flow both directions. */
        public <T> Stream<T> flow() {
            return Stream.concat(leftToRight(), rightToLeft());
        }
    }
}
