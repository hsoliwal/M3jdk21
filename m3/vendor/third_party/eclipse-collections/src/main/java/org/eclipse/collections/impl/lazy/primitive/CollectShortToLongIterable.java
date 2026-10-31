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
import org.eclipse.collections.api.block.function.primitive.ShortToLongFunction;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.api.iterator.LongIterator;

/**
 * This file was automatically generated from template file collectPrimitiveToPrimitiveIterable.stg.
 */
public class CollectShortToLongIterable
        extends AbstractLazyLongIterable
{
    private final ShortIterable iterable;
    private final ShortToLongFunction function;

    public CollectShortToLongIterable(ShortIterable iterable, ShortToLongFunction function)
    {
        this.iterable = iterable;
        this.function = function;
    }

    @Override
    public void each(final LongProcedure procedure)
    {
        this.iterable.forEach((short each) -> procedure.value(this.function.valueOf(each)));
    }

    @Override
    public LongIterator longIterator()
    {
        return new LongIterator()
        {
            private final ShortIterator iterator = CollectShortToLongIterable.this.iterable.shortIterator();

            @Override
            public boolean hasNext()
            {
                return this.iterator.hasNext();
            }

            @Override
            public long next()
            {
                return CollectShortToLongIterable.this.function.valueOf(this.iterator.next());
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
