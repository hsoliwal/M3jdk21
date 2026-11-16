/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.api.stack.primitive;

import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.DoubleIntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.DoublePredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.DoubleProcedure;
import jdk.m3.collections.eclipse.api.stack.ImmutableStack;

/**
 * This file was automatically generated from template file immutablePrimitiveStack.stg.
 *
 * @since 3.0.
 */
public interface ImmutableDoubleStack extends DoubleStack
{
    /**
     * @since 9.0.
     */
    @Override
    default ImmutableDoubleStack tap(DoubleProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    ImmutableDoubleStack push(double item);

    ImmutableDoubleStack pop();

    ImmutableDoubleStack pop(int count);

    @Override
    ImmutableDoubleStack select(DoublePredicate predicate);

    @Override
    ImmutableDoubleStack reject(DoublePredicate predicate);

    @Override
    <V> ImmutableStack<V> collect(DoubleToObjectFunction<? extends V> function);

    /**
     * Returns a new ImmutableDoubleStack including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ImmutableDoubleStack selectWithIndex(DoubleIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new ImmutableDoubleStack excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ImmutableDoubleStack rejectWithIndex(DoubleIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new ImmutableStack using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> ImmutableStack<V> collectWithIndex(DoubleIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }
}
