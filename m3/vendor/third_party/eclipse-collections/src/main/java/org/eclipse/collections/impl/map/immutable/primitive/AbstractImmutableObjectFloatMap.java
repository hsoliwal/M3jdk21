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

import org.eclipse.collections.api.map.primitive.ImmutableObjectFloatMap;
import org.eclipse.collections.api.map.primitive.ObjectFloatMap;
import org.eclipse.collections.api.map.primitive.MutableObjectFloatMap;
import org.eclipse.collections.impl.block.procedure.checked.primitive.CheckedObjectFloatProcedure;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;

/**
 * This file was automatically generated from template file abstractImmutableObjectPrimitiveMap.stg.
 *
 * @since 4.0.
 */
public abstract class AbstractImmutableObjectFloatMap<V> implements ImmutableObjectFloatMap<V>
{
    protected static class ImmutableObjectFloatMapSerializationProxy<V> implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private ObjectFloatMap<V> map;

        public ImmutableObjectFloatMapSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableObjectFloatMapSerializationProxy(ObjectFloatMap<V> map)
        {
            this.map = map;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.map.size());
            try
            {
                this.map.forEachKeyValue(new CheckedObjectFloatProcedure<V>()
                {
                    @Override
                    public void safeValue(V key, float value) throws IOException
                    {
                        out.writeObject(key);
                        out.writeFloat(value);
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
            MutableObjectFloatMap<V> deserializedMap = new ObjectFloatHashMap<>();

            for (int i = 0; i < size; i++)
            {
                deserializedMap.put((V) in.readObject(), in.readFloat());
            }

            this.map = deserializedMap;
        }

        protected Object readResolve()
        {
            return this.map.toImmutable();
        }
    }
}
