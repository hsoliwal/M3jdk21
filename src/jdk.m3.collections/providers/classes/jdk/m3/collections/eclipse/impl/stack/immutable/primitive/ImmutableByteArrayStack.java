/*
 * Copyright (c) 2026 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.stack.immutable.primitive;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import jdk.m3.collections.eclipse.api.ByteIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.ByteToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.BytePredicate;
import jdk.m3.collections.eclipse.api.list.primitive.MutableByteList;
import jdk.m3.collections.eclipse.api.stack.ImmutableStack;
import jdk.m3.collections.eclipse.api.stack.primitive.ByteStack;
import jdk.m3.collections.eclipse.api.stack.primitive.ImmutableByteStack;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableByteStack;
import jdk.m3.collections.eclipse.impl.block.procedure.checked.primitive.CheckedByteProcedure;
import jdk.m3.collections.eclipse.impl.factory.Stacks;
import jdk.m3.collections.eclipse.impl.factory.primitive.ByteStacks;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.ByteArrayList;
import jdk.m3.collections.eclipse.impl.stack.mutable.primitive.ByteArrayStack;
import jdk.m3.collections.eclipse.impl.stack.primitive.AbstractByteStack;

/**
 * ImmutableByteArrayStack is the non-modifiable equivalent of {@link ByteArrayStack}.
 * This file was automatically generated from template file immutablePrimitiveArrayStack.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("serial")
final class ImmutableByteArrayStack extends AbstractByteStack
        implements ImmutableByteStack, Serializable
{
    private static final long serialVersionUID = 1L;
    private final ByteArrayList delegate;

    private ImmutableByteArrayStack(byte[] newElements)
    {
        this.checkOptimizedSize(newElements.length);
        this.delegate = new ByteArrayList(newElements);
    }

    private ImmutableByteArrayStack(ByteArrayList newElements)
    {
        this.checkOptimizedSize(newElements.size());
        this.delegate = newElements;
    }

    private void checkOptimizedSize(int length)
    {
        if (length <= 1)
        {
            throw new IllegalArgumentException("Use ByteStacks.immutable.with() to instantiate an optimized collection");
        }
    }

    public static ImmutableByteArrayStack newStack(ByteIterable iterable)
    {
        return new ImmutableByteArrayStack(iterable.toArray());
    }

    public static ImmutableByteArrayStack newStackWith(byte... elements)
    {
        byte[] newArray = new byte[elements.length];
        System.arraycopy(elements, 0, newArray, 0, elements.length);
        return new ImmutableByteArrayStack(newArray);
    }

    public static ImmutableByteArrayStack newStackFromTopToBottom(byte... items)
    {
        return new ImmutableByteArrayStack(ByteArrayList.newListWith(items).reverseThis());
    }

    public static ImmutableByteArrayStack newStackFromTopToBottom(ByteIterable items)
    {
        return new ImmutableByteArrayStack(ByteArrayList.newList(items).reverseThis());
    }

    @Override
    protected ByteArrayList getDelegate()
    {
        return this.delegate;
    }

    @Override
    public ImmutableByteStack push(byte item)
    {
        ByteArrayList newDelegate = ByteArrayList.newList(this.delegate);
        newDelegate.add(item);
        return new ImmutableByteArrayStack(newDelegate);
    }

    @Override
    public ImmutableByteStack pop()
    {
        ByteArrayList newDelegate = ByteArrayList.newList(this.delegate);
        newDelegate.removeAtIndex(this.delegate.size() - 1);
        return ByteStacks.immutable.with(newDelegate.toArray());
    }

    @Override
    public ImmutableByteStack pop(int count)
    {
        this.checkNegativeCount(count);
        if (count == 0)
        {
            return this;
        }
        this.checkSizeLessThanCount(count);
        ByteArrayList newDelegate = ByteArrayList.newList(this.delegate);
        while (count > 0)
        {
            newDelegate.removeAtIndex(newDelegate.size() - 1);
            count--;
        }
        return ByteStacks.immutable.with(newDelegate.toArray());
    }

    private void checkNegativeCount(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Count must be positive but was " + count);
        }
    }

    @Override
    public ImmutableByteStack select(BytePredicate predicate)
    {
        return ByteStacks.immutable.withAllReversed(this.delegate.toReversed().select(predicate));
    }

    @Override
    public ImmutableByteStack reject(BytePredicate predicate)
    {
        return ByteStacks.immutable.withAllReversed(this.delegate.toReversed().reject(predicate));
    }

    @Override
    public <V> ImmutableStack<V> collect(ByteToObjectFunction<? extends V> function)
    {
        return Stacks.immutable.withAllReversed(this.delegate.asReversed().collect(function));
    }

    @Override
    public ImmutableByteStack toImmutable()
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
    public MutableByteList toSortedList()
    {
        return ByteArrayList.newList(this).sortThis();
    }

    private Object writeReplace()
    {
        return new ImmutableByteStackSerializationProxy(this);
    }

    @Override
    public MutableByteStack toStack()
    {
        return ByteStacks.mutable.withAllReversed(this);
    }

    private static class ImmutableByteStackSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private ByteStack stack;

        public ImmutableByteStackSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableByteStackSerializationProxy(ByteStack stack)
        {
            this.stack = stack;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.stack.size());
            try
            {
                this.stack.forEach(new CheckedByteProcedure()
                {
                    @Override
                    public void safeValue(byte item) throws IOException
                    {
                        out.writeByte(item);
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
            ByteArrayList deserializedDelegate = new ByteArrayList(size);

            for (int i = 0; i < size; i++)
            {
                deserializedDelegate.add(in.readByte());
            }

            this.stack = ImmutableByteArrayStack.newStackFromTopToBottom(deserializedDelegate);
        }

        protected Object readResolve()
        {
            return this.stack;
        }
    }
}
