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
package jdk.m3.collections;

import static jdk.m3.collections.M3MatSignalSemantics.Evidence.DOMAIN_ASSERTED;
import static jdk.m3.collections.M3MatSignalSemantics.Evidence.EXACT_DECLARATION;
import static jdk.m3.collections.M3MatSignalSemantics.Evidence.HEURISTIC;
import static jdk.m3.collections.M3MatSignalSemantics.Evidence.OBSERVED_SAMPLE;
import static jdk.m3.collections.M3MatSignalSemantics.Evidence.STATIC_POSSIBLE;
import static jdk.m3.collections.M3MatSignalSemantics.Invalidation.BINARY_ROOT;
import static jdk.m3.collections.M3MatSignalSemantics.Invalidation.CLASSPATH_AND_SOURCE;
import static jdk.m3.collections.M3MatSignalSemantics.Invalidation.OBJECT_VERSION;
import static jdk.m3.collections.M3MatSignalSemantics.Invalidation.QUERY_AND_SOURCE;
import static jdk.m3.collections.M3MatSignalSemantics.Invalidation.RUNTIME_EPOCH;
import static jdk.m3.collections.M3MatSignalSemantics.Invalidation.SOURCE_ROOT;
import static jdk.m3.collections.M3MatSignalSemantics.Role.CONFIDENCE;
import static jdk.m3.collections.M3MatSignalSemantics.Role.COST;
import static jdk.m3.collections.M3MatSignalSemantics.Role.COUNT;
import static jdk.m3.collections.M3MatSignalSemantics.Role.DISTANCE;
import static jdk.m3.collections.M3MatSignalSemantics.Role.FLAG;
import static jdk.m3.collections.M3MatSignalSemantics.Role.LOWER_BOUND;
import static jdk.m3.collections.M3MatSignalSemantics.Role.ORDINAL;
import static jdk.m3.collections.M3MatSignalSemantics.Role.SKETCH;
import static jdk.m3.collections.M3MatSignalSemantics.Role.WEIGHT;
import static jdk.m3.collections.M3MatSignalSemantics.Scope.COLLECTION;
import static jdk.m3.collections.M3MatSignalSemantics.Scope.EDGE;
import static jdk.m3.collections.M3MatSignalSemantics.Scope.FIELD;
import static jdk.m3.collections.M3MatSignalSemantics.Scope.GRAPH;
import static jdk.m3.collections.M3MatSignalSemantics.Scope.METHOD;
import static jdk.m3.collections.M3MatSignalSemantics.Scope.OBJECT;
import static jdk.m3.collections.M3MatSignalSemantics.Scope.PACKAGE;
import static jdk.m3.collections.M3MatSignalSemantics.Scope.QUERY;
import static jdk.m3.collections.M3MatSignalSemantics.Scope.TYPE;
import static jdk.m3.collections.M3MatSignalSemantics.Stage.BYTECODE;
import static jdk.m3.collections.M3MatSignalSemantics.Stage.COMPILER_ATTRIBUTED;
import static jdk.m3.collections.M3MatSignalSemantics.Stage.DOMAIN_DECLARED;
import static jdk.m3.collections.M3MatSignalSemantics.Stage.QUERY_RELATIVE;
import static jdk.m3.collections.M3MatSignalSemantics.Stage.RUNTIME_OBSERVED;
import static jdk.m3.collections.M3MatSignalSemantics.Stage.SOURCE_PARSE;

import jdk.m3.collections.M3MatSignalSemantics.Descriptor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Versioned M3 signal-category catalogue for source-time recipe discovery and signal admission.
 *
 * <p>These categories indicate possible feature inputs, not proof of a runtime call graph,
 * semantic equivalence, safe refactoring, total order or admissible metric. Existing MAT and
 * source-structure owners remain the value, graph and sort authorities.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatSignalCategoryCatalogue.java}
 * blob {@code 7f589997ef6770a249dc7dbdd3cbcaa2754a17bb} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatSignalCategoryCatalogue {
    public static final String VERSION = "M3_SIGNAL_CATEGORY_CATALOGUE_V1";
    private static final List<Descriptor> ENTRIES = buildEntries();
    private static final Map<String, Descriptor> BY_ID = index(ENTRIES);
    private static final String ROOT_SHA256 = root(ENTRIES);

    private M3MatSignalCategoryCatalogue() {}

    public static List<Descriptor> entries() { return ENTRIES; }
    public static String rootSha256() { return ROOT_SHA256; }

    public static Descriptor find(String id) {
        return BY_ID.get(Objects.requireNonNull(id, "id"));
    }

    public static List<Descriptor> forScope(M3MatSignalSemantics.Scope scope) {
        Objects.requireNonNull(scope, "scope");
        return ENTRIES.stream().filter(entry -> entry.scope() == scope).toList();
    }

    public static List<Descriptor> fromStage(M3MatSignalSemantics.Stage stage) {
        Objects.requireNonNull(stage, "stage");
        return ENTRIES.stream().filter(entry -> entry.stage() == stage).toList();
    }

    private static List<Descriptor> buildEntries() {
        List<Descriptor> result = new ArrayList<>();
        // Source-parsed counts and sketches are declaration facts, not semantic equivalence.
        result.add(d("name.package-depth", PACKAGE, SOURCE_PARSE, COUNT, EXACT_DECLARATION, SOURCE_ROOT, "segments"));
        result.add(d("name.type-tokens", TYPE, SOURCE_PARSE, SKETCH, HEURISTIC, SOURCE_ROOT, "tokens"));
        result.add(d("name.method-ngrams", METHOD, SOURCE_PARSE, SKETCH, HEURISTIC, SOURCE_ROOT, "ngrams"));
        result.add(d("name.field-ngrams", FIELD, SOURCE_PARSE, SKETCH, HEURISTIC, SOURCE_ROOT, "ngrams"));
        result.add(d("signature.method-arity", METHOD, SOURCE_PARSE, COUNT, EXACT_DECLARATION, SOURCE_ROOT, "parameters"));
        result.add(d("signature.generic-parameters", TYPE, SOURCE_PARSE, COUNT, EXACT_DECLARATION, SOURCE_ROOT, "parameters"));
        result.add(d("signature.thrown-types", METHOD, SOURCE_PARSE, COUNT, EXACT_DECLARATION, SOURCE_ROOT, "types"));
        result.add(d("signature.annotations", METHOD, SOURCE_PARSE, COUNT, EXACT_DECLARATION, SOURCE_ROOT, "annotations"));
        result.add(d("type.field-count", TYPE, SOURCE_PARSE, COUNT, EXACT_DECLARATION, SOURCE_ROOT, "fields"));
        result.add(d("type.method-count", TYPE, SOURCE_PARSE, COUNT, EXACT_DECLARATION, SOURCE_ROOT, "methods"));
        result.add(d("type.inheritance-depth-estimate", TYPE, SOURCE_PARSE, COUNT, HEURISTIC, SOURCE_ROOT, "levels"));
        result.add(d("type.mutable-field-count", TYPE, SOURCE_PARSE, COUNT, EXACT_DECLARATION, SOURCE_ROOT, "fields"));
        result.add(d("control.branches", METHOD, SOURCE_PARSE, COUNT, EXACT_DECLARATION, SOURCE_ROOT, "syntax-nodes"));
        result.add(d("control.loops", METHOD, SOURCE_PARSE, COUNT, EXACT_DECLARATION, SOURCE_ROOT, "syntax-nodes"));
        result.add(d("control.nesting-depth", METHOD, SOURCE_PARSE, COUNT, EXACT_DECLARATION, SOURCE_ROOT, "levels"));
        result.add(d("control.switch-arms", METHOD, SOURCE_PARSE, COUNT, EXACT_DECLARATION, SOURCE_ROOT, "syntax-nodes"));
        result.add(d("control.cyclomatic-estimate", METHOD, SOURCE_PARSE, WEIGHT, HEURISTIC, SOURCE_ROOT, "points"));
        result.add(d("control.exception-handlers", METHOD, SOURCE_PARSE, COUNT, EXACT_DECLARATION, SOURCE_ROOT, "handlers"));
        result.add(d("control.synchronized-blocks", METHOD, SOURCE_PARSE, COUNT, EXACT_DECLARATION, SOURCE_ROOT, "syntax-nodes"));
        result.add(d("control.lambda-count", METHOD, SOURCE_PARSE, COUNT, EXACT_DECLARATION, SOURCE_ROOT, "syntax-nodes"));
        result.add(d("allocation.new-expressions", METHOD, SOURCE_PARSE, COUNT, EXACT_DECLARATION, SOURCE_ROOT, "syntax-nodes"));
        result.add(d("allocation.estimated-bytes", METHOD, RUNTIME_OBSERVED, COST, OBSERVED_SAMPLE, RUNTIME_EPOCH, "bytes"));
        result.add(d("effects.may-write-fields", METHOD, COMPILER_ATTRIBUTED, FLAG, STATIC_POSSIBLE, CLASSPATH_AND_SOURCE, "boolean"));
        result.add(d("effects.may-throw", METHOD, COMPILER_ATTRIBUTED, FLAG, STATIC_POSSIBLE, CLASSPATH_AND_SOURCE, "boolean"));
        result.add(d("effects.may-io", METHOD, COMPILER_ATTRIBUTED, FLAG, HEURISTIC, CLASSPATH_AND_SOURCE, "boolean"));
        result.add(d("dataflow.def-use-count", METHOD, COMPILER_ATTRIBUTED, COUNT, STATIC_POSSIBLE, CLASSPATH_AND_SOURCE, "edges"));
        result.add(d("dataflow.escape-candidates", METHOD, COMPILER_ATTRIBUTED, COUNT, STATIC_POSSIBLE, CLASSPATH_AND_SOURCE, "objects"));
        result.add(d("call.attributed-sites", METHOD, COMPILER_ATTRIBUTED, COUNT, EXACT_DECLARATION, CLASSPATH_AND_SOURCE, "call-sites"));
        result.add(d("call.possible-dispatch-targets", EDGE, COMPILER_ATTRIBUTED, COUNT, STATIC_POSSIBLE, CLASSPATH_AND_SOURCE, "targets"));
        result.add(d("graph.in-degree", GRAPH, SOURCE_PARSE, COUNT, EXACT_DECLARATION, SOURCE_ROOT, "edges"));
        result.add(d("graph.out-degree", GRAPH, SOURCE_PARSE, COUNT, EXACT_DECLARATION, SOURCE_ROOT, "edges"));
        result.add(d("graph.scc-size", GRAPH, SOURCE_PARSE, COUNT, EXACT_DECLARATION, SOURCE_ROOT, "nodes"));
        result.add(d("graph.change-impact-estimate", GRAPH, SOURCE_PARSE, WEIGHT, HEURISTIC, SOURCE_ROOT, "relative"));
        result.add(d("graph.centrality-estimate", GRAPH, SOURCE_PARSE, WEIGHT, HEURISTIC, SOURCE_ROOT, "relative"));
        result.add(d("api.descriptor-delta", METHOD, BYTECODE, COUNT, EXACT_DECLARATION, BINARY_ROOT, "descriptors"));
        result.add(d("api.compatibility-risk", METHOD, BYTECODE, WEIGHT, HEURISTIC, BINARY_ROOT, "relative"));
        result.add(d("test.coverage-sampled", METHOD, RUNTIME_OBSERVED, CONFIDENCE, OBSERVED_SAMPLE, RUNTIME_EPOCH, "fraction"));
        result.add(d("test.failure-frequency", METHOD, RUNTIME_OBSERVED, WEIGHT, OBSERVED_SAMPLE, RUNTIME_EPOCH, "per-run"));
        result.add(d("runtime.cpu-time", METHOD, RUNTIME_OBSERVED, COST, OBSERVED_SAMPLE, RUNTIME_EPOCH, "ns"));
        result.add(d("runtime.cache-misses", METHOD, RUNTIME_OBSERVED, COST, OBSERVED_SAMPLE, RUNTIME_EPOCH, "events"));
        result.add(d("runtime.lock-contention", METHOD, RUNTIME_OBSERVED, COST, OBSERVED_SAMPLE, RUNTIME_EPOCH, "ns"));
        result.add(d("runtime.reuse-distance", OBJECT, RUNTIME_OBSERVED, WEIGHT, OBSERVED_SAMPLE, RUNTIME_EPOCH, "accesses"));
        result.add(d("runtime.heap-headroom", COLLECTION, RUNTIME_OBSERVED, COST, OBSERVED_SAMPLE, RUNTIME_EPOCH, "bytes"));
        result.add(d("query.edit-distance", QUERY, QUERY_RELATIVE, DISTANCE, DOMAIN_ASSERTED, QUERY_AND_SOURCE, "edits"));
        result.add(d("query.semantic-similarity", QUERY, QUERY_RELATIVE, WEIGHT, HEURISTIC, QUERY_AND_SOURCE, "relative"));
        result.add(d("query.regex-candidate", QUERY, QUERY_RELATIVE, FLAG, HEURISTIC, QUERY_AND_SOURCE, "boolean"));
        result.add(d("domain.absolute-weight", OBJECT, DOMAIN_DECLARED, WEIGHT, DOMAIN_ASSERTED, OBJECT_VERSION, "domain"));
        result.add(d("domain.distance", OBJECT, DOMAIN_DECLARED, DISTANCE, DOMAIN_ASSERTED, OBJECT_VERSION, "domain"));
        result.add(d("domain.lexical-ordinal", OBJECT, DOMAIN_DECLARED, ORDINAL, DOMAIN_ASSERTED, OBJECT_VERSION, "lexical"));
        result.add(d("domain.relevance-confidence", OBJECT, DOMAIN_DECLARED, CONFIDENCE, DOMAIN_ASSERTED, OBJECT_VERSION, "fraction"));
        result.sort(Comparator.comparing(Descriptor::id));
        return List.copyOf(result);
    }

    private static Descriptor d(
            String id, M3MatSignalSemantics.Scope scope, M3MatSignalSemantics.Stage stage,
            M3MatSignalSemantics.Role role, M3MatSignalSemantics.Evidence evidence,
            M3MatSignalSemantics.Invalidation invalidation, String unit) {
        return new Descriptor(id, scope, stage, role, evidence, invalidation, unit, false, false);
    }

    private static Map<String, Descriptor> index(List<Descriptor> values) {
        TreeMap<String, Descriptor> map = new TreeMap<>();
        for (Descriptor value : values) {
            if (map.putIfAbsent(value.id(), value) != null) {
                throw new IllegalStateException("duplicate signal category: " + value.id());
            }
        }
        return Map.copyOf(map);
    }

    private static String root(List<Descriptor> values) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            update(sha, VERSION);
            for (Descriptor item : values) {
                update(sha, item.id());
                update(sha, item.scope().name());
                update(sha, item.stage().name());
                update(sha, item.role().name());
                update(sha, item.evidence().name());
                update(sha, item.invalidation().name());
                update(sha, item.unit());
                update(sha, Boolean.toString(item.exactOrderClaim()));
                update(sha, Boolean.toString(item.admissibleBoundClaim()));
            }
            return HexFormat.of().formatHex(sha.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static void update(MessageDigest sha, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        sha.update((byte) (bytes.length >>> 24));
        sha.update((byte) (bytes.length >>> 16));
        sha.update((byte) (bytes.length >>> 8));
        sha.update((byte) bytes.length);
        sha.update(bytes);
    }
}
