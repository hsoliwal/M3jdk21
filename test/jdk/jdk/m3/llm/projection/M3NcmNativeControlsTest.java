// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.projection;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class M3NcmNativeControlsTest {
    private static final M3NcmNativeOps.Mode JAVA = M3NcmNativeOps.Mode.JAVA;
    private static final M3NcmNativeOps.Mode AUTO = M3NcmNativeOps.Mode.AUTO;
    private static final M3NcmNativeOps.Mode NATIVE = M3NcmNativeOps.Mode.NATIVE_REQUIRED;
    private static final M3NcmOps.Budget BUDGET = M3NcmOps.DEFAULT_BUDGET;

    @Test void controlCapabilityIsIndependentAndRequiredModeNeverFallsBack() {
        if (Boolean.getBoolean("ncm.test.controlled.required"))
            assertTrue(M3NcmNativeOps.controlledAvailable(), "controlled JNI must actually be present");
        if (Boolean.getBoolean("ncm.test.controlled.unavailable")) {
            assertTrue(M3NcmNativeOps.available(), "legacy ABI1 library must actually be present");
            assertFalse(M3NcmNativeOps.controlledAvailable());
        }
        AtomicInteger calls = new AtomicInteger();
        M3NcmOps.Cancellation cancellation = calls::incrementAndGet;
        assertArrayEquals(new int[]{0}, M3NcmNativeOps.nearest(
                new float[]{1}, 1, new float[]{0,2}, AUTO, BUDGET, cancellation));
        assertTrue(calls.get() > 0);
        if (!M3NcmNativeOps.controlledAvailable()) {
            assertThrows(UnsupportedOperationException.class, () -> M3NcmNativeOps.nearest(
                    new float[]{1}, 1, new float[]{0,2}, NATIVE, BUDGET, cancellation));
            if (M3NcmNativeOps.available())
                assertArrayEquals(new int[]{0}, M3NcmNativeOps.nearest(
                        new float[]{1}, 1, new float[]{0,2}, NATIVE));
        }
    }

    @Test void javaDispatchValidatesAndChargesOnce() {
        float[] rows={1,2,3,4,5,6}, book={0,0,0,3,3,3};
        AtomicInteger direct=new AtomicInteger(), dispatched=new AtomicInteger();
        int[] expected=M3NcmOps.nearest(rows,3,book,BUDGET,direct::incrementAndGet);
        int[] actual=M3NcmNativeOps.nearest(rows,3,book,JAVA,BUDGET,dispatched::incrementAndGet);
        assertArrayEquals(expected,actual);
        assertEquals(direct.get(),dispatched.get(), "fallback must not repeat validation checkpoints");
        if (!M3NcmNativeOps.controlledAvailable()) {
            dispatched.set(0);
            assertArrayEquals(expected,M3NcmNativeOps.nearest(rows,3,book,AUTO,BUDGET,dispatched::incrementAndGet));
            assertEquals(direct.get(),dispatched.get());
        }
    }

    @Test void strictBudgetsRejectEveryPrimitiveBeforeAnyNativeWork() {
        var budget=new M3NcmOps.Budget(1,1);
        for (var mode:M3NcmNativeOps.Mode.values()) {
            assertThrows(IllegalArgumentException.class,()->M3NcmNativeOps.nearest(
                    new float[]{1,2},1,new float[]{0,2},mode,budget,M3NcmOps.Cancellation.NONE));
            assertThrows(IllegalArgumentException.class,()->M3NcmNativeOps.areaResize(
                    new float[]{1,2},1,2,1,1,mode,budget,M3NcmOps.Cancellation.NONE));
            assertThrows(IllegalArgumentException.class,()->M3NcmNativeOps.linearResize(
                    new float[]{1,2},1,2,1,3,mode,budget,M3NcmOps.Cancellation.NONE));
            assertThrows(IllegalArgumentException.class,()->M3NcmNativeOps.phi(
                    new float[]{1},1,1,1,new float[]{0,1,0},new float[]{0},.5f,mode,budget,M3NcmOps.Cancellation.NONE));
            assertThrows(IllegalArgumentException.class,()->M3NcmNativeOps.tiedLogits(
                    new float[]{1},1,new float[]{0,2},0,null,mode,budget,M3NcmOps.Cancellation.NONE));
        }
    }

    @Test void cancellationInsideJniPropagatesOriginalExceptionAndReleasesResources() {
        if (!M3NcmNativeOps.controlledAvailable()) {
            assertThrows(UnsupportedOperationException.class,()->M3NcmNativeOps.nearest(
                    new float[]{0},1,new float[]{0},NATIVE,BUDGET,()->{}));
            return;
        }
        float[] rows=new float[64*3], book=new float[32*3];
        Arrays.fill(rows,.5f); Arrays.fill(book,.25f);
        float[] originalRows=rows.clone(), originalBook=book.clone();
        for (int round=0;round<12;round++) {
            var marker=new CancellationException("native cancellation marker "+round);
            AtomicInteger calls=new AtomicInteger();
            boolean[] inNative={false};
            M3NcmOps.Cancellation cancellation=()->{
                if (calls.incrementAndGet()==16) {
                    inNative[0]=Arrays.stream(Thread.currentThread().getStackTrace()).anyMatch(
                            frame->frame.isNativeMethod()&&frame.getClassName().equals(M3NcmNativeOps.class.getName()));
                    throw marker;
                }
            };
            assertSame(marker,assertThrows(CancellationException.class,()->M3NcmNativeOps.nearest(
                    rows,3,book,NATIVE,BUDGET,cancellation)));
            assertTrue(inNative[0], "cancellation must occur inside JNI, beyond Java admission");
            assertArrayEquals(originalRows,rows,0f); assertArrayEquals(originalBook,book,0f);
            assertArrayEquals(new int[64],M3NcmNativeOps.nearest(rows,3,book,NATIVE));
        }
    }

    @Test void controlledAllocationCanCrossTheLegacyCeiling() {
        int count=16_777_217;
        var budget=new M3NcmOps.Budget(count,12L*count+2);
        if (!M3NcmNativeOps.controlledAvailable()) {
            assertThrows(UnsupportedOperationException.class,()->M3NcmNativeOps.areaResize(
                    new float[]{2},1,1,1,count,NATIVE,budget,M3NcmOps.Cancellation.NONE));
            return;
        }
        float[] result=M3NcmNativeOps.areaResize(
                new float[]{2},1,1,1,count,NATIVE,budget,M3NcmOps.Cancellation.NONE);
        assertEquals(count,result.length);
        for (float value:result) assertEquals(2f,value,0f);
    }

    @Test void cancellationAndNullControlFailuresLeaveInputsUnchanged() {
        float[] input={1,2,3}, before=input.clone();
        var marker=new CancellationException("before native admission");
        for (var mode:M3NcmNativeOps.Mode.values()) {
            assertSame(marker,assertThrows(CancellationException.class,()->M3NcmNativeOps.nearest(
                    input,1,new float[]{0},mode,BUDGET,()->{throw marker;})));
            assertThrows(IllegalArgumentException.class,()->M3NcmNativeOps.nearest(
                    input,1,new float[]{0},mode,null,M3NcmOps.Cancellation.NONE));
            assertThrows(IllegalArgumentException.class,()->M3NcmNativeOps.nearest(
                    input,1,new float[]{0},mode,BUDGET,null));
        }
        assertArrayEquals(before,input,0f);
    }
}
