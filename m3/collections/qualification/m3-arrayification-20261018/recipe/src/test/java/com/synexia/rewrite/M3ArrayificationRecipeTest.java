/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;
import org.openrewrite.config.Environment;
import static org.junit.jupiter.api.Assertions.*;

/** Actual OpenRewrite SDK generation, refusal and second-run fixed-point proof. */
final class M3ArrayificationRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/arrayification-20261018/";
    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT,
                M3ArrayificationRecipeTest.class::getResourceAsStream, () -> {});
    }
    private static SourceFile source(String path,String body) {
        return PlainText.builder().sourcePath(java.nio.file.Path.of(path)).text(body).build();
    }
    private static String beforeOwner() throws Exception {
        try(InputStream in=M3ArrayificationRecipeTest.class.getResourceAsStream(ROOT+"before-M3FiveLibraryIndexViews.java")){
            assertNotNull(in);return new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static List<SourceFile> input(boolean after, boolean includeAdditions) throws Exception {
        List<SourceFile> files=new ArrayList<>();
        for(var t:manifest().targets()){
            if(t.addition()&&!includeAdditions)continue;
            String text=t.path().endsWith("/M3FiveLibraryIndexViews.java")&&!after?beforeOwner():t.text();
            assertNotNull(text);
            files.add(source(t.path(),text));
        }
        return files;
    }
    @Test void actualOpenRewriteSdkGeneratesFourOutputsFromExactPreimage() throws Exception {
        var errors=new ArrayList<Throwable>();
        var run=new M3Arrayification20261018Recipe().run(
            new InMemoryLargeSourceSet(input(false,false)),new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(),errors.toString());
        var result=run.getChangeset().getAllResults();
        assertEquals(4,result.size());
        for(var t:manifest().targets()){
            var changed=result.stream().filter(x->x.getAfter()!=null
                    && x.getAfter().getSourcePath().toString().replace('\\','/').equals(t.path())).findFirst().orElseThrow();
            assertEquals(t.text(),changed.getAfter().printAll());
        }
    }
    @Test void alreadyInstalledSourceIsAFixedPoint() throws Exception {
        var errors=new ArrayList<Throwable>();
        var result=new M3Arrayification20261018Recipe().run(
            new InMemoryLargeSourceSet(input(true,true)),new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(),errors.toString());
        assertTrue(result.getChangeset().getAllResults().isEmpty());
    }
    @Test void sourceDriftRefuses() throws Exception {
        var originals=input(false,false);
        var first=originals.getFirst();
        originals.set(0,source(first.getSourcePath().toString(),first.printAll()+"\n// drift"));
        var errors=new ArrayList<Throwable>();
        try {new M3Arrayification20261018Recipe().run(
                new InMemoryLargeSourceSet(originals),new InMemoryExecutionContext(errors::add));}
        catch(RuntimeException e){errors.add(e);}
        assertFalse(errors.isEmpty(),"drift must not generate candidates");
    }
    @Test void namedRecipeDiscoverable() {
        var r=Environment.builder().scanClassLoader(getClass().getClassLoader()).build()
                .activateRecipes("com.synexia.m3.Arrayification20261018");
        assertTrue(r.validateAll().stream().allMatch(v->v.isValid()));
    }
}
