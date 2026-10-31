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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableLongCollection;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.factory.Bags;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.map.primitive.MutableLongValuesMap;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.bag.mutable.primitive.LongHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.SynchronizedLongCollection;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableLongCollection;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.LongBags;
import org.eclipse.collections.impl.factory.primitive.LongLists;
import org.eclipse.collections.impl.lazy.primitive.LazyLongIterableAdapter;
import org.eclipse.collections.impl.primitive.AbstractLongIterable;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;

/**
 * This file was automatically generated from template file abstractMutablePrimitiveValuesMap.stg.
 *
 * @since 6.0.
 */
public abstract class AbstractMutableLongValuesMap extends AbstractLongIterable implements MutableLongValuesMap
{
    protected abstract int getOccupiedWithData();

    protected abstract SentinelValues getSentinelValues();

    protected abstract void setSentinelValuesNull();

    protected abstract long getEmptyValue();

    protected abstract long getValueAtIndex(int index);

    protected abstract int getTableSize();

    protected abstract boolean isNonSentinelAtIndex(int index);

    protected void addEmptyKeyValue(long value)
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

    protected void addRemovedKeyValue(long value)
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
    public boolean contains(long value)
    {
        return this.containsValue(value);
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        return source.allSatisfy((long value) -> AbstractMutableLongValuesMap.this.contains(value));
    }

    @Override
    public long max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        LongIterator iterator = this.longIterator();
        long max = iterator.next();
        while (iterator.hasNext())
        {
            long value = iterator.next();
            if (max < value)
            {
                max = value;
            }
        }
        return max;
    }

    @Override
    public long min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        LongIterator iterator = this.longIterator();
        long min = iterator.next();
        while (iterator.hasNext())
        {
            long value = iterator.next();
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
    public void each(LongProcedure procedure)
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
    public long[] toArray()
    {
        long[] array = new long[this.size()];
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
    public long[] toArray(long[] target)
    {
        if (target.length < this.size())
        {
            target = new long[this.size()];
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
    public MutableLongBag select(LongPredicate predicate)
    {
        return this.select(predicate, new LongHashBag());
    }

    @Override
    public MutableLongBag reject(LongPredicate predicate)
    {
        return this.reject(predicate, new LongHashBag());
    }

    @Override
    public <V> MutableBag<V> collect(LongToObjectFunction<? extends V> function)
    {
        return this.collect(function, Bags.mutable.withInitialCapacity(this.size()));
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long value)
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
    public int count(LongPredicate predicate)
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
    public boolean anySatisfy(LongPredicate predicate)
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
    public boolean allSatisfy(LongPredicate predicate)
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
    public RichIterable<LongIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<LongIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(LongBags.mutable.withAll(this));
            }
            else
            {
                LongIterator iterator = this.longIterator();
                while (iterator.hasNext())
                {
                    MutableLongBag batch = LongBags.mutable.empty();
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
    public boolean containsValue(long value)
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
    public void forEachValue(LongProcedure procedure)
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
        protected long zeroValue;
        protected long oneValue;

        public boolean containsValue(long value)
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

    protected abstract class AbstractLongValuesCollection implements MutableLongCollection
    {
        @Override
        public void clear()
        {
            AbstractMutableLongValuesMap.this.clear();
        }

        @Override
        public MutableLongCollection select(LongPredicate predicate)
        {
            return AbstractMutableLongValuesMap.this.select(predicate);
        }

        @Override
        public MutableLongCollection reject(LongPredicate predicate)
        {
            return AbstractMutableLongValuesMap.this.reject(predicate);
        }

        @Override
        public long detectIfNone(LongPredicate predicate, long ifNone)
        {
            return AbstractMutableLongValuesMap.this.detectIfNone(predicate, ifNone);
        }

        @Override
        public <V> MutableCollection<V> collect(LongToObjectFunction<? extends V> function)
        {
            return AbstractMutableLongValuesMap.this.collect(function);
        }

        @Override
        public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
        {
            return AbstractMutableLongValuesMap.this.injectInto(injectedValue, function);
        }

        @Override
        public RichIterable<LongIterable> chunk(int size)
        {
            return AbstractMutableLongValuesMap.this.chunk(size);
        }

        @Override
        public long sum()
        {
            return AbstractMutableLongValuesMap.this.sum();
        }

        @Override
        public long max()
        {
            return AbstractMutableLongValuesMap.this.max();
        }

        @Override
        public long maxIfEmpty(long defaultValue)
        {
            return AbstractMutableLongValuesMap.this.maxIfEmpty(defaultValue);
        }

        @Override
        public long min()
        {
            return AbstractMutableLongValuesMap.this.min();
        }

        @Override
        public long minIfEmpty(long defaultValue)
        {
            return AbstractMutableLongValuesMap.this.minIfEmpty(defaultValue);
        }

        @Override
        public double average()
        {
            return AbstractMutableLongValuesMap.this.average();
        }

        @Override
        public double median()
        {
            return AbstractMutableLongValuesMap.this.median();
        }

        @Override
        public long[] toSortedArray()
        {
            return AbstractMutableLongValuesMap.this.toSortedArray();
        }

        @Override
        public MutableLongList toSortedList()
        {
            return AbstractMutableLongValuesMap.this.toSortedList();
        }

        @Override
        public MutableLongCollection with(long element)
        {
            throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableLongCollection without(long element)
        {
            throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableLongCollection withAll(LongIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableLongCollection withoutAll(LongIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableLongCollection asUnmodifiable()
        {
            return UnmodifiableLongCollection.of(this);
        }

        @Override
        public MutableLongCollection asSynchronized()
        {
            return SynchronizedLongCollection.of(this);
        }

        @Override
        public ImmutableLongCollection toImmutable()
        {
            return LongLists.immutable.withAll(this);
        }

        @Override
        public boolean contains(long value)
        {
            return AbstractMutableLongValuesMap.this.containsValue(value);
        }

        @Override
        public boolean containsAll(long... source)
        {
            return AbstractMutableLongValuesMap.this.containsAll(source);
        }

        @Override
        public boolean containsAll(LongIterable source)
        {
            return AbstractMutableLongValuesMap.this.containsAll(source);
        }

        @Override
        public MutableLongList toList()
        {
            return AbstractMutableLongValuesMap.this.toList();
        }

        @Override
        public MutableLongSet toSet()
        {
            return AbstractMutableLongValuesMap.this.toSet();
        }

        @Override
        public MutableLongBag toBag()
        {
            return AbstractMutableLongValuesMap.this.toBag();
        }

        @Override
        public LazyLongIterable asLazy()
        {
            return new LazyLongIterableAdapter(this);
        }

        @Override
        public boolean isEmpty()
        {
            return AbstractMutableLongValuesMap.this.isEmpty();
        }

        @Override
        public boolean notEmpty()
        {
            return AbstractMutableLongValuesMap.this.notEmpty();
        }

        @Override
        public String makeString()
        {
            return AbstractMutableLongValuesMap.this.makeString();
        }

        @Override
        public String makeString(String separator)
        {
            return AbstractMutableLongValuesMap.this.makeString(separator);
        }

        @Override
        public String makeString(String start, String separator, String end)
        {
            return AbstractMutableLongValuesMap.this.makeString(start, separator, end);
        }

        @Override
        public void appendString(Appendable appendable)
        {
            AbstractMutableLongValuesMap.this.appendString(appendable);
        }

        @Override
        public void appendString(Appendable appendable, String separator)
        {
            AbstractMutableLongValuesMap.this.appendString(appendable, separator);
        }

        @Override
        public void appendString(Appendable appendable, String start, String separator, String end)
        {
            AbstractMutableLongValuesMap.this.appendString(appendable, start, separator, end);
        }

        @Override
        public void each(LongProcedure procedure)
        {
            AbstractMutableLongValuesMap.this.each(procedure);
        }

        @Override
        public int count(LongPredicate predicate)
        {
            return AbstractMutableLongValuesMap.this.count(predicate);
        }

        @Override
        public boolean anySatisfy(LongPredicate predicate)
        {
            return AbstractMutableLongValuesMap.this.anySatisfy(predicate);
        }

        @Override
        public boolean allSatisfy(LongPredicate predicate)
        {
            return AbstractMutableLongValuesMap.this.allSatisfy(predicate);
        }

        @Override
        public boolean noneSatisfy(LongPredicate predicate)
        {
            return AbstractMutableLongValuesMap.this.noneSatisfy(predicate);
        }

        @Override
        public boolean add(long element)
        {
            throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(long... source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(LongIterable source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean removeAll(LongIterable source)
        {
            int oldSize = AbstractMutableLongValuesMap.this.size();

            LongIterator iterator = source.longIterator();
            while (iterator.hasNext())
            {
                this.remove(iterator.next());
            }
            return oldSize != AbstractMutableLongValuesMap.this.size();
        }

        @Override
        public boolean removeAll(long... source)
        {
            int oldSize = AbstractMutableLongValuesMap.this.size();

            for (long item : source)
            {
                this.remove(item);
            }
            return oldSize != AbstractMutableLongValuesMap.this.size();
        }

        @Override
        public boolean retainAll(long... source)
        {
            return this.retainAll(LongHashSet.newSetWith(source));
        }

        @Override
        public int size()
        {
            return AbstractMutableLongValuesMap.this.size();
        }

        @Override
        public long[] toArray()
        {
            return AbstractMutableLongValuesMap.this.toArray();
        }

        @Override
        public long[] toArray(long[] target)
        {
            return AbstractMutableLongValuesMap.this.toArray(target);
        }
    }
}
