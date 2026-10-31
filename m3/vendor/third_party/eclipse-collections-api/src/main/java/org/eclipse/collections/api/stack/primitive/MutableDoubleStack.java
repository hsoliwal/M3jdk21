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

import org.eclipse.collections.api.block.function.primitive.DoubleIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoubleIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.list.primitive.DoubleList;
import org.eclipse.collections.api.stack.MutableStack;

/**
 * This file was automatically generated from template file mutablePrimitiveStack.stg.
 *
 * @since 3.0.
 */
public interface MutableDoubleStack extends DoubleStack
{
    /**
     * Adds an item to the top of the stack.
     */
    void push(double item);

    /**
     * Removes and returns the top element of the stack.
     */
    double pop();

    /**
     * Removes and returns a DoubleList of the number of elements specified by the count, beginning with the top of the stack.
     */
    DoubleList pop(int count);

    /**
     * Clears the Stack
     */
    void clear();

    @Override
    MutableDoubleStack select(DoublePredicate predicate);

    @Override
    MutableDoubleStack reject(DoublePredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default MutableDoubleStack tap(DoubleProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    <V> MutableStack<V> collect(DoubleToObjectFunction<? extends V> function);

    /**
     * Returns a new MutableDoubleStack including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default MutableDoubleStack selectWithIndex(DoubleIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new MutableDoubleStack excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default MutableDoubleStack rejectWithIndex(DoubleIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new MutableStack using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> MutableStack<V> collectWithIndex(DoubleIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    MutableDoubleStack asUnmodifiable();

    MutableDoubleStack asSynchronized();

    /**
     * Creates a new empty mutable version of the same stack type.
     *
     * @since 9.2.
     */
    default MutableDoubleStack newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
