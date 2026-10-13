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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableIntCollection;
import org.eclipse.collections.api.collection.primitive.MutableIntCollection;
import org.eclipse.collections.api.factory.Bags;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.map.primitive.MutableIntValuesMap;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.bag.mutable.primitive.IntHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.SynchronizedIntCollection;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableIntCollection;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.IntBags;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.eclipse.collections.impl.lazy.primitive.LazyIntIterableAdapter;
import org.eclipse.collections.impl.primitive.AbstractIntIterable;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

/**
 * This file was automatically generated from template file abstractMutablePrimitiveValuesMap.stg.
 *
 * @since 6.0.
 */
public abstract class AbstractMutableIntValuesMap extends AbstractIntIterable implements MutableIntValuesMap
{
    protected abstract int getOccupiedWithData();

    protected abstract SentinelValues getSentinelValues();

    protected abstract void setSentinelValuesNull();

    protected abstract int getEmptyValue();

    protected abstract int getValueAtIndex(int index);

    protected abstract int getTableSize();

    protected abstract boolean isNonSentinelAtIndex(int index);

    protected void addEmptyKeyValue(int value)
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

    protected void addRemovedKeyValue(int value)
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
    public boolean contains(int value)
    {
        return this.containsValue(value);
    }

    @Override
    public boolean containsAll(IntIterable source)
    {
        return source.allSatisfy((int value) -> AbstractMutableIntValuesMap.this.contains(value));
    }

    @Override
    public int max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        IntIterator iterator = this.intIterator();
        int max = iterator.next();
        while (iterator.hasNext())
        {
            int value = iterator.next();
            if (max < value)
            {
                max = value;
            }
        }
        return max;
    }

    @Override
    public int min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        IntIterator iterator = this.intIterator();
        int min = iterator.next();
        while (iterator.hasNext())
        {
            int value = iterator.next();
            if (value < min)
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
    public void each(IntProcedure procedure)
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
    public int[] toArray()
    {
        int[] array = new int[this.size()];
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
    public int[] toArray(int[] target)
    {
        if (target.length < this.size())
        {
            target = new int[this.size()];
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
    public MutableIntBag select(IntPredicate predicate)
    {
        return this.select(predicate, new IntHashBag());
    }

    @Override
    public MutableIntBag reject(IntPredicate predicate)
    {
        return this.reject(predicate, new IntHashBag());
    }

    @Override
    public <V> MutableBag<V> collect(IntToObjectFunction<? extends V> function)
    {
        return this.collect(function, Bags.mutable.withInitialCapacity(this.size()));
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int value)
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
    public int count(IntPredicate predicate)
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
    public boolean anySatisfy(IntPredicate predicate)
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
    public boolean allSatisfy(IntPredicate predicate)
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
    public RichIterable<IntIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<IntIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(IntBags.mutable.withAll(this));
            }
            else
            {
                IntIterator iterator = this.intIterator();
                while (iterator.hasNext())
                {
                    MutableIntBag batch = IntBags.mutable.empty();
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
    public long sum()
    {
        long result = 0L;

        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey)
            {
                result += this.getSentinelValues().zeroValue;
            }
            if (this.getSentinelValues().containsOneKey)
            {
                result += this.getSentinelValues().oneValue;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (this.isNonSentinelAtIndex(i))
            {
                result += this.getValueAtIndex(i);
            }
        }

        return result;
    }

    @Override
    public boolean containsValue(int value)
    {
        if (this.getSentinelValues() != null && this.getSentinelValues().containsValue(value))
        {
            return true;
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (this.isNonSentinelAtIndex(i) && this.getValueAtIndex(i) == value)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void forEachValue(IntProcedure procedure)
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
        protected int zeroValue;
        protected int oneValue;

        public boolean containsValue(int value)
        {
            boolean valueEqualsZeroValue = this.containsZeroKey && this.zeroValue == value;
            boolean valueEqualsOneValue = this.containsOneKey && this.oneValue == value;
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

    protected abstract class AbstractIntValuesCollection implements MutableIntCollection
    {
        @Override
        public void clear()
        {
            AbstractMutableIntValuesMap.this.clear();
        }

        @Override
        public MutableIntCollection select(IntPredicate predicate)
        {
            return AbstractMutableIntValuesMap.this.select(predicate);
        }

        @Override
        public MutableIntCollection reject(IntPredicate predicate)
        {
            return AbstractMutableIntValuesMap.this.reject(predicate);
        }

        @Override
        public int detectIfNone(IntPredicate predicate, int ifNone)
        {
            return AbstractMutableIntValuesMap.this.detectIfNone(predicate, ifNone);
        }

        @Override
        public <V> MutableCollection<V> collect(IntToObjectFunction<? extends V> function)
        {
            return AbstractMutableIntValuesMap.this.collect(function);
        }

        @Override
        public <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
        {
            return AbstractMutableIntValuesMap.this.injectInto(injectedValue, function);
        }

        @Override
        public RichIterable<IntIterable> chunk(int size)
        {
            return AbstractMutableIntValuesMap.this.chunk(size);
        }

        @Override
        public long sum()
        {
            return AbstractMutableIntValuesMap.this.sum();
        }

        @Override
        public int max()
        {
            return AbstractMutableIntValuesMap.this.max();
        }

        @Override
        public int maxIfEmpty(int defaultValue)
        {
            return AbstractMutableIntValuesMap.this.maxIfEmpty(defaultValue);
        }

        @Override
        public int min()
        {
            return AbstractMutableIntValuesMap.this.min();
        }

        @Override
        public int minIfEmpty(int defaultValue)
        {
            return AbstractMutableIntValuesMap.this.minIfEmpty(defaultValue);
        }

        @Override
        public double average()
        {
            return AbstractMutableIntValuesMap.this.average();
        }

        @Override
        public double median()
        {
            return AbstractMutableIntValuesMap.this.median();
        }

        @Override
        public int[] toSortedArray()
        {
            return AbstractMutableIntValuesMap.this.toSortedArray();
        }

        @Override
        public MutableIntList toSortedList()
        {
            return AbstractMutableIntValuesMap.this.toSortedList();
        }

        @Override
        public MutableIntCollection with(int element)
        {
            throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableIntCollection without(int element)
        {
            throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableIntCollection withAll(IntIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableIntCollection withoutAll(IntIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableIntCollection asUnmodifiable()
        {
            return UnmodifiableIntCollection.of(this);
        }

        @Override
        public MutableIntCollection asSynchronized()
        {
            return SynchronizedIntCollection.of(this);
        }

        @Override
        public ImmutableIntCollection toImmutable()
        {
            return IntLists.immutable.withAll(this);
        }

        @Override
        public boolean contains(int value)
        {
            return AbstractMutableIntValuesMap.this.containsValue(value);
        }

        @Override
        public boolean containsAll(int... source)
        {
            return AbstractMutableIntValuesMap.this.containsAll(source);
        }

        @Override
        public boolean containsAll(IntIterable source)
        {
            return AbstractMutableIntValuesMap.this.containsAll(source);
        }

        @Override
        public MutableIntList toList()
        {
            return AbstractMutableIntValuesMap.this.toList();
        }

        @Override
        public MutableIntSet toSet()
        {
            return AbstractMutableIntValuesMap.this.toSet();
        }

        @Override
        public MutableIntBag toBag()
        {
            return AbstractMutableIntValuesMap.this.toBag();
        }

        @Override
        public LazyIntIterable asLazy()
        {
            return new LazyIntIterableAdapter(this);
        }

        @Override
        public boolean isEmpty()
        {
            return AbstractMutableIntValuesMap.this.isEmpty();
        }

        @Override
        public boolean notEmpty()
        {
            return AbstractMutableIntValuesMap.this.notEmpty();
        }

        @Override
        public String makeString()
        {
            return AbstractMutableIntValuesMap.this.makeString();
        }

        @Override
        public String makeString(String separator)
        {
            return AbstractMutableIntValuesMap.this.makeString(separator);
        }

        @Override
        public String makeString(String start, String separator, String end)
        {
            return AbstractMutableIntValuesMap.this.makeString(start, separator, end);
        }

        @Override
        public void appendString(Appendable appendable)
        {
            AbstractMutableIntValuesMap.this.appendString(appendable);
        }

        @Override
        public void appendString(Appendable appendable, String separator)
        {
            AbstractMutableIntValuesMap.this.appendString(appendable, separator);
        }

        @Override
        public void appendString(Appendable appendable, String start, String separator, String end)
        {
            AbstractMutableIntValuesMap.this.appendString(appendable, start, separator, end);
        }

        @Override
        public void each(IntProcedure procedure)
        {
            AbstractMutableIntValuesMap.this.each(procedure);
        }

        @Override
        public int count(IntPredicate predicate)
        {
            return AbstractMutableIntValuesMap.this.count(predicate);
        }

        @Override
        public boolean anySatisfy(IntPredicate predicate)
        {
            return AbstractMutableIntValuesMap.this.anySatisfy(predicate);
        }

        @Override
        public boolean allSatisfy(IntPredicate predicate)
        {
            return AbstractMutableIntValuesMap.this.allSatisfy(predicate);
        }

        @Override
        public boolean noneSatisfy(IntPredicate predicate)
        {
            return AbstractMutableIntValuesMap.this.noneSatisfy(predicate);
        }

        @Override
        public boolean add(int element)
        {
            throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(int... source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(IntIterable source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean removeAll(IntIterable source)
        {
            int oldSize = AbstractMutableIntValuesMap.this.size();

            IntIterator iterator = source.intIterator();
            while (iterator.hasNext())
            {
                this.remove(iterator.next());
            }
            return oldSize != AbstractMutableIntValuesMap.this.size();
        }

        @Override
        public boolean removeAll(int... source)
        {
            int oldSize = AbstractMutableIntValuesMap.this.size();

            for (int item : source)
            {
                this.remove(item);
            }
            return oldSize != AbstractMutableIntValuesMap.this.size();
        }

        @Override
        public boolean retainAll(int... source)
        {
            return this.retainAll(IntHashSet.newSetWith(source));
        }

        @Override
        public int size()
        {
            return AbstractMutableIntValuesMap.this.size();
        }

        @Override
        public int[] toArray()
        {
            return AbstractMutableIntValuesMap.this.toArray();
        }

        @Override
        public int[] toArray(int[] target)
        {
            return AbstractMutableIntValuesMap.this.toArray(target);
        }
    }
}
