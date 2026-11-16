/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.list.mutable.primitive;

import java.util.Collection;

import jdk.m3.collections.eclipse.api.DoubleIterable;
import jdk.m3.collections.eclipse.api.LazyDoubleIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectDoubleIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.DoubleIntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.DoublePredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.DoubleIntProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableDoubleCollection;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.tuple.primitive.DoubleDoublePair;
import jdk.m3.collections.eclipse.api.tuple.primitive.DoubleObjectPair;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableDoubleStack;
import jdk.m3.collections.eclipse.api.list.primitive.DoubleList;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableDoubleList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableDoubleList;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.AbstractUnmodifiableDoubleCollection;
import jdk.m3.collections.eclipse.impl.factory.primitive.DoubleLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.DoubleStacks;
import jdk.m3.collections.eclipse.impl.lazy.primitive.ReverseDoubleIterable;
import java.util.Spliterator;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveList.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableDoubleList
        extends AbstractUnmodifiableDoubleCollection
        implements MutableDoubleList
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableDoubleList(MutableDoubleList list)
    {
        super(list);
    }

    private MutableDoubleList getMutableDoubleList()
    {
        return (MutableDoubleList) this.getDoubleCollection();
    }

    @Override
    public double get(int index)
    {
        return this.getMutableDoubleList().get(index);
    }

    @Override
    public double getFirst()
    {
        return this.getMutableDoubleList().getFirst();
    }

    @Override
    public double getLast()
    {
        return this.getMutableDoubleList().getLast();
    }

    @Override
    public int indexOf(double value)
    {
        return this.getMutableDoubleList().indexOf(value);
    }

    @Override
    public int lastIndexOf(double value)
    {
        return this.getMutableDoubleList().lastIndexOf(value);
    }

    @Override
    public MutableList<Double> boxed()
    {
        return new BoxedMutableDoubleList(this);
    }

    @Override
    public void addAtIndex(int index, double element)
    {
        throw new UnsupportedOperationException("Cannot call addAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAllAtIndex(int index, double... source)
    {
        throw new UnsupportedOperationException("Cannot call addAllAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAllAtIndex(int index, DoubleIterable source)
    {
        throw new UnsupportedOperationException("Cannot call addAllAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public double removeAtIndex(int index)
    {
        throw new UnsupportedOperationException("Cannot call removeAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public double set(int index, double element)
    {
        throw new UnsupportedOperationException("Cannot call set() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableDoubleList with(double element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableDoubleList without(double element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableDoubleList withAll(DoubleIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableDoubleList withoutAll(DoubleIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableDoubleList select(DoublePredicate predicate)
    {
        return this.getMutableDoubleList().select(predicate);
    }

    @Override
    public MutableDoubleList reject(DoublePredicate predicate)
    {
        return this.getMutableDoubleList().reject(predicate);
    }

    @Override
    public <V> MutableList<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        return this.getMutableDoubleList().collect(function);
    }

    @Override
    public MutableDoubleList sortThis()
    {
        throw new UnsupportedOperationException("Cannot call sortThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableDoubleList shuffleThis()
    {
        throw new UnsupportedOperationException("Cannot call shuffleThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public int binarySearch(double value)
    {
        return this.getMutableDoubleList().binarySearch(value);
    }

    @Override
    public double dotProduct(DoubleList list)
    {
        return this.getMutableDoubleList().dotProduct(list);
    }

    @Override
    public boolean equals(Object otherList)
    {
        return this.getMutableDoubleList().equals(otherList);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableDoubleList().hashCode();
    }

    @Override
    public MutableDoubleList asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableDoubleList asSynchronized()
    {
        return new SynchronizedDoubleList(this);
    }

    @Override
    public ImmutableDoubleList toImmutable()
    {
        int size = this.size();
        if (size == 0)
        {
            return DoubleLists.immutable.with();
        }
        if (size == 1)
        {
            return DoubleLists.immutable.with(this.getFirst());
        }
        return DoubleLists.immutable.with(this.toArray());
    }

    /**
     * @since 9.2.
     */
    public MutableDoubleList newEmpty()
    {
        return this.getMutableDoubleList().newEmpty();
    }

    @Override
    public MutableDoubleList reverseThis()
    {
        throw new UnsupportedOperationException("Cannot call reverseThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableDoubleList toReversed()
    {
        return this.getMutableDoubleList().toReversed();
    }

    @Override
    public void forEachWithIndex(DoubleIntProcedure procedure)
    {
        this.getMutableDoubleList().forEachWithIndex(procedure);
    }

    @Override
    public LazyDoubleIterable asReversed()
    {
        return ReverseDoubleIterable.adapt(this);
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectDoubleIntToObjectFunction<? super T, ? extends T> function)
    {
        return this.getMutableDoubleList().injectIntoWithIndex(injectedValue, function);
    }

    /**
     * @since 6.0.
     */
    @Override
    public MutableDoubleList distinct()
    {
        return this.getMutableDoubleList().distinct();
    }

    @Override
    public MutableDoubleList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    public MutableList<DoubleDoublePair> zipDouble(DoubleIterable iterable)
    {
        return this.getMutableDoubleList().zipDouble(iterable);
    }

    /**
     * @since 9.1.
     */
    public <T> MutableList<DoubleObjectPair<T>> zip(Iterable<T> iterable)
    {
        return this.getMutableDoubleList().zip(iterable);
    }

    /**
     * Returns a new MutableDoubleList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableDoubleList selectWithIndex(DoubleIntPredicate predicate)
    {
        return this.getMutableDoubleList().selectWithIndex(predicate);
    }

    /**
     * Returns a new MutableDoubleCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableDoubleCollection> R selectWithIndex(DoubleIntPredicate predicate, R target)
    {
        return this.getMutableDoubleList().selectWithIndex(predicate, target);
    }

    /**
     * Returns a new MutableDoubleList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableDoubleList rejectWithIndex(DoubleIntPredicate predicate)
    {
        return this.getMutableDoubleList().rejectWithIndex(predicate);
    }

    /**
     * Returns a new MutableDoubleCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableDoubleCollection> R rejectWithIndex(DoubleIntPredicate predicate, R target)
    {
        return this.getMutableDoubleList().rejectWithIndex(predicate, target);
    }

    /**
     * Returns a new MutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    public <V> MutableList<V> collectWithIndex(DoubleIntToObjectFunction<? extends V> function)
    {
        return this.getMutableDoubleList().collectWithIndex(function);
    }

    /**
     * Adds elements to the target Collection using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    public <V, R extends Collection<V>> R collectWithIndex(DoubleIntToObjectFunction<? extends V> function, R target)
    {
        return this.getMutableDoubleList().collectWithIndex(function, target);
    }

    @Override
    public MutableDoubleStack toStack()
    {
        return DoubleStacks.mutable.withAll(this);
    }

    @Override
    public Spliterator.OfDouble spliterator()
    {
        return this.getMutableDoubleList().spliterator();
    }
}
