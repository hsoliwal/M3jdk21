// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
// CPU qualification adapter: reuse unchanged donor fixture/accessor/generation/cursor contracts.
package jdk.m3.text;
import java.io.*;import java.lang.management.ManagementFactory;import java.lang.reflect.*;import java.net.*;import java.nio.*;import java.nio.charset.StandardCharsets;import java.util.*;
import com.synexia.dictshared.api.IndexHandle;import jdk.m3.lexicon.M3TokenId;
import org.junit.jupiter.api.Test;import org.junit.platform.launcher.core.*;import org.junit.platform.launcher.listeners.*;
import static org.junit.jupiter.api.Assertions.*;import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
public final class M3WordIdentityQualification {
 static final Map<String,Object> metrics=new TreeMap<>();static volatile long blackhole;
 static Object invoke(Method m,Object...args)throws Exception{m.setAccessible(true);try{return m.invoke(null,args);}catch(InvocationTargetException e){if(e.getCause() instanceof Error x)throw x;if(e.getCause() instanceof Exception x)throw x;throw new AssertionError(e.getCause());}}
 static Object donor(String name,Class<?>[] types,Object...args)throws Exception{return invoke(M3WordRowsImageRefusalContractTest.class.getDeclaredMethod(name,types),args);}
 static ByteBuffer image(int language,long fingerprint)throws Exception{var b=(ByteBuffer)donor("fixture",new Class<?>[0]);b.putInt(12,language).putLong(24,fingerprint);return b;}
 static M3WordRows decode(ByteBuffer b)throws Exception{return (M3WordRows)invoke(M3WordRows.class.getDeclaredMethod("decode",ByteBuffer.class),b);}
 static M3WordRows rows()throws Exception{return decode(image(42,0x1122334455667788L));}
 @Test void unchangedDonorAccessorContract()throws Exception{donor("accessors",new Class<?>[]{M3WordRows.class},rows());}
 @Test void unchangedDonorGenerationContract()throws Exception{donor("generation",new Class<?>[]{M3WordRows.class},rows());}
 @Test void unchangedDonorCursorContract()throws Exception{donor("cursors",new Class<?>[]{M3WordRows.class,M3WordRows.class},rows(),rows());}
 @Test void languageEndpointsRoundTripActualOwners()throws Exception{
  int count=0;for(int language:new int[]{0,1,42,255,256,32767,32768,65534,65535}){
   var r=decode(image(language,7));for(int local=0;local<3;local++){
    long id=r.wordId(local);assertEquals(language,(int)(id>>>48));assertEquals(local,r.localId(id));assertEquals(128+(local<<7),r.rowByteOffset(local));count++;
    if(language!=0&&local!=0){assertEquals(id,IndexHandle.pack(language,local));IndexHandle.validate(id);}
   }
  }metrics.put("actualRowIdRoundTrips",count);
 }
 @Test void declaredSourceMappingCannotBeReplacedByBitIdentity()throws Exception{
  var r=rows();
  // This fixture declares Synexia entry1 as "a", which is target row0; target row1 is "ab".
  int declaredTargetRowForSourceEntryOne=0;long source=IndexHandle.pack(42,1);
  assertEquals(r.wordId(1),source);assertEquals(1,r.localId(source));
  assertNotEquals(r.flags(declaredTargetRowForSourceEntryOne),r.flags(r.localId(source)));
  assertEquals(5,r.flags(declaredTargetRowForSourceEntryOne));assertEquals(10,r.flags(r.localId(source)));
  long token=M3TokenId.pack(42,1);assertNotEquals(source,token);assertEquals(0,M3TokenId.languageId(source));
  assertThrows(IllegalArgumentException.class,()->r.localId(token));
  long high=IndexHandle.pack(42,1L<<47);IndexHandle.validate(high);assertEquals(1L<<47,IndexHandle.entryId(high));
  assertThrows(IllegalArgumentException.class,()->r.localId(high));metrics.put("rawIdentityAliasCounterexample",true);
 }
 @Test void zeroOverlayForeignAndBoundsRemainDomainSpecific()throws Exception{
  var zero=decode(image(0,1));assertEquals(0L,zero.wordId(0));assertEquals(0,zero.localId(0));
  assertThrows(IllegalArgumentException.class,()->IndexHandle.validate(0));
  assertThrows(IllegalArgumentException.class,()->IndexHandle.pack(0,1));
  assertThrows(IllegalArgumentException.class,()->IndexHandle.pack(42,0));
  assertThrows(IllegalArgumentException.class,()->M3TokenId.pack(42,-1));
  var r=rows();assertThrows(IndexOutOfBoundsException.class,()->r.wordId(-1));assertThrows(IndexOutOfBoundsException.class,()->r.wordId(3));
  for(long id:new long[]{IndexHandle.pack(43,1),r.wordId(0)|M3WordRows.OVERLAY_BIT,IndexHandle.pack(42,3),IndexHandle.pack(42,1L<<46)})assertThrows(IllegalArgumentException.class,()->r.localId(id));
  long maximum=IndexHandle.pack(IndexHandle.MAX_NAMESPACE,IndexHandle.MAX_ENTRY);assertEquals(-1L,maximum);IndexHandle.validate(maximum);
  assertEquals(IndexHandle.MAX_ENTRY,IndexHandle.entryId(maximum));assertEquals(IndexHandle.MAX_NAMESPACE,IndexHandle.namespaceId(maximum));
 }
 @Test void samePackedIdStillNeedsIndependentSnapshotBinding()throws Exception{
  var a=rows();var b=decode(image(42,a.lexiconFingerprint()^1));
  assertEquals(a.wordId(1),b.wordId(1));assertEquals(1,b.localId(a.wordId(1)));
  assertThrows(IllegalArgumentException.class,()->b.requireGeneration(a.languageId(),a.wordCount(),a.lexiconFingerprint(),a.languageTag(),a.dictionaryVersion()));
  assertThrows(IllegalArgumentException.class,()->a.cursor().reset(1).sameWord(b.cursor().reset(1)));
  assertThrows(IllegalArgumentException.class,()->a.requireGeneration(43,3,a.lexiconFingerprint(),null,null));
  assertEquals("tag",assertThrows(NullPointerException.class,()->a.requireGeneration(42,3,a.lexiconFingerprint(),null,"v3")).getMessage());
  metrics.put("packedIdContainsSnapshot",false);
 }
 @Test void explicitJavaBatchUsesExistingReadOnlyRows()throws Exception{
  var r=rows();assertFalse(M3WordRowsNative.available());assertArrayEquals(new int[]{15,5,10,5},M3WordRowsNative.flagsBatch(r,new int[]{2,0,1,0}));
  assertEquals(0,M3WordRowsNative.flagsBatch(r,new int[0]).length);
  assertThrows(IndexOutOfBoundsException.class,()->M3WordRowsNative.flagsBatch(r,new int[]{0,3}));
  assertEquals("rows",assertThrows(NullPointerException.class,()->M3WordRowsNative.flagsBatch(null,null)).getMessage());
  assertEquals("localIds",assertThrows(NullPointerException.class,()->M3WordRowsNative.flagsBatch(r,null)).getMessage());
  assertTrue(r.nativeImage().isReadOnly());assertThrows(ReadOnlyBufferException.class,()->r.nativeImage().put(0,(byte)0));assertFalse(M3WordRowsNative.available());
 }
 static long cursorLoop(M3WordRows.Cursor a,M3WordRows.Cursor b,int iterations){
  long sum=0;for(int i=0;i<iterations;i++){a.reset(i%3);b.reset((i+1)%3);sum+=a.flags()+a.corpusCount()+(a.isPrefixOf(b)?1:0)+(a.isSuffixOf(b)?1:0)+(a.sameWord(b)?7:0);}return sum;
 }
 @Test void calibratedPrecomputedCursorLoopHasNoObservedAllocation()throws Exception{
  var r=rows();var a=r.cursor();var b=r.cursor();
  var bean=assertInstanceOf(com.sun.management.ThreadMXBean.class,ManagementFactory.getThreadMXBean());assertTrue(bean.isThreadAllocatedMemorySupported());
  if(!bean.isThreadAllocatedMemoryEnabled())bean.setThreadAllocatedMemoryEnabled(true);
  long thread=Thread.currentThread().threadId();bean.getThreadAllocatedBytes(thread);
  for(int i=0;i<3;i++)blackhole=cursorLoop(a,b,100000);
  long[] emptyControlBytes=new long[2];
  for(int i=0;i<2;i++){long before=bean.getThreadAllocatedBytes(thread);long start=System.nanoTime();blackhole=cursorLoop(a,b,0);blackhole=System.nanoTime()-start;emptyControlBytes[i]=bean.getThreadAllocatedBytes(thread)-before;}
  metrics.put("emptyControlAllocatedBytes",emptyControlBytes);assertEquals(0L,emptyControlBytes[1]);
  int iterations=300000;long allocatedBefore=bean.getThreadAllocatedBytes(thread);long start=System.nanoTime();
  long checksum=cursorLoop(a,b,iterations);blackhole=checksum;
  long nanos=System.nanoTime()-start;long allocated=bean.getThreadAllocatedBytes(thread)-allocatedBefore;
  metrics.put("warmCursorIterations",iterations);metrics.put("warmCursorNanos",nanos);metrics.put("warmCursorAllocatedBytes",allocated);
  assertEquals(9100000L,checksum);assertEquals(0L,allocated);assertTrue(nanos>0);
 }
 public static void main(String[] args)throws Exception{
  var listener=new SummaryGeneratingListener();var l=LauncherFactory.create();l.registerTestExecutionListeners(listener);l.execute(LauncherDiscoveryRequestBuilder.request().selectors(selectClass(M3WordIdentityQualification.class)).build());
  var s=listener.getSummary();s.printTo(new PrintWriter(System.out,true));s.printFailuresTo(new PrintWriter(System.out,true));
  var checks=M3WordRowsImageRefusalContractTest.class.getDeclaredField("checks");checks.setAccessible(true);
  var written=M3WordRowsImageRefusalContractTest.class.getDeclaredField("WRITTEN");written.setAccessible(true);assertTrue(((List<?>)written.get(null)).isEmpty());
  var out=new TreeMap<String,Object>();out.put("found",s.getTestsFoundCount());out.put("passed",s.getTestsSucceededCount());out.put("failed",s.getTestsFailedCount());out.put("skipped",s.getTestsSkippedCount());out.put("aborted",s.getTestsAbortedCount());out.put("metrics",metrics);out.put("unmodifiedDonorAssertions",checks.getLong(null));out.put("donorFilesystemMainInvoked",false);out.put("productFileChannelOpenInvoked",false);out.put("donorJniLoadInvoked",false);
  byte[] data=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(out);var c=(HttpURLConnection)URI.create(args[0]).toURL().openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setFixedLengthStreamingMode(data.length);try(var stream=c.getOutputStream()){stream.write(data);}if(c.getResponseCode()!=204)throw new AssertionError("receipt");c.disconnect();
  if(s.getTestsFoundCount()!=9||s.getTestsSucceededCount()!=9||s.getTestsFailedCount()!=0||s.getTestsSkippedCount()!=0||s.getTestsAbortedCount()!=0)throw new AssertionError("word identity test counts");
  System.out.println("M3_WORD_IDENTITY_CONTRACT_PASS tests=9");
 }
}
