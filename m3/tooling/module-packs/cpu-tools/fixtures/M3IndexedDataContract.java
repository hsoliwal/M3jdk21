// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
import com.m3.tools.cpu.*;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/** Independent JDK oracles for primitive temporal, codec, quality, spatial, CRDT, and receipt atoms. */
public final class M3IndexedDataContract {
    private M3IndexedDataContract() { }
    private static int checks;
    private static final Random RANDOM = new Random(0x4d335352434f4445L);
    private static void ok(boolean expression) {
        if (!expression) throw new AssertionError("M3IndexedDataContract case=" + checks);
        checks++;
    }
    private static void reject(Runnable run) {
        try { run.run(); throw new AssertionError("missing refusal"); }
        catch (IllegalArgumentException | IllegalStateException expected) { checks++; }
    }
    public static void main(String[] args) throws Exception {
        temporal(); quality(); codec(); spatial(); provenance(); convergence(); schema();
        System.out.println("M3_INDEXED_DATA_PASS checks=" + checks);
    }
    private static void temporal() {
        for (int t = 0; t < 2400; t++) {
            int n = RANDOM.nextInt(80);
            long[] timestamp = new long[n], values = new long[n];
            long now = RANDOM.nextInt(101) - 50;
            for (int i = 0; i < n; i++) {
                now += RANDOM.nextInt(4);
                timestamp[i] = now; values[i] = RANDOM.nextInt(31) - 15;
            }
            long origin = RANDOM.nextInt(101) - 50, width = RANDOM.nextInt(9) + 1;
            M3TemporalIndex.Windows result = M3TemporalIndex.tumble(timestamp, values, origin, width);
            TreeMap<Long, long[]> expected = new TreeMap<>();
            for (int i = 0; i < n; i++) {
                long key = Math.floorDiv(timestamp[i] - origin, width);
                long[] window = expected.computeIfAbsent(key, ignore -> new long[2]);
                window[0]++; window[1] += values[i];
            }
            ok(result.bucket().length == expected.size());
            int at = 0;
            for (var e : expected.entrySet()) {
                ok(result.bucket()[at] == e.getKey());
                ok(result.count()[at] == e.getValue()[0]);
                ok(result.sum()[at] == e.getValue()[1]);
                at++;
            }
            long[] starts = new long[n], ends = new long[n];
            for (int i = 0; i < n; i++) {
                starts[i] = RANDOM.nextInt(30) - 15;
                ends[i] = starts[i] + RANDOM.nextInt(6);
            }
            long[] got = M3TemporalIndex.overlapMask(starts, ends);
            for (int i = 0; i < n; i++) {
                boolean hit = false;
                for (int j = 0; j < n; j++) if (i != j && starts[i] < ends[i] && starts[j] < ends[j]) {
                    hit |= starts[i] < ends[j] && starts[j] < ends[i];
                }
                ok(((got[i >>> 6] >>> (i & 63)) & 1) == (hit ? 1L : 0L));
            }
        }
        reject(() -> M3TemporalIndex.tumble(new long[]{2,1},new long[]{2,1},0,1));
        reject(() -> M3TemporalIndex.overlapMask(new long[]{3},new long[]{2}));
    }
    private static void quality() {
        for (int t = 0; t < 3000; t++) {
            int n = RANDOM.nextInt(140);
            int[] values = new int[n]; Map<Integer, Integer> frequency = new HashMap<>();
            for (int i = 0; i < n; i++) {
                int key = RANDOM.nextInt(60) - 30;
                values[i] = key;
                frequency.merge(key, 1, Integer::sum);
            }
            long[] mask = M3DataQuality.duplicates(values);
            for (int i = 0; i < n; i++)
                ok(((mask[i >>> 6] >>> (i & 63)) & 1) == (frequency.get(values[i]) > 1 ? 1L : 0L));
            int[] keys = new int[RANDOM.nextInt(30)];
            for (int i = 0; i < keys.length; i++) keys[i] = i * 3 - 40;
            long[] missing = M3DataQuality.missingReferences(values, keys);
            for (int i = 0; i < values.length; i++)
                ok(((missing[i >>> 6] >>> (i & 63)) & 1) == (Arrays.binarySearch(keys, values[i]) < 0 ? 1L : 0L));
            int rows = RANDOM.nextInt(20), cols = RANDOM.nextInt(12);
            int slots = rows * cols;
            long[] missingCells = new long[(slots + 63) >>> 6];
            long[] expected = new long[cols];
            for (int at = 0; at < slots; at++) if (RANDOM.nextBoolean()) {
                missingCells[at >>> 6] |= 1L << (at & 63);
                expected[at % cols]++;
            }
            ok(Arrays.equals(expected,M3DataQuality.missingCounts(missingCells,rows,cols)));
        }
        reject(() -> M3DataQuality.missingReferences(new int[0], new int[]{3,2}));
        reject(() -> M3DataQuality.missingCounts(new long[]{-1},1,1));
    }
    private static void codec() {
        for (int t = 0; t < 2400; t++) {
            byte[] data = new byte[RANDOM.nextInt(500)];
            for (int i = 0; i < data.length; i++) data[i] = (byte)(i > 0 && RANDOM.nextInt(4) != 0
                    ? data[i - 1] : RANDOM.nextInt(256));
            byte[] encoded = M3CompactCodec.rle(data);
            ok(Arrays.equals(data, M3CompactCodec.unrle(encoded,data.length)));
            ok((encoded.length & 1) == 0);
            if (data.length > 0) reject(() -> M3CompactCodec.unrle(encoded,data.length - 1));
            for (int i = 0; i < encoded.length; i+=2) ok((encoded[i] & 255) > 0);
        }
        reject(() -> M3CompactCodec.unrle(new byte[]{0,2}, 3));
        reject(() -> M3CompactCodec.unrle(new byte[]{1}, 3));
        reject(() -> M3CompactCodec.unrle(new byte[]{1,8,1,8}, 3));
    }
    private static void spatial() {
        for (int t = 0; t < 2400; t++) {
            int n = RANDOM.nextInt(120);
            int[] x = new int[n], y = new int[n];
            for (int i = 0; i < n; i++) { x[i] = RANDOM.nextInt(2001) - 1000; y[i] = RANDOM.nextInt(2001) - 1000; }
            int qx = RANDOM.nextInt(2001) - 1000, qy = RANDOM.nextInt(2001) - 1000;
            int nearest = M3SpatialQueries.nearest(x,y,qx,qy), expected = -1;
            long least = Long.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                long dx = x[i] - (long) qx, dy = y[i] - (long) qy, d = dx*dx+dy*dy;
                if (d < least) { least = d; expected = i; }
            }
            ok(nearest == expected);
            int minX = -500, maxX=500,minY=-250,maxY=250;
            long[] included = M3SpatialQueries.rectangle(x,y,minX,minY,maxX,maxY);
            for (int i = 0; i < n; i++) ok(((included[i >>> 6] >>> (i & 63)) & 1) ==
                    (x[i] >= minX && x[i] <= maxX && y[i] >= minY && y[i] <= maxY ? 1L : 0L));
        }
        reject(() -> M3SpatialQueries.nearest(new int[]{2},new int[]{},1,1));
        reject(() -> M3SpatialQueries.rectangle(new int[]{0},new int[]{0},2,1,1,3));
    }
    private static void provenance() throws Exception {
        for (int t = 0; t < 2200; t++) {
            int n = RANDOM.nextInt(25);
            int[] offsets = new int[n + 1];
            for (int i = 0; i < n; i++) offsets[i + 1] = offsets[i] + RANDOM.nextInt(12);
            byte[] bytes = new byte[offsets[n]]; RANDOM.nextBytes(bytes);
            M3ProvenanceLedger.Chain chain = M3ProvenanceLedger.chain(bytes, offsets);
            byte[] previous = new byte[32];
            for (int i = 0; i < n; i++) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update("M3-PROVENANCE-V1".getBytes(StandardCharsets.US_ASCII));
                digest.update(previous);
                int width = offsets[i+1]-offsets[i];
                digest.update(new byte[]{(byte)(width >>> 24),(byte)(width >>> 16),(byte)(width >>> 8),(byte)width});
                digest.update(bytes,offsets[i],width);
                previous = digest.digest();
                ok(Arrays.equals(previous, Arrays.copyOfRange(chain.entries(),i*32,(i+1)*32)));
            }
            ok(Arrays.equals(previous,chain.root()));
            ok(M3ProvenanceLedger.verify(bytes,offsets,chain.root()));
            int prefix = RANDOM.nextInt(n + 1);
            byte[] prefixReference = prefix == 0 ? new byte[32] : Arrays.copyOfRange(chain.entries(),(prefix-1)*32,prefix*32);
            ok(Arrays.equals(prefixReference,M3ProvenanceLedger.prefixRoot(bytes,offsets,prefix)));
            if (bytes.length != 0) {
                byte[] changed = bytes.clone(); changed[RANDOM.nextInt(changed.length)] ^= 1;
                ok(!M3ProvenanceLedger.verify(changed,offsets,chain.root()));
            }
        }
        reject(() -> M3ProvenanceLedger.chain(new byte[]{1},new int[]{0,2}));
        reject(() -> M3ProvenanceLedger.verify(new byte[0],new int[]{0},new byte[30]));
    }
    private static void convergence() {
        for (int t = 0; t < 3000; t++) {
            int n = RANDOM.nextInt(20);
            long[] a = new long[n], b = new long[n], c = new long[n];
            for (int i=0;i<n;i++){a[i]=RANDOM.nextInt(100);b[i]=RANDOM.nextInt(100);c[i]=RANDOM.nextInt(100);}
            ok(Arrays.equals(M3CounterConvergence.merge(a,b),M3CounterConvergence.merge(b,a)));
            ok(Arrays.equals(M3CounterConvergence.merge(a,a),a));
            ok(Arrays.equals(M3CounterConvergence.merge(M3CounterConvergence.merge(a,b),c),
                    M3CounterConvergence.merge(a,M3CounterConvergence.merge(b,c))));
            ok(!M3CounterConvergence.happensBefore(a,a));
            ok(M3CounterConvergence.concurrent(a,b) == (!M3CounterConvergence.happensBefore(a,b)
                    && !M3CounterConvergence.happensBefore(b,a) && !Arrays.equals(a,b)));
            if(n>0){long[] next=M3CounterConvergence.increment(a,0,1);ok(next[0]==a[0]+1);}
        }
        reject(() -> M3CounterConvergence.merge(new long[]{-1},new long[]{1}));
        reject(() -> M3CounterConvergence.increment(new long[]{0},0,-1));
    }
    private static void schema() {
        ok(M3SchemaCompatibility.incompatible(
                new int[]{1,3},new byte[]{M3SchemaCompatibility.INT32,M3SchemaCompatibility.TEXT},
                new int[]{1,2,3},new byte[]{M3SchemaCompatibility.INT64,M3SchemaCompatibility.TEXT,M3SchemaCompatibility.TEXT},
                new long[]{2})[0] == 2);
        ok(M3SchemaCompatibility.incompatible(new int[]{1},new byte[]{M3SchemaCompatibility.INT64},
                new int[]{1},new byte[]{M3SchemaCompatibility.INT32},new long[]{1})[0] == 1);
        ok(M3SchemaCompatibility.incompatible(new int[]{1},new byte[]{M3SchemaCompatibility.FLOAT32},
                new int[]{1},new byte[]{M3SchemaCompatibility.FLOAT64},new long[]{1})[0] == 0);
        reject(() -> M3SchemaCompatibility.incompatible(new int[]{3,2},new byte[]{1,1},
                new int[0],new byte[0],new long[0]));
        reject(() -> M3SchemaCompatibility.incompatible(new int[0],new byte[0],
                new int[]{0},new byte[]{1},new long[]{2}));
    }
}
