// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.tools.cpu.M3RunLengthPostingIndex;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/** Independent scalar/TreeMap oracle; no production binary search is reused. */
public final class M3RunLengthPostingIndexOracle {
    private static long checks;
    private static void require(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason + " check=" + checks);
    }
    private static void refused(Class<? extends Throwable> type, Runnable call, String caseName) {
        checks++;
        try { call.run(); }
        catch (Throwable actual) {
            if (type.isInstance(actual)) return;
            throw new AssertionError(caseName + " wrong exception", actual);
        }
        throw new AssertionError("missing refusal " + caseName);
    }

    private static void verify(int[] raw, Random rand) {
        int[] saved = raw.clone();
        M3RunLengthPostingIndex index = M3RunLengthPostingIndex.fromSorted(raw);
        require(index.size() == raw.length, "size");
        require(Arrays.equals(raw, index.toArray()), "reconstruction");
        int[] exported = index.toArray();
        if (exported.length != 0) exported[0] ^= 0x3175;
        require(Arrays.equals(raw, index.toArray()), "reconstructed output detached");
        TreeMap<Integer, Integer> counts = new TreeMap<>();
        for (int value : saved) counts.merge(value, 1, Integer::sum);
        require(index.runCount() == counts.size(), "distinct runs");
        int[] unique = index.distinctValues();
        int[] ends = index.runEndsExclusive();
        require(unique.length == ends.length, "metadata length");
        int[] multiplicities = new int[ends.length];
        int cumulative = 0;
        int at = 0;
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            require(unique[at] == entry.getKey(), "unique value order");
            cumulative += entry.getValue();
            require(ends[at] == cumulative, "cumulative ends");
            multiplicities[at] = entry.getValue();
            at++;
        }
        require(cumulative == raw.length, "cumulative total");
        M3RunLengthPostingIndex direct = M3RunLengthPostingIndex.fromRuns(unique, multiplicities);
        require(Arrays.equals(saved, direct.toArray()), "fromRuns exact reconstruction");
        require(Arrays.equals(ends, direct.runEndsExclusive()), "fromRuns exact index");
        if (unique.length != 0) unique[0] ^= 0x7812;
        if (ends.length != 0) ends[0] ^= 0x7812;
        if (multiplicities.length != 0) multiplicities[0] ^= 0x7812;
        require(Arrays.equals(saved, index.toArray()) && Arrays.equals(saved, direct.toArray()),
                "detached input and projections");
        for (int value : new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE+1, -17, -1, 0, 1,
                17, Integer.MAX_VALUE-1, Integer.MAX_VALUE, rand.nextInt()}) {
            int less=0, lessOrEqual=0;
            for (int element : saved) {
                if (element<value) less++;
                if (element<=value) lessOrEqual++;
            }
            require(index.lowerBound(value)==less, "lowerBound");
            require(index.upperBound(value)==lessOrEqual, "upperBound");
            require(index.countOf(value)==counts.getOrDefault(value,0), "countOf");
            int begin= saved.length==0 ? 0 : rand.nextInt(saved.length+1);
            int end=begin+(saved.length==begin ? 0:rand.nextInt(saved.length-begin+1));
            int expected=0;
            for (int i=begin;i<end;i++) if (saved[i]==value) expected++;
            require(index.countInPositions(value,begin,end)==expected, "position window");
        }
        for (int k=0;k<12;k++) {
            int a=rand.nextInt(), b=rand.nextInt();
            int low=Math.min(a,b), high=Math.max(a,b);
            int expected=0;
            for (int value:saved) if (value>=low && value<high) expected++;
            require(index.rangeCount(low,high)==expected, "value range");
        }
        if (saved.length>0) {
            for (int k=0;k<Math.min(20,saved.length);k++) {
                int pos=rand.nextInt(saved.length);
                require(index.select(pos)==saved[pos], "select");
            }
        }
        require(Arrays.equals(saved,raw), "no input mutation");
    }

    public static void main(String[] args) {
        Random rand=new Random(0x1971cbd3a6L);
        verify(new int[0],rand);
        verify(new int[]{Integer.MIN_VALUE,Integer.MIN_VALUE,-1,0,0,0,Integer.MAX_VALUE},rand);
        verify(new int[]{Integer.MIN_VALUE,Integer.MAX_VALUE},rand);
        for(int trial=0;trial<12000;trial++) {
            int length=rand.nextInt(300);
            int[] input=new int[length];
            for(int i=0;i<length;i++) {
                int bucket=rand.nextInt(24);
                input[i]=bucket==0?Integer.MIN_VALUE:bucket==1?Integer.MAX_VALUE
                        :bucket<16?rand.nextInt(13)-6:rand.nextInt();
            }
            Arrays.sort(input);
            verify(input,rand);
        }
        int[] allDuplicate = new int[M3RunLengthPostingIndex.MAX_ITEMS];
        Arrays.fill(allDuplicate, Integer.MAX_VALUE);
        M3RunLengthPostingIndex dense=M3RunLengthPostingIndex.fromSorted(allDuplicate);
        require(dense.size()==M3RunLengthPostingIndex.MAX_ITEMS,"max total");
        require(dense.runCount()==1,"max repeated run");
        require(dense.countOf(Integer.MAX_VALUE)==M3RunLengthPostingIndex.MAX_ITEMS,"max count");
        require(dense.select(0)==Integer.MAX_VALUE && dense.select(dense.size()-1)==Integer.MAX_VALUE,
                "large select");
        int[] distinct=new int[M3RunLengthPostingIndex.MAX_ITEMS];
        for(int i=0;i<distinct.length;i++) distinct[i]=i-1_000_000;
        M3RunLengthPostingIndex wide=M3RunLengthPostingIndex.fromSorted(distinct);
        require(wide.runCount()==wide.size(),"all unique");
        require(wide.select(1_000_000)==0,"max select");
        require(wide.lowerBound(17)==1_000_017,"large lower");
        require(wide.upperBound(17)==1_000_018,"large upper");
        refused(NullPointerException.class,()->M3RunLengthPostingIndex.fromSorted(null),"null sorted");
        refused(NullPointerException.class,()->M3RunLengthPostingIndex.fromRuns(null,new int[0]),"null unique");
        refused(NullPointerException.class,()->M3RunLengthPostingIndex.fromRuns(new int[0],null),"null counts");
        refused(IllegalArgumentException.class,()->M3RunLengthPostingIndex.fromSorted(new int[]{0,-1}),"unsorted");
        refused(IllegalArgumentException.class,()->M3RunLengthPostingIndex.fromSorted(new int[2_000_001]),"oversized");
        refused(IllegalArgumentException.class,()->M3RunLengthPostingIndex.fromRuns(new int[]{1,2},new int[]{1}),"lane mismatch");
        refused(IllegalArgumentException.class,()->M3RunLengthPostingIndex.fromRuns(new int[]{2,1},new int[]{1,1}),"run inversion");
        refused(IllegalArgumentException.class,()->M3RunLengthPostingIndex.fromRuns(new int[]{1,1},new int[]{1,1}),"duplicate run key");
        refused(IllegalArgumentException.class,()->M3RunLengthPostingIndex.fromRuns(new int[]{1},new int[]{0}),"empty run");
        refused(IllegalArgumentException.class,()->M3RunLengthPostingIndex.fromRuns(new int[]{1,2},new int[]{2_000_000,1}),"sum overflow");
        refused(IndexOutOfBoundsException.class,()->wide.select(-1),"negative select");
        refused(IndexOutOfBoundsException.class,()->wide.select(wide.size()),"past-end select");
        refused(IndexOutOfBoundsException.class,()->wide.countInPositions(0,-1,0),"negative window");
        refused(IndexOutOfBoundsException.class,()->wide.countInPositions(0,10,5),"inverse window");
        refused(IndexOutOfBoundsException.class,()->wide.countInPositions(0,0,wide.size()+1),"oversize window");
        refused(IllegalArgumentException.class,()->wide.rangeCount(10,-10),"inverse value range");
        System.out.println("M3_RUNLENGTH_POSTINGS_JAVA_PASS checks="+checks);
    }
}
