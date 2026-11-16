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

import jdk.m3.collections.eclipse.api.DoubleIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.DoublePredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.DoubleProcedure;
import jdk.m3.collections.eclipse.api.collection.ImmutableCollection;

/**
 * This file was automatically generated from template file immutablePrimitiveCollection.stg.
 *
 * @since 3.0.
 */
public interface ImmutableDoubleCollection extends DoubleIterable
{
    @Override
    <V> ImmutableCollection<V> collect(DoubleToObjectFunction<? extends V> function);

    /**
     * @since 9.0.
     */
    @Override
    default ImmutableDoubleCollection tap(DoubleProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    ImmutableDoubleCollection select(DoublePredicate predicate);

    @Override
    ImmutableDoubleCollection reject(DoublePredicate predicate);

    ImmutableDoubleCollection newWith(double element);

    ImmutableDoubleCollection newWithout(double element);

    ImmutableDoubleCollection newWithAll(DoubleIterable elements);

    ImmutableDoubleCollection newWithoutAll(DoubleIterable elements);
}
