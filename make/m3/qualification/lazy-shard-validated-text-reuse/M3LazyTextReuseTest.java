// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
import jdk.m3.text.*;
import jdk.m3.text.SharedLexiconCatalog.*;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.*;
import java.nio.*;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class M3LazyTextReuseTest {
 static long checks; static volatile Object sink;
 static final MessageDigest digest = sha();
 static final Method validate = method(SharedLexiconCatalog.class,"validateShardSidecars",int.class,SharedLexiconImage.class);
 static final Class<?> slotType = slotType();
 static MessageDigest sha(){try{return MessageDigest.getInstance("SHA-256");}catch(Exception e){throw new AssertionError(e);}}
 static Class<?> slotType(){try{return Class.forName("jdk.m3.text.SharedLexiconCatalog$ShardSlot");}catch(Exception e){throw new AssertionError(e);}}
 static Method method(Class<?> c,String n,Class<?>... t){try{Method m=c.getDeclaredMethod(n,t);m.setAccessible(true);return m;}catch(Exception e){throw new AssertionError(e);}}
 static Field field(Class<?> c,String n){try{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}catch(Exception e){throw new AssertionError(e);}}
 static void check(boolean ok,String label){checks++;assertTrue(ok,label);digest.update(label.getBytes(java.nio.charset.StandardCharsets.UTF_8));digest.update((byte)0);}
 static void text(String s){for(int i=0;i<s.length();i++){digest.update((byte)s.charAt(i));digest.update((byte)(s.charAt(i)>>>8));}}
 static void run(Fixture f,int shard) throws Exception {
  try{validate.invoke(f.catalog,shard,f.images.get(shard));}
  catch(InvocationTargetException e){Throwable t=e.getCause();if(t instanceof Exception x)throw x;throw (Error)t;}
 }
 static String failure(Fixture f,int shard) throws Exception {
  try{run(f,shard);throw new AssertionError("expected refusal");}
  catch(IOException e){String s=e.getClass().getName()+":"+e.getMessage()+":"+e.getStackTrace()[0].getClassName()+"#"+e.getStackTrace()[0].getMethodName();text(s);return e.getMessage();}
 }
 static SharedLexiconImage image(ByteBuffer b) throws Exception {
  check(b instanceof MappedByteBuffer,"direct buffer uses RAM-backed MappedByteBuffer subtype");
  Constructor<SharedLexiconImage> c=SharedLexiconImage.class.getDeclaredConstructor(MappedByteBuffer.class);c.setAccessible(true);
  return c.newInstance((MappedByteBuffer)b);
 }
 static ByteBuffer bytes(List<String> words) {
  int units=0;for(String s:words)units=Math.addExact(units,s.length());
  int payload=64+12*words.size();ByteBuffer b=ByteBuffer.allocateDirect(payload+units*2).order(ByteOrder.BIG_ENDIAN);
  b.putLong(0,0x4d334c4558303031L).putInt(8,2).putInt(12,words.size()).putLong(16,payload).putLong(24,units);
  int offset=0;for(int i=0;i<words.size();i++){
   String s=words.get(i);b.putInt(64+i*12,offset).putInt(68+i*12,s.length()).putInt(72+i*12,s.hashCode());
   for(int at=0;at<s.length();at++){int p=payload+(offset+at)*2;b.put(p,(byte)s.charAt(at)).put(p+1,(byte)(s.charAt(at)>>>8));}
   offset+=s.length();
  }
  MessageDigest d=sha();d.update(b.asReadOnlyBuffer().slice(0,32));d.update(b.asReadOnlyBuffer().slice(64,b.capacity()-64));b.put(32,d.digest());return b;
 }
 static final class Fixture {
  final SharedLexiconCatalog catalog;final List<SharedLexiconImage> images=new ArrayList<>();
  final List<ByteBuffer> buffers=new ArrayList<>();final List<Object> slots=new ArrayList<>();
  final List<List<String>> words;final int senses;
  Fixture(List<List<String>> input,int senses) throws Exception {
   this.words=input;this.senses=senses;List<String> files=new ArrayList<>(),first=new ArrayList<>(),last=new ArrayList<>();
   Map<Coordinate,List<SourceMapping>> mappings=new HashMap<>();Map<Coordinate,PrecomputeFacts> facts=new HashMap<>();
   Constructor<?> sc=slotType.getDeclaredConstructor(Path.class,String.class,String.class,String.class,int.class,long.class,byte[].class);sc.setAccessible(true);
   long records=0;
   for(int sh=0;sh<input.size();sh++){
    List<String> ws=input.get(sh);ByteBuffer b=bytes(ws);buffers.add(b);images.add(image(b));String file="ram-"+sh+".m3lex";
    long units=ws.stream().mapToLong(String::length).sum();slots.add(sc.newInstance(Path.of("ram-model",file),file,ws.get(0),ws.get(ws.size()-1),ws.size(),units,new byte[32]));
    files.add(file);first.add(ws.get(0));last.add(ws.get(ws.size()-1));records+=ws.size();
    for(int row=0;row<ws.size();row++){
     String s=ws.get(row);Coordinate co=new Coordinate(sh,row);List<SourceMapping> ms=new ArrayList<>();
     for(int sense=0;sense<senses;sense++)ms.add(new SourceMapping("fixture-source-"+sense,"fixture-path","fixture-kind","und",
       id(sh,row),new String(s),co,"MAPPING_"+sense,"SENSE_"+sense,"fixture-translation","fixture-profile","{\"flags\":"+(sense+7)+"}"));
     mappings.put(co,ms);int unpaired=0,nonBmp=0;boolean ascii=true,latin=true,space=false;
     for(int at=0;at<s.length();at++){char u=s.charAt(at);ascii&=u<128;latin&=u<256;space|=Character.isWhitespace(u);
      if(Character.isHighSurrogate(u)&&at+1<s.length()&&Character.isLowSurrogate(s.charAt(at+1))){nonBmp++;at++;ascii=false;latin=false;}
      else if(Character.isSurrogate(u))unpaired++;
     }
     facts.put(co,new PrecomputeFacts(co,s.length(),Integer.toUnsignedLong(s.hashCode()),s.codePointCount(0,s.length()),unpaired,nonBmp,ascii,latin,space,"fixture-profile"));
    }
   }
   Constructor<SharedLexiconCatalog> cc=SharedLexiconCatalog.class.getDeclaredConstructor(List.class,List.class,List.class,List.class,Map.class,Map.class,List.class,Optional.class,Optional.class,long.class);cc.setAccessible(true);
   catalog=cc.newInstance(slots,files,first,last,mappings,facts,List.of(),Optional.empty(),Optional.empty(),records);
  }
  void publish() throws Exception {for(int i=0;i<slots.size();i++)field(slotType,"image").set(slots.get(i),images.get(i));}
  boolean unpublished() throws Exception {for(Object s:slots)if(field(slotType,"image").get(s)!=null)return false;return true;}
  @SuppressWarnings("unchecked") List<SourceMapping>[] rows() throws Exception {return (List<SourceMapping>[])field(SharedLexiconCatalog.class,"mappingRows").get(catalog);}
  PrecomputeFacts[] facts() throws Exception {return (PrecomputeFacts[])field(SharedLexiconCatalog.class,"precomputeRows").get(catalog);}
 }
 static String id(int sh,int row){return Long.toString(9007199254740993L+(long)sh*100000+row);}
 static List<String> words(int count){List<String> r=new ArrayList<>();for(int i=0;i<count;i++)r.add("word-"+String.format(Locale.ROOT,"%08x",i)+"-shared-text");return List.copyOf(r);}
 static SourceMapping changed(SourceMapping m,String s){return new SourceMapping(m.sourceId(),m.sourcePath(),m.sourceKind(),m.languageTag(),m.recordId(),s,m.coordinate(),m.mappingId(),m.mappingName(),m.translationProfile(),m.precomputeProfile(),m.precomputePayload());}
 static PrecomputeFacts changed(PrecomputeFacts f,long length,long hash){return new PrecomputeFacts(f.coordinate(),length,hash,f.codePoints(),f.unpairedSurrogates(),f.nonBmpCodePoints(),f.ascii(),f.latin1(),f.containsWhitespace(),f.precomputeProfile());}
 @Test void a_allUtf16Units() throws Exception {
  List<String> all=new ArrayList<>(65536);for(int i=0;i<=65535;i++)all.add(String.valueOf((char)i));
  Fixture f=new Fixture(List.of(all),1);run(f,0);check(f.unpublished(),"validator does not publish image");
  f.publish();
  for(int i=0;i<=65535;i++){SourceMapping m=f.catalog.findMapping("fixture-source-0",id(0,i)).orElseThrow();check(m.lexeme().charAt(0)==(char)i,"utf16 identity");text(m.lexeme());}
  f.catalog.close();
 }
 @Test void b_multipleShardsAndSenses() throws Exception {
  List<String> w=words(93);Fixture f=new Fixture(List.of(w.subList(0,3),w.subList(3,33),w.subList(33,93)),3);
  for(int sh=0;sh<3;sh++)run(f,sh);f.publish();
  for(int sh=0;sh<3;sh++)for(int row=0;row<f.words.get(sh).size();row++){
   Coordinate co=new Coordinate(sh,row);List<SourceMapping> ms=f.catalog.mappingsAt(co);check(ms.size()==3,"all senses retained");
   for(int se=0;se<3;se++){SourceMapping m=ms.get(se);check(f.catalog.findMapping(m.sourceId(),m.recordId()).orElseThrow()==m,"same source mapping object");text(m.recordId());check(f.catalog.findPrecomputePayload(m.sourceId(),m.recordId()).orElseThrow().requireInt("flags")==se+7,"separate supplied owner payload");}
   check(f.catalog.find(f.words.get(sh).get(row)).orElseThrow().equals(co),"exact lookup routing");check(f.catalog.precomputeAt(co).utf16Units()==f.words.get(sh).get(row).length(),"facts coordinate");
  }
  check(f.catalog.findMapping("wrong-source",id(0,0)).isEmpty(),"source namespace isolation");f.catalog.close();
 }
 @Test void c_mappingFaultPrecedesFacts() throws Exception {
  for(int which:new int[]{0,1}){Fixture f=new Fixture(List.of(words(7)),2);List<SourceMapping>[] rs=f.rows();List<SourceMapping> row=new ArrayList<>(rs[6]);row.set(which,changed(row.get(which),"wrong"));rs[6]=List.copyOf(row);
   PrecomputeFacts p=f.facts()[0];f.facts()[0]=changed(p,p.utf16Units()+1,p.javaHash());check(failure(f,0).equals("mapping lexeme does not match image coordinate"),"complete mapping errors precede earlier facts error");check(f.unpublished(),"failed validation not published");f.catalog.close();}
 }
 @Test void d_factLengthAndUnsignedHashFaults() throws Exception {
  for(int which=0;which<2;which++){Fixture f=new Fixture(List.of(words(4)),2);PrecomputeFacts p=f.facts()[3];f.facts()[3]=changed(p,p.utf16Units()+(which==0?1:0),p.javaHash()+(which==1?1:0));
   check(failure(f,0).equals("precompute text facts do not match image"),"fact refusal exact");check(f.unpublished(),"facts failure not published");f.facts()[3]=p;run(f,0);check(f.unpublished(),"repaired validation leaves publication to existing caller");f.catalog.close();}
 }
 @Test void e_externalMutationBeforeValidation() throws Exception {
  Fixture f=new Fixture(List.of(List.of("alpha","omega")),1);ByteBuffer b=f.buffers.get(0);int at=64+2*12;byte old=b.get(at);b.put(at,(byte)'z');
  check(failure(f,0).equals("mapping lexeme does not match image coordinate"),"mutation before mapping validation refused");b.put(at,old);run(f,0);
  f.publish();LocalM3Arena arena=new LocalM3Arena();check(f.images.get(0).copyRecord(0,arena).toString().equals("alpha"),"immutable local copy");
  b.put(at,(byte)'z');IOException changed=assertThrows(IOException.class,()->f.images.get(0).copyRecord(0,arena));check(changed.getMessage().equals("mapped record changed"),"existing copy corruption detection unchanged");b.put(at,old);f.catalog.close();
 }
 @Test void f_unicodeSequencesAndLifecycle() throws Exception {
  TreeSet<String> ws=new TreeSet<>(List.of("a","a b","e"+(char)0x301,"latin-"+(char)0xff,"pair-"+(char)0xd83d+(char)0xde00,"high-"+(char)0xd800,"low-"+(char)0xdc00,"zero-"+(char)0));
  Fixture f=new Fixture(List.of(List.copyOf(ws)),2);run(f,0);f.publish();int row=0;
  for(String s:ws){Coordinate co=new Coordinate(0,row++);check(f.catalog.textAt(co).equals(s),"utf16 sequence materialization");text(s);check(f.catalog.find(s).orElseThrow().equals(co),"unicode lookup");}
  check(f.catalog.prefix("pair-",3).size()==1,"prefix preserved");
  M3PrecomputePayload escaped=f.catalog.findPrecomputePayload("fixture-source-1",id(0,0)).orElseThrow();f.catalog.close();f.catalog.close();
  check(escaped.requireInt("flags")==8,"escaped payload survives close");
  check(assertThrows(IllegalStateException.class,()->f.catalog.find("a")).getMessage().equals("catalog closed"),"close fences lookups");
 }
 @Test void g_concurrentImmutableValidation() throws Exception {
  Fixture f=new Fixture(List.of(words(127)),3);ExecutorService pool=Executors.newFixedThreadPool(8);
  try{List<Future<Boolean>> jobs=new ArrayList<>();for(int n=0;n<8;n++)jobs.add(pool.submit(()->{for(int i=0;i<20;i++)run(f,0);return true;}));
   for(Future<Boolean> job:jobs)check(job.get(30,TimeUnit.SECONDS),"concurrent complete validation");check(f.unpublished(),"concurrent validator adds no publication");}
  finally{pool.shutdownNow();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS));f.catalog.close();}
 }
 @Test void h_noNewRetainedState() throws Exception {
  Fixture f=new Fixture(List.of(words(9)),2);List<Field> fields=Arrays.stream(SharedLexiconCatalog.class.getDeclaredFields()).filter(x->!Modifier.isStatic(x.getModifiers())).sorted(Comparator.comparing(Field::getName)).toList();List<Object> before=new ArrayList<>();
  for(Field x:fields){x.setAccessible(true);before.add(x.get(f.catalog));}
  run(f,0);for(int i=0;i<fields.size();i++)check(before.get(i)==fields.get(i).get(f.catalog),"retained field identity");
  check(((Map<?,?>)field(SharedLexiconCatalog.class,"payloads").get(f.catalog)).isEmpty(),"validation adds no payload cache");f.catalog.close();
 }
 public static void measure() throws Exception {
  var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();check(bean.isThreadAllocatedMemorySupported(),"allocation counter supported");if(!bean.isThreadAllocatedMemoryEnabled())bean.setThreadAllocatedMemoryEnabled(true);
  Fixture f=new Fixture(List.of(words(1024)),2);for(int i=0;i<3000;i++)run(f,0);
  for(int sample=0;sample<7;sample++){int it=2000;long allocated=bean.getThreadAllocatedBytes(Thread.currentThread().threadId()),start=System.nanoTime();
   for(int i=0;i<it;i++)run(f,0);
   long ns=System.nanoTime()-start,size=bean.getThreadAllocatedBytes(Thread.currentThread().threadId())-allocated;
   System.out.println("M3_LAZY_MEASURE\t"+sample+"\t"+it+"\t1024\t"+ns+"\t"+size);}
  f.catalog.close();sink=f.catalog;
 }

 public static void mutationWindow(String variant) throws Exception {
  Fixture f=new Fixture(List.of(List.of("alpha")),1);List<SourceMapping> original=f.rows()[0];
  ByteBuffer b=f.buffers.get(0);int at=64+12;byte old=b.get(at);boolean[] changed={false};
  f.rows()[0]=new AbstractList<SourceMapping>(){
   public SourceMapping get(int index){return original.get(index);}
   public int size(){return original.size();}
   public Iterator<SourceMapping> iterator(){return new Iterator<SourceMapping>(){
    int index;
    public boolean hasNext(){if(index==1&&!changed[0]){b.put(at,(byte)'z');changed[0]=true;}return index<1;}
    public SourceMapping next(){if(index>=1)throw new NoSuchElementException();return original.get(index++);}
   };}
  };
  String outcome;
  try{run(f,0);outcome="ACCEPT_VERIFIED_TEXT";}catch(IOException e){outcome=e.getMessage();}
  assertTrue(changed[0]);
  assertEquals(variant.equals("before")?"precompute text facts do not match image":"ACCEPT_VERIFIED_TEXT",outcome);
  System.out.println("M3_MUTATION_WINDOW variant="+variant+" outcome="+outcome+" scope=OUTSIDE_IMMUTABLE_PUBLISHED_FILE_CONTRACT");
  b.put(at,old);f.catalog.close();
 }

 public static String api(){List<String> sig=new ArrayList<>();for(Method m:SharedLexiconCatalog.class.getDeclaredMethods())if(Modifier.isPublic(m.getModifiers()))sig.add(m.toGenericString());for(Field f:SharedLexiconCatalog.class.getDeclaredFields())if(!Modifier.isStatic(f.getModifiers()))sig.add(f.getName()+":"+f.getType().getTypeName()+":"+f.getModifiers());Collections.sort(sig);return String.join("|",sig);}
 public static String result(){return HexFormat.of().formatHex(digest.digest());}
}
