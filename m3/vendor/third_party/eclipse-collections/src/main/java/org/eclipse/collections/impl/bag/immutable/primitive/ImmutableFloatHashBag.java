/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.bag.immutable.primitive;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.FloatBag;
import org.eclipse.collections.api.bag.primitive.ImmutableFloatBag;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.impl.bag.mutable.primitive.FloatHashBag;
import org.eclipse.collections.impl.block.procedure.checked.primitive.CheckedFloatIntProcedure;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.FloatBags;
import org.eclipse.collections.impl.iterator.UnmodifiableFloatIterator;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.primitive.FloatIntPair;
import org.eclipse.collections.api.set.primitive.ImmutableFloatSet;
import org.eclipse.collections.impl.factory.primitive.FloatSets;

/**
 * ImmutableFloatHashBag is the non-modifiable equivalent of {@link FloatHashBag}.
 * This file was automatically generated from template file immutablePrimitiveHashBag.stg.
 *
 * @since 4.0.
 */
final class ImmutableFloatHashBag implements ImmutableFloatBag, Serializable
{
    private static final long serialVersionUID = 1L;

    private final MutableFloatBag delegate;

    private ImmutableFloatHashBag(float[] newElements)
    {
        this.checkOptimizedSize(newElements.length);
        this.delegate = FloatHashBag.newBagWith(newElements);
    }

    private void checkOptimizedSize(int length)
    {
        if (length <= 1)
        {
            throw new IllegalArgumentException("Use FloatBags.immutable.with() to instantiate an optimized collection");
        }
    }

    public static ImmutableFloatHashBag newBagWith(float... elements)
    {
        return new ImmutableFloatHashBag(elements);
    }

    @Override
    public ImmutableFloatBag newWith(float element)
    {
        return FloatHashBag.newBag(this.delegate).with(element).toImmutable();
    }

    @Override
    public ImmutableFloatBag newWithout(float element)
    {
        FloatHashBag hashBag = FloatHashBag.newBag(this.delegate);
        hashBag.remove(element);
        return hashBag.toImmutable();
    }

    @Override
    public ImmutableFloatBag newWithAll(FloatIterable elements)
    {
        FloatHashBag bag = FloatHashBag.newBag(this.delegate);
        bag.addAll(elements);
        return bag.toImmutable();
    }

    @Override
    public ImmutableFloatBag newWithoutAll(FloatIterable elements)
    {
        FloatHashBag bag = FloatHashBag.newBag(this.delegate);
        bag.removeAll(elements);
        return bag.toImmutable();
    }

    @Override
    public int size()
    {
        return this.delegate.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.delegate.isEmpty();
    }

    @Override
    public boolean notEmpty()
    {
        return this.delegate.notEmpty();
    }

    @Override
    public boolean contains(float value)
    {
        return this.delegate.contains(value);
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public boolean containsAll(float... elements)
    {
        return this.delegate.containsAll(elements);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
    {
        this.delegate.forEach(procedure);
    }

    @Override
    public ImmutableFloatBag select(FloatPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableFloatBag selectByOccurrences(IntPredicate predicate)
    {
        return this.delegate.selectByOccurrences(predicate).toImmutable();
    }

    @Override
    public ImmutableFloatSet selectUnique()
    {
        MutableFloatSet result = FloatSets.mutable.empty();
        this.forEachWithOccurrences((each, occurrences) ->
        {
            if (occurrences == 1)
            {
                result.add(each);
            }
        });
        return result.toImmutable();
    }

    @Override
    public ImmutableList<FloatIntPair> topOccurrences(int count)
    {
        return this.delegate.topOccurrences(count).toImmutable();
    }

    @Override
    public ImmutableList<FloatIntPair> bottomOccurrences(int count)
    {
        return this.delegate.bottomOccurrences(count).toImmutable();
    }

    @Override
    public ImmutableFloatBag reject(FloatPredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public <V> ImmutableBag<V> collect(FloatToObjectFunction<? extends V> function)
    {
        MutableBag<V> bag = this.delegate.collect(function);
        return bag.toImmutable();
    }

    @Override
    public MutableFloatList toList()
    {
        return this.delegate.toList();
    }

    @Override
    public int sizeDistinct()
    {
        return this.delegate.sizeDistinct();
    }

    @Override
    public int occurrencesOf(float item)
    {
        return this.delegate.occurrencesOf(item);
    }

    @Override
    public void forEachWithOccurrences(FloatIntProcedure floatIntProcedure)
    {
        this.delegate.forEachWithOccurrences(floatIntProcedure);
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        return this.delegate.detectIfNone(predicate, ifNone);
    }

    @Override
    public int count(FloatPredicate predicate)
    {
        return this.delegate.count(predicate);
    }

    @Override
    public boolean anySatisfy(FloatPredicate predicate)
    {
        return this.delegate.anySatisfy(predicate);
    }

    @Override
    public double sum()
    {
        return this.delegate.sum();
    }

    @Override
    public float min()
    {
        return this.delegate.min();
    }

    @Override
    public float max()
    {
        return this.delegate.max();
    }

    @Override
    public float maxIfEmpty(float defaultValue)
    {
        return this.delegate.maxIfEmpty(defaultValue);
    }

    @Override
    public float minIfEmpty(float defaultValue)
    {
        return this.delegate.minIfEmpty(defaultValue);
    }

    @Override
    public double average()
    {
        return this.delegate.average();
    }

    @Override
    public double median()
    {
        return this.delegate.median();
    }

    @Override
    public float[] toSortedArray()
    {
        return this.delegate.toSortedArray();
    }

    @Override
    public MutableFloatList toSortedList()
    {
        return this.delegate.toSortedList();
    }

    @Override
    public boolean noneSatisfy(FloatPredicate predicate)
    {
        return this.delegate.noneSatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(FloatPredicate predicate)
    {
        return this.delegate.allSatisfy(predicate);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        return this.delegate.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<FloatIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }

        MutableList<FloatIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(this);
            }
            else
            {
                FloatIterator iterator = this.floatIterator();
                while (iterator.hasNext())
                {
                    MutableFloatBag batch = FloatBags.mutable.empty();
                    for (int i = 0; i < size && iterator.hasNext(); i++)
                    {
                        batch.add(iterator.next());
                    }
                    result.add(batch.toImmutable());
                }
            }
        }
        return result.toImmutable();
    }

    @Override
    public boolean equals(Object obj)
    {
        return this.delegate.equals(obj);
    }

    @Override
    public int hashCode()
    {
        return this.delegate.hashCode();
    }

    @Override
    public MutableFloatSet toSet()
    {
        return this.delegate.toSet();
    }

    @Override
    public MutableFloatBag toBag()
    {
        return this.delegate.toBag();
    }

    @Override
    public ImmutableFloatBag toImmutable()
    {
        return this;
    }

    @Override
    public LazyFloatIterable asLazy()
    {
        return this.delegate.asLazy();
    }

    @Override
    public float[] toArray()
    {
        return this.delegate.toArray();
    }

    @Override
    public float[] toArray(float[] target)
    {
        return this.delegate.toArray(target);
    }

    @Override
    public String toString()
    {
        return this.delegate.toString();
    }

    @Override
    public String makeString()
    {
        return this.delegate.makeString();
    }

    @Override
    public String makeString(String separator)
    {
        return this.delegate.makeString(separator);
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        return this.delegate.makeString(start, separator, end);
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.delegate.appendString(appendable);
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.delegate.appendString(appendable, separator);
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        this.delegate.appendString(appendable, start, separator, end);
    }

    @Override
    public FloatIterator floatIterator()
    {
        return new UnmodifiableFloatIterator(this.delegate.floatIterator());
    }

    private Object writeReplace()
    {
        return new ImmutableFloatBagSerializationProxy(this);
    }

    protected static class ImmutableFloatBagSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private FloatBag bag;

        public ImmutableFloatBagSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableFloatBagSerializationProxy(FloatBag bag)
        {
            this.bag = bag;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException
        {
            out.writeInt(this.bag.sizeDistinct());
            try
            {
                this.bag.forEachWithOccurrences(new CheckedFloatIntProcedure()
                {
                    @Override
                    public void safeValue(float item, int count) throws IOException
                    {
                        out.writeFloat(item);
                        out.writeInt(count);
                    }
                });
            }
            catch (RuntimeException e)
            {
                if (e.getCause() instanceof IOException)
                {
                    throw (IOException) e.getCause();
                }
                throw e;
            }
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
        {
            int size = in.readInt();
            MutableFloatBag deserializedBag = new FloatHashBag();

            for (int i = 0; i < size; i++)
            {
                deserializedBag.addOccurrences(in.readFloat(), in.readInt());
            }

            this.bag = deserializedBag;
        }

        protected Object readResolve()
        {
            return this.bag.toImmutable();
        }
    }
}
