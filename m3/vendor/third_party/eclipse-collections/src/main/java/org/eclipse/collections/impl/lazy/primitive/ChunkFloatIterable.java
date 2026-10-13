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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.factory.primitive.FloatLists;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.utility.internal.IterableIterate;

/**
 * This file was automatically generated from template file chunkPrimitiveIterable.stg.
 */
public class ChunkFloatIterable
        extends AbstractLazyIterable<FloatIterable>
{
    private final FloatIterable adapted;
    private final int size;

    public ChunkFloatIterable(FloatIterable delegate, int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }

        this.adapted = delegate;
        this.size = size;
    }

    @Override
    public Iterator<FloatIterable> iterator()
    {
        return new ChunkFloatIterator(this.adapted, this.size);
    }

    @Override
    public void each(Procedure<? super FloatIterable> procedure)
    {
        IterableIterate.forEach(this, procedure);
    }

    public static class ChunkFloatIterator implements Iterator<FloatIterable>
    {
        private final FloatIterator iterator;
        private final int size;
        private final Function0<MutableFloatCollection> speciesNewStrategy;

        public ChunkFloatIterator(FloatIterable iterable, int size)
        {
            if (size <= 0)
            {
                throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
            }

            this.size = size;
            this.iterator = iterable.floatIterator();

            this.speciesNewStrategy = iterable instanceof MutableFloatCollection
                    ? ((MutableFloatCollection) iterable)::newEmpty
                    : FloatLists.mutable::empty;
        }

        @Override
        public boolean hasNext()
        {
            return this.iterator.hasNext();
        }

        @Override
        public FloatIterable next()
        {
            if (!this.iterator.hasNext())
            {
                throw new NoSuchElementException();
            }

            int i = this.size;
            MutableFloatCollection result = this.speciesNewStrategy.value();
            while (i > 0 && this.iterator.hasNext())
            {
                result.add(this.iterator.next());
                i -= 1;
            }

            return result;
        }
    }
}
