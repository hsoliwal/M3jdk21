/*******************************************************************************
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
package com.m3.collections.interop;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class M3ApacheStreamsTest {
    @Test void orderedMultimapAndBag() {
        var data = new ArrayListValuedHashMap<String,Integer>();
        data.put("x",1); data.put("x",1);
        var frozen = M3ApacheStreams.snapshotValues(data,"x");
        var live = M3ApacheStreams.liveValues(data,"x");
        data.put("x",3);
        assertEquals(List.of(1,1),frozen.collect(Collectors.toList()));
        assertEquals(List.of(1,1,3),live.collect(Collectors.toList()));
        assertEquals(List.of(2,2,6),live.map(x -> x*2).collect(Collectors.toList()));
        var bag = new HashBag<Integer>(); bag.add(9,3);
        assertEquals(3L,M3ApacheStreams.snapshotBag(bag).count());
    }

    @Test void allIterableCollectionFamiliesUseTheSameOperators() {
        var list = new java.util.ArrayList<>(List.of(4,4));
        var snap = M3ApacheStreams.snapshotAny(list);
        var live = M3ApacheStreams.liveAny(list);
        list.add(5);
        assertEquals(List.of(4,4),snap.collect(Collectors.toList()));
        assertEquals(List.of(4,4,5),live.collect(Collectors.toList()));
        assertEquals(List.of(8,8,10),live.map(v -> v*2).collect(Collectors.toList()));
    }
}
