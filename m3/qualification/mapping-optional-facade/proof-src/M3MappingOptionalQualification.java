// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
// Maintenance QA: real compiled owner, preserved admission fixtures, C-donor arithmetic control and Optional lifecycle discriminator.
package jdk.m3.text;
import java.io.*;import java.lang.invoke.*;import java.lang.reflect.*;import java.math.BigInteger;
import java.net.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.security.*;
import java.util.*;import org.junit.jupiter.api.Test;import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;import org.objectweb.asm.*;
import static org.junit.jupiter.api.Assertions.*;import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
public final class M3MappingOptionalQualification {
 static String mode;static Path image;static String firstText;static MethodHandle slot;static volatile long sink;static volatile Object escapingSink;
 static final List<Map<String,Object>> measurements=new ArrayList<>();
 static final Map<String,Object> evidence=new LinkedHashMap<>();
 static Field field(String name)throws Exception{var f=SharedLexiconCatalog.class.getDeclaredField(name);f.setAccessible(true);return f;}
 static Object get(Object o,String name)throws Exception{return field(name).get(o);}
 static int initial(String source,String record,int length)throws Throwable{return (int)slot.invokeExact(source,record,length);}
 static String sha(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
 static String text(String encoded){return encoded.equals("-")?null:new String(Base64.getDecoder().decode(encoded),StandardCharsets.UTF_8);}
 static byte[] ownerBytes()throws Exception{
  ClassLoader loader=M3MappingOptionalQualification.class.getClassLoader();
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
  try(var in=M3MappingOptionalQualification.class.getResourceAsStream("/range-oracle.tsv")){
   assertNotNull(in);for(String line:new String(in.readAllBytes(),StandardCharsets.UTF_8).split("\n")){if(line.isEmpty())continue;var f=line.split("\t",-1);assertEquals(5,f.length);
    String source=text(f[0]),record=text(f[1]);int length=Integer.parseInt(f[2]);long word=Long.parseLong(f[3]);int oracle=Integer.parseInt(f[4]);
    int hash=31*Objects.hashCode(source)+Objects.hashCode(record),mixed=hash*0x9E3779B9,folded=mixed^(mixed>>>16);
    assertEquals(word,Integer.toUnsignedLong(folded),"same exact C oracle input");
    int exact=BigInteger.valueOf(word).multiply(BigInteger.valueOf(length)).shiftRight(32).intValueExact();assertEquals(exact,oracle);
    int actual=initial(source,record,length);assertTrue(actual>=0&&actual<length);
    assertEquals(mode.startsWith("range")?oracle:Math.floorMod(folded,length),actual);
    if(actual!=oracle)different++;checked++;
   }
  }
  assertTrue(checked>=12000);assertEquals(mode.startsWith("range"),different==0);evidence.put("oracleVectors",checked);evidence.put("differentFromDonor",different);
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
  assertArrayEquals(mode.startsWith("range")?new int[]{0,1,1}:new int[]{1,0,0},operations);
  evidence.put("apiAndFieldFingerprint",sha(String.join("\n",apiSurface).getBytes(StandardCharsets.UTF_8)));evidence.put("floorModCalls",operations[0]);
  int[] shape=new int[3];new ClassReader(ownerBytes()).accept(new ClassVisitor(Opcodes.ASM9){
   @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
    if(name.equals("mappingValueWhileLocked")){shape[0]++;assertEquals(Opcodes.ACC_PRIVATE,access);}
    if(!name.equals("findMapping"))return null;
    return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){
     if(owner.equals("java/util/Optional")){shape[1]++;if(name.equals("ofNullable"))shape[2]++;}
    }};
   }
  },ClassReader.SKIP_DEBUG);
  assertArrayEquals(mode.endsWith("after")?new int[]{1,1,1}:new int[]{0,2,0},shape);
  evidence.put("optionalFacadeShape",List.of(shape[0],shape[1],shape[2]));
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

 static final java.util.concurrent.atomic.AtomicInteger factoryCalls=new java.util.concurrent.atomic.AtomicInteger();
 static volatile boolean pauseFactory;
 static volatile java.util.concurrent.CountDownLatch factoryEntered,factoryRelease;
 public static Optional<Object> guardedFactory(Object catalog,Object value){
  try{
   var f=catalog.getClass().getDeclaredField("lifecycle");f.setAccessible(true);
   var lock=(java.util.concurrent.locks.ReentrantReadWriteLock)f.get(catalog);
   assertTrue(lock.getReadHoldCount()>0,"Optional factory must remain under actual reader lock");factoryCalls.incrementAndGet();
   if(pauseFactory){factoryEntered.countDown();assertTrue(factoryRelease.await(5,java.util.concurrent.TimeUnit.SECONDS),"release factory");}
   return Optional.ofNullable(value);
  }catch(ReflectiveOperationException e){throw new AssertionError(e);}
   catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}
 }
 static Map<String,byte[]> compiledClasses()throws Exception{
  ClassLoader loader=M3MappingOptionalQualification.class.getClassLoader();
  for(var f:loader.getClass().getDeclaredFields()){if(!Map.class.isAssignableFrom(f.getType()))continue;f.setAccessible(true);
   if(f.get(loader) instanceof Map<?,?> m&&m.get("jdk.m3.text.SharedLexiconCatalog") instanceof ByteArrayOutputStream){
    var result=new HashMap<String,byte[]>();for(var e:m.entrySet())if(e.getKey() instanceof String n&&e.getValue() instanceof ByteArrayOutputStream b)result.put(n,b.toByteArray());return result;
   }}
  throw new AssertionError("RAM compiled classes");
 }
 @Test void actualOptionalFactoryRemainsLockedAndCloseWaits()throws Exception{
  var bytes=compiledClasses();String owner="jdk.m3.text.SharedLexiconCatalog";int[] patched={0};
  var writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);
  new ClassReader(bytes.get(owner)).accept(new ClassVisitor(Opcodes.ASM9,writer){
   @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] exceptions){
    MethodVisitor delegate=super.visitMethod(access,name,desc,sig,exceptions);if(!name.equals("findMapping"))return delegate;
    return new MethodVisitor(Opcodes.ASM9,delegate){
     @Override public void visitMethodInsn(int op,String target,String name,String descriptor,boolean itf){
      if(op==Opcodes.INVOKESTATIC&&target.equals("java/util/Optional")&&Set.of("empty","of","ofNullable").contains(name)){
       if(name.equals("empty")){super.visitVarInsn(Opcodes.ALOAD,0);super.visitInsn(Opcodes.ACONST_NULL);}
       else{super.visitVarInsn(Opcodes.ALOAD,0);super.visitInsn(Opcodes.SWAP);}
       super.visitMethodInsn(Opcodes.INVOKESTATIC,"jdk/m3/text/M3MappingOptionalQualification","guardedFactory","(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Optional;",false);patched[0]++;
      }else super.visitMethodInsn(op,target,name,descriptor,itf);
     }
    };
   }
  },0);bytes.put(owner,writer.toByteArray());assertEquals(mode.endsWith("after")?1:2,patched[0]);
  ClassLoader child=new ClassLoader(M3MappingOptionalQualification.class.getClassLoader()){
   @Override protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
    Class<?> value=findLoadedClass(name);
    if(value==null&&name.startsWith("jdk.m3.text.")&&!name.startsWith("jdk.m3.text.M3MappingOptionalQualification")){
     byte[] content=bytes.get(name);if(content!=null)value=defineClass(name,content,0,content.length);
    }
    if(value==null)value=super.loadClass(name,false);if(resolve)resolveClass(value);return value;
   }
  };
  Class<?> test=child.loadClass("jdk.m3.text.SharedLexiconCatalogSiWarmTest");assertSame(child,test.getClassLoader());
  var fixture=test.getDeclaredMethod("fixture",Path.class,boolean.class,boolean.class);fixture.setAccessible(true);
  Object f=fixture.invoke(null,image,false,false);var accessor=f.getClass().getDeclaredMethod("catalog");accessor.setAccessible(true);
  Object catalog=accessor.invoke(f);Class<?> type=catalog.getClass();assertSame(child,type.getClassLoader());
  var lookup=type.getMethod("findMapping",String.class,String.class);var close=type.getMethod("close");
  var lifecycle=type.getDeclaredField("lifecycle");lifecycle.setAccessible(true);
  var lock=(java.util.concurrent.locks.ReentrantReadWriteLock)lifecycle.get(catalog);
  factoryCalls.set(0);pauseFactory=false;assertTrue(((Optional<?>)lookup.invoke(catalog,"absent","U0")).isEmpty());assertEquals(1,factoryCalls.get());
  factoryEntered=new java.util.concurrent.CountDownLatch(1);factoryRelease=new java.util.concurrent.CountDownLatch(1);pauseFactory=true;
  var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();var escaped=new java.util.concurrent.atomic.AtomicReference<Object>();
  Thread reader=new Thread(()->{try{escaped.set(lookup.invoke(catalog,"dictlang.si-units","U0"));}catch(Throwable e){failure.set(e);}},"optional-factory-reader");
  Thread closer=new Thread(()->{try{close.invoke(catalog);}catch(Throwable e){failure.set(e);}},"optional-factory-closer");reader.setDaemon(true);closer.setDaemon(true);
  try{
   reader.start();assertTrue(factoryEntered.await(5,java.util.concurrent.TimeUnit.SECONDS));closer.start();
   long deadline=System.nanoTime()+5_000_000_000L;while(!lock.hasQueuedThread(closer)&&closer.isAlive()&&System.nanoTime()<deadline)java.util.concurrent.locks.LockSupport.parkNanos(100_000);
   assertTrue(lock.hasQueuedThread(closer),"closer reached write lock and is blocked by factory");assertTrue(closer.isAlive());
  }finally{factoryRelease.countDown();pauseFactory=false;reader.join(5000);if(closer.getState()!=Thread.State.NEW)closer.join(5000);close.invoke(catalog);}
  assertFalse(reader.isAlive());assertFalse(closer.isAlive());assertNull(failure.get());assertEquals(2,factoryCalls.get());
  Object mapping=((Optional<?>)escaped.get()).orElseThrow();assertEquals("U0",mapping.getClass().getMethod("recordId").invoke(mapping));
  var error=assertThrows(InvocationTargetException.class,()->lookup.invoke(catalog,null,null));assertInstanceOf(IllegalStateException.class,error.getCause());assertEquals(2,factoryCalls.get());
  evidence.put("factoryLock",Map.of("actualClassBytesInstrumented",true,"factoryCalls",factoryCalls.get(),"closeQueuedWhileFactoryPaused",true,"threadsReaped",true));
 }
 @Test void lookupFailureAndNullOrderingRemainsExact()throws Exception{
  var create=SharedLexiconCatalogSiWarmTest.class.getDeclaredMethod("fixture",Path.class,boolean.class,boolean.class);create.setAccessible(true);
  Object f=create.invoke(null,image,true,true);var accessor=f.getClass().getDeclaredMethod("catalog");accessor.setAccessible(true);
  var catalog=(SharedLexiconCatalog)accessor.invoke(f);
  try{
   assertTrue(catalog.findMapping("absent","U0").isEmpty());
   assertThrows(NullPointerException.class,()->catalog.findMapping(null,"U0"));
   assertThrows(NullPointerException.class,()->catalog.findMapping("dictlang.si-units",null));
   var first=assertThrows(java.io.UncheckedIOException.class,()->catalog.findMapping("dictlang.si-units","U0"));
   var second=assertThrows(java.io.UncheckedIOException.class,()->catalog.findMapping("dictlang.si-units","U0"));
   assertEquals(first.getMessage(),second.getMessage());assertEquals(first.getCause().getClass(),second.getCause().getClass());
   evidence.put("mappingFailure",Map.of("type",first.getClass().getName(),"cause",first.getCause().getClass().getName(),"repeatable",true));
  }finally{catalog.close();}
  assertThrows(IllegalStateException.class,()->catalog.findMapping(null,null));
 }
 @Test void escapedOptionalKeepsTheSameImmutableMapping()throws Exception{
  var values=numbers(4);var f=fixture(values);Optional<SharedLexiconCatalog.SourceMapping> first=f.catalog().findMapping("numbers","3"),second=f.catalog().findMapping("numbers","3"),miss=f.catalog().findMapping("numbers","missing");
  f.close();assertSame(values.get(3),first.orElseThrow());assertEquals(first,second);assertTrue(miss.isEmpty());assertEquals("3",first.orElseThrow().recordId());
  assertThrows(IllegalStateException.class,()->f.catalog().findMapping(null,null));
 }

 record Query(String source,String record){}
 static long batch(SharedLexiconCatalog catalog,Query[] queries,int calls){
  long sum=0;for(int i=0;i<calls;i++){var q=queries[i&8191];var value=catalog.findMapping(q.source(),q.record());sum+=value.isPresent()?value.orElseThrow().recordId().hashCode():17;}sink=sum;return sum;
 }
 static long batchEscaping(SharedLexiconCatalog catalog,Query[] queries,int calls,Object[] escaped){
  long sum=0;for(int i=0;i<calls;i++){var q=queries[i&8191];var value=catalog.findMapping(q.source(),q.record());escaped[i&1023]=value;sum+=value.isPresent()?value.orElseThrow().recordId().hashCode():17;}sink=sum;return sum;
 }
 static void measure()throws Throwable{
  var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();assertTrue(bean.isThreadAllocatedMemorySupported());bean.setThreadAllocatedMemoryEnabled(true);
  var values=numbers(10001);for(String scenario:List.of("numeric-hits","numeric-misses","mixed-language")){
   var extra=new ArrayList<>(values);if(scenario.equals("mixed-language"))for(int n=0;n<10001;n++)extra.add(mapping("language:हिन्दी","पद/"+n,"{}"));
   try(var f=fixture(extra)){var random=new Random(0x4d3352414e47454cL);var q=new Query[8192];
    for(int i=0;i<q.length;i++){int n=random.nextInt(10001);q[i]=scenario.equals("numeric-misses")?new Query("numbers","absent/"+n):scenario.equals("mixed-language")&&(i&1)==0?new Query("language:हिन्दी","पद/"+n):new Query("numbers",Integer.toString(n));q[i].source().hashCode();q[i].record().hashCode();}
    Object[] escaped=new Object[1024];escapingSink=escaped;
    for(int warm=0;warm<15;warm++){batch(f.catalog(),q,200000);batchEscaping(f.catalog(),q,200000,escaped);}
    var ps=probes(f.catalog(),Arrays.asList(q));evidence.put(scenario+"Probes",ps);
    for(String consumer:List.of("non-escaping","escaping"))for(int sample=0;sample<7;sample++){long startBytes=bean.getThreadAllocatedBytes(Thread.currentThread().threadId()),start=System.nanoTime();long answer=consumer.equals("escaping")?batchEscaping(f.catalog(),q,1000000,escaped):batch(f.catalog(),q,1000000);long nanos=System.nanoTime()-start,bytes=bean.getThreadAllocatedBytes(Thread.currentThread().threadId())-startBytes;measurements.add(Map.of("scenario",scenario,"consumer",consumer,"sample",sample,"calls",1000000,"nanos",nanos,"bytes",bytes,"answer",answer));}
   }
  }
 }
 public static void main(String[] a)throws Throwable{
  mode=a[0];image=Path.of(a[1]);firstText=SharedLexiconImage.open(image).recordText(0);slot=MethodHandles.privateLookupIn(SharedLexiconCatalog.class,MethodHandles.lookup()).findStatic(SharedLexiconCatalog.class,"identitySlot",MethodType.methodType(int.class,String.class,String.class,int.class));
  byte[] before=MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(image));
  var vm=java.lang.management.ManagementFactory.getPlatformMXBean(com.sun.management.HotSpotDiagnosticMXBean.class);for(String option:List.of("UsePerfData","PerfDataSaveToFile"))assertEquals("false",vm.getVMOption(option).getValue());assertEquals("true",vm.getVMOption("PerfDisableSharedMem").getValue());
  var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);launcher.execute(LauncherDiscoveryRequestBuilder.request().selectors(selectClass(M3MappingOptionalQualification.class)).build());
  var s=listener.getSummary();s.printTo(new PrintWriter(System.out,true));s.printFailuresTo(new PrintWriter(System.out,true));if(s.getTestsFoundCount()!=13||s.getTestsSucceededCount()!=13||s.getTestsFailedCount()!=0||s.getTestsAbortedCount()!=0||s.getTestsSkippedCount()!=0)throw new AssertionError("qualification counts");
  measure();assertArrayEquals(before,MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(image)));
  var result=new LinkedHashMap<String,Object>();result.put("mode",mode);result.put("found",13);result.put("passed",13);result.put("failed",0);result.put("aborted",0);result.put("evidence",evidence);result.put("measurements",measurements);
  byte[] bytes=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(result);
  var c=(HttpURLConnection)URI.create(a[2]).toURL().openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setFixedLengthStreamingMode(bytes.length);try(var out=c.getOutputStream()){out.write(bytes);}if(c.getResponseCode()!=204)throw new AssertionError("receipt");c.disconnect();
  System.out.println("M3_MAPPING_OPTIONAL_QUALIFICATION_PASS mode="+mode+" tests=13");
 }
}
