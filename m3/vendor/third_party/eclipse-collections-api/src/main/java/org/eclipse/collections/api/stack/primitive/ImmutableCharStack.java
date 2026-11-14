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

import org.eclipse.collections.api.block.function.primitive.CharIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.stack.ImmutableStack;

/**
 * This file was automatically generated from template file immutablePrimitiveStack.stg.
 *
 * @since 3.0.
 */
public interface ImmutableCharStack extends CharStack
{
    /**
     * @since 9.0.
     */
    @Override
    default ImmutableCharStack tap(CharProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    ImmutableCharStack push(char item);

    ImmutableCharStack pop();

    ImmutableCharStack pop(int count);

    @Override
    ImmutableCharStack select(CharPredicate predicate);

    @Override
    ImmutableCharStack reject(CharPredicate predicate);

    @Override
    <V> ImmutableStack<V> collect(CharToObjectFunction<? extends V> function);

    /**
     * Returns a new ImmutableCharStack including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ImmutableCharStack selectWithIndex(CharIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new ImmutableCharStack excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ImmutableCharStack rejectWithIndex(CharIntPredicate predicate)
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
    default <V> ImmutableStack<V> collectWithIndex(CharIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }
}
