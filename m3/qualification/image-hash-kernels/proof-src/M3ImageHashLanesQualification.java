// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
// CPU QA over the actual image owner; RAM buffers are constructor fixtures, not OS mappings.
package jdk.m3.text;
import java.io.*;import java.lang.invoke.*;import java.lang.reflect.*;import java.net.*;
import java.nio.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.security.*;
import java.util.*;import java.util.concurrent.atomic.AtomicInteger;import java.util.function.IntConsumer;
import org.junit.jupiter.api.Test;import org.junit.platform.launcher.core.*;import org.junit.platform.launcher.listeners.*;
import org.objectweb.asm.*;
import static org.junit.jupiter.api.Assertions.*;import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
public final class M3ImageHashLanesQualification {
 static String mode;static Path existing;static volatile long sink;static volatile Object ownerSink;
 static final Map<String,Object> evidence=new LinkedHashMap<>();
 static final List<Map<String,Object>> measurements=new ArrayList<>();
 static final Provider oracleProvider=Objects.requireNonNull(Security.getProvider("SUN"));
 static final MethodHandle HASH=hashHandle(),CONSTRUCT=constructorHandle();
 static MethodHandle hashHandle(){try{return MethodHandles.privateLookupIn(SharedLexiconImage.class,MethodHandles.lookup()).findVirtual(SharedLexiconImage.class,"javaHash",MethodType.methodType(int.class,int.class));}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 static MethodHandle constructorHandle(){try{return MethodHandles.privateLookupIn(SharedLexiconImage.class,MethodHandles.lookup()).findConstructor(SharedLexiconImage.class,MethodType.methodType(void.class,MappedByteBuffer.class));}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 static SharedLexiconImage open(MappedByteBuffer b)throws Throwable{return (SharedLexiconImage)CONSTRUCT.invokeExact(b);}
 static int hash(SharedLexiconImage image,int row)throws Throwable{return (int)HASH.invokeExact(image,row);}
 static byte[] sha(byte[] bytes)throws Exception{return MessageDigest.getInstance("SHA-256",oracleProvider).digest(bytes);}
 static String hex(byte[] bytes){return HexFormat.of().formatHex(bytes);}
 record Ram(MappedByteBuffer buffer,List<String> words,int version,int payload,int[] offsets){}
 static void seal(ByteBuffer b)throws Exception{
  var md=MessageDigest.getInstance("SHA-256",oracleProvider);md.update(b.asReadOnlyBuffer().slice(0,32));md.update(b.asReadOnlyBuffer().slice(64,b.limit()-64));b.put(32,md.digest());
 }
 static Ram ram(List<String> words,int version,int gap)throws Exception{
  int directory=version==1?8:12,units=gap;var offsets=new int[words.size()];
  for(int i=0;i<words.size();i++){offsets[i]=units;units=Math.addExact(units,words.get(i).length()+gap);}
  int payload=64+directory*words.size(),size=payload+units*2;
  ByteBuffer raw=ByteBuffer.allocateDirect(size+32);assertInstanceOf(MappedByteBuffer.class,raw);MappedByteBuffer b=(MappedByteBuffer)raw;b.limit(size);b.order(ByteOrder.BIG_ENDIAN);
  for(int i=64;i<size;i++)b.put(i,(byte)0xa5);
  b.putLong(0,0x4d334c4558303031L);b.putInt(8,version);b.putInt(12,words.size());b.putLong(16,payload);b.putLong(24,units);
  for(int row=0;row<words.size();row++){
   String text=words.get(row);int at=64+row*directory;b.putInt(at,offsets[row]);b.putInt(at+4,text.length());if(version==2)b.putInt(at+8,text.hashCode());
   for(int i=0;i<text.length();i++){int p=payload+(offsets[row]+i)*2;char c=text.charAt(i);b.put(p,(byte)c);b.put(p+1,(byte)(c>>>8));}
  }
  seal(b);b.position(0);return new Ram(b,List.copyOf(words),version,payload,offsets);
 }
 static List<String> corpus(String name,int count){
  var sorted=new TreeSet<String>();for(int i=0;i<count;i++){
   String value=switch(name){
    case "empty" -> "";
    case "short-1" -> String.valueOf((char)i);
    case "short","short-2" -> new String(new char[]{(char)(i>>>8),(char)(i&255)});
    case "short-3" -> new String(new char[]{(char)(i>>>8),(char)(i&255),0x8800});
    case "identifier" -> "id/"+Integer.toString(i,36)+"/HTTPServerCase";
    case "long-multilingual" -> "हिन्दी/汉字/عربي/"+Integer.toHexString(i)+"/"+"Ab\uD800\uDC00\uFFFF".repeat(12);
    default -> throw new AssertionError(name);};sorted.add(value);
  }assertEquals(count,sorted.size());return List.copyOf(sorted);
 }
 static void checkHashes(Ram r)throws Throwable{
  var image=open(r.buffer());assertEquals(r.words().size(),image.size());assertEquals(r.version(),image.version());
  for(int i=0;i<r.words().size();i++)assertEquals(r.words().get(i).hashCode(),hash(image,i),"record "+i);
 }
 @Test void everyUtf16CodeUnitAndEmptyRecords()throws Throwable{
  var values=new ArrayList<String>();values.add("");for(int i=0;i<65535;i++)values.add(String.valueOf((char)i));
  for(int version:List.of(1,2))checkHashes(ram(values,version,0));
  for(int version:List.of(1,2))checkHashes(ram(List.of("\uFFFF"),version,0));
  for(int version:List.of(1,2)){assertEquals(0,open(ram(List.of(),version,0).buffer()).size());checkHashes(ram(List.of(""),version,0));}
  evidence.put("allUtf16Units",65536);evidence.put("emptyRecordAndZeroRecord",true);
 }
 @Test void packedLanesTailsAndUnalignedRecords()throws Throwable{
  char[] pool={0,1,0x7f,0xff,0x100,0x8000,0xd800,0xffff};var values=new TreeSet<String>();
  for(char a:pool)for(char b:pool)for(char c:pool)for(char d:pool)values.add(new String(new char[]{a,b,c,d}));
  var random=new Random(0x4d33484153484cL);
  for(int size=0;size<130;size++)for(int trial=0;trial<8;trial++){char[] text=new char[size];for(int i=0;i<size;i++)text[i]=(char)random.nextInt(65536);values.add(new String(text));}
  values.addAll(List.of("Aa","BB","AaAa","BBBB","😀x","x\uD800","x\uDC00","e\u0301","\u00e9","HTTPServer","camelCase","kebab-case"));
  var words=List.copyOf(values);for(int version:List.of(1,2))for(int gap=0;gap<4;gap++)checkHashes(ram(words,version,gap));
  assertEquals("Aa".hashCode(),"BB".hashCode());evidence.put("packedDistinctStrings",words.size());evidence.put("gapVariants",4);evidence.put("maxTestLength",129);
 }
 @Test void privateHashIndexChecksRemain()throws Throwable{
  var image=open(ram(List.of("longer-word"),2,0).buffer());
  assertThrows(IndexOutOfBoundsException.class,()->hash(image,-1));assertThrows(IndexOutOfBoundsException.class,()->hash(image,1));assertThrows(IndexOutOfBoundsException.class,()->hash(image,Integer.MAX_VALUE));
  var empty=open(ram(List.of(),2,0).buffer());assertThrows(IndexOutOfBoundsException.class,()->hash(empty,0));
 }
 @Test void lookupAndPrefixRetainStringOrder()throws Throwable{
  var words=new ArrayList<String>();for(int i=0;i<=10000;i++)words.add(Integer.toString(i));words.addAll(List.of("Aa","BB","HTTPServer","हिन्दी","العربية","汉字","😀","\uD800","\uDC00"));Collections.sort(words);
  for(int version:List.of(1,2)){var image=open(ram(words,version,1).buffer());for(int i=0;i<words.size();i++)assertEquals(i,image.findRecord(words.get(i)));
   for(String q:List.of("","01","10001","a","ह","😀","😀x","\uFFFF")){
    int at=Collections.binarySearch(words,q);assertEquals(at>=0?at:-1,image.findRecord(q));int lower=at>=0?at:-at-1;assertEquals(lower,image.lowerBound(q));
    for(int i=0;i<words.size();i+=31)assertEquals(words.get(i).startsWith(q),image.startsWith(i,q));
   }
  }evidence.put("numberIds",10001);
 }
 @Test void headerDimensionsAndInitialChecksumRefuse()throws Exception{
  var header=ram(List.of("four"),2,0);header.buffer().putLong(0,0L);assertEquals("unsupported image header",assertThrows(IOException.class,()->open(header.buffer())).getMessage());
  var wrongVersion=ram(List.of("four"),2,0);wrongVersion.buffer().putInt(8,3);assertEquals("unsupported image version",assertThrows(IOException.class,()->open(wrongVersion.buffer())).getMessage());
  var dimensions=ram(List.of("four"),2,0);dimensions.buffer().putLong(24,-1L);assertEquals("invalid image dimensions",assertThrows(IOException.class,()->open(dimensions.buffer())).getMessage());
  var checksum=ram(List.of("four"),2,0);checksum.buffer().put(checksum.payload(),(byte)99);assertEquals("image checksum mismatch",assertThrows(IOException.class,()->open(checksum.buffer())).getMessage());
 }
 @Test void recordRangeHashAndSortRefuseInOrder()throws Exception{
  var range=ram(List.of("aaaa","bbbb"),2,0);range.buffer().putInt(64,-1);seal(range.buffer());assertEquals("invalid record range",assertThrows(IOException.class,()->open(range.buffer())).getMessage());
  var length=ram(List.of("aaaa"),2,0);length.buffer().putInt(68,Integer.MAX_VALUE);seal(length.buffer());assertEquals("invalid record range",assertThrows(IOException.class,()->open(length.buffer())).getMessage());
  var hash=ram(List.of("aaaa"),2,0);hash.buffer().putInt(72,123);seal(hash.buffer());assertEquals("record Java hash mismatch",assertThrows(IOException.class,()->open(hash.buffer())).getMessage());
  var hashBeforeSort=ram(List.of("bbbb","aaaa"),2,0);hashBeforeSort.buffer().putInt(84,123);seal(hashBeforeSort.buffer());assertEquals("record Java hash mismatch",assertThrows(IOException.class,()->open(hashBeforeSort.buffer())).getMessage());
  for(int version:List.of(1,2))for(var words:List.of(List.of("same","same"),List.of("bbbb","aaaa"))){var r=ram(words,version,0);assertEquals("records must be UTF-16 sorted",assertThrows(IOException.class,()->open(r.buffer())).getMessage());}
 }
 @Test void mappingPositionLimitMarkAndByteOrder()throws Throwable{
  var r=ram(List.of("","four","longer-word","汉字-😀"),2,3);int limit=r.buffer().limit();
  r.buffer().position(7);r.buffer().mark();r.buffer().position(11);r.buffer().order(ByteOrder.LITTLE_ENDIAN);
  var image=open(r.buffer());assertEquals(11,r.buffer().position());assertEquals(limit,r.buffer().limit());assertEquals(ByteOrder.BIG_ENDIAN,r.buffer().order());r.buffer().reset();assertEquals(7,r.buffer().position());
  for(int i=0;i<image.size();i++)assertEquals(r.words().get(i),image.recordText(i));
 }
 @Test void immutableCopyAndChangedRecordDetection()throws Throwable{
  var r=ram(List.of("aaaa","longer-value"),2,1);var image=open(r.buffer());var arena=new LocalM3Arena();var copy=image.copyRecord(1,arena);assertEquals("longer-value",copy.toString());
  r.buffer().put(r.payload()+r.offsets()[1]*2,(byte)'X');assertEquals("mapped record changed",assertThrows(IOException.class,()->image.copyRecord(1,arena)).getMessage());assertEquals("longer-value",copy.toString());
 }
 public static final class ProbeProvider extends Provider{
  private static final long serialVersionUID=1L;
  ProbeProvider(){super("M3ImageHashProbe","1","QA-only SHA256 observer");put("MessageDigest.SHA-256",ProbeDigest.class.getName());}
 }
 public static final class ProbeDigest extends MessageDigestSpi{
  static final AtomicInteger acquired=new AtomicInteger();static volatile int failAt;static volatile IntConsumer afterDigest;
  final int ordinal=acquired.incrementAndGet();final MessageDigest delegate;
  public ProbeDigest(){try{delegate=MessageDigest.getInstance("SHA-256",oracleProvider);}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
  void guard(){if(ordinal==failAt)throw new ProviderException("probe fault "+ordinal);}
  @Override protected void engineUpdate(byte value){guard();delegate.update(value);}
  @Override protected void engineUpdate(byte[] value,int offset,int length){guard();delegate.update(value,offset,length);}
  @Override protected byte[] engineDigest(){guard();byte[] result=delegate.digest();IntConsumer callback=afterDigest;if(callback!=null)callback.accept(ordinal);return result;}
  @Override protected void engineReset(){delegate.reset();}
 }
 interface Throwing{void run()throws Throwable;}
 static void provider(Throwing task)throws Throwable{
  Provider p=new ProbeProvider();assertEquals(1,Security.insertProviderAt(p,1));
  try{assertSame(p,MessageDigest.getInstance("SHA-256").getProvider());ProbeDigest.acquired.set(0);ProbeDigest.failAt=0;ProbeDigest.afterDigest=null;task.run();}
  finally{ProbeDigest.afterDigest=null;ProbeDigest.failAt=0;Security.removeProvider(p.getName());}
 }
 @Test void providerAcquisitionsAndFailurePrecedenceUnchanged()throws Throwable{
  for(var words:List.of(List.<String>of(),List.of("four"),List.of("aaaa","bbbb","cccc"))){
   var r=ram(words,2,1);provider(()->{open(r.buffer());assertEquals(words.size()+2,ProbeDigest.acquired.get());});
  }
  var invalid=ram(List.of("aaaa"),2,0);invalid.buffer().putInt(72,123);seal(invalid.buffer());
  provider(()->{ProbeDigest.failAt=2;assertEquals("record Java hash mismatch",assertThrows(IOException.class,()->open(invalid.buffer())).getMessage());assertEquals(1,ProbeDigest.acquired.get());});
  var valid=ram(List.of("aaaa"),2,0);provider(()->{ProbeDigest.failAt=2;assertEquals("probe fault 2",assertThrows(ProviderException.class,()->open(valid.buffer())).getMessage());assertEquals(2,ProbeDigest.acquired.get());});
  evidence.put("providerAcquisitionCountsPreserved",true);evidence.put("validationBeforeProviderFault",true);
 }
 @Test void finalWholeImageChecksumStillDetectsGapMutation()throws Throwable{
  var r=ram(List.of("aaaa","bbbb","cccc"),2,1);
  provider(()->{ProbeDigest.afterDigest=n->{if(n==r.words().size()+1)r.buffer().put(r.payload(),(byte)0);};
   assertEquals("image changed while opening",assertThrows(IOException.class,()->open(r.buffer())).getMessage());assertEquals(r.words().size()+2,ProbeDigest.acquired.get());});
  evidence.put("finalImageMutationRefused",true);
 }
 static byte[] ownerBytes()throws Exception{
  ClassLoader loader=M3ImageHashLanesQualification.class.getClassLoader();for(var f:loader.getClass().getDeclaredFields()){if(!Map.class.isAssignableFrom(f.getType()))continue;f.setAccessible(true);
   if(f.get(loader) instanceof Map<?,?> m&&m.get("jdk.m3.text.SharedLexiconImage") instanceof ByteArrayOutputStream out)return out.toByteArray();}
  throw new AssertionError("RAM classbytes");
 }
 @Test void apiFieldsAndOtherMethodsAreBound()throws Exception{
  var surface=new TreeSet<String>();int[] shape={0,0,0};var other=new ClassWriter(0);
  new ClassReader(ownerBytes()).accept(new ClassVisitor(Opcodes.ASM9,other){
   @Override public FieldVisitor visitField(int access,String name,String desc,String sig,Object value){surface.add("F:"+access+":"+name+":"+desc);return super.visitField(access,name,desc,sig,value);}
   @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] exceptions){
    if((access&(Opcodes.ACC_PUBLIC|Opcodes.ACC_PROTECTED))!=0)surface.add("M:"+access+":"+name+desc);
    if(!name.equals("javaHash"))return super.visitMethod(access,name,desc,sig,exceptions);
    assertEquals(Opcodes.ACC_PRIVATE,access);assertEquals("(I)I",desc);
    return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String owner,String name,String descriptor,boolean itf){
     if(name.equals("getShort"))shape[0]++;if(name.equals("get")&&descriptor.equals("(I)B"))shape[1]++;if(owner.equals("java/lang/Short")&&name.equals("reverseBytes"))shape[2]++;
    }};
   }
  },ClassReader.SKIP_DEBUG);
  assertArrayEquals(mode.equals("after")?new int[]{1,2,1}:new int[]{0,2,0},shape);
  evidence.put("apiAndFieldFingerprint",hex(sha(String.join("\n",surface).getBytes(StandardCharsets.UTF_8))));
  evidence.put("nonHashMethodsFingerprint",hex(sha(other.toByteArray())));evidence.put("hashBytecodeShape",List.of(shape[0],shape[1],shape[2]));
 }

 static final java.util.concurrent.atomic.AtomicLong byteReads=new java.util.concurrent.atomic.AtomicLong(),longReads=new java.util.concurrent.atomic.AtomicLong();
 public static byte countedByte(ByteBuffer b,int at){byteReads.incrementAndGet();return b.get(at);}
 public static short countedShort(ByteBuffer b,int at){longReads.incrementAndGet();return b.getShort(at);}
 @Test void actualHashReadsAreReducedAtEveryTailBoundary()throws Throwable{
  var writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);
  new ClassReader(ownerBytes()).accept(new ClassVisitor(Opcodes.ASM9,writer){
   @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] exceptions){
    var delegate=super.visitMethod(access,name,desc,sig,exceptions);if(!name.equals("javaHash"))return delegate;
    return new MethodVisitor(Opcodes.ASM9,delegate){@Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){
     if(op==Opcodes.INVOKEVIRTUAL&&name.equals("get")&&desc.equals("(I)B"))
      super.visitMethodInsn(Opcodes.INVOKESTATIC,"jdk/m3/text/M3ImageHashLanesQualification","countedByte","(Ljava/nio/ByteBuffer;I)B",false);
     else if(op==Opcodes.INVOKEVIRTUAL&&name.equals("getShort")&&desc.equals("(I)S"))
      super.visitMethodInsn(Opcodes.INVOKESTATIC,"jdk/m3/text/M3ImageHashLanesQualification","countedShort","(Ljava/nio/ByteBuffer;I)S",false);
     else super.visitMethodInsn(op,owner,name,desc,itf);
    }};
   }
  },0);
  byte[] content=writer.toByteArray();ClassLoader child=new ClassLoader(M3ImageHashLanesQualification.class.getClassLoader()){
   @Override protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
    Class<?> type=findLoadedClass(name);if(type==null&&name.equals("jdk.m3.text.SharedLexiconImage"))type=defineClass(name,content,0,content.length);
    if(type==null)type=super.loadClass(name,false);if(resolve)resolveClass(type);return type;
   }
  };
  Class<?> type=child.loadClass("jdk.m3.text.SharedLexiconImage");assertSame(child,type.getClassLoader());
  var lookup=MethodHandles.privateLookupIn(type,MethodHandles.lookup());
  var ctor=lookup.findConstructor(type,MethodType.methodType(void.class,MappedByteBuffer.class)).asType(MethodType.methodType(Object.class,MappedByteBuffer.class));
  var hash=lookup.findVirtual(type,"javaHash",MethodType.methodType(int.class,int.class)).asType(MethodType.methodType(int.class,Object.class,int.class));
  var words=new TreeSet<String>();for(int size:List.of(0,1,2,3,4,5,7,8,9,15,16,17,31,32,33,63,64,65,129))words.add("Abcd".repeat((size+3)/4).substring(0,size));
  var r=ram(List.copyOf(words),2,1);Object image=(Object)ctor.invokeExact(r.buffer());byteReads.set(0);longReads.set(0);long units=0,full=0,tail=0;
  for(int i=0;i<r.words().size();i++){int answer=(int)hash.invokeExact(image,i);assertEquals(r.words().get(i).hashCode(),answer);int size=r.words().get(i).length();units+=size;full+=size/4;tail+=size%4;}
  assertEquals(mode.equals("after")?1:0,longReads.get());assertEquals(mode.equals("after")?2*(units-1):2*units,byteReads.get());
  evidence.put("executedReadCounts",Map.of("records",r.words().size(),"units",units,"fullLanes",full,"tailUnits",tail,"shortReads",longReads.get(),"byteReads",byteReads.get()));
 }

 @Test void actualReadOnlyFileStillOpensAndCopies()throws Exception{
  var image=SharedLexiconImage.open(existing);var arena=new LocalM3Arena();assertEquals(2,image.version());assertEquals(7,image.size());
  for(int i=0;i<image.size();i++)assertEquals(image.recordText(i),image.copyRecord(i,arena).toString());
 }
 @Test void incumbentSiSemantics()throws Exception{SharedLexiconCatalogSiWarmTest.semantics(existing);}
 @Test void incumbentSiWarm()throws Exception{SharedLexiconCatalogSiWarmTest.warm(existing);}
 @Test void incumbentSiConcurrentClose()throws Exception{SharedLexiconCatalogSiWarmTest.concurrent(existing);}
 static long hashBatch(SharedLexiconImage image,int[] rows,int calls)throws Throwable{
  long answer=0;for(int i=0;i<calls;i++)answer+=hash(image,rows[i&8191]);sink=answer;return answer;
 }
 static void measure()throws Throwable{
  var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();assertTrue(bean.isThreadAllocatedMemorySupported());bean.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().threadId();
  for(String scenario:List.of("empty","short-1","short-2","short-3","identifier","long-multilingual")){
   var r=ram(corpus(scenario,scenario.equals("empty")?1:8192),2,1);var image=open(r.buffer());var rows=new int[8192];var random=new Random(0x4d33484153484cL);for(int i=0;i<rows.length;i++)rows[i]=random.nextInt(image.size());
   for(int i=0;i<10;i++)hashBatch(image,rows,50000);
   long expected=0;for(int i=0;i<200000;i++)expected+=r.words().get(rows[i&8191]).hashCode();
   for(int sample=0;sample<7;sample++){long first=bean.getThreadAllocatedBytes(thread),time=System.nanoTime();long answer=hashBatch(image,rows,200000);long nanos=System.nanoTime()-time,bytes=bean.getThreadAllocatedBytes(thread)-first;assertEquals(expected,answer);
    measurements.add(Map.of("kind","hash","scenario",scenario,"sample",sample,"calls",200000,"nanos",nanos,"bytes",bytes,"answer",answer));}
  }
  for(int count:List.of(0,64,4096,16384)){
   var r=ram(corpus("identifier",count),2,1);for(int warm=0;warm<5;warm++)ownerSink=open(r.buffer());int calls=count<100?20:3;
   for(int sample=0;sample<7;sample++){long first=bean.getThreadAllocatedBytes(thread),time=System.nanoTime();long answer=0;
    for(int i=0;i<calls;i++){var image=open(r.buffer());ownerSink=image;answer+=image.size();}
    long nanos=System.nanoTime()-time,bytes=bean.getThreadAllocatedBytes(thread)-first;assertEquals((long)count*calls,answer);
    measurements.add(Map.of("kind","open-constructor","scenario",Integer.toString(count),"sample",sample,"calls",calls,"nanos",nanos,"bytes",bytes,"answer",answer));
   }
  }
 }
 public static void main(String[] a)throws Throwable{
  mode=a[0];existing=Path.of(a[1]);byte[] before=sha(Files.readAllBytes(existing));
  var vm=java.lang.management.ManagementFactory.getPlatformMXBean(com.sun.management.HotSpotDiagnosticMXBean.class);for(String option:List.of("UsePerfData","PerfDataSaveToFile"))assertEquals("false",vm.getVMOption(option).getValue());assertEquals("true",vm.getVMOption("PerfDisableSharedMem").getValue());
  assertEquals("false",vm.getVMOption("BackgroundCompilation").getValue());assertEquals("2",vm.getVMOption("CICompilerCount").getValue());
  evidence.put("vmCompilation",Map.of("BackgroundCompilation",vm.getVMOption("BackgroundCompilation").getValue(),"CICompilerCount",vm.getVMOption("CICompilerCount").getValue()));
  var firstRam=ram(corpus("identifier",16384),2,1);var firstBean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();assertTrue(firstBean.isThreadAllocatedMemorySupported());firstBean.setThreadAllocatedMemoryEnabled(true);
  long firstBytes=firstBean.getThreadAllocatedBytes(Thread.currentThread().threadId()),firstStart=System.nanoTime();var firstImage=open(firstRam.buffer());long firstNanos=System.nanoTime()-firstStart,firstAllocated=firstBean.getThreadAllocatedBytes(Thread.currentThread().threadId())-firstBytes;assertEquals(16384,firstImage.size());ownerSink=firstImage;
  evidence.put("firstImageConstruction",Map.of("records",16384,"nanos",firstNanos,"bytes",firstAllocated,"preparedFixture",true,"endToEndColdJvm",false));
  var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);launcher.execute(LauncherDiscoveryRequestBuilder.request().selectors(selectClass(M3ImageHashLanesQualification.class)).build());
  var s=listener.getSummary();s.printTo(new PrintWriter(System.out,true));s.printFailuresTo(new PrintWriter(System.out,true));if(s.getTestsFoundCount()!=16||s.getTestsSucceededCount()!=16||s.getTestsFailedCount()!=0||s.getTestsAbortedCount()!=0||s.getTestsSkippedCount()!=0)throw new AssertionError("JUnit counts");
  measure();assertArrayEquals(before,sha(Files.readAllBytes(existing)));
  var result=new LinkedHashMap<String,Object>();result.put("mode",mode);result.put("found",16);result.put("passed",16);result.put("failed",0);result.put("aborted",0);result.put("evidence",evidence);result.put("measurements",measurements);
  byte[] bytes=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(result);var connection=(HttpURLConnection)URI.create(a[2]).toURL().openConnection();connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setConnectTimeout(10000);connection.setReadTimeout(10000);connection.setFixedLengthStreamingMode(bytes.length);
  try(var out=connection.getOutputStream()){out.write(bytes);}if(connection.getResponseCode()!=204)throw new AssertionError("result receipt");connection.disconnect();System.out.println("M3_IMAGE_HASH_LANES_QUALIFICATION_PASS mode="+mode+" tests=16");
 }
}
