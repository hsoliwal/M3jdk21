// SPDX-License-Identifier: Apache-2.0
package jdk.m3.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Immutable hierarchical semantic index persisted by M3IndexDB.
 *
 * <p>Leaf fingerprints come from analyzers. Every node with children is recomputed child-first from
 * canonical role + child-semantic-key ordering, so FILE through REPOSITORY identities are stable
 * across parallel producer order. Producer edge ordinals remain evidence/tie-breakers only.
 */
public final class SemanticIndex {
    public static final String ARTIFACT_KIND = "m3.semantic-index";
    public static final int FORMAT_VERSION = 1;

    private final List<SemanticNode> nodes;
    private final List<SemanticEdge> edges;
    private final Map<String, SemanticNode> byId;
    private final Map<String, List<SemanticNode>> exact;
    private final Map<String, List<SemanticNode>> structure;
    private final Map<String, List<SemanticNode>> logic;
    private final Map<String, List<SemanticNode>> patternRoles;
    private final Map<String, List<SemanticEdge>> children;
    private final Map<String, List<SemanticEdge>> parents;

    private SemanticIndex(
            List<SemanticNode> nodes,
            List<SemanticEdge> edges) {
        this.nodes = List.copyOf(nodes);
        this.edges = List.copyOf(edges);
        this.byId = indexById(this.nodes);
        this.exact = indexByHash(this.nodes, HashKind.EXACT);
        this.structure = indexByHash(this.nodes, HashKind.STRUCTURE);
        this.logic = indexByHash(this.nodes, HashKind.LOGIC);
        this.patternRoles = indexByPatternRole(this.nodes);
        this.children = edgeIndex(this.edges, true, this.byId);
        this.parents = edgeIndex(this.edges, false, this.byId);
    }

    public static SemanticIndex of(
            Collection<SemanticNode> nodeRows,
            Collection<SemanticEdge> edgeRows) {
        Objects.requireNonNull(nodeRows, "nodeRows");
        Objects.requireNonNull(edgeRows, "edgeRows");

        TreeMap<String, SemanticNode> unique = new TreeMap<>();
        for (SemanticNode node : nodeRows) {
            SemanticNode checked = Objects.requireNonNull(node, "node");
            SemanticNode prior = unique.putIfAbsent(checked.nodeId(), checked);
            if (prior != null && !prior.equals(checked)) {
                throw new IllegalArgumentException(
                        "conflicting semantic node rows for " + checked.nodeId());
            }
        }

        List<SemanticEdge> edges = canonicalEdges(edgeRows);
        validateEdges(unique, edges);
        edges = normalizeOrdinals(unique, edges);
        Map<String, List<SemanticEdge>> children = edgeIndex(edges, true, unique);
        TreeMap<String, SemanticNode> recomposed = new TreeMap<>();
        HashMap<String, VisitState> state = new HashMap<>();
        for (String nodeId : unique.keySet()) {
            composeNode(nodeId, unique, children, recomposed, state);
        }
        return new SemanticIndex(List.copyOf(recomposed.values()), edges);
    }

    public static String nodeId(SemanticKind kind, CharSequence semanticKey) {
        Objects.requireNonNull(kind, "kind");
        String key = Objects.requireNonNull(semanticKey, "semanticKey").toString();
        return SemanticFingerprint.utf16Sha256(
                "M3_NODE_V1|" + kind + "|" + key);
    }

    public List<SemanticNode> nodes() {
        return nodes;
    }

    public List<SemanticEdge> edges() {
        return edges;
    }

    public Optional<SemanticNode> find(String nodeId) {
        return Optional.ofNullable(byId.get(Objects.requireNonNull(nodeId, "nodeId")));
    }

    public SemanticNode require(String nodeId) {
        return find(nodeId).orElseThrow(
                () -> new IllegalArgumentException("unknown semantic node: " + nodeId));
    }

    public List<SemanticNode> nodesOfKind(SemanticKind kind) {
        Objects.requireNonNull(kind, "kind");
        return nodes.stream().filter(node -> node.kind() == kind).toList();
    }

    public List<SemanticNode> exactSha256(String sha256) {
        return exact.getOrDefault(requireSha(sha256), List.of());
    }

    public List<SemanticNode> structuralSha256(String sha256) {
        return structure.getOrDefault(requireSha(sha256), List.of());
    }

    public List<SemanticNode> logicSha256(String sha256) {
        return logic.getOrDefault(requireSha(sha256), List.of());
    }

    public List<SemanticNode> patternRole(String patternRole) {
        String checked = Objects.requireNonNull(patternRole, "patternRole").strip();
        if (checked.isEmpty()) throw new IllegalArgumentException("patternRole");
        return patternRoles.getOrDefault(checked, List.of());
    }

    public List<SemanticNode> structuralHash64(long hash) {
        return nodes.stream()
                .filter(node -> node.fingerprint().structuralHash64() == hash)
                .toList();
    }

    public List<SemanticNode> logicHash64(long hash) {
        return nodes.stream()
                .filter(node -> node.fingerprint().logicHash64() == hash)
                .toList();
    }

    /** Candidate-only locality-sensitive lookup; never an equivalence decision. */
    public List<Similarity> nearSimHash(long simHash64, int maximumHammingDistance) {
        if (maximumHammingDistance < 0 || maximumHammingDistance > Long.SIZE) {
            throw new IllegalArgumentException("maximumHammingDistance");
        }
        ArrayList<Similarity> matches = new ArrayList<>();
        for (SemanticNode node : nodes) {
            int distance = SemanticFingerprint.hammingDistance(
                    simHash64,
                    node.fingerprint().simHash64());
            if (distance <= maximumHammingDistance) {
                matches.add(new Similarity(node, distance));
            }
        }
        matches.sort(
                Comparator.comparingInt(Similarity::hammingDistance)
                        .thenComparing(match -> match.node().semanticKey())
                        .thenComparing(match -> match.node().nodeId()));
        return List.copyOf(matches);
    }

    public List<SemanticNode> children(String parentId) {
        return children.getOrDefault(
                        Objects.requireNonNull(parentId, "parentId"),
                        List.of())
                .stream()
                .map(edge -> require(edge.childId()))
                .toList();
    }

    public List<SemanticNode> parents(String childId) {
        return parents.getOrDefault(
                        Objects.requireNonNull(childId, "childId"),
                        List.of())
                .stream()
                .map(edge -> require(edge.parentId()))
                .toList();
    }

    public byte[] encode() {
        return SemanticCodec.encode(this);
    }

    public static SemanticIndex decode(byte[] payload) {
        return SemanticCodec.decode(payload);
    }

    public Artifact store(M3DB database, String artifactName) throws IOException {
        return Objects.requireNonNull(database, "database")
                .putSemanticIndex(artifactName, this);
    }

    public static SemanticIndex load(M3DB database, String artifactName)
            throws IOException {
        return Objects.requireNonNull(database, "database")
                .requireSemanticIndex(artifactName);
    }

    public record Similarity(SemanticNode node, int hammingDistance) {
        public Similarity {
            node = Objects.requireNonNull(node, "node");
            if (hammingDistance < 0 || hammingDistance > Long.SIZE) {
                throw new IllegalArgumentException("hammingDistance");
            }
        }
    }

    private static SemanticNode composeNode(
            String nodeId,
            Map<String, SemanticNode> source,
            Map<String, List<SemanticEdge>> children,
            Map<String, SemanticNode> output,
            Map<String, VisitState> state) {
        SemanticNode known = output.get(nodeId);
        if (known != null) return known;
        if (state.get(nodeId) == VisitState.VISITING) {
            throw new IllegalArgumentException("cyclic semantic composition at " + nodeId);
        }

        state.put(nodeId, VisitState.VISITING);
        SemanticNode node = source.get(nodeId);
        List<SemanticEdge> outgoing = children.getOrDefault(nodeId, List.of());
        SemanticNode result = node;
        if (!outgoing.isEmpty()) {
            ArrayList<SemanticFingerprint.Component> components =
                    new ArrayList<>(outgoing.size());
            for (SemanticEdge edge : outgoing) {
                SemanticNode child =
                        composeNode(edge.childId(), source, children, output, state);
                components.add(
                        new SemanticFingerprint.Component(
                                edge.role(),
                                child.fingerprint()));
            }
            result = node.withFingerprint(
                    SemanticFingerprint.compose(
                            node.kind() + ":" + node.semanticKey(),
                            components));
        }

        output.put(nodeId, result);
        state.put(nodeId, VisitState.DONE);
        return result;
    }

    private static List<SemanticEdge> canonicalEdges(
            Collection<SemanticEdge> rows) {
        ArrayList<SemanticEdge> sorted = new ArrayList<>(rows.size());
        for (SemanticEdge edge : rows) {
            sorted.add(Objects.requireNonNull(edge, "edge"));
        }
        sorted.sort(
                Comparator.comparing(SemanticEdge::parentId)
                        .thenComparing(SemanticEdge::role)
                        .thenComparing(SemanticEdge::childId)
                        .thenComparingInt(SemanticEdge::ordinal));

        ArrayList<SemanticEdge> unique = new ArrayList<>(sorted.size());
        SemanticEdge previous = null;
        for (SemanticEdge edge : sorted) {
            if (!edge.equals(previous)) {
                unique.add(edge);
                previous = edge;
            }
        }
        return List.copyOf(unique);
    }

    private static List<SemanticEdge> normalizeOrdinals(
            Map<String, SemanticNode> nodes,
            List<SemanticEdge> edges) {
        ArrayList<SemanticEdge> sorted = new ArrayList<>(edges);
        sorted.sort((left, right) -> compareForComposition(left, right, nodes));

        ArrayList<SemanticEdge> normalized = new ArrayList<>(sorted.size());
        String priorParent = null;
        String priorRole = null;
        int canonicalOrdinal = 0;
        int priorOrderedOrdinal = -1;
        for (SemanticEdge edge : sorted) {
            boolean newGroup = !edge.parentId().equals(priorParent)
                    || !edge.role().equals(priorRole);
            if (newGroup) {
                canonicalOrdinal = 0;
                priorOrderedOrdinal = -1;
                priorParent = edge.parentId();
                priorRole = edge.role();
            }

            if (orderedRole(edge.role())) {
                if (edge.ordinal() == priorOrderedOrdinal) {
                    throw new IllegalArgumentException(
                            "conflicting ordered semantic ordinal at "
                                    + edge.parentId() + "/" + edge.role() + "#" + edge.ordinal());
                }
                normalized.add(edge);
                priorOrderedOrdinal = edge.ordinal();
            } else {
                normalized.add(
                        new SemanticEdge(
                                edge.parentId(),
                                edge.childId(),
                                edge.role(),
                                canonicalOrdinal++));
            }
        }
        return List.copyOf(normalized);
    }

    private static int compareForComposition(
            SemanticEdge left,
            SemanticEdge right,
            Map<String, SemanticNode> nodes) {
        int compared = left.parentId().compareTo(right.parentId());
        if (compared != 0) return compared;
        compared = left.role().compareTo(right.role());
        if (compared != 0) return compared;

        if (orderedRole(left.role())) {
            compared = Integer.compare(left.ordinal(), right.ordinal());
            if (compared != 0) return compared;
        }

        compared = nodes.get(left.childId())
                .semanticKey()
                .compareTo(nodes.get(right.childId()).semanticKey());
        if (compared != 0) return compared;
        return left.childId().compareTo(right.childId());
    }

    private static boolean orderedRole(String role) {
        return "ATOM".equals(role)
                || "FIELD".equals(role)
                || role.startsWith("ORDERED:");
    }

    private static void validateEdges(
            Map<String, SemanticNode> nodes,
            List<SemanticEdge> edges) {
        for (SemanticEdge edge : edges) {
            if (!nodes.containsKey(edge.parentId())) {
                throw new IllegalArgumentException(
                        "dangling semantic parent: " + edge.parentId());
            }
            if (!nodes.containsKey(edge.childId())) {
                throw new IllegalArgumentException(
                        "dangling semantic child: " + edge.childId());
            }
        }
    }

    private static Map<String, SemanticNode> indexById(
            List<SemanticNode> nodes) {
        LinkedHashMap<String, SemanticNode> result = new LinkedHashMap<>();
        for (SemanticNode node : nodes) result.put(node.nodeId(), node);
        return Map.copyOf(result);
    }

    private static Map<String, List<SemanticNode>> indexByPatternRole(
            List<SemanticNode> nodes) {
        HashMap<String, ArrayList<SemanticNode>> mutable = new HashMap<>();
        for (SemanticNode node : nodes) {
            mutable.computeIfAbsent(node.patternRole(), ignored -> new ArrayList<>()).add(node);
        }
        HashMap<String, List<SemanticNode>> result = new HashMap<>();
        mutable.forEach((role, values) -> {
            values.sort(Comparator.comparing(SemanticNode::semanticKey));
            result.put(role, List.copyOf(values));
        });
        return Map.copyOf(result);
    }

    private static Map<String, List<SemanticNode>> indexByHash(
            List<SemanticNode> nodes,
            HashKind kind) {
        HashMap<String, ArrayList<SemanticNode>> mutable = new HashMap<>();
        for (SemanticNode node : nodes) {
            String hash = switch (kind) {
                case EXACT -> node.fingerprint().exactSha256();
                case STRUCTURE -> node.fingerprint().structuralSha256();
                case LOGIC -> node.fingerprint().logicSha256();
            };
            mutable.computeIfAbsent(hash, ignored -> new ArrayList<>()).add(node);
        }
        HashMap<String, List<SemanticNode>> result = new HashMap<>();
        mutable.forEach((hash, values) -> {
            values.sort(Comparator.comparing(SemanticNode::semanticKey));
            result.put(hash, List.copyOf(values));
        });
        return Map.copyOf(result);
    }

    private static Map<String, List<SemanticEdge>> edgeIndex(
            List<SemanticEdge> edges,
            boolean byParent,
            Map<String, SemanticNode> nodes) {
        HashMap<String, ArrayList<SemanticEdge>> mutable = new HashMap<>();
        for (SemanticEdge edge : edges) {
            String key = byParent ? edge.parentId() : edge.childId();
            mutable.computeIfAbsent(key, ignored -> new ArrayList<>()).add(edge);
        }
        HashMap<String, List<SemanticEdge>> result = new HashMap<>();
        mutable.forEach((key, values) -> {
            values.sort(byParent
                    ? (left, right) -> compareForComposition(left, right, nodes)
                    : Comparator.comparing(
                                    (SemanticEdge edge) ->
                                            nodes.get(edge.parentId()).semanticKey())
                            .thenComparing(SemanticEdge::role)
                            .thenComparingInt(SemanticEdge::ordinal)
                            .thenComparing(SemanticEdge::parentId));
            result.put(key, List.copyOf(values));
        });
        return Map.copyOf(result);
    }

    private static String requireSha(String value) {
        String checked = Objects.requireNonNull(value, "sha256");
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("lowercase SHA-256 required");
        }
        return checked;
    }

    private enum HashKind {
        EXACT,
        STRUCTURE,
        LOGIC
    }

    private enum VisitState {
        VISITING,
        DONE
    }
}
