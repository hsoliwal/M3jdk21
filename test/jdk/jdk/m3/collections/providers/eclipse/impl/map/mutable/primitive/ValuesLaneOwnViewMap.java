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
import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableIntCollection;
import jdk.m3.collections.eclipse.api.iterator.MutableIntIterator;

/**
 * A top-level subclass of IntIntHashMap whose values() is its own nest member: an
 * AbstractIntValuesCollection whose remove(int) logs each value before delegating. Its nest host
 * is this class, not a module map, so the patched removeAll must keep the pre-patch per-element
 * remove(int) calls even though the nest host equals the owner class.
 */
//FLLDCIM: LEETCODIFY — foreign owner whose own values view must keep the pre-patch path
@SuppressWarnings("serial")
final class ValuesLaneOwnViewMap extends IntIntHashMap {

    final List<Integer> removed = new ArrayList<>();

    @Override
    public MutableIntCollection values() {
        return new OwnValues();
    }

    private final class OwnValues extends AbstractIntValuesCollection {

        @Override
        public MutableIntIterator intIterator() {
            return ValuesLaneOwnViewMap.this.intIterator();
        }

        @Override
        public boolean remove(int value) {
            ValuesLaneOwnViewMap.this.removed.add(value);
            return ValuesLaneOwnViewMap.super.values().remove(value);
        }

        @Override
        public boolean retainAll(IntIterable source) {
            return ValuesLaneOwnViewMap.super.values().retainAll(source);
        }
    }
}
