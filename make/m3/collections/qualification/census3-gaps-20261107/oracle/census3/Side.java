// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package census3;

/** One implementation side of the census3 m3/collections differential (donor or restored owner). */
public interface Side {
    void newDeque(int capacity, boolean max);
    void offer(int index, long value);
    void evictBefore(int minimumIndexInclusive);
    long lastIndex();
    long lastValue();

    void newTree(int length, int expectedAdditionalVersions);
    void newTreeDefault(int length);
    int forkAdd(int baseVersion, int index, long delta);
    int rootNode(int version);
    int length();
    int versionCount();
    /** Created nodes, excluding the all-zero sentinel (the owner's nodeCount() counts it, the donor's does not). */
    int createdNodes();
    long rangeSum(int version, int from, int to);
    long total(int version);
}
