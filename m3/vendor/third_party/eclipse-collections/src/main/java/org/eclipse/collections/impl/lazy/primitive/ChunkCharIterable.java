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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.factory.primitive.CharLists;
import org.eclipse.collections.api.iterator.CharIterator;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.utility.internal.IterableIterate;

/**
 * This file was automatically generated from template file chunkPrimitiveIterable.stg.
 */
public class ChunkCharIterable
        extends AbstractLazyIterable<CharIterable>
{
    private final CharIterable adapted;
    private final int size;

    public ChunkCharIterable(CharIterable delegate, int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }

        this.adapted = delegate;
        this.size = size;
    }

    @Override
    public Iterator<CharIterable> iterator()
    {
        return new ChunkCharIterator(this.adapted, this.size);
    }

    @Override
    public void each(Procedure<? super CharIterable> procedure)
    {
        IterableIterate.forEach(this, procedure);
    }

    public static class ChunkCharIterator implements Iterator<CharIterable>
    {
        private final CharIterator iterator;
        private final int size;
        private final Function0<MutableCharCollection> speciesNewStrategy;

        public ChunkCharIterator(CharIterable iterable, int size)
        {
            if (size <= 0)
            {
                throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
            }

            this.size = size;
            this.iterator = iterable.charIterator();

            this.speciesNewStrategy = iterable instanceof MutableCharCollection
                    ? ((MutableCharCollection) iterable)::newEmpty
                    : CharLists.mutable::empty;
        }

        @Override
        public boolean hasNext()
        {
            return this.iterator.hasNext();
        }

        @Override
        public CharIterable next()
        {
            if (!this.iterator.hasNext())
            {
                throw new NoSuchElementException();
            }

            int i = this.size;
            MutableCharCollection result = this.speciesNewStrategy.value();
            while (i > 0 && this.iterator.hasNext())
            {
                result.add(this.iterator.next());
                i -= 1;
            }

            return result;
        }
    }
}
