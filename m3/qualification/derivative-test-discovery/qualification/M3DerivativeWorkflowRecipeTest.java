// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Parser;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.tree.Yaml;
import static org.junit.jupiter.api.Assertions.*;
public final class M3DerivativeWorkflowRecipeTest {
 static final String CRATE="m3jdk21-derivative-test-discovery";
 static final Map<String,String> qualified=new TreeMap<>();
 static Map<String,String> outputs(){return Map.copyOf(qualified);}
 static String resource(String n)throws IOException{try(var in=M3DerivativeWorkflowRecipeTest.class.getResourceAsStream("/com/synexia/rewrite/hash-pinned-yaml/"+CRATE+"/"+n)){if(in==null)throw new IOException(n);return new String(in.readAllBytes(),StandardCharsets.UTF_8);}}
 static String[] row()throws IOException{return resource("manifest.tsv").stripTrailing().split("\t",-1);}
 static InMemoryExecutionContext context(List<Throwable> errors){return new InMemoryExecutionContext(errors::add);}
 static SourceFile source(String path,String text){
  var errors=new ArrayList<Throwable>();
  var parsed=YamlParser.builder().build().parseInputs(List.of(Parser.Input.fromString(Path.of(path),text)),null,context(errors)).toList();
  assertTrue(errors.isEmpty(),errors.toString());assertEquals(1,parsed.size());assertInstanceOf(Yaml.Documents.class,parsed.getFirst());assertEquals(text,parsed.getFirst().printAll());return parsed.getFirst();
 }
 static SourceFile before()throws IOException{var r=row();return source(r[0],resource(r[3]+".before"));}
 static void reject(List<SourceFile> files){var errors=new ArrayList<Throwable>();try{var run=new M3HashPinnedYamlSnapshotRecipe(CRATE).run(new InMemoryLargeSourceSet(files),context(errors));assertTrue(run.getChangeset().getAllResults().isEmpty());assertFalse(errors.isEmpty());}catch(IllegalStateException expected){assertNotNull(expected.getMessage());}}
 @Test void replayAndFixedPoint()throws IOException{
  var errors=new ArrayList<Throwable>();var old=before();var recipe=new M3HashPinnedYamlSnapshotRecipe(CRATE);
  var other=PlainText.builder().sourcePath(Path.of("unrelated.txt")).text("keep\n").build();
  var changes=recipe.run(new InMemoryLargeSourceSet(List.of(old,other)),context(errors)).getChangeset().getAllResults();
  assertTrue(errors.isEmpty(),errors.toString());assertEquals(1,changes.size());var after=changes.getFirst().getAfter();assertNotNull(after);assertInstanceOf(Yaml.Documents.class,after);
  assertEquals(old.getId(),after.getId());assertEquals(old.getSourcePath(),after.getSourcePath());assertEquals(resource(row()[3]),after.printAll());qualified.put(row()[0],after.printAll());
  var second=recipe.run(new InMemoryLargeSourceSet(List.of(after,other)),context(errors));assertTrue(second.getChangeset().getAllResults().isEmpty());assertTrue(errors.isEmpty());
 }
 @Test void missingRequiredRefuses(){reject(List.of());}
 @Test void duplicateRefuses()throws IOException{var old=before();reject(List.of(old,old));}
 @Test void sourceDriftRefuses()throws IOException{var old=before();reject(List.of(source(old.getSourcePath().toString(),old.printAll()+"# drift\n")));}
 @Test void wrongAstRefuses()throws IOException{var old=before();reject(List.of(PlainText.builder().sourcePath(old.getSourcePath()).text(old.printAll()).build()));}
 @Test void caseAliasRefuses()throws IOException{reject(List.of(before().withSourcePath(Path.of(".github/workflows/M3-DERIVATIVE-REGEX-PORT.yml"))));}
 @Test void noncanonicalPathRefuses()throws IOException{reject(List.of(before().withSourcePath(Path.of(".github/workflows/../workflows/m3-derivative-regex-port.yml"))));}
 @Test void changedAfterScanRefuses()throws IOException{
  var errors=new ArrayList<Throwable>();var ctx=context(errors);var r=new M3HashPinnedYamlSnapshotRecipe(CRATE);var inventory=r.getInitialValue(ctx);var old=before();r.getScanner(inventory).visit(old,ctx);var visitor=r.getVisitor(inventory);
  var changed=source(old.getSourcePath().toString(),old.printAll()+"# moved\n");var failure=assertThrows(RuntimeException.class,()->visitor.visit(changed,ctx));Throwable cause=failure;while(cause.getCause()!=null)cause=cause.getCause();assertInstanceOf(IllegalStateException.class,cause);assertEquals("target changed after scan: "+row()[0],cause.getMessage());
 }
 @Test void selectorsBindToPinnedReceivingClasses()throws IOException{
  var names=new ArrayList<String>();for(String file:List.of("RegexDerivativeTest.java.txt","RegexDerivativeEffectsBridgeTest.java.txt")){
   var m=Pattern.compile("(?m)^final class ([A-Za-z0-9_]+) \\{").matcher(resource(file));assertTrue(m.find());names.add(m.group(1));assertFalse(m.find());
  }
  String after=resource(row()[3]),expected=String.join(",",names);var selectors=Pattern.compile("-Dtest=([^ ]+) test").matcher(after);int count=0;
  while(selectors.find()){assertEquals(expected,selectors.group(1));count++;}assertEquals(2,count);
  assertFalse(after.contains("failIfNoSpecifiedTests=false"));assertFalse(after.contains("continue-on-error"));assertTrue(after.contains("-Pnative"));
 }
}
