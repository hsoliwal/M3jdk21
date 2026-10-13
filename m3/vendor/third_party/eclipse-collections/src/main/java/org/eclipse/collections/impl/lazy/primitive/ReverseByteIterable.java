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

import java.util.NoSuchElementException;

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.factory.primitive.ByteBags;
import org.eclipse.collections.api.factory.primitive.ByteLists;
import org.eclipse.collections.api.factory.primitive.ByteSets;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.impl.lazy.ReverseIterable;

/**
 * This file was automatically generated from template file reversePrimitiveIterable.stg.
 *
 * @see ReverseIterable
 * @since 5.0.
 */
public class ReverseByteIterable extends AbstractLazyByteIterable
{
    private final ByteList adapted;

    public ReverseByteIterable(ByteList newAdapted)
    {
        this.adapted = newAdapted;
    }

    public static ReverseByteIterable adapt(ByteList byteList)
    {
        return new ReverseByteIterable(byteList);
    }

    @Override
    public ByteIterator byteIterator()
    {
        return new ReverseByteIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ByteProcedure procedure)
    {
        ByteIterator iterator = this.byteIterator();
        while (iterator.hasNext())
        {
            procedure.value(iterator.next());
        }
    }

    @Override
    public long sum()
    {
        return this.adapted.sum();
    }

    @Override
    public byte max()
    {
        return this.adapted.max();
    }

    @Override
    public byte min()
    {
        return this.adapted.min();
    }

    @Override
    public byte minIfEmpty(byte defaultValue)
    {
        if (this.adapted.isEmpty())
        {
            return defaultValue;
        }
        return this.adapted.min();
    }

    @Override
    public byte maxIfEmpty(byte defaultValue)
    {
        if (this.adapted.isEmpty())
        {
            return defaultValue;
        }
        return this.adapted.max();
    }

    @Override
    public double average()
    {
        return this.adapted.average();
    }

    @Override
    public double median()
    {
        return this.adapted.median();
    }

    @Override
    public byte[] toSortedArray()
    {
        return this.adapted.toSortedArray();
    }

    @Override
    public byte[] toArray()
    {
        byte[] results = new byte[this.adapted.size()];
        int index = 0;
        ByteIterator iterator = this.byteIterator();
        while (iterator.hasNext())
        {
            results[index] = iterator.next();
            index++;
        }
        return results;
    }

    @Override
    public boolean contains(byte value)
    {
        return this.adapted.contains(value);
    }

    @Override
    public boolean containsAll(byte... source)
    {
        return this.adapted.containsAll(source);
    }

    @Override
    public boolean containsAll(ByteIterable source)
    {
        return this.adapted.containsAll(source);
    }

    @Override
    public int size()
    {
        return this.adapted.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.adapted.isEmpty();
    }

    @Override
    public boolean notEmpty()
    {
        return this.adapted.notEmpty();
    }

    @Override
    public MutableByteList toList()
    {
        return ByteLists.mutable.withAll(this);
    }

    @Override
    public MutableByteSet toSet()
    {
        return ByteSets.mutable.withAll(this);
    }

    @Override
    public MutableByteBag toBag()
    {
        return ByteBags.mutable.withAll(this);
    }

    @Override
    public LazyByteIterable asLazy()
    {
        return new LazyByteIterableAdapter(this);
    }

    private class ReverseByteIterator implements ByteIterator
    {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int currentIndex = ReverseByteIterable.this.adapted.size() - 1;

        @Override
        public boolean hasNext()
        {
            return this.currentIndex != -1;
        }

        @Override
        public byte next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            byte next = ReverseByteIterable.this.adapted.get(this.currentIndex);
            this.currentIndex--;
            return next;
        }
    }
}
