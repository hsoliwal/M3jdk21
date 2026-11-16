/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.api.collection.primitive;

import jdk.m3.collections.eclipse.api.FloatIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.FloatPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.FloatProcedure;
import jdk.m3.collections.eclipse.api.collection.ImmutableCollection;

/**
 * This file was automatically generated from template file immutablePrimitiveCollection.stg.
 *
 * @since 3.0.
 */
public interface ImmutableFloatCollection extends FloatIterable
{
    @Override
    <V> ImmutableCollection<V> collect(FloatToObjectFunction<? extends V> function);

    /**
     * @since 9.0.
     */
    @Override
    default ImmutableFloatCollection tap(FloatProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    ImmutableFloatCollection select(FloatPredicate predicate);

    @Override
    ImmutableFloatCollection reject(FloatPredicate predicate);

    ImmutableFloatCollection newWith(float element);

    ImmutableFloatCollection newWithout(float element);

    ImmutableFloatCollection newWithAll(FloatIterable elements);

    ImmutableFloatCollection newWithoutAll(FloatIterable elements);
}
