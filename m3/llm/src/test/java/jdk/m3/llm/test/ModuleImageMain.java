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
import jdk.m3.llm.model.M3ModelInput;
import jdk.m3.llm.model.M3NcmCodes;
import jdk.m3.llm.model.M3NcmQuantizer;
import jdk.m3.llm.model.M3SystemOneSchema;
import jdk.m3.llm.model.M3SystemOneRequest;
import java.lang.module.ModuleDescriptor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
        for (String name : List.of("chrome", "chrome.aix", "evidence", "extract", "summary", "projection", "model"))
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
        String dependencyRoot = "a".repeat(64);
        String exactModelSource = "Source \ud800 remains exact";
        M3ModelInput modelInput = M3ModelInput.of(exactModelSource, dependencyRoot,
                dependencyRoot, dependencyRoot, dependencyRoot);
        require(modelInput.text() == exactModelSource, "ordinary immutable String is retained");
        require(M3ModelInput.class.getModule() == module, "model input has product module owner");
        M3NcmQuantizer quantizer = M3NcmQuantizer.of(2, 1, new int[] {1, 2},
                new int[] {2, 2}, new float[] {0, 2, 0, 1}, M3NcmQuantizer.Backend.JAVA);
        M3NcmCodes codes = quantizer.encode(modelInput, new float[] {2, 3});
        require(codes.matches(modelInput, quantizer.codebookRoot()), "version-qualified code binding");
        require(java.util.Arrays.equals(new float[] {2, 3}, quantizer.reconstruct(codes)),
                "residual quantization primitive in named module");
        byte[] checkpoint = quantizer.checkpointBytes(4096);
        M3NcmQuantizer restored = M3NcmQuantizer.fromCheckpoint(checkpoint,
                quantizer.codebookRoot(), 4096, M3NcmQuantizer.Backend.JAVA);
        require(java.util.Arrays.equals(checkpoint, restored.checkpointBytes(4096)),
                "canonical checkpoint bytes survive restore in named module");
        require(codes.equals(restored.encode(modelInput, new float[] {2, 3})),
                "checkpoint restore preserves numerical code identity");
        require(java.util.Arrays.equals(new float[] {2, 2}, restored.nextLevelInput(codes, 1)),
                "completed concepts supply the next level input");
        require(java.util.Arrays.equals(new float[] {2, 2}, restored.nextConceptInputs(codes)),
                "next-concept concatenation in named module");
        M3SystemOneSchema schema = M3SystemOneSchema.compile(List.of(
                new M3SystemOneSchema.NoulQuestion("supported", null, "source supports", "source does not support")));
        M3ModelInput prepared = new M3SystemOneRequest("caller-model", exactModelSource, schema)
                .prepare(dependencyRoot, dependencyRoot, dependencyRoot);
        require(prepared.text() == exactModelSource && schema.rootHash().equals(prepared.namespace().schemaRoot()),
                "typed request prepares exact ordinary String input");
        require(M3SystemOneSchema.class.getModule() == module, "typed schema has product module owner");
        require(args.length <= 1, "optional single explicit NCM native library path");
        boolean nativeExecuted = args.length == 1;
        if (nativeExecuted) {
            M3NcmQuantizer.loadNative(Path.of(args[0]));
            M3NcmQuantizer nativeQuantizer = M3NcmQuantizer.fromCheckpoint(checkpoint,
                    quantizer.codebookRoot(), 4096, M3NcmQuantizer.Backend.NATIVE);
            require(codes.equals(nativeQuantizer.encode(modelInput, new float[] {2, 3})),
                    "JNI numerical result agrees in named module");
        }
        System.out.println("M3_LLM_IMAGE_PASS module=jdk.m3.llm thirdPartyRuntimeDependencies=0 modelCalls=0"
                + " ncmPrimitive=true checkpoint=true nextConceptInput=true systemOneStructure=true ncmNative=" + nativeExecuted);
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
