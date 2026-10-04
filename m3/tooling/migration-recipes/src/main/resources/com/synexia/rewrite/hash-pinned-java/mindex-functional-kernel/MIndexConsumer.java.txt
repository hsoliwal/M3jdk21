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
import java.util.function.Consumer;

/** Flat immutable Consumer sequence implementing Consumer.andThen without nested wrappers. */
public final class MIndexConsumer<T> implements Consumer<T> {
    private final Consumer<Object>[] stages;

    @SuppressWarnings("unchecked")
    private MIndexConsumer(Consumer<?>[] stages) {
        this.stages = (Consumer<Object>[]) stages;
    }

    public static <T> MIndexConsumer<T> of(Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer);
        if (consumer instanceof MIndexConsumer<?> plan) {
            @SuppressWarnings("unchecked")
            MIndexConsumer<T> typed = (MIndexConsumer<T>) plan;
            return typed;
        }
        return new MIndexConsumer<>(new Consumer<?>[] {consumer});
    }

    @Override
    public void accept(T value) {
        for (Consumer<Object> stage : stages) {
            stage.accept(value);
        }
    }

    @Override
    public MIndexConsumer<T> andThen(Consumer<? super T> after) {
        Objects.requireNonNull(after);
        Consumer<?>[] suffix =
                after instanceof MIndexConsumer<?> plan
                        ? plan.stages
                        : new Consumer<?>[] {after};
        Consumer<?>[] joined =
                Arrays.copyOf(stages, Math.addExact(stages.length, suffix.length));
        System.arraycopy(suffix, 0, joined, stages.length, suffix.length);
        return new MIndexConsumer<>(joined);
    }

    public int stageCount() {
        return stages.length;
    }
}
