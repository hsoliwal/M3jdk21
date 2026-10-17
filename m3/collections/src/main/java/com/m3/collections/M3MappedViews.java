/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;

/** Purpose-owned wrapper atoms. Invoked through {@link M3CollectionWrappers}. */
final class M3MappedViews {
    private M3MappedViews() {
    }


    static <F, T> Collection<T> collection(
            Collection<F> source, Function<? super F, ? extends T> mapper) {
        return Collections.unmodifiableCollection(new MappedCollection<>(source, mapper));
    }

    static <F, T> List<T> list(
            List<F> source, Function<? super F, ? extends T> mapper) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mapper, "mapper");
        List<T> view = source instanceof RandomAccess
                ? new MappedRandomAccessList<>(source, mapper)
                : new MappedList<>(source, mapper);
        return Collections.unmodifiableList(view);
    }

    static <F, T> Collection<T> flatMapped(
            Collection<F> source,
            Function<? super F, ? extends Iterable<? extends T>> mapper) {
        return Collections.unmodifiableCollection(new FlatMappedCollection<>(source, mapper));
    }

    static <K, V, T> Map<K, T> values(
            Map<K, V> source, Function<? super V, ? extends T> mapper) {
        return Collections.unmodifiableMap(new MappedValuesMap<>(source, mapper));
    }

    private static final class MappedCollection<F, T> extends AbstractCollection<T> {
        private final Collection<F> source;
        private final Function<? super F, ? extends T> mapper;

        private MappedCollection(Collection<F> source, Function<? super F, ? extends T> mapper) {
            this.source = Objects.requireNonNull(source, "source");
            this.mapper = Objects.requireNonNull(mapper, "mapper");
        }

        @Override
        public Iterator<T> iterator() {
            Iterator<F> sourceIterator = source.iterator();
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return sourceIterator.hasNext();
                }

                @Override
                public T next() {
                    return mapper.apply(sourceIterator.next());
                }
            };
        }

        @Override
        public int size() {
            return source.size();
        }
    }

    private static class MappedList<F, T> extends AbstractList<T> {
        private final List<F> source;
        private final Function<? super F, ? extends T> mapper;

        private MappedList(List<F> source, Function<? super F, ? extends T> mapper) {
            this.source = source;
            this.mapper = mapper;
        }

        @Override
        public T get(int index) {
            return mapper.apply(source.get(index));
        }

        @Override
        public int size() {
            return source.size();
        }

        @Override
        public ListIterator<T> listIterator(int index) {
            ListIterator<F> sourceIterator = source.listIterator(index);
            return new ListIterator<>() {
                @Override
                public boolean hasNext() {
                    return sourceIterator.hasNext();
                }

                @Override
                public T next() {
                    return mapper.apply(sourceIterator.next());
                }

                @Override
                public boolean hasPrevious() {
                    return sourceIterator.hasPrevious();
                }

                @Override
                public T previous() {
                    return mapper.apply(sourceIterator.previous());
                }

                @Override
                public int nextIndex() {
                    return sourceIterator.nextIndex();
                }

                @Override
                public int previousIndex() {
                    return sourceIterator.previousIndex();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("mapped view");
                }

                @Override
                public void set(T value) {
                    throw new UnsupportedOperationException("mapped view");
                }

                @Override
                public void add(T value) {
                    throw new UnsupportedOperationException("mapped view");
                }
            };
        }
    }

    private static final class MappedRandomAccessList<F, T> extends MappedList<F, T>
            implements RandomAccess {
        private MappedRandomAccessList(List<F> source, Function<? super F, ? extends T> mapper) {
            super(source, mapper);
        }
    }

    private static final class FlatMappedCollection<F, T> extends AbstractCollection<T> {
        private final Collection<F> source;
        private final Function<? super F, ? extends Iterable<? extends T>> mapper;

        private FlatMappedCollection(Collection<F> source,
                Function<? super F, ? extends Iterable<? extends T>> mapper) {
            this.source = Objects.requireNonNull(source, "source");
            this.mapper = Objects.requireNonNull(mapper, "mapper");
        }

        @Override
        public Iterator<T> iterator() {
            Iterator<F> outer = source.iterator();
            return new Iterator<>() {
                private Iterator<? extends T> inner = Collections.emptyIterator();

                @Override
                public boolean hasNext() {
                    while (!inner.hasNext() && outer.hasNext()) {
                        inner = Objects.requireNonNull(mapper.apply(outer.next()),
                                "flatMapper result").iterator();
                    }
                    return inner.hasNext();
                }

                @Override
                public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return inner.next();
                }
            };
        }

        /** JDK Collection.size ceiling, retaining the original single scan. */
        @Override
        public int size() {
            int count = 0;
            for (T ignored : this) {
                if (++count == Integer.MAX_VALUE) return Integer.MAX_VALUE;
            }
            return count;
        }

        /**
         * A flattened view has no exact cardinality without evaluating mapper.
         * Do not let the inherited SIZED collection cursor force a size() scan
         * before findFirst/count/limit. One cursor is created only on traversal;
         * parallel streams are correct but intentionally cannot split this view.
         */
        @Override
        public Spliterator<T> spliterator() {
            int order = source.spliterator().characteristics() & Spliterator.ORDERED;
            return new Spliterator<>() {
                private Iterator<T> cursor;

                private Iterator<T> cursor() {
                    if (cursor == null) cursor = FlatMappedCollection.this.iterator();
                    return cursor;
                }

                @Override public boolean tryAdvance(Consumer<? super T> action) {
                    Objects.requireNonNull(action, "action");
                    Iterator<T> it = cursor();
                    if (!it.hasNext()) return false;
                    action.accept(it.next());
                    return true;
                }

                @Override public void forEachRemaining(Consumer<? super T> action) {
                    Objects.requireNonNull(action, "action");
                    cursor().forEachRemaining(action);
                }

                @Override public Spliterator<T> trySplit() { return null; }
                @Override public long estimateSize() { return Long.MAX_VALUE; }
                @Override public int characteristics() { return order; }
            };
        }
    }

    private static final class MappedValuesMap<K, V, T> extends AbstractMap<K, T> {
        private final Map<K, V> source;
        private final Function<? super V, ? extends T> mapper;

        private MappedValuesMap(Map<K, V> source, Function<? super V, ? extends T> mapper) {
            this.source = Objects.requireNonNull(source, "source");
            this.mapper = Objects.requireNonNull(mapper, "mapper");
        }

        @Override
        public int size() {
            return source.size();
        }

        @Override
        public boolean containsKey(Object key) {
            return source.containsKey(key);
        }

        @Override
        public T get(Object key) {
            return source.containsKey(key) ? mapper.apply(source.get(key)) : null;
        }

        @Override
        public Set<Entry<K, T>> entrySet() {
            return new AbstractSet<>() {
                @Override
                public int size() {
                    return source.size();
                }

                @Override
                public Iterator<Entry<K, T>> iterator() {
                    Iterator<Entry<K, V>> iterator = source.entrySet().iterator();
                    return new Iterator<>() {
                        @Override
                        public boolean hasNext() {
                            return iterator.hasNext();
                        }

                        @Override
                        public Entry<K, T> next() {
                            Entry<K, V> entry = iterator.next();
                            return new SimpleImmutableEntry<>(entry.getKey(),
                                    mapper.apply(entry.getValue()));
                        }
                    };
                }
            };
        }
    }

}
