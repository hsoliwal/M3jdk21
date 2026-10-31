/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.lazy.primitive;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.collection.primitive.MutableShortCollection;
import org.eclipse.collections.api.factory.primitive.ShortLists;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.utility.internal.IterableIterate;

/**
 * This file was automatically generated from template file chunkPrimitiveIterable.stg.
 */
public class ChunkShortIterable
        extends AbstractLazyIterable<ShortIterable>
{
    private final ShortIterable adapted;
    private final int size;

    public ChunkShortIterable(ShortIterable delegate, int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }

        this.adapted = delegate;
        this.size = size;
    }

    @Override
    public Iterator<ShortIterable> iterator()
    {
        return new ChunkShortIterator(this.adapted, this.size);
    }

    @Override
    public void each(Procedure<? super ShortIterable> procedure)
    {
        IterableIterate.forEach(this, procedure);
    }

    public static class ChunkShortIterator implements Iterator<ShortIterable>
    {
        private final ShortIterator iterator;
        private final int size;
        private final Function0<MutableShortCollection> speciesNewStrategy;

        public ChunkShortIterator(ShortIterable iterable, int size)
        {
            if (size <= 0)
            {
                throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
            }

            this.size = size;
            this.iterator = iterable.shortIterator();

            this.speciesNewStrategy = iterable instanceof MutableShortCollection
                    ? ((MutableShortCollection) iterable)::newEmpty
                    : ShortLists.mutable::empty;
        }

        @Override
        public boolean hasNext()
        {
            return this.iterator.hasNext();
        }

        @Override
        public ShortIterable next()
        {
            if (!this.iterator.hasNext())
            {
                throw new NoSuchElementException();
            }

            int i = this.size;
            MutableShortCollection result = this.speciesNewStrategy.value();
            while (i > 0 && this.iterator.hasNext())
            {
                result.add(this.iterator.next());
                i -= 1;
            }

            return result;
        }
    }
}
