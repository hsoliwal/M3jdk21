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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.block.function.primitive.ByteToFloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.iterator.FloatIterator;

/**
 * This file was automatically generated from template file collectPrimitiveToPrimitiveIterable.stg.
 */
public class CollectByteToFloatIterable
        extends AbstractLazyFloatIterable
{
    private final ByteIterable iterable;
    private final ByteToFloatFunction function;

    public CollectByteToFloatIterable(ByteIterable iterable, ByteToFloatFunction function)
    {
        this.iterable = iterable;
        this.function = function;
    }

    @Override
    public void each(final FloatProcedure procedure)
    {
        this.iterable.forEach((byte each) -> procedure.value(this.function.valueOf(each)));
    }

    @Override
    public FloatIterator floatIterator()
    {
        return new FloatIterator()
        {
            private final ByteIterator iterator = CollectByteToFloatIterable.this.iterable.byteIterator();

            @Override
            public boolean hasNext()
            {
                return this.iterator.hasNext();
            }

            @Override
            public float next()
            {
                return CollectByteToFloatIterable.this.function.valueOf(this.iterator.next());
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
