/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * This test is free software under the GNU General Public License version 2 only.
 */
/*
 * @test
 * @summary Preserve exact frozen placement geometry after modular-injectivity precheck
 * @modules java.base/java.lang:+open
 * @run main/othervm M3FrozenPreparedGeometryTest
 * @run main/othervm -Xint M3FrozenPreparedGeometryTest
 */
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
public final class M3FrozenPreparedGeometryTest {
 static final Class<?> INDEX, GROUPS, SOURCE, EQUALITY;
 static final Method PREPARED, BUILD, CANDIDATE, ROWS, DISPS, SEED, AVAILABLE, MIX;
 static final Constructor<?> GROUP;
 static final MessageDigest DIGEST;
 static long checks, placements, builds, callbackCount;
 static {
  try {
   INDEX=Class.forName("java.lang.M3FrozenLexiconIndex");
   GROUPS=Class.forName("java.lang.M3FrozenLexiconIndex$Groups");
   SOURCE=Class.forName("java.lang.M3FrozenLexiconIndex$ExactKeySource");
   EQUALITY=Class.forName("java.lang.M3FrozenLexiconIndex$ExactEquality");
   PREPARED=m("placePrepared",GROUPS,long.class,int.class,int.class);
   BUILD=m("buildExact",int.class,SOURCE,EQUALITY);
   CANDIDATE=m("candidateExact",long.class);ROWS=m("rows");DISPS=m("displacements");
   SEED=m("seed");AVAILABLE=m("available");MIX=m("mix64",long.class);
   GROUP=GROUPS.getDeclaredConstructor(long[].class,int[].class);GROUP.setAccessible(true);
   DIGEST=MessageDigest.getInstance("SHA-256");
  } catch (ReflectiveOperationException|java.security.NoSuchAlgorithmException e) {throw new ExceptionInInitializerError(e);}
 }
 interface Keys {long at(int row,long seed);}
 interface Same {boolean at(int a,int b);}
 static Method m(String n,Class<?>... p)throws NoSuchMethodException {Method x=INDEX.getDeclaredMethod(n,p);x.setAccessible(true);return x;}
 static Object call(Method m,Object owner,Object... args)throws Exception {
  try {return m.invoke(owner,args);}catch(InvocationTargetException x) {
   Throwable t=x.getCause();if(t instanceof Exception e)throw e;if(t instanceof Error e)throw e;throw new AssertionError(t);
  }
 }
 static Object source(Keys keys){return Proxy.newProxyInstance(INDEX.getClassLoader(),new Class<?>[]{SOURCE},(p,m,a)->keys.at((int)a[0],(long)a[1]));}
 static Object equality(Same same){return Proxy.newProxyInstance(INDEX.getClassLoader(),new Class<?>[]{EQUALITY},(p,m,a)->same.at((int)a[0],(int)a[1]));}
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static void feed(long n){DIGEST.update(ByteBuffer.allocate(8).putLong(n).array());}
 static void geometry(Object index)throws Exception {
  if(index==null){feed(-1);return;}
  feed((long)call(SEED,index));int[] rows=(int[])call(ROWS,index),disps=(int[])call(DISPS,index);
  feed(rows.length);for(int x:rows)feed(x);feed(disps.length);for(int x:disps)feed(x);
 }
 static long undo(long z,int s){long x=z;for(int i=0;i<6;i++)x=z^(x>>>s);return x;}
 static long inverse(long a){long x=a;for(int i=0;i<6;i++)x*=2-a*x;return x;}
 static long unmix(long z){z=undo(z,31);z*=inverse(0x94d049bb133111ebL);z=undo(z,27);z*=inverse(0xbf58476d1ce4e5b9L);return undo(z,30);}
 static long key(long seed,String s){long h=0xcbf29ce484222325L^seed^0x4d33455841435431L;h=(h^Integer.toUnsignedLong(s.length()))*0x100000001b3L;for(int i=0;i<s.length();i++)h=(h^s.charAt(i))*0x100000001b3L;return h;}
 static int pow2(int n){return n<=1?1:Integer.highestOneBit(n-1)<<1;}
 static void modularLaw(){
  for(int slots=2;slots<=32;slots*=2)
   for(int a=0;a<slots;a++)for(int b=a+1;b<slots;b++)for(int d=0;d<slots;d++)
    check(((a+d)&(slots-1))!=((b+d)&(slots-1)),"common translation injective");
  Random r=new Random(0x3247ab091L);
  for(int i=0;i<20000;i++){int slots=1<<(1+r.nextInt(20)),a=r.nextInt(slots),b=r.nextInt(slots),d=r.nextInt();
   if(a!=b)check(((a+d)&(slots-1))!=((b+d)&(slots-1)),"overflow/wrap injectivity");}
 }
 static void prepared()throws Exception {
  Random random=new Random(0x61cd492L);
  for(int count:new int[]{0,1,2,3,4,7,16,31,32,64,128,512})
   for(int buckets:new int[]{1,2,4,8,16})
    for(int shape=0;shape<3;shape++){
     int slots=pow2(Math.max(2,2*count));long[] keys=new long[count];int[] rows=new int[count];
     for(int i=0;i<count;i++){int b=shape==0?0:i&(buckets-1),ordinal=shape==0?i:i/buckets;
      int base=shape==2?(slots-1-ordinal)&(slots-1):ordinal;
      keys[i]=unmix(((long)base<<32)|b);rows[i]=(i+1)|(i%13==0?Integer.MIN_VALUE:0);
      check((long)call(MIX,null,keys[i])==(((long)base<<32)|b),"inverse fixture");}
     long[] oldKeys=keys.clone();int[] oldRows=rows.clone();long seed=random.nextLong();
     Object groups=GROUP.newInstance(keys,rows);Object index=call(PREPARED,null,groups,seed,buckets,slots);placements++;geometry(index);
     check(index!=null,"qualified unique-base placement");
     check((long)call(SEED,index)==seed,"content seed unchanged");
     for(int i=0;i<count;i++)check((int)call(CANDIDATE,index,keys[i])==rows[i],"row markers and slot geometry");
     check(Arrays.equals(keys,oldKeys)&&Arrays.equals(rows,oldRows),"group lanes immutable");
    }
  for(int slots:new int[]{2,4,16,256,4096}){
   long[] keys={unmix(1L<<32),unmix((1L<<32)|1)};
   Object groups=GROUP.newInstance(keys,new int[]{1,2});
   Object index=call(PREPARED,null,groups,0x7314L,1,slots);placements++;geometry(index);
   check(index==null,"aliased bases reject before displacement");
  }
 }
 static Object build(String[] words,boolean trace)throws Exception {
  Keys k=(row,seed)->{if(trace){callbackCount++;feed(row);feed(seed);}return key(seed,words[row]);};
  Same eq=(a,b)->{if(trace){callbackCount++;feed(a);feed(b);}return words[a].equals(words[b]);};
  return call(BUILD,null,words.length,source(k),equality(eq));
 }
 static void spellings()throws Exception {
  List<String[]> suites=new ArrayList<>();
  suites.add(new String[0]);suites.add(new String[]{""});
  suites.add(new String[]{"","Aa","BB",String.valueOf((char)0),String.valueOf((char)0xffff),String.valueOf((char)0xd800),String.valueOf((char)0xdfff),new String(new char[]{0xd83d,0xde00}),"Aa",""});
  Random r=new Random(0x721aefL);
  for(int count:new int[]{2,3,7,16,31,128,513,1024,10001}){
   String[] words=new String[count];for(int i=0;i<count;i++)words[i]=Integer.toString(i);suites.add(words);
  }
  for(int trial=0;trial<64;trial++){
   String[] words=new String[1+r.nextInt(160)];
   for(int i=0;i<words.length;i++){
    char[] c=new char[r.nextInt(33)];for(int j=0;j<c.length;j++)c[j]=(char)r.nextInt(65536);
    words[i]=new String(c);
    if(i>0&&i%5==0)words[i]=words[r.nextInt(i)];
   }suites.add(words);
  }
  for(String[] words:suites){
   Object index=build(words,true);builds++;geometry(index);
   check(words.length==0?!(boolean)call(AVAILABLE,index):(boolean)call(AVAILABLE,index),"content keys admitted");
   if(words.length==0)continue;
   long seed=(long)call(SEED,index);
   Map<String,Integer> first=new HashMap<>();Map<String,Integer> counts=new HashMap<>();
   for(int i=0;i<words.length;i++){first.putIfAbsent(words[i],i);counts.merge(words[i],1,Integer::sum);}
   for(var e:first.entrySet()){
    int expected=e.getValue()+1;if(counts.get(e.getKey())>1)expected|=Integer.MIN_VALUE;
    check((int)call(CANDIDATE,index,key(seed,e.getKey()))==expected,"exact source identity/duplicate marker");
   }
   Object again=build(words,false);check(Arrays.equals((int[])call(ROWS,index),(int[])call(ROWS,again)),"deterministic rows");
   check(Arrays.equals((int[])call(DISPS,index),(int[])call(DISPS,again)),"deterministic displacement");
   check(call(SEED,index).equals(call(SEED,again)),"deterministic seed");
   for(int i=0;i<32;i++){String absent="ABSENT-"+i+"-"+words.length;if(first.containsKey(absent))continue;
    int encoded=(int)call(CANDIDATE,index,key(seed,absent));if(encoded!=0){
     int row=(encoded&Integer.MAX_VALUE)-1;check(row>=0&&row<words.length,"absent candidate bounded");
     check(!absent.equals(words[row]),"exact payload comparison rejects absent candidate");
    }}
  }
  int[] calls={0};Object source=source((row,seed)->{calls[0]++;feed(row);feed(seed);return 0;});
  Object empty=call(BUILD,null,7,source,equality((a,b)->false));geometry(empty);
  check(!(boolean)call(AVAILABLE,empty)&&calls[0]==64*7,"all unresolvable content seeds use existing empty fallback");
  Keys fault=(row,seed)->{throw new ArithmeticException("fixture");};
  check(!(boolean)call(AVAILABLE,call(BUILD,null,1,source(fault),equality((a,b)->false))),"arithmetic fallback unchanged");
  for(int count:new int[]{0,(1<<21)+1,Integer.MAX_VALUE}){
   check(!(boolean)call(AVAILABLE,call(BUILD,null,count,source((a,b)->{throw new AssertionError("unexpected callback");}),equality((a,b)->false))),"empty/oversized skip callback");
  }
  try{call(BUILD,null,-1,source((a,b)->0),equality((a,b)->false));throw new AssertionError("negative count");}catch(IllegalArgumentException expected){checks++;}
  try{call(BUILD,null,1,null,equality((a,b)->false));throw new AssertionError("null source");}catch(NullPointerException expected){checks++;}
  try{call(BUILD,null,1,source((a,b)->0),null);throw new AssertionError("null equality");}catch(NullPointerException expected){checks++;}
 }
 static void concurrency()throws Exception {
  String[] words=new String[128];for(int i=0;i<words.length;i++)words[i]="parallel-"+i;
  Object expected=build(words,false);int[] rows=(int[])call(ROWS,expected),ds=(int[])call(DISPS,expected);
  ExecutorService pool=Executors.newFixedThreadPool(4);
  try {List<Callable<Boolean>> tasks=new ArrayList<>();for(int i=0;i<32;i++)tasks.add(()->{Object x=build(words,false);return Arrays.equals(rows,(int[])call(ROWS,x))&&Arrays.equals(ds,(int[])call(DISPS,x));});
   for(Future<Boolean> f:pool.invokeAll(tasks))check(f.get(),"independent concurrent freeze geometry");
  } finally {pool.shutdownNow();check(pool.awaitTermination(10,TimeUnit.SECONDS),"worker threads stopped");}
 }
 public static void main(String[] args)throws Exception {
  check(INDEX.getClassLoader()==null&&INDEX.getModule()==Object.class.getModule(),"actual java.base owner");
  modularLaw();prepared();spellings();concurrency();
  System.out.println("M3_FROZEN_PREPARED_GEOMETRY_PASS checks="+checks+" placements="+placements+" builds="+builds+" callbacks="+callbackCount+" geometry="+HexFormat.of().formatHex(DIGEST.digest()));
 }
}
