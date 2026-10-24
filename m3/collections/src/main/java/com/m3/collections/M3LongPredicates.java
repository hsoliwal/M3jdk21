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
package com.m3.collections;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Internal persistent conjunction for nested primitive filters.
 *
 * <p>Default {@link LongPredicate#and(LongPredicate)} compositions are represented by a
 * height-balanced immutable tree, preserving left-to-right short-circuit evaluation while
 * bounding Java call depth to O(log n). A predicate class that overrides {@code and} is
 * composed through that virtual method so custom composition semantics remain authoritative.
 */
final class M3LongPredicates {
    private static final ClassValue<Boolean> DEFAULT_AND = new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type) {
            try {
                Method method = type.getMethod("and", LongPredicate.class);
                return method.getDeclaringClass() == LongPredicate.class;
            } catch (NoSuchMethodException | SecurityException denied) {
                // The original virtual call remains the safe, contract-preserving fallback.
                return false;
            }
        }
    };

    private M3LongPredicates() { }

    static LongPredicate and(LongPredicate left, LongPredicate right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (!DEFAULT_AND.get(left.getClass())) {
            return left.and(right);
        }
        Node lhs = left instanceof Chain chain ? chain.root : new Leaf(left);
        return new Chain(join(lhs, new Leaf(right)));
    }

    private sealed interface Node permits Leaf, Branch {
        int height();
        boolean test(long value);
    }

    private record Leaf(LongPredicate predicate) implements Node {
        @Override public int height() { return 1; }
        @Override public boolean test(long value) { return predicate.test(value); }
    }

    private record Branch(Node left, Node right, int height) implements Node {
        Branch(Node left, Node right) {
            this(left, right, Math.max(left.height(), right.height()) + 1);
        }
        @Override public boolean test(long value) {
            return left.test(value) && right.test(value);
        }
    }

    private static final class Chain implements LongPredicate {
        private final Node root;
        private Chain(Node root) { this.root = root; }
        @Override public boolean test(long value) { return root.test(value); }
    }

    /**
     * AVL join descends the taller tree to the insertion frontier before rotating.
     * A single root rotation without this descent does not preserve logarithmic
     * depth after repeated nested filtered-view compositions.
     * Retains existing immutable leaves and their exact encounter order.
     */
    private static Node join(Node left, Node right) {
        if (left.height() > right.height() + 1) {
            Branch branch = (Branch) left;
            return rebalance(branch.left(), join(branch.right(), right));
        }
        if (right.height() > left.height() + 1) {
            Branch branch = (Branch) right;
            return rebalance(join(left, branch.left()), branch.right());
        }
        return new Branch(left, right);
    }

    /** No leaf or predicate result is materialized during rotations. */
    private static Node rebalance(Node left, Node right) {
        if (left.height() > right.height() + 1) {
            Branch branch = (Branch) left;
            Node a = branch.left();
            Node b = branch.right();
            if (a.height() >= b.height()) {
                return new Branch(a, new Branch(b, right));
            }
            Branch middle = (Branch) b;
            return new Branch(new Branch(a, middle.left()),
                    new Branch(middle.right(), right));
        }
        if (right.height() > left.height() + 1) {
            Branch branch = (Branch) right;
            Node b = branch.left();
            Node c = branch.right();
            if (c.height() >= b.height()) {
                return new Branch(new Branch(left, b), c);
            }
            Branch middle = (Branch) b;
            return new Branch(new Branch(left, middle.left()),
                    new Branch(middle.right(), c));
        }
        return new Branch(left, right);
    }
}
