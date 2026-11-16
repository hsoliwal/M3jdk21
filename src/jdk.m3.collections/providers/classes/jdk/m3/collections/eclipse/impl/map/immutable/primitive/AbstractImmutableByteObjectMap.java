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

import jdk.m3.collections.eclipse.api.block.function.Function;
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.IntFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongFunction;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableByteObjectMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableObjectDoubleMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableObjectLongMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableByteObjectMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableObjectDoubleMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableObjectLongMap;
import jdk.m3.collections.eclipse.api.map.primitive.ByteObjectMap;
import jdk.m3.collections.eclipse.impl.block.factory.PrimitiveFunctions;
import jdk.m3.collections.eclipse.impl.block.procedure.checked.primitive.CheckedByteObjectProcedure;
import jdk.m3.collections.eclipse.impl.factory.primitive.ObjectDoubleMaps;
import jdk.m3.collections.eclipse.impl.factory.primitive.ObjectLongMaps;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.ByteObjectHashMap;

/**
 * This file was automatically generated from template file abstractImmutablePrimitiveObjectMap.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings({"missing-explicit-ctor", "serial", "unchecked"})
public abstract class AbstractImmutableByteObjectMap<V> implements ImmutableByteObjectMap<V>
{
    protected static class ImmutableByteObjectMapSerializationProxy<V> implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private ByteObjectMap<V> map;

        public ImmutableByteObjectMapSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableByteObjectMapSerializationProxy(ByteObjectMap<V> map)
        {
            this.map = map;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.map.size());
            try
            {
                this.map.forEachKeyValue(new CheckedByteObjectProcedure<V>()
                {
                    @Override
                    public void safeValue(byte key, V value) throws IOException
                    {
                        out.writeByte(key);
                        out.writeObject(value);
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
            MutableByteObjectMap<V> deserializedMap = new ByteObjectHashMap<>();

            for (int i = 0; i < size; i++)
            {
                deserializedMap.put(in.readByte(), (V) in.readObject());
            }

            this.map = deserializedMap;
        }

        protected Object readResolve()
        {
            return this.map.toImmutable();
        }
    }

    @Override
    public <V1> ImmutableObjectLongMap<V1> sumByInt(Function<? super V, ? extends V1> groupBy, IntFunction<? super V> function)
    {
        MutableObjectLongMap<V1> result = ObjectLongMaps.mutable.empty();
        return this.injectInto(result, PrimitiveFunctions.sumByIntFunction(groupBy, function)).toImmutable();
    }

    @Override
    public <V1> ImmutableObjectDoubleMap<V1> sumByFloat(Function<? super V, ? extends V1> groupBy, FloatFunction<? super V> function)
    {
        MutableObjectDoubleMap<V1> result = ObjectDoubleMaps.mutable.empty();
        return this.injectInto(result, PrimitiveFunctions.sumByFloatFunction(groupBy, function)).toImmutable();
    }

    @Override
    public <V1> ImmutableObjectLongMap<V1> sumByLong(Function<? super V, ? extends V1> groupBy, LongFunction<? super V> function)
    {
        MutableObjectLongMap<V1> result = ObjectLongMaps.mutable.empty();
        return this.injectInto(result, PrimitiveFunctions.sumByLongFunction(groupBy, function)).toImmutable();
    }

    @Override
    public <V1> ImmutableObjectDoubleMap<V1> sumByDouble(Function<? super V, ? extends V1> groupBy, DoubleFunction<? super V> function)
    {
        MutableObjectDoubleMap<V1> result = ObjectDoubleMaps.mutable.empty();
        return this.injectInto(result, PrimitiveFunctions.sumByDoubleFunction(groupBy, function)).toImmutable();
    }
}
