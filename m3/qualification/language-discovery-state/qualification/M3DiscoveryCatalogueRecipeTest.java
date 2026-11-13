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


import static org.junit.jupiter.api.Assertions.*;
public final class M3DiscoveryCatalogueRecipeTest {
 static final String CRATE="language-discovery-state";
 static final Map<String,String> qualified=new TreeMap<>();
 static Map<String,String> outputs(){return Map.copyOf(qualified);}
 static String resource(String n)throws IOException{try(var in=M3DiscoveryCatalogueRecipeTest.class.getResourceAsStream("/com/synexia/rewrite/hash-pinned-text/"+CRATE+"/"+n)){if(in==null)throw new IOException(n);return new String(in.readAllBytes(),StandardCharsets.UTF_8);}}
 static String[] row()throws IOException{return resource("manifest.tsv").stripTrailing().split("\t",-1);}
 static InMemoryExecutionContext context(List<Throwable> errors){return new InMemoryExecutionContext(errors::add);}
 static SourceFile source(String path,String text){return PlainText.builder().sourcePath(Path.of(path)).text(text).build();}
 static SourceFile before()throws IOException{var r=row();return source(r[0],resource(r[3]+".before"));}
 static void reject(List<SourceFile> files){var errors=new ArrayList<Throwable>();try{var run=new M3HashPinnedTextSnapshotRecipe(CRATE).run(new InMemoryLargeSourceSet(files),context(errors));assertTrue(run.getChangeset().getAllResults().isEmpty());assertFalse(errors.isEmpty());}catch(IllegalStateException expected){assertNotNull(expected.getMessage());}}
 @Test void replayAndFixedPoint()throws IOException{
  var errors=new ArrayList<Throwable>();var old=before();var recipe=new M3HashPinnedTextSnapshotRecipe(CRATE);
  var other=PlainText.builder().sourcePath(Path.of("unrelated.txt")).text("keep\n").build();
  var changes=recipe.run(new InMemoryLargeSourceSet(List.of(old,other)),context(errors)).getChangeset().getAllResults();
  assertTrue(errors.isEmpty(),errors.toString());assertEquals(1,changes.size());var after=changes.getFirst().getAfter();assertNotNull(after);
  assertEquals(old.getId(),after.getId());assertEquals(old.getSourcePath(),after.getSourcePath());assertEquals(resource(row()[3]),after.printAll());qualified.put(row()[0],after.printAll());
  var second=recipe.run(new InMemoryLargeSourceSet(List.of(after,other)),context(errors));assertTrue(second.getChangeset().getAllResults().isEmpty());assertTrue(errors.isEmpty());
 }
 @Test void missingDuplicateAndChangedSourcesRefuse()throws Exception{
  var old=before();reject(List.of());reject(List.of(old,old));reject(List.of(source(old.getSourcePath().toString(),old.printAll()+"# drift\n")));
 }
}
