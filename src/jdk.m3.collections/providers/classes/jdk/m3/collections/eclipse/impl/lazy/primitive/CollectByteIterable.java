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

import java.util.Arrays;
import java.util.Iterator;

import jdk.m3.collections.eclipse.api.ByteIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableByteBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.ByteFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.BytePredicate;
import jdk.m3.collections.eclipse.api.block.procedure.Procedure2;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ByteProcedure;
import jdk.m3.collections.eclipse.api.factory.primitive.ByteBags;
import jdk.m3.collections.eclipse.api.factory.primitive.ByteLists;
import jdk.m3.collections.eclipse.api.factory.primitive.ByteSets;
import jdk.m3.collections.eclipse.api.iterator.ByteIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableByteList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableByteSet;

/**
 * This file was automatically generated from template file collectPrimitiveIterable.stg.
 */
@SuppressWarnings("deprecation")
public class CollectByteIterable<T>
        extends AbstractLazyByteIterable
{
    private final RichIterable<T> iterable;
    private final ByteFunction<? super T> function;
    private final ByteFunctionToProcedure<T> byteFunctionToProcedure;

    public CollectByteIterable(RichIterable<T> adapted, ByteFunction<? super T> function)
    {
        this.iterable = adapted;
        this.function = function;
        this.byteFunctionToProcedure = new ByteFunctionToProcedure<>(function);
    }

    @Override
    public ByteIterator byteIterator()
    {
        return new ByteIterator()
        {
            private final Iterator<T> iterator = CollectByteIterable.this.iterable.iterator();

            @Override
            public byte next()
            {
                return CollectByteIterable.this.function.byteValueOf(this.iterator.next());
            }

            @Override
            public boolean hasNext()
            {
                return this.iterator.hasNext();
            }
        };
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ByteProcedure procedure)
    {
        this.iterable.forEachWith(this.byteFunctionToProcedure, procedure);
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

    @Override
    public int count(final BytePredicate predicate)
    {
        return this.iterable.count((T each) -> predicate.accept(this.function.byteValueOf(each)));
    }

    @Override
    public boolean anySatisfy(final BytePredicate predicate)
    {
        return this.iterable.anySatisfy((T each) -> predicate.accept(this.function.byteValueOf(each)));
    }

    @Override
    public boolean allSatisfy(final BytePredicate predicate)
    {
        return this.iterable.allSatisfy((T each) -> predicate.accept(this.function.byteValueOf(each)));
    }

    @Override
    public byte[] toArray()
    {
        final byte[] array = new byte[this.size()];
        this.iterable.forEachWithIndex((T each, int index) -> array[index] = this.function.byteValueOf(each));
        return array;
    }

    @Override
    public byte[] toSortedArray()
    {
        byte[] array = this.toArray();
        Arrays.sort(array);
        return array;
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

    private static final class ByteFunctionToProcedure<T> implements Procedure2<T, ByteProcedure>
    {
        private static final long serialVersionUID = 1L;
        private final ByteFunction<? super T> function;

        private ByteFunctionToProcedure(ByteFunction<? super T> function)
        {
            this.function = function;
        }

        @Override
        public void value(T each, ByteProcedure procedure)
        {
            procedure.value(this.function.byteValueOf(each));
        }
    }
}
