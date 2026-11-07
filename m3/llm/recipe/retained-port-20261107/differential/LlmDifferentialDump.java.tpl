// SPDX-License-Identifier: Apache-2.0
// Differential driver of recipe m3/llm/recipe/retained-port-20261107: the same source is compiled once against the
// donor classes (package __PKG__ = com.m3.llm at 58bc0d2d297e) and once against the restored classes (jdk.m3.llm);
// both dumps must be byte-identical except the declared adaptation (the capabilities schema id).
package __PKG__.differential;

import __PKG__.chrome.ChromeMlTaskService;
import __PKG__.evidence.SourceAnchoredSummary;
import __PKG__.extract.ExtractiveSummarizer;
import __PKG__.internal.CanonicalJson;
import __PKG__.internal.LuhnSummarizer;
import __PKG__.internal.PlanHash;
import __PKG__.internal.Sha256;
import __PKG__.projection.Bm25Index;
import __PKG__.projection.CellKey;
import __PKG__.projection.ExtractiveConsolidator;
import __PKG__.projection.InventorySnapshot;
import __PKG__.projection.TextInventoryCell;
import __PKG__.summary.LuhnSummary;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

public final class LlmDifferentialDump {
    private static final long SEED = 20261107L;
    private static final int DOCUMENTS = 300;
    private static final int QUERIES = 60;
    private static final String[] WORDS = {
        "alpha", "evidence", "stays", "exact", "source", "summary", "chrome", "runtime", "bounded", "capture",
        "ledger", "projection", "budget", "passage", "digest", "snapshot", "candidate", "authority", "refusal",
        "token", "sentence", "frequency", "order", "retained", "omitted", "mandatory", "version", "search", "index",
        "receipt", "coverage", "segment", "plan", "hash", "canonical", "json", "grammar", "unicode", "surrogate",
        "naïve", "हिन्दी", "😀", "Zürich", "coöperate", "lane", "atom", "pattern", "recipe", "donor", "restore",
        "module", "package", "export", "requires", "process", "stream", "cancel", "deadline", "call", "model",
        "proposal", "consolidate", "combine", "cell", "key", "family", "qualifier", "namespace", "row", "entropy",
        "value", "bloom", "hint", "luhn", "extractive", "mixed", "mode", "browser", "tree", "strict",
    };
    private static final String[] ENDS = {".", ".", ".", "!", "?"};

    private LlmDifferentialDump() { }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        Random random = new Random(SEED);
        List<String> documents = new ArrayList<>();
        for (int i = 0; i < DOCUMENTS; i++) documents.add(document(random));
        try (PrintStream dump = new PrintStream(Files.newOutputStream(out), false, StandardCharsets.UTF_8)) {
            summaries(dump, documents, random);
            json(dump, documents);
            projection(dump, documents, random);
            tasks(dump, documents);
            dump.println("capabilities " + CanonicalJson.string(new TreeMap<>(ChromeMlTaskService.capabilities())));
        }
    }

    private static String document(Random random) {
        StringBuilder text = new StringBuilder();
        int sentences = 3 + random.nextInt(10);
        for (int s = 0; s < sentences; s++) {
            int words = 4 + random.nextInt(11);
            for (int w = 0; w < words; w++) {
                String word = WORDS[random.nextInt(WORDS.length)];
                if (w == 0) word = Character.toUpperCase(word.charAt(0)) + word.substring(1);
                text.append(w == 0 ? "" : " ").append(word);
            }
            text.append(ENDS[random.nextInt(ENDS.length)]);
            text.append(random.nextInt(7) == 0 ? "\n" : " ");
        }
        return text.toString().strip();
    }

    private static void summaries(PrintStream dump, List<String> documents, Random random) {
        ExtractiveSummarizer extractive = new ExtractiveSummarizer();
        for (int i = 0; i < documents.size(); i++) {
            String doc = documents.get(i);
            int limit = 1 + random.nextInt(4);
            byte[] utf8 = doc.getBytes(StandardCharsets.UTF_8);
            dump.println("doc " + i + " sha256 " + Sha256.digest(utf8) + " plan " + PlanHash.sha256(doc) + " limit " + limit);
            dump.println("extractive " + i + " " + extractive.summarize(doc, limit).replace("\n", "\\n"));
            dump.println("luhnSummary " + i + " " + LuhnSummary.luhnSummary(doc, limit).replace("\n", "\\n"));
            dump.println("luhnSummarizer " + i + " " + LuhnSummarizer.summarize(doc, limit));
            try {
                SourceAnchoredSummary anchored = SourceAnchoredSummary.summarize(doc, limit);
                dump.println("anchored " + i + " root " + anchored.root() + " spans " + anchored.spans().size()
                        + " render " + anchored.render(doc).replace("\n", "\\n"));
            } catch (RuntimeException failure) {
                dump.println("anchored " + i + " " + failure.getClass().getName() + ": " + failure.getMessage());
            }
        }
    }

    private static void json(PrintStream dump, List<String> documents) {
        for (int i = 0; i < documents.size(); i += 7) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("index", i);
            value.put("text", documents.get(i));
            value.put("ratio", i / 7.0);
            value.put("flags", List.of(true, false, i % 3 == 0));
            value.put("nested", Map.of("sha", Sha256.digest(documents.get(i).getBytes(StandardCharsets.UTF_8))));
            String once = CanonicalJson.string(value);
            String twice = CanonicalJson.string(CanonicalJson.parse(once));
            dump.println("json " + i + " " + once);
            dump.println("json-roundtrip " + i + " " + once.equals(twice) + " bytes " + CanonicalJson.bytes(value).length);
        }
    }

    private static void projection(PrintStream dump, List<String> documents, Random random) {
        List<TextInventoryCell> cells = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            cells.add(new TextInventoryCell(new CellKey("ns", "row" + i, "fam", "q", i % 5), documents.get(i)));
        }
        InventorySnapshot snapshot = new InventorySnapshot(cells);
        dump.println("snapshot root " + snapshot.root() + " size " + snapshot.size());
        Bm25Index index = new Bm25Index(snapshot, "ns", "fam", "q", 4);
        dump.println("index root " + index.sourceRoot() + " documents " + index.documentCount());
        for (int q = 0; q < QUERIES; q++) {
            String query = WORDS[random.nextInt(WORDS.length)] + " " + WORDS[random.nextInt(WORDS.length)]
                    + (random.nextBoolean() ? " " + WORDS[random.nextInt(WORDS.length)] : "");
            List<Bm25Index.Hit> hits = index.search(query, 10);
            StringBuilder line = new StringBuilder("query " + q + " [" + query + "]");
            for (Bm25Index.Hit hit : hits) line.append(' ').append(hit.cell().key().row()).append('=').append(hit.score());
            dump.println(line);
            int budget = 300 + random.nextInt(2500);
            Set<CellKey> mandatory = hits.isEmpty() ? Set.of() : Set.of(hits.get(hits.size() - 1).cell().key());
            try {
                ExtractiveConsolidator.Projection projection = ExtractiveConsolidator.consolidate(hits, budget, mandatory);
                dump.println("projection " + q + " budget " + budget + " bytes " + projection.utf8Bytes() + " root "
                        + projection.root() + " selected " + rows(projection.selected()) + " omitted "
                        + rows(projection.omitted()) + " text " + Sha256.digest(projection.text().getBytes(StandardCharsets.UTF_8)));
            } catch (RuntimeException failure) {
                dump.println("projection " + q + " " + failure.getClass().getName() + ": " + failure.getMessage());
            }
        }
    }

    private static List<String> rows(List<TextInventoryCell> cells) {
        return cells.stream().map(cell -> cell.key().row()).toList();
    }

    private static void tasks(PrintStream dump, List<String> documents) {
        ChromeMlTaskService.Kind[] kinds = ChromeMlTaskService.Kind.values();
        for (int i = 0; i < documents.size(); i += 5) {
            String source = documents.get(i).repeat(1 + (i % 4) * 7);
            ChromeMlTaskService.Kind kind = kinds[i % kinds.length];
            try {
                ChromeMlTaskService.Request request = new ChromeMlTaskService.Request(kind, source, "en", 8, 256, Duration.ofSeconds(30));
                List<ChromeMlTaskService.Segment> segments = ChromeMlTaskService.plan(request);
                dump.println("plan " + i + " " + kind + " bytes " + source.getBytes(StandardCharsets.UTF_8).length + " segments " + segments);
            } catch (RuntimeException failure) {
                dump.println("plan " + i + " " + kind + " " + failure.getClass().getName() + ": " + failure.getMessage());
            }
        }
    }
}
