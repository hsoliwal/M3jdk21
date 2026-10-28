/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;

/**
 * Independent stable boxed-sort oracle for original M3 typed columns and Boolean
 * postfix selection with primitive rank-by-mask Top-K. No challenge source copied.
 */
public final class M3SignalRankedQuerySelfTest {
    private static final String DIGEST = "9".repeat(64);
    private static final long VERSION = 41;
    private static long checks;

    private M3SignalRankedQuerySelfTest() { }

    private static void require(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason + " assertion=" + checks);
    }
    private static void rejects(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError("unexpected exception: " + failure, failure);
        }
        throw new AssertionError("failed to reject " + kind.getName());
    }

    private record Fixture(M3SignalColumns columns, M3SignalQualitySchema quality,
            M3SignalDependencyEpochs epochs, M3SignalBooleanQuery query,
            int[] ints, long[] longs, double[] doubles, long[] validity) { }

    private static Fixture fixture(int[] ints, long[] longs, double[] doubles, long[] valid,
            boolean intDesc, boolean longDesc, boolean doubleDesc) {
        int rows = ints.length;
        M3SignalColumns columns = new M3SignalColumns(rows,
                new M3SignalColumns.Column(M3SignalColumns.Kind.INTEGER,
                        M3SignalColumns.Origin.STATIC, 1L, intDesc, valid, ints, null, null),
                new M3SignalColumns.Column(M3SignalColumns.Kind.LONG,
                        M3SignalColumns.Origin.DOMAIN, 2L, longDesc, valid, null, longs, null),
                new M3SignalColumns.Column(M3SignalColumns.Kind.DOUBLE,
                        M3SignalColumns.Origin.PROFILED, 3L, doubleDesc, valid, null, null, doubles));
        M3SignalDependencyEpochs epochs = M3SignalDependencyEpochs.from(
                M3SignalRelations.of(rows, new int[0], new int[0], new byte[0], new int[0]), false);
        M3SignalQualitySchema quality = M3SignalQualitySchema.declared(columns, epochs,
                VERSION, DIGEST,
                new M3SignalQualitySchema.Spec("test.int", M3SignalQualitySchema.Family.STRUCTURAL,
                        M3SignalQualitySchema.Phase.COMPILER,
                        M3SignalQualitySchema.Reliability.EXACT_FACT,
                        M3SignalQualitySchema.Invalidation.RECORDED_DEPENDENCIES,
                        "count", "javac", Double.NaN, ""),
                new M3SignalQualitySchema.Spec("test.long", M3SignalQualitySchema.Family.ORDERING,
                        M3SignalQualitySchema.Phase.INDEX,
                        M3SignalQualitySchema.Reliability.EXACT_FACT,
                        M3SignalQualitySchema.Invalidation.RECORDED_DEPENDENCIES,
                        "ordinal", "domain", Double.NaN, ""),
                new M3SignalQualitySchema.Spec("test.double", M3SignalQualitySchema.Family.STATISTICAL,
                        M3SignalQualitySchema.Phase.RUNTIME,
                        M3SignalQualitySchema.Reliability.ESTIMATE,
                        M3SignalQualitySchema.Invalidation.PROFILE_REFRESH,
                        "score", "profile", 0.7, ""));
        return new Fixture(columns, quality, epochs, M3SignalBooleanQuery.build(quality, 0, 1, 2),
                ints, longs, doubles, valid);
    }

    private static int rank(Fixture f, int a, int b, int[] priority) {
        for (int dimension : priority) {
            boolean va = (f.validity[a >>> 6] & (1L << (a & 63))) != 0;
            boolean vb = (f.validity[b >>> 6] & (1L << (b & 63))) != 0;
            if (va != vb) return va ? -1 : 1;
            if (!va) continue;
            int sign = switch (dimension) {
                case 0 -> Integer.compare(f.ints[a], f.ints[b]);
                case 1 -> Long.compare(f.longs[a], f.longs[b]);
                case 2 -> Double.compare(f.doubles[a], f.doubles[b]);
                default -> throw new AssertionError("invalid test dimension");
            };
            boolean descending = f.columns.snapshotColumn(dimension).descending();
            if (sign != 0) return descending ? -Integer.signum(sign) : Integer.signum(sign);
        }
        return Integer.compare(a, b);
    }

    private static int[] oracle(Fixture f, int[] selected, int[] order, int k) {
        Integer[] positions = new Integer[selected.length];
        for (int i = 0; i < positions.length; i++) positions[i] = selected[i];
        Arrays.sort(positions, (a,b) -> rank(f, a, b, order));
        int[] ranked = new int[Math.min(k, positions.length)];
        for (int i = 0; i < ranked.length; i++) ranked[i] = positions[i];
        return ranked;
    }

    private static void fixedCases() {
        int n = 9;
        int[] ints = {1, 1, 1, 0, 3, 1, 3, 1, 1};
        long[] longs = {Long.MAX_VALUE, (1L << 53) + 1, (1L << 53),
                Long.MIN_VALUE, 3, Long.MAX_VALUE, 2, Long.MAX_VALUE, 7};
        double[] doubles = {-0.0, 0.0, Double.NaN, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, -0.0, 2, -0.0, 9};
        long[] valid = {0x1fdL}; // row 1 missing, not zero or NaN
        Fixture f = fixture(ints,longs,doubles,valid,false,false,false);
        var compiled = f.query.compile(M3SignalBooleanQuery.Logic.ALL);
        int[] selected = compiled.evaluate(VERSION,DIGEST);
        int[] order = {1,2,0};
        int[] expected = oracle(f,selected,order,5);
        require(Arrays.equals(expected,compiled.evaluateTopK(5,order,VERSION,DIGEST)),
                "exact 64-bit/NaN and tie ordering of Boolean ALL");
        require(Arrays.equals(new int[0],compiled.evaluateTopK(0,order,VERSION,DIGEST)),
                "zero limited selection must return no rows");
        int[] all = compiled.evaluateTopK(100,order,VERSION,DIGEST);
        require(Arrays.equals(all,oracle(f,selected,order,100)),
                "excessive k capped at number of actual matches");
        long[] padded = {-1L};
        rejects(IllegalArgumentException.class,
                () -> new M3SignalTopK(f.columns,order).topKByMask(padded,4));
        rejects(IllegalArgumentException.class,
                () -> new M3SignalTopK(f.columns,order).topKByMask(new long[2],4));
        rejects(IllegalArgumentException.class,
                () -> compiled.evaluateTopK(-1,order,VERSION,DIGEST));
        rejects(IllegalArgumentException.class,
                () -> compiled.evaluateTopK(3,new int[]{0,0},VERSION,DIGEST));
        rejects(IllegalStateException.class,
                () -> compiled.evaluateTopK(3,order,VERSION+1,DIGEST));
        int[] existingWindow = compiled.evaluateWindow(2,3,VERSION,DIGEST).rows();
        require(Arrays.equals(existingWindow,Arrays.copyOfRange(selected,2,5)),
                "existing physical-order viewport contract remains unchanged");
        f.epochs.invalidateAll();
        rejects(IllegalStateException.class,
                () -> compiled.evaluateTopK(3,order,VERSION,DIGEST));
    }

    private static void randomizedOracle() {
        Random random = new Random(0x4d33524b4d41534bL);
        for(int trial=0;trial<660;trial++) {
            int n=random.nextInt(170);
            int[] ints=new int[n];long[] longs=new long[n];double[] doubles=new double[n];
            long[] valid=new long[(n+63) >>>6];
            for(int i=0;i<n;i++) {
                if(random.nextInt(6)!=0)valid[i>>>6]|=1L<<(i&63);
                ints[i]=random.nextInt(13)-6;
                longs[i]=switch(random.nextInt(12)) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    default -> (1L<<53)+random.nextInt(11)-5;
                };
                doubles[i]=switch(random.nextInt(8)) {
                    case 0 -> Double.NaN;
                    case 1 -> -0.0;
                    case 2 -> Double.POSITIVE_INFINITY;
                    case 3 -> Double.NEGATIVE_INFINITY;
                    default -> random.nextInt(11)-5.0;
                };
            }
            Fixture f=fixture(ints,longs,doubles,valid,random.nextBoolean(),
                    random.nextBoolean(),random.nextBoolean());
            int[] precedence={0,1,2};
            for(int i=2;i>0;i--) {int at=random.nextInt(i+1);int t=precedence[i];
                precedence[i]=precedence[at];precedence[at]=t;}
            M3SignalBooleanQuery.Compiled program=f.query.compile(
                    new M3SignalBooleanQuery.Term(0,
                            new M3SignalBooleanQuery.IntBetween(-2,3)),
                    new M3SignalBooleanQuery.Term(1,
                            new M3SignalBooleanQuery.LongBetween((1L<<53)-2,Long.MAX_VALUE)),
                    M3SignalBooleanQuery.Logic.AND,
                    new M3SignalBooleanQuery.Term(2,M3SignalBooleanQuery.Missing.INSTANCE),
                    M3SignalBooleanQuery.Logic.OR);
            int[] selected=program.evaluate(VERSION,DIGEST);
            M3SignalTopK ranker=new M3SignalTopK(f.columns,precedence);
            long[] bits=new long[(n+63)>>>6];
            for(int row:selected)bits[row>>>6]|=1L<<(row&63);
            int[] copy=bits.length==0?new int[0]:selected.clone();
            for(int q=0;q<6;q++) {
                int k=random.nextInt(n+5);
                int[] expected=oracle(f,selected,precedence,k);
                int[] ranked=program.evaluateTopK(k,precedence,VERSION,DIGEST);
                require(Arrays.equals(expected,ranked),"seeded ranked Boolean oracle");
                require(Arrays.equals(expected,ranker.topKByMask(bits,k)),"direct mask API parity");
                require(Arrays.equals(selected,copy),"source row ordinal ownership");
            }
        }
    }

    private static void cancellationAndReentrantInvalidation() {
        int[] values=new int[64];long[] longs=new long[64];double[] doubles=new double[64];
        long[] valid=M3SignalColumns.allValid(64);
        Fixture f=fixture(values,longs,doubles,valid,false,false,false);
        var program=f.query.compile(M3SignalBooleanQuery.Logic.ALL);
        class Probe implements M3ProgressMonitor {
            boolean cancel;boolean changeOnDone;int done;long worked;
            long totalWork = UNKNOWN;
            String taskName = "", subTaskName = "";
            @Override public void beginTask(String name,long total) {
                taskName = name;
                totalWork = total;
                worked = 0L;
            }
            @Override public void worked(long n) { worked+=n; }
            @Override public void done() { done++; if(changeOnDone) f.epochs.invalidateAll(); }
            @Override public void setProgress(long completed,long total) {
                worked = completed;
                totalWork = total;
            }
            @Override public void setTaskName(String name) { taskName = name; }
            @Override public void subTask(String name) { subTaskName = name; }
            @Override public boolean isCanceled(){return cancel;}
            @Override public void setCanceled(boolean canceled) { cancel = canceled; }
            @Override public M3ProgressState state() {
                return new M3ProgressState(taskName,subTaskName,worked,totalWork,
                        cancel,done>0,0L,0L);
            }
        }
        Probe probe=new Probe();
        probe.cancel=true;
        rejects(CancellationException.class, () -> program.evaluateTopK(5,new int[]{0},VERSION,DIGEST,probe));
        require(probe.done==0,"no done on cancelled query");
        Probe bad=new Probe();bad.changeOnDone=true;
        rejects(IllegalStateException.class, () -> program.evaluateTopK(5,new int[]{0},VERSION,DIGEST,bad));
        require(bad.done==1,"reentrant invalidation checked after done");
        require(bad.state().done(),"finished monitor exposes its completed state");
        Probe completeContract = new Probe();
        completeContract.setTaskName("ranked probe");
        completeContract.subTask("phase");
        completeContract.setProgress(2L,4L);
        require(completeContract.state().taskName().equals("ranked probe")
                && completeContract.state().subTaskName().equals("phase")
                && completeContract.state().worked()==2L
                && completeContract.state().totalWork()==4L,
                "all original progress SPI state fields are implemented");
        completeContract.setCanceled(true);
        require(completeContract.isCanceled() && completeContract.state().canceled(),
                "original SPI cancellation setter propagates to immutable state");
    }

    public static void main(String[] args) {
        fixedCases();
        randomizedOracle();
        cancellationAndReentrantInvalidation();
        System.out.println("M3_SIGNAL_RANKED_QUERY_PASS checks="+checks);
    }
}
