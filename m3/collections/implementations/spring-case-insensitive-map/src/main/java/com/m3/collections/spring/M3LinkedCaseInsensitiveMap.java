/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modified 2026 by Hitesh Soliwal and contributors: M3 array-owned storage and nominal type.
// Storage atoms retain their Apache-2.0 M3/Synexia attribution.
package com.m3.collections.spring;

import java.io.Serializable;
import java.io.Serial;
import java.util.Arrays;
import java.util.Objects;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Array-backed map that stores String keys in a case-insensitive manner,
 * for example for key-based access in a results table.
 *
 * <p>Preserves the original order as well as the original casing of keys,
 * while allowing for contains, get and remove calls with any case of key.
 *
 * <p>Does <i>not</i> support {@code null} keys.
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.0
 * @param <V> the value type
 */
@SuppressWarnings("serial")
public class M3LinkedCaseInsensitiveMap<V> implements Map<String, V>, Serializable, Cloneable {

	@Serial private static final long serialVersionUID = 1L;

	private final Table<V> targetMap;

	private final Table<String> caseInsensitiveKeys;

	private final Locale locale;

	private transient volatile Set<String> keySet;

	private transient volatile Collection<V> values;

	private transient volatile Set<Entry<String, V>> entrySet;


	/**
	 * Create a new M3LinkedCaseInsensitiveMap that stores case-insensitive keys
	 * according to the default Locale (by default in lower case).
	 * @see #convertKey(String)
	 */
	public M3LinkedCaseInsensitiveMap() {
		this((Locale) null);
	}

	/**
	 * Create a new M3LinkedCaseInsensitiveMap that stores case-insensitive keys
	 * according to the given Locale (in lower case).
	 * @param locale the Locale to use for case-insensitive key conversion
	 * @see #convertKey(String)
	 */
	public M3LinkedCaseInsensitiveMap(Locale locale) {
		this(12, locale);  // equivalent to LinkedHashMap's initial capacity of 16
	}

	/**
	 * Create a new array-backed M3LinkedCaseInsensitiveMap with the specified
	 * expected size as its initial capacity hint,
	 * storing case-insensitive keys according to the default Locale (in lower case).
	 * @param expectedSize the expected number of elements (with a corresponding
	 * capacity to be derived so that no resize/rehash operations are needed)
	 * @see #convertKey(String)
	 */
	public M3LinkedCaseInsensitiveMap(int expectedSize) {
		this(expectedSize, null);
	}

	/**
	 * Create a new array-backed M3LinkedCaseInsensitiveMap with the specified
	 * expected size as its initial capacity hint,
	 * storing case-insensitive keys according to the given Locale (in lower case).
	 * @param expectedSize the expected number of elements (with a corresponding
	 * capacity to be derived so that no resize/rehash operations are needed)
	 * @param locale the Locale to use for case-insensitive key conversion
	 * @see #convertKey(String)
	 */
	public M3LinkedCaseInsensitiveMap(int expectedSize, Locale locale) {
        int targetCapacity = (int) (expectedSize / 0.75f);
        if (targetCapacity < 0) throw new IllegalArgumentException("Illegal initial capacity: " + targetCapacity);
        int aliasCapacity = (int) Math.ceil(expectedSize / (double) 0.75f);
        if (aliasCapacity < 0) throw new IllegalArgumentException("Illegal initial capacity: " + aliasCapacity);
        this.targetMap = new Table<>(targetCapacity, this);
        this.caseInsensitiveKeys = new Table<>(aliasCapacity, null);
        this.locale = locale != null ? locale : Locale.getDefault();
    }

	/**
	 * Copy constructor.
	 */
	@SuppressWarnings("unchecked")
	private M3LinkedCaseInsensitiveMap(M3LinkedCaseInsensitiveMap<V> other) {
		this.targetMap = other.targetMap.copy();
		this.caseInsensitiveKeys = other.caseInsensitiveKeys.copy();
		this.locale = other.locale;
	}


	// Implementation of java.util.Map

	@Override
	public int size() {
		return this.targetMap.size();
	}

	@Override
	public boolean isEmpty() {
		return this.targetMap.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return (key instanceof String string && this.caseInsensitiveKeys.containsKey(convertKey(string)));
	}

	@Override
	public boolean containsValue(Object value) {
		return this.targetMap.containsValue(value);
	}

	@Override
	public V get(Object key) {
		if (key instanceof String string) {
			String caseInsensitiveKey = this.caseInsensitiveKeys.get(convertKey(string));
			if (caseInsensitiveKey != null) {
				return this.targetMap.get(caseInsensitiveKey);
			}
		}
		return null;
	}

	@Override
	public V getOrDefault(Object key, V defaultValue) {
		if (key instanceof String string) {
			String caseInsensitiveKey = this.caseInsensitiveKeys.get(convertKey(string));
			if (caseInsensitiveKey != null) {
				return this.targetMap.get(caseInsensitiveKey);
			}
		}
		return defaultValue;
	}

	@Override
	public V put(String key, V value) {
		String oldKey = this.caseInsensitiveKeys.put(convertKey(key), key);
		V oldKeyValue = null;
		if (oldKey != null && !oldKey.equals(key)) {
			oldKeyValue = this.targetMap.remove(oldKey);
		}
		V oldValue = this.targetMap.put(key, value);
		return (oldKeyValue != null ? oldKeyValue : oldValue);
	}

	@Override
	public void putAll(Map<? extends String, ? extends V> map) {
		if (map.isEmpty()) {
			return;
		}
		map.forEach(this::put);
	}

	@Override
	public V putIfAbsent(String key, V value) {
		String oldKey = this.caseInsensitiveKeys.putIfAbsent(convertKey(key), key);
		if (oldKey != null) {
			V oldKeyValue = this.targetMap.get(oldKey);
			if (oldKeyValue != null) {
				return oldKeyValue;
			}
			else {
				key = oldKey;
			}
		}
		return this.targetMap.putIfAbsent(key, value);
	}

	@Override
	public V computeIfAbsent(String key, Function<? super String, ? extends V> mappingFunction) {
		String oldKey = this.caseInsensitiveKeys.putIfAbsent(convertKey(key), key);
		if (oldKey != null) {
			V oldKeyValue = this.targetMap.get(oldKey);
			if (oldKeyValue != null) {
				return oldKeyValue;
			}
			else {
				key = oldKey;
			}
		}
		return this.targetMap.computeIfAbsent(key, mappingFunction);
	}

	@Override
	public V remove(Object key) {
		if (key instanceof String string) {
			String caseInsensitiveKey = removeCaseInsensitiveKey(string);
			if (caseInsensitiveKey != null) {
				return this.targetMap.remove(caseInsensitiveKey);
			}
		}
		return null;
	}

	@Override
	public void clear() {
		this.caseInsensitiveKeys.clear();
		this.targetMap.clear();
	}

	@Override
	public Set<String> keySet() {
		Set<String> keySet = this.keySet;
		if (keySet == null) {
			keySet = new KeySet(this.targetMap.keySet());
			this.keySet = keySet;
		}
		return keySet;
	}

	@Override
	public Collection<V> values() {
		Collection<V> values = this.values;
		if (values == null) {
			values = new Values(this.targetMap.values());
			this.values = values;
		}
		return values;
	}

	@Override
	public Set<Entry<String, V>> entrySet() {
		Set<Entry<String, V>> entrySet = this.entrySet;
		if (entrySet == null) {
			entrySet = new EntrySet(this.targetMap.entrySet());
			this.entrySet = entrySet;
		}
		return entrySet;
	}

	@Override
	public void forEach(BiConsumer<? super String, ? super V> action) {
		this.targetMap.forEach(action);
	}

	@Override
	public M3LinkedCaseInsensitiveMap<V> clone() {
		return new M3LinkedCaseInsensitiveMap<>(this);
	}

	@Override
	public boolean equals(Object other) {
		return (this == other || this.targetMap.equals(other));
	}

	@Override
	public int hashCode() {
		return this.targetMap.hashCode();
	}

	@Override
	public String toString() {
		return this.targetMap.toString();
	}


	// Specific to M3LinkedCaseInsensitiveMap

	/**
	 * Return the locale used by this {@code M3LinkedCaseInsensitiveMap}.
	 * Used for case-insensitive key conversion.
	 * @since 4.3.10
	 * @see #M3LinkedCaseInsensitiveMap(Locale)
	 * @see #convertKey(String)
	 */
	public Locale getLocale() {
		return this.locale;
	}

	/**
	 * Convert the given key to a case-insensitive key.
	 * <p>The default implementation converts the key
	 * to lower-case according to this Map's Locale.
	 * @param key the user-specified key
	 * @return the key to use for storing
	 * @see String#toLowerCase(Locale)
	 */
	protected String convertKey(String key) {
		return key.toLowerCase(getLocale());
	}

	/**
	 * Determine whether this map should remove the given eldest entry.
	 * @param eldest the candidate entry
	 * @return {@code true} for removing it, {@code false} for keeping it
	 * @see java.util.LinkedHashMap#removeEldestEntry
	 */
	protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
		return false;
	}

	private String removeCaseInsensitiveKey(String key) {
		return this.caseInsensitiveKeys.remove(convertKey(key));
	}


	private class KeySet extends AbstractSet<String> {

		private final Set<String> delegate;

		KeySet(Set<String> delegate) {
			this.delegate = delegate;
		}

		@Override
		public int size() {
			return this.delegate.size();
		}

		@Override
		public boolean contains(Object o) {
			return this.delegate.contains(o);
		}

		@Override
		public Iterator<String> iterator() {
			return new KeySetIterator();
		}

		@Override
		public boolean remove(Object o) {
			return M3LinkedCaseInsensitiveMap.this.remove(o) != null;
		}

		@Override
		public void clear() {
			M3LinkedCaseInsensitiveMap.this.clear();
		}

		@Override
		public Spliterator<String> spliterator() {
			return this.delegate.spliterator();
		}

		@Override
		public void forEach(Consumer<? super String> action) {
			this.delegate.forEach(action);
		}
	}


	private class Values extends AbstractCollection<V> {

		private final Collection<V> delegate;

		Values(Collection<V> delegate) {
			this.delegate = delegate;
		}

		@Override
		public int size() {
			return this.delegate.size();
		}

		@Override
		public boolean contains(Object o) {
			return this.delegate.contains(o);
		}

		@Override
		public Iterator<V> iterator() {
			return new ValuesIterator();
		}

		@Override
		public void clear() {
			M3LinkedCaseInsensitiveMap.this.clear();
		}

		@Override
		public Spliterator<V> spliterator() {
			return this.delegate.spliterator();
		}

		@Override
		public void forEach(Consumer<? super V> action) {
			this.delegate.forEach(action);
		}
	}


	private class EntrySet extends AbstractSet<Entry<String, V>> {

		private final Set<Entry<String, V>> delegate;

		public EntrySet(Set<Entry<String, V>> delegate) {
			this.delegate = delegate;
		}

		@Override
		public int size() {
			return this.delegate.size();
		}

		@Override
		public boolean contains(Object o) {
			return this.delegate.contains(o);
		}

		@Override
		public Iterator<Entry<String, V>> iterator() {
			return new EntrySetIterator();
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean remove(Object o) {
			if (this.delegate.remove(o)) {
				removeCaseInsensitiveKey(((Map.Entry<String, V>) o).getKey());
				return true;
			}
			return false;
		}

		@Override
		public void clear() {
			this.delegate.clear();
			caseInsensitiveKeys.clear();
		}

		@Override
		public Spliterator<Entry<String, V>> spliterator() {
			return this.delegate.spliterator();
		}

		@Override
		public void forEach(Consumer<? super Entry<String, V>> action) {
			this.delegate.forEach(action);
		}
	}


    private abstract class EntryIterator<T> implements Iterator<T> {
        private final Table<V>.Cursor delegate = targetMap.cursor();
        private String lastKey;
        private boolean lastPresent;
        protected int nextSlot() {
            int slot = delegate.nextSlot(); lastKey = targetMap.keyAt(slot); lastPresent = true; return slot;
        }
        @Override public boolean hasNext() { return delegate.hasNext(); }
        @Override public void remove() {
            delegate.remove();
            if (lastPresent) { removeCaseInsensitiveKey(lastKey); lastKey = null; lastPresent = false; }
        }
    }
    private class KeySetIterator extends EntryIterator<String> {
        @Override public String next() { return targetMap.keyAt(nextSlot()); }
    }
    private class ValuesIterator extends EntryIterator<V> {
        @Override public V next() { return targetMap.valueAt(nextSlot()); }
    }
    private class EntrySetIterator extends EntryIterator<Entry<String, V>> {
        @Override public Entry<String, V> next() { return targetMap.entryAt(nextSlot()); }
    }

    // Adapted from Apache-2.0 M3LinkedHashMap.Table/M3SlotArrays/M3SlotMap.
    // Payloads and aliases have separate instances; no stored Entry objects.
    private static final class Table<T> implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private static final byte LIVE = 1;
        private final int initialCapacity;
        private final M3LinkedCaseInsensitiveMap<T> hookOwner;
        private Data data = new Data();
        private transient int modifications;
        private transient Set<String> keysView;
        private transient Collection<T> valuesView;
        private transient Set<Entry<String, T>> entriesView;

        Table(int capacity, M3LinkedCaseInsensitiveMap<T> hookOwner) {
            this.initialCapacity = capacity;
            this.hookOwner = hookOwner;
        }

        // The bundle changes on clear; growth updates its array lanes in place.
        // Active callback frames retain the old clear-generation, not Entry nodes.
        private static final class Data implements Serializable {
            @Serial private static final long serialVersionUID = 1L;
            private Object[] keys = new Object[0], values = new Object[0];
            private int[] hashes = new int[0], before = new int[0], after = new int[0],
                    positions = new int[0], freeNext = new int[0], generations = new int[0];
            private byte[] states = new byte[0];
            private int[] index = new int[0];
            private int first = -1, last = -1, free = -1, used, occupied, size;

            Data copy() {
                Data result = new Data();
                result.keys = keys.clone(); result.values = values.clone();
                result.hashes = hashes.clone(); result.before = before.clone(); result.after = after.clone();
                result.positions = positions.clone(); result.freeNext = freeNext.clone();
                result.generations = generations.clone(); result.states = states.clone();
                result.index = index.clone(); result.first = first; result.last = last;
                result.free = free; result.used = used; result.occupied = occupied; result.size = size;
                return result;
            }
        }

        private static int hash(Object key) {
            int h = key == null ? 0 : key.hashCode();
            return h ^ h >>> 16;
        }
        private static int tableCapacity(int requested) {
            if (requested <= 2) return 2;
            if (requested >= 1 << 30) return 1 << 30;
            return Integer.highestOneBit(requested - 1) << 1;
        }
        private static int growCapacity(int old) {
            if (old >= Integer.MAX_VALUE - 8) throw new OutOfMemoryError("collection capacity exhausted");
            return (int) Math.min(Integer.MAX_VALUE - 8L, Math.max(8L, old + (old >> 1) + 1L));
        }
        private static int doubled(int old) {
            if (old >= 1 << 30) throw new OutOfMemoryError("hash index capacity exhausted");
            return old << 1;
        }
        private int find(Object key, int hash) {
            Data d = data;
            if (d.index.length == 0) return -1;
            for (int p = hash & (d.index.length - 1); d.index[p] != 0; p = (p + 1) & (d.index.length - 1)) {
                int s = d.index[p] - 1;
                if (s >= 0 && d.hashes[s] == hash && Objects.equals(key, d.keys[s])) return s;
            }
            return -1;
        }
        private int vacant(int hash) {
            Data d = data;
            int p = hash & (d.index.length - 1);
            while (d.index[p] > 0) p = (p + 1) & (d.index.length - 1);
            return p;
        }
        private void rehash(int capacity) {
            Data d = data;
            int[] replacement = new int[capacity];
            for (int s = d.first; s >= 0; s = d.after[s]) {
                int p = d.hashes[s] & (capacity - 1);
                while (replacement[p] != 0) p = (p + 1) & (capacity - 1);
                replacement[p] = s + 1; d.positions[s] = p;
            }
            d.index = replacement; d.occupied = d.size;
        }
        private void grow() {
            Data d = data;
            int n = d.keys.length == 0
                    ? (int) Math.max(8L, Math.min(Integer.MAX_VALUE - 8L, initialCapacity * 3L / 4))
                    : growCapacity(d.keys.length);
            Object[] keys = Arrays.copyOf(d.keys, n), values = Arrays.copyOf(d.values, n);
            int[] hashes = Arrays.copyOf(d.hashes, n), before = Arrays.copyOf(d.before, n),
                    after = Arrays.copyOf(d.after, n), positions = Arrays.copyOf(d.positions, n),
                    freeNext = Arrays.copyOf(d.freeNext, n), generations = Arrays.copyOf(d.generations, n);
            byte[] states = Arrays.copyOf(d.states, n);
            d.keys = keys; d.values = values; d.hashes = hashes; d.before = before; d.after = after;
            d.positions = positions; d.freeNext = freeNext; d.generations = generations; d.states = states;
        }
        private void link(int s) {
            Data d = data;
            d.before[s] = d.last; d.after[s] = -1;
            if (d.last < 0) d.first = s; else d.after[d.last] = s;
            d.last = s;
        }
        private void unlink(int s) {
            Data d = data;
            int b = d.before[s], a = d.after[s];
            if (b < 0) d.first = a; else d.after[b] = a;
            if (a < 0) d.last = b; else d.before[a] = b;
        }
        private void insert(String key, T value, int hash) {
            Data d = data;
            if (d.free < 0 && d.used == d.keys.length) grow();
            if (d.index.length == 0) rehash(tableCapacity(initialCapacity));
            else {
                int limit = (d.index.length >>> 1) + (d.index.length >>> 2);
                if (d.occupied + 1 > limit) rehash(d.size + 1 > limit ? doubled(d.index.length) : d.index.length);
            }
            int p = vacant(hash), s;
            if (d.free < 0) s = d.used++;
            else { s = d.free; d.free = d.freeNext[s]; }
            if (d.index[p] == 0) d.occupied++;
            d.keys[s] = key; d.values[s] = value; d.hashes[s] = hash;
            d.index[p] = s + 1; d.positions[s] = p; d.states[s] = LIVE; d.generations[s]++;
            link(s); d.size++; modifications++;
            if (hookOwner != null) {
                int eldest = d.first;
                String eldestKey = keyAt(eldest);
                if (hookOwner.removeEldestEntry(entryAt(eldest))) {
                    hookOwner.removeCaseInsensitiveKey(eldestKey);
                    remove(eldestKey);
                }
            }
        }
        private T erase(int s) {
            Data d = data;
            T previous = valueAt(s);
            unlink(s); d.index[d.positions[s]] = -1;
            d.keys[s] = null; d.values[s] = null; d.states[s] = 0;
            d.before[s] = -1; d.after[s] = -1; d.positions[s] = -1;
            d.freeNext[s] = d.free; d.free = s; d.size--; modifications++;
            return previous;
        }
        private String keyAt(int s) { return (String) data.keys[s]; }
        @SuppressWarnings("unchecked") private T valueAt(int s) { return (T) data.values[s]; }
        @SuppressWarnings("unchecked") private T valueAt(Data frame, int s) { return (T) frame.values[s]; }
        private T writeAt(int s, T value) {
            T previous = valueAt(s); data.values[s] = value; return previous;
        }
        int size() { return data.size; }
        boolean isEmpty() { return data.size == 0; }
        boolean containsKey(Object key) {
            return hookOwner == null ? find(key, hash(key)) >= 0 : hookOwner.containsKey(key);
        }
        boolean containsValue(Object value) {
            Data frame = data;
            for (int s = frame.first; s >= 0; s = frame.after[s]) {
                if (Objects.equals(value, frame.values[s])) return true;
            }
            return false;
        }
        T get(Object key) {
            int s = find(key, hash(key)); return s < 0 ? null : valueAt(s);
        }
        T put(String key, T value) {
            int h = hash(key), s = find(key, h);
            if (s >= 0) return writeAt(s, value);
            insert(key, value, h); return null;
        }
        T putIfAbsent(String key, T value) {
            int h = hash(key), s = find(key, h);
            if (s < 0) { insert(key, value, h); return null; }
            T previous = valueAt(s);
            if (previous == null) writeAt(s, value);
            return previous;
        }
        T computeIfAbsent(String key, Function<? super String, ? extends T> action) {
            Objects.requireNonNull(action);
            int h = hash(key), s = find(key, h);
            T previous = s < 0 ? null : valueAt(s);
            if (previous != null) return previous;
            int expected = modifications;
            T next = action.apply(key);
            if (modifications != expected) throw new ConcurrentModificationException();
            if (next != null) {
                if (s >= 0) writeAt(s, next); else insert(key, next, h);
            }
            return next;
        }
        T remove(Object key) {
            int s = find(key, hash(key)); return s < 0 ? null : erase(s);
        }
        void clear() { data = new Data(); modifications++; }
        Table<T> copy() {
            Table<T> result = new Table<>(initialCapacity, hookOwner);
            result.data = data.copy(); return result;
        }

        // API facades are temporary outputs. The owner never retains one.
        private final class ApiEntry implements Entry<String, T> {
            private final Data frame;
            private final int slot, generation;
            private final String key;
            private T detachedValue;
            ApiEntry(Data frame, int slot) {
                this.frame = frame; this.slot = slot; this.generation = frame.generations[slot];
                this.key = (String) frame.keys[slot]; this.detachedValue = valueAt(frame, slot);
            }
            private boolean attached() {
                return (frame.states[slot] & LIVE) != 0 && frame.generations[slot] == generation;
            }
            @Override public String getKey() { return key; }
            @Override public T getValue() { return attached() ? valueAt(frame, slot) : detachedValue; }
            @Override public T setValue(T value) {
                T previous = getValue();
                if (attached()) frame.values[slot] = value;
                detachedValue = value; return previous;
            }
            @Override public boolean equals(Object other) {
                return other instanceof Entry<?, ?> e && Objects.equals(key, e.getKey()) && Objects.equals(getValue(), e.getValue());
            }
            @Override public int hashCode() { return Objects.hashCode(key) ^ Objects.hashCode(getValue()); }
            @Override public String toString() { return key + "=" + getValue(); }
        }
        private Entry<String, T> entryAt(int s) { return new ApiEntry(data, s); }

        private final class Cursor {
            private int next = data.first, current = -1, expected = modifications;
            boolean hasNext() { return next >= 0; }
            int nextSlot() {
                if (expected != modifications) throw new ConcurrentModificationException();
                if (next < 0) throw new NoSuchElementException();
                current = next; next = data.after[current]; return current;
            }
            void remove() {
                if (current < 0) throw new IllegalStateException();
                if (expected != modifications) throw new ConcurrentModificationException();
                erase(current); current = -1; expected = modifications;
            }
        }
        private Cursor cursor() { return new Cursor(); }
        private <E> Iterator<E> iterator(int kind) {
            Cursor cursor = cursor();
            return new Iterator<>() {
                @Override public boolean hasNext() { return cursor.hasNext(); }
                @SuppressWarnings("unchecked") @Override public E next() {
                    int s = cursor.nextSlot();
                    return (E) (kind == 0 ? keyAt(s) : kind == 1 ? valueAt(s) : entryAt(s));
                }
                @Override public void remove() { cursor.remove(); }
            };
        }
        @FunctionalInterface private interface SlotAction { void accept(Data frame, int s); }
        private void traverse(SlotAction action) {
            Data frame = data; int expected = modifications;
            for (int s = frame.first; s >= 0; s = frame.after[s]) action.accept(frame, s);
            if (modifications != expected) throw new ConcurrentModificationException();
        }
        void forEach(BiConsumer<? super String, ? super T> action) {
            Objects.requireNonNull(action);
            traverse((frame, s) -> action.accept((String) frame.keys[s], valueAt(frame, s)));
        }
        Set<String> keySet() {
            if (keysView == null) keysView = new AbstractSet<>() {
                @Override public int size() { return Table.this.size(); }
                @Override public boolean contains(Object key) { return containsKey(key); }
                @Override public Iterator<String> iterator() { return Table.this.iterator(0); }
                @Override public Spliterator<String> spliterator() {
                    return java.util.Spliterators.spliterator(this, Spliterator.ORDERED | Spliterator.DISTINCT);
                }
                @Override public void forEach(Consumer<? super String> action) {
                    Objects.requireNonNull(action);
                    traverse((frame, s) -> action.accept((String) frame.keys[s]));
                }
            };
            return keysView;
        }
        Collection<T> values() {
            if (valuesView == null) valuesView = new AbstractCollection<>() {
                @Override public int size() { return Table.this.size(); }
                @Override public boolean contains(Object value) { return containsValue(value); }
                @Override public Iterator<T> iterator() { return Table.this.iterator(1); }
                @Override public Spliterator<T> spliterator() {
                    return java.util.Spliterators.spliterator(this, Spliterator.ORDERED);
                }
                @Override public void forEach(Consumer<? super T> action) {
                    Objects.requireNonNull(action);
                    traverse((frame, s) -> action.accept(valueAt(frame, s)));
                }
            };
            return valuesView;
        }
        Set<Entry<String, T>> entrySet() {
            if (entriesView == null) entriesView = new AbstractSet<>() {
                @Override public int size() { return Table.this.size(); }
                @Override public void clear() { Table.this.clear(); }
                @Override public Iterator<Entry<String, T>> iterator() { return Table.this.iterator(2); }
                @Override public boolean contains(Object candidate) {
                    if (!(candidate instanceof Entry<?, ?> e)) return false;
                    Object key = e.getKey();
                    int s = find(key, hash(key));
                    return s >= 0 && Objects.equals(data.keys[s], e.getKey()) && Objects.equals(valueAt(s), e.getValue());
                }
                @Override public boolean remove(Object candidate) {
                    if (!(candidate instanceof Entry<?, ?> e)) return false;
                    Object key = e.getKey();
                    Object value = e.getValue();
                    int s = find(key, hash(key));
                    if (s < 0 || !Objects.equals(value, valueAt(s))) return false;
                    erase(s); return true;
                }
                @Override public Spliterator<Entry<String, T>> spliterator() {
                    return java.util.Spliterators.spliterator(this, Spliterator.ORDERED | Spliterator.DISTINCT);
                }
                @Override public void forEach(Consumer<? super Entry<String, T>> action) {
                    Objects.requireNonNull(action);
                    traverse((frame, s) -> action.accept(new ApiEntry(frame, s)));
                }
            };
            return entriesView;
        }
        @Override public int hashCode() {
            int hash = 0; Cursor cursor = cursor();
            while (cursor.hasNext()) {
                int s = cursor.nextSlot(); hash += Objects.hashCode(keyAt(s)) ^ Objects.hashCode(valueAt(s));
            }
            return hash;
        }
        @Override public boolean equals(Object other) {
            if (other == this) return true;
            if (!(other instanceof Map<?, ?> map) || map.size() != size()) return false;
            try {
                Cursor cursor = cursor();
                while (cursor.hasNext()) {
                    int s = cursor.nextSlot(); String key = keyAt(s); T value = valueAt(s);
                    if (value == null) {
                        if (map.get(key) != null || !map.containsKey(key)) return false;
                    } else if (!value.equals(map.get(key))) return false;
                }
            } catch (ClassCastException | NullPointerException ignored) { return false; }
            return true;
        }
        @Override public String toString() {
            StringBuilder text = new StringBuilder("{"); Cursor cursor = cursor(); boolean comma = false;
            while (cursor.hasNext()) {
                int s = cursor.nextSlot();
                if (comma) text.append(", "); comma = true;
                text.append(keyAt(s)).append('=').append(valueAt(s));
            }
            return text.append('}').toString();
        }
    }

}
