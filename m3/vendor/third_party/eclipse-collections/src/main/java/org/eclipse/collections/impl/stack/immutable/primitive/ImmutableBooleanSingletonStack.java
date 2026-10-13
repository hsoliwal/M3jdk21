/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.stack.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.LazyBooleanIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableBooleanBag;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.iterator.BooleanIterator;
import org.eclipse.collections.api.list.primitive.BooleanList;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;
import org.eclipse.collections.api.stack.ImmutableStack;
import org.eclipse.collections.api.stack.primitive.BooleanStack;
import org.eclipse.collections.api.stack.primitive.ImmutableBooleanStack;
import org.eclipse.collections.impl.bag.mutable.primitive.BooleanHashBag;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Stacks;
import org.eclipse.collections.impl.factory.primitive.BooleanStacks;
import org.eclipse.collections.impl.iterator.SingletonBooleanIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyBooleanIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.BooleanHashSet;

/**
 * ImmutableBooleanSingletonStack is an optimization for {@link ImmutableBooleanStack} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonStack.stg.
 */
final class ImmutableBooleanSingletonStack implements ImmutableBooleanStack, Serializable
{
    private static final long serialVersionUID = 1L;
    private final boolean element1;

    ImmutableBooleanSingletonStack(boolean element)
    {
        this.element1 = element;
    }

    @Override
    public BooleanIterator booleanIterator()
    {
        return new SingletonBooleanIterator(element1);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(BooleanProcedure procedure)
    {
        procedure.value(this.element1);
    }

    @Override
    public int count(BooleanPredicate predicate)
    {
        return predicate.accept(this.element1) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(BooleanPredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean allSatisfy(BooleanPredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean noneSatisfy(BooleanPredicate predicate)
    {
        return !predicate.accept(this.element1);
    }

    @Override
    public boolean peek()
    {
        return this.element1;
    }

    @Override
    public BooleanList peek(int count)
    {
        this.checkNegativeCount(count);
        if (count == 0)
        {
            return new BooleanArrayList(0);
        }
        if (count == 1)
        {
            return BooleanArrayList.newListWith(this.element1);
        }
        throw new IllegalArgumentException("Count must be less than or equal to size: Count = " + count + " Size = 1");
    }

    @Override
    public boolean peekAt(int index)
    {
        this.checkNegativeCount(index);
        if (index == 0)
        {
            return this.element1;
        }
        throw new IllegalArgumentException("Index must be less than size: Index = " + index + " Size = 1");
    }

    @Override
    public ImmutableBooleanStack select(BooleanPredicate predicate)
    {
        return predicate.accept(this.element1) ? BooleanStacks.immutable.with(this.element1)
                : BooleanStacks.immutable.with();
    }

    @Override
    public ImmutableBooleanStack reject(BooleanPredicate predicate)
    {
        return predicate.accept(this.element1) ? BooleanStacks.immutable.with()
                : BooleanStacks.immutable.with(this.element1);
    }

    @Override
    public boolean detectIfNone(BooleanPredicate predicate, boolean ifNone)
    {
        return predicate.accept(this.element1) ? this.element1 : ifNone;
    }

    @Override
    public <V> ImmutableStack<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        return Stacks.immutable.with(function.valueOf(this.element1));
    }

    @Override
    public boolean[] toArray()
    {
        return new boolean[]{this.element1};
    }

    @Override
    public boolean[] toArray(boolean[] target)
    {
        if (target.length < 1)
        {
            target = new boolean[]{this.element1};
        }
        else
        {
            target[0] = this.element1;
        }
        return target;
    }

    @Override
    public boolean contains(boolean value)
    {
        return this.element1 == value;
    }

    @Override
    public boolean containsAll(boolean... source)
    {
        for (boolean value : source)
        {
            if (this.element1 != value)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(BooleanIterable source)
    {
        for (BooleanIterator iterator = source.booleanIterator(); iterator.hasNext(); )
        {
            if (this.element1 != iterator.next())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableBooleanList toList()
    {
        return BooleanArrayList.newListWith(this.element1);
    }

    @Override
    public MutableBooleanSet toSet()
    {
        return BooleanHashSet.newSetWith(this.element1);
    }

    @Override
    public MutableBooleanBag toBag()
    {
        return BooleanHashBag.newBagWith(this.element1);
    }

    @Override
    public LazyBooleanIterable asLazy()
    {
        return new LazyBooleanIterableAdapter(this);
    }

    @Override
    public ImmutableBooleanStack toImmutable()
    {
        return this;
    }

    @Override
    public ImmutableBooleanStack push(boolean element)
    {
        return BooleanStacks.immutable.with(this.element1, element);
    }

    @Override
    public ImmutableBooleanStack pop()
    {
        return BooleanStacks.immutable.with();
    }

    @Override
    public ImmutableBooleanStack pop(int count)
    {
        this.checkNegativeCount(count);
        if (count == 0)
        {
            return this;
        }
        if (count == 1)
        {
            return BooleanStacks.immutable.with();
        }
        throw new IllegalArgumentException("Count must be less than size: Count = " + count + " Size = 1");
    }

    private void checkNegativeCount(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Count must be positive but was " + count);
        }
    }

    @Override
    public int size()
    {
        return 1;
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public boolean notEmpty()
    {
        return true;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectBooleanToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1);
    }

    @Override
    public RichIterable<BooleanIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }

        return Lists.immutable.with(this);
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
        if (stack.size() != 1)
        {
            return false;
        }
        return this.element1 == stack.peek();
    }

    @Override
    public int hashCode()
    {
        return 31 + (this.element1 ? 1231 : 1237);
    }

    @Override
    public String toString()
    {
        return "[" + this.element1 + ']';
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
            appendable.append(String.valueOf(this.element1));
            appendable.append(end);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
