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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.IntIntProcedure;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.bag.mutable.primitive.IntHashBag;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.eclipse.collections.impl.iterator.SingletonIntIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyIntIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.ReverseIntIterable;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import java.util.Spliterator;
import org.eclipse.collections.impl.stream.primitive.IntSingletonSpliterator;

/**
 * ImmutableIntSingletonList is an optimization for {@link ImmutableIntList} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonList.stg.
 */
final class ImmutableIntSingletonList implements ImmutableIntList, Serializable
{
    private static final long serialVersionUID = 1L;
    private final int element1;

    ImmutableIntSingletonList(int element)
    {
        this.element1 = element;
    }

    @Override
    public int get(int index)
    {
        if (index == 0)
        {
            return this.element1;
        }
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: 1");
    }

    @Override
    public int getFirst()
    {
        return this.element1;
    }

    @Override
    public int getLast()
    {
        return this.element1;
    }

    @Override
    public int indexOf(int value)
    {
        return this.element1 == value ? 0 : -1;
    }

    @Override
    public int lastIndexOf(int value)
    {
        return this.element1 == value ? 0 : -1;
    }

    @Override
    public IntIterator intIterator()
    {
        return new SingletonIntIterator(element1);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(IntProcedure procedure)
    {
        procedure.value(this.element1);
    }

    @Override
    public void forEachWithIndex(IntIntProcedure procedure)
    {
        procedure.value(this.element1, 0);
    }

    @Override
    public int count(IntPredicate predicate)
    {
        return predicate.accept(this.element1) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(IntPredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean allSatisfy(IntPredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean noneSatisfy(IntPredicate predicate)
    {
        return !predicate.accept(this.element1);
    }

    @Override
    public ImmutableIntList select(IntPredicate predicate)
    {
        return predicate.accept(this.element1) ? IntArrayList.newListWith(this.element1).toImmutable()
                : new IntArrayList().toImmutable();
    }

    @Override
    public ImmutableIntList reject(IntPredicate predicate)
    {
        return predicate.accept(this.element1) ? new IntArrayList().toImmutable()
                : IntArrayList.newListWith(this.element1).toImmutable();
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int ifNone)
    {
        return predicate.accept(this.element1) ? this.element1 : ifNone;
    }

    @Override
    public <V> ImmutableList<V> collect(IntToObjectFunction<? extends V> function)
    {
        return Lists.immutable.with(function.valueOf(this.element1));
    }

    @Override
    public long sum()
    {
        return this.element1;
    }

    @Override
    public int max()
    {
        return this.element1;
    }

    @Override
    public int maxIfEmpty(int defaultValue)
    {
        return this.element1;
    }

    @Override
    public int min()
    {
        return this.element1;
    }

    @Override
    public int minIfEmpty(int defaultValue)
    {
        return this.element1;
    }

    @Override
    public double average()
    {
        return this.element1;
    }

    @Override
    public double median()
    {
        return this.element1;
    }

    @Override
    public int[] toSortedArray()
    {
        return new int[]{this.element1};
    }

    @Override
    public MutableIntList toSortedList()
    {
        return IntArrayList.newListWith(this.element1);
    }

    @Override
    public int binarySearch(int value)
    {
        if (this.element1 == value)
        {
            return 0;
        }
        if (this.element1 < value)
        {
            return -2;
        }
        return -1;
    }

    @Override
    public long dotProduct(IntList list)
    {
        if (list.size() != 1)
        {
            throw new IllegalArgumentException("Lists used in dotProduct must be the same size");
        }
        return (long) this.element1 * list.getFirst();
    }

    @Override
    public int[] toArray()
    {
        return new int[]{this.element1};
    }

    @Override
    public int[] toArray(int[] target)
    {
        if (target.length < 1)
        {
            target = new int[]{this.element1};
        }
        else
        {
            target[0] = this.element1;
        }
        return target;
    }

    @Override
    public boolean contains(int value)
    {
        return this.element1 == value;
    }

    @Override
    public boolean containsAll(int... source)
    {
        for (int value : source)
        {
            if (this.element1 != value)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(IntIterable source)
    {
        for (IntIterator iterator = source.intIterator(); iterator.hasNext(); )
        {
            if (this.element1 != iterator.next())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public LazyIntIterable asReversed()
    {
        return ReverseIntIterable.adapt(this);
    }

    @Override
    public MutableIntList toList()
    {
        return IntArrayList.newListWith(this.element1);
    }

    @Override
    public MutableIntSet toSet()
    {
        return IntHashSet.newSetWith(this.element1);
    }

    @Override
    public MutableIntBag toBag()
    {
        return IntHashBag.newBagWith(this.element1);
    }

    @Override
    public LazyIntIterable asLazy()
    {
        return new LazyIntIterableAdapter(this);
    }

    @Override
    public ImmutableIntList toImmutable()
    {
        return this;
    }

    @Override
    public ImmutableIntSingletonList toReversed()
    {
        return this;
    }

    @Override
    public ImmutableIntList newWith(int element)
    {
        return IntLists.immutable.with(this.element1, element);
    }

    @Override
    public ImmutableIntList newWithout(int element)
    {
        return this.element1 == element ? IntLists.immutable.with() : this;
    }

    @Override
    public ImmutableIntList newWithAll(IntIterable elements)
    {
        IntArrayList arrayList = IntArrayList.newListWith(this.element1);
        arrayList.addAll(elements);
        return arrayList.toImmutable();
    }

    @Override
    public ImmutableIntList newWithoutAll(IntIterable elements)
    {
        return elements.contains(this.element1) ? IntLists.immutable.with() : this;
    }

    @Override
    public int size()
    {
        return 1;
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public boolean notEmpty()
    {
        return true;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1);
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectIntIntToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1, 0);
    }

    @Override
    public RichIterable<IntIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        return Lists.immutable.with(this);
    }

    @Override
    public boolean equals(Object otherList)
    {
        if (otherList == this)
        {
            return true;
        }
        if (!(otherList instanceof IntList))
        {
            return false;
        }
        IntList list = (IntList) otherList;
        if (list.size() != 1)
        {
            return false;
        }
        return this.element1 == list.get(0);
    }

    @Override
    public int hashCode()
    {
        return 31 + this.element1;
    }

    @Override
    public String toString()
    {
        return "[" + this.element1 + ']';
    }

    @Override
    public String makeString()
    {
        return this.makeString(", ");
    }

    @Override
    public String makeString(String separator)
    {
        return this.makeString("", separator, "");
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        Appendable stringBuilder = new StringBuilder();
        this.appendString(stringBuilder, start, separator, end);
        return stringBuilder.toString();
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.appendString(appendable, ", ");
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.appendString(appendable, "", separator, "");
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        try
        {
            appendable.append(start);
            appendable.append(String.valueOf(this.element1));
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
    public ImmutableIntList distinct()
    {
        return this;
    }

    @Override
    public ImmutableIntList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    @Override
    public ImmutableList<IntIntPair> zipInt(IntIterable iterable)
    {
        if (iterable.isEmpty())
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.with(PrimitiveTuples.pair(this.element1, iterable.intIterator().next()));
    }

    /**
     * @since 9.1.
     */
    @Override
    public <T> ImmutableList<IntObjectPair<T>> zip(Iterable<T> iterable)
    {
        if (Iterate.isEmpty(iterable))
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.with(PrimitiveTuples.pair(this.element1, Iterate.getFirst(iterable)));
    }

    @Override
    public Spliterator.OfInt spliterator()
    {
        return new IntSingletonSpliterator(this.element1);
    }
}
