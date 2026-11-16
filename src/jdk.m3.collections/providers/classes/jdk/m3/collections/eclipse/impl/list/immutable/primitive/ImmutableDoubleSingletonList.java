/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.list.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;

import jdk.m3.collections.eclipse.api.DoubleIterable;
import jdk.m3.collections.eclipse.api.LazyDoubleIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableDoubleBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectDoubleToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectDoubleIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.DoublePredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.DoubleProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.DoubleIntProcedure;
import jdk.m3.collections.eclipse.api.iterator.DoubleIterator;
import jdk.m3.collections.eclipse.api.list.ImmutableList;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableDoubleList;
import jdk.m3.collections.eclipse.api.list.primitive.DoubleList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableDoubleList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableDoubleSet;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableDoubleStack;
import jdk.m3.collections.eclipse.api.tuple.primitive.DoubleDoublePair;
import jdk.m3.collections.eclipse.api.tuple.primitive.DoubleObjectPair;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.DoubleHashBag;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.DoubleLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.DoubleStacks;
import jdk.m3.collections.eclipse.impl.iterator.SingletonDoubleIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyDoubleIterableAdapter;
import jdk.m3.collections.eclipse.impl.lazy.primitive.ReverseDoubleIterable;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.DoubleArrayList;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.DoubleHashSet;
import jdk.m3.collections.eclipse.impl.utility.Iterate;
import jdk.m3.collections.eclipse.impl.tuple.primitive.PrimitiveTuples;
import java.util.Spliterator;
import jdk.m3.collections.eclipse.impl.stream.primitive.DoubleSingletonSpliterator;

/**
 * ImmutableDoubleSingletonList is an optimization for {@link ImmutableDoubleList} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonList.stg.
 */
final class ImmutableDoubleSingletonList implements ImmutableDoubleList, Serializable
{
    private static final long serialVersionUID = 1L;
    private final double element1;

    ImmutableDoubleSingletonList(double element)
    {
        this.element1 = element;
    }

    @Override
    public double get(int index)
    {
        if (index == 0)
        {
            return this.element1;
        }
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: 1");
    }

    @Override
    public double getFirst()
    {
        return this.element1;
    }

    @Override
    public double getLast()
    {
        return this.element1;
    }

    @Override
    public int indexOf(double value)
    {
        return Double.compare(this.element1, value) == 0 ? 0 : -1;
    }

    @Override
    public int lastIndexOf(double value)
    {
        return Double.compare(this.element1, value) == 0 ? 0 : -1;
    }

    @Override
    public DoubleIterator doubleIterator()
    {
        return new SingletonDoubleIterator(element1);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(DoubleProcedure procedure)
    {
        procedure.value(this.element1);
    }

    @Override
    public void forEachWithIndex(DoubleIntProcedure procedure)
    {
        procedure.value(this.element1, 0);
    }

    @Override
    public int count(DoublePredicate predicate)
    {
        return predicate.accept(this.element1) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(DoublePredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean noneSatisfy(DoublePredicate predicate)
    {
        return !predicate.accept(this.element1);
    }

    @Override
    public ImmutableDoubleList select(DoublePredicate predicate)
    {
        return predicate.accept(this.element1) ? DoubleArrayList.newListWith(this.element1).toImmutable()
                : new DoubleArrayList().toImmutable();
    }

    @Override
    public ImmutableDoubleList reject(DoublePredicate predicate)
    {
        return predicate.accept(this.element1) ? new DoubleArrayList().toImmutable()
                : DoubleArrayList.newListWith(this.element1).toImmutable();
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        return predicate.accept(this.element1) ? this.element1 : ifNone;
    }

    @Override
    public <V> ImmutableList<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        return Lists.immutable.with(function.valueOf(this.element1));
    }

    @Override
    public double sum()
    {
        return this.element1;
    }

    @Override
    public double max()
    {
        return this.element1;
    }

    @Override
    public double maxIfEmpty(double defaultValue)
    {
        return this.element1;
    }

    @Override
    public double min()
    {
        return this.element1;
    }

    @Override
    public double minIfEmpty(double defaultValue)
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
    public double[] toSortedArray()
    {
        return new double[]{this.element1};
    }

    @Override
    public MutableDoubleList toSortedList()
    {
        return DoubleArrayList.newListWith(this.element1);
    }

    @Override
    public int binarySearch(double value)
    {
        if (Double.compare(this.element1, value) == 0)
        {
            return 0;
        }
        if (Double.compare(this.element1, value) < 0)
        {
            return -2;
        }
        return -1;
    }

    @Override
    public double dotProduct(DoubleList list)
    {
        if (list.size() != 1)
        {
            throw new IllegalArgumentException("Lists used in dotProduct must be the same size");
        }
        return this.element1 * list.getFirst();
    }

    @Override
    public double[] toArray()
    {
        return new double[]{this.element1};
    }

    @Override
    public double[] toArray(double[] target)
    {
        if (target.length < 1)
        {
            target = new double[]{this.element1};
        }
        else
        {
            target[0] = this.element1;
        }
        return target;
    }

    @Override
    public boolean contains(double value)
    {
        return Double.compare(this.element1, value) == 0;
    }

    @Override
    public boolean containsAll(double... source)
    {
        for (double value : source)
        {
            if (Double.compare(this.element1, value) != 0)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(DoubleIterable source)
    {
        for (DoubleIterator iterator = source.doubleIterator(); iterator.hasNext(); )
        {
            if (Double.compare(this.element1, iterator.next()) != 0)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public LazyDoubleIterable asReversed()
    {
        return ReverseDoubleIterable.adapt(this);
    }

    @Override
    public MutableDoubleList toList()
    {
        return DoubleArrayList.newListWith(this.element1);
    }

    @Override
    public MutableDoubleSet toSet()
    {
        return DoubleHashSet.newSetWith(this.element1);
    }

    @Override
    public MutableDoubleBag toBag()
    {
        return DoubleHashBag.newBagWith(this.element1);
    }

    @Override
    public LazyDoubleIterable asLazy()
    {
        return new LazyDoubleIterableAdapter(this);
    }

    @Override
    public ImmutableDoubleList toImmutable()
    {
        return this;
    }

    @Override
    public ImmutableDoubleSingletonList toReversed()
    {
        return this;
    }

    @Override
    public ImmutableDoubleList newWith(double element)
    {
        return DoubleLists.immutable.with(this.element1, element);
    }

    @Override
    public ImmutableDoubleList newWithout(double element)
    {
        return Double.compare(this.element1, element) == 0 ? DoubleLists.immutable.with() : this;
    }

    @Override
    public ImmutableDoubleList newWithAll(DoubleIterable elements)
    {
        DoubleArrayList arrayList = DoubleArrayList.newListWith(this.element1);
        arrayList.addAll(elements);
        return arrayList.toImmutable();
    }

    @Override
    public ImmutableDoubleList newWithoutAll(DoubleIterable elements)
    {
        return elements.contains(this.element1) ? DoubleLists.immutable.with() : this;
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
    public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1);
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectDoubleIntToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1, 0);
    }

    @Override
    public RichIterable<DoubleIterable> chunk(int size)
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
        if (!(otherList instanceof DoubleList))
        {
            return false;
        }
        DoubleList list = (DoubleList) otherList;
        if (list.size() != 1)
        {
            return false;
        }
        return Double.compare(this.element1, list.get(0)) == 0;
    }

    @Override
    public int hashCode()
    {
        return 31 + (int) (Double.doubleToLongBits(this.element1) ^ Double.doubleToLongBits(this.element1) >>> 32);
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
    public ImmutableDoubleList distinct()
    {
        return this;
    }

    @Override
    public ImmutableDoubleList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    @Override
    public ImmutableList<DoubleDoublePair> zipDouble(DoubleIterable iterable)
    {
        if (iterable.isEmpty())
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.with(PrimitiveTuples.pair(this.element1, iterable.doubleIterator().next()));
    }

    /**
     * @since 9.1.
     */
    @Override
    public <T> ImmutableList<DoubleObjectPair<T>> zip(Iterable<T> iterable)
    {
        if (Iterate.isEmpty(iterable))
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.with(PrimitiveTuples.pair(this.element1, Iterate.getFirst(iterable)));
    }

    @Override
    public MutableDoubleStack toStack()
    {
        return DoubleStacks.mutable.with(this.element1);
    }

    @Override
    public Spliterator.OfDouble spliterator()
    {
        return new DoubleSingletonSpliterator(this.element1);
    }
}
