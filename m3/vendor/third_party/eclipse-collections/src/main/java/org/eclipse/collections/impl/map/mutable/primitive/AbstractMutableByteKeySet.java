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
import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.set.primitive.ImmutableByteSet;
import org.eclipse.collections.impl.bag.mutable.primitive.ByteHashBag;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.ByteSets;
import org.eclipse.collections.impl.lazy.primitive.LazyByteIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.api.set.primitive.ByteSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.SynchronizedByteSet;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableByteSet;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.tuple.primitive.ByteBytePair;

/**
 * This file was automatically generated from template file abstractMutablePrimitiveKeySet.stg.
 *
 * @since 6.0.
 */
public abstract class AbstractMutableByteKeySet implements MutableByteSet, Externalizable
{
    private static final long serialVersionUID = 1L;
    private static final byte EMPTY_KEY = (byte) 0;
    private static final byte REMOVED_KEY = (byte) 1;

    private static boolean isEmptyKey(byte key)
    {
        return key == EMPTY_KEY;
    }

    private static boolean isRemovedKey(byte key)
    {
        return key == REMOVED_KEY;
    }

    private static boolean isNonSentinel(byte key)
    {
        return !isEmptyKey(key) && !isRemovedKey(key);
    }

    protected abstract byte getKeyAtIndex(int index);

    protected abstract int getTableSize();

    protected abstract MutableByteKeysMap getOuter();

    protected abstract AbstractSentinelValues getSentinelValues();

    /**
     * @since 7.0.
     */
    @Override
    public void each(ByteProcedure procedure)
    {
        this.getOuter().forEachKey(procedure);
    }

    @Override
    public int count(BytePredicate predicate)
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
    public boolean anySatisfy(BytePredicate predicate)
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
    public boolean allSatisfy(BytePredicate predicate)
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
    public MutableByteSet select(BytePredicate predicate)
    {
        MutableByteSet result = new ByteHashSet();
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
    public MutableByteSet reject(BytePredicate predicate)
    {
        MutableByteSet result = new ByteHashSet();
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
    public MutableByteSet with(byte element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableByteSet without(byte element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableByteSet withAll(ByteIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableByteSet withoutAll(ByteIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public <V> MutableSet<V> collect(ByteToObjectFunction<? extends V> function)
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
    public boolean remove(byte key)
    {
        int oldSize = this.getOuter().size();
        this.getOuter().removeKey(key);
        return oldSize != this.getOuter().size();
    }

    @Override
    public boolean removeAll(ByteIterable source)
    {
        int oldSize = this.getOuter().size();
        ByteIterator iterator = source.byteIterator();
        while (iterator.hasNext())
        {
            this.getOuter().removeKey(iterator.next());
        }
        return oldSize != this.getOuter().size();
    }

    @Override
    public boolean removeAll(byte... source)
    {
        int oldSize = this.getOuter().size();
        for (byte item : source)
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
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
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
    public MutableByteSet asUnmodifiable()
    {
        return UnmodifiableByteSet.of(this);
    }

    @Override
    public MutableByteSet asSynchronized()
    {
        return SynchronizedByteSet.of(this);
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
    public byte max()
    {
        if (this.getOuter().isEmpty())
        {
            throw new NoSuchElementException();
        }

        byte max = 0;
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
    public byte maxIfEmpty(byte defaultValue)
    {
        if (this.getOuter().isEmpty())
        {
            return defaultValue;
        }

        return this.max();
    }

    @Override
    public byte min()
    {
        if (this.getOuter().isEmpty())
        {
            throw new NoSuchElementException();
        }

        byte min = 0;
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
    public byte minIfEmpty(byte defaultValue)
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
        byte[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            byte first = sortedArray[middleIndex];
            byte second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public byte[] toSortedArray()
    {
        byte[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public MutableByteList toSortedList()
    {
        return ByteArrayList.newList(this).sortThis();
    }

    @Override
    public byte[] toArray()
    {
        int size = this.getOuter().size();
        final byte[] result = new byte[size];
        this.getOuter().forEachKey(new ByteProcedure()
        {
            private int index;

            public void value(byte each)
            {
                result[this.index] = each;
                this.index++;
            }
        });
        return result;
    }

    @Override
    public byte[] toArray(byte[] result)
    {
        int size = this.getOuter().size();
        if (result.length < size)
        {
            result = new byte[size];
        }

        byte[] finalBypass = result;

        this.getOuter().forEachKey(new ByteProcedure()
        {
            private int index;

            public void value(byte each)
            {
                finalBypass[this.index] = each;
                this.index++;
            }
        });
        return result;
    }

    @Override
    public boolean contains(byte value)
    {
        return this.getOuter().containsKey(value);
    }

    @Override
    public boolean containsAll(byte... source)
    {
        for (byte item : source)
        {
            if (!this.getOuter().containsKey(item))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(ByteIterable source)
    {
        ByteIterator iterator = source.byteIterator();
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
    public LazyIterable<ByteBytePair> cartesianProduct(ByteSet set)
    {
        return ByteSets.cartesianProduct(this, set);
    }

    @Override
    public MutableByteList toList()
    {
        return ByteArrayList.newList(this);
    }

    @Override
    public MutableByteSet toSet()
    {
        return ByteHashSet.newSet(this);
    }

    @Override
    public MutableByteBag toBag()
    {
        return ByteHashBag.newBag(this);
    }

    @Override
    public LazyByteIterable asLazy()
    {
        return new LazyByteIterableAdapter(this);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
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
    public ImmutableByteSet toImmutable()
    {
        return ByteSets.immutable.withAll(this);
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

        if (!(obj instanceof ByteSet))
        {
            return false;
        }

        ByteSet other = (ByteSet) obj;
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
                result.add(ByteSets.mutable.withAll(this));
            }
            else
            {
                ByteIterator iterator = this.byteIterator();
                while (iterator.hasNext())
                {
                    MutableByteSet batch = ByteSets.mutable.empty();
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

        private transient AbstractMutableByteKeySet original;
        private transient ByteHashSet deserialized;

        @SuppressWarnings("RedundantModifier")
        public SerRep()
        {
            // for Externalizable
        }

        private SerRep(AbstractMutableByteKeySet original)
        {
            this.original = original;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException
        {
            out.writeInt(this.original.size());
            ByteIterator it = this.original.byteIterator();
            while (it.hasNext())
            {
                out.writeByte(it.next());
            }
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
        {
            int unread = in.readInt();
            this.deserialized = new ByteHashSet(unread);
            while (unread > 0)
            {
                this.deserialized.add(in.readByte());
                unread--;
            }
        }

        private Object readResolve() throws ObjectStreamException
        {
            return this.deserialized;
        }
    }
}
