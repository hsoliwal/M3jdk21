/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.FindAndReplace;
import org.openrewrite.text.PlainText;

/** Actual OpenRewrite Java source generation, POM text operation, and refusal tests. */
final class M3ApacheInteropRecipeTest {
    private static final String ROOT="/com/synexia/rewrite/hash-pinned-java/apache-collections-interop/";
    private record Input(String path,String hash,String text) { }
    private static String resource(String path) throws Exception {
        try(var stream=M3ApacheInteropRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream,path);return new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static List<Input> targets() throws Exception {
        var records=new ArrayList<Input>();
        for (String row : resource(ROOT+"manifest.tsv").lines().toList()) {
            if(row.startsWith("#")||row.isBlank())continue;
            String[] cells=row.split("\t");assertEquals(4,cells.length);assertEquals("ABSENT",cells[1]);
            String text=resource(ROOT+cells[3]);assertEquals(cells[2],sha(text));
            records.add(new Input(cells[0],cells[2],text));
        }
        assertEquals(3,records.size());return records;
    }
    private static Map<String,String> apply(Map<String,String> source) throws Exception {
        var input=new ArrayList<SourceFile>();
        source.forEach((p,t)->input.add(PlainText.builder().sourcePath(Path.of(p)).text(t).build()));
        var errors=new ArrayList<Throwable>();
        var changes=new M3HashPinnedJavaSnapshotRecipe("apache-collections-interop",false)
                .run(new InMemoryLargeSourceSet(input),new InMemoryExecutionContext(errors::add),1)
                .getChangeset().getAllResults();
        assertTrue(errors.isEmpty(),()->errors.toString());
        var output=new TreeMap<String,String>();
        for(Result item:changes) {
            assertNotNull(item.getAfter());
            assertInstanceOf(PlainText.class,item.getAfter());
            assertNull(output.put(item.getAfter().getSourcePath().toString(),item.getAfter().printAll()));
        }
        return output;
    }
    @Test void allEightJavaInstallationStatesAndFixedPoint() throws Exception {
        var rows=targets();
        for(int mask=0;mask<8;mask++) for(boolean reverse:List.of(false,true)) {
            var input=new LinkedHashMap<String,String>();
            for(int i=0;i<3;i++)if((mask & 1<<i)!=0)input.put(rows.get(i).path,rows.get(i).text);
            if(reverse){var inverted=new LinkedHashMap<String,String>();
                var keys=new ArrayList<>(input.keySet());java.util.Collections.reverse(keys);
                for(String k:keys)inverted.put(k,input.get(k));input=inverted;}
            input.put("unrelated.txt","untouched\n");
            var changed=apply(input);assertEquals(3-Integer.bitCount(mask),changed.size());
            input.putAll(changed);
            for(var row:rows)assertEquals(row.hash,sha(input.get(row.path)));
            assertTrue(apply(input).isEmpty());
        }
    }
    @Test void driftAndDuplicateRefuse() throws Exception {
        var core=new M3HashPinnedJavaSnapshotRecipe("apache-collections-interop",false);
        for(var row:targets()){
            var context=new InMemoryExecutionContext();
            var scan=core.getInitialValue(context);
            core.getScanner(scan).visit(PlainText.builder().sourcePath(Path.of(row.path)).text("drift").build(),context);
            assertThrows(IllegalStateException.class,()->core.generate(scan,context));
            var duplicate=core.getInitialValue(context);
            var source=PlainText.builder().sourcePath(Path.of(row.path)).text(row.text).build();
            core.getScanner(duplicate).visit(source,context);core.getScanner(duplicate).visit(source,context);
            assertThrows(IllegalStateException.class,()->core.generate(duplicate,context));
        }
    }
    @Test void nativeOpenRewritePomEditIsExactAndFixedPoint() throws Exception {
        String root="/com/synexia/rewrite/hash-pinned-text/apache-collections-pom/";
        String old=resource(root+"pom.xml.before"),next=resource(root+"pom.xml.after");
        String row=resource(root+"manifest.tsv").strip();
        String[] cols=row.split("\t");assertEquals(4,cols.length);
        assertEquals(sha(old),cols[1]);assertEquals(sha(next),cols[2]);
        String spring="    <dependency><groupId>org.springframework</groupId><artifactId>spring-core</artifactId><version>6.2.0</version></dependency>";
        String replacement=spring+"\n    <dependency><groupId>org.apache.commons</groupId><artifactId>commons-collections4</artifactId><version>4.5.0</version></dependency>";
        assertEquals(1,old.split(java.util.regex.Pattern.quote(spring),-1).length-1);
        Recipe edit=new FindAndReplace(spring,replacement,false,true,false,false,"**/m3/collections/interop/pom.xml");
        Map<String,String> source=Map.of(cols[0],old);
        var file=PlainText.builder().sourcePath(Path.of(cols[0])).text(old).build();
        var changes=edit.run(new InMemoryLargeSourceSet(List.of(file)),new InMemoryExecutionContext(),1)
                .getChangeset().getAllResults();
        assertEquals(1,changes.size());
        assertEquals(next,changes.getFirst().getAfter().printAll());
        assertEquals(sha(next),sha(changes.getFirst().getAfter().printAll()));
        assertTrue(edit.run(new InMemoryLargeSourceSet(List.of(changes.getFirst().getAfter())),
                new InMemoryExecutionContext(),1).getChangeset().getAllResults().isEmpty());
        var factory=javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        factory.newDocumentBuilder().parse(new ByteArrayInputStream(next.getBytes(StandardCharsets.UTF_8)));
        assertEquals(source.size(),1);
    }
    @Test void generatedJavaHashesAreSealed() throws Exception {
        Map<String,String> results=apply(Map.of());assertEquals(3,results.size());
        for(var row:targets()){assertEquals(row.text,results.get(row.path));assertEquals(row.hash,sha(results.get(row.path)));}
    }
}
