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
import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.bag.primitive.MutableCharBag;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.iterator.CharIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.api.set.primitive.ImmutableCharSet;
import org.eclipse.collections.impl.bag.mutable.primitive.CharHashBag;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.CharSets;
import org.eclipse.collections.impl.lazy.primitive.LazyCharIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.CharArrayList;
import org.eclipse.collections.api.set.primitive.CharSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.primitive.CharHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.SynchronizedCharSet;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableCharSet;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.tuple.primitive.CharCharPair;

/**
 * This file was automatically generated from template file abstractMutablePrimitiveKeySet.stg.
 *
 * @since 6.0.
 */
public abstract class AbstractMutableCharKeySet implements MutableCharSet, Externalizable
{
    private static final long serialVersionUID = 1L;
    private static final char EMPTY_KEY = (char) 0;
    private static final char REMOVED_KEY = (char) 1;

    private static boolean isEmptyKey(char key)
    {
        return key == EMPTY_KEY;
    }

    private static boolean isRemovedKey(char key)
    {
        return key == REMOVED_KEY;
    }

    private static boolean isNonSentinel(char key)
    {
        return !isEmptyKey(key) && !isRemovedKey(key);
    }

    protected abstract char getKeyAtIndex(int index);

    protected abstract int getTableSize();

    protected abstract MutableCharKeysMap getOuter();

    protected abstract AbstractSentinelValues getSentinelValues();

    /**
     * @since 7.0.
     */
    @Override
    public void each(CharProcedure procedure)
    {
        this.getOuter().forEachKey(procedure);
    }

    @Override
    public int count(CharPredicate predicate)
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
    public boolean anySatisfy(CharPredicate predicate)
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
    public boolean allSatisfy(CharPredicate predicate)
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
    public MutableCharSet select(CharPredicate predicate)
    {
        MutableCharSet result = new CharHashSet();
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
    public MutableCharSet reject(CharPredicate predicate)
    {
        MutableCharSet result = new CharHashSet();
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
    public MutableCharSet with(char element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableCharSet without(char element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableCharSet withAll(CharIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableCharSet withoutAll(CharIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public <V> MutableSet<V> collect(CharToObjectFunction<? extends V> function)
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
    public boolean remove(char key)
    {
        int oldSize = this.getOuter().size();
        this.getOuter().removeKey(key);
        return oldSize != this.getOuter().size();
    }

    @Override
    public boolean removeAll(CharIterable source)
    {
        int oldSize = this.getOuter().size();
        CharIterator iterator = source.charIterator();
        while (iterator.hasNext())
        {
            this.getOuter().removeKey(iterator.next());
        }
        return oldSize != this.getOuter().size();
    }

    @Override
    public boolean removeAll(char... source)
    {
        int oldSize = this.getOuter().size();
        for (char item : source)
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
    public char detectIfNone(CharPredicate predicate, char ifNone)
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
    public MutableCharSet asUnmodifiable()
    {
        return UnmodifiableCharSet.of(this);
    }

    @Override
    public MutableCharSet asSynchronized()
    {
        return SynchronizedCharSet.of(this);
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
    public char max()
    {
        if (this.getOuter().isEmpty())
        {
            throw new NoSuchElementException();
        }

        char max = 0;
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
    public char maxIfEmpty(char defaultValue)
    {
        if (this.getOuter().isEmpty())
        {
            return defaultValue;
        }

        return this.max();
    }

    @Override
    public char min()
    {
        if (this.getOuter().isEmpty())
        {
            throw new NoSuchElementException();
        }

        char min = 0;
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
    public char minIfEmpty(char defaultValue)
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
        char[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            char first = sortedArray[middleIndex];
            char second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public char[] toSortedArray()
    {
        char[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public MutableCharList toSortedList()
    {
        return CharArrayList.newList(this).sortThis();
    }

    @Override
    public char[] toArray()
    {
        int size = this.getOuter().size();
        final char[] result = new char[size];
        this.getOuter().forEachKey(new CharProcedure()
        {
            private int index;

            public void value(char each)
            {
                result[this.index] = each;
                this.index++;
            }
        });
        return result;
    }

    @Override
    public char[] toArray(char[] result)
    {
        int size = this.getOuter().size();
        if (result.length < size)
        {
            result = new char[size];
        }

        char[] finalBypass = result;

        this.getOuter().forEachKey(new CharProcedure()
        {
            private int index;

            public void value(char each)
            {
                finalBypass[this.index] = each;
                this.index++;
            }
        });
        return result;
    }

    @Override
    public boolean contains(char value)
    {
        return this.getOuter().containsKey(value);
    }

    @Override
    public boolean containsAll(char... source)
    {
        for (char item : source)
        {
            if (!this.getOuter().containsKey(item))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(CharIterable source)
    {
        CharIterator iterator = source.charIterator();
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
    public LazyIterable<CharCharPair> cartesianProduct(CharSet set)
    {
        return CharSets.cartesianProduct(this, set);
    }

    @Override
    public MutableCharList toList()
    {
        return CharArrayList.newList(this);
    }

    @Override
    public MutableCharSet toSet()
    {
        return CharHashSet.newSet(this);
    }

    @Override
    public MutableCharBag toBag()
    {
        return CharHashBag.newBag(this);
    }

    @Override
    public LazyCharIterable asLazy()
    {
        return new LazyCharIterableAdapter(this);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectCharToObjectFunction<? super T, ? extends T> function)
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
    public ImmutableCharSet toImmutable()
    {
        return CharSets.immutable.withAll(this);
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

        if (!(obj instanceof CharSet))
        {
            return false;
        }

        CharSet other = (CharSet) obj;
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
                result.add(CharSets.mutable.withAll(this));
            }
            else
            {
                CharIterator iterator = this.charIterator();
                while (iterator.hasNext())
                {
                    MutableCharSet batch = CharSets.mutable.empty();
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

        private transient AbstractMutableCharKeySet original;
        private transient CharHashSet deserialized;

        @SuppressWarnings("RedundantModifier")
        public SerRep()
        {
            // for Externalizable
        }

        private SerRep(AbstractMutableCharKeySet original)
        {
            this.original = original;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException
        {
            out.writeInt(this.original.size());
            CharIterator it = this.original.charIterator();
            while (it.hasNext())
            {
                out.writeChar(it.next());
            }
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
        {
            int unread = in.readInt();
            this.deserialized = new CharHashSet(unread);
            while (unread > 0)
            {
                this.deserialized.add(in.readChar());
                unread--;
            }
        }

        private Object readResolve() throws ObjectStreamException
        {
            return this.deserialized;
        }
    }
}
