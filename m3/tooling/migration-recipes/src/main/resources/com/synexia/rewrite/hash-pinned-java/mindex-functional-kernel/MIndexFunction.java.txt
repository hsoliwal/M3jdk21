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
import java.util.function.Function;

/**
 * Flat immutable generic Function chain.
 *
 * <p>Unlike repeated default Function.andThen/compose wrappers, a plan keeps one flat stage array.
 * User-provided functions remain ordinary JDK functions; this class only removes nested wrapper
 * structure and supplies a stable lowering target.</p>
 */
public final class MIndexFunction<T, R> implements Function<T, R> {
    private final Function<Object, Object>[] stages;

    @SuppressWarnings("unchecked")
    private MIndexFunction(Function<?, ?>[] stages) {
        this.stages = (Function<Object, Object>[]) stages;
    }

    public static <T, R> MIndexFunction<T, R> of(
            Function<? super T, ? extends R> function) {
        Objects.requireNonNull(function);
        if (function instanceof MIndexFunction<?, ?> plan) {
            @SuppressWarnings("unchecked")
            MIndexFunction<T, R> typed = (MIndexFunction<T, R>) plan;
            return typed;
        }
        return new MIndexFunction<>(new Function<?, ?>[] {function});
    }

    @Override
    @SuppressWarnings("unchecked")
    public R apply(T value) {
        Object current = value;
        for (Function<Object, Object> stage : stages) {
            current = stage.apply(current);
        }
        return (R) current;
    }

    @Override
    public <V> MIndexFunction<V, R> compose(
            Function<? super V, ? extends T> before) {
        Objects.requireNonNull(before);
        Function<?, ?>[] prefix =
                before instanceof MIndexFunction<?, ?> plan
                        ? plan.stages
                        : new Function<?, ?>[] {before};
        Function<?, ?>[] joined =
                Arrays.copyOf(prefix, Math.addExact(prefix.length, stages.length));
        System.arraycopy(stages, 0, joined, prefix.length, stages.length);
        return new MIndexFunction<>(joined);
    }

    @Override
    public <V> MIndexFunction<T, V> andThen(
            Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        Function<?, ?>[] suffix =
                after instanceof MIndexFunction<?, ?> plan
                        ? plan.stages
                        : new Function<?, ?>[] {after};
        Function<?, ?>[] joined =
                Arrays.copyOf(stages, Math.addExact(stages.length, suffix.length));
        System.arraycopy(suffix, 0, joined, stages.length, suffix.length);
        return new MIndexFunction<>(joined);
    }

    public int stageCount() {
        return stages.length;
    }

    public long estimatedRetainedBytes() {
        return (long) Long.BYTES * stages.length;
    }
}
