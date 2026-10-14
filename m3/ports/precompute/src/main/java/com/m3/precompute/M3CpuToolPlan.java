// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Source- and contract-aware immutable CPU DAG preparation for the M3JDK precompute port.
 * No JDK platform class is modified. A prepared matcher is private to its plan, not a
 * global cache of caller data or an authorization to run external tools.
 */
public final class M3CpuToolPlan {
    private static final int MAX_NODES = 128;
    private static final int MAX_EDGES = 512;
    private static final int MAX_CONSTANT_UTF8 = 4096;
    private static final String ID_REGEX = "[a-z][a-z0-9_.-]{0,63}";

    private M3CpuToolPlan() {}

    /** A serialized declaration contains values, not callbacks, executable paths or shell. */
    public record Step(String id, String operation, List<String> dependencies, String constant) {
        public Step {
            requireId(id);
            requireId(operation);
            dependencies = List.copyOf(Objects.requireNonNull(dependencies, "dependencies"));
            if (dependencies.size() > 16) throw new IllegalArgumentException("too many inputs");
            for (String dependency : dependencies) requireId(dependency);
            if (new HashSet<>(dependencies).size() != dependencies.size()) {
                throw new IllegalArgumentException("duplicate input");
            }
            constant = Objects.requireNonNull(constant, "constant");
            if (constant.length() > MAX_CONSTANT_UTF8) {
                throw new IllegalArgumentException("constant too long");
            }
        }
    }

    static final class PreparedStep {
        final Step declaration;
        final byte[] literal;
        final int[] prefix;
        final byte xor;

        PreparedStep(Step declaration) {
            this.declaration = declaration;
            this.literal = declaration.operation().equals("search.literal")
                    ? utf8Exact(declaration.constant()) : new byte[0];
            if (declaration.operation().equals("search.literal") && literal.length == 0) {
                throw new IllegalArgumentException("empty literal");
            }
            this.prefix = kmpPrefix(literal);
            if (declaration.operation().equals("bytes.xor")) {
                if (!declaration.constant().matches("[0-9a-fA-F]{2}")) {
                    throw new IllegalArgumentException("xor constant must be two hexadecimal digits");
                }
                this.xor = (byte) Integer.parseInt(declaration.constant(), 16);
            } else {
                this.xor = 0;
                if (!declaration.operation().equals("search.literal")
                        && !declaration.constant().isEmpty()) {
                    throw new IllegalArgumentException("unused constant: " + declaration.operation());
                }
            }
        }
    }

    /** Stable source definitions can be exported; prepared private arrays never escape. */
    public static final class Compiled {
        private final List<Step> definitions;
        private final List<PreparedStep> order;
        private final String outputId;
        private final String sha256;

        private Compiled(List<Step> definitions, List<PreparedStep> order, String outputId) {
            this.definitions = List.copyOf(definitions);
            this.order = List.copyOf(order);
            this.outputId = outputId;
            this.sha256 = fingerprint(definitions, outputId);
        }

        public List<Step> definitions() { return definitions; }
        public String outputId() { return outputId; }
        public String sha256() { return sha256; }
        public int stages() { return order.size(); }
        List<PreparedStep> orderedExecution() { return order; }
    }

    /** Precompute all stable IDs, the topology, UTF-8 literal bytes and KMP failure tables. */
    public static Compiled compile(List<Step> steps, String outputId) {
        Objects.requireNonNull(steps, "steps");
        requireId(outputId);
        if (steps.isEmpty() || steps.size() > MAX_NODES) {
            throw new IllegalArgumentException("node budget");
        }
        Map<String, Step> byId = new TreeMap<>();
        int edges = 0;
        for (Step step : steps) {
            Objects.requireNonNull(step, "step");
            if (byId.putIfAbsent(step.id(), step) != null) {
                throw new IllegalArgumentException("duplicate node: " + step.id());
            }
            if (!M3CpuToolRuntime.operations().contains(step.operation())) {
                throw new IllegalArgumentException("unimplemented operation: " + step.operation());
            }
            int degree = step.dependencies().size();
            if (step.operation().equals("bytes.concat") ? degree < 2 : degree > 1) {
                throw new IllegalArgumentException("invalid input arity: " + step.id());
            }
            edges += degree;
            if (edges > MAX_EDGES) throw new IllegalArgumentException("edge budget");
        }
        if (!byId.containsKey(outputId)) throw new IllegalArgumentException("missing output");
        for (Step step : steps) {
            for (String input : step.dependencies()) {
                if (!byId.containsKey(input)) throw new IllegalArgumentException("missing input: " + input);
            }
        }
        Set<String> finished = new HashSet<>();
        Set<String> active = new HashSet<>();
        List<Step> topological = new ArrayList<>(steps.size());
        visit(outputId, byId, active, finished, topological);
        if (finished.size() != steps.size()) {
            throw new IllegalArgumentException("unreachable node");
        }
        List<PreparedStep> prepared = new ArrayList<>(topological.size());
        for (Step step : topological) prepared.add(new PreparedStep(step));
        return new Compiled(List.copyOf(byId.values()), prepared, outputId);
    }

    private static void visit(String id, Map<String, Step> nodes, Set<String> active,
            Set<String> finished, List<Step> ordered) {
        if (finished.contains(id)) return;
        if (!active.add(id)) throw new IllegalArgumentException("cyclic DAG");
        Step step = nodes.get(id);
        for (String dependency : step.dependencies()) {
            visit(dependency, nodes, active, finished, ordered);
        }
        active.remove(id);
        finished.add(id);
        ordered.add(step);
    }

    private static void requireId(String id) {
        if (id == null || !id.matches(ID_REGEX)) {
            throw new IllegalArgumentException("invalid atom ID: " + id);
        }
    }

    static byte[] utf8Exact(String value) {
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(java.nio.CharBuffer.wrap(value));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            if (bytes.length > MAX_CONSTANT_UTF8) throw new IllegalArgumentException("UTF-8 bound");
            return bytes;
        } catch (CharacterCodingException malformed) {
            throw new IllegalArgumentException("malformed constant UTF-16", malformed);
        }
    }

    static int[] kmpPrefix(byte[] literal) {
        int[] prefix = new int[literal.length];
        for (int i = 1, matched = 0; i < literal.length; i++) {
            while (matched > 0 && literal[i] != literal[matched]) matched = prefix[matched - 1];
            if (literal[i] == literal[matched]) matched++;
            prefix[i] = matched;
        }
        return prefix;
    }

    private static String fingerprint(List<Step> definitions, String outputId) {
        MessageDigest digest = sha256();
        frame(digest, "M3_CPU_PRECOMPUTE_PLAN_V1");
        frame(digest, outputId);
        frame(digest, Integer.toString(definitions.size()));
        for (Step step : definitions) {
            frame(digest, step.id());
            frame(digest, step.operation());
            frame(digest, step.constant());
            frame(digest, Integer.toString(step.dependencies().size()));
            for (String id : step.dependencies()) frame(digest, id);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException unavailable) { throw new IllegalStateException(unavailable); }
    }

    static void frame(MessageDigest digest, String value) {
        byte[] bytes = utf8Exact(value);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
