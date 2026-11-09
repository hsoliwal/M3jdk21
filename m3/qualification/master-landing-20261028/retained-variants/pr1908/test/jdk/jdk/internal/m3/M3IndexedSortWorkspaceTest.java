// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Stable indexed view ordering, callbacks, epochs and ownership with allocation observations
 * @modules java.base/jdk.internal.m3 jdk.management
 * @run main/othervm -Xmx512m M3IndexedSortWorkspaceTest
 */
import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import jdk.internal.m3.M3IndexedArray;

public final class M3IndexedSortWorkspaceTest {
    private record Item(int id, int key) { }
    private static int checks;
    private static volatile long consumed;
    private static void check(boolean value, String why) {
        checks++; if(!value)throw new AssertionError(why);
    }
    private static int id(Item x) { return x==null?-1:x.id(); }
    private static int compare(Item a,Item b) {
        return a==b?0:a==null?-1:b==null?1:Integer.compare(a.key(),b.key());
    }
    /** Independent list-of-runs merge oracle; records the original stable comparison order. */
    private static List<Item> expected(List<Item> selected,List<Long> trace) {
        List<List<Item>> runs=new ArrayList<>();
        for(Item x:selected){List<Item> run=new ArrayList<>();run.add(x);runs.add(run);}
        while(runs.size()>1) {
            List<List<Item>> merged=new ArrayList<>();
            for(int k=0;k<runs.size();k+=2) {
                if(k+1==runs.size()){merged.add(runs.get(k));continue;}
                List<Item> a=runs.get(k),b=runs.get(k+1),out=new ArrayList<>();int i=0,j=0;
                while(i<a.size()&&j<b.size()) {
                    Item left=a.get(i),right=b.get(j);trace.add(((long)id(left)<<32)|Integer.toUnsignedLong(id(right)));
                    if(compare(left,right)<=0){out.add(left);i++;}else{out.add(right);j++;}
                }
                out.addAll(a.subList(i,a.size()));out.addAll(b.subList(j,b.size()));merged.add(out);
            }
            runs=merged;
        }
        return runs.isEmpty()?List.of():runs.getFirst();
    }
    private static void behavior() {
        Random random=new Random(0x3a4421L);
        for(int n:new int[]{0,1,2,3,4,5,7,8,9,15,16,17,31,32,33,63,64,65,127,128,129}) {
            M3IndexedArray<Item> owner=new M3IndexedArray<>();List<Item> logical=new ArrayList<>();List<Long> handles=new ArrayList<>();
            for(int i=0;i<n;i++){Item x=i%17==0?null:new Item(i,random.nextInt(9));logical.add(x);handles.add(owner.append(x));}
            // Recycle physical slots, preserving independent logical encounter order.
            for(int i=n-1;i>=0;i--)if(i%7==3){owner.remove(handles.remove(i));logical.remove(i);}
            for(int i=0;i<n/7;i++){Item x=new Item(n+i,random.nextInt(9));logical.add(x);handles.add(owner.append(x));}
            for(int i=0;i<handles.size();i++)if(i%3==0)owner.retain(handles.get(i));
            for(int mode=0;mode<4;mode++)for(int selection:new int[]{0,1,2,5}) {
                boolean masked=(mode&1)!=0,filtered=(mode&2)!=0;
                List<Integer> calls=new ArrayList<>(),expectedCalls=new ArrayList<>();List<Long> trace=new ArrayList<>(),expectedTrace=new ArrayList<>();
                Predicate<Item> predicate=x->{calls.add(id(x));return selection==0||selection!=1&&Math.floorMod(id(x),selection)==0;};
                List<Item> selected=new ArrayList<>();
                for(int i=0;i<logical.size();i++)if(!masked||i%3==0) {
                    Item x=logical.get(i);if(filtered)expectedCalls.add(id(x));
                    if(!filtered||selection==0||selection!=1&&Math.floorMod(id(x),selection)==0)selected.add(x);
                }
                Comparator<Item> order=(a,b)->{trace.add(((long)id(a)<<32)|Integer.toUnsignedLong(id(b)));return compare(a,b);};
                List<Item> view=masked?(filtered?owner.filteredMaskedSortedStates(predicate,M3IndexedArray.LEASED,0,order):owner.maskedSortedStates(M3IndexedArray.LEASED,0,order)):(filtered?owner.filteredSorted(predicate,order):owner.sorted(order));
                List<Item> want=expected(selected,expectedTrace);check(view.size()==want.size(),"exact selection size");
                for(int i=0;i<want.size();i++)check(view.get(i)==want.get(i),"stable identity and logical order");
                check(calls.equals(expectedCalls),"predicate order/count");check(trace.equals(expectedTrace),"comparator order/count");
                int count=calls.size()+trace.size();view.size();check(count==calls.size()+trace.size(),"cached view must not re-sort");
                if(!view.isEmpty()){List<Item> range=view.subList(0,view.size());check(range.getFirst()==view.getFirst(),"range identity");}
            }
        }
        M3IndexedArray<Integer> owner=new M3IndexedArray<>();List<Long> handles=new ArrayList<>();for(int i=0;i<16;i++)handles.add(owner.append(i));
        List<Integer> view=owner.filteredSorted(x->x%2==0,Comparator.reverseOrder());check(view.equals(List.of(14,12,10,8,6,4,2,0)),"reverse view");
        Iterator<Integer> cursor=view.iterator();List<Integer> range=view.subList(1,3);owner.replace(handles.get(0),100);
        expect(ConcurrentModificationException.class,cursor::hasNext);expect(ConcurrentModificationException.class,range::size);check(view.getFirst()==100,"refresh after replacement");
        RuntimeException failure=new RuntimeException("comparator sentinel");AtomicBoolean fail=new AtomicBoolean(true);
        List<Integer> retry=owner.filteredSorted(x->x%2==0,(a,b)->{if(fail.get())throw failure;return Integer.compare(a,b);});
        try{retry.size();throw new AssertionError("missing exception");}catch(RuntimeException e){check(e==failure,"comparator exception identity");}
        fail.set(false);check(retry.getFirst()==2,"failed sort not published");
        AtomicBoolean mutate=new AtomicBoolean(true);List<Integer> invalid=owner.filteredSorted(x->true,(a,b)->{if(mutate.getAndSet(false))owner.append(50);return Integer.compare(a,b);});
        expect(ConcurrentModificationException.class,invalid::size);check(invalid.size()==17,"reentrant failure recovers");
        AtomicBoolean lease=new AtomicBoolean(true);List<Integer> states=owner.filteredMaskedSortedStates(x->x%2==0,M3IndexedArray.LIVE,0,(a,b)->{if(lease.getAndSet(false))owner.retain(handles.get(1));return Integer.compare(a,b);});
        expect(ConcurrentModificationException.class,states::size);check(states.size()==9,"state epoch failure recovers");
    }
    private static void expect(Class<? extends Throwable> type,Runnable task) {
        try{task.run();throw new AssertionError("missing "+type.getName());}catch(Throwable e){check(type.isInstance(e),"exception class "+e);}
    }
    private static void allocation() {
        ThreadMXBean bean=(ThreadMXBean)ManagementFactory.getThreadMXBean();
        if(!bean.isThreadAllocatedMemorySupported()){System.out.println("SKIP allocation counter unsupported");return;}
        bean.setThreadAllocatedMemoryEnabled(true);long tid=Thread.currentThread().threadId();
        M3IndexedArray<Integer> owner=new M3IndexedArray<>();int n=8192;
        for(int i=n-1;i>=0;i--)owner.append(i);
        Predicate<Integer> even=x->(x&1)==0;Comparator<Integer> order=Integer::compare;
        for(int i=0;i<8;i++)consumed+=owner.filteredSorted(even,order).size();
        long allocated=Long.MAX_VALUE;
        for(int i=0;i<4;i++) {
            List<Integer> view=owner.filteredSorted(even,order);long before=bean.getThreadAllocatedBytes(tid);
            consumed+=view.size();long bytes=bean.getThreadAllocatedBytes(tid)-before;allocated=Math.min(allocated,bytes);
            check(view.size()==n/2&&view.getFirst()==0&&view.getLast()==n-2,"allocation case semantics");
        }
        // This is an observation, not an optimization acceptance gate. The
        // workspace-reuse candidate was rejected by CPU/live-memory evidence.
        System.out.println("allocation_bytes="+allocated+" consumed="+consumed);
    }
    public static void main(String[] args) {
        if(args.length==0||args[0].equals("behavior"))behavior();
        if(args.length==0||args[0].equals("allocation"))allocation();
        System.out.println("M3IndexedSortWorkspaceTest PASS checks="+checks);
    }
}
