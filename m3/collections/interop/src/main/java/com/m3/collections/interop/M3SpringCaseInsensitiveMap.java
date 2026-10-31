/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0. */
package com.m3.collections.interop;

import com.m3.collections.M3LinkedHashMap;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Case-insensitive ordered Map semantics over two existing M3 indexed maps.
 *
 * <p>The authoritative payload references and encounter ordering belong to
 * M3LinkedHashMap. A second M3LinkedHashMap maps normalized UTF-16 keys to the
 * currently stored representative. No node/Entry object is kept per element;
 * only public Map.Entry iterator projections may allocate. Key casing follows
 * Spring Framework 6.2 LinkedCaseInsensitiveMap's insertion/putIfAbsent rules.
 * The original Spring concrete class, subclass hooks and serialized form are
 * <em>not</em> emulated or named as implemented. Single-threaded only.
 * Null keys are rejected, null values are permitted.</p>
 *
 * <p>This is a source-level Map adapter with first-party Apache-2.0 code;
 * Spring remains an external oracle/dependency, and no Spring source is copied.
 * No M3 String interning or retained Java heap pointer is required.</p>
 */
public class M3SpringCaseInsensitiveMap<V> extends AbstractMap<String,V> {
    private final M3LinkedHashMap<String,V> values;
    private final M3LinkedHashMap<String,String> canonical;
    private final Locale locale;
    private transient Set<Entry<String,V>> entries;
    private transient Set<String> keys;
    private transient Collection<V> valueView;

    public M3SpringCaseInsensitiveMap() { this(Locale.getDefault()); }
    public M3SpringCaseInsensitiveMap(Locale locale) {
        this.locale = Objects.requireNonNull(locale, "locale");
        this.values = new M3LinkedHashMap<>();
        this.canonical = new M3LinkedHashMap<>();
    }
    public Locale getLocale() { return locale; }
    protected String convertKey(String key) { return Objects.requireNonNull(key, "key").toLowerCase(locale); }
    private String resident(Object query) {
        return query instanceof String s ? canonical.get(convertKey(s)) : null;
    }
    @Override public int size() { return values.size(); }
    @Override public boolean isEmpty() { return values.isEmpty(); }
    @Override public boolean containsKey(Object key) { return resident(key) != null; }
    @Override public boolean containsValue(Object value) { return values.containsValue(value); }
    @Override public V get(Object key) {
        String match = resident(key);
        return match == null ? null : values.get(match);
    }
    @Override public V getOrDefault(Object key, V defaultValue) {
        String match = resident(key);
        return match == null ? defaultValue : values.get(match);
    }
    /** Explicit put adopts the *new* caller spelling and moves a changed key to the end. */
    @Override public V put(String key, V value) {
        String folded = convertKey(key);
        String old = canonical.put(folded, key);
        V replaced = null;
        if (old != null && !old.equals(key)) replaced = values.remove(old);
        V previous = values.put(key, value);
        return replaced != null ? replaced : previous;
    }
    /** Existing spelling survives this operation, even for a null-valued mapping. */
    @Override public V putIfAbsent(String key, V value) {
        String folded = convertKey(key);
        String stored = canonical.putIfAbsent(folded, key);
        if (stored != null) {
            V existing = values.get(stored);
            if (existing != null) return existing;
            key = stored;
        }
        return values.putIfAbsent(key, value);
    }
    /** Callback receives the stored spelling when a null-valued mapping exists. */
    @Override public V computeIfAbsent(String key, Function<? super String, ? extends V> callback) {
        Objects.requireNonNull(callback, "callback");
        String folded = convertKey(key);
        String stored = canonical.putIfAbsent(folded, key);
        if (stored != null) {
            V existing = values.get(stored);
            if (existing != null) return existing;
            key = stored;
        }
        return values.computeIfAbsent(key, callback);
    }
    @Override public void putAll(Map<? extends String,? extends V> source) {
        Objects.requireNonNull(source,"source");
        if (source != this) source.forEach(this::put);
    }
    @Override public V remove(Object key) {
        if (!(key instanceof String s)) return null;
        String stored = canonical.remove(convertKey(s));
        return stored == null ? null : values.remove(stored);
    }
    @Override public void clear() { canonical.clear(); values.clear(); }

    @Override public Set<Entry<String,V>> entrySet() {
        if (entries == null) entries = new AbstractSet<>() {
            @Override public int size() { return values.size(); }
            @Override public void clear() { M3SpringCaseInsensitiveMap.this.clear(); }
            @Override public boolean contains(Object probe) {
                return probe instanceof Entry<?,?> e && values.entrySet().contains(e);
            }
            @Override public boolean remove(Object probe) {
                if (!contains(probe)) return false;
                var entry = (Entry<?,?>) probe;
                M3SpringCaseInsensitiveMap.this.remove(entry.getKey());
                return true;
            }
            @Override public Iterator<Entry<String,V>> iterator() {
                Iterator<Entry<String,V>> cursor = values.entrySet().iterator();
                return new Iterator<>() {
                    private String last;
                    private boolean removable;
                    @Override public boolean hasNext() { return cursor.hasNext(); }
                    @Override public Entry<String,V> next() {
                        Entry<String,V> next = cursor.next();
                        last = next.getKey();
                        removable = true;
                        return new Entry<>() {
                            @Override public String getKey() { return next.getKey(); }
                            @Override public V getValue() { return next.getValue(); }
                            @Override public V setValue(V v) { return next.setValue(v); }
                            @Override public boolean equals(Object item) { return next.equals(item); }
                            @Override public int hashCode() { return next.hashCode(); }
                        };
                    }
                    @Override public void remove() {
                        if (!removable) throw new IllegalStateException("no current entry");
                        cursor.remove();
                        canonical.remove(convertKey(last));
                        removable = false;
                    }
                };
            }
        };
        return entries;
    }
    @Override public Set<String> keySet() {
        if (keys == null) keys = new AbstractSet<>() {
            @Override public int size() { return values.size(); }
            @Override public void clear() { M3SpringCaseInsensitiveMap.this.clear(); }
            @Override public boolean contains(Object key) { return M3SpringCaseInsensitiveMap.this.containsKey(key); }
            @Override public boolean remove(Object key) {
                return M3SpringCaseInsensitiveMap.this.remove(key) != null;
            }
            @Override public Iterator<String> iterator() {
                Iterator<Entry<String,V>> wrapped = entrySet().iterator();
                return new Iterator<>() {
                    @Override public boolean hasNext() { return wrapped.hasNext(); }
                    @Override public String next() { return wrapped.next().getKey(); }
                    @Override public void remove() { wrapped.remove(); }
                };
            }
        };
        return keys;
    }
    @Override public Collection<V> values() {
        if (valueView == null) valueView = new AbstractCollection<>() {
            @Override public int size() { return values.size(); }
            @Override public void clear() { M3SpringCaseInsensitiveMap.this.clear(); }
            @Override public boolean contains(Object x) { return values.containsValue(x); }
            @Override public Iterator<V> iterator() {
                Iterator<Entry<String,V>> wrapped = entrySet().iterator();
                return new Iterator<>() {
                    @Override public boolean hasNext() { return wrapped.hasNext(); }
                    @Override public V next() { return wrapped.next().getValue(); }
                    @Override public void remove() { wrapped.remove(); }
                };
            }
        };
        return valueView;
    }
}
