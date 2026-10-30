// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.mop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Patternize pillar: versioned, append-only pattern registry and atom classification.
 *
 * <p>A pattern is the javac tree-kind shape of an atom under schema {@value #SCHEMA}; its id is
 * {@code M3P1-} plus 16 hex of the domain-separated shape key. KNOWN_PATTERN: the exact atom bytes
 * recur in the run or are a registered exemplar. UNPROVEN_LEAF: the shape recurs (in the run or in
 * the registry) with other bytes, so substitution still needs a gate proof. UNKNOWN_PATTERN: no
 * recurrence. Registry rows are only appended: each new exemplar of a pattern id gets the next
 * version, and the byte prefix is proven unchanged with {@link M3RecipeMemory#verifyAppendOnly}.
 * Ported from Synexia M3AtomPatternIndex (#13966) plus the persisted registry its gap G2 lacked.</p>
 */
//FLLDCIM: MOP — patterns versioned; UNKNOWN_PATTERN and UNPROVEN_LEAF are explicit states
public final class M3PatternRegistry {
    public static final String SCHEMA = "M3-PATTERN/1";
    public static final String HEADER = "schema\tpatternId\tversion\tkind\tshapeKey\texemplarSha256"
            + "\texemplar\tstate";
    public static final String PATTERNS_HEADER = "atomId\tpath\tkind\towner\tname\tpatternSchema"
            + "\tpatternId\tpatternVersion\tstate\tmembers\texactMembers";
    private static final String KEY_DOMAIN = "M3_PATTERN_KEY_V1";
    private static final Pattern ID = Pattern.compile("M3P1-[0-9a-f]{16}");
    private static final Pattern HEX = Pattern.compile("[0-9a-f]{64}");
    private static final long MAX_BYTES = 16L << 20;

    public enum State { KNOWN_PATTERN, UNPROVEN_LEAF, UNKNOWN_PATTERN }

    /** One registered exemplar of a pattern version. */
    public record Row(String patternId, int version, String kind, String shapeKey,
            String exemplarSha256, String exemplar, State state) {
        String tsv() {
            return String.join("\t", SCHEMA, patternId, Integer.toString(version), kind, shapeKey,
                    exemplarSha256, exemplar, state.name());
        }

        String key() { return patternId + '\t' + exemplarSha256; }
    }

    /** Pattern verdict of one atom; {@code version} 0 means the pattern is not registered. */
    public record Verdict(M3Atom atom, String patternId, int version, State state, int members,
            int exactMembers) {
        public String row() {
            return String.join("\t", atom.id(), atom.path(), atom.kind(), atom.owner(), atom.name(),
                    SCHEMA, patternId, Integer.toString(version), state.name(),
                    Integer.toString(members), Integer.toString(exactMembers));
        }
    }

    private final List<Row> rows;
    private final Map<String, Integer> exemplars;
    private final Map<String, Integer> latest;

    private M3PatternRegistry(List<Row> rows) {
        this.rows = List.copyOf(rows);
        this.exemplars = rows.stream().collect(Collectors.toMap(Row::key, Row::version));
        this.latest = rows.stream().collect(Collectors.toMap(Row::patternId, Row::version, Math::max));
    }

    public static M3PatternRegistry empty() { return new M3PatternRegistry(List.of()); }

    public List<Row> rows() { return rows; }

    /** Content-addressed pattern id of a shape key. */
    public static String patternId(String shapeKey) {
        return "M3P1-" + M3MopReceipt.canonical(KEY_DOMAIN, SCHEMA, shapeKey).substring(0, 16);
    }

    /** Reads and validates a registry; an absent file is the empty registry. */
    public static M3PatternRegistry read(Path file) throws IOException {
        if (!Files.exists(file)) return empty();
        if (!Files.isRegularFile(file) || Files.size(file) > MAX_BYTES) {
            throw new IOException("M3_PATTERN_REGISTRY_UNSAFE " + file);
        }
        String content = Files.readString(file, StandardCharsets.UTF_8);
        if (!content.startsWith(HEADER + "\n") || !content.endsWith("\n") || content.contains("\r")
                || content.contains("\n\n")) {
            throw new IOException("M3_PATTERN_REGISTRY_HEADER " + file);
        }
        List<Row> parsed = rows(content.substring(HEADER.length() + 1));
        validate(parsed);
        return new M3PatternRegistry(parsed);
    }

    private static List<Row> rows(String body) throws IOException {
        try {
            return body.lines().map(M3PatternRegistry::parse).toList();
        } catch (IllegalArgumentException malformed) {
            throw new IOException(malformed.getMessage(), malformed);
        }
    }

    /** Classifies every atom against the run and the registry; order independent. */
    public List<Verdict> classify(List<M3Atom> atoms) {
        Map<String, Long> exact = count(atoms, M3Atom::exactSha256);
        Map<String, Long> members = count(atoms, M3Atom::shapeKey);
        Map<String, Long> roots = atoms.stream().collect(Collectors.groupingBy(M3Atom::shapeKey,
                Collectors.mapping(M3Atom::exactSha256, Collectors.collectingAndThen(
                        Collectors.toSet(), set -> (long) set.size()))));
        return atoms.stream().map(atom -> verdict(atom, exact.get(atom.exactSha256()),
                members.get(atom.shapeKey()), roots.get(atom.shapeKey()))).toList();
    }

    /** Appends the not yet registered exemplars of recurring patterns; returns rows appended. */
    public static synchronized int register(Path file, List<Verdict> verdicts) throws IOException {
        M3PatternRegistry current = read(file);
        List<Row> additions = current.additions(verdicts);
        if (additions.isEmpty()) return 0;
        byte[] before = Files.exists(file) ? Files.readAllBytes(file)
                : (HEADER + "\n").getBytes(StandardCharsets.UTF_8);
        String appended = additions.stream().map(row -> row.tsv() + "\n").collect(Collectors.joining());
        java.io.ByteArrayOutputStream after = new java.io.ByteArrayOutputStream();
        after.writeBytes(before);
        after.writeBytes(appended.getBytes(StandardCharsets.UTF_8));
        M3MopReceipt.writeBytes(file, after.toByteArray());
        M3RecipeMemory.verifyAppendOnly(before, Files.readAllBytes(file));
        read(file);
        return additions.size();
    }

    List<Row> additions(List<Verdict> verdicts) {
        Map<String, Verdict> fresh = verdicts.stream().filter(v -> v.state() != State.UNKNOWN_PATTERN)
                .sorted(Comparator.comparing((Verdict v) -> v.atom().path())
                        .thenComparingInt(v -> v.atom().startChar()))
                .filter(v -> !exemplars.containsKey(v.patternId() + '\t' + v.atom().exactSha256()))
                .collect(Collectors.toMap(v -> v.patternId() + '\t' + v.atom().exactSha256(),
                        Function.identity(), (first, later) -> first, TreeMap::new));
        Map<String, Integer> next = new HashMap<>(latest);
        return fresh.values().stream().map(v -> new Row(v.patternId(),
                next.merge(v.patternId(), 1, Integer::sum), v.atom().kind(), v.atom().shapeKey(),
                v.atom().exactSha256(), v.atom().path() + '#' + v.atom().owner() + '#' + v.atom().name(),
                v.state())).toList();
    }

    private Verdict verdict(M3Atom atom, long exactRun, long members, long roots) {
        String id = patternId(atom.shapeKey());
        Integer exactVersion = exemplars.get(id + '\t' + atom.exactSha256());
        int registered = latest.getOrDefault(id, 0);
        State state = exactRun > 1 || exactVersion != null ? State.KNOWN_PATTERN
                : roots > 1 || registered > 0 ? State.UNPROVEN_LEAF : State.UNKNOWN_PATTERN;
        int version = exactVersion != null ? exactVersion : registered;
        return new Verdict(atom, id, version, state, Math.toIntExact(members), Math.toIntExact(exactRun));
    }

    private static Row parse(String line) {
        String[] cells = line.split("\t", -1);
        if (cells.length != 8 || !SCHEMA.equals(cells[0]) || !ID.matcher(cells[1]).matches()
                || !HEX.matcher(cells[4]).matches() || !HEX.matcher(cells[5]).matches()
                || Arrays.stream(cells).anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("M3_PATTERN_REGISTRY_ROW " + line);
        }
        return new Row(cells[1], Integer.parseInt(cells[2]), cells[3], cells[4], cells[5], cells[6],
                State.valueOf(cells[7]));
    }

    private static void validate(List<Row> rows) throws IOException {
        Map<String, Integer> versions = new HashMap<>();
        Set<String> keys = new HashSet<>();
        List<String> faults = rows.stream().filter(row -> !patternId(row.shapeKey()).equals(row.patternId())
                || row.version() != versions.merge(row.patternId(), 1, Integer::sum)
                || row.state() == State.UNKNOWN_PATTERN || !keys.add(row.key()))
                .map(Row::tsv).toList();
        if (!faults.isEmpty()) throw new IOException("M3_PATTERN_REGISTRY_INVALID " + faults.get(0));
    }

    private static Map<String, Long> count(List<M3Atom> atoms, Function<M3Atom, String> key) {
        return atoms.stream().collect(Collectors.groupingBy(key, Collectors.counting()));
    }

    /** Atoms per state, every state present (0 when absent). */
    public static Map<State, Long> states(List<Verdict> verdicts) {
        Map<State, Long> counts = verdicts.stream()
                .collect(Collectors.groupingBy(Verdict::state, Collectors.counting()));
        return Stream.of(State.values()).collect(Collectors.toMap(Function.identity(),
                state -> counts.getOrDefault(state, 0L), Long::sum, () -> new java.util.EnumMap<>(State.class)));
    }
}
