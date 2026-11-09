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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.stack.ImmutableStack;
import org.eclipse.collections.api.stack.primitive.IntStack;
import org.eclipse.collections.api.stack.primitive.ImmutableIntStack;
import org.eclipse.collections.impl.block.procedure.checked.primitive.CheckedIntProcedure;
import org.eclipse.collections.impl.factory.Stacks;
import org.eclipse.collections.impl.factory.primitive.IntStacks;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.stack.mutable.primitive.IntArrayStack;
import org.eclipse.collections.impl.stack.primitive.AbstractIntStack;

/**
 * ImmutableIntArrayStack is the non-modifiable equivalent of {@link IntArrayStack}.
 * This file was automatically generated from template file immutablePrimitiveArrayStack.stg.
 *
 * @since 4.0.
 */
final class ImmutableIntArrayStack extends AbstractIntStack
        implements ImmutableIntStack, Serializable
{
    private static final long serialVersionUID = 1L;
    private final IntArrayList delegate;

    private ImmutableIntArrayStack(int[] newElements)
    {
        this.checkOptimizedSize(newElements.length);
        this.delegate = new IntArrayList(newElements);
    }

    private ImmutableIntArrayStack(IntArrayList newElements)
    {
        this.checkOptimizedSize(newElements.size());
        this.delegate = newElements;
    }

    private void checkOptimizedSize(int length)
    {
        if (length <= 1)
        {
            throw new IllegalArgumentException("Use IntStacks.immutable.with() to instantiate an optimized collection");
        }
    }

    public static ImmutableIntArrayStack newStack(IntIterable iterable)
    {
        return new ImmutableIntArrayStack(iterable.toArray());
    }

    public static ImmutableIntArrayStack newStackWith(int... elements)
    {
        int[] newArray = new int[elements.length];
        System.arraycopy(elements, 0, newArray, 0, elements.length);
        return new ImmutableIntArrayStack(newArray);
    }

    public static ImmutableIntArrayStack newStackFromTopToBottom(int... items)
    {
        return new ImmutableIntArrayStack(IntArrayList.newListWith(items).reverseThis());
    }

    public static ImmutableIntArrayStack newStackFromTopToBottom(IntIterable items)
    {
        return new ImmutableIntArrayStack(IntArrayList.newList(items).reverseThis());
    }

    @Override
    protected IntArrayList getDelegate()
    {
        return this.delegate;
    }

    @Override
    public ImmutableIntStack push(int item)
    {
        IntArrayList newDelegate = IntArrayList.newList(this.delegate);
        newDelegate.add(item);
        return new ImmutableIntArrayStack(newDelegate);
    }

    @Override
    public ImmutableIntStack pop()
    {
        IntArrayList newDelegate = IntArrayList.newList(this.delegate);
        newDelegate.removeAtIndex(this.delegate.size() - 1);
        return IntStacks.immutable.with(newDelegate.toArray());
    }

    @Override
    public ImmutableIntStack pop(int count)
    {
        this.checkNegativeCount(count);
        if (count == 0)
        {
            return this;
        }
        this.checkSizeLessThanCount(count);
        IntArrayList newDelegate = IntArrayList.newList(this.delegate);
        while (count > 0)
        {
            newDelegate.removeAtIndex(newDelegate.size() - 1);
            count--;
        }
        return IntStacks.immutable.with(newDelegate.toArray());
    }

    private void checkNegativeCount(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Count must be positive but was " + count);
        }
    }

    @Override
    public ImmutableIntStack select(IntPredicate predicate)
    {
        return IntStacks.immutable.withAllReversed(this.delegate.toReversed().select(predicate));
    }

    @Override
    public ImmutableIntStack reject(IntPredicate predicate)
    {
        return IntStacks.immutable.withAllReversed(this.delegate.toReversed().reject(predicate));
    }

    @Override
    public <V> ImmutableStack<V> collect(IntToObjectFunction<? extends V> function)
    {
        return Stacks.immutable.withAllReversed(this.delegate.asReversed().collect(function));
    }

    @Override
    public ImmutableIntStack toImmutable()
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
    public MutableIntList toSortedList()
    {
        return IntArrayList.newList(this).sortThis();
    }

    private Object writeReplace()
    {
        return new ImmutableIntStackSerializationProxy(this);
    }

    private static class ImmutableIntStackSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private IntStack stack;

        public ImmutableIntStackSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableIntStackSerializationProxy(IntStack stack)
        {
            this.stack = stack;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.stack.size());
            try
            {
                this.stack.forEach(new CheckedIntProcedure()
                {
                    @Override
                    public void safeValue(int item) throws IOException
                    {
                        out.writeInt(item);
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
            IntArrayList deserializedDelegate = new IntArrayList(size);

            for (int i = 0; i < size; i++)
            {
                deserializedDelegate.add(in.readInt());
            }

            this.stack = ImmutableIntArrayStack.newStackFromTopToBottom(deserializedDelegate);
        }

        protected Object readResolve()
        {
            return this.stack;
        }
    }
}
