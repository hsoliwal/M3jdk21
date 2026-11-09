/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * RELATIONAL cases of the jdk.m3.graphics atom tests (gen_atom_tests.py v3): a binary geometry or comparison member
 * (Rect.intersect(Rect), IRect.contains(IPoint), Point.offset(Point), equals) is called with an argument built from the
 * receiver by a relation, which seeded rounds practically never produce. Receiver and argument are built by
 * field-mapping constructors (the generator proved that each parameter is stored into its own field) whose parameters
 * play the roles L, T, R, B of a rectangle or X, Y of a point. A spec is such a constructor's parameter descriptor
 * followed by the roles in parameter order ({@code (IIII)LTRB}); an argument spec starts with the parameter position
 * ({@code 0(II)XY}). Relations: two rectangles are identical, share the right, left, bottom or top edge, touch at the
 * bottom-right or top-left corner, one contains the other (both ways), overlap, or are disjoint ({@link RectRect});
 * a point lies inside a rectangle, on each edge, on each corner, or one unit outside each edge ({@link RectPoint});
 * two points are identical, negated, zero, share x or y, or differ ({@link PointPoint}). Every relation runs on the
 * base receivers (the first is (0, 0, 10, 10)) plus, for float constructors, one with fractional coordinates. Case
 * {@code i} is relation {@code i / bases}, base {@code i % bases}; the other parameters get the member's ordinary
 * values of round {@code i}. The generator computes the same case count (gen_atom_tests.py RELATIONS, BASES).
 */
final class M3GraphicsAtomsRelations {
    /** A relation: the argument's coordinates (L, T, R, B or X, Y) from the receiver's. */
    interface Relation {
        double[] of(double[] receiver);
    }

    /** The argument rectangle against the receiver rectangle. */
    enum RectRect implements Relation {
        IDENTICAL(b -> b.clone()),
        SHARED_EDGE_RIGHT(b -> new double[] {b[R], b[T], b[R] + w(b), b[B]}),
        SHARED_EDGE_LEFT(b -> new double[] {b[L] - w(b), b[T], b[L], b[B]}),
        SHARED_EDGE_BOTTOM(b -> new double[] {b[L], b[B], b[R], b[B] + h(b)}),
        SHARED_EDGE_TOP(b -> new double[] {b[L], b[T] - h(b), b[R], b[T]}),
        CORNER_BOTTOM_RIGHT(b -> new double[] {b[R], b[B], b[R] + w(b), b[B] + h(b)}),
        CORNER_TOP_LEFT(b -> new double[] {b[L] - w(b), b[T] - h(b), b[L], b[T]}),
        CONTAINED(b -> new double[] {b[L] + w(b) / QUARTER, b[T] + h(b) / QUARTER, b[R] - w(b) / QUARTER,
            b[B] - h(b) / QUARTER}),
        CONTAINING(b -> new double[] {b[L] - UNIT, b[T] - UNIT, b[R] + UNIT, b[B] + UNIT}),
        OVERLAPPING(b -> new double[] {b[L] + w(b) / HALF, b[T] + h(b) / HALF, b[R] + w(b) / HALF,
            b[B] + h(b) / HALF}),
        DISJOINT(b -> new double[] {b[R] + w(b), b[B] + h(b), b[R] + TWICE * w(b), b[B] + TWICE * h(b)});

        private final UnaryOperator<double[]> rule;

        RectRect(UnaryOperator<double[]> rule) {
            this.rule = rule;
        }

        @Override
        public double[] of(double[] receiver) {
            return rule.apply(receiver);
        }
    }

    /** The argument point against the receiver rectangle. */
    enum RectPoint implements Relation {
        INSIDE(b -> new double[] {cx(b), cy(b)}),
        EDGE_LEFT(b -> new double[] {b[L], cy(b)}),
        EDGE_RIGHT(b -> new double[] {b[R], cy(b)}),
        EDGE_TOP(b -> new double[] {cx(b), b[T]}),
        EDGE_BOTTOM(b -> new double[] {cx(b), b[B]}),
        CORNER_TOP_LEFT(b -> new double[] {b[L], b[T]}),
        CORNER_TOP_RIGHT(b -> new double[] {b[R], b[T]}),
        CORNER_BOTTOM_LEFT(b -> new double[] {b[L], b[B]}),
        CORNER_BOTTOM_RIGHT(b -> new double[] {b[R], b[B]}),
        OUTSIDE_LEFT(b -> new double[] {b[L] - UNIT, cy(b)}),
        OUTSIDE_RIGHT(b -> new double[] {b[R] + UNIT, cy(b)}),
        OUTSIDE_TOP(b -> new double[] {cx(b), b[T] - UNIT}),
        OUTSIDE_BOTTOM(b -> new double[] {cx(b), b[B] + UNIT});

        private final UnaryOperator<double[]> rule;

        RectPoint(UnaryOperator<double[]> rule) {
            this.rule = rule;
        }

        @Override
        public double[] of(double[] receiver) {
            return rule.apply(receiver);
        }
    }

    /** The argument point against the receiver point. */
    enum PointPoint implements Relation {
        IDENTICAL(b -> b.clone()),
        NEGATED(b -> new double[] {-b[X], -b[Y]}),
        ZERO(b -> new double[] {ORIGIN, ORIGIN}),
        SAME_X(b -> new double[] {b[X], b[Y] + UNIT}),
        SAME_Y(b -> new double[] {b[X] + UNIT, b[Y]}),
        DISJOINT(b -> new double[] {b[X] + SHIFT_X, b[Y] + SHIFT_Y});

        private final UnaryOperator<double[]> rule;

        PointPoint(UnaryOperator<double[]> rule) {
            this.rule = rule;
        }

        @Override
        public double[] of(double[] receiver) {
            return rule.apply(receiver);
        }
    }

    /** A parsed spec: parameter position (none for the receiver), constructor parameter descriptor, roles. */
    record Spec(int position, String descriptor, String roles) {
        boolean rect() {
            return roles.length() == RECT_ROLES.length();
        }
    }

    private static final String RECT_ROLES = "LTRB";
    private static final String POINT_ROLES = "XY";
    private static final int NO_POSITION = -1;
    private static final int L = 0;
    private static final int T = 1;
    private static final int R = 2;
    private static final int B = 3;
    private static final int X = 0;
    private static final int Y = 1;
    private static final double[][] RECT_BASES = {{0, 0, 10, 10}, {-20, -8, 12, 24}, {100, 50, 104, 58}};
    private static final double[] RECT_FRACTIONAL = {0.5, -1.25, 3.75, 2.5};
    private static final double[][] POINT_BASES = {{3, 4}, {-7, 2}, {0, 0}};
    private static final double[] POINT_FRACTIONAL = {0.5, -2.25};
    private static final double ORIGIN = 0;
    private static final double QUARTER = 4;
    private static final double HALF = 2;
    private static final double UNIT = 1;
    private static final double TWICE = 2;
    private static final double SHIFT_X = 7;
    private static final double SHIFT_Y = -5;
    private static final char INT = 'I';
    private static final char LONG = 'J';
    private static final char FLOAT = 'F';
    private static final char PARAMS_START = '(';
    private static final char PARAMS_END = ')';
    private static final String OBJECT = "java.lang.Object";
    private static final String ON_BASE = " on base ";
    private static final String OPEN = " (";
    private static final String CLOSE = ")";

    private M3GraphicsAtomsRelations() {}

    /** The number of cases of a RELATIONAL row: relations of the role pair times the receiver's bases. */
    static int cases(String receiver, String argument) {
        Spec r = parse(receiver);
        return relations(r, parse(argument)).size() * bases(r).size();
    }

    /** The relation and base of case {@code i}, for mismatch messages. */
    static String label(String receiver, String argument, int i) {
        Spec r = parse(receiver);
        int n = bases(r).size();
        return OPEN + relations(r, parse(argument)).get(i / n) + ON_BASE + i % n + CLOSE;
    }

    /** Case {@code i} as logical values: the receiver, then the arguments (the related one at its position). */
    static List<M3GraphicsAtomsValues.Logical> logicalCase(String atom, String key, List<String> tokens,
            String receiver, String argument, int i) {
        Spec r = parse(receiver);
        Spec a = parse(argument);
        List<double[]> bases = bases(r);
        double[] base = bases.get(i % bases.size());
        double[] related = relations(r, a).get(i / bases.size()).of(base);
        String binary = M3GraphicsAtomsOracle.binaryName(tokens.get(a.position()));
        String type = OBJECT.equals(binary) ? atom : M3GraphicsAtomsOracle.shortName(binary);
        M3GraphicsAtomsValues.Context ctx = new M3GraphicsAtomsValues.Context(atom, key,
                M3GraphicsAtomsValues.seed(key), List.of());
        return Stream.concat(Stream.of(constructed(atom, r, base)), IntStream.range(0, tokens.size())
                .mapToObj(p -> p == a.position() ? constructed(type, a, related)
                        : M3GraphicsAtomsValues.arg(tokens.get(p), ctx, i, p))).toList();
    }

    private static Spec parse(String spec) {
        int open = spec.indexOf(PARAMS_START);
        int close = spec.indexOf(PARAMS_END);
        int position = open == 0 ? NO_POSITION : Integer.parseInt(spec.substring(0, open));
        return new Spec(position, spec.substring(open, close + 1), spec.substring(close + 1));
    }

    private static List<Relation> relations(Spec r, Spec a) {
        Relation[] all = r.rect() ? (a.rect() ? RectRect.values() : RectPoint.values()) : PointPoint.values();
        return List.of(all);
    }

    private static List<double[]> bases(Spec r) {
        List<double[]> out = new ArrayList<>(List.of(r.rect() ? RECT_BASES : POINT_BASES));
        if (r.descriptor().indexOf(FLOAT) >= 0) {
            out.add(r.rect() ? RECT_FRACTIONAL : POINT_FRACTIONAL);
        }
        return out;
    }

    private static double w(double[] b) {
        return b[R] - b[L];
    }

    private static double h(double[] b) {
        return b[B] - b[T];
    }

    private static double cx(double[] b) {
        return b[L] + w(b) / HALF;
    }

    private static double cy(double[] b) {
        return b[T] + h(b) / HALF;
    }

    /** A value built by the spec's constructor: parameter p takes the coordinate of its role, in its own type. */
    private static M3GraphicsAtomsValues.Logical constructed(String type, Spec s, double[] coordinates) {
        List<String> params = M3GraphicsAtomsOracle.params(s.descriptor());
        String roles = s.rect() ? RECT_ROLES : POINT_ROLES;
        return new M3GraphicsAtomsValues.Constructed(type, s.descriptor(), IntStream.range(0, params.size())
                .mapToObj(p -> plain(params.get(p).charAt(0), coordinates[roles.indexOf(s.roles().charAt(p))]))
                .toList());
    }

    private static M3GraphicsAtomsValues.Logical plain(char kind, double v) {
        Object boxed = kind == INT ? (Object) (int) v : kind == LONG ? (Object) (long) v
                : kind == FLOAT ? (Object) (float) v : (Object) v;
        return new M3GraphicsAtomsValues.Plain(boxed);
    }
}
