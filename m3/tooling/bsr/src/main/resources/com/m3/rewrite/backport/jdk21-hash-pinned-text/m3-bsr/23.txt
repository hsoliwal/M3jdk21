// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Bitmap directional scans reuse maintained exact counts without changing semantics
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm --add-opens=java.base/jdk.internal.mindex=ALL-UNNAMED M3BitScanTest
 */
import jdk.internal.mindex.M3BitLane28;
import java.lang.reflect.Field;
import java.util.BitSet;
import java.util.Random;
import java.util.TreeSet;

public final class M3BitScanTest {
    private static final int LIMIT = 1 << 28;
    private static long checks, nativeCalls;
    private static boolean nativeEnabled;

    public static void main(String[] args) throws Exception {
        String library = System.getProperty("bsr.native");
        if (library != null) Class.forName("jdk.internal.mindex.Lane28Loader")
                .getMethod("load", String.class).invoke(null, library);
        nativeEnabled = true;
        if (args.length != 0 && args[0].equals("work")) { work(); return; }
        emptyAndBounds();
        boundaries();
        randomMutations();
        long checksum = hotScans();
        System.out.printf("BSR checks=%d operations=4000 nativeCalls=%d checksum=%d API=unchanged lazyCounts=true stablePages=true%n",
                checks, nativeCalls, checksum);
    }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static Object field(M3BitLane28 lane, String name) throws Exception {
        Field field = M3BitLane28.class.getDeclaredField(name);
        field.setAccessible(true); return field.get(lane);
    }
    private static void rejects(Runnable action) {
        checks++;
        try { action.run(); } catch (IndexOutOfBoundsException expected) { return; }
        throw new AssertionError("bounds must precede empty shortcut");
    }
    private static void emptyAndBounds() throws Exception {
        var lane = new M3BitLane28();
        rejects(() -> lane.nextSetBit(-1));
        rejects(() -> lane.nextSetBit(LIMIT + 1));
        rejects(() -> lane.previousSetBit(-2));
        rejects(() -> lane.previousSetBit(LIMIT));
        check(lane.nextSetBit(LIMIT) == -1, "valid end");
        check(lane.previousSetBit(-1) == -1, "valid empty prefix");
        check(lane.nextSetBit(0) == -1 && lane.previousSetBit(LIMIT - 1) == -1, "empty scans");
        check(lane.allocatedBlockCount() == 0 && field(lane, "regionCounts") == null, "empty scans allocate nothing");
        lane.set(4096); lane.rank(1); lane.clear(4096);
        rejects(() -> lane.nextSetBit(-1)); rejects(() -> lane.previousSetBit(LIMIT));
        Object counts = field(lane, "regionCounts");
        check(lane.nextSetBit(0) == -1 && lane.previousSetBit(LIMIT - 1) == -1, "allocated empty scans");
        check(field(lane, "regionCounts") == counts && lane.allocatedBlockCount() == 1, "no residency change");
    }
    private static void query(M3BitLane28 lane, TreeSet<Integer> oracle, int from) {
        Integer ceiling = oracle.ceiling(from);
        check(lane.nextSetBit(from) == (ceiling == null ? -1 : ceiling), "next exact set oracle");
        int previous = Math.min(from, LIMIT - 1);
        Integer floor = oracle.floor(previous);
        check(lane.previousSetBit(previous) == (floor == null ? -1 : floor), "previous exact set oracle");
    }
    private static void boundaries() throws Exception {
        int[] slots = {0,1,62,63,64,65,127,128,4094,4095,4096,4097,
                (1<<20)-1,1<<20,(1<<20)+1,128<<20,LIMIT-65,LIMIT-64,LIMIT-2,LIMIT-1};
        var lane = new M3BitLane28(); var oracle = new TreeSet<Integer>();
        for (int slot : slots) { lane.set(slot); oracle.add(slot); }
        int blocks = lane.allocatedBlockCount(); Object regions = field(lane,"regions");
        for (int slot : slots) for (int delta=-1;delta<=1;delta++) {
            int from=Math.max(0,Math.min(LIMIT,slot+delta)); query(lane,oracle,from);
        }
        check(field(lane,"regionCounts")==null && field(lane,"pageCounts")==null, "cold scans must not prepare counts");
        check(lane.rank(65)==oracle.headSet(65).size(), "prepare rank");
        Object totals=field(lane,"regionCounts"), pages=field(lane,"pageCounts");
        for(int slot:slots) { lane.clear(slot);oracle.remove(slot);query(lane,oracle,slot); }
        for(int slot:slots) { lane.set(slot);oracle.add(slot);query(lane,oracle,slot); }
        lane.set(32<<20);oracle.add(32<<20);query(lane,oracle,32<<20);
        lane.set((1<<20)+8192);oracle.add((1<<20)+8192);query(lane,oracle,(1<<20)+8192);
        check(field(lane,"regionCounts")==totals && field(lane,"pageCounts")==pages, "same maintained summaries");
        check(field(lane,"regions")==regions && lane.allocatedBlockCount()==blocks+2, "stable directories and pages");
        int i=0;for(int slot:oracle)check(lane.select(i++)==slot,"select retained");
        if(nativeEnabled) {check(lane.cardinalityNative(0,LIMIT)==oracle.size(),"native full parity");nativeCalls++;}
    }
    private static void randomMutations() throws Exception {
        var lane=new M3BitLane28();var set=new TreeSet<Integer>();var bits=new BitSet();
        var random=new Random(0x425352L);int domain=3<<20;
        for(int operation=0;operation<4000;operation++) {
            int slot=random.nextInt(domain);int choice=random.nextInt(4);
            if(choice==0) {lane.clear(slot);set.remove(slot);bits.clear(slot);}
            else if(choice==1) {lane.flip(slot);if(!set.remove(slot))set.add(slot);bits.flip(slot);}
            else {lane.set(slot);set.add(slot);bits.set(slot);}
            if(operation==50) check(lane.rank(domain)==set.size(),"prepare random summaries");
            int from=random.nextInt(domain+1);query(lane,set,from);
            check(lane.nextSetBit(from)==bits.nextSetBit(from),"BitSet next parity");
            check(lane.previousSetBit(from)==bits.previousSetBit(from),"BitSet previous parity");
            check(lane.cardinality()==set.size(),"maintained cardinality");
            if((operation&31)==0) {
                int end=Math.min(domain,from+8193), expected=set.subSet(from,end).size();
                check(lane.cardinality(from,end)==expected,"range cardinality retained");
                check(lane.rank(from)==set.headSet(from).size(),"rank retained after mutation");
                if(nativeEnabled) {check(lane.cardinalityNative(from,end)==expected,"native range parity");nativeCalls++;}
            }
        }
        Object counts=field(lane,"regionCounts");int blocks=lane.allocatedBlockCount();
        for(int slot:set)lane.clear(slot);set.clear();
        query(lane,set,0);query(lane,set,LIMIT);
        for(int slot:new int[]{0,64,4096,1<<20,LIMIT-1}) {lane.set(slot);set.add(slot);query(lane,set,slot);}
        check(field(lane,"regionCounts")==counts,"cleared summaries remain reusable");
        check(lane.allocatedBlockCount()>=blocks,"clearing retains allocated pages");
    }
    private static long hotScans() {
        var lane=new M3BitLane28();lane.set(0);lane.set(8191);lane.rank(1);
        long checksum=0;
        for(int i=0;i<12000;i++) {
            int q=i&8191;checksum+=lane.nextSetBit(q);checksum+=lane.previousSetBit(q);
            if((i&255)==0){lane.set(4096);lane.clear(4096);}
        }
        check(checksum>0,"hot scan results");return checksum;
    }
    private static long counter(String name) throws Exception {
        return M3BitLane28.class.getField(name).getLong(null);
    }
    private static void resetCounters() throws Exception {
        for(String name:new String[]{"visits","wordReads"})M3BitLane28.class.getField(name).setLong(null,0);
    }
    private static void work() throws Exception {
        var lane=new M3BitLane28();
        for(int r=0;r<32;r++)for(int b=0;b<16;b++){int slot=(r<<20)|(b<<12);lane.set(slot);lane.clear(slot);}
        resetCounters();check(lane.nextSetBit(0)==-1 && lane.previousSetBit(LIMIT-1)==-1,"empty work answers");
        System.out.printf("empty visits=%d wordReads=%d%n",counter("visits"),counter("wordReads"));
        lane.set(0);lane.set(LIMIT-1);
        resetCounters();check(lane.nextSetBit(1)==LIMIT-1 && lane.previousSetBit(LIMIT-2)==0,"cold work answers");
        System.out.printf("cold visits=%d wordReads=%d%n",counter("visits"),counter("wordReads"));
        lane.rank(1); // Existing cold preparation is explicit, outside the measured warm scans.
        resetCounters();check(lane.nextSetBit(1)==LIMIT-1 && lane.previousSetBit(LIMIT-2)==0,"warm work answers");
        System.out.printf("warm visits=%d wordReads=%d%n",counter("visits"),counter("wordReads"));
    }
}
