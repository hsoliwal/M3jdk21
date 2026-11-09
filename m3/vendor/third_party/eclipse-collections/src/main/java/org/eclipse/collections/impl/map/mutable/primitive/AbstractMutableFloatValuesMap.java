/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.mutable.primitive;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableFloatCollection;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.factory.Bags;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.map.primitive.MutableFloatValuesMap;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.impl.bag.mutable.primitive.FloatHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.SynchronizedFloatCollection;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableFloatCollection;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.FloatBags;
import org.eclipse.collections.impl.factory.primitive.FloatLists;
import org.eclipse.collections.impl.lazy.primitive.LazyFloatIterableAdapter;
import org.eclipse.collections.impl.primitive.AbstractFloatIterable;
import org.eclipse.collections.impl.set.mutable.primitive.FloatHashSet;

/**
 * This file was automatically generated from template file abstractMutablePrimitiveValuesMap.stg.
 *
 * @since 6.0.
 */
public abstract class AbstractMutableFloatValuesMap extends AbstractFloatIterable implements MutableFloatValuesMap
{
    protected abstract int getOccupiedWithData();

    protected abstract SentinelValues getSentinelValues();

    protected abstract void setSentinelValuesNull();

    protected abstract float getEmptyValue();

    protected abstract float getValueAtIndex(int index);

    protected abstract int getTableSize();

    protected abstract boolean isNonSentinelAtIndex(int index);

    protected void addEmptyKeyValue(float value)
    {
        this.getSentinelValues().containsZeroKey = true;
        this.getSentinelValues().zeroValue = value;
    }

    protected void removeEmptyKey()
    {
        if (this.getSentinelValues().containsOneKey)
        {
            this.getSentinelValues().containsZeroKey = false;
            this.getSentinelValues().zeroValue = this.getEmptyValue();
        }
        else
        {
            this.setSentinelValuesNull();
        }
    }

    protected void addRemovedKeyValue(float value)
    {
        this.getSentinelValues().containsOneKey = true;
        this.getSentinelValues().oneValue = value;
    }

    protected void removeRemovedKey()
    {
        if (this.getSentinelValues().containsZeroKey)
        {
            this.getSentinelValues().containsOneKey = false;
            this.getSentinelValues().oneValue = this.getEmptyValue();
        }
        else
        {
            this.setSentinelValuesNull();
        }
    }

    @Override
    public boolean contains(float value)
    {
        return this.containsValue(value);
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        return source.allSatisfy((float value) -> AbstractMutableFloatValuesMap.this.contains(value));
    }

    @Override
    public float max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        FloatIterator iterator = this.floatIterator();
        float max = iterator.next();
        while (iterator.hasNext())
        {
            float value = iterator.next();
            if (Float.compare(max, value) < 0)
            {
                max = value;
            }
        }
        return max;
    }

    @Override
    public float min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        FloatIterator iterator = this.floatIterator();
        float min = iterator.next();
        while (iterator.hasNext())
        {
            float value = iterator.next();
            if (Float.compare(value, min) < 0)
            {
                min = value;
            }
        }
        return min;
    }

    @Override
    public int size()
    {
        return this.getOccupiedWithData() + (this.getSentinelValues() == null ? 0 : this.getSentinelValues().size());
    }

    @Override
    public boolean isEmpty()
    {
        return this.getOccupiedWithData() == 0 && (this.getSentinelValues() == null || this.getSentinelValues().size() == 0);
    }

    @Override
    public boolean notEmpty()
    {
        return this.getOccupiedWithData() != 0 || (this.getSentinelValues() != null && this.getSentinelValues().size() != 0);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
    {
        this.forEachValue(procedure);
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        try
        {
            appendable.append(start);

            boolean first = true;

            if (this.getSentinelValues() != null)
            {
                if (this.getSentinelValues().containsZeroKey)
                {
                    appendable.append(String.valueOf(this.getSentinelValues().zeroValue));
                    first = false;
                }
                if (this.getSentinelValues().containsOneKey)
                {
                    if (!first)
                    {
                        appendable.append(separator);
                    }
                    appendable.append(String.valueOf(this.getSentinelValues().oneValue));
                    first = false;
                }
            }
            for (int i = 0; i < this.getTableSize(); i++)
            {
                if (this.isNonSentinelAtIndex(i))
                {
                    if (!first)
                    {
                        appendable.append(separator);
                    }
                    appendable.append(String.valueOf(this.getValueAtIndex(i)));
                    first = false;
                }
            }
            appendable.append(end);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public float[] toArray()
    {
        float[] array = new float[this.size()];
        int index = 0;

        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey)
            {
                array[index] = this.getSentinelValues().zeroValue;
                index++;
            }
            if (this.getSentinelValues().containsOneKey)
            {
                array[index] = this.getSentinelValues().oneValue;
                index++;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (this.isNonSentinelAtIndex(i))
            {
                array[index] = this.getValueAtIndex(i);
                index++;
            }
        }

        return array;
    }

    @Override
    public float[] toArray(float[] target)
    {
        if (target.length < this.size())
        {
            target = new float[this.size()];
        }
        int index = 0;

        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey)
            {
                target[index] = this.getSentinelValues().zeroValue;
                index++;
            }
            if (this.getSentinelValues().containsOneKey)
            {
                target[index] = this.getSentinelValues().oneValue;
                index++;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (this.isNonSentinelAtIndex(i))
            {
                target[index] = this.getValueAtIndex(i);
                index++;
            }
        }

        return target;
    }

    @Override
    public MutableFloatBag select(FloatPredicate predicate)
    {
        return this.select(predicate, new FloatHashBag());
    }

    @Override
    public MutableFloatBag reject(FloatPredicate predicate)
    {
        return this.reject(predicate, new FloatHashBag());
    }

    @Override
    public <V> MutableBag<V> collect(FloatToObjectFunction<? extends V> function)
    {
        return this.collect(function, Bags.mutable.withInitialCapacity(this.size()));
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float value)
    {
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey && predicate.accept(this.getSentinelValues().zeroValue))
            {
                return this.getSentinelValues().zeroValue;
            }
            if (this.getSentinelValues().containsOneKey && predicate.accept(this.getSentinelValues().oneValue))
            {
                return this.getSentinelValues().oneValue;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (this.isNonSentinelAtIndex(i) && predicate.accept(this.getValueAtIndex(i)))
            {
                return this.getValueAtIndex(i);
            }
        }
        return value;
    }

    @Override
    public int count(FloatPredicate predicate)
    {
        int count = 0;
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey && predicate.accept(this.getSentinelValues().zeroValue))
            {
                count++;
            }
            if (this.getSentinelValues().containsOneKey && predicate.accept(this.getSentinelValues().oneValue))
            {
                count++;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (this.isNonSentinelAtIndex(i) && predicate.accept(this.getValueAtIndex(i)))
            {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean anySatisfy(FloatPredicate predicate)
    {
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey && predicate.accept(this.getSentinelValues().zeroValue))
            {
                return true;
            }
            if (this.getSentinelValues().containsOneKey && predicate.accept(this.getSentinelValues().oneValue))
            {
                return true;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (this.isNonSentinelAtIndex(i) && predicate.accept(this.getValueAtIndex(i)))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allSatisfy(FloatPredicate predicate)
    {
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey && !predicate.accept(this.getSentinelValues().zeroValue))
            {
                return false;
            }
            if (this.getSentinelValues().containsOneKey && !predicate.accept(this.getSentinelValues().oneValue))
            {
                return false;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (this.isNonSentinelAtIndex(i) && !predicate.accept(this.getValueAtIndex(i)))
            {
                return false;
            }
        }
        return true;
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
                result.add(FloatBags.mutable.withAll(this));
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
                    result.add(batch);
                }
            }
        }
        return result;
    }

    @Override
    public double sum()
    {
        double result = 0.0;
        double compensation = 0.0;

        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey)
            {
                double adjustedValue = this.getSentinelValues().zeroValue - compensation;
                double nextSum = result + adjustedValue;
                compensation = nextSum - result - adjustedValue;
                result = nextSum;
            }
            if (this.getSentinelValues().containsOneKey)
            {
                double adjustedValue = this.getSentinelValues().oneValue - compensation;
                double nextSum = result + adjustedValue;
                compensation = nextSum - result - adjustedValue;
                result = nextSum;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (this.isNonSentinelAtIndex(i))
            {
                double adjustedValue = this.getValueAtIndex(i) - compensation;
                double nextSum = result + adjustedValue;
                compensation = nextSum - result - adjustedValue;
                result = nextSum;
            }
        }

        return result;
    }

    @Override
    public boolean containsValue(float value)
    {
        if (this.getSentinelValues() != null && this.getSentinelValues().containsValue(value))
        {
            return true;
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (this.isNonSentinelAtIndex(i) && Float.compare(this.getValueAtIndex(i), value) == 0)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void forEachValue(FloatProcedure procedure)
    {
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey)
            {
                procedure.value(this.getSentinelValues().zeroValue);
            }
            if (this.getSentinelValues().containsOneKey)
            {
                procedure.value(this.getSentinelValues().oneValue);
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (this.isNonSentinelAtIndex(i))
            {
                procedure.value(this.getValueAtIndex(i));
            }
        }
    }

    protected static class SentinelValues extends AbstractSentinelValues
    {
        protected float zeroValue;
        protected float oneValue;

        public boolean containsValue(float value)
        {
            boolean valueEqualsZeroValue = this.containsZeroKey && Float.compare(this.zeroValue, value) == 0;
            boolean valueEqualsOneValue = this.containsOneKey && Float.compare(this.oneValue, value) == 0;
            return valueEqualsZeroValue || valueEqualsOneValue;
        }

        public SentinelValues copy()
        {
            SentinelValues sentinelValues = new SentinelValues();
            sentinelValues.zeroValue = this.zeroValue;
            sentinelValues.oneValue = this.oneValue;
            sentinelValues.containsOneKey = this.containsOneKey;
            sentinelValues.containsZeroKey = this.containsZeroKey;
            return sentinelValues;
        }
    }

    protected abstract class AbstractFloatValuesCollection implements MutableFloatCollection
    {
        @Override
        public void clear()
        {
            AbstractMutableFloatValuesMap.this.clear();
        }

        @Override
        public MutableFloatCollection select(FloatPredicate predicate)
        {
            return AbstractMutableFloatValuesMap.this.select(predicate);
        }

        @Override
        public MutableFloatCollection reject(FloatPredicate predicate)
        {
            return AbstractMutableFloatValuesMap.this.reject(predicate);
        }

        @Override
        public float detectIfNone(FloatPredicate predicate, float ifNone)
        {
            return AbstractMutableFloatValuesMap.this.detectIfNone(predicate, ifNone);
        }

        @Override
        public <V> MutableCollection<V> collect(FloatToObjectFunction<? extends V> function)
        {
            return AbstractMutableFloatValuesMap.this.collect(function);
        }

        @Override
        public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
        {
            return AbstractMutableFloatValuesMap.this.injectInto(injectedValue, function);
        }

        @Override
        public RichIterable<FloatIterable> chunk(int size)
        {
            return AbstractMutableFloatValuesMap.this.chunk(size);
        }

        @Override
        public double sum()
        {
            return AbstractMutableFloatValuesMap.this.sum();
        }

        @Override
        public float max()
        {
            return AbstractMutableFloatValuesMap.this.max();
        }

        @Override
        public float maxIfEmpty(float defaultValue)
        {
            return AbstractMutableFloatValuesMap.this.maxIfEmpty(defaultValue);
        }

        @Override
        public float min()
        {
            return AbstractMutableFloatValuesMap.this.min();
        }

        @Override
        public float minIfEmpty(float defaultValue)
        {
            return AbstractMutableFloatValuesMap.this.minIfEmpty(defaultValue);
        }

        @Override
        public double average()
        {
            return AbstractMutableFloatValuesMap.this.average();
        }

        @Override
        public double median()
        {
            return AbstractMutableFloatValuesMap.this.median();
        }

        @Override
        public float[] toSortedArray()
        {
            return AbstractMutableFloatValuesMap.this.toSortedArray();
        }

        @Override
        public MutableFloatList toSortedList()
        {
            return AbstractMutableFloatValuesMap.this.toSortedList();
        }

        @Override
        public MutableFloatCollection with(float element)
        {
            throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableFloatCollection without(float element)
        {
            throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableFloatCollection withAll(FloatIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableFloatCollection withoutAll(FloatIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableFloatCollection asUnmodifiable()
        {
            return UnmodifiableFloatCollection.of(this);
        }

        @Override
        public MutableFloatCollection asSynchronized()
        {
            return SynchronizedFloatCollection.of(this);
        }

        @Override
        public ImmutableFloatCollection toImmutable()
        {
            return FloatLists.immutable.withAll(this);
        }

        @Override
        public boolean contains(float value)
        {
            return AbstractMutableFloatValuesMap.this.containsValue(value);
        }

        @Override
        public boolean containsAll(float... source)
        {
            return AbstractMutableFloatValuesMap.this.containsAll(source);
        }

        @Override
        public boolean containsAll(FloatIterable source)
        {
            return AbstractMutableFloatValuesMap.this.containsAll(source);
        }

        @Override
        public MutableFloatList toList()
        {
            return AbstractMutableFloatValuesMap.this.toList();
        }

        @Override
        public MutableFloatSet toSet()
        {
            return AbstractMutableFloatValuesMap.this.toSet();
        }

        @Override
        public MutableFloatBag toBag()
        {
            return AbstractMutableFloatValuesMap.this.toBag();
        }

        @Override
        public LazyFloatIterable asLazy()
        {
            return new LazyFloatIterableAdapter(this);
        }

        @Override
        public boolean isEmpty()
        {
            return AbstractMutableFloatValuesMap.this.isEmpty();
        }

        @Override
        public boolean notEmpty()
        {
            return AbstractMutableFloatValuesMap.this.notEmpty();
        }

        @Override
        public String makeString()
        {
            return AbstractMutableFloatValuesMap.this.makeString();
        }

        @Override
        public String makeString(String separator)
        {
            return AbstractMutableFloatValuesMap.this.makeString(separator);
        }

        @Override
        public String makeString(String start, String separator, String end)
        {
            return AbstractMutableFloatValuesMap.this.makeString(start, separator, end);
        }

        @Override
        public void appendString(Appendable appendable)
        {
            AbstractMutableFloatValuesMap.this.appendString(appendable);
        }

        @Override
        public void appendString(Appendable appendable, String separator)
        {
            AbstractMutableFloatValuesMap.this.appendString(appendable, separator);
        }

        @Override
        public void appendString(Appendable appendable, String start, String separator, String end)
        {
            AbstractMutableFloatValuesMap.this.appendString(appendable, start, separator, end);
        }

        @Override
        public void each(FloatProcedure procedure)
        {
            AbstractMutableFloatValuesMap.this.each(procedure);
        }

        @Override
        public int count(FloatPredicate predicate)
        {
            return AbstractMutableFloatValuesMap.this.count(predicate);
        }

        @Override
        public boolean anySatisfy(FloatPredicate predicate)
        {
            return AbstractMutableFloatValuesMap.this.anySatisfy(predicate);
        }

        @Override
        public boolean allSatisfy(FloatPredicate predicate)
        {
            return AbstractMutableFloatValuesMap.this.allSatisfy(predicate);
        }

        @Override
        public boolean noneSatisfy(FloatPredicate predicate)
        {
            return AbstractMutableFloatValuesMap.this.noneSatisfy(predicate);
        }

        @Override
        public boolean add(float element)
        {
            throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(float... source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(FloatIterable source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean removeAll(FloatIterable source)
        {
            int oldSize = AbstractMutableFloatValuesMap.this.size();

            FloatIterator iterator = source.floatIterator();
            while (iterator.hasNext())
            {
                this.remove(iterator.next());
            }
            return oldSize != AbstractMutableFloatValuesMap.this.size();
        }

        @Override
        public boolean removeAll(float... source)
        {
            int oldSize = AbstractMutableFloatValuesMap.this.size();

            for (float item : source)
            {
                this.remove(item);
            }
            return oldSize != AbstractMutableFloatValuesMap.this.size();
        }

        @Override
        public boolean retainAll(float... source)
        {
            return this.retainAll(FloatHashSet.newSetWith(source));
        }

        @Override
        public int size()
        {
            return AbstractMutableFloatValuesMap.this.size();
        }

        @Override
        public float[] toArray()
        {
            return AbstractMutableFloatValuesMap.this.toArray();
        }

        @Override
        public float[] toArray(float[] target)
        {
            return AbstractMutableFloatValuesMap.this.toArray(target);
        }
    }
}
