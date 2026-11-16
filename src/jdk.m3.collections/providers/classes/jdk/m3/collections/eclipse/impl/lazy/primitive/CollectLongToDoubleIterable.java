/*
 * Copyright (c) 2026 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.lazy.primitive;

import jdk.m3.collections.eclipse.api.LongIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongToDoubleFunction;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.DoubleProcedure;
import jdk.m3.collections.eclipse.api.iterator.LongIterator;
import jdk.m3.collections.eclipse.api.iterator.DoubleIterator;

/**
 * This file was automatically generated from template file collectPrimitiveToPrimitiveIterable.stg.
 */
public class CollectLongToDoubleIterable
        extends AbstractLazyDoubleIterable
{
    private final LongIterable iterable;
    private final LongToDoubleFunction function;

    public CollectLongToDoubleIterable(LongIterable iterable, LongToDoubleFunction function)
    {
        this.iterable = iterable;
        this.function = function;
    }

    @Override
    public void each(final DoubleProcedure procedure)
    {
        this.iterable.forEach((long each) -> procedure.value(this.function.valueOf(each)));
    }

    @Override
    public DoubleIterator doubleIterator()
    {
        return new DoubleIterator()
        {
            private final LongIterator iterator = CollectLongToDoubleIterable.this.iterable.longIterator();

            @Override
            public boolean hasNext()
            {
                return this.iterator.hasNext();
            }

            @Override
            public double next()
            {
                return CollectLongToDoubleIterable.this.function.valueOf(this.iterator.next());
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
