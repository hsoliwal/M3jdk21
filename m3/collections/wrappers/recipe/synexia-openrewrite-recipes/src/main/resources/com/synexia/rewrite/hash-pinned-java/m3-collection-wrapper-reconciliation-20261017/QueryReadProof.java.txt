// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
import com.m3.collections.M3FilteredMaps;
import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3TreeMap;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.IntSupplier;

/** Identical program runs against old/new SDK-compiled owners; no cached predicates. */
public final class QueryReadProof {
    private static volatile int sink;
    private static final StringBuilder trace = new StringBuilder();
    private record Value(int id) {
        @Override public boolean equals(Object o) {trace.append('e').append(id).append(';');return o instanceof Value v&&v.id==id;}
        @Override public int hashCode() {trace.append('h').append(id).append(';');return id;}
    }
    private static Map<Integer, Value> owner(int kind) {
        return switch(kind){case 0->new LinkedHashMap<>();case 1->new M3LinkedHashMap<>();case 2->new TreeMap<>();default->new M3TreeMap<>();};
    }
    private static void semantics() throws Exception {
        for(int kind=0;kind<4;kind++)for(int seed=0;seed<16;seed++) {
            Map<Integer,Value> raw=owner(kind);Random r=new Random(seed);
            for(int i=0;i<40;i++)raw.put(i,i%9==0?null:new Value(r.nextInt(20)));
            Map<Integer,Value> view=M3FilteredMaps.filtered(raw,(k,v)->{trace.append('p').append(k).append(';');return (k&3)==0;});
            trace.append(view.size()).append('/').append(view.isEmpty()).append('/').append(view.hashCode());
            for(int value=-1;value<21;value++)trace.append(view.containsValue(value<0?null:new Value(value))).append(';');
            raw.put(42,new Value(seed));trace.append(view.containsValue(new Value(seed)));
            // Stop at the same throwing predicate; no swallowed or moved callbacks.
            var broken=M3FilteredMaps.filtered(raw,(k,v)->{trace.append('x').append(k);if(k==7)throw new IllegalStateException("predicate");return true;});
            for(IntSupplier action:List.<IntSupplier>of(broken::size,broken::hashCode,()->broken.containsValue(new Value(100))?1:0)) {
                try{action.getAsInt();throw new AssertionError("expected predicate failure");}
                catch(IllegalStateException expected){trace.append("THROW");}
            }
        }
        String h=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(trace.toString().getBytes(StandardCharsets.UTF_8)));
        System.out.println("QUERY_TRACE sha256="+h+" events_chars="+trace.length());
    }
    private static long allocation(IntSupplier action) {
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        if(!bean.isThreadAllocatedMemorySupported())throw new AssertionError("allocation metric unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);
        for(int i=0;i<80;i++)sink^=action.getAsInt();
        long id=Thread.currentThread().threadId(),start=bean.getThreadAllocatedBytes(id);
        for(int i=0;i<64;i++)sink^=action.getAsInt();
        return bean.getThreadAllocatedBytes(id)-start;
    }
    private static void metrics() {
        Map<Integer,Integer> raw=new LinkedHashMap<>();for(int i=0;i<4096;i++)raw.put(i,i);
        var view=M3FilteredMaps.filtered(raw,(k,v)->(k&1)==0);
        System.out.println("ALLOC hashCode="+allocation(view::hashCode)+" containsValueMiss="+allocation(()->view.containsValue(-1)?1:0)+
                " size="+allocation(view::size)+" isEmpty="+allocation(()->view.isEmpty()?1:0)+" calls=64 entries=4096 escape_analysis=false");
    }
    public static void main(String[]args)throws Exception{if(args.length>0)metrics();else semantics();}
}
