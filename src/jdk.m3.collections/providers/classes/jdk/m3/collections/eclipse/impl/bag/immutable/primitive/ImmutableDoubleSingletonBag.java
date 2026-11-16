/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.bag.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;

import jdk.m3.collections.eclipse.api.DoubleIterable;
import jdk.m3.collections.eclipse.api.LazyDoubleIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.ImmutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableDoubleBag;
import jdk.m3.collections.eclipse.api.bag.primitive.DoubleBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableDoubleBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectDoubleToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.DoublePredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.DoubleIntProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.DoubleProcedure;
import jdk.m3.collections.eclipse.api.iterator.DoubleIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableDoubleList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableDoubleSet;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.DoubleHashBag;
import jdk.m3.collections.eclipse.impl.factory.Bags;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.DoubleBags;
import jdk.m3.collections.eclipse.impl.iterator.SingletonDoubleIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyDoubleIterableAdapter;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.DoubleArrayList;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.DoubleHashSet;
import jdk.m3.collections.eclipse.api.list.ImmutableList;
import jdk.m3.collections.eclipse.api.tuple.primitive.DoubleIntPair;
import jdk.m3.collections.eclipse.impl.tuple.primitive.PrimitiveTuples;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableDoubleSet;
import jdk.m3.collections.eclipse.impl.factory.primitive.DoubleSets;

/**
 * ImmutableDoubleSingletonBag is an optimization for {@link ImmutableDoubleBag} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonBag.stg.
 *
 * @since 4.0.
 */
final class ImmutableDoubleSingletonBag implements ImmutableDoubleBag, Serializable
{
    private static final long serialVersionUID = 1L;

    private final double element1;

    ImmutableDoubleSingletonBag(double element)
    {
        this.element1 = element;
    }

    @Override
    public ImmutableDoubleBag newWith(double element)
    {
        return DoubleBags.immutable.with(this.element1, element);
    }

    @Override
    public ImmutableDoubleBag newWithout(double element)
    {
        return Double.compare(this.element1, element) == 0 ? DoubleBags.immutable.with() : this;
    }

    @Override
    public ImmutableDoubleBag newWithAll(DoubleIterable elements)
    {
        return DoubleHashBag.newBag(elements).with(this.element1).toImmutable();
    }

    @Override
    public ImmutableDoubleBag newWithoutAll(DoubleIterable elements)
    {
        return elements.contains(this.element1) ? DoubleBags.immutable.with() : this;
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
    public boolean contains(double value)
    {
        return Double.compare(this.element1, value) == 0;
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

    /**
     * @since 7.0.
     */
    @Override
    public void each(DoubleProcedure procedure)
    {
        procedure.value(this.element1);
    }

    @Override
    public ImmutableDoubleBag select(DoublePredicate predicate)
    {
        return predicate.accept(this.element1)
            ? DoubleBags.immutable.with(this.element1)
            : DoubleBags.immutable.empty();
    }

    @Override
    public ImmutableDoubleBag selectByOccurrences(IntPredicate predicate)
    {
        return predicate.accept(1)
            ? DoubleBags.immutable.with(this.element1)
            : DoubleBags.immutable.empty();
    }

    @Override
    public ImmutableDoubleSet selectUnique()
    {
        return DoubleSets.immutable.of(this.element1);
    }

    @Override
    public ImmutableList<DoubleIntPair> topOccurrences(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Cannot use a value of count < 0");
        }
        if (count == 0)
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.with(PrimitiveTuples.pair(this.element1, 1));
    }

    @Override
    public ImmutableList<DoubleIntPair> bottomOccurrences(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Cannot use a value of count < 0");
        }
        if (count == 0)
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.with(PrimitiveTuples.pair(this.element1, 1));
    }

    @Override
    public ImmutableDoubleBag reject(DoublePredicate predicate)
    {
        return predicate.accept(this.element1)
            ? DoubleBags.immutable.empty()
            : DoubleBags.immutable.with(this.element1);
    }

    @Override
    public <V> ImmutableBag<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        return Bags.immutable.with(function.valueOf(this.element1));
    }

    @Override
    public MutableDoubleList toList()
    {
        return DoubleArrayList.newListWith(this.element1);
    }

    @Override
    public int sizeDistinct()
    {
        return 1;
    }

    @Override
    public int occurrencesOf(double item)
    {
        return Double.compare(this.element1, item) == 0 ? 1 : 0;
    }

    @Override
    public void forEachWithOccurrences(DoubleIntProcedure doubleIntProcedure)
    {
        doubleIntProcedure.value(this.element1, 1);
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        return predicate.accept(this.element1) ? this.element1 : ifNone;
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
    public double sum()
    {
        return this.element1;
    }

    @Override
    public double min()
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
    public boolean noneSatisfy(DoublePredicate predicate)
    {
        return !predicate.accept(this.element1);
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1);
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
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof DoubleBag))
        {
            return false;
        }
        DoubleBag bag = (DoubleBag) obj;
        if (bag.size() != 1)
        {
            return false;
        }
        return this.occurrencesOf(this.element1) == bag.occurrencesOf(this.element1);
    }

    @Override
    public int hashCode()
    {
        return (int) (Double.doubleToLongBits(this.element1) ^ Double.doubleToLongBits(this.element1) >>> 32) ^ 1;
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
    public ImmutableDoubleBag toImmutable()
    {
        return this;
    }

    @Override
    public LazyDoubleIterable asLazy()
    {
        return new LazyDoubleIterableAdapter(this);
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
    public String toString()
    {
        return '[' + this.makeString() + ']';
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

    @Override
    public DoubleIterator doubleIterator()
    {
        return new SingletonDoubleIterator(element1);
    }
}
