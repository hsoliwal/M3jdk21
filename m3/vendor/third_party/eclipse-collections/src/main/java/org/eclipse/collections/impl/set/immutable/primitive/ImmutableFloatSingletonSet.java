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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.factory.primitive.FloatBags;
import org.eclipse.collections.api.factory.primitive.FloatLists;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableFloatSet;
import org.eclipse.collections.api.set.primitive.FloatSet;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.impl.iterator.SingletonFloatIterator;
import org.eclipse.collections.impl.set.mutable.primitive.FloatHashSet;
import org.eclipse.collections.impl.factory.primitive.FloatSets;
import org.eclipse.collections.impl.lazy.primitive.LazyFloatIterableAdapter;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.tuple.primitive.FloatFloatPair;

/**
 * ImmutableFloatSingletonSet is an optimization for {@link ImmutableFloatSet} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonSet.stg.
 *
 * @since 4.0.
 */
final class ImmutableFloatSingletonSet implements ImmutableFloatSet, Serializable
{
    private static final long serialVersionUID = 1L;

    private final float element;

    ImmutableFloatSingletonSet(float element)
    {
        this.element = element;
    }

    @Override
    public ImmutableFloatSet newWith(float element)
    {
        return FloatSets.immutable.with(this.element, element);
    }

    @Override
    public ImmutableFloatSet newWithout(float element)
    {
        return Float.compare(this.element, element) == 0 ? FloatSets.immutable.with() : this;
    }

    @Override
    public ImmutableFloatSet newWithAll(FloatIterable elements)
    {
        return FloatHashSet.newSet(elements).with(this.element).toImmutable();
    }

    @Override
    public ImmutableFloatSet newWithoutAll(FloatIterable elements)
    {
        return elements.contains(this.element) ? FloatSets.immutable.with() : this;
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
    public boolean contains(float value)
    {
        return Float.compare(this.element, value) == 0;
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        for (FloatIterator iterator = source.floatIterator(); iterator.hasNext(); )
        {
            if (Float.compare(this.element, iterator.next()) != 0)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(float... source)
    {
        for (float value : source)
        {
            if (Float.compare(this.element, value) != 0)
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
    public void each(FloatProcedure procedure)
    {
        procedure.value(this.element);
    }

    @Override
    public ImmutableFloatSet select(FloatPredicate predicate)
    {
        return predicate.accept(this.element) ? FloatHashSet.newSetWith(this.element).toImmutable()
                : new FloatHashSet().toImmutable();
    }

    @Override
    public ImmutableFloatSet reject(FloatPredicate predicate)
    {
        return predicate.accept(this.element) ? new FloatHashSet().toImmutable()
                : FloatHashSet.newSetWith(this.element).toImmutable();
    }

    @Override
    public <V> ImmutableSet<V> collect(FloatToObjectFunction<? extends V> function)
    {
        return Sets.immutable.with(function.valueOf(this.element));
    }

    @Override
    public MutableFloatList toList()
    {
        return FloatLists.mutable.with(this.element);
    }

    public int sizeDistinct()
    {
        return 1;
    }

    public int occurrencesOf(float item)
    {
        return Float.compare(this.element, item) == 0 ? 1 : 0;
    }

    public void forEachWithOccurrences(FloatIntProcedure floatIntProcedure)
    {
        floatIntProcedure.value(this.element, 1);
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        return predicate.accept(this.element) ? this.element : ifNone;
    }

    @Override
    public int count(FloatPredicate predicate)
    {
        return predicate.accept(this.element) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(FloatPredicate predicate)
    {
        return predicate.accept(this.element);
    }

    @Override
    public double sum()
    {
        return this.element;
    }

    @Override
    public float min()
    {
        return this.element;
    }

    @Override
    public float max()
    {
        return this.element;
    }

    @Override
    public float maxIfEmpty(float defaultValue)
    {
        return this.element;
    }

    @Override
    public float minIfEmpty(float defaultValue)
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
    public LazyIterable<FloatFloatPair> cartesianProduct(FloatSet set)
    {
        return FloatSets.cartesianProduct(this, set);
    }

    @Override
    public float[] toSortedArray()
    {
        return new float[]{this.element};
    }

    @Override
    public MutableFloatList toSortedList()
    {
        return FloatLists.mutable.with(this.element);
    }

    @Override
    public boolean noneSatisfy(FloatPredicate predicate)
    {
        return !predicate.accept(this.element);
    }

    @Override
    public boolean allSatisfy(FloatPredicate predicate)
    {
        return predicate.accept(this.element);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element);
    }

    @Override
    public RichIterable<FloatIterable> chunk(int size)
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
        if (!(obj instanceof FloatSet))
        {
            return false;
        }
        FloatSet set = (FloatSet) obj;
        if (set.size() != 1)
        {
            return false;
        }
        return set.contains(this.element);
    }

    @Override
    public int hashCode()
    {
        return Float.floatToIntBits(this.element);
    }

    @Override
    public MutableFloatSet toSet()
    {
        return FloatSets.mutable.with(this.element);
    }

    @Override
    public MutableFloatBag toBag()
    {
        return FloatBags.mutable.with(this.element);
    }

    @Override
    public FloatSet freeze()
    {
        return this;
    }

    @Override
    public ImmutableFloatSet toImmutable()
    {
        return this;
    }

    @Override
    public LazyFloatIterable asLazy()
    {
        return new LazyFloatIterableAdapter(this);
    }

    @Override
    public float[] toArray()
    {
        return new float[]{this.element};
    }

    @Override
    public float[] toArray(float[] target)
    {
        if (target.length < 1)
        {
            target = new float[]{this.element};
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
    public FloatIterator floatIterator()
    {
        return new SingletonFloatIterator(element);
    }
}
