/* Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

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

/** Actual Maven/OpenRewrite recipe scheduler admission; no substitute writer. */
class M3CommonsBagRecipeTest {
    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(
                "/com/synexia/rewrite/m3-commons-bag-20261018/",
                M3CommonsBagRecipeTest.class::getResourceAsStream, () -> { });
    }

    @Test void generatesExactSourceAndOracle() {
        var errors=new ArrayList<Throwable>();
        var result=new M3CommonsBagRecipe().run(
                new InMemoryLargeSourceSet(List.of()),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(),errors.toString());
        var actual=new HashMap<String,String>();
        for(var change:result.getChangeset().getAllResults()) {
            assertNull(change.getBefore());
            SourceFile output=change.getAfter();
            assertNotNull(output);
            assertNull(actual.put(output.getSourcePath().toString().replace('\\','/'),
                    output.printAll()));
        }
        assertEquals(2,actual.size());
        for(var item:manifest().targets()) {
            if(item.text()!=null) {
                assertEquals(item.text(),actual.get(item.path()));
                assertEquals(item.after(),M3GpuToolJoinManifest.sha256(item.text()));
            }
        }
    }

    @Test void fixedPointOnExactOutputs() {
        var files=new ArrayList<SourceFile>();
        for(var item:manifest().targets()) {
            if(item.text()!=null) files.add(PlainText.builder()
                    .sourcePath(Path.of(item.path())).text(item.text()).build());
        }
        var errors=new ArrayList<Throwable>();
        var result=new M3CommonsBagRecipe().run(
                new InMemoryLargeSourceSet(files),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(),errors.toString());
        assertTrue(result.getChangeset().getAllResults().isEmpty());
    }

    @Test void rejectsOccupiedOrDriftedOutput() {
        var item=manifest().targets().iterator().next();
        var source=PlainText.builder().sourcePath(Path.of(item.path()))
                .text("drifted source").build();
        var errors=new ArrayList<Throwable>();
        try {
            new M3CommonsBagRecipe().run(new InMemoryLargeSourceSet(List.of(source)),
                    new InMemoryExecutionContext(errors::add));
        } catch(RuntimeException refused) {
            errors.add(refused);
        }
        assertFalse(errors.isEmpty(),"occupied unrelated source cannot be overwritten");
    }

    @Test void namedCompositeIsDiscoverable() {
        var recipe=Environment.builder().scanClassLoader(getClass().getClassLoader())
                .build().activateRecipes("com.synexia.m3.CommonsBag20261018");
        assertTrue(recipe.validateAll().stream().allMatch(v->v.isValid()));
    }
}
