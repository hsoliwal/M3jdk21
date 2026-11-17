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
import java.util.Objects;

import jdk.m3.collections.brownies.exceptions.DuplicateKeyException;
import jdk.m3.collections.brownies.helper.GapLists;

/**
 * This class implements helper functions.
 * Public access is granted through {@link GapLists}.
 *
 * @author Thomas Mauch
 */
@SuppressWarnings("missing-explicit-ctor")
public class GapListsImpl {

    /**
     * Implementation of {@link KeyListImpl#addAll} which adds an explanatory exception message
     * why adding failed with a {@link DuplicateKeyException}.
     */
    protected static <E, EE extends E> boolean addAllExplained(KeyListImpl<E> list, Collection<EE> adds) {
        boolean added = false;
        for (EE add : adds) {
            if (addExplained(list, add)) {
                added = true;
            }
        }
        return added;
    }

    /**
     * Implementation of {@link KeyListImpl#add} which adds an explanatory exception message
     * why adding failed with a {@link DuplicateKeyException}.
     */
    protected static <E, EE extends E> boolean addExplained(KeyListImpl<E> list, EE add) {
        try {
            return list.add(add);

        } catch (DuplicateKeyException e) {
            Object key = e.getKey();
            E exists = null;
            int numKeyMaps = list.keyColl.getNumKeyMaps();
            for (int i = 1; i < numKeyMaps; i++) {
                Object k = list.getKeyMapper(i).apply(add);
                if (Objects.equals(k, key)) {
                    exists = list.getByKey(i, k);
                    if (exists != null) {
                        break;
                    }
                }
            }

            if (exists != null) {
                String msg = e.getMessage() + "\n (exists: " + exists + ", add: " + add + ")";
                throw new IllegalArgumentException(msg, e);
            } else {
                throw e;
            }
        }
    }

    /**
     * Implementation of {@link KeyCollectionImpl#addAll} which adds an explanatory exception message
     * why adding failed with a {@link DuplicateKeyException}.
     */
    protected static <E, EE extends E> boolean addAllExplained(KeyCollectionImpl<E> coll, Collection<EE> adds) {
        boolean added = false;
        for (EE add : adds) {
            if (addExplained(coll, add)) {
                added = true;
            }
        }
        return added;
    }

    /**
     * Implementation of {@link KeyCollectionImpl#add} which adds an explanatory exception message
     * why adding failed with a {@link DuplicateKeyException}.
     */
    protected static <E, EE extends E> boolean addExplained(KeyCollectionImpl<E> coll, EE add) {
        try {
            return coll.add(add);

        } catch (DuplicateKeyException e) {
            Object key = e.getKey();
            E exists = null;
            int numKeyMaps = coll.getNumKeyMaps();
            for (int i = 1; i < numKeyMaps; i++) {
                Object key1 = coll.getKeyMapper(1).apply(add);
                if (Objects.equals(key1, key)) {
                    exists = coll.getByKey(i, key1);
                    if (exists != null) {
                        break;
                    }
                }
            }

            if (exists != null) {
                String msg = e.getMessage() + "\n (exists: " + exists + ", add: " + add + ")";
                throw new IllegalArgumentException(msg);
            } else {
                throw e;
            }
        }
    }

}
