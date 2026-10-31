/* SPDX-License-Identifier: Apache-2.0 */

import com.m3.vcpu.internal.NativeVcpu;
import com.synexia.m3.cpugpu.CpuRv32iEngine;
import com.synexia.m3.cpugpu.Rv32iAssembler;
import com.synexia.m3.cpugpu.Rv32iBatch;

import java.util.SplittableRandom;

/**
 * Parity of the native m3k_rv32im_slice kernel (through JNI) with the donor Java oracle CpuRv32iEngine
 * (cpu-gpu-rv32im @3bf82671, compiled in place from the staged donor sources). Every slice compares the complete
 * memory, register and state arrays. Directed programs use the donor Rv32iAssembler; the random differential uses
 * raw encodings so that illegal and corner-case instructions are covered too.
 */
public final class NativeVcpuOracleParity {
    private static long checks;
    private static long failures;

    private NativeVcpuOracleParity() {
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok && ++failures <= 30) {
            System.out.println("FAIL " + what);
        }
    }

    private static void compare(String label, String what, int[] device, int[] oracle) {
        int first = -1;
        int count = 0;
        for (int k = 0; k < oracle.length; k++) {
            if (device[k] != oracle[k]) {
                count++;
                if (first < 0) {
                    first = k;
                }
            }
        }
        check(count == 0, count == 0 ? label : String.format("%s %s: %d mismatches, first at %d: device 0x%08x oracle 0x%08x",
                label, what, count, first, device[first], oracle[first]));
    }

    private static int[] download(long buffer, int length) {
        int[] out = new int[length];
        NativeVcpu.download(buffer, 0, out, 0, length);
        return out;
    }

    private static void run(long session, String label, Rv32iBatch batch, int slices, int budget, int local) {
        CpuRv32iEngine oracle = new CpuRv32iEngine();
        int cores = batch.coreCount();
        int words = batch.memoryWordsPerCore();
        int[] memory = batch.snapshotMemory();
        int[] registers = batch.snapshotRegisters();
        int[] state = batch.snapshotState();
        long mem = NativeVcpu.alloc(session, 4L * memory.length);
        long reg = NativeVcpu.alloc(session, 4L * registers.length);
        long st = NativeVcpu.alloc(session, 4L * state.length);
        long cfg = NativeVcpu.alloc(session, 12);
        try {
            NativeVcpu.upload(mem, 0, memory, 0, memory.length);
            NativeVcpu.upload(reg, 0, registers, 0, registers.length);
            NativeVcpu.upload(st, 0, state, 0, state.length);
            for (int s = 0; s < slices; s++) {
                NativeVcpu.upload(cfg, 0, new int[] {cores, words, budget}, 0, 3);
                NativeVcpu.launch(session, NativeVcpu.RV32IM_SLICE, cores, local, new long[] {mem, reg, st, cfg});
                oracle.runSlice(batch, budget);
                String at = label + " slice " + s;
                compare(at, "memory", download(mem, memory.length), batch.snapshotMemory());
                compare(at, "registers", download(reg, registers.length), batch.snapshotRegisters());
                compare(at, "state", download(st, state.length), batch.snapshotState());
            }
        } finally {
            NativeVcpu.close(mem);
            NativeVcpu.close(reg);
            NativeVcpu.close(st);
            NativeVcpu.close(cfg);
        }
    }

    private static void directed(long session, String backend) {
        Rv32iBatch sum = new Rv32iBatch(130, 256);
        sum.loadWordsAllCores(0, Rv32iAssembler.words(
                Rv32iAssembler.addi(10, 0, 0), Rv32iAssembler.addi(11, 0, 1), Rv32iAssembler.addi(12, 0, 101),
                Rv32iAssembler.add(10, 10, 11), Rv32iAssembler.addi(11, 11, 1), Rv32iAssembler.blt(11, 12, -8),
                Rv32iAssembler.halt()));
        run(session, backend + " sum 1..100 budget 7", sum, 45, 7, 64);
        check(sum.exitCode(129) == 5050 && sum.retiredInstructions(129) == 304, backend + " oracle sum");

        Rv32iBatch m = new Rv32iBatch(4, 128);
        int[][] operands = {{Integer.MIN_VALUE, -1}, {7, 0}, {-7, 2}, {0x12345678, 0x9ABCDEF0}};
        for (int core = 0; core < 4; core++) {
            m.setRegister(core, 1, operands[core][0]);
            m.setRegister(core, 2, operands[core][1]);
            m.loadWords(core, 0, Rv32iAssembler.words(
                    Rv32iAssembler.mul(10, 1, 2), Rv32iAssembler.mulh(11, 1, 2), Rv32iAssembler.mulhsu(12, 1, 2),
                    Rv32iAssembler.mulhu(13, 1, 2), Rv32iAssembler.div(14, 1, 2), Rv32iAssembler.divu(15, 1, 2),
                    Rv32iAssembler.rem(16, 1, 2), Rv32iAssembler.remu(17, 1, 2), Rv32iAssembler.sra(18, 1, 2),
                    Rv32iAssembler.srl(19, 1, 2), Rv32iAssembler.sll(20, 1, 2), Rv32iAssembler.sltu(21, 1, 2),
                    Rv32iAssembler.slt(22, 1, 2), Rv32iAssembler.sub(23, 1, 2), Rv32iAssembler.halt()));
        }
        run(session, backend + " M extension corners", m, 1, 100, 0);

        Rv32iBatch mem = new Rv32iBatch(3, 256);
        for (int core = 0; core < 3; core++) {
            mem.setRegister(core, 1, 128 + core);
            mem.setRegister(core, 2, 0x8899AABB);
            mem.loadWords(core, 0, Rv32iAssembler.words(
                    Rv32iAssembler.sb(2, 1, 0), Rv32iAssembler.lbu(3, 1, 0), Rv32iAssembler.lb(4, 1, 0),
                    Rv32iAssembler.sh(2, 1, 2), Rv32iAssembler.lh(5, 1, 2), Rv32iAssembler.lhu(6, 1, 2),
                    Rv32iAssembler.sw(2, 1, 4), Rv32iAssembler.lw(7, 1, 4), Rv32iAssembler.ecall()));
        }
        run(session, backend + " loads/stores and alignment traps", mem, 2, 50, 1);
    }

    static int r(int f7, int rs2, int rs1, int f3, int rd, int op) {
        return (f7 << 25) | (rs2 << 20) | (rs1 << 15) | (f3 << 12) | (rd << 7) | op;
    }

    static int i(int imm, int rs1, int f3, int rd, int op) {
        return ((imm & 0xFFF) << 20) | (rs1 << 15) | (f3 << 12) | (rd << 7) | op;
    }

    static int s(int imm, int rs2, int rs1, int f3) {
        return (((imm >> 5) & 0x7F) << 25) | (rs2 << 20) | (rs1 << 15) | (f3 << 12) | ((imm & 0x1F) << 7) | 0x23;
    }

    static int b(int imm, int rs2, int rs1, int f3) {
        return (((imm >> 12) & 1) << 31) | (((imm >> 5) & 0x3F) << 25) | (rs2 << 20) | (rs1 << 15) | (f3 << 12)
                | (((imm >> 1) & 0xF) << 8) | (((imm >> 11) & 1) << 7) | 0x63;
    }

    static int j(int imm, int rd) {
        return (((imm >> 20) & 1) << 31) | (((imm >> 1) & 0x3FF) << 21) | (((imm >> 11) & 1) << 20)
                | (((imm >> 12) & 0xFF) << 12) | (rd << 7) | 0x6F;
    }

    private static int instruction(SplittableRandom rnd) {
        int k = rnd.nextInt(100);
        int rd = rnd.nextInt(32);
        int a = rnd.nextInt(32);
        int bb = rnd.nextInt(32);
        int base = 1 + rnd.nextInt(8);
        int jump = (rnd.nextInt(17) - 8) * 4 + (rnd.nextInt(8) == 0 ? 2 : 0);
        if (k < 16) {
            int[] f7s = {0, 0x20, 1, 1};
            return r(rnd.nextInt(20) == 0 ? rnd.nextInt(128) : f7s[rnd.nextInt(4)], bb, a, rnd.nextInt(8), rd, 0x33);
        }
        if (k < 30) {
            int f3 = rnd.nextInt(8);
            int imm = rnd.nextInt(4096) - 2048;
            if (f3 == 1 || f3 == 5) {
                int f7 = rnd.nextInt(10) == 0 ? rnd.nextInt(128) : (f3 == 5 && rnd.nextBoolean() ? 0x20 : 0);
                imm = (f7 << 5) | rnd.nextInt(32);
            }
            return i(imm, a, f3, rd, 0x13);
        }
        if (k < 44) {
            int[] f3s = {0, 1, 2, 4, 5};
            return i(rnd.nextInt(64) - 32, base, rnd.nextInt(10) == 0 ? rnd.nextInt(8) : f3s[rnd.nextInt(5)], rd, 0x03);
        }
        if (k < 56) {
            return s(rnd.nextInt(64) - 32, bb, base, rnd.nextInt(10) == 0 ? rnd.nextInt(8) : rnd.nextInt(3));
        }
        if (k < 68) {
            return b(jump, bb, a, rnd.nextInt(8));
        }
        if (k < 72) {
            return j(jump, rd);
        }
        if (k < 76) {
            return i(rnd.nextInt(64) - 32, rnd.nextBoolean() ? base : a, rnd.nextInt(8) == 0 ? 1 : 0, rd, 0x67);
        }
        if (k < 82) {
            return (rnd.nextInt(1 << 20) << 12) | (rd << 7) | (rnd.nextBoolean() ? 0x37 : 0x17);
        }
        if (k < 85) {
            return 0x0F | ((rnd.nextInt(4) == 0 ? rnd.nextInt(8) : rnd.nextInt(2)) << 12);
        }
        if (k < 88) {
            int[] sys = {0x00000073, 0x00100073, 0x00000473, 0x30200073};
            return sys[rnd.nextInt(4)];
        }
        if (k < 92) {
            return 0x0000000B;
        }
        return rnd.nextInt();
    }

    private static void fuzz(long session, String backend, long seed, int cores, int memoryBytes, int slices,
            int budget, int local) {
        SplittableRandom rnd = new SplittableRandom(seed);
        Rv32iBatch batch = new Rv32iBatch(cores, memoryBytes);
        int words = memoryBytes / 4;
        int[] specials = {0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE, 2};
        for (int core = 0; core < cores; core++) {
            int[] image = new int[words];
            for (int w = 0; w < words; w++) {
                image[w] = w < 64 || rnd.nextInt(4) == 0 ? instruction(rnd) : rnd.nextInt();
            }
            batch.loadWords(core, 0, image);
            for (int reg = 1; reg < 32; reg++) {
                int k = rnd.nextInt(100);
                int value = k < 30 ? rnd.nextInt(33) - 16 : k < 45 ? specials[rnd.nextInt(6)]
                        : k < 60 ? rnd.nextInt(memoryBytes) & ~3 : rnd.nextInt();
                if (reg <= 8) {
                    int a = rnd.nextInt(20);
                    value = a < 14 ? rnd.nextInt(memoryBytes - 3) & ~3
                            : a < 18 ? rnd.nextInt(memoryBytes) : a == 18 ? memoryBytes + rnd.nextInt(9) - 4
                            : -1 - rnd.nextInt(8);
                }
                batch.setRegister(core, reg, value);
            }
            if (rnd.nextInt(10) == 0) {
                batch.setProgramCounter(core, rnd.nextInt(64) * 4);
            }
        }
        run(session, backend + " fuzz seed " + seed, batch, slices, budget, local);
    }

    public static void main(String[] args) {
        NativeVcpu.load();
        for (String backend : new String[] {"cpu", "opencl:0", "opencl:1"}) {
            long session;
            try {
                session = NativeVcpu.open(backend, -1);
            } catch (UnsupportedOperationException unavailable) {
                System.out.println("SKIP " + backend + ": " + unavailable.getMessage());
                continue;
            }
            long before = failures;
            long start = System.nanoTime();
            try {
                directed(session, backend);
                fuzz(session, backend, 11, 777, 256, 6, 61, 64);
                fuzz(session, backend, 12, 513, 512, 4, 200, 0);
                fuzz(session, backend, 13, 1024, 256, 8, 17, 128);
                fuzz(session, backend, 14, 61, 1024, 3, 1000, 1);
            } finally {
                NativeVcpu.close(session);
            }
            System.out.printf("%s: oracle parity done in %d ms, failures %d%n", backend,
                    (System.nanoTime() - start) / 1_000_000, failures - before);
        }
        System.out.printf("NativeVcpuOracleParity: %d checks, %d failures -> %s%n", checks, failures,
                failures == 0 ? "PASS" : "FAIL");
        System.exit(failures == 0 ? 0 : 1);
    }
}
