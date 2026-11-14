// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
// Bounded CPU qualification of the existing payload escape atom.
import jdk.m3.text.M3PrecomputePayload;
import java.io.*;
import java.lang.invoke.*;
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
public final class M3PayloadHexQualification {
 static String mode;static volatile Object sink;static volatile long answerSink;
 static final Map<String,Object> evidence=new TreeMap<>();
 static final List<Map<String,Object>> measurements=new ArrayList<>();
 static final MethodHandle APPEND;
 static {
  try{APPEND=MethodHandles.privateLookupIn(M3PrecomputePayload.class,MethodHandles.lookup()).findStatic(M3PrecomputePayload.class,"appendString",MethodType.methodType(StringBuilder.class,StringBuilder.class,String.class));}
  catch(ReflectiveOperationException e){throw new ExceptionInInitializerError(e);}
 }
 static StringBuilder append(StringBuilder out,String value)throws Throwable{return (StringBuilder)APPEND.invokeExact(out,value);}
 static String quoted(String value)throws Throwable{return append(new StringBuilder(),value).toString();}
 static String json(String value){return "{\"source_id\":"+legacyAppend(new StringBuilder(),value)+"}";}
 static String sha(byte[] bytes)throws Exception{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}
 static Map<String,byte[]> classes()throws Exception{
  var loader=M3PayloadHexQualification.class.getClassLoader();
  for(var f:loader.getClass().getDeclaredFields()){
   if(!Map.class.isAssignableFrom(f.getType()))continue;f.setAccessible(true);
   if(f.get(loader) instanceof Map<?,?>m&&m.get("jdk.m3.text.M3PrecomputePayload") instanceof ByteArrayOutputStream){
    var out=new TreeMap<String,byte[]>();for(var e:m.entrySet())if(e.getKey() instanceof String n&&n.startsWith("jdk.m3.text.M3PrecomputePayload")&&e.getValue() instanceof ByteArrayOutputStream b)out.put(n,b.toByteArray());return out;
   }
  }throw new AssertionError("RAM owner bytes");
 }
    private static StringBuilder legacyAppend(StringBuilder out, String value) {
        out.append('"');
        for (int at = 0; at < value.length(); at++) {
            char current = value.charAt(at);
            switch (current) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (current < 0x20 || current > 0x7f) out.append(String.format("\\u%04x", (int) current));
                    else out.append(current);
                }
            }
        }
        return out.append('"');
    }


 @Test void incumbentBase(){M3PrecomputePayloadTest.main(new String[0]);}
 @Test void incumbentParserContract(){M3PrecomputePayloadParserContractTest.main(new String[0]);}
 @Test void incumbentFieldPattern()throws Exception{M3PrecomputePayloadFieldPatternTest.main(new String[0]);}
 @Test void everyUtf16UnitAndPublicRoundTrip()throws Throwable{
  for(int unit=0;unit<=65535;unit++){
   String value=String.valueOf((char)unit);String expected=legacyAppend(new StringBuilder(),value).toString();
   assertEquals(expected,quoted(value));var p=M3PrecomputePayload.parse("{\"source_id\":"+expected+"}");
   assertEquals(value,p.requireString("source_id"));assertEquals("{\"source_id\":"+expected+"}",p.canonicalJson());
   assertEquals(p.fields(),M3PrecomputePayload.parse(p.canonicalJson()).fields());
  }evidence.put("utf16Units",65536);evidence.put("publicUnitRoundTrips",65536);
 }
 @Test void seededFullUtf16Composition()throws Throwable{
  var random=new Random(0x4d3348455834L);
  for(int n=0;n<4096;n++){char[] chars=new char[random.nextInt(65)];for(int i=0;i<chars.length;i++)chars[i]=(char)random.nextInt(65536);String value=new String(chars);
   String expected=legacyAppend(new StringBuilder("prefix/"),value).toString();var b=new StringBuilder("prefix/");assertSame(b,append(b,value));assertEquals(expected,b.toString());
   var p=M3PrecomputePayload.parse(json(value));assertEquals(value,p.requireString("source_id"));assertEquals(p.fields(),M3PrecomputePayload.parse(p.canonicalJson()).fields());
  }evidence.put("seededUtf16Strings",4096);
 }
 @Test void formatLocalesPreserveAsciiHex()throws Throwable{
  Locale old=Locale.getDefault(Locale.Category.FORMAT);int checks=0;
  try{for(String tag:List.of("und","en-US","fr-FR","tr-TR","ar-EG","hi-IN","th-TH-u-nu-thai","ja-JP","fa-IR","en-US-u-nu-arab")){
   Locale.setDefault(Locale.Category.FORMAT,tag.equals("und")?Locale.ROOT:Locale.forLanguageTag(tag));
   for(String v:List.of("","\u0000\u0001\u001f\u007f\u0080\u00ff","\u0123\uabcd\uffff","हिन्दी/汉字/العربية","😀\uD800/\uDC00","\"\\/\b\f\n\r\t")){
    assertEquals(legacyAppend(new StringBuilder(),v).toString(),quoted(v));checks++;
   }
  }}finally{Locale.setDefault(Locale.Category.FORMAT,old);}
  evidence.put("formatLocales",10);evidence.put("localeSamples",checks);
 }
 @Test void exactEscapePolicyAndChunkComposition()throws Throwable{
  assertEquals("\"\\u0000\\u0001\\u001f\u007f\\u0080\\u00ff\\uabcd\\ud800\\udc00\"",quoted("\u0000\u0001\u001f\u007f\u0080\u00ff\uabcd\uD800\uDC00"));
  assertEquals("\"\\\"\\\\/\\b\\f\\n\\r\\t\"",quoted("\"\\/\b\f\n\r\t"));
  for(String value:List.of("e\u0301","\u00e9","😀","\uDBFF\uDFFF","\uD800x\uDC00","a\u2028b\u2029c")){
   String whole=quoted(value);
   for(int cut=0;cut<=value.length();cut++){String a=quoted(value.substring(0,cut)),b=quoted(value.substring(cut));assertEquals(whole,a.substring(0,a.length()-1)+b.substring(1));}
  }evidence.put("escapePolicy","LOWERCASE_4_HEX_UNITS_DEL_RAW_NO_SURROGATE_REWRITE");
 }
 @Test void nullAndBuilderMutationBoundary()throws Throwable{
  for(String prefix:List.of("","prefix")){
   var actual=new StringBuilder(prefix);var expected=new StringBuilder(prefix);
   assertThrows(NullPointerException.class,()->legacyAppend(expected,null));assertThrows(NullPointerException.class,()->append(actual,null));assertEquals(expected.toString(),actual.toString());assertEquals(prefix+"\"",actual.toString());
  }
  for(String value:Arrays.asList(null,"","x","\uD800"))assertThrows(NullPointerException.class,()->append(null,value));
  var out=new StringBuilder("keep");assertSame(out,append(out,""));assertEquals("keep\"\"",out.toString());
  evidence.put("nullValuePrefixMutation","OPENING_QUOTE_BEFORE_NPE");evidence.put("nullBuilderCases",4);
 }
 @Test void compiledDonorAlphabetMatchesEveryNibble(){
  char[] digits=com.fasterxml.jackson.core.io.CharTypes.copyHexChars(false);
  assertEquals("0123456789abcdef",new String(digits));assertEquals("2.16.1",com.fasterxml.jackson.core.json.PackageVersion.VERSION.toString());
  for(int value=0;value<=65535;value++){String expected=String.format(Locale.ROOT,"%04x",value);assertEquals(expected,new String(new char[]{digits[value>>>12],digits[(value>>>8)&15],digits[(value>>>4)&15],digits[value&15]}));}
  evidence.put("compiledDonor","jackson-core-2.16.1");evidence.put("donorNibbleCases",65536);
 }
 @Test void canonicalCachePhasesAndFreshStrings()throws Exception{
  var cached=M3PrecomputePayload.class.getDeclaredField("canonicalJson");cached.setAccessible(true);var rendered=M3PrecomputePayload.class.getDeclaredField("canonicalJsonRendered");rendered.setAccessible(true);
  for(String value:List.of("","ascii","é/हिन्दी/汉字/😀","\uD800\u0001")){
   var p=M3PrecomputePayload.parse(json(value));assertNull(cached.get(p));assertFalse(rendered.getBoolean(p));
   String a=p.canonicalJson();assertNull(cached.get(p));assertTrue(rendered.getBoolean(p));
   String b=p.canonicalJson();Object held=cached.get(p);assertNotNull(held);assertNotSame(held,b);assertNotSame(a,b);
   String c=p.canonicalJson(),d=p.canonicalJson();assertEquals(a,b);assertEquals(a,c);assertEquals(a,d);assertNotSame(b,c);assertNotSame(c,d);assertSame(held,cached.get(p));
  }evidence.put("cachePhasesPreserved",true);evidence.put("freshApiStringsPreserved",true);
 }
 @Test void publicOwnershipAndMixedCanonicalKinds(){
  var p=M3PrecomputePayload.parse("{\"memberships\":[1,-2],\"subjects\":[-9223372036854775808,9223372036854775807],\"si_offset\":-0.0,\"si_prefixable\":false,\"source_id\":\"\\u0939\",\"unknown\":1.25e2}");
  var snapshot=p.fields();assertThrows(UnsupportedOperationException.class,()->snapshot.clear());
  var array=p.requireLongArray("subjects");array[0]=0;assertEquals(Long.MIN_VALUE,p.requireLongArray("subjects")[0]);
  String a=p.canonicalJson(),b=p.canonicalJson(),c=p.canonicalJson();assertEquals(a,b);assertEquals(a,c);assertEquals(snapshot,M3PrecomputePayload.parse(a).fields());assertEquals(-0.0,p.requireDouble("si_offset"));
  evidence.put("ownershipAndTypedFieldsPreserved",true);
 }
 @Test void concurrentCanonicalViews()throws Exception{
  var p=M3PrecomputePayload.parse(json("हिन्दी/汉字/😀/\uD800"));String expected=p.canonicalJson();var pool=Executors.newFixedThreadPool(8);
  try{var tasks=new ArrayList<Callable<Integer>>();for(int t=0;t<8;t++)tasks.add(()->{int n=0;for(int i=0;i<1024;i++){String got=p.canonicalJson();assertEquals(expected,got);assertEquals(p.fields(),M3PrecomputePayload.parse(got).fields());n++;}return n;});for(var f:pool.invokeAll(tasks))assertEquals(1024,f.get());}
  finally{pool.shutdown();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS));}
  evidence.put("concurrentReads",8192);
 }
 static final AtomicInteger formats=new AtomicInteger();
 public static String formatted(String pattern,Object[] args){formats.incrementAndGet();return String.format(pattern,args);}
 @Test void actualFormatterInvocationBoundary()throws Throwable{
  var all=classes();byte[] bytes=all.get("jdk.m3.text.M3PrecomputePayload");var writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);
  new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9,writer){
   @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] exceptions){var delegate=super.visitMethod(access,name,desc,sig,exceptions);if(!name.equals("appendString"))return delegate;
    return new MethodVisitor(Opcodes.ASM9,delegate){@Override public void visitMethodInsn(int op,String owner,String n,String d,boolean itf){
     if(owner.equals("java/lang/String")&&n.equals("format")&&d.equals("(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"))super.visitMethodInsn(Opcodes.INVOKESTATIC,"M3PayloadHexQualification","formatted",d,false);
     else super.visitMethodInsn(op,owner,n,d,itf);
    }};
   }},0);all.put("jdk.m3.text.M3PrecomputePayload",writer.toByteArray());
  ClassLoader child=new ClassLoader(M3PayloadHexQualification.class.getClassLoader()){
   @Override protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{Class<?> type=findLoadedClass(name);byte[] b=all.get(name);if(type==null&&b!=null)type=defineClass(name,b,0,b.length);if(type==null)type=super.loadClass(name,false);if(resolve)resolveClass(type);return type;}
  };
  var type=child.loadClass("jdk.m3.text.M3PrecomputePayload");var handle=MethodHandles.privateLookupIn(type,MethodHandles.lookup()).findStatic(type,"appendString",MethodType.methodType(StringBuilder.class,StringBuilder.class,String.class));
  String value="é😀\u0001";formats.set(0);StringBuilder out=(StringBuilder)handle.invokeExact(new StringBuilder(),value);assertEquals(legacyAppend(new StringBuilder(),value).toString(),out.toString());int calls=formats.get();assertEquals(mode.equals("after")?0:4,calls);
  formats.set(0);StringBuilder ascii=(StringBuilder)handle.invokeExact(new StringBuilder(),"ascii/\u007f");assertEquals("\"ascii/\u007f\"",ascii.toString());assertEquals(0,formats.get());
  evidence.put("executedFormatterCalls",calls);evidence.put("instrumentedGenericEscapeUnits",4);
 }
 @Test void apiFieldsAndNonEscapeBytecodeUnchanged()throws Exception{
  var surface=new TreeSet<String>();var others=new TreeMap<String,String>();int[] formats={0};
  for(var e:classes().entrySet()){var writer=new ClassWriter(0);
   new ClassReader(e.getValue()).accept(new ClassVisitor(Opcodes.ASM9,writer){
    @Override public FieldVisitor visitField(int access,String name,String desc,String sig,Object value){surface.add(e.getKey()+"/F:"+access+":"+name+":"+desc);return super.visitField(access,name,desc,sig,value);}
    @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] exceptions){
     if((access&(Opcodes.ACC_PUBLIC|Opcodes.ACC_PROTECTED))!=0)surface.add(e.getKey()+"/M:"+access+":"+name+desc);
     if(!name.equals("appendString"))return super.visitMethod(access,name,desc,sig,exceptions);
     return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String owner,String n,String d,boolean itf){if(owner.equals("java/lang/String")&&n.equals("format"))formats[0]++;}};
    }},ClassReader.SKIP_DEBUG);others.put(e.getKey(),sha(writer.toByteArray()));
  }
  assertEquals(mode.equals("after")?0:1,formats[0]);evidence.put("formatterBytecodeCalls",formats[0]);evidence.put("apiFieldsFingerprint",sha(String.join("\n",surface).getBytes(StandardCharsets.UTF_8)));evidence.put("nonEscapeFingerprint",sha(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(others)));
 }
 static String sample(String name){return switch(name){case "ascii"->"label/ASCII_0123456789_-";case "latin1"->"éàñößåµÿ".repeat(4);case "multilingual"->"हिन्दी/汉字/العربية/தமிழ்";case "supplementary"->"😀😁😂🚀🌍🧠".repeat(3);case "unpaired"->"\uD800x\uDC00y\uDBFFz\uDFFF".repeat(4);case "controls"->"\u0000\u0001\u001f\u007f\b\f\n\r\t\"\\/".repeat(3);default->throw new AssertionError(name);};}
 static long batch(String operation,String value,M3PrecomputePayload[] payloads,M3PrecomputePayload warm,StringBuilder builder,int calls)throws Throwable{
  long answer=0;for(int i=0;i<calls;i++){if(operation.equals("append")){builder.setLength(0);append(builder,value);answer+=builder.length();sink=builder;}else{String text=(operation.equals("warm")?warm:payloads[i]).canonicalJson();answer+=text.length();sink=text;}}answerSink=answer;return answer;
 }
 static M3PrecomputePayload[] prepare(String operation,String input,int calls){
  if(operation.equals("append")||operation.equals("warm"))return null;
  var p=new M3PrecomputePayload[calls];for(int i=0;i<calls;i++){p[i]=M3PrecomputePayload.parse(input);if(operation.equals("second"))p[i].canonicalJson();}return p;
 }
 static void measure()throws Throwable{
  var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();assertTrue(bean.isThreadAllocatedMemorySupported());bean.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().threadId();
  for(String scenario:List.of("ascii","latin1","multilingual","supplementary","unpaired","controls")){
   String value=sample(scenario),input=json(value);var warm=M3PrecomputePayload.parse(input);warm.canonicalJson();warm.canonicalJson();var builder=new StringBuilder(value.length()*6+2);
   for(String operation:List.of("append","first","second","warm")){
    int calls=operation.equals("append")||operation.equals("warm")?2048:512;
    for(int w=0;w<3;w++)batch(operation,value,prepare(operation,input,calls),warm,builder,calls);
    for(int n=0;n<7;n++){var payloads=prepare(operation,input,calls);long allocated=bean.getThreadAllocatedBytes(thread),start=System.nanoTime();long answer=batch(operation,value,payloads,warm,builder,calls);long nanos=System.nanoTime()-start,bytes=bean.getThreadAllocatedBytes(thread)-allocated;
     measurements.add(Map.of("operation",operation,"scenario",scenario,"sample",n,"calls",calls,"nanos",nanos,"bytes",bytes,"answer",answer));}
   }
  }
 }
 public static void main(String[] a)throws Throwable{
  mode=a[0];var vm=java.lang.management.ManagementFactory.getPlatformMXBean(com.sun.management.HotSpotDiagnosticMXBean.class);
  assertEquals("false",vm.getVMOption("UsePerfData").getValue());assertEquals("true",vm.getVMOption("PerfDisableSharedMem").getValue());assertEquals("false",vm.getVMOption("BackgroundCompilation").getValue());assertEquals("2",vm.getVMOption("CICompilerCount").getValue());
  var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);launcher.execute(LauncherDiscoveryRequestBuilder.request().selectors(selectClass(M3PayloadHexQualification.class)).build());var s=listener.getSummary();s.printTo(new PrintWriter(System.out,true));s.printFailuresTo(new PrintWriter(System.out,true));
  if(s.getTestsFoundCount()!=14||s.getTestsSucceededCount()!=14||s.getTestsFailedCount()!=0||s.getTestsAbortedCount()!=0||s.getTestsSkippedCount()!=0)throw new AssertionError("JUnit counts");
  measure();var result=new LinkedHashMap<String,Object>();result.put("mode",mode);result.put("found",14);result.put("passed",14);result.put("failed",0);result.put("aborted",0);result.put("evidence",evidence);result.put("measurements",measurements);
  byte[] bytes=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(result);var c=(HttpURLConnection)URI.create(a[1]).toURL().openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setFixedLengthStreamingMode(bytes.length);try(var out=c.getOutputStream()){out.write(bytes);}if(c.getResponseCode()!=204)throw new AssertionError("result");c.disconnect();System.out.println("M3_PAYLOAD_HEX_QUALIFICATION_PASS mode="+mode+" tests=14");
 }
}
