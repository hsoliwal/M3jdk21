// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.HashMap;
import java.util.Map;

//FLLDCIM: RELOCATE - donor blob efb4d9e2cdbc at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/graph-trie-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/graph-trie-20261106/relocate.py v1
public final class StringTrie {
    private final Node root = new Node();
    private int words;

    //FLLDCIM: RELOCATE - explicit constructor inserted by the INSERT_AFTER row of m3/algorithms/recipe/graph-trie-20261106/RELOCATION_MAP.tsv: the donor relies on the implicit public constructor, which -Xlint:all (missing-explicit-ctor) refuses for a public class of the exported package jdk.m3.algorithm; same contract
    public StringTrie() {
    }

    public boolean insert(String value, ProgressMonitor monitor) {
        int[] codePoints = value.codePoints().toArray();
        AlgorithmSupport.begin(monitor, "trie-insert", codePoints.length);
        try {
            Node node = root;
            Node pendingParent = null;
            Node pendingFirst = null;
            int pendingCodePoint = 0;
            for (int codePoint : codePoints) {
                AlgorithmSupport.tick(monitor);
                Node next = node.children.get(codePoint);
                if (next == null) {
                    next = new Node();
                    if (pendingFirst == null) {
                        pendingParent = node;
                        pendingFirst = next;
                        pendingCodePoint = codePoint;
                    } else {
                        node.children.put(codePoint, next);
                    }
                }
                node = next;
            }
            if (node.terminal) {
                return false;
            }
            node.terminal = true;
            if (pendingFirst != null) {
                pendingParent.children.put(pendingCodePoint, pendingFirst);
            }
            words++;
            return true;
        } finally {
            monitor.done();
        }
    }

    public boolean contains(String value, ProgressMonitor monitor) {
        Node node = find(value, monitor);
        return node != null && node.terminal;
    }

    public boolean startsWith(String prefix, ProgressMonitor monitor) {
        return find(prefix, monitor) != null;
    }

    public boolean remove(String value, ProgressMonitor monitor) {
        int[] codePoints = value.codePoints().toArray();
        AlgorithmSupport.begin(monitor, "trie-remove", codePoints.length * 2L);
        try {
            Node[] path = new Node[codePoints.length + 1];
            path[0] = root;
            Node node = root;
            for (int index = 0; index < codePoints.length; index++) {
                AlgorithmSupport.tick(monitor);
                node = node.children.get(codePoints[index]);
                if (node == null) {
                    return false;
                }
                path[index + 1] = node;
            }
            if (!node.terminal) {
                return false;
            }
            node.terminal = false;
            words--;
            for (int index = codePoints.length - 1; index >= 0; index--) {
                AlgorithmSupport.tick(monitor);
                Node child = path[index + 1];
                if (child.terminal || !child.children.isEmpty()) {
                    break;
                }
                path[index].children.remove(codePoints[index]);
            }
            return true;
        } finally {
            monitor.done();
        }
    }

    public int wordCount(ProgressMonitor monitor) {
        monitor.checkCanceled();
        return words;
    }

    private Node find(String value, ProgressMonitor monitor) {
        int[] codePoints = value.codePoints().toArray();
        AlgorithmSupport.begin(monitor, "trie-find", codePoints.length);
        try {
            Node node = root;
            for (int codePoint : codePoints) {
                AlgorithmSupport.tick(monitor);
                node = node.children.get(codePoint);
                if (node == null) {
                    return null;
                }
            }
            return node;
        } finally {
            monitor.done();
        }
    }

    private static final class Node {
        private final Map<Integer, Node> children = new HashMap<>();
        private boolean terminal;
    }
}
