/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.list.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.LazyBooleanIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableBooleanBag;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.BooleanIntProcedure;
import org.eclipse.collections.api.iterator.BooleanIterator;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;
import org.eclipse.collections.api.list.primitive.BooleanList;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;
import org.eclipse.collections.api.tuple.primitive.BooleanBooleanPair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.impl.bag.mutable.primitive.BooleanHashBag;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.BooleanLists;
import org.eclipse.collections.impl.iterator.ImmutableEmptyBooleanIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyBooleanIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.ReverseBooleanIterable;
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.BooleanHashSet;

/**
 * ImmutableBooleanEmptyList is an optimization for {@link ImmutableBooleanList} of size 0.
 * This file was automatically generated from template file immutablePrimitiveEmptyList.stg.
 */
final class ImmutableBooleanEmptyList implements ImmutableBooleanList, Serializable
{
    static final ImmutableBooleanList INSTANCE = new ImmutableBooleanEmptyList();
    private static final long serialVersionUID = 1L;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public boolean get(int index)
    {
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: 0");
    }

    @Override
    public boolean getFirst()
    {
        throw new IndexOutOfBoundsException("Index: 0, Size: 0");
    }

    @Override
    public boolean getLast()
    {
        throw new IndexOutOfBoundsException("Index: 0, Size: 0");
    }

    @Override
    public int indexOf(boolean value)
    {
        return -1;
    }

    @Override
    public int lastIndexOf(boolean value)
    {
        return -1;
    }

    @Override
    public BooleanIterator booleanIterator()
    {
        return ImmutableEmptyBooleanIterator.INSTANCE;
    }

    @Override
    public void forEach(BooleanProcedure procedure)
    {
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(BooleanProcedure procedure)
    {
    }

    @Override
    public void forEachWithIndex(BooleanIntProcedure procedure)
    {
    }

    @Override
    public int count(BooleanPredicate predicate)
    {
        return 0;
    }

    @Override
    public boolean anySatisfy(BooleanPredicate predicate)
    {
        return false;
    }

    @Override
    public boolean allSatisfy(BooleanPredicate predicate)
    {
        return true;
    }

    @Override
    public boolean noneSatisfy(BooleanPredicate predicate)
    {
        return true;
    }

    @Override
    public ImmutableBooleanList select(BooleanPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableBooleanList reject(BooleanPredicate predicate)
    {
        return this;
    }

    @Override
    public boolean detectIfNone(BooleanPredicate predicate, boolean ifNone)
    {
        return ifNone;
    }

    @Override
    public <V> ImmutableList<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        return Lists.immutable.of();
    }

    @Override
    public boolean[] toArray()
    {
        return new boolean[0];
    }

    @Override
    public boolean[] toArray(boolean[] target)
    {
        return target;
    }

    @Override
    public boolean contains(boolean value)
    {
        return false;
    }

    @Override
    public boolean containsAll(boolean... source)
    {
        return source.length == 0;
    }

    @Override
    public boolean containsAll(BooleanIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public LazyBooleanIterable asReversed()
    {
        return ReverseBooleanIterable.adapt(this);
    }

    @Override
    public MutableBooleanList toList()
    {
        return new BooleanArrayList();
    }

    @Override
    public MutableBooleanSet toSet()
    {
        return new BooleanHashSet();
    }

    @Override
    public MutableBooleanBag toBag()
    {
        return new BooleanHashBag();
    }

    @Override
    public LazyBooleanIterable asLazy()
    {
        return new LazyBooleanIterableAdapter(this);
    }

    @Override
    public ImmutableBooleanList toImmutable()
    {
        return this;
    }

    @Override
    public ImmutableBooleanEmptyList toReversed()
    {
        return this;
    }

    @Override
    public ImmutableBooleanList newWith(boolean element)
    {
        return BooleanLists.immutable.with(element);
    }

    @Override
    public ImmutableBooleanList newWithout(boolean element)
    {
        return this;
    }

    @Override
    public ImmutableBooleanList newWithAll(BooleanIterable elements)
    {
        return BooleanLists.immutable.withAll(elements);
    }

    @Override
    public ImmutableBooleanList newWithoutAll(BooleanIterable elements)
    {
        return this;
    }

    @Override
    public int size()
    {
        return 0;
    }

    @Override
    public boolean isEmpty()
    {
        return true;
    }

    @Override
    public boolean notEmpty()
    {
        return false;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectBooleanToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectBooleanIntToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public RichIterable<BooleanIterable> chunk(int size)
    {
        return Lists.immutable.empty();
    }

    @Override
    public boolean equals(Object otherList)
    {
        if (otherList == this)
        {
            return true;
        }
        if (!(otherList instanceof BooleanList))
        {
            return false;
        }
        BooleanList list = (BooleanList) otherList;
        return list.isEmpty();
    }

    @Override
    public int hashCode()
    {
        return 1;
    }

    @Override
    public String toString()
    {
        return "[]";
    }

    @Override
    public String makeString()
    {
        return "";
    }

    @Override
    public String makeString(String separator)
    {
        return "";
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        return start + end;
    }

    @Override
    public void appendString(Appendable appendable)
    {
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        try
        {
            appendable.append(start);
            appendable.append(end);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * @since 6.0.
     */
    @Override
    public ImmutableBooleanList distinct()
    {
        return INSTANCE;
    }

    @Override
    public ImmutableBooleanList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    @Override
    public ImmutableList<BooleanBooleanPair> zipBoolean(BooleanIterable iterable)
    {
        return Lists.immutable.empty();
    }

    /**
     * @since 9.1.
     */
    @Override
    public <T> ImmutableList<BooleanObjectPair<T>> zip(Iterable<T> iterable)
    {
        return Lists.immutable.empty();
    }
}
