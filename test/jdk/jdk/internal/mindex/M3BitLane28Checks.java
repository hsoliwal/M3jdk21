/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/*
 * @test
 * @summary Direct java.base sorted-view and bitmap boundary contracts
 * @compile --patch-module java.base=${test.src} M3BitLane28Checks.java
 * @run main/othervm --patch-module java.base=${test.classes} jdk.internal.mindex.M3BitLane28Checks
 */
package jdk.internal.mindex;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;
import java.util.TreeSet;

/** Loads the existing libjava JNI symbol into the bootstrap loader for source-level tests. */
public final class M3BitLane28Checks {
    private static final int LIMIT = M3Address28.MAX_SLOTS;
    private static final MessageDigest TAPE = digest();
    private static long checks;
    private static volatile long blackhole;
    private M3BitLane28Checks() { }

    public static void main(String[] args) throws Exception {
        // A source-built M3JDK21 resolves this peer from its own libjava.
        // The optional absolute path is for the standalone exact-source JNI proof.
        if (args.length > 0)
            System.load(Path.of(args[0]).toAbsolutePath().toString());
        require(M3BitLane28.class.getClassLoader() == null, "actual bootstrap owner");
        require(M3BitLane28.class.getModule().getName().equals("java.base"), "actual java.base owner");
        if (args.length > 1 && args[1].equals("bench")) { benchmark(); return; }
        for(int pattern=0;pattern<4;pattern++) spectrum(pattern);
        nativeRefusals();
        System.out.println("PASS bitmap checks="+checks+" tape="+HexFormat.of().formatHex(TAPE.digest()));
    }

    private static void spectrum(int pattern) throws Exception {
        M3BitLane28 lane=new M3BitLane28();
        TreeSet<Integer> reference=new TreeSet<>();
        int[] pages={0,4096,(1<<20)-4096,1<<20,(2<<20)+8192,LIMIT-4096};
        Random r=new Random(0x6144L+pattern);
        for(int page:pages) for(int offset=0;offset<4096;offset++) {
            boolean set=switch(pattern) {case 0->false;case 1->true;case 2->(offset&1)==1;default->r.nextBoolean();};
            if(set) {lane.set(page+offset);reference.add(page+offset);}
        }
        for(int bit:new int[]{0,1,62,63,64,65,4094,4095,4096,4097,(1<<20)-1,1<<20,LIMIT-2,LIMIT-1}) {
            lane.set(bit);reference.add(bit);
        }
        int[] expected=reference.stream().mapToInt(Integer::intValue).toArray();
        require(field(lane,"regionCounts")==null,"counts initially lazy");
        reads(lane,expected,r,0);
        require(field(lane,"regionCounts")==null,"range reads do not prepare metadata");
        require(lane.rank(4095)==lower(expected,4095),"prepare exact rank");
        Object totals=field(lane,"regionCounts"), counts=field(lane,"pageCounts");
        require(totals!=null && counts!=null,"metadata prepared");
        reads(lane,expected,r,1);
        require(field(lane,"regionCounts")==totals && field(lane,"pageCounts")==counts,"metadata identity stable");
        for(int round=0;round<5;round++) {
            for(int i=0;i<80;i++) {
                int slot=i%3==0?(17<<20)+r.nextInt(8192):pages[r.nextInt(pages.length)]+r.nextInt(4096);
                switch(r.nextInt(3)) {
                    case 0 -> {lane.set(slot);reference.add(slot);}
                    case 1 -> {lane.clear(slot);reference.remove(slot);}
                    default -> {lane.flip(slot);if(!reference.add(slot))reference.remove(slot);}
                }
                require(lane.get(slot)==reference.contains(slot),"mutated membership");
            }
            expected=reference.stream().mapToInt(Integer::intValue).toArray();
            for(int i=0;i<150;i++) {
                int from=r.nextInt(LIMIT+1),to=r.nextInt(LIMIT+1);
                query(lane,expected,Math.min(from,to),Math.max(from,to));
                int end=r.nextInt(LIMIT+1);
                require(lane.rank(end)==lower(expected,end),"rank after writes");
            }
            require(lane.cardinality()==expected.length,"maintained total");
            require(field(lane,"regionCounts")==totals,"no metadata replacement on writes");
        }
        require(lane.select(-1)==-1 && lane.select(expected.length)==-1,"select out of range");
        for(int ordinal=0;ordinal<expected.length;ordinal+=Math.max(1,expected.length/97))
            require(lane.select(ordinal)==expected[ordinal],"select exact coordinate");
        for(int from:new int[]{0,1,63,64,4095,4096,(1<<20)-1,1<<20,LIMIT-1,LIMIT}) {
            int index=lower(expected,from);
            int next=index==expected.length?-1:expected[index];
            require(lane.nextSetBit(from)==next,"next bit");
            if(from<LIMIT) {
                int upper=lower(expected,from+1);
                require(lane.previousSetBit(from)==(upper==0?-1:expected[upper-1]),"previous bit");
            }
        }
        require(lane.previousSetBit(-1)==-1,"empty previous prefix");
        for(int[] range:new int[][]{{-1,0},{0,-1},{2,1},{0,LIMIT+1},{LIMIT+1,LIMIT+1},
                {Integer.MIN_VALUE,0},{0,Integer.MAX_VALUE}}) {
            expect(IndexOutOfBoundsException.class,()->lane.cardinality(range[0],range[1]));
            expect(IndexOutOfBoundsException.class,()->lane.cardinalityNative(range[0],range[1]));
        }
    }

    private static void reads(M3BitLane28 lane,int[] expected,Random r,int round) throws Exception {
        Object directory=field(lane,"regions");
        long fingerprint=stateHash(lane), bytes=lane.payloadBytes();
        int blocks=lane.allocatedBlockCount(), regions=lane.allocatedRegionCount();
        int[] starts={0,4032,(1<<20)-64,LIMIT-128};
        for(int start:starts) for(int offset=0;offset<64;offset++) for(int length=0;length<=128;length+=1+round) {
            int from=start+offset,to=Math.min(LIMIT,from+length);
            query(lane,expected,from,to);
        }
        for(int i=0;i<700;i++) {
            int page=switch(i%4) {case 0->0;case 1->1<<20;case 2->LIMIT-4096;default->5<<20;};
            int a=r.nextInt(4097),b=r.nextInt(4097);
            query(lane,expected,page+Math.min(a,b),page+Math.max(a,b));
        }
        for(int[] q:new int[][]{{0,LIMIT},{LIMIT,LIMIT},{0,0},{0,4096},{4096,8192},
                {1,4095},{63,65},{1<<20,2<<20},{LIMIT-4096,LIMIT}}) query(lane,expected,q[0],q[1]);
        require(lane.cardinality()==expected.length,"total scalar oracle");
        require(lane.payloadBytes()==bytes && lane.allocatedBlockCount()==blocks
                && lane.allocatedRegionCount()==regions,"no new payload allocation on reads");
        require(field(lane,"regions")==directory && stateHash(lane)==fingerprint,"bitmap and metadata unchanged");
    }

    private static void query(M3BitLane28 lane,int[] expected,int from,int to) {
        int wanted=lower(expected,to)-lower(expected,from);
        int j=lane.cardinality(from,to), n=lane.cardinalityNative(from,to);
        require(j==wanted && n==wanted,"range "+from+":"+to+" expected="+wanted+" java="+j+" native="+n);
        tape(from);tape(to);tape(j);tape(n);
    }
    private static int lower(int[] values,int key) {
        int low=0,high=values.length;
        while(low<high) {int mid=(low+high)>>>1;if(values[mid]<key)low=mid+1;else high=mid;}
        return low;
    }
    private static Object field(M3BitLane28 lane,String name)throws Exception {
        var f=M3BitLane28.class.getDeclaredField(name);f.setAccessible(true);return f.get(lane);
    }
    private static long stateHash(M3BitLane28 lane)throws Exception {
        long hash=1;
        for(long[][] region:(long[][][])field(lane,"regions")) if(region!=null)
            for(long[] page:region) if(page!=null) for(long word:page)hash=31*hash+word;
        int[] totals=(int[])field(lane,"regionCounts");if(totals!=null)for(int n:totals)hash=31*hash+n;
        short[][] counts=(short[][])field(lane,"pageCounts");if(counts!=null)for(short[] region:counts)
            if(region!=null)for(short n:region)hash=31*hash+n;
        return hash;
    }
    private static void nativeRefusals()throws Exception {
        Method method=M3BitLane28.class.getDeclaredMethod("cardinality0",long[][][].class,int.class,int.class);
        method.setAccessible(true);
        long[][][] good=new long[256][][];
        long[][][] badRegion=new long[256][][];badRegion[0]=new long[255][];
        long[][][] badPage=new long[256][][];badPage[0]=new long[256][];badPage[0][0]=new long[63];
        Object[][] cases={{null,0,0},{new long[255][][],0,0},{good,-1,0},{good,2,1},
                {good,0,LIMIT+1},{badRegion,0,1},{badPage,0,1}};
        for(Object[] input:cases) {
            try {method.invoke(null,input);throw new AssertionError("native accepted invalid input");}
            catch(InvocationTargetException e){require(e.getCause() instanceof IllegalArgumentException,"native refusal type");}
        }
    }
    private static void benchmark() {
        M3BitLane28 lane=new M3BitLane28();
        for(int i=0;i<4096;i++)if((i%7)!=0)lane.set(i);
        for(int warm=0;warm<4;warm++){timed(lane,false,50000);timed(lane,true,50000);}
        long[] javaTimes=new long[7],nativeTimes=new long[7];
        for(int r=0;r<7;r++){javaTimes[r]=timed(lane,false,100000);nativeTimes[r]=timed(lane,true,100000);}
        Arrays.sort(javaTimes);Arrays.sort(nativeTimes);
        System.out.println("{\"java_ns_per_query\":"+(javaTimes[3]/100000.0)
                +",\"native_ns_per_query\":"+(nativeTimes[3]/100000.0)
                +",\"payload_bytes\":"+lane.payloadBytes()+",\"blackhole\":"+blackhole+"}");
    }
    private static long timed(M3BitLane28 lane,boolean nativeMode,int n) {
        long sum=0,start=System.nanoTime();
        for(int i=0;i<n;i++) {int from=i&63,to=4096-((i*17)&63);
            sum+=nativeMode?lane.cardinalityNative(from,to):lane.cardinality(from,to);}
        long elapsed=System.nanoTime()-start;blackhole=sum;return elapsed;
    }
    private static void tape(int x){for(int shift=24;shift>=0;shift-=8)TAPE.update((byte)(x>>>shift));}
    private static MessageDigest digest(){try{return MessageDigest.getInstance("SHA-256");}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    private static void require(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void expect(Class<? extends Throwable>type,Runnable r){checks++;try{r.run();}catch(Throwable e){if(type.isInstance(e))return;throw new AssertionError(e);}throw new AssertionError("missing "+type);}
}
