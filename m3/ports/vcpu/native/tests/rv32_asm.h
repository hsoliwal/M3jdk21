/* SPDX-License-Identifier: Apache-2.0 */
/* rv32_asm.h - tiny RV32IM encoder for tests (field layouts per the RISC-V unprivileged spec). */
#ifndef RV32_ASM_H
#define RV32_ASM_H

#include <stdint.h>

#define RV_U_(x) ((uint32_t)(x))
/* Encoders are macros so they form constant expressions (static tables). Arguments may be
 * evaluated more than once: pass plain values, not calls with side effects. */
#define rv_r(f7, rs2, rs1, f3, rd, op)     ((RV_U_(f7) << 25) | (RV_U_(rs2) << 20) | (RV_U_(rs1) << 15) | (RV_U_(f3) << 12) | (RV_U_(rd) << 7) | RV_U_(op))
#define rv_i(imm, rs1, f3, rd, op)     (((RV_U_(imm) & 0xFFFu) << 20) | (RV_U_(rs1) << 15) | (RV_U_(f3) << 12) | (RV_U_(rd) << 7) | RV_U_(op))
#define rv_s(imm, rs2, rs1, f3)     ((((RV_U_(imm) >> 5) & 0x7Fu) << 25) | (RV_U_(rs2) << 20) | (RV_U_(rs1) << 15) | (RV_U_(f3) << 12)      | ((RV_U_(imm) & 0x1Fu) << 7) | 0x23u)
#define rv_b(imm, rs2, rs1, f3)     ((((RV_U_(imm) >> 12) & 1u) << 31) | (((RV_U_(imm) >> 5) & 0x3Fu) << 25) | (RV_U_(rs2) << 20)      | (RV_U_(rs1) << 15) | (RV_U_(f3) << 12) | (((RV_U_(imm) >> 1) & 0xFu) << 8) | (((RV_U_(imm) >> 11) & 1u) << 7)      | 0x63u)
#define rv_j(imm, rd)     ((((RV_U_(imm) >> 20) & 1u) << 31) | (((RV_U_(imm) >> 1) & 0x3FFu) << 21) | (((RV_U_(imm) >> 11) & 1u) << 20)      | (((RV_U_(imm) >> 12) & 0xFFu) << 12) | (RV_U_(rd) << 7) | 0x6Fu)
#define RV_RR(f7, f3, rd, a, b) rv_r(f7, RV_U_(b), RV_U_(a), f3, RV_U_(rd), 0x33u)
#define ADD(rd, a, b) RV_RR(0x00u, 0u, rd, a, b)
#define SUB(rd, a, b) RV_RR(0x20u, 0u, rd, a, b)
#define SLL(rd, a, b) RV_RR(0x00u, 1u, rd, a, b)
#define SLT(rd, a, b) RV_RR(0x00u, 2u, rd, a, b)
#define SLTU(rd, a, b) RV_RR(0x00u, 3u, rd, a, b)
#define XOR(rd, a, b) RV_RR(0x00u, 4u, rd, a, b)
#define SRL(rd, a, b) RV_RR(0x00u, 5u, rd, a, b)
#define SRA(rd, a, b) RV_RR(0x20u, 5u, rd, a, b)
#define OR(rd, a, b) RV_RR(0x00u, 6u, rd, a, b)
#define AND(rd, a, b) RV_RR(0x00u, 7u, rd, a, b)
#define MUL(rd, a, b) RV_RR(0x01u, 0u, rd, a, b)
#define MULH(rd, a, b) RV_RR(0x01u, 1u, rd, a, b)
#define MULHSU(rd, a, b) RV_RR(0x01u, 2u, rd, a, b)
#define MULHU(rd, a, b) RV_RR(0x01u, 3u, rd, a, b)
#define DIV(rd, a, b) RV_RR(0x01u, 4u, rd, a, b)
#define DIVU(rd, a, b) RV_RR(0x01u, 5u, rd, a, b)
#define REM(rd, a, b) RV_RR(0x01u, 6u, rd, a, b)
#define REMU(rd, a, b) RV_RR(0x01u, 7u, rd, a, b)

#define RV_OPI(f3, rd, a, imm) rv_i((int32_t)(imm), RV_U_(a), f3, RV_U_(rd), 0x13u)
#define ADDI(rd, a, imm) RV_OPI(0u, rd, a, imm)
#define SLTI(rd, a, imm) RV_OPI(2u, rd, a, imm)
#define SLTIU(rd, a, imm) RV_OPI(3u, rd, a, imm)
#define XORI(rd, a, imm) RV_OPI(4u, rd, a, imm)
#define ORI(rd, a, imm) RV_OPI(6u, rd, a, imm)
#define ANDI(rd, a, imm) RV_OPI(7u, rd, a, imm)
#define SLLI(rd, a, sh) RV_OPI(1u, rd, a, (sh) & 31)
#define SRLI(rd, a, sh) RV_OPI(5u, rd, a, (sh) & 31)
#define SRAI(rd, a, sh) RV_OPI(5u, rd, a, 0x400 | ((sh) & 31))

#define RV_LD(f3, rd, a, imm) rv_i((int32_t)(imm), RV_U_(a), f3, RV_U_(rd), 0x03u)
#define LB(rd, a, imm) RV_LD(0u, rd, a, imm)
#define LH(rd, a, imm) RV_LD(1u, rd, a, imm)
#define LW(rd, a, imm) RV_LD(2u, rd, a, imm)
#define LBU(rd, a, imm) RV_LD(4u, rd, a, imm)
#define LHU(rd, a, imm) RV_LD(5u, rd, a, imm)
#define SB(src, a, imm) rv_s((int32_t)(imm), RV_U_(src), RV_U_(a), 0u)
#define SH(src, a, imm) rv_s((int32_t)(imm), RV_U_(src), RV_U_(a), 1u)
#define SW(src, a, imm) rv_s((int32_t)(imm), RV_U_(src), RV_U_(a), 2u)

#define BEQ(a, b, off) rv_b((int32_t)(off), RV_U_(b), RV_U_(a), 0u)
#define BNE(a, b, off) rv_b((int32_t)(off), RV_U_(b), RV_U_(a), 1u)
#define BLT(a, b, off) rv_b((int32_t)(off), RV_U_(b), RV_U_(a), 4u)
#define BGE(a, b, off) rv_b((int32_t)(off), RV_U_(b), RV_U_(a), 5u)
#define BLTU(a, b, off) rv_b((int32_t)(off), RV_U_(b), RV_U_(a), 6u)
#define BGEU(a, b, off) rv_b((int32_t)(off), RV_U_(b), RV_U_(a), 7u)
#define JAL(rd, off) rv_j((int32_t)(off), RV_U_(rd))
#define JALR(rd, a, imm) rv_i((int32_t)(imm), RV_U_(a), 0u, RV_U_(rd), 0x67u)
#define LUI(rd, imm20) ((RV_U_(imm20) << 12) | (RV_U_(rd) << 7) | 0x37u)
#define AUIPC(rd, imm20) ((RV_U_(imm20) << 12) | (RV_U_(rd) << 7) | 0x17u)

#define FENCE 0x0000000Fu
#define FENCE_I 0x0000100Fu
#define ECALL 0x00000073u
#define EBREAK 0x00100073u
#define XM3_HALT 0x0000000Bu

#endif /* RV32_ASM_H */
