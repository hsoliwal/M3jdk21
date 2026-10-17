/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.FindAndReplace;

/** Actual hash-sealed OpenRewrite Java output, bounded replay, and metadata recipe proof. */
final class M3FiveProviderDirectRecipeTest {
  private static final String CRATE="five-provider-direct-integration";
  private static final String ROOT="/com/synexia/rewrite/hash-pinned-java/"+CRATE+"/";
  private static final String TEXT="/com/synexia/rewrite/hash-pinned-text/five-provider-direct-bindings/";
  private static final String FACTORY="m3/collections/interop/src/main/java/com/m3/collections/interop/M3LibraryCollections.java";
  private static final String BINDING="m3/collections/interop/sdk-family-bindings.tsv";
  private record Target(String path,String before,String after,String text) { }
  private static String read(String p) throws Exception {
    try(var in=M3FiveProviderDirectRecipeTest.class.getResourceAsStream(p)){
      assertNotNull(in,p);return new String(in.readAllBytes(),StandardCharsets.UTF_8);
    }
  }
  private static String sha(String s) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
        .digest(s.getBytes(StandardCharsets.UTF_8)));
  }
  private static List<Target> all() throws Exception {
    var result=new ArrayList<Target>();
    for(var row:read(ROOT+"manifest.tsv").lines().toList()){
      if(row.isEmpty()||row.startsWith("#"))continue;
      var p=row.split("\t",-1);assertEquals(4,p.length);
      var body=read(ROOT+p[3]);assertEquals(p[2],sha(body));
      result.add(new Target(p[0],p[1],p[2],body));
    }
    assertEquals(8,result.size());
    assertEquals(FACTORY,result.get(0).path());
    assertEquals(1,result.stream().filter(p->!p.before().equals("ABSENT")).count());
    assertEquals(result.stream().map(Target::path).sorted().toList(),
        result.stream().map(Target::path).toList());
    assertEquals(result.get(0).before(),sha(read("/preimage/M3LibraryCollections.java.txt")));
    return result;
  }
  private static Map<String,String> run(Map<String,String> installed)throws Exception {
    var inputs=new ArrayList<SourceFile>();
    installed.forEach((p,s)->inputs.add(PlainText.builder().sourcePath(Path.of(p)).text(s).build()));
    var errors=new ArrayList<Throwable>();
    var results=new M3HashPinnedJavaSnapshotRecipe(CRATE,false).run(
      new InMemoryLargeSourceSet(inputs),new InMemoryExecutionContext(errors::add),1)
        .getChangeset().getAllResults();
    assertTrue(errors.isEmpty(),()->"OpenRewrite errors "+errors);
    var output=new TreeMap<String,String>();
    for(var v:results){
      assertInstanceOf(PlainText.class,v.getAfter());
      assertNull(output.put(v.getAfter().getSourcePath().toString(),v.getAfter().printAll()));
    }
    return output;
  }
  @Test void firstGenerationAndFixedPoint() throws Exception {
    var expected=all();
    var before=read("/preimage/M3LibraryCollections.java.txt");
    var output=run(Map.of(FACTORY,before));
    assertEquals(8,output.size());
    for(var target:expected)assertEquals(target.after(),sha(output.get(target.path())));
    assertTrue(run(output).isEmpty());
    assertTrue(output.get("m3/collections/src/main/java/com/m3/collections/M3ArrayList.java")
        .contains("private Object[]"));
    assertTrue(output.get("m3/collections/interop/src/main/java/com/m3/collections/interop/M3SpringArrayMultiValueMap.java")
        .contains("implements MultiValueMap<K, V>"));
  }
  @Test void partialResumesAndUnrelatedInputs() throws Exception {
    var expected=all();
    for(int variant=0;variant<5;variant++) {
      var inputs=new LinkedHashMap<String,String>();
      inputs.put(FACTORY,variant==0?read("/preimage/M3LibraryCollections.java.txt"):expected.get(0).text());
      for(int i=1;i<expected.size();i++)if((i+variant)%3==0)inputs.put(expected.get(i).path(),expected.get(i).text());
      inputs.put("unrelated.txt","retained\n");
      var output=run(inputs);inputs.putAll(output);
      assertTrue(run(inputs).isEmpty());
      for(var x:expected)assertEquals(x.after(),sha(inputs.get(x.path())));
      assertEquals("retained\n",inputs.get("unrelated.txt"));
    }
  }
  @Test void missingDriftAndDuplicateFailClosed() throws Exception {
    var r=new M3HashPinnedJavaSnapshotRecipe(CRATE,false);
    var context=new InMemoryExecutionContext();
    assertThrows(IllegalStateException.class,()->r.generate(r.getInitialValue(context),context));
    var drift=r.getInitialValue(context);
    r.getScanner(drift).visit(PlainText.builder().sourcePath(Path.of(FACTORY)).text("tampered").build(),context);
    assertThrows(IllegalStateException.class,()->r.generate(drift,context));
    var duplicates=r.getInitialValue(context);
    var src=PlainText.builder().sourcePath(Path.of(FACTORY))
        .text(read("/preimage/M3LibraryCollections.java.txt")).build();
    r.getScanner(duplicates).visit(src,context);r.getScanner(duplicates).visit(src,context);
    assertThrows(IllegalStateException.class,()->r.generate(duplicates,context));
  }
  @Test void bindingRecipeMustProduceOneExactNewOwnerName() throws Exception {
    var row=read(TEXT+"manifest.tsv").strip().split("\t",-1);
    assertEquals(4,row.length);assertEquals(BINDING,row[0]);
    var old=read(TEXT+"sdk-family-bindings.tsv.before");
    var updated=read(TEXT+"sdk-family-bindings.tsv.after");
    assertEquals(row[1],sha(old));assertEquals(row[2],sha(updated));
    var find="com.m3.collections.interop.M3SpringMultiValueMap";
    var replace="com.m3.collections.interop.M3SpringArrayMultiValueMap";
    assertEquals(1,old.split(find,-1).length-1);
    var rewrite=new FindAndReplace(find,replace,false,true,false,false,
        "**/sdk-family-bindings.tsv");
    var file=PlainText.builder().sourcePath(Path.of(BINDING)).text(old).build();
    var diff=rewrite.run(new InMemoryLargeSourceSet(List.of(file)),
        new InMemoryExecutionContext(),1).getChangeset().getAllResults();
    assertEquals(1,diff.size());
    assertEquals(updated,diff.get(0).getAfter().printAll());
    assertTrue(rewrite.run(new InMemoryLargeSourceSet(List.of(diff.get(0).getAfter())),
        new InMemoryExecutionContext(),1).getChangeset().getAllResults().isEmpty());
  }
}
