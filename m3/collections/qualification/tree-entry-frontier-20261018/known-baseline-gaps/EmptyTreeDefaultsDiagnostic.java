// SPDX-License-Identifier: Apache-2.0
// SOURCE_AWARE_PUBLIC_REFERENCE fixture; no compile-time M3 dependency.
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.*;
public final class EmptyTreeDefaultsDiagnostic {
  static final MessageDigest HASH;
  static final List<String> MISMATCHES=new ArrayList<>();
  static int cases; static long referenceComparisons,targetComparisons;
  static {try{HASH=MessageDigest.getInstance("SHA-256");}catch(Exception e){throw new ExceptionInInitializerError(e);}}
  private EmptyTreeDefaultsDiagnostic(){}
  static String q(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\t","\\t").replace("\n","\\n").replace("\r","\\r")+"\"";}
  static final class Gate implements Comparator<Object>{
    boolean armed; int comparisons;
    public int compare(Object a,Object b){
      comparisons++;
      if(armed)throw new IllegalStateException("comparison-gate");
      return Integer.compare((Integer)a,(Integer)b);
    }
  }
  static final class Factory{
    final Constructor<?> normal,ordered;
    Factory(String name)throws Exception{Class<?> c=Class.forName(name);normal=c.getConstructor();ordered=c.getConstructor(Comparator.class);}
    @SuppressWarnings("unchecked")
    NavigableMap<Object,Object> make(Gate c)throws Exception{
      try{return (NavigableMap<Object,Object>)(c==null?normal.newInstance():ordered.newInstance(c));}
      catch(InvocationTargetException e){throw new AssertionError("constructor",e.getCause());}
    }
  }
  static String label(Object value,Object input){
    if(value==null)return "null";
    if(value==input)return "input-key";
    if(value.getClass()==Object.class)return "other-object";
    return String.valueOf(value);
  }
  static String state(Map<Object,Object> root,Object input){
    StringBuilder b=new StringBuilder("{");
    for(var e:root.entrySet())b.append(label(e.getKey(),input)).append('=').append(label(e.getValue(),input)).append(';');
    return b.append('}').toString();
  }
  static Object callback(int mode){
    if(mode==2)throw new IllegalArgumentException("callback-gate");
    return mode==0?null:"callback-value";
  }
  static String observe(Factory f,int order,int view,int keyKind,String op,int mode,boolean nullValue,boolean reference)throws Exception{
    Gate gate=order==0?null:new Gate();
    NavigableMap<Object,Object> root=f.make(gate);
    NavigableMap<Object,Object> selected=switch(view){
      case 0->root;case 1->root.descendingMap();case 2->root.subMap(-1,true,1,false);
      default->throw new AssertionError(view);
    };
    Object key=keyKind==0?null:new Object();
    List<String> trace=new ArrayList<>();
    Function<Object,Object> one=k->{trace.add("callback("+label(k,key)+")");return callback(mode);};
    BiFunction<Object,Object,Object> two=(k,old)->{trace.add("callback("+label(k,key)+","+label(old,key)+")");return callback(mode);};
    BiFunction<Object,Object,Object> remap=(old,v)->{trace.add("callback("+label(old,key)+","+label(v,key)+")");return callback(mode);};
    if(gate!=null){gate.comparisons=0;gate.armed=true;}
    String outcome;
    try{
      Object result=switch(op){
        case "computeIfAbsent"->selected.computeIfAbsent(key,one);
        case "compute"->selected.compute(key,two);
        case "computeIfPresent"->selected.computeIfPresent(key,two);
        case "merge"->selected.merge(key,nullValue?null:"incoming",remap);
        case "putIfAbsent"->selected.putIfAbsent(key,nullValue?null:"incoming");
        default->throw new AssertionError(op);
      };
      outcome="return:"+label(result,key);
    }catch(RuntimeException e){outcome="throw:"+e.getClass().getName();}
    // Traverse the unbounded root, never a bounded view under an armed comparator.
    String result=outcome+"|callbacks="+trace+"|root="+state(root,key);
    if(gate!=null){
      if(reference)referenceComparisons+=gate.comparisons;else targetComparisons+=gate.comparisons;
    }
    return result;
  }
  static void scenario(Factory reference,Factory target,int order,int view,int key,String op,int mode,boolean nullValue)throws Exception{
    String id=order+"/"+view+"/"+key+"/"+op+"/"+mode+"/"+nullValue;
    String expected=observe(reference,order,view,key,op,mode,nullValue,true);
    String actual=observe(target,order,view,key,op,mode,nullValue,false);
    if(!expected.equals(actual))MISMATCHES.add(id+"\t"+expected+"\t"+actual);
    HASH.update((id+"="+expected+"\n").getBytes(StandardCharsets.UTF_8));cases++;
  }
  public static void main(String[] args)throws Exception{
    if(args.length!=1)throw new IllegalArgumentException("oracle|MapClass");
    boolean oracle=args[0].equals("oracle");String frozen=System.getProperty("expectedDigest");
    if(!oracle&&(frozen==null||!frozen.matches("[0-9a-fA-F]{64}")))throw new IllegalArgumentException("Freeze JDK oracle first; supply -DexpectedDigest=<SHA256>");
    Factory reference=new Factory("java.util.TreeMap"),target=new Factory(oracle?"java.util.TreeMap":args[0]);
    for(int order=0;order<2;order++)for(int view=0;view<3;view++)for(int key=0;key<2;key++){
      for(String op:List.of("computeIfAbsent","compute","computeIfPresent"))for(int mode=0;mode<3;mode++)
        scenario(reference,target,order,view,key,op,mode,false);
      for(int mode=0;mode<3;mode++)for(boolean nil:new boolean[]{false,true})
        scenario(reference,target,order,view,key,"merge",mode,nil);
      for(boolean nil:new boolean[]{false,true})scenario(reference,target,order,view,key,"putIfAbsent",0,nil);
    }
    if(cases!=204)throw new AssertionError("case matrix incomplete:"+cases);
    String digest=HexFormat.of().formatHex(HASH.digest());
    if(!oracle&&!digest.equalsIgnoreCase(frozen))throw new AssertionError("frozen digest differs expected="+frozen+" actual="+digest);
    StringJoiner rows=new StringJoiner(",","[","]");for(String row:MISMATCHES)rows.add(q(row));
    System.out.println("{\"classification\":\"SOURCE_AWARE_PUBLIC_REFERENCE\",\"cases\":"+cases+",\"expectedDigest\":\""+digest+"\",\"diagnosticReferenceComparisons\":"+referenceComparisons+",\"diagnosticTargetComparisons\":"+targetComparisons+",\"comparisonCountsAreContract\":false,\"targetExecuted\":"+!oracle+",\"status\":\""+(MISMATCHES.isEmpty()?"PASS":"FAIL")+"\",\"mismatches\":"+rows+"}");
    if(!MISMATCHES.isEmpty())System.exit(1);
  }
}
