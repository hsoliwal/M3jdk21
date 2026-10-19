/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0. */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

/** Execute the actual M3JDK21 OpenRewrite Java-LST owner, not an independent generator. */
final class M3DirectArrayDequeHeapReceiverTest {
    private static final String CRATE = "m3-native-generic-deque-priority-20261018";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    private record Target(String path, String hash, String value) { }

    private static String content(String name) throws Exception {
        try (InputStream input = M3DirectArrayDequeHeapReceiverTest.class.getResourceAsStream(ROOT+name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static String sha(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }
    private static List<Target> targets() throws Exception {
        var out=new ArrayList<Target>();
        String last="";
        for(String row:content("manifest.tsv").lines().toList()) {
            if(row.isBlank() || row.startsWith("#"))continue;
            String[] cells=row.split("\t",-1);
            assertEquals(4,cells.length);
            assertEquals("ABSENT",cells[1]);
            assertTrue(cells[0].compareTo(last)>0);
            String javaText=content(cells[3]);
            assertEquals(cells[2],sha(javaText));
            out.add(new Target(cells[0],cells[2],javaText));
            last=cells[0];
        }
        assertEquals(4,out.size());
        return out;
    }
    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(failure -> {throw new IllegalStateException(failure);});
    }
    private static Map<String,SourceFile> run(List<SourceFile> input) {
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var changes = recipe.run(new InMemoryLargeSourceSet(input), context(), 1).getChangeset().getAllResults();
        Map<String, SourceFile> output=new TreeMap<>();
        for (var change:changes) {
            var after=change.getAfter();
            assertNotNull(after);
            assertNull(output.put(after.getSourcePath().toString().replace('\\','/'),after));
        }
        return output;
    }
    @Test void exactFourJdk21SourceImagesAndFixedPoint() throws Exception {
        var out=run(List.of());
        assertEquals(4,out.size());
        for(var t:targets()) {
            assertNotNull(out.get(t.path()));
            assertEquals(t.value(),out.get(t.path()).printAll());
            assertEquals(t.hash(),sha(out.get(t.path()).printAll()));
        }
        assertTrue(run(new ArrayList<>(out.values())).isEmpty());
    }
    @Test void allSixteenPartialSourceSetsAndBothOrdersConverge() throws Exception {
        var full=run(List.of());
        var targets=targets();
        for(int mask=0;mask<16;mask++) {
            for(boolean reversed:List.of(false,true)) {
                var input=new ArrayList<SourceFile>();
                for(int i=0;i<4;i++)if((mask&(1<<i))!=0)input.add(full.get(targets.get(i).path()));
                if(reversed)Collections.reverse(input);
                var additions=run(input);
                assertEquals(4-Integer.bitCount(mask),additions.size());
                input.addAll(additions.values());
                assertTrue(run(input).isEmpty());
            }
        }
    }
    @Test void alteredOrDuplicateTargetIsRefusedByRealEngine() throws Exception {
        var expected=targets().get(0);
        var parsed=JavaParser.fromJavaVersion().build().parseInputs(List.of(
            Parser.Input.fromString(Path.of(expected.path()),
                "package com.m3.collections; public final class M3ArrayDeque {}")),
            null,context()).toList();
        assertEquals(1,parsed.size());
        assertThrows(RuntimeException.class,()->run(parsed));
        var valid=run(List.of()).get(expected.path());
        assertThrows(RuntimeException.class,()->run(List.of(valid,valid)));
    }
}
