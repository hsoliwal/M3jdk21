/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.stack.primitive;

import java.util.EmptyStackException;

import org.eclipse.collections.api.LazyBooleanIterable;
import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableBooleanBag;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.BooleanIntProcedure;
import org.eclipse.collections.api.iterator.BooleanIterator;
import org.eclipse.collections.api.list.primitive.BooleanList;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;
import org.eclipse.collections.api.stack.primitive.BooleanStack;
import org.eclipse.collections.impl.bag.mutable.primitive.BooleanHashBag;
import org.eclipse.collections.impl.iterator.UnmodifiableBooleanIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyBooleanIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.BooleanHashSet;

/**
 * This file was automatically generated from template file abstractPrimitiveStack.stg.
 */
public abstract class AbstractBooleanStack implements BooleanStack
{
    protected abstract BooleanArrayList getDelegate();

    protected void checkEmptyStack()
    {
        if (this.getDelegate().isEmpty())
        {
            throw new EmptyStackException();
        }
    }

    @Override
    public boolean peek()
    {
        this.checkEmptyStack();
        return this.getDelegate().getLast();
    }

    @Override
    public BooleanList peek(int count)
    {
        this.checkPositiveValueForCount(count);
        this.checkSizeLessThanCount(count);
        if (count == 0)
        {
            return new BooleanArrayList(0);
        }
        MutableBooleanList subList = new BooleanArrayList(count);
        int index = this.getDelegate().size() - 1;
        for (int i = 0; i < count; i++)
        {
            subList.add(this.getDelegate().get(index - i));
        }
        return subList;
    }

    @Override
    public boolean peekAt(int index)
    {
        this.rangeCheck(index);
        return this.getDelegate().get(this.getDelegate().size() - 1 - index);
    }

    protected void rangeCheck(int index)
    {
        if (index < 0 || index > this.getDelegate().size() - 1)
        {
            throw new IllegalArgumentException("Index " + index + " out of range.Should be between 0 and " + (this.getDelegate().size() - 1));
        }
    }

    protected void checkPositiveValueForCount(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Count must be positive but was " + count);
        }
    }

    protected void checkSizeLessThanCount(int count)
    {
        if (this.getDelegate().size() < count)
        {
            throw new IllegalArgumentException("Count must be less than size: Count = " + count + " Size = " + this.getDelegate().size());
        }
    }

    @Override
    public BooleanIterator booleanIterator()
    {
        return new UnmodifiableBooleanIterator(this.getDelegate().asReversed().booleanIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(BooleanProcedure procedure)
    {
        this.getDelegate().asReversed().forEach(procedure);
    }

    @Override
    public int count(BooleanPredicate predicate)
    {
        return this.getDelegate().asReversed().count(predicate);
    }

    @Override
    public boolean anySatisfy(BooleanPredicate predicate)
    {
        return this.getDelegate().asReversed().anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(BooleanPredicate predicate)
    {
        return this.getDelegate().asReversed().allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(BooleanPredicate predicate)
    {
        return this.getDelegate().asReversed().noneSatisfy(predicate);
    }

    @Override
    public boolean detectIfNone(BooleanPredicate predicate, boolean ifNone)
    {
        return this.getDelegate().asReversed().detectIfNone(predicate, ifNone);
    }

    @Override
    public boolean[] toArray()
    {
        return this.getDelegate().asReversed().toArray();
    }

    @Override
    public boolean[] toArray(boolean[] target)
    {
        return this.getDelegate().asReversed().toArray(target);
    }

    @Override
    public boolean contains(boolean value)
    {
        return this.getDelegate().asReversed().contains(value);
    }

    @Override
    public boolean containsAll(boolean... source)
    {
        return this.getDelegate().asReversed().containsAll(source);
    }

    @Override
    public boolean containsAll(BooleanIterable source)
    {
        return this.getDelegate().asReversed().containsAll(source);
    }

    @Override
    public MutableBooleanList toList()
    {
        return BooleanArrayList.newList(this);
    }

    @Override
    public MutableBooleanSet toSet()
    {
        return BooleanHashSet.newSet(this);
    }

    @Override
    public MutableBooleanBag toBag()
    {
        return BooleanHashBag.newBag(this);
    }

    @Override
    public <V> V injectInto(V injectedValue, ObjectBooleanToObjectFunction<? super V, ? extends V> function)
    {
        return this.getDelegate().toReversed().injectInto(injectedValue, function);
    }

    @Override
    public LazyBooleanIterable asLazy()
    {
        return new LazyBooleanIterableAdapter(this);
    }

    @Override
    public int size()
    {
        return this.getDelegate().size();
    }

    @Override
    public boolean equals(Object otherStack)
    {
        if (otherStack == this)
        {
            return true;
        }
        if (!(otherStack instanceof BooleanStack))
        {
            return false;
        }
        BooleanStack stack = (BooleanStack) otherStack;
        if (this.size() != stack.size())
        {
            return false;
        }
        for (int i = 0; i < this.size(); i++)
        {
            if (this.peekAt(i) != stack.peekAt(i))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hashCode = 1;
        BooleanIterable iterable = this.getDelegate().asReversed();
        BooleanIterator iterator = iterable.booleanIterator();
        while (iterator.hasNext())
        {
            boolean item = iterator.next();
            hashCode = 31 * hashCode + (item ? 1231 : 1237);
        }
        return hashCode;
    }

    @Override
    public String toString()
    {
        return this.getDelegate().asReversed().toString();
    }

    @Override
    public String makeString()
    {
        return this.getDelegate().asReversed().makeString();
    }

    @Override
    public String makeString(String separator)
    {
        return this.getDelegate().asReversed().makeString(separator);
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        return this.getDelegate().asReversed().makeString(start, separator, end);
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.getDelegate().asReversed().appendString(appendable);
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.getDelegate().asReversed().appendString(appendable, separator);
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        this.getDelegate().asReversed().appendString(appendable, start, separator, end);
    }

    @Override
    public boolean getFirst()
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".getFirst() not implemented yet");
    }

    @Override
    public int indexOf(boolean value)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".indexOf() not implemented yet");
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectBooleanIntToObjectFunction<? super T, ? extends T> function)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".injectIntoWithIndex() not implemented yet");
    }

    @Override
    public void forEachWithIndex(BooleanIntProcedure procedure)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".forEachWithIndex() not implemented yet");
    }

    @Override
    public RichIterable<BooleanIterable> chunk(int size)
    {
        return this.getDelegate().asReversed().chunk(size);
    }
}
