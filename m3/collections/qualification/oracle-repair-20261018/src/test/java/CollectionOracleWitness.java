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
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

/** Independent JDK and arithmetic witnesses for the four existing test failures. */
public final class CollectionOracleWitness {
    private CollectionOracleWitness() { }

    private record Key(String id, boolean keep) {
        @Override public boolean equals(Object value) {
            return value instanceof Key key && id.equals(key.id);
        }
        @Override public int hashCode() { return id.hashCode(); }
    }

    public static void main(String[] args) {
        TreeSet<Long> signed = new TreeSet<>();
        for (long value = 1; value <= 3; value++) {
            signed.add(value * 7);
            signed.add(-value * 7);
        }
        signed.removeIf(value -> value % 21 != 0);
        if (!signed.equals(new TreeSet<>(List.of(-21L, 21L))) || signed.contains(0L)) {
            throw new AssertionError("signed progression oracle");
        }
        System.out.println("SIGNED_SURVIVORS=" + signed + " ZERO_GAP=" + (signed.last() - signed.first()));

        TreeSet<Long> owner = new TreeSet<>();
        for (long value = -200; value <= 200; value += 3) owner.add(value);
        NavigableSet<Long> bounded = owner.subSet(-50L, true, 50L, false);
        boolean duplicate = bounded.add(7L);
        boolean added = bounded.add(8L);
        if (duplicate || !added) throw new AssertionError("bounded membership oracle");
        System.out.println("JDK_BOUNDED_ADD_7=" + duplicate + " JDK_BOUNDED_ADD_8=" + added);

        Key hidden = new Key("same", false);
        Key visible = new Key("same", true);
        ArrayList<Key> equalMembers = new ArrayList<>(List.of(hidden, visible));
        if (!equalMembers.remove(visible) || equalMembers.size() != 1 || equalMembers.getFirst() != visible) {
            throw new AssertionError("ArrayList first-equals removal oracle");
        }
        System.out.println("REMOVE_OBJECT_LEAVES_VISIBLE_REFERENCE=" + (equalMembers.getFirst() == visible));
        equalMembers = new ArrayList<>(List.of(hidden, visible));
        var iterator = equalMembers.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() == visible) { iterator.remove(); break; }
        }
        if (equalMembers.size() != 1 || equalMembers.getFirst() != hidden) {
            throw new AssertionError("identity-selected iterator removal oracle");
        }
        System.out.println("IDENTITY_ITERATOR_REMOVAL_LEAVES_HIDDEN_REFERENCE=" + (equalMembers.getFirst() == hidden));

        // The documented half-split rule, independent of the M3 production source.
        int capacity = 4;
        int members = 10;
        List<Integer> occupancy = new ArrayList<>();
        occupancy.add(0);
        for (int inserted = 0; inserted < members; inserted++) {
            int last = occupancy.size() - 1;
            int count = occupancy.get(last);
            if (count == capacity) {
                occupancy.set(last, count / 2);
                occupancy.add(count - count / 2);
                last++;
            }
            occupancy.set(last, occupancy.get(last) + 1);
        }
        int minimum = (members + capacity - 1) / capacity;
        int maximum = (members + capacity / 2 - 1) / (capacity / 2);
        if (!occupancy.equals(List.of(2, 2, 2, 4)) || occupancy.size() < minimum || occupancy.size() > maximum) {
            throw new AssertionError("documented page occupancy bounds");
        }
        System.out.println("HALF_SPLIT_OCCUPANCY=" + occupancy + " PAGE_BOUNDS=" + minimum + ".." + maximum);
        System.out.println("COLLECTION_ORACLE_WITNESS_PASS");
    }
}
