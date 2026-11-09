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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.stack.ImmutableStack;
import org.eclipse.collections.api.stack.primitive.BooleanStack;
import org.eclipse.collections.api.stack.primitive.ImmutableBooleanStack;
import org.eclipse.collections.impl.block.procedure.checked.primitive.CheckedBooleanProcedure;
import org.eclipse.collections.impl.factory.Stacks;
import org.eclipse.collections.impl.factory.primitive.BooleanStacks;
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList;
import org.eclipse.collections.impl.stack.mutable.primitive.BooleanArrayStack;
import org.eclipse.collections.impl.stack.primitive.AbstractBooleanStack;

/**
 * ImmutableBooleanArrayStack is the non-modifiable equivalent of {@link BooleanArrayStack}.
 * This file was automatically generated from template file immutablePrimitiveArrayStack.stg.
 *
 * @since 4.0.
 */
final class ImmutableBooleanArrayStack extends AbstractBooleanStack
        implements ImmutableBooleanStack, Serializable
{
    private static final long serialVersionUID = 1L;
    private final BooleanArrayList delegate;

    private ImmutableBooleanArrayStack(boolean[] newElements)
    {
        this.checkOptimizedSize(newElements.length);
        this.delegate = new BooleanArrayList(newElements);
    }

    private ImmutableBooleanArrayStack(BooleanArrayList newElements)
    {
        this.checkOptimizedSize(newElements.size());
        this.delegate = newElements;
    }

    private void checkOptimizedSize(int length)
    {
        if (length <= 1)
        {
            throw new IllegalArgumentException("Use BooleanStacks.immutable.with() to instantiate an optimized collection");
        }
    }

    public static ImmutableBooleanArrayStack newStack(BooleanIterable iterable)
    {
        return new ImmutableBooleanArrayStack(iterable.toArray());
    }

    public static ImmutableBooleanArrayStack newStackWith(boolean... elements)
    {
        boolean[] newArray = new boolean[elements.length];
        System.arraycopy(elements, 0, newArray, 0, elements.length);
        return new ImmutableBooleanArrayStack(newArray);
    }

    public static ImmutableBooleanArrayStack newStackFromTopToBottom(boolean... items)
    {
        return new ImmutableBooleanArrayStack(BooleanArrayList.newListWith(items).reverseThis());
    }

    public static ImmutableBooleanArrayStack newStackFromTopToBottom(BooleanIterable items)
    {
        return new ImmutableBooleanArrayStack(BooleanArrayList.newList(items).reverseThis());
    }

    @Override
    protected BooleanArrayList getDelegate()
    {
        return this.delegate;
    }

    @Override
    public ImmutableBooleanStack push(boolean item)
    {
        BooleanArrayList newDelegate = BooleanArrayList.newList(this.delegate);
        newDelegate.add(item);
        return new ImmutableBooleanArrayStack(newDelegate);
    }

    @Override
    public ImmutableBooleanStack pop()
    {
        BooleanArrayList newDelegate = BooleanArrayList.newList(this.delegate);
        newDelegate.removeAtIndex(this.delegate.size() - 1);
        return BooleanStacks.immutable.with(newDelegate.toArray());
    }

    @Override
    public ImmutableBooleanStack pop(int count)
    {
        this.checkNegativeCount(count);
        if (count == 0)
        {
            return this;
        }
        this.checkSizeLessThanCount(count);
        BooleanArrayList newDelegate = BooleanArrayList.newList(this.delegate);
        while (count > 0)
        {
            newDelegate.removeAtIndex(newDelegate.size() - 1);
            count--;
        }
        return BooleanStacks.immutable.with(newDelegate.toArray());
    }

    private void checkNegativeCount(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Count must be positive but was " + count);
        }
    }

    @Override
    public ImmutableBooleanStack select(BooleanPredicate predicate)
    {
        return BooleanStacks.immutable.withAllReversed(this.delegate.toReversed().select(predicate));
    }

    @Override
    public ImmutableBooleanStack reject(BooleanPredicate predicate)
    {
        return BooleanStacks.immutable.withAllReversed(this.delegate.toReversed().reject(predicate));
    }

    @Override
    public <V> ImmutableStack<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        return Stacks.immutable.withAllReversed(this.delegate.asReversed().collect(function));
    }

    @Override
    public ImmutableBooleanStack toImmutable()
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

    private Object writeReplace()
    {
        return new ImmutableBooleanStackSerializationProxy(this);
    }

    private static class ImmutableBooleanStackSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private BooleanStack stack;

        public ImmutableBooleanStackSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableBooleanStackSerializationProxy(BooleanStack stack)
        {
            this.stack = stack;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.stack.size());
            try
            {
                this.stack.forEach(new CheckedBooleanProcedure()
                {
                    @Override
                    public void safeValue(boolean item) throws IOException
                    {
                        out.writeBoolean(item);
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
            BooleanArrayList deserializedDelegate = new BooleanArrayList(size);

            for (int i = 0; i < size; i++)
            {
                deserializedDelegate.add(in.readBoolean());
            }

            this.stack = ImmutableBooleanArrayStack.newStackFromTopToBottom(deserializedDelegate);
        }

        protected Object readResolve()
        {
            return this.stack;
        }
    }
}
