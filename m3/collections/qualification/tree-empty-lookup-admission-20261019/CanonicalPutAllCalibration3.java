// SPDX-License-Identifier: Apache-2.0
// Adapted owned public fixture; source-aware calibration, no implementation source copied.
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.security.MessageDigest;
import java.util.*;

public final class CanonicalPutAllCalibration3 {
  private CanonicalPutAllCalibration3() { }
  static final String MARKER="M3_EC_CALIBRATION3_SOURCE_AWARE_V1";
  interface Action { Object run(); }
  static Map<String,Object> row(Object... pairs) {
    Map<String,Object> out=new LinkedHashMap<>();
    for(int i=0;i<pairs.length;i+=2)out.put((String)pairs[i],pairs[i+1]);
    return out;
  }
  static String json(Object value) {
    if(value==null)return "null";
    if(value instanceof Number||value instanceof Boolean)return value.toString();
    if(value instanceof Map<?,?> map) {
      List<String> parts=new ArrayList<>();
      for(Map.Entry<?,?> entry:map.entrySet())parts.add(json(entry.getKey())+":"+json(entry.getValue()));
      return "{"+String.join(",",parts)+"}";
    }
    if(value instanceof Iterable<?> list) {
      List<String> parts=new ArrayList<>();for(Object item:list)parts.add(json(item));
      return "["+String.join(",",parts)+"]";
    }
    return "\""+String.valueOf(value).replace("\\","\\\\").replace("\"","\\\"").replace("\r","\\r").replace("\n","\\n").replace("\t","\\t")+"\"";
  }
  static Map<String,Object> failure(Throwable error) {
    return row("kind","throw","class",error.getClass().getName(),"message",error.getMessage());
  }
  static Map<String,Object> outcome(Action action) {
    try { return row("kind","return","value",action.run()); }
    catch(RuntimeException|Error error) { return failure(error); }
  }
  static List<Object> entries(Iterable<Map.Entry<Integer,String>> values) {
    List<Object> out=new ArrayList<>();int count=0;
    for(Map.Entry<Integer,String> entry:values) {
      if(++count>16)throw new IllegalStateException("snapshot-entry-limit");
      out.add(row("key",entry.getKey(),"value",entry.getValue()));
    }
    return out;
  }
  static Map<String,Object> snapshot(SortedMap<Integer,String> map) {
    Map<String,Object> out=new LinkedHashMap<>();
    try {out.put("size",map.size());}catch(RuntimeException error){out.put("size",failure(error));}
    try {out.put("empty",map.isEmpty());}catch(RuntimeException error){out.put("empty",failure(error));}
    try {out.put("entries",entries(map.entrySet()));}catch(RuntimeException error){out.put("entries",failure(error));}
    try {out.put("sidePresent",map.containsKey(99));out.put("sideValue",map.get(99));}
    catch(RuntimeException error){out.put("sideLookup",failure(error));}
    return out;
  }
  static Map<String,Object> rangeSnapshot(SortedMap<Integer,String> map,Set<Map.Entry<Integer,String>> retained) {
    Map<String,Object> out=new LinkedHashMap<>();
    try {out.put("size",map.size());}catch(RuntimeException error){out.put("size",failure(error));}
    try {out.put("empty",map.isEmpty());}catch(RuntimeException error){out.put("empty",failure(error));}
    try {out.put("entries",entries(retained));}catch(RuntimeException error){out.put("entries",failure(error));}
    return out;
  }
  static final class Order implements Comparator<Integer> {
    final String name;final List<Object> events;
    Order(String name,List<Object> events){this.name=name;this.events=events;}
    public int compare(Integer left,Integer right) {
      events.add(row("operation","compare","receiver",name,"left",left,"right",right));
      return Integer.compare(left,right);
    }
    public boolean equals(Object other) {
      String rhs=other instanceof Order order?order.name:other==null?"null":"other";
      boolean answer=this==other||name.equals("target")&&rhs.equals("source");
      events.add(row("operation","equals","receiver",name,"other",rhs,"result",answer));return answer;
    }
    public int hashCode(){return 1;}
  }
  static final class Source extends AbstractMap<Integer,String> implements SortedMap<Integer,String> {
    final SortedMap<Integer,String> target;final String scenario;
    final Comparator<Integer> order;final List<Object> events;
    final SortedMap<Integer,String> range;
    final Set<Map.Entry<Integer,String>> retained;
    Iterator<Map.Entry<Integer,String>> held;
    boolean inserted,firstNext;
    Source(SortedMap<Integer,String> target,String scenario,Comparator<Integer> order,List<Object> events) {
      this.target=target;this.scenario=scenario;this.order=order;this.events=events;
      range=scenario.equals("range-cache")?target.headMap(50):null;
      retained=range==null?null:range.entrySet();
    }
    void touch(String callback) {
      Map<String,Object> event=row("callback",callback,"before",snapshot(target));events.add(event);
      if(callback.equals("comparator")&&!scenario.equals("asymmetric-equals")&&!inserted) {
        inserted=true;event.put("insert99",outcome(()->target.put(99,"side")));
      }
      if(callback.equals("iterator")&&scenario.equals("held-remove")) {
        held=target.entrySet().iterator();event.put("heldAcquired",true);
        event.put("heldHasNext",outcome(held::hasNext));
      }
      if(callback.equals("next")&&!firstNext) {
        firstNext=true;
        if(range!=null)event.put("retainedRange",rangeSnapshot(range,retained));
      }
      event.put("after",snapshot(target));
    }
    public int size(){touch("size");return 2;}
    public Comparator<? super Integer> comparator(){touch("comparator");return order;}
    public Set<Map.Entry<Integer,String>> entrySet() {
      touch("entrySet");
      return new AbstractSet<>() {
        public int size(){touch("entrySet.size");return 2;}
        public Iterator<Map.Entry<Integer,String>> iterator() {
          touch("iterator");
          Iterator<Map.Entry<Integer,String>> fixed=List.<Map.Entry<Integer,String>>of(
              new SimpleImmutableEntry<>(1,"a"),new SimpleImmutableEntry<>(2,"b")).iterator();
          return new Iterator<>() {
            public boolean hasNext(){touch("hasNext");return fixed.hasNext();}
            public Map.Entry<Integer,String> next(){touch("next");return fixed.next();}
          };
        }
      };
    }
    public Integer firstKey(){touch("firstKey");return 1;}
    public Integer lastKey(){touch("lastKey");return 2;}
    public SortedMap<Integer,String> headMap(Integer key){throw new UnsupportedOperationException("source headMap unused");}
    public SortedMap<Integer,String> tailMap(Integer key){throw new UnsupportedOperationException("source tailMap unused");}
    public SortedMap<Integer,String> subMap(Integer low,Integer high){throw new UnsupportedOperationException("source subMap unused");}
  }
  static Map<String,Object> identity(Class<?> type) throws Exception {
    String resource="/"+type.getName().replace('.','/')+".class";URL url=type.getResource(resource);byte[] bytes;
    try(java.io.InputStream input=type.getResourceAsStream(resource)) {
      if(input==null)throw new IllegalStateException("missing resource");bytes=input.readAllBytes();
    }
    return row("class",type.getName(),"codeSource",String.valueOf(type.getProtectionDomain().getCodeSource().getLocation()),
        "resource",String.valueOf(url),"classSha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
  }
  @SuppressWarnings("unchecked")
  static SortedMap<Integer,String> receiver(Class<?> type,Comparator<Integer> order) throws Exception {
    return (SortedMap<Integer,String>)(order==null?type.getConstructor().newInstance():type.getConstructor(Comparator.class).newInstance(order));
  }
  public static void main(String[] args) throws Exception {
    if(args.length!=2)throw new IllegalArgumentException("receiverClass apiClass");
    Class<?> type=Class.forName(args[0]),api=Class.forName(args[1]);
    System.out.println(json(row("kind","identity","marker",MARKER,"receiver",identity(type),"api",identity(api),
        "javaVersion",System.getProperty("java.version"),"javaVendor",System.getProperty("java.vendor"),
        "javaHome",System.getProperty("java.home"),"temp",System.getProperty("java.io.tmpdir"),
        "userHome",System.getProperty("user.home"),"jvmArguments",ManagementFactory.getRuntimeMXBean().getInputArguments())));
    for(String scenario:List.of("held-remove","range-cache","asymmetric-equals")) {
      List<Object> events=new ArrayList<>();
      Comparator<Integer> targetOrder=scenario.equals("asymmetric-equals")?new Order("target",events):null;
      Comparator<Integer> sourceOrder=targetOrder==null?null:new Order("source",events);
      SortedMap<Integer,String> target=receiver(type,targetOrder);
      Source source=new Source(target,scenario,sourceOrder,events);
      Map<String,Object> result=row("kind","case","marker",MARKER,"scenario",scenario,"initial",snapshot(target));
      result.put("putAll",outcome(()->{target.putAll(source);return "completed";}));
      result.put("afterPutAll",snapshot(target));
      if(source.held!=null) {
        Iterator<Map.Entry<Integer,String>> held=source.held;
        result.put("hasNextBeforeNext",outcome(held::hasNext));
        result.put("next",outcome(()->{Map.Entry<Integer,String> entry=held.next();return row("key",entry.getKey(),"value",entry.getValue());}));
        result.put("afterNext",snapshot(target));result.put("hasNextBeforeRemove",outcome(held::hasNext));
        result.put("remove",outcome(()->{held.remove();return "completed";}));
        result.put("afterRemove",snapshot(target));result.put("hasNextAfterRemove",outcome(held::hasNext));
      }
      if(source.range!=null)result.put("retainedRangeAfter",rangeSnapshot(source.range,source.retained));
      result.put("events",events);result.put("final",snapshot(target));System.out.println(json(result));
    }
  }
}
