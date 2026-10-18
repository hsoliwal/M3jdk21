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

import java.util.Collection;
import java.util.Iterator;
import com.m3.collections.M3LinkedList;
import java.util.Objects;
import java.util.Queue;

/**
 * An M3ApacheIteratorChain is an Iterator that wraps a number of Iterators.
 * <p>
 * This class makes multiple iterators look like one to the caller. When any
 * method from the Iterator interface is called, the M3ApacheIteratorChain will delegate
 * to a single underlying Iterator. The M3ApacheIteratorChain will invoke the Iterators
 * in sequence until all Iterators are exhausted.
 * </p>
 * <p>
 * Under many circumstances, linking Iterators together in this manner is more
 * efficient (and convenient) than reading out the contents of each Iterator
 * into a List and creating a new Iterator.
 * </p>
 * <p>
 * Calling a method that adds new Iterator <i>after a method in the Iterator
 * interface has been called</i> will result in an UnsupportedOperationException.
 * </p>
 * <p>
 * NOTE: As from version 3.0, the M3ApacheIteratorChain may contain no iterators. In
 * this case the class will function as an empty iterator.
 * </p>
 * <p>
 * NOTE: As from version 4.0, the M3ApacheIteratorChain stores the iterators in a queue
 * and removes any reference to them as soon as they are not used anymore. Thus,
 * the methods {@code setIterator(Iterator)} and {@code getIterators()} have been
 * removed and {@link #size()} will return the number of remaining iterators in
 * the queue.
 * </p>
 *
 * @param <E> the type of elements in this iterator.
 * @since 2.1
 */
public final class M3ApacheIteratorChain<E> implements Iterator<E> {

    /** The chain of iterators */
    private final Queue<Iterator<? extends E>> iteratorQueue = new M3LinkedList<>();

    /** The current iterator */
    private Iterator<? extends E> currentIterator;

    /**
     * The "last used" Iterator is the Iterator upon which next() or hasNext()
     * was most recently called used for the remove() operation only
     */
    private Iterator<? extends E> lastUsedIterator;

    /**
     * ComparatorChain is "locked" after the first time compare(Object,Object)
     * is called
     */
    private boolean isLocked;

    /**
     * Constructs an M3ApacheIteratorChain with no Iterators.
     * <p>
     * You will normally use {@link #addIterator(Iterator)} to add some
     * iterators after using this constructor.
     * </p>
     */
    public M3ApacheIteratorChain() {
    }

    /**
     * Constructs a new {@code M3ApacheIteratorChain} over the collection of
     * iterators.
     * <p>
     * This method takes a collection of iterators. The newly constructed
     * iterator will iterate through each one of the input iterators in turn.
     * </p>
     *
     * @param iteratorQueue the collection of iterators, not null
     * @throws NullPointerException if iterators collection is or contains null
     * @throws ClassCastException if iterators collection doesn't contain an
     * iterator
     */
    public M3ApacheIteratorChain(final Collection<? extends Iterator<? extends E>> iteratorQueue) {
        for (final Iterator<? extends E> iterator : iteratorQueue) {
            addIterator(iterator);
        }
    }

    /**
     * Constructs an M3ApacheIteratorChain with a single Iterator.
     * <p>
     * This method takes one iterator. The newly constructed iterator will
     * iterate through that iterator. Thus calling this constructor on its own
     * will have no effect other than decorating the input iterator.
     * </p>
     * <p>
     * You will normally use {@link #addIterator(Iterator)} to add some more
     * iterators after using this constructor.
     * </p>
     *
     * @param iterator the first child iterator in the M3ApacheIteratorChain, not null
     * @throws NullPointerException if the iterator is null
     */
    public M3ApacheIteratorChain(final Iterator<? extends E> iterator) {
        addIterator(iterator);
    }

    /**
     * Constructs a new {@code M3ApacheIteratorChain} over the array of iterators.
     * <p>
     * This method takes an array of iterators. The newly constructed iterator
     * will iterate through each one of the input iterators in turn.
     * </p>
     *
     * @param iteratorQueue the array of iterators, not null
     * @throws NullPointerException if iterators array is or contains null
     */
    @SafeVarargs // Donor only reads the array and copies iterator references into its queue.
    public M3ApacheIteratorChain(final Iterator<? extends E>... iteratorQueue) {
        for (final Iterator<? extends E> element : iteratorQueue) {
            addIterator(element);
        }
    }

    /**
     * Constructs a new {@code M3ApacheIteratorChain} over the two given iterators.
     * <p>
     * This method takes two iterators. The newly constructed iterator will
     * iterate through each one of the input iterators in turn.
     * </p>
     *
     * @param first the first child iterator in the M3ApacheIteratorChain, not null
     * @param second the second child iterator in the M3ApacheIteratorChain, not null
     * @throws NullPointerException if either iterator is null
     */
    public M3ApacheIteratorChain(final Iterator<? extends E> first, final Iterator<? extends E> second) {
        addIterator(first);
        addIterator(second);
    }

    /**
     * Add an Iterator to the end of the chain
     *
     * @param iterator Iterator to add
     * @throws IllegalStateException if I've already started iterating
     * @throws NullPointerException if the iterator is null
     */
    public void addIterator(final Iterator<? extends E> iterator) {
        checkLocked();
        iteratorQueue.add(Objects.requireNonNull(iterator, "iterator"));
    }

    /**
     * Checks whether the iterator chain is now locked and in use.
     */
    private void checkLocked() {
        if (isLocked) {
            throw new UnsupportedOperationException("M3ApacheIteratorChain cannot be changed after the first use of a method from the Iterator interface");
        }
    }

    /**
     * Return true if any Iterator in the M3ApacheIteratorChain has a remaining element.
     *
     * @return true if elements remain
     */
    @Override
    public boolean hasNext() {
        lockChain();
        updateCurrentIterator();
        lastUsedIterator = currentIterator;
        return currentIterator.hasNext();
    }

    /**
     * Determine if modifications can still be made to the M3ApacheIteratorChain.
     * IteratorChains cannot be modified once they have executed a method from
     * the Iterator interface.
     *
     * @return true if M3ApacheIteratorChain cannot be modified, false if it can
     */
    public boolean isLocked() {
        return isLocked;
    }

    /**
     * Lock the chain so no more iterators can be added. This must be called
     * from all Iterator interface methods.
     */
    private void lockChain() {
        if (!isLocked) {
            isLocked = true;
        }
    }

    /**
     * Returns the next Object of the current Iterator
     *
     * @return Object from the current Iterator
     * @throws java.util.NoSuchElementException if all the Iterators are
     * exhausted
     */
    @Override
    public E next() {
        lockChain();
        updateCurrentIterator();
        lastUsedIterator = currentIterator;

        return currentIterator.next();
    }

    /**
     * Removes from the underlying collection the last element returned by the
     * Iterator. As with next() and hasNext(), this method calls remove() on the
     * underlying Iterator. Therefore, this method may throw an
     * UnsupportedOperationException if the underlying Iterator does not support
     * this method.
     *
     * @throws UnsupportedOperationException if the remove operator is not
     * supported by the underlying Iterator
     * @throws IllegalStateException if the next method has not yet been called,
     * or the remove method has already been called after the last call to the
     * next method.
     */
    @Override
    public void remove() {
        lockChain();
        if (currentIterator == null) {
            updateCurrentIterator();
        }
        lastUsedIterator.remove();
    }

    /**
     * Returns the remaining number of Iterators in the current M3ApacheIteratorChain.
     *
     * @return Iterator count
     */
    public int size() {
        return iteratorQueue.size();
    }

    /**
     * Updates the current iterator field to ensure that the current Iterator is
     * not exhausted
     */
    protected void updateCurrentIterator() {
        if (currentIterator == null) {
            if (iteratorQueue.isEmpty()) {
                currentIterator = M3ApacheEmptyIterator.<E>emptyIterator();
            } else {
                currentIterator = iteratorQueue.remove();
            }
            // set last used iterator here, in case the user calls remove
            // before calling hasNext() or next() (although they shouldn't)
            lastUsedIterator = currentIterator;
        }
        while (!currentIterator.hasNext() && !iteratorQueue.isEmpty()) {
            currentIterator = iteratorQueue.remove();
        }
    }

}
