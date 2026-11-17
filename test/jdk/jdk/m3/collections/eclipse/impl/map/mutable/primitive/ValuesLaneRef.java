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

import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableIntCollection;
import jdk.m3.collections.eclipse.api.iterator.IntIterator;

/**
 * Pre-patch bodies of AbstractMutableIntValuesMap.containsAll(IntIterable) and
 * AbstractIntValuesCollection.removeAll(IntIterable), copied verbatim and retargeted from the
 * outer instance to parameters. The source stays the first parameter, so a null source raises
 * the same helpful NullPointerException message as the owner. contains(int) and remove(int)
 * are untouched by the patch, so running these on a twin map reproduces pre-patch behaviour.
 */
//FLLDCIM: LEETCODIFY — reference: verbatim pre-patch code path driven on a twin map
final class ValuesLaneRef {

    private ValuesLaneRef() {
    }

    static boolean containsAll(IntIterable source, AbstractMutableIntValuesMap map) {
        return source.allSatisfy((int value) -> map.contains(value));
    }

    static boolean removeAll(IntIterable source, AbstractMutableIntValuesMap map,
            MutableIntCollection view) {
        int oldSize = map.size();

        IntIterator iterator = source.intIterator();
        while (iterator.hasNext()) {
            view.remove(iterator.next());
        }
        return oldSize != map.size();
    }
}
