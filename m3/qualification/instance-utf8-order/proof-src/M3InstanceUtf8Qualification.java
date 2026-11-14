// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
// CPU qualification of actual index and its existing public views.
import jdk.m3.text.M3InstanceIndexPrecompute;
import jdk.m3.text.M3InstanceIndexPrecompute.InstanceRecord;
import jdk.m3.text.M3InstanceIndexPrecompute.InstanceType;
import java.io.*;import java.lang.invoke.*;import java.lang.reflect.*;import java.net.*;
import java.nio.charset.*;import java.security.*;import java.util.*;import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;import org.junit.platform.launcher.core.*;import org.junit.platform.launcher.listeners.*;import org.objectweb.asm.*;
import static org.junit.jupiter.api.Assertions.*;import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
public final class M3InstanceUtf8Qualification {
 static String mode;static volatile long sink;static volatile Object ownerSink;
 static final Map<String,Object> evidence=new LinkedHashMap<>();
 static final List<Map<String,Object>> measurements=new ArrayList<>();
 static final MethodHandle CMP=comparatorHandle();
 static MethodHandle comparatorHandle(){try{return MethodHandles.privateLookupIn(M3InstanceIndexPrecompute.class,MethodHandles.lookup()).findStatic(M3InstanceIndexPrecompute.class,"compareUtf8",MethodType.methodType(int.class,String.class,String.class));}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 static int compare(String a,String b)throws Throwable{return (int)CMP.invokeExact(a,b);}
 static int reference(String left,String right){
  byte[] leftBytes=left.getBytes(StandardCharsets.UTF_8),rightBytes=right.getBytes(StandardCharsets.UTF_8);int limit=Math.min(leftBytes.length,rightBytes.length);
  for(int at=0;at<limit;at++){int a=leftBytes[at]&255,b=rightBytes[at]&255;if(a!=b)return Integer.compare(a,b);}return Integer.compare(leftBytes.length,rightBytes.length);
 }
 static void pair(String left,String right)throws Throwable{assertEquals(reference(left,right),compare(left,right));}
 static boolean wellFormed(String s){for(int i=0;i<s.length();i++){char c=s.charAt(i);if(Character.isHighSurrogate(c)){if(++i>=s.length()||!Character.isLowSurrogate(s.charAt(i)))return false;}else if(Character.isLowSurrogate(c))return false;}return true;}
 static String sha(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
 static Map<String,byte[]> classes()throws Exception{
  var loader=M3InstanceUtf8Qualification.class.getClassLoader();for(var f:loader.getClass().getDeclaredFields()){if(!Map.class.isAssignableFrom(f.getType()))continue;f.setAccessible(true);if(f.get(loader) instanceof Map<?,?>m&&m.get("jdk.m3.text.M3InstanceIndexPrecompute") instanceof ByteArrayOutputStream){
   var out=new TreeMap<String,byte[]>();for(var e:m.entrySet())if(e.getKey() instanceof String n&&n.startsWith("jdk.m3.text.M3InstanceIndexPrecompute")&&e.getValue() instanceof ByteArrayOutputStream b)out.put(n,b.toByteArray());return out;}}
  throw new AssertionError("RAM owner classbytes");
 }
 @Test void incumbentBaseContract(){M3InstanceIndexPrecomputeTest.main(new String[0]);}
 @Test void incumbentEdgeContract(){M3InstanceIndexPrecomputeEdgeContractTest.main(new String[0]);}
 @Test void everyUtf16UnitIncludingMalformed()throws Throwable{
  long cases=0;for(int c=0;c<=65535;c++){String s=String.valueOf((char)c);for(String anchor:List.of("","?","\uD800","\uDC00","\uE000","😀")){pair(s,anchor);pair("prefix/"+s,"prefix/"+anchor);cases+=2;}}
  evidence.put("utf16Units",65536);evidence.put("unitPairs",cases);
 }
 @Test void allScalarCodePointsAndDonorOrder()throws Throwable{
  int scalars=0;String previous="";for(int cp=0;cp<=Character.MAX_CODE_POINT;cp++){
   if(cp>=Character.MIN_SURROGATE&&cp<=Character.MAX_SURROGATE)continue;String s=new String(Character.toChars(cp));
   pair(s,"\uFFFD");pair(s,s+"x");if(cp!=0)assertTrue(compare(previous,s)<0);
   assertEquals(reference(s,"\uFFFD"),M3InstanceUtf8DonorOracle.compare(s,"\uFFFD"));previous=s;scalars++;
  }evidence.put("validScalars",scalars);
 }
 @Test void prefixSurrogateAndEncodingBoundaryMatrix()throws Throwable{
  var strings=List.of("","?","??","\u0000","\u007F","\u0080","\u07FF","\u0800","\uD7FF","\uD800","\uD800x","\uDBFF","\uDC00","\uDFFF","\uE000","\uFFFF","😀","😁","\uDBFF\uDFFF","ab","ab?","ab\uD800","ab😀","ab😀x","e\u0301","\u00e9");
  int cases=0;for(String a:strings)for(String b:strings){pair(a,b);assertEquals(-Integer.signum(compare(b,a)),Integer.signum(compare(a,b)));if(wellFormed(a)&&wellFormed(b))assertEquals(reference(a,b),M3InstanceUtf8DonorOracle.compare(a,b));cases++;}
  assertEquals(0,compare("\uD800","?"));assertEquals(0,compare("\uDC00","?"));assertEquals(0,compare("\uD800\uD800","??"));assertTrue(compare("\uFFFD","😀")<0);evidence.put("edgePairs",cases);
 }
 @Test void deterministicRandomFullUtf16AndTransitivity()throws Throwable{
  var random=new Random(0x4d335554463843L);var corpus=new ArrayList<String>();
  for(int n=0;n<4096;n++){char[] text=new char[random.nextInt(25)];for(int i=0;i<text.length;i++)text[i]=(char)random.nextInt(65536);corpus.add(new String(text));}
  for(int n=0;n<100000;n++){String a=corpus.get(random.nextInt(corpus.size())),b=corpus.get(random.nextInt(corpus.size()));pair(a,b);}
  var expected=new ArrayList<>(corpus);expected.sort(M3InstanceUtf8Qualification::reference);var actual=new ArrayList<>(corpus);actual.sort((a,b)->{try{return compare(a,b);}catch(Throwable e){throw new AssertionError(e);}});assertEquals(expected,actual);
  evidence.put("randomPairs",100000);
 }
 static InstanceRecord record(String name,long concept){return new InstanceRecord(name,InstanceType.PERSON,concept,Map.of("?","question","?x","suffix","\uFFFD","replacement","😀","face"));}
 static M3InstanceIndexPrecompute index(List<String> names){
  var map=new LinkedHashMap<String,InstanceRecord>();for(int i=0;i<names.size();i++)map.put(names.get(i),record(names.get(i),1+i%3));
  return new M3InstanceIndexPrecompute("fixture","revision","fingerprint","ascii-fold-utf8-v1",Set.of(1L,2L,3L),map);
 }
 static Map<String,InstanceRecord> referenceMap(M3InstanceIndexPrecompute index){
  var map=new TreeMap<String,InstanceRecord>(M3InstanceUtf8Qualification::reference);map.putAll(index.recordsByName());return map;
 }
 static String outcome(Callable<?> call){try{Object value=call.call();return "V:"+String.valueOf(value);}catch(Throwable e){return "E:"+e.getClass().getName()+(e instanceof NullPointerException||e instanceof ClassCastException?":VM_DETAIL":":"+e.getMessage());}}
 @Test void rawMapMalformedAliasesAndNulls(){
  for(var names:List.of(List.<String>of(),List.of("?"),List.of("?","?x","??","name","\uFFFD","😀"))){
   var index=index(names);var oracle=referenceMap(index);var raw=index.recordsByName();
   for(Object q:Arrays.asList(null,"","\uD800","\uDC00","\uD800x","\uD800\uD800","?","??","missing",17)){
    assertEquals(outcome(()->oracle.get(q)),outcome(()->raw.get(q)));
    assertEquals(outcome(()->oracle.containsKey(q)),outcome(()->raw.containsKey(q)));
    assertEquals(outcome(()->oracle.keySet().contains(q)),outcome(()->raw.keySet().contains(q)));
   }
  }
  var index=index(List.of("?","?x"));assertEquals(index.recordsByName().get("?"),index.recordsByName().get("\uD800"));
  assertThrows(IllegalArgumentException.class,()->index.findByName("\uD800"));assertThrows(IllegalArgumentException.class,()->index.findByName(null));
 }
 @Test void metadataAndReverseNameQueriesRetainFallback(){
  var index=index(List.of("?","?x","??","name","😀"));for(var record:index.recordsByName().values()){
   var oracle=new TreeMap<String,String>(M3InstanceUtf8Qualification::reference);oracle.putAll(record.metadata());
   for(String q:Arrays.asList(null,"?","\uD800","\uD800x","\uDC00","😀","missing"))
    assertEquals(outcome(()->oracle.get(q)),outcome(()->record.metadata().get(q)));
  }
  for(var entry:index.namesByConceptX().entrySet()){
   var expected=new TreeSet<String>();expected.addAll(entry.getValue());
   for(String q:Arrays.asList(null,"?","\uD800","\uD800x","\uDC00","😀","missing"))
    assertEquals(outcome(()->expected.contains(q)),outcome(()->entry.getValue().contains(q)));
  }
 }
 @Test void snapshotsViewsAndNormalizationRemain(){
  var concepts=new LinkedHashSet<Long>(List.of(1L));var metadata=new LinkedHashMap<String,String>();metadata.put("UPPER","case");var record=new InstanceRecord("ÀBC Z",InstanceType.AUTHOR,1,metadata);
  var records=new LinkedHashMap<String,InstanceRecord>();records.put("Àbc z",record);
  var index=new M3InstanceIndexPrecompute("source","rev","fp","norm",concepts,records);concepts.clear();metadata.clear();records.clear();
  assertEquals(Set.of(1L),index.frozenConceptX());assertSame(record,index.findByName("ÀBC Z"));assertNull(index.recordsByName().get("ÀBC Z"));assertEquals("case",record.metadata().get("UPPER"));
  assertThrows(UnsupportedOperationException.class,()->index.recordsByName().clear());assertThrows(UnsupportedOperationException.class,()->index.frozenConceptX().clear());assertThrows(UnsupportedOperationException.class,()->index.namesByConceptX().get(1L).clear());assertThrows(UnsupportedOperationException.class,()->index.instancesOf(1L).clear());assertThrows(UnsupportedOperationException.class,()->record.metadata().clear());
  for(InstanceType type:InstanceType.values())assertEquals(type,new InstanceRecord("name",type,1,Map.of()).type());
 }
 @Test void publicResultsKeepIncumbentMixedOrders(){
  var names=List.of("z","a","\uE000","\uFFFD","😀","\uDBFF\uDFFF","हिन्दी","العربية","汉字");
  var index=index(names);var expected=new ArrayList<>(names);expected.sort(M3InstanceUtf8Qualification::reference);assertEquals(expected,new ArrayList<>(index.recordsByName().keySet()));
  for(long concept:List.of(1L,2L,3L)){var expectedNames=new ArrayList<>(index.namesByConceptX().get(concept));expectedNames.sort(Comparator.naturalOrder());assertEquals(expectedNames,index.instancesOf(concept).stream().map(r->M3InstanceIndexPrecompute.normalizedInstanceName(r.nameUtf8())).toList());}
 }
 @Test void directNullComparatorFailuresRetainTypeAndOrder()throws Throwable{
  for(String[] values:List.of(new String[]{null,null},new String[]{null,"name"},new String[]{"name",null})){
   var expected=assertThrows(NullPointerException.class,()->reference(values[0],values[1]));
   var actual=assertThrows(NullPointerException.class,()->compare(values[0],values[1]));
   evidence.put("nullMessage"+Arrays.toString(values),Map.of("reference",String.valueOf(expected.getMessage()),"actual",String.valueOf(actual.getMessage())));
  }
  evidence.put("directNullMessageCases",3);evidence.put("reverseNameOrder","UTF16_NATURAL_INCUMBENT");evidence.put("recordMetadataOrder","UNSIGNED_UTF8_INCUMBENT");
 }
 static final AtomicInteger encodings=new AtomicInteger();
 public static byte[] encoded(String value,Charset charset){encodings.incrementAndGet();return value.getBytes(charset);}
 @Test void actualEncodingAllocationBoundaryIsObserved()throws Throwable{
  byte[] before=classes().get("jdk.m3.text.M3InstanceIndexPrecompute");var writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);
  new ClassReader(before).accept(new ClassVisitor(Opcodes.ASM9,writer){@Override public MethodVisitor visitMethod(int access,String name,String desc,String signature,String[] exceptions){
   var delegate=super.visitMethod(access,name,desc,signature,exceptions);if(!name.equals("compareUtf8"))return delegate;
   return new MethodVisitor(Opcodes.ASM9,delegate){@Override public void visitMethodInsn(int op,String owner,String name,String descriptor,boolean itf){
    if(owner.equals("java/lang/String")&&name.equals("getBytes")&&descriptor.equals("(Ljava/nio/charset/Charset;)[B"))super.visitMethodInsn(Opcodes.INVOKESTATIC,"M3InstanceUtf8Qualification","encoded","(Ljava/lang/String;Ljava/nio/charset/Charset;)[B",false);
    else super.visitMethodInsn(op,owner,name,descriptor,itf);
   }};
  }},0);byte[] bytes=writer.toByteArray();
  ClassLoader child=new ClassLoader(M3InstanceUtf8Qualification.class.getClassLoader()){@Override protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
   Class<?> type=findLoadedClass(name);if(type==null&&name.equals("jdk.m3.text.M3InstanceIndexPrecompute"))type=defineClass(name,bytes,0,bytes.length);if(type==null)type=super.loadClass(name,false);if(resolve)resolveClass(type);return type;
  }};
  Class<?> type=child.loadClass("jdk.m3.text.M3InstanceIndexPrecompute");var handle=MethodHandles.privateLookupIn(type,MethodHandles.lookup()).findStatic(type,"compareUtf8",MethodType.methodType(int.class,String.class,String.class));
  int valid=0;for(var pair:List.of(List.of("alpha","beta"),List.of("हिन्दी","汉字"),List.of("\uFFFD","😀"),List.of("same","same"),List.of("","x"))){encodings.set(0);int result=(int)handle.invokeExact(pair.get(0),pair.get(1));assertEquals(reference(pair.get(0),pair.get(1)),result);assertEquals(mode.equals("after")?0:2,encodings.get());valid++;}
  for(String[] values:List.of(new String[]{null,null},new String[]{null,"name"},new String[]{"name",null})){
   encodings.set(0);assertThrows(NullPointerException.class,()->{int ignored=(int)handle.invokeExact(values[0],values[1]);});assertEquals(values[0]==null?1:2,encodings.get());
  }
  evidence.put("nullOperandEncodingOrder",List.of(1,1,2));
  encodings.set(0);assertEquals(0,(int)handle.invokeExact("\uD800","?"));assertEquals(2,encodings.get());evidence.put("validEncodingProbePairs",valid);evidence.put("validEncodingsPerCompare",mode.equals("after")?0:2);evidence.put("malformedFallbackEncodings",2);
 }
 @Test void apiAndOtherMethodFingerprints()throws Exception{
  var surface=new TreeSet<String>();var others=new TreeMap<String,String>();int[] codepoints={0},getbytes={0};
  for(var e:classes().entrySet()){var writer=new ClassWriter(0);
   new ClassReader(e.getValue()).accept(new ClassVisitor(Opcodes.ASM9,writer){
    @Override public FieldVisitor visitField(int access,String name,String desc,String sig,Object value){surface.add(e.getKey()+"/F:"+access+":"+name+":"+desc);return super.visitField(access,name,desc,sig,value);}
    @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] exceptions){
     if((access&(Opcodes.ACC_PUBLIC|Opcodes.ACC_PROTECTED))!=0)surface.add(e.getKey()+"/M:"+access+":"+name+desc);
     if(!name.equals("compareUtf8"))return super.visitMethod(access,name,desc,sig,exceptions);
     return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String owner,String name,String descriptor,boolean itf){if(owner.equals("java/lang/String")&&name.equals("codePointAt"))codepoints[0]++;if(owner.equals("java/lang/String")&&name.equals("getBytes"))getbytes[0]++;}};
    }},ClassReader.SKIP_DEBUG);others.put(e.getKey(),sha(writer.toByteArray()));
  }
  assertEquals(mode.equals("after")?2:0,codepoints[0]);assertEquals(2,getbytes[0]);
  evidence.put("apiFieldsFingerprint",sha(String.join("\n",surface).getBytes(StandardCharsets.UTF_8)));evidence.put("otherMethodsFingerprint",sha(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(others)));evidence.put("codePointCalls",codepoints[0]);
 }
 @Test void concurrentPublicLookups()throws Exception{
  var names=List.of("alpha","beta","हिन्दी","汉字","😀");var index=index(names);var pool=Executors.newFixedThreadPool(8);
  try{var tasks=new ArrayList<Callable<Long>>();for(int t=0;t<8;t++)tasks.add(()->{long sum=0;for(int i=0;i<1000;i++){String n=names.get(i%names.size());assertSame(index.recordsByName().get(n),index.findByName(n));sum+=index.findByName(n).instanceOfX();}return sum;});for(var f:pool.invokeAll(tasks))assertTrue(f.get()>0);}
  finally{pool.shutdown();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS));}
 }
 record Pair(String left,String right){}
 static long compareBatch(Pair[] pairs,int calls)throws Throwable{long sum=0;for(int i=0;i<calls;i++){var p=pairs[i&8191];sum+=compare(p.left(),p.right());}sink=sum;return sum;}
 static long lookupBatch(M3InstanceIndexPrecompute index,String[] queries,int calls,boolean raw){
  long sum=0;for(int i=0;i<calls;i++){String key=queries[i&8191];var value=raw?index.recordsByName().get(key):index.findByName(key);sum+=value==null?17:value.instanceOfX();}sink=sum;return sum;
 }
 static List<String> names(String scenario,int count){var out=new ArrayList<String>();for(int i=0;i<count;i++)out.add(switch(scenario){case "ascii"->"person/"+Integer.toString(i,36)+"/name";case "multilingual"->"हिन्दी/汉字/العربية/"+Integer.toHexString(i);case "supplementary"->"😀/name/"+Integer.toHexString(i)+"/\uDBFF\uDFFF";default->throw new AssertionError(scenario);});return out;}
 static void measure()throws Throwable{
  var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();assertTrue(bean.isThreadAllocatedMemorySupported());bean.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().threadId();
  for(String scenario:List.of("ascii","multilingual","supplementary")){
   var names=names(scenario,8192);var pairs=new Pair[8192];var queries=new String[8192];var random=new Random(0x4d3355544638L);
   for(int i=0;i<pairs.length;i++){String key=names.get(random.nextInt(names.size()));queries[i]=key;pairs[i]=new Pair(key,names.get(random.nextInt(names.size())));}
   var index=index(names);
   for(int i=0;i<10;i++){compareBatch(pairs,30000);lookupBatch(index,queries,30000,true);lookupBatch(index,queries,30000,false);}
   for(String operation:List.of("compare","raw-lookup","normalized-lookup"))for(int sample=0;sample<7;sample++){
    long allocated=bean.getThreadAllocatedBytes(thread),start=System.nanoTime();long answer=operation.equals("compare")?compareBatch(pairs,200000):lookupBatch(index,queries,200000,operation.equals("raw-lookup"));
    long nanos=System.nanoTime()-start,bytes=bean.getThreadAllocatedBytes(thread)-allocated;
    measurements.add(Map.of("operation",operation,"scenario",scenario,"sample",sample,"calls",200000,"nanos",nanos,"bytes",bytes,"answer",answer));
   }
   var input=new LinkedHashMap<String,InstanceRecord>();for(String n:names.subList(0,4096))input.put(n,record(n,1));
   for(int warm=0;warm<3;warm++)ownerSink=new M3InstanceIndexPrecompute("source","rev","fp","norm",Set.of(1L),input);
   for(int sample=0;sample<7;sample++){long allocated=bean.getThreadAllocatedBytes(thread),start=System.nanoTime();var made=new M3InstanceIndexPrecompute("source","rev","fp","norm",Set.of(1L),input);ownerSink=made;long nanos=System.nanoTime()-start,bytes=bean.getThreadAllocatedBytes(thread)-allocated;
    measurements.add(Map.of("operation","construct","scenario",scenario,"sample",sample,"calls",1,"nanos",nanos,"bytes",bytes,"answer",(long)made.size()));
   }
  }
 }
 public static void main(String[] a)throws Throwable{
  mode=a[0];var vm=java.lang.management.ManagementFactory.getPlatformMXBean(com.sun.management.HotSpotDiagnosticMXBean.class);
  assertEquals("false",vm.getVMOption("UsePerfData").getValue());assertEquals("true",vm.getVMOption("PerfDisableSharedMem").getValue());assertEquals("false",vm.getVMOption("BackgroundCompilation").getValue());assertEquals("2",vm.getVMOption("CICompilerCount").getValue());
  var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);launcher.execute(LauncherDiscoveryRequestBuilder.request().selectors(selectClass(M3InstanceUtf8Qualification.class)).build());
  var s=listener.getSummary();s.printTo(new PrintWriter(System.out,true));s.printFailuresTo(new PrintWriter(System.out,true));if(s.getTestsFoundCount()!=14||s.getTestsSucceededCount()!=14||s.getTestsFailedCount()!=0||s.getTestsAbortedCount()!=0||s.getTestsSkippedCount()!=0)throw new AssertionError("JUnit counts");
  measure();var result=new LinkedHashMap<String,Object>();result.put("mode",mode);result.put("found",14);result.put("passed",14);result.put("failed",0);result.put("aborted",0);result.put("evidence",evidence);result.put("measurements",measurements);
  byte[] bytes=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(result);var connection=(HttpURLConnection)URI.create(a[1]).toURL().openConnection();connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setConnectTimeout(10000);connection.setReadTimeout(10000);connection.setFixedLengthStreamingMode(bytes.length);
  try(var out=connection.getOutputStream()){out.write(bytes);}if(connection.getResponseCode()!=204)throw new AssertionError("result");connection.disconnect();System.out.println("M3_INSTANCE_UTF8_QUALIFICATION_PASS mode="+mode+" tests=14");
 }
}
