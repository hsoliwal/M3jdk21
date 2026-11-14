// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
import jdk.m3.text.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;
import org.objectweb.asm.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
public final class M3BigStringUnitQualification {
 static String mode;static volatile long sink;
 static final Map<String,Object> evidence=new TreeMap<>();
 static final List<Map<String,Object>> measurements=new ArrayList<>();
 record Pair(M3BigString actual,jdk.m3.reference.M3BigString reference,String flat){}
 static Pair pair(CharSequence... chunks){StringBuilder flat=new StringBuilder();for(var c:chunks)for(int i=0;i<c.length();i++)flat.append(c.charAt(i));return new Pair(M3BigString.of(chunks),jdk.m3.reference.M3BigString.of(chunks),flat.toString());}
 static Pair slice(Pair p,int a,int b){return new Pair(p.actual.subSequence(a,b),p.reference.subSequence(a,b),p.flat.substring(a,b));}
 static void search(Pair p,String needle,long from){long expected=from>Integer.MAX_VALUE?(needle.isEmpty()?p.flat.length():-1):p.flat.indexOf(needle,(int)Math.max(0,from));assertEquals(expected,p.actual.indexOf(needle,from));assertEquals(p.reference.indexOf(needle,from),p.actual.indexOf(needle,from));}
 static void summary(Pair p){assertEquals(p.reference.summary().toString(),p.actual.summary().toString());assertEquals(p.flat.hashCode(),p.actual.summary().javaStringHash());assertEquals(p.flat.codePointCount(0,p.flat.length()),p.actual.summary().codePointCount());assertEquals(p.flat,p.actual.flatten());}
 static long bit(char c){int mixed=(c*0x9e3779b9)^(c>>>3)^(c<<7);return 1L<<(mixed&63);}
 static char collision(char target){for(int c=0;c<=65535;c++)if(c!=target&&bit((char)c)==bit(target))return (char)c;throw new AssertionError("collision");}
 static String sha(byte[] bytes)throws Exception{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}
 static Map<String,byte[]> classes()throws Exception{
  var loader=M3BigStringUnitQualification.class.getClassLoader();for(var f:loader.getClass().getDeclaredFields()){
   if(!Map.class.isAssignableFrom(f.getType()))continue;f.setAccessible(true);
   if(f.get(loader) instanceof Map<?,?>m&&m.get("jdk.m3.text.M3BigString") instanceof ByteArrayOutputStream){var out=new TreeMap<String,byte[]>();for(var e:m.entrySet())if(e.getKey() instanceof String n&&n.startsWith("jdk.m3.text.M3BigString")&&e.getValue() instanceof ByteArrayOutputStream b)out.put(n,b.toByteArray());return out;}
  }throw new AssertionError("RAM owner bytes");
 }
 @Test void unchangedBigStringContract()throws Exception{M3BigStringContractTest.main(new String[0]);}
 @Test void pinnedStreamingWindowSubset()throws Exception{M3BigStringStreamingWindowsOracle.main(new String[0]);}
 @Test void allUtf16UnitsAcrossStringAndOwnedLeaves(){
  var arena=new LocalM3Arena();long checks=0;
  for(int c=0;c<=65535;c++){String needle=String.valueOf((char)c);var text=pair("a",needle,"b",needle);
   for(long from:new long[]{Long.MIN_VALUE,0,1,2,3,4,5,Long.MAX_VALUE}){search(text,needle,from);checks++;}
   var piece=arena.copyUtf16(new char[]{'a',(char)c,'b',(char)c});var owned=pair(piece.subSequence(0,2),piece.subSequence(2,4));
   for(long from:new long[]{0,1,2,3,4}){search(owned,needle,from);checks++;}
  }
  for(int c=0;c<256;c++){var piece=arena.copyLatin1(new byte[]{(byte)c,0,(byte)c});search(pair(piece),String.valueOf((char)c),0);}
  evidence.put("utf16Units",65536);evidence.put("unitSearchCases",checks);evidence.put("latin1Units",256);
 }
 @Test void exhaustiveViewsAndConcatBoundaries(){
  var arena=new LocalM3Arena();var p=pair("ab","\uD83D","\uDE00","\r","\n","é",arena.copyUtf16(new char[]{'z','a'}));
  int windows=0;for(int a=0;a<=p.flat.length();a++)for(int b=a;b<=p.flat.length();b++){var v=slice(p,a,b);summary(v);
   for(String n:List.of("","a","b","\uD83D","\uDE00","😀","\r\n","é","z","az","zz"))for(long from:new long[]{-1,0,1,v.flat.length(),v.flat.length()+1L})search(v,n,from);windows++;
  }
  var whole=new Pair(M3BigString.concat(p.actual.subSequence(1,7),p.actual.subSequence(0,3)),jdk.m3.reference.M3BigString.concat(p.reference.subSequence(1,7),p.reference.subSequence(0,3)),p.flat.substring(1,7)+p.flat.substring(0,3));
  summary(whole);for(String n:List.of("a","b","é","\uD83D","\uDE00","😀","ab"))search(whole,n,0);
  var nested=new Pair(M3BigString.concat(whole.actual.subSequence(1,whole.actual.length()-1)),jdk.m3.reference.M3BigString.concat(whole.reference.subSequence(1,whole.reference.length()-1)),whole.flat.substring(1,whole.flat.length()-1));summary(nested);for(int i=0;i<nested.flat.length();i++)search(nested,String.valueOf(nested.flat.charAt(i)),0);
  evidence.put("exhaustiveWindows",windows);
 }
 @Test void seededRopesNeedlesAndRanges(){
  var random=new Random(0x4d3342494753494eL);int cases=0;
  for(int n=0;n<1000;n++){var chunks=new ArrayList<CharSequence>();for(int j=0;j<1+random.nextInt(6);j++){char[] chars=new char[random.nextInt(14)];for(int k=0;k<chars.length;k++)chars[k]=(char)random.nextInt(65536);chunks.add(new String(chars));}var p=pair(chunks.toArray(CharSequence[]::new));summary(p);
   for(int j=0;j<12;j++){String needle;if(j%3==0&&p.flat.length()>0){int a=random.nextInt(p.flat.length());needle=p.flat.substring(a,Math.min(p.flat.length(),a+1+random.nextInt(3)));}else needle=String.valueOf((char)random.nextInt(65536));search(p,needle,random.nextInt(p.flat.length()+7)-3);cases++;}
  }evidence.put("randomRopes",1000);evidence.put("randomSearchCases",cases);
 }
 @Test void longCoordinatesUseExistingViews(){
  String chunk="a".repeat(65536);M3BigString a=M3BigString.of(chunk);jdk.m3.reference.M3BigString r=jdk.m3.reference.M3BigString.of(chunk);
  for(int i=0;i<16;i++){a=M3BigString.concat(a,a);r=jdk.m3.reference.M3BigString.concat(r,r);}
  assertEquals(4294967296L,a.length());assertEquals(r.summary().toString(),a.summary().toString());
  a=M3BigString.concat(a,M3BigString.of("z"));r=jdk.m3.reference.M3BigString.concat(r,jdk.m3.reference.M3BigString.of("z"));
  for(String needle:List.of("a","z","az","","zz"))for(long from:new long[]{a.length()-4,a.length()-1,a.length(),a.length()+1,Long.MAX_VALUE})assertEquals(r.indexOf(needle,from),a.indexOf(needle,from));
  assertEquals(4294967296L,a.indexOf("z",4294967292L));assertEquals(0,a.indexOf("",Long.MIN_VALUE));assertThrows(IllegalStateException.class,a::asCharSequence);assertThrows(IllegalStateException.class,a::flatten);evidence.put("longUtf16Length",a.length());
 }
 static final class ScriptNeedle implements CharSequence {
  final List<String> events=new ArrayList<>();final int variant;int reads,lengths;
  ScriptNeedle(int variant){this.variant=variant;}
  @Override public int length(){events.add("length:"+lengths);lengths++;if(variant==4&&lengths==3)throw new IllegalStateException("script length");return 1;}
  @Override public char charAt(int at){events.add("char:"+at+":"+reads);reads++;if(variant==2&&reads==3)throw new IllegalArgumentException("script read");if(variant==3&&at==0)return reads<4?'a':'z';return variant==1?'z':'a';}
  @Override public CharSequence subSequence(int a,int b){throw new AssertionError("unexpected subsequence");}
  @Override public String toString(){throw new AssertionError("unexpected materialization");}
 }
 static String outcome(java.util.concurrent.Callable<Long> action){try{return "VALUE:"+action.call();}catch(Exception e){return e.getClass().getName()+":"+e.getMessage();}}
 @Test void arbitraryNeedleCallbackSchedulesPreserved(){
  var p=pair("aab","aa","baz");int cases=0;
  for(int variant=0;variant<5;variant++)for(long from:new long[]{-1,0,1,5,8,9,Long.MAX_VALUE}){
   var a=new ScriptNeedle(variant);var b=new ScriptNeedle(variant);
   String before=outcome(()->p.reference.indexOf(b,from)),after=outcome(()->p.actual.indexOf(a,from));assertEquals(before,after);assertEquals(b.events,a.events);cases++;
  }evidence.put("callbackCases",cases);
 }
 @Test void originalKmpAndNonStringNeedlesRemainExact(){
  var p=pair("xaa","ab","aab","\uD83D","\uDE00","az");
  for(String needle:List.of("aab","aaab","😀","ab😀","az","zz",""))for(int from=-2;from<=p.flat.length()+2;from++)search(p,needle,from);
  for(String needle:List.of("a","z","\uD83D","\uDE00","aab"))for(int from=-2;from<=p.flat.length()+2;from++)assertEquals(p.reference.indexOf(new StringBuilder(needle),from),p.actual.indexOf(new StringBuilder(needle),from));
  assertEquals(-1,M3BigString.of("ab","ca").indexOf("ac",0));evidence.put("generalKmpFallbackCovered",true);
 }
 @Test void bloomCollisionsAndExcludedRangeAreNotMatches(){
  char c=collision('z');String collision=String.valueOf(c).repeat(65);var p=pair(collision,collision,collision);
  assertTrue(p.actual.mayContainLiteral("z"));assertEquals(-1,p.actual.indexOf("z",0));assertEquals(p.reference.indexOf("z",0),p.actual.indexOf("z",0));
  var v=slice(pair(collision+"z"),0,collision.length());assertTrue(v.actual.mayContainLiteral("z"));assertEquals(-1,v.actual.indexOf("z",0));assertEquals(v.reference.indexOf("z",0),v.actual.indexOf("z",0));
  var joined=M3StringPiece.join(new LocalM3Arena().copyUtf16(collision.toCharArray()),new LocalM3Arena().copyLatin1(new byte[]{'z'}));
  search(pair(joined),"z",0);search(pair(joined).actual.length()>0?pair(joined):p,"z",collision.length());evidence.put("collisionUnit",(int)c);evidence.put("boundedStringRangeCovered",true);
 }
 @Test void summariesRegexAndStreamingRemainCompatible()throws Exception{
  var p=pair("ab\r","\n\uD83D","\uDE00é","\uD800x","\uDC00");for(String n:List.of("a","\r","\n","\uD83D","\uDE00","é","\uD800","\uDC00")){search(p,n,0);summary(p);}
  for(String regex:List.of("(?s).","\\X","(\\r\\n)","(😀)","(?<=ab)\\r\\n")){
   var a=p.actual.matcher(java.util.regex.Pattern.compile(regex));var r=java.util.regex.Pattern.compile(regex).matcher(p.flat);
   while(r.find()){assertTrue(a.find());assertEquals(r.start(),a.start());assertEquals(r.end(),a.end());assertEquals(r.group(),a.group());}assertFalse(a.find());
  }
  for(int a=0;a<=p.flat.length();a++)for(int b=a;b<=p.flat.length();b++){var out=new StringBuilder();p.actual.subSequence(a,b).appendTo(out);assertEquals(p.flat.substring(a,b),out.toString());}
  evidence.put("regexAndSummaryParity",true);
 }
 @Test void immutableInputDirectoryAndOwnedBytes(){
  var arena=new LocalM3Arena();char[] bytes={'a','z'};var piece=arena.copyUtf16(bytes);bytes[0]='x';M3StringPiece[] pieces={piece,piece.subSequence(1,2)};var joined=M3StringPiece.join(pieces);pieces[0]=arena.copyLatin1(new byte[]{'x'});CharSequence[] input={"a",joined};var p=pair(input);input[0]="x";summary(p);search(p,"z",0);assertEquals("aazz",p.actual.flatten());assertEquals(piece.storageIdentity(),piece.subSequence(1,2).storageIdentity());
  var failure=assertThrows(IllegalArgumentException.class,()->M3BigString.of(new StringBuilder("x")));assertEquals("chunks must be immutable String or M3StringPiece values",failure.getMessage());assertThrows(NullPointerException.class,()->p.actual.indexOf(null,0));evidence.put("inputOwnershipPreserved",true);
 }
 @Test void concurrentReadOnlySearch()throws Exception{
  var p=pair("a".repeat(256),"हिन्दी",M3StringPiece.join(new LocalM3Arena().copyUtf16(new char[]{'\uD83D'}),new LocalM3Arena().copyUtf16(new char[]{'\uDE00'})),"z");var pool=Executors.newFixedThreadPool(8);
  try{var jobs=new ArrayList<Callable<Integer>>();for(int t=0;t<8;t++)jobs.add(()->{for(int i=0;i<1000;i++)search(p,List.of("a","ह","\uD83D","\uDE00","z").get(i%5),i%p.flat.length());return 1000;});for(var f:pool.invokeAll(jobs))assertEquals(1000,f.get());}
  finally{pool.shutdown();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS));}evidence.put("concurrentQueries",8000);
 }
 static final AtomicInteger charReads=new AtomicInteger(),jdkRanges=new AtomicInteger(),failureArrays=new AtomicInteger();
 public static char read(CharSequence value,int at){charReads.incrementAndGet();return value.charAt(at);}
 public static int range(String value,int target,int a,int b){jdkRanges.incrementAndGet();return value.indexOf(target,a,b);}
 public static int[] failure(int length){failureArrays.incrementAndGet();return new int[length];}
 @Test void actualCompiledReadsAndFailureAllocationAreObserved()throws Exception{
  var all=classes();var writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);
  new ClassReader(all.get("jdk.m3.text.M3BigString")).accept(new ClassVisitor(Opcodes.ASM9,writer){
   @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] exceptions){var delegate=super.visitMethod(access,name,desc,sig,exceptions);if(!name.equals("indexOf")&&!name.equals("indexOfSingleUnit"))return delegate;
    return new MethodVisitor(Opcodes.ASM9,delegate){
     @Override public void visitMethodInsn(int op,String owner,String n,String d,boolean itf){
      if(owner.equals("java/lang/CharSequence")&&n.equals("charAt")&&d.equals("(I)C"))super.visitMethodInsn(Opcodes.INVOKESTATIC,"M3BigStringUnitQualification","read","(Ljava/lang/CharSequence;I)C",false);
      else if(owner.equals("java/lang/String")&&n.equals("indexOf")&&d.equals("(III)I"))super.visitMethodInsn(Opcodes.INVOKESTATIC,"M3BigStringUnitQualification","range","(Ljava/lang/String;III)I",false);
      else super.visitMethodInsn(op,owner,n,d,itf);
     }
     @Override public void visitIntInsn(int op,int operand){if(op==Opcodes.NEWARRAY&&operand==Opcodes.T_INT)super.visitMethodInsn(Opcodes.INVOKESTATIC,"M3BigStringUnitQualification","failure","(I)[I",false);else super.visitIntInsn(op,operand);}
    };
   }},0);all.put("jdk.m3.text.M3BigString",writer.toByteArray());
  ClassLoader child=new ClassLoader(M3BigStringUnitQualification.class.getClassLoader()){@Override protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{Class<?> type=findLoadedClass(name);byte[] bytes=all.get(name);if(type==null&&bytes!=null)type=defineClass(name,bytes,0,bytes.length);if(type==null)type=super.loadClass(name,false);if(resolve)resolveClass(type);return type;}};
  Class<?> type=child.loadClass("jdk.m3.text.M3BigString");CharSequence[] chunks=new CharSequence[256];Arrays.fill(chunks,"a".repeat(64));chunks[255]="a".repeat(63)+"z";Object rope=type.getMethod("of",CharSequence[].class).invoke(null,(Object)chunks);var method=type.getMethod("indexOf",CharSequence.class,long.class);
  charReads.set(0);jdkRanges.set(0);failureArrays.set(0);assertEquals(16383L,method.invoke(rope,"z",0L));int reads=charReads.get(),ranges=jdkRanges.get(),arrays=failureArrays.get();assertEquals(mode.equals("after")?1:32768,reads);assertEquals(mode.equals("after")?1:0,ranges);assertEquals(mode.equals("after")?0:1,arrays);
  charReads.set(0);jdkRanges.set(0);failureArrays.set(0);assertEquals(16383L,method.invoke(rope,new StringBuilder("z"),0L));assertEquals(32768,charReads.get());assertEquals(0,jdkRanges.get());assertEquals(1,failureArrays.get());
  evidence.put("instrumentedUnits",16384);evidence.put("instrumentedChunks",256);evidence.put("singleUnitCharSequenceReads",reads);evidence.put("singleUnitJdkRangeCalls",ranges);evidence.put("singleUnitFailureArrays",arrays);evidence.put("genericFallbackReads",charReads.get());
 }
 @Test void fieldsApiAndOtherMethodsAreUnchanged()throws Exception{
  var surface=new TreeSet<String>();var others=new TreeMap<String,String>();int[] ranges={0},flatten={0};
  for(var e:classes().entrySet()){var writer=new ClassWriter(0);new ClassReader(e.getValue()).accept(new ClassVisitor(Opcodes.ASM9,writer){
   @Override public FieldVisitor visitField(int access,String name,String desc,String sig,Object value){surface.add(e.getKey()+"/F:"+access+":"+name+":"+desc);return super.visitField(access,name,desc,sig,value);}
   @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] exceptions){
    if((access&(Opcodes.ACC_PUBLIC|Opcodes.ACC_PROTECTED))!=0)surface.add(e.getKey()+"/M:"+access+":"+name+desc);
    if(!name.equals("indexOf")&&!name.equals("indexOfSingleUnit"))return super.visitMethod(access,name,desc,sig,exceptions);
    return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String owner,String n,String d,boolean itf){if(owner.equals("java/lang/String")&&n.equals("indexOf")&&d.equals("(III)I"))ranges[0]++;if(n.equals("flatten")||n.equals("toString"))flatten[0]++;}};
   }},ClassReader.SKIP_DEBUG);others.put(e.getKey(),sha(writer.toByteArray()));}
  assertEquals(mode.equals("after")?1:0,ranges[0]);assertEquals(0,flatten[0]);evidence.put("apiFieldsFingerprint",sha(String.join("\n",surface).getBytes(StandardCharsets.UTF_8)));evidence.put("nonIndexFingerprint",sha(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(others)));evidence.put("jdkRangeBytecodeCalls",ranges[0]);evidence.put("indexMaterializationCalls",flatten[0]);
 }
 record Bench(String name,M3BigString rope,CharSequence needle,long from,int calls){}
 static long batch(Bench b,int calls){long value=0;for(int i=0;i<calls;i++)value+=b.rope.indexOf(b.needle,b.from);sink=value;return value;}
 static void measure(){
  var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();assertTrue(bean.isThreadAllocatedMemorySupported());bean.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().threadId();
  CharSequence[] ascii=new CharSequence[4096];Arrays.fill(ascii,"a".repeat(256));ascii[4095]="a".repeat(255)+"z";var big=M3BigString.of(ascii);
  CharSequence[] unicode=new CharSequence[4096];Arrays.fill(unicode,"ह".repeat(128));unicode[4095]="ह".repeat(127)+"z";var wide=M3BigString.of(unicode);
  var arena=new LocalM3Arena();CharSequence[] pieces=ascii.clone();var shared=arena.copyUtf16("a".repeat(256).toCharArray());Arrays.fill(pieces,shared);pieces[4095]="a".repeat(255)+"z";var pieceRope=M3BigString.of(pieces);
  String collide=String.valueOf(collision('z')).repeat(256);CharSequence[] collisions=new CharSequence[1024];Arrays.fill(collisions,collide);var collisionRope=M3BigString.of(collisions);
  var narrow=M3BigString.of(collide.repeat(4)+"z").subSequence(0,1024);
  CharSequence[] small=new CharSequence[256];Arrays.fill(small,"a".repeat(64));small[255]="a".repeat(63)+"z";var fallback=M3BigString.of(small);
  var cases=List.of(new Bench("first-ascii",big,"a",0,16384),new Bench("late-ascii",big,"z",0,8),new Bench("late-unicode",wide,"z",0,8),new Bench("late-owned-piece",pieceRope,"z",0,8),new Bench("bloom-collision-miss",collisionRope,"z",0,16),new Bench("bounded-range-miss",narrow,"z",0,128),new Bench("absent-whole-bloom",big,"Ω",0,16384),new Bench("generic-needle-fallback",fallback,new StringBuilder("z"),0,32),new Bench("multi-kmp-fallback",fallback,"az",0,32),new Bench("empty-needle",big,"",Long.MAX_VALUE,16384));
  for(var b:cases){for(int w=0;w<4;w++)batch(b,Math.min(b.calls,256));for(int n=0;n<7;n++){long allocated=bean.getThreadAllocatedBytes(thread),start=System.nanoTime(),answer=batch(b,b.calls);long nanos=System.nanoTime()-start,bytes=bean.getThreadAllocatedBytes(thread)-allocated;measurements.add(Map.of("scenario",b.name,"sample",n,"calls",b.calls,"nanos",nanos,"bytes",bytes,"answer",answer));}}
 }
 public static void main(String[] a)throws Exception{
  mode=a[0];var vm=java.lang.management.ManagementFactory.getPlatformMXBean(com.sun.management.HotSpotDiagnosticMXBean.class);assertEquals("false",vm.getVMOption("UsePerfData").getValue());assertEquals("true",vm.getVMOption("PerfDisableSharedMem").getValue());assertEquals("false",vm.getVMOption("BackgroundCompilation").getValue());assertEquals("2",vm.getVMOption("CICompilerCount").getValue());
  var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);launcher.execute(LauncherDiscoveryRequestBuilder.request().selectors(selectClass(M3BigStringUnitQualification.class)).build());var s=listener.getSummary();s.printTo(new PrintWriter(System.out,true));s.printFailuresTo(new PrintWriter(System.out,true));
  if(s.getTestsFoundCount()!=14||s.getTestsSucceededCount()!=14||s.getTestsFailedCount()!=0||s.getTestsSkippedCount()!=0||s.getTestsAbortedCount()!=0)throw new AssertionError("JUnit counts");measure();
  var result=new LinkedHashMap<String,Object>();result.put("mode",mode);result.put("found",14);result.put("passed",14);result.put("failed",0);result.put("aborted",0);result.put("evidence",evidence);result.put("measurements",measurements);
  byte[] bytes=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(result);var c=(HttpURLConnection)URI.create(a[1]).toURL().openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setFixedLengthStreamingMode(bytes.length);try(var out=c.getOutputStream()){out.write(bytes);}if(c.getResponseCode()!=204)throw new AssertionError("result");c.disconnect();System.out.println("M3_BIGSTRING_UNIT_QUALIFICATION_PASS mode="+mode+" tests=14");
 }
}
