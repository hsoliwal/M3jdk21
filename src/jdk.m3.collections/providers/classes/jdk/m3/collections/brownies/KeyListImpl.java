/*
 * Copyright 2012 by Thomas Mauch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * $Id$
 */
package jdk.m3.collections.brownies;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

import jdk.m3.collections.brownies.KeyCollectionImpl.KeyMap;
import jdk.m3.collections.brownies.helper.Option;

/**
 * A KeyList add key handling features to GapList.
 *
 * @author Thomas Mauch
 *
 * @see GapList
 * @param <E> type of elements stored in the list
 */
@SuppressWarnings({"serial", "this-escape"})
public class KeyListImpl<E> extends IList<E> {

    /**
     * Key collection used for key storage (never null).
     */
    KeyCollectionImpl<E> keyColl;
    /**
     * List where the list content of this KeyListImpl is stored (never null).
     * If this is list sorted by element (key 0), keyColl.keyMaps[0].keysList will also reference this list.
     */
    IList<E> list;

    /** If true the invariants are checked for debugging */
    private static final boolean DEBUG_CHECK = false;

    /**
     * Private method to check invariant of KeyListImpl.
     * It is only used for debugging.
     */
    private void debugCheck() {
        keyColl.debugCheck();
        //list.debugCheck();
        assert (keyColl.keyList == this);
        assert (list.size() == keyColl.size() || (keyColl.size() == 0 && keyColl.keyMaps == null));
    }

    /**
     * Constructor.
     */
    KeyListImpl() {
    }

    @Override
    protected void doAssign(IList<E> that) {
        KeyListImpl<E> list = (KeyListImpl<E>) that;
        this.keyColl = list.keyColl;
        this.list = list.list;
    }

    @Override
    public KeyListImpl<E> copy() {
        if (isReadOnly()) {
            KeyListImpl<E> list = new KeyListImpl<>(null);
            list.initCopy(this);
            return list;
        } else {
            return doCopy();
        }
    }

    protected KeyListImpl<E> doCopy() {
        KeyListImpl<E> copy = (KeyListImpl<E>) super.clone();
        copy.initCopy(this);
        return copy;
    }

    @Override
    public KeyListImpl<E> clone() {
        if (isReadOnly()) {
            return this;
        } else {
            return doCopy();
        }
    }

    @Override
    public KeyListImpl<E> crop() {
        if (isReadOnly()) {
            KeyListImpl<E> list = new KeyListImpl<>(null);
            list.initCrop(this);
            return list;
        } else {
            return doCrop();
        }
    }

    protected KeyListImpl<E> doCrop() {
        KeyListImpl<E> copy = (KeyListImpl<E>) super.clone();
        copy.initCrop(this);
        return copy;
    }

    /**
     * Returns the elements stored in the KeyList as simple IList without constraints etc.
     * The KeyList itself becomes empty, it is equal to a list created by calling crop().
     * This method is useful if you need performance or safety of a KeyList just while building the result,
     * but you then want to return a simple IList without constraints and the KeyList is not needed any longer.
     *
     * @return IList with elements
     */
    public IList<E> unwrap() {
        IList<E> l = list;
        this.initCrop(this);
        return l;
    }

    /**
     * Initialize object for crop() operation.
     *
     * @param that source object
     */
    @SuppressWarnings("unchecked")
    void initCrop(KeyListImpl<E> that) {
        // KeyCollectionImpl
        keyColl = new KeyCollectionImpl<E>();
        keyColl.initCrop(that.keyColl);
        keyColl.keyList = this;

        // List
        if (that.keyColl.keyMaps != null && that.keyColl.keyMaps[0] != null && that.list == that.keyColl.keyMaps[0].keysList) {
            list = (IList<E>) keyColl.keyMaps[0].keysList;
        } else {
            list = that.list.doCreate(-1);
        }

        if (DEBUG_CHECK)
            debugCheck();
    }

    /**
     * Initialize object for copy() operation.
     *
     * @param that source object
     */
    @SuppressWarnings("unchecked")
    void initCopy(KeyListImpl<E> that) {
        // KeyCollectionImpl
        keyColl = new KeyCollectionImpl<E>();
        keyColl.initCopy(that.keyColl);
        if (keyColl.keyList != null) {
            keyColl.keyList = this;
        }

        // List
        if (that.keyColl.keyMaps != null && that.keyColl.keyMaps[0] != null && that.list == that.keyColl.keyMaps[0].keysList) {
            list = (IList<E>) keyColl.keyMaps[0].keysList;
        } else {
            list = that.list.doCreate(-1);
            list.addAll(that.list);
        }

        if (DEBUG_CHECK)
            debugCheck();
    }

    @Override
    protected void doClone(IList<E> that) {
        // overridden as not used, initCopy() and initCrop() are used instead
    }

    /**
     * Returns a Set view of the element set.
     *
     * @return set view
     * @throws IllegalArgumentException if the element set cannot be viewed as Set
     */
    public Set<E> asSet() {
        return new CollectionAsSet<E>(this, false, true);
    }

    //-- Read

    /**
     * Determines whether this list is sorted or not.
     *
     * @return true if this a sorted list, false if not
     */
    public boolean isSorted() {
        return keyColl.isListSorted();
    }

    /**
     * Returns index of the key controlling the sorting of the collection (with 0 for the element).
     */
    public int getSortedByKey() {
        return keyColl.getListSortedByKey();
    }

    @Override
    public int capacity() {
        return list.capacity();
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public E get(int index) {
        return list.get(index);
    }

    @Override
    protected E doGet(int index) {
        return list.doGet(index);
    }

    @Override
    protected <T> void doGetAll(T[] array, int index, int len) {
        list.doGetAll(array, index, len);
    }

    /**
     * {@inheritDoc}
     * <p>
     * If the list defines maps for the keys, they are used to speed up the lookup operation.
     * So if the list defines a primary key, performance will be similar to a lookup in a set.
     */
    @Override
    public boolean contains(Object elem) {
        if (keyColl.keyMaps != null) {
            return keyColl.contains(elem);
        } else {
            return super.contains(elem);
        }
    }

    /**
     * Determines whether calling contains() will be fast, i.e. it can use some sort of key lookup instead of traversing through all elements.
     *
     * @return  true if calling contains() will be fast, otherwise false
     */
    boolean isContainsFast() {
        if (keyColl.keyMaps != null) {
            return keyColl.isContainsFast();
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * If the list is sorted, this is used to speed up the remove operation.
     * If the list defines unsorted map, this does not improve performance.
     */
    @Override
    public boolean remove(Object elem) {
        return super.remove(elem);
    }

    //--

    /**
     * {@inheritDoc}
     * <p><i>
     * Note that the behavior of the operation depends on the defined constraints.
     * </i></p>
     */
    // This method is only overwritten to change the Javadoc comment.
    @Override
    public boolean add(E elem) {
        return super.add(elem);
    }

    /**
     * {@inheritDoc}
     * <p><i>
     * Note that the behavior of the operation depends on the defined constraints.
     * </i></p>
     */
    // This method is only overwritten to change the Javadoc comment.
    @Override
    public void add(int index, E elem) {
        super.add(index, elem);
    }

    /**
     * {@inheritDoc}
     * <p><i>
     * Note that the behavior of the operation depends on the defined constraints.
     * </i></p>
     */
    // This method is only overwritten to change the Javadoc comment.
    @Override
    public E set(int index, E elem) {
        return super.set(index, elem);
    }

    @Override
    public void clear() {
        keyColl.clear();
        list.clear();
    }

    @Override
    public void ensureCapacity(int minCapacity) {
        // Make sure that we never allocate more slots than needed.
        // The add methods make sure that we never use to many slots.
        if (keyColl.maxSize != 0) {
            minCapacity = Math.min(minCapacity, keyColl.maxSize);
        }
        super.ensureCapacity(minCapacity);
    }

    @Override
    protected boolean doAdd(int index, E elem) {
        // This method is also called by doAdd(E)
        keyColl.checkElemAllowed(elem);

        // Handle maximum size and window
        if (keyColl.maxSize != 0 && size() >= keyColl.maxSize) {
            if (keyColl.movingWindow) {
                if (index == 0) {
                    // the element inserted at position 0 will be removed again due to the limited size
                    return false;
                }
                if (index == -1) {
                    index = size();
                }
                doRemove(0);
                index = index - 1;
            } else {
                KeyCollectionImpl.errorMaxSize();
            }
        }

        // Add element
        if (keyColl.isListSorted()) {
            // Sorted list
            if (index == -1) {
                index = keyColl.binarySearchAdd(elem);
                if (index < 0) {
                    index = -index - 1;
                }
            }
            keyColl.addListSorted(index, elem);
            // If list is sorted by element, keyColl points to list so no explicit call to its add method is needed
            if (!keyColl.isListSortedByElem()) {
                list.doAdd(index, elem);
            }
        } else {
            // Unsorted list
            keyColl.addListUnsorted(elem);
            if (index == -1) {
                // Element is already added to keyColl
                index = list.size();
            }
            list.doAdd(index, elem);
        }

        if (DEBUG_CHECK)
            debugCheck();
        return true;
    }

    @Override
    protected E doSet(int index, E elem) {
        keyColl.checkElemAllowed(elem);

        E remove = doGet(index);
        if (keyColl.isListSorted()) {
            keyColl.setSorted(index, elem, remove);

        } else {
            keyColl.remove(remove);
            try {
                keyColl.add(elem);
            } catch (RuntimeException e) {
                keyColl.add(remove);
                throw e;
            }
        }
        list.doSet(index, elem);
        if (DEBUG_CHECK)
            debugCheck();
        return remove;
    }

    @Override
    protected E doRemove(int index) {
        E removed = list.get(index);
        keyColl.remove(removed);
        if (!keyColl.isListSortedByElem()) {
            list.remove(index);
        }
        if (DEBUG_CHECK)
            debugCheck();
        return removed;
    }

    @Override
    protected void doRemoveAll(int index, int len) {
        KeyMap<?, ?> ignore = (keyColl.isListSortedByElem()) ? keyColl.keyMaps[0] : null;
        for (int i = 0; i < len; i++) {
            E removed = list.get(index + i);
            keyColl.remove(removed, ignore);
        }
        list.doRemoveAll(index, len);
        if (DEBUG_CHECK)
            debugCheck();
    }

    @Override
    protected E doReSet(int index, E elem) {
        return list.doReSet(index, elem);
    }

    /**
     * {@inheritDoc}
     * <p>
     * If the list is sorted, this is used to speed up the lookup operation.
     * If the list defines unsorted map, this does not improve performance.
     */
    @Override
    @SuppressWarnings("unchecked")
    public int indexOf(Object elem) {
        if (keyColl.isListSorted()) {
            return keyColl.indexOfSorted((E) elem);
        } else {
            return super.indexOf(elem);
        }
    }

    /**
     * Determines whether calling indexOf() will be fast, i.e. it can use some sort of key lookup instead of traversing through all elements.
     *
     * @return  true if calling indexOf() will be fast, otherwise false
     */
    boolean isIndexOfFast() {
        return keyColl.isListSorted();
    }

    /**
     * Find given key and return its index.
     *
     * @param keyIndex  key index
     * @param key       key to find
     * @return          index of key or -1 if not found
     */
    public int indexOfKey(int keyIndex, Object key) {
        return indexOfKey(keyIndex, key, 0);
    }

    /**
     * Find given key and return its index.
     *
     * @param keyIndex  key index
     * @param key       key to find
     * @param start     start index for search
     * @return          index of key or -1 if not found
     */
    public int indexOfKey(int keyIndex, Object key, int start) {
        int size = size();
        for (int i = start; i < size; i++) {
            Object elemKey = keyColl.getKey(keyIndex, doGet(i));
            if (equalsElem(elemKey, key)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks whether the specified key exists in this list.
     *
     * @param keyIndex  key index
     * @param key       key to look for
     * @return          true if the key exists, otherwise false
     */
    public boolean containsKey(int keyIndex, Object key) {
        return indexOfKey(keyIndex, key) != -1;
    }

    /**
     * Returns mapper for specified key map.
     *
     * @param keyIndex  key index
     * @return          mapper for specified key map
     */
    public Function<E, Object> getKeyMapper(int keyIndex) {
        return keyColl.getKeyMapper(keyIndex);
    }

    /**
     * Returns value for given key.
     * If there are several values for this key, the first is returned.
     * If the key is not found, null is returned.
     *
     * @param keyIndex  key index
     * @param key       key to find
     * @return          value of specified key or null
     */
    public E getByKey(int keyIndex, Object key) {
        return keyColl.getByKey(keyIndex, key);
    }

    /**
     * Returns a list with all elements with the specified key.
     *
     * @param keyIndex  key index
     * @param key       key which elements must have
     * @return          list with all elements
     */
    public IList<E> getAllByKey(int keyIndex, Object key) {
        return (IList<E>) keyColl.getAllByKey(keyIndex, key);
    }

    public IList<E> getAllByKeys(int keyIndex1, Object key1, int keyIndex2, Object key2) {
        return (IList<E>) keyColl.getAllByKeys(keyIndex1, key1, keyIndex2, key2);
    }

    /**
     * Returns number of elements with specified key.
     *
     * @param keyIndex  key index
     * @param key       key which elements must have
     * @return          number of elements with key
     */
    public int getCountByKey(int keyIndex, Object key) {
        return keyColl.getCountByKey(keyIndex, key);
    }

    /**
     * Removes element by key.
     * If there are duplicates, only one element is removed.
     *
     * @param keyIndex  key index
     * @param key       key of element to remove
     * @return          removed element or null if no element has been removed
     */
    protected E removeByKey(int keyIndex, Object key) {
        Option<E> removed = keyColl.doRemoveByKey(keyIndex, key);
        if (removed.hasValue()) {
            int index = list.indexOf(removed.getValue());
            if (index == -1) {
                KeyCollectionImpl.errorInvalidData();
            }
            list.doRemove(index);
        }
        if (DEBUG_CHECK)
            debugCheck();
        return removed.getValueOrNull();
    }

    /**
     * Put element by key into list.
     *
     * @param keyIndex  key index
     * @param elem      element to put
     * @param replace   true to replace an existing element with the same key, false to let the element unchanged
     * @return          element with the same key (i.e. element which was replaced if replace is true / which was left unchanged if false),
     *                  null if no element with the same key has been found
     */
    protected E putByKey(int keyIndex, E elem, boolean replace) {
        int index;
        if (keyIndex == 0 && (keyColl.keyMaps == null || keyColl.keyMaps[0] == null)) {
            index = indexOf(elem);
        } else {
            Object key = keyColl.getKey(keyIndex, elem);
            index = indexOfKey(keyIndex, key);
        }
        E replaced = null;
        if (index == -1) {
            doAdd(-1, elem);
        } else {
            if (replace) {
                replaced = doSet(index, elem);
            } else {
                replaced = doGet(index);
            }
        }
        if (DEBUG_CHECK)
            debugCheck();
        return replaced;
    }

    /**
     * Removes element by key.
     * If there are duplicates, all elements are removed.
     *
     * @param keyIndex  key index
     * @param key       key of element to remove
     * @return          true if elements have been removed, false otherwise
     */
    protected IList<E> removeAllByKey(int keyIndex, Object key) {
        IList<E> removeds = doCreate(-1);
        keyColl.removeAllByKey(keyIndex, key, removeds);
        if (!removeds.isEmpty()) {
            if (!keyColl.isListSortedByElem()) {
                if (!list.removeAll(removeds)) {
                    KeyCollectionImpl.errorInvalidData();
                }
            }
        }
        if (DEBUG_CHECK)
            debugCheck();
        return removeds;
    }

    /**
     * Returns list with all keys in element order.
     *
     * @param keyIndex  key index
     * @return          list with all keys
     */
    protected IList<Object> getAllKeys(int keyIndex) {
        Function<E, Object> mapper = keyColl.getKeyMap(keyIndex).mapper;
        GapList<Object> list = GapList.create();
        for (E obj : this) {
            list.add(mapper.apply(obj));
        }
        return list;
    }

    /**
     * Returns set with all distinct keys.
     *
     * @param keyIndex  key index
     * @return          set with all distinct keys
     */
    public Set<?> getDistinctKeys(int keyIndex) {
        return keyColl.getDistinctKeys(keyIndex);
    }

    /**
     * Returns iterator for all distinct keys.
     *
     * @param keyIndex  key index
     * @return          iterator for all distinct keys
     */
    public Iterator<?> iteratorDistinctKeys(int keyIndex) {
        return keyColl.iteratorDistinctKeys(keyIndex);
    }

    /**
     * Returns count for all distinct keys.
     *
     * @param keyIndex  key index
     * @return          count for all distinct keys
     */
    public int countDistinctKeys(int keyIndex) {
        return keyColl.countDistinctKeys(keyIndex);
    }

    @Override
    public <K> int binarySearch(int index, int len, K key, Comparator<? super K> comparator) {
        // If this is a sorted list, it is obvious that binarySearch will work.
        // The list can however also be sorted without been declared as being ordered,
        // so we just try to do the binary search (as if Collections.binarySearch is called)
        return list.binarySearch(index, len, key, comparator);
    }

    @Override
    public void sort(int index, int len, Comparator<? super E> comparator) {
        if (keyColl.isListSorted()) {
            // If this is sorted list, the comparator must be equal to the specified one
            if (keyColl.isListSortedByElem()) {
                if (GapList.equalsElem(comparator, keyColl.getElemSortComparator())) {
                    return;
                }
            }
            throw new IllegalArgumentException("Different comparator specified for sorted list");
        } else {
            list.sort(index, len, comparator);
        }
    }

    //-- Element methods

    @Override
    public IList<E> getAll(E elem) {
        if (keyColl.hasElemSet()) {
            return getAllByKey(0, elem);
        } else {
            return list.getAll(elem);
        }
    }

    @Override
    public int count(E elem) {
        if (keyColl.hasElemSet()) {
            return getCountByKey(0, elem);
        } else {
            return list.count(elem);
        }
    }

    @Override
    public IList<E> removeAll(E elem) {
        if (keyColl.hasElemSet()) {
            return removeAllByKey(0, elem);
        } else {
            return list.removeAll(elem);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<E> getDistinct() {
        if (keyColl.hasElemSet()) {
            return (Set<E>) getDistinctKeys(0);
        } else {
            return super.getDistinct();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<E> iteratorDistinct() {
        if (keyColl.hasElemSet()) {
            return (Iterator<E>) iteratorDistinctKeys(0);
        } else {
            return super.iteratorDistinct();
        }
    }

    @Override
    public int countDistinct() {
        if (keyColl.hasElemSet()) {
            return countDistinctKeys(0);
        } else {
            return super.countDistinct();
        }
    }

    /**
     * Adds or replaces element.
     * If there is no such element, the element is added.
     * If there is such an element, the element is replaced.
     * So said simply, it is a shortcut for the following code:
     * <pre>
     * if (contains(elem)) {
     *   remove(elem);
     * }
     * add(elem);
     * </pre>
     * However the method is atomic in the sense that all or none operations are executed.
     * So if there is already such an element, but adding the new one fails due to a constraint violation,
     * the old element remains in the list.
     *
     * @param elem  element
     * @return      element which has been replaced or null otherwise
     */
    protected E put(E elem) {
        // If contains() is fast but indexOf() slow, we check first whether the element is contained and then call
        // add if it is not (we do not use the index for adding, so we can skip this costly operation)
        if (!isIndexOfFast() && isContainsFast()) {
            if (!contains(elem)) {
                add(elem);
                return null;
            }
        }

        int index = indexOf(elem);
        if (index != -1) {
            return set(index, elem);
        } else {
            add(elem);
            return null;
        }
    }

    /**
     * Invalidate element, i.e. all keys of the element are extracted
     * again and stored in the key maps. Old key values are removed if needed.
     * You must call an invalidate method if an element's key value has changed after adding it to the collection.
     *
     * @param elem element to invalidate
     */
    protected void invalidate(E elem) {
        keyColl.invalidate(elem);
        if (keyColl.isListSorted() && !keyColl.isListSortedByElem()) {
            int oldIndex = super.indexOf(elem);
            int newIndex = keyColl.indexOfSorted(elem);
            if (oldIndex != newIndex) {
                list.doRemove(oldIndex);
                if (oldIndex < newIndex) {
                    newIndex--;
                }
                list.doAdd(newIndex, elem);
            }
        }
        if (DEBUG_CHECK)
            debugCheck();
    }

    /**
     * Invalidate key value of element.
     * You must call an invalidate method if an element's key value has changed after adding it to the collection.
     *
     * @param keyIndex  key index
     * @param oldKey    old key value
     * @param newKey    new key value
     * @param elem      element to invalidate (can be null if there are no duplicates with this key)
     */
    protected void invalidateKey(int keyIndex, Object oldKey, Object newKey, E elem) {
        elem = keyColl.doInvalidateKey(keyIndex, oldKey, newKey, elem);
        if (keyColl.orderByKey == keyIndex && list != null) {
            list.doRemove(super.indexOf(elem));
            int index = keyColl.indexOfSorted(elem);
            list.doAdd(index, elem);
        }
        if (DEBUG_CHECK)
            debugCheck();
    }

    @Override
    protected E getDefaultElem() {
        return null;
    }

    @Override
    protected void doEnsureCapacity(int minCapacity) {
        list.doEnsureCapacity(minCapacity);
    }

    @Override
    public void trimToSize() {
        list.trimToSize();
    }

    @Override
    protected IList<E> doCreate(int capacity) {
        return list.doCreate(capacity);
    }

    // --- ReadOnlyKeyListImpl ---

    @Override
    public boolean isReadOnly() {
        return this instanceof ReadOnlyKeyListImpl;
    }

    @Override
    public KeyListImpl<E> unmodifiableList() {
        if (isReadOnly()) {
            return this;
        } else {
            return new ReadOnlyKeyListImpl<E>(this);
        }
    }

    @Override
    public KeyListImpl<E> immutableList() {
        if (isReadOnly()) {
            return this;
        } else {
            return new ReadOnlyKeyListImpl<E>(copy());
        }
    }

    protected KeyListImpl(KeyListImpl<E> that) {
        if (that != null) {
            doAssign(that);
        }
    }

    /**
     * A read-only version of {@link KeyList}.
     * It is used to implement both unmodifiable and immutable lists.
     * Note that the client cannot change the list, but the content may change if the underlying list is changed.
     */
    protected static class ReadOnlyKeyListImpl<E> extends KeyListImpl<E> {

        /** UID for serialization */
        private static final long serialVersionUID = -1352274047348922584L;

        /**
         * Private constructor used internally.
         *
         * @param that  list to create an immutable view of
         */
        protected ReadOnlyKeyListImpl(KeyListImpl<E> that) {
            super(that);
        }

        @Override
        protected void doEnsureCapacity(int capacity) {
            error();
        }

        @Override
        protected boolean doAdd(int index, E elem) {
            error();
            return false;
        }

        @Override
        protected E doSet(int index, E elem) {
            error();
            return null;
        }

        @Override
        protected E doReSet(int index, E elem) {
            error();
            return null;
        }

        @Override
        protected E doRemove(int index) {
            error();
            return null;
        }

        @Override
        protected void doRemoveAll(int index, int len) {
            error();
        }

        @Override
        protected void doClear() {
            error();
        }

        @Override
        protected void doModify() {
            error();
        }

        @Override
        protected E removeByKey(int keyIndex, Object key) {
            error();
            return null;
        }

        @Override
        protected IList<E> removeAllByKey(int keyIndex, Object key) {
            error();
            return null;
        }

        @Override
        protected E putByKey(int keyIndex, E elem, boolean replace) {
            error();
            return null;
        }

        @Override
        protected void invalidateKey(int keyIndex, Object oldKey, Object newKey, E elem) {
            error();
        }

        /**
         * Throw exception if an attempt is made to change an immutable list.
         */
        private void error() {
            throw new UnsupportedOperationException("list is immutable");
        }
    }

}
