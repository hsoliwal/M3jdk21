// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
import jdk.m3.precompute.*;
import jdk.m3.precompute.IndexPrecomputeManifest.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
@TestMethodOrder(MethodOrderer.MethodName.class)
public final class M3ManifestIdentityTest {
 static boolean after;static long checks,accepted,rejected;static volatile Object sink;
 static final MessageDigest D=sha();
 static MessageDigest sha(){try{return MessageDigest.getInstance("SHA-256");}catch(Exception e){throw new AssertionError(e);}}
 static String hash(String s){return HexFormat.of().formatHex(sha().digest(s.getBytes(StandardCharsets.UTF_8)));}
 static String enc(String s){return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
 static void check(boolean b,String label){checks++;assertTrue(b,label);D.update(label.getBytes(StandardCharsets.UTF_8));D.update((byte)0);}
 static SourceDescriptor sd(String id,String root,String type){return new SourceDescriptor(id,root,type);}
 static SourceDescriptor sd(String id){return sd(id,hash(id),"fixture");}
 static IndexPrecomputeManifest make(List<SourceDescriptor>s,List<SourceCapabilityBinding>b,List<String>p,Set<IndexPrecomputeCapability>c,long budget){
  return new IndexPrecomputeManifest("g",budget,0,false,0,0,0,false,s,b,List.of(),Map.of(),0,p,c,Map.of(),Map.of());
 }
 static IndexPrecomputeManifest modern(List<SourceDescriptor>s){return make(s,List.of(),List.of(),Set.of(),0);}
 static IndexPrecomputeManifest legacy(List<SourceDescriptor>s){return new IndexPrecomputeManifest("g",0,0,false,0,0,0,false,s,List.of(),Map.of(),0,List.of(),Set.of(),Map.of(),Map.of());}
 static String model(List<SourceDescriptor>s){
  var rows=new ArrayList<>(s);rows.sort(Comparator.comparing(SourceDescriptor::id).thenComparing(SourceDescriptor::rootHash).thenComparing(SourceDescriptor::sourceType));
  var out=new StringBuilder("G\tZw\nB\t0\t0\t0\t0\t0\t0\t0\t0\n");
  for(var x:rows)out.append("S\t").append(enc(x.id())).append('\t').append(x.rootHash()).append('\t').append(enc(x.sourceType())).append('\n');
  return out.toString();
 }
 static List<List<SourceDescriptor>> duplicates(){return List.of(
  List.of(sd("same"),sd("same")),
  List.of(sd("same","a".repeat(64),"t"),sd("same","b".repeat(64),"t")),
  List.of(sd("same","a".repeat(64),"t"),sd("same","a".repeat(64),"u")),
  List.of(sd(" same "),sd("same")),
  List.of(sd("z"),sd("a"),sd("a")),
  List.of(sd("b"),sd("a"),sd("b"),sd("a")));
 }
 @Test public void a_uniqueCanonicalRootsAndPermutationModel(){
  Random random=new Random(20261113L);
  for(int n:new int[]{0,1,2,8,32}){
   List<SourceDescriptor> rows=new ArrayList<>();for(int i=0;i<n;i++)rows.add(sd("source-"+i));
   String expected=model(rows);
   for(int pass=0;pass<12;pass++){
    Collections.shuffle(rows,random);
    for(var m:List.of(modern(rows),legacy(rows))){
     check(m.canonicalText().equals(expected),"canonical n="+n+" p="+pass);
     check(m.rootHash().equals(hash(expected)),"root n="+n+" p="+pass);
     var parsed=IndexPrecomputeManifest.parseCanonical(m.canonicalText());
     check(parsed.canonicalText().equals(expected)&&parsed.rootHash().equals(m.rootHash()),"roundtrip n="+n+" p="+pass);
    }
   }
  }
 }
 @Test public void b_ambiguousSourcesAtThreeIngresses(){
  for(var rows:duplicates()){
   String id=rows.stream().map(SourceDescriptor::id).sorted().filter(x->rows.stream().filter(y->y.id().equals(x)).count()>1).findFirst().orElseThrow();
   for(int mode=0;mode<3;mode++){
    IllegalArgumentException failure=null;
    try {if(mode==0)legacy(rows);else if(mode==1)modern(rows);else IndexPrecomputeManifest.parseCanonical(model(rows));}
    catch(IllegalArgumentException e){failure=e;}
    checks++;
    if(after){assertNotNull(failure);assertEquals("duplicate precompute source id "+id,failure.getMessage());rejected++;}
    else{assertNull(failure);accepted++;}
   }
  }
 }
 @Test public void c_bindingsAndKnownRefusalOrder(){
  var c=IndexPrecomputeCapability.PROVENANCE;var one=List.of(sd("u"));var selected=Set.of(c);
  var bindings=List.of(new SourceCapabilityBinding("u",c));
  var m=make(one,bindings,List.of("p"),selected,0);
  check(m.sourceCapabilityBindings().equals(bindings),"source capability bindings retained");
  check(IndexPrecomputeManifest.parseCanonical(m.canonicalText()).canonicalText().equals(m.canonicalText()),"binding canonical roundtrip");
  check(assertThrows(IllegalArgumentException.class,()->make(one,List.of(new SourceCapabilityBinding("absent",c)),List.of(),selected,0)).getMessage().equals("source capability binding references unknown source absent"),"unknown binding source refusal");
  check(assertThrows(IllegalArgumentException.class,()->make(one,bindings,List.of(),Set.of(),0)).getMessage().equals("source capability binding is not selected: PROVENANCE"),"unselected binding refusal");
  check(assertThrows(IllegalArgumentException.class,()->make(one,List.of(bindings.get(0),bindings.get(0)),List.of(),selected,0)).getMessage().equals("duplicate source capability binding: u:PROVENANCE"),"duplicate binding refusal");
  for(var rows:List.of(one,duplicates().get(0))){
   check(assertThrows(IllegalArgumentException.class,()->make(rows,List.of(),List.of(),Set.of(),-1)).getMessage().equals("negative manifest scalar"),"negative scalar precedes identity");
   check(assertThrows(IllegalArgumentException.class,()->make(rows,List.of(),List.of("p","p"),Set.of(),0)).getMessage().equals("duplicate provider id p"),"duplicate provider precedes identity");
  }
 }
 @Test public void d_inputOwnershipAndPublicApi(){
  var input=new ArrayList<>(List.of(sd("u")));var m=modern(input);String root=m.rootHash();input.add(sd("v"));
  check(m.sources().size()==1&&m.rootHash().equals(root),"input list copied");
  check(assertThrows(UnsupportedOperationException.class,()->m.sources().add(sd("z")))!=null,"sources immutable");
  check(assertThrows(UnsupportedOperationException.class,()->m.sourceCapabilityBindings().clear())!=null,"bindings immutable");
  check(IndexPrecomputeManifest.class.getConstructors().length==2,"legacy and bound constructor signatures retained");
  check(Arrays.stream(SourceCapabilityBinding.class.getRecordComponents()).map(x->x.getName()).toList().equals(List.of("sourceId","capability")),"binding API unchanged; full owner association still separate");
 }
 @Test public void e_unicodeAndNormalizationIdentity(){
  var rows=List.of(sd("é"),sd("e\u0301"),sd("x\0y"),sd("中"),sd("ID"),sd("id"));
  var m=modern(rows);
  check(m.sources().size()==6,"distinct code-unit identities retained");
  check(m.canonicalText().equals(model(rows))&&m.rootHash().equals(hash(model(rows))),"unicode canonical identity");
  check(sd(" trimmed ").id().equals("trimmed"),"existing descriptor trim preserved");
  check(assertThrows(NullPointerException.class,()->sd(null,"a".repeat(64),"t"))!=null,"null ID refusal");
  check(assertThrows(IllegalArgumentException.class,()->sd(" ")).getMessage().equals("blank source id"),"blank ID refusal");
  check(assertThrows(IllegalArgumentException.class,()->sd("u","bad","t"))!=null,"invalid root refusal");
 }
 @Test public void f_noncanonicalRefusalsPreserved(){
  String value=modern(List.of(sd("u"))).canonicalText();
  for(String bad:List.of(value+"\n",value.replace("G\tZw","G\tZw=="),value.replace("S\t","Z\t"))){
   check(assertThrows(IllegalArgumentException.class,()->IndexPrecomputeManifest.parseCanonical(bad))!=null,"noncanonical canonical parser refusal");
  }
 }
 static String api(){var out=new ArrayList<String>();for(var m:IndexPrecomputeManifest.class.getDeclaredMethods())if(java.lang.reflect.Modifier.isPublic(m.getModifiers()))out.add(m.toGenericString());for(var c:IndexPrecomputeManifest.class.getConstructors())out.add(c.toGenericString());Collections.sort(out);return String.join("|",out);}
 static String digest(){return HexFormat.of().formatHex(D.digest());}
 static void measure(){
  var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
  if(!bean.isThreadAllocatedMemorySupported())throw new AssertionError("allocation counter unavailable");
  if(!bean.isThreadAllocatedMemoryEnabled())bean.setThreadAllocatedMemoryEnabled(true);
  long tid=Thread.currentThread().threadId();int iterations=100;
  for(int n:new int[]{1,32}){
   var rows=new ArrayList<SourceDescriptor>();for(int i=0;i<n;i++)rows.add(sd("source-"+i));
   for(int i=0;i<200;i++){var m=modern(rows);sink=IndexPrecomputeManifest.parseCanonical(m.canonicalText());}
   for(int sample=0;sample<5;sample++){
    long bytes=bean.getThreadAllocatedBytes(tid),start=System.nanoTime();
    for(int i=0;i<iterations;i++){var m=modern(rows);sink=IndexPrecomputeManifest.parseCanonical(m.canonicalText());}
    long nanos=System.nanoTime()-start,allocated=bean.getThreadAllocatedBytes(tid)-bytes;
    System.out.println("M3_MANIFEST_IDENTITY_MEASURE\t"+n+"\t"+sample+"\t"+iterations+"\t"+nanos+"\t"+allocated);
   }
  }
 }
}
