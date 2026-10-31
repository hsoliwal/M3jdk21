// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.ClassReader;

/**
 * Exact bytecode offset of every instruction, per method ({@code name + desc}), decoded from the
 * raw {@code Code} attribute (the ASM tree API keeps instruction order but not offsets). The
 * k-th real ASM instruction of a method is the k-th decoded instruction.
 */
final class M3BytecodeOffsets {
    private M3BytecodeOffsets() {
    }

    static Map<String, int[]> of(ClassReader r) {
        Map<String, int[]> out = new HashMap<>();
        char[] buf = new char[r.getMaxStringLength()];
        int u = r.header + 6;
        u += 2 + 2 * r.readUnsignedShort(u);
        int fields = r.readUnsignedShort(u);
        u += 2;
        for (int i = 0; i < fields; i++) u = skipMember(r, u);
        int methods = r.readUnsignedShort(u);
        u += 2;
        for (int i = 0; i < methods; i++) {
            String name = r.readUTF8(u + 2, buf), desc = r.readUTF8(u + 4, buf);
            int attributes = r.readUnsignedShort(u + 6);
            u += 8;
            for (int a = 0; a < attributes; a++) {
                int length = r.readInt(u + 2);
                if ("Code".equals(r.readUTF8(u, buf))) out.put(name + desc, decode(r, u + 14, r.readInt(u + 10)));
                u += 6 + length;
            }
        }
        return out;
    }

    private static int skipMember(ClassReader r, int u) {
        int attributes = r.readUnsignedShort(u + 6);
        u += 8;
        for (int a = 0; a < attributes; a++) u += 6 + r.readInt(u + 2);
        return u;
    }

    static int[] decode(ClassReader r, int start, int length) {
        int[] offsets = new int[Math.max(16, length / 2)];
        int n = 0, pc = 0;
        while (pc < length) {
            if (n == offsets.length) offsets = Arrays.copyOf(offsets, n * 2);
            offsets[n++] = pc;
            pc += size(r, start, pc);
        }
        return Arrays.copyOf(offsets, n);
    }

    private static int size(ClassReader r, int start, int pc) {
        int op = r.readByte(start + pc);
        switch (op) {
            case 16: case 18: case 21: case 22: case 23: case 24: case 25: case 54: case 55: case 56: case 57:
            case 58: case 169: case 188:
                return 2;
            case 17: case 19: case 20: case 132: case 178: case 179: case 180: case 181: case 182: case 183:
            case 184: case 187: case 189: case 192: case 193: case 198: case 199:
                return 3;
            case 197:
                return 4;
            case 185: case 186: case 200: case 201:
                return 5;
            case 196:
                return r.readByte(start + pc + 1) == 132 ? 6 : 4;
            case 170: {
                int pad = (4 - ((pc + 1) & 3)) & 3, at = start + pc + 1 + pad;
                int low = r.readInt(at + 4), high = r.readInt(at + 8);
                return 1 + pad + 12 + 4 * (high - low + 1);
            }
            case 171: {
                int pad = (4 - ((pc + 1) & 3)) & 3, at = start + pc + 1 + pad;
                return 1 + pad + 8 + 8 * r.readInt(at + 4);
            }
            default:
                return op >= 153 && op <= 168 ? 3 : 1;
        }
    }
}
