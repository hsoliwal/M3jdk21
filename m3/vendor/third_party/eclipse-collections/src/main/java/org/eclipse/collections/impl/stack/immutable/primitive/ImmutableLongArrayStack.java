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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.stack.ImmutableStack;
import org.eclipse.collections.api.stack.primitive.LongStack;
import org.eclipse.collections.api.stack.primitive.ImmutableLongStack;
import org.eclipse.collections.impl.block.procedure.checked.primitive.CheckedLongProcedure;
import org.eclipse.collections.impl.factory.Stacks;
import org.eclipse.collections.impl.factory.primitive.LongStacks;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.stack.mutable.primitive.LongArrayStack;
import org.eclipse.collections.impl.stack.primitive.AbstractLongStack;

/**
 * ImmutableLongArrayStack is the non-modifiable equivalent of {@link LongArrayStack}.
 * This file was automatically generated from template file immutablePrimitiveArrayStack.stg.
 *
 * @since 4.0.
 */
final class ImmutableLongArrayStack extends AbstractLongStack
        implements ImmutableLongStack, Serializable
{
    private static final long serialVersionUID = 1L;
    private final LongArrayList delegate;

    private ImmutableLongArrayStack(long[] newElements)
    {
        this.checkOptimizedSize(newElements.length);
        this.delegate = new LongArrayList(newElements);
    }

    private ImmutableLongArrayStack(LongArrayList newElements)
    {
        this.checkOptimizedSize(newElements.size());
        this.delegate = newElements;
    }

    private void checkOptimizedSize(int length)
    {
        if (length <= 1)
        {
            throw new IllegalArgumentException("Use LongStacks.immutable.with() to instantiate an optimized collection");
        }
    }

    public static ImmutableLongArrayStack newStack(LongIterable iterable)
    {
        return new ImmutableLongArrayStack(iterable.toArray());
    }

    public static ImmutableLongArrayStack newStackWith(long... elements)
    {
        long[] newArray = new long[elements.length];
        System.arraycopy(elements, 0, newArray, 0, elements.length);
        return new ImmutableLongArrayStack(newArray);
    }

    public static ImmutableLongArrayStack newStackFromTopToBottom(long... items)
    {
        return new ImmutableLongArrayStack(LongArrayList.newListWith(items).reverseThis());
    }

    public static ImmutableLongArrayStack newStackFromTopToBottom(LongIterable items)
    {
        return new ImmutableLongArrayStack(LongArrayList.newList(items).reverseThis());
    }

    @Override
    protected LongArrayList getDelegate()
    {
        return this.delegate;
    }

    @Override
    public ImmutableLongStack push(long item)
    {
        LongArrayList newDelegate = LongArrayList.newList(this.delegate);
        newDelegate.add(item);
        return new ImmutableLongArrayStack(newDelegate);
    }

    @Override
    public ImmutableLongStack pop()
    {
        LongArrayList newDelegate = LongArrayList.newList(this.delegate);
        newDelegate.removeAtIndex(this.delegate.size() - 1);
        return LongStacks.immutable.with(newDelegate.toArray());
    }

    @Override
    public ImmutableLongStack pop(int count)
    {
        this.checkNegativeCount(count);
        if (count == 0)
        {
            return this;
        }
        this.checkSizeLessThanCount(count);
        LongArrayList newDelegate = LongArrayList.newList(this.delegate);
        while (count > 0)
        {
            newDelegate.removeAtIndex(newDelegate.size() - 1);
            count--;
        }
        return LongStacks.immutable.with(newDelegate.toArray());
    }

    private void checkNegativeCount(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Count must be positive but was " + count);
        }
    }

    @Override
    public ImmutableLongStack select(LongPredicate predicate)
    {
        return LongStacks.immutable.withAllReversed(this.delegate.toReversed().select(predicate));
    }

    @Override
    public ImmutableLongStack reject(LongPredicate predicate)
    {
        return LongStacks.immutable.withAllReversed(this.delegate.toReversed().reject(predicate));
    }

    @Override
    public <V> ImmutableStack<V> collect(LongToObjectFunction<? extends V> function)
    {
        return Stacks.immutable.withAllReversed(this.delegate.asReversed().collect(function));
    }

    @Override
    public ImmutableLongStack toImmutable()
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
    public MutableLongList toSortedList()
    {
        return LongArrayList.newList(this).sortThis();
    }

    private Object writeReplace()
    {
        return new ImmutableLongStackSerializationProxy(this);
    }

    private static class ImmutableLongStackSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private LongStack stack;

        public ImmutableLongStackSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableLongStackSerializationProxy(LongStack stack)
        {
            this.stack = stack;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.stack.size());
            try
            {
                this.stack.forEach(new CheckedLongProcedure()
                {
                    @Override
                    public void safeValue(long item) throws IOException
                    {
                        out.writeLong(item);
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
            LongArrayList deserializedDelegate = new LongArrayList(size);

            for (int i = 0; i < size; i++)
            {
                deserializedDelegate.add(in.readLong());
            }

            this.stack = ImmutableLongArrayStack.newStackFromTopToBottom(deserializedDelegate);
        }

        protected Object readResolve()
        {
            return this.stack;
        }
    }
}
