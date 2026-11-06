/*
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
 */

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * One performance test per method atom of every owned decorator: each TARGETS.tsv row is timed
 * by {@code M3DecoratorDifferential.perf} (equal backings, the same seeded arguments, warm-up,
 * median of the measured rounds), and each method fails when the owned decorator is more than
 * {@code m3.perf.bound} times slower per call than the Commons oracle (default 4). A target with
 * an atom over the bound is measured once more and judged on its better pass, because JIT state
 * left by earlier targets skews a first pass (a probe measured {@code isEmpty} at 39 ns first and
 * 5.2 ns warm, equal to Commons). This is a guard against pathological regressions, such as a copy
 * where Commons delegates; it is not a benchmark. With {@code m3.perf.out} set, one TSV row per
 * atom is appended there (label, atom, owned ns, Commons ns, ratio).
 *
 * <p>Properties: {@code m3.decorator.dir}, {@code m3.perf.calls} (default 500),
 * {@code m3.perf.rounds} (default 5), {@code m3.perf.bound}, {@code m3.perf.out}.
 */
final class M3DecoratorPerfTest {
    static final String PROP_CALLS = "m3.perf.calls";
    static final String PROP_ROUNDS = "m3.perf.rounds";
    static final String PROP_BOUND = "m3.perf.bound";
    static final String PROP_OUT = "m3.perf.out";
    static final int DEFAULT_CALLS = 500;
    static final int DEFAULT_ROUNDS = 5;
    static final String DEFAULT_BOUND = "4";
    /** A Commons time below one nanosecond per call is clamped to one before the ratio. */
    static final double MIN_ORACLE_NS = 1.0;
    static final int OWNED_NS = 0;
    static final int ORACLE_NS = 1;
    static final String NO_ORACLE = "no oracle timing";
    static final String OVER_BOUND = "%.1f ns per call against %.1f for Commons";
    static final String ROW = "%s\t%s\t%.1f\t%.1f\t%.2f";
    static final int CALLS = Integer.getInteger(PROP_CALLS, DEFAULT_CALLS);
    static final int ROUNDS = Integer.getInteger(PROP_ROUNDS, DEFAULT_ROUNDS);
    static final double BOUND = Double.parseDouble(System.getProperty(PROP_BOUND, DEFAULT_BOUND));
    static final String OUT = System.getProperty(PROP_OUT, "");

    @TestFactory
    Stream<DynamicNode> everyMethodAtomOfEveryTarget() throws IOException {
        return M3DecoratorAtomTest.targets(M3DecoratorAtomTest.DIR).stream()
                .map(M3DecoratorPerfTest::target);
    }

    private static DynamicNode target(String[] t) {
        String label = M3DecoratorAtomTest.label(t);
        Map<String, double[]> times;
        try {
            times = measure(t);
            if (times.values().stream().anyMatch(M3DecoratorPerfTest::over)) {
                times = best(times, measure(t));
            }
            record(label, times);
        } catch (ReflectiveOperationException | IOException e) {
            return DynamicTest.dynamicTest(label, () -> {
                throw e;
            });
        }
        return DynamicContainer.dynamicContainer(label, times.entrySet().stream()
                .map(e -> DynamicTest.dynamicTest(e.getKey(), () -> {
                    double[] ns = e.getValue();
                    Assumptions.assumeFalse(Double.isNaN(ns[ORACLE_NS]), NO_ORACLE);
                    assertTrue(!over(ns), String.format(Locale.ROOT, OVER_BOUND, ns[OWNED_NS],
                            ns[ORACLE_NS]));
                })));
    }

    private static Map<String, double[]> measure(String[] t) throws ReflectiveOperationException {
        return M3DecoratorDifferential.perf(t[M3DecoratorAtomTest.COL_OWNED],
                t[M3DecoratorAtomTest.COL_ORACLE], t[M3DecoratorAtomTest.COL_BACKING],
                t[M3DecoratorAtomTest.COL_FACTORY], M3DecoratorAtomTest.policy(t), CALLS, ROUNDS);
    }

    private static boolean over(double[] ns) {
        return ns[OWNED_NS] > BOUND * Math.max(ns[ORACLE_NS], MIN_ORACLE_NS);
    }

    private static double ratio(double[] ns) {
        return ns[OWNED_NS] / ns[ORACLE_NS];
    }

    /** Per atom, the pass with the lower owned-to-Commons ratio. */
    private static Map<String, double[]> best(Map<String, double[]> a, Map<String, double[]> b) {
        Map<String, double[]> out = new TreeMap<>(a);
        b.forEach((k, v) -> out.merge(k, v, (x, y) -> ratio(y) < ratio(x) ? y : x));
        return out;
    }

    private static void record(String label, Map<String, double[]> times) throws IOException {
        if (!OUT.isEmpty()) {
            List<String> rows = times.entrySet().stream().map(e -> String.format(Locale.ROOT,
                    ROW, label, e.getKey(), e.getValue()[OWNED_NS], e.getValue()[ORACLE_NS],
                    ratio(e.getValue()))).toList();
            Files.write(Path.of(OUT), rows, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        }
    }
}
