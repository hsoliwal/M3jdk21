/*
 * Copyright 2014 by Thomas Mauch
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
import java.util.Comparator;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import jdk.m3.collections.brownies.KeyCollectionImpl.KeyCollectionImplBuilder;

/**
 * Helper class to instances of key list, key collection, key set with more than 2 keys.
 *
 * @author Thomas Mauch
 */
@SuppressWarnings({"missing-explicit-ctor", "serial", "unchecked"})
public class KeyCollections {

    /**
     * Class {@link KeyNList} implements a key list with a dynamic number of keys.
     *
     * @author Thomas Mauch
     *
     * @see Key2List
     * @param <E> type of elements stored in the list
     */
    public static class KeyNList<E> extends KeyListImpl<E> {

        /** Build {@link KeyNList} with specified builder function */
        public static <E> KeyNList<E> buildList(UnaryOperator<KeyNList.Builder<E>> builder) {
            return builder.apply(new KeyNList.Builder<>()).build();
        }

        /**
         * Builder for {@link KeyNList}.
         */
        public static class Builder<E> extends KeyCollectionImplBuilder<E, Builder<E>> {

            /**
             * Default constructor.
             */
            public Builder() {
                this(null);
            }

            /**
             * Protected constructor used if extending KeyNList.
             *
             * @param keyList   key list
             */
            protected Builder(KeyNList<E> keyList) {
                this.keyList = keyList;
            }

            public KeyNList<E> build() {
                if (keyColl == null) {
                    keyColl = new KeyCollectionImpl<>();
                }
                build(keyColl, true);
                if (keyList == null) {
                    keyList = new KeyNList<E>();
                }
                init(keyColl, keyList);
                return (KeyNList<E>) keyList;
            }
        }

        /**
         * @return builder to use in extending classes
         */
        protected Builder<E> getBuilder() {
            return new Builder<E>(this);
        }
    }

    /**
     * Class {@link KeyNCollection} implements a key collection with a dynamic number of keys.
     *
     * @author Thomas Mauch
     *
     * @see Key2Collection
     * @param <E> type of elements stored in the collection
     */
    public static class KeyNCollection<E> extends KeyCollectionImpl<E> {

        /** Build {@link KeyNList} with specified builder function */
        public static <E> KeyNCollection<E> buildCollection(UnaryOperator<KeyNCollection.Builder<E>> builder) {
            return builder.apply(new KeyNCollection.Builder<>()).build();
        }

        /**
         * Builder for {@link KeyNCollection}.
         */
        public static class Builder<E> extends KeyCollectionImplBuilder<E, Builder<E>> {

            /**
             * Default constructor.
             */
            public Builder() {
                this(null);
            }

            /**
             * Protected constructor used if extending KeyNCollection.
             *
             * @param keyColl   key collection
             */
            protected Builder(KeyNCollection<E> keyColl) {
                this.keyColl = keyColl;
            }

            /**
             * @return created collection
             */
            public KeyNCollection<E> build() {
                if (keyColl == null) {
                    keyColl = new KeyNCollection<>();
                }
                build(keyColl, false);
                init(keyColl);
                return (KeyNCollection<E>) keyColl;
            }
        }

        /**
         * @return builder to use in extending classes
         */
        protected Builder<E> getBuilder() {
            return new Builder<E>(this);
        }
    }

    /**
     * Class {@link KeyNSet} implements a key set with a dynamic number of keys.
     *
     * @author Thomas Mauch
     *
     * @see Key2Set
     * @param <E> type of elements stored in the set
     */
    public static class KeyNSet<E> extends KeyNCollection<E> implements Set<E> {

        /** Build {@link KeyNList} with specified builder function */
        public static <E> KeyNSet<E> buildSet(UnaryOperator<KeyNSet.Builder<E>> builder) {
            return builder.apply(new KeyNSet.Builder<>()).build();
        }

        /**
         * Builder for {@link KeyNSet}.
         */
        public static class Builder<E> extends KeyNCollection.Builder<E> {

            /**
             * Default constructor.
             */
            public Builder() {
                this(null);
            }

            /**
             * Protected constructor used if extending KeyNSet.
             *
             * @param keySet    key set
             */
            protected Builder(KeyNSet<E> keySet) {
                this.keyColl = keySet;
            }

            /**
             * @return created collection
             */
            @Override
            public KeyNSet<E> build() {
                withSetBehavior(true);
                withElemDuplicates(false);

                if (keyColl == null) {
                    keyColl = new KeyNSet<>();
                }
                build(keyColl, false);
                init(keyColl);
                return (KeyNSet<E>) keyColl;
            }

            // Builder

            @Override
            public Builder<E> withCapacity(int capacity) {
                return (Builder<E>) super.withCapacity(capacity);
            }

            @Override
            public Builder<E> withConstraint(Predicate<E> constraint) {
                return (Builder<E>) super.withConstraint(constraint);
            }

            @Override
            public Builder<E> withContent(Collection<? extends E> elements) {
                return (Builder<E>) super.withContent(elements);
            }

            @Override
            @SuppressWarnings("unchecked")
            public Builder<E> withContent(E... elements) {
                return (Builder<E>) super.withContent(elements);
            }

            @Override
            public Builder<E> withMaxSize(int maxSize) {
                return (Builder<E>) super.withMaxSize(maxSize);
            }

            @Override
            public Builder<E> withNull(boolean allowNull) {
                return (Builder<E>) super.withNull(allowNull);
            }

            @Override
            public Builder<E> withSetBehavior(boolean setBehavior) {
                return (Builder<E>) super.withSetBehavior(setBehavior);
            }

            // Key Map N

            @SuppressWarnings("rawtypes")
            @Override
            public Builder<E> withKeyMap(int keyIndex, Function mapper) {
                return (Builder<E>) super.withKeyMap(keyIndex, mapper);
            }

            @SuppressWarnings("rawtypes")
            @Override
            public Builder<E> withPrimaryKeyMap(int keyIndex, Function mapper) {
                return (Builder<E>) super.withPrimaryKeyMap(keyIndex, mapper);
            }

            @SuppressWarnings("rawtypes")
            @Override
            public Builder<E> withUniqueKeyMap(int keyIndex, Function mapper) {
                return (Builder<E>) super.withUniqueKeyMap(keyIndex, mapper);
            }

            @Override
            public Builder<E> withKeyNull(int keyIndex, boolean allowNull) {
                return (Builder<E>) super.withKeyNull(keyIndex, allowNull);
            }

            @Override
            public Builder<E> withKeyDuplicates(int keyIndex, boolean allowDuplicates) {
                return (Builder<E>) super.withKeyDuplicates(keyIndex, allowDuplicates);
            }

            @Override
            public Builder<E> withKeyDuplicates(int keyIndex, boolean allowDuplicates, boolean allowDuplicatesNull) {
                return (Builder<E>) super.withKeyDuplicates(keyIndex, allowDuplicates, allowDuplicatesNull);
            }

            @Override
            public Builder<E> withKeySort(int keyIndex, boolean sort) {
                return (Builder<E>) super.withKeySort(keyIndex, sort);
            }

            @Override
            public Builder<E> withKeySort(int keyIndex, Comparator<?> comparator) {
                return (Builder<E>) super.withKeySort(keyIndex, comparator);
            }

            @Override
            public Builder<E> withKeySort(int keyIndex, Comparator<?> comparator, boolean sortNullsFirst) {
                return (Builder<E>) super.withKeySort(keyIndex, comparator, sortNullsFirst);
            }

            @Override
            public Builder<E> withOrderByKey(int keyIndex, boolean orderBy) {
                return (Builder<E>) super.withOrderByKey(keyIndex, orderBy);
            }

            @Override
            protected Builder<E> withOrderByKey(int keyIndex, Class<?> type) {
                return (Builder<E>) super.withOrderByKey(keyIndex, type);
            }
        }

        /**
         * @return builder to use in extending classes
         */
        @Override
        protected Builder<E> getBuilder() {
            return new Builder<E>(this);
        }
    }
}
