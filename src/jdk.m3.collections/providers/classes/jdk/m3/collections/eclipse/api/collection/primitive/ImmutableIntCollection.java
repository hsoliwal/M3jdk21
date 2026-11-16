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

import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.IntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.IntProcedure;
import jdk.m3.collections.eclipse.api.collection.ImmutableCollection;

/**
 * This file was automatically generated from template file immutablePrimitiveCollection.stg.
 *
 * @since 3.0.
 */
public interface ImmutableIntCollection extends IntIterable
{
    @Override
    <V> ImmutableCollection<V> collect(IntToObjectFunction<? extends V> function);

    /**
     * @since 9.0.
     */
    @Override
    default ImmutableIntCollection tap(IntProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    ImmutableIntCollection select(IntPredicate predicate);

    @Override
    ImmutableIntCollection reject(IntPredicate predicate);

    ImmutableIntCollection newWith(int element);

    ImmutableIntCollection newWithout(int element);

    ImmutableIntCollection newWithAll(IntIterable elements);

    ImmutableIntCollection newWithoutAll(IntIterable elements);
}
