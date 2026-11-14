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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.collection.ImmutableCollection;

/**
 * This file was automatically generated from template file immutablePrimitiveCollection.stg.
 *
 * @since 3.0.
 */
public interface ImmutableBooleanCollection extends BooleanIterable
{
    @Override
    <V> ImmutableCollection<V> collect(BooleanToObjectFunction<? extends V> function);

    /**
     * @since 9.0.
     */
    @Override
    default ImmutableBooleanCollection tap(BooleanProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    ImmutableBooleanCollection select(BooleanPredicate predicate);

    @Override
    ImmutableBooleanCollection reject(BooleanPredicate predicate);

    ImmutableBooleanCollection newWith(boolean element);

    ImmutableBooleanCollection newWithout(boolean element);

    ImmutableBooleanCollection newWithAll(BooleanIterable elements);

    ImmutableBooleanCollection newWithoutAll(BooleanIterable elements);
}
