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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableCharBag;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableCharCollection;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.factory.Bags;
import org.eclipse.collections.api.iterator.CharIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.api.map.primitive.MutableCharValuesMap;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.impl.bag.mutable.primitive.CharHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.SynchronizedCharCollection;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableCharCollection;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.CharBags;
import org.eclipse.collections.impl.factory.primitive.CharLists;
import org.eclipse.collections.impl.lazy.primitive.LazyCharIterableAdapter;
import org.eclipse.collections.impl.primitive.AbstractCharIterable;
import org.eclipse.collections.impl.set.mutable.primitive.CharHashSet;

/**
 * This file was automatically generated from template file abstractMutablePrimitiveValuesMap.stg.
 *
 * @since 6.0.
 */
public abstract class AbstractMutableCharValuesMap extends AbstractCharIterable implements MutableCharValuesMap
{
    protected abstract int getOccupiedWithData();

    protected abstract SentinelValues getSentinelValues();

    protected abstract void setSentinelValuesNull();

    protected abstract char getEmptyValue();

    protected abstract char getValueAtIndex(int index);

    protected abstract int getTableSize();

    protected abstract boolean isNonSentinelAtIndex(int index);

    protected void addEmptyKeyValue(char value)
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

    protected void addRemovedKeyValue(char value)
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
    public boolean contains(char value)
    {
        return this.containsValue(value);
    }

    @Override
    public boolean containsAll(CharIterable source)
    {
        return source.allSatisfy((char value) -> AbstractMutableCharValuesMap.this.contains(value));
    }

    @Override
    public char max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        CharIterator iterator = this.charIterator();
        char max = iterator.next();
        while (iterator.hasNext())
        {
            char value = iterator.next();
            if (max < value)
            {
                max = value;
            }
        }
        return max;
    }

    @Override
    public char min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        CharIterator iterator = this.charIterator();
        char min = iterator.next();
        while (iterator.hasNext())
        {
            char value = iterator.next();
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
    public void each(CharProcedure procedure)
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
    public char[] toArray()
    {
        char[] array = new char[this.size()];
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
    public char[] toArray(char[] target)
    {
        if (target.length < this.size())
        {
            target = new char[this.size()];
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
    public MutableCharBag select(CharPredicate predicate)
    {
        return this.select(predicate, new CharHashBag());
    }

    @Override
    public MutableCharBag reject(CharPredicate predicate)
    {
        return this.reject(predicate, new CharHashBag());
    }

    @Override
    public <V> MutableBag<V> collect(CharToObjectFunction<? extends V> function)
    {
        return this.collect(function, Bags.mutable.withInitialCapacity(this.size()));
    }

    @Override
    public char detectIfNone(CharPredicate predicate, char value)
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
    public int count(CharPredicate predicate)
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
    public boolean anySatisfy(CharPredicate predicate)
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
    public boolean allSatisfy(CharPredicate predicate)
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
    public RichIterable<CharIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<CharIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(CharBags.mutable.withAll(this));
            }
            else
            {
                CharIterator iterator = this.charIterator();
                while (iterator.hasNext())
                {
                    MutableCharBag batch = CharBags.mutable.empty();
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
    public boolean containsValue(char value)
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
    public void forEachValue(CharProcedure procedure)
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
        protected char zeroValue;
        protected char oneValue;

        public boolean containsValue(char value)
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

    protected abstract class AbstractCharValuesCollection implements MutableCharCollection
    {
        @Override
        public void clear()
        {
            AbstractMutableCharValuesMap.this.clear();
        }

        @Override
        public MutableCharCollection select(CharPredicate predicate)
        {
            return AbstractMutableCharValuesMap.this.select(predicate);
        }

        @Override
        public MutableCharCollection reject(CharPredicate predicate)
        {
            return AbstractMutableCharValuesMap.this.reject(predicate);
        }

        @Override
        public char detectIfNone(CharPredicate predicate, char ifNone)
        {
            return AbstractMutableCharValuesMap.this.detectIfNone(predicate, ifNone);
        }

        @Override
        public <V> MutableCollection<V> collect(CharToObjectFunction<? extends V> function)
        {
            return AbstractMutableCharValuesMap.this.collect(function);
        }

        @Override
        public <T> T injectInto(T injectedValue, ObjectCharToObjectFunction<? super T, ? extends T> function)
        {
            return AbstractMutableCharValuesMap.this.injectInto(injectedValue, function);
        }

        @Override
        public RichIterable<CharIterable> chunk(int size)
        {
            return AbstractMutableCharValuesMap.this.chunk(size);
        }

        @Override
        public long sum()
        {
            return AbstractMutableCharValuesMap.this.sum();
        }

        @Override
        public char max()
        {
            return AbstractMutableCharValuesMap.this.max();
        }

        @Override
        public char maxIfEmpty(char defaultValue)
        {
            return AbstractMutableCharValuesMap.this.maxIfEmpty(defaultValue);
        }

        @Override
        public char min()
        {
            return AbstractMutableCharValuesMap.this.min();
        }

        @Override
        public char minIfEmpty(char defaultValue)
        {
            return AbstractMutableCharValuesMap.this.minIfEmpty(defaultValue);
        }

        @Override
        public double average()
        {
            return AbstractMutableCharValuesMap.this.average();
        }

        @Override
        public double median()
        {
            return AbstractMutableCharValuesMap.this.median();
        }

        @Override
        public char[] toSortedArray()
        {
            return AbstractMutableCharValuesMap.this.toSortedArray();
        }

        @Override
        public MutableCharList toSortedList()
        {
            return AbstractMutableCharValuesMap.this.toSortedList();
        }

        @Override
        public MutableCharCollection with(char element)
        {
            throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableCharCollection without(char element)
        {
            throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableCharCollection withAll(CharIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableCharCollection withoutAll(CharIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableCharCollection asUnmodifiable()
        {
            return UnmodifiableCharCollection.of(this);
        }

        @Override
        public MutableCharCollection asSynchronized()
        {
            return SynchronizedCharCollection.of(this);
        }

        @Override
        public ImmutableCharCollection toImmutable()
        {
            return CharLists.immutable.withAll(this);
        }

        @Override
        public boolean contains(char value)
        {
            return AbstractMutableCharValuesMap.this.containsValue(value);
        }

        @Override
        public boolean containsAll(char... source)
        {
            return AbstractMutableCharValuesMap.this.containsAll(source);
        }

        @Override
        public boolean containsAll(CharIterable source)
        {
            return AbstractMutableCharValuesMap.this.containsAll(source);
        }

        @Override
        public MutableCharList toList()
        {
            return AbstractMutableCharValuesMap.this.toList();
        }

        @Override
        public MutableCharSet toSet()
        {
            return AbstractMutableCharValuesMap.this.toSet();
        }

        @Override
        public MutableCharBag toBag()
        {
            return AbstractMutableCharValuesMap.this.toBag();
        }

        @Override
        public LazyCharIterable asLazy()
        {
            return new LazyCharIterableAdapter(this);
        }

        @Override
        public boolean isEmpty()
        {
            return AbstractMutableCharValuesMap.this.isEmpty();
        }

        @Override
        public boolean notEmpty()
        {
            return AbstractMutableCharValuesMap.this.notEmpty();
        }

        @Override
        public String makeString()
        {
            return AbstractMutableCharValuesMap.this.makeString();
        }

        @Override
        public String makeString(String separator)
        {
            return AbstractMutableCharValuesMap.this.makeString(separator);
        }

        @Override
        public String makeString(String start, String separator, String end)
        {
            return AbstractMutableCharValuesMap.this.makeString(start, separator, end);
        }

        @Override
        public void appendString(Appendable appendable)
        {
            AbstractMutableCharValuesMap.this.appendString(appendable);
        }

        @Override
        public void appendString(Appendable appendable, String separator)
        {
            AbstractMutableCharValuesMap.this.appendString(appendable, separator);
        }

        @Override
        public void appendString(Appendable appendable, String start, String separator, String end)
        {
            AbstractMutableCharValuesMap.this.appendString(appendable, start, separator, end);
        }

        @Override
        public void each(CharProcedure procedure)
        {
            AbstractMutableCharValuesMap.this.each(procedure);
        }

        @Override
        public int count(CharPredicate predicate)
        {
            return AbstractMutableCharValuesMap.this.count(predicate);
        }

        @Override
        public boolean anySatisfy(CharPredicate predicate)
        {
            return AbstractMutableCharValuesMap.this.anySatisfy(predicate);
        }

        @Override
        public boolean allSatisfy(CharPredicate predicate)
        {
            return AbstractMutableCharValuesMap.this.allSatisfy(predicate);
        }

        @Override
        public boolean noneSatisfy(CharPredicate predicate)
        {
            return AbstractMutableCharValuesMap.this.noneSatisfy(predicate);
        }

        @Override
        public boolean add(char element)
        {
            throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(char... source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(CharIterable source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean removeAll(CharIterable source)
        {
            int oldSize = AbstractMutableCharValuesMap.this.size();

            CharIterator iterator = source.charIterator();
            while (iterator.hasNext())
            {
                this.remove(iterator.next());
            }
            return oldSize != AbstractMutableCharValuesMap.this.size();
        }

        @Override
        public boolean removeAll(char... source)
        {
            int oldSize = AbstractMutableCharValuesMap.this.size();

            for (char item : source)
            {
                this.remove(item);
            }
            return oldSize != AbstractMutableCharValuesMap.this.size();
        }

        @Override
        public boolean retainAll(char... source)
        {
            return this.retainAll(CharHashSet.newSetWith(source));
        }

        @Override
        public int size()
        {
            return AbstractMutableCharValuesMap.this.size();
        }

        @Override
        public char[] toArray()
        {
            return AbstractMutableCharValuesMap.this.toArray();
        }

        @Override
        public char[] toArray(char[] target)
        {
            return AbstractMutableCharValuesMap.this.toArray(target);
        }
    }
}
