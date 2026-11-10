/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.PriorityQueue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Explicit choices for generic array-backed JDK families. No semantic auto-replacement.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/collections/flat/FlatCollections.java}
 * blob {@code 11d98a6acdc938b51988553c357c28416695dbd6} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class FlatCollections {
    private FlatCollections() { }
    public static <E> ArrayList<E> objectList() { return new ArrayList<>(); }
    public static <E> ArrayDeque<E> objectDeque() { return new ArrayDeque<>(); }
    public static <E> PriorityQueue<E> objectPriorityQueue() { return new PriorityQueue<>(); }
    public static <K, V> IdentityHashMap<K, V> identityMap() { return new IdentityHashMap<>(); }
    public static <E extends Enum<E>> EnumSet<E> enumSet(Class<E> type) { return EnumSet.noneOf(type); }
    public static <E extends Enum<E>, V> EnumMap<E, V> enumMap(Class<E> type) { return new EnumMap<>(type); }
    public static BitSet bitSet() { return new BitSet(); }
    public static <E> ArrayBlockingQueue<E> blockingQueue(int capacity, boolean fair) {
        return new ArrayBlockingQueue<>(capacity, fair);
    }
}
