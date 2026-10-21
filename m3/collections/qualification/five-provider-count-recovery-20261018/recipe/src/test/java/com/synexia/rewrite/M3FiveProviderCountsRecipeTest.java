/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Exact seven-source recipe admission, fixed point, drift and missing-owner refusals. */
final class M3FiveProviderCountsRecipeTest {
    private static final String ROOT="/com/synexia/rewrite/five-provider-count-recovery-20261018/";

    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT,
                M3FiveProviderCountsRecipeTest.class::getResourceAsStream, () -> {});
    }
    private static String resource(String name) throws IOException {
        try (InputStream input=M3FiveProviderCountsRecipeTest.class.getResourceAsStream(ROOT+name)) {
            if (input==null) throw new IOException("missing qualified resource: "+name);
            return new String(input.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static SourceFile source(String path,String content) {
        return PlainText.builder().sourcePath(Path.of(path)).text(content).build();
    }
    private static List<SourceFile> inputs(boolean postimage) throws IOException {
        List<SourceFile> sources = new ArrayList<>();
        for (var target:manifest().targets()) {
            if (postimage) {
                sources.add(source(target.path(),target.text()));
            } else if (!target.addition()) {
                String name=target.path().substring(target.path().lastIndexOf('/')+1);
                String before=resource("preimages/"+name);
                assertEquals(target.before(),M3GpuToolJoinManifest.sha256(before));
                sources.add(source(target.path(),before));
            }
        }
        return sources;
    }
    @Test void recipeActuallyGeneratesAllSourceFiles() throws Exception {
        var errors=new ArrayList<Throwable>();
        var results=new M3FiveProviderCountsRecipe().run(
                new InMemoryLargeSourceSet(inputs(false)),
                new InMemoryExecutionContext(errors::add)).getChangeset().getAllResults();
        assertTrue(errors.isEmpty(),errors.toString());
        var actual=new LinkedHashMap<String,String>();
        for (var result:results) {
            assertNotNull(result.getAfter());
            String path=result.getAfter().getSourcePath().toString().replace('\\','/');
            assertNull(actual.put(path,result.getAfter().printAll()),"duplicate result path");
        }
        for(var output:manifest().targets()) assertEquals(output.text(),actual.get(output.path()),output.path());
        assertEquals(7,actual.size());
    }
    @Test void secondReplayIsFixedPoint() throws Exception {
        var errors=new ArrayList<Throwable>();
        var results=new M3FiveProviderCountsRecipe().run(
                new InMemoryLargeSourceSet(inputs(true)),
                new InMemoryExecutionContext(errors::add)).getChangeset().getAllResults();
        assertTrue(errors.isEmpty(),errors.toString());
        assertTrue(results.isEmpty(),"source-sealed transformation must reach fixed point");
    }
    @Test void driftAndMissingPreimagesRefusePromotion() throws Exception {
        var initial=inputs(false);
        var first=initial.get(0);
        initial.set(0,source(first.getSourcePath().toString(),first.printAll()+"\n// tampered"));
        var errors=new ArrayList<Throwable>();
        try {
            new M3FiveProviderCountsRecipe().run(new InMemoryLargeSourceSet(initial),
                    new InMemoryExecutionContext(errors::add));
        } catch(RuntimeException failure){errors.add(failure);}
        assertFalse(errors.isEmpty(),"drift must reject");
        var missing=inputs(false);
        missing.remove(0);
        errors.clear();
        try {
            new M3FiveProviderCountsRecipe().run(new InMemoryLargeSourceSet(missing),
                    new InMemoryExecutionContext(errors::add));
        } catch(RuntimeException failure){errors.add(failure);}
        assertFalse(errors.isEmpty(),"missing required source must reject");
    }
    @Test void yamlNameResolvesThroughRealOpenRewriteEnvironment() {
        var recipe=Environment.builder().scanClassLoader(getClass().getClassLoader())
                .build().activateRecipes("com.synexia.m3.FiveProviderCountRecovery20261018");
        assertTrue(recipe.validateAll().stream().allMatch(validation -> validation.isValid()));
    }
}
