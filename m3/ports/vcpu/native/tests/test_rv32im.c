/* SPDX-License-Identifier: Apache-2.0 */
/*
 * test_rv32im.c - RV32IM differential suite. Runs the built-in m3k_rv32im_slice kernel on a
 * backend ("cpu": every ISA variant and thread count; "opencl": every device) and compares the
 * complete memory / register / state arrays with the C reference after every slice, plus
 * hand-computed expectations. Exit 77 = backend unavailable (ctest skip).
 *   m3vcpu_test_rv32im [cpu|opencl|<backend>]
 */
#include "m3vcpu.h"
#include "m3vcpu_check.h"
#include "m3vcpu_rv32im.h"
#include "rv32_asm.h"
#include "rv32_ref.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define NREG M3VCPU_RV32_REGISTER_COUNT
#define NST M3VCPU_RV32_STATE_STRIDE
#define MIN32 ((int32_t)0x80000000u)

typedef struct batch {
    int32_t cores, words;
    int32_t *mem, *regs, *state;    /* device mirror */
    int32_t *rmem, *rregs, *rstate; /* reference */
    m3vcpu_buf *buf[4];
} batch;

static m3vcpu_session *g_s;
static m3vcpu_kernel *g_k;
static uint32_t g_local = 64;
static uint64_t g_global_extra;
static rv32_coverage g_cov;
static char g_label[160];
static uint64_t g_rng;

/* ---- batch plumbing -------------------------------------------------------------------- */
static size_t n_mem(const batch *b) { return (size_t)b->cores * (size_t)b->words; }
static size_t n_reg(const batch *b) { return (size_t)b->cores * NREG; }
static size_t n_st(const batch *b) { return (size_t)b->cores * NST; }

static batch *batch_new(int32_t cores, int32_t words)
{
    batch *b = (batch *)calloc(1, sizeof *b);
    int32_t c;
    b->cores = cores;
    b->words = words;
    b->mem = (int32_t *)calloc(n_mem(b) + 1u, sizeof(int32_t));
    b->regs = (int32_t *)calloc(n_reg(b), sizeof(int32_t));
    b->state = (int32_t *)calloc(n_st(b), sizeof(int32_t));
    b->rmem = (int32_t *)calloc(n_mem(b) + 1u, sizeof(int32_t));
    b->rregs = (int32_t *)calloc(n_reg(b), sizeof(int32_t));
    b->rstate = (int32_t *)calloc(n_st(b), sizeof(int32_t));
    for (c = 0; c < cores; ++c) {
        b->state[(size_t)c * NST + M3VCPU_RV32_STATE_TRAP_CAUSE] = M3VCPU_RV32_TRAP_NONE;
    }
    return b;
}

static void batch_free(batch *b)
{
    int i;
    for (i = 0; i < 4; ++i) {
        m3vcpu_buffer_free(g_s, b->buf[i]);
    }
    free(b->mem);
    free(b->regs);
    free(b->state);
    free(b->rmem);
    free(b->rregs);
    free(b->rstate);
    free(b);
}

static int32_t *REG(batch *b, int32_t core) { return b->regs + (size_t)core * NREG; }
static int32_t *ST(batch *b, int32_t core) { return b->state + (size_t)core * NST; }

static void load(batch *b, int32_t core, int32_t byte_address, const uint32_t *words, size_t n)
{
    size_t i;
    for (i = 0; i < n; ++i) {
        b->mem[(size_t)core * (size_t)b->words + (size_t)byte_address / 4u + i] = (int32_t)words[i];
    }
}

static void upload(batch *b)
{
    size_t sizes[4];
    int i;
    sizes[0] = n_mem(b) * 4u;
    sizes[1] = n_reg(b) * 4u;
    sizes[2] = n_st(b) * 4u;
    sizes[3] = M3VCPU_RV32_CONFIG_STRIDE * 4u;
    for (i = 0; i < 4; ++i) {
        int32_t rc = m3vcpu_buffer_alloc(g_s, sizes[i], 0, &b->buf[i]);
        CHECK(rc == M3VCPU_OK, "%s alloc %d: %s", g_label, i, m3vcpu_last_error(g_s));
    }
    CHECK(m3vcpu_buffer_write(g_s, b->buf[0], 0, b->mem, sizes[0]) == M3VCPU_OK, "write mem");
    CHECK(m3vcpu_buffer_write(g_s, b->buf[1], 0, b->regs, sizes[1]) == M3VCPU_OK, "write regs");
    CHECK(m3vcpu_buffer_write(g_s, b->buf[2], 0, b->state, sizes[2]) == M3VCPU_OK, "write state");
    memcpy(b->rmem, b->mem, sizes[0]);
    memcpy(b->rregs, b->regs, sizes[1]);
    memcpy(b->rstate, b->state, sizes[2]);
}

static void compare(const char *what, const int32_t *dev, const int32_t *ref, size_t n, size_t stride)
{
    size_t i;
    int shown = 0;
    long bad = 0;
    for (i = 0; i < n; ++i) {
        if (dev[i] != ref[i]) {
            ++bad;
            if (shown++ < 3) {
                CHECK(0, "%s %s[core %zu, slot %zu]: device 0x%08x reference 0x%08x", g_label, what, i / stride,
                      i % stride, (unsigned)dev[i], (unsigned)ref[i]);
            }
        }
    }
    CHECK(bad == 0, "%s %s: %ld mismatching words", g_label, what, bad);
}

/* One launch on the device and one reference slice, then full-state comparison. */
static void slice(batch *b, int32_t budget)
{
    int32_t config[M3VCPU_RV32_CONFIG_STRIDE];
    m3vcpu_arg args[4];
    int32_t rc;
    int i;
    config[M3VCPU_RV32_CONFIG_CORE_COUNT] = b->cores;
    config[M3VCPU_RV32_CONFIG_MEMORY_WORDS_PER_CORE] = b->words;
    config[M3VCPU_RV32_CONFIG_INSTRUCTION_BUDGET] = budget;
    CHECK(m3vcpu_buffer_write(g_s, b->buf[3], 0, config, sizeof config) == M3VCPU_OK, "write config");
    for (i = 0; i < 4; ++i) {
        args[i].kind = M3VCPU_ARG_BUFFER;
        args[i].i32 = 0;
        args[i].buf = b->buf[i];
    }
    rc = m3vcpu_launch_1d(g_s, g_k, (uint64_t)b->cores + g_global_extra, g_local, args, 4);
    CHECK(rc == M3VCPU_OK, "%s launch: %s", g_label, m3vcpu_last_error(g_s));
    CHECK(m3vcpu_finish(g_s) == M3VCPU_OK, "%s finish: %s", g_label, m3vcpu_last_error(g_s));
    CHECK(m3vcpu_buffer_read(g_s, b->buf[0], 0, b->mem, n_mem(b) * 4u) == M3VCPU_OK, "read mem");
    CHECK(m3vcpu_buffer_read(g_s, b->buf[1], 0, b->regs, n_reg(b) * 4u) == M3VCPU_OK, "read regs");
    CHECK(m3vcpu_buffer_read(g_s, b->buf[2], 0, b->state, n_st(b) * 4u) == M3VCPU_OK, "read state");
    rv32_ref_slice(b->rmem, b->rregs, b->rstate, b->cores, b->words, budget, &g_cov);
    compare("memory", b->mem, b->rmem, n_mem(b), (size_t)b->words);
    compare("registers", b->regs, b->rregs, n_reg(b), NREG);
    compare("state", b->state, b->rstate, n_st(b), NST);
}

static void expect_reg(batch *b, int32_t core, int r, uint32_t want, const char *what)
{
    CHECK((uint32_t)REG(b, core)[r] == want, "%s %s: core %d x%d = 0x%08x, want 0x%08x", g_label, what, core, r,
          (unsigned)REG(b, core)[r], (unsigned)want);
}

static void expect_state(batch *b, int32_t core, int32_t status, int32_t cause, int32_t tval, int32_t pc,
                         int32_t retired, const char *what)
{
    int32_t *st = ST(b, core);
    CHECK(st[M3VCPU_RV32_STATE_STATUS] == status, "%s %s: core %d status %d want %d", g_label, what, core,
          st[M3VCPU_RV32_STATE_STATUS], status);
    if (status == M3VCPU_RV32_STATUS_TRAPPED) {
        CHECK(st[M3VCPU_RV32_STATE_TRAP_CAUSE] == cause && st[M3VCPU_RV32_STATE_TRAP_VALUE] == tval,
              "%s %s: core %d trap (%d, 0x%08x) want (%d, 0x%08x)", g_label, what, core,
              st[M3VCPU_RV32_STATE_TRAP_CAUSE], (unsigned)st[M3VCPU_RV32_STATE_TRAP_VALUE], cause, (unsigned)tval);
    }
    CHECK(st[M3VCPU_RV32_STATE_PC] == pc, "%s %s: core %d pc %d want %d", g_label, what, core,
          st[M3VCPU_RV32_STATE_PC], pc);
    CHECK(st[M3VCPU_RV32_STATE_RETIRED] == retired, "%s %s: core %d retired %d want %d", g_label, what, core,
          st[M3VCPU_RV32_STATE_RETIRED], retired);
}

static void expect_exit(batch *b, int32_t core, int32_t code, const char *what)
{
    CHECK(ST(b, core)[M3VCPU_RV32_STATE_STATUS] == M3VCPU_RV32_STATUS_HALTED
              && ST(b, core)[M3VCPU_RV32_STATE_EXIT_CODE] == code,
          "%s %s: core %d status %d exit %d want HALTED %d", g_label, what, core,
          ST(b, core)[M3VCPU_RV32_STATE_STATUS], ST(b, core)[M3VCPU_RV32_STATE_EXIT_CODE], code);
}

/* li rd, value as LUI + ADDI. */
static void li(uint32_t *out, uint32_t rd, int32_t value)
{
    uint32_t v = (uint32_t)value;
    uint32_t lo = v & 0xFFFu;
    uint32_t hi20 = ((v + ((lo & 0x800u) << 1)) >> 12) & 0xFFFFFu;
    out[0] = LUI(rd, hi20);
    out[1] = ADDI(rd, rd, (int32_t)(lo ^ 0x800u) - 0x800);
}

/* ---- directed cases ----------------------------------------------------------------------- */
static void test_alu(void)
{
    static const uint32_t prog[] = {
        ADD(10, 1, 2),       SUB(11, 5, 2),    SLL(12, 2, 4),        SRL(13, 3, 4),    SRA(14, 3, 4),
        SLT(15, 3, 2),       SLTU(16, 3, 2),   XOR(17, 1, 3),        OR(18, 1, 3),     AND(19, 1, 3),
        ADDI(20, 0, -2048),  SLTI(21, 3, -7),  SLTIU(22, 2, -1),     XORI(23, 3, -1),  ORI(24, 0, 0x7FF),
        ANDI(25, 3, 0xF0),   SLLI(26, 2, 31),  SRLI(27, 5, 31),      SRAI(28, 5, 31),  LUI(29, 0xFFFFF),
        AUIPC(30, 1),        ADD(0, 1, 1),     ADDI(31, 0, 1),       XM3_HALT};
    batch *b = batch_new(3, 64);
    int32_t c;
    for (c = 0; c < 3; ++c) {
        load(b, c, 0, prog, sizeof prog / sizeof prog[0]);
    }
    REG(b, 0)[1] = 0x7FFFFFFF;
    REG(b, 0)[2] = 1;
    REG(b, 0)[3] = -8;
    REG(b, 0)[4] = 33;
    REG(b, 0)[5] = MIN32;
    REG(b, 1)[1] = -1;
    REG(b, 1)[2] = 0x7FFFFFFF;
    REG(b, 1)[3] = 12345;
    REG(b, 1)[4] = -1;
    REG(b, 1)[5] = 31;
    REG(b, 2)[1] = 0x5A5A5A5A;
    REG(b, 2)[2] = -0x5A5A5A5A;
    REG(b, 2)[3] = 0;
    REG(b, 2)[4] = 0;
    REG(b, 2)[5] = -1;
    upload(b);
    slice(b, 100);
    expect_reg(b, 0, 10, 0x80000000u, "ADD overflow wraps");
    expect_reg(b, 0, 11, 0x7FFFFFFFu, "SUB underflow wraps");
    expect_reg(b, 0, 12, 2u, "SLL shamt masked to 5 bits");
    expect_reg(b, 0, 13, 0x7FFFFFFCu, "SRL");
    expect_reg(b, 0, 14, 0xFFFFFFFCu, "SRA");
    expect_reg(b, 0, 15, 1u, "SLT");
    expect_reg(b, 0, 16, 0u, "SLTU");
    expect_reg(b, 0, 17, 0x80000007u, "XOR");
    expect_reg(b, 0, 18, 0xFFFFFFFFu, "OR");
    expect_reg(b, 0, 19, 0x7FFFFFF8u, "AND");
    expect_reg(b, 0, 20, 0xFFFFF800u, "ADDI -2048");
    expect_reg(b, 0, 21, 1u, "SLTI");
    expect_reg(b, 0, 22, 1u, "SLTIU sign-extended imm");
    expect_reg(b, 0, 23, 7u, "XORI -1");
    expect_reg(b, 0, 24, 0x7FFu, "ORI");
    expect_reg(b, 0, 25, 0xF0u, "ANDI");
    expect_reg(b, 0, 26, 0x80000000u, "SLLI 31");
    expect_reg(b, 0, 27, 1u, "SRLI 31");
    expect_reg(b, 0, 28, 0xFFFFFFFFu, "SRAI 31");
    expect_reg(b, 0, 29, 0xFFFFF000u, "LUI");
    expect_reg(b, 0, 30, 0x1050u, "AUIPC");
    expect_reg(b, 0, 31, 1u, "ADDI");
    expect_reg(b, 0, 0, 0u, "x0 hard-wired");
    expect_exit(b, 0, MIN32, "ALU exit a0");
    expect_state(b, 0, M3VCPU_RV32_STATUS_HALTED, 0, 0, 96, 24, "ALU halt");
    batch_free(b);
}

static void test_mext(void)
{
    static const uint32_t prog[] = {
        DIV(10, 1, 2),    REM(11, 1, 2),    DIV(12, 4, 3),    DIVU(13, 4, 3),   REM(14, 4, 3),
        REMU(15, 5, 3),   DIV(16, 5, 6),    REM(17, 5, 6),    DIVU(18, 5, 6),   REMU(19, 5, 6),
        MUL(20, 8, 9),    MULH(21, 8, 9),   MULHSU(22, 8, 9), MULHU(23, 8, 9),  MULH(24, 1, 1),
        MULHU(25, 7, 7),  MULHSU(26, 2, 7), MUL(27, 1, 2),    DIVU(28, 1, 2),   REMU(29, 1, 2),
        MULHSU(30, 9, 8), XM3_HALT};
    batch *b = batch_new(2, 64);
    int32_t c;
    for (c = 0; c < 2; ++c) {
        load(b, c, 0, prog, sizeof prog / sizeof prog[0]);
        REG(b, c)[1] = MIN32;
        REG(b, c)[2] = -1;
        REG(b, c)[3] = 0;
        REG(b, c)[4] = 7;
        REG(b, c)[5] = -7;
        REG(b, c)[6] = 2;
        REG(b, c)[7] = -1;
        REG(b, c)[8] = 0x12345678;
        REG(b, c)[9] = (int32_t)0x9ABCDEF0u;
    }
    REG(b, 1)[3] = 3; /* second core: no zero divisor */
    upload(b);
    slice(b, 100);
    expect_reg(b, 0, 10, 0x80000000u, "DIV MIN/-1 overflow");
    expect_reg(b, 0, 11, 0u, "REM MIN/-1 overflow");
    expect_reg(b, 0, 12, 0xFFFFFFFFu, "DIV by zero");
    expect_reg(b, 0, 13, 0xFFFFFFFFu, "DIVU by zero");
    expect_reg(b, 0, 14, 7u, "REM by zero = dividend");
    expect_reg(b, 0, 15, 0xFFFFFFF9u, "REMU by zero = dividend");
    expect_reg(b, 0, 16, 0xFFFFFFFDu, "DIV truncates toward zero");
    expect_reg(b, 0, 17, 0xFFFFFFFFu, "REM sign follows dividend");
    expect_reg(b, 0, 18, 0x7FFFFFFCu, "DIVU");
    expect_reg(b, 0, 19, 1u, "REMU");
    expect_reg(b, 0, 20, 0x242D2080u, "MUL");
    expect_reg(b, 0, 21, 0xF8CC93D6u, "MULH");
    expect_reg(b, 0, 22, 0x0B00EA4Eu, "MULHSU");
    expect_reg(b, 0, 23, 0x0B00EA4Eu, "MULHU");
    expect_reg(b, 0, 24, 0x40000000u, "MULH MIN*MIN");
    expect_reg(b, 0, 25, 0xFFFFFFFEu, "MULHU max*max");
    expect_reg(b, 0, 26, 0xFFFFFFFFu, "MULHSU -1 * 0xFFFFFFFF");
    expect_reg(b, 0, 27, 0x80000000u, "MUL MIN*-1");
    expect_reg(b, 0, 28, 0u, "DIVU 0x80000000/0xFFFFFFFF");
    expect_reg(b, 0, 29, 0x80000000u, "REMU 0x80000000%0xFFFFFFFF");
    expect_reg(b, 0, 30, 0xF8CC93D6u, "MULHSU negative rs1");
    batch_free(b);
}

static void test_mem(void)
{
    static const uint32_t ok[] = {SW(2, 1, 0),  LB(10, 1, 0),  LBU(11, 1, 0), LB(12, 1, 3),  LH(13, 1, 2),
                                  LHU(14, 1, 0), LW(15, 1, 0), SB(3, 1, 1),   SH(3, 1, 6),   LW(16, 1, 0),
                                  LW(17, 1, 4),  LBU(18, 4, 0), LHU(19, 5, 0), LW(20, 6, 0),  XM3_HALT};
    static const struct {
        uint32_t insn;
        int32_t cause, tval;
    } traps[] = {
        {LH(10, 1, 1), 4, 129},  {SW(2, 1, 2), 6, 130},  {LW(10, 7, 0), 5, 256}, {SB(2, 0, -1), 7, -1},
        {LW(10, 8, 0), 5, -4},   {LHU(10, 4, 0), 4, 255}, {SH(2, 5, 2), 7, 256}, {LBU(10, 7, 0), 5, 256},
        {SH(2, 1, 1), 6, 129},   {LW(10, 1, 2), 4, 130}, {SW(2, 8, 0), 7, -4},  {LB(10, 8, 3), 5, -1},
    };
    int32_t n = (int32_t)(sizeof traps / sizeof traps[0]);
    batch *b = batch_new(1 + n, 64);
    int32_t c;
    load(b, 0, 0, ok, sizeof ok / sizeof ok[0]);
    for (c = 0; c <= n; ++c) {
        REG(b, c)[1] = 128;
        REG(b, c)[2] = (int32_t)0x8899AABBu;
        REG(b, c)[3] = 0x123;
        REG(b, c)[4] = 255;
        REG(b, c)[5] = 254;
        REG(b, c)[6] = 252;
        REG(b, c)[7] = 256;
        REG(b, c)[8] = -4;
        b->mem[(size_t)c * 64u + 63u] = (int32_t)0xDEADBEEFu;
        if (c > 0) {
            uint32_t p[3];
            p[0] = ADDI(31, 0, 1);
            p[1] = traps[c - 1].insn;
            p[2] = XM3_HALT;
            load(b, c, 0, p, 3);
        }
    }
    upload(b);
    slice(b, 100);
    expect_reg(b, 0, 10, 0xFFFFFFBBu, "LB sign-extends");
    expect_reg(b, 0, 11, 0xBBu, "LBU");
    expect_reg(b, 0, 12, 0xFFFFFF88u, "LB byte 3");
    expect_reg(b, 0, 13, 0xFFFF8899u, "LH upper half");
    expect_reg(b, 0, 14, 0xAABBu, "LHU");
    expect_reg(b, 0, 15, 0x8899AABBu, "LW");
    expect_reg(b, 0, 16, 0x889923BBu, "SB read-modify-write");
    expect_reg(b, 0, 17, 0x01230000u, "SH upper half");
    expect_reg(b, 0, 18, 0xDEu, "LBU last byte of RAM");
    expect_reg(b, 0, 19, 0xDEADu, "LHU last half of RAM");
    expect_reg(b, 0, 20, 0xDEADBEEFu, "LW last word of RAM");
    CHECK((uint32_t)b->mem[32] == 0x889923BBu && (uint32_t)b->mem[33] == 0x01230000u,
          "%s little-endian packing: 0x%08x 0x%08x", g_label, (unsigned)b->mem[32], (unsigned)b->mem[33]);
    for (c = 1; c <= n; ++c) {
        expect_state(b, c, M3VCPU_RV32_STATUS_TRAPPED, traps[c - 1].cause, traps[c - 1].tval, 4, 1, "memory trap");
        expect_reg(b, c, 10, 0u, "faulting load does not write rd");
    }
    batch_free(b);
}

static void test_branches(void)
{
    static const uint32_t sum[] = {ADDI(10, 0, 0),    ADDI(11, 0, 1),   ADDI(12, 0, 101), ADD(10, 10, 11),
                                   ADDI(11, 11, 1),   BLT(11, 12, -8),  XM3_HALT};
    static const uint32_t mask[] = {BEQ(1, 2, 8),  ADDI(10, 10, 1),  BNE(1, 2, 8),  ADDI(10, 10, 2),  BLT(1, 2, 8),
                                    ADDI(10, 10, 4), BGE(1, 2, 8),   ADDI(10, 10, 8), BLTU(1, 2, 8),  ADDI(10, 10, 16),
                                    BGEU(1, 2, 8), ADDI(10, 10, 32), XM3_HALT};
    static const uint32_t jumps[] = {JAL(1, 16), ADDI(10, 10, 1000), XM3_HALT,         XM3_HALT, ADDI(5, 0, 29),
                                     JALR(6, 5, 0), XM3_HALT,        ADDI(10, 10, 7), XM3_HALT};
    static const uint32_t back[] = {ADDI(10, 0, 3), ADDI(10, 10, -1), BNE(10, 0, -4), XM3_HALT};
    static const uint32_t negimm[] = {ADDI(5, 0, 40), JALR(1, 5, -8), XM3_HALT, XM3_HALT, XM3_HALT,
                                      XM3_HALT,       XM3_HALT,       XM3_HALT, ADDI(10, 0, 55), XM3_HALT};
    batch *b = batch_new(10, 64);
    uint32_t p;
    load(b, 0, 0, sum, 7);
    load(b, 1, 0, mask, 13);
    load(b, 2, 0, mask, 13);
    load(b, 3, 0, mask, 13);
    REG(b, 1)[1] = -1;
    REG(b, 1)[2] = 1;
    REG(b, 2)[1] = 1;
    REG(b, 2)[2] = 1;
    REG(b, 3)[1] = 1;
    REG(b, 3)[2] = -1;
    load(b, 4, 0, jumps, 9);
    p = JALR(0, 7, 0);
    load(b, 5, 0, &p, 1);
    REG(b, 5)[7] = 6;
    p = JAL(0, 2);
    load(b, 6, 0, &p, 1);
    p = BEQ(0, 0, 6);
    load(b, 7, 0, &p, 1);
    load(b, 8, 0, back, 4);
    load(b, 9, 0, negimm, 10);
    upload(b);
    slice(b, 1000);
    expect_exit(b, 0, 5050, "sum 1..100");
    expect_state(b, 0, M3VCPU_RV32_STATUS_HALTED, 0, 0, 28, 304, "sum 1..100");
    expect_exit(b, 1, 25, "branch matrix (-1, 1)");
    expect_exit(b, 2, 22, "branch matrix (1, 1)");
    expect_exit(b, 3, 37, "branch matrix (1, -1)");
    expect_exit(b, 4, 7, "JAL/JALR");
    expect_reg(b, 4, 1, 4u, "JAL link");
    expect_reg(b, 4, 6, 24u, "JALR link, target bit 0 cleared");
    expect_state(b, 4, M3VCPU_RV32_STATUS_HALTED, 0, 0, 36, 5, "JAL/JALR");
    expect_state(b, 5, M3VCPU_RV32_STATUS_TRAPPED, 0, 6, 0, 0, "JALR misaligned target");
    expect_state(b, 6, M3VCPU_RV32_STATUS_TRAPPED, 0, 2, 0, 0, "JAL misaligned target");
    expect_state(b, 7, M3VCPU_RV32_STATUS_TRAPPED, 0, 6, 0, 0, "branch misaligned target");
    expect_exit(b, 8, 0, "backward branch loop");
    expect_state(b, 8, M3VCPU_RV32_STATUS_HALTED, 0, 0, 16, 8, "backward branch loop");
    expect_exit(b, 9, 55, "JALR negative immediate");
    expect_reg(b, 9, 1, 8u, "JALR negative immediate link");
    batch_free(b);
}

static void test_halt(void)
{
    static const int32_t codes[] = {0, 77, -1, 0x7FFFFFFF, MIN32, 2047, -2048, 0x12345678};
    int32_t n = (int32_t)(sizeof codes / sizeof codes[0]);
    batch *b = batch_new(n + 1, 16);
    int32_t c;
    int32_t before[NST * 16];
    for (c = 0; c < n; ++c) {
        uint32_t p[3];
        li(p, 10, codes[c]);
        p[2] = XM3_HALT;
        load(b, c, 0, p, 3);
    }
    {
        uint32_t h = XM3_HALT;
        load(b, n, 0, &h, 1);
        REG(b, n)[10] = -99;
    }
    upload(b);
    slice(b, 10);
    for (c = 0; c < n; ++c) {
        expect_exit(b, c, codes[c], "XM3_HALT exit code");
        expect_state(b, c, M3VCPU_RV32_STATUS_HALTED, 0, 0, 12, 3, "XM3_HALT state");
    }
    expect_exit(b, n, -99, "XM3_HALT as first instruction");
    expect_state(b, n, M3VCPU_RV32_STATUS_HALTED, 0, 0, 4, 1, "XM3_HALT first");
    memcpy(before, b->state, n_st(b) * 4u);
    slice(b, 10);
    CHECK(memcmp(before, b->state, n_st(b) * 4u) == 0, "%s halted cores are skipped by the next launch", g_label);
    batch_free(b);
}

static void test_traps(void)
{
    static const struct {
        uint32_t insn;
        int32_t cause;
    } cases[] = {
        {0xFFFFFFFFu, 2},
        {0x0000100Bu, 2},
        {ECALL, 8},
        {0x0000200Fu, 2},
        {0x34011073u, 2},
        {0x02009513u, 2},          /* SLLI with funct7 = 1 */
        {0x2000D513u, 2},          /* SRLI/SRAI with funct7 = 0x10 */
        {0x40001533u, 2},          /* OP funct7 0x20 funct3 1 */
        {0x00009567u, 2},          /* JALR funct3 1 */
        {0x00002463u, 2},          /* BRANCH funct3 2 */
        {0x00003503u, 2},          /* LOAD funct3 3 */
        {0x00006503u, 2},          /* LOAD funct3 6 */
        {0x00003023u, 2},          /* STORE funct3 3 */
        {0x04000533u, 2},          /* OP funct7 2 */
        {0x00000000u, 2},
        {0x00000473u, 2},          /* SYSTEM, not exact ECALL */
        {0x00200073u, 2},          /* URET-like encoding: illegal here */
    };
    int32_t n = (int32_t)(sizeof cases / sizeof cases[0]);
    int32_t extra = 5;
    batch *b = batch_new(n + extra, 64);
    int32_t c;
    for (c = 0; c < n; ++c) {
        load(b, c, 0, &cases[c].insn, 1);
    }
    {
        uint32_t eb[2];
        uint32_t fence[3];
        eb[0] = ADDI(31, 0, 1);
        eb[1] = EBREAK;
        fence[0] = FENCE;
        fence[1] = FENCE_I;
        fence[2] = XM3_HALT;
        load(b, n, 0, eb, 2);
        load(b, n + 1, 0, fence, 3);
    }
    ST(b, n + 2)[M3VCPU_RV32_STATE_PC] = 2;
    ST(b, n + 3)[M3VCPU_RV32_STATE_PC] = 256;
    ST(b, n + 4)[M3VCPU_RV32_STATE_PC] = -4;
    upload(b);
    slice(b, 10);
    for (c = 0; c < n; ++c) {
        expect_state(b, c, M3VCPU_RV32_STATUS_TRAPPED, cases[c].cause,
                     cases[c].cause == 8 ? 0 : (int32_t)cases[c].insn, 0, 0, "trap");
    }
    expect_state(b, n, M3VCPU_RV32_STATUS_TRAPPED, 3, 4, 4, 1, "EBREAK tval = pc");
    expect_state(b, n + 1, M3VCPU_RV32_STATUS_HALTED, 0, 0, 12, 3, "FENCE / FENCE.I are no-ops");
    expect_state(b, n + 2, M3VCPU_RV32_STATUS_TRAPPED, 0, 2, 2, 0, "fetch misaligned");
    expect_state(b, n + 3, M3VCPU_RV32_STATUS_TRAPPED, 1, 256, 256, 0, "fetch past RAM");
    expect_state(b, n + 4, M3VCPU_RV32_STATUS_TRAPPED, 1, -4, -4, 0, "fetch negative pc");
    batch_free(b);
}

static void test_slicing(void)
{
    static const uint32_t sum[] = {ADDI(10, 0, 0),  ADDI(11, 0, 1),  ADDI(12, 0, 101), ADD(10, 10, 11),
                                   ADDI(11, 11, 1), BLT(11, 12, -8), XM3_HALT};
    batch *b = batch_new(5, 32);
    int32_t c, launches = 0;
    for (c = 0; c < 5; ++c) {
        load(b, c, 0, sum, 7);
    }
    upload(b);
    while (ST(b, 0)[M3VCPU_RV32_STATE_STATUS] != M3VCPU_RV32_STATUS_HALTED && launches < 100) {
        slice(b, 7);
        ++launches;
        if (ST(b, 0)[M3VCPU_RV32_STATE_STATUS] == M3VCPU_RV32_STATUS_YIELDED) {
            CHECK(ST(b, 0)[M3VCPU_RV32_STATE_RETIRED] == 7 * launches, "%s slice %d retired %d", g_label, launches,
                  ST(b, 0)[M3VCPU_RV32_STATE_RETIRED]);
        }
    }
    CHECK(launches == 44, "%s budget-7 slices to halt: %d (want 44)", g_label, launches);
    expect_exit(b, 0, 5050, "sliced sum");
    expect_state(b, 0, M3VCPU_RV32_STATUS_HALTED, 0, 0, 28, 304, "sliced sum");
    batch_free(b);

    b = batch_new(2, 32);
    load(b, 0, 0, sum, 7);
    load(b, 1, 0, sum, 7);
    upload(b);
    for (c = 0; c < 12; ++c) {
        slice(b, 1);
    }
    expect_state(b, 0, M3VCPU_RV32_STATUS_YIELDED, 0, 0, 12, 12, "budget-1 slices");
    batch_free(b);
}

static void test_x0_and_status(void)
{
    batch *b = batch_new(8, 32);
    uint32_t p[4];
    p[0] = 0xFFFFFFFFu;
    load(b, 0, 0, p, 1);
    REG(b, 0)[0] = 0x1234;
    p[0] = ADDI(0, 0, 5);
    p[1] = ADD(10, 0, 0);
    p[2] = XM3_HALT;
    load(b, 1, 0, p, 3);
    REG(b, 1)[0] = 0x1234;
    p[0] = ADDI(10, 0, 9);
    p[1] = XM3_HALT;
    load(b, 2, 0, p, 2);
    ST(b, 2)[M3VCPU_RV32_STATE_STATUS] = 7;
    load(b, 3, 0, p, 2);
    ST(b, 3)[M3VCPU_RV32_STATE_STATUS] = M3VCPU_RV32_STATUS_TRAPPED;
    ST(b, 3)[M3VCPU_RV32_STATE_TRAP_CAUSE] = 5;
    load(b, 4, 0, p, 2);
    ST(b, 4)[M3VCPU_RV32_STATE_STATUS] = M3VCPU_RV32_STATUS_HALTED;
    p[0] = 0;
    p[1] = ADDI(10, 0, 1);
    p[2] = XM3_HALT;
    load(b, 5, 0, p, 3);
    ST(b, 5)[M3VCPU_RV32_STATE_STATUS] = M3VCPU_RV32_STATUS_YIELDED;
    ST(b, 5)[M3VCPU_RV32_STATE_PC] = 4;
    ST(b, 5)[M3VCPU_RV32_STATE_RETIRED] = 100;
    p[0] = ADDI(10, 0, 1);
    p[1] = 0xFFFFFFFFu;
    load(b, 6, 0, p, 2);
    REG(b, 6)[0] = 0x55;
    load(b, 7, 0, p, 2);
    REG(b, 7)[0] = 0x66;
    ST(b, 7)[M3VCPU_RV32_STATE_RETIRED] = 0x7FFFFFFF;
    upload(b);
    slice(b, 50);
    CHECK(REG(b, 0)[0] == 0x1234, "%s trap before any retire leaves the x0 slot untouched", g_label);
    expect_exit(b, 1, 0, "x0 writes ignored, reads 0");
    CHECK(REG(b, 1)[0] == 0, "%s x0 slot zeroed after a retire", g_label);
    expect_exit(b, 2, 9, "unknown status resumes as READY");
    expect_state(b, 3, M3VCPU_RV32_STATUS_TRAPPED, 5, 0, 0, 0, "trapped core skipped");
    expect_state(b, 4, M3VCPU_RV32_STATUS_HALTED, 0, 0, 0, 0, "halted core skipped");
    expect_exit(b, 5, 1, "yielded core resumes");
    expect_state(b, 5, M3VCPU_RV32_STATUS_HALTED, 0, 0, 12, 102, "yielded core resumes");
    CHECK(REG(b, 6)[0] == 0, "%s x0 slot zeroed when a trap follows a retire", g_label);
    CHECK(ST(b, 7)[M3VCPU_RV32_STATE_RETIRED] == MIN32, "%s RETIRED wraps like a Java int", g_label);
    batch_free(b);

    b = batch_new(2, 16);
    p[0] = ADDI(10, 0, 1);
    load(b, 0, 0, p, 1);
    REG(b, 0)[0] = 0x77;
    upload(b);
    slice(b, 0);
    expect_state(b, 0, M3VCPU_RV32_STATUS_YIELDED, 0, 0, 0, 0, "budget 0 yields");
    CHECK(REG(b, 0)[0] == 0, "%s budget 0 still zeroes the x0 slot", g_label);
    batch_free(b);
}

static void test_multicore(void)
{
    static const uint32_t locals[] = {0, 1, 64, 256};
    size_t li_;
    for (li_ = 0; li_ < sizeof locals / sizeof locals[0]; ++li_) {
        uint32_t saved = g_local;
        batch *b = batch_new(1000, 32);
        int32_t c;
        g_local = locals[li_];
        for (c = 0; c < 1000; ++c) {
            int32_t nn = c % 97;
            uint32_t p[7];
            p[0] = ADDI(10, 0, 0);
            p[1] = ADDI(11, 0, 1);
            p[2] = ADDI(12, 0, nn + 1);
            p[3] = ADD(10, 10, 11);
            p[4] = ADDI(11, 11, 1);
            p[5] = BLT(11, 12, -8);
            p[6] = XM3_HALT;
            load(b, c, 0, p, 7);
        }
        upload(b);
        slice(b, 1000);
        for (c = 0; c < 1000; ++c) {
            int32_t nn = c % 97;
            expect_exit(b, c, nn == 0 ? 1 : nn * (nn + 1) / 2, "multi-core sum");
        }
        g_local = saved;
        batch_free(b);
    }
    {
        uint64_t saved = g_global_extra;
        batch *b = batch_new(3, 16);
        uint32_t p[2];
        p[0] = ADDI(10, 0, 42);
        p[1] = XM3_HALT;
        load(b, 0, 0, p, 2);
        load(b, 1, 0, p, 2);
        load(b, 2, 0, p, 2);
        g_global_extra = 509; /* work-items past CORE_COUNT must return without touching state */
        upload(b);
        slice(b, 5);
        expect_exit(b, 2, 42, "global > cores guard");
        g_global_extra = saved;
        batch_free(b);
    }
}

/* ---- random differential ------------------------------------------------------------------ */
static uint64_t rnd(void)
{
    uint64_t z = (g_rng += 0x9E3779B97F4A7C15ull);
    z = (z ^ (z >> 30)) * 0xBF58476D1CE4E5B9ull;
    z = (z ^ (z >> 27)) * 0x94D049BB133111EBull;
    return z ^ (z >> 31);
}

static uint32_t below(uint32_t n) { return (uint32_t)(rnd() % n); }
static int32_t span(int32_t lo, int32_t hi) { return lo + (int32_t)below((uint32_t)(hi - lo + 1)); }

static uint32_t gen_insn(void)
{
    uint32_t k = below(100);
    uint32_t rd = below(32), a = below(32), bb = below(32), base = 1u + below(8);
    int32_t jump = span(-8, 8) * 4 + (below(8) == 0 ? 2 : 0);
    if (k < 14) {
        static const uint32_t f7s[4] = {0u, 0x20u, 1u, 1u};
        uint32_t f7 = below(20) == 0 ? below(128) : f7s[below(4)];
        uint32_t f3 = below(8);
        return rv_r(f7, bb, a, f3, rd, 0x33u);
    }
    if (k < 28) {
        uint32_t f3 = below(8);
        int32_t imm = span(-2048, 2047);
        if (f3 == 1u || f3 == 5u) {
            uint32_t f7 = below(10) == 0 ? below(128) : (f3 == 5u && below(2) ? 0x20u : 0u);
            imm = (int32_t)((f7 << 5) | below(32));
        }
        return rv_i(imm, a, f3, rd, 0x13u);
    }
    if (k < 42) {
        static const uint32_t f3s[5] = {0u, 1u, 2u, 4u, 5u};
        uint32_t f3 = below(10) == 0 ? below(8) : f3s[below(5)];
        int32_t imm = span(-32, 31);
        return rv_i(imm, base, f3, rd, 0x03u);
    }
    if (k < 54) {
        int32_t imm = span(-32, 31);
        uint32_t f3 = below(10) == 0 ? below(8) : below(3);
        return rv_s(imm, bb, base, f3);
    }
    if (k < 66) {
        return rv_b(jump, bb, a, below(8));
    }
    if (k < 70) {
        return JAL(rd, jump);
    }
    if (k < 74) {
        int32_t imm = span(-32, 31);
        uint32_t src = below(2) ? base : a;
        uint32_t f3 = below(8) == 0 ? 1u : 0u;
        return rv_i(imm, src, f3, rd, 0x67u);
    }
    if (k < 78) {
        uint32_t imm20 = below(1u << 20);
        return LUI(rd, imm20);
    }
    if (k < 81) {
        uint32_t imm20 = below(1u << 20);
        return AUIPC(rd, imm20);
    }
    if (k < 84) {
        uint32_t f3 = below(4) == 0 ? below(8) : below(2);
        uint32_t r = below(32);
        return 0x0Fu | (f3 << 12) | (r << 7);
    }
    if (k < 87) {
        static const uint32_t sys[4] = {ECALL, EBREAK, 0x00000473u, 0x30200073u};
        return sys[below(4)];
    }
    if (k < 91) {
        return XM3_HALT;
    }
    if (k < 96) {
        uint32_t f3 = below(8);
        return rv_r(1u, bb, a, f3, rd, 0x33u);
    }
    return (uint32_t)(rnd() >> 32);
}

static int32_t gen_value(int32_t mem_bytes)
{
    static const int32_t special[6] = {0, 1, -1, 0x7FFFFFFF, (int32_t)0x80000000u, 2};
    uint32_t k = below(100);
    if (k < 30) {
        return span(-16, 16);
    }
    if (k < 45) {
        return special[below(6)];
    }
    if (k < 60) {
        return span(0, mem_bytes - 1) & ~3;
    }
    return (int32_t)(uint32_t)(rnd() >> 32);
}

static void test_fuzz(uint64_t seed, int32_t cores, int32_t words, int32_t slices, int32_t budget)
{
    batch *b = batch_new(cores, words);
    int32_t mem_bytes = words * 4;
    int32_t c, r, s;
    size_t w;
    g_rng = seed;
    for (c = 0; c < cores; ++c) {
        int32_t *regs = REG(b, c);
        for (w = 0; w < (size_t)words; ++w) {
            b->mem[(size_t)c * (size_t)words + w] = w < 64u || below(4) == 0 ? (int32_t)gen_insn()
                                                                           : (int32_t)(uint32_t)(rnd() >> 32);
        }
        for (r = 0; r < NREG; ++r) {
            regs[r] = gen_value(mem_bytes);
        }
        for (r = 1; r <= 8; ++r) {
            uint32_t k = below(20);
            regs[r] = k < 14 ? (span(0, mem_bytes - 4) & ~3)
                             : (k < 18 ? span(0, mem_bytes - 1) : (k == 18 ? mem_bytes + span(-4, 4) : span(-8, -1)));
        }
        if (below(8) == 0) {
            regs[0] = 0;
        }
        if (below(10) == 0) {
            ST(b, c)[M3VCPU_RV32_STATE_PC] = below(4) == 0 ? span(-8, mem_bytes + 8) : (span(0, 63) * 4);
        }
    }
    upload(b);
    for (s = 0; s < slices; ++s) {
        slice(b, budget);
    }
    batch_free(b);
}

static void coverage_report(void)
{
    int op, t;
    uint64_t total = 0;
    int missing = 0;
    printf("reference coverage (retired instructions per op):");
    for (op = 0; op < RV_OP_COUNT; ++op) {
        printf(" %s=%llu", rv32_op_name(op), (unsigned long long)g_cov.retired[op]);
        total += g_cov.retired[op];
        if (g_cov.retired[op] == 0u) {
            ++missing;
        }
        CHECK(g_cov.retired[op] > 0u, "coverage: %s never retired", rv32_op_name(op));
    }
    printf("\ntraps by cause:");
    for (t = 0; t < 9; ++t) {
        printf(" %d=%llu", t, (unsigned long long)g_cov.traps[t]);
        CHECK(g_cov.traps[t] > 0u, "coverage: trap cause %d never raised", t);
    }
    printf("\ntaken branches=%llu, total retired=%llu, ops never retired=%d\n",
           (unsigned long long)g_cov.taken_branches, (unsigned long long)total, missing);
}

/* ---- driver ------------------------------------------------------------------------------- */
static void run_suite(void)
{
    uint64_t t0 = m3_now_ms();
    long before = m3t_failures;
    int32_t rc = m3vcpu_kernel_builtin(g_s, M3VCPU_RV32_KERNEL_ENTRY, &g_k);
    CHECK(rc == M3VCPU_OK, "%s kernel_builtin: %s", g_label, m3vcpu_last_error(g_s));
    if (rc != M3VCPU_OK) {
        return;
    }
    test_alu();
    test_mext();
    test_mem();
    test_branches();
    test_halt();
    test_traps();
    test_slicing();
    test_x0_and_status();
    test_multicore();
    test_fuzz(0x5EED0001ull, 777, 256, 6, 61);
    test_fuzz(0x5EED0002ull, 513, 128, 4, 200);
    test_fuzz(0x5EED0003ull, 1024, 64, 8, 17);
    printf("[%s] suite done in %llu ms, failures %ld\n", g_label, (unsigned long long)(m3_now_ms() - t0),
           m3t_failures - before);
    fflush(stdout);
}

static void run_open(const char *selector, int32_t device, const m3vcpu_ctx_opts *opts, const char *label)
{
    m3vcpu_device_info info;
    int32_t rc = m3vcpu_open(selector, device, opts, &g_s);
    if (rc == M3VCPU_E_UNAVAILABLE && opts != NULL && opts->isa != M3VCPU_ISA_AUTO) {
        printf("[%s] SKIP: %s\n", label, m3vcpu_last_error(NULL));
        return;
    }
    CHECK(rc == M3VCPU_OK, "open %s: %s", label, m3vcpu_last_error(NULL));
    if (rc != M3VCPU_OK) {
        return;
    }
    (void)m3vcpu_session_device(g_s, NULL, &info);
    m3_format(g_label, sizeof g_label, "%s | %s | device best isa %s", label, info.name, info.isa);
    printf("[%s] device %s (%s, %s)\n", label, info.name, info.vendor, info.version);
    run_suite();
    m3vcpu_close(g_s);
    g_s = NULL;
    g_k = NULL;
}

int main(int argc, char **argv)
{
    const char *backend = argc > 1 ? argv[1] : "cpu";
    if (strcmp(backend, "cpu") == 0) {
        static const struct {
            uint32_t isa, threads;
            const char *label;
        } configs[] = {
            {M3VCPU_ISA_SCALAR, 1, "cpu isa=scalar threads=1"},
            {M3VCPU_ISA_SCALAR, 0, "cpu isa=scalar threads=all"},
            {M3VCPU_ISA_AVX2, 0, "cpu isa=avx2 threads=all"},
            {M3VCPU_ISA_AVX2, 5, "cpu isa=avx2 threads=5"},
            {M3VCPU_ISA_AVX512, 0, "cpu isa=avx512 threads=all"},
        };
        size_t i;
        for (i = 0; i < sizeof configs / sizeof configs[0]; ++i) {
            m3vcpu_ctx_opts opts;
            memset(&opts, 0, sizeof opts);
            opts.struct_size = sizeof opts;
            opts.isa = configs[i].isa;
            opts.threads = configs[i].threads;
            run_open("cpu", 0, &opts, configs[i].label);
        }
    } else {
        int32_t index = m3vcpu_backend_index(backend);
        m3vcpu_probe probe;
        int32_t d, count;
        if (index < 0) {
            printf("SKIP: backend '%s' not registered\n", backend);
            return 77;
        }
        if (m3vcpu_backend_probe(index, &probe) != M3VCPU_OK || !probe.available) {
            printf("SKIP: backend '%s' unavailable: %s\n", backend, probe.reason);
            return 77;
        }
        printf("backend %s (%s): %s\n", backend, m3vcpu_backend_origin(index), probe.reason);
        count = m3vcpu_backend_at(index)->device_count();
        for (d = 0; d < count; ++d) {
            char label[64];
            m3_format(label, sizeof label, "%s device %d", backend, (int)d);
            run_open(backend, d, NULL, label);
        }
    }
    coverage_report();
    return m3t_summary("m3vcpu_test_rv32im");
}
