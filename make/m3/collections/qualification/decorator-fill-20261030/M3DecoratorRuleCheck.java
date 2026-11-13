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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Classifier side-condition checks (refutations R4, R5, R6 of the PREDICATED skeptic): each
 * case runs one real or
 * synthetic atom through the same classification code and compares the category with the
 * expected one. A planted
 * defect in any guard method (ctorArgIsParam, superIsDelegate, unshadowed, lanesInOrder,
 * siblingIsSuper,
 * checkSetValueValidates) turns its case into FAIL.
 */
final class M3DecoratorRuleCheck {
    private static final String PKG = "org.apache.commons.collections4.";

    private M3DecoratorRuleCheck() {
    }

    static int run(Path sources, Path out) throws IOException, ReflectiveOperationException {
        List<String> rows =
                new ArrayList<>(
                List.of("check\trefutation\tcommons\tinput\texpected\tactual\tresult"));
        String entrySet = "java.util.Set<java.util.Map.Entry<K, V>> entrySet()";
        // v1.5: the transforming hook (return L.apply(value);) is an emitted lane member
        rows.add(
                real(sources, "R4_TRANSFORMING_HOOK", "R4", "map.TransformedMap", entrySet,
                "CHECKED_ENTRY_SET"));
        rows.add(
                unemittedHook(sources, "R4_UNEMITTED_HOOK", "map.TransformedMap", entrySet,
                "MANUAL"));
        rows.add(
                real(sources, "R4_VALIDATING_HOOK", "R4", "map.PredicatedMap", entrySet,
                "CHECKED_ENTRY_SET"));
        rows.add(
                ctor(sources, "R5_SELF_LOCK", "collection.SynchronizedCollection", 1,
                "MANUAL"));
        rows.add(
                ctor(sources, "R5_LOCK_PARAMETER", "collection.SynchronizedCollection", 2,
                "LANES_CONSTRUCTOR"));
        rows.add(
                synthetic(sources, "R6_SUPER_NOT_DELEGATE", "list.PredicatedList",
                "boolean add(E arg0)",
                "validate(object); return super.add(object);", "object", "MANUAL"));
        rows.add(
                real(sources, "R6_SUPER_DELEGATE", "R6", "map.PredicatedMap",
                "void putAll(java.util.Map<? extends K, ? extends V> arg0)",
                "GUARD_DELEGATE/each-entry"));
        rows.add(
                synthetic(sources, "R6_SHADOWED_ALIAS", "map.PredicatedMap",
                "boolean containsKey(Object arg0)",
                "return map.containsKey(map);", "map", "MANUAL"));
        rows.add(
                real(sources, "R6_FIELD_ALIAS", "R6", "map.PredicatedMap",
                "V put(K arg0, V arg1)", "GUARD_DELEGATE/entry"));
        rows.add(
                synthetic(sources, "R6_LANES_SWAPPED", "map.PredicatedSortedMap",
                "java.util.SortedMap<K, V> headMap(K arg0)",
                "final SortedMap<K, V> map = getSortedMap().headMap(toKey); return new " +
                        "PredicatedSortedMap<>(map, valuePredicate, keyPredicate);",
                "toKey", "MANUAL"));
        rows.add(
                real(sources, "R6_LANES_IN_ORDER", "R6", "map.PredicatedSortedMap",
                "java.util.SortedMap<K, V> headMap(K arg0)", "LANE_VIEW/self"));
        rows.add(
                synthetic(sources, "R6_SIBLING_NOT_SUPER", "set.PredicatedSortedSet",
                "java.util.SortedSet<E> headSet(E arg0)",
                "final SortedSet<E> head = decorated().headSet(toElement); return new " +
                        "PredicatedNavigableSet<>(head, predicate);",
                "toElement", "MANUAL"));
        rows.add(
                real(sources, "R6_SIBLING_SUPER", "R6", "set.PredicatedNavigableSet",
                "java.util.SortedSet<E> headSet(E arg0)", "LANE_VIEW/sibling"));
        Files.write(out, rows, StandardCharsets.UTF_8);
        long fail = rows.stream().filter(r -> r.endsWith("\tFAIL")).count();
        M3DecoratorFill.LOG.info(
                () -> "M3_RULE_CHECK checks=" + (rows.size() - 1) + " fail=" + fail);
        return fail == 0 ? 0 : 4;
    }

    private static M3DecoratorFill generator(Path sources, String fqn) throws IOException,
            ReflectiveOperationException {
        Class<?> c = Class.forName(PKG + fqn);
        String scaffold = M3DecoratorFill.synth(c, "M3" + c.getSimpleName(), true);
        M3DecoratorFill g = new M3DecoratorFill(sources, c, scaffold);
        g.atoms(scaffold);
        return g;
    }

    /**
     * One case: the classifier's category, or ERROR:<exception> when a (planted) defect makes
     * it throw.
     */
    interface Case {
        String category() throws IOException, ReflectiveOperationException;
    }

    private static String guarded(Case c) {
        try {
            return c.category();
        } catch (IOException | ReflectiveOperationException | RuntimeException e) {
            return "ERROR:" + e.getClass().getSimpleName();
        }
    }

    private static String real(Path sources, String id, String refutation, String fqn,
            String sig, String expected) {
        return row(id, refutation, fqn, sig, expected,
                guarded(() -> generator(sources, fqn)
                        .classify(M3DecoratorFill.atom("public", sig)).category()));
    }

    /**
     * R4 case with the checkSetValue hook recorded as not emitted (empty member): the
     * entrySet view must stay MANUAL.
     */
    private static String unemittedHook(Path sources, String id, String fqn, String sig,
            String expected) {
        return row(id, "R4", fqn, sig + " with checkSetValue not emitted", expected,
                guarded(() -> {
                    M3DecoratorFill g = generator(sources, fqn);
                    g.members.put("checkSetValue/1", "");
                    return g.classify(M3DecoratorFill.atom("public", sig)).category();
                }));
    }

    private static String synthetic(Path sources, String id, String fqn, String sig,
            String body, String names, String expected) {
        return row(id, "R6", fqn, body, expected, guarded(() -> {
            M3DecoratorFill g = generator(sources, fqn);
            g.declaringNow = g.commons;
            return g.shape(M3DecoratorFill.atom("public", sig), body + "\u0000" + names).category();
        }));
    }

    private static String ctor(Path sources, String id, String fqn, int arity,
            String expected) {
        return row(id, "R5", fqn, fqn + " constructor arity " + arity, expected, guarded(() -> {
            Class<?> c = Class.forName(PKG + fqn);
            M3DecoratorFill g =
                    new M3DecoratorFill(sources, c,
                    M3DecoratorFill.synth(c, "M3" + c.getSimpleName(), false));
            g.laneArity = arity;
            g.lanesConstructor();
            return g.ctorError == null ? "LANES_CONSTRUCTOR" : "MANUAL";
        }));
    }

    private static String row(String id, String refutation, String fqn, String input,
            String expected, String actual) {
        return String.join("\t", id, refutation, fqn, input.replace('\t', ' '), expected,
                actual, expected.equals(actual) ? "PASS" : "FAIL");
    }
}
