// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.tools.cpu.M3ByteRadixTrie;
import jdk.m3.tools.cpu.M3ByteRadixTrieNative;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Random;

/** Java/JNI exact byte-prefix counts, including query order and detached copies. */
public final class M3ByteRadixTrieNativeOracle {
    private static long checks;
    private M3ByteRadixTrieNativeOracle() { }
    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label + " @" + checks);
    }
    private record Packed(byte[] data, int[] starts, int[] lengths) { }
    private static Packed pack(byte[][] keys) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int[] starts = new int[keys.length];
        int[] lengths = new int[keys.length];
        for (int i=0;i<keys.length;i++) {
            starts[i]=bytes.size();lengths[i]=keys[i].length;bytes.writeBytes(keys[i]);
        }
        return new Packed(bytes.toByteArray(),starts,lengths);
    }
    private static void compare(byte[][] keys, byte[][] queries) {
        Packed p=pack(keys), q=pack(queries);
        M3ByteRadixTrie index=M3ByteRadixTrie.copyOfPacked(p.data,p.starts,p.lengths);
        int[] javaCounts=index.prefixCounts(q.data,q.starts,q.lengths);
        int[] nativeCounts=M3ByteRadixTrieNative.prefixCounts(index,q.data,q.starts,q.lengths);
        check(Arrays.equals(javaCounts,nativeCounts),"native exact prefix parity");
        check(javaCounts != nativeCounts,"detached output");
        if(nativeCounts.length>0) nativeCounts[0]^=31;
        check(Arrays.equals(javaCounts,M3ByteRadixTrieNative.prefixCounts(index,
                q.data,q.starts,q.lengths)),"native owner unchanged");
        check(Arrays.equals(javaCounts,index.prefixCounts(q.data,q.starts,q.lengths)),
                "Java owner unchanged");
    }
    public static void main(String[] args) {
        M3ByteRadixTrie empty=M3ByteRadixTrie.copyOfPacked(new byte[0],new int[0],new int[0]);
        if (!M3ByteRadixTrieNative.isAvailable()) {
            try {
                M3ByteRadixTrieNative.prefixCounts(empty,new byte[0],new int[0],new int[0]);
                throw new AssertionError("disabled JNI admitted");
            } catch (IllegalStateException expected) {checks++;}
            System.out.println("M3_RADIX_NATIVE_DISABLED_PASS checks="+checks);
            return;
        }
        compare(new byte[0][],new byte[][]{new byte[0],new byte[]{-1}});
        compare(new byte[][]{new byte[0],new byte[0],new byte[]{-1},new byte[]{-1,0},
            new byte[]{-1},new byte[]{0,0}},new byte[][] {new byte[0],new byte[]{-1},
            new byte[]{-1,0},new byte[]{0},new byte[]{0,0},new byte[]{0,0,0}});
        Random rng=new Random(0x4d335241444e4154L);
        for (int round=0;round<8500;round++) {
            byte[][] keys=new byte[rng.nextInt(24)][];
            for(int i=0;i<keys.length;i++) {
                keys[i]=new byte[rng.nextInt(13)];
                for(int j=0;j<keys[i].length;j++) keys[i][j]=(byte)rng.nextInt(256);
            }
            byte[][] query=new byte[rng.nextInt(24)][];
            for(int i=0;i<query.length;i++) {
                query[i]=new byte[rng.nextInt(14)];
                rng.nextBytes(query[i]);
                if(keys.length!=0&&rng.nextBoolean()) {
                    byte[] key=keys[rng.nextInt(keys.length)];
                    query[i]=Arrays.copyOf(key,rng.nextInt(key.length+1));
                }
            }
            compare(keys,query);
        }
        // A single long key proves no pointer retention or recursive C traversal.
        byte[] large=new byte[120_000];rng.nextBytes(large);
        compare(new byte[][]{large,Arrays.copyOf(large,12),new byte[0]},
                new byte[][]{new byte[0],Arrays.copyOf(large,12),
                        Arrays.copyOf(large,30_000),large,new byte[]{1}});
        M3ByteRadixTrie sample=M3ByteRadixTrie.copyOfPacked(new byte[]{1,2,3},
                new int[]{0},new int[]{3});
        try {
            M3ByteRadixTrieNative.prefixCounts(sample,new byte[]{1},new int[]{1},new int[]{1});
            throw new AssertionError("invalid query accepted");
        } catch (IllegalArgumentException expected) {checks++;}
        System.out.println("M3_RADIX_NATIVE_PASS checks="+checks);
    }
}
