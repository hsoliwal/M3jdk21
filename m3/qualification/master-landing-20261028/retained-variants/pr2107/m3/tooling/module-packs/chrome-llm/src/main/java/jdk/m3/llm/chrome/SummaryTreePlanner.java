/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 * Derived from Synexia 2ab439480ecb0643615e85b42def954dbf3b5627:
 * cpullm-opdag/java/chrome-ai-mechanical/src/main/java/jdk/internal/m3/cpullm/chrome/SummaryTreePlanner.java
 * Adapted for the M3JDK21-owned namespace and qualified summary contracts.
 */
package jdk.m3.llm.chrome;

import static jdk.m3.llm.chrome.MechanicalModel.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Exact UTF-8 chunking + deterministic fixed-fanin Composite summary-tree planner. */
public final class SummaryTreePlanner {
    private SummaryTreePlanner() {}

    public record TreeConfig(
            int maxLeafBytes, int minLeafBytes, int fanin, int maxNodeOutputBytes,
            int maxParentInputBytes, int maxNodes) {
        public TreeConfig {
            if (maxLeafBytes < 1024 || maxLeafBytes > 60 * 1024) throw new IllegalArgumentException("maxLeafBytes");
            if (minLeafBytes < 1 || minLeafBytes > maxLeafBytes) throw new IllegalArgumentException("minLeafBytes");
            if (fanin < 2 || fanin > 32) throw new IllegalArgumentException("fanin");
            if (maxNodeOutputBytes < 256 || maxNodeOutputBytes > 32 * 1024) throw new IllegalArgumentException("maxNodeOutputBytes");
            if (maxParentInputBytes < 1024 || maxParentInputBytes > 60 * 1024) throw new IllegalArgumentException("maxParentInputBytes");
            if (maxNodes < 1 || maxNodes > 1_000_000) throw new IllegalArgumentException("maxNodes");
            if ((long) fanin * (maxNodeOutputBytes + 256L) > maxParentInputBytes) {
                throw new IllegalArgumentException("fanin/output budget exceeds parent input budget");
            }
        }
        public static TreeConfig defaults() { return new TreeConfig(48 * 1024, 8 * 1024, 4, 8 * 1024, 48 * 1024, 100_000); }
    }

    public static SummaryTreePlan plan(String sourceRef, String sourceText, TreeConfig config) {
        if (sourceRef == null || sourceRef.isBlank()) throw new IllegalArgumentException("sourceRef required");
        if (sourceText == null || sourceText.isEmpty()) throw new IllegalArgumentException("sourceText required");
        Objects.requireNonNull(config, "config");
        Hashing.utf8(sourceRef);
        return canonicalPlan(sourceRef, Hashing.utf8(sourceText), config);
    }

    /** Generate the original ordered topology, with allocation bounded at each node. */
    private static SummaryTreePlan canonicalPlan(String sourceRef, byte[] bytes, TreeConfig config) {
        long minimumLeaves = ((long) bytes.length + config.maxLeafBytes() - 1) / config.maxLeafBytes();
        if (minimumLeaves > config.maxNodes()) throw new IllegalArgumentException("tree exceeds maxNodes");
        String sourceSha = Hashing.sha256Hex(bytes);
        List<SummaryNode> leaves = chunk(bytes, sourceSha, config);
        List<SummaryNode> all = new ArrayList<>(leaves);
        List<SummaryNode> previous = leaves;
        int level = 1;
        while (previous.size() > 1) {
            List<SummaryNode> current = new ArrayList<>();
            for (int start = 0, ordinal = 0; start < previous.size(); start += config.fanin(), ordinal++) {
                if (all.size() + current.size() >= config.maxNodes()) {
                    throw new IllegalArgumentException("tree exceeds maxNodes");
                }
                List<SummaryNode> children = previous.subList(start, Math.min(start + config.fanin(), previous.size()));
                List<String> childIds = children.stream().map(SummaryNode::nodeId).toList();
                List<Integer> coverage = children.stream().flatMap(c -> c.coverageLeafOrdinals().stream()).toList();
                String id = Hashing.hash("CPULLM_JAVA_SUMMARY_NODE_V1", "REDUCE", level, ordinal,
                        String.join(",", childIds), coverage);
                current.add(new SummaryNode(id, "REDUCE", level, ordinal, -1, -1, "",
                        childIds, coverage));
            }
            all.addAll(current);
            if (all.size() > config.maxNodes()) throw new IllegalArgumentException("tree exceeds maxNodes");
            previous = current;
            level++;
        }
        SummaryNode root = all.getLast();
        String mechanicalRoot = Hashing.hash("CPULLM_JAVA_SUMMARY_TREE_V1", sourceRef, sourceSha, bytes.length,
                config, all.stream().map(SummaryNode::nodeId).toList(), root.nodeId());
        return new SummaryTreePlan(sourceRef, sourceSha, bytes.length, config,
                leaves.size(), level, all, root.nodeId(), mechanicalRoot);
    }

    static boolean verify(SummaryTreePlan plan, String sourceText) {
        try {
            if (plan == null || sourceText == null || sourceText.isEmpty()
                    || plan.sourceRef() == null || plan.sourceRef().isBlank()
                    || plan.config() == null || plan.nodes().isEmpty()
                    || plan.nodes().size() > plan.config().maxNodes()) return false;
            Hashing.utf8(plan.sourceRef());
            byte[] bytes = Hashing.utf8(sourceText);
            if (bytes.length != plan.sourceBytes()) return false;
            // A re-signed arbitrary graph is not the canonical deterministic plan.
            // Full replay checks every record field, ordering, leaf coverage and level.
            // It also replaces the previous repeated linear child lookup.
            return canonicalPlan(plan.sourceRef(), bytes, plan.config()).equals(plan);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static List<SummaryNode> chunk(byte[] bytes, String sourceSha, TreeConfig config) {
        List<SummaryNode> leaves = new ArrayList<>();
        int start = 0;
        int ordinal = 0;
        while (start < bytes.length) {
            if (leaves.size() >= config.maxNodes()) throw new IllegalArgumentException("tree exceeds maxNodes");
            int desired = start + Math.min(config.maxLeafBytes(), bytes.length - start);
            int end = safeEnd(bytes, start, desired, config.minLeafBytes());
            byte[] raw = Arrays.copyOfRange(bytes, start, end);
            String inputSha = Hashing.sha256Hex(raw);
            String id = Hashing.hash("CPULLM_JAVA_SUMMARY_NODE_V1", "LEAF", 0, ordinal,
                    sourceSha, start, end, inputSha);
            leaves.add(new SummaryNode(id, "LEAF", 0, ordinal, start, end, inputSha,
                    List.of(), List.of(ordinal)));
            start = end;
            ordinal++;
        }
        return List.copyOf(leaves);
    }

    private static int safeEnd(byte[] bytes, int start, int desired, int minimum) {
        if (desired >= bytes.length) return bytes.length;
        int floor = Math.min(desired, start + minimum);
        for (int i = desired - 1; i >= floor; i--) {
            if (bytes[i] == '\n') return i + 1;
        }
        int end = desired;
        while (end > start && end < bytes.length && (bytes[end] & 0xC0) == 0x80) end--;
        if (end <= start) throw new IllegalArgumentException("unable to create UTF-8 safe chunk");
        try { decodeStrict(Arrays.copyOfRange(bytes, start, end)); }
        catch (CharacterCodingException e) { throw new IllegalArgumentException("invalid UTF-8 split", e); }
        return end;
    }

    private static CharBuffer decodeStrict(byte[] bytes) throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes));
    }
}
