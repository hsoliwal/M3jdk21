// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
// Maintenance QA: real compiled owner, preserved admission fixtures, C-donor oracle.
package jdk.m3.text;
import java.io.*;import java.lang.invoke.*;import java.lang.reflect.*;import java.math.BigInteger;
import java.net.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.security.*;
import java.util.*;import org.junit.jupiter.api.Test;import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;import org.objectweb.asm.*;
import static org.junit.jupiter.api.Assertions.*;import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
public final class M3IdentityRangeQualification {
 static String mode;static Path image;static String firstText;static MethodHandle slot;static volatile long sink;
 static final List<Map<String,Object>> measurements=new ArrayList<>();
 static final Map<String,Object> evidence=new LinkedHashMap<>();
 static Field field(String name)throws Exception{var f=SharedLexiconCatalog.class.getDeclaredField(name);f.setAccessible(true);return f;}
 static Object get(Object o,String name)throws Exception{return field(name).get(o);}
 static int initial(String source,String record,int length)throws Throwable{return (int)slot.invokeExact(source,record,length);}
 static String sha(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
 static String text(String encoded){return encoded.equals("-")?null:new String(Base64.getDecoder().decode(encoded),StandardCharsets.UTF_8);}
 static byte[] ownerBytes()throws Exception{
  ClassLoader loader=M3IdentityRangeQualification.class.getClassLoader();
  for(var f:loader.getClass().getDeclaredFields()){if(!Map.class.isAssignableFrom(f.getType()))continue;f.setAccessible(true);
   if(f.get(loader) instanceof Map<?,?> m && m.get("jdk.m3.text.SharedLexiconCatalog") instanceof ByteArrayOutputStream out)return out.toByteArray();}
  throw new AssertionError("RAM compiler owner bytes");
 }
 record Fixture(SharedLexiconCatalog catalog,SharedLexiconCatalog base,int added) implements AutoCloseable {
  public void close(){catalog.close();base.close();}
 }
 static Fixture fixture(List<SharedLexiconCatalog.SourceMapping> extras)throws Exception{
  var create=SharedLexiconCatalogSiWarmTest.class.getDeclaredMethod("fixture",Path.class,boolean.class,boolean.class);create.setAccessible(true);
  Object original=create.invoke(null,image,false,false);var accessor=original.getClass().getDeclaredMethod("catalog");accessor.setAccessible(true);
  var base=(SharedLexiconCatalog)accessor.invoke(original);
  var mappings=new HashMap<SharedLexiconCatalog.Coordinate,List<SharedLexiconCatalog.SourceMapping>>();
  var facts=new HashMap<SharedLexiconCatalog.Coordinate,SharedLexiconCatalog.PrecomputeFacts>();
  for(int row=0;row<base.recordCount();row++){var c=new SharedLexiconCatalog.Coordinate(0,row);var values=new ArrayList<>(base.mappingsAt(c));if(row==0)values.addAll(extras);mappings.put(c,values);facts.put(c,base.precomputeAt(c));}
  var ctor=SharedLexiconCatalog.class.getDeclaredConstructors()[0];assertEquals(10,ctor.getParameterCount());ctor.setAccessible(true);
  try{var catalog=(SharedLexiconCatalog)ctor.newInstance(get(base,"shards"),get(base,"files"),get(base,"firstLexemes"),get(base,"lastLexemes"),mappings,facts,base.precomputeProfiles(),Optional.empty(),Optional.empty(),base.recordCount());return new Fixture(catalog,base,extras.size());}
  catch(Throwable failure){base.close();throw failure;}
 }
 static SharedLexiconCatalog.SourceMapping mapping(String source,String record,String payload){
  return new SharedLexiconCatalog.SourceMapping(source,"memory","range-qualification","und",record,firstText,new SharedLexiconCatalog.Coordinate(0,0),record,"NAME_"+record,"none","si",payload);
 }
 static List<SharedLexiconCatalog.SourceMapping> numbers(int count){
  var result=new ArrayList<SharedLexiconCatalog.SourceMapping>();for(int i=0;i<count;i++)result.add(mapping("numbers",Integer.toString(i),"{\"n\":"+i+"}"));return result;
 }
 static SharedLexiconCatalog.SourceMapping[] index(SharedLexiconCatalog catalog)throws Exception{return (SharedLexiconCatalog.SourceMapping[])get(catalog,"mappingsByIdentity");}
 static Map<String,Object> probes(SharedLexiconCatalog catalog,List<Query> queries)throws Throwable{
  var index=index(catalog);long total=0;int max=0,wraps=0;
  for(var q:queries){int at=initial(q.source(),q.record(),index.length),count=0;while(index[at]!=null){count++;var value=index[at];if(q.source().equals(value.sourceId())&&q.record().equals(value.recordId()))break;if(++at==index.length){at=0;wraps++;}assertTrue(count<index.length);}
   if(index[at]==null)count++;total+=count;max=Math.max(max,count);
  }
  return Map.of("queries",queries.size(),"probes",total,"maxProbes",max,"wrappedSteps",wraps,"slots",index.length);
 }
 @Test void donorPortAndBigIntegerAgree()throws Throwable{
  int checked=0,different=0;
  try(var in=M3IdentityRangeQualification.class.getResourceAsStream("/range-oracle.tsv")){
   assertNotNull(in);for(String line:new String(in.readAllBytes(),StandardCharsets.UTF_8).split("\n")){if(line.isEmpty())continue;var f=line.split("\t",-1);assertEquals(5,f.length);
    String source=text(f[0]),record=text(f[1]);int length=Integer.parseInt(f[2]);long word=Long.parseLong(f[3]);int oracle=Integer.parseInt(f[4]);
    int hash=31*Objects.hashCode(source)+Objects.hashCode(record),mixed=hash*0x9E3779B9,folded=mixed^(mixed>>>16);
    assertEquals(word,Integer.toUnsignedLong(folded),"same exact C oracle input");
    int exact=BigInteger.valueOf(word).multiply(BigInteger.valueOf(length)).shiftRight(32).intValueExact();assertEquals(exact,oracle);
    int actual=initial(source,record,length);assertTrue(actual>=0&&actual<length);
    assertEquals(mode.equals("after")?oracle:Math.floorMod(folded,length),actual);
    if(actual!=oracle)different++;checked++;
   }
  }
  assertTrue(checked>=12000);assertEquals(mode.equals("after"),different==0);evidence.put("oracleVectors",checked);evidence.put("differentFromDonor",different);
 }
 @Test void compiledArithmeticAndApiAreBound()throws Exception{
  int[] operations=new int[3];var apiSurface=new TreeSet<String>();
  new ClassReader(ownerBytes()).accept(new ClassVisitor(Opcodes.ASM9){
   @Override public FieldVisitor visitField(int access,String name,String descriptor,String signature,Object value){apiSurface.add("F:"+access+":"+name+":"+descriptor);return null;}
   @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
    if((access&(Opcodes.ACC_PUBLIC|Opcodes.ACC_PROTECTED))!=0)apiSurface.add("M:"+access+":"+name+descriptor);
    if(!name.equals("identitySlot"))return null;assertEquals("(Ljava/lang/String;Ljava/lang/String;I)I",descriptor);
    return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int opcode,String owner,String name,String desc,boolean itf){if(owner.equals("java/lang/Math")&&name.equals("floorMod"))operations[0]++;}
     @Override public void visitInsn(int opcode){if(opcode==Opcodes.LMUL)operations[1]++;if(opcode==Opcodes.LUSHR)operations[2]++;}};
   }
  },ClassReader.SKIP_DEBUG);
  assertArrayEquals(mode.equals("after")?new int[]{0,1,1}:new int[]{1,0,0},operations);
  evidence.put("apiAndFieldFingerprint",sha(String.join("\n",apiSurface).getBytes(StandardCharsets.UTF_8)));evidence.put("floorModCalls",operations[0]);
 }
 @Test void numericAndLanguageIdentitiesRemainExact()throws Throwable{
  var values=new ArrayList<>(numbers(10001));for(String language:List.of("en","de","hi-देव","ar-عرب","zh-汉","el-Ελ","ja-かな"))for(int i=0;i<31;i++)values.add(mapping("language:"+language,"record/"+i,"{}"));
  try(var f=fixture(values)){var idx=index(f.catalog());int count=0;for(var value:idx)if(value!=null)count++;assertEquals(count*2,idx.length);
   for(var value:values)assertSame(value,f.catalog().findMapping(value.sourceId(),value.recordId()).orElseThrow());
   assertTrue(f.catalog().findMapping("numbers","10001").isEmpty());assertTrue(f.catalog().findMapping("NUMBERS","1").isEmpty());assertTrue(f.catalog().findMapping("numbers","01").isEmpty());
   assertThrows(NullPointerException.class,()->f.catalog().findMapping(null,"1"));assertThrows(NullPointerException.class,()->f.catalog().findMapping("numbers",null));
   evidence.put("identityCount",values.size());evidence.put("sameIdentitySlots",idx.length);
  }
 }
 @Test void collidingKeysAndWrappedProbesRemainComplete()throws Throwable{
  var ids=new ArrayList<String>();for(int n=0;n<64;n++){var b=new StringBuilder();for(int bit=0;bit<6;bit++)b.append((n&(1<<bit))==0?"Aa":"BB");ids.add(b.toString());}
  assertEquals(1,ids.stream().map(String::hashCode).distinct().count());int wrapped=0;long comparisons=0;
  for(int group=0;group<32;group++){String source="collision/"+group;var values=new ArrayList<SharedLexiconCatalog.SourceMapping>();var queries=new ArrayList<Query>();
   for(String id:ids){values.add(mapping(source,id,"{}"));queries.add(new Query(source,id));}
   try(var f=fixture(values)){for(var value:values)assertSame(value,f.catalog().findMapping(source,value.recordId()).orElseThrow());
    assertTrue(f.catalog().findMapping(source+"other",ids.getFirst()).isEmpty());var p=probes(f.catalog(),queries);wrapped+=((Number)p.get("wrappedSteps")).intValue();comparisons+=((Number)p.get("probes")).longValue();}
  }
  assertTrue(wrapped>0,"actual collision chain wraps array end");evidence.put("collisionProbeSteps",comparisons);evidence.put("wrappedProbeSteps",wrapped);
 }
 @Test void payloadsKeepFullSourceIdentityOnOneImageRow()throws Exception{
  var values=numbers(64);try(var f=fixture(values)){M3PrecomputePayload first=f.catalog().findPrecomputePayload("numbers","1").orElseThrow(),second=f.catalog().findPrecomputePayload("numbers","2").orElseThrow();
   assertEquals(1,first.requireInt("n"));assertEquals(2,second.requireInt("n"));assertNotSame(first,second);assertSame(first,f.catalog().findPrecomputePayload("numbers","1").orElseThrow());
   assertTrue(f.catalog().findPrecomputePayload("other","1").isEmpty());
  }
 }
 @Test void incumbentSiSemantics()throws Exception{SharedLexiconCatalogSiWarmTest.semantics(image);}
 @Test void incumbentSiWarmLoaderRunsOnce()throws Exception{SharedLexiconCatalogSiWarmTest.warm(image);}
 @Test void incumbentSiConcurrencyAndCloseWait()throws Exception{SharedLexiconCatalogSiWarmTest.concurrent(image);}
 @Test void escapedPayloadSurvivesCloseAndNewLookupRefuses()throws Exception{
  var f=fixture(numbers(4));var payload=f.catalog().findPrecomputePayload("numbers","3").orElseThrow();String before=payload.canonicalJson();f.close();
  assertEquals(3,payload.requireInt("n"));assertEquals(before,payload.canonicalJson());assertThrows(IllegalStateException.class,()->f.catalog().findMapping("numbers","3"));assertThrows(IllegalStateException.class,()->f.catalog().findPrecomputePayload("numbers","3"));
 }
 @Test void missKeepsPayloadCacheCold()throws Exception{
  try(var f=fixture(numbers(4))){for(int i=4;i<100;i++)assertTrue(f.catalog().findPrecomputePayload("numbers",Integer.toString(i)).isEmpty());assertTrue(((Map<?,?>)get(f.catalog(),"payloads")).isEmpty());}
 }
 record Query(String source,String record){}
 static long batch(SharedLexiconCatalog catalog,Query[] queries,int calls){
  long sum=0;for(int i=0;i<calls;i++){var q=queries[i&8191];var value=catalog.findMapping(q.source(),q.record());sum+=value.isPresent()?value.orElseThrow().recordId().hashCode():17;}sink=sum;return sum;
 }
 static void measure()throws Throwable{
  var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();assertTrue(bean.isThreadAllocatedMemorySupported());bean.setThreadAllocatedMemoryEnabled(true);
  var values=numbers(10001);for(String scenario:List.of("numeric-hits","numeric-misses","mixed-language")){
   var extra=new ArrayList<>(values);if(scenario.equals("mixed-language"))for(int n=0;n<10001;n++)extra.add(mapping("language:हिन्दी","पद/"+n,"{}"));
   try(var f=fixture(extra)){var random=new Random(0x4d3352414e47454cL);var q=new Query[8192];
    for(int i=0;i<q.length;i++){int n=random.nextInt(10001);q[i]=scenario.equals("numeric-misses")?new Query("numbers","absent/"+n):scenario.equals("mixed-language")&&(i&1)==0?new Query("language:हिन्दी","पद/"+n):new Query("numbers",Integer.toString(n));q[i].source().hashCode();q[i].record().hashCode();}
    for(int warm=0;warm<15;warm++)batch(f.catalog(),q,200000);
    var ps=probes(f.catalog(),Arrays.asList(q));evidence.put(scenario+"Probes",ps);
    for(int sample=0;sample<7;sample++){long startBytes=bean.getThreadAllocatedBytes(Thread.currentThread().threadId()),start=System.nanoTime();long answer=batch(f.catalog(),q,1000000);long nanos=System.nanoTime()-start,bytes=bean.getThreadAllocatedBytes(Thread.currentThread().threadId())-startBytes;measurements.add(Map.of("scenario",scenario,"sample",sample,"calls",1000000,"nanos",nanos,"bytes",bytes,"answer",answer));}
   }
  }
 }
 public static void main(String[] a)throws Throwable{
  mode=a[0];image=Path.of(a[1]);firstText=SharedLexiconImage.open(image).recordText(0);slot=MethodHandles.privateLookupIn(SharedLexiconCatalog.class,MethodHandles.lookup()).findStatic(SharedLexiconCatalog.class,"identitySlot",MethodType.methodType(int.class,String.class,String.class,int.class));
  byte[] before=MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(image));
  var vm=java.lang.management.ManagementFactory.getPlatformMXBean(com.sun.management.HotSpotDiagnosticMXBean.class);for(String option:List.of("UsePerfData","PerfDataSaveToFile"))assertEquals("false",vm.getVMOption(option).getValue());assertEquals("true",vm.getVMOption("PerfDisableSharedMem").getValue());
  var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);launcher.execute(LauncherDiscoveryRequestBuilder.request().selectors(selectClass(M3IdentityRangeQualification.class)).build());
  var s=listener.getSummary();s.printTo(new PrintWriter(System.out,true));s.printFailuresTo(new PrintWriter(System.out,true));if(s.getTestsFoundCount()!=10||s.getTestsSucceededCount()!=10||s.getTestsFailedCount()!=0||s.getTestsAbortedCount()!=0||s.getTestsSkippedCount()!=0)throw new AssertionError("qualification counts");
  measure();assertArrayEquals(before,MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(image)));
  var result=new LinkedHashMap<String,Object>();result.put("mode",mode);result.put("found",10);result.put("passed",10);result.put("failed",0);result.put("aborted",0);result.put("evidence",evidence);result.put("measurements",measurements);
  byte[] bytes=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(result);
  var c=(HttpURLConnection)URI.create(a[2]).toURL().openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setFixedLengthStreamingMode(bytes.length);try(var out=c.getOutputStream()){out.write(bytes);}if(c.getResponseCode()!=204)throw new AssertionError("receipt");c.disconnect();
  System.out.println("M3_IDENTITY_RANGE_QUALIFICATION_PASS mode="+mode+" tests=10");
 }
}
