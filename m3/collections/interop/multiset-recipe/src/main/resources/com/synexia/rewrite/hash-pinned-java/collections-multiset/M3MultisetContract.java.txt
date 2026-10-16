/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.m3.collections.M3LinkedHashMap;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.IntStream;

/** Finite independent provider and storage tests; not a complete API proof. */
public final class M3MultisetContract {
    private M3MultisetContract() { }
    private static long checks;
    private record Key(int value) { @Override public int hashCode() { return 7; } }
    private static void yes(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }
    private static void eq(Object a, Object b) { checks++; if (!Objects.equals(a,b)) throw new AssertionError(a+" != "+b+" at "+checks); }
    private static void throwsType(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("expected " + type, error);
        }
        throw new AssertionError("missing " + type);
    }
    @SuppressWarnings("unchecked")
    private static void invariant(M3Multiset<?> bag) throws Exception {
        Field f=M3Multiset.class.getDeclaredField("rows"); f.setAccessible(true);
        M3LinkedHashMap<Object,Integer> rows=(M3LinkedHashMap<Object,Integer>)f.get(bag);
        rows.verifyInvariants();
        f=M3Multiset.class.getDeclaredField("counts");f.setAccessible(true);int[] counts=(int[])f.get(bag);
        f=M3Multiset.class.getDeclaredField("highWater");f.setAccessible(true);int used=f.getInt(bag);
        f=M3Multiset.class.getDeclaredField("freeHead");f.setAccessible(true);int free=f.getInt(bag);
        boolean[] seen=new boolean[used];long total=0;
        for (Integer row:rows.values()) { yes(row>=0 && row<used && !seen[row]); seen[row]=true;yes(counts[row]>0);total+=counts[row]; }
        while(free>=0) { yes(free<used && !seen[free]);seen[free]=true;yes(counts[free]<=0);free=-counts[free]-1; }
        for(boolean present:seen)yes(present);
        eq(total,bag.longSize());eq(rows.size(),bag.elementSet().size());
    }
    private static void same(M3Multiset<Key> actual, Multiset<Key> expected) throws Exception {
        eq(expected,actual);eq(actual,expected);eq(actual.hashCode(),expected.hashCode());
        eq(actual.size(),expected.size());eq(actual.isEmpty(),expected.isEmpty());
        eq(new ArrayList<>(actual.elementSet()),new ArrayList<>(expected.elementSet()));
        eq(actual.entrySet(),expected.entrySet());
        for(int i=-1;i<64;i++){Key k=i<0?null:new Key(i);eq(actual.count(k),expected.count(k));}
        invariant(actual);
    }
    private static void traces() throws Exception {
        for(int seed=0;seed<12;seed++) {
            Random r=new Random(seed);M3Multiset<Key>a=new M3Multiset<>();Multiset<Key>b=LinkedHashMultiset.create();
            for(int step=0;step<1600;step++) {
                Key k=r.nextInt(17)==0?null:new Key(r.nextInt(64));int n=r.nextInt(12);
                switch(r.nextInt(12)) {
                    case 0,1,2 -> eq(a.add(k,n),b.add(k,n));
                    case 3 -> eq(a.remove(k,n),b.remove(k,n));
                    case 4 -> eq(a.setCount(k,n),b.setCount(k,n));
                    case 5 -> {int old=r.nextBoolean()?a.count(k):r.nextInt(12);eq(a.setCount(k,old,n),b.setCount(k,old,n));}
                    case 6 -> eq(a.elementSet().remove(k),b.elementSet().remove(k));
                    case 7 -> {var e=Multisets.immutableEntry(k,n);eq(a.entrySet().remove(e),b.entrySet().remove(e));}
                    case 8 -> {
                        if(!a.isEmpty()){var ai=a.iterator();var bi=b.iterator();int skip=r.nextInt(Math.min(a.size(),12));
                            for(int j=0;j<=skip;j++)eq(ai.next(),bi.next());ai.remove();bi.remove();
                            throwsType(IllegalStateException.class,ai::remove);}
                    }
                    case 9 -> {var keys=Arrays.asList(k,new Key(r.nextInt(64)));eq(a.removeAll(keys),b.removeAll(keys));}
                    case 10 -> {if(step%97==0){a.clear();b.clear();}else {var keys=Arrays.asList(k,new Key(1),null);eq(a.addAll(keys),b.addAll(keys));}}
                    default -> {if(step%31==0){var keys=Arrays.asList(k,new Key(1),null);eq(a.retainAll(keys),b.retainAll(keys));}}
                }
                same(a,b);
            }
            eq(new ArrayList<>(a),new ArrayList<>(b));
        }
    }
    private static void boundaries() throws Exception {
        M3Multiset<String>a=new M3Multiset<>();
        eq(a.add(null,0),0);yes(a.isEmpty());throwsType(IllegalArgumentException.class,()->a.add(null,-1));
        throwsType(IllegalArgumentException.class,()->a.remove(null,-1));throwsType(IllegalArgumentException.class,()->a.setCount(null,-1));
        throwsType(IllegalArgumentException.class,()->a.setCount("absent",-1,0));
        throwsType(IllegalArgumentException.class,()->a.setCount("absent",1,-1));
        a.add(null,Integer.MAX_VALUE);a.add("x",Integer.MAX_VALUE);
        eq(a.size(),Integer.MAX_VALUE);eq(a.longSize(),2L*Integer.MAX_VALUE);
        throwsType(IllegalArgumentException.class,()->a.add(null,1));eq(a.count(null),Integer.MAX_VALUE);
        Multiset<String>b=LinkedHashMultiset.create();b.add(null,Integer.MAX_VALUE);b.add("x",Integer.MAX_VALUE);
        eq(a,b);eq(b,a);eq(a.hashCode(),b.hashCode());
        b.remove("x");yes(!a.equals(b));yes(!b.equals(a));
        throwsType(IllegalArgumentException.class,()->a.addAll(a));eq(a.longSize(),2L*Integer.MAX_VALUE);
        invariant(a);a.clear();invariant(a);
        var elements=a.elementSet();var entries=a.entrySet();a.add("z",3);a.add(null,2);
        eq(elements.size(),2);eq(entries.size(),2);var snapshot=entries.iterator().next();a.add("z");eq(snapshot.getCount(),3);
        throwsType(UnsupportedOperationException.class,()->elements.add("new"));
        Iterator<String> stale=a.iterator();a.add("z");throwsType(ConcurrentModificationException.class,stale::next);
        Iterator<String> it=a.iterator();throwsType(IllegalStateException.class,it::remove);
        while(it.hasNext()){it.next();it.remove();invariant(a);}yes(a.isEmpty());
        throwsType(NoSuchElementException.class,it::next);
        a.add("a",2);a.add("b",3);a.addAll(a);eq(a.count("a"),4);eq(a.longSize(),10L);
        yes(a.containsAll(Collections.nCopies(100,"a")));yes(a.retainAll(Collections.singleton("a")));eq(a.count("a"),4);
        yes(a.removeAll(a));yes(!a.removeAll(a));yes(!a.retainAll(a));
        for(int j=0;j<3000;j++){a.add("key"+j,7);a.remove("key"+j,7);}invariant(a);
        Field f=M3Multiset.class.getDeclaredField("highWater");f.setAccessible(true);yes(f.getInt(a)<=2);
        a.add("old");throwsType(ConcurrentModificationException.class,()->a.forEachEntry((k,c)->a.add("new")));
        a.clear();a.add(null,4);a.add("a",3);var ei=a.entrySet().iterator();ei.next();ei.remove();eq(a.count(null),0);invariant(a);
        eq(a.toString(),LinkedHashMultiset.create(a).toString());
        Object[] array=a.toArray();eq(array.length,a.size());eq(Arrays.asList(a.toArray(new String[0])),new ArrayList<>(a));
    }
    private static void algebra() throws Exception {
        for(int seed=0;seed<100;seed++) {
            Random r=new Random(seed);M3Multiset<Integer>a=new M3Multiset<>(),b=new M3Multiset<>();
            for(int k=0;k<25;k++){a.add(k,r.nextInt(12));b.add(k,r.nextInt(12));}a.add(null,3);b.add(null,7);
            M3Multiset<Integer> u=M3Multisets.union(a,b),i=M3Multisets.intersection(a,b),s=M3Multisets.sum(a,b),d=M3Multisets.difference(a,b);
            eq(u,Multisets.union(a,b));eq(i,Multisets.intersection(a,b));eq(s,Multisets.sum(a,b));eq(d,Multisets.difference(a,b));
            yes(M3Multisets.containsOccurrences(u,a));yes(M3Multisets.containsOccurrences(a,i));
            int retained=u.count(null);a.clear();b.clear();eq(u.count(null),retained);
            invariant(u);invariant(i);invariant(s);invariant(d);
        }
        M3Multiset<Integer>a=new M3Multiset<>(),b=new M3Multiset<>();a.add(1,Integer.MAX_VALUE);b.add(1,1);
        throwsType(IllegalArgumentException.class,()->M3Multisets.sum(a,b));eq(a.count(1),Integer.MAX_VALUE);eq(b.count(1),1);
        var sequential=IntStream.range(0,20000).mapToObj(x->x%57).collect(M3Multisets.toMultiset());
        var parallel=IntStream.range(0,20000).parallel().mapToObj(x->x%57).collect(M3Multisets.toMultiset());
        eq(sequential,parallel);eq(new ArrayList<>(sequential.elementSet()),new ArrayList<>(parallel.elementSet()));invariant(parallel);
        eq(M3Multiset.create(sequential),sequential);eq(M3Multisets.copyOf(sequential),sequential);
    }
    private static void countLanes(boolean nativeMode) throws Exception {
        if(nativeMode)M3CountAlgebra.loadNativeLibrary();
        for(int n:new int[]{0,1,2,1023,1024,1025,2047,2048,2049,8193}) {
            Random r=new Random(n);int[]a=new int[n],b=new int[n];for(int k=0;k<n;k++){a[k]=r.nextInt(100000);b[k]=r.nextInt(100000);}
            int[]oa=a.clone(),ob=b.clone();
            for(var op:M3CountAlgebra.Operation.values()){
                int[]value=nativeMode?M3CountAlgebra.combineNative(a,b,op):M3CountAlgebra.combine(a,b,op);
                for(int k=0;k<n;k++){long x=a[k],y=b[k];long e=switch(op){case SUM->x+y;case MAX->Math.max(x,y);case MIN->Math.min(x,y);case DIFFERENCE->Math.max(0,x-y);};eq(value[k],(int)e);}
                yes(Arrays.equals(a,oa));yes(Arrays.equals(b,ob));yes(value!=a&&value!=b);
            }
        }
        for(int n:new int[]{1,1024,1025,2049}){
            int[]a=new int[n],b=new int[n];a[n-1]=Integer.MAX_VALUE;b[n-1]=1;
            throwsType(IllegalArgumentException.class,()->M3CountAlgebra.combine(a,b,M3CountAlgebra.Operation.SUM));
            if(nativeMode)throwsType(IllegalArgumentException.class,()->M3CountAlgebra.combineNative(a,b,M3CountAlgebra.Operation.SUM));
            for(var op:M3CountAlgebra.Operation.values()) {a[n-1]=-1;
                throwsType(IllegalArgumentException.class,()->M3CountAlgebra.combine(a,b,op));
                if(nativeMode)throwsType(IllegalArgumentException.class,()->M3CountAlgebra.combineNative(a,b,op));}
        }
        throwsType(NullPointerException.class,()->M3CountAlgebra.combine(null,new int[0],M3CountAlgebra.Operation.MIN));
        throwsType(IllegalArgumentException.class,()->M3CountAlgebra.combine(new int[1],new int[0],M3CountAlgebra.Operation.MIN));
        if(nativeMode){
            var method=M3CountAlgebra.class.getDeclaredMethod("combine0",int[].class,int[].class,int.class);method.setAccessible(true);
            for(Object[]args:new Object[][]{{null,new int[0],0},{new int[0],null,0},{new int[1],new int[0],0},{new int[0],new int[0],-1},{new int[0],new int[0],4}}){
                checks++;try{method.invoke(null,args);throw new AssertionError("native refusal");}catch(InvocationTargetException error){yes(error.getCause() instanceof IllegalArgumentException || error.getCause() instanceof NullPointerException);}
            }
        }
    }
    public static long run(boolean nativeMode) throws Exception {
        checks=0;traces();boundaries();algebra();countLanes(nativeMode);
        System.out.println("PASS M3MultisetContract mode="+(nativeMode?"JNI":"JAVA")+" assertions="+checks+" randomized_operations=19200");return checks;
    }
    public static void main(String[]args) throws Exception {
        if(args.length>0&&args[0].equals("missing")){
            throwsType(UnsatisfiedLinkError.class,M3CountAlgebra::loadNativeLibrary);System.out.println("PASS missing-library refusal");return;
        }
        run(args.length>0&&args[0].equals("native"));
    }
}
