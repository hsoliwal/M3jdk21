/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Consumes a reference and a primitive char without boxing the latter. */
@FunctionalInterface
public interface ObjCharConsumer<T> {
    void accept(T object, char value);

    /** Runs the other consumer only after the first succeeds. */
    default ObjCharConsumer<T> andThen(ObjCharConsumer<? super T> after) {
        java.util.Objects.requireNonNull(after);
        return (object, value) -> {
            accept(object, value);
            after.accept(object, value);
        };
    }
}
