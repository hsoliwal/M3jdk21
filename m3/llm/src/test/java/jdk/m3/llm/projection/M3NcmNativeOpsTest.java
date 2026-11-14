// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.projection;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Random;
import org.junit.jupiter.api.Test;

final class M3NcmNativeOpsTest {
    private static final M3NcmNativeOps.Mode JAVA = M3NcmNativeOps.Mode.JAVA;
    private static final M3NcmNativeOps.Mode NATIVE = M3NcmNativeOps.Mode.NATIVE_REQUIRED;

    @Test void selectionNeverDisguisesARequiredNativeRun() {
        if (Boolean.getBoolean("ncm.test.native.required")) assertTrue(M3NcmNativeOps.available());
        if (!M3NcmNativeOps.available()) {
            assertThrows(UnsupportedOperationException.class,
                    () -> M3NcmNativeOps.nearest(new float[]{0}, 1, new float[]{0}, NATIVE));
        }
        assertArrayEquals(new int[]{0},
                M3NcmNativeOps.nearest(new float[]{1}, 1, new float[]{0,2}, JAVA));
    }

    @Test void seededPrimitiveBatchesAgreeWithJava() {
        var mode = M3NcmNativeOps.available() ? NATIVE : M3NcmNativeOps.Mode.AUTO;
        Random random = new Random(0x4e434dL);
        for (int sample = 0; sample < 80; sample++) {
            int channels = 1 + random.nextInt(7), length = 1 + random.nextInt(11);
            int batch = 1 + random.nextInt(3), target = 1 + random.nextInt(13);
            float[] rows = values(random, batch*length*channels);
            float[] codes = values(random, (1+random.nextInt(9))*channels);
            float[] weights = values(random, channels*channels*3);
            float[] bias = values(random, channels);
            assertArrayEquals(M3NcmOps.nearest(rows, channels, codes),
                    M3NcmNativeOps.nearest(rows, channels, codes, mode));
            assertArrayEquals(M3NcmOps.areaResize(rows,batch,length,channels,target),
                    M3NcmNativeOps.areaResize(rows,batch,length,channels,target,mode), 0f);
            assertArrayEquals(M3NcmOps.linearResize(rows,batch,length,channels,target),
                    M3NcmNativeOps.linearResize(rows,batch,length,channels,target,mode), 0f);
            assertArrayEquals(M3NcmOps.phi(rows,batch,length,channels,weights,bias,.5f),
                    M3NcmNativeOps.phi(rows,batch,length,channels,weights,bias,.5f,mode), 0f);
            float[] headBias = values(random,codes.length/channels);
            assertArrayEquals(M3NcmOps.tiedLogits(rows,channels,codes,.25f,headBias),
                    M3NcmNativeOps.tiedLogits(rows,channels,codes,.25f,headBias,mode), 2e-5f);
        }
    }

    @Test void invalidArgumentsRefuseBeforeEnteringNativeAndDoNotMutateInput() {
        for (var mode : M3NcmNativeOps.Mode.values()) {
            assertThrows(IllegalArgumentException.class,
                    () -> M3NcmNativeOps.nearest(new float[]{Float.NaN},1,new float[]{0},mode));
            assertThrows(IllegalArgumentException.class,
                    () -> M3NcmNativeOps.areaResize(new float[2],1,2,1,Integer.MAX_VALUE,mode));
            assertThrows(IllegalArgumentException.class,
                    () -> M3NcmNativeOps.phi(new float[1],1,1,1,new float[2],new float[1],.5f,mode));
        }
        float[] input = {0,2,4,6,8}, original = input.clone();
        assertArrayEquals(new float[]{1,4,7},
                M3NcmNativeOps.areaResize(input,1,5,1,3,JAVA), 0f);
        assertArrayEquals(original,input,0f);
        assertThrows(NullPointerException.class,
                () -> M3NcmNativeOps.nearest(input,1,new float[]{0},null));
    }

    private static float[] values(Random random,int count) {
        float[] result = new float[count];
        for (int i=0;i<count;i++) result[i] = (random.nextInt(2001)-1000)/128f;
        return result;
    }
}
