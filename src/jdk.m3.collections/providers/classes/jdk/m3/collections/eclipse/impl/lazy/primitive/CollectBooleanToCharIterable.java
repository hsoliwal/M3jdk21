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

import jdk.m3.collections.eclipse.api.BooleanIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanToCharFunction;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.CharProcedure;
import jdk.m3.collections.eclipse.api.iterator.BooleanIterator;
import jdk.m3.collections.eclipse.api.iterator.CharIterator;

/**
 * This file was automatically generated from template file collectPrimitiveToPrimitiveIterable.stg.
 */
public class CollectBooleanToCharIterable
        extends AbstractLazyCharIterable
{
    private final BooleanIterable iterable;
    private final BooleanToCharFunction function;

    public CollectBooleanToCharIterable(BooleanIterable iterable, BooleanToCharFunction function)
    {
        this.iterable = iterable;
        this.function = function;
    }

    @Override
    public void each(final CharProcedure procedure)
    {
        this.iterable.forEach((boolean each) -> procedure.value(this.function.valueOf(each)));
    }

    @Override
    public CharIterator charIterator()
    {
        return new CharIterator()
        {
            private final BooleanIterator iterator = CollectBooleanToCharIterable.this.iterable.booleanIterator();

            @Override
            public boolean hasNext()
            {
                return this.iterator.hasNext();
            }

            @Override
            public char next()
            {
                return CollectBooleanToCharIterable.this.function.valueOf(this.iterator.next());
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
