/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.block.function.primitive;

import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;

/**
 * This file was automatically generated from template file primitiveCaseFunction.stg.
 */
public class IntCaseFunction<V> implements IntToObjectFunction<V>
{
    private static final long serialVersionUID = 1L;

    private final MutableList<Pair<IntPredicate, IntToObjectFunction<? extends V>>> predicateFunctions = Lists.mutable.empty();
    private IntToObjectFunction<? extends V> defaultFunction;

    public IntCaseFunction()
    {
    }

    public IntCaseFunction(IntToObjectFunction<? extends V> newDefaultFunction)
    {
        this.setDefault(newDefaultFunction);
    }

    public IntCaseFunction<V> addCase(
            IntPredicate predicate,
            IntToObjectFunction<? extends V> function)
    {
        this.predicateFunctions.add(Tuples.pair(predicate, function));
        return this;
    }

    public IntCaseFunction<V> setDefault(IntToObjectFunction<? extends V> function)
    {
        this.defaultFunction = function;
        return this;
    }

    @Override
    public V valueOf(int argument)
    {
        int localSize = this.predicateFunctions.size();
        for (int i = 0; i < localSize; i++)
        {
            Pair<IntPredicate, IntToObjectFunction<? extends V>> pair = this.predicateFunctions.get(i);
            if (pair.getOne().accept(argument))
            {
                return pair.getTwo().valueOf(argument);
            }
        }

        if (this.defaultFunction != null)
        {
            return this.defaultFunction.valueOf(argument);
        }
        return null;
    }

    @Override
    public String toString()
    {
        return "new IntCaseFunction(" + this.predicateFunctions + ')';
    }
}
