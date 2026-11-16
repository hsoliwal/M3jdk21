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

import jdk.m3.collections.eclipse.api.CharIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.CharPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.CharProcedure;
import jdk.m3.collections.eclipse.api.collection.ImmutableCollection;

/**
 * This file was automatically generated from template file immutablePrimitiveCollection.stg.
 *
 * @since 3.0.
 */
public interface ImmutableCharCollection extends CharIterable
{
    @Override
    <V> ImmutableCollection<V> collect(CharToObjectFunction<? extends V> function);

    /**
     * @since 9.0.
     */
    @Override
    default ImmutableCharCollection tap(CharProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    ImmutableCharCollection select(CharPredicate predicate);

    @Override
    ImmutableCharCollection reject(CharPredicate predicate);

    ImmutableCharCollection newWith(char element);

    ImmutableCharCollection newWithout(char element);

    ImmutableCharCollection newWithAll(CharIterable elements);

    ImmutableCharCollection newWithoutAll(CharIterable elements);
}
