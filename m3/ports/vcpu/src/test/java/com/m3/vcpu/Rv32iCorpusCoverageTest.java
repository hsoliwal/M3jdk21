/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.vcpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.parallel.Backend;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/** Proves the differential corpus covers the full implemented ISA and that its illegal encodings really trap. */
final class Rv32iCorpusCoverageTest {
    private static final Set<String> RV32IM = Set.of(
            "LUI", "AUIPC", "JAL", "JALR", "BEQ", "BNE", "BLT", "BGE", "BLTU", "BGEU",
            "LB", "LH", "LW", "LBU", "LHU", "SB", "SH", "SW",
            "ADDI", "SLTI", "SLTIU", "XORI", "ORI", "ANDI", "SLLI", "SRLI", "SRAI",
            "ADD", "SUB", "SLL", "SLT", "SLTU", "XOR", "SRL", "SRA", "OR", "AND",
            "FENCE", "FENCE.I", "ECALL", "EBREAK",
            "MUL", "MULH", "MULHSU", "MULHU", "DIV", "DIVU", "REM", "REMU",
            "XM3_HALT");
    private static final String[] BRANCH = {"BEQ", "BNE", "", "", "BLT", "BGE", "BLTU", "BGEU"};
    private static final String[] LOAD = {"LB", "LH", "LW", "", "LBU", "LHU", "", ""};
    private static final String[] STORE = {"SB", "SH", "SW", "", "", "", "", ""};
    private static final String[] OP_IMM = {"ADDI", "", "SLTI", "SLTIU", "XORI", "", "ORI", "ANDI"};
    private static final String[] OP = {"ADD", "SLL", "SLT", "SLTU", "XOR", "SRL", "OR", "AND"};
    private static final String[] OP_M = {"MUL", "MULH", "MULHSU", "MULHU", "DIV", "DIVU", "REM", "REMU"};

    @Test
    void directedProgramsContainEveryImplementedInstruction() {
        Set<String> seen = Rv32iCorpus.directedPrograms().stream()
                .flatMapToInt(Arrays::stream)
                .mapToObj(Rv32iCorpusCoverageTest::mnemonic)
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(new TreeSet<>(RV32IM), seen);
        assertEquals(50, RV32IM.size());
    }

    @Test
    void everyIllegalEncodingTrapsWithCauseTwoOnBothEngines() {
        int[] illegal = Rv32iCorpus.illegalEncodings();
        assertTrue(illegal.length >= 20);
        Rv32iBatch expected = new Rv32iBatch(illegal.length, 4096);
        for (int core = 0; core < illegal.length; core++) {
            assertTrue(mnemonic(illegal[core]).isEmpty(), "decodes as legal: " + Integer.toHexString(illegal[core]));
            expected.loadWords(core, 0, illegal[core]);
            expected.setProgramCounter(core, 0);
        }
        Rv32iBatch actual = expected.copy();
        new CpuRv32iEngine().runSlice(expected, 4);
        try (Rv32iSession session = new Rv32iSession(actual, Backend.reference(), 8)) {
            session.runSlice(4);
            session.synchronizeAll();
        }
        for (int core = 0; core < illegal.length; core++) {
            assertEquals(Rv32i.STATUS_TRAPPED, expected.status(core));
            assertEquals(Rv32i.TRAP_ILLEGAL_INSTRUCTION, expected.trapCause(core));
            assertEquals(illegal[core], expected.trapValue(core));
            assertEquals(0, expected.retiredInstructions(core));
        }
        assertEquals(Rv32iBulkExecutor.stateRoot(expected), Rv32iBulkExecutor.stateRoot(actual));
    }

    static Optional<String> mnemonic(int word) {
        if (word == Rv32i.XM3_HALT) {
            return Optional.of("XM3_HALT");
        }
        int funct3 = word >>> 12 & 0x7;
        int funct7 = word >>> 25;
        String name = switch (word & 0x7F) {
            case 0x37 -> "LUI";
            case 0x17 -> "AUIPC";
            case 0x6F -> "JAL";
            case 0x67 -> funct3 == 0 ? "JALR" : "";
            case 0x63 -> BRANCH[funct3];
            case 0x03 -> LOAD[funct3];
            case 0x23 -> STORE[funct3];
            case 0x13 -> immediate(funct3, funct7);
            case 0x33 -> register(funct3, funct7);
            case 0x0F -> funct3 == 0 ? "FENCE" : funct3 == 1 ? "FENCE.I" : "";
            case 0x73 -> word == 0x0000_0073 ? "ECALL" : word == 0x0010_0073 ? "EBREAK" : "";
            default -> "";
        };
        return name.isEmpty() ? Optional.empty() : Optional.of(name);
    }

    private static String immediate(int funct3, int funct7) {
        if (funct3 == 1) {
            return funct7 == 0 ? "SLLI" : "";
        }
        if (funct3 == 5) {
            return funct7 == 0 ? "SRLI" : funct7 == 0x20 ? "SRAI" : "";
        }
        return OP_IMM[funct3];
    }

    private static String register(int funct3, int funct7) {
        if (funct7 == 0x01) {
            return OP_M[funct3];
        }
        if (funct7 == 0) {
            return OP[funct3];
        }
        if (funct7 == 0x20 && funct3 == 0) {
            return "SUB";
        }
        return funct7 == 0x20 && funct3 == 5 ? "SRA" : "";
    }
}
