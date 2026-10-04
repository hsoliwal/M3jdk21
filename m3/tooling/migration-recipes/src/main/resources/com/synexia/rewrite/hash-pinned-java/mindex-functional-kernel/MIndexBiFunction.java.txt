/*
 * Copyright (c) 2026, Contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

package jdk.internal.mindex.function;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Flat immutable BiFunction source plus Function tail stages. */
public final class MIndexBiFunction<T, U, R> implements BiFunction<T, U, R> {
    private final BiFunction<Object, Object, Object> source;
    private final Function<Object, Object>[] tail;

    @SuppressWarnings("unchecked")
    private MIndexBiFunction(
            BiFunction<?, ?, ?> source,
            Function<?, ?>[] tail) {
        this.source = (BiFunction<Object, Object, Object>) source;
        this.tail = (Function<Object, Object>[]) tail;
    }

    public static <T, U, R> MIndexBiFunction<T, U, R> of(
            BiFunction<? super T, ? super U, ? extends R> function) {
        Objects.requireNonNull(function);
        if (function instanceof MIndexBiFunction<?, ?, ?> plan) {
            @SuppressWarnings("unchecked")
            MIndexBiFunction<T, U, R> typed =
                    (MIndexBiFunction<T, U, R>) plan;
            return typed;
        }
        return new MIndexBiFunction<>(function, new Function<?, ?>[0]);
    }

    @Override
    @SuppressWarnings("unchecked")
    public R apply(T first, U second) {
        Object value = source.apply(first, second);
        for (Function<Object, Object> stage : tail) {
            value = stage.apply(value);
        }
        return (R) value;
    }

    @Override
    public <V> MIndexBiFunction<T, U, V> andThen(
            Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        Function<?, ?>[] joined = Arrays.copyOf(tail, tail.length + 1);
        joined[tail.length] = after;
        return new MIndexBiFunction<>(source, joined);
    }

    public int tailStageCount() {
        return tail.length;
    }
}
