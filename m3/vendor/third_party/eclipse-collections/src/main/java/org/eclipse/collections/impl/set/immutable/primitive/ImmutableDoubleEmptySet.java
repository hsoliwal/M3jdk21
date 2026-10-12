/*
 * Copyright (c) 2022 Goldman Sachs.
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
import java.util.NoSuchElementException;

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.LazyIterable;
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
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.tuple.primitive.DoubleDoublePair;
import org.eclipse.collections.impl.factory.primitive.DoubleSets;
import org.eclipse.collections.impl.iterator.ImmutableEmptyDoubleIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyDoubleIterableAdapter;

/**
 * ImmutableDoubleEmptySet is an optimization for {@link ImmutableDoubleSet} of size 0.
 * This file was automatically generated from template file immutablePrimitiveEmptySet.stg.
 *
 * @since 4.0.
 */
final class ImmutableDoubleEmptySet implements ImmutableDoubleSet, Serializable
{
    static final ImmutableDoubleSet INSTANCE = new ImmutableDoubleEmptySet();

    private static final long serialVersionUID = 1L;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public ImmutableDoubleSet newWith(double element)
    {
        return new ImmutableDoubleSingletonSet(element);
    }

    @Override
    public ImmutableDoubleSet newWithout(double element)
    {
        return this;
    }

    @Override
    public ImmutableDoubleSet newWithAll(DoubleIterable elements)
    {
        return DoubleSets.immutable.withAll(elements);
    }

    @Override
    public ImmutableDoubleSet newWithoutAll(DoubleIterable elements)
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
    public boolean contains(double value)
    {
        return false;
    }

    @Override
    public boolean containsAll(DoubleIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public boolean containsAll(double... elements)
    {
        return elements.length == 0;
    }

    @Override
    public void forEach(DoubleProcedure procedure)
    {
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(DoubleProcedure procedure)
    {
    }

    @Override
    public ImmutableDoubleSet select(DoublePredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableDoubleSet reject(DoublePredicate predicate)
    {
        return this;
    }

    @Override
    public <V> ImmutableSet<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        return Sets.immutable.of();
    }

    @Override
    public MutableDoubleList toList()
    {
        return DoubleLists.mutable.empty();
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        return ifNone;
    }

    @Override
    public int count(DoublePredicate predicate)
    {
        return 0;
    }

    @Override
    public boolean anySatisfy(DoublePredicate predicate)
    {
        return false;
    }

    @Override
    public double sum()
    {
        return 0.0;
    }

    @Override
    public double min()
    {
        throw new NoSuchElementException();
    }

    @Override
    public double max()
    {
        throw new NoSuchElementException();
    }

    @Override
    public double maxIfEmpty(double defaultValue)
    {
        return defaultValue;
    }

    @Override
    public double minIfEmpty(double defaultValue)
    {
        return defaultValue;
    }

    @Override
    public double average()
    {
        throw new ArithmeticException();
    }

    @Override
    public double median()
    {
        throw new ArithmeticException();
    }

    @Override
    public double[] toSortedArray()
    {
        return new double[0];
    }

    @Override
    public MutableDoubleList toSortedList()
    {
        return DoubleLists.mutable.empty();
    }

    @Override
    public LazyIterable<DoubleDoublePair> cartesianProduct(DoubleSet set)
    {
        return DoubleSets.cartesianProduct(this, set);
    }

    @Override
    public boolean noneSatisfy(DoublePredicate predicate)
    {
        return true;
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        return true;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public RichIterable<DoubleIterable> chunk(int size)
    {
        return Lists.immutable.empty();
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
        return set.isEmpty();
    }

    @Override
    public int hashCode()
    {
        return 0;
    }

    @Override
    public MutableDoubleSet toSet()
    {
        return DoubleSets.mutable.empty();
    }

    @Override
    public MutableDoubleBag toBag()
    {
        return DoubleBags.mutable.empty();
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
        return new double[0];
    }

    @Override
    public double[] toArray(double[] target)
    {
        return target;
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

    @Override
    public DoubleIterator doubleIterator()
    {
        return ImmutableEmptyDoubleIterator.INSTANCE;
    }
}
