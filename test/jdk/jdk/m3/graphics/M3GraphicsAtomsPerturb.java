/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Per-field perturbation cases of the jdk.m3.graphics atom tests: equals and hashCode of a value built by a
 * field-mapping constructor (the generator checked that each parameter is stored into its own field and nothing else
 * happens) against a copy with exactly one parameter, so exactly one field, changed. A dropped field in equals makes
 * the M3 side answer true where the oracle answers false. Each perturbable position (a parameter that is not always
 * null) gets {@link #ROUNDS} cases. The base round is the first of a few alternatives the oracle builds; the changed
 * argument comes from the first later round whose oracle value differs (not equal, not NaN, not +0/-0) and whose copy
 * builds. Both choices depend on the oracle only. Hashes compare exactly when identity hashes are pinned or the oracle
 * values hash without identity; otherwise the pair's hash equality must agree between the sides.
 */
final class M3GraphicsAtomsPerturb {
    /** A receiver and its one-field copy. */
    record Pair(M3GraphicsAtomsValues.Logical receiver, M3GraphicsAtomsValues.Logical copy) {}

    /** What one side did: the two values, then x.equals(y), y.equals(x), x.hashCode(), y.hashCode(); or a throwable. */
    record Observed(Object x, Object y, List<Object> results, Throwable thrown) {}

    static final int ROUNDS = 2;
    private static final int ATTEMPTS = 8;
    private static final int STRIDE = 7919;
    private static final int FIRST_ALTERNATIVE = 1;
    private static final int X_EQUALS_Y = 0;
    private static final int Y_EQUALS_X = 1;
    private static final int HASH_X = 2;
    private static final int HASH_Y = 3;
    private static final String SALT = "#perturb";
    private static final String THROWN = "perturbed";
    private static final String EQUALS = "perturbed.equals";
    private static final String HASH = "perturbed.hashCode";
    private static final String PARTITION = "perturbed.hashCode equality";
    private static final String NATIVE = "native code reached in a perturbation case: ";
    private static final String VERSUS = " / oracle ";
    private static final String COLON = ": M3 ";

    private M3GraphicsAtomsPerturb() {}

    /** The positions of a constructor's parameters that can change: those that are not always null. */
    static List<Integer> positions(String descriptor) {
        List<String> tokens = M3GraphicsAtomsOracle.params(descriptor);
        return IntStream.range(0, tokens.size()).filter(p -> !M3GraphicsAtomsValues.alwaysNull(tokens.get(p)))
                .boxed().toList();
    }

    /** Case {@code i}: position positions[i / ROUNDS], base round choice i % ROUNDS. */
    static Pair pair(String atom, String descriptor, List<Integer> positions, int i) {
        String key = M3GraphicsAtoms.key(atom, M3GraphicsAtomsOracle.CONSTRUCTOR, descriptor);
        M3GraphicsAtomsValues.Context c = new M3GraphicsAtomsValues.Context(atom, key,
                M3GraphicsAtomsValues.seed(key + SALT), List.of());
        List<String> tokens = M3GraphicsAtomsOracle.params(descriptor);
        int position = positions.get(i / ROUNDS);
        int first = i % ROUNDS;
        int base = IntStream.range(0, ATTEMPTS).map(k -> first + k * STRIDE)
                .filter(r -> builds(atom, descriptor, args(tokens, c, r))).findFirst().orElse(first);
        List<M3GraphicsAtomsValues.Logical> args = args(tokens, c, base);
        List<M3GraphicsAtomsValues.Logical> copy = IntStream.rangeClosed(FIRST_ALTERNATIVE, ATTEMPTS)
                .mapToObj(k -> with(args, position, M3GraphicsAtomsValues.arg(tokens.get(position), c, base + k,
                        position)))
                .filter(a -> differs(args.get(position), a.get(position)) && builds(atom, descriptor, a))
                .findFirst().orElseGet(() -> with(args, position, M3GraphicsAtomsValues.arg(tokens.get(position), c,
                        base + FIRST_ALTERNATIVE, position)));
        return new Pair(new M3GraphicsAtomsValues.Constructed(atom, descriptor, args),
                new M3GraphicsAtomsValues.Constructed(atom, descriptor, copy));
    }

    /** Builds both values on one side and calls equals both ways and hashCode on each. */
    static Observed observe(Pair p, M3GraphicsAtomsOracle.Side side) {
        Object x = null;
        Object y = null;
        try {
            x = M3GraphicsAtomsValues.materialize(p.receiver(), side);
            y = M3GraphicsAtomsValues.materialize(p.copy(), side);
            return new Observed(x, y, List.of(x.equals(y), y.equals(x), x.hashCode(), y.hashCode()), null);
        } catch (Throwable t) {
            return new Observed(x, y, List.of(), t instanceof InvocationTargetException e ? e.getCause() : t);
        }
    }

    /** The first difference between the two sides' observations of one pair, empty when they agree. */
    static Optional<String> compare(Observed m, Observed o) {
        if (m.thrown() != null || o.thrown() != null) {
            return thrown(m.thrown(), o.thrown());
        }
        if (!m.results().subList(X_EQUALS_Y, HASH_X).equals(o.results().subList(X_EQUALS_Y, HASH_X))) {
            return differ(EQUALS, m.results().subList(X_EQUALS_Y, HASH_X), o.results().subList(X_EQUALS_Y, HASH_X));
        }
        int mx = (Integer) m.results().get(HASH_X);
        int my = (Integer) m.results().get(HASH_Y);
        int ox = (Integer) o.results().get(HASH_X);
        int oy = (Integer) o.results().get(HASH_Y);
        if (M3GraphicsAtomsHashes.exact(o.x()) && M3GraphicsAtomsHashes.exact(o.y())) {
            M3GraphicsAtomsHashes.exactCompared();
            return mx == ox && my == oy ? Optional.empty() : differ(HASH, List.of(mx, my), List.of(ox, oy));
        }
        M3GraphicsAtomsHashes.partitionCompared();
        return (mx == my) == (ox == oy) ? Optional.empty() : differ(PARTITION, mx == my, ox == oy);
    }

    private static Optional<String> thrown(Throwable m, Throwable o) {
        if (M3GraphicsAtoms.reachesNatives(m) || M3GraphicsAtoms.reachesNatives(o)) {
            return Optional.of(NATIVE + M3GraphicsAtomsCompare.show(m) + VERSUS + M3GraphicsAtomsCompare.show(o));
        }
        return m != null && o != null ? M3GraphicsAtomsMessages.compare(THROWN, m, o) : differ(THROWN, m, o);
    }

    private static Optional<String> differ(String path, Object m, Object o) {
        return Optional.of(path + COLON + M3GraphicsAtomsCompare.show(m) + VERSUS + M3GraphicsAtomsCompare.show(o));
    }

    private static List<M3GraphicsAtomsValues.Logical> args(List<String> tokens, M3GraphicsAtomsValues.Context c,
            int round) {
        return IntStream.range(0, tokens.size()).mapToObj(p -> M3GraphicsAtomsValues.arg(tokens.get(p), c, round, p))
                .toList();
    }

    private static List<M3GraphicsAtomsValues.Logical> with(List<M3GraphicsAtomsValues.Logical> args, int position,
            M3GraphicsAtomsValues.Logical value) {
        List<M3GraphicsAtomsValues.Logical> out = new ArrayList<>(args);
        out.set(position, value);
        return List.copyOf(out);
    }

    private static boolean builds(String atom, String descriptor, List<M3GraphicsAtomsValues.Logical> args) {
        try {
            M3GraphicsAtomsValues.materialize(new M3GraphicsAtomsValues.Constructed(atom, descriptor, args),
                    M3GraphicsAtomsOracle.Side.ORACLE);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return false;
        }
    }

    /** True when two logical arguments materialize to clearly different oracle values. */
    private static boolean differs(M3GraphicsAtomsValues.Logical a, M3GraphicsAtomsValues.Logical b) {
        try {
            Object x = M3GraphicsAtomsValues.materialize(a, M3GraphicsAtomsOracle.Side.ORACLE);
            Object y = M3GraphicsAtomsValues.materialize(b, M3GraphicsAtomsOracle.Side.ORACLE);
            if (x instanceof Float f && y instanceof Float g) {
                return !f.isNaN() && !g.isNaN() && f.floatValue() != g.floatValue();
            }
            if (x instanceof Double d && y instanceof Double e) {
                return !d.isNaN() && !e.isNaN() && d.doubleValue() != e.doubleValue();
            }
            return !Objects.deepEquals(x, y);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return false;
        }
    }
}
