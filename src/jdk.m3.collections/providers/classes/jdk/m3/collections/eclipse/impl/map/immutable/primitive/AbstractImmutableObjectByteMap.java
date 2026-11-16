/*
 * Copyright (c) 2026 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.map.immutable.primitive;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import jdk.m3.collections.eclipse.api.map.primitive.ImmutableObjectByteMap;
import jdk.m3.collections.eclipse.api.map.primitive.ObjectByteMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableObjectByteMap;
import jdk.m3.collections.eclipse.impl.block.procedure.checked.primitive.CheckedObjectByteProcedure;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.ObjectByteHashMap;

/**
 * This file was automatically generated from template file abstractImmutableObjectPrimitiveMap.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings({"missing-explicit-ctor", "serial", "unchecked"})
public abstract class AbstractImmutableObjectByteMap<V> implements ImmutableObjectByteMap<V>
{
    protected static class ImmutableObjectByteMapSerializationProxy<V> implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private ObjectByteMap<V> map;

        public ImmutableObjectByteMapSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableObjectByteMapSerializationProxy(ObjectByteMap<V> map)
        {
            this.map = map;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.map.size());
            try
            {
                this.map.forEachKeyValue(new CheckedObjectByteProcedure<V>()
                {
                    @Override
                    public void safeValue(V key, byte value) throws IOException
                    {
                        out.writeObject(key);
                        out.writeByte(value);
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
            MutableObjectByteMap<V> deserializedMap = new ObjectByteHashMap<>();

            for (int i = 0; i < size; i++)
            {
                deserializedMap.put((V) in.readObject(), in.readByte());
            }

            this.map = deserializedMap;
        }

        protected Object readResolve()
        {
            return this.map.toImmutable();
        }
    }
}
