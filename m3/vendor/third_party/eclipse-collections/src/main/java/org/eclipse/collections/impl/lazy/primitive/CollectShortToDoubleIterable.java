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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.block.function.primitive.ShortToDoubleFunction;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.api.iterator.DoubleIterator;

/**
 * This file was automatically generated from template file collectPrimitiveToPrimitiveIterable.stg.
 */
public class CollectShortToDoubleIterable
        extends AbstractLazyDoubleIterable
{
    private final ShortIterable iterable;
    private final ShortToDoubleFunction function;

    public CollectShortToDoubleIterable(ShortIterable iterable, ShortToDoubleFunction function)
    {
        this.iterable = iterable;
        this.function = function;
    }

    @Override
    public void each(final DoubleProcedure procedure)
    {
        this.iterable.forEach((short each) -> procedure.value(this.function.valueOf(each)));
    }

    @Override
    public DoubleIterator doubleIterator()
    {
        return new DoubleIterator()
        {
            private final ShortIterator iterator = CollectShortToDoubleIterable.this.iterable.shortIterator();

            @Override
            public boolean hasNext()
            {
                return this.iterator.hasNext();
            }

            @Override
            public double next()
            {
                return CollectShortToDoubleIterable.this.function.valueOf(this.iterator.next());
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
