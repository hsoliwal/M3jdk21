// SPDX-License-Identifier: Apache-2.0
// Source-aware property proof; adapts existing M3 public reflection/oracle helpers.
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
public final class AvlGrowthFrontierProof {
  static final MessageDigest H;
  static final long[] SEEDS={0x41c64e6dL,0x9e3779b9L,0x5eedL};
  static String where="start"; static long checks,updates,randomOps,invariants;
  static int permutations,scenarios; static F maps,sets;
  static {try{H=MessageDigest.getInstance("SHA-256");}catch(Exception e){throw new ExceptionInInitializerError(e);}}
  private AvlGrowthFrontierProof(){}
  static final class F {
    final Class<?> type; final Constructor<?> normal,ordered; final Method invariant;
    F(String name,boolean oracle)throws Exception{
      type=Class.forName(name);normal=type.getConstructor();ordered=type.getConstructor(Comparator.class);
      invariant=oracle?null:type.getMethod("verifyInvariants");
    }
    Object make(Comparator<Integer> c,boolean explicit)throws Exception{
      try{return c==null&&!explicit?normal.newInstance():ordered.newInstance(c);}
      catch(InvocationTargetException e){throw new AssertionError(where+"/constructor",e.getCause());}
    }
    void verify(Object root)throws Exception{
      if(invariant!=null)try{invariant.invoke(root);invariants++;}
      catch(InvocationTargetException e){throw new AssertionError(where+"/invariants",e.getCause());}
    }
  }
  static Comparator<Integer> order(int mode){
    return switch(mode){case 0->null;case 1->Comparator.reverseOrder();
      case 2->Comparator.nullsFirst(Integer::compare);
      case 3->Comparator.comparingInt((Integer k)->Math.abs(k));default->throw new AssertionError(mode);};
  }
  static void eq(String id,Object expected,Object actual){
    checks++;if(!Objects.equals(expected,actual))throw new AssertionError(where+"/"+id+" expected="+expected+" actual="+actual);
    H.update((where+"/"+id+"="+expected+"\n").getBytes(StandardCharsets.UTF_8));
  }
  static String entry(Map.Entry<Integer,Integer> e){return e==null?"<none>":e.getKey()+"="+e.getValue();}
  static String state(Map<Integer,Integer> m){
    StringBuilder b=new StringBuilder();for(var e:m.entrySet())b.append(e.getKey()).append('=').append(e.getValue()).append(';');return b.toString();
  }
  static final class M {
    final NavigableMap<Integer,Integer> e,a;
    @SuppressWarnings("unchecked") M(Comparator<Integer> c,boolean explicit)throws Exception{
      e=new TreeMap<>(c);a=(NavigableMap<Integer,Integer>)maps.make(c,explicit);scenarios++;
    }
    void put(Integer k,Integer v){updates++;Integer old=e.put(k,v);eq("put/"+k,old,a.put(k,v));}
    void remove(Integer k){updates++;Integer old=e.remove(k);eq("remove/"+k,old,a.remove(k));}
    void check()throws Exception{
      eq("size",e.size(),a.size());eq("entries",state(e),state(a));
      eq("descending",state(e.descendingMap()),state(a.descendingMap()));
      eq("first",entry(e.firstEntry()),entry(a.firstEntry()));eq("last",entry(e.lastEntry()),entry(a.lastEntry()));maps.verify(a);
    }
    void neighbors(Integer k){
      eq("lower",entry(e.lowerEntry(k)),entry(a.lowerEntry(k)));eq("floor",entry(e.floorEntry(k)),entry(a.floorEntry(k)));
      eq("ceiling",entry(e.ceilingEntry(k)),entry(a.ceilingEntry(k)));eq("higher",entry(e.higherEntry(k)),entry(a.higherEntry(k)));
    }
  }
  static final class S {
    final NavigableSet<Integer> e,a;
    @SuppressWarnings("unchecked") S(Comparator<Integer> c)throws Exception{e=new TreeSet<>(c);a=(NavigableSet<Integer>)sets.make(c,false);scenarios++;}
    void add(Integer k){updates++;boolean old=e.add(k);eq("add/"+k,old,a.add(k));}
    void remove(Integer k){updates++;boolean old=e.remove(k);eq("remove/"+k,old,a.remove(k));}
    void check()throws Exception{
      eq("size",e.size(),a.size());eq("keys",new ArrayList<>(e),new ArrayList<>(a));
      eq("descending",new ArrayList<>(e.descendingSet()),new ArrayList<>(a.descendingSet()));sets.verify(a);
    }
  }
  static void permutation(int[] keys,int at)throws Exception{
    if(at<keys.length){for(int i=at;i<keys.length;i++){int x=keys[at];keys[at]=keys[i];keys[i]=x;permutation(keys,at+1);x=keys[at];keys[at]=keys[i];keys[i]=x;}return;}
    int id=permutations++;
    for(int d=0;d<2;d++){
      M p=new M(null,false);where="perm/"+id+"/"+d;
      for(int k:keys){p.put(k,k*11);p.check();}
      p.put(keys[3],null);p.check();p.put(keys[3],-1);p.check();
      while(!p.e.isEmpty()){Integer k=d==0?new ArrayList<>(p.e.keySet()).get(p.e.size()/2):p.e.lastKey();p.remove(k);p.check();}
    }
    if(sets!=null){
      S p=new S(null);where="setPerm/"+id;for(int k:keys){p.add(k);p.check();}
      p.add(keys[3]);p.check();
      while(!p.e.isEmpty()){Integer k=(id&1)==0?new ArrayList<>(p.e).get(p.e.size()/2):p.e.last();p.remove(k);p.check();}
    }
  }
  static void rotations()throws Exception{
    int[][] cases={{3,2,1},{3,1,2},{1,2,3},{1,3,2}};
    for(int mode=0;mode<2;mode++)for(int i=0;i<cases.length;i++){
      where="rotation/"+mode+"/"+i;M p=new M(order(mode),true);
      for(int k:cases[i]){p.put(k,k);p.check();}
      if(sets!=null){S s=new S(order(mode));for(int k:cases[i]){s.add(k);s.check();}}
    }
  }
  interface Op{Object run();}
  static String attempt(Op op){try{return "return:"+op.run();}catch(RuntimeException e){return "throw:"+e.getClass().getName();}}
  static Integer low(int mode){return switch(mode){case 1->Integer.valueOf(100);case 2->null;case 3->Integer.valueOf(10);default->Integer.valueOf(-100);};}
  static Integer high(int mode){return mode==1?-100:mode==3?200:100;}
  static NavigableMap<Integer,Integer> range(NavigableMap<Integer,Integer> p,int mode){return p.subMap(low(mode),true,high(mode),false);}
  static NavigableSet<Integer> range(NavigableSet<Integer> p,int mode){return p.subSet(low(mode),true,high(mode),false);}
  static Integer key(SplittableRandom r,int mode){return mode==2&&r.nextInt(17)==0?null:r.nextInt(-512,513);}
  static void fuzzMaps()throws Exception{
    for(int mode=0;mode<4;mode++)for(long seed:SEEDS){
      final int kind=mode;M p=new M(order(mode),false);SplittableRandom r=new SplittableRandom(seed+mode);
      for(int i=0;i<6144;i++){
        where="mapFuzz/"+mode+"/"+seed+"/"+i;Integer k=key(r,mode),v=r.nextInt(7)==0?null:r.nextInt(5000);randomOps++;
        switch(r.nextInt(16)){
          case 0,1,2,3->p.put(k,v);
          case 4->p.remove(k);
          case 5->{eq("get",p.e.get(k),p.a.get(k));eq("contains",p.e.containsKey(k),p.a.containsKey(k));}
          case 6->{Integer old=p.e.putIfAbsent(k,v);eq("putIfAbsent",old,p.a.putIfAbsent(k,v));}
          case 7->{Integer old=p.e.replace(k,v);eq("replace",old,p.a.replace(k,v));}
          case 8->{Integer old=p.e.get(k);eq("removePair",p.e.remove(k,old),p.a.remove(k,old));}
          case 9->eq("pollFirst",entry(p.e.pollFirstEntry()),entry(p.a.pollFirstEntry()));
          case 10->eq("pollLast",entry(p.e.pollLastEntry()),entry(p.a.pollLastEntry()));
          case 11->p.neighbors(k);
          case 12->{if(!p.e.isEmpty()){var e=p.e.entrySet().iterator().next();var a=p.a.entrySet().iterator().next();eq("entryKey",e.getKey(),a.getKey());Integer old=e.setValue(v);eq("entryWrite",old,a.setValue(v));}}
          case 13->{Integer old=p.e.descendingMap().put(k,v);eq("descendingPut",old,p.a.descendingMap().put(k,v));}
          case 14->{eq("boundedPut",attempt(()->range(p.e,kind).put(k,v)),attempt(()->range(p.a,kind).put(k,v)));eq("boundedState",state(range(p.e,kind)),state(range(p.a,kind)));}
          case 15->{Integer old=range(p.e,kind).descendingMap().remove(k);eq("boundedDescendingRemove",old,range(p.a,kind).descendingMap().remove(k));}
          default->throw new AssertionError();
        }
        if((i&63)==63)p.check();
      }
      p.check();
    }
  }
  static void fuzzSets()throws Exception{
    if(sets==null)return;
    for(int mode=0;mode<4;mode++)for(long seed:SEEDS){
      final int kind=mode;S p=new S(order(mode));SplittableRandom r=new SplittableRandom(seed+mode);
      for(int i=0;i<2048;i++){
        where="setFuzz/"+mode+"/"+seed+"/"+i;Integer k=key(r,mode);randomOps++;
        switch(r.nextInt(10)){
          case 0,1,2->p.add(k);case 3->p.remove(k);
          case 4->{eq("contains",p.e.contains(k),p.a.contains(k));eq("lower",p.e.lower(k),p.a.lower(k));eq("floor",p.e.floor(k),p.a.floor(k));eq("ceiling",p.e.ceiling(k),p.a.ceiling(k));eq("higher",p.e.higher(k),p.a.higher(k));}
          case 5->eq("pollFirst",p.e.pollFirst(),p.a.pollFirst());
          case 6->eq("boundedAdd",attempt(()->range(p.e,kind).add(k)),attempt(()->range(p.a,kind).add(k)));
          case 7->eq("descendingRemove",p.e.descendingSet().remove(k),p.a.descendingSet().remove(k));
          case 8->eq("pollLast",p.e.pollLast(),p.a.pollLast());
          case 9->eq("boundedRemove",range(p.e,kind).remove(k),range(p.a,kind).remove(k));
          default->throw new AssertionError();
        }
        if((i&63)==63)p.check();
      }
      p.check();
    }
  }
  static void medians(int lo,int hi,List<Integer> out){if(lo>=hi)return;int mid=(lo+hi)>>>1;out.add(mid);medians(lo,mid,out);medians(mid+1,hi,out);}
  static void bulk(int mode,int n)throws Exception{
    where="bulk/"+mode+"/"+n;Comparator<Integer> c=order(mode);TreeMap<Integer,Integer> source=new TreeMap<>(c);
    for(int i=0;i<n;i++)source.put(i,i*3);if(mode==2)source.put(null,null);
    M p=new M(c,false);p.e.putAll(source);p.a.putAll(source);p.check();
    List<Integer> cut=new ArrayList<>();medians(0,n,cut);
    for(int i=0;i<n/2;i++){where="bulkDelete/"+mode+"/"+n+"/"+i;p.remove(cut.get(i));if((i&255)==255)p.check();}
    p.check();
    for(int i=0;i<n/2;i++){where="bulkReuse/"+mode+"/"+n+"/"+i;p.put(n+i,-i);if((i&255)==255)p.check();}
    p.check();
    List<Integer> drain=new ArrayList<>(p.e.descendingKeySet());
    for(int i=0;i<drain.size();i++){where="bulkDrain/"+mode+"/"+n+"/"+i;p.remove(drain.get(i));if((i&255)==255)p.check();}
    p.check();
  }
  public static void main(String[] args)throws Exception{
    if(args.length<1||args.length>2)throw new IllegalArgumentException("oracle|MapClass [SetClass]");
    boolean oracle=args[0].equals("oracle");String frozen=System.getProperty("expectedDigest");
    if(!oracle&&(frozen==null||!frozen.matches("[0-9a-fA-F]{64}")))throw new IllegalArgumentException("Freeze oracle expectedDigest first; supply -DexpectedDigest=<SHA256>");
    maps=new F(oracle?"java.util.TreeMap":args[0],oracle);
    if(args.length==2)sets=new F(oracle?"java.util.TreeSet":args[1],oracle);
    eq("setCoverage",sets!=null,sets!=null);rotations();permutation(new int[]{0,1,2,3,4,5,6},0);
    if(permutations!=5040)throw new AssertionError("permutation count");
    fuzzMaps();fuzzSets();for(int mode=0;mode<4;mode++)bulk(mode,1023);bulk(0,8192);
    String digest=HexFormat.of().formatHex(H.digest());
    if(!oracle&&!digest.equalsIgnoreCase(frozen))throw new AssertionError("frozen digest differs expected="+frozen+" actual="+digest);
    System.out.println("{\"classification\":\"SOURCE_AWARE_PUBLIC_PROPERTY_PROOF\",\"oracle\":"+oracle+",\"permutations\":"+permutations+",\"scenarios\":"+scenarios+",\"randomOperations\":"+randomOps+",\"trackedUpdates\":"+updates+",\"checks\":"+checks+",\"invariantCalls\":"+invariants+",\"setCovered\":"+(sets!=null)+",\"expectedDigest\":\""+digest+"\",\"nativeExecuted\":false}");
  }
}
