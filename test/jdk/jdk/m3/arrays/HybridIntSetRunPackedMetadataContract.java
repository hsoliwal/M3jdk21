// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.lang.reflect.Field;
import java.util.Arrays;

/** Fail-closed postimage proof: packed RUNS cardinality and run pairs share one int lane. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/recipe-crates/m3-mat-hybrid-run-binary-20261030/src/test/java/com/synexia/matcollections/MatHybridRunPackedMetadataOracle.java blob 94ce30ad8f94; recipe m3/arrays/recipe/hybrid-int-set-20261106/relocate.py v3
public final class HybridIntSetRunPackedMetadataContract {
    private static int checks;
    private HybridIntSetRunPackedMetadataContract() { }

    public static void main(String[] args) throws Exception {
        for(int runs : new int[] {1,2,3,7,63,127,256,512,1023,1024}) {
            int[] raw = new int[runs * 8];
            int out = 0;
            for(int r=0;r<runs;r++)for(int j=0;j<8;j++)raw[out++]=r*11+j;
            HybridIntSet set = HybridIntSet.of(raw);
            check(set.chunkKind(0)==HybridIntSet.ChunkKind.RUNS,"run kind");
            Field f=HybridIntSet.class.getDeclaredField("counts");
            f.setAccessible(true);
            int[] data=(int[])f.get(set);
            check(data.length==1,"count lane single chunk");
            check((data[0] & 0x1ffff)==runs*8,"cardinality low 17 bits");
            check((data[0] >>> 17)==runs,"pair count high bits");
            Field run=HybridIntSet.class.getDeclaredField("runData");
            run.setAccessible(true);
            int[] lane=(int[])run.get(set);
            check(lane.length==runs*2,"exact pair lane size");
            for(int r=0;r<runs;r++) {
                check(lane[r*2]==r*11,"run first");
                check(lane[r*2+1]==8,"run length");
            }
            check(Arrays.equals(set.toArray(),raw),"no encoded count leaks into exported content");
            long bytes=set.primitivePayloadBytes();
            check(bytes==4L*3+1L+4L*lane.length,"payload matches exact primitive fields");
        }
        System.out.println("M3_RUN_PACKED_METADATA_PASS checks="+checks);
    }
    private static void check(boolean value,String description){checks++;if(!value)throw new AssertionError(description+" #"+checks);}
}
