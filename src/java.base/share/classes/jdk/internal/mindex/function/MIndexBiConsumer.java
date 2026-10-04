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
import java.util.function.BiConsumer;

/** Flat immutable BiConsumer sequence implementing BiConsumer.andThen. */
public final class MIndexBiConsumer<T, U> implements BiConsumer<T, U> {
    private final BiConsumer<Object, Object>[] stages;

    @SuppressWarnings("unchecked")
    private MIndexBiConsumer(BiConsumer<?, ?>[] stages) {
        this.stages = (BiConsumer<Object, Object>[]) stages;
    }

    public static <T, U> MIndexBiConsumer<T, U> of(
            BiConsumer<? super T, ? super U> consumer) {
        Objects.requireNonNull(consumer);
        if (consumer instanceof MIndexBiConsumer<?, ?> plan) {
            @SuppressWarnings("unchecked")
            MIndexBiConsumer<T, U> typed = (MIndexBiConsumer<T, U>) plan;
            return typed;
        }
        return new MIndexBiConsumer<>(new BiConsumer<?, ?>[] {consumer});
    }

    @Override
    public void accept(T first, U second) {
        for (BiConsumer<Object, Object> stage : stages) {
            stage.accept(first, second);
        }
    }

    @Override
    public MIndexBiConsumer<T, U> andThen(
            BiConsumer<? super T, ? super U> after) {
        Objects.requireNonNull(after);
        BiConsumer<?, ?>[] suffix =
                after instanceof MIndexBiConsumer<?, ?> plan
                        ? plan.stages
                        : new BiConsumer<?, ?>[] {after};
        BiConsumer<?, ?>[] joined =
                Arrays.copyOf(stages, Math.addExact(stages.length, suffix.length));
        System.arraycopy(suffix, 0, joined, stages.length, suffix.length);
        return new MIndexBiConsumer<>(joined);
    }

    public int stageCount() {
        return stages.length;
    }
}
