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
package jdk.m3.collections.apache.map;

/**
 * Lockstep key whose {@code equals} counts its calls and, while armed, changes the target map:
 * it removes the first key or puts a fresh one, alternately, until the budget is spent. Its hash
 * code is its id, so it shares buckets with the {@code Integer} keys of the same value.
 */
final class ReentrantKey {

    /** First fresh key put by a reentrant call. */
    private static final int FRESH = 100;

    /** The map an armed equals changes. */
    private static LinkedMap<Object, Object> target;

    /** The changes left to make. */
    private static int budget;

    /** The equals calls since the last arm. */
    private static int calls;

    /** The identity and hash code of this key. */
    private final int id;

    ReentrantKey(final int id) {
        this.id = id;
    }

    /**
     * Arms the keys for one call on one side.
     *
     * @param map The map an armed equals changes
     * @param changes The number of changes to make
     */
    static void arm(final LinkedMap<Object, Object> map, final int changes) {
        target = map;
        budget = changes;
        calls = 0;
    }

    /**
     * Disarms the keys.
     *
     * @return The equals calls since the last arm
     */
    static int disarm() {
        target = null;
        budget = 0;
        return calls;
    }

    @Override
    public boolean equals(final Object other) {
        calls++;
        if (budget > 0) {
            budget--;
            change();
        }
        return this == other;
    }

    private static void change() {
        if (budget % 2 == 0 && !target.isEmpty()) {
            target.remove(target.firstKey());
        } else {
            target.put(FRESH + target.size(), "fresh");
        }
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "K" + id;
    }
}
