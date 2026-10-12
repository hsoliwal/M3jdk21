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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.stack.ImmutableStack;
import org.eclipse.collections.api.stack.primitive.DoubleStack;
import org.eclipse.collections.api.stack.primitive.ImmutableDoubleStack;
import org.eclipse.collections.impl.block.procedure.checked.primitive.CheckedDoubleProcedure;
import org.eclipse.collections.impl.factory.Stacks;
import org.eclipse.collections.impl.factory.primitive.DoubleStacks;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.stack.mutable.primitive.DoubleArrayStack;
import org.eclipse.collections.impl.stack.primitive.AbstractDoubleStack;

/**
 * ImmutableDoubleArrayStack is the non-modifiable equivalent of {@link DoubleArrayStack}.
 * This file was automatically generated from template file immutablePrimitiveArrayStack.stg.
 *
 * @since 4.0.
 */
final class ImmutableDoubleArrayStack extends AbstractDoubleStack
        implements ImmutableDoubleStack, Serializable
{
    private static final long serialVersionUID = 1L;
    private final DoubleArrayList delegate;

    private ImmutableDoubleArrayStack(double[] newElements)
    {
        this.checkOptimizedSize(newElements.length);
        this.delegate = new DoubleArrayList(newElements);
    }

    private ImmutableDoubleArrayStack(DoubleArrayList newElements)
    {
        this.checkOptimizedSize(newElements.size());
        this.delegate = newElements;
    }

    private void checkOptimizedSize(int length)
    {
        if (length <= 1)
        {
            throw new IllegalArgumentException("Use DoubleStacks.immutable.with() to instantiate an optimized collection");
        }
    }

    public static ImmutableDoubleArrayStack newStack(DoubleIterable iterable)
    {
        return new ImmutableDoubleArrayStack(iterable.toArray());
    }

    public static ImmutableDoubleArrayStack newStackWith(double... elements)
    {
        double[] newArray = new double[elements.length];
        System.arraycopy(elements, 0, newArray, 0, elements.length);
        return new ImmutableDoubleArrayStack(newArray);
    }

    public static ImmutableDoubleArrayStack newStackFromTopToBottom(double... items)
    {
        return new ImmutableDoubleArrayStack(DoubleArrayList.newListWith(items).reverseThis());
    }

    public static ImmutableDoubleArrayStack newStackFromTopToBottom(DoubleIterable items)
    {
        return new ImmutableDoubleArrayStack(DoubleArrayList.newList(items).reverseThis());
    }

    @Override
    protected DoubleArrayList getDelegate()
    {
        return this.delegate;
    }

    @Override
    public ImmutableDoubleStack push(double item)
    {
        DoubleArrayList newDelegate = DoubleArrayList.newList(this.delegate);
        newDelegate.add(item);
        return new ImmutableDoubleArrayStack(newDelegate);
    }

    @Override
    public ImmutableDoubleStack pop()
    {
        DoubleArrayList newDelegate = DoubleArrayList.newList(this.delegate);
        newDelegate.removeAtIndex(this.delegate.size() - 1);
        return DoubleStacks.immutable.with(newDelegate.toArray());
    }

    @Override
    public ImmutableDoubleStack pop(int count)
    {
        this.checkNegativeCount(count);
        if (count == 0)
        {
            return this;
        }
        this.checkSizeLessThanCount(count);
        DoubleArrayList newDelegate = DoubleArrayList.newList(this.delegate);
        while (count > 0)
        {
            newDelegate.removeAtIndex(newDelegate.size() - 1);
            count--;
        }
        return DoubleStacks.immutable.with(newDelegate.toArray());
    }

    private void checkNegativeCount(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Count must be positive but was " + count);
        }
    }

    @Override
    public ImmutableDoubleStack select(DoublePredicate predicate)
    {
        return DoubleStacks.immutable.withAllReversed(this.delegate.toReversed().select(predicate));
    }

    @Override
    public ImmutableDoubleStack reject(DoublePredicate predicate)
    {
        return DoubleStacks.immutable.withAllReversed(this.delegate.toReversed().reject(predicate));
    }

    @Override
    public <V> ImmutableStack<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        return Stacks.immutable.withAllReversed(this.delegate.asReversed().collect(function));
    }

    @Override
    public ImmutableDoubleStack toImmutable()
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
    public MutableDoubleList toSortedList()
    {
        return DoubleArrayList.newList(this).sortThis();
    }

    private Object writeReplace()
    {
        return new ImmutableDoubleStackSerializationProxy(this);
    }

    private static class ImmutableDoubleStackSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private DoubleStack stack;

        public ImmutableDoubleStackSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableDoubleStackSerializationProxy(DoubleStack stack)
        {
            this.stack = stack;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.stack.size());
            try
            {
                this.stack.forEach(new CheckedDoubleProcedure()
                {
                    @Override
                    public void safeValue(double item) throws IOException
                    {
                        out.writeDouble(item);
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
            DoubleArrayList deserializedDelegate = new DoubleArrayList(size);

            for (int i = 0; i < size; i++)
            {
                deserializedDelegate.add(in.readDouble());
            }

            this.stack = ImmutableDoubleArrayStack.newStackFromTopToBottom(deserializedDelegate);
        }

        protected Object readResolve()
        {
            return this.stack;
        }
    }
}
