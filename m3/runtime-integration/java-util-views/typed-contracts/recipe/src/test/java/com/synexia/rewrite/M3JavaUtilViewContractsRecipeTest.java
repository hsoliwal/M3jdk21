// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.config.*;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3JavaUtilViewContractsRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/java-util-view-contracts/";
    private static final String DECOY = "class Decoy { String text = \"CollectionViews count++; SORTED\"; }\n";
    private static String resource(String path) throws IOException {
        try (var in = M3JavaUtilViewContractsRecipeTest.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("missing resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static List<String[]> rows() throws IOException {
        return resource(ROOT + "manifest.tsv").lines().map(s -> s.split("\t", -1)).toList();
    }
    private static Map<String,String> before() throws IOException {
        Map<String,String> image = new LinkedHashMap<>();
        for (String[] row : rows()) if (!row[1].equals("ABSENT")) {
            String text = resource(ROOT + "before/" + row[0]);
            assertEquals(row[1], M3GpuToolJoinManifest.sha256(text)); image.put(row[0], text);
        }
        image.put("unrelated/Decoy.java", DECOY); return image;
    }
    private static List<SourceFile> files(Map<String,String> image) {
        List<SourceFile> files = new ArrayList<>();
        image.forEach((p,t) -> files.add(PlainText.builder().sourcePath(Path.of(p)).text(t).build())); return files;
    }
    private static Recipe recipe() throws IOException {
        String path = "/META-INF/rewrite/m3-java-util-view-contracts.yml";
        try (var in = M3JavaUtilViewContractsRecipeTest.class.getResourceAsStream(path)) {
            return Environment.builder().load(new YamlResourceLoader(in,URI.create("classpath:"+path),
                    new Properties(),M3JavaUtilViewContractsRecipeTest.class.getClassLoader()))
                    .build().activateRecipes("com.synexia.m3.JavaUtilViewContracts");
        }
    }
    private static List<Result> sdk(List<SourceFile> files) throws IOException {
        List<Throwable> errors = new ArrayList<>();
        var run = recipe().run(new InMemoryLargeSourceSet(files), new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), () -> "SDK errors prohibit promotion: " + errors);
        return run.getChangeset().getAllResults();
    }
    private static Map<String,String> apply(Map<String,String> before,List<Result> results) {
        Map<String,String> after = new TreeMap<>(before);
        for (Result result : results) { SourceFile file = result.getAfter(); assertNotNull(file);
            after.put(file.getSourcePath().toString(),file.printAll()); }
        return after;
    }
    private static void expected(Map<String,String> after) throws IOException {
        for (String[] row : rows()) assertEquals(row[2],M3GpuToolJoinManifest.sha256(after.get(row[0])),row[0]);
        assertEquals(DECOY,after.get("unrelated/Decoy.java"));
    }
    @Test void namedResultsFixedPointAndActualEmission() throws Exception {
        Map<String,String> before = before(); List<Result> result = sdk(files(before));
        assertEquals(3,result.size()); Map<String,String> after = apply(before,result); expected(after);
        assertEquals(0,sdk(files(after)).size());
        Path out = Path.of(System.getProperty("proof.root"),"sdk-results");
        Files.createDirectories(out.getParent()); Files.createDirectory(out);
        for (Result r : result) {
            SourceFile f = Objects.requireNonNull(r.getAfter()); Path p = out.resolve(f.getSourcePath());
            Files.createDirectories(p.getParent()); Files.writeString(p,f.printAll(),StandardOpenOption.CREATE_NEW);
        }
        Files.writeString(out.resolve("SDK_RECEIPT.txt"),"ACTUAL_NAMED_OPENREWRITE_RESULTS=3\nFIXED_POINT=0\n",StandardOpenOption.CREATE_NEW);
    }
    @Test void everyPartialStateAndInputOrder() throws Exception {
        List<String[]> changed = rows().stream().filter(r -> !r[3].equals("KEEP")).toList();
        for (int mask = 0; mask < 8; mask++) for (int order = 0; order < 3; order++) {
            Map<String,String> input = before();
            for (int i=0;i<3;i++) if ((mask & (1<<i))!=0) input.put(changed.get(i)[0],resource(ROOT+changed.get(i)[3]));
            List<SourceFile> sources = files(input);
            if (order == 1) Collections.reverse(sources);
            if (order == 2) Collections.rotate(sources,2);
            List<Result> results = sdk(sources); assertEquals(3-Integer.bitCount(mask),results.size());
            Map<String,String> after = apply(input,results); expected(after); assertTrue(sdk(files(after)).isEmpty());
        }
    }
    @Test void independentWorkerReplay() throws Exception {
        Map<String,String> input = before(), expected = apply(input,sdk(files(input)));
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<Map<String,String>> first = pool.submit(() -> apply(input,sdk(files(input))));
            Future<Map<String,String>> second = pool.submit(() -> apply(input,sdk(files(input))));
            assertEquals(expected,first.get(60,TimeUnit.SECONDS)); assertEquals(expected,second.get(60,TimeUnit.SECONDS));
        }
    }
    private static void refuses(List<SourceFile> input) throws IOException {
        List<Throwable> errors = new ArrayList<>();
        try { recipe().run(new InMemoryLargeSourceSet(input),new InMemoryExecutionContext(errors::add)); }
        catch (RuntimeException e) { errors.add(e); }
        assertFalse(errors.isEmpty(),"Any error bars all results from receiving");
    }
    @Test void missingDriftDuplicateAndOccupiedTargetsRefused() throws Exception {
        for (String[] row : rows()) {
            Map<String,String> drift = before(); drift.put(row[0],"// foreign or occupied source\n"); refuses(files(drift));
            if (!row[1].equals("ABSENT")) { Map<String,String> missing = before();missing.remove(row[0]);refuses(files(missing)); }
        }
        List<SourceFile> duplicate=files(before());duplicate.add(duplicate.getFirst());refuses(duplicate);
    }
    @Test void templateTamperLateEditAndCancellationRefused() throws Exception {
        for (String[] row : rows()) if (!row[3].equals("KEEP")) {
            assertThrows(IllegalStateException.class,() -> M3GpuToolJoinManifest.load(ROOT,path -> {
                String data=resource(path);if(path.equals(ROOT+row[3]))data+="// tamper\n";
                return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
            },() -> { }));
        }
        var manifest=M3GpuToolJoinManifest.load(ROOT,M3JavaUtilViewContractsRecipeTest.class::getResourceAsStream,() -> { });
        var scan=manifest.scan(); before().forEach((p,t) -> scan.observe(p,M3GpuToolJoinManifest.sha256(t)));scan.requireAdmissible();
        assertThrows(IllegalStateException.class,() -> scan.editing(rows().getFirst()[0],"0".repeat(64)));
        assertThrows(CancellationException.class,() -> M3GpuToolJoinManifest.load(ROOT,
                M3JavaUtilViewContractsRecipeTest.class::getResourceAsStream,() -> { throw new CancellationException("cancel"); }));
        var context=new InMemoryExecutionContext();
        context.putMessage(M3GpuToolJoinRecipe.CHECKPOINT_MESSAGE,(Runnable)() -> {throw new CancellationException("cancel");});
        assertThrows(CancellationException.class,() -> new M3JavaUtilViewContractsRecipe().getInitialValue(context));
    }
}
