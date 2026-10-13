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
import org.eclipse.collections.api.block.function.primitive.ShortToShortFunction;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.iterator.ShortIterator;

/**
 * This file was automatically generated from template file collectPrimitiveToPrimitiveIterable.stg.
 */
public class CollectShortToShortIterable
        extends AbstractLazyShortIterable
{
    private final ShortIterable iterable;
    private final ShortToShortFunction function;

    public CollectShortToShortIterable(ShortIterable iterable, ShortToShortFunction function)
    {
        this.iterable = iterable;
        this.function = function;
    }

    @Override
    public void each(final ShortProcedure procedure)
    {
        this.iterable.forEach((short each) -> procedure.value(this.function.valueOf(each)));
    }

    @Override
    public ShortIterator shortIterator()
    {
        return new ShortIterator()
        {
            private final ShortIterator iterator = CollectShortToShortIterable.this.iterable.shortIterator();

            @Override
            public boolean hasNext()
            {
                return this.iterator.hasNext();
            }

            @Override
            public short next()
            {
                return CollectShortToShortIterable.this.function.valueOf(this.iterator.next());
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
