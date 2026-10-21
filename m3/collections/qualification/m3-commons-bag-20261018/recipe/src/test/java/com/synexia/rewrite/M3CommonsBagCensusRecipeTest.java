/* Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;
import static org.junit.jupiter.api.Assertions.*;

/** Exact four-target plain-text OpenRewrite rewrite; requires real preimages. */
class M3CommonsBagCensusRecipeTest {
    private static final String ROOT="/com/synexia/rewrite/m3-commons-bag-census-20261018/";
    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT,
                M3CommonsBagCensusRecipeTest.class::getResourceAsStream,()->{});
    }
    private static String pre(String name) throws IOException {
        try(InputStream in=M3CommonsBagCensusRecipeTest.class.getResourceAsStream(ROOT+"pre/"+name)) {
            assertNotNull(in,name);
            return new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static List<SourceFile> sources(boolean after) throws IOException {
        List<SourceFile> items=new ArrayList<>();
        for(var owner:manifest().targets()) {
            String name=Path.of(owner.path()).getFileName().toString();
            String text=after ? owner.text() : pre(name);
            assertEquals(after?owner.after():owner.before(),
                    M3GpuToolJoinManifest.sha256(text),name);
            items.add(PlainText.builder().sourcePath(Path.of(owner.path())).text(text).build());
        }
        return items;
    }
    @Test void exactCensusRefinement() throws IOException {
        var errors=new ArrayList<Throwable>();
        var run=new M3CommonsBagCensusRecipe().run(
                new InMemoryLargeSourceSet(sources(false)),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(),errors.toString());
        var actual=new HashMap<String,String>();
        for(var change:run.getChangeset().getAllResults()) {
            assertNotNull(change.getBefore());
            assertNotNull(change.getAfter());
            actual.put(change.getAfter().getSourcePath().toString().replace('\\','/'),
                    change.getAfter().printAll());
        }
        assertEquals(4,actual.size());
        for(var owner:manifest().targets()) assertEquals(owner.text(),actual.get(owner.path()));
    }
    @Test void exactFixedPoint() throws IOException {
        var errors=new ArrayList<Throwable>();
        var run=new M3CommonsBagCensusRecipe().run(
                new InMemoryLargeSourceSet(sources(true)),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(),errors.toString());
        assertTrue(run.getChangeset().getAllResults().isEmpty());
    }
    @Test void missingAndDriftRefusal() throws IOException {
        var before=sources(false);
        for(int probe=0;probe<2;probe++) {
            var inputs=new ArrayList<>(before);
            if(probe==0) inputs.remove(0);
            else {
                SourceFile one=inputs.get(0);
                inputs.set(0,PlainText.builder().sourcePath(one.getSourcePath())
                        .text(one.printAll()+"\nsource drift\n").build());
            }
            var errors=new ArrayList<Throwable>();
            try {
                new M3CommonsBagCensusRecipe().run(new InMemoryLargeSourceSet(inputs),
                        new InMemoryExecutionContext(errors::add));
            } catch(RuntimeException refused) { errors.add(refused); }
            assertFalse(errors.isEmpty(),"census must refuse unqualified source");
        }
    }
    @Test void namedRecipeDiscovered() {
        var recipe=Environment.builder().scanClassLoader(getClass().getClassLoader())
                .build().activateRecipes("com.synexia.m3.CommonsBagCensus20261018");
        assertTrue(recipe.validateAll().stream().allMatch(v->v.isValid()));
    }
}
