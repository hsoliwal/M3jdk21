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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Conjunction of named constraints; exclusions never overwrite positive constraints.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/grail/ContextGuard.java}
 * blob {@code 56708468f8c33140bc43c1a76925995df9f3fe5e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record ContextGuard(List<Atom> atoms) {
    public record Atom(String dimension, String description, BiPredicate<Object, ContextVector> predicate) {
        public Atom {
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(predicate, "predicate");
            if (dimension.isBlank()) throw new IllegalArgumentException("Blank dimension");
        }
        public boolean matches(ContextVector context) {
            return predicate.test(context.values().get(dimension), context);
        }
    }
    public ContextGuard { atoms = List.copyOf(atoms); }
    public static ContextGuard always() { return new ContextGuard(List.of()); }
    public static ContextGuard of(Atom... atoms) { return new ContextGuard(Arrays.asList(atoms)); }
    public boolean matches(ContextVector context) { return atoms.stream().allMatch(atom -> atom.matches(context)); }
    public int specificity() { return Math.toIntExact(atoms.stream().map(Atom::dimension).distinct().count()); }
    public List<String> failures(ContextVector context) {
        return atoms.stream().filter(atom -> !atom.matches(context)).map(Atom::description).toList();
    }
    public ContextGuard and(ContextGuard other) {
        return new ContextGuard(java.util.stream.Stream.concat(atoms.stream(), other.atoms.stream()).toList());
    }
    public static Atom eq(String dimension, Object expected) {
        Object immutable = new ContextVector(java.util.Collections.singletonMap(dimension, expected)).values().get(dimension);
        return new Atom(dimension, dimension + " == " + immutable,
                (value, context) -> context.values().containsKey(dimension) && Objects.equals(value, immutable));
    }
    public static Atom anyOf(String dimension, Object... expected) {
        var copied = new ContextVector(java.util.Map.of("values", Arrays.asList(expected))).values().get("values");
        List<?> values = (List<?>) copied;
        return new Atom(dimension, dimension + " in " + values,
                (value, context) -> context.values().containsKey(dimension) && values.contains(value));
    }
    public static Atom between(String dimension, BigDecimal low, BigDecimal high, boolean inclusive) {
        Objects.requireNonNull(low, "low"); Objects.requireNonNull(high, "high");
        if (low.compareTo(high) > 0) throw new IllegalArgumentException("Reversed interval");
        return new Atom(dimension, dimension + " between " + low + " and " + high, (value, context) -> {
            if (!(value instanceof Number number)) return false;
            var numeric = new BigDecimal(number.toString());
            int lo = numeric.compareTo(low), hi = numeric.compareTo(high);
            return inclusive ? lo >= 0 && hi <= 0 : lo > 0 && hi < 0;
        });
    }
    public static Atom not(Atom atom) {
        return new Atom(atom.dimension(), "not (" + atom.description() + ")", (value, context) -> !atom.matches(context));
    }
    public static Atom wildcard(String dimension) { return new Atom(dimension, "*", (value, context) -> true); }
}
