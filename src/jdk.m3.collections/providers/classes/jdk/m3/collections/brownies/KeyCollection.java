/*
 * Copyright 2013 by Thomas Mauch
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

import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * KeyCollection implements a collection.
 * It can provide fast access to its elements like a Set, but allows duplicates.
 * The elements allowed in the collection can be constraint (null/duplicate values).
 *
 * @author Thomas Mauch
 *
 * @see Key1List
 * @param <E> type of elements stored in the collection
 */
@SuppressWarnings({"serial", "this-escape"})
public class KeyCollection<E> extends KeyCollectionImpl<E> {

    /** Build {@link KeyCollection} with specified builder function */
    public static <E> KeyCollection<E> buildCollection(UnaryOperator<KeyCollection.Builder<E>> builder) {
        return builder.apply(new KeyCollection.Builder<>()).build();
    }

    /**
     * Builder to construct KeyCollection instances.
     */
    public static class Builder<E> extends KeyCollectionImplBuilder<E, Builder<E>> {

        /**
         * Default constructor.
         */
        public Builder() {
            this(null);
        }

        /**
         * Protected constructor used if extending KeyCollection.
         *
         * @param keyColl   key collection
         */
        protected Builder(KeyCollection<E> keyColl) {
            this.keyColl = keyColl;
            initKeyMapBuilder(0);
        }

        /**
         * @return created collection
         */
        public KeyCollection<E> build() {
            if (keyColl == null) {
                keyColl = new KeyCollection<>();
            }
            build(keyColl, false);
            init(keyColl);
            return (KeyCollection<E>) keyColl;
        }

        // -- Builder

        @Override
        public Builder<E> withElemCount(boolean count) {
            return super.withElemCount(count);
        }
    }

    /**
     * Protected constructor used by builder or derived collections.
     */
    protected KeyCollection() {
    }

    /**
     * @return builder to use in extending classes
     */
    protected Builder<E> getBuilder() {
        return new Builder<>(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public KeyCollection<E> copy() {
        return (KeyCollection<E>) super.copy();
    }

    @SuppressWarnings("unchecked")
    @Override
    public KeyCollection<E> crop() {
        return (KeyCollection<E>) super.crop();
    }

    //-- Element methods

    @Override
    public Collection<E> getAll(E elem) {
        return super.getAll(elem);
    }

    @Override
    public Collection<E> removeAll(E elem) {
        return super.removeAll(elem);
    }

    @Override
    public E put(E elem) {
        return super.put(elem);
    }

    @Override
    public void invalidate(E elem) {
        super.invalidate(elem);
    }

    @Override
    @SuppressWarnings("unchecked")
    public KeyCollection<E> filter(Predicate<? super E> filter) {
        return (KeyCollection<E>) super.filter(filter);
    }

    /**
     * Determines whether this collection is only counting, but not storing all elements.
     *
     * @return true if this collection is only counting, but not storing all elements
     */
    public boolean hasElemCount() {
        return keyMaps[0].count;
    }

}
