// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.test;

import jdk.m3.llm.chrome.ChromeMlRunner;
import jdk.m3.llm.chrome.ChromeMlRunnerResult;
import jdk.m3.llm.chrome.ChromeMlTaskService;
import jdk.m3.llm.evidence.SourceAnchoredSummary;
import jdk.m3.llm.extract.ExtractiveSummarizer;
import jdk.m3.llm.projection.Bm25Index;
import jdk.m3.llm.projection.CellKey;
import jdk.m3.llm.projection.ExtractiveConsolidator;
import jdk.m3.llm.projection.TextInventoryCell;
import jdk.m3.llm.summary.LuhnSummary;
import java.lang.module.ModuleDescriptor;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;

/** Run with the received module resolved in an actual linked image, without a test library. */
public final class ModuleImageMain {
    private ModuleImageMain() { }

    public static void main(String[] args) throws Exception {
        Module module = SourceAnchoredSummary.class.getModule();
        require(module.isNamed() && "jdk.m3.llm".equals(module.getName()), "named product module");
        require(module == ChromeMlRunner.class.getModule(), "single product owner");
        require(!module.isExported("jdk.m3.llm.internal"), "internal helpers are not exported");
        require(!module.isExported("jdk.m3.llm.internal.json"), "private task JSON is not exported");
        for (String name : List.of("chrome", "chrome.aix", "evidence", "extract", "summary", "projection"))
            require(module.isExported("jdk.m3.llm." + name), "public package is exported: " + name);
        Set<String> requires = module.getDescriptor().requires().stream()
                .map(ModuleDescriptor.Requires::name).collect(java.util.stream.Collectors.toSet());
        require(requires.equals(Set.of("java.base", "jdk.httpserver")), "JDK-only runtime closure");
        require(module.getDescriptor().packages().stream().noneMatch(p -> p.startsWith("com.synexia")
                || p.startsWith("com.dictlang")), "target package ownership");
        String source = "Alpha evidence stays exact. हिन्दी 😀 remains available. Alpha repeats.";
        SourceAnchoredSummary summary = SourceAnchoredSummary.summarize(source, 2);
        require(!summary.render(source).isBlank(), "anchored CPU summary");
        require(summary.spans().size() <= 2, "sentence budget");
        for (SourceAnchoredSummary.Span span : summary.spans()) {
            require(source.substring(span.startInclusive(), span.endExclusive()).equals(span.text()), "exact source span");
        }
        boolean refused = false;
        try { summary.render(source + " modified"); } catch (RuntimeException expected) { refused = true; }
        require(refused, "changed source is refused");
        require(LuhnSummary.luhnSummary("", 1).isEmpty(), "empty Luhn projection");
        require(!LuhnSummary.luhnSummary(source, 1).isBlank(), "Luhn projection");
        require(!new ExtractiveSummarizer().summarize(source, 1).isBlank(), "frequency projection");
        CellKey key = new CellKey("image", "row", "source", "text", 1);
        TextInventoryCell cell = new TextInventoryCell(key, source);
        var projection = ExtractiveConsolidator.consolidate(List.of(new Bm25Index.Hit(cell, 1.0)),
                source.getBytes(StandardCharsets.UTF_8).length, Set.of(key));
        require(source.equals(projection.text()) && projection.omitted().isEmpty(), "named-module exact byte projection");
        ChromeMlTaskService tasks = new ChromeMlTaskService((request, sink) ->
                new ChromeMlRunnerResult("scripted candidate", 1, "", ""));
        var result = tasks.execute(new ChromeMlTaskService.Request(ChromeMlTaskService.Kind.SUMMARY,
                "x".repeat(9000), "", 3, 128, Duration.ofSeconds(5)));
        require(result.coverage().size() == 2 && result.receipts().size() == 3,
                "named-module task map/reduce with explicit scripted runner");
        System.out.println("M3_LLM_IMAGE_PASS module=jdk.m3.llm thirdPartyRuntimeDependencies=0 modelCalls=0");
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
