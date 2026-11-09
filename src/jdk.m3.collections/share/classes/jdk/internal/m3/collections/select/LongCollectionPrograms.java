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
package jdk.internal.m3.collections.select;

import jdk.internal.m3.collections.LongOrderedSet;
import jdk.internal.m3.collections.packed.PackedLongBagContract;
import jdk.internal.m3.collections.packed.PackedLongTopK;
import jdk.internal.m3.collections.packed.PackedMutableLongSet;
import jdk.m3.collections.LongCollection;
import jdk.m3.collections.LongIterator;
import java.util.Objects;

/**
 * Reusable primitive collection programs. Inputs are borrowed and never changed;
 * outputs are new mutable collections. No boxed element, stream or tuple pipeline.
 * Hints size output storage; they are not admission caps. Top-K has an explicit bound.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/selection/LongCollectionPrograms.java}
 * blob {@code 58a8ae05902f4dcf7c63b1b8510601bacfb29ffb} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections catalogue chain after API_UNION U3 (v6.2): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LongCollectionPrograms {
    private LongCollectionPrograms() {}

    public static PackedLongBagContract frequencies(LongCollection input, int expectedDistinct) {
        Objects.requireNonNull(input, "input");
        var request = CollectionRequest.forFamily(CollectionFamilies.LONG_BAG)
                .withExpectedSize(expectedDistinct)
                .withWorkload(new CollectionRequest.Workload(0, 100, 0, 0, 0, 0));
        PackedLongBagContract output = FuzzyCollectionFactory.standard().select(request).create();
        LongIterator iterator = input.iterator();
        while (iterator.hasNext()) { output.add(iterator.nextLong()); }
        return output;
    }

    public static LongOrderedSet distinctInEncounterOrder(LongCollection input, int expectedDistinct) {
        Objects.requireNonNull(input, "input");
        LongOrderedSet output = FuzzyCollectionFactory.standard().select(
                CollectionRequest.forFamily(CollectionFamilies.ORDERED_LONG_SET).withExpectedSize(expectedDistinct)).create();
        LongIterator iterator = input.iterator();
        while (iterator.hasNext()) { output.add(iterator.nextLong()); }
        return output;
    }

    public static PackedMutableLongSet union(LongCollection left, LongCollection right, int expectedDistinct) {
        Objects.requireNonNull(left, "left"); Objects.requireNonNull(right, "right");
        var request = CollectionRequest.forFamily(CollectionFamilies.LONG_SET).withExpectedSize(expectedDistinct)
                .withWorkload(new CollectionRequest.Workload(0, 100, 0, 0, 0, 0));
        PackedMutableLongSet output = FuzzyCollectionFactory.standard().select(request).create();
        LongIterator first = left.iterator(), second = right.iterator();
        while (first.hasNext()) { output.add(first.nextLong()); }
        while (second.hasNext()) { output.add(second.nextLong()); }
        return output;
    }

    public static PackedLongTopK topK(LongCollection input, ConfiguredCollectionProviders.Bound bound,
            ConfiguredCollectionProviders.Direction direction) {
        Objects.requireNonNull(input, "input");
        var provider = ConfiguredCollectionProviders.topK(bound, direction);
        PackedLongTopK output = ConfiguredCollectionProviders.plan(provider,
                Math.min(input.size(), bound.maximumEntries())).create();
        LongIterator iterator = input.iterator();
        while (iterator.hasNext()) { output.offer(iterator.nextLong()); }
        return output;
    }
}
