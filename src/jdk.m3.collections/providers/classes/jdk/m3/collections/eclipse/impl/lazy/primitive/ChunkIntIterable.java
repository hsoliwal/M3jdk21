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

import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.api.block.function.Function0;
import jdk.m3.collections.eclipse.api.block.procedure.Procedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableIntCollection;
import jdk.m3.collections.eclipse.api.factory.primitive.IntLists;
import jdk.m3.collections.eclipse.api.iterator.IntIterator;
import jdk.m3.collections.eclipse.impl.lazy.AbstractLazyIterable;
import jdk.m3.collections.eclipse.impl.utility.internal.IterableIterate;

/**
 * This file was automatically generated from template file chunkPrimitiveIterable.stg.
 */
public class ChunkIntIterable
        extends AbstractLazyIterable<IntIterable>
{
    private final IntIterable adapted;
    private final int size;

    public ChunkIntIterable(IntIterable delegate, int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }

        this.adapted = delegate;
        this.size = size;
    }

    @Override
    public Iterator<IntIterable> iterator()
    {
        return new ChunkIntIterator(this.adapted, this.size);
    }

    @Override
    public void each(Procedure<? super IntIterable> procedure)
    {
        IterableIterate.forEach(this, procedure);
    }

    public static class ChunkIntIterator implements Iterator<IntIterable>
    {
        private final IntIterator iterator;
        private final int size;
        private final Function0<MutableIntCollection> speciesNewStrategy;

        public ChunkIntIterator(IntIterable iterable, int size)
        {
            if (size <= 0)
            {
                throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
            }

            this.size = size;
            this.iterator = iterable.intIterator();

            this.speciesNewStrategy = iterable instanceof MutableIntCollection
                    ? ((MutableIntCollection) iterable)::newEmpty
                    : IntLists.mutable::empty;
        }

        @Override
        public boolean hasNext()
        {
            return this.iterator.hasNext();
        }

        @Override
        public IntIterable next()
        {
            if (!this.iterator.hasNext())
            {
                throw new NoSuchElementException();
            }

            int i = this.size;
            MutableIntCollection result = this.speciesNewStrategy.value();
            while (i > 0 && this.iterator.hasNext())
            {
                result.add(this.iterator.next());
                i -= 1;
            }

            return result;
        }
    }
}
