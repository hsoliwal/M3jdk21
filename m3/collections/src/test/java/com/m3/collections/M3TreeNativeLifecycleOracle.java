// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Independent headless native-tree oracle: actual physical parent deletion destroys descendants. */
public final class M3TreeNativeLifecycleOracle {
    private static long checks;
    private static long digest = 0xcbf29ce484222325L;
    private static final int ROOT = -1;

    private M3TreeNativeLifecycleOracle() { }
    private static void yes(boolean okay, String why) {
        checks++;
        if (!okay) throw new AssertionError(why + " check=" + checks);
    }
    private static void record(long value) {
        digest = (digest ^ value) * 0x100000001b3L;
    }

    private record Row(int parent, int child, int physicalParent) { }
    private static final class Window {
        private final ArrayList<Row> entries = new ArrayList<>();
        private int add(int parent, int child, int physicalParent) {
            entries.add(new Row(parent, child, physicalParent));
            return entries.size() - 1;
        }
        private int size() { return entries.size(); }
        private int[] parentIds() { return entries.stream().mapToInt(Row::parent).toArray(); }
        private int[] childIndices() { return entries.stream().mapToInt(Row::child).toArray(); }
        private int[] physicalParents() { return entries.stream().mapToInt(Row::physicalParent).toArray(); }
    }
    private static final class NativeNode {
        private final long handle;
        private final long key;
        private final long parent;
        private final ArrayList<Long> children = new ArrayList<>();
        private NativeNode(long handle, long key, long parent) {
            this.handle = handle; this.key = key; this.parent = parent;
        }
    }
    private static final class NativeTree {
        private final Map<Long, NativeNode> nodeByHandle = new HashMap<>();
        private final ArrayList<Long> roots = new ArrayList<>();
        private long nextHandle = 1;

        private List<Long> siblings(long parent) {
            return parent == 0 ? roots : nodeByHandle.get(parent).children;
        }
        private void destroy(long handle) {
            NativeNode node = nodeByHandle.get(handle);
            yes(node != null, "topmost native retirement must exist");
            for (long child : List.copyOf(node.children)) destroy(child);
            yes(siblings(node.parent).remove(handle), "native sibling reference must exist");
            yes(nodeByHandle.remove(handle) == node, "destroy exact native row");
            record(~handle);
        }
        private long insert(long parent, long after, long key) {
            yes(parent == 0 || nodeByHandle.containsKey(parent), "insert parent is resident");
            List<Long> children = siblings(parent);
            int offset = after == 0 ? 0 : children.indexOf(after) + 1;
            yes(after == 0 || offset != 0, "previous physical sibling must exist");
            long handle = nextHandle++;
            yes(handle != 0 && !nodeByHandle.containsKey(handle), "fresh handle unique");
            children.add(offset, handle);
            nodeByHandle.put(handle, new NativeNode(handle, key, parent));
            record(handle);
            return handle;
        }
        private void traverse(long handle, List<Long> sequence, Set<Long> seen) {
            NativeNode node = nodeByHandle.get(handle);
            yes(node != null && node.handle == handle, "native row has stable identity");
            yes(seen.add(handle), "physical structure must be acyclic");
            sequence.add(handle);
            for (long child : node.children) {
                yes(nodeByHandle.get(child).parent == handle, "native child parent pointer");
                traverse(child, sequence, seen);
            }
        }
        private List<Long> preorder() {
            List<Long> result = new ArrayList<>();
            Set<Long> seen = new HashSet<>();
            for (long root : roots) {
                yes(nodeByHandle.get(root).parent == 0, "native root parent pointer");
                traverse(root, result, seen);
            }
            yes(seen.size() == nodeByHandle.size(), "all native rows must be reachable");
            return result;
        }
        private void apply(M3TreeWindowPlan plan, Window target, long[] previousHandles) {
            int n = target.size();
            int[] parents = target.parentIds();
            int[] children = target.childIndices();
            int[] phy = target.physicalParents();
            plan.plan(n, parents, children, phy);
            for (int i = 0; i < plan.retiredCount(); i++) {
                long retired = plan.retired(i);
                yes(nodeByHandle.containsKey(retired), "retired handle must be native");
                destroy(retired);
            }
            long[] arrived = new long[n];
            for (int i = 0; i < n; i++) {
                int pp = phy[i];
                long parent = pp == -1 ? 0 : arrived[pp];
                int sib = plan.previousSibling(i);
                long preceding = sib == -1 ? 0 : arrived[sib];
                if (sib != -1) yes(phy[sib] == pp, "previous sibling parent coordinate");
                long key = M3TreeWindowPlan.key(parents[i], children[i]);
                long retained = plan.plannedHandle(i);
                if (retained == 0) {
                    arrived[i] = insert(parent, preceding, key);
                } else {
                    NativeNode nativeNode = nodeByHandle.get(retained);
                    yes(nativeNode != null, "planned retained handle must survive parent retirement");
                    yes(nativeNode.parent == parent, "planned retained native parent matches");
                    yes(nativeNode.key == key, "retained native logical coordinate matches");
                    arrived[i] = retained;
                }
                record(arrived[i]);
            }
            plan.commit(n, parents, children, phy, arrived);
            List<Long> observed = preorder();
            yes(observed.size() == n, "bounded native physical residency matches logical window count");
            for (int i=0; i<n; i++) {
                yes(observed.get(i) == arrived[i], "native preorder equals planned order");
                yes(plan.handleOf(parents[i], children[i]) == arrived[i], "coordinate-to-handle map bijection");
                yes(plan.entryOf(arrived[i]) == i, "native handle-to-entry map bijection");
                yes(plan.handle(i) == arrived[i], "committed handle lane");
                yes(plan.keyOf(i) == M3TreeWindowPlan.key(parents[i], children[i]), "committed key lane");
            }
            for (long old : previousHandles) if (old != 0 && !nodeByHandle.containsKey(old)) {
                yes(plan.entryOf(old) == -1, "retired native handles excluded from handle index");
            }
        }
    }
    private static void children(Window window, int id, int physical, int level, int width,
                                 Random rng, int frame) {
        if (level >= 4) return;
        for (int k=0; k<width; k++) {
            // A deterministic blend of sparse/cold windows and deep visible branches.
            if (rng.nextInt(100) < (level == 0 ? 18 : 45)) continue;
            int child = k;
            int internal = id * 7 + child + 1;
            int entry = window.add(id, child, physical);
            children(window, internal, entry, level + 1, width == 3 ? 2 : 3, rng, frame);
        }
    }
    private static Window window(Random random, int frame) {
        Window w = new Window();
        for (int i=0; i<8; i++) {
            if (random.nextInt(100) < 20) continue;
            // Root index variations destroy exactly one native ancestor while descendants keep keys.
            int logicalRootIndex = i * 8 + (((frame + i * 17) % 9 == 0) ? 1 : 0);
            int entry = w.add(ROOT, logicalRootIndex, -1);
            children(w, 1000+i, entry, 0, 3, random, frame);
        }
        return w;
    }
    public static void main(String[] args) {
        M3TreeWindowPlan plan = new M3TreeWindowPlan();
        NativeTree nativeTree = new NativeTree();
        Random random = new Random(0x4d334e4154495645L);
        long[] last = new long[0];
        for (int frame=0; frame<6_000; frame++) {
            Window w = window(random, frame);
            nativeTree.apply(plan, w, last);
            last = new long[plan.size()];
            for (int i=0;i<last.length;i++) last[i]=plan.handle(i);
            record(frame);
        }
        System.out.println("M3_TREE_NATIVE_LIFECYCLE_PASS frames=6000 checks="+checks+
                " digest="+Long.toUnsignedString(digest,16));
    }
}
