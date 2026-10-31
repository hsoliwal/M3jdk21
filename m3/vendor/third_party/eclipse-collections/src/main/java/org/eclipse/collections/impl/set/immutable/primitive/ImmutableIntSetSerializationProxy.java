/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.set.immutable.primitive;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.block.procedure.checked.primitive.CheckedIntProcedure;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

/**
 * This file was automatically generated from template file immutablePrimitiveSetSerializationProxy.stg.
 *
 * @since 4.0.
 */
public final class ImmutableIntSetSerializationProxy implements Externalizable
{
    private static final long serialVersionUID = 1L;

    private IntSet set;

    @SuppressWarnings("UnusedDeclaration")
    public ImmutableIntSetSerializationProxy()
    {
        // Empty constructor for Externalizable class
    }

    public ImmutableIntSetSerializationProxy(IntSet set)
    {
        this.set = set;
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException
    {
        out.writeInt(this.set.size());
        try
        {
            this.set.forEach(new CheckedIntProcedure()
            {
                @Override
                public void safeValue(int item) throws Exception
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
    public void readExternal(ObjectInput in) throws IOException
    {
        int size = in.readInt();
        MutableIntSet deserializedSet = new IntHashSet(size);

        for (int i = 0; i < size; i++)
        {
            deserializedSet.add(in.readInt());
        }

        this.set = deserializedSet;
    }

    private Object readResolve()
    {
        return this.set.toImmutable();
    }
}
