/* SPDX-License-Identifier: Apache-2.0 */
/*
 * rv32_ref.h - small independent C reference of the RV32IM slice semantics, written in the shape
 * of the donor Java oracle CpuRv32iEngine (state persisted after every retire, 64-bit arithmetic),
 * not in the shape of the kernel. Used only by tests. Also counts instruction / trap coverage.
 */
#ifndef RV32_REF_H
#define RV32_REF_H

#include <stdint.h>

enum {
    RV_LUI, RV_AUIPC, RV_JAL, RV_JALR, RV_BEQ, RV_BNE, RV_BLT, RV_BGE, RV_BLTU, RV_BGEU,
    RV_LB, RV_LH, RV_LW, RV_LBU, RV_LHU, RV_SB, RV_SH, RV_SW,
    RV_ADDI, RV_SLTI, RV_SLTIU, RV_XORI, RV_ORI, RV_ANDI, RV_SLLI, RV_SRLI, RV_SRAI,
    RV_ADD, RV_SUB, RV_SLL, RV_SLT, RV_SLTU, RV_XOR, RV_SRL, RV_SRA, RV_OR, RV_AND,
    RV_MUL, RV_MULH, RV_MULHSU, RV_MULHU, RV_DIV, RV_DIVU, RV_REM, RV_REMU,
    RV_FENCE, RV_FENCE_I, RV_HALT, RV_OP_COUNT
};

typedef struct rv32_coverage {
    uint64_t retired[RV_OP_COUNT];
    uint64_t traps[9];
    uint64_t taken_branches;
} rv32_coverage;

/* One slice over every core, exactly like one kernel launch with global >= cores. */
void rv32_ref_slice(int32_t *memory, int32_t *registers, int32_t *state, int32_t cores, int32_t words,
                    int32_t budget, rv32_coverage *coverage);
const char *rv32_op_name(int op);

#endif /* RV32_REF_H */
