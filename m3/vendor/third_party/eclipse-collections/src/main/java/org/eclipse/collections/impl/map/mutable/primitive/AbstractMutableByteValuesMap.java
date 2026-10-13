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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableByteCollection;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.factory.Bags;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.map.primitive.MutableByteValuesMap;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.impl.bag.mutable.primitive.ByteHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.SynchronizedByteCollection;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableByteCollection;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.ByteBags;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.eclipse.collections.impl.lazy.primitive.LazyByteIterableAdapter;
import org.eclipse.collections.impl.primitive.AbstractByteIterable;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;

/**
 * This file was automatically generated from template file abstractMutablePrimitiveValuesMap.stg.
 *
 * @since 6.0.
 */
public abstract class AbstractMutableByteValuesMap extends AbstractByteIterable implements MutableByteValuesMap
{
    protected abstract int getOccupiedWithData();

    protected abstract SentinelValues getSentinelValues();

    protected abstract void setSentinelValuesNull();

    protected abstract byte getEmptyValue();

    protected abstract byte getValueAtIndex(int index);

    protected abstract int getTableSize();

    protected abstract boolean isNonSentinelAtIndex(int index);

    protected void addEmptyKeyValue(byte value)
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

    protected void addRemovedKeyValue(byte value)
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
    public boolean contains(byte value)
    {
        return this.containsValue(value);
    }

    @Override
    public boolean containsAll(ByteIterable source)
    {
        return source.allSatisfy((byte value) -> AbstractMutableByteValuesMap.this.contains(value));
    }

    @Override
    public byte max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        ByteIterator iterator = this.byteIterator();
        byte max = iterator.next();
        while (iterator.hasNext())
        {
            byte value = iterator.next();
            if (max < value)
            {
                max = value;
            }
        }
        return max;
    }

    @Override
    public byte min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        ByteIterator iterator = this.byteIterator();
        byte min = iterator.next();
        while (iterator.hasNext())
        {
            byte value = iterator.next();
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
    public void each(ByteProcedure procedure)
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
    public byte[] toArray()
    {
        byte[] array = new byte[this.size()];
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
    public byte[] toArray(byte[] target)
    {
        if (target.length < this.size())
        {
            target = new byte[this.size()];
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
    public MutableByteBag select(BytePredicate predicate)
    {
        return this.select(predicate, new ByteHashBag());
    }

    @Override
    public MutableByteBag reject(BytePredicate predicate)
    {
        return this.reject(predicate, new ByteHashBag());
    }

    @Override
    public <V> MutableBag<V> collect(ByteToObjectFunction<? extends V> function)
    {
        return this.collect(function, Bags.mutable.withInitialCapacity(this.size()));
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte value)
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
    public int count(BytePredicate predicate)
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
    public boolean anySatisfy(BytePredicate predicate)
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
    public boolean allSatisfy(BytePredicate predicate)
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
    public RichIterable<ByteIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<ByteIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(ByteBags.mutable.withAll(this));
            }
            else
            {
                ByteIterator iterator = this.byteIterator();
                while (iterator.hasNext())
                {
                    MutableByteBag batch = ByteBags.mutable.empty();
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
    public boolean containsValue(byte value)
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
    public void forEachValue(ByteProcedure procedure)
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
        protected byte zeroValue;
        protected byte oneValue;

        public boolean containsValue(byte value)
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

    protected abstract class AbstractByteValuesCollection implements MutableByteCollection
    {
        @Override
        public void clear()
        {
            AbstractMutableByteValuesMap.this.clear();
        }

        @Override
        public MutableByteCollection select(BytePredicate predicate)
        {
            return AbstractMutableByteValuesMap.this.select(predicate);
        }

        @Override
        public MutableByteCollection reject(BytePredicate predicate)
        {
            return AbstractMutableByteValuesMap.this.reject(predicate);
        }

        @Override
        public byte detectIfNone(BytePredicate predicate, byte ifNone)
        {
            return AbstractMutableByteValuesMap.this.detectIfNone(predicate, ifNone);
        }

        @Override
        public <V> MutableCollection<V> collect(ByteToObjectFunction<? extends V> function)
        {
            return AbstractMutableByteValuesMap.this.collect(function);
        }

        @Override
        public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
        {
            return AbstractMutableByteValuesMap.this.injectInto(injectedValue, function);
        }

        @Override
        public RichIterable<ByteIterable> chunk(int size)
        {
            return AbstractMutableByteValuesMap.this.chunk(size);
        }

        @Override
        public long sum()
        {
            return AbstractMutableByteValuesMap.this.sum();
        }

        @Override
        public byte max()
        {
            return AbstractMutableByteValuesMap.this.max();
        }

        @Override
        public byte maxIfEmpty(byte defaultValue)
        {
            return AbstractMutableByteValuesMap.this.maxIfEmpty(defaultValue);
        }

        @Override
        public byte min()
        {
            return AbstractMutableByteValuesMap.this.min();
        }

        @Override
        public byte minIfEmpty(byte defaultValue)
        {
            return AbstractMutableByteValuesMap.this.minIfEmpty(defaultValue);
        }

        @Override
        public double average()
        {
            return AbstractMutableByteValuesMap.this.average();
        }

        @Override
        public double median()
        {
            return AbstractMutableByteValuesMap.this.median();
        }

        @Override
        public byte[] toSortedArray()
        {
            return AbstractMutableByteValuesMap.this.toSortedArray();
        }

        @Override
        public MutableByteList toSortedList()
        {
            return AbstractMutableByteValuesMap.this.toSortedList();
        }

        @Override
        public MutableByteCollection with(byte element)
        {
            throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableByteCollection without(byte element)
        {
            throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableByteCollection withAll(ByteIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableByteCollection withoutAll(ByteIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableByteCollection asUnmodifiable()
        {
            return UnmodifiableByteCollection.of(this);
        }

        @Override
        public MutableByteCollection asSynchronized()
        {
            return SynchronizedByteCollection.of(this);
        }

        @Override
        public ImmutableByteCollection toImmutable()
        {
            return ByteLists.immutable.withAll(this);
        }

        @Override
        public boolean contains(byte value)
        {
            return AbstractMutableByteValuesMap.this.containsValue(value);
        }

        @Override
        public boolean containsAll(byte... source)
        {
            return AbstractMutableByteValuesMap.this.containsAll(source);
        }

        @Override
        public boolean containsAll(ByteIterable source)
        {
            return AbstractMutableByteValuesMap.this.containsAll(source);
        }

        @Override
        public MutableByteList toList()
        {
            return AbstractMutableByteValuesMap.this.toList();
        }

        @Override
        public MutableByteSet toSet()
        {
            return AbstractMutableByteValuesMap.this.toSet();
        }

        @Override
        public MutableByteBag toBag()
        {
            return AbstractMutableByteValuesMap.this.toBag();
        }

        @Override
        public LazyByteIterable asLazy()
        {
            return new LazyByteIterableAdapter(this);
        }

        @Override
        public boolean isEmpty()
        {
            return AbstractMutableByteValuesMap.this.isEmpty();
        }

        @Override
        public boolean notEmpty()
        {
            return AbstractMutableByteValuesMap.this.notEmpty();
        }

        @Override
        public String makeString()
        {
            return AbstractMutableByteValuesMap.this.makeString();
        }

        @Override
        public String makeString(String separator)
        {
            return AbstractMutableByteValuesMap.this.makeString(separator);
        }

        @Override
        public String makeString(String start, String separator, String end)
        {
            return AbstractMutableByteValuesMap.this.makeString(start, separator, end);
        }

        @Override
        public void appendString(Appendable appendable)
        {
            AbstractMutableByteValuesMap.this.appendString(appendable);
        }

        @Override
        public void appendString(Appendable appendable, String separator)
        {
            AbstractMutableByteValuesMap.this.appendString(appendable, separator);
        }

        @Override
        public void appendString(Appendable appendable, String start, String separator, String end)
        {
            AbstractMutableByteValuesMap.this.appendString(appendable, start, separator, end);
        }

        @Override
        public void each(ByteProcedure procedure)
        {
            AbstractMutableByteValuesMap.this.each(procedure);
        }

        @Override
        public int count(BytePredicate predicate)
        {
            return AbstractMutableByteValuesMap.this.count(predicate);
        }

        @Override
        public boolean anySatisfy(BytePredicate predicate)
        {
            return AbstractMutableByteValuesMap.this.anySatisfy(predicate);
        }

        @Override
        public boolean allSatisfy(BytePredicate predicate)
        {
            return AbstractMutableByteValuesMap.this.allSatisfy(predicate);
        }

        @Override
        public boolean noneSatisfy(BytePredicate predicate)
        {
            return AbstractMutableByteValuesMap.this.noneSatisfy(predicate);
        }

        @Override
        public boolean add(byte element)
        {
            throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(byte... source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(ByteIterable source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean removeAll(ByteIterable source)
        {
            int oldSize = AbstractMutableByteValuesMap.this.size();

            ByteIterator iterator = source.byteIterator();
            while (iterator.hasNext())
            {
                this.remove(iterator.next());
            }
            return oldSize != AbstractMutableByteValuesMap.this.size();
        }

        @Override
        public boolean removeAll(byte... source)
        {
            int oldSize = AbstractMutableByteValuesMap.this.size();

            for (byte item : source)
            {
                this.remove(item);
            }
            return oldSize != AbstractMutableByteValuesMap.this.size();
        }

        @Override
        public boolean retainAll(byte... source)
        {
            return this.retainAll(ByteHashSet.newSetWith(source));
        }

        @Override
        public int size()
        {
            return AbstractMutableByteValuesMap.this.size();
        }

        @Override
        public byte[] toArray()
        {
            return AbstractMutableByteValuesMap.this.toArray();
        }

        @Override
        public byte[] toArray(byte[] target)
        {
            return AbstractMutableByteValuesMap.this.toArray(target);
        }
    }
}
