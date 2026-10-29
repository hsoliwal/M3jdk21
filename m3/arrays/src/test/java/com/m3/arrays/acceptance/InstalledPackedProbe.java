// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays.acceptance;

import com.m3.arrays.M3ArrayNative;
import com.m3.arrays.M3Arrays;
import com.m3.arrays.packed.Int40Array;
import com.m3.arrays.packed.Int48Array;
import com.m3.arrays.packed.Int56Array;
import com.m3.arrays.packed.PackedIntArray;
import com.m3.arrays.packed.VInt;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Independent runtime oracle: load the actual installed module and installed native library. */
public final class InstalledPackedProbe {
    private InstalledPackedProbe() { }

    public static void main(String[] arguments) {
        require(arguments.length == 0, "No classpath/native path override is accepted");
        Module module = Int40Array.class.getModule();
        require(module.isNamed() && "com.m3.arrays".equals(module.getName()), "installed named arrays module");
        require(module == M3Arrays.class.getModule() && module == VInt.class.getModule(), "single arrays owner");
        ModuleDescriptor descriptor = module.getDescriptor();
        Set<String> exports = descriptor.exports().stream().map(ModuleDescriptor.Exports::source).collect(Collectors.toSet());
        require(exports.equals(Set.of("com.m3.arrays", "com.m3.arrays.packed")), "only deliberate exports");
        require(descriptor.requires().stream().map(ModuleDescriptor.Requires::name).collect(Collectors.toSet())
                .equals(Set.of("java.base")), "no new runtime dependency");
        Path library = Path.of(System.getProperty("java.home"), "lib", System.mapLibraryName("m3_arrays"));
        require(Files.isRegularFile(library), "installed native payload");
        M3ArrayNative.load(library);
        require(M3ArrayNative.isLoaded(), "loaded installed library");
        for (int bits : new int[] {40, 48, 56}) {
            long[] values = {-(1L << (bits - 1)), -1, 0, 1, (1L << (bits - 1)) - 1};
            PackedIntArray packed = switch (bits) {
                case 40 -> new Int40Array(values.length);
                case 48 -> new Int48Array(values.length);
                case 56 -> new Int56Array(values.length);
                default -> throw new AssertionError(bits);
            };
            packed.setAll(0, values, 0, values.length);
            require(Arrays.equals(values, packed.toLongArray()), "signed-width boundaries");
            require(packed.payloadBytes() == (long) values.length * (bits / 8), "exact compact payload");
            byte[] encoded = M3ArrayNative.encodePackedRequired(values, bits);
            require(Arrays.equals(packed.toByteArray(), encoded), "native fixed-width bytes");
            require(Arrays.equals(values, M3ArrayNative.decodePackedRequired(encoded, bits)), "native fixed-width decode");
        }
        long[] values = {Long.MIN_VALUE, Long.MAX_VALUE, -1, 0, 1, -64, 64, 127, 128, 16383, 16384};
        byte[] unsigned = M3ArrayNative.encodeUnsigned64Required(values);
        byte[] zigzag = M3ArrayNative.encodeZigZag64Required(values);
        require(Arrays.equals(unsigned, VInt.encodeUnsigned64(values)), "unsigned64 portable/native bytes");
        require(Arrays.equals(zigzag, VInt.encodeZigZag64(values)), "ZigZag64 portable/native bytes");
        require(Arrays.equals(values, M3ArrayNative.decodeUnsigned64Required(unsigned)), "unsigned64 decode");
        require(Arrays.equals(values, M3ArrayNative.decodeZigZag64Required(zigzag)), "ZigZag64 decode");
        try {
            M3ArrayNative.decodeUnsigned64Required(new byte[] {(byte) 0x80, 0});
            throw new AssertionError("noncanonical value accepted");
        } catch (IllegalArgumentException expected) {
            // Complete format refusal is required from the installed JNI library.
        }
        require(M3ArrayNative.compareUtf16(new char[] {'a'}, 0, new char[] {'z'}, 0, 1) == -25, "old compare ABI");
        require(Arrays.equals(new char[] {'a', '\ud800', '\udc00'},
                M3ArrayNative.concatenateUtf16(new char[] {'a', '\ud800'}, new char[] {'\udc00'})), "old UTF16 join ABI");
        require(M3ArrayNative.allocateObjectsRequired(3).length == 3, "old Object allocation ABI");
        require(Arrays.equals(new byte[3], M3ArrayNative.allocateBytesRequired(3)), "old byte allocation ABI");
        require(Arrays.equals(new int[3], M3ArrayNative.allocateIntsRequired(3)), "old int allocation ABI");
        require(Arrays.equals(new long[3], M3ArrayNative.allocateLongsRequired(3)), "old long allocation ABI");
        System.out.println("M3_PACKED_INSTALLED_PASS module=" + module.getName()
                + " runtime=" + Runtime.version() + " native=" + library);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
