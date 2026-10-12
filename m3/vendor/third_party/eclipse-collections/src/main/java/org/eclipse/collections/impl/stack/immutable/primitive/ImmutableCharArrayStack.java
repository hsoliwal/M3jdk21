/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.stack.immutable.primitive;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.api.stack.ImmutableStack;
import org.eclipse.collections.api.stack.primitive.CharStack;
import org.eclipse.collections.api.stack.primitive.ImmutableCharStack;
import org.eclipse.collections.impl.block.procedure.checked.primitive.CheckedCharProcedure;
import org.eclipse.collections.impl.factory.Stacks;
import org.eclipse.collections.impl.factory.primitive.CharStacks;
import org.eclipse.collections.impl.list.mutable.primitive.CharArrayList;
import org.eclipse.collections.impl.stack.mutable.primitive.CharArrayStack;
import org.eclipse.collections.impl.stack.primitive.AbstractCharStack;

/**
 * ImmutableCharArrayStack is the non-modifiable equivalent of {@link CharArrayStack}.
 * This file was automatically generated from template file immutablePrimitiveArrayStack.stg.
 *
 * @since 4.0.
 */
final class ImmutableCharArrayStack extends AbstractCharStack
        implements ImmutableCharStack, Serializable
{
    private static final long serialVersionUID = 1L;
    private final CharArrayList delegate;

    private ImmutableCharArrayStack(char[] newElements)
    {
        this.checkOptimizedSize(newElements.length);
        this.delegate = new CharArrayList(newElements);
    }

    private ImmutableCharArrayStack(CharArrayList newElements)
    {
        this.checkOptimizedSize(newElements.size());
        this.delegate = newElements;
    }

    private void checkOptimizedSize(int length)
    {
        if (length <= 1)
        {
            throw new IllegalArgumentException("Use CharStacks.immutable.with() to instantiate an optimized collection");
        }
    }

    public static ImmutableCharArrayStack newStack(CharIterable iterable)
    {
        return new ImmutableCharArrayStack(iterable.toArray());
    }

    public static ImmutableCharArrayStack newStackWith(char... elements)
    {
        char[] newArray = new char[elements.length];
        System.arraycopy(elements, 0, newArray, 0, elements.length);
        return new ImmutableCharArrayStack(newArray);
    }

    public static ImmutableCharArrayStack newStackFromTopToBottom(char... items)
    {
        return new ImmutableCharArrayStack(CharArrayList.newListWith(items).reverseThis());
    }

    public static ImmutableCharArrayStack newStackFromTopToBottom(CharIterable items)
    {
        return new ImmutableCharArrayStack(CharArrayList.newList(items).reverseThis());
    }

    @Override
    protected CharArrayList getDelegate()
    {
        return this.delegate;
    }

    @Override
    public ImmutableCharStack push(char item)
    {
        CharArrayList newDelegate = CharArrayList.newList(this.delegate);
        newDelegate.add(item);
        return new ImmutableCharArrayStack(newDelegate);
    }

    @Override
    public ImmutableCharStack pop()
    {
        CharArrayList newDelegate = CharArrayList.newList(this.delegate);
        newDelegate.removeAtIndex(this.delegate.size() - 1);
        return CharStacks.immutable.with(newDelegate.toArray());
    }

    @Override
    public ImmutableCharStack pop(int count)
    {
        this.checkNegativeCount(count);
        if (count == 0)
        {
            return this;
        }
        this.checkSizeLessThanCount(count);
        CharArrayList newDelegate = CharArrayList.newList(this.delegate);
        while (count > 0)
        {
            newDelegate.removeAtIndex(newDelegate.size() - 1);
            count--;
        }
        return CharStacks.immutable.with(newDelegate.toArray());
    }

    private void checkNegativeCount(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Count must be positive but was " + count);
        }
    }

    @Override
    public ImmutableCharStack select(CharPredicate predicate)
    {
        return CharStacks.immutable.withAllReversed(this.delegate.toReversed().select(predicate));
    }

    @Override
    public ImmutableCharStack reject(CharPredicate predicate)
    {
        return CharStacks.immutable.withAllReversed(this.delegate.toReversed().reject(predicate));
    }

    @Override
    public <V> ImmutableStack<V> collect(CharToObjectFunction<? extends V> function)
    {
        return Stacks.immutable.withAllReversed(this.delegate.asReversed().collect(function));
    }

    @Override
    public ImmutableCharStack toImmutable()
    {
        return this;
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
    public MutableCharList toSortedList()
    {
        return CharArrayList.newList(this).sortThis();
    }

    private Object writeReplace()
    {
        return new ImmutableCharStackSerializationProxy(this);
    }

    private static class ImmutableCharStackSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private CharStack stack;

        public ImmutableCharStackSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableCharStackSerializationProxy(CharStack stack)
        {
            this.stack = stack;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.stack.size());
            try
            {
                this.stack.forEach(new CheckedCharProcedure()
                {
                    @Override
                    public void safeValue(char item) throws IOException
                    {
                        out.writeChar(item);
                    }
                });
            }
            catch (RuntimeException e)
            {
                if (e.getCause() instanceof IOException)
                {
                    throw (IOException) e.getCause();
                }
                throw e;
            }
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
        {
            int size = in.readInt();
            CharArrayList deserializedDelegate = new CharArrayList(size);

            for (int i = 0; i < size; i++)
            {
                deserializedDelegate.add(in.readChar());
            }

            this.stack = ImmutableCharArrayStack.newStackFromTopToBottom(deserializedDelegate);
        }

        protected Object readResolve()
        {
            return this.stack;
        }
    }
}
