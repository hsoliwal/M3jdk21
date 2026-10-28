/* Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;
import static org.junit.jupiter.api.Assertions.*;
/** Proves exact generated postimage, repeat fixed point and refusal on drift. */
final class M3ArrayMaskSparseRecipeTest {
    private static final String PATH="src/java.base/share/classes/jdk/internal/mindex/M3CollectionMaskCompactor.java";
    private static final String ROOT="/com/synexia/rewrite/m3-array-mask-sparse-20261018/";
    private static String read(String path) throws Exception {
        try(var in=M3ArrayMaskSparseRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(in,path);
            return new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        }
    }
    private static SourceFile source(String value) {
        return PlainText.builder().sourcePath(Path.of(PATH)).text(value).build();
    }
    @Test void generatedSourceAndFixedPoint() throws Exception {
        var failures=new ArrayList<Throwable>();
        String before=read("/preimage/M3CollectionMaskCompactor.java");
        String after=read(ROOT+"M3CollectionMaskCompactor.java.txt");
        var generated=new M3ArrayMaskSparseRecipe().run(
             new InMemoryLargeSourceSet(List.of(source(before))),
             new InMemoryExecutionContext(failures::add));
        assertTrue(failures.isEmpty(),failures.toString());
        var output=generated.getChangeset().getAllResults();
        assertEquals(1,output.size());
        assertEquals(after,output.get(0).getAfter().printAll());
        var second=new M3ArrayMaskSparseRecipe().run(
             new InMemoryLargeSourceSet(List.of(source(after))),
             new InMemoryExecutionContext(failures::add));
        assertTrue(failures.isEmpty(),failures.toString());
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }
    @Test void refusesSourceDrift() throws Exception {
        var failures=new ArrayList<Throwable>();
        try {
            new M3ArrayMaskSparseRecipe().run(
                new InMemoryLargeSourceSet(List.of(
                    source(read("/preimage/M3CollectionMaskCompactor.java")+"\n//drift"))),
                new InMemoryExecutionContext(failures::add));
        } catch(RuntimeException expected) { failures.add(expected); }
        assertFalse(failures.isEmpty());
    }
    @Test void refusesMissingPreimage() {
        var failures=new ArrayList<Throwable>();
        try {
            new M3ArrayMaskSparseRecipe().run(new InMemoryLargeSourceSet(List.of()),
                 new InMemoryExecutionContext(failures::add));
        } catch(RuntimeException expected) { failures.add(expected); }
        assertFalse(failures.isEmpty());
    }
    @Test void discoversNamedRecipe() {
        var named=Environment.builder().scanClassLoader(getClass().getClassLoader()).build()
            .activateRecipes("com.synexia.m3.M3ArrayMaskSparse20261018");
        assertTrue(named.validateAll().stream().allMatch(v->v.isValid()));
    }
}
