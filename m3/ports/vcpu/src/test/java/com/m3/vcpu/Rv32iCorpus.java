/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.vcpu;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic full-ISA corpus for differential and golden-receipt tests.
 *
 * <p>Every core gets its own seeded registers and data RAM and runs one program: a directed program per instruction
 * class (OP-IMM, OP, M, loads/stores, branches/jumps/LUI/AUIPC, FENCE, ECALL, EBREAK, every illegal-encoding family,
 * misaligned and out-of-range fetch/load/store/jump targets, XM3_HALT, a sliced loop, self-modifying code) or a random
 * straight-line program. Only the public {@link Rv32iBatch} and {@link Rv32iAssembler} API is used, so the same
 * source also compiles against the donor classes to produce golden receipts.</p>
 */
final class Rv32iCorpus {
    static final int MEMORY_BYTES = 4096;
    static final int DATA_BASE = 2048;
    static final long SEED = 0x5EED_0F_A11L;
    static final int[] SLICE_BUDGETS = {1, 2, 5, 17, 64, 256, 1000};

    private static final int RANDOM_LENGTH = 48;
    private static final int[] ILLEGAL = {
            0xFFFF_FFFF,
            0x0000_0000,
            0x40 << 25 | 6 << 20 | 5 << 15 | 10 << 7 | 0x33,
            0x20 << 25 | 6 << 20 | 5 << 15 | 1 << 12 | 10 << 7 | 0x33,
            0x02 << 25 | 6 << 20 | 5 << 15 | 10 << 7 | 0x33,
            5 << 15 | 1 << 12 | 1 << 7 | 0x67,
            20 << 15 | 3 << 12 | 10 << 7 | 0x03,
            20 << 15 | 6 << 12 | 10 << 7 | 0x03,
            20 << 15 | 7 << 12 | 10 << 7 | 0x03,
            5 << 20 | 20 << 15 | 3 << 12 | 0x23,
            6 << 20 | 5 << 15 | 2 << 12 | 8 << 7 | 0x63,
            6 << 20 | 5 << 15 | 3 << 12 | 8 << 7 | 0x63,
            0x0000_1073,
            0x3020_0073,
            0x1050_0073,
            0x0000_200F,
            1 << 25 | 3 << 20 | 5 << 15 | 1 << 12 | 10 << 7 | 0x13,
            0x10 << 25 | 3 << 20 | 5 << 15 | 5 << 12 | 10 << 7 | 0x13,
            0x0000_008B,
            0x0000_100B,
            0x0000_202F,
            0x0000_2007,
            0x0000_4501,
    };
    private static final int[] CORNERS = {
            0, 1, -1, 2, -2, 3, 7, 31, 32, 33, 0x80, 0xFF, 0x7FF, 0x800, 0xFFFF, 0x1_0000,
            Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE + 1, 0x5555_5555, 0xAAAA_AAAA,
    };
    private static final List<int[]> DIRECTED = directed();

    private Rv32iCorpus() {
    }

    /** Number of program slots: directed programs plus as many random-program slots. */
    static int programSlots() {
        return DIRECTED.size() * 2;
    }

    static List<int[]> directedPrograms() {
        return DIRECTED;
    }

    static int[] illegalEncodings() {
        return ILLEGAL.clone();
    }

    static Rv32iBatch batch(int cores, long seed) {
        Rv32iBatch batch = new Rv32iBatch(cores, MEMORY_BYTES);
        for (int core = 0; core < cores; core++) {
            Rng rng = new Rng(seed * 0x9E37_79B9_7F4A_7C15L + core);
            for (int register = 1; register < 32; register++) {
                batch.setRegister(core, register,
                        rng.below(2) == 0 ? CORNERS[rng.below(CORNERS.length)] : rng.next());
            }
            for (int address = DATA_BASE; address < MEMORY_BYTES; address += 4) {
                batch.writeWord(core, address, rng.next());
            }
            int slot = core % programSlots();
            int[] program = slot < DIRECTED.size() ? DIRECTED.get(slot) : random(rng, core);
            batch.loadWords(core, 0, program);
            batch.setProgramCounter(core, 0);
        }
        return batch;
    }

    static int[] random(Rng rng, int core) {
        List<Integer> words = new ArrayList<>();
        words.add(Rv32iAssembler.addi(20, 0, 2047));
        words.add(Rv32iAssembler.addi(20, 20, 1));
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            words.add(randomInstruction(rng, words.size(), RANDOM_LENGTH + 2, core));
        }
        words.add(Rv32iAssembler.halt());
        return words.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int randomInstruction(Rng rng, int index, int haltIndex, int core) {
        int rd = rng.below(31);
        rd = rd >= 20 ? rd + 1 : rd;
        int rs1 = rng.below(32);
        int rs2 = rng.below(32);
        int imm = rng.below(4096) - 2048;
        int shift = rng.below(32);
        int kind = rng.below(64);
        return switch (kind) {
            case 0 -> Rv32iAssembler.addi(rd, rs1, imm);
            case 1 -> Rv32iAssembler.slti(rd, rs1, imm);
            case 2 -> Rv32iAssembler.sltiu(rd, rs1, imm);
            case 3 -> Rv32iAssembler.xori(rd, rs1, imm);
            case 4 -> Rv32iAssembler.ori(rd, rs1, imm);
            case 5 -> Rv32iAssembler.andi(rd, rs1, imm);
            case 6 -> Rv32iAssembler.slli(rd, rs1, shift);
            case 7 -> Rv32iAssembler.srli(rd, rs1, shift);
            case 8 -> Rv32iAssembler.srai(rd, rs1, shift);
            case 9 -> Rv32iAssembler.add(rd, rs1, rs2);
            case 10 -> Rv32iAssembler.sub(rd, rs1, rs2);
            case 11 -> Rv32iAssembler.sll(rd, rs1, rs2);
            case 12 -> Rv32iAssembler.slt(rd, rs1, rs2);
            case 13 -> Rv32iAssembler.sltu(rd, rs1, rs2);
            case 14 -> Rv32iAssembler.xor(rd, rs1, rs2);
            case 15 -> Rv32iAssembler.srl(rd, rs1, rs2);
            case 16 -> Rv32iAssembler.sra(rd, rs1, rs2);
            case 17 -> Rv32iAssembler.or(rd, rs1, rs2);
            case 18 -> Rv32iAssembler.and(rd, rs1, rs2);
            case 19 -> Rv32iAssembler.mul(rd, rs1, rs2);
            case 20 -> Rv32iAssembler.mulh(rd, rs1, rs2);
            case 21 -> Rv32iAssembler.mulhsu(rd, rs1, rs2);
            case 22 -> Rv32iAssembler.mulhu(rd, rs1, rs2);
            case 23 -> Rv32iAssembler.div(rd, rs1, rs2);
            case 24 -> Rv32iAssembler.divu(rd, rs1, rs2);
            case 25 -> Rv32iAssembler.rem(rd, rs1, rs2);
            case 26 -> Rv32iAssembler.remu(rd, rs1, rs2);
            case 27 -> Rv32iAssembler.lui(rd, rng.below(1 << 20));
            case 28 -> Rv32iAssembler.auipc(rd, rng.below(1 << 20));
            case 29, 30 -> Rv32iAssembler.lw(rd, 20, dataOffset(rng));
            case 31 -> Rv32iAssembler.lh(rd, 20, dataOffset(rng));
            case 32 -> Rv32iAssembler.lhu(rd, 20, dataOffset(rng));
            case 33 -> Rv32iAssembler.lb(rd, 20, dataOffset(rng));
            case 34 -> Rv32iAssembler.lbu(rd, 20, dataOffset(rng));
            case 35, 36 -> Rv32iAssembler.sw(rs2, 20, dataOffset(rng));
            case 37 -> Rv32iAssembler.sh(rs2, 20, dataOffset(rng));
            case 38 -> Rv32iAssembler.sb(rs2, 20, dataOffset(rng));
            case 39 -> Rv32iAssembler.beq(rs1, rs2, forward(rng, index, haltIndex));
            case 40 -> Rv32iAssembler.bne(rs1, rs2, forward(rng, index, haltIndex));
            case 41 -> Rv32iAssembler.blt(rs1, rs2, forward(rng, index, haltIndex));
            case 42 -> Rv32iAssembler.bge(rs1, rs2, forward(rng, index, haltIndex));
            case 43 -> Rv32iAssembler.bltu(rs1, rs2, forward(rng, index, haltIndex));
            case 44 -> Rv32iAssembler.bgeu(rs1, rs2, forward(rng, index, haltIndex));
            case 45 -> Rv32iAssembler.jal(rd, forward(rng, index, haltIndex));
            case 46 -> Rv32iAssembler.jalr(rd, 0, (index << 2) + forward(rng, index, haltIndex) + rng.below(2));
            case 47 -> Rv32iAssembler.fence();
            case 48 -> Rv32iAssembler.fenceI();
            case 49 -> rare(rng, core);
            default -> Rv32iAssembler.addi(rd, rs1, imm);
        };
    }

    private static int rare(Rng rng, int core) {
        return switch (rng.below(8)) {
            case 0 -> Rv32iAssembler.ecall();
            case 1 -> Rv32iAssembler.ebreak();
            case 2 -> Rv32iAssembler.halt();
            case 3 -> Rv32iAssembler.beq(0, 0, 2);
            default -> ILLEGAL[(core + rng.below(ILLEGAL.length)) % ILLEGAL.length];
        };
    }

    private static int dataOffset(Rng rng) {
        int offset = rng.below(DATA_BASE / 4) << 2;
        if (rng.below(8) == 0) {
            offset += rng.below(4);
        }
        if (rng.below(16) == 0) {
            offset = -rng.below(DATA_BASE);
        }
        return Math.min(offset, 2047);
    }

    private static int forward(Rng rng, int index, int haltIndex) {
        int maxSkip = Math.min(4, haltIndex - index);
        return (1 + rng.below(Math.max(1, maxSkip))) << 2;
    }

    private static List<int[]> directed() {
        List<int[]> programs = new ArrayList<>();
        programs.add(aluImmediate());
        programs.add(aluRegister());
        programs.add(multiplyDivide());
        programs.add(loadsAndStores());
        programs.add(branchesAndJumps());
        programs.add(Rv32iAssembler.words(Rv32iAssembler.fence(), Rv32iAssembler.fenceI(),
                Rv32iAssembler.addi(10, 0, 5), Rv32iAssembler.ecall(), Rv32iAssembler.halt()));
        programs.add(Rv32iAssembler.words(Rv32iAssembler.addi(10, 0, 6), Rv32iAssembler.ebreak(),
                Rv32iAssembler.halt()));
        for (int illegal : ILLEGAL) {
            programs.add(Rv32iAssembler.words(Rv32iAssembler.addi(10, 0, 1), illegal, Rv32iAssembler.halt()));
        }
        programs.addAll(faults());
        programs.add(Rv32iAssembler.words(
                Rv32iAssembler.addi(1, 0, 0),
                Rv32iAssembler.addi(2, 0, 1),
                Rv32iAssembler.addi(3, 0, 101),
                Rv32iAssembler.add(1, 1, 2),
                Rv32iAssembler.addi(2, 2, 1),
                Rv32iAssembler.blt(2, 3, -8),
                Rv32iAssembler.addi(10, 1, 0),
                Rv32iAssembler.halt()));
        programs.add(selfModifying());
        return List.copyOf(programs);
    }

    private static int[] aluImmediate() {
        return Rv32iAssembler.words(
                Rv32iAssembler.addi(10, 5, -2048),
                Rv32iAssembler.addi(11, 6, 2047),
                Rv32iAssembler.slti(12, 5, -1),
                Rv32iAssembler.slti(13, 6, 0),
                Rv32iAssembler.sltiu(14, 5, -1),
                Rv32iAssembler.sltiu(15, 7, 1),
                Rv32iAssembler.xori(16, 5, -1),
                Rv32iAssembler.xori(17, 6, 0x555),
                Rv32iAssembler.ori(18, 7, -2048),
                Rv32iAssembler.ori(19, 8, 0x0F0),
                Rv32iAssembler.andi(20, 5, 0x7FF),
                Rv32iAssembler.andi(21, 6, -16),
                Rv32iAssembler.slli(22, 5, 0),
                Rv32iAssembler.slli(23, 6, 31),
                Rv32iAssembler.slli(24, 7, 13),
                Rv32iAssembler.srli(25, 5, 1),
                Rv32iAssembler.srli(26, 6, 31),
                Rv32iAssembler.srli(27, 7, 0),
                Rv32iAssembler.srai(28, 5, 31),
                Rv32iAssembler.srai(29, 6, 7),
                Rv32iAssembler.srai(30, 7, 0),
                Rv32iAssembler.addi(0, 5, 1),
                Rv32iAssembler.slli(0, 6, 3),
                Rv32iAssembler.add(31, 0, 0),
                Rv32iAssembler.xor(10, 10, 16),
                Rv32iAssembler.halt());
    }

    private static int[] aluRegister() {
        return Rv32iAssembler.words(
                Rv32iAssembler.add(10, 5, 6),
                Rv32iAssembler.sub(11, 5, 6),
                Rv32iAssembler.sll(12, 5, 6),
                Rv32iAssembler.slt(13, 5, 6),
                Rv32iAssembler.sltu(14, 5, 6),
                Rv32iAssembler.xor(15, 5, 6),
                Rv32iAssembler.srl(16, 5, 6),
                Rv32iAssembler.sra(17, 5, 6),
                Rv32iAssembler.or(18, 5, 6),
                Rv32iAssembler.and(19, 5, 6),
                Rv32iAssembler.add(20, 7, 8),
                Rv32iAssembler.sub(21, 8, 7),
                Rv32iAssembler.sll(22, 9, 7),
                Rv32iAssembler.slt(23, 9, 8),
                Rv32iAssembler.sltu(24, 8, 9),
                Rv32iAssembler.srl(25, 7, 9),
                Rv32iAssembler.sra(26, 9, 5),
                Rv32iAssembler.sra(27, 6, 6),
                Rv32iAssembler.add(0, 5, 6),
                Rv32iAssembler.sub(28, 0, 5),
                Rv32iAssembler.slt(29, 0, 5),
                Rv32iAssembler.sltu(30, 0, 5),
                Rv32iAssembler.or(31, 0, 0),
                Rv32iAssembler.xor(10, 10, 11),
                Rv32iAssembler.halt());
    }

    private static int[] multiplyDivide() {
        return Rv32iAssembler.words(
                Rv32iAssembler.mul(10, 5, 6),
                Rv32iAssembler.mulh(11, 5, 6),
                Rv32iAssembler.mulhsu(12, 5, 6),
                Rv32iAssembler.mulhu(13, 5, 6),
                Rv32iAssembler.div(14, 5, 6),
                Rv32iAssembler.divu(15, 5, 6),
                Rv32iAssembler.rem(16, 5, 6),
                Rv32iAssembler.remu(17, 5, 6),
                Rv32iAssembler.mul(18, 7, 8),
                Rv32iAssembler.mulh(19, 8, 9),
                Rv32iAssembler.mulhsu(21, 9, 7),
                Rv32iAssembler.mulhu(22, 7, 9),
                Rv32iAssembler.div(23, 8, 7),
                Rv32iAssembler.divu(24, 9, 8),
                Rv32iAssembler.rem(25, 7, 9),
                Rv32iAssembler.remu(26, 8, 5),
                Rv32iAssembler.lui(27, 0x80000),
                Rv32iAssembler.addi(28, 0, -1),
                Rv32iAssembler.div(29, 27, 28),
                Rv32iAssembler.rem(30, 27, 28),
                Rv32iAssembler.div(31, 5, 0),
                Rv32iAssembler.rem(20, 5, 0),
                Rv32iAssembler.divu(1, 6, 0),
                Rv32iAssembler.remu(2, 6, 0),
                Rv32iAssembler.mulhsu(3, 27, 28),
                Rv32iAssembler.mulhu(4, 28, 28),
                Rv32iAssembler.mulh(9, 27, 27),
                Rv32iAssembler.divu(8, 27, 28),
                Rv32iAssembler.remu(7, 28, 27),
                Rv32iAssembler.xor(10, 10, 14),
                Rv32iAssembler.halt());
    }

    private static int[] loadsAndStores() {
        return Rv32iAssembler.words(
                Rv32iAssembler.addi(20, 0, 2047),
                Rv32iAssembler.addi(20, 20, 1),
                Rv32iAssembler.sw(5, 20, 0),
                Rv32iAssembler.sh(6, 20, 4),
                Rv32iAssembler.sh(7, 20, 6),
                Rv32iAssembler.sb(8, 20, 8),
                Rv32iAssembler.sb(9, 20, 9),
                Rv32iAssembler.sb(5, 20, 10),
                Rv32iAssembler.sb(6, 20, 11),
                Rv32iAssembler.lw(10, 20, 0),
                Rv32iAssembler.lh(11, 20, 4),
                Rv32iAssembler.lhu(12, 20, 6),
                Rv32iAssembler.lb(13, 20, 8),
                Rv32iAssembler.lbu(14, 20, 9),
                Rv32iAssembler.lb(15, 20, 11),
                Rv32iAssembler.lbu(16, 20, 10),
                Rv32iAssembler.lw(17, 20, 12),
                Rv32iAssembler.lh(18, 20, 14),
                Rv32iAssembler.lhu(19, 20, 16),
                Rv32iAssembler.lb(21, 20, 19),
                Rv32iAssembler.lbu(22, 20, 17),
                Rv32iAssembler.addi(23, 20, 64),
                Rv32iAssembler.sw(6, 23, -4),
                Rv32iAssembler.lw(24, 23, -4),
                Rv32iAssembler.lw(25, 20, 2044),
                Rv32iAssembler.sb(5, 20, 2047),
                Rv32iAssembler.lbu(26, 20, 2047),
                Rv32iAssembler.sh(6, 20, 2046),
                Rv32iAssembler.lh(27, 20, 2046),
                Rv32iAssembler.lhu(28, 20, 2),
                Rv32iAssembler.lb(29, 20, 3),
                Rv32iAssembler.sh(0, 20, 0),
                Rv32iAssembler.lw(30, 20, 0),
                Rv32iAssembler.lw(0, 20, 0),
                Rv32iAssembler.xor(10, 10, 30),
                Rv32iAssembler.halt());
    }

    private static int[] branchesAndJumps() {
        return Rv32iAssembler.words(
                Rv32iAssembler.addi(10, 0, 0),
                Rv32iAssembler.beq(5, 5, 8),
                Rv32iAssembler.addi(10, 10, 1),
                Rv32iAssembler.bne(5, 6, 8),
                Rv32iAssembler.addi(10, 10, 2),
                Rv32iAssembler.blt(5, 6, 8),
                Rv32iAssembler.addi(10, 10, 4),
                Rv32iAssembler.bge(5, 6, 8),
                Rv32iAssembler.addi(10, 10, 8),
                Rv32iAssembler.bltu(5, 6, 8),
                Rv32iAssembler.addi(10, 10, 16),
                Rv32iAssembler.bgeu(5, 6, 8),
                Rv32iAssembler.addi(10, 10, 32),
                Rv32iAssembler.beq(0, 7, 8),
                Rv32iAssembler.addi(10, 10, 64),
                Rv32iAssembler.bne(8, 9, 8),
                Rv32iAssembler.addi(10, 10, 128),
                Rv32iAssembler.blt(9, 8, 8),
                Rv32iAssembler.addi(10, 10, 256),
                Rv32iAssembler.bgeu(9, 8, 8),
                Rv32iAssembler.addi(10, 10, 512),
                Rv32iAssembler.jal(1, 8),
                Rv32iAssembler.addi(10, 10, 1024),
                Rv32iAssembler.auipc(11, 0x12345),
                Rv32iAssembler.lui(12, 0xFFFFF),
                Rv32iAssembler.auipc(13, 0),
                Rv32iAssembler.jalr(14, 13, 12),
                Rv32iAssembler.addi(10, 10, 2047),
                Rv32iAssembler.addi(15, 0, 5),
                Rv32iAssembler.addi(15, 15, -1),
                Rv32iAssembler.bne(15, 0, -4),
                Rv32iAssembler.auipc(16, 0),
                Rv32iAssembler.jalr(0, 16, 13),
                Rv32iAssembler.addi(10, 10, 1000),
                Rv32iAssembler.jal(0, 8),
                Rv32iAssembler.addi(10, 10, 999),
                Rv32iAssembler.bltu(0, 0, 8),
                Rv32iAssembler.bgeu(0, 0, 8),
                Rv32iAssembler.addi(10, 10, 777),
                Rv32iAssembler.bne(0, 0, 6),
                Rv32iAssembler.halt());
    }

    private static List<int[]> faults() {
        return List.of(
                Rv32iAssembler.words(Rv32iAssembler.addi(10, 0, 1), Rv32iAssembler.jal(1, 2),
                        Rv32iAssembler.halt()),
                Rv32iAssembler.words(Rv32iAssembler.addi(10, 0, 1), Rv32iAssembler.beq(0, 0, 6),
                        Rv32iAssembler.halt()),
                Rv32iAssembler.words(Rv32iAssembler.addi(6, 0, 2), Rv32iAssembler.jalr(1, 6, 0),
                        Rv32iAssembler.halt()),
                Rv32iAssembler.words(Rv32iAssembler.addi(6, 0, 2047), Rv32iAssembler.addi(6, 6, 2047),
                        Rv32iAssembler.addi(6, 6, 2), Rv32iAssembler.jalr(0, 6, 0), Rv32iAssembler.halt()),
                Rv32iAssembler.words(Rv32iAssembler.addi(6, 0, -4), Rv32iAssembler.jalr(1, 6, 0),
                        Rv32iAssembler.halt()),
                Rv32iAssembler.words(Rv32iAssembler.addi(20, 0, 2047), Rv32iAssembler.addi(20, 20, 2),
                        Rv32iAssembler.lh(10, 20, 0), Rv32iAssembler.halt()),
                Rv32iAssembler.words(Rv32iAssembler.addi(20, 0, 2047), Rv32iAssembler.addi(20, 20, 3),
                        Rv32iAssembler.lw(10, 20, 0), Rv32iAssembler.halt()),
                Rv32iAssembler.words(Rv32iAssembler.addi(20, 0, 2047), Rv32iAssembler.addi(20, 20, 2),
                        Rv32iAssembler.lhu(10, 20, 0), Rv32iAssembler.halt()),
                Rv32iAssembler.words(Rv32iAssembler.lw(10, 0, -4), Rv32iAssembler.halt()),
                Rv32iAssembler.words(Rv32iAssembler.lbu(10, 0, -1), Rv32iAssembler.halt()),
                Rv32iAssembler.words(Rv32iAssembler.addi(6, 0, 2047), Rv32iAssembler.addi(6, 6, 2047),
                        Rv32iAssembler.addi(6, 6, 2), Rv32iAssembler.lw(10, 6, 0), Rv32iAssembler.halt()),
                Rv32iAssembler.words(Rv32iAssembler.addi(6, 0, 2047), Rv32iAssembler.addi(6, 6, 2047),
                        Rv32iAssembler.lb(10, 6, 2), Rv32iAssembler.halt()),
                Rv32iAssembler.words(Rv32iAssembler.addi(20, 0, 2047), Rv32iAssembler.sh(5, 20, 0),
                        Rv32iAssembler.halt()),
                Rv32iAssembler.words(Rv32iAssembler.sw(5, 0, 2), Rv32iAssembler.halt()),
                Rv32iAssembler.words(Rv32iAssembler.sw(5, 0, -4), Rv32iAssembler.halt()),
                Rv32iAssembler.words(Rv32iAssembler.sb(5, 0, -1), Rv32iAssembler.halt()),
                Rv32iAssembler.words(Rv32iAssembler.addi(6, 0, 2047), Rv32iAssembler.addi(6, 6, 2047),
                        Rv32iAssembler.sh(5, 6, 2), Rv32iAssembler.halt()));
    }

    private static int[] selfModifying() {
        return Rv32iAssembler.words(
                Rv32iAssembler.lui(5, 0x02A00),
                Rv32iAssembler.addi(5, 5, 0x513),
                Rv32iAssembler.sw(5, 0, 20),
                Rv32iAssembler.fenceI(),
                Rv32iAssembler.fence(),
                Rv32iAssembler.addi(10, 0, 1),
                Rv32iAssembler.halt());
    }

    /** SplitMix64; deterministic across JDKs. */
    static final class Rng {
        private long state;

        Rng(long seed) {
            this.state = seed;
        }

        int next() {
            state += 0x9E37_79B9_7F4A_7C15L;
            long z = state;
            z = (z ^ (z >>> 30)) * 0xBF58_476D_1CE4_E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D0_49BB_1331_11EBL;
            return (int) ((z ^ (z >>> 31)) >>> 32);
        }

        int below(int bound) {
            return (int) ((next() & 0xFFFF_FFFFL) % bound);
        }
    }
}
