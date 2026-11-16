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

import jdk.m3.collections.eclipse.api.ShortIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ShortPredicate;
import jdk.m3.collections.eclipse.api.list.primitive.MutableShortList;
import jdk.m3.collections.eclipse.api.stack.ImmutableStack;
import jdk.m3.collections.eclipse.api.stack.primitive.ShortStack;
import jdk.m3.collections.eclipse.api.stack.primitive.ImmutableShortStack;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableShortStack;
import jdk.m3.collections.eclipse.impl.block.procedure.checked.primitive.CheckedShortProcedure;
import jdk.m3.collections.eclipse.impl.factory.Stacks;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortStacks;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.ShortArrayList;
import jdk.m3.collections.eclipse.impl.stack.mutable.primitive.ShortArrayStack;
import jdk.m3.collections.eclipse.impl.stack.primitive.AbstractShortStack;

/**
 * ImmutableShortArrayStack is the non-modifiable equivalent of {@link ShortArrayStack}.
 * This file was automatically generated from template file immutablePrimitiveArrayStack.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("serial")
final class ImmutableShortArrayStack extends AbstractShortStack
        implements ImmutableShortStack, Serializable
{
    private static final long serialVersionUID = 1L;
    private final ShortArrayList delegate;

    private ImmutableShortArrayStack(short[] newElements)
    {
        this.checkOptimizedSize(newElements.length);
        this.delegate = new ShortArrayList(newElements);
    }

    private ImmutableShortArrayStack(ShortArrayList newElements)
    {
        this.checkOptimizedSize(newElements.size());
        this.delegate = newElements;
    }

    private void checkOptimizedSize(int length)
    {
        if (length <= 1)
        {
            throw new IllegalArgumentException("Use ShortStacks.immutable.with() to instantiate an optimized collection");
        }
    }

    public static ImmutableShortArrayStack newStack(ShortIterable iterable)
    {
        return new ImmutableShortArrayStack(iterable.toArray());
    }

    public static ImmutableShortArrayStack newStackWith(short... elements)
    {
        short[] newArray = new short[elements.length];
        System.arraycopy(elements, 0, newArray, 0, elements.length);
        return new ImmutableShortArrayStack(newArray);
    }

    public static ImmutableShortArrayStack newStackFromTopToBottom(short... items)
    {
        return new ImmutableShortArrayStack(ShortArrayList.newListWith(items).reverseThis());
    }

    public static ImmutableShortArrayStack newStackFromTopToBottom(ShortIterable items)
    {
        return new ImmutableShortArrayStack(ShortArrayList.newList(items).reverseThis());
    }

    @Override
    protected ShortArrayList getDelegate()
    {
        return this.delegate;
    }

    @Override
    public ImmutableShortStack push(short item)
    {
        ShortArrayList newDelegate = ShortArrayList.newList(this.delegate);
        newDelegate.add(item);
        return new ImmutableShortArrayStack(newDelegate);
    }

    @Override
    public ImmutableShortStack pop()
    {
        ShortArrayList newDelegate = ShortArrayList.newList(this.delegate);
        newDelegate.removeAtIndex(this.delegate.size() - 1);
        return ShortStacks.immutable.with(newDelegate.toArray());
    }

    @Override
    public ImmutableShortStack pop(int count)
    {
        this.checkNegativeCount(count);
        if (count == 0)
        {
            return this;
        }
        this.checkSizeLessThanCount(count);
        ShortArrayList newDelegate = ShortArrayList.newList(this.delegate);
        while (count > 0)
        {
            newDelegate.removeAtIndex(newDelegate.size() - 1);
            count--;
        }
        return ShortStacks.immutable.with(newDelegate.toArray());
    }

    private void checkNegativeCount(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Count must be positive but was " + count);
        }
    }

    @Override
    public ImmutableShortStack select(ShortPredicate predicate)
    {
        return ShortStacks.immutable.withAllReversed(this.delegate.toReversed().select(predicate));
    }

    @Override
    public ImmutableShortStack reject(ShortPredicate predicate)
    {
        return ShortStacks.immutable.withAllReversed(this.delegate.toReversed().reject(predicate));
    }

    @Override
    public <V> ImmutableStack<V> collect(ShortToObjectFunction<? extends V> function)
    {
        return Stacks.immutable.withAllReversed(this.delegate.asReversed().collect(function));
    }

    @Override
    public ImmutableShortStack toImmutable()
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
    public MutableShortList toSortedList()
    {
        return ShortArrayList.newList(this).sortThis();
    }

    private Object writeReplace()
    {
        return new ImmutableShortStackSerializationProxy(this);
    }

    @Override
    public MutableShortStack toStack()
    {
        return ShortStacks.mutable.withAllReversed(this);
    }

    private static class ImmutableShortStackSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private ShortStack stack;

        public ImmutableShortStackSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableShortStackSerializationProxy(ShortStack stack)
        {
            this.stack = stack;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.stack.size());
            try
            {
                this.stack.forEach(new CheckedShortProcedure()
                {
                    @Override
                    public void safeValue(short item) throws IOException
                    {
                        out.writeShort(item);
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
            ShortArrayList deserializedDelegate = new ShortArrayList(size);

            for (int i = 0; i < size; i++)
            {
                deserializedDelegate.add(in.readShort());
            }

            this.stack = ImmutableShortArrayStack.newStackFromTopToBottom(deserializedDelegate);
        }

        protected Object readResolve()
        {
            return this.stack;
        }
    }
}
