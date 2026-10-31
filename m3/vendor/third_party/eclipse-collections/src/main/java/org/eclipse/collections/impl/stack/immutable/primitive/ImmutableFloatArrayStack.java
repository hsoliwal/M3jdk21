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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.stack.ImmutableStack;
import org.eclipse.collections.api.stack.primitive.FloatStack;
import org.eclipse.collections.api.stack.primitive.ImmutableFloatStack;
import org.eclipse.collections.impl.block.procedure.checked.primitive.CheckedFloatProcedure;
import org.eclipse.collections.impl.factory.Stacks;
import org.eclipse.collections.impl.factory.primitive.FloatStacks;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.eclipse.collections.impl.stack.mutable.primitive.FloatArrayStack;
import org.eclipse.collections.impl.stack.primitive.AbstractFloatStack;

/**
 * ImmutableFloatArrayStack is the non-modifiable equivalent of {@link FloatArrayStack}.
 * This file was automatically generated from template file immutablePrimitiveArrayStack.stg.
 *
 * @since 4.0.
 */
final class ImmutableFloatArrayStack extends AbstractFloatStack
        implements ImmutableFloatStack, Serializable
{
    private static final long serialVersionUID = 1L;
    private final FloatArrayList delegate;

    private ImmutableFloatArrayStack(float[] newElements)
    {
        this.checkOptimizedSize(newElements.length);
        this.delegate = new FloatArrayList(newElements);
    }

    private ImmutableFloatArrayStack(FloatArrayList newElements)
    {
        this.checkOptimizedSize(newElements.size());
        this.delegate = newElements;
    }

    private void checkOptimizedSize(int length)
    {
        if (length <= 1)
        {
            throw new IllegalArgumentException("Use FloatStacks.immutable.with() to instantiate an optimized collection");
        }
    }

    public static ImmutableFloatArrayStack newStack(FloatIterable iterable)
    {
        return new ImmutableFloatArrayStack(iterable.toArray());
    }

    public static ImmutableFloatArrayStack newStackWith(float... elements)
    {
        float[] newArray = new float[elements.length];
        System.arraycopy(elements, 0, newArray, 0, elements.length);
        return new ImmutableFloatArrayStack(newArray);
    }

    public static ImmutableFloatArrayStack newStackFromTopToBottom(float... items)
    {
        return new ImmutableFloatArrayStack(FloatArrayList.newListWith(items).reverseThis());
    }

    public static ImmutableFloatArrayStack newStackFromTopToBottom(FloatIterable items)
    {
        return new ImmutableFloatArrayStack(FloatArrayList.newList(items).reverseThis());
    }

    @Override
    protected FloatArrayList getDelegate()
    {
        return this.delegate;
    }

    @Override
    public ImmutableFloatStack push(float item)
    {
        FloatArrayList newDelegate = FloatArrayList.newList(this.delegate);
        newDelegate.add(item);
        return new ImmutableFloatArrayStack(newDelegate);
    }

    @Override
    public ImmutableFloatStack pop()
    {
        FloatArrayList newDelegate = FloatArrayList.newList(this.delegate);
        newDelegate.removeAtIndex(this.delegate.size() - 1);
        return FloatStacks.immutable.with(newDelegate.toArray());
    }

    @Override
    public ImmutableFloatStack pop(int count)
    {
        this.checkNegativeCount(count);
        if (count == 0)
        {
            return this;
        }
        this.checkSizeLessThanCount(count);
        FloatArrayList newDelegate = FloatArrayList.newList(this.delegate);
        while (count > 0)
        {
            newDelegate.removeAtIndex(newDelegate.size() - 1);
            count--;
        }
        return FloatStacks.immutable.with(newDelegate.toArray());
    }

    private void checkNegativeCount(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Count must be positive but was " + count);
        }
    }

    @Override
    public ImmutableFloatStack select(FloatPredicate predicate)
    {
        return FloatStacks.immutable.withAllReversed(this.delegate.toReversed().select(predicate));
    }

    @Override
    public ImmutableFloatStack reject(FloatPredicate predicate)
    {
        return FloatStacks.immutable.withAllReversed(this.delegate.toReversed().reject(predicate));
    }

    @Override
    public <V> ImmutableStack<V> collect(FloatToObjectFunction<? extends V> function)
    {
        return Stacks.immutable.withAllReversed(this.delegate.asReversed().collect(function));
    }

    @Override
    public ImmutableFloatStack toImmutable()
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
    public MutableFloatList toSortedList()
    {
        return FloatArrayList.newList(this).sortThis();
    }

    private Object writeReplace()
    {
        return new ImmutableFloatStackSerializationProxy(this);
    }

    private static class ImmutableFloatStackSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private FloatStack stack;

        public ImmutableFloatStackSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableFloatStackSerializationProxy(FloatStack stack)
        {
            this.stack = stack;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.stack.size());
            try
            {
                this.stack.forEach(new CheckedFloatProcedure()
                {
                    @Override
                    public void safeValue(float item) throws IOException
                    {
                        out.writeFloat(item);
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
            FloatArrayList deserializedDelegate = new FloatArrayList(size);

            for (int i = 0; i < size; i++)
            {
                deserializedDelegate.add(in.readFloat());
            }

            this.stack = ImmutableFloatArrayStack.newStackFromTopToBottom(deserializedDelegate);
        }

        protected Object readResolve()
        {
            return this.stack;
        }
    }
}
