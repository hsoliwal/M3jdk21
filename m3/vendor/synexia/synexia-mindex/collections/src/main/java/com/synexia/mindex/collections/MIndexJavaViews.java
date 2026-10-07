// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Explicit java.util compatibility boundaries for MIndex-native collections.
 *
 * <p>The native collections remain primitive-ID structures. Iterator and
 * Map.Entry objects are created only when a caller deliberately crosses into
 * the java.util API. Frozen iterators read through their canonical owner and
 * coordinate instead of copying the complete ID lane at iterator creation.
 */
public final class MIndexJavaViews {
    private MIndexJavaViews() { }

    public static <E> List<E> mutableList(
            MIndexList<E> source) {
        requireJavaEquality(
                java.util.Objects.requireNonNull(source, "source").space());
        return new MutableListView<>(source);
    }

    public static <E> List<E> readOnlyList(
            MIndexFrozenList<E> source) {
        requireJavaEquality(
                java.util.Objects.requireNonNull(source, "source").space());
        return new FrozenListView<>(source);
    }

    public static <E> Set<E> mutableSet(
            MIndexSet<E> source) {
        requireJavaEquality(
                java.util.Objects.requireNonNull(source, "source").space());
        return new MutableSetView<>(source);
    }

    public static <E> Set<E> readOnlySet(
            MIndexFrozenSet<E> source) {
        requireJavaEquality(
                java.util.Objects.requireNonNull(source, "source").space());
        return new FrozenSetView<>(source);
    }

    public static <K, V> Map<K, V> mutableMap(
            MIndexMap<K, V> source) {
        MIndexMap<K, V> actual =
                java.util.Objects.requireNonNull(source, "source");
        requireJavaEquality(actual.keySpace());
        requireJavaEquality(actual.valueSpace());
        return new MutableMapView<>(source);
    }

    public static <E> Set<E> readOnlySet(
            MIndexFrozenOrderedSet<E> source) {
        requireJavaEquality(
                java.util.Objects.requireNonNull(
                        source, "source").space());
        return new FrozenOrderedSetView<>(source);
    }

    /**
     * Read-only JDK map projection of bag element -> positive occurrence count.
     */
    public static <E> Map<E, Integer> readOnlyBagCounts(
            MIndexFrozenBag<E> source) {
        requireJavaEquality(
                java.util.Objects.requireNonNull(
                        source, "source").space());
        return new FrozenBagCountView<>(source);
    }

    public static <K, V> Map<K, V> readOnlyMap(
            MIndexFrozenMap<K, V> source) {
        MIndexFrozenMap<K, V> actual =
                java.util.Objects.requireNonNull(source, "source");
        requireJavaEquality(actual.keySpace());
        requireJavaEquality(actual.valueSpace());
        return new FrozenMapView<>(source);
    }

    private static void requireJavaEquality(MIndexSpace<?> space) {
        if (!space.javaEqualityCompatible()) {
            throw new IllegalArgumentException(
                    "java.util view requires a Java-equality-compatible MIndexSpace");
        }
    }

    private static final class MutableListView<E>
            extends AbstractList<E>
            implements RandomAccess {
        private final MIndexList<E> source;

        MutableListView(MIndexList<E> source) {
            this.source = java.util.Objects.requireNonNull(
                    source, "source");
        }

        @Override
        public E get(int index) {
            return source.get(index);
        }

        @Override
        public int size() {
            return source.size();
        }

        @Override
        public E set(int index, E element) {
            return source.set(index, element);
        }

        @Override
        public void add(int index, E element) {
            source.insert(index, element);
            modCount++;
        }

        @Override
        public E remove(int index) {
            E previous = source.removeAt(index);
            modCount++;
            return previous;
        }

        @Override
        public void clear() {
            if (!source.isEmpty()) {
                source.clear();
                modCount++;
            }
        }
    }

    private static final class FrozenListView<E>
            extends AbstractList<E>
            implements RandomAccess {
        private final MIndexFrozenList<E> source;

        FrozenListView(MIndexFrozenList<E> source) {
            this.source = java.util.Objects.requireNonNull(
                    source, "source");
        }

        @Override
        public E get(int index) {
            return source.get(index);
        }

        @Override
        public int size() {
            return source.size();
        }
    }

    private static final class MutableSetView<E>
            extends AbstractSet<E> {
        private final MIndexSet<E> source;

        MutableSetView(MIndexSet<E> source) {
            this.source = java.util.Objects.requireNonNull(
                    source, "source");
        }

        @Override
        public int size() {
            return source.size();
        }

        @Override
        public boolean add(E value) {
            return source.add(value);
        }

        @Override
        public boolean contains(Object value) {
            try {
                return source.contains(cast(value));
            } catch (ClassCastException failure) {
                return false;
            }
        }

        @Override
        public boolean remove(Object value) {
            try {
                return source.remove(cast(value));
            } catch (ClassCastException failure) {
                return false;
            }
        }

        @Override
        public void clear() {
            source.clear();
        }

        @Override
        public Iterator<E> iterator() {
            int[] ids = source.snapshotIdsSorted();
            return new Iterator<>() {
                private int cursor;
                private int currentId = -1;
                private boolean removable;

                @Override
                public boolean hasNext() {
                    return cursor < ids.length;
                }

                @Override
                public E next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    currentId = ids[cursor++];
                    removable = true;
                    return source.space().value(currentId);
                }

                @Override
                public void remove() {
                    if (!removable) {
                        throw new IllegalStateException();
                    }
                    source.removeId(currentId);
                    removable = false;
                }
            };
        }
    }

    private static final class FrozenSetView<E>
            extends AbstractSet<E> {
        private final MIndexFrozenSet<E> source;

        FrozenSetView(MIndexFrozenSet<E> source) {
            this.source = java.util.Objects.requireNonNull(
                    source, "source");
        }

        @Override
        public int size() {
            return source.size();
        }

        @Override
        public boolean contains(Object value) {
            try {
                return source.contains(cast(value));
            } catch (ClassCastException failure) {
                return false;
            }
        }

        @Override
        public Iterator<E> iterator() {
            int count = source.size();
            MIndexCompositeIndex owner = source.compositeIndex();
            int canonicalId = source.canonicalId();
            MIndexSpace<E> space = source.space();
            return new Iterator<>() {
                private int cursor;

                @Override
                public boolean hasNext() {
                    return cursor < count;
                }

                @Override
                public E next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return space.value(owner.valueAt(canonicalId, cursor++));
                }
            };
        }
    }

    private static final class FrozenOrderedSetView<E>
            extends AbstractSet<E> {
        private final MIndexFrozenOrderedSet<E> source;

        FrozenOrderedSetView(
                MIndexFrozenOrderedSet<E> source) {
            this.source =
                    java.util.Objects.requireNonNull(
                            source, "source");
        }

        @Override
        public int size() {
            return source.size();
        }

        @Override
        public boolean contains(Object value) {
            try {
                return source.contains(cast(value));
            } catch (ClassCastException failure) {
                return false;
            }
        }

        @Override
        public Iterator<E> iterator() {
            int count = source.size();
            MIndexSpace<E> space = source.space();
            return new Iterator<>() {
                private int cursor;

                @Override
                public boolean hasNext() {
                    return cursor < count;
                }

                @Override
                public E next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return space.value(source.idAt(cursor++));
                }
            };
        }
    }

    private static final class FrozenBagCountView<E>
            extends AbstractMap<E, Integer> {
        private final MIndexFrozenBag<E> source;

        FrozenBagCountView(MIndexFrozenBag<E> source) {
            this.source =
                    java.util.Objects.requireNonNull(
                            source, "source");
        }

        @Override
        public int size() {
            return source.distinctSize();
        }

        @Override
        public boolean containsKey(Object key) {
            return get(key) != null;
        }

        @Override
        public Integer get(Object key) {
            try {
                int count = source.count(cast(key));
                return count == 0 ? null : count;
            } catch (ClassCastException failure) {
                return null;
            }
        }

        @Override
        public void forEach(BiConsumer<? super E, ? super Integer> action) {
            java.util.Objects.requireNonNull(action, "action");
            for (int ordinal = 0; ordinal < source.distinctSize(); ordinal++) {
                action.accept(source.space().value(source.idAt(ordinal)), source.countAt(ordinal));
            }
        }

        @Override
        public Set<Entry<E, Integer>> entrySet() {
            return new AbstractSet<>() {
                @Override
                public int size() {
                    return source.distinctSize();
                }

                @Override
                public Iterator<Entry<E, Integer>> iterator() {
                    int count = source.distinctSize();
                    MIndexSpace<E> space = source.space();
                    return new Iterator<>() {
                        private int cursor;

                        @Override
                        public boolean hasNext() {
                            return cursor < count;
                        }

                        @Override
                        public Entry<E, Integer> next() {
                            if (!hasNext()) {
                                throw new NoSuchElementException();
                            }
                            int id = source.idAt(cursor);
                            int occurrences = source.countAt(cursor++);
                            return new SimpleImmutableEntry<>(
                                    space.value(id), occurrences);
                        }
                    };
                }
            };
        }
    }

    private static final class MutableMapView<K, V>
            extends AbstractMap<K, V> {
        private final MIndexMap<K, V> source;

        MutableMapView(MIndexMap<K, V> source) {
            this.source = java.util.Objects.requireNonNull(
                    source, "source");
        }

        @Override
        public int size() {
            return source.size();
        }

        @Override
        public boolean containsKey(Object key) {
            try {
                return source.containsKey(cast(key));
            } catch (ClassCastException failure) {
                return false;
            }
        }

        @Override
        public V get(Object key) {
            try {
                return source.get(cast(key));
            } catch (ClassCastException failure) {
                return null;
            }
        }

        @Override
        public V put(K key, V value) {
            boolean present = source.containsKey(key);
            V previous = present ? source.get(key) : null;
            source.put(key, value);
            return previous;
        }

        @Override
        public V remove(Object key) {
            try {
                K typed = cast(key);
                if (!source.containsKey(typed)) {
                    return null;
                }
                V previous = source.get(typed);
                source.remove(typed);
                return previous;
            } catch (ClassCastException failure) {
                return null;
            }
        }

        @Override
        public void clear() {
            source.clear();
        }

        /** Iterate the same sorted snapshot as entrySet without constructing entries. */
        @Override
        public void forEach(BiConsumer<? super K, ? super V> action) {
            java.util.Objects.requireNonNull(action, "action");
            int[] lane = source.snapshotSortedLane();
            for (int offset = 0; offset < lane.length; offset += 2) {
                action.accept(source.keySpace().value(lane[offset]),
                        source.valueSpace().value(lane[offset + 1]));
            }
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {
                @Override
                public int size() {
                    return source.size();
                }

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    int[] lane = source.snapshotSortedLane();
                    return new Iterator<>() {
                        private int cursor;
                        private int currentKeyId = -1;
                        private boolean removable;

                        @Override
                        public boolean hasNext() {
                            return cursor < lane.length;
                        }

                        @Override
                        public Entry<K, V> next() {
                            if (!hasNext()) {
                                throw new NoSuchElementException();
                            }
                            currentKeyId = lane[cursor++];
                            int valueId = lane[cursor++];
                            removable = true;
                            return new SimpleImmutableEntry<>(
                                    source.keySpace().value(currentKeyId),
                                    source.valueSpace().value(valueId));
                        }

                        @Override
                        public void remove() {
                            if (!removable) {
                                throw new IllegalStateException();
                            }
                            source.removeKeyId(currentKeyId);
                            removable = false;
                        }
                    };
                }
            };
        }
    }

    private static final class FrozenMapView<K, V>
            extends AbstractMap<K, V> {
        private final MIndexFrozenMap<K, V> source;

        FrozenMapView(MIndexFrozenMap<K, V> source) {
            this.source = java.util.Objects.requireNonNull(
                    source, "source");
        }

        @Override
        public int size() {
            return source.size();
        }

        @Override
        public boolean containsKey(Object key) {
            try {
                return source.containsKey(cast(key));
            } catch (ClassCastException failure) {
                return false;
            }
        }

        @Override
        public V get(Object key) {
            try {
                return source.get(cast(key));
            } catch (ClassCastException failure) {
                return null;
            }
        }

        /** Frozen traversal reads canonical lanes directly, with no copied lane or entries. */
        @Override
        public void forEach(BiConsumer<? super K, ? super V> action) {
            source.forEach(action);
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {
                @Override
                public int size() {
                    return source.size();
                }

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    int count = source.size();
                    MIndexCompositeIndex owner = source.compositeIndex();
                    int canonicalId = source.canonicalId();
                    MIndexSpace<K> keys = source.keySpace();
                    MIndexSpace<V> values = source.valueSpace();
                    return new Iterator<>() {
                        private int cursor;

                        @Override
                        public boolean hasNext() {
                            return cursor < count;
                        }

                        @Override
                        public Entry<K, V> next() {
                            if (!hasNext()) {
                                throw new NoSuchElementException();
                            }
                            int offset = cursor++ << 1;
                            int keyId = owner.valueAt(canonicalId, offset);
                            int valueId = owner.valueAt(canonicalId, offset + 1);
                            return new SimpleImmutableEntry<>(
                                    keys.value(keyId),
                                    values.value(valueId));
                        }
                    };
                }
            };
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }
}
