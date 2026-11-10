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

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Immutable resolver inventory: specificity, descending priority, stable ID; no arbitrary expression evaluation.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/grail/LayerResolver.java}
 * blob {@code 868024332eacde4b472e4c752520c9430e69ee29} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LayerResolver<T> {
    public enum Composition { CHAIN, FIRST }
    public record Layer<T>(String id, String joinPoint, ContextGuard guard, int priority,
                           Composition composition, BiFunction<T, ContextVector, T> effect) {
        public Layer {
            Objects.requireNonNull(id, "id"); Objects.requireNonNull(joinPoint, "joinPoint");
            Objects.requireNonNull(guard, "guard"); Objects.requireNonNull(composition, "composition");
            Objects.requireNonNull(effect, "effect");
            if (id.isBlank() || joinPoint.isBlank()) throw new IllegalArgumentException("Blank layer identifier");
        }
    }
    public record Decision<T>(String joinPoint, ContextVector context, T input, T output,
                              List<String> matched, List<String> executed) {
        public Decision { matched = List.copyOf(matched); executed = List.copyOf(executed); }
    }
    private final List<Layer<T>> layers;
    public LayerResolver(List<Layer<T>> layers) {
        this.layers = List.copyOf(layers);
        if (layers.stream().map(layer -> List.of(layer.joinPoint(), layer.id())).distinct().count() != layers.size())
            throw new IllegalArgumentException("Duplicate layer ID within join point");
    }
    /** Preserve all existing layers; conflicting IDs fail rather than silently overwriting behavior. */
    public LayerResolver<T> plus(Layer<T> layer) {
        return new LayerResolver<>(java.util.stream.Stream.concat(layers.stream(), java.util.stream.Stream.of(layer)).toList());
    }
    public Decision<T> resolve(String joinPoint, T input, ContextVector context) {
        Objects.requireNonNull(joinPoint, "joinPoint"); Objects.requireNonNull(context, "context");
        var matched = layers.stream().filter(layer -> layer.joinPoint().equals(joinPoint))
                .filter(layer -> layer.guard().matches(context)).toList();
        int specificity = matched.stream().mapToInt(layer -> layer.guard().specificity()).max().orElse(-1);
        var selected = matched.stream().filter(layer -> layer.guard().specificity() == specificity)
                .sorted(Comparator.<Layer<T>>comparingInt(Layer::priority).reversed().thenComparing(Layer::id)).toList();
        int first = java.util.stream.IntStream.range(0, selected.size())
                .filter(i -> selected.get(i).composition() == Composition.FIRST).findFirst().orElse(selected.size() - 1);
        var executed = selected.stream().limit(first + 1L).toList();
        if (executed.size() > 256) throw new IllegalStateException("Layer execution budget exceeds 256");
        // Compose functions, not a mutable accumulator hidden in a stream or an invalid parallel reduction.
        java.util.function.UnaryOperator<T> identity = value -> value;
        var chain = executed.stream().<java.util.function.UnaryOperator<T>>map(layer -> value -> layer.effect().apply(value, context))
                .reduce(identity, (left, right) -> value -> right.apply(left.apply(value)));
        return new Decision<>(joinPoint, context, input, chain.apply(input),
                matched.stream().map(Layer::id).toList(), executed.stream().map(Layer::id).toList());
    }
}
