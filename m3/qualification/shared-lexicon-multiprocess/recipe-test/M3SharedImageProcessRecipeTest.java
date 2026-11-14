// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;
import static org.junit.jupiter.api.Assertions.*;
public final class M3SharedImageProcessRecipeTest {
 static final String CRATE="m3-shared-image-process-probe";
 static final Map<String,String> qualified=new TreeMap<>();
 static String resource(String n)throws Exception{try(var in=M3SharedImageProcessRecipeTest.class.getResourceAsStream("/com/synexia/rewrite/hash-pinned-java/"+CRATE+"/"+n)){if(in==null)throw new IllegalStateException(n);return new String(in.readAllBytes(),StandardCharsets.UTF_8);}}
 static String path()throws Exception{return resource("manifest.tsv").strip().split("\t",-1)[0];}
 static InMemoryExecutionContext ctx(List<Throwable> errors){var c=new InMemoryExecutionContext(errors::add);c.putMessage("com.synexia.rewrite.hashPinnedJavaSnapshotRecipe.parserClasspath",List.<Path>of());return c;}
 static SourceFile candidate()throws Exception{var e=new ArrayList<Throwable>();var r=new M3HashPinnedJavaSnapshotRecipe(CRATE);var run=r.run(new InMemoryLargeSourceSet(List.of()),ctx(e));assertTrue(e.isEmpty(),e.toString());var changes=run.getChangeset().getAllResults();assertEquals(1,changes.size());assertNull(changes.getFirst().getBefore());var f=changes.getFirst().getAfter();assertNotNull(f);assertInstanceOf(J.CompilationUnit.class,f);assertEquals(path(),f.getSourcePath().toString().replace('\\','/'));assertEquals(resource("M3SharedImageProcessProbe.java.txt"),f.printAll());return f;}
 @Test void additiveReplayAndFixedPoint()throws Exception{var f=candidate();qualified.put(path(),f.printAll());var e=new ArrayList<Throwable>();var run=new M3HashPinnedJavaSnapshotRecipe(CRATE).run(new InMemoryLargeSourceSet(List.of(f)),ctx(e));assertTrue(e.isEmpty(),e.toString());assertTrue(run.getChangeset().getAllResults().isEmpty());}
 static void reject(List<SourceFile> f){var e=new ArrayList<Throwable>();try{var result=new M3HashPinnedJavaSnapshotRecipe(CRATE).run(new InMemoryLargeSourceSet(f),ctx(e));assertTrue(result.getChangeset().getAllResults().isEmpty());assertFalse(e.isEmpty());}catch(IllegalStateException expected){assertNotNull(expected.getMessage());}}
 @Test void duplicateRefuses()throws Exception{var f=candidate();reject(List.of(f,f));}
 @Test void wrongAstRefuses()throws Exception{reject(List.of(PlainText.builder().sourcePath(Path.of(path())).text(resource("M3SharedImageProcessProbe.java.txt")).build()));}
 @Test void changedAfterScanRefuses()throws Exception{var f=candidate();var e=new ArrayList<Throwable>();var c=ctx(e);var r=new M3HashPinnedJavaSnapshotRecipe(CRATE);var i=r.getInitialValue(c);r.getScanner(i).visit(f,c);var failure=assertThrows(RuntimeException.class,()->r.getVisitor(i).visit(((J.CompilationUnit)f).withPrefix(org.openrewrite.java.tree.Space.format("// drift\n")),c));Throwable cause=failure;while(cause.getCause()!=null)cause=cause.getCause();assertInstanceOf(IllegalStateException.class,cause);assertTrue(cause.getMessage().startsWith("target changed after scan: "));}
 @Test void observedDataHasNoProductionWrites()throws Exception{String text=resource("M3SharedImageProcessProbe.java.txt");assertTrue(text.contains("ReadOnlyBufferException"));assertTrue(text.contains("reachabilityFence(image)"));assertFalse(text.contains("System.gc("));assertFalse(text.contains("setAccessible(true); address.set"));assertEquals(Set.of(path()),new M3HashPinnedJavaSnapshotRecipe(CRATE).postimageManifest().keySet());}
}
