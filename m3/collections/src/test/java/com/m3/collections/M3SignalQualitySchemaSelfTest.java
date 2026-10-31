/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/** Deterministic contract and hostile inputs against original typed signal and epoch owners. */
public final class M3SignalQualitySchemaSelfTest {
    private static final String SOURCE = "a".repeat(64);
    private static final String PROOF = "b".repeat(64);
    private static long checks;
    private M3SignalQualitySchemaSelfTest() { }

    private static void ok(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void fails(Class<? extends Throwable> expected, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable error) {
            if (expected.isInstance(error)) return;
            throw new AssertionError("wrong exception: " + error, error);
        }
        throw new AssertionError("missing " + expected.getSimpleName());
    }
    private static M3SignalQualitySchema.Spec spec(String name,
            M3SignalQualitySchema.Family family, M3SignalQualitySchema.Phase phase,
            M3SignalQualitySchema.Reliability reliability, double confidence,
            String evidenceSha256) {
        return new M3SignalQualitySchema.Spec(name, family, phase, reliability,
                M3SignalQualitySchema.Invalidation.RECORDED_DEPENDENCIES,
                "units", "M3SourceSignalPlugin", confidence, evidenceSha256);
    }
    private static M3SignalColumns facts(int count) {
        int[] types = new int[count];
        double[] profile = new double[count];
        long[] ordinals = new long[count];
        for (int i=0;i<count;i++) { types[i]=i; profile[i]=i+0.25; ordinals[i]=i; }
        return new M3SignalColumns(count,
                new M3SignalColumns.Column(M3SignalColumns.Kind.INTEGER,
                        M3SignalColumns.Origin.STATIC, 15, false,
                        M3SignalColumns.allValid(count), types, null, null),
                new M3SignalColumns.Column(M3SignalColumns.Kind.LONG,
                        M3SignalColumns.Origin.DOMAIN, 16, false,
                        M3SignalColumns.allValid(count), null, ordinals, null),
                new M3SignalColumns.Column(M3SignalColumns.Kind.DOUBLE,
                        M3SignalColumns.Origin.PROFILED, 17, true,
                        M3SignalColumns.allValid(count), null, null, profile));
    }
    private static M3SignalQualitySchema.Spec[] specs() {
        return new M3SignalQualitySchema.Spec[] {
            spec("compiler.fieldCount", M3SignalQualitySchema.Family.STRUCTURAL,
                    M3SignalQualitySchema.Phase.COMPILER,
                    M3SignalQualitySchema.Reliability.EXACT_FACT, Double.NaN, ""),
            spec("domain.rankBound", M3SignalQualitySchema.Family.ORDERING,
                    M3SignalQualitySchema.Phase.INDEX,
                    M3SignalQualitySchema.Reliability.SOUND_LOWER_BOUND, Double.NaN, PROOF),
            spec("runtime.allocEstimate", M3SignalQualitySchema.Family.ALLOCATION,
                    M3SignalQualitySchema.Phase.RUNTIME,
                    M3SignalQualitySchema.Reliability.ESTIMATE, 0.6, "")
        };
    }
    private static M3SignalDependencyEpochs graph(int rows) {
        // Source caller -> callee. Changing callee 1 must invalidate callers 0 and 2.
        return M3SignalDependencyEpochs.from(M3SignalRelations.of(rows,
                new int[]{0, 2}, new int[]{1, 1},
                new byte[]{M3SignalRelations.DIRECT, M3SignalRelations.DIRECT},
                new int[]{1, 1}), false);
    }

    private static void contracts() {
        var signals=facts(4);
        var epochs=graph(4);
        var schema=M3SignalQualitySchema.declared(signals,epochs,9,SOURCE,specs());
        ok(schema.rowCount()==4 && schema.dimensionCount()==3,"shape");
        ok(schema.sourceVersion()==9 && schema.sourceSha256().equals(SOURCE),"identity");
        ok(schema.sourceAuthority()==M3SignalQualitySchema.SourceAuthority.DECLARED,"declared");
        ok(schema.kind(0)==M3SignalColumns.Kind.INTEGER,"typed int");
        ok(schema.kind(1)==M3SignalColumns.Kind.LONG,"typed long");
        ok(schema.kind(2)==M3SignalColumns.Kind.DOUBLE,"typed double");
        ok(schema.origin(2)==M3SignalColumns.Origin.PROFILED,"profiled");
        ok(schema.evidenceId(0)==15,"original evidence ID");
        ok(schema.validAt(1,0),"original validity");
        ok(schema.descriptor(0).name().equals("compiler.fieldCount"),"column name");
        ok(schema.reliability(1)==M3SignalQualitySchema.Reliability.SOUND_LOWER_BOUND,"bound retained as claim");
        ok(Double.isNaN(schema.descriptor(1).confidence()),"bounds do not have statistical confidence");
        ok(schema.descriptor(2).confidence()==0.6,"estimate confidence");
        schema.requireCurrent(9,SOURCE);
        fails(IllegalStateException.class,()->schema.requireCurrent(10,SOURCE));
        fails(IllegalStateException.class,()->schema.requireCurrent(9,PROOF));
        var rows=new int[]{0,3,0};
        var ticket=schema.capture(rows);
        rows[0]=2; // Caller mutation must not rewrite captured physical selection.
        ok(Arrays.equals(ticket.rows(),new int[]{0,3,0}),"capture copied row coordinates");
        var snapshots=ticket.stamps();
        snapshots[0]=999;
        ok(schema.unchanged(ticket,9,SOURCE),"capture copied stamps");
        ok(!schema.unchanged(ticket,8,SOURCE),"version mismatch");
        ok(!schema.unchanged(ticket,9,PROOF),"digest mismatch");
        epochs.invalidate(1);
        ok(!schema.unchanged(ticket,9,SOURCE),"callee change invalidates recorded callers");
        var other=schema.capture(new int[]{3});
        epochs.invalidate(1);
        ok(schema.unchanged(other,9,SOURCE),"disjoint source row remains fresh");
        epochs.invalidateAll();
        ok(!schema.unchanged(other,9,SOURCE),"unknown dependency invalidates all");
        var separate=M3SignalQualitySchema.declared(signals,epochs,9,SOURCE,specs());
        fails(IllegalArgumentException.class,()->separate.unchanged(ticket,9,SOURCE));
        fails(IndexOutOfBoundsException.class,()->schema.capture(new int[]{0,4}));
        fails(IndexOutOfBoundsException.class,()->schema.descriptor(3));
        fails(IndexOutOfBoundsException.class,()->schema.validAt(1,5));
    }

    private static void invalidSchemas() {
        var facts=facts(4);
        var epochs=graph(4);
        var original=specs();
        fails(IllegalArgumentException.class, () -> M3SignalQualitySchema.declared(
                facts,epochs,0,SOURCE,original[0],original[1]));
        fails(IllegalArgumentException.class, () -> M3SignalQualitySchema.declared(
                facts,epochs,-1,SOURCE,original));
        fails(IllegalArgumentException.class, () -> M3SignalQualitySchema.declared(
                facts,epochs,0,"not a digest",original));
        fails(IllegalArgumentException.class, () -> M3SignalQualitySchema.declared(
                facts,graph(5),0,SOURCE,original));
        var duplicate=specs();
        duplicate[1]=spec(duplicate[0].name(),duplicate[1].family(),duplicate[1].phase(),
                duplicate[1].reliability(),Double.NaN,PROOF);
        fails(IllegalArgumentException.class, () -> M3SignalQualitySchema.declared(
                facts,epochs,0,SOURCE,duplicate));
        var forged=specs();
        forged[2]=spec("runtime.allocEstimate",M3SignalQualitySchema.Family.ALLOCATION,
                M3SignalQualitySchema.Phase.COMPILER,
                M3SignalQualitySchema.Reliability.ESTIMATE,0.6,"");
        fails(IllegalArgumentException.class, () -> M3SignalQualitySchema.declared(
                facts,epochs,0,SOURCE,forged));
        fails(IllegalArgumentException.class, () -> spec("invalid signal",M3SignalQualitySchema.Family.TYPE,
                M3SignalQualitySchema.Phase.COMPILER,
                M3SignalQualitySchema.Reliability.EXACT_FACT,Double.NaN,""));
        fails(IllegalArgumentException.class, () -> spec("bounded",M3SignalQualitySchema.Family.DISTANCE,
                M3SignalQualitySchema.Phase.INDEX,
                M3SignalQualitySchema.Reliability.SOUND_UPPER_BOUND,Double.NaN,""));
        fails(IllegalArgumentException.class, () -> spec("confidence",M3SignalQualitySchema.Family.EXECUTION,
                M3SignalQualitySchema.Phase.RUNTIME,
                M3SignalQualitySchema.Reliability.ESTIMATE,1.01,""));
        fails(IllegalArgumentException.class, () -> spec("exact",M3SignalQualitySchema.Family.CORRECTNESS,
                M3SignalQualitySchema.Phase.COMPILER,
                M3SignalQualitySchema.Reliability.EXACT_FACT,0.90,""));
        fails(IllegalArgumentException.class, () -> spec("nothex",M3SignalQualitySchema.Family.DISTANCE,
                M3SignalQualitySchema.Phase.INDEX,
                M3SignalQualitySchema.Reliability.SOUND_LOWER_BOUND,Double.NaN,"1"));
        fails(NullPointerException.class, () -> M3SignalQualitySchema.declared(
                facts,epochs,0,SOURCE,(M3SignalQualitySchema.Spec[])null));
    }

    private static void taxonomyAndRandomFreshness() {
        var families=M3SignalQualitySchema.Family.values();
        ok(families.length>=24,"PRD taxonomy coverage");
        for (var f:families) {
            var x=spec("domain."+f.name(),f,M3SignalQualitySchema.Phase.INDEX,
                    M3SignalQualitySchema.Reliability.UNKNOWN,Double.NaN,"");
            ok(x.family()==f,"every enum family has a descriptor");
        }
        var random=new Random(0x3a30b2dfL);
        for(int trial=0;trial<500;trial++) {
            int n=2+random.nextInt(30);
            int m=random.nextInt(n*n+1);
            int[] from=new int[m],to=new int[m],sites=new int[m];
            byte[] kinds=new byte[m];
            for(int i=0;i<m;i++) {
                from[i]=random.nextInt(n);to[i]=random.nextInt(n);sites[i]=1;
                kinds[i]=random.nextBoolean()?M3SignalRelations.DIRECT:M3SignalRelations.DISPATCH_CANDIDATE;
            }
            var relations=M3SignalRelations.of(n,from,to,kinds,sites);
            boolean include=random.nextBoolean();
            var epochs=M3SignalDependencyEpochs.from(relations,include);
            var quality=M3SignalQualitySchema.declared(facts(n),epochs,trial,SOURCE,specs());
            int[] selected=new int[n];for(int i=0;i<n;i++)selected[i]=i;
            var ticket=quality.capture(selected);
            ok(quality.unchanged(ticket,trial,SOURCE),"random fresh");
            int seed=random.nextInt(n);
            epochs.invalidate(seed);
            boolean[] affected=new boolean[n];affected[seed]=true;
            for(int iteration=0;iteration<n;iteration++) {
                for(int e=0;e<m;e++) {
                    if((include||kinds[e]==M3SignalRelations.DIRECT)&&affected[to[e]])
                        affected[from[e]]=true;
                }
            }
            final int replayEpoch=trial;
            for(int row=0;row<n;row++) {
                var probe=M3SignalQualitySchema.declared(facts(n),epochs,trial,SOURCE,specs());
                // Cross-schema ownership refusal must hold even with identical metadata.
                fails(IllegalArgumentException.class,()->probe.unchanged(ticket,replayEpoch,SOURCE));
                var baseline=quality.capture(new int[]{row});
                ok(quality.unchanged(baseline,trial,SOURCE),"new row capture is fresh");
                ok((epochs.stamp(row)==epochs.epoch())==affected[row],"reverse-reachability equality");
            }
            ok(!quality.unchanged(ticket,trial,SOURCE),"seed affected a selected row");
        }
    }

    public static void main(String[] args) {
        contracts();
        invalidSchemas();
        taxonomyAndRandomFreshness();
        System.out.println("M3_SIGNAL_QUALITY_SCHEMA_PASS checks="+checks);
    }
}
