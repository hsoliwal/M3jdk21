/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.lazy.primitive;

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.block.function.primitive.IntToDoubleFunction;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.iterator.DoubleIterator;

/**
 * This file was automatically generated from template file collectPrimitiveToPrimitiveIterable.stg.
 */
public class CollectIntToDoubleIterable
        extends AbstractLazyDoubleIterable
{
    private final IntIterable iterable;
    private final IntToDoubleFunction function;

    public CollectIntToDoubleIterable(IntIterable iterable, IntToDoubleFunction function)
    {
        this.iterable = iterable;
        this.function = function;
    }

    @Override
    public void each(final DoubleProcedure procedure)
    {
        this.iterable.forEach((int each) -> procedure.value(this.function.valueOf(each)));
    }

    @Override
    public DoubleIterator doubleIterator()
    {
        return new DoubleIterator()
        {
            private final IntIterator iterator = CollectIntToDoubleIterable.this.iterable.intIterator();

            @Override
            public boolean hasNext()
            {
                return this.iterator.hasNext();
            }

            @Override
            public double next()
            {
                return CollectIntToDoubleIterable.this.function.valueOf(this.iterator.next());
            }
        };
    }

    @Override
    public int size()
    {
        return this.iterable.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.iterable.isEmpty();
    }

    @Override
    public boolean notEmpty()
    {
        return this.iterable.notEmpty();
    }
}
