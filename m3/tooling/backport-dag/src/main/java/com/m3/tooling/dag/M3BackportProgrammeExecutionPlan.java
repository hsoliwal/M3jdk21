// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import com.m3.rewrite.scope.M3EditScope;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Joins programme status to concrete packet DAGs without widening compatibility or scope authority.
 */
public final class M3BackportProgrammeExecutionPlan {
    public static final String SCHEMA = "M3_BACKPORT_PROGRAMME_EXECUTION_PLAN_V1";

    public enum State {
        RUNNABLE,
        AUTHOR_RECIPE,
        SCOPE_APPROVAL_HOLD,
        EVIDENCE_HOLD,
        POLICY_HOLD,
        REJECT
    }

    public record Row(
            int order,
            String workId,
            String sourceKind,
            int release,
            int jep,
            String programmeState,
            State state,
            String packet,
            M3EditScope scope,
            String dagRoot,
            String nextAction,
            boolean schedulerProjection,
            boolean productWriteAuthority,
            boolean promotionAuthority) {
        public Row {
            if (order < 0 || release < 0 || jep < 0) throw new IllegalArgumentException("identity");
            workId = required(workId, "workId");
            sourceKind = required(sourceKind, "sourceKind");
            if ("JEP".equals(sourceKind) && jep <= 0) {
                throw new IllegalArgumentException("JEP numeric identity");
            }
            programmeState = required(programmeState, "programmeState");
            state = Objects.requireNonNull(state, "state");
            packet = Objects.toString(packet, "");
            scope = Objects.requireNonNull(scope, "scope");
            dagRoot = Objects.toString(dagRoot, "");
            nextAction = required(nextAction, "nextAction");
            if (schedulerProjection != (state == State.RUNNABLE)) {
                throw new IllegalArgumentException("schedulerProjection");
            }
            if (schedulerProjection && !dagRoot.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("dagRoot");
            }
            if (!schedulerProjection && !dagRoot.isEmpty()) {
                throw new IllegalArgumentException("held row cannot carry dagRoot");
            }
            if (productWriteAuthority || promotionAuthority) {
                throw new IllegalArgumentException("planner grants no mutation/promotion authority");
            }
        }
    }

    public record Plan(List<Row> rows, String root) {
        public Plan {
            rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
            if (rows.isEmpty()) throw new IllegalArgumentException("empty plan");
            if (!root.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("root");
            for (int index = 0; index < rows.size(); index++) {
                if (rows.get(index).order() != index) throw new IllegalArgumentException("order");
            }
        }
    }

    private M3BackportProgrammeExecutionPlan() {}

    public static Plan compile(
            String programmeTsv,
            String packetIndexTsv,
            Map<String, String> packetContents) {
        List<Map<String, String>> programme = tsv(programmeTsv);
        Map<String, String> packetIndex = packetIndex(packetIndexTsv);
        Map<String, String> packets = Map.copyOf(Objects.requireNonNull(packetContents, "packetContents"));

        ArrayList<Row> rows = new ArrayList<>();
        for (Map<String, String> source : programme) {
            int jep = optionalInteger(source, "jep", 0);
            int release = integer(source, "release");
            String workId =
                    optional(source, "work_id").isEmpty()
                            ? "JEP-" + jep
                            : optional(source, "work_id");
            String sourceKind =
                    optional(source, "source_kind").isEmpty()
                            ? "JEP"
                            : optional(source, "source_kind");
            String programmeState = cell(source, "programme_state");
            String nextAction = cell(source, "next_action");
            String packetName = packetIndex.getOrDefault(workId, "");
            M3BackportPacket packet = null;
            if (!packetName.isEmpty()) {
                String packetText = packets.get(packetName);
                if (packetText == null) {
                    throw new IllegalArgumentException("packet index references missing packet: " + packetName);
                }
                packet = M3BackportPacketLoader.parse(packetText);
            }

            State state;
            String dagRoot = "";
            M3EditScope scope = packet == null ? M3EditScope.FILE : packet.maximumScope();
            boolean projection = false;

            switch (programmeState) {
                case "FILE_READY" -> {
                    if (packet == null) throw new IllegalArgumentException("FILE_READY without packet: " + workId);
                    if (packet.maximumScope() != M3EditScope.FILE) {
                        throw new IllegalArgumentException("FILE_READY packet escaped FILE: " + workId);
                    }
                    M3RecipeDag dag = M3PacketDagComposer.compose(packet);
                    dagRoot = M3DagSemanticRoot.of(dag);
                    state = State.RUNNABLE;
                    projection = true;
                }
                case "SCOPE_READY" -> {
                    if (packet == null) throw new IllegalArgumentException("SCOPE_READY without packet: " + workId);
                    boolean approved = packet.atoms().stream()
                            .filter(atom -> atom.scope() != M3EditScope.FILE)
                            .allMatch(M3RecipeAtom::scopePromotionApproved);
                    if (approved) {
                        M3RecipeDag dag = M3PacketDagComposer.compose(packet);
                        dagRoot = M3DagSemanticRoot.of(dag);
                        state = State.RUNNABLE;
                        projection = true;
                    } else {
                        state = State.SCOPE_APPROVAL_HOLD;
                        nextAction = "APPROVE_SCOPE_PROMOTION_THEN_REPLAN";
                    }
                }
                case "AUTHOR_RECIPE" -> state = State.AUTHOR_RECIPE;
                case "EVIDENCE_HOLD", "SCOPE_HOLD" -> state = State.EVIDENCE_HOLD;
                case "HOLD" -> state = State.POLICY_HOLD;
                case "REJECT" -> state = State.REJECT;
                default -> throw new IllegalArgumentException("unknown programme state: " + programmeState);
            }

            rows.add(new Row(
                    rows.size(), workId, sourceKind, release, jep, programmeState, state, packetName, scope,
                    dagRoot, nextAction, projection, false, false));
        }
        return new Plan(rows, root(rows));
    }

    public static String render(Plan plan) {
        StringBuilder out = new StringBuilder(
                "order\twork_id\tsource_kind\trelease\tjep\tprogramme_state\tplan_state\tpacket\tscope\t"
                        + "dag_root\tnext_action\tscheduler_projection\tproduct_write_authority\tpromotion_authority\n");
        for (Row row : plan.rows()) {
            out.append(row.order()).append('\t')
                    .append(row.workId()).append('\t')
                    .append(row.sourceKind()).append('\t')
                    .append(row.release()).append('\t')
                    .append(row.jep()).append('\t')
                    .append(row.programmeState()).append('\t')
                    .append(row.state()).append('\t')
                    .append(row.packet()).append('\t')
                    .append(row.scope()).append('\t')
                    .append(row.dagRoot()).append('\t')
                    .append(clean(row.nextAction())).append('\t')
                    .append(row.schedulerProjection()).append("\tfalse\tfalse\n");
        }
        out.append("# plan_root\t").append(plan.root()).append('\n');
        return out.toString();
    }

    private static Map<String, String> packetIndex(String tsv) {
        List<Map<String, String>> rows = tsv(tsv);
        LinkedHashMap<String, String> index = new LinkedHashMap<>();
        for (Map<String, String> row : rows) {
            String work = cell(row, "work_id");
            String packet = cell(row, "packet");
            if (index.putIfAbsent(work, packet) != null) {
                throw new IllegalArgumentException("duplicate packet index work item: " + work);
            }
        }
        return Map.copyOf(index);
    }

    private static List<Map<String, String>> tsv(String text) {
        List<String> lines = Objects.requireNonNull(text, "text").lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .toList();
        if (lines.size() < 2) throw new IllegalArgumentException("empty TSV");
        String[] header = lines.getFirst().split("\t", -1);
        ArrayList<Map<String, String>> rows = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] cells = line.split("\t", -1);
            if (cells.length != header.length) throw new IllegalArgumentException("TSV width");
            HashMap<String, String> row = new HashMap<>();
            for (int i = 0; i < header.length; i++) row.put(header[i], cells[i]);
            rows.add(Map.copyOf(row));
        }
        return List.copyOf(rows);
    }

    private static String root(List<Row> rows) {
        MessageDigest digest = sha256();
        update(digest, SCHEMA);
        for (Row row : rows) {
            update(digest, Integer.toString(row.order()));
            update(digest, row.workId());
            update(digest, row.sourceKind());
            update(digest, row.programmeState());
            update(digest, row.state().name());
            update(digest, row.packet());
            update(digest, row.scope().name());
            update(digest, row.dagRoot());
            update(digest, row.nextAction());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    private static int integer(Map<String, String> row, String name) {
        return Integer.parseInt(cell(row, name));
    }

    private static int optionalInteger(
            Map<String, String> row, String name, int fallback) {
        String value = optional(row, name);
        return value.isEmpty() ? fallback : Integer.parseInt(value);
    }

    private static String optional(Map<String, String> row, String name) {
        return Objects.toString(row.get(name), "").strip();
    }

    private static String cell(Map<String, String> row, String name) {
        return required(row.get(name), name);
    }

    private static String required(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String clean(String value) {
        return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
