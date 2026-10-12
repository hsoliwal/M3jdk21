/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.stack.primitive;

import org.eclipse.collections.api.block.function.primitive.ShortIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.stack.ImmutableStack;

/**
 * This file was automatically generated from template file immutablePrimitiveStack.stg.
 *
 * @since 3.0.
 */
public interface ImmutableShortStack extends ShortStack
{
    /**
     * @since 9.0.
     */
    @Override
    default ImmutableShortStack tap(ShortProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    ImmutableShortStack push(short item);

    ImmutableShortStack pop();

    ImmutableShortStack pop(int count);

    @Override
    ImmutableShortStack select(ShortPredicate predicate);

    @Override
    ImmutableShortStack reject(ShortPredicate predicate);

    @Override
    <V> ImmutableStack<V> collect(ShortToObjectFunction<? extends V> function);

    /**
     * Returns a new ImmutableShortStack including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ImmutableShortStack selectWithIndex(ShortIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new ImmutableShortStack excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ImmutableShortStack rejectWithIndex(ShortIntPredicate predicate)
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
    default <V> ImmutableStack<V> collectWithIndex(ShortIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }
}
