/* SPDX-License-Identifier: Apache-2.0 */

import com.m3.vcpu.internal.NativeVcpu;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Runs the unchanged {@link NativeVcpuOracleParity} body (directed programs and the four fuzz seeds, every slice
 * compared with the donor oracle CpuRv32iEngine) on every device of the "webgpu" backend: selectors webgpu:0,
 * webgpu:1, ... until one does not open. The body's private methods are reached by reflection so that no test
 * logic is duplicated.
 */
public final class NativeVcpuOracleParityWebgpu {
    private NativeVcpuOracleParityWebgpu() {
    }

    private static Object call(Method m, Object... args) throws ReflectiveOperationException {
        try {
            return m.invoke(null, args);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw failure;
        }
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        NativeVcpu.load();
        Class<NativeVcpuOracleParity> body = NativeVcpuOracleParity.class;
        Method directed = body.getDeclaredMethod("directed", long.class, String.class);
        Method fuzz = body.getDeclaredMethod("fuzz", long.class, String.class, long.class, int.class, int.class,
                int.class, int.class, int.class);
        Field checks = body.getDeclaredField("checks");
        Field failures = body.getDeclaredField("failures");
        directed.setAccessible(true);
        fuzz.setAccessible(true);
        checks.setAccessible(true);
        failures.setAccessible(true);
        int devices = 0;
        for (int device = 0; device < 16; device++) {
            String selector = "webgpu:" + device;
            long session;
            try {
                session = NativeVcpu.open(selector, -1);
            } catch (UnsupportedOperationException unavailable) {
                System.out.println("stop at " + selector + ": " + unavailable.getMessage());
                break;
            }
            devices++;
            long checksBefore = checks.getLong(null);
            long before = failures.getLong(null);
            long start = System.nanoTime();
            try {
                call(directed, session, selector);
                call(fuzz, session, selector, 11L, 777, 256, 6, 61, 64);
                call(fuzz, session, selector, 12L, 513, 512, 4, 200, 0);
                call(fuzz, session, selector, 13L, 1024, 256, 8, 17, 128);
                call(fuzz, session, selector, 14L, 61, 1024, 3, 1000, 1);
            } finally {
                NativeVcpu.close(session);
            }
            System.out.printf("%s: oracle parity done in %d ms, %d checks, failures %d%n", selector,
                    (System.nanoTime() - start) / 1_000_000, checks.getLong(null) - checksBefore,
                    failures.getLong(null) - before);
        }
        long total = checks.getLong(null);
        long failed = failures.getLong(null);
        boolean pass = failed == 0 && devices > 0;
        System.out.printf("NativeVcpuOracleParityWebgpu: %d device(s), %d checks, %d failures -> %s%n", devices, total,
                failed, pass ? "PASS" : "FAIL");
        System.exit(pass ? 0 : 1);
    }
}
