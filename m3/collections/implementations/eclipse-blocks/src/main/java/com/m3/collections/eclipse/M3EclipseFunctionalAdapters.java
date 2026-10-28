/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 * Original, first-party M3 operations; provider functional interfaces are separately licensed.
 */
package com.m3.collections.eclipse;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure;

/**
 * Eclipse 11.1.0 callback protocol over JDK21 functional atoms. These are
 * closure adaptors only: they do not retain a second collection, per-element
 * Node/Entry, native payload, index lane or predicate result cache. M3 array
 * owners continue to handle selection, masking, filtering and sorting.
 */
public final class M3EclipseFunctionalAdapters {
    private M3EclipseFunctionalAdapters() { }

    public static <T, R> Function<T, R> function(
            java.util.function.Function<? super T, ? extends R> operation) {
        Objects.requireNonNull(operation, "operation");
        return operation::apply;
    }

    public static <A, B, R> Function2<A, B, R> function2(
            BiFunction<? super A, ? super B, ? extends R> operation) {
        Objects.requireNonNull(operation, "operation");
        return operation::apply;
    }

    public static <T> Predicate<T> predicate(
            java.util.function.Predicate<? super T> condition) {
        Objects.requireNonNull(condition, "condition");
        return condition::test;
    }

    public static <A, B> Predicate2<A, B> predicate2(
            BiPredicate<? super A, ? super B> condition) {
        Objects.requireNonNull(condition, "condition");
        return condition::test;
    }

    public static <T> Procedure<T> procedure(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action");
        return action::accept;
    }
}
