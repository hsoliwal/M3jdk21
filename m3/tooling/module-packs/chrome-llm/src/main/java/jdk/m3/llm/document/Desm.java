/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * Adapted for M3JDK21 from synexia-flldcim/tools/m3-text-pipeline/src/main/java/com/synexia/flldcim/tools/Desm.java
 * Synexia revision: 2ab439480ecb0643615e85b42def954dbf3b5627.
 * Namespace mapping preserves algorithms and original schema/hash identities.
 */
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.document;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Descriptive skeleton, retaining exact source ranges; never promotes extracted prose to truth. */
public final class Desm {
    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern LIST = Pattern.compile("^( *)(?:[-+*]|[0-9]+[.)])\\s+(.+)$");
    private static final Pattern CONSTRAINT = Pattern.compile("\\b(MUST|SHALL|NEVER)\\b");
    private static final Pattern OBLIGATION = Pattern.compile("(?i)\\b(TODO|FIXME|pending)\\b");
    private static final java.util.Map<String, Pattern> FACETS = java.util.Map.of(
            "CAUSAL_CANDIDATE", Pattern.compile("(?i)\\b(because|due to)\\b"),
            "EFFECT_CANDIDATE", Pattern.compile("(?i)\\b(therefore|result)\\b"),
            "DECISION_CANDIDATE", Pattern.compile("(?i)\\b(decision|chosen|use)\\b"),
            "FAILURE_CANDIDATE", Pattern.compile("(?i)\\b(failed|error|cannot)\\b"),
            "PROVENANCE_CANDIDATE", Pattern.compile("https?://|\\]\\("),
            "IDENTIFIER_CANDIDATE", Pattern.compile("[0-9a-fA-F]{40,64}|[A-Za-z0-9_]+/[A-Za-z0-9_./-]+"),
            "TEMPORAL_CANDIDATE", Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}"));
    private record Level(int depth, String id) { }
    private Desm() { }
    public static List<TreeNode> extract(String path, String source) {
        List<TreeNode> out = new ArrayList<>();
        var headings = new ArrayDeque<Level>(); var lists = new ArrayDeque<Level>();
        headings.push(new Level(0, path));
        int offset = 0, fenceLength = 0; char fenceChar = 0; String fenceId = "";
        for (String line : source.split("(?<=\\n)", -1)) {
            if (line.isEmpty()) continue;
            String text = line.stripTrailing(), trimmed = text.stripLeading();
            String id = path + "#desm/" + out.size(), parent = headings.peek().id();
            String kind = "DESCRIPTION_CANDIDATE";
            int marker = 0;
            if (!trimmed.isEmpty() && (trimmed.charAt(0) == '`' || trimmed.charAt(0) == '~'))
                while (marker < trimmed.length() && trimmed.charAt(marker) == trimmed.charAt(0)) marker++;
            if (fenceLength > 0) {
                parent = fenceId; kind = "EMBEDDED_SOURCE";
                if (marker >= fenceLength && trimmed.charAt(0) == fenceChar
                        && trimmed.substring(marker).isBlank()) { kind = "FENCE_CLOSE"; fenceLength = 0; }
            } else if (marker >= 3) {
                kind = "FENCE_OPEN"; fenceLength = marker; fenceChar = trimmed.charAt(0); fenceId = id;
            } else {
                var heading = HEADING.matcher(text); var list = LIST.matcher(text);
                if (heading.matches()) {
                    int level = heading.group(1).length();
                    while (headings.peek().depth() >= level) headings.pop();
                    parent = headings.peek().id(); headings.push(new Level(level, id)); lists.clear();
                    kind = "HEADING";
                } else if (list.matches()) {
                    int indent = list.group(1).length();
                    while (!lists.isEmpty() && lists.peek().depth() >= indent) lists.pop();
                    parent = lists.isEmpty() ? headings.peek().id() : lists.peek().id();
                    lists.push(new Level(indent, id)); kind = "LIST_ITEM";
                } else if (!text.isBlank()) lists.clear();
                if (CONSTRAINT.matcher(text).find()) kind += ":CONSTRAINT_CANDIDATE";
                if (OBLIGATION.matcher(text).find()) kind += ":OBLIGATION";
                for (String facet : FACETS.keySet().stream().sorted().toList())
                    if (FACETS.get(facet).matcher(text).find()) kind += ":" + facet;
            }
            out.add(new TreeNode(id, parent, kind, offset, offset + line.length(), text, Hashes.sha(line)));
            offset += line.length();
        }
        if (fenceLength > 0) out.add(new TreeNode(path + "#desm/residue", path,
                "TYPED_RESIDUE", source.length(), source.length(), "UNCLOSED_FENCE", Hashes.sha("UNCLOSED_FENCE")));
        return List.copyOf(out);
    }
}
