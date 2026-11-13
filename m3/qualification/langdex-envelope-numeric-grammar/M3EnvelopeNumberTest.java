// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
import jdk.m3.text.M3LangDexSourceEnvelope;
import jdk.m3.text.M3LangDexSourceEnvelope.Receipt;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
@TestMethodOrder(MethodOrderer.MethodName.class)
public final class M3EnvelopeNumberTest {
 static long checks;static final MessageDigest D=sha();static volatile Object sink;
 static final String REV="0123456789abcdef0123456789abcdef01234567";
 static final String HEAD="#M3-LANGDEX-SOURCE-1\t"+REV+"\nrow_type\tlexeme_id\tglottocode_b64\tlemma_b64\tsource_b64\tconcept_id\n";
 static final Method NUMBER=number();
 static MessageDigest sha(){try{return MessageDigest.getInstance("SHA-256");}catch(Exception e){throw new AssertionError(e);}}
 static Method number(){try{Method m=M3LangDexSourceEnvelope.class.getDeclaredMethod("requireCanonicalLong",String.class,boolean.class);m.setAccessible(true);return m;}catch(Exception e){throw new AssertionError(e);}}
 static void hash(String s){if(s==null){D.update((byte)0xff);return;}D.update((byte)0);for(int i=0;i<s.length();i++){char c=s.charAt(i);D.update((byte)c);D.update((byte)(c>>>8));}}
 static void check(boolean b,String label){checks++;assertTrue(b,label);hash(label);}
 static String parserFailure(NumberFormatException e){return "P|"+e.getClass().getName()+"|"+e.getMessage();}
 static String oracle(String s,boolean nullable){
  if(nullable&&"~".equals(s))return "OK";
  try{long n=Long.parseLong(s);return Long.toString(n).equals(s)?"OK":"C";}
  catch(NumberFormatException e){return parserFailure(e);}
 }
 static String numericIo(IOException e){
  check(e.getStackTrace()[0].getClassName().equals(M3LangDexSourceEnvelope.class.getName())&&e.getStackTrace()[0].getMethodName().equals("requireCanonicalLong"),"numeric failure top frame");
  if(e.getMessage().equals("LangDex integer is not canonical decimal")){
   check(e.getCause()==null,"canonical failure no cause");return "C";
  }
  check(e.getMessage().equals("LangDex integer is outside canonical signed-64-bit range"),"parser wrapper message");
  check(e.getCause() instanceof NumberFormatException,"parser cause type");return parserFailure((NumberFormatException)e.getCause());
 }
 static String actual(String s,boolean nullable)throws Exception{
  try{NUMBER.invoke(null,s,nullable);return "OK";}
  catch(InvocationTargetException e){if(e.getCause() instanceof IOException io)return numericIo(io);throw new AssertionError("unexpected numeric exception",e.getCause());}
 }
 static void same(String s,boolean nullable)throws Exception{
  String expected=oracle(s,nullable),observed=actual(s,nullable);checks++;assertEquals(expected,observed,"numeric oracle");hash(s);hash(Boolean.toString(nullable));hash(observed);
 }
 static String hex(byte[] b){return HexFormat.of().formatHex(b);}
 static final class Tracked extends ByteArrayInputStream{
  boolean closed;final int chunk;Tracked(byte[] b,int chunk){super(b);this.chunk=chunk;}
  @Override public synchronized int read(byte[] b,int off,int len){return super.read(b,off,Math.min(len,chunk));}
  @Override public void close(){closed=true;}
 }
 static Receipt receipt(byte[] data,long lex,long links){return new Receipt(REV,lex,links,hex(sha().digest(data)));}
 static String verify(String row,long lex,long links)throws Exception{
  byte[] data=(HEAD+row+"\n").getBytes(StandardCharsets.UTF_8);Tracked input=new Tracked(data,7);String out;
  try{Receipt expected=receipt(data,lex,links);check(M3LangDexSourceEnvelope.verify(input,expected).equals(expected),"matching receipt roundtrip");out="OK";}
  catch(IOException e){if(e.getMessage().startsWith("LangDex integer "))out=numericIo(e);else out="W|"+e.getMessage()+"|"+(e.getCause()==null);}
  check(!input.closed,"public stream remains open");hash(row);hash(out);return out;
 }
 @Test public void a_originalMainUnchanged()throws Exception{LangDexSourceEnvelopeTest.main(new String[0]);check(true,"original envelope main returned");}
 @Test public void b_extremaNullabilityAndParserPrecedence()throws Exception{
  List<String> cases=new ArrayList<>(Arrays.asList(null,"","~","0","1","-1","+0","+1","-0","00","01","-01","+","-","--1","1x"," 1","1 ","\t1","1\n","9223372036854775807","-9223372036854775808","9223372036854775808","-9223372036854775809","+9223372036854775808","09223372036854775808","-09223372036854775809","\u0661","\uff11","1\u0662","-\u0660","\ud835\udfd9"));
  cases.add("0".repeat(65536));cases.add("0".repeat(65536)+"1");cases.add("-"+"0".repeat(65536)+"1");
  for(String s:cases){same(s,true);same(s,false);}
 }
 @Test public void c_everyUtf16CodeUnitInNumericPlacements()throws Exception{
  for(int c=0;c<=65535;c++){String s=String.valueOf((char)c);same(s,true);same("1"+s,true);same("-"+s,true);}
 }
 @Test public void d_seededSignedRangeAndSpellingMutations()throws Exception{
  Random r=new Random(0x4d334e554d424552L);
  for(int i=0;i<12000;i++){long v=r.nextLong();String s=Long.toString(v);same(s,true);same("+"+s,true);same("0"+s,true);same("-0"+s,true);}
 }
 @Test public void e_matchingReceiptNumericColumnsAndInvalidOrder()throws Exception{
  List<String> values=List.of("~","0","1","-1","9223372036854775807","-9223372036854775808","07","+0","-0","+1","01","-01","","9223372036854775808","-9223372036854775809","+9223372036854775808","0".repeat(100)+"1");
  for(String s:values){
   String expected=oracle(s,true);
   check(verify("LEXEME\t"+s+"\t~\t~\t~\t-",1,0).equals(expected),"lexeme ID matching-digest grammar");
   check(verify("CONCEPT_LINK\t"+s+"\t-\t-\t-\t7",0,1).equals(expected),"link lexeme ID matching-digest grammar");
   check(verify("CONCEPT_LINK\t7\t-\t-\t-\t"+s,0,1).equals(expected),"link concept ID matching-digest grammar");
  }
  check(verify("LEXEME\t9223372036854775808\tbad\tbad\tbad\tbad",1,0).startsWith("P|"),"parse failure before later field errors");
  check(verify("LEXEME\t9223372036854775808\t~\t~\t~\t-",0,0).equals("W|LangDex lexeme row count exceeds receipt|true"),"lexeme count before numeric");
  check(verify("CONCEPT_LINK\t9223372036854775808\t-\t-\t-\twrong",0,0).equals("W|LangDex concept-link row count exceeds receipt|true"),"link count before numeric");
  check(verify("CONCEPT_LINK\t9223372036854775808\t-\t-\t-\t\u0661",0,1).equals("W|LangDex source wire must be ASCII with LF newlines|true"),"line ASCII validation before numeric");
  check(verify("CONCEPT_LINK\t1\tbad\t-\t-\t9223372036854775808",0,1).equals("W|LangDex unused field must be '-'|true"),"unused columns precede concept numeric");
 }
 @Test public void f_chunkedInputLifetimeAndApiShape()throws Exception{
  byte[] data=(HEAD+"LEXEME\t~\t~\t~\t~\t-\nLEXEME\t7\t~\t~\t~\t-\nLEXEME\t7\t~\t~\t~\t-\nCONCEPT_LINK\t7\t-\t-\t-\t~\n").getBytes(StandardCharsets.US_ASCII);Receipt expected=receipt(data,3,1);
  for(int chunk:List.of(1,2,7,8192)){Tracked input=new Tracked(data,chunk);check(M3LangDexSourceEnvelope.verify(input,expected).equals(expected),"chunked receipt");check(!input.closed,"chunked stream open");}
  for(Field f:M3LangDexSourceEnvelope.class.getDeclaredFields())check(Modifier.isStatic(f.getModifiers()),"no per-receipt retained field:"+f.getName());
  check(expected.lexemeRows()==3&&expected.conceptLinkRows()==1,"null/duplicate custody remains nonadmitting");
 }
 public static void measure()throws Exception{
  StringBuilder wire=new StringBuilder(HEAD);for(int i=0;i<1024;i++)wire.append("CONCEPT_LINK\t").append(9007199254740993L+i).append("\t-\t-\t-\t").append(700000000000000000L+i).append('\n');
  byte[] data=wire.toString().getBytes(StandardCharsets.US_ASCII);Receipt expected=receipt(data,0,1024);
  var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();check(bean.isThreadAllocatedMemorySupported(),"allocation counter supported");if(!bean.isThreadAllocatedMemoryEnabled())bean.setThreadAllocatedMemoryEnabled(true);
  for(int i=0;i<300;i++)sink=M3LangDexSourceEnvelope.verify(new ByteArrayInputStream(data),expected);
  for(int sample=0;sample<7;sample++){
   int it=100;long bytes=bean.getThreadAllocatedBytes(Thread.currentThread().threadId()),start=System.nanoTime();
   for(int i=0;i<it;i++)sink=M3LangDexSourceEnvelope.verify(new ByteArrayInputStream(data),expected);
   long ns=System.nanoTime()-start,size=bean.getThreadAllocatedBytes(Thread.currentThread().threadId())-bytes;
   System.out.println("M3_ENVELOPE_NUMBER_MEASURE\t"+sample+"\t"+it+"\t1024\t"+ns+"\t"+size);
  }
 }
 public static String api(){List<String> s=new ArrayList<>();for(Method m:M3LangDexSourceEnvelope.class.getDeclaredMethods())if(Modifier.isPublic(m.getModifiers()))s.add(m.toGenericString());for(Field f:M3LangDexSourceEnvelope.class.getDeclaredFields())s.add(f.getName()+":"+f.getType().getName()+":"+f.getModifiers());Collections.sort(s);return String.join("|",s);}
 public static String result(){return HexFormat.of().formatHex(D.digest());}
}
