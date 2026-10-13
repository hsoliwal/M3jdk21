/*
 * Copyright 2014-2024 Real Logic Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.agrona.collections;

import org.agrona.generation.DoNotSub;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.LongPredicate;
import java.util.function.ToLongFunction;

import static java.util.Objects.requireNonNull;
import static org.agrona.BitUtil.findNextPositivePowerOfTwo;
import static org.agrona.collections.CollectionUtil.validateLoadFactor;

/**
 * {@link java.util.Map} implementation specialised for long values using open addressing and
 * linear probing for cache efficient access. The implementation is mirror copy of {@link Long2ObjectHashMap}
 * and it also relies on missing value concept from {@link Long2LongHashMap}
 *
 * @param <K> type of keys stored in the {@link java.util.Map}
 */
public class Object2LongHashMap<K> implements Map<K, Long>
{
    @DoNotSub static final int MIN_CAPACITY = 8;

    private final float loadFactor;
    private final long missingValue;
    @DoNotSub private int resizeThreshold;
    @DoNotSub private int size;
    private final boolean shouldAvoidAllocation;

    private K[] keys;
    private long[] values;

    private ValueCollection valueCollection;
    private KeySet keySet;
    private EntrySet entrySet;

    /**
     * Construct a map with default capacity and load factor.
     *
     * @param missingValue value to be used as a null maker in the map
     */
    public Object2LongHashMap(final long missingValue)
    {
        this(MIN_CAPACITY, Hashing.DEFAULT_LOAD_FACTOR, missingValue);
    }

    /**
     * Construct a new map allowing a configuration for initial capacity and load factor.
     *
     * @param initialCapacity for the backing array
     * @param loadFactor      limit for resizing on puts
     * @param missingValue    value to be used as a null marker in the map
     */
    public Object2LongHashMap(
        @DoNotSub final int initialCapacity,
        final float loadFactor,
        final long missingValue)
    {
        this(initialCapacity, loadFactor, missingValue, true);
    }

    /**
     * Construct a new map allowing a configuration for initial capacity and load factor.
     *
     * @param initialCapacity       for the backing array
     * @param loadFactor            limit for resizing on puts
     * @param missingValue          value to be used as a null marker in the map
     * @param shouldAvoidAllocation should allocation be avoided by caching iterators and map entries.
     */
    @SuppressWarnings("unchecked")
    public Object2LongHashMap(
        @DoNotSub final int initialCapacity,
        final float loadFactor,
        final long missingValue,
        final boolean shouldAvoidAllocation)
    {
        validateLoadFactor(loadFactor);

        this.loadFactor = loadFactor;
        /* @DoNotSub */ final int capacity = findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, initialCapacity));
        /* @DoNotSub */ resizeThreshold = (int)(capacity * loadFactor);

        this.missingValue = missingValue;
        this.shouldAvoidAllocation = shouldAvoidAllocation;
        keys = (K[])new Object[capacity];
        values = new long[capacity];
        Arrays.fill(values, missingValue);
    }

    /**
     * Copy construct a new map from an existing one.
     *
     * @param mapToCopy for construction.
     */
    public Object2LongHashMap(final Object2LongHashMap<K> mapToCopy)
    {
        this.loadFactor = mapToCopy.loadFactor;
        this.resizeThreshold = mapToCopy.resizeThreshold;
        this.size = mapToCopy.size;
        this.missingValue = mapToCopy.missingValue;
        this.shouldAvoidAllocation = mapToCopy.shouldAvoidAllocation;

        keys = mapToCopy.keys.clone();
        values = mapToCopy.values.clone();
    }

    /**
     * The value to be used as a null marker in the map.
     *
     * @return value to be used as a null marker in the map.
     */
    public long missingValue()
    {
        return missingValue;
    }

    /**
     * Get the load factor beyond which the map will increase size.
     *
     * @return load factor for when the map should increase size.
     */
    public float loadFactor()
    {
        return loadFactor;
    }

    /**
     * Get the total capacity for the map to which the load factor will be a fraction of.
     *
     * @return the total capacity for the map.
     */
    @DoNotSub public int capacity()
    {
        return values.length;
    }

    /**
     * Get the actual threshold which when reached the map will resize.
     * This is a function of the current capacity and load factor.
     *
     * @return the threshold when the map will resize.
     */
    @DoNotSub public int resizeThreshold()
    {
        return resizeThreshold;
    }

    /**
     * {@inheritDoc}
     */
    @DoNotSub public int size()
    {
        return size;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty()
    {
        return 0 == size;
    }

    /**
     * {@inheritDoc}
     * Overloaded version of {@link Map#containsKey(Object)} that takes a primitive long key.
     *
     * @param key for indexing the {@link Map}
     * @return true if the key is found otherwise false.
     */
    @SuppressWarnings("unchecked")
    public boolean containsKey(final Object key)
    {
        return missingValue != getValue((K)key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(final Object value)
    {
        return containsValue((long)value);
    }

    /**
     * Overloaded version to avoid boxing.
     *
     * @param value to check.
     * @return true if the collection contains the value.
     */
    public boolean containsValue(final long value)
    {
        if (missingValue == value)
        {
            return false;
        }

        boolean found = false;
        final long[] values = this.values;
        for (final long v : values)
        {
            if (value == v)
            {
                found = true;
                break;
            }
        }

        return found;
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code defaultValue} if this map contains no mapping
     * for the key.
     *
     * @param key          whose associated value is to be returned.
     * @param defaultValue the default mapping of the key
     * @return the value to which the specified key is mapped, or {@code defaultValue} if this map contains no mapping
     * for the key.
     */
    @SuppressWarnings("unchecked")
    public long getOrDefault(final Object key, final long defaultValue)
    {
        final long value = getValue((K)key);
        return missingValue != value ? value : defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Long get(final Object key)
    {
        return valueOrNull(getValue((K)key));
    }

    /**
     * Overloaded version of {@link Map#get(Object)} that takes a primitive long key.
     * Due to type erasure have to rename the method
     *
     * @param key for indexing the {@link Map}
     * @return the value if found otherwise missingValue
     */
    public long getValue(final K key)
    {
        requireNonNull(key);
        final long missingValue = this.missingValue;
        final K[] keys = this.keys;
        final long[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

        long value;
        while (missingValue != (value = values[index]))
        {
            if (Objects.equals(keys[index], key))
            {
                break;
            }

            index = ++index & mask;
        }

        return value;
    }

    /**
     * Get a value for a given key, or if it does not exist then default the value via a
     * {@link java.util.function.LongFunction} and put it in the map.
     * <p>
     * Primitive specialized version of {@link java.util.Map#computeIfAbsent}.
     *
     * @param key             to search on.
     * @param mappingFunction to provide a value if the get returns missingValue.
     * @return old value if found otherwise the newly computed value.
     */
    @SuppressWarnings("overloads")
    public long computeIfAbsent(final K key, final ToLongFunction<? super K> mappingFunction)
    {
        requireNonNull(key);
        final long missingValue = this.missingValue;
        final K[] keys = this.keys;
        final long[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

        long value;
        while (missingValue != (value = values[index]))
        {
            if (Objects.equals(keys[index], key))
            {
                return value;
            }

            index = ++index & mask;
        }

        final long newValue = mappingFunction.applyAsLong(key);
        if (missingValue != newValue)
        {
            keys[index] = key;
            values[index] = newValue;
            if (++size > resizeThreshold)
            {
                increaseCapacity();
            }
        }

        return newValue;
    }

    /**
     * If the value for the specified key is present, attempts to compute a new
     * mapping given the key and its current mapped value.
     * <p>
     * If the function returns missingValue, the mapping is removed
     * <p>
     * Primitive specialized version of {@link java.util.Map#computeIfPresent(Object, BiFunction)}.
     *
     * @param key               to search on.
     * @param remappingFunction to provide a value if the get returns missingValue.
     * @return the new value associated with the specified key, or missingValue if none
     */
    @SuppressWarnings("overloads")
    public long computeIfPresent(final K key, final ObjectLongToLongFunction<? super K> remappingFunction)
    {
        requireNonNull(key);
        final long missingValue = this.missingValue;
        final K[] keys = this.keys;
        final long[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

        long value;
        while (missingValue != (value = values[index]))
        {
            if (Objects.equals(keys[index], key))
            {
                final long newValue = remappingFunction.apply(key, value);
                values[index] = newValue;
                if (missingValue == newValue)
                {
                    keys[index] = null;
                    size--;
                    compactChain(index);
                }
                return newValue;
            }

            index = ++index & mask;
        }

        return missingValue;
    }

    /**
     * Attempts to compute a mapping for the specified key and its current mapped
     * value (or missingValue if there is no current mapping).
     * <p>
     * If the function returns missingValue, the mapping is removed (or remains
     * absent if initially absent).
     * <p>
     * Primitive specialized version of {@link java.util.Map#compute(Object, BiFunction)}.
     *
     * @param key               to search on.
     * @param remappingFunction to provide a value if the get returns missingValue.
     * @return the new value associated with the specified key, or missingValue if none
     */
    @SuppressWarnings("overloads")
    public long compute(final K key, final ObjectLongToLongFunction<? super K> remappingFunction)
    {
        requireNonNull(key);
        final long missingValue = this.missingValue;
        final K[] keys = this.keys;
        final long[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

        long oldValue;
        while (missingValue != (oldValue = values[index]))
        {
            if (Objects.equals(keys[index], key))
            {
                break;
            }

            index = ++index & mask;
        }

        final long newValue = remappingFunction.apply(key, oldValue);
        if (missingValue != newValue)
        {
            values[index] = newValue;
            if (missingValue == oldValue)
            {
                keys[index] = key;
                if (++size > resizeThreshold)
                {
                    increaseCapacity();
                }
            }
        }
        else if (missingValue != oldValue)
        {
            keys[index] = null;
            values[index] = missingValue;
            --size;
            compactChain(index);
        }

        return newValue;
    }

    /**
     * If the specified key is not already associated with a value associates it with the given value. Otherwise,
     * replaces the associated value with the results of the given remapping function, or removes if the result is
     * {@link #missingValue()}. This method may be of use when combining multiple mapped values for a key. If the
     * function returns {@link #missingValue()} the mapping is removed.
     * <p>
     * Primitive specialized version of {@link java.util.Map#merge(Object, Object, BiFunction)}.
     *
     * @param key               with which the resulting value is to be associated.
     * @param value             to be merged with the existing value associated with the key or, if no existing value
     *                          is associated with the key, to be associated with the key.
     * @param remappingFunction the function to recompute a value if present.
     * @return the new value associated with the specified key, or {@link #missingValue()} if no value is associated
     * with the key.
     */
    public long merge(final K key, final long value, final LongLongFunction remappingFunction)
    {
        requireNonNull(key);
        requireNonNull(remappingFunction);
        final long missingValue = this.missingValue;
        if (missingValue == value)
        {
            throw new IllegalArgumentException("cannot accept missingValue");
        }

        final K[] keys = this.keys;
        final long[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

        long oldValue;
        while (missingValue != (oldValue = values[index]))
        {
            if (Objects.equals(keys[index], key))
            {
                break;
            }

            index = ++index & mask;
        }

        final long newValue = missingValue == oldValue ? value : remappingFunction.apply(oldValue, value);
        if (missingValue != newValue)
        {
            keys[index] = key;
            values[index] = newValue;
            if (++size > resizeThreshold)
            {
                increaseCapacity();
            }
        }
        else
        {
            keys[index] = null;
            values[index] = missingValue;
            --size;
            compactChain(index);
        }
        return newValue;
    }

    /**
     * {@inheritDoc}
     */
    public Long put(final K key, final Long value)
    {
        return valueOrNull(put(key, (long)value));
    }

    /**
     * Overloaded version of {@link Map#put(Object, Object)} that takes a primitive long key.
     *
     * @param key   for indexing the {@link Map}
     * @param value to be inserted in the {@link Map}
     * @return the previous value if found otherwise missingValue
     */
    public long put(final K key, final long value)
    {
        requireNonNull(key);
        final long missingValue = this.missingValue;
        if (missingValue == value)
        {
            throw new IllegalArgumentException("cannot accept missingValue");
        }

        final K[] keys = this.keys;
        final long[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

        long oldValue;
        while (missingValue != (oldValue = values[index]))
        {
            if (Objects.equals(keys[index], key))
            {
                break;
            }

            index = ++index & mask;
        }

        if (missingValue == oldValue)
        {
            ++size;
            keys[index] = key;
        }

        values[index] = value;

        if (size > resizeThreshold)
        {
            increaseCapacity();
        }

        return oldValue;
    }

    /**
     * {@inheritDoc}
     */
    public Long putIfAbsent(final K key, final Long value)
    {
        return valueOrNull(putIfAbsent(key, (long)value));
    }

    /**
     * If the specified key is not already associated with a value associates it with the given value and returns
     * {@link #missingValue()}, else returns the current value.
     *
     * @param key   with which the specified value is to be associated.
     * @param value to be associated with the specified key.
     * @return the existing value associated with the specified key, or {@link #missingValue()} if there was no mapping
     * for the key.
     * @throws IllegalArgumentException if {@code value == missingValue()}
     */
    public long putIfAbsent(final K key, final long value)
    {
        requireNonNull(key);
        final long missingValue = this.missingValue;
        if (missingValue == value)
        {
            throw new IllegalArgumentException("cannot accept missingValue");
        }

        final K[] keys = this.keys;
        final long[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

        long oldValue;
        while (missingValue != (oldValue = values[index]))
        {
            if (Objects.equals(keys[index], key))
            {
                return oldValue;
            }

            index = ++index & mask;
        }

        keys[index] = key;
        values[index] = value;

        if (++size > resizeThreshold)
        {
            increaseCapacity();
        }

        return missingValue;
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(final Object key, final Object value)
    {
        return remove(key, (long)value);
    }

    /**
     * Primitive overload  of the {@link Map#remove(Object, Object)} that avoids boxing on the value.
     *
     * @param key   with which the specified value is associated.
     * @param value expected to be associated with the specified key.
     * @return {@code true} if the value was removed.
     */
    public boolean remove(final Object key, final long value)
    {
        final long missingValue = this.missingValue;
        final K[] keys = this.keys;
        final long[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

        long existingValue;
        while (missingValue != (existingValue = values[index]))
        {
            if (Objects.equals(keys[index], key))
            {
                if (value == existingValue)
                {
                    keys[index] = null;
                    values[index] = missingValue;
                    --size;

                    compactChain(index);
                    return true;
                }
                break;
            }

            index = ++index & mask;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Long remove(final Object key)
    {
        return valueOrNull(removeKey(((K)key)));
    }

    /**
     * Overloaded version of {@link Map#remove(Object)} that takes a key and returns a primitive long value.
     * Due to type erasure have to rename the method
     *
     * @param key for indexing the {@link Map}
     * @return the value if found otherwise missingValue
     */
    public long removeKey(final K key)
    {
        requireNonNull(key);
        final long missingValue = this.missingValue;
        final K[] keys = this.keys;
        final long[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

        long value;
        while (missingValue != (value = values[index]))
        {
            if (Objects.equals(keys[index], key))
            {
                keys[index] = null;
                values[index] = missingValue;
                --size;

                compactChain(index);
                break;
            }

            index = ++index & mask;
        }

        return value;
    }

    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        if (size > 0)
        {
            Arrays.fill(keys, null);
            Arrays.fill(values, missingValue);
            size = 0;
        }
    }

    /**
     * Compact the {@link Map} backing arrays by rehashing with a capacity just larger than current size
     * and giving consideration to the load factor.
     */
    public void compact()
    {
        @DoNotSub final int idealCapacity = (int)Math.round(size() * (1.0d / loadFactor));
        rehash(findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, idealCapacity)));
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(final Map<? extends K, ? extends Long> map)
    {
        for (final Entry<? extends K, ? extends Long> entry : map.entrySet())
        {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Puts all values from the given map to this map.
     *
     * @param map whose values to be added to this map.
     */
    public void putAll(final Object2LongHashMap<? extends K> map)
    {
        final long missingValue = map.missingValue;
        final K[] keys = map.keys;
        final long[] values = map.values;
        @DoNotSub final int length = values.length;

        for (@DoNotSub int index = 0, remaining = map.size; remaining > 0 && index < length; index++)
        {
            final long value = values[index];
            if (missingValue != value)
            {
                put(keys[index], value);
                remaining--;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public KeySet keySet()
    {
        if (null == keySet)
        {
            keySet = new KeySet();
        }

        return keySet;
    }

    /**
     * {@inheritDoc}
     */
    public ValueCollection values()
    {
        if (null == valueCollection)
        {
            valueCollection = new ValueCollection();
        }

        return valueCollection;
    }

    /**
     * {@inheritDoc}
     */
    public EntrySet entrySet()
    {
        if (null == entrySet)
        {
            entrySet = new EntrySet();
        }

        return entrySet;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        if (isEmpty())
        {
            return "{}";
        }

        final EntryIterator entryIterator = new EntryIterator();
        entryIterator.reset();

        final StringBuilder sb = new StringBuilder().append('{');
        while (true)
        {
            entryIterator.next();
            sb.append(entryIterator.getKey()).append('=').append(entryIterator.getLongValue());
            if (!entryIterator.hasNext())
            {
                return sb.append('}').toString();
            }
            sb.append(',').append(' ');
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof Map))
        {
            return false;
        }

        final Map<?, ?> that = (Map<?, ?>)o;

        if (size != that.size())
        {
            return false;
        }

        final K[] keys = this.keys;
        final long[] values = this.values;
        final long missingValue = this.missingValue;
        final long thatMissingValue =
            o instanceof Object2LongHashMap ? ((Object2LongHashMap<?>)o).missingValue : missingValue;
        for (@DoNotSub int i = 0, length = values.length; i < length; i++)
        {
            final long thisValue = values[i];
            if (missingValue != thisValue)
            {
                final Object thatValueObject = that.get(keys[i]);
                if (!(thatValueObject instanceof Long))
                {
                    return false;
                }

                final long thatValue = (Long)thatValueObject;
                if (thatMissingValue == thatValue || thisValue != thatValue)
                {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @DoNotSub public int hashCode()
    {
        @DoNotSub int result = 0;

        final K[] keys = this.keys;
        final long[] values = this.values;
        for (@DoNotSub int i = 0, length = values.length; i < length; i++)
        {
            final long value = values[i];
            if (missingValue != value)
            {
                result += (keys[i].hashCode() ^ Long.hashCode(value));
            }
        }

        return result;
    }

    /**
     * Primitive specialised version of {@link Map#replace(Object, Object)}
     *
     * @param key   with which the specified value is associated.
     * @param value to be associated with the specified key.
     * @return the previous value associated with the specified key, or {@link #missingValue()} if there was no mapping
     * for the key.
     */
    public long replace(final K key, final long value)
    {
        requireNonNull(key);
        final long missingValue = this.missingValue;
        if (missingValue == value)
        {
            throw new IllegalArgumentException("cannot accept missingValue");
        }

        final K[] keys = this.keys;
        final long[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

        long existingValue;
        while (missingValue != (existingValue = values[index]))
        {
            if (Objects.equals(keys[index], key))
            {
                values[index] = value;
                return existingValue;
            }

            index = ++index & mask;
        }

        return missingValue;
    }

    /**
     * Primitive specialised version of {@link Map#replace(Object, Object, Object)}
     *
     * @param key      key with which the specified value is associated.
     * @param oldValue value expected to be associated with the specified key.
     * @param newValue value to be associated with the specified key.
     * @return {@code true} if the value was replaced.
     */
    public boolean replace(final K key, final long oldValue, final long newValue)
    {
        requireNonNull(key);
        final long missingValue = this.missingValue;
        if (missingValue == newValue)
        {
            throw new IllegalArgumentException("cannot accept missingValue");
        }

        final K[] keys = this.keys;
        final long[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

        long existingValue;
        while (missingValue != (existingValue = values[index]))
        {
            if (Objects.equals(keys[index], key))
            {
                if (oldValue == existingValue)
                {
                    values[index] = newValue;
                    return true;
                }
                break;
            }

            index = ++index & mask;
        }

        return false;
    }

    /**
     * Primitive specialised version of {@link Map#replaceAll(BiFunction)}.
     * <p>
     * NB: Renamed from replaceAll to avoid overloading on parameter types of lambda
     * expression, which doesn't play well with type inference in lambda expressions.
     *
     * @param function the function to apply to each entry.
     */
    public void replaceAllLong(final ObjectLongToLongFunction<? super K> function)
    {
        requireNonNull(function);
        final long missingValue = this.missingValue;
        final K[] keys = this.keys;
        final long[] values = this.values;
        @DoNotSub final int length = values.length;

        for (@DoNotSub int index = 0, remaining = size; remaining > 0 && index < length; index++)
        {
            final long oldValue = values[index];
            if (missingValue != oldValue)
            {
                final long newVal = function.apply(keys[index], oldValue);
                if (missingValue == newVal)
                {
                    throw new IllegalArgumentException("cannot accept missingValue");
                }
                values[index] = newVal;
                --remaining;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void forEach(final BiConsumer<? super K, ? super Long> action)
    {
        forEachLong(action::accept);
    }

    /**
     * Performs the given action for each entry in this map until all entries have been processed or the action throws
     * an exception.
     *
     * @param action to be performed for each entry.
     */
    public void forEachLong(final ObjLongConsumer<? super K> action)
    {
        requireNonNull(action);
        final long missingValue = this.missingValue;
        final K[] keys = this.keys;
        final long[] values = this.values;
        @DoNotSub final int length = values.length;

        for (@DoNotSub int index = 0, remaining = size; remaining > 0 && index < length; index++)
        {
            final long oldValue = values[index];
            if (missingValue != oldValue)
            {
                action.accept(keys[index], oldValue);
                --remaining;
            }
        }
    }

    private void increaseCapacity()
    {
        @DoNotSub final int newCapacity = values.length << 1;
        if (newCapacity < 0)
        {
            throw new IllegalStateException("max capacity reached at size=" + size);
        }

        rehash(newCapacity);
    }

    private void rehash(@DoNotSub final int newCapacity)
    {
        @DoNotSub final int mask = newCapacity - 1;
        /* @DoNotSub */ resizeThreshold = (int)(newCapacity * loadFactor);

        @SuppressWarnings("unchecked")
        final K[] tempKeys = (K[])new Object[newCapacity];
        final long[] tempValues = new long[newCapacity];
        Arrays.fill(tempValues, missingValue);

        final K[] keys = this.keys;
        final long[] values = this.values;
        for (@DoNotSub int i = 0, size = values.length; i < size; i++)
        {
            final long value = values[i];
            if (missingValue != value)
            {
                final K key = keys[i];
                @DoNotSub int index = Hashing.hash(key, mask);
                while (missingValue != tempValues[index])
                {
                    index = ++index & mask;
                }

                tempKeys[index] = key;
                tempValues[index] = value;
            }
        }

        this.keys = tempKeys;
        this.values = tempValues;
    }

    @SuppressWarnings("FinalParameters")
    private void compactChain(@DoNotSub int deleteIndex)
    {
        final K[] keys = this.keys;
        final long[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = deleteIndex;

        while (true)
        {
            index = ++index & mask;
            final long value = values[index];
            if (missingValue == value)
            {
                break;
            }

            final K key = keys[index];
            @DoNotSub final int hash = Hashing.hash(key, mask);

            if ((index < hash && (hash <= deleteIndex || deleteIndex <= index)) ||
                (hash <= deleteIndex && deleteIndex <= index))
            {
                keys[deleteIndex] = key;
                values[deleteIndex] = value;

                keys[index] = null;
                values[index] = missingValue;
                deleteIndex = index;
            }
        }
    }

    private Long valueOrNull(final long value)
    {
        return value == missingValue ? null : value;
    }

    /**
     * Set of keys that can optionally cache iterators to avoid allocation.
     */
    public final class KeySet extends AbstractSet<K>
    {
        private final KeyIterator keyIterator = shouldAvoidAllocation ? new KeyIterator() : null;

        /**
         * {@inheritDoc}
         */
        public KeyIterator iterator()
        {
            KeyIterator keyIterator = this.keyIterator;
            if (null == keyIterator)
            {
                keyIterator = new KeyIterator();
            }

            keyIterator.reset();
            return keyIterator;
        }

        /**
         * {@inheritDoc}
         */
        @DoNotSub public int size()
        {
            return Object2LongHashMap.this.size();
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(final Object o)
        {
            return Object2LongHashMap.this.containsKey(o);
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public boolean remove(final Object o)
        {
            return missingValue != Object2LongHashMap.this.removeKey((K)o);
        }

        /**
         * {@inheritDoc}
         */
        public void clear()
        {
            Object2LongHashMap.this.clear();
        }
    }

    /**
     * Collection of values which can optionally cache iterators to avoid allocation.
     */
    public final class ValueCollection extends AbstractCollection<Long>
    {
        private final ValueIterator valueIterator = shouldAvoidAllocation ? new ValueIterator() : null;

        /**
         * {@inheritDoc}
         */
        public ValueIterator iterator()
        {
            ValueIterator valueIterator = this.valueIterator;
            if (null == valueIterator)
            {
                valueIterator = new ValueIterator();
            }

            valueIterator.reset();
            return valueIterator;
        }

        /**
         * {@inheritDoc}
         */
        @DoNotSub public int size()
        {
            return Object2LongHashMap.this.size();
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(final Object o)
        {
            return containsValue(o);
        }

        /**
         * Checks if the value is contained in the map.
         *
         * @param value to be checked.
         * @return {@code true} if value is contained in this map.
         */
        public boolean contains(final long value)
        {
            return containsValue(value);
        }

        /**
         * {@inheritDoc}
         */
        public void clear()
        {
            Object2LongHashMap.this.clear();
        }

        /**
         * Removes all the elements of this collection that satisfy the given predicate.
         *
         * @param filter a predicate which returns {@code true} for elements to be removed.
         * @return {@code true} if any elements were removed.
         */
        public boolean removeIfLong(final LongPredicate filter)
        {
            boolean removed = false;
            final ValueIterator iterator = iterator();
            while (iterator.hasNext())
            {
                if (filter.test(iterator.nextLong()))
                {
                    iterator.remove();
                    removed = true;
                }
            }
            return removed;
        }
    }

    /**
     * Set of entries which can optionally cache iterators to avoid allocation.
     */
    public final class EntrySet extends AbstractSet<Map.Entry<K, Long>>
    {
        private final EntryIterator entryIterator = shouldAvoidAllocation ? new EntryIterator() : null;

        /**
         * {@inheritDoc}
         */
        public EntryIterator iterator()
        {
            EntryIterator entryIterator = this.entryIterator;
            if (null == entryIterator)
            {
                entryIterator = new EntryIterator();
            }

            entryIterator.reset();

            return entryIterator;
        }

        /**
         * {@inheritDoc}
         */
        @DoNotSub public int size()
        {
            return Object2LongHashMap.this.size();
        }

        /**
         * {@inheritDoc}
         */
        public void clear()
        {
            Object2LongHashMap.this.clear();
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(final Object o)
        {
            if (!(o instanceof Entry))
            {
                return false;
            }

            @SuppressWarnings("rawtypes") final Entry entry = (Entry)o;
            final Long value = get(entry.getKey());

            return value != null && value.equals(entry.getValue());
        }

        /**
         * Removes all the elements of this collection that satisfy the given predicate.
         *
         * @param filter a predicate which returns {@code true} for elements to be removed.
         * @return {@code true} if any elements were removed.
         */
        public boolean removeIfLong(final ObjLongPredicate<? super K> filter)
        {
            boolean removed = false;
            final EntryIterator iterator = iterator();
            while (iterator.hasNext())
            {
                iterator.findNext();
                if (filter.test(iterator.getKey(), iterator.getLongValue()))
                {
                    iterator.remove();
                    removed = true;
                }
            }
            return removed;
        }

        /**
         * {@inheritDoc}
         */
        public Object[] toArray()
        {
            return toArray(new Object[size()]);
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(final T[] a)
        {
            final T[] array = a.length >= size ?
                a : (T[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
            final EntryIterator it = iterator();

            for (@DoNotSub int i = 0; i < array.length; i++)
            {
                if (it.hasNext())
                {
                    it.next();
                    array[i] = (T)it.allocateDuplicateEntry();
                }
                else
                {
                    array[i] = null;
                    break;
                }
            }

            return array;
        }
    }

    /**
     * Base iterator implementation that contains basic logic of traversing the element in the backing array.
     *
     * @param <T> type of elements.
     */
    abstract class AbstractIterator<T> implements Iterator<T>
    {
        @DoNotSub private int posCounter;
        @DoNotSub private int stopCounter;
        @DoNotSub private int remaining;
        private boolean isPositionValid = false;

        /**
         * Position of the current element.
         *
         * @return position of the element in the array.
         */
        @DoNotSub protected final int position()
        {
            return posCounter & (values.length - 1);
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext()
        {
            return remaining > 0;
        }

        /**
         * Find next element.
         *
         * @throws NoSuchElementException if no more elements.
         */
        protected final void findNext()
        {
            if (!hasNext())
            {
                throw new NoSuchElementException();
            }

            final long missingValue = Object2LongHashMap.this.missingValue;
            final long[] values = Object2LongHashMap.this.values;
            @DoNotSub final int mask = values.length - 1;

            for (@DoNotSub int i = posCounter - 1; i >= stopCounter; i--)
            {
                @DoNotSub final int index = i & mask;
                if (missingValue != values[index])
                {
                    posCounter = i;
                    isPositionValid = true;
                    --remaining;

                    return;
                }
            }

            isPositionValid = false;
            throw new IllegalStateException();
        }

        /**
         * {@inheritDoc}
         */
        public abstract T next();

        /**
         * {@inheritDoc}
         */
        public void remove()
        {
            if (isPositionValid)
            {
                @DoNotSub final int position = position();
                values[position] = missingValue;
                keys[position] = null;
                --size;

                compactChain(position);

                isPositionValid = false;
            }
            else
            {
                throw new IllegalStateException();
            }
        }

        final void reset()
        {
            remaining = Object2LongHashMap.this.size;
            final long[] values = Object2LongHashMap.this.values;
            @DoNotSub final int capacity = values.length;

            @DoNotSub int i = capacity;
            if (missingValue != values[capacity - 1])
            {
                for (i = 0; i < capacity; i++)
                {
                    if (missingValue == values[i])
                    {
                        break;
                    }
                }
            }

            stopCounter = i;
            posCounter = i + capacity;
            isPositionValid = false;
        }
    }

    /**
     * Iterator over values providing unboxed access via {@link #nextLong()}.
     */
    public final class ValueIterator extends AbstractIterator<Long>
    {
        /**
         * {@inheritDoc}
         */
        public Long next()
        {
            return nextLong();
        }

        /**
         * Get next value without boxing.
         *
         * @return next value.
         */
        public long nextLong()
        {
            findNext();
            return values[position()];
        }
    }

    /**
     * Iterator over keys.
     */
    public final class KeyIterator extends AbstractIterator<K>
    {
        /**
         * {@inheritDoc}
         */
        public K next()
        {
            findNext();
            return keys[position()];
        }
    }

    /**
     * Iterator over entries which can provide unboxed access and optionally avoid allocation.
     */
    public final class EntryIterator
        extends AbstractIterator<Entry<K, Long>>
        implements Entry<K, Long>
    {
        /**
         * {@inheritDoc}
         */
        public Entry<K, Long> next()
        {
            findNext();
            if (shouldAvoidAllocation)
            {
                return this;
            }

            return allocateDuplicateEntry();
        }

        private Entry<K, Long> allocateDuplicateEntry()
        {
            return new MapEntry(getKey(), getLongValue());
        }

        /**
         * {@inheritDoc}
         */
        public K getKey()
        {
            return keys[position()];
        }

        /**
         * Get long value without boxing.
         *
         * @return value.
         */
        public long getLongValue()
        {
            return values[position()];
        }

        /**
         * {@inheritDoc}
         */
        public Long getValue()
        {
            return getLongValue();
        }

        /**
         * {@inheritDoc}
         */
        public Long setValue(final Long value)
        {
            return setValue((long)value);
        }

        /**
         * Set value at current position without boxing.
         *
         * @param value to be set.
         * @return old value.
         * @throws IllegalArgumentException if {@code missingValue == value}.
         */
        public long setValue(final long value)
        {
            if (missingValue == value)
            {
                throw new IllegalArgumentException("cannot accept missingValue");
            }

            @DoNotSub final int pos = position();
            final long oldValue = values[pos];
            values[pos] = value;

            return oldValue;
        }

        /**
         * {@inheritDoc}
         */
        @DoNotSub public int hashCode()
        {
            return getKey().hashCode() ^ Long.hashCode(getLongValue());
        }

        /**
         * {@inheritDoc}
         */
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Entry))
            {
                return false;
            }

            final Entry<?, ?> e = (Entry<?, ?>)o;
            return Objects.equals(getKey(), e.getKey()) && e.getValue() instanceof Long &&
                getLongValue() == (Long)e.getValue();
        }

        /**
         * An {@link java.util.Map.Entry} implementation.
         */
        public final class MapEntry implements Entry<K, Long>
        {
            private final K k;
            private long v;

            /**
             * @param k key.
             * @param v value.
             */
            public MapEntry(final K k, final long v)
            {
                this.k = k;
                this.v = v;
            }

            /**
             * {@inheritDoc}
             */
            public K getKey()
            {
                return k;
            }

            /**
             * {@inheritDoc}
             */
            public Long getValue()
            {
                return v;
            }

            /**
             * {@inheritDoc}
             */
            public Long setValue(final Long value)
            {
                final Long oldValue = Object2LongHashMap.this.put(k, value);
                v = value;
                return oldValue;
            }

            /**
             * {@inheritDoc}
             */
            @DoNotSub public int hashCode()
            {
                return getKey().hashCode() ^ Long.hashCode(v);
            }

            /**
             * {@inheritDoc}
             */
            public boolean equals(final Object o)
            {
                if (this == o)
                {
                    return true;
                }
                if (!(o instanceof Entry))
                {
                    return false;
                }

                final Entry<?, ?> e = (Entry<?, ?>)o;
                return Objects.equals(getKey(), e.getKey()) && e.getValue() instanceof Long &&
                    v == (Long)e.getValue();
            }

            /**
             * {@inheritDoc}
             */
            public String toString()
            {
                return k + "=" + v;
            }
        }
    }
}
