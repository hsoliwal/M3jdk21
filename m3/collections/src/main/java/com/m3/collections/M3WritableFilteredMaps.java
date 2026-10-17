// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Explicit mutable, live, filtered Map and NavigableMap decorators.
 *
 * <p>Read semantics are delegated to the existing Synexia {@link M3FilteredMaps}
 * owner. Only admitted mappings are presented. Mutations of existing hidden entries
 * are suppressed unless a new accepted {@code put} deliberately exposes that key.
 * Each filtered iterator has one lookahead candidate and does not support remove;
 * explicit bulk operations on entry/key/value views remove visible mappings only.
 * No reference array, entry node, hash table or key/value payload is retained by
 * this facade. Optional transient arrays during batch preflight are released.
 *
 * <p>Predicates must be deterministic, equality-consistent, and not modify the
 * backing map. Source iterators retain their own concurrency/fail-fast contract.
 * This is not a transaction, thread-safe map or serializable adapter. Backing
 * provider failure during a sequence of admitted writes may leave a prefix.
 * Comparator navigation delegates to the existing O(n) filtered read view;
 * there is no claim of faster seeking or JNI predicate execution.
 */
public final class M3WritableFilteredMaps {
    private M3WritableFilteredMaps() { }

    /** Mutations write through, but hidden entries are not removed. */
    public static <K,V> Map<K,V> filtered(
            Map<K,V> source, BiPredicate<? super K,? super V> predicate) {
        return new MutableMap<>(source, predicate);
    }

    /** Same semantics, retaining live comparator, range and descending behavior. */
    public static <K,V> NavigableMap<K,V> filtered(
            NavigableMap<K,V> source, BiPredicate<? super K,? super V> predicate) {
        return new MutableNavigableMap<>(source, predicate);
    }

    private static class MutableMap<K,V> extends AbstractMap<K,V> {
        final Map<K,V> source;
        final BiPredicate<? super K,? super V> predicate;
        final Map<K,V> visibleRead;
        private transient Set<Entry<K,V>> entries;
        private transient Set<K> keys;
        private transient Collection<V> values;

        MutableMap(Map<K,V> source, BiPredicate<? super K,? super V> predicate) {
            this.source = Objects.requireNonNull(source, "source");
            this.predicate = Objects.requireNonNull(predicate, "predicate");
            visibleRead = M3FilteredMaps.filtered(source, predicate);
        }
        @Override public int size() { return visibleRead.size(); }
        @Override public boolean isEmpty() { return visibleRead.isEmpty(); }
        @Override public boolean containsKey(Object key) { return visibleRead.containsKey(key); }
        @Override public boolean containsValue(Object value) { return visibleRead.containsValue(value); }
        @Override public V get(Object key) { return visibleRead.get(key); }

        private void requireVisible(K key, V value) {
            if (!predicate.test(key, value)) {
                throw new IllegalArgumentException("mapping rejected by filtered view");
            }
        }
        /**
         * The existing TreeMap key survives a comparator-equivalent put.
         * Validate the key actually retained by the owner, not the caller alias.
         * A malformed lookup retains the original predicate-then-put failure path.
         */
        private K keyForScalarWrite(K key) {
            if (source instanceof SortedMap<?,?>) {
                try {
                    if (source.containsKey(key)) {
                        return M3FilteredMaps.storedRepresentativeKey(source, key);
                    }
                } catch (ClassCastException | NullPointerException invalidKey) {
                    // Original key-validation priority remains authoritative.
                }
            }
            return key;
        }
        @Override public V put(K key, V value) {
            requireVisible(keyForScalarWrite(key), value);
            V previous = get(key);
            source.put(key, value);
            return previous;
        }
        @Override public V remove(Object key) {
            if (!containsKey(key)) return null;
            return source.remove(key);
        }
        @Override public void clear() { removeMatching((k,v) -> true); }

        /**
         * Store only temporary alternating key/value references, not entry wrappers.
         * All predicates are checked before the first source write. The source may
         * itself be a filtered view and can be safely iterated only once.
         */
        private void publishPairs(Object[] pairs, int used) {
            for (int at=0;at<used;at+=2) {
                @SuppressWarnings("unchecked") K key=(K)pairs[at];
                @SuppressWarnings("unchecked") V value=(V)pairs[at+1];
                source.put(key,value);
            }
        }
        private static Object[] appendPair(Object[] pairs, int used, Object key, Object value) {
            if (used > Integer.MAX_VALUE-2) throw new IllegalArgumentException("too many map pairs");
            if (used+2 > pairs.length) {
                int next=(int)Math.min((long)Integer.MAX_VALUE-8, Math.max(8L,(long)pairs.length*2));
                if (next < used+2) throw new IllegalArgumentException("too many map pairs");
                pairs=Arrays.copyOf(pairs,next);
            }
            pairs[used]=key; pairs[used+1]=value; return pairs;
        }
        /**
         * The first inserted key in each SortedMap comparator class is retained.
         * Source keys take precedence; for as-yet-absent keys use a primitive
         * AVL index of previously admitted (but not yet published) batch pairs.
         * All predicates run before publication. Generic Map owners are unchanged.
         */
        @Override public void putAll(Map<? extends K,? extends V> candidate) {
            Objects.requireNonNull(candidate, "candidate");
            Object[] pairs = new Object[0]; int used=0;
            if (source instanceof SortedMap<?,?> sortedSource) {
                @SuppressWarnings("unchecked")
                SortedMap<K,V> sorted=(SortedMap<K,V>) sortedSource;
                M3PendingSortedKeys<K> pending = new M3PendingSortedKeys<>(sorted.comparator());
                for (Entry<? extends K,? extends V> e : candidate.entrySet()) {
                    K key=e.getKey(); V value=e.getValue(); K retained=key;
                    boolean firstPending=false;
                    try {
                        if (source.containsKey(key)) {
                            retained=M3FilteredMaps.storedRepresentativeKey(source,key);
                        } else {
                            int offset=pending.firstOffset(pairs,key);
                            if (offset>=0) {
                                @SuppressWarnings("unchecked") K first=(K)pairs[offset];
                                retained=first;
                            } else {
                                firstPending=true;
                            }
                        }
                    } catch (ClassCastException | NullPointerException invalidKey) {
                        // Retain the original caller-key predicate priority for invalid keys.
                        firstPending=false;
                    }
                    requireVisible(retained,value);
                    pairs=appendPair(pairs,used,key,value);
                    if (firstPending) pending.recordFirst(pairs,used);
                    used+=2;
                }
            } else {
                for (Entry<? extends K,? extends V> e : candidate.entrySet()) {
                    K key=e.getKey(); V value=e.getValue(); requireVisible(key,value);
                    pairs=appendPair(pairs,used,key,value); used+=2;
                }
            }
            publishPairs(pairs,used);
        }
        /** Callback/predicate errors are preflighted before committing any replacement. */
        @Override public void replaceAll(BiFunction<? super K,? super V,? extends V> function) {
            Objects.requireNonNull(function, "function");
            Object[] pairs = new Object[0]; int used=0;
            for (Entry<K,V> e : visibleRead.entrySet()) {
                K key=e.getKey(); V value=function.apply(key,e.getValue());
                requireVisible(key,value);
                pairs=appendPair(pairs,used,key,value); used+=2;
            }
            publishPairs(pairs,used);
        }
        @Override public V putIfAbsent(K key, V value) {
            V previous=get(key);
            if (previous == null) return put(key,value);
            return previous;
        }
        @Override public boolean remove(Object key, Object value) {
            if (!containsKey(key) || !Objects.equals(get(key),value)) return false;
            source.remove(key); return true;
        }
        @Override public boolean replace(K key, V oldValue, V newValue) {
            if (!containsKey(key) || !Objects.equals(get(key),oldValue)) return false;
            put(key,newValue); return true;
        }
        @Override public V replace(K key, V value) {
            if (!containsKey(key)) return null;
            return put(key,value);
        }
        @Override public V computeIfAbsent(K key, Function<? super K,? extends V> function) {
            Objects.requireNonNull(function, "function");
            V previous=get(key);
            if (previous != null) return previous;
            V next=function.apply(key);
            if (next != null) put(key,next);
            return next;
        }
        @Override public V computeIfPresent(K key, BiFunction<? super K,? super V,? extends V> function) {
            Objects.requireNonNull(function, "function");
            V previous=get(key);
            if (previous == null) return null;
            V next=function.apply(key,previous);
            if (next == null) remove(key); else put(key,next);
            return next;
        }
        @Override public V compute(K key, BiFunction<? super K,? super V,? extends V> function) {
            Objects.requireNonNull(function, "function");
            V previous=get(key); boolean had=previous != null || containsKey(key);
            V next=function.apply(key,previous);
            if (next == null) { if(had) remove(key); }
            else put(key,next);
            return next;
        }
        @Override public V merge(K key, V value, BiFunction<? super V,? super V,? extends V> function) {
            Objects.requireNonNull(function, "function");Objects.requireNonNull(value,"value");
            V previous=get(key);
            V next=previous == null ? value : function.apply(previous,value);
            if (next == null) remove(key); else put(key,next);
            return next;
        }
        private boolean removeMatching(BiPredicate<? super K,? super V> condition) {
            boolean removed=false;
            Iterator<Entry<K,V>> iter=source.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<K,V> entry=iter.next();
                K key=entry.getKey(); V value=entry.getValue();
                if (predicate.test(key,value) && condition.test(key,value)) {
                    iter.remove(); removed=true;
                }
            }
            return removed;
        }

        private final class MutableEntry implements Entry<K,V> {
            private final K key;
            MutableEntry(K key) { this.key=key; }
            @Override public K getKey() { return key; }
            @Override public V getValue() { return MutableMap.this.get(key); }
            @Override public V setValue(V value) {
                if (!MutableMap.this.containsKey(key))
                    throw new IllegalStateException("entry no longer visible");
                return MutableMap.this.put(key,value);
            }
            @Override public boolean equals(Object other) {
                return other instanceof Entry<?,?> e
                        && Objects.equals(key,e.getKey()) && Objects.equals(getValue(),e.getValue());
            }
            @Override public int hashCode() { return Objects.hashCode(key)^Objects.hashCode(getValue()); }
            @Override public String toString() { return key+"="+getValue(); }
        }
        /** Selects without retaining a node per entry; only public projection allocates. */
        private Iterator<Entry<K,V>> selectedIterator() {
            Iterator<Entry<K,V>> sourceIter=source.entrySet().iterator();
            return new Iterator<>() {
                private boolean prepared, found, failed;
                private K nextKey;
                @Override public boolean hasNext() {
                    if (failed) throw new IllegalStateException("filtered iterator failed");
                    if (!prepared) {
                        failed=true;
                        while (sourceIter.hasNext()) {
                            Entry<K,V> entry=sourceIter.next();
                            if (predicate.test(entry.getKey(),entry.getValue())) {
                                nextKey=entry.getKey(); found=true; break;
                            }
                        }
                        prepared=true; failed=false;
                    }
                    return found;
                }
                @Override public Entry<K,V> next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    K key=nextKey; nextKey=null; prepared=false; found=false;
                    return new MutableEntry(key);
                }
                @Override public void remove() { throw new UnsupportedOperationException("lookahead remove"); }
            };
        }
        @Override public Set<Entry<K,V>> entrySet() {
            if(entries==null) entries=new AbstractSet<>() {
                @Override public Iterator<Entry<K,V>> iterator() { return selectedIterator(); }
                @Override public int size() { return MutableMap.this.size(); }
                @Override public boolean contains(Object object) {
                    if (!(object instanceof Entry<?,?> entry)) return false;
                    return MutableMap.this.containsKey(entry.getKey())
                            && Objects.equals(MutableMap.this.get(entry.getKey()),entry.getValue());
                }
                @Override public boolean remove(Object object) {
                    if (!contains(object)) return false;
                    MutableMap.this.remove(((Entry<?,?>)object).getKey()); return true;
                }
                @Override public void clear() { MutableMap.this.clear(); }
                @Override public boolean removeIf(Predicate<? super Entry<K,V>> filter) {
                    Objects.requireNonNull(filter,"filter");
                    return removeMatching((key,value)->filter.test(new SimpleImmutableEntry<>(key,value)));
                }
                @Override public boolean removeAll(Collection<?> c) {
                    Objects.requireNonNull(c,"collection");
                    return removeMatching((key,value)->c.contains(new SimpleImmutableEntry<>(key,value)));
                }
                @Override public boolean retainAll(Collection<?> c) {
                    Objects.requireNonNull(c,"collection");
                    return removeMatching((key,value)->!c.contains(new SimpleImmutableEntry<>(key,value)));
                }
            };
            return entries;
        }
        @Override public Set<K> keySet() {
            if(keys==null) keys=new AbstractSet<>() {
                @Override public Iterator<K> iterator() {
                    Iterator<Entry<K,V>> selected=selectedIterator();
                    return new Iterator<>() {
                        @Override public boolean hasNext() { return selected.hasNext(); }
                        @Override public K next() { return selected.next().getKey(); }
                        @Override public void remove() { throw new UnsupportedOperationException("lookahead remove"); }
                    };
                }
                @Override public int size() { return MutableMap.this.size(); }
                @Override public boolean contains(Object key) { return MutableMap.this.containsKey(key); }
                @Override public boolean remove(Object key) {
                    if(!contains(key))return false;MutableMap.this.remove(key);return true;
                }
                @Override public void clear() { MutableMap.this.clear(); }
                @Override public boolean removeIf(Predicate<? super K> f) {
                    Objects.requireNonNull(f,"filter");return removeMatching((key,value)->f.test(key));
                }
                @Override public boolean removeAll(Collection<?> c) {
                    Objects.requireNonNull(c,"collection");return removeMatching((key,value)->c.contains(key));
                }
                @Override public boolean retainAll(Collection<?> c) {
                    Objects.requireNonNull(c,"collection");return removeMatching((key,value)->!c.contains(key));
                }
            };
            return keys;
        }
        @Override public Collection<V> values() {
            if(values==null) values=new AbstractCollection<>() {
                @Override public Iterator<V> iterator() {
                    Iterator<Entry<K,V>> selected=selectedIterator();
                    return new Iterator<>() {
                        @Override public boolean hasNext() { return selected.hasNext(); }
                        @Override public V next() { return selected.next().getValue(); }
                        @Override public void remove() { throw new UnsupportedOperationException("lookahead remove"); }
                    };
                }
                @Override public int size() { return MutableMap.this.size(); }
                @Override public boolean contains(Object value) { return MutableMap.this.containsValue(value); }
                @Override public boolean remove(Object value) {
                    Iterator<Entry<K,V>> iter=source.entrySet().iterator();
                    while(iter.hasNext()) {
                        Entry<K,V> e=iter.next();
                        if(predicate.test(e.getKey(),e.getValue())&&Objects.equals(value,e.getValue())) {
                            iter.remove();return true;
                        }
                    }
                    return false;
                }
                @Override public void clear() { MutableMap.this.clear(); }
                @Override public boolean removeIf(Predicate<? super V> f) {
                    Objects.requireNonNull(f,"filter");return removeMatching((key,value)->f.test(value));
                }
                @Override public boolean removeAll(Collection<?> c) {
                    Objects.requireNonNull(c,"collection");return removeMatching((key,value)->c.contains(value));
                }
                @Override public boolean retainAll(Collection<?> c) {
                    Objects.requireNonNull(c,"collection");return removeMatching((key,value)->!c.contains(value));
                }
            };
            return values;
        }
    }
    private static final class MutableNavigableMap<K,V> extends MutableMap<K,V>
            implements NavigableMap<K,V> {
        private final NavigableMap<K,V> ordered;
        private final NavigableMap<K,V> orderedRead;
        MutableNavigableMap(NavigableMap<K,V> source, BiPredicate<? super K,? super V> predicate) {
            super(source,predicate);
            ordered=source;
            orderedRead=M3FilteredMaps.filtered(source,predicate);
        }
        @Override public Comparator<? super K> comparator() { return ordered.comparator(); }
        @Override public K firstKey() { return orderedRead.firstKey(); }
        @Override public K lastKey() { return orderedRead.lastKey(); }
        @Override public Entry<K,V> firstEntry() { return orderedRead.firstEntry(); }
        @Override public Entry<K,V> lastEntry() { return orderedRead.lastEntry(); }
        @Override public Entry<K,V> lowerEntry(K key) { return orderedRead.lowerEntry(key); }
        @Override public Entry<K,V> floorEntry(K key) { return orderedRead.floorEntry(key); }
        @Override public Entry<K,V> ceilingEntry(K key) { return orderedRead.ceilingEntry(key); }
        @Override public Entry<K,V> higherEntry(K key) { return orderedRead.higherEntry(key); }
        @Override public K lowerKey(K key) { return orderedRead.lowerKey(key); }
        @Override public K floorKey(K key) { return orderedRead.floorKey(key); }
        @Override public K ceilingKey(K key) { return orderedRead.ceilingKey(key); }
        @Override public K higherKey(K key) { return orderedRead.higherKey(key); }
        @Override public Entry<K,V> pollFirstEntry() {
            Entry<K,V> first=orderedRead.firstEntry();
            if(first!=null) ordered.remove(first.getKey());
            return first;
        }
        @Override public Entry<K,V> pollLastEntry() {
            Entry<K,V> last=orderedRead.lastEntry();
            if(last!=null) ordered.remove(last.getKey());
            return last;
        }
        @Override public NavigableMap<K,V> descendingMap() {
            return filtered(ordered.descendingMap(),predicate);
        }
        @Override public NavigableMap<K,V> reversed() { return descendingMap(); }
        @Override public NavigableSet<K> navigableKeySet() {
            return M3CollectionViews.filtered(ordered.navigableKeySet(),
                    key->predicate.test(key,ordered.get(key)));
        }
        @Override public NavigableSet<K> descendingKeySet() { return descendingMap().navigableKeySet(); }
        @Override public Set<K> keySet() { return navigableKeySet(); }
        @Override public NavigableMap<K,V> subMap(K from,boolean fromInclusive,K to,boolean toInclusive) {
            return filtered(ordered.subMap(from,fromInclusive,to,toInclusive),predicate);
        }
        @Override public NavigableMap<K,V> headMap(K to,boolean inclusive) {
            return filtered(ordered.headMap(to,inclusive),predicate);
        }
        @Override public NavigableMap<K,V> tailMap(K from,boolean inclusive) {
            return filtered(ordered.tailMap(from,inclusive),predicate);
        }
        @Override public SortedMap<K,V> subMap(K from,K to) { return subMap(from,true,to,false); }
        @Override public SortedMap<K,V> headMap(K to) { return headMap(to,false); }
        @Override public SortedMap<K,V> tailMap(K from) { return tailMap(from,true); }
    }
}
