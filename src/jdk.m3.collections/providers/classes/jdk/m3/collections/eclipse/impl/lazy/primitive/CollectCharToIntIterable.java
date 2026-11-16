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

import jdk.m3.collections.eclipse.api.CharIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharToIntFunction;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.IntProcedure;
import jdk.m3.collections.eclipse.api.iterator.CharIterator;
import jdk.m3.collections.eclipse.api.iterator.IntIterator;

/**
 * This file was automatically generated from template file collectPrimitiveToPrimitiveIterable.stg.
 */
public class CollectCharToIntIterable
        extends AbstractLazyIntIterable
{
    private final CharIterable iterable;
    private final CharToIntFunction function;

    public CollectCharToIntIterable(CharIterable iterable, CharToIntFunction function)
    {
        this.iterable = iterable;
        this.function = function;
    }

    @Override
    public void each(final IntProcedure procedure)
    {
        this.iterable.forEach((char each) -> procedure.value(this.function.valueOf(each)));
    }

    @Override
    public IntIterator intIterator()
    {
        return new IntIterator()
        {
            private final CharIterator iterator = CollectCharToIntIterable.this.iterable.charIterator();

            @Override
            public boolean hasNext()
            {
                return this.iterator.hasNext();
            }

            @Override
            public int next()
            {
                return CollectCharToIntIterable.this.function.valueOf(this.iterator.next());
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
