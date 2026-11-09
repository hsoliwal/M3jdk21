/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.collection.primitive;

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.collection.ImmutableCollection;

/**
 * This file was automatically generated from template file immutablePrimitiveCollection.stg.
 *
 * @since 3.0.
 */
public interface ImmutableByteCollection extends ByteIterable
{
    @Override
    <V> ImmutableCollection<V> collect(ByteToObjectFunction<? extends V> function);

    /**
     * @since 9.0.
     */
    @Override
    default ImmutableByteCollection tap(ByteProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    ImmutableByteCollection select(BytePredicate predicate);

    @Override
    ImmutableByteCollection reject(BytePredicate predicate);

    ImmutableByteCollection newWith(byte element);

    ImmutableByteCollection newWithout(byte element);

    ImmutableByteCollection newWithAll(ByteIterable elements);

    ImmutableByteCollection newWithoutAll(ByteIterable elements);
}
