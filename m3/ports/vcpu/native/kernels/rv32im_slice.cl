/* SPDX-License-Identifier: Apache-2.0 */
/*
 * rv32im_slice.cl - RV32IM virtual-CPU slice kernel, written once in "M3 kernel C".
 *
 * Semantics are those of the donor TornadoVM kernel Rv32iKernel.java
 * (hsoliwal/synexia-tornadovm m3/cpu-gpu @3bf826714a48d7fdf346ffde8a0fc25c525572c5, Apache-2.0):
 *   - one work-item = one independent virtual core; work-items with id >= CORE_COUNT return
 *   - RV32I 2.1 + M 2.0, FENCE/FENCE.I no-ops, ECALL (cause 8), EBREAK (cause 3, tval = pc),
 *     custom-0 XM3_HALT 0x0000000B (exit code = a0, retired, pc += 4, status HALTED)
 *   - x0 reads as 0; its storage slot is zeroed after every retire, at halt and at yield
 *   - status READY -> YIELDED (budget spent) | HALTED | TRAPPED; HALTED/TRAPPED cores are skipped
 *   - traps use RISC-V cause numbers; the faulting instruction is not retired and PC stays on it
 *   - INSTRUCTION_BUDGET instructions at most per launch (watchdog-friendly slicing)
 * Implementation choices that do not change observable end state: the register file and PC /
 * RETIRED are kept in private storage and written back once per launch (per-work-item VM
 * state copied in/out, pattern from GPUOS gpu-rv32ima, MIT); decode is one opcode switch whose
 * register write-back is suppressed when the instruction traps (pattern from cnlohr
 * mini-rv32ima, MIT); no donor code is copied - see PROVENANCE.md. All arithmetic is
 * done on unsigned 32/64-bit values so wrap-around matches Java int semantics without C
 * undefined behaviour (signed overflow, shifts of negative values).
 */
#ifndef M3K_PORTABILITY_H
#include "m3k_portability.h"
#endif

#define M3K_RV_REGS 32
#define M3K_RV_STATE_PC 0
#define M3K_RV_STATE_STATUS 1
#define M3K_RV_STATE_RETIRED 2
#define M3K_RV_STATE_TRAP_CAUSE 3
#define M3K_RV_STATE_TRAP_VALUE 4
#define M3K_RV_STATE_EXIT_CODE 5
#define M3K_RV_STATE_STRIDE 6
#define M3K_RV_READY 0
#define M3K_RV_HALTED 1
#define M3K_RV_TRAPPED 2
#define M3K_RV_YIELDED 3
#define M3K_RV_TRAP_NONE (-1)
#define M3K_RV_TRAP_FETCH_MISALIGNED 0
#define M3K_RV_TRAP_FETCH_FAULT 1
#define M3K_RV_TRAP_ILLEGAL 2
#define M3K_RV_TRAP_BREAKPOINT 3
#define M3K_RV_TRAP_LOAD_MISALIGNED 4
#define M3K_RV_TRAP_LOAD_FAULT 5
#define M3K_RV_TRAP_STORE_MISALIGNED 6
#define M3K_RV_TRAP_STORE_FAULT 7
#define M3K_RV_TRAP_ECALL 8
#define M3K_RV_CONFIG_CORE_COUNT 0
#define M3K_RV_CONFIG_WORDS 1
#define M3K_RV_CONFIG_BUDGET 2
#define M3K_RV_XM3_HALT 0x0000000Bu

/* Sign-extends the low `bits` bits of value (1 <= bits <= 31). */
M3_FN m3_i32 m3k_sext(m3_u32 value, m3_u32 bits)
{
    m3_u32 sign = (m3_u32)1 << (bits - 1u);
    m3_u32 field = value & ((sign << 1) - 1u);
    return (m3_i32)((field ^ sign) - sign);
}

M3_FN m3_i32 m3k_imm_i(m3_u32 in)
{
    return m3k_sext(in >> 20, 12u);
}

M3_FN m3_i32 m3k_imm_s(m3_u32 in)
{
    return m3k_sext(((in >> 25) << 5) | ((in >> 7) & 0x1Fu), 12u);
}

M3_FN m3_i32 m3k_imm_b(m3_u32 in)
{
    return m3k_sext(((in >> 31) << 12) | (((in >> 7) & 0x1u) << 11)
                    | (((in >> 25) & 0x3Fu) << 5) | (((in >> 8) & 0xFu) << 1), 13u);
}

M3_FN m3_u32 m3k_imm_u(m3_u32 in)
{
    return in & 0xFFFFF000u;
}

M3_FN m3_i32 m3k_imm_j(m3_u32 in)
{
    return m3k_sext(((in >> 31) << 20) | (((in >> 12) & 0xFFu) << 12)
                    | (((in >> 20) & 0x1u) << 11) | (((in >> 21) & 0x3FFu) << 1), 21u);
}

/* Arithmetic shift right on the two's-complement bit pattern. */
M3_FN m3_u32 m3k_sra(m3_u32 v, m3_u32 s)
{
    m3_u32 fill = (m3_u32)0 - (v >> 31);
    return (v >> s) | (fill & ~(0xFFFFFFFFu >> s));
}

/* Donor inRange: address >= 0 && address <= memoryBytes - width, with Java int wrap. */
M3_FN m3_i32 m3k_in_range(m3_i32 address, m3_i32 width, m3_i32 memory_bytes)
{
    m3_i32 limit = (m3_i32)((m3_u32)memory_bytes - (m3_u32)width);
    return address >= 0 && address <= limit;
}

M3_FN m3_u32 m3k_div(m3_u32 a, m3_u32 b)
{
    if (b == 0u) {
        return 0xFFFFFFFFu;
    }
    if (a == 0x80000000u && b == 0xFFFFFFFFu) {
        return 0x80000000u;
    }
    return (m3_u32)((m3_i32)a / (m3_i32)b);
}

M3_FN m3_u32 m3k_rem(m3_u32 a, m3_u32 b)
{
    if (b == 0u) {
        return a;
    }
    if (a == 0x80000000u && b == 0xFFFFFFFFu) {
        return 0u;
    }
    return (m3_u32)((m3_i32)a % (m3_i32)b);
}

M3_FN m3_u32 m3k_divu(m3_u32 a, m3_u32 b)
{
    return b == 0u ? 0xFFFFFFFFu : a / b;
}

M3_FN m3_u32 m3k_remu(m3_u32 a, m3_u32 b)
{
    return b == 0u ? a : a % b;
}

M3_FN m3_u32 m3k_mulh(m3_u32 a, m3_u32 b)
{
    m3_i64 p = (m3_i64)(m3_i32)a * (m3_i64)(m3_i32)b;
    return (m3_u32)((m3_u64)p >> 32);
}

M3_FN m3_u32 m3k_mulhsu(m3_u32 a, m3_u32 b)
{
    m3_i64 p = (m3_i64)(m3_i32)a * (m3_i64)b;
    return (m3_u32)((m3_u64)p >> 32);
}

M3_FN m3_u32 m3k_mulhu(m3_u32 a, m3_u32 b)
{
    return (m3_u32)(((m3_u64)a * (m3_u64)b) >> 32);
}

M3_KERNEL void m3k_rv32im_slice(M3_GID_PARAM M3_GLOBAL m3_i32 *memory, M3_GLOBAL m3_i32 *registers,
                                M3_GLOBAL m3_i32 *state, M3_GLOBAL const m3_i32 *config)
{
    m3_i32 core = M3_GID0;
    m3_i32 x[M3K_RV_REGS];
    m3_i32 words, budget, memory_bytes, status, outcome, cause, tval, exit_code, any_retired, step, r;
    m3_u32 pc, retired;
    M3_GLOBAL m3_i32 *mem;
    M3_GLOBAL m3_i32 *reg;
    M3_GLOBAL m3_i32 *st;

    if (core >= config[M3K_RV_CONFIG_CORE_COUNT]) {
        return;
    }
    words = config[M3K_RV_CONFIG_WORDS];
    budget = config[M3K_RV_CONFIG_BUDGET];
    memory_bytes = (m3_i32)((m3_u32)words << 2);
    /* Per-VM base pointers with 64-bit offsets (pattern: GPUOS rv_run_multi). */
    mem = memory + (m3_u64)(m3_u32)core * (m3_u64)(m3_u32)words;
    reg = registers + (m3_u64)(m3_u32)core * (m3_u64)M3K_RV_REGS;
    st = state + (m3_u64)(m3_u32)core * (m3_u64)M3K_RV_STATE_STRIDE;

    status = st[M3K_RV_STATE_STATUS];
    if (status == M3K_RV_HALTED || status == M3K_RV_TRAPPED) {
        return;
    }
    st[M3K_RV_STATE_STATUS] = M3K_RV_READY;
    pc = (m3_u32)st[M3K_RV_STATE_PC];
    retired = (m3_u32)st[M3K_RV_STATE_RETIRED];
    for (r = 0; r < M3K_RV_REGS; ++r) {
        x[r] = reg[r];
    }
    x[0] = 0;

    outcome = M3K_RV_YIELDED;
    cause = M3K_RV_TRAP_NONE;
    tval = 0;
    exit_code = 0;
    any_retired = 0;

    for (step = 0; step < budget; ++step) {
        m3_u32 insn, opcode, rd, f3, rs1, rs2, f7, a, b, next, value;
        m3_i32 legal, write;

        if ((pc & 3u) != 0u) {
            cause = M3K_RV_TRAP_FETCH_MISALIGNED;
            tval = (m3_i32)pc;
            break;
        }
        if (!m3k_in_range((m3_i32)pc, 4, memory_bytes)) {
            cause = M3K_RV_TRAP_FETCH_FAULT;
            tval = (m3_i32)pc;
            break;
        }
        insn = (m3_u32)mem[pc >> 2];
        if (insn == M3K_RV_XM3_HALT) {
            retired += 1u;
            pc += 4u;
            exit_code = x[10];
            outcome = M3K_RV_HALTED;
            break;
        }

        opcode = insn & 0x7Fu;
        rd = (insn >> 7) & 0x1Fu;
        f3 = (insn >> 12) & 0x7u;
        rs1 = (insn >> 15) & 0x1Fu;
        rs2 = (insn >> 20) & 0x1Fu;
        f7 = insn >> 25;
        a = (m3_u32)x[rs1];
        b = (m3_u32)x[rs2];
        next = pc + 4u;
        value = 0u;
        legal = 1;
        write = 0;

        switch (opcode) {
        case 0x37u: /* LUI */
            value = m3k_imm_u(insn);
            write = 1;
            break;
        case 0x17u: /* AUIPC */
            value = pc + m3k_imm_u(insn);
            write = 1;
            break;
        case 0x6Fu: { /* JAL */
            m3_u32 target = pc + (m3_u32)m3k_imm_j(insn);
            if ((target & 3u) != 0u) {
                cause = M3K_RV_TRAP_FETCH_MISALIGNED;
                tval = (m3_i32)target;
            } else {
                value = next;
                write = 1;
                next = target;
            }
            break;
        }
        case 0x67u: /* JALR */
            if (f3 != 0u) {
                legal = 0;
            } else {
                m3_u32 target = (a + (m3_u32)m3k_imm_i(insn)) & ~1u;
                if ((target & 3u) != 0u) {
                    cause = M3K_RV_TRAP_FETCH_MISALIGNED;
                    tval = (m3_i32)target;
                } else {
                    value = next;
                    write = 1;
                    next = target;
                }
            }
            break;
        case 0x63u: { /* BRANCH */
            m3_i32 take = 0;
            if (f3 == 0u) {
                take = a == b;
            } else if (f3 == 1u) {
                take = a != b;
            } else if (f3 == 4u) {
                take = (m3_i32)a < (m3_i32)b;
            } else if (f3 == 5u) {
                take = (m3_i32)a >= (m3_i32)b;
            } else if (f3 == 6u) {
                take = a < b;
            } else if (f3 == 7u) {
                take = a >= b;
            } else {
                legal = 0;
            }
            if (legal && take) {
                m3_u32 target = pc + (m3_u32)m3k_imm_b(insn);
                if ((target & 3u) != 0u) {
                    cause = M3K_RV_TRAP_FETCH_MISALIGNED;
                    tval = (m3_i32)target;
                } else {
                    next = target;
                }
            }
            break;
        }
        case 0x03u: { /* LOAD: LB LH LW LBU LHU */
            m3_i32 address = (m3_i32)(a + (m3_u32)m3k_imm_i(insn));
            m3_i32 width = (m3_i32)1 << (f3 & 3u);
            if (f3 == 3u || f3 > 5u) {
                legal = 0;
            } else if ((address & (width - 1)) != 0) {
                cause = M3K_RV_TRAP_LOAD_MISALIGNED;
                tval = address;
            } else if (!m3k_in_range(address, width, memory_bytes)) {
                cause = M3K_RV_TRAP_LOAD_FAULT;
                tval = address;
            } else {
                m3_u32 word = (m3_u32)mem[(m3_u32)address >> 2];
                m3_u32 lane = word >> (((m3_u32)address & 3u) << 3);
                if (f3 == 0u) {
                    value = (m3_u32)m3k_sext(lane & 0xFFu, 8u);
                } else if (f3 == 1u) {
                    value = (m3_u32)m3k_sext(lane & 0xFFFFu, 16u);
                } else if (f3 == 2u) {
                    value = word;
                } else if (f3 == 4u) {
                    value = lane & 0xFFu;
                } else {
                    value = lane & 0xFFFFu;
                }
                write = 1;
            }
            break;
        }
        case 0x23u: { /* STORE: SB SH SW */
            m3_i32 address = (m3_i32)(a + (m3_u32)m3k_imm_s(insn));
            m3_i32 width = (m3_i32)1 << (f3 & 3u);
            if (f3 > 2u) {
                legal = 0;
            } else if ((address & (width - 1)) != 0) {
                cause = M3K_RV_TRAP_STORE_MISALIGNED;
                tval = address;
            } else if (!m3k_in_range(address, width, memory_bytes)) {
                cause = M3K_RV_TRAP_STORE_FAULT;
                tval = address;
            } else {
                m3_u32 index = (m3_u32)address >> 2;
                if (f3 == 2u) {
                    mem[index] = (m3_i32)b;
                } else {
                    m3_u32 shift = ((m3_u32)address & 3u) << 3;
                    m3_u32 mask = (f3 == 0u ? 0xFFu : 0xFFFFu) << shift;
                    m3_u32 word = (m3_u32)mem[index];
                    mem[index] = (m3_i32)((word & ~mask) | ((b << shift) & mask));
                }
            }
            break;
        }
        case 0x13u: { /* OP-IMM */
            m3_u32 imm = (m3_u32)m3k_imm_i(insn);
            m3_u32 shamt = (insn >> 20) & 0x1Fu;
            write = 1;
            switch (f3) {
            case 0u:
                value = a + imm;
                break;
            case 2u:
                value = (m3_i32)a < (m3_i32)imm ? 1u : 0u;
                break;
            case 3u:
                value = a < imm ? 1u : 0u;
                break;
            case 4u:
                value = a ^ imm;
                break;
            case 6u:
                value = a | imm;
                break;
            case 7u:
                value = a & imm;
                break;
            case 1u:
                if (f7 != 0u) {
                    legal = 0;
                } else {
                    value = a << shamt;
                }
                break;
            default: /* 5: SRLI / SRAI */
                if (f7 == 0u) {
                    value = a >> shamt;
                } else if (f7 == 0x20u) {
                    value = m3k_sra(a, shamt);
                } else {
                    legal = 0;
                }
                break;
            }
            break;
        }
        case 0x33u: /* OP and M extension */
            write = 1;
            if (f7 == 0x01u) {
                switch (f3) {
                case 0u:
                    value = a * b;
                    break;
                case 1u:
                    value = m3k_mulh(a, b);
                    break;
                case 2u:
                    value = m3k_mulhsu(a, b);
                    break;
                case 3u:
                    value = m3k_mulhu(a, b);
                    break;
                case 4u:
                    value = m3k_div(a, b);
                    break;
                case 5u:
                    value = m3k_divu(a, b);
                    break;
                case 6u:
                    value = m3k_rem(a, b);
                    break;
                default:
                    value = m3k_remu(a, b);
                    break;
                }
            } else if (f7 == 0u) {
                switch (f3) {
                case 0u:
                    value = a + b;
                    break;
                case 1u:
                    value = a << (b & 0x1Fu);
                    break;
                case 2u:
                    value = (m3_i32)a < (m3_i32)b ? 1u : 0u;
                    break;
                case 3u:
                    value = a < b ? 1u : 0u;
                    break;
                case 4u:
                    value = a ^ b;
                    break;
                case 5u:
                    value = a >> (b & 0x1Fu);
                    break;
                case 6u:
                    value = a | b;
                    break;
                default:
                    value = a & b;
                    break;
                }
            } else if (f7 == 0x20u && f3 == 0u) {
                value = a - b;
            } else if (f7 == 0x20u && f3 == 5u) {
                value = m3k_sra(a, b & 0x1Fu);
            } else {
                legal = 0;
            }
            break;
        case 0x0Fu: /* MISC-MEM: FENCE, FENCE.I are no-ops */
            if (f3 > 1u) {
                legal = 0;
            }
            break;
        case 0x73u: /* SYSTEM: exact ECALL / EBREAK only */
            if (insn == 0x00000073u) {
                cause = M3K_RV_TRAP_ECALL;
                tval = 0;
            } else if (insn == 0x00100073u) {
                cause = M3K_RV_TRAP_BREAKPOINT;
                tval = (m3_i32)pc;
            } else {
                legal = 0;
            }
            break;
        default:
            legal = 0;
            break;
        }

        if (!legal) {
            cause = M3K_RV_TRAP_ILLEGAL;
            tval = (m3_i32)insn;
        }
        if (cause != M3K_RV_TRAP_NONE) {
            break;
        }
        if (write) {
            x[rd] = (m3_i32)value;
        }
        x[0] = 0;
        retired += 1u;
        pc = next;
        any_retired = 1;
    }

    for (r = 1; r < M3K_RV_REGS; ++r) {
        reg[r] = x[r];
    }
    st[M3K_RV_STATE_PC] = (m3_i32)pc;
    st[M3K_RV_STATE_RETIRED] = (m3_i32)retired;
    if (cause != M3K_RV_TRAP_NONE) {
        st[M3K_RV_STATE_STATUS] = M3K_RV_TRAPPED;
        st[M3K_RV_STATE_TRAP_CAUSE] = cause;
        st[M3K_RV_STATE_TRAP_VALUE] = tval;
        if (any_retired) {
            reg[0] = 0;
        }
    } else {
        st[M3K_RV_STATE_STATUS] = outcome;
        if (outcome == M3K_RV_HALTED) {
            st[M3K_RV_STATE_EXIT_CODE] = exit_code;
        }
        reg[0] = 0;
    }
}
