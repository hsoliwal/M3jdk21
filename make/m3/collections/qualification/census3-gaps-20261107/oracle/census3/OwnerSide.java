// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package census3;

import jdk.m3.collections.M3MonotoneLongDeque;
import jdk.m3.collections.M3PersistentLongSumTree;

/** Owner side: jdk.m3.collections M3MonotoneLongDeque and M3PersistentLongSumTree with the restored members. */
public final class OwnerSide implements Side {
    private M3MonotoneLongDeque deque;
    private M3PersistentLongSumTree tree;

    public void newDeque(int capacity, boolean max) {
        deque = new M3MonotoneLongDeque(capacity,
                max ? M3MonotoneLongDeque.Mode.MAX : M3MonotoneLongDeque.Mode.MIN);
    }
    public void offer(int index, long value) { deque.offer(index, value); }
    public void evictBefore(int minimumIndexInclusive) { deque.evictBefore(minimumIndexInclusive); }
    public long lastIndex() { return deque.lastIndex(); }
    public long lastValue() { return deque.lastValue(); }

    public void newTree(int length, int expected) { tree = new M3PersistentLongSumTree(length, expected); }
    public void newTreeDefault(int length) { tree = new M3PersistentLongSumTree(length); }
    public int forkAdd(int baseVersion, int index, long delta) { return tree.forkAdd(baseVersion, index, delta); }
    public int rootNode(int version) { return tree.rootNode(version); }
    public int length() { return tree.length(); }
    public int versionCount() { return tree.versionCount(); }
    public int createdNodes() { return tree.nodeCount() - 1; }
    public long rangeSum(int version, int from, int to) { return tree.rangeSum(version, from, to); }
    public long total(int version) { return tree.total(version); }
}
