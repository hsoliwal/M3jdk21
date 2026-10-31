/* SPDX-License-Identifier: Apache-2.0 */
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jdk.m3.llm.summary.Data;
import jdk.m3.llm.summary.Summary;

/** Compares complete score tables and roots with the frozen independent Python donor. */
public final class SummaryChecks {
    private static int checks;
    private SummaryChecks() {}
    private static void yes(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void rejects(Runnable action, String message) {
        boolean refused = false;
        try { action.run(); } catch (IllegalArgumentException expected) { refused = true; }
        yes(refused, message);
    }
    public static void main(String[] arguments) throws Exception {
        Map<String, Object> fixture = Data.map(Data.read(Path.of(arguments[0])));
        List<Map<String, Object>> cases = Data.rows(fixture.get("cases"));
        for (Map<String, Object> entry : cases) {
            String name = Data.s(entry, "name");
            List<Map<String, Object>> records = Data.rows(entry.get("records"));
            String before = Data.json(records);
            Map<String, Object> output = summarize(entry, records);
            String actual = Data.json(output);
            yes(actual.equals(Data.json(entry.get("expected"))), name + ": full donor output differs");
            yes(Summary.cited(output).equals(Data.s(entry, "cited")), name + ": cited source text differs");
            yes(before.equals(Data.json(records)), name + ": source records mutated");
            yes(actual.equals(Data.json(summarize(entry, records))), name + ": replay differs");

            List<Map<String, Object>> sentences = Data.rows(output.get("sentences"));
            Map<String, String> byId = new HashMap<>();
            for (Map<String, Object> sentence : sentences) {
                String id = Data.s(sentence, "evidence_id");
                yes(byId.put(id, Data.s(sentence, "text")) == null, name + ": duplicate evidence identity");
            }
            for (String id : Data.strings(output.get("evidence_ids")))
                yes(byId.containsKey(id), name + ": invented evidence reference");
            for (Map<String, Object> node : Data.rows(output.get("nodes"))) {
                Set<String> methods = Data.map(node.get("method_scores")).keySet();
                yes(methods.equals(Set.of("luhn", "sumbasic", "edmundson", "lexrank", "query")),
                        name + ": missing ranking strategy");
                for (String id : Data.strings(node.get("evidence_ids")))
                    yes(byId.containsKey(id), name + ": node has unknown evidence");
            }
        }
        List<Map<String, Object>> records = Data.rows(cases.getFirst().get("records"));
        rejects(() -> Summary.hierarchical(records, "", 255, 1, 1, 2), "chunk budget refused");
        rejects(() -> Summary.hierarchical(records, "", 256, 0, 1, 2), "leaf budget refused");
        rejects(() -> Summary.hierarchical(records, "", 256, 1, 0, 2), "parent budget refused");
        rejects(() -> Summary.hierarchical(records, "", 256, 1, 1, 1), "nonconvergent fan-in refused");
        rejects(() -> Summary.splitSentences(List.of()), "empty evidence refused");
        List<Map<String, Object>> excessive = new ArrayList<>();
        for (int i = 0; i < 4097; i++) excessive.add(Map.of());
        rejects(() -> Summary.group(excessive, "", 1, 0, 0, List.of()), "quadratic rank budget refused");
        yes(Summary.tokens("Straße STRASSE ẞ").equals(List.of("strasse", "strasse")),
                "full Unicode casefold table retained");
        System.out.println("SUMMARY_DONOR_PARITY_PASS cases=" + cases.size() + " checks=" + checks);
    }
    private static Map<String, Object> summarize(Map<String, Object> entry,
                                               List<Map<String, Object>> records) {
        return Summary.hierarchical(records, Data.s(entry, "query"),
                Data.n(entry, "chunk_characters", 4000), Data.n(entry, "leaf_sentences", 5),
                Data.n(entry, "parent_sentences", 7), Data.n(entry, "fan_in", 8));
    }
}
