/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
public class StringApiProbe {
 static final Field VALUE; static final MessageDigest DIGEST; static int checks;
 static {try {VALUE=String.class.getDeclaredField("value"); VALUE.setAccessible(true); DIGEST=MessageDigest.getInstance("SHA-256");}catch(Exception e){throw new ExceptionInInitializerError(e);}}
 static native String roundTrip(String s);
 static byte[] backing(String s)throws Exception{return (byte[])VALUE.get(s);}
 static void check(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}
 static void record(String s){for(char c:s.toCharArray()){DIGEST.update((byte)(c>>8));DIGEST.update((byte)c);}DIGEST.update((byte)255);}
 static void record(int i){record(Integer.toString(i));}
 static void expect(Class<? extends Throwable> c,Runnable r){try{r.run();throw new AssertionError("missing exception");}catch(Throwable t){check(c.isInstance(t));record(t.getClass().getName());}}
 static void exercise(char[] chars)throws Exception{
  char[] mutable=chars.clone();String s=new String(mutable),copy=new String(chars);Arrays.fill(mutable,'x');
  check(Arrays.equals(s.toCharArray(),chars));check(s!=copy&&s.equals(copy));check(s.compareTo(copy)==0);check(s.hashCode()==copy.hashCode());check(s.intern()==copy.intern());check(backing(s)==backing(new String(s)));
  check(new String(chars,0,chars.length).equals(s));check(new StringBuilder(s).toString().equals(s));check(new StringBuffer(s).toString().equals(s));check(roundTrip(s).equals(s));
  record(s);record(s.hashCode());record(s.codePointCount(0,s.length()));record(s+":"+copy);record(s.concat(copy));record(s.repeat(2));
  for(int i=0;i<s.length();i++){check(s.charAt(i)==chars[i]);record(s.codePointAt(i));record(s.codePointBefore(i+1));}
  for(int i=0;i<=s.length();i++){record(s.substring(i));record(s.substring(0,i));}
  for(String needle:new String[]{"","a","\u00e9","\ud83d\ude00","\ud800","xy"}){
   record(s.indexOf(needle));record(s.lastIndexOf(needle));record(s.compareTo(needle));record(s.replace(needle,"Q"));Matcher m=Pattern.compile(Pattern.quote(needle)).matcher(s);while(m.find()){record(m.start());record(m.end());}}
  record(s.replaceAll("(?s).","x"));record(s.toLowerCase(Locale.ROOT));record(s.toUpperCase(Locale.ROOT));
  for(Charset cs:new Charset[]{StandardCharsets.UTF_8,StandardCharsets.UTF_16,StandardCharsets.UTF_16LE,StandardCharsets.ISO_8859_1,StandardCharsets.US_ASCII}){
   byte[] b=s.getBytes(cs);record(Arrays.toString(b));String decoded=new String(b,cs);record(decoded);if(b.length>0)b[0]^=127;check(decoded.equals(new String(s.getBytes(cs),cs)));}
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ObjectOutputStream out=new ObjectOutputStream(bytes)){out.writeObject(s);}try(ObjectInputStream in=new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))){check(s.equals(in.readObject()));}
 }
 static void semantics()throws Exception{
  for(String s:new String[]{"","abc","Aa","BB","\0\0","\u00ff","\u0100","\uffff","a\ud800z","\udc00","\ud800\ud800","\udc00\ud800","\ud83d\ude00","\u0130\u00df\u03a3","  x\n\t"})exercise(s.toCharArray());
  Random rng=new Random(210035);for(int j=0;j<300;j++){char[] c=new char[rng.nextInt(25)];for(int k=0;k<c.length;k++)c[k]=(char)rng.nextInt(j%2==0?256:65536);exercise(c);}
  for(int n:new int[]{127,128,129,255,256,257,2048}){char[] c=new char[n];Arrays.fill(c,'q');String s=new String(c);check(s.length()==n);check(Arrays.equals(c,s.toCharArray()));record(s);}
  for(byte[] b:new byte[][]{{(byte)0xc0,(byte)0xaf},{(byte)0xed,(byte)0xa0,(byte)0x80},{(byte)0xff},{0,65,0}}){record(new String(b,StandardCharsets.UTF_8));record(new String(b,StandardCharsets.UTF_16));}
  check(new String(new int[]{0x1f600,0x100,0},0,3).equals("\ud83d\ude00\u0100\0"));
  expect(NullPointerException.class,()->new String((char[])null));expect(IndexOutOfBoundsException.class,()->new String(new char[2],-1,1));expect(IndexOutOfBoundsException.class,()->new String(new char[2],1,2));expect(IllegalArgumentException.class,()->new String(new int[]{0x110000},0,1));
  ExecutorService ex=Executors.newFixedThreadPool(8);List<Future<Boolean>> results=new ArrayList<>();for(int t=0;t<8;t++)results.add(ex.submit(()->{for(int i=0;i<30000;i++){char[] c={(char)i,(char)(i>>>8),'a','b'};String s=new String(c);if(!Arrays.equals(c,s.toCharArray())||!s.equals(new String(c)))return false;}return true;}));for(Future<Boolean> f:results)check(f.get());ex.shutdown();
  String a=new String("alpha\u0100omega".toCharArray()),b=new String(a.toCharArray());for(int i=0;i<200000;i++){check(a.equals(b));check(a.compareTo(b)==0);check(a.indexOf("omega")==6);check(a.hashCode()==b.hashCode());}
  System.out.println("SEMANTICS checks="+checks+" digest="+HexFormat.of().formatHex(DIGEST.digest()));
 }
 public static void main(String[] args)throws Exception{System.load(System.getProperty("probe.native"));semantics();}
}
