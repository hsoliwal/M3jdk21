// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
// Constructor-only qualification; direct buffers exercise the existing byte owner without filesystem output.
import jdk.m3.text.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;
import org.objectweb.asm.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
public final class M3ImageWindowQualification {
 static String mode;static volatile Object sink;static volatile long answerSink;
 static final Map<String,Object> evidence=new TreeMap<>();
 static final List<Map<String,Object>> measurements=new ArrayList<>();
 static final Class<?> REFERENCE=jdk.m3.text.SharedLexiconImageReference.class;
 static final AtomicInteger factories=new AtomicInteger(),arrayDigests=new AtomicInteger(),directDigests=new AtomicInteger(),recordViews=new AtomicInteger(),sliceViews=new AtomicInteger(),readOnlyViews=new AtomicInteger();
 static volatile Runnable digestHook;
 @FunctionalInterface interface Action{void run()throws Throwable;}
 static ByteBuffer direct(byte[] bytes){return ByteBuffer.allocateDirect(bytes.length).put(bytes).flip();}
 static Object construct(Class<?> type,ByteBuffer data)throws Throwable{
  var c=type.getDeclaredConstructor(MappedByteBuffer.class);c.setAccessible(true);
  try{return c.newInstance((MappedByteBuffer)data);}catch(InvocationTargetException e){throw e.getCause();}
 }
 static SharedLexiconImage image(byte[] bytes)throws Throwable{return (SharedLexiconImage)construct(SharedLexiconImage.class,direct(bytes));}
 static byte[] table(Object image)throws Exception{var f=image.getClass().getDeclaredField("recordDigests");f.setAccessible(true);return ((byte[])f.get(image)).clone();}
 static byte[] bytes(int version,List<String> words)throws Exception{return SharedLexiconImageContractTest.image(version,words);}
 static String sha(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
 static void parity(int version,List<String> words)throws Throwable{
  byte[] sealed=bytes(version,words);var actual=image(sealed);var expected=construct(REFERENCE,direct(sealed));
  assertArrayEquals(table(expected),table(actual));assertEquals(words.size(),actual.size());assertEquals(version,actual.version());
  assertEquals(REFERENCE.getMethod("imageIdentity").invoke(expected),actual.imageIdentity());
  long units=0;var arena=new LocalM3Arena();
  for(int i=0;i<words.size();i++){assertEquals(words.get(i),actual.copyRecord(i,arena).flatten());units+=words.get(i).length();}
  assertEquals(units,actual.utf16Units());
 }
 static String outcome(Class<?> owner,byte[] b)throws Throwable{
  try{construct(owner,direct(b));return "ACCEPT";}catch(Exception|AssertionError e){return e.getClass().getName()+":"+e.getMessage();}
 }
 static void refusal(byte[] b,String fragment)throws Throwable{
  String original=outcome(REFERENCE,b);assertNotEquals("ACCEPT",original);assertTrue(original.contains(fragment),original);assertEquals(original,outcome(SharedLexiconImage.class,b));
 }
 @Test void directBufferSubtypeActualConstructor()throws Throwable{
  var b=direct(bytes(1,List.of("a","b")));assertInstanceOf(MappedByteBuffer.class,b);
  var actual=(SharedLexiconImage)construct(SharedLexiconImage.class,b);assertEquals(2,actual.size());
  evidence.put("directBufferSubtype",b.getClass().getName());evidence.put("filesystemCalls",0);
 }
 @Test void everyUtf16UnitBothVersions()throws Throwable{
  var words=new ArrayList<String>();for(int c=0;c<=65535;c++)words.add(String.valueOf((char)c));
  for(int v:List.of(1,2))parity(v,words);
  evidence.put("utf16Units",65536);evidence.put("maximumRecords",65536);evidence.put("allUnitVersionCases",131072);
 }
 @Test void digestPaddingAndBoundaryRecords()throws Throwable{
  var words=new TreeSet<String>();for(int n:new int[]{0,1,3,4,27,28,31,32,33,55,56,63,64,65,127,128,129,1024})words.add("a".repeat(n));
  words.addAll(List.of("हिन्दी","العربية","汉字","😀","\ud800","\udfff","ab\0cd","é"));
  parity(1,List.copyOf(words));parity(2,List.copyOf(words));evidence.put("boundaryRecords",words.size());
 }
 @Test void seededValidImagesCompareOriginal()throws Throwable{
  var random=new Random(0x4d33444947455354L);int rows=0;
  for(int n=0;n<256;n++){var set=new TreeSet<String>();int count=1+random.nextInt(40);
   while(set.size()<count){char[] word=new char[random.nextInt(33)];for(int i=0;i<word.length;i++)word[i]=(char)random.nextInt(65536);set.add(new String(word));}
   var words=List.copyOf(set);parity(n%2+1,words);rows+=words.size();
  }evidence.put("seededImages",256);evidence.put("seededRows",rows);
 }
 @Test void emptyAndFirstRecordBoundaries()throws Throwable{
  for(int v:List.of(1,2)){parity(v,List.of());parity(v,List.of(""));parity(v,List.of("","x"));}
  var b=ByteBuffer.wrap(bytes(1,List.of("a","b"))).order(ByteOrder.BIG_ENDIAN);b.putInt(64,-1);
  refusal(SharedLexiconImageContractTest.seal(b.array()),"invalid record range");evidence.put("emptyVariants",6);
 }
 @Test void dimensionChecksumAndFailurePrecedence()throws Throwable{
  byte[] good=bytes(2,List.of("ab","cd"));
  for(int offset:new int[]{0,8,12,16,24,32,good.length-1}){byte[] bad=good.clone();bad[offset]^=(byte)0x80;refusal(bad,offset==0?"unsupported image header":offset==8?"unsupported image version":offset<32?"invalid image dimensions":"image checksum mismatch");}
  var bad=ByteBuffer.wrap(good.clone()).order(ByteOrder.BIG_ENDIAN);bad.putInt(64,-1);refusal(SharedLexiconImageContractTest.seal(bad.array()),"invalid record range");
  bad=ByteBuffer.wrap(good.clone()).order(ByteOrder.BIG_ENDIAN);bad.putInt(72,7);refusal(SharedLexiconImageContractTest.seal(bad.array()),"record Java hash mismatch");
  refusal(bytes(1,List.of("b","a")),"records must be UTF-16 sorted");refusal(bytes(2,List.of("a","a")),"records must be UTF-16 sorted");
  refusal(new byte[63],"unsupported image header");evidence.put("refusalFamilies",7);
 }
 @Test void postAdmissionMutationCannotPublishRecord()throws Throwable{
  byte[] good=bytes(2,List.of("ab","cd"));ByteBuffer b=direct(good);var actual=(SharedLexiconImage)construct(SharedLexiconImage.class,b);
  b.put(good.length-1,(byte)99);var arena=new LocalM3Arena();var e=assertThrows(IOException.class,()->actual.copyRecord(1,arena));assertEquals("mapped record changed",e.getMessage());
  assertEquals("ab",actual.copyRecord(0,arena).flatten());evidence.put("postAdmissionMutationRefused",true);
 }
 @Test void absentProviderRefusesAndRestores()throws Throwable{
  byte[] sealed=bytes(1,List.of("a","b"));var admitted=image(sealed);
  Class<?> action=Class.forName("SharedLexiconImageContractTest$Action");var method=SharedLexiconImageContractTest.class.getDeclaredMethod("withoutSha256",action);method.setAccessible(true);
  Object proxy=java.lang.reflect.Proxy.newProxyInstance(action.getClassLoader(),new Class<?>[]{action},(p,m,a)->{
   if(m.getName().equals("run")){for(Class<?> owner:List.of(SharedLexiconImage.class,REFERENCE)){var e=assertThrows(AssertionError.class,()->construct(owner,direct(sealed)));assertInstanceOf(NoSuchAlgorithmException.class,e.getCause());}var e=assertThrows(AssertionError.class,()->admitted.copyRecord(0,new LocalM3Arena()));assertInstanceOf(NoSuchAlgorithmException.class,e.getCause());return null;}throw new AssertionError(m);
  });
  try{method.invoke(null,proxy);}catch(InvocationTargetException e){throw e.getCause();}
  assertEquals("a",admitted.copyRecord(0,new LocalM3Arena()).flatten());evidence.put("pinnedProviderHelperExecuted",true);
 }
 public static final class CountingSha extends MessageDigestSpi {
  private final MessageDigest delegate;
  public CountingSha(){try{delegate=MessageDigest.getInstance("SHA-256","SUN");factories.incrementAndGet();}catch(GeneralSecurityException e){throw new AssertionError(e);}}
  @Override protected void engineUpdate(byte b){delegate.update(b);}
  @Override protected void engineUpdate(byte[] b,int o,int n){delegate.update(b,o,n);}
  @Override protected byte[] engineDigest(){arrayDigests.incrementAndGet();byte[] out=delegate.digest();hook();return out;}
  @Override protected int engineDigest(byte[] b,int o,int n)throws DigestException{directDigests.incrementAndGet();int out=delegate.digest(b,o,n);hook();return out;}
  @Override protected void engineReset(){delegate.reset();}
  @Override protected int engineGetDigestLength(){return 32;}
 }
 public static final class FallbackSha extends MessageDigestSpi {
  private final MessageDigest delegate;
  public FallbackSha(){try{delegate=MessageDigest.getInstance("SHA-256","SUN");factories.incrementAndGet();}catch(GeneralSecurityException e){throw new AssertionError(e);}}
  @Override protected void engineUpdate(byte b){delegate.update(b);}
  @Override protected void engineUpdate(byte[] b,int o,int n){delegate.update(b,o,n);}
  @Override protected byte[] engineDigest(){arrayDigests.incrementAndGet();byte[] out=delegate.digest();hook();return out;}
  @Override protected void engineReset(){delegate.reset();}
  @Override protected int engineGetDigestLength(){return 32;}
 }
 static void hook(){Runnable r=digestHook;if(r!=null)r.run();}
 static void provider(Class<? extends MessageDigestSpi> type,Action body)throws Throwable{
  String name="M3ImageWindowQualificationProvider";assertNull(Security.getProvider(name));
  Provider p=new Provider(name,"1.0","Bounded SHA256 test adapter"){private static final long serialVersionUID=1L;};
  p.put("MessageDigest.SHA-256",type.getName());assertEquals(1,Security.insertProviderAt(p,1));
  factories.set(0);arrayDigests.set(0);directDigests.set(0);
  try{body.run();}finally{digestHook=null;Security.removeProvider(name);}
 }
 @Test void directProviderResetsPerRecordAndSkipsEmptyFactory()throws Throwable{
  byte[] sealed=bytes(2,List.of("","a","abcd","x".repeat(64),"😀"));
  byte[] empty=bytes(1,List.of());byte[] expected=table(construct(REFERENCE,direct(sealed)));
  provider(CountingSha.class,()->{var actual=image(sealed);assertArrayEquals(expected,table(actual));assertEquals(!mode.equals("original")?3:7,factories.get());assertEquals(!mode.equals("original")?2:7,arrayDigests.get());assertEquals(!mode.equals("original")?5:0,directDigests.get());evidence.put("providerFactories",factories.get());evidence.put("providerArrayDigests",arrayDigests.get());evidence.put("providerDirectDigests",directDigests.get());
   factories.set(0);arrayDigests.set(0);directDigests.set(0);assertEquals(0,image(empty).size());assertEquals(2,factories.get());assertEquals(2,arrayDigests.get());assertEquals(0,directDigests.get());
  });evidence.put("emptyFactories",2);
 }
 @Test void defaultSpiFallbackRemainsCompatible()throws Throwable{
  byte[] sealed=bytes(1,List.of("","abcd","x".repeat(64),"😀"));byte[] expected=table(construct(REFERENCE,direct(sealed)));
  provider(FallbackSha.class,()->{assertArrayEquals(expected,table(image(sealed)));assertEquals(!mode.equals("original")?3:6,factories.get());assertEquals(6,arrayDigests.get());assertEquals(0,directDigests.get());});evidence.put("defaultSpiFallbackCovered",true);
 }
 @Test void imageMutationDuringHashingStillRefuses()throws Throwable{
  byte[] sealed=bytes(2,List.of("a","b"));ByteBuffer b=direct(sealed);
  provider(CountingSha.class,()->{var calls=new AtomicInteger();digestHook=()->{if(calls.incrementAndGet()==2)b.put(0,(byte)(b.get(0)^1));};
   var e=assertThrows(IOException.class,()->construct(SharedLexiconImage.class,b));assertEquals("image changed while opening",e.getMessage());
  });evidence.put("changedWhileOpeningRefused",true);
 }
 @Test void concurrentIndependentConstruction()throws Throwable{
  byte[] sealed=bytes(2,List.of("","abc","हिन्दी","😀","\udfff"));byte[] expected=table(construct(REFERENCE,direct(sealed)));
  var pool=Executors.newFixedThreadPool(4);var tasks=new ArrayList<Callable<Integer>>();
  for(int i=0;i<64;i++)tasks.add(()->{try{assertArrayEquals(expected,table(image(sealed)));return 1;}catch(Throwable e){throw new AssertionError(e);}});
  try{int n=0;for(var f:pool.invokeAll(tasks))n+=f.get();assertEquals(64,n);}finally{pool.shutdown();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS));}
  evidence.put("concurrentImages",64);
 }

 static final List<byte[]> retainedOutputs=new ArrayList<>();
 public static final class RetainingSha extends MessageDigestSpi {
  private final MessageDigest delegate;
  public RetainingSha(){try{delegate=MessageDigest.getInstance("SHA-256","SUN");}catch(GeneralSecurityException e){throw new AssertionError(e);}}
  @Override protected void engineUpdate(byte b){delegate.update(b);}
  @Override protected void engineUpdate(byte[] b,int o,int n){delegate.update(b,o,n);}
  @Override protected byte[] engineDigest(){byte[] b=delegate.digest();retainedOutputs.add(b);return b;}
  @Override protected int engineDigest(byte[] b,int o,int n)throws DigestException{int size=delegate.digest(b,o,n);retainedOutputs.add(b);return size;}
  @Override protected void engineReset(){delegate.reset();}
  @Override protected int engineGetDigestLength(){return 32;}
 }
 @Test void retainedProviderOutputCannotMutatePublishedTable()throws Throwable{
  var words=List.of("","abc","😀");byte[] sealed=bytes(2,words);retainedOutputs.clear();
  provider(RetainingSha.class,()->{var actual=image(sealed);byte[] original=table(actual);assertFalse(retainedOutputs.isEmpty());
   for(byte[] output:retainedOutputs)Arrays.fill(output,(byte)0);
   assertArrayEquals(original,table(actual),"provider must not retain private precompute table");
   for(int i=0;i<words.size();i++)assertEquals(words.get(i),actual.copyRecord(i,new LocalM3Arena()).flatten());
  });retainedOutputs.clear();evidence.put("retainedProviderOutputIsolated",true);
 }


 static byte[] layout(int version,String payload,int[] offsets,int[] lengths)throws Exception{
  assertEquals(offsets.length,lengths.length);int dir=version==1?8:12,begin=64+dir*offsets.length;
  var b=ByteBuffer.allocate(begin+2*payload.length()).order(ByteOrder.BIG_ENDIAN);
  b.putLong(0,0x4d334c4558303031L).putInt(8,version).putInt(12,offsets.length).putLong(16,begin).putLong(24,payload.length());
  for(int i=0;i<offsets.length;i++){int entry=64+i*dir;b.putInt(entry,offsets[i]).putInt(entry+4,lengths[i]);if(version==2)b.putInt(entry+8,payload.substring(offsets[i],offsets[i]+lengths[i]).hashCode());}
  for(int i=0;i<payload.length();i++){char unit=payload.charAt(i);b.put(begin+2*i,(byte)unit).put(begin+2*i+1,(byte)(unit>>>8));}
  return SharedLexiconImageContractTest.seal(b.array());
 }
 static ByteBuffer framed(byte[] bytes){
  var backing=ByteBuffer.allocateDirect(13+bytes.length+37);for(int i=0;i<backing.capacity();i++)backing.put(i,(byte)0x5a);
  backing.position(13).put(bytes).position(13);var data=backing.slice().asReadOnlyBuffer();data.limit(bytes.length).position(7).mark().order(ByteOrder.LITTLE_ENDIAN);return data;
 }
 static void rawParity(int version,String payload,int[] offsets,int[] lengths,List<String> expected)throws Throwable{
  byte[] sealed=layout(version,payload,offsets,lengths);var data=framed(sealed);var actual=(SharedLexiconImage)construct(SharedLexiconImage.class,data);var ref=construct(REFERENCE,framed(sealed));
  assertArrayEquals(table(ref),table(actual));assertEquals(expected.size(),actual.size());assertEquals(payload.length(),actual.utf16Units());
  assertEquals(7,data.position());assertEquals(sealed.length,data.limit());assertEquals(ByteOrder.BIG_ENDIAN,data.order());data.reset();assertEquals(7,data.position());
  for(int i=0;i<expected.size();i++)assertEquals(expected.get(i),actual.copyRecord(i,new LocalM3Arena()).flatten());
 }
 @Test void nonMonotonicOverlappingAndEmptyPhysicalWindows()throws Throwable{
  int cases=0;
  for(int v:List.of(1,2)){
   rawParity(v,"ba",new int[]{1,0},new int[]{1,1},List.of("a","b"));cases++;
   rawParity(v,"ab",new int[]{0,0},new int[]{1,2},List.of("a","ab"));cases++;
   rawParity(v,"abc",new int[]{0,1},new int[]{2,2},List.of("ab","bc"));cases++;
   rawParity(v,"ba",new int[]{1,0},new int[]{1,2},List.of("a","ba"));cases++;
   rawParity(v,"a",new int[]{1,0},new int[]{0,1},List.of("","a"));cases++;
   rawParity(v,"a?b",new int[]{0,2},new int[]{1,1},List.of("a","b"));cases++;
   rawParity(v,"zabcd?y",new int[]{7,1,1,2,6,0},new int[]{0,1,4,1,1,1},List.of("","a","abcd","b","y","z"));cases++;
   rawParity(v,"a",new int[]{},new int[]{},List.of());cases++;
   rawParity(v,"ab",new int[]{0},new int[]{0},List.of(""));cases++;
   rawParity(v,"ab",new int[]{1},new int[]{0},List.of(""));cases++;
   rawParity(v,"ab",new int[]{2},new int[]{0},List.of(""));cases++;
   for(int[] pair:List.of(new int[]{2,0},new int[]{1,1},new int[]{Integer.MAX_VALUE,1})){
    byte[] b=layout(v,"a",new int[]{0},new int[]{1});ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).putInt(64,pair[0]).putInt(68,pair[1]);SharedLexiconImageContractTest.seal(b);
    String original;try{construct(REFERENCE,framed(b));throw new AssertionError("original accepted");}catch(IOException e){original=e.getMessage();}
    var e=assertThrows(IOException.class,()->construct(SharedLexiconImage.class,framed(b)));assertEquals("invalid record range",original);assertEquals(original,e.getMessage());
   }
   byte[] shared=layout(v,"abc",new int[]{0,1},new int[]{2,2});var writable=direct(shared);var actual=(SharedLexiconImage)construct(SharedLexiconImage.class,writable);
   int begin=64+(v==1?8:12)*2;writable.put(begin+2,(byte)'X');for(int row=0;row<2;row++){int index=row;assertEquals("mapped record changed",assertThrows(IOException.class,()->actual.copyRecord(index,new LocalM3Arena())).getMessage());}
  }
  parity(2,List.of("a".repeat(2047),"a".repeat(2048),"a".repeat(2049)));
  evidence.put("physicalLayoutCases",cases);evidence.put("capacityBeyondLimitRefusals",6);evidence.put("spiEvenByteBoundaries",List.of(4094,4096,4098));
 }
 static final List<ByteBuffer> retainedInputs=new ArrayList<>();
 static final List<byte[]> capturedInputs=new ArrayList<>();
 public static final class WindowSha extends MessageDigestSpi{
  private final MessageDigest delegate;
  public WindowSha(){try{delegate=MessageDigest.getInstance("SHA-256","SUN");}catch(GeneralSecurityException e){throw new AssertionError(e);}}
  @Override protected void engineUpdate(byte b){delegate.update(b);}
  @Override protected void engineUpdate(byte[] b,int o,int n){delegate.update(b,o,n);}
  @Override protected void engineUpdate(ByteBuffer b){
   assertTrue(b.isReadOnly());retainedInputs.add(b);byte[] copy=new byte[b.remaining()];b.asReadOnlyBuffer().get(copy);capturedInputs.add(copy);delegate.update(b);assertEquals(b.limit(),b.position());
  }
  @Override protected byte[] engineDigest(){return delegate.digest();}
  @Override protected int engineDigest(byte[] b,int o,int n)throws DigestException{return delegate.digest(b,o,n);}
  @Override protected void engineReset(){delegate.reset();}
  @Override protected int engineGetDigestLength(){return 32;}
 }
 @Test void retainedProviderInputCursorsRemainIndependent()throws Throwable{
  var words=List.of("","a","abcdef","हिन्दी","😀");byte[] sealed=bytes(2,words);ByteBuffer data=framed(sealed);retainedInputs.clear();capturedInputs.clear();
  SharedLexiconImage[] admitted={null};byte[][] savedTable={null};
  provider(WindowSha.class,()->{
   var actual=(SharedLexiconImage)construct(SharedLexiconImage.class,data);admitted[0]=actual;savedTable[0]=table(actual);assertEquals(words.size()+4,retainedInputs.size());
   var ids=Collections.newSetFromMap(new IdentityHashMap<ByteBuffer,Boolean>());
   for(int i=0;i<words.size();i++){
    ByteBuffer view=retainedInputs.get(i+2);ids.add(view);assertNotSame(data,view);String word=words.get(i);byte[] expected=new byte[2*word.length()];
    for(int u=0;u<word.length();u++){expected[2*u]=(byte)word.charAt(u);expected[2*u+1]=(byte)(word.charAt(u)>>>8);}assertArrayEquals(expected,capturedInputs.get(i+2));
   }
   assertEquals(mode.equals("after")?1:words.size(),ids.size());evidence.put("providerRecordWindowIdentities",ids.size());
  });
  for(ByteBuffer view:List.copyOf(retainedInputs)){view.clear();if(view.capacity()>0)assertThrows(ReadOnlyBufferException.class,()->view.put(0,(byte)7));view.limit(0);}
  assertEquals(7,data.position());assertEquals(sealed.length,data.limit());data.reset();assertEquals(7,data.position());assertArrayEquals(savedTable[0],table(admitted[0]));
  for(int i=0;i<words.size();i++)assertEquals(words.get(i),admitted[0].copyRecord(i,new LocalM3Arena()).flatten());
  retainedInputs.clear();capturedInputs.clear();evidence.put("retainedProviderInputIsolated",true);
 }
 public static void observedRecordView(){recordViews.incrementAndGet();}
 public static ByteBuffer observedSlice(ByteBuffer b,int o,int n){sliceViews.incrementAndGet();return b.slice(o,n);}
 public static MappedByteBuffer observedMappedSlice(ByteBuffer b,int o,int n){sliceViews.incrementAndGet();return (MappedByteBuffer)b.slice(o,n);}
 public static ByteBuffer observedReadOnly(ByteBuffer b){readOnlyViews.incrementAndGet();return b.asReadOnlyBuffer();}
 public static MappedByteBuffer observedMappedReadOnly(ByteBuffer b){readOnlyViews.incrementAndGet();return (MappedByteBuffer)b.asReadOnlyBuffer();}

 static byte[] ownerBytes()throws Exception{
  var loader=M3ImageWindowQualification.class.getClassLoader();
  for(var f:loader.getClass().getDeclaredFields()){if(!Map.class.isAssignableFrom(f.getType()))continue;f.setAccessible(true);
   if(f.get(loader) instanceof Map<?,?> m&&m.get("jdk.m3.text.SharedLexiconImage") instanceof ByteArrayOutputStream b)return b.toByteArray();
  }throw new AssertionError("RAM owner bytes");
 }
 public static MessageDigest observedFactory(String algorithm)throws NoSuchAlgorithmException{factories.incrementAndGet();return MessageDigest.getInstance(algorithm);}
 public static byte[] observedArray(MessageDigest d){arrayDigests.incrementAndGet();return d.digest();}
 public static int observedDirect(MessageDigest d,byte[] b,int o,int n)throws DigestException{directDigests.incrementAndGet();return d.digest(b,o,n);}
 @Test void actualCompiledFactoryAndOutputCalls()throws Throwable{
  var writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);
  new ClassReader(ownerBytes()).accept(new ClassVisitor(Opcodes.ASM9,writer){
   @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] e){return new MethodVisitor(Opcodes.ASM9,super.visitMethod(a,n,d,s,e)){
    @Override public void visitCode(){super.visitCode();if(n.equals("recordBytes"))super.visitMethodInsn(Opcodes.INVOKESTATIC,"M3ImageWindowQualification","observedRecordView","()V",false);}
    @Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){
     if(owner.equals("java/nio/ByteBuffer")||owner.equals("java/nio/MappedByteBuffer")){
      if(name.equals("slice")&&(desc.equals("(II)Ljava/nio/ByteBuffer;")||desc.equals("(II)Ljava/nio/MappedByteBuffer;"))){
       boolean mapped=desc.endsWith("MappedByteBuffer;");super.visitMethodInsn(Opcodes.INVOKESTATIC,"M3ImageWindowQualification",mapped?"observedMappedSlice":"observedSlice",mapped?"(Ljava/nio/ByteBuffer;II)Ljava/nio/MappedByteBuffer;":"(Ljava/nio/ByteBuffer;II)Ljava/nio/ByteBuffer;",false);return;
      }
      if(name.equals("asReadOnlyBuffer")&&(desc.equals("()Ljava/nio/ByteBuffer;")||desc.equals("()Ljava/nio/MappedByteBuffer;"))){
       boolean mapped=desc.endsWith("MappedByteBuffer;");super.visitMethodInsn(Opcodes.INVOKESTATIC,"M3ImageWindowQualification",mapped?"observedMappedReadOnly":"observedReadOnly",mapped?"(Ljava/nio/ByteBuffer;)Ljava/nio/MappedByteBuffer;":"(Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;",false);return;
      }
     }
     if(owner.equals("java/security/MessageDigest")){
      if(name.equals("getInstance")&&desc.equals("(Ljava/lang/String;)Ljava/security/MessageDigest;")){super.visitMethodInsn(Opcodes.INVOKESTATIC,"M3ImageWindowQualification","observedFactory",desc,false);return;}
      if(name.equals("digest")&&desc.equals("()[B")){super.visitMethodInsn(Opcodes.INVOKESTATIC,"M3ImageWindowQualification","observedArray","(Ljava/security/MessageDigest;)[B",false);return;}
      if(name.equals("digest")&&desc.equals("([BII)I")){super.visitMethodInsn(Opcodes.INVOKESTATIC,"M3ImageWindowQualification","observedDirect","(Ljava/security/MessageDigest;[BII)I",false);return;}
     }super.visitMethodInsn(op,owner,name,desc,itf);
    }};}
  },0);
  byte[] instrumented=writer.toByteArray();ClassLoader child=new ClassLoader(M3ImageWindowQualification.class.getClassLoader()){
   @Override protected synchronized Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException{
    if(n.equals("jdk.m3.text.SharedLexiconImage")){Class<?> c=findLoadedClass(n);if(c==null)c=defineClass(n,instrumented,0,instrumented.length);if(resolve)resolveClass(c);return c;}return super.loadClass(n,resolve);
   }
  };
  var words=new ArrayList<String>();for(int i=0;i<256;i++)words.add(Integer.toString(1000+i));byte[] sealed=bytes(2,words);var type=child.loadClass("jdk.m3.text.SharedLexiconImage");
  factories.set(0);arrayDigests.set(0);directDigests.set(0);recordViews.set(0);sliceViews.set(0);readOnlyViews.set(0);var out=construct(type,direct(sealed));
  assertEquals(!mode.equals("original")?3:258,factories.get());assertEquals(!mode.equals("original")?2:258,arrayDigests.get());assertEquals(!mode.equals("original")?256:0,directDigests.get());
  assertEquals(mode.equals("after")?0:256,recordViews.get());assertEquals(mode.equals("after")?4:260,sliceViews.get());assertEquals(mode.equals("after")?5:4,readOnlyViews.get());
  evidence.put("executedRecordViews",recordViews.get());evidence.put("executedSlices",sliceViews.get());evidence.put("executedReadOnlyViews",readOnlyViews.get());evidence.put("executedFactories",factories.get());evidence.put("executedArrayDigests",arrayDigests.get());evidence.put("executedDirectDigests",directDigests.get());assertArrayEquals(table(construct(REFERENCE,direct(sealed))),table(out));
 }
 @Test void fieldsApiAndOtherMethodsUnchanged()throws Exception{
  var surface=new TreeSet<String>();var writer=new ClassWriter(0);
  new ClassReader(ownerBytes()).accept(new ClassVisitor(Opcodes.ASM9,writer){
   @Override public FieldVisitor visitField(int a,String n,String d,String s,Object v){surface.add("F:"+a+":"+n+":"+d);return super.visitField(a,n,d,s,v);}
   @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] e){if((a&(Opcodes.ACC_PUBLIC|Opcodes.ACC_PROTECTED))!=0)surface.add("M:"+a+":"+n+d);return n.equals("<init>")?null:super.visitMethod(a,n,d,s,e);}
  },ClassReader.SKIP_DEBUG);
  evidence.put("apiFieldsFingerprint",sha(String.join("\n",surface).getBytes(StandardCharsets.UTF_8)));evidence.put("nonConstructorFingerprint",sha(writer.toByteArray()));
 }
 static List<String> corpus(String name){
  if(name.equals("empty"))return List.of();if(name.equals("one"))return List.of("a");
  int count=switch(name){case "64-ascii"->64;case "1024-ascii","1024-boundary"->1024;case "16384-multilingual"->16384;case "65536-ascii"->65536;case "256-long"->256;default->throw new AssertionError(name);};
  var words=new ArrayList<String>();for(int i=0;i<count;i++){String s=Integer.toString(100000+i);if(name.endsWith("multilingual"))s+="हिन्दी汉字😀";if(name.equals("256-long"))s+="x".repeat(2048);if(name.equals("1024-boundary"))s+="x".repeat(i%66);words.add(s);}return words;
 }
 static long batch(ByteBuffer data,int calls)throws Throwable{long answer=0;for(int i=0;i<calls;i++){var image=(SharedLexiconImage)construct(SharedLexiconImage.class,data);answer+=image.size()+image.utf16Units()+image.imageIdentity().hashCode();sink=image;}answerSink=answer;return answer;}
 static void measure()throws Throwable{
  var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();assertTrue(bean.isThreadAllocatedMemorySupported());bean.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().threadId();
  evidence.put("sha256Provider",MessageDigest.getInstance("SHA-256").getProvider().getName());
  for(String name:List.of("empty","one","64-ascii","1024-ascii","16384-multilingual","65536-ascii","256-long","1024-boundary")){
   var words=corpus(name);var data=direct(bytes(2,words));int calls=words.size()>=16384?1:words.size()>=256?4:32;
   for(int i=0;i<6;i++)batch(data,calls);
   for(int sample=0;sample<7;sample++){long allocated=bean.getThreadAllocatedBytes(thread),start=System.nanoTime();long answer=batch(data,calls);long nanos=System.nanoTime()-start,used=bean.getThreadAllocatedBytes(thread)-allocated;
    measurements.add(Map.of("scenario",name,"sample",sample,"calls",calls,"nanos",nanos,"bytes",used,"answer",answer,"records",words.size(),"imageBytes",data.limit()));
   }
  }
 }
 public static void main(String[] args)throws Throwable{
  mode=args[0];assertTrue(List.of("original","digest","after").contains(mode));
  var vm=java.lang.management.ManagementFactory.getPlatformMXBean(com.sun.management.HotSpotDiagnosticMXBean.class);
  assertEquals("false",vm.getVMOption("UsePerfData").getValue());assertEquals("true",vm.getVMOption("PerfDisableSharedMem").getValue());assertEquals("false",vm.getVMOption("BackgroundCompilation").getValue());assertEquals("2",vm.getVMOption("CICompilerCount").getValue());
  var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);launcher.execute(LauncherDiscoveryRequestBuilder.request().selectors(selectClass(M3ImageWindowQualification.class)).build());
  var s=listener.getSummary();s.printTo(new PrintWriter(System.out,true));s.printFailuresTo(new PrintWriter(System.out,true));
  if(s.getTestsFoundCount()!=17||s.getTestsSucceededCount()!=17||s.getTestsFailedCount()!=0||s.getTestsAbortedCount()!=0||s.getTestsSkippedCount()!=0)throw new AssertionError("JUnit counts");
  measure();var result=new LinkedHashMap<String,Object>();result.put("mode",mode);result.put("found",17);result.put("passed",17);result.put("failed",0);result.put("aborted",0);result.put("evidence",evidence);result.put("measurements",measurements);
  byte[] value=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(result);var c=(HttpURLConnection)URI.create(args[1]).toURL().openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setFixedLengthStreamingMode(value.length);try(var out=c.getOutputStream()){out.write(value);}if(c.getResponseCode()!=204)throw new AssertionError("result");c.disconnect();System.out.println("M3_IMAGE_WINDOW_QUALIFICATION_PASS mode="+mode+" tests=17");
 }
}
