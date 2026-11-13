// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;
import static org.junit.jupiter.api.Assertions.*;
public final class M3CorpusOptionalPeriodRecipeTest {
 static final String CRATE=System.getProperty("m3.recipe.crate");
 static final String STAGE=System.getProperty("m3.recipe.stage");
 static final String CORPUS="m3/core/src/jdk/m3/text/M3LanguageCorpusManifest.java";
 static final Map<String,String> qualified=new TreeMap<>();
 static String resource(String n)throws Exception{try(var in=M3CorpusOptionalPeriodRecipeTest.class.getResourceAsStream("/com/synexia/rewrite/hash-pinned-java/"+CRATE+"/"+n)){if(in==null)throw new IllegalStateException(n);return new String(in.readAllBytes(),StandardCharsets.UTF_8);}}
 static String[] row()throws Exception{return resource("manifest.tsv").strip().split("\t",-1);}
 static InMemoryExecutionContext ctx(List<Throwable> errors){var c=new InMemoryExecutionContext(errors::add);c.putMessage("com.synexia.rewrite.hashPinnedJavaSnapshotRecipe.parserClasspath",List.<Path>of());return c;}
 static SourceFile before()throws Exception{var errors=new ArrayList<Throwable>();var files=JavaParser.fromJavaVersion().classpath(List.<Path>of()).build().parseInputs(List.of(Parser.Input.fromString(Path.of(row()[0]),resource("Target.java.before"))),null,ctx(errors)).toList();assertTrue(errors.isEmpty(),errors.toString());assertEquals(1,files.size());assertInstanceOf(J.CompilationUnit.class,files.getFirst());return files.getFirst();}
 static void reject(List<SourceFile> f){var e=new ArrayList<Throwable>();try{var result=new M3HashPinnedJavaSnapshotRecipe(CRATE).run(new InMemoryLargeSourceSet(f),ctx(e));assertTrue(result.getChangeset().getAllResults().isEmpty());assertFalse(e.isEmpty());}catch(IllegalStateException expected){assertNotNull(expected.getMessage());}}
 @Test void replayAndFixedPoint()throws Exception{var original=before();var other=PlainText.builder().sourcePath(Path.of("unrelated.txt")).text("keep\n").build();var errors=new ArrayList<Throwable>();var recipe=new M3HashPinnedJavaSnapshotRecipe(CRATE);var changes=recipe.run(new InMemoryLargeSourceSet(List.of(original,other)),ctx(errors)).getChangeset().getAllResults();assertTrue(errors.isEmpty(),errors.toString());assertEquals(1,changes.size());var after=changes.getFirst().getAfter();assertNotNull(after);assertInstanceOf(J.CompilationUnit.class,after);assertEquals(original.getId(),after.getId());assertEquals(original.getSourcePath(),after.getSourcePath());assertEquals(resource("Target.java.txt"),after.printAll());qualified.put(row()[0],after.printAll());var replay=recipe.run(new InMemoryLargeSourceSet(List.of(after,other)),ctx(errors));assertTrue(errors.isEmpty(),errors.toString());assertTrue(replay.getChangeset().getAllResults().isEmpty());}
 @Test void missingOwnerRefuses(){reject(List.of());}
 @Test void duplicateRefuses()throws Exception{var f=before();reject(List.of(f,f));}
 @Test void wrongAstRefuses()throws Exception{reject(List.of(PlainText.builder().sourcePath(Path.of(row()[0])).text(resource("Target.java.before")).build()));}
 @Test void sourceDriftRefuses()throws Exception{reject(List.of(((J.CompilationUnit)before()).withPrefix(org.openrewrite.java.tree.Space.format("// drift\n"))));}
 @Test void changedAfterScanRefuses()throws Exception{var f=before();var errors=new ArrayList<Throwable>();var c=ctx(errors);var r=new M3HashPinnedJavaSnapshotRecipe(CRATE);var i=r.getInitialValue(c);r.getScanner(i).visit(f,c);var failure=assertThrows(RuntimeException.class,()->r.getVisitor(i).visit(((J.CompilationUnit)f).withPrefix(org.openrewrite.java.tree.Space.format("// moved\n")),c));Throwable cause=failure;while(cause.getCause()!=null)cause=cause.getCause();assertInstanceOf(IllegalStateException.class,cause);assertTrue(cause.getMessage().startsWith("target changed after scan: "));}
 @Test void onlyFrozenAtomChanges()throws Exception{String before=resource("Target.java.before"),after=resource("Target.java.txt"),old=resource("atom-before.txt"),change=resource("atom-after.txt");assertEquals(2,before.split(java.util.regex.Pattern.quote(old),-1).length);assertEquals(before.replace(old,change),after);}
 @Test void exactOwnerAdmissionAndNeighborsRemainClosed()throws Exception{
  boolean scope=!STAGE.equals("scope"),java=STAGE.equals("corpus");
  if(scope){var location=M3ScopeInference.describeJavaTarget(CORPUS);assertEquals("m3/core",location.module());assertEquals("jdk.m3.text",location.packageName());}
  else assertThrows(IllegalArgumentException.class,()->M3ScopeInference.describeJavaTarget(CORPUS));
  var path=M3HashPinnedJavaSnapshotRecipe.class.getDeclaredMethod("javaPath",String.class);path.setAccessible(true);
  assertEquals(java,path.invoke(null,CORPUS));
  for(String neighbor:List.of("m3/core/src/jdk/m3/text/M3LanguageCorpusManifestExtra.java","m3/core/src/jdk/m3/text/m3languagecorpusmanifest.java","m3/core/src/jdk/m3/text/M3CorpusSourceReceipt.java")){
   assertThrows(IllegalArgumentException.class,()->M3ScopeInference.describeJavaTarget(neighbor));assertEquals(false,path.invoke(null,neighbor));
  }
 }
}
