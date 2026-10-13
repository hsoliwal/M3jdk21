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

import org.eclipse.collections.api.block.function.primitive.LongIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.list.primitive.LongList;
import org.eclipse.collections.api.stack.MutableStack;

/**
 * This file was automatically generated from template file mutablePrimitiveStack.stg.
 *
 * @since 3.0.
 */
public interface MutableLongStack extends LongStack
{
    /**
     * Adds an item to the top of the stack.
     */
    void push(long item);

    /**
     * Removes and returns the top element of the stack.
     */
    long pop();

    /**
     * Removes and returns a LongList of the number of elements specified by the count, beginning with the top of the stack.
     */
    LongList pop(int count);

    /**
     * Clears the Stack
     */
    void clear();

    @Override
    MutableLongStack select(LongPredicate predicate);

    @Override
    MutableLongStack reject(LongPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default MutableLongStack tap(LongProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    <V> MutableStack<V> collect(LongToObjectFunction<? extends V> function);

    /**
     * Returns a new MutableLongStack including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default MutableLongStack selectWithIndex(LongIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new MutableLongStack excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default MutableLongStack rejectWithIndex(LongIntPredicate predicate)
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
    default <V> MutableStack<V> collectWithIndex(LongIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    MutableLongStack asUnmodifiable();

    MutableLongStack asSynchronized();

    /**
     * Creates a new empty mutable version of the same stack type.
     *
     * @since 9.2.
     */
    default MutableLongStack newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
