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
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.factory.primitive.ByteBags;
import org.eclipse.collections.api.factory.primitive.ByteLists;
import org.eclipse.collections.api.factory.primitive.ByteSets;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.set.primitive.MutableByteSet;

/**
 * This file was automatically generated from template file selectPrimitiveIterable.stg.
 */
public class SelectByteIterable
        extends AbstractLazyByteIterable
{
    private final ByteIterable delegate;
    private final BytePredicate predicate;

    public SelectByteIterable(ByteIterable delegate, BytePredicate predicate)
    {
        this.delegate = delegate;
        this.predicate = predicate;
    }

    @Override
    public ByteIterator byteIterator()
    {
        return new SelectByteIterator(this.delegate, this.predicate);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ByteProcedure procedure)
    {
        this.delegate.forEach(new IfByteProcedure(procedure));
    }

    @Override
    public int size()
    {
        return this.delegate.count(this.predicate);
    }

    @Override
    public boolean isEmpty()
    {
        return !this.byteIterator().hasNext();
    }

    @Override
    public boolean notEmpty()
    {
        return this.byteIterator().hasNext();
    }

    @Override
    public int count(BytePredicate predicate)
    {
        CountByteProcedure countByteProcedure = new CountByteProcedure(predicate);
        this.forEach(countByteProcedure);
        return countByteProcedure.getCount();
    }

    @Override
    public boolean anySatisfy(BytePredicate predicate)
    {
        return this.delegate.anySatisfy(each -> this.predicate.accept(each) && predicate.accept(each));
    }

    @Override
    public boolean allSatisfy(BytePredicate predicate)
    {
        return this.noneSatisfy(each -> !predicate.accept(each));
    }

    @Override
    public byte[] toArray()
    {
        final byte[] array = new byte[this.size()];
        this.forEach(new ByteProcedure()
        {
            @SuppressWarnings("FieldMayBeFinal")
            private int index = 0;

            @Override
            public void value(byte each)
            {
                array[this.index++] = each;
            }
        });
        return array;
    }

    @Override
    public boolean containsAll(byte... source)
    {
        for (byte value : source)
        {
            if (!this.contains(value))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(ByteIterable source)
    {
        for (ByteIterator iterator = source.byteIterator(); iterator.hasNext(); )
        {
            if (!this.contains(iterator.next()))
            {
                return false;
            }
        }
        return true;
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

    private static final class CountByteProcedure implements ByteProcedure
    {
        private static final long serialVersionUID = 1L;
        private final BytePredicate predicate;
        private int counter = 0;

        private CountByteProcedure(BytePredicate predicate)
        {
            this.predicate = predicate;
        }

        @Override
        public void value(byte each)
        {
            if (this.predicate.accept(each))
            {
                this.counter++;
            }
        }

        public int getCount()
        {
            return this.counter;
        }
    }

    private final class IfByteProcedure implements ByteProcedure
    {
        private static final long serialVersionUID = 1L;
        private final ByteProcedure procedure;

        private IfByteProcedure(ByteProcedure procedure)
        {
            this.procedure = procedure;
        }

        @Override
        public void value(byte each)
        {
            if (SelectByteIterable.this.predicate.accept(each))
            {
                this.procedure.value(each);
            }
        }
    }

    private static final class SelectByteIterator
            implements ByteIterator
    {
        private final ByteIterator iterator;
        private final BytePredicate predicate;
        private byte next;
        private boolean verifiedHasNext = false;

        private SelectByteIterator(ByteIterable iterable, BytePredicate predicate)
        {
            this(iterable.byteIterator(), predicate);
        }

        private SelectByteIterator(ByteIterator iterator, BytePredicate predicate)
        {
            this.iterator = iterator;
            this.predicate = predicate;
        }

        @Override
        public boolean hasNext()
        {
            if (this.verifiedHasNext)
            {
                return true;
            }
            while (this.iterator.hasNext())
            {
                byte temp = this.iterator.next();
                if (this.predicate.accept(temp))
                {
                    this.next = temp;
                    this.verifiedHasNext = true;
                    return true;
                }
            }
            return false;
        }

        @Override
        public byte next()
        {
            if (this.verifiedHasNext || this.hasNext())
            {
                this.verifiedHasNext = false;
                return this.next;
            }
            throw new NoSuchElementException();
        }
    }
}
