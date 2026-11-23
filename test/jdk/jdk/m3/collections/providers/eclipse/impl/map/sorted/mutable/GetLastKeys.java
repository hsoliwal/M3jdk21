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
package jdk.m3.collections.eclipse.impl.map.sorted.mutable;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Random;
import java.util.stream.IntStream;

/** Counting keys, values and a trapping comparator for the getLast differential proof. */
final class GetLastKeys {

    static final int DOMAIN = 64;
    static final int MODE_NORMAL = 0;
    static final int MODE_HASH = 1;
    static final int MODE_EQUALS = 2;
    static final int MODE_HOOK = 3;

    /** Calls of Key.compareTo, Key.equals and Key.hashCode since the start of the run. */
    static long callbacks;
    /** Runs on every callback of a key in MODE_HOOK. */
    static Runnable hook;

    private static final Key[] POOL =
            IntStream.range(0, DOMAIN).mapToObj(Key::new).toArray(Key[]::new);

    private GetLastKeys() {
    }

    /** Canonical key, a fresh equal copy, or null when nulls are allowed. */
    static Key key(Random random, boolean nulls) {
        if (nulls && random.nextInt(20) == 0) {
            return null;
        }
        int id = random.nextInt(DOMAIN);
        return random.nextInt(4) == 0 ? new Key(id) : POOL[id];
    }

    /** A fresh value object, or null one time in ten. */
    static Val val(Random random) {
        return random.nextInt(10) == 0 ? null : new Val(random.nextInt(DOMAIN / 2));
    }

    /** Comparable key whose id orders and identifies it; hash may collide on purpose. */
    static final class Key implements Comparable<Key>, Serializable {
        private static final long serialVersionUID = 1L;
        final int id;
        final int hash;
        int mode;

        Key(int id) {
            this(id, id);
        }

        Key(int id, int hash) {
            this.id = id;
            this.hash = hash;
        }

        private void callback(int trap) {
            callbacks++;
            if (this.mode == trap) {
                throw new IllegalStateException("trap " + this.id);
            }
            Runnable now = hook;
            if (this.mode == MODE_HOOK && now != null) {
                hook = null;
                try {
                    now.run();
                } finally {
                    hook = now;
                }
            }
        }

        @Override
        public int compareTo(Key other) {
            this.callback(-1);
            return Integer.compare(this.id, other.id);
        }

        @Override
        public boolean equals(Object other) {
            this.callback(MODE_EQUALS);
            return other instanceof Key key && key.id == this.id;
        }

        @Override
        public int hashCode() {
            this.callback(MODE_HASH);
            return this.hash;
        }

        @Override
        public String toString() {
            return "K" + this.id;
        }
    }

    /** Value whose equality is by id, so equal values may be distinct objects. */
    record Val(int id) implements Serializable {
    }

    /** Natural order that throws on its n-th compare and may run a reentrant hook. */
    static final class TrapOrder implements Comparator<Key>, Serializable {
        private static final long serialVersionUID = 1L;
        int countdown;
        transient Runnable onCompare;

        @Override
        public int compare(Key first, Key second) {
            if (--this.countdown == 0) {
                throw new IllegalStateException("compare trap");
            }
            Runnable now = this.onCompare;
            if (now != null) {
                this.onCompare = null;
                try {
                    now.run();
                } finally {
                    this.onCompare = now;
                }
            }
            return first.compareTo(second);
        }
    }
}
