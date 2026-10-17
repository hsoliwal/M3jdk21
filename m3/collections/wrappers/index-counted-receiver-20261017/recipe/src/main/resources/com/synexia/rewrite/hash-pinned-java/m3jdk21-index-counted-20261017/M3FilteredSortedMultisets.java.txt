// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Opt-in, counted, comparator-ordered filters over the existing M3Index primitive-ID multiset.
 *
 * <p>Visibility is a live view; groups are not copied into an alternative map or ID store.
 * Predicate evaluation is once per distinct group during ordinary iteration, even when
 * its count is very large. Underlying count/structure changes invalidate an in-progress
 * iterator through the canonical mutation stamp, including changes to otherwise hidden
 * groups. This is fail-fast, not synchronization or a concurrent-snapshot guarantee.
 *
 * <p>Count, rank and ordinal selection preserve duplicates without expanding them.
 * Sorting, range bounds, null handling and exact object references stay with the backing
 * domain and ordered store. Entry records are transient boundary projections only.
 * No new per-element retained arrays, global caches, JNI symbols or interner ownership.
 *
 * <p>Comparators must be consistent with equals, predicates pure/stable and insensitive
 * to distinct query representatives for the same comparator key, and domains stable.
 * Explicit write operations mutate visible groups only; read-only variants refuse even
 * no-op writes. Iterator.remove is unsupported due to filter lookahead; bulk mutators
 * use the original owner iterators. Not concurrent, serializable, weak, blocking or
 * OOME-safe. Group traversal O(d), occurrence delivery O(selected occurrences), and
 * existing-group count lookup O(log d), excluding client predicate/domain costs.
 */
public final class M3FilteredSortedMultisets {
    private M3FilteredSortedMultisets() { }

    /** Mutable live filtered occurrence view; admission of rejected elements is refused. */
    public static <E> View<E> filtered(M3IndexSortedMultiset<E> source,
            Predicate<? super E> predicate) {
        return new View<>(source, predicate, false);
    }

    /** Read-only live filtered occurrence view, including all nested range projections. */
    public static <E> View<E> readOnlyFiltered(M3IndexSortedMultiset<E> source,
            Predicate<? super E> predicate) {
        return new View<>(source, predicate, true);
    }

    /** Count-aware live view; not a binary-compatible Guava or Commons SortedBag subtype. */
    public static final class View<E> extends AbstractCollection<E> {
        private final M3IndexSortedMultiset<E> backing;
        private final Predicate<? super E> predicate;
        private final boolean readOnly;

        private View(M3IndexSortedMultiset<E> source, Predicate<? super E> filter, boolean frozen) {
            backing = Objects.requireNonNull(source, "source");
            predicate = Objects.requireNonNull(filter, "predicate");
            readOnly = frozen;
        }

        /** Conjunctive nested filter without copying or rebuilding the primitive ID lanes. */
        public View<E> filtered(Predicate<? super E> additional) {
            Objects.requireNonNull(additional, "predicate");
            return new View<>(backing, value -> predicate.test(value) && additional.test(value), readOnly);
        }
        public MIndexSpace<E> space() { return backing.space(); }
        public Comparator<? super E> comparator() { return backing.comparator(); }
        public boolean readOnly() { return readOnly; }
        private boolean visible(E value) { return predicate.test(value); }
        private void writable() {
            if (readOnly) throw new UnsupportedOperationException("read-only filtered multiset");
        }
        private E project(int id) { return id == -1 ? null : backing.space().value(id); }
        private void check(long expected) {
            if (backing.mutationEpoch() != expected)
                throw new ConcurrentModificationException("filtered count backing changed");
        }
        private boolean selected(E value, long expected) {
            boolean result = visible(value);
            check(expected);
            return result;
        }
        /** Visits canonical primitive ID/count groups once, with no occurrence expansion. */
        public void forEachIdCount(M3IndexSortedMultiset.IdCountConsumer action) {
            Objects.requireNonNull(action, "action");
            backing.forEachIdCount((id, count) -> {
                long expected = backing.mutationEpoch();
                E value = project(id); check(expected);
                if (selected(value, expected)) action.accept(id, count);
                check(expected);
            });
        }
        public int distinctSize() {
            int[] groups = {0};
            forEachIdCount((id, count) -> groups[0]++);
            return groups[0];
        }
        public long longSize() {
            long[] total = {0};
            forEachIdCount((id, count) -> total[0] += count);
            return total[0];
        }
        @Override public int size() { return (int) Math.min(Integer.MAX_VALUE, longSize()); }
        @Override public boolean isEmpty() {
            Iterator<E> iterator = elementSet().iterator(); return !iterator.hasNext();
        }
        @SuppressWarnings("unchecked") public int count(Object element) {
            int occurrences = backing.count(element);
            if (occurrences == 0) return 0;
            long epoch = backing.mutationEpoch();
            boolean found = selected((E) element, epoch);
            return found ? occurrences : 0;
        }
        @Override public boolean contains(Object element) { return count(element) > 0; }

        private void approve(E value, int positiveCount) {
            if (positiveCount > 0 && !visible(value))
                throw new IllegalArgumentException("element rejected by filtered multiset");
        }
        private static void requireNonnegative(int count) {
            if (count < 0) throw new IllegalArgumentException("negative occurrence count");
        }
        @Override public boolean add(E element) { add(element, 1); return true; }
        public int add(E element, int count) {
            writable(); requireNonnegative(count);
            if (count == 0) return count(element);
            approve(element, count);
            return backing.add(element, count);
        }
        @Override public boolean addAll(Collection<? extends E> elements) {
            writable();
            ArrayList<E> snapshot = new ArrayList<>(Objects.requireNonNull(elements, "elements"));
            for (E value : snapshot) approve(value, 1);
            return backing.addAll(snapshot);
        }
        @Override public boolean remove(Object value) { return remove(value, 1) > 0; }
        public int remove(Object value, int count) {
            writable(); requireNonnegative(count);
            int previous = count(value);
            if (previous == 0 || count == 0) return previous;
            backing.remove(value, count);
            return previous;
        }
        public int setCount(E element, int count) {
            writable(); requireNonnegative(count);
            if (count == 0) return remove(element, Integer.MAX_VALUE);
            approve(element, count);
            return backing.setCount(element, count);
        }
        public boolean setCount(E element, int expected, int count) {
            writable(); requireNonnegative(expected); requireNonnegative(count);
            int previous = count(element);
            if (previous != expected) return false;
            if (count == 0) { remove(element, Integer.MAX_VALUE); return true; }
            approve(element, count);
            backing.setCount(element, count);
            return true;
        }

        /** Select once per comparator-group and emit each accepted occurrence lazily. */
        @Override public Iterator<E> iterator() {
            Iterator<M3IndexSortedMultiset.Entry<E>> groups = backing.entrySet().iterator();
            return new Iterator<>() {
                private final long expected = backing.mutationEpoch();
                private E current;
                private int remaining;
                private boolean prepared;
                private boolean hasGroup;
                private boolean failed;
                private void prepare() {
                    check(expected);
                    if (failed) throw new IllegalStateException("filtered multiset iterator failed");
                    if (remaining != 0 || prepared) return;
                    failed = true;
                    while (groups.hasNext()) {
                        M3IndexSortedMultiset.Entry<E> entry = groups.next();
                        if (selected(entry.element(), expected)) {
                            current = entry.element(); remaining = entry.count(); hasGroup = true; break;
                        }
                    }
                    prepared = true; failed = false;
                }
                @Override public boolean hasNext() { prepare(); return remaining > 0 && hasGroup; }
                @Override public E next() {
                    prepare();
                    if (!hasGroup || remaining == 0) throw new NoSuchElementException();
                    --remaining;
                    E value = current;
                    if (remaining == 0) { current = null; hasGroup = false; prepared = false; }
                    check(expected);
                    return value;
                }
                @Override public void remove() {
                    throw new UnsupportedOperationException("filtered iterator removal uses lookahead");
                }
            };
        }
        @Override public void clear() {
            writable();
            Iterator<E> groups = backing.elementSet().iterator();
            long expected = backing.mutationEpoch();
            while (groups.hasNext()) {
                E value = groups.next();
                if (selected(value, expected)) { groups.remove(); expected = backing.mutationEpoch(); }
            }
        }
        /** Inherited occurrence mutation semantics: callback evaluated once per visible occurrence. */
        @Override public boolean removeIf(Predicate<? super E> filter) {
            writable(); Objects.requireNonNull(filter, "filter");
            Iterator<E> iterator = backing.iterator();
            boolean changed = false;
            while (iterator.hasNext()) {
                E value = iterator.next();
                long expected = backing.mutationEpoch();
                if (selected(value, expected) && filter.test(value)) {
                    check(expected); iterator.remove(); changed = true;
                } else check(expected);
            }
            return changed;
        }
        @Override public boolean removeAll(Collection<?> c) {
            writable(); Objects.requireNonNull(c, "elements");
            if (c == this) { boolean any = !isEmpty(); clear(); return any; }
            return elementSet().removeIf(c::contains);
        }
        @Override public boolean retainAll(Collection<?> c) {
            writable(); Objects.requireNonNull(c, "elements");
            if (c == this) return false;
            return elementSet().removeIf(e -> !c.contains(e));
        }
        public View<E> headMultiset(E endpoint, boolean inclusive) {
            return new View<>(backing.headMultiset(endpoint, inclusive), predicate, readOnly);
        }
        public View<E> tailMultiset(E endpoint, boolean inclusive) {
            return new View<>(backing.tailMultiset(endpoint, inclusive), predicate, readOnly);
        }
        public View<E> subMultiset(E from, boolean fromInclusive, E to, boolean toInclusive) {
            return new View<>(backing.subMultiset(from, fromInclusive, to, toInclusive), predicate, readOnly);
        }
        public View<E> descendingMultiset() {
            return new View<>(backing.descendingMultiset(), predicate, readOnly);
        }
        private M3IndexSortedMultiset.Entry<E> extremum(boolean first) {
            Iterator<E> cursor = first ? elementSet().iterator() : elementSet().descendingIterator();
            if (!cursor.hasNext()) return null;
            E value = cursor.next();
            return new M3IndexSortedMultiset.Entry<>(value, backing.count(value));
        }
        public M3IndexSortedMultiset.Entry<E> firstEntry() { return extremum(true); }
        public M3IndexSortedMultiset.Entry<E> lastEntry() { return extremum(false); }
        public M3IndexSortedMultiset.Entry<E> pollFirstEntry() {
            writable();
            M3IndexSortedMultiset.Entry<E> entry = firstEntry();
            if (entry != null) backing.setCount(entry.element(), 0);
            return entry;
        }
        public M3IndexSortedMultiset.Entry<E> pollLastEntry() {
            writable();
            M3IndexSortedMultiset.Entry<E> entry = lastEntry();
            if (entry != null) backing.setCount(entry.element(), 0);
            return entry;
        }
        /** Occurrence rank in this view's comparator order (strict or inclusive). */
        public long rankOf(E element, boolean inclusive) {
            compare(element, element); // same type and null/comparator admission as backing
            long[] ordinal = {0};
            forEachIdCount((id, count) -> {
                int relation = compare(project(id), element);
                if (relation < 0 || inclusive && relation == 0) ordinal[0] += count;
            });
            return ordinal[0];
        }
        /** Zero-based filtered occurrence selection without materializing a large count group. */
        public E elementAtOccurrence(long ordinal) {
            if (ordinal < 0) throw new IndexOutOfBoundsException("negative occurrence");
            long[] offset = {ordinal};
            int[] selected = {Integer.MIN_VALUE};
            forEachIdCount((id, count) -> {
                if (selected[0] != Integer.MIN_VALUE) return;
                if (offset[0] < count) selected[0] = id;
                else offset[0] -= count;
            });
            if (selected[0] == Integer.MIN_VALUE)
                throw new IndexOutOfBoundsException("occurrence outside filtered multiset");
            return project(selected[0]);
        }
        @SuppressWarnings("unchecked") private int compare(E left, E right) {
            Comparator<? super E> ordering = comparator();
            return ordering == null ? ((Comparable<? super E>) Objects.requireNonNull(left,"key")).compareTo(right)
                    : ordering.compare(left, right);
        }
        /** Sorted but not DISTINCT or SIZED: filtering does not materialize hidden runs. */
        @Override public Spliterator<E> spliterator() {
            Iterator<E> iterator = iterator();
            Comparator<? super E> ordering = comparator();
            return new Spliterator<>() {
                @Override public boolean tryAdvance(Consumer<? super E> action) {
                    Objects.requireNonNull(action, "action");
                    if (!iterator.hasNext()) return false;
                    action.accept(iterator.next()); return true;
                }
                @Override public Spliterator<E> trySplit() { return null; }
                @Override public long estimateSize() { return Long.MAX_VALUE; }
                @Override public int characteristics() { return SORTED | ORDERED; }
                @Override public Comparator<? super E> getComparator() { return ordering; }
            };
        }
        @Override public void forEach(Consumer<? super E> action) {
            Objects.requireNonNull(action, "action");
            for (E value : this) action.accept(value);
        }
        public NavigableSet<E> elementSet() { return new FilteredElements(); }
        private final class FilteredElements extends AbstractSet<E> implements NavigableSet<E> {
            private Iterator<E> selectedGroups() {
                Iterator<E> groups=backing.elementSet().iterator();
                return new Iterator<>() {
                    private final long expected=backing.mutationEpoch();
                    private boolean prepared, found, failed;
                    private E next;
                    @Override public boolean hasNext() {
                        check(expected);
                        if (failed) throw new IllegalStateException("filtered element iterator failed");
                        if (!prepared) {
                            failed = true;
                            while (groups.hasNext()) {
                                E candidate = groups.next();
                                if (selected(candidate,expected)) { next=candidate;found=true;break; }
                            }
                            prepared=true;failed=false;
                        }
                        return found;
                    }
                    @Override public E next() {
                        if (!hasNext()) throw new NoSuchElementException();
                        E value=next;next=null;found=false;prepared=false;
                        check(expected);return value;
                    }
                    @Override public void remove() { throw new UnsupportedOperationException("filtered element iterator lookahead"); }
                };
            }
            @Override public Comparator<? super E> comparator() { return View.this.comparator(); }
            @Override public Iterator<E> iterator() { return selectedGroups(); }
            @Override public Iterator<E> descendingIterator() { return View.this.descendingMultiset().elementSet().iterator(); }
            @Override public NavigableSet<E> descendingSet() { return View.this.descendingMultiset().elementSet(); }
            @Override public NavigableSet<E> reversed() { return descendingSet(); }
            @Override public int size() { return distinctSize(); }
            @Override public boolean isEmpty() { return View.this.isEmpty(); }
            @Override public boolean contains(Object o) { return View.this.contains(o); }
            @Override public boolean add(E value) { throw new UnsupportedOperationException("count group insertion via multiset only"); }
            @Override public boolean addAll(Collection<? extends E> c) { throw new UnsupportedOperationException("count group insertion via multiset only"); }
            @Override public boolean remove(Object o) {
                writable(); int previous=View.this.count(o);
                if (previous==0) return false;
                backing.remove(o,Integer.MAX_VALUE);return true;
            }
            @Override public void clear() { View.this.clear(); }
            @Override public boolean removeIf(Predicate<? super E> filter) {
                writable();Objects.requireNonNull(filter,"filter");
                Iterator<E> i=backing.elementSet().iterator(); boolean changed=false;
                long expected=backing.mutationEpoch();
                while(i.hasNext()) {
                    E current=i.next();
                    if(selected(current,expected) && filter.test(current)) {
                        check(expected);i.remove();expected=backing.mutationEpoch();changed=true;
                    } else check(expected);
                }
                return changed;
            }
            @Override public boolean removeAll(Collection<?> c) {
                writable();Objects.requireNonNull(c,"elements");
                if(c==this) { boolean any=!isEmpty();clear();return any; }
                return removeIf(c::contains);
            }
            @Override public boolean retainAll(Collection<?> c) {
                writable();Objects.requireNonNull(c,"elements");
                if(c==this) return false;
                return removeIf(e->!c.contains(e));
            }
            /** Queries scan distinct groups in the actual filtered view order. */
            private E seek(E key,int relation) {
                compare(key,key);boolean found=false;E best=null;
                for(E candidate:this) {
                    int order=compare(candidate,key);
                    if(relation<0) {
                        if(order<0 || relation==-1 && order==0) {best=candidate;found=true;}
                        else break;
                    } else if(order>0 || relation==1 && order==0) return candidate;
                }
                return found?best:null;
            }
            @Override public E lower(E e) { return seek(e,-2); }
            @Override public E floor(E e) { return seek(e,-1); }
            @Override public E ceiling(E e) { return seek(e,1); }
            @Override public E higher(E e) { return seek(e,2); }
            @Override public E first() { return required(iterator()); }
            @Override public E last() { return required(descendingIterator()); }
            private E required(Iterator<E> iterator) {
                if(!iterator.hasNext()) throw new NoSuchElementException();return iterator.next();
            }
            @Override public E pollFirst() { return poll(true); }
            @Override public E pollLast() { return poll(false); }
            private E poll(boolean first) {
                writable();Iterator<E> iterator=first?iterator():descendingIterator();
                if(!iterator.hasNext()) return null;
                E value=iterator.next();backing.remove(value,Integer.MAX_VALUE);return value;
            }
            @Override public NavigableSet<E> subSet(E from,boolean fi,E to,boolean ti) {
                return View.this.subMultiset(from,fi,to,ti).elementSet();
            }
            @Override public NavigableSet<E> headSet(E to,boolean incl) { return View.this.headMultiset(to,incl).elementSet(); }
            @Override public NavigableSet<E> tailSet(E from,boolean incl) { return View.this.tailMultiset(from,incl).elementSet(); }
            @Override public NavigableSet<E> subSet(E from,E to) { return subSet(from,true,to,false); }
            @Override public NavigableSet<E> headSet(E to) { return headSet(to,false); }
            @Override public NavigableSet<E> tailSet(E from) { return tailSet(from,true); }
            @Override public Spliterator<E> spliterator() {
                Iterator<E> iterator=iterator(); Comparator<? super E> ordering=comparator();
                return new Spliterator<>() {
                    @Override public boolean tryAdvance(Consumer<? super E> action) {
                        Objects.requireNonNull(action,"action");
                        if(!iterator.hasNext())return false;
                        action.accept(iterator.next());return true;
                    }
                    @Override public Spliterator<E> trySplit() {return null;}
                    @Override public long estimateSize() {return Long.MAX_VALUE;}
                    @Override public int characteristics() {return ORDERED|SORTED|DISTINCT;}
                    @Override public Comparator<? super E> getComparator(){return ordering;}
                };
            }
        }
        public Set<M3IndexSortedMultiset.Entry<E>> entrySet() {
            return new AbstractSet<>() {
                @Override public int size() { return distinctSize(); }
                @Override public Iterator<M3IndexSortedMultiset.Entry<E>> iterator() {
                    Iterator<E> elements=elementSet().iterator();
                    return new Iterator<>() {
                        @Override public boolean hasNext() {return elements.hasNext();}
                        @Override public M3IndexSortedMultiset.Entry<E> next() {
                            E element=elements.next();return new M3IndexSortedMultiset.Entry<>(element,backing.count(element));
                        }
                        @Override public void remove() {throw new UnsupportedOperationException("filtered entry iterator lookahead");}
                    };
                }
                @Override public boolean contains(Object o) {
                    if(!(o instanceof M3IndexSortedMultiset.Entry<?> e))return false;
                    return e.count()>0 && View.this.count(e.element())==e.count();
                }
                @Override public boolean add(M3IndexSortedMultiset.Entry<E> e) {
                    throw new UnsupportedOperationException("count group insertion via multiset only");
                }
                @Override public boolean addAll(Collection<? extends M3IndexSortedMultiset.Entry<E>> c) {
                    throw new UnsupportedOperationException("count group insertion via multiset only");
                }
                @Override public boolean remove(Object o) {
                    writable();
                    if(!(o instanceof M3IndexSortedMultiset.Entry<?> e))return false;
                    if(e.count()<=0||View.this.count(e.element())!=e.count())return false;
                    backing.remove(e.element(),Integer.MAX_VALUE);return true;
                }
                @Override public void clear() { View.this.clear(); }
                @Override public boolean removeIf(Predicate<? super M3IndexSortedMultiset.Entry<E>> filter) {
                    writable();Objects.requireNonNull(filter,"filter");
                    Iterator<E> groups=backing.elementSet().iterator();long expected=backing.mutationEpoch();
                    boolean changed=false;
                    while(groups.hasNext()) {
                        E value=groups.next();
                        if(selected(value,expected)) {
                            M3IndexSortedMultiset.Entry<E> entry=new M3IndexSortedMultiset.Entry<>(value,backing.count(value));
                            if(filter.test(entry)) {check(expected);groups.remove();expected=backing.mutationEpoch();changed=true;}
                            else check(expected);
                        }
                    }
                    return changed;
                }
                @Override public boolean removeAll(Collection<?> c) {
                    writable();Objects.requireNonNull(c,"elements");
                    if(c==this){boolean any=!isEmpty();clear();return any;}
                    return removeIf(c::contains);
                }
                @Override public boolean retainAll(Collection<?> c) {
                    writable();Objects.requireNonNull(c,"elements");
                    if(c==this)return false;
                    return removeIf(e->!c.contains(e));
                }
            };
        }
        /** Equality is scoped to this filtered multiset facade; not an arbitrary Collection. */
        @Override public boolean equals(Object other) {
            if(other==this)return true;
            if(!(other instanceof View<?> view))return false;
            if(longSize()!=view.longSize()||distinctSize()!=view.distinctSize())return false;
            for(var entry:entrySet()) if(view.count(entry.element())!=entry.count())return false;
            return true;
        }
        @Override public int hashCode() {
            int[] value={0};
            forEachIdCount((id,count)->value[0]+=Objects.hashCode(project(id))^count);
            return value[0];
        }
        @Override public String toString() {return entrySet().toString();}
    }
}
