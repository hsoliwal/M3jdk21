/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections.eclipse.impl.map.mutable.primitive;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.iterator.MutableIntIterator;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.IntArrayList;

/**
 * Foreign fixtures that must keep the pre-patch path: an IntArrayList subclass running a hook
 * before each element (throwing or mutating sources), an owner subclass overriding
 * containsValue and removeKey, and a values view of a real IntIntHashMap that is not the map's
 * own nest member.
 */
//FLLDCIM: LEETCODIFY — foreign sources, owners and views for the fallback differential
final class ValuesLaneSources {

    private ValuesLaneSources() {
    }

    /** IntArrayList subclass whose intIterator and allSatisfy run a hook before each element. */
    @SuppressWarnings("serial")
    static final class HookedList extends IntArrayList {
        private final IntConsumer hook;

        HookedList(IntConsumer hook, int... items) {
            super(items);
            this.hook = hook;
        }

        @Override
        public MutableIntIterator intIterator() {
            return new HookedIterator(super.intIterator(), this.hook);
        }

        @Override
        public boolean allSatisfy(IntPredicate predicate) {
            MutableIntIterator iterator = this.intIterator();
            while (iterator.hasNext()) {
                if (!predicate.accept(iterator.next())) {
                    return false;
                }
            }
            return true;
        }
    }

    /** Iterator that runs the hook with the element index before returning each element. */
    private static final class HookedIterator implements MutableIntIterator {
        private final MutableIntIterator inner;
        private final IntConsumer hook;
        private int index;

        HookedIterator(MutableIntIterator inner, IntConsumer hook) {
            this.inner = inner;
            this.hook = hook;
        }

        @Override
        public boolean hasNext() {
            return this.inner.hasNext();
        }

        @Override
        public int next() {
            this.hook.accept(this.index++);
            return this.inner.next();
        }

        @Override
        public void remove() {
            this.inner.remove();
        }
    }

    /** Owner subclass: containsValue answers true, removeKey logs each key. */
    @SuppressWarnings("serial")
    static final class TrueContainsMap extends IntIntHashMap {
        final List<Integer> removedKeys = new ArrayList<>();

        @Override
        public boolean containsValue(int value) {
            return true;
        }

        @Override
        public void removeKey(int key) {
            this.removedKeys.add(key);
            super.removeKey(key);
        }
    }

    /** Foreign values view bound to a real IntIntHashMap; remove(int) logs each value. */
    static final class LoggingView extends AbstractMutableIntValuesMap.AbstractIntValuesCollection {
        final List<Integer> log = new ArrayList<>();
        private final IntIntHashMap map;

        LoggingView(IntIntHashMap map) {
            map.super();
            this.map = map;
        }

        @Override
        public MutableIntIterator intIterator() {
            return this.map.intIterator();
        }

        @Override
        public boolean remove(int value) {
            this.log.add(value);
            return this.map.values().remove(value);
        }

        @Override
        public boolean retainAll(IntIterable elements) {
            return this.map.values().retainAll(elements);
        }
    }
}
