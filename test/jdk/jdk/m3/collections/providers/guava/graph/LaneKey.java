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
package jdk.m3.collections.guava.graph;

/**
 * Test node equal by {@code id} only, hashed to {@code id % 3} so hash buckets collide, with a
 * {@code tag} that tells equal instances apart so the instance a connection keeps is visible.
 */
final class LaneKey {

    private static final int BUCKETS = 3;

    final int id;
    final int tag;

    LaneKey(int id, int tag) {
        this.id = id;
        this.tag = tag;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LaneKey key && key.id == id;
    }

    @Override
    public int hashCode() {
        return id % BUCKETS;
    }

    @Override
    public String toString() {
        return "k" + id + "#" + tag;
    }
}
