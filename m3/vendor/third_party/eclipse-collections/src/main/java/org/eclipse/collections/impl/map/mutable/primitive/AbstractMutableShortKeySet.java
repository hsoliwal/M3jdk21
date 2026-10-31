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
import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectStreamException;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.NoSuchElementException;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.api.set.primitive.ImmutableShortSet;
import org.eclipse.collections.impl.bag.mutable.primitive.ShortHashBag;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.ShortSets;
import org.eclipse.collections.impl.lazy.primitive.LazyShortIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.eclipse.collections.api.set.primitive.ShortSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.SynchronizedShortSet;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableShortSet;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.tuple.primitive.ShortShortPair;

/**
 * This file was automatically generated from template file abstractMutablePrimitiveKeySet.stg.
 *
 * @since 6.0.
 */
public abstract class AbstractMutableShortKeySet implements MutableShortSet, Externalizable
{
    private static final long serialVersionUID = 1L;
    private static final short EMPTY_KEY = (short) 0;
    private static final short REMOVED_KEY = (short) 1;

    private static boolean isEmptyKey(short key)
    {
        return key == EMPTY_KEY;
    }

    private static boolean isRemovedKey(short key)
    {
        return key == REMOVED_KEY;
    }

    private static boolean isNonSentinel(short key)
    {
        return !isEmptyKey(key) && !isRemovedKey(key);
    }

    protected abstract short getKeyAtIndex(int index);

    protected abstract int getTableSize();

    protected abstract MutableShortKeysMap getOuter();

    protected abstract AbstractSentinelValues getSentinelValues();

    /**
     * @since 7.0.
     */
    @Override
    public void each(ShortProcedure procedure)
    {
        this.getOuter().forEachKey(procedure);
    }

    @Override
    public int count(ShortPredicate predicate)
    {
        int count = 0;
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey && predicate.accept(EMPTY_KEY))
            {
                count++;
            }
            if (this.getSentinelValues().containsOneKey && predicate.accept(REMOVED_KEY))
            {
                count++;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)) && predicate.accept(this.getKeyAtIndex(i)))
            {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean anySatisfy(ShortPredicate predicate)
    {
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey && predicate.accept(EMPTY_KEY))
            {
                return true;
            }
            if (this.getSentinelValues().containsOneKey && predicate.accept(REMOVED_KEY))
            {
                return true;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)) && predicate.accept(this.getKeyAtIndex(i)))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allSatisfy(ShortPredicate predicate)
    {
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey && !predicate.accept(EMPTY_KEY))
            {
                return false;
            }
            if (this.getSentinelValues().containsOneKey && !predicate.accept(REMOVED_KEY))
            {
                return false;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)) && !predicate.accept(this.getKeyAtIndex(i)))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean add(short element)
    {
        throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(short... source)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(ShortIterable source)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortSet select(ShortPredicate predicate)
    {
        MutableShortSet result = new ShortHashSet();
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey && predicate.accept(EMPTY_KEY))
            {
                result.add(EMPTY_KEY);
            }
            if (this.getSentinelValues().containsOneKey && predicate.accept(REMOVED_KEY))
            {
                result.add(REMOVED_KEY);
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)) && predicate.accept(this.getKeyAtIndex(i)))
            {
                result.add(this.getKeyAtIndex(i));
            }
        }
        return result;
    }

    @Override
    public MutableShortSet reject(ShortPredicate predicate)
    {
        MutableShortSet result = new ShortHashSet();
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey && !predicate.accept(EMPTY_KEY))
            {
                result.add(EMPTY_KEY);
            }
            if (this.getSentinelValues().containsOneKey && !predicate.accept(REMOVED_KEY))
            {
                result.add(REMOVED_KEY);
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)) && !predicate.accept(this.getKeyAtIndex(i)))
            {
                result.add(this.getKeyAtIndex(i));
            }
        }
        return result;
    }

    @Override
    public MutableShortSet with(short element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortSet without(short element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortSet withAll(ShortIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortSet withoutAll(ShortIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public <V> MutableSet<V> collect(ShortToObjectFunction<? extends V> function)
    {
        MutableSet<V> result = Sets.mutable.with();
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey)
            {
                result.add(function.valueOf(EMPTY_KEY));
            }
            if (this.getSentinelValues().containsOneKey)
            {
                result.add(function.valueOf(REMOVED_KEY));
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)))
            {
                result.add(function.valueOf(this.getKeyAtIndex(i)));
            }
        }
        return result;
    }

    @Override
    public boolean remove(short key)
    {
        int oldSize = this.getOuter().size();
        this.getOuter().removeKey(key);
        return oldSize != this.getOuter().size();
    }

    @Override
    public boolean removeAll(ShortIterable source)
    {
        int oldSize = this.getOuter().size();
        ShortIterator iterator = source.shortIterator();
        while (iterator.hasNext())
        {
            this.getOuter().removeKey(iterator.next());
        }
        return oldSize != this.getOuter().size();
    }

    @Override
    public boolean removeAll(short... source)
    {
        int oldSize = this.getOuter().size();
        for (short item : source)
        {
            this.getOuter().removeKey(item);
        }
        return oldSize != this.getOuter().size();
    }

    @Override
    public void clear()
    {
        this.getOuter().clear();
    }

    @Override
    public short detectIfNone(ShortPredicate predicate, short ifNone)
    {
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey && predicate.accept(EMPTY_KEY))
            {
                return EMPTY_KEY;
            }
            if (this.getSentinelValues().containsOneKey && predicate.accept(REMOVED_KEY))
            {
                return REMOVED_KEY;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)) && predicate.accept(this.getKeyAtIndex(i)))
            {
                return this.getKeyAtIndex(i);
            }
        }
        return ifNone;
    }

    @Override
    public MutableShortSet asUnmodifiable()
    {
        return UnmodifiableShortSet.of(this);
    }

    @Override
    public MutableShortSet asSynchronized()
    {
        return SynchronizedShortSet.of(this);
    }

    @Override
    public long sum()
    {
        long result = 0L;
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey)
            {
                result += EMPTY_KEY;
            }
            if (this.getSentinelValues().containsOneKey)
            {
                result += REMOVED_KEY;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)))
            {
                result += this.getKeyAtIndex(i);
            }
        }
        return result;
    }

    @Override
    public short max()
    {
        if (this.getOuter().isEmpty())
        {
            throw new NoSuchElementException();
        }

        short max = 0;
        boolean isMaxSet = false;

        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey)
            {
                max = EMPTY_KEY;
                isMaxSet = true;
            }
            if (this.getSentinelValues().containsOneKey)
            {
                max = REMOVED_KEY;
                isMaxSet = true;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)) && (!isMaxSet || max < this.getKeyAtIndex(i)))
            {
                max = this.getKeyAtIndex(i);
                isMaxSet = true;
            }
        }
        return max;
    }

    @Override
    public short maxIfEmpty(short defaultValue)
    {
        if (this.getOuter().isEmpty())
        {
            return defaultValue;
        }

        return this.max();
    }

    @Override
    public short min()
    {
        if (this.getOuter().isEmpty())
        {
            throw new NoSuchElementException();
        }

        short min = 0;
        boolean isMinSet = false;

        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey)
            {
                min = EMPTY_KEY;
                isMinSet = true;
            }
            if (this.getSentinelValues().containsOneKey && !isMinSet)
            {
                min = REMOVED_KEY;
                isMinSet = true;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)) && (!isMinSet || this.getKeyAtIndex(i) < min))
            {
                min = this.getKeyAtIndex(i);
                isMinSet = true;
            }
        }
        return min;
    }

    @Override
    public short minIfEmpty(short defaultValue)
    {
        if (this.getOuter().isEmpty())
        {
            return defaultValue;
        }

        return this.min();
    }

    @Override
    public double average()
    {
        if (this.isEmpty())
        {
            throw new ArithmeticException();
        }
        return (double) this.sum() / (double) this.size();
    }

    @Override
    public double median()
    {
        if (this.isEmpty())
        {
            throw new ArithmeticException();
        }
        short[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            short first = sortedArray[middleIndex];
            short second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public short[] toSortedArray()
    {
        short[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public MutableShortList toSortedList()
    {
        return ShortArrayList.newList(this).sortThis();
    }

    @Override
    public short[] toArray()
    {
        int size = this.getOuter().size();
        final short[] result = new short[size];
        this.getOuter().forEachKey(new ShortProcedure()
        {
            private int index;

            public void value(short each)
            {
                result[this.index] = each;
                this.index++;
            }
        });
        return result;
    }

    @Override
    public short[] toArray(short[] result)
    {
        int size = this.getOuter().size();
        if (result.length < size)
        {
            result = new short[size];
        }

        short[] finalBypass = result;

        this.getOuter().forEachKey(new ShortProcedure()
        {
            private int index;

            public void value(short each)
            {
                finalBypass[this.index] = each;
                this.index++;
            }
        });
        return result;
    }

    @Override
    public boolean contains(short value)
    {
        return this.getOuter().containsKey(value);
    }

    @Override
    public boolean containsAll(short... source)
    {
        for (short item : source)
        {
            if (!this.getOuter().containsKey(item))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(ShortIterable source)
    {
        ShortIterator iterator = source.shortIterator();
        while (iterator.hasNext())
        {
            if (!this.getOuter().containsKey(iterator.next()))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public LazyIterable<ShortShortPair> cartesianProduct(ShortSet set)
    {
        return ShortSets.cartesianProduct(this, set);
    }

    @Override
    public MutableShortList toList()
    {
        return ShortArrayList.newList(this);
    }

    @Override
    public MutableShortSet toSet()
    {
        return ShortHashSet.newSet(this);
    }

    @Override
    public MutableShortBag toBag()
    {
        return ShortHashBag.newBag(this);
    }

    @Override
    public LazyShortIterable asLazy()
    {
        return new LazyShortIterableAdapter(this);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectShortToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey)
            {
                result = function.valueOf(result, EMPTY_KEY);
            }
            if (this.getSentinelValues().containsOneKey)
            {
                result = function.valueOf(result, REMOVED_KEY);
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)))
            {
                result = function.valueOf(result, this.getKeyAtIndex(i));
            }
        }
        return result;
    }

    @Override
    public ImmutableShortSet toImmutable()
    {
        return ShortSets.immutable.withAll(this);
    }

    @Override
    public int size()
    {
        return this.getOuter().size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.getOuter().isEmpty();
    }

    @Override
    public boolean notEmpty()
    {
        return this.getOuter().notEmpty();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof ShortSet))
        {
            return false;
        }

        ShortSet other = (ShortSet) obj;
        return this.size() == other.size() && this.containsAll(other.toArray());
    }

    @Override
    public int hashCode()
    {
        int result = 0;

        if (this.getSentinelValues() != null)
        {
            if (this.getSentinelValues().containsZeroKey)
            {
                result += (int) EMPTY_KEY;
            }
            if (this.getSentinelValues().containsOneKey)
            {
                result += (int) REMOVED_KEY;
            }
        }
        for (int i = 0; i < this.getTableSize(); i++)
        {
            if (isNonSentinel(this.getKeyAtIndex(i)))
            {
                result += (int) this.getKeyAtIndex(i);
            }
        }

        return result;
    }

    @Override
    public String toString()
    {
        return this.makeString("[", ", ", "]");
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
            boolean first = true;
            if (this.getSentinelValues() != null)
            {
                if (this.getSentinelValues().containsZeroKey)
                {
                    appendable.append(String.valueOf(EMPTY_KEY));
                    first = false;
                }
                if (this.getSentinelValues().containsOneKey)
                {
                    if (!first)
                    {
                        appendable.append(separator);
                    }
                    appendable.append(String.valueOf(REMOVED_KEY));
                    first = false;
                }
            }
            for (int i = 0; i < this.getTableSize(); i++)
            {
                if (isNonSentinel(this.getKeyAtIndex(i)))
                {
                    if (!first)
                    {
                        appendable.append(separator);
                    }
                    appendable.append(String.valueOf(this.getKeyAtIndex(i)));
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
    public RichIterable<ShortIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<ShortIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(ShortSets.mutable.withAll(this));
            }
            else
            {
                ShortIterator iterator = this.shortIterator();
                while (iterator.hasNext())
                {
                    MutableShortSet batch = ShortSets.mutable.empty();
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

    public Object writeReplace() throws ObjectStreamException
    {
        return new SerRep(this);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
    }

    private static class SerRep implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private transient AbstractMutableShortKeySet original;
        private transient ShortHashSet deserialized;

        @SuppressWarnings("RedundantModifier")
        public SerRep()
        {
            // for Externalizable
        }

        private SerRep(AbstractMutableShortKeySet original)
        {
            this.original = original;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException
        {
            out.writeInt(this.original.size());
            ShortIterator it = this.original.shortIterator();
            while (it.hasNext())
            {
                out.writeShort(it.next());
            }
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
        {
            int unread = in.readInt();
            this.deserialized = new ShortHashSet(unread);
            while (unread > 0)
            {
                this.deserialized.add(in.readShort());
                unread--;
            }
        }

        private Object readResolve() throws ObjectStreamException
        {
            return this.deserialized;
        }
    }
}
