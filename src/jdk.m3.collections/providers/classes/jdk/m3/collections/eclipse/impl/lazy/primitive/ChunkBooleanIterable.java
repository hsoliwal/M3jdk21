/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.lazy.primitive;

import java.util.Iterator;
import java.util.NoSuchElementException;

import jdk.m3.collections.eclipse.api.BooleanIterable;
import jdk.m3.collections.eclipse.api.block.function.Function0;
import jdk.m3.collections.eclipse.api.block.procedure.Procedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableBooleanCollection;
import jdk.m3.collections.eclipse.api.factory.primitive.BooleanLists;
import jdk.m3.collections.eclipse.api.iterator.BooleanIterator;
import jdk.m3.collections.eclipse.impl.lazy.AbstractLazyIterable;
import jdk.m3.collections.eclipse.impl.utility.internal.IterableIterate;

/**
 * This file was automatically generated from template file chunkPrimitiveIterable.stg.
 */
public class ChunkBooleanIterable
        extends AbstractLazyIterable<BooleanIterable>
{
    private final BooleanIterable adapted;
    private final int size;

    public ChunkBooleanIterable(BooleanIterable delegate, int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }

        this.adapted = delegate;
        this.size = size;
    }

    @Override
    public Iterator<BooleanIterable> iterator()
    {
        return new ChunkBooleanIterator(this.adapted, this.size);
    }

    @Override
    public void each(Procedure<? super BooleanIterable> procedure)
    {
        IterableIterate.forEach(this, procedure);
    }

    public static class ChunkBooleanIterator implements Iterator<BooleanIterable>
    {
        private final BooleanIterator iterator;
        private final int size;
        private final Function0<MutableBooleanCollection> speciesNewStrategy;

        public ChunkBooleanIterator(BooleanIterable iterable, int size)
        {
            if (size <= 0)
            {
                throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
            }

            this.size = size;
            this.iterator = iterable.booleanIterator();

            this.speciesNewStrategy = iterable instanceof MutableBooleanCollection
                    ? ((MutableBooleanCollection) iterable)::newEmpty
                    : BooleanLists.mutable::empty;
        }

        @Override
        public boolean hasNext()
        {
            return this.iterator.hasNext();
        }

        @Override
        public BooleanIterable next()
        {
            if (!this.iterator.hasNext())
            {
                throw new NoSuchElementException();
            }

            int i = this.size;
            MutableBooleanCollection result = this.speciesNewStrategy.value();
            while (i > 0 && this.iterator.hasNext())
            {
                result.add(this.iterator.next());
                i -= 1;
            }

            return result;
        }
    }
}
