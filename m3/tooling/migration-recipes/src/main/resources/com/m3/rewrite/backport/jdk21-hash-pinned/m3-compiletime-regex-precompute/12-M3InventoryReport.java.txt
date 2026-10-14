// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/** Writes the exact per-corpus summary and the distinct constant-regex corpus. */
final class M3InventoryReport {
    private M3InventoryReport() {}

    static void write(M3InventoryState state, Path out) throws IOException {
        Map<String, Long> all = new TreeMap<>();
        state.totals.get(0).forEach((k, v) -> all.merge(k, v, Long::sum));
        state.totals.get(1).forEach((k, v) -> all.merge(k, v, Long::sum));
        try (BufferedWriter w = Files.newBufferedWriter(out.resolve("COMPILETIME_STRING_SUMMARY.tsv"))) {
            w.write("corpus\tcategory\tmetric\tvalue\n");
            for (int c = 0; c < 2; c++) {
                Map<String, Long> m = new TreeMap<>(state.totals.get(c));
                dedup(m, "a", "ldc", state.ldc, c);
                dedup(m, "d", "concat_recipe", state.concatRecipes, c);
                dedup(m, "c", "format_string", state.formats, c);
                regex(m, state, c);
                rows(w, M3InventoryState.CORPORA[c], m);
            }
            dedup(all, "a", "ldc", state.ldc, 2);
            dedup(all, "d", "concat_recipe", state.concatRecipes, 2);
            dedup(all, "c", "format_string", state.formats, 2);
            regex(all, state, 2);
            all.put("a\tldc_distinct_strings_in_both_corpora", state.ldc.values().stream()
                    .filter(s -> s.classes[0] > 0 && s.classes[1] > 0).count());
            rows(w, "all", all);
        }
        regexCorpus(state, out);
    }

    private static void rows(BufferedWriter w, String corpus, Map<String, Long> m) throws IOException {
        for (Map.Entry<String, Long> e : m.entrySet()) {
            w.write(corpus + '\t' + e.getKey() + '\t' + e.getValue() + '\n');
        }
    }

    /** corpus 2 = union of both corpora. */
    private static void dedup(Map<String, Long> m, String cat, String name,
            Map<String, M3InventoryState.Seen> table, int corpus) {
        long distinct = 0;
        long multi = 0;
        long pairs = 0;
        long redundantUnits = 0;
        long distinctUnits = 0;
        for (Map.Entry<String, M3InventoryState.Seen> e : table.entrySet()) {
            int classes = corpus == 2 ? e.getValue().classes[0] + e.getValue().classes[1]
                    : e.getValue().classes[corpus];
            if (classes == 0) {
                continue;
            }
            distinct++;
            pairs += classes;
            distinctUnits += e.getKey().length();
            if (classes > 1) {
                multi++;
                redundantUnits += (long) (classes - 1) * e.getKey().length();
            }
        }
        m.put(cat + '\t' + name + "_distinct", distinct);
        m.put(cat + '\t' + name + "_distinct_units", distinctUnits);
        m.put(cat + '\t' + name + "_in_2plus_classes", multi);
        m.put(cat + '\t' + name + "_class_pairs", pairs);
        m.put(cat + '\t' + name + "_redundant_class_copies", pairs - distinct);
        m.put(cat + '\t' + name + "_redundant_class_copy_units", redundantUnits);
    }

    private static void regex(Map<String, Long> m, M3InventoryState state, int corpus) {
        for (M3InventoryState.RegexKey key : state.regex.values()) {
            int sites = corpus == 2 ? key.sites[0] + key.sites[1] : key.sites[corpus];
            if (sites == 0) {
                continue;
            }
            m.merge("b\tregex_distinct_keys", 1L, Long::sum);
            m.merge("b\tregex_distinct_keys_" + key.analysis.kind(), 1L, Long::sum);
            if (key.shapeLiteral) {
                m.merge("b\tregex_distinct_keys_shape_literal", 1L, Long::sum);
            }
            if (key.analysis.matcherStateAdmitted()) {
                m.merge("b\tregex_distinct_keys_matcher_state_admitted", 1L, Long::sum);
            }
        }
    }

    private static void regexCorpus(M3InventoryState state, Path out) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(out.resolve("CONSTANT_REGEX_CORPUS.tsv"))) {
            w.write("pattern\tflags\tkind\tmatcher_state_admitted\tshape_literal\tatoms\tjdk21_sites"
                    + "\tvendor_sites\tapis\tunit_count\tfirst_unit\treason\n");
            for (M3InventoryState.RegexKey k : state.regex.values()) {
                w.write(M3Tsv.escape(k.pattern) + '\t' + k.flags + '\t' + k.analysis.kind() + '\t'
                        + k.analysis.matcherStateAdmitted() + '\t' + k.shapeLiteral + '\t'
                        + k.analysis.requirement().atomCount() + '\t' + k.sites[0] + '\t' + k.sites[1]
                        + '\t' + String.join(",", k.apis) + '\t' + k.units.size() + '\t'
                        + k.units.first() + '\t' + M3Tsv.escape(k.analysis.reason()) + '\n');
            }
        }
    }
}
