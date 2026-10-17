// SPDX-License-Identifier: Apache-2.0
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
            } catch (NoSuchMethodException impossible) {
                throw new AssertionError(impossible);
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

    /** AVL-style persistent concatenation; in-order traversal is predicate evaluation order. */
    private static Node join(Node left, Node right) {
        int delta = left.height() - right.height();
        if (delta > 1 && left instanceof Branch branch) {
            Node a = branch.left();
            Node b = branch.right();
            if (a.height() >= b.height()) {
                return new Branch(a, new Branch(b, right));
            }
            if (b instanceof Branch middle) {
                return new Branch(new Branch(a, middle.left()),
                        new Branch(middle.right(), right));
            }
        } else if (delta < -1 && right instanceof Branch branch) {
            Node b = branch.left();
            Node c = branch.right();
            if (c.height() >= b.height()) {
                return new Branch(new Branch(left, b), c);
            }
            if (b instanceof Branch middle) {
                return new Branch(new Branch(left, middle.left()),
                        new Branch(middle.right(), c));
            }
        }
        return new Branch(left, right);
    }
}
