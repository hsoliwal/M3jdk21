// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.Comparator;
import java.util.Objects;

//FLLDCIM: RELOCATE - donor blob 91798c577e28 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
public final class ListAlgorithms {
    private ListAlgorithms() {
    }

    public static final class Node<T> {
        private final T value;
        private Node<T> next;

        public Node(T value) {
            this.value = value;
        }

        public T value() {
            return value;
        }

        public Node<T> next() {
            return next;
        }

        public Node<T> next(Node<T> value) {
            next = value;
            return this;
        }
    }

    public static <T> Node<T> reverse(Node<T> head, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "reverse-list", ProgressMonitor.UNKNOWN);
        try {
            Node<T> previous = null;
            Node<T> current = head;
            while (current != null) {
                AlgorithmSupport.tick(monitor);
                Node<T> next = current.next;
                current.next = previous;
                previous = current;
                current = next;
            }
            return previous;
        } finally {
            monitor.done();
        }
    }

    public static <T> Node<T> middle(Node<T> head, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "list-middle", ProgressMonitor.UNKNOWN);
        try {
            Node<T> slow = head;
            Node<T> fast = head;
            while (fast != null && fast.next != null) {
                AlgorithmSupport.tick(monitor);
                slow = slow.next;
                fast = fast.next.next;
            }
            return slow;
        } finally {
            monitor.done();
        }
    }

    public static <T> Node<T> cycleStart(Node<T> head, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "floyd-cycle-start", ProgressMonitor.UNKNOWN);
        try {
            Node<T> slow = head;
            Node<T> fast = head;
            do {
                if (fast == null || fast.next == null) {
                    return null;
                }
                AlgorithmSupport.tick(monitor);
                slow = slow.next;
                fast = fast.next.next;
            } while (slow != fast);

            slow = head;
            while (slow != fast) {
                AlgorithmSupport.tick(monitor);
                slow = slow.next;
                fast = fast.next;
            }
            return slow;
        } finally {
            monitor.done();
        }
    }

    public static <T> Node<T> mergeSorted(
            Node<T> left,
            Node<T> right,
            Comparator<? super T> comparator,
            ProgressMonitor monitor) {
        Objects.requireNonNull(comparator, "comparator");
        AlgorithmSupport.begin(monitor, "merge-sorted-lists", ProgressMonitor.UNKNOWN);
        try {
            Node<T> dummy = new Node<>(null);
            Node<T> tail = dummy;
            while (left != null && right != null) {
                AlgorithmSupport.tick(monitor);
                if (comparator.compare(left.value, right.value) <= 0) {
                    tail.next = left;
                    left = left.next;
                } else {
                    tail.next = right;
                    right = right.next;
                }
                tail = tail.next;
            }
            tail.next = left != null ? left : right;
            return dummy.next;
        } finally {
            monitor.done();
        }
    }

    public static <T> Node<T> removeNthFromEnd(
            Node<T> head,
            int n,
            ProgressMonitor monitor) {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be positive");
        }
        AlgorithmSupport.begin(monitor, "remove-nth-from-end", ProgressMonitor.UNKNOWN);
        try {
            Node<T> dummy = new Node<>(null);
            dummy.next = head;
            Node<T> fast = dummy;
            for (int step = 0; step < n; step++) {
                AlgorithmSupport.tick(monitor);
                fast = fast.next;
                if (fast == null) {
                    throw new IllegalArgumentException("n exceeds list length");
                }
            }
            Node<T> slow = dummy;
            while (fast.next != null) {
                AlgorithmSupport.tick(monitor);
                fast = fast.next;
                slow = slow.next;
            }
            slow.next = slow.next == null ? null : slow.next.next;
            return dummy.next;
        } finally {
            monitor.done();
        }
    }
}
