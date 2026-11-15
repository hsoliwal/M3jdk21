/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Canonical kernel admission refuses an existing incompatible registered body
 * @modules m3.parallel/jdk.m3.parallel
 * @run main/othervm CanonicalKernelConflictTest
 */

import jdk.m3.parallel.JavaKernel;
import jdk.m3.parallel.JavaKernels;
import jdk.m3.parallel.KernelId;

public class CanonicalKernelConflictTest {
    public static void main(String[] args) {
        JavaKernel incompatible = (index, buffers) -> { throw new AssertionError("must not execute"); };
        check("m3.parallel.int-add.v1", incompatible, JavaKernels::intAdd);
        check("m3.parallel.int-bitcount.v1", incompatible, JavaKernels::intBitCount);
        System.out.println("CanonicalKernelConflictTest passed");
    }

    private static void check(String name, JavaKernel body, Runnable admission) {
        KernelId id = KernelId.of(name);
        JavaKernels.register(id, body);
        try {
            admission.run();
            throw new AssertionError("conflicting body accepted: " + id);
        } catch (IllegalStateException expected) {
            if (JavaKernels.lookup(id).orElseThrow() != body) throw new AssertionError("previous body replaced");
        }
    }
}
