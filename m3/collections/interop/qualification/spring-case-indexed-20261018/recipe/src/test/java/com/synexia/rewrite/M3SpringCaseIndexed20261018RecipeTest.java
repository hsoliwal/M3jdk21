/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
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

final class M3SpringCaseIndexed20261018RecipeTest {
    private static final String CRATE="m3-spring-case-indexed-20261018";
    private static final String RESOURCE="/com/synexia/rewrite/hash-pinned-java/"+CRATE+"/";
    private record Target(String path,String sha,String text) { }
    private static String read(String path) throws Exception {
        try(InputStream input=M3SpringCaseIndexed20261018RecipeTest.class.getResourceAsStream(RESOURCE+path)) {
            assertNotNull(input,path);
            return new String(input.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static String sha(String contents) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(contents.getBytes(StandardCharsets.UTF_8)));
    }
    private static List<Target> targets() throws Exception {
        var result=new ArrayList<Target>();
        String previous="";
        for(String row:read("manifest.tsv").lines().toList()) {
            if(row.isBlank()||row.startsWith("#"))continue;
            String[] f=row.split("\t",-1);assertEquals(4,f.length);
            assertEquals("ABSENT",f[1]);
            assertTrue(f[0].compareTo(previous)>0);
            var content=read(f[3]);assertEquals(f[2],sha(content));
            result.add(new Target(f[0],f[2],content));
            previous=f[0];
        }
        assertEquals(3,result.size());
        return result;
    }
    private static InMemoryExecutionContext ctx() {
        return new InMemoryExecutionContext(e -> {throw new IllegalStateException(e);});
    }
    private static Map<String,SourceFile> execute(Collection<? extends SourceFile> inputs) {
        var core=new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var run=core.run(new InMemoryLargeSourceSet(new ArrayList<>(inputs)),ctx(),1);
        Map<String,SourceFile> generated=new TreeMap<>();
        for(var diff:run.getChangeset().getAllResults()) {
            var output=diff.getAfter();assertNotNull(output);
            assertNull(generated.put(output.getSourcePath().toString().replace('\\','/'),output));
        }
        return generated;
    }
    @Test void exactJdk21LstsAndRepeatFixedPoint() throws Exception {
        var changed=execute(List.of());assertEquals(3,changed.size());
        for(var row:targets()) {
            var after=changed.get(row.path());assertNotNull(after);
            assertEquals(row.text(),after.printAll());
            assertEquals(row.sha(),sha(after.printAll()));
        }
        assertTrue(execute(changed.values()).isEmpty());
    }
    @Test void everyPartialInstallationAndInputOrderConverge() throws Exception {
        var all=execute(List.of());
        var targets=targets();
        for(int subset=0;subset<8;subset++)for(boolean reverse:List.of(false,true)) {
            List<SourceFile> inputs=new ArrayList<>();
            for(int i=0;i<3;i++)if((subset&(1<<i))!=0)inputs.add(all.get(targets.get(i).path()));
            if(reverse)Collections.reverse(inputs);
            var delta=execute(inputs);
            assertEquals(3-Integer.bitCount(subset),delta.size());
            inputs.addAll(delta.values());
            assertEquals(3,inputs.size());
            assertTrue(execute(inputs).isEmpty());
        }
    }
    @Test void driftAndDuplicateSourcesRefuse() throws Exception {
        var target=targets().get(0);
        var parsed=JavaParser.fromJavaVersion().build().parseInputs(List.of(
                Parser.Input.fromString(Path.of(target.path()),
                    "package com.m3.collections.interop; public class M3SpringCaseInsensitiveMap {}")),
                null,ctx()).toList();
        assertEquals(1,parsed.size());
        assertThrows(RuntimeException.class,()->execute(parsed));
        var admitted=execute(List.of()).get(target.path());
        assertThrows(RuntimeException.class,()->execute(List.of(admitted,admitted)));
    }
}
