// Modified 2026 by Hitesh Soliwal and Synexia contributors: owned M3 array storage, pinned bounded helper closure and documented query-contract repairs.
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.m3.collections.apache;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import com.m3.collections.M3LinkedHashMap;
import java.util.Map;

import org.apache.commons.collections4.MultiValuedMap;

/**
 * Implements a {@code SetValuedMap}, using a {@link M3LinkedHashMap} to provide data
 * storage and {@link M3LinkedHashSetGroup}s as value collections. This is the standard
 * implementation of a SetValuedMap.
 * <p>
 * <strong>Note that M3SetValuedMap is not synchronized and is not
 * thread-safe.</strong> If you wish to use this map from multiple threads
 * concurrently, you must use appropriate synchronization. This class may throw
 * exceptions when accessed by concurrent threads without synchronization.
 * </p>
 *
 * @param <K> the type of the keys in this map
 * @param <V> the type of the values in this map
 * @since 4.5.0-M3
 */
public final class M3SetValuedMap<K, V> extends M3AbstractSetValuedMap<K, V>
    implements Serializable {

    /** Serialization Version */
    private static final long serialVersionUID = 1L;

    /**
     * The initial map capacity used when none specified in constructor.
     */
    private static final int DEFAULT_INITIAL_MAP_CAPACITY = 16;

    /**
     * The initial set capacity when using none specified in constructor.
     */
    private static final int DEFAULT_INITIAL_SET_CAPACITY = 3;

    /**
     * The initial list capacity when creating a new value collection.
     */
    private final int initialSetCapacity;

    /**
     * Creates an empty LinkedHashSetValuedHashMap with the default initial
     * map capacity (16) and the default initial set capacity (3).
     */
    public M3SetValuedMap() {
        this(DEFAULT_INITIAL_MAP_CAPACITY, DEFAULT_INITIAL_SET_CAPACITY);
    }

    /**
     * Creates an empty LinkedHashSetValuedHashMap with the default initial
     * map capacity (16) and the specified initial set capacity.
     *
     * @param initialSetCapacity  the initial capacity used for value collections
     */
    public M3SetValuedMap(final int initialSetCapacity) {
        this(DEFAULT_INITIAL_MAP_CAPACITY, initialSetCapacity);
    }

    /**
     * Creates an empty LinkedHashSetValuedHashMap with the specified initial
     * map and list capacities.
     *
     * @param initialMapCapacity  the initial hashmap capacity
     * @param initialSetCapacity  the initial capacity used for value collections
     */
    public M3SetValuedMap(final int initialMapCapacity, final int initialSetCapacity) {
        super(newDirectory(initialMapCapacity));
        this.initialSetCapacity = initialSetCapacity;
    }

    /**
     * Creates an LinkedHashSetValuedHashMap copying all the mappings of the given map.
     *
     * @param map a {@code Map} to copy into this map
     */
    public M3SetValuedMap(final Map<? extends K, ? extends V> map) {
        this(map.size(), DEFAULT_INITIAL_SET_CAPACITY);
        super.putAll(map);
    }

    /**
     * Creates an LinkedHashSetValuedHashMap copying all the mappings of the given map.
     *
     * @param map a {@code MultiValuedMap} to copy into this map
     */
    public M3SetValuedMap(final MultiValuedMap<? extends K, ? extends V> map) {
        this(map.size(), DEFAULT_INITIAL_SET_CAPACITY);
        super.putAll(map);
    }

    @Override
    protected M3LinkedHashSetGroup<V> createCollection() {
        return new M3LinkedHashSetGroup<>(initialSetCapacity);
    }

    /**
     * Deserializes an instance from an ObjectInputStream.
     *
     * @param in The source ObjectInputStream.
     * @throws IOException            Any of the usual Input/Output related exceptions.
     * @throws ClassNotFoundException A class of a serialized object cannot be found.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        setMap(new M3LinkedHashMap<>());
        doReadObject(in);
    }

    /**
     * Serializes this object to an ObjectOutputStream.
     *
     * @param out the target ObjectOutputStream.
     * @throws IOException thrown when an I/O errors occur writing to the target stream.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        doWriteObject(out);
    }

}
