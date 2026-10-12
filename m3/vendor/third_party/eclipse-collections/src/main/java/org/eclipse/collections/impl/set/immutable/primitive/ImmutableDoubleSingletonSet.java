/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.set.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.factory.primitive.DoubleBags;
import org.eclipse.collections.api.factory.primitive.DoubleLists;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableDoubleSet;
import org.eclipse.collections.api.set.primitive.DoubleSet;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.impl.iterator.SingletonDoubleIterator;
import org.eclipse.collections.impl.set.mutable.primitive.DoubleHashSet;
import org.eclipse.collections.impl.factory.primitive.DoubleSets;
import org.eclipse.collections.impl.lazy.primitive.LazyDoubleIterableAdapter;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.tuple.primitive.DoubleDoublePair;

/**
 * ImmutableDoubleSingletonSet is an optimization for {@link ImmutableDoubleSet} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonSet.stg.
 *
 * @since 4.0.
 */
final class ImmutableDoubleSingletonSet implements ImmutableDoubleSet, Serializable
{
    private static final long serialVersionUID = 1L;

    private final double element;

    ImmutableDoubleSingletonSet(double element)
    {
        this.element = element;
    }

    @Override
    public ImmutableDoubleSet newWith(double element)
    {
        return DoubleSets.immutable.with(this.element, element);
    }

    @Override
    public ImmutableDoubleSet newWithout(double element)
    {
        return Double.compare(this.element, element) == 0 ? DoubleSets.immutable.with() : this;
    }

    @Override
    public ImmutableDoubleSet newWithAll(DoubleIterable elements)
    {
        return DoubleHashSet.newSet(elements).with(this.element).toImmutable();
    }

    @Override
    public ImmutableDoubleSet newWithoutAll(DoubleIterable elements)
    {
        return elements.contains(this.element) ? DoubleSets.immutable.with() : this;
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
        return Double.compare(this.element, value) == 0;
    }

    @Override
    public boolean containsAll(DoubleIterable source)
    {
        for (DoubleIterator iterator = source.doubleIterator(); iterator.hasNext(); )
        {
            if (Double.compare(this.element, iterator.next()) != 0)
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
            if (Double.compare(this.element, value) != 0)
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
        procedure.value(this.element);
    }

    @Override
    public ImmutableDoubleSet select(DoublePredicate predicate)
    {
        return predicate.accept(this.element) ? DoubleHashSet.newSetWith(this.element).toImmutable()
                : new DoubleHashSet().toImmutable();
    }

    @Override
    public ImmutableDoubleSet reject(DoublePredicate predicate)
    {
        return predicate.accept(this.element) ? new DoubleHashSet().toImmutable()
                : DoubleHashSet.newSetWith(this.element).toImmutable();
    }

    @Override
    public <V> ImmutableSet<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        return Sets.immutable.with(function.valueOf(this.element));
    }

    @Override
    public MutableDoubleList toList()
    {
        return DoubleLists.mutable.with(this.element);
    }

    public int sizeDistinct()
    {
        return 1;
    }

    public int occurrencesOf(double item)
    {
        return Double.compare(this.element, item) == 0 ? 1 : 0;
    }

    public void forEachWithOccurrences(DoubleIntProcedure doubleIntProcedure)
    {
        doubleIntProcedure.value(this.element, 1);
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        return predicate.accept(this.element) ? this.element : ifNone;
    }

    @Override
    public int count(DoublePredicate predicate)
    {
        return predicate.accept(this.element) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(DoublePredicate predicate)
    {
        return predicate.accept(this.element);
    }

    @Override
    public double sum()
    {
        return this.element;
    }

    @Override
    public double min()
    {
        return this.element;
    }

    @Override
    public double max()
    {
        return this.element;
    }

    @Override
    public double maxIfEmpty(double defaultValue)
    {
        return this.element;
    }

    @Override
    public double minIfEmpty(double defaultValue)
    {
        return this.element;
    }

    @Override
    public double average()
    {
        return this.element;
    }

    @Override
    public double median()
    {
        return this.element;
    }

    @Override
    public LazyIterable<DoubleDoublePair> cartesianProduct(DoubleSet set)
    {
        return DoubleSets.cartesianProduct(this, set);
    }

    @Override
    public double[] toSortedArray()
    {
        return new double[]{this.element};
    }

    @Override
    public MutableDoubleList toSortedList()
    {
        return DoubleLists.mutable.with(this.element);
    }

    @Override
    public boolean noneSatisfy(DoublePredicate predicate)
    {
        return !predicate.accept(this.element);
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        return predicate.accept(this.element);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element);
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
        if (!(obj instanceof DoubleSet))
        {
            return false;
        }
        DoubleSet set = (DoubleSet) obj;
        if (set.size() != 1)
        {
            return false;
        }
        return set.contains(this.element);
    }

    @Override
    public int hashCode()
    {
        return (int) (Double.doubleToLongBits(this.element) ^ Double.doubleToLongBits(this.element) >>> 32);
    }

    @Override
    public MutableDoubleSet toSet()
    {
        return DoubleSets.mutable.with(this.element);
    }

    @Override
    public MutableDoubleBag toBag()
    {
        return DoubleBags.mutable.with(this.element);
    }

    @Override
    public DoubleSet freeze()
    {
        return this;
    }

    @Override
    public ImmutableDoubleSet toImmutable()
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
        return new double[]{this.element};
    }

    @Override
    public double[] toArray(double[] target)
    {
        if (target.length < 1)
        {
            target = new double[]{this.element};
        }
        else
        {
            target[0] = this.element;
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
            appendable.append(String.valueOf(this.element));
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
        return new SingletonDoubleIterator(element);
    }
}
