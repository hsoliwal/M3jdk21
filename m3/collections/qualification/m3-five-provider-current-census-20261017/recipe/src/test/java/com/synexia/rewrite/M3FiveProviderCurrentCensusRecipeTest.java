/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.synexia.rewrite;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;
import static org.junit.jupiter.api.Assertions.*;

/** Actual OpenRewrite scheduler qualification, not text-file-copy simulation. */
final class M3FiveProviderCurrentCensusRecipeTest {
    private static final String ROOT="/com/synexia/rewrite/m3-five-provider-current-census-20261017/";
    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT,
                M3FiveProviderCurrentCensusRecipeTest.class::getResourceAsStream, () -> {});
    }
    private static SourceFile source(String path,String value) {
        return PlainText.builder().sourcePath(Path.of(path)).text(value).build();
    }
    private static String resource(String name) {
        try (InputStream in=M3FiveProviderCurrentCensusRecipeTest.class.getResourceAsStream(ROOT+name)) {
            assertNotNull(in,name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException failure) { throw new AssertionError(failure); }
    }
    private static List<SourceFile> sources(boolean after) {
        var all=new ArrayList<SourceFile>();
        for (var target:manifest().targets()) {
            if (!after && target.addition()) continue;
            String value= after ? target.text() : resource("preimages/"+target.path().substring(target.path().lastIndexOf('/')+1));
            if (!after) assertEquals(target.before(),M3GpuToolJoinManifest.sha256(value),target.path());
            all.add(source(target.path(),value));
        }
        return all;
    }
    @Test void sdkGeneratesEveryExactPostimage() {
        var errors=new ArrayList<Throwable>();
        var run=new M3FiveProviderCurrentCensusRecipe().run(new InMemoryLargeSourceSet(sources(false)),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(),errors.toString());
        var found=new HashMap<String,String>();
        for(var change:run.getChangeset().getAllResults()) {
            assertNotNull(change.getAfter());
            found.put(change.getAfter().getSourcePath().toString().replace('\\','/'),
                    change.getAfter().printAll());
        }
        for(var target:manifest().targets()) assertEquals(target.text(),found.get(target.path()),target.path());
        assertEquals(4,found.size());
    }
    @Test void fixedPointWithAllOriginalAndGeneratedSources() {
        var errors=new ArrayList<Throwable>();
        var run=new M3FiveProviderCurrentCensusRecipe().run(new InMemoryLargeSourceSet(sources(true)),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(),errors.toString());
        assertTrue(run.getChangeset().getAllResults().isEmpty());
    }
    @Test void failClosedOnDriftMissingAndDuplicates() {
        var input=sources(false);
        input.set(0,source(input.get(0).getSourcePath().toString(),input.get(0).printAll()+"DRIFT"));
        refuses(input);
        input=sources(false);input.remove(0);refuses(input);
        input=sources(false);input.add(input.get(0));refuses(input);
    }
    private static void refuses(List<SourceFile> sources) {
        var errors=new ArrayList<Throwable>();
        try { new M3FiveProviderCurrentCensusRecipe().run(
                new InMemoryLargeSourceSet(sources),new InMemoryExecutionContext(errors::add)); }
        catch(RuntimeException thrown) { errors.add(thrown); }
        assertFalse(errors.isEmpty(),"unsafe recipe run accepted");
    }
    @Test void namedRecipeDiscovery() {
        var recipe=Environment.builder().scanClassLoader(getClass().getClassLoader()).build()
                .activateRecipes("com.synexia.m3.FiveProviderCurrentCensus20261017");
        assertTrue(recipe.validateAll().stream().allMatch(v->v.isValid()));
    }
}
