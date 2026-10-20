/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.Objects;

/**
 * Prepared-fact subtree pruning for exact literal search over the canonical M3 String tuple DAG.
 *
 * <p>This is an accelerator only. Positive summary facts never establish a match. Unprepared
 * source/range facts disable the tree path. When both tuple children remain possible, the existing
 * KMP/adaptive-BMH authority searches the complete current range instead of recursively expanding
 * both branches.</p>
 */
final class M3StringTreeSearch {
    static final int NOT_APPLICABLE = Integer.MIN_VALUE;

    private static final int MIN_TREE_UNITS = 128;
    private static final int MAX_TREE_DEPTH = 64;

    private M3StringTreeSearch() {}

    static int indexOf(
            M3String source,
            M3String needle,
            M3StringSearchPrecompute.Plan plan,
            int fromIndex,
            int endIndex) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(needle, "needle");
        Objects.requireNonNull(plan, "plan");
        if (source.length() < MIN_TREE_UNITS
                || !(source.owner() instanceof M3StringTuple)
                || source.factsIfPrepared() == null) {
            return NOT_APPLICABLE;
        }
        M3StringFacts needleFacts = needle.facts();
        return forward(
                source,
                source,
                0,
                needle,
                needleFacts,
                plan,
                fromIndex,
                endIndex,
                0);
    }

    static int lastIndexOf(
            M3String source,
            M3String needle,
            M3StringSearchPrecompute.Plan plan,
            int maximumStart) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(needle, "needle");
        Objects.requireNonNull(plan, "plan");
        if (source.length() < MIN_TREE_UNITS
                || !(source.owner() instanceof M3StringTuple)
                || source.factsIfPrepared() == null) {
            return NOT_APPLICABLE;
        }
        M3StringFacts needleFacts = needle.facts();
        return reverse(source, source, 0, needle, needleFacts, plan, maximumStart, 0);
    }

    private static int forward(
            M3String root,
            M3String view,
            int base,
            M3String needle,
            M3StringFacts needleFacts,
            M3StringSearchPrecompute.Plan plan,
            int fromIndex,
            int endIndex,
            int depth) {
        int width = plan.patternLength;
        int candidateFrom = Math.max(fromIndex, base);
        int candidateMax = Math.min(endIndex - width, base + view.length() - width);
        if (candidateFrom > candidateMax) return -1;
        if (!mayContainPrepared(view, needleFacts)) return -1;
        if (depth >= MAX_TREE_DEPTH || view.length() < MIN_TREE_UNITS) {
            return exactForward(root, needle, plan, candidateFrom, candidateMax);
        }

        Split split = split(view);
        if (split == null) {
            return exactForward(root, needle, plan, candidateFrom, candidateMax);
        }
        if (split.right == null) {
            return forward(
                    root,
                    split.left,
                    base,
                    needle,
                    needleFacts,
                    plan,
                    fromIndex,
                    endIndex,
                    depth + 1);
        }

        int boundary = Math.addExact(base, split.left.length());
        boolean leftPossible =
                hasCandidate(split.left, base, fromIndex, endIndex, width)
                        && mayContainPrepared(split.left, needleFacts);
        boolean rightPossible =
                hasCandidate(split.right, boundary, fromIndex, endIndex, width)
                        && mayContainPrepared(split.right, needleFacts);

        // No proven tree rejection: retain the existing exact whole-range algorithm.
        if (leftPossible && rightPossible) {
            return exactForward(root, needle, plan, candidateFrom, candidateMax);
        }

        if (leftPossible) {
            int found =
                    forward(
                            root,
                            split.left,
                            base,
                            needle,
                            needleFacts,
                            plan,
                            fromIndex,
                            endIndex,
                            depth + 1);
            if (found >= 0) return found;
        }

        int seam =
                seamFirst(
                        root,
                        needle,
                        plan,
                        base,
                        base + view.length(),
                        boundary,
                        fromIndex,
                        endIndex);
        if (seam >= 0) return seam;

        return rightPossible
                ? forward(
                        root,
                        split.right,
                        boundary,
                        needle,
                        needleFacts,
                        plan,
                        fromIndex,
                        endIndex,
                        depth + 1)
                : -1;
    }

    private static int reverse(
            M3String root,
            M3String view,
            int base,
            M3String needle,
            M3StringFacts needleFacts,
            M3StringSearchPrecompute.Plan plan,
            int maximumStart,
            int depth) {
        int width = plan.patternLength;
        int candidateFrom = base;
        int candidateMax = Math.min(maximumStart, base + view.length() - width);
        if (candidateFrom > candidateMax) return -1;
        if (!mayContainPrepared(view, needleFacts)) return -1;
        if (depth >= MAX_TREE_DEPTH || view.length() < MIN_TREE_UNITS) {
            return exactReverse(root, needle, plan, candidateFrom, candidateMax);
        }

        Split split = split(view);
        if (split == null) {
            return exactReverse(root, needle, plan, candidateFrom, candidateMax);
        }
        if (split.right == null) {
            return reverse(
                    root,
                    split.left,
                    base,
                    needle,
                    needleFacts,
                    plan,
                    maximumStart,
                    depth + 1);
        }

        int boundary = Math.addExact(base, split.left.length());
        boolean leftPossible =
                hasReverseCandidate(split.left, base, maximumStart, width)
                        && mayContainPrepared(split.left, needleFacts);
        boolean rightPossible =
                hasReverseCandidate(split.right, boundary, maximumStart, width)
                        && mayContainPrepared(split.right, needleFacts);

        if (leftPossible && rightPossible) {
            return exactReverse(root, needle, plan, candidateFrom, candidateMax);
        }

        if (rightPossible) {
            int found =
                    reverse(
                            root,
                            split.right,
                            boundary,
                            needle,
                            needleFacts,
                            plan,
                            maximumStart,
                            depth + 1);
            if (found >= 0) return found;
        }

        int seam =
                seamLast(
                        root,
                        needle,
                        plan,
                        base,
                        base + view.length(),
                        boundary,
                        maximumStart);
        if (seam >= 0) return seam;

        return leftPossible
                ? reverse(
                        root,
                        split.left,
                        base,
                        needle,
                        needleFacts,
                        plan,
                        maximumStart,
                        depth + 1)
                : -1;
    }

    private static boolean hasCandidate(
            M3String view,
            int base,
            int fromIndex,
            int endIndex,
            int width) {
        int first = Math.max(fromIndex, base);
        int last = Math.min(endIndex - width, base + view.length() - width);
        return first <= last;
    }

    private static boolean hasReverseCandidate(
            M3String view, int base, int maximumStart, int width) {
        return base <= Math.min(maximumStart, base + view.length() - width);
    }

    private static boolean mayContainPrepared(M3String view, M3StringFacts needleFacts) {
        M3StringFacts prepared = view.factsIfPrepared();
        return prepared == null || prepared.mayContain(needleFacts);
    }

    private static int exactForward(
            M3String root,
            M3String needle,
            M3StringSearchPrecompute.Plan plan,
            int candidateFrom,
            int candidateMax) {
        return M3StringSearchPrecompute.indexOf(
                root,
                needle,
                plan,
                candidateFrom,
                Math.addExact(candidateMax, plan.patternLength));
    }

    private static int exactReverse(
            M3String root,
            M3String needle,
            M3StringSearchPrecompute.Plan plan,
            int candidateFrom,
            int candidateMax) {
        return lastKmp(root, needle, plan, candidateFrom, candidateMax);
    }

    private static int seamFirst(
            M3String root,
            M3String needle,
            M3StringSearchPrecompute.Plan plan,
            int viewStart,
            int viewEnd,
            int boundary,
            int fromIndex,
            int endIndex) {
        int width = plan.patternLength;
        int first =
                Math.max(
                        Math.max(viewStart, fromIndex),
                        Math.addExact(boundary, 1 - width));
        int last =
                Math.min(
                        Math.min(boundary - 1, endIndex - width),
                        viewEnd - width);
        if (first > last) return -1;
        int found =
                M3StringSearchPrecompute.indexOf(
                        root,
                        needle,
                        plan,
                        first,
                        Math.addExact(last, width));
        return found >= first && found <= last ? found : -1;
    }

    private static int seamLast(
            M3String root,
            M3String needle,
            M3StringSearchPrecompute.Plan plan,
            int viewStart,
            int viewEnd,
            int boundary,
            int maximumStart) {
        int width = plan.patternLength;
        int first = Math.max(viewStart, Math.addExact(boundary, 1 - width));
        int last = Math.min(Math.min(boundary - 1, maximumStart), viewEnd - width);
        return first <= last ? lastKmp(root, needle, plan, first, last) : -1;
    }

    /**
     * Bounded overlap-preserving reverse answer using the already prepared forward KMP prefix lane.
     */
    private static int lastKmp(
            M3String source,
            M3String pattern,
            M3StringSearchPrecompute.Plan plan,
            int candidateFrom,
            int candidateMax) {
        if (candidateFrom > candidateMax) return -1;
        M3String.CodeUnitCursor sourceUnits = new M3String.CodeUnitCursor(source);
        M3String.CodeUnitCursor patternUnits = new M3String.CodeUnitCursor(pattern);
        int matched = 0;
        int last = -1;
        int end = Math.addExact(candidateMax, plan.patternLength);
        for (int index = candidateFrom; index < end; index++) {
            char unit = sourceUnits.at(index);
            while (matched > 0 && unit != patternUnits.at(matched)) {
                matched = plan.prefix[matched - 1];
            }
            if (unit == patternUnits.at(matched)) matched++;
            if (matched == plan.patternLength) {
                int start = index - plan.patternLength + 1;
                if (start >= candidateFrom && start <= candidateMax) last = start;
                matched = plan.prefix[matched - 1];
            }
        }
        return last;
    }

    /**
     * Project an exact logical range of one tuple owner onto one or both child coordinates.
     *
     * <p>A one-child split is still useful: a slice of a tuple may live wholly inside one child,
     * so descent can continue without ever materializing or flattening that range.</p>
     */
    private static Split split(M3String view) {
        if (!(view.owner() instanceof M3StringTuple tuple)) return null;
        int start = view.start();
        int end = view.end();
        int boundary = tuple.left.length();
        if (end <= boundary) {
            return new Split(tuple.left.slice(start, end), null);
        }
        if (start >= boundary) {
            return new Split(tuple.right.slice(start - boundary, end - boundary), null);
        }
        return new Split(
                tuple.left.slice(start, boundary),
                tuple.right.slice(0, end - boundary));
    }

    private record Split(M3String left, M3String right) {
        Split {
            Objects.requireNonNull(left, "left");
        }
    }
}
