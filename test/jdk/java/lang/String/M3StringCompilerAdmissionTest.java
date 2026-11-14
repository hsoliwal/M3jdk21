/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 *
 * @test
 * @summary Observe the effective interpreter boundary for ordinary M3-backed String despite compiler requests
 * @modules java.management
 *          jdk.management
 *          java.base/java.lang:open
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -Xcomp -XX:+UseM3StringStorage M3StringCompilerAdmissionTest xcomp-before
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Xcomp M3StringCompilerAdmissionTest xcomp-after
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseCompiler -XX:+UseM3StringStorage M3StringCompilerAdmissionTest compiler-before
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:+UseCompiler M3StringCompilerAdmissionTest compiler-after
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:TieredStopAtLevel=1 -Xbatch M3StringCompilerAdmissionTest c1-requested
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-TieredCompilation -Xbatch M3StringCompilerAdmissionTest c2-requested
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -Xcomp -XX:+UseM3StringStorage -XX:-CompactStrings M3StringCompilerAdmissionTest xcomp-before-wide
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings -Xcomp M3StringCompilerAdmissionTest xcomp-after-wide
 */

import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;

/** A successful compiler request is not evidence that a method was compiled. */
public final class M3StringCompilerAdmissionTest {
    private M3StringCompilerAdmissionTest() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("one requested-configuration label is required");
        }
        HotSpotDiagnosticMXBean vm =
                ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        if (vm == null) {
            throw new AssertionError("HotSpot diagnostic bean is required for observed VM flags");
        }
        requireFlag(vm, "UseM3StringStorage", true);
        requireFlag(vm, "UseCompiler", false);
        requireFlag(vm, "TieredCompilation", false);
        requireFlag(vm, "UseStringDeduplication", false);
        String mode = System.getProperty("java.vm.info");
        if (mode == null || !mode.startsWith("interpreted mode")) {
            throw new AssertionError("M3 interpreter boundary changed: " + mode);
        }

        // Primitive code units are the independent oracle. Include NUL, an unpaired
        // high surrogate and an unpaired low surrogate; UTF-8 replacement is invalid.
        char[] units = {'m', '3', 0, (char) 0xd800, 'X', (char) 0xdc00, (char) 0x0100};
        String value = new String(units);
        Field storage = String.class.getDeclaredField("m3");
        Field compatibility = String.class.getDeclaredField("value");
        storage.setAccessible(true);
        compatibility.setAccessible(true);
        requireM3(value, storage, compatibility);
        checkUnits(value, units);

        String joined = value.substring(0, 4).concat(value.substring(4));
        requireM3(joined, storage, compatibility);
        checkUnits(joined, units);
        if (value.intern() != joined.intern()) {
            throw new AssertionError("exact interning differs across M3 composition shapes");
        }
        System.out.println("M3_STRING_COMPILER_BOUNDARY_PASS|requested=" + args[0]
                + "|effective=INTERPRETER_ONLY|UseCompiler=false|m3StorageObserved=true");
    }

    private static void requireFlag(
            HotSpotDiagnosticMXBean vm, String name, boolean expected) {
        String actual = vm.getVMOption(name).getValue();
        if (!Boolean.toString(expected).equals(actual)) {
            throw new AssertionError(name + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void requireM3(String value, Field storage, Field compatibility)
            throws IllegalAccessException {
        if (storage.get(value) == null || ((byte[]) compatibility.get(value)).length != 0) {
            throw new AssertionError("nonempty ordinary String must use canonical M3 storage");
        }
    }

    private static void checkUnits(String value, char[] expected) {
        if (value.length() != expected.length) {
            throw new AssertionError("M3 UTF-16 length mismatch");
        }
        int expectedHash = 0;
        for (int index = 0; index < expected.length; index++) {
            if (value.charAt(index) != expected[index]) {
                throw new AssertionError("M3 UTF-16 content mismatch at " + index);
            }
            expectedHash = 31 * expectedHash + expected[index];
        }
        if (value.hashCode() != expectedHash) {
            throw new AssertionError("M3 Java hash mismatch");
        }
    }
}
