/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
 * Caller-owned Guava Ordering API callback fixture; provider collection selected by FQN.
 */
import com.google.common.collect.Ordering;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

public final class GuavaMinMaxOrderingHookProof {
    private static Class<?> owner;
    private static MessageDigest digest;
    private static int rows,checks;
    private GuavaMinMaxOrderingHookProof() { }
    private record Item(String id,int rank) { @Override public String toString(){return id+"@"+rank;} }
    @FunctionalInterface private interface Op { Object run() throws Throwable; }
    private static Object invoke(Method method,Object target,Object... args) throws Throwable{
        try{return method.invoke(target,args);}catch(InvocationTargetException wrapped){throw wrapped.getCause();}
    }
    private static String outcome(Op action){try{Object result=action.run();return "VALUE/"+result;}catch(Throwable failure){if(failure instanceof AssertionError error)throw error;String message=failure.getMessage();return "EXCEPTION/"+failure.getClass().getName()+"/"+(message==null?"null":message.replace(owner.getName(),"<OWNER>"));}}
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static void row(String label,Object value){String line="REQUIRED_CONCRETE\t"+label+"\t"+value;digest.update((line+"\n").getBytes(StandardCharsets.UTF_8));rows++;System.out.println(line);}
    private static final class Hook extends Ordering<Item> {
        final String name,mode;final List<String> calls;final Hook other;int reverseCalls;
        Hook(String name,String mode,List<String> calls){this.name=name;this.mode=mode;this.calls=calls;this.other=mode.equals("other")||mode.equals("inconsistent")||mode.equals("callback")?new Hook("returned",mode.equals("inconsistent")?"forward":"reverse",calls):null;}
        @Override public int compare(Item a,Item b){calls.add(name+".compare/"+a+":"+b);return mode.equals("reverse")?Integer.compare(b.rank,a.rank):Integer.compare(a.rank,b.rank);}
        @Override @SuppressWarnings("unchecked") public <S extends Item> Ordering<S> reverse(){reverseCalls++;calls.add(name+".reverse");if(mode.equals("throw"))throw new IllegalStateException("reverse-sentinel");if(mode.equals("null"))return null;if(mode.equals("callback"))calls.add("reverse-side-effect");return (Ordering<S>)(other==null?this:other);}
        @Override public String toString(){return "caller-"+name+"-"+mode;}
    }
    private static final class Source extends AbstractCollection<Item> {
        final List<Item> values;final List<String> calls;final int report;final int throwSize;
        Source(List<Item> values,List<String> calls,int report,int throwSize){this.values=values;this.calls=calls;this.report=report;this.throwSize=throwSize;}
        @Override public int size(){calls.add("source.size/"+report);if(throwSize!=0)throw new IllegalStateException("size-sentinel");return report;}
        @Override public Iterator<Item> iterator(){calls.add("source.iterator");return new Iterator<>(){int index;
            @Override public boolean hasNext(){calls.add("source.hasNext/"+(index<values.size()));return index<values.size();}
            @Override public Item next(){Item value=values.get(index++);calls.add("source.next/"+value);return value;}};}
    }
    @SuppressWarnings("unchecked") private static Queue<Item> construct(Hook hook,Iterable<Item> source) throws Throwable{
        Object b=invoke(owner.getMethod("orderedBy",Comparator.class),null,hook);return (Queue<Item>)invoke(b.getClass().getMethod("create",Iterable.class),b,source);
    }
    public static void main(String[] args) throws Throwable{
        if(args.length!=1)throw new IllegalArgumentException("owner FQN");owner=Class.forName(args[0]);digest=MessageDigest.getInstance("SHA-256");
        for(String mode:new String[]{"self","other","inconsistent","null","throw","callback"})for(int n:new int[]{0,1,3}){
            List<String> calls=new ArrayList<>();Hook hook=new Hook("supplied",mode,calls);List<Item> initial=new ArrayList<>();for(int i=0;i<n;i++)initial.add(new Item("initial"+i,3-i));Source source=new Source(initial,calls,n,0);Queue<Item>[] holder=holder();
            String creation=outcome(()->{holder[0]=construct(hook,source);return "CREATED";});Queue<Item> q=holder[0];check(hook.reverseCalls==1,"exactly one caller reverse per attempted construction");List<String> constructorCalls=new ArrayList<>(calls);
            check(constructorCalls.getFirst().startsWith("source.size"),"Collection size before reverse");check(constructorCalls.get(1).equals("supplied.reverse"),"reverse after size");
            if(q==null){row("Ordering/construct/"+mode+"/"+n,creation+"|calls="+constructorCalls);continue;}
            Object comparator=invoke(owner.getMethod("comparator"),q);check(comparator==hook,"caller Ordering passthrough identity");calls.clear();String first=outcome(()->q.offer(new Item("added0",0))),second=outcome(()->q.offer(new Item("added1",4)));String max=outcome(()->invoke(owner.getMethod("peekLast"),q));row("Ordering/construct/"+mode+"/"+n,creation+"|constructor="+constructorCalls+"|passthrough=true|first="+first+"|second="+second+"|max="+max+"|operationCalls="+calls+"|size="+q.size()+"|array="+Arrays.toString(q.toArray()));
        }
        for(int report:new int[]{0,2,10})for(boolean collection:List.of(false,true)){
            List<String> calls=new ArrayList<>();Hook hook=new Hook("supplied","callback",calls);Source source=new Source(List.of(new Item("one",1),new Item("two",2)),calls,report,0);Iterable<Item> input=collection?source:source::iterator;String outcome=outcome(()->{Queue<Item> q=construct(hook,input);return Arrays.toString(q.toArray());});row("Ordering/timing/"+report+"/"+collection,outcome+"|calls="+calls);check(hook.reverseCalls==1,"one reverse for timing");
        }
        List<String> calls=new ArrayList<>();Hook hook=new Hook("supplied","callback",calls);String failure=outcome(()->construct(hook,new Source(List.of(),calls,3,1)));check(hook.reverseCalls==0,"size exception precedes reverse");row("Ordering/size-throw",failure+"|calls="+calls);
        List<String> nullCalls=new ArrayList<>();Hook nullSource=new Hook("supplied","callback",nullCalls);row("Ordering/null-source",outcome(()->construct(nullSource,null))+"|calls="+nullCalls+"|reverseCount="+nullSource.reverseCalls);
        System.out.println("{\"status\":\"PASS\",\"partition\":\"REQUIRED_CONCRETE\",\"rows\":"+rows+",\"checks\":"+checks+",\"sha256\":\""+HexFormat.of().formatHex(digest.digest())+"\",\"guavaDependency\":\"caller Ordering API callbacks only\"}");
    }
    @SuppressWarnings("unchecked") private static Queue<Item>[] holder(){return (Queue<Item>[])new Queue<?>[1];}
}
