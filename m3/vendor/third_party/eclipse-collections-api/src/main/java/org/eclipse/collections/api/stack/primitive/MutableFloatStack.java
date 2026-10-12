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

import org.eclipse.collections.api.block.function.primitive.FloatIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.list.primitive.FloatList;
import org.eclipse.collections.api.stack.MutableStack;

/**
 * This file was automatically generated from template file mutablePrimitiveStack.stg.
 *
 * @since 3.0.
 */
public interface MutableFloatStack extends FloatStack
{
    /**
     * Adds an item to the top of the stack.
     */
    void push(float item);

    /**
     * Removes and returns the top element of the stack.
     */
    float pop();

    /**
     * Removes and returns a FloatList of the number of elements specified by the count, beginning with the top of the stack.
     */
    FloatList pop(int count);

    /**
     * Clears the Stack
     */
    void clear();

    @Override
    MutableFloatStack select(FloatPredicate predicate);

    @Override
    MutableFloatStack reject(FloatPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default MutableFloatStack tap(FloatProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    <V> MutableStack<V> collect(FloatToObjectFunction<? extends V> function);

    /**
     * Returns a new MutableFloatStack including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default MutableFloatStack selectWithIndex(FloatIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new MutableFloatStack excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default MutableFloatStack rejectWithIndex(FloatIntPredicate predicate)
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
    default <V> MutableStack<V> collectWithIndex(FloatIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    MutableFloatStack asUnmodifiable();

    MutableFloatStack asSynchronized();

    /**
     * Creates a new empty mutable version of the same stack type.
     *
     * @since 9.2.
     */
    default MutableFloatStack newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
