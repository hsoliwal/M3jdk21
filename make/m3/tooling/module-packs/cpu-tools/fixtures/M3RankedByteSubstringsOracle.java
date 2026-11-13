// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.tools.cpu.M3RankedByteSubstrings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

/** Independent brute-force TreeSet oracle; no suffix sorting or LCP helpers are reused. */
public final class M3RankedByteSubstringsOracle {
    private static long checks;
    private static final int[] DOMAIN = {0, 1, 255};
    private static void require(boolean yes, String context) {
        checks++;
        if (!yes) throw new AssertionError(context + " check=" + checks);
    }
    private static int unsignedCompare(byte[] a, byte[] b) {
        for (int i=0;i<Math.min(a.length,b.length);i++) {
            int x = a[i]&255, y=b[i]&255;
            if (x != y) return Integer.compare(x,y);
        }
        return Integer.compare(a.length,b.length);
    }
    private static List<byte[]> distinctBrute(byte[] source) {
        TreeSet<byte[]> distinct = new TreeSet<>(M3RankedByteSubstringsOracle::unsignedCompare);
        for (int i=0;i<source.length;i++) for(int j=i+1;j<=source.length;j++)
            distinct.add(Arrays.copyOfRange(source,i,j));
        return new ArrayList<>(distinct);
    }
    private static void testSmall(byte[] source) {
        byte[] unchanged=source.clone();
        M3RankedByteSubstrings index=M3RankedByteSubstrings.copyOf(source);
        require(index.size()==source.length,"size");
        List<byte[]> ref=distinctBrute(source);
        require(index.distinctCount()==ref.size(),"count");
        require(index.rankOf(new byte[0])==-1,"empty rank absent");
        for(int k=1;k<=ref.size();k++) {
            byte[] value=index.select(k);
            require(Arrays.equals(value,ref.get(k-1)),"k-th lex order k="+k);
            require(index.rankOf(value)==k,"inverse rank k="+k);
            int[] span=index.selectSpan(k);
            require(span.length==2 && span[0]>=0 && span[1]==value.length
                    && span[0]+span[1]<=source.length,"span bound k="+k);
            require(Arrays.equals(Arrays.copyOfRange(source,span[0],span[0]+span[1]),value),"span content");
            require(value!=index.select(k),"detached result");
            if (value.length>0) {
                value[0]^=0x77;
                require(Arrays.equals(index.select(k),ref.get(k-1)),"mutated result isolated");
            }
        }
        long[] ranks=new long[ref.size()];
        for(int i=0;i<ranks.length;i++)ranks[i]=ranks.length-i;
        int[] spans=index.selectSpans(ranks);
        require(spans.length==2*ranks.length,"batch size");
        for(int i=0;i<ranks.length;i++) {
            byte[] actual=Arrays.copyOfRange(source,spans[i*2],spans[i*2]+spans[i*2+1]);
            require(Arrays.equals(actual,ref.get(ref.size()-i-1)),"batch value");
        }
        require(Arrays.equals(source,unchanged),"input untouched");
        require(index.rankOf(new byte[]{(byte)17})==-1 ||
                ref.stream().anyMatch(b->Arrays.equals(b,new byte[]{17})),"absent unless present");
        requireIllegal(() -> index.select(0),"zero rank");
        requireIllegal(() -> index.select(ref.size()+1L),"high rank");
        requireIllegal(() -> index.selectSpans(new long[]{0}),"bad batch");
    }
    private static void requireIllegal(Runnable action,String context) {
        try { action.run(); throw new AssertionError("invalid admitted "+context); }
        catch (IllegalArgumentException expected) { checks++; }
    }
    private static void enumerate(byte[] source,int offset) {
        testSmall(Arrays.copyOf(source,offset));
        if(offset==source.length)return;
        for(int v:DOMAIN) { source[offset]=(byte)v; enumerate(source,offset+1); }
    }
    public static void main(String[] args) {
        enumerate(new byte[7],0);
        Random rnd=new Random(0x4d335f72616e6bL);
        for(int i=0;i<1500;i++) {
            byte[] source=new byte[rnd.nextInt(17)];
            rnd.nextBytes(source);
            testSmall(source);
        }
        // Max-text adversarial run; long rank uses a distinct-count larger than int.
        byte[] run=new byte[131072];
        Arrays.fill(run,(byte)255);
        M3RankedByteSubstrings repetitive=M3RankedByteSubstrings.copyOf(run);
        require(repetitive.distinctCount()==run.length,"repeated byte distinct count");
        require(repetitive.rankOf(new byte[]{(byte)255})==1,"repeated byte lowest rank");
        require(repetitive.select(run.length).length==run.length,"long repeated select");
        byte[] random=new byte[131072];rnd.nextBytes(random);
        M3RankedByteSubstrings high=M3RankedByteSubstrings.copyOf(random);
        require(high.distinctCount()>Integer.MAX_VALUE,"long rank overflow");
        for(long rank:new long[]{1,2,3,1000,2147483648L,high.distinctCount()/2,high.distinctCount()}) {
            require(high.rankOf(high.select(rank))==rank,"long rank inverse "+rank);
        }
        try { M3RankedByteSubstrings.copyOf(null); throw new AssertionError("null admitted"); }
        catch (NullPointerException expected) { checks++; }
        try { M3RankedByteSubstrings.copyOf(new byte[131073]); throw new AssertionError("oversize admitted"); }
        catch (IllegalArgumentException expected) { checks++; }
        System.out.println("M3_RANKED_BYTE_ORACLE_PASS checks="+checks);
    }
}
