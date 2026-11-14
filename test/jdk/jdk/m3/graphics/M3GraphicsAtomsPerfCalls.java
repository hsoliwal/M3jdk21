/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * The prepared calls of one perf row (M3GraphicsAtomsPerfSupport): the first cases of the atom's tests
 * (M3GraphicsAtoms.logicalCase, at most {@link #MAX_CALLS}), materialized once per side and probed once per side.
 * Cases that return on both sides are timed; when there is none, cases that throw the same type on both sides are timed
 * (exception path). A case whose outcome differs between the sides is counted as divergent (the tests report the
 * difference; timing different behaviour says nothing).
 */
final class M3GraphicsAtomsPerfCalls {
    /** The prepared calls of a row per side, how they behave, and how many cases diverged. */
    record Calls(Object[][] m3, Object[][] oracle, boolean exceptionPath, int divergent) {}

    private static final int MAX_CALLS = 64;

    private M3GraphicsAtomsPerfCalls() {}

    /** The calls of a member: up to MAX_CALLS cases, probed on both sides. */
    static Calls of(String atom, M3GraphicsAtoms.Kind kind, String name, String descriptor) {
        return calls(atom, kind, name, descriptor, M3GraphicsAtomsPerfSupport.handle(M3GraphicsAtomsOracle.Side.M3,
                atom, kind, name, descriptor));
    }

    private static Calls calls(String atom, M3GraphicsAtoms.Kind kind, String name, String descriptor,
            MethodHandle m3) {
        List<String> tokens = M3GraphicsAtomsOracle.params(descriptor);
        String key = M3GraphicsAtoms.key(atom, name, descriptor);
        int n = Math.min(MAX_CALLS, M3GraphicsAtoms.cases(kind, tokens.size(), M3GraphicsAtomsValues.constants(atom)));
        MethodHandle oracle = M3GraphicsAtomsPerfSupport.handle(M3GraphicsAtomsOracle.Side.ORACLE, atom, kind, name,
                descriptor);
        List<Object[]> returnsM3 = new ArrayList<>();
        List<Object[]> returnsOracle = new ArrayList<>();
        List<Object[]> throwsM3 = new ArrayList<>();
        List<Object[]> throwsOracle = new ArrayList<>();
        int[] divergent = {0};
        IntStream.range(0, n).forEach(i -> {
            List<M3GraphicsAtomsValues.Logical> l = M3GraphicsAtoms.logicalCase(atom, kind, key, tokens, i);
            Optional<Throwable> a = probe(l, kind, M3GraphicsAtomsOracle.Side.M3, m3);
            Optional<Throwable> b = probe(l, kind, M3GraphicsAtomsOracle.Side.ORACLE, oracle);
            boolean same = a.isEmpty() == b.isEmpty() && (a.isEmpty()
                    || M3GraphicsAtomsCompare.sameType(a.get().getClass(), b.get().getClass()));
            divergent[0] += same ? 0 : 1;
            List<Object[]> toM3 = a.isEmpty() ? returnsM3 : throwsM3;
            List<Object[]> toOracle = a.isEmpty() ? returnsOracle : throwsOracle;
            prepared(l, kind, M3GraphicsAtomsOracle.Side.M3).filter(x -> same).ifPresent(x -> {
                prepared(l, kind, M3GraphicsAtomsOracle.Side.ORACLE).ifPresent(y -> {
                    toM3.add(x);
                    toOracle.add(y);
                });
            });
        });
        boolean exceptionPath = returnsM3.isEmpty();
        List<Object[]> m3Calls = exceptionPath ? throwsM3 : returnsM3;
        List<Object[]> oracleCalls = exceptionPath ? throwsOracle : returnsOracle;
        return new Calls(m3Calls.toArray(Object[][]::new), oracleCalls.toArray(Object[][]::new), exceptionPath,
                divergent[0]);
    }

    private static Optional<Throwable> probe(List<M3GraphicsAtomsValues.Logical> l, M3GraphicsAtoms.Kind kind,
            M3GraphicsAtomsOracle.Side side, MethodHandle h) {
        try {
            Object unused = h.invokeExact(materialize(l, kind, side));
            return Optional.empty();
        } catch (InvocationTargetException e) {
            return Optional.of(e.getCause());
        } catch (Throwable t) {
            return Optional.of(t);
        }
    }

    private static Optional<Object[]> prepared(List<M3GraphicsAtomsValues.Logical> l, M3GraphicsAtoms.Kind kind,
            M3GraphicsAtomsOracle.Side side) {
        try {
            return Optional.of(materialize(l, kind, side));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return Optional.empty();
        }
    }

    private static Object[] materialize(List<M3GraphicsAtomsValues.Logical> l, M3GraphicsAtoms.Kind kind,
            M3GraphicsAtomsOracle.Side side) throws ReflectiveOperationException {
        boolean instance = kind == M3GraphicsAtoms.Kind.METHOD || kind == M3GraphicsAtoms.Kind.FIELD;
        List<M3GraphicsAtomsValues.Logical> used = instance ? l : l.subList(1, l.size());
        Object[] call = new Object[used.size()];
        for (int p = 0; p < call.length; p++) {
            call[p] = M3GraphicsAtomsValues.materialize(used.get(p), side);
        }
        return call;
    }
}
