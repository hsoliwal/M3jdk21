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
import jdk.m3.collections.eclipse.api.block.function.primitive.CharToByteFunction;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ByteProcedure;
import jdk.m3.collections.eclipse.api.iterator.CharIterator;
import jdk.m3.collections.eclipse.api.iterator.ByteIterator;

/**
 * This file was automatically generated from template file collectPrimitiveToPrimitiveIterable.stg.
 */
public class CollectCharToByteIterable
        extends AbstractLazyByteIterable
{
    private final CharIterable iterable;
    private final CharToByteFunction function;

    public CollectCharToByteIterable(CharIterable iterable, CharToByteFunction function)
    {
        this.iterable = iterable;
        this.function = function;
    }

    @Override
    public void each(final ByteProcedure procedure)
    {
        this.iterable.forEach((char each) -> procedure.value(this.function.valueOf(each)));
    }

    @Override
    public ByteIterator byteIterator()
    {
        return new ByteIterator()
        {
            private final CharIterator iterator = CollectCharToByteIterable.this.iterable.charIterator();

            @Override
            public boolean hasNext()
            {
                return this.iterator.hasNext();
            }

            @Override
            public byte next()
            {
                return CollectCharToByteIterable.this.function.valueOf(this.iterator.next());
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
