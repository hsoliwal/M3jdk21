import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Callable;

/** Source-blind public-API fixture; original oracle must be run and sealed first. */
public final class TreeMapLookupEntryProof {
  static final MessageDigest HASH;
  static F oracle, target;
  static int cases;
  static { try { HASH=MessageDigest.getInstance("SHA-256"); }
    catch(Exception e){throw new ExceptionInInitializerError(e);} }

  @SuppressWarnings("serial")
  static final class Blocked extends RuntimeException {}
  static final class Gate implements Comparator<Object> {
    final int mode;
    boolean armed;
    Gate(int mode){this.mode=mode;}
    public int compare(Object a,Object b){
      if(armed)throw new Blocked();
      if(mode==2){
        if(a==b)return 0;
        if(a==null)return -1;
        if(b==null)return 1;
      }
      return mode==1?Integer.compare((Integer)b,(Integer)a)
          :Integer.compare((Integer)a,(Integer)b);
    }
  }
  static final class F {
    final Constructor<?> empty,cmp;
    F(String name)throws Exception{
      Class<?> c=Class.forName(name);
      empty=c.getConstructor();
      cmp=c.getConstructor(Comparator.class);
    }
    @SuppressWarnings("unchecked")
    SortedMap<Object,Object> make(boolean natural,Gate gate)throws Exception{
      try{return (SortedMap<Object,Object>)(natural?empty.newInstance():cmp.newInstance(gate));}
      catch(InvocationTargetException e){
        Throwable t=e.getCause();
        if(t instanceof Exception x)throw x;
        if(t instanceof Error x)throw x;
        throw e;
      }
    }
  }
  interface Scenario { String run(F f)throws Exception; }
  static String state(Map<Object,Object> m){
    List<String> out=new ArrayList<>();
    for(Map.Entry<Object,Object> e:m.entrySet())
      out.add(e.getKey()+"="+e.getValue());
    return out.toString();
  }
  static String attempt(Callable<Object> action){
    try{return "return:"+String.valueOf(action.call());}
    catch(Throwable e){return "throw:"+e.getClass().getName();}
  }
  static void pair(String id,Scenario action)throws Exception{
    String expected=action.run(oracle); // Freeze original before matching target execution.
    HASH.update((id+"|"+expected+"\n").getBytes(StandardCharsets.UTF_8));
    String actual=action.run(target);
    if(!expected.equals(actual))
      throw new AssertionError(id+" expected="+expected+" actual="+actual);
    cases++;
  }
  static Object query(int q){
    if(q==0)return null;
    if(q==1)return new Object();
    if(q==2)return Integer.valueOf(1);
    return "wrong-type";
  }
  static void fill(Map<Object,Object> m,boolean nullKey){
    m.put(1,"A");m.put(2,"B");m.put(3,"C");m.put(4,"D");
    if(nullKey)m.put(null,"N");
  }
  static String lookup(F f,boolean natural,boolean populated,int mode,int q,int op)throws Exception{
    Gate gate=new Gate(mode);
    SortedMap<Object,Object> m=f.make(natural,gate);
    if(populated)fill(m,false);
    Object key=query(q);
    if(!natural)gate.armed=true;
    String result;
    try{
      result=attempt(()->op==0?m.get(key):op==1?m.containsKey(key):m.remove(key));
    }finally{gate.armed=false;}
    return result+"|state:"+state(m);
  }
  static SortedMap<Object,Object> view(SortedMap<Object,Object> m,int kind){
    List<Object> keys=new ArrayList<>(m.keySet());
    return switch(kind){
      case 0->m;
      case 1->m.subMap(keys.get(1),keys.get(keys.size()-1));
      case 2->m.reversed();
      case 3->m.subMap(keys.get(0),keys.get(keys.size()-1)).reversed();
      default->throw new AssertionError(kind);
    };
  }
  static Map.Entry<Object,Object> entry(SortedMap<Object,Object> m,int access){
    return switch(access){
      case 0->m.entrySet().iterator().next();
      case 1->m.sequencedEntrySet().getFirst();
      case 2->m.sequencedEntrySet().getLast();
      default->throw new AssertionError(access);
    };
  }
  static String entryCase(F f,int mode,int kind,int access,boolean set)throws Exception{
    Gate gate=new Gate(mode);
    SortedMap<Object,Object> base=f.make(false,gate);
    fill(base,mode==2);
    SortedMap<Object,Object> v=view(base,kind);
    Map.Entry<Object,Object> e=entry(v,access); // Select before arming comparator.
    Object key=e.getKey();
    Object before=e.getValue();
    gate.armed=true;
    String result;
    try{result=attempt(()->set?e.setValue("changed"):e.getValue());}
    finally{gate.armed=false;}
    return "key:"+key+"|before:"+before+"|"+result+"|state:"+state(base);
  }
  static void lifetime(int kind,int access)throws Exception{
    Gate gate=new Gate(0);
    SortedMap<Object,Object> base=target.make(false,gate);
    fill(base,false);
    Map.Entry<Object,Object> e=entry(view(base,kind),access);
    Object key=e.getKey();
    base.remove(key);base.put(8,"fresh");
    String get=attempt(e::getValue);
    String set=attempt(()->e.setValue("diagnostic"));
    System.out.println("DIAGNOSTIC postStructuralEntry view="+kind+" access="+access
        +" "+get+" "+set+" state="+attempt(()->state(base)));
  }
  @SuppressWarnings("unchecked")
  static void linked(boolean isOracle)throws Exception{
    String name=isOracle?"java.util.LinkedHashMap":"com.m3.collections.M3LinkedHashMap";
    Class<?> c;
    try{c=Class.forName(name);}
    catch(ClassNotFoundException e){
      System.out.println("OPEN optionalPublicOwner="+name);return;
    }
    Map<Object,Object> expected=new LinkedHashMap<>();
    Map<Object,Object> actual=(Map<Object,Object>)c.getConstructor().newInstance();
    fill(expected,false);fill(actual,false);
    Map.Entry<Object,Object> ee=expected.entrySet().iterator().next();
    Map.Entry<Object,Object> ae=actual.entrySet().iterator().next();
    Object old=ee.getValue();
    if(!Objects.equals(old,ae.getValue()))throw new AssertionError("linked entry get");
    Object replaced=ee.setValue("changed");
    if(!Objects.equals(replaced,ae.setValue("changed")))
      throw new AssertionError("linked entry set return");
    if(!state(expected).equals(state(actual)))throw new AssertionError("linked entry post-state");
    System.out.println("PASS optionalPublicEntryOwner="+name);
  }
  public static void main(String[] args)throws Exception{
    String name=args.length==0||args[0].equals("oracle")
        ?"java.util.TreeMap":args[0];
    oracle=new F("java.util.TreeMap");target=new F(name);
    for(int mode=0;mode<3;mode++)for(int q=0;q<4;q++)for(int op=0;op<3;op++){
      final int a=mode,b=q,c=op;
      pair("emptyCmp/"+a+"/"+b+"/"+c,f->lookup(f,false,false,a,b,c));
    }
    for(int populated=0;populated<2;populated++)for(int q=0;q<4;q++)for(int op=0;op<3;op++){
      final boolean p=populated!=0;final int a=q,b=op;
      pair("natural/"+p+"/"+a+"/"+b,f->lookup(f,true,p,0,a,b));
    }
    for(int mode=0;mode<3;mode++)for(int v=0;v<4;v++)
      for(int a=0;a<3;a++)for(int set=0;set<2;set++){
        final int x=mode,y=v,z=a;final boolean s=set!=0;
        pair("entry/"+x+"/"+y+"/"+z+"/"+s,f->entryCase(f,x,y,z,s));
      }
    for(int v=0;v<4;v++)for(int a=0;a<3;a++)lifetime(v,a);
    linked(name.equals("java.util.TreeMap"));
    System.out.println("PASS finiteCases="+cases+" expectedSHA256="
        +HexFormat.of().formatHex(HASH.digest())+" target="+name
        +" fullOwnerMatrix=OPEN comparatorTraceCounts=NOT_ASSERTED");
  }
}
