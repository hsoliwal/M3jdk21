/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.immutable.primitive;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.eclipse.collections.api.map.primitive.ImmutableObjectLongMap;
import org.eclipse.collections.api.map.primitive.ObjectLongMap;
import org.eclipse.collections.api.map.primitive.MutableObjectLongMap;
import org.eclipse.collections.impl.block.procedure.checked.primitive.CheckedObjectLongProcedure;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectLongHashMap;

/**
 * This file was automatically generated from template file abstractImmutableObjectPrimitiveMap.stg.
 *
 * @since 4.0.
 */
public abstract class AbstractImmutableObjectLongMap<V> implements ImmutableObjectLongMap<V>
{
    protected static class ImmutableObjectLongMapSerializationProxy<V> implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private ObjectLongMap<V> map;

        public ImmutableObjectLongMapSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableObjectLongMapSerializationProxy(ObjectLongMap<V> map)
        {
            this.map = map;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.map.size());
            try
            {
                this.map.forEachKeyValue(new CheckedObjectLongProcedure<V>()
                {
                    @Override
                    public void safeValue(V key, long value) throws IOException
                    {
                        out.writeObject(key);
                        out.writeLong(value);
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
            MutableObjectLongMap<V> deserializedMap = new ObjectLongHashMap<>();

            for (int i = 0; i < size; i++)
            {
                deserializedMap.put((V) in.readObject(), in.readLong());
            }

            this.map = deserializedMap;
        }

        protected Object readResolve()
        {
            return this.map.toImmutable();
        }
    }
}
