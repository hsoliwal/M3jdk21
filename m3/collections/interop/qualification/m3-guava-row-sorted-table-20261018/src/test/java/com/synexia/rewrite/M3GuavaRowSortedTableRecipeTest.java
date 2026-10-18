/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

/** Real Maven/OpenRewrite Java LST admission and fixed-point tests. */
final class M3GuavaRowSortedTableRecipeTest {
    private static final String CRATE="m3-guava-row-sorted-table-20261018";
    private static final String DIR="m3/collections/interop/src/";
    private static final String[] NAMES={
        "main/java/com/m3/collections/interop/M3GuavaRowSortedTable.java",
        "test/java/com/m3/collections/interop/M3GuavaRowSortedTableContract.java",
        "test/java/com/m3/collections/interop/M3GuavaRowSortedTableTest.java"
    };

    private static InMemoryExecutionContext ctx(){
        return new InMemoryExecutionContext(error->{throw new AssertionError(error);});
    }
    private static Map<String,SourceFile> generate(List<SourceFile> input) {
        var result = new M3HashPinnedJavaSnapshotRecipe(CRATE).run(
                new InMemoryLargeSourceSet(input),ctx(),1).getChangeset().getAllResults();
        var out = new TreeMap<String,SourceFile>();
        for(var change:result){
            SourceFile after=change.getAfter();
            assertNotNull(after);
            assertInstanceOf(org.openrewrite.java.tree.J.CompilationUnit.class,after);
            assertNull(out.put(after.getSourcePath().toString().replace('\\','/'),after));
        }
        return out;
    }
    @Test void actualLSTGenerationAndFixedPoint() {
        var files=generate(List.of());
        assertEquals(NAMES.length,files.size());
        for(var name:NAMES) {
            assertNotNull(files.get(DIR+name));
            assertTrue(files.get(DIR+name).printAll().contains("package com.m3.collections.interop;"));
        }
        assertTrue(generate(List.copyOf(files.values())).isEmpty());
    }
    @Test void partialOrdersAndRefusal() {
        var all=generate(List.of());
        for(int mask=0;mask<8;mask++){
            for(boolean reverse:List.of(false,true)){
                var sources=new ArrayList<SourceFile>();
                for(int i=0;i<NAMES.length;i++)if((mask&(1<<i))!=0)sources.add(all.get(DIR+NAMES[i]));
                if(reverse)Collections.reverse(sources);
                var missing=generate(sources);
                assertEquals(3-Integer.bitCount(mask),missing.size());
                sources.addAll(missing.values());
                assertTrue(generate(sources).isEmpty());
            }
        }
        String path=DIR+NAMES[0];
        var bad=JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(path),
                        "package com.m3.collections.interop; public final class M3GuavaRowSortedTable {}")),
                null,ctx()).toList();
        assertThrows(RuntimeException.class,()->generate(bad));
        var original=all.get(path);
        assertThrows(RuntimeException.class,()->generate(List.of(original,original)));
    }
}
