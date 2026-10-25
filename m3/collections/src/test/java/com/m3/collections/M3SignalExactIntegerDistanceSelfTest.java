/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;

/** Independent arbitrary-precision reference against full-width primitive metric code. */
public final class M3SignalExactIntegerDistanceSelfTest {
    private static final String SOURCE = "5".repeat(64);
    private static long checks;
    private M3SignalExactIntegerDistanceSelfTest() { }

    private static void check(boolean yes, String reason) {
        checks++;
        if (!yes) throw new AssertionError(reason + " check=" + checks);
    }
    private static void refuse(Class<? extends Throwable> type, Runnable operation) {
        checks++;
        try { operation.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("wrong exception instead of " + type.getSimpleName(), error);
        }
        throw new AssertionError("missing " + type.getSimpleName());
    }
    private static long[] masks(boolean[] present) {
        long[] bits = new long[(present.length + 63) >>> 6];
        for (int i = 0; i < present.length; i++) {
            if (present[i]) bits[i >>> 6] |= 1L << (i & 63);
        }
        return bits;
    }
    private static M3SignalQualitySchema schema(long[][] longs, int[][] ints,
            boolean[][] present, M3SignalQualitySchema.Reliability[] reliability) {
        int dimensionCount = longs.length + ints.length;
        int rows = dimensionCount == 0 ? 0 :
                longs.length != 0 ? longs[0].length : ints[0].length;
        M3SignalColumns.Column[] columns = new M3SignalColumns.Column[dimensionCount];
        M3SignalQualitySchema.Spec[] specs = new M3SignalQualitySchema.Spec[dimensionCount];
        for (int d = 0; d < dimensionCount; d++) {
            boolean isLong = d < longs.length;
            columns[d] = new M3SignalColumns.Column(
                    isLong ? M3SignalColumns.Kind.LONG : M3SignalColumns.Kind.INTEGER,
                    M3SignalColumns.Origin.DOMAIN, d + 77L, false,
                    masks(present[d]), isLong ? null : ints[d - longs.length],
                    isLong ? longs[d] : null, null);
            specs[d] = new M3SignalQualitySchema.Spec(
                    "d_" + d, M3SignalQualitySchema.Family.DISTANCE,
                    M3SignalQualitySchema.Phase.INDEX, reliability[d],
                    M3SignalQualitySchema.Invalidation.RECORDED_DEPENDENCIES,
                    "integer units", "test-domain-producer", Double.NaN, "");
        }
        var graph = M3SignalRelations.of(rows, new int[0], new int[0],
                new byte[0], new int[0]);
        return M3SignalQualitySchema.declared(new M3SignalColumns(rows, columns),
                M3SignalDependencyEpochs.from(graph, false), 21L, SOURCE, specs);
    }
    private static M3SignalQualitySchema.Reliability[] exact(int n) {
        M3SignalQualitySchema.Reliability[] a = new M3SignalQualitySchema.Reliability[n];
        Arrays.fill(a, M3SignalQualitySchema.Reliability.EXACT_FACT);
        return a;
    }
    private static BigInteger oracle(long[][] longs, int[][] ints, int[] dimensions,
            int[] weights, int left, int right) {
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < dimensions.length; i++) {
            int dim = dimensions[i];
            BigInteger a = dim < longs.length
                    ? BigInteger.valueOf(longs[dim][left])
                    : BigInteger.valueOf(ints[dim - longs.length][left]);
            BigInteger b = dim < longs.length
                    ? BigInteger.valueOf(longs[dim][right])
                    : BigInteger.valueOf(ints[dim - longs.length][right]);
            result = result.add(a.subtract(b).abs().multiply(BigInteger.valueOf(weights[i])));
        }
        return result;
    }
    private static void extremes() {
        long[][] l = {{Long.MIN_VALUE, Long.MAX_VALUE, 0, 1L << 53,
                (1L << 53) + 1, Long.MIN_VALUE, Long.MAX_VALUE, -1}};
        int[][] in = {{Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1, -2, Integer.MIN_VALUE,
                Integer.MAX_VALUE, -1}};
        boolean[][] valid = {new boolean[8], new boolean[8]};
        Arrays.fill(valid[0], true);
        Arrays.fill(valid[1], true);
        var owner = schema(l, in, valid, exact(2));
        int[] dims = {0,1};
        int[] weight = {1_000_000, 1_000_000};
        var plan = M3SignalExactIntegerDistance.of(owner, dims, weight);
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                var got = plan.distance(i,j,21,SOURCE);
                check(got.toBigInteger().equals(oracle(l,in,dims,weight,i,j)),
                        "full signed-long and weighted integer oracle");
                check(got.compareTo(plan.distance(j,i,21,SOURCE))==0,
                        "symmetric stored-coordinate distance");
                check(got.high() >= 0, "chosen bounded dimensions fit nonnegative high limb");
            }
        }
        long[][] reduced = {{0L,Long.MAX_VALUE,Long.MIN_VALUE}};
        boolean[][] present = {{true,true,true}};
        var clean = schema(reduced,new int[0][],present,exact(1));
        var p = M3SignalExactIntegerDistance.of(clean,new int[]{0},new int[]{1});
        check(p.distance(1,2,21,SOURCE).toBigInteger()
                .equals(BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE)),
                "full 2^64-1 delta never rounded via floating point");
        check(p.distance(0,0,21,SOURCE).toBigInteger().equals(BigInteger.ZERO),
                "identical row has exact zero coordinate distance");
    }
    private static void randomizedTopK() {
        Random rnd = new Random(0x4d334c3145584143L);
        for (int trial = 0; trial < 600; trial++) {
            int n = 1 + rnd.nextInt(80);
            long[][] longs = new long[2][n];
            int[][] ints = new int[1][n];
            boolean[][] valid = new boolean[3][n];
            for (int row = 0; row < n; row++) {
                longs[0][row] = switch (rnd.nextInt(9)) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> (1L << 53) + rnd.nextInt(4);
                    default -> rnd.nextLong();
                };
                longs[1][row] = rnd.nextInt(7) - 3;
                ints[0][row] = rnd.nextInt(31) - 15;
                for (int d=0; d<3; d++) valid[d][row] = rnd.nextInt(7) != 0;
            }
            // Force a query row and a nonempty set of candidate rows with all facts valid.
            int query = rnd.nextInt(n);
            for (int d=0; d<3; d++) valid[d][query] = true;
            var owner = schema(longs, ints, valid, exact(3));
            int[] dimensions = {0, 1, 2};
            int[] weights = {1 + rnd.nextInt(1_000_000),1 + rnd.nextInt(256),
                    1 + rnd.nextInt(256)};
            var plan = M3SignalExactIntegerDistance.of(owner,dimensions,weights);
            List<Integer> eligible = new ArrayList<>();
            for (int row=0; row<n; row++) {
                if (valid[0][row]&&valid[1][row]&&valid[2][row]) eligible.add(row);
            }
            int occurrences = rnd.nextInt(140);
            int[] candidates = new int[occurrences];
            for (int i=0;i<occurrences;i++) candidates[i]=eligible.get(rnd.nextInt(eligible.size()));
            int k = rnd.nextInt(occurrences+6);
            int expectedLimit = Math.min(k,candidates.length);
            Integer[] ordering = new Integer[candidates.length];
            for (int i=0;i<ordering.length;i++) ordering[i]=i;
            Arrays.sort(ordering, (a,b) -> {
                int order = oracle(longs,ints,dimensions,weights,query,candidates[a])
                        .compareTo(oracle(longs,ints,dimensions,weights,query,candidates[b]));
                return order != 0 ? order : Integer.compare(a,b);
            });
            int[] result = plan.nearest(query,candidates,k,21,SOURCE);
            check(result.length==expectedLimit,"bounded top-K cardinality");
            for (int i=0;i<result.length;i++) {
                check(result[i]==candidates[ordering[i]],"stable exact Top-K result");
            }
            if (trial % 7 == 0) {
                int target = eligible.get(rnd.nextInt(eligible.size()));
                check(plan.distance(query,target,21,SOURCE).toBigInteger()
                        .equals(oracle(longs,ints,dimensions,weights,query,target)),
                        "random unsigned 128 magnitude parity");
            }
        }
    }
    private static void refusalsAndInvalidation() {
        long[][] longs = {{0,Long.MIN_VALUE,Long.MAX_VALUE,8}};
        int[][] ints = {{1,2,3,4}};
        boolean[][] valid = {{true,true,true,false},{true,true,true,true}};
        var schema = schema(longs,ints,valid,exact(2));
        var plan = M3SignalExactIntegerDistance.of(schema,new int[]{0,1},new int[]{3,4});
        refuse(IllegalArgumentException.class,() -> plan.distance(0,3,21,SOURCE));
        refuse(IllegalArgumentException.class,() -> plan.nearest(0,new int[]{2,3},2,21,SOURCE));
        refuse(IndexOutOfBoundsException.class,() -> plan.distance(0,4,21,SOURCE));
        refuse(IndexOutOfBoundsException.class,() -> plan.nearest(0,new int[]{4},1,21,SOURCE));
        refuse(IllegalArgumentException.class,() -> plan.nearest(0,new int[]{1},-1,21,SOURCE));
        refuse(IllegalStateException.class,() -> plan.distance(0,1,22,SOURCE));
        refuse(IllegalStateException.class,() -> plan.distance(0,1,21,"f".repeat(64)));
        refuse(IllegalArgumentException.class,() -> M3SignalExactIntegerDistance.of(schema,
                new int[]{0,0},new int[]{1,2}));
        refuse(IllegalArgumentException.class,() -> M3SignalExactIntegerDistance.of(schema,
                new int[]{0},new int[]{0}));
        refuse(IllegalArgumentException.class,() -> M3SignalExactIntegerDistance.of(schema,
                new int[]{0},new int[]{1_000_001}));
        refuse(IllegalArgumentException.class,() -> M3SignalExactIntegerDistance.of(schema,
                new int[]{0,1},new int[]{1}));
        var estimates=exact(2);
        estimates[1] = M3SignalQualitySchema.Reliability.UNKNOWN;
        var unknown=schema(longs,ints,valid,estimates);
        refuse(IllegalArgumentException.class,() -> M3SignalExactIntegerDistance.of(unknown,
                new int[]{1},new int[]{1}));
        schema.invalidationEpochsOwner().invalidateAll();
        refuse(IllegalStateException.class,() -> plan.distance(0,1,21,SOURCE));
        refuse(IllegalStateException.class,() -> plan.nearest(0,new int[]{1},1,21,SOURCE));
    }
    private static void monitorContracts() {
        long[][] longs={{0,1,2,3}};
        boolean[][] valid={{true,true,true,true}};
        var owner=schema(longs,new int[0][],valid,exact(1));
        var plan=M3SignalExactIntegerDistance.of(owner,new int[]{0},new int[]{1});
        final int[] progress={0,0,0};
        M3ProgressMonitor good=(M3ProgressMonitor)Proxy.newProxyInstance(
                M3ProgressMonitor.class.getClassLoader(),new Class<?>[]{M3ProgressMonitor.class},
                (proxy,method,args)->switch(method.getName()){
                    case "beginTask" -> {progress[0]++;yield null;}
                    case "worked" -> {progress[1]+=(int)(long)args[0];yield null;}
                    case "done" -> {progress[2]++;yield null;}
                    case "isCanceled" -> false;
                    default -> null;
                });
        check(Arrays.equals(plan.nearest(0,new int[]{3,2,1},2,21,SOURCE,good),new int[]{1,2}),
                "progress nearest preserves ordering");
        check(Arrays.equals(progress,new int[]{1,3,1}),"progress completed exactly");
        M3ProgressMonitor cancel=(M3ProgressMonitor)Proxy.newProxyInstance(
                M3ProgressMonitor.class.getClassLoader(),new Class<?>[]{M3ProgressMonitor.class},
                (proxy,method,args)-> {
                    if(method.getName().equals("checkCanceled"))throw new CancellationException("cancel");
                    if(method.getName().equals("isCanceled"))return true;
                    return null;
                });
        refuse(CancellationException.class,()->plan.nearest(0,new int[]{3,2,1},2,21,SOURCE,cancel));
        check(owner.invalidationEpochsOwner().epoch()==0,"cancellation does not mutate original owner");
    }

    public static void main(String[] args) {
        extremes();
        randomizedTopK();
        refusalsAndInvalidation();
        monitorContracts();
        System.out.println("M3_EXACT_INTEGER_SIGNAL_DISTANCE_PASS checks="+checks);
    }
}
