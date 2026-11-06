/*
 * @test
 * @summary Sparse zero-fill preserves bounds, allocation and overlap contracts
 * @modules java.base/jdk.internal.mindex
 * @run main M3SparseFillTest
 */
// SPDX-License-Identifier: Apache-2.0
import jdk.internal.mindex.M3IntLane28;
import jdk.internal.mindex.M3LongLane28;
import java.util.Random;
import java.util.TreeMap;

public final class M3SparseFillTest {
    private static final int MAX=1<<28, PAGE=4096, REGION=1<<20;
    private static long checks;
    private static volatile long sink;
    private interface Lane {
        long get(int p); void set(int p,long v); void fill(int f,int t,long v);
        int blocks(); int regions(); long bytes(); boolean allocated(int p);
    }
    private static Lane ints() {
        var lane=new M3IntLane28();
        return new Lane() {
            public long get(int p){return lane.get(p);}
            public void set(int p,long v){lane.set(p,(int)v);}
            public void fill(int f,int t,long v){lane.fill(f,t,(int)v);}
            public int blocks(){return lane.allocatedBlockCount();}
            public int regions(){return lane.allocatedRegionCount();}
            public long bytes(){return lane.payloadBytes();}
            public boolean allocated(int p){return lane.isBlockAllocated(p);}
        };
    }
    private static Lane longs() {
        var lane=new M3LongLane28();
        return new Lane() {
            public long get(int p){return lane.get(p);}
            public void set(int p,long v){lane.set(p,v);}
            public void fill(int f,int t,long v){lane.fill(f,t,v);}
            public int blocks(){return lane.allocatedBlockCount();}
            public int regions(){return lane.allocatedRegionCount();}
            public long bytes(){return lane.payloadBytes();}
            public boolean allocated(int p){return lane.isBlockAllocated(p);}
        };
    }
    private static void equal(long a,long b){checks++;if(a!=b)throw new AssertionError(a+" != "+b);}
    private static void bounds(Lane lane) {
        int[][] bad={{-1,0},{0,MAX+1},{2,1},{Integer.MIN_VALUE,Integer.MAX_VALUE},{MAX+1,MAX+1},{-1,-1}};
        for(int[] range:bad)for(int value:new int[]{0,1}) {
            try{lane.fill(range[0],range[1],value);throw new AssertionError("bounds accepted");}
            catch(IndexOutOfBoundsException expected){checks++;}
        }
    }
    private static void verify(Lane lane,int bytes) {
        bounds(lane);
        lane.fill(0,MAX,0); lane.fill(MAX,MAX,0); lane.fill(MAX,MAX,7);
        equal(0,lane.blocks());equal(0,lane.regions());equal(0,lane.bytes());
        int[] edges={0,1,PAGE-1,PAGE,PAGE+1,REGION-1,REGION,REGION+1,MAX-PAGE,MAX-2,MAX-1};
        var expected=new TreeMap<Integer,Long>();
        for(int p:edges){lane.set(p,17);expected.put(p,17L);}
        bounds(lane);
        int blocks=lane.blocks(),regions=lane.regions();
        for(int from:edges)for(int to:edges)if(from<=to) {
            for(int p:edges){lane.set(p,17);expected.put(p,17L);}
            lane.fill(from,to,0);expected.subMap(from,true,to,false).clear();
            for(int p:edges)equal(expected.getOrDefault(p,0L),lane.get(p));
            equal(blocks,lane.blocks());equal(regions,lane.regions());equal((long)blocks*PAGE*bytes,lane.bytes());
        }
        var random=new Random(0x5a4628);
        int[] bases={0,REGION-2*PAGE,17*REGION+PAGE,127*REGION-PAGE,MAX-3*PAGE};
        for(int op=0;op<1500;op++) {
            int from=bases[random.nextInt(bases.length)]+random.nextInt(PAGE);
            int to=from+random.nextInt(PAGE+1);
            long value=random.nextInt(7)-3;
            if(op%7==0){from=random.nextInt(MAX);to=from+random.nextInt(MAX-from+1);value=0;}
            int beforeBlocks=lane.blocks(),beforeRegions=lane.regions();
            lane.fill(from,to,value);
            if(value==0){expected.subMap(from,true,to,false).clear();equal(beforeBlocks,lane.blocks());equal(beforeRegions,lane.regions());}
            else for(int p=from;p<to;p++)expected.put(p,value);
            for(int b:bases)for(int j=0;j<12;j++){int p=b+random.nextInt(PAGE*2);equal(expected.getOrDefault(p,0L),lane.get(p));}
            for(int p:edges)equal(expected.getOrDefault(p,0L),lane.get(p));
        }
        blocks=lane.blocks();regions=lane.regions();
        lane.fill(0,MAX,0);equal(blocks,lane.blocks());equal(regions,lane.regions());
        for(int p:expected.keySet())equal(0,lane.get(p));
        for(int p:edges){equal(0,lane.get(p));equal(1,lane.allocated(p)?1:0);}
        bounds(lane);
    }
    private static void bulk() {
        var ints=new M3IntLane28(); var longs=new M3LongLane28();
        int[] ia=new int[PAGE*2];long[] la=new long[ia.length];
        for(int i=0;i<ia.length;i++){ia[i]=i+1;la[i]=((long)i<<34)+i;}
        int start=REGION-PAGE;
        ints.copyFrom(ia,0,start,ia.length);longs.copyFrom(la,0,start,la.length);
        ints.fill(REGION-9,REGION+11,0);longs.fill(REGION-9,REGION+11,0);
        java.util.Arrays.fill(ia,PAGE-9,PAGE+11,0);java.util.Arrays.fill(la,PAGE-9,PAGE+11,0);
        ints.copyFrom(ints,start,start+7,ia.length-7);longs.copyFrom(longs,start,start+7,la.length-7);
        System.arraycopy(ia,0,ia,7,ia.length-7);System.arraycopy(la,0,la,7,la.length-7);
        int[] io=new int[ia.length];long[] lo=new long[la.length];
        ints.copyTo(start,io,0,io.length);longs.copyTo(start,lo,0,lo.length);
        for(int i=0;i<ia.length;i++){equal(ia[i],io[i]);equal(la[i],lo[i]);}
        ints.set(0,Integer.MAX_VALUE);longs.set(0,Long.MAX_VALUE);
        try{ints.incrementAndGet(0);throw new AssertionError("int overflow");}catch(ArithmeticException expected){checks++;}
        try{longs.addAndGet(0,1);throw new AssertionError("long overflow");}catch(ArithmeticException expected){checks++;}
        equal(Integer.MAX_VALUE,ints.get(0));equal(Long.MAX_VALUE,longs.get(0));
    }
    private static void cost() {
        for(String kind:new String[]{"empty","sparse","dense-window"}) {
            var ints=new M3IntLane28();var longs=new M3LongLane28();
            int end=kind.equals("dense-window")?2*REGION:MAX;
            if(kind.equals("sparse")){ints.set(MAX-1,9);longs.set(MAX-1,9);}
            if(kind.equals("dense-window")){ints.fill(0,end,9);longs.fill(0,end,9);}
            for(int i=0;i<1500;i++){ints.fill(0,end,0);longs.fill(0,end,0);}
            long t=System.nanoTime();for(int i=0;i<2000;i++)ints.fill(0,end,0);long it=System.nanoTime()-t;
            t=System.nanoTime();for(int i=0;i<2000;i++)longs.fill(0,end,0);long lt=System.nanoTime()-t;
            sink=ints.get(end-1)+longs.get(end-1)+ints.allocatedBlockCount()+longs.allocatedBlockCount();
            System.out.println("COST case="+kind+" iterations=2000 int_ns="+it+" long_ns="+lt+" sink="+sink);
        }
    }
    public static void main(String[] args) {
        if(args.length>0){cost();return;}
        verify(ints(),4);verify(longs(),8);bulk();
        System.out.println("PASS checks="+checks+" random_operations=3000 full_domain="+MAX+" API=unchanged");
    }
}
