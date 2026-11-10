/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Immutable payloads for COP contexts. Metadata never confers execution authority.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/dag/ContextDagModel.java}
 * blob {@code 13c68131180e19ccb0b12f1315cb58e3c4be1c89} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class DagModel {
    private DagModel() { }

    /** Host estimates in work units, nanoseconds, cost micro-units and bytes. */
    public record Estimate(long work, long nanos, long costMicros, long memoryBytes) {
        public static final Estimate ZERO = new Estimate(0, 0, 0, 0);
        public Estimate {
            require(work >= 0 && nanos >= 0 && costMicros >= 0 && memoryBytes >= 0,
                    "NEGATIVE_ESTIMATE");
        }
        public Estimate plus(Estimate other) {
            return new Estimate(Math.addExact(work, other.work), Math.addExact(nanos, other.nanos),
                    Math.addExact(costMicros, other.costMicros),
                    Math.addExact(memoryBytes, other.memoryBytes));
        }
        public Estimate times(long size) {
            require(size >= 0, "NEGATIVE_TASK_SIZE");
            return new Estimate(Math.multiplyExact(work, size), Math.multiplyExact(nanos, size),
                    Math.multiplyExact(costMicros, size), Math.multiplyExact(memoryBytes, size));
        }
        public boolean fits(Estimate budget) {
            return work <= budget.work && nanos <= budget.nanos
                    && costMicros <= budget.costMicros && memoryBytes <= budget.memoryBytes;
        }
    }

    /** Costs for a leaf, or explicit overhead for a composite (children are counted separately). */
    public record CostModel(Estimate fixed, Estimate perItem) {
        public CostModel {
            Objects.requireNonNull(fixed, "fixed");
            Objects.requireNonNull(perItem, "perItem");
        }
        public Estimate at(long size) { return fixed.plus(perItem.times(size)); }
    }

    /** One invocation of a versioned skill; dependencies are local graph node IDs. */
    public record Node(String id, String skillKey, Set<String> dependencies) {
        public Node {
            id = identifier(id); skillKey = identifier(skillKey);
            dependencies = identifiers(dependencies, 31);
            require(!dependencies.contains(id), "SELF_DEPENDENCY");
        }
    }

    /** Small immutable graph; nested skill references give DAGs of DAGs without eager expansion. */
    public record Graph(List<Node> nodes) {
        public Graph {
            nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
            require(!nodes.isEmpty() && nodes.size() <= 32, "GRAPH_NODE_LIMIT");
            Set<String> ids = nodes.stream().map(Node::id).collect(Collectors.toSet());
            require(ids.size() == nodes.size(), "DUPLICATE_NODE");
            require(nodes.stream().allMatch(n -> ids.containsAll(n.dependencies())),
                    "MISSING_DEPENDENCY");
            nodes = nodes.stream().sorted(java.util.Comparator.comparing(Node::id)).toList();
        }
    }

    /** Exactly one of capability (leaf) or graph (composite) is present. Key includes version. */
    public record Skill(String key, Set<String> tags, long minSize, long maxSize,
                        CostModel costs, String capability, Graph graph) {
        public Skill {
            key = identifier(key); tags = normalizedTags(tags);
            require(minSize >= 0 && maxSize >= minSize, "TASK_SIZE_RANGE");
            Objects.requireNonNull(costs, "costs");
            require((capability == null) != (graph == null), "LEAF_OR_COMPOSITE_REQUIRED");
            if (capability != null) capability = identifier(capability);
        }
        public static Skill leaf(String key, Set<String> tags, long minSize, long maxSize,
                                 CostModel costs, String capability) {
            return new Skill(key, tags, minSize, maxSize, costs, capability, null);
        }
        public static Skill composite(String key, Set<String> tags, long minSize, long maxSize,
                                      CostModel overhead, Graph graph) {
            return new Skill(key, tags, minSize, maxSize, overhead, null, graph);
        }
    }

    /** Append-only skill inventory. Updating a definition requires a new versioned key. */
    public record Catalog(Map<String, Skill> skills) {
        public Catalog {
            Objects.requireNonNull(skills, "skills");
            require(skills.size() <= 4096, "CATALOG_LIMIT");
            TreeMap<String, Skill> copy = new TreeMap<>();
            skills.forEach((key, skill) -> {
                require(Objects.requireNonNull(skill, "skill").key().equals(key), "SKILL_KEY_MISMATCH");
                copy.put(key, skill);
            });
            skills = Collections.unmodifiableMap(copy);
        }
        public static Catalog empty() { return new Catalog(Map.of()); }
        public Catalog add(List<Skill> additions) {
            Objects.requireNonNull(additions, "additions");
            require(additions.size() <= 4096, "CATALOG_LIMIT");
            TreeMap<String, Skill> copy = new TreeMap<>(skills);
            additions.forEach(skill -> {
                Objects.requireNonNull(skill, "skill");
                Skill prior = copy.putIfAbsent(skill.key(), skill);
                require(prior == null || prior.equals(skill), "SKILL_VERSION_CONFLICT");
            });
            return new Catalog(copy);
        }
        public String root() {
            List<String> fields = new ArrayList<>(List.of("COP-SKILL-CATALOG/1", "" + skills.size()));
            skills.values().forEach(skill -> {
                fields.add(skill.key()); fields.add("" + skill.tags().size()); fields.addAll(skill.tags());
                fields.add("" + skill.minSize()); fields.add("" + skill.maxSize());
                estimateFields(fields, skill.costs().fixed()); estimateFields(fields, skill.costs().perItem());
                fields.add(skill.graph() == null ? "LEAF" : "GRAPH");
                if (skill.graph() == null) fields.add(skill.capability());
                else {
                    fields.add("" + skill.graph().nodes().size());
                    skill.graph().nodes().forEach(node -> {
                        fields.add(node.id()); fields.add(node.skillKey());
                        fields.add("" + node.dependencies().size()); fields.addAll(node.dependencies());
                    });
                }
            });
            return hash(fields);
        }
    }

    /** Host bounds; maxNodes also bounds the flat plan accepted by the existing CPU compiler. */
    public record Limits(int maxDepth, int maxNodes, int maxCandidates, Estimate budget) {
        public Limits {
            require(maxDepth >= 1 && maxDepth <= 32, "DEPTH_LIMIT");
            require(maxNodes >= 1 && maxNodes <= 32, "EXPANDED_NODE_LIMIT");
            require(maxCandidates >= 1 && maxCandidates <= 4096, "CANDIDATE_LIMIT");
            Objects.requireNonNull(budget, "budget");
        }
    }

    /** Host-scoped search, not a policy supplied by a model. Zero score threshold admits tagless search. */
    public record Query(Set<String> tags, long taskSize, Set<String> allowedCapabilities,
                        Limits limits, double minTagSimilarity) {
        public Query {
            tags = normalizedTags(tags);
            require(taskSize >= 0, "NEGATIVE_TASK_SIZE");
            allowedCapabilities = identifiers(allowedCapabilities, 256);
            require(!allowedCapabilities.isEmpty(), "CAPABILITY_SCOPE_REQUIRED");
            Objects.requireNonNull(limits, "limits");
            require(Double.isFinite(minTagSimilarity) && minTagSimilarity >= 0
                    && minTagSimilarity <= 1, "TAG_THRESHOLD");
        }
        public String root() {
            List<String> fields = new ArrayList<>(List.of("COP-DAG-QUERY/1", "" + taskSize,
                    "" + limits.maxDepth(), "" + limits.maxNodes(), "" + limits.maxCandidates(),
                    Double.toHexString(minTagSimilarity), "" + tags.size()));
            fields.addAll(tags); fields.add("" + allowedCapabilities.size());
            fields.addAll(allowedCapabilities); estimateFields(fields, limits.budget());
            return hash(fields);
        }
    }

    /** Stable leaf invocation. Host code binds input explicitly; no URI or executable expression exists. */
    public record Leaf(String id, String skillKey, String capability, List<String> path,
                       Set<String> dependencies) {
        public Leaf {
            id = identifier(id); skillKey = identifier(skillKey); capability = identifier(capability);
            path = List.copyOf(path); dependencies = identifiers(dependencies, 31);
            require(!path.isEmpty() && path.size() <= 33, "PATH_LIMIT");
            path.forEach(DagModel::identifier);
        }
    }

    /** Planner output is immutable advisory data, not an execution permission. */
    public record Plan(String catalogRoot, String queryRoot, String skillKey, List<Leaf> leaves,
                       Estimate estimate, double tagSimilarity, double score) {
        public Plan {
            require(catalogRoot != null && catalogRoot.matches("[0-9a-f]{64}"), "CATALOG_ROOT");
            require(queryRoot != null && queryRoot.matches("[0-9a-f]{64}"), "QUERY_ROOT");
            skillKey = identifier(skillKey); leaves = List.copyOf(leaves);
            require(!leaves.isEmpty() && leaves.size() <= 32, "EXPANDED_NODE_LIMIT");
            Objects.requireNonNull(estimate, "estimate");
            require(Double.isFinite(tagSimilarity) && tagSimilarity >= 0 && tagSimilarity <= 1
                    && Double.isFinite(score) && score >= 0 && score <= 1, "INVALID_SCORE");
        }
        public String root() {
            List<String> fields = new ArrayList<>(List.of("COP-DAG-PLAN/1", catalogRoot,
                    queryRoot, skillKey, Double.toHexString(tagSimilarity), Double.toHexString(score),
                    "" + leaves.size()));
            estimateFields(fields, estimate);
            leaves.forEach(leaf -> {
                fields.add(leaf.id()); fields.add(leaf.skillKey()); fields.add(leaf.capability());
                fields.add("" + leaf.path().size()); fields.addAll(leaf.path());
                fields.add("" + leaf.dependencies().size()); fields.addAll(leaf.dependencies());
            });
            return hash(fields);
        }
        public boolean executionAuthority() { return false; }
    }

    /** Canonical bounded text evidence identity; hashing does not authenticate its producer. */
    public static String textEvidenceRoot(String text) {
        require(text != null && text.length() <= 65_536, "TEXT_EVIDENCE_LIMIT");
        return hash(List.of("COP-DAG-TEXT/1", text));
    }

    static void require(boolean condition, String reason) {
        if (!condition) throw new IllegalArgumentException(reason);
    }
    static String identifier(String text) {
        require(text != null && text.matches("[A-Za-z][A-Za-z0-9_.-]{0,95}"), "INVALID_IDENTIFIER");
        return text;
    }
    private static Set<String> identifiers(Set<String> input, int max) {
        Objects.requireNonNull(input, "identifiers"); require(input.size() <= max, "IDENTIFIER_LIMIT");
        TreeSet<String> result = input.stream().map(DagModel::identifier)
                .collect(Collectors.toCollection(TreeSet::new));
        return Collections.unmodifiableSet(result);
    }
    private static Set<String> normalizedTags(Set<String> input) {
        Objects.requireNonNull(input, "tags"); require(input.size() <= 64, "TAG_LIMIT");
        TreeSet<String> result = input.stream().map(tag -> {
            String normalized = Objects.requireNonNull(tag, "tag").strip().toLowerCase(Locale.ROOT);
            require(!normalized.isEmpty() && normalized.length() <= 96, "INVALID_TAG");
            return normalized;
        }).collect(Collectors.toCollection(TreeSet::new));
        return Collections.unmodifiableSet(result);
    }
    private static void estimateFields(List<String> fields, Estimate value) {
        fields.add("" + value.work()); fields.add("" + value.nanos());
        fields.add("" + value.costMicros()); fields.add("" + value.memoryBytes());
    }
    /** Length-prefixed UTF-8 fields prevent concatenation ambiguities. */
    static String hash(List<String> fields) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            fields.forEach(field -> {
                byte[] bytes = field.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
                digest.update(bytes);
            });
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
