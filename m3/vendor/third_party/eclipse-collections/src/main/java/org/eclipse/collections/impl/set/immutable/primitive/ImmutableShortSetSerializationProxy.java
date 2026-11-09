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

import org.eclipse.collections.api.set.primitive.ShortSet;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.impl.block.procedure.checked.primitive.CheckedShortProcedure;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;

/**
 * This file was automatically generated from template file immutablePrimitiveSetSerializationProxy.stg.
 *
 * @since 4.0.
 */
public final class ImmutableShortSetSerializationProxy implements Externalizable
{
    private static final long serialVersionUID = 1L;

    private ShortSet set;

    @SuppressWarnings("UnusedDeclaration")
    public ImmutableShortSetSerializationProxy()
    {
        // Empty constructor for Externalizable class
    }

    public ImmutableShortSetSerializationProxy(ShortSet set)
    {
        this.set = set;
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException
    {
        out.writeInt(this.set.size());
        try
        {
            this.set.forEach(new CheckedShortProcedure()
            {
                @Override
                public void safeValue(short item) throws Exception
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
    public void readExternal(ObjectInput in) throws IOException
    {
        int size = in.readInt();
        MutableShortSet deserializedSet = new ShortHashSet(size);

        for (int i = 0; i < size; i++)
        {
            deserializedSet.add(in.readShort());
        }

        this.set = deserializedSet;
    }

    private Object readResolve()
    {
        return this.set.toImmutable();
    }
}
