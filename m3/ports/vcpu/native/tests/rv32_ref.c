/* SPDX-License-Identifier: Apache-2.0 */
/* rv32_ref.c - see rv32_ref.h. Semantics: donor CpuRv32iEngine.java / Rv32iKernel.java @3bf82671. */
#include "rv32_ref.h"

#include "m3vcpu_rv32im.h"

#include <stddef.h>

static const char *const rv32_names[RV_OP_COUNT] = {
    "LUI", "AUIPC", "JAL", "JALR", "BEQ", "BNE", "BLT", "BGE", "BLTU", "BGEU",
    "LB", "LH", "LW", "LBU", "LHU", "SB", "SH", "SW",
    "ADDI", "SLTI", "SLTIU", "XORI", "ORI", "ANDI", "SLLI", "SRLI", "SRAI",
    "ADD", "SUB", "SLL", "SLT", "SLTU", "XOR", "SRL", "SRA", "OR", "AND",
    "MUL", "MULH", "MULHSU", "MULHU", "DIV", "DIVU", "REM", "REMU",
    "FENCE", "FENCE.I", "XM3_HALT"};

const char *rv32_op_name(int op) { return op >= 0 && op < RV_OP_COUNT ? rv32_names[op] : "?"; }

static int32_t wrap(int64_t v) { return (int32_t)(uint32_t)(uint64_t)v; }

static int32_t sx(uint32_t v, int bits)
{
    int64_t half = (int64_t)1 << (bits - 1);
    int64_t x = (int64_t)v;
    return (int32_t)(x >= half ? x - 2 * half : x);
}

static int32_t imm_i(uint32_t in) { return sx(in >> 20, 12); }
static int32_t imm_s(uint32_t in) { return sx(((in >> 25) << 5) | ((in >> 7) & 31u), 12); }
static int32_t imm_b(uint32_t in)
{
    return sx((((in >> 31) & 1u) << 12) | (((in >> 7) & 1u) << 11) | (((in >> 25) & 63u) << 5) | (((in >> 8) & 15u) << 1),
              13);
}
static int32_t imm_j(uint32_t in)
{
    return sx((((in >> 31) & 1u) << 20) | (((in >> 12) & 255u) << 12) | (((in >> 20) & 1u) << 11)
                  | (((in >> 21) & 1023u) << 1),
              21);
}

/* high word of a signed 64-bit product, by exact division (no shifts of negative values) */
static int32_t hi(int64_t p)
{
    int64_t lo = (int64_t)(uint32_t)(uint64_t)p;
    return (int32_t)((p - lo) / 4294967296LL);
}

static int32_t sra(int32_t v, unsigned s) { return v >= 0 ? v >> s : ~((~v) >> s); }

static int32_t rreg(const int32_t *r, uint32_t i) { return i == 0u ? 0 : r[i]; }

static void wreg(int32_t *r, uint32_t i, int32_t v)
{
    if (i != 0u) {
        r[i] = v;
    }
}

static int in_range(int32_t address, int32_t width, int32_t mem_bytes)
{
    return address >= 0 && address <= wrap((int64_t)mem_bytes - width);
}

static void trap(int32_t *st, int32_t pc, int32_t cause, int32_t value, rv32_coverage *cov)
{
    st[M3VCPU_RV32_STATE_PC] = pc;
    st[M3VCPU_RV32_STATE_STATUS] = M3VCPU_RV32_STATUS_TRAPPED;
    st[M3VCPU_RV32_STATE_TRAP_CAUSE] = cause;
    st[M3VCPU_RV32_STATE_TRAP_VALUE] = value;
    if (cov != NULL && cause >= 0 && cause < 9) {
        cov->traps[cause]++;
    }
}

static uint32_t word_at(const int32_t *mem, int32_t address) { return (uint32_t)mem[address / 4]; }

static void run_core(int32_t *mem, int32_t *regs, int32_t *st, int32_t mem_bytes, int32_t budget, rv32_coverage *cov)
{
    int32_t status = st[M3VCPU_RV32_STATE_STATUS];
    int32_t pc, retired, step;
    if (status == M3VCPU_RV32_STATUS_HALTED || status == M3VCPU_RV32_STATUS_TRAPPED) {
        return;
    }
    st[M3VCPU_RV32_STATE_STATUS] = M3VCPU_RV32_STATUS_READY;
    pc = st[M3VCPU_RV32_STATE_PC];
    retired = st[M3VCPU_RV32_STATE_RETIRED];
    for (step = 0; step < budget; ++step) {
        uint32_t insn, opcode, rd, f3, rs1, rs2, f7;
        int32_t left, right, next, tval = 0, cause = -1, op = -1, address;
        int legal = 1;
        if (((uint32_t)pc & 3u) != 0u) {
            trap(st, pc, 0, pc, cov);
            return;
        }
        if (!in_range(pc, 4, mem_bytes)) {
            trap(st, pc, 1, pc, cov);
            return;
        }
        insn = word_at(mem, pc);
        if (insn == (uint32_t)M3VCPU_RV32_XM3_HALT) {
            retired = wrap((int64_t)retired + 1);
            pc = wrap((int64_t)pc + 4);
            st[M3VCPU_RV32_STATE_PC] = pc;
            st[M3VCPU_RV32_STATE_RETIRED] = retired;
            st[M3VCPU_RV32_STATE_STATUS] = M3VCPU_RV32_STATUS_HALTED;
            st[M3VCPU_RV32_STATE_EXIT_CODE] = rreg(regs, 10);
            regs[0] = 0;
            if (cov != NULL) {
                cov->retired[RV_HALT]++;
            }
            return;
        }
        opcode = insn & 0x7Fu;
        rd = (insn >> 7) & 31u;
        f3 = (insn >> 12) & 7u;
        rs1 = (insn >> 15) & 31u;
        rs2 = (insn >> 20) & 31u;
        f7 = insn >> 25;
        left = rreg(regs, rs1);
        right = rreg(regs, rs2);
        next = wrap((int64_t)pc + 4);
        switch (opcode) {
        case 0x37u:
            op = RV_LUI;
            wreg(regs, rd, (int32_t)(insn & 0xFFFFF000u));
            break;
        case 0x17u:
            op = RV_AUIPC;
            wreg(regs, rd, wrap((int64_t)pc + (int32_t)(insn & 0xFFFFF000u)));
            break;
        case 0x6Fu: {
            int32_t target = wrap((int64_t)pc + imm_j(insn));
            op = RV_JAL;
            if (((uint32_t)target & 3u) != 0u) {
                cause = 0;
                tval = target;
            } else {
                wreg(regs, rd, next);
                next = target;
            }
            break;
        }
        case 0x67u:
            op = RV_JALR;
            if (f3 != 0u) {
                legal = 0;
            } else {
                int32_t target = (int32_t)((uint32_t)wrap((int64_t)left + imm_i(insn)) & ~1u);
                if (((uint32_t)target & 3u) != 0u) {
                    cause = 0;
                    tval = target;
                } else {
                    wreg(regs, rd, next);
                    next = target;
                }
            }
            break;
        case 0x63u: {
            int take = 0;
            switch (f3) {
            case 0u:
                op = RV_BEQ;
                take = left == right;
                break;
            case 1u:
                op = RV_BNE;
                take = left != right;
                break;
            case 4u:
                op = RV_BLT;
                take = left < right;
                break;
            case 5u:
                op = RV_BGE;
                take = left >= right;
                break;
            case 6u:
                op = RV_BLTU;
                take = (uint32_t)left < (uint32_t)right;
                break;
            case 7u:
                op = RV_BGEU;
                take = (uint32_t)left >= (uint32_t)right;
                break;
            default:
                legal = 0;
                break;
            }
            if (legal && take) {
                int32_t target = wrap((int64_t)pc + imm_b(insn));
                if (((uint32_t)target & 3u) != 0u) {
                    cause = 0;
                    tval = target;
                } else {
                    next = target;
                    if (cov != NULL) {
                        cov->taken_branches++;
                    }
                }
            }
            break;
        }
        case 0x03u: {
            int32_t width = f3 == 0u || f3 == 4u ? 1 : (f3 == 1u || f3 == 5u ? 2 : 4);
            address = wrap((int64_t)left + imm_i(insn));
            if (f3 == 3u || f3 > 5u) {
                legal = 0;
            } else if (width > 1 && ((uint32_t)address % (uint32_t)width) != 0u) {
                cause = 4;
                tval = address;
            } else if (!in_range(address, width, mem_bytes)) {
                cause = 5;
                tval = address;
            } else {
                uint32_t word = word_at(mem, address);
                uint32_t lane = word >> (8u * ((uint32_t)address % 4u));
                switch (f3) {
                case 0u:
                    op = RV_LB;
                    wreg(regs, rd, sx(lane & 0xFFu, 8));
                    break;
                case 1u:
                    op = RV_LH;
                    wreg(regs, rd, sx(lane & 0xFFFFu, 16));
                    break;
                case 2u:
                    op = RV_LW;
                    wreg(regs, rd, (int32_t)word);
                    break;
                case 4u:
                    op = RV_LBU;
                    wreg(regs, rd, (int32_t)(lane & 0xFFu));
                    break;
                default:
                    op = RV_LHU;
                    wreg(regs, rd, (int32_t)(lane & 0xFFFFu));
                    break;
                }
            }
            break;
        }
        case 0x23u: {
            int32_t width = f3 == 0u ? 1 : (f3 == 1u ? 2 : 4);
            address = wrap((int64_t)left + imm_s(insn));
            if (f3 > 2u) {
                legal = 0;
            } else if (width > 1 && ((uint32_t)address % (uint32_t)width) != 0u) {
                cause = 6;
                tval = address;
            } else if (!in_range(address, width, mem_bytes)) {
                cause = 7;
                tval = address;
            } else {
                int32_t index = address / 4;
                uint32_t shift = 8u * ((uint32_t)address % 4u);
                uint32_t mask = (width == 1 ? 0xFFu : (width == 2 ? 0xFFFFu : 0xFFFFFFFFu)) << shift;
                uint32_t old = (uint32_t)mem[index];
                mem[index] = (int32_t)((old & ~mask) | (((uint32_t)right << shift) & mask));
                op = width == 1 ? RV_SB : (width == 2 ? RV_SH : RV_SW);
            }
            break;
        }
        case 0x13u: {
            int32_t imm = imm_i(insn);
            unsigned shamt = (unsigned)((insn >> 20) & 31u);
            switch (f3) {
            case 0u:
                op = RV_ADDI;
                wreg(regs, rd, wrap((int64_t)left + imm));
                break;
            case 2u:
                op = RV_SLTI;
                wreg(regs, rd, left < imm ? 1 : 0);
                break;
            case 3u:
                op = RV_SLTIU;
                wreg(regs, rd, (uint32_t)left < (uint32_t)imm ? 1 : 0);
                break;
            case 4u:
                op = RV_XORI;
                wreg(regs, rd, left ^ imm);
                break;
            case 6u:
                op = RV_ORI;
                wreg(regs, rd, left | imm);
                break;
            case 7u:
                op = RV_ANDI;
                wreg(regs, rd, left & imm);
                break;
            case 1u:
                if (f7 != 0u) {
                    legal = 0;
                } else {
                    op = RV_SLLI;
                    wreg(regs, rd, (int32_t)((uint32_t)left << shamt));
                }
                break;
            default:
                if (f7 == 0u) {
                    op = RV_SRLI;
                    wreg(regs, rd, (int32_t)((uint32_t)left >> shamt));
                } else if (f7 == 0x20u) {
                    op = RV_SRAI;
                    wreg(regs, rd, sra(left, shamt));
                } else {
                    legal = 0;
                }
                break;
            }
            break;
        }
        case 0x33u: {
            unsigned s = (unsigned)((uint32_t)right & 31u);
            uint32_t ul = (uint32_t)left;
            uint32_t ur = (uint32_t)right;
            if (f7 == 1u) {
                switch (f3) {
                case 0u:
                    op = RV_MUL;
                    wreg(regs, rd, wrap((int64_t)left * right));
                    break;
                case 1u:
                    op = RV_MULH;
                    wreg(regs, rd, hi((int64_t)left * right));
                    break;
                case 2u:
                    op = RV_MULHSU;
                    wreg(regs, rd, hi((int64_t)left * (int64_t)ur));
                    break;
                case 3u:
                    op = RV_MULHU;
                    wreg(regs, rd, (int32_t)(uint32_t)(((uint64_t)ul * ur) >> 32));
                    break;
                case 4u:
                    op = RV_DIV;
                    wreg(regs, rd, right == 0 ? -1 : wrap((int64_t)left / right));
                    break;
                case 5u:
                    op = RV_DIVU;
                    wreg(regs, rd, ur == 0u ? -1 : (int32_t)(ul / ur));
                    break;
                case 6u:
                    op = RV_REM;
                    wreg(regs, rd, right == 0 ? left : wrap((int64_t)left % right));
                    break;
                default:
                    op = RV_REMU;
                    wreg(regs, rd, ur == 0u ? left : (int32_t)(ul % ur));
                    break;
                }
            } else if (f7 == 0u || (f7 == 0x20u && (f3 == 0u || f3 == 5u))) {
                switch (f3) {
                case 0u:
                    op = f7 == 0u ? RV_ADD : RV_SUB;
                    wreg(regs, rd, f7 == 0u ? wrap((int64_t)left + right) : wrap((int64_t)left - right));
                    break;
                case 1u:
                    op = RV_SLL;
                    wreg(regs, rd, (int32_t)(ul << s));
                    break;
                case 2u:
                    op = RV_SLT;
                    wreg(regs, rd, left < right ? 1 : 0);
                    break;
                case 3u:
                    op = RV_SLTU;
                    wreg(regs, rd, ul < ur ? 1 : 0);
                    break;
                case 4u:
                    op = RV_XOR;
                    wreg(regs, rd, left ^ right);
                    break;
                case 5u:
                    op = f7 == 0u ? RV_SRL : RV_SRA;
                    wreg(regs, rd, f7 == 0u ? (int32_t)(ul >> s) : sra(left, s));
                    break;
                case 6u:
                    op = RV_OR;
                    wreg(regs, rd, left | right);
                    break;
                default:
                    op = RV_AND;
                    wreg(regs, rd, left & right);
                    break;
                }
            } else {
                legal = 0;
            }
            break;
        }
        case 0x0Fu:
            if (f3 == 0u) {
                op = RV_FENCE;
            } else if (f3 == 1u) {
                op = RV_FENCE_I;
            } else {
                legal = 0;
            }
            break;
        case 0x73u:
            if (insn == 0x00000073u) {
                cause = 8;
            } else if (insn == 0x00100073u) {
                cause = 3;
                tval = pc;
            } else {
                legal = 0;
            }
            break;
        default:
            legal = 0;
            break;
        }
        if (!legal) {
            trap(st, pc, 2, (int32_t)insn, cov);
            return;
        }
        if (cause != -1) {
            trap(st, pc, cause, tval, cov);
            return;
        }
        retired = wrap((int64_t)retired + 1);
        pc = next;
        regs[0] = 0;
        st[M3VCPU_RV32_STATE_PC] = pc;
        st[M3VCPU_RV32_STATE_RETIRED] = retired;
        if (cov != NULL && op >= 0) {
            cov->retired[op]++;
        }
    }
    st[M3VCPU_RV32_STATE_PC] = pc;
    st[M3VCPU_RV32_STATE_RETIRED] = retired;
    st[M3VCPU_RV32_STATE_STATUS] = M3VCPU_RV32_STATUS_YIELDED;
    regs[0] = 0;
}

void rv32_ref_slice(int32_t *memory, int32_t *registers, int32_t *state, int32_t cores, int32_t words,
                    int32_t budget, rv32_coverage *coverage)
{
    int32_t core;
    int32_t mem_bytes = wrap((int64_t)words * 4);
    for (core = 0; core < cores; ++core) {
        run_core(memory + (size_t)core * (size_t)words, registers + (size_t)core * M3VCPU_RV32_REGISTER_COUNT,
                 state + (size_t)core * M3VCPU_RV32_STATE_STRIDE, mem_bytes, budget, coverage);
    }
}
