// Copyright 2026 Hitesh Soliwal, Synexia, and contributors.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.lexicon;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Standalone header-registry regression tests; this gate does not validate hierarchy bodies. */
public final class HierarchyHeaderTest {
 private static final String[] LEGACY={"M3LEX001","SYNARR01","MIWROWS1","SYNDICT1","SYNTULP1","SYNSEMG1","SNXSTR01","SNXLEX01","M3PAIR01","M3LIMG01","M3GEO001","M3FOLD01","M3RXW001","M3SEPB01"};
 private static boolean candidate(){return true;}
 // Layout provenance: Synexia MappedHierarchicalLexicon 4d08f568e0c44c717dc3595d5de5965bfc31b835
 // and Format 25a1b5e2b8fd3ffbd4c3d6da0eaeb6df3c8344b3. No donor runtime dependency.
 private static ByteBuffer donorHeader(int capacity,int version){
  ByteBuffer b=ByteBuffer.allocate(Math.max(128,capacity)).order(ByteOrder.LITTLE_ENDIAN);
  b.putLong(0x31524549484e5953L).putInt(version);b.position(0);b.limit(capacity);return b;
 }
 private static M3LexiconFormat hierarchy(){return M3LexiconFormat.valueOf("SYNHIER1");}
 private static void acceptOrUnknown(ByteBuffer b)throws Exception{
  if(candidate()){assertSame(hierarchy(),M3LexiconFormat.checkHeader(b));assertSame(hierarchy(),M3LexiconFormat.sniff(b).orElseThrow());}
  else {assertTrue(M3LexiconFormat.sniff(b).isEmpty());assertEquals("unknown lexicon image magic",assertThrows(M3LexiconFormatException.class,()->M3LexiconFormat.checkHeader(b)).getMessage());}
 }
 @Test void hierarchyHeaderFacts()throws Exception{
  ByteBuffer b=donorHeader(128,1);
  assertEquals(ByteOrder.LITTLE_ENDIAN,b.order());
  byte[] magic=new byte[8];b.get(0,magic);assertArrayEquals("SYNHIER1".getBytes(StandardCharsets.US_ASCII),magic);
  acceptOrUnknown(b);
  if(candidate()){assertEquals(0x53594e4849455231L,hierarchy().magic());assertSame(ByteOrder.LITTLE_ENDIAN,hierarchy().order());assertEquals(128,hierarchy().headerBytes());assertEquals("NONE",hierarchy().m3Owner());}
 }
 @Test void everyTruncatedHeaderRejected()throws Exception{
  for(int length=0;length<128;length++){
   ByteBuffer b=donorHeader(length,1);
   var e=assertThrows(M3LexiconFormatException.class,()->M3LexiconFormat.checkHeader(b),"cut "+length);
   assertEquals(candidate()&&length>=8?"truncated header":"unknown lexicon image magic",e.getMessage());
   assertEquals(candidate()&&length>=8,M3LexiconFormat.sniff(b).isPresent());
  }
 }
 @Test void versionBoundariesAndByteOrder()throws Exception{
  for(int version:new int[]{Integer.MIN_VALUE,-1,0,2,255,256,Integer.MAX_VALUE}){
   ByteBuffer b=donorHeader(128,version);assertEquals(candidate()?"unsupported version":"unknown lexicon image magic",assertThrows(M3LexiconFormatException.class,()->M3LexiconFormat.checkHeader(b)).getMessage());
  }
  ByteBuffer wrong=donorHeader(128,1);wrong.order(ByteOrder.BIG_ENDIAN).putInt(8,1);
  assertThrows(M3LexiconFormatException.class,()->M3LexiconFormat.checkHeader(wrong));
 }
 @Test void everyMagicByteMutationRefused()throws Exception{
  for(int i=0;i<8;i++)for(int bit=0;bit<8;bit++){
   ByteBuffer b=donorHeader(128,1);b.put(i,(byte)(b.get(i)^(1<<bit)));
   assertTrue(M3LexiconFormat.sniff(b).isEmpty());assertThrows(M3LexiconFormatException.class,()->M3LexiconFormat.checkHeader(b));
  }
 }
 @Test void absoluteOriginAndBufferEffects()throws Exception{
  for(boolean direct:new boolean[]{false,true})for(boolean readOnly:new boolean[]{false,true})for(ByteOrder order:new ByteOrder[]{ByteOrder.BIG_ENDIAN,ByteOrder.LITTLE_ENDIAN}){
   ByteBuffer raw=direct?ByteBuffer.allocateDirect(256):ByteBuffer.allocate(256);
   raw.position(37);raw.put(donorHeader(128,1));raw.position(37);raw.limit(165);ByteBuffer b=raw.slice();
   if(readOnly)b=b.asReadOnlyBuffer();
   b.order(order);b.position(19);b.mark();b.position(23);
   acceptOrUnknown(b);assertEquals(23,b.position());assertEquals(128,b.limit());assertSame(order,b.order());b.reset();assertEquals(19,b.position());
  }
 }
 @Test void originalFormatsAndOrdinalCoordinatesRemain()throws Exception{
  assertEquals(candidate()?15:14,M3LexiconFormat.values().length);
  for(int i=0;i<LEGACY.length;i++){
   var f=M3LexiconFormat.valueOf(LEGACY[i]);assertEquals(i,f.ordinal());assertSame(f,M3LexiconFormat.values()[i]);
   int[] sizes={64,64,104,128,8,8,72,40,176,224,256,64,128,64};assertEquals(sizes[i],f.headerBytes());
   ByteBuffer b=ByteBuffer.allocate(Math.max(16,f.headerBytes()));b.putLong(0,f.magic());b.order(f.order()).putInt(8,i==2?3:1);
   assertSame(f,M3LexiconFormat.checkHeader(b));assertSame(f,M3LexiconFormat.sniff(b).orElseThrow());
  }
  if(candidate())assertEquals(14,hierarchy().ordinal());
 }
 @Test void publicValuesCopiesDoNotExposePrecompute()throws Exception{
  var values=M3LexiconFormat.values();Arrays.fill(values,null);
  assertNotNull(M3LexiconFormat.values()[0]);
  ByteBuffer b=ByteBuffer.allocate(64).putLong(0,M3LexiconFormat.M3LEX001.magic()).putInt(8,1);
  assertSame(M3LexiconFormat.M3LEX001,M3LexiconFormat.checkHeader(b));
  acceptOrUnknown(donorHeader(128,1));
 }
 @Test void nullContractAndNoBodyAdmission()throws Exception{
  assertThrows(NullPointerException.class,()->M3LexiconFormat.sniff(null));assertThrows(NullPointerException.class,()->M3LexiconFormat.checkHeader(null));
  ByteBuffer b=donorHeader(128,1);
  // This gate is intentionally only the header: zeros below do not constitute a valid hierarchy body.
  b.putInt(12,Integer.MIN_VALUE);b.putLong(24,Long.MAX_VALUE);
  acceptOrUnknown(b);
 }
 @Test void concurrentHeadersHaveNoSharedCursorState()throws Exception{
  var executor=Executors.newFixedThreadPool(4);
  try{
   List<Callable<Boolean>> jobs=new ArrayList<>();
   for(int k=0;k<12;k++)jobs.add(()->{for(int i=0;i<250;i++){ByteBuffer b=donorHeader(128,1);b.position(12);acceptOrUnknown(b);assertEquals(12,b.position());}return true;});
   for(var f:executor.invokeAll(jobs))assertTrue(f.get());
  }finally{executor.shutdownNow();assertTrue(executor.awaitTermination(10,TimeUnit.SECONDS));}
 }
}
