// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.tools.cpu.M3ByteRadixTrie;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/** Independently enumerated binary-prefix counts and full-key match lengths. */
public final class M3ByteRadixTrieOracle {
    private static long checks;
    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private M3ByteRadixTrieOracle() { }
    private static void check(boolean truth, String description) {
        checks++;
        if (!truth) throw new AssertionError(description + " @" + checks);
    }
    private static String identity(byte[] value) { return ENCODER.encodeToString(value); }
    private static int count(byte[][] keys, byte[] query, boolean exact) {
        int matched = 0;
        for (byte[] key : keys) {
            if (exact ? Arrays.equals(key, query) : begins(key, query)) matched++;
        }
        return matched;
    }
    private static boolean begins(byte[] key, byte[] prefix) {
        if (prefix.length > key.length) return false;
        for (int i = 0; i < prefix.length; i++) if (key[i] != prefix[i]) return false;
        return true;
    }
    private static int longest(byte[][] keys, byte[] query) {
        int longest = -1;
        for (byte[] key : keys) {
            if (begins(query, key)) longest = Math.max(longest, key.length);
        }
        return longest;
    }
    private record Packed(byte[] data, int[] offsets, int[] lengths) { }
    private static Packed pack(byte[][] keys) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        int[] positions = new int[keys.length];
        int[] lengths = new int[keys.length];
        for (int i = 0; i < keys.length; i++) {
            positions[i] = data.size();
            lengths[i] = keys[i].length;
            data.writeBytes(keys[i]);
        }
        return new Packed(data.toByteArray(), positions, lengths);
    }
    private static void verify(byte[][] keys, byte[][] queries) {
        Packed packed = pack(keys);
        M3ByteRadixTrie index = M3ByteRadixTrie.copyOfPacked(packed.data, packed.offsets, packed.lengths);
        Set<String> unique = new HashSet<>();
        Set<String> prefixes = new HashSet<>();
        prefixes.add("");
        for (byte[] key : keys) {
            unique.add(identity(key));
            for (int n = 0; n <= key.length; n++)
                prefixes.add(identity(Arrays.copyOf(key, n)));
        }
        check(index.size() == keys.length, "key multiplicity");
        check(index.distinctKeyCount() == unique.size(), "distinct key count");
        check(index.nodeCount() == prefixes.size(), "unique prefix node count");
        check(index.edgeCount() == prefixes.size() - 1, "tree edge count");
        Packed queryBatch = pack(queries);
        int[] bulk = index.prefixCounts(queryBatch.data, queryBatch.offsets, queryBatch.lengths);
        check(bulk.length == queries.length, "batch length");
        for (int i = 0; i < queries.length; i++) {
            byte[] query = queries[i];
            int pref = count(keys, query, false);
            check(index.countPrefix(query) == pref, "prefix count " + i);
            check(index.countExact(query) == count(keys, query, true), "exact count " + i);
            check(index.containsPrefix(query) == (pref != 0), "presence " + i);
            check(index.longestStoredPrefixLength(query) == longest(keys, query), "stored-prefix length " + i);
            check(bulk[i] == pref, "packed batch " + i);
        }
        int[] detached = index.prefixCounts(queryBatch.data, queryBatch.offsets, queryBatch.lengths);
        check(detached != bulk && Arrays.equals(detached, bulk), "detached bulk output");
        if (bulk.length > 0) {
            bulk[0] ^= 0x54;
            check(Arrays.equals(detached, index.prefixCounts(queryBatch.data,
                    queryBatch.offsets, queryBatch.lengths)), "output mutation isolation");
        }
        // Mutating the caller's source after construction cannot mutate the stored trie.
        if (packed.data.length != 0) packed.data[0] ^= 0x7f;
        if (packed.offsets.length != 0) packed.offsets[0] ^= 1;
        if (packed.lengths.length != 0) packed.lengths[0] ^= 1;
        for (byte[] query : queries) check(index.countPrefix(query) == count(keys, query, false),
                "source snapshot isolation");
    }
    private static void refused(Runnable action, String description) {
        try { action.run(); throw new AssertionError("accepted " + description); }
        catch (IllegalArgumentException | NullPointerException expected) { checks++; }
    }
    public static void main(String[] args) {
        verify(new byte[0][], new byte[][] {new byte[0],new byte[]{0}});
        verify(new byte[][] {new byte[0],new byte[0],new byte[]{-1},new byte[]{-1},
                new byte[]{-1,0}, new byte[]{0},new byte[]{0,0}},
                new byte[][] {new byte[0],new byte[]{-1},new byte[]{-1,0},new byte[]{-1,1},
                        new byte[]{0},new byte[]{0,0},new byte[]{0,0,0},new byte[]{1}});
        Random rng = new Random(0x4d335241444958L);
        for (int round = 0; round < 8000; round++) {
            byte[][] keys = new byte[rng.nextInt(21)][];
            for (int i = 0; i < keys.length; i++) {
                int len = rng.nextInt(9);
                keys[i] = new byte[len];
                for (int j = 0; j < len; j++) {
                    int symbol = rng.nextInt(12);
                    keys[i][j] = (byte)(symbol == 0 ? 255 : symbol == 1 ? 128 : symbol % 5);
                }
            }
            byte[][] queries = new byte[rng.nextInt(20)][];
            for (int i = 0; i < queries.length; i++) {
                byte[] query = new byte[rng.nextInt(10)];
                for (int j = 0; j < query.length; j++) query[j] = (byte)rng.nextInt(8);
                if (keys.length != 0 && rng.nextBoolean()) {
                    byte[] key = keys[rng.nextInt(keys.length)];
                    query = Arrays.copyOf(key, rng.nextInt(key.length + 1));
                }
                queries[i] = query;
            }
            verify(keys, queries);
        }
        // Single long key validates non-recursive construction and frozen CSR lanes.
        byte[] longKey = new byte[120_000];
        rng.nextBytes(longKey);
        M3ByteRadixTrie longIndex = M3ByteRadixTrie.copyOfPacked(longKey,new int[]{0},
                new int[]{longKey.length});
        check(longIndex.nodeCount() == longKey.length + 1, "long chain nodes");
        check(longIndex.edgeCount() == longKey.length, "long chain edges");
        check(longIndex.countPrefix(Arrays.copyOf(longKey,90_000)) == 1, "long prefix");
        check(longIndex.countExact(longKey) == 1, "long key");
        check(longIndex.longestStoredPrefixLength(longKey) == longKey.length, "long full key");
        byte[] mutated = longKey.clone(); mutated[longKey.length-1] ^= 1;
        check(longIndex.countExact(mutated) == 0, "long key mutation");
        // 256 outgoing byte symbols and unsigned edge sorting.
        byte[] all = new byte[256];
        int[] starts = new int[256];int[] sizes = new int[256];
        for(int i=0;i<256;i++){all[i]=(byte)i;starts[i]=i;sizes[i]=1;}
        M3ByteRadixTrie full = M3ByteRadixTrie.copyOfPacked(all,starts,sizes);
        check(full.edgeCount() == 256, "full byte alphabet");
        for (int i=0;i<256;i++) check(full.countExact(new byte[]{(byte)i}) == 1,
                "unsigned byte edge " + i);
        M3ByteRadixTrie overlap = M3ByteRadixTrie.copyOfPacked(new byte[]{1,2,3},
                new int[]{0,0,1,3},new int[]{3,2,2,0});
        check(overlap.countPrefix(new byte[]{2})==1, "overlapping slices");
        check(overlap.countExact(new byte[0])==1, "empty key snapshot");
        refused(()->M3ByteRadixTrie.copyOfPacked(null,new int[0],new int[0]),"null packed");
        refused(()->M3ByteRadixTrie.copyOfPacked(new byte[0],null,new int[0]),"null starts");
        refused(()->M3ByteRadixTrie.copyOfPacked(new byte[0],new int[0],null),"null lengths");
        refused(()->M3ByteRadixTrie.copyOfPacked(new byte[0],new int[]{0},new int[0]),"count mismatch");
        refused(()->M3ByteRadixTrie.copyOfPacked(new byte[3],new int[]{2},new int[]{2}),"slice overrun");
        refused(()->M3ByteRadixTrie.copyOfPacked(new byte[3],new int[]{-1},new int[]{1}),"negative start");
        refused(()->M3ByteRadixTrie.copyOfPacked(new byte[3],new int[]{0},new int[]{-1}),"negative length");
        refused(()->M3ByteRadixTrie.copyOfPacked(new byte[2_000_001],new int[0],new int[0]),"bytes budget");
        refused(()->M3ByteRadixTrie.copyOfPacked(new byte[0],new int[1_000_001],new int[1_000_001]),"keys budget");
        refused(()->M3ByteRadixTrie.copyOfPacked(new byte[1_000_001],new int[]{0,0},
                new int[]{1_000_001,1_000_001}),"sum of slices");
        refused(()->full.countExact(null),"null exact query");
        refused(()->full.countPrefix(null),"null prefix query");
        refused(()->full.longestStoredPrefixLength(null),"null longest query");
        refused(()->full.prefixCounts(new byte[0],new int[]{0},new int[0]),"batch lengths");
        System.out.println("M3_RADIX_JAVA_PASS checks="+checks);
    }
}
