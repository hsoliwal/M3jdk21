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

import jdk.m3.collections.eclipse.api.ShortIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ShortPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortProcedure;
import jdk.m3.collections.eclipse.api.collection.ImmutableCollection;

/**
 * This file was automatically generated from template file immutablePrimitiveCollection.stg.
 *
 * @since 3.0.
 */
public interface ImmutableShortCollection extends ShortIterable
{
    @Override
    <V> ImmutableCollection<V> collect(ShortToObjectFunction<? extends V> function);

    /**
     * @since 9.0.
     */
    @Override
    default ImmutableShortCollection tap(ShortProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    ImmutableShortCollection select(ShortPredicate predicate);

    @Override
    ImmutableShortCollection reject(ShortPredicate predicate);

    ImmutableShortCollection newWith(short element);

    ImmutableShortCollection newWithout(short element);

    ImmutableShortCollection newWithAll(ShortIterable elements);

    ImmutableShortCollection newWithoutAll(ShortIterable elements);
}
