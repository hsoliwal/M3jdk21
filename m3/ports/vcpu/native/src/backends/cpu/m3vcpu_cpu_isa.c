/* SPDX-License-Identifier: Apache-2.0 */
/*
 * m3vcpu_cpu_isa.c - x86-64 micro-architecture level detection. Feature bits per the Intel SDM
 * CPUID leaves 1, 7 and 0x80000001, gated on OS-enabled register state (XCR0). Pattern donor
 * (no code copied): Highway runtime dispatch (targets chosen by CPUID at run time).
 */
#include "m3vcpu_cpu_isa.h"
#include "m3vcpu.h"

#include <string.h>

#if defined(_M_X64) || defined(__x86_64__)
#define M3_X86_64 1
#if defined(_MSC_VER)
#include <intrin.h>
static void m3_cpuid(uint32_t leaf, uint32_t sub, uint32_t r[4])
{
    int v[4];
    __cpuidex(v, (int)leaf, (int)sub);
    r[0] = (uint32_t)v[0];
    r[1] = (uint32_t)v[1];
    r[2] = (uint32_t)v[2];
    r[3] = (uint32_t)v[3];
}
#else
#include <cpuid.h>
static void m3_cpuid(uint32_t leaf, uint32_t sub, uint32_t r[4])
{
    unsigned a = 0, b = 0, c = 0, d = 0;
    __cpuid_count(leaf, sub, a, b, c, d);
    r[0] = a;
    r[1] = b;
    r[2] = c;
    r[3] = d;
}
#endif
#if defined(_MSC_VER) && !defined(__clang__)
#include <immintrin.h>
static uint64_t m3_xgetbv0(void) { return (uint64_t)_xgetbv(0); }
#else
static uint64_t m3_xgetbv0(void)
{
    uint32_t lo, hi;
    __asm__ __volatile__("xgetbv" : "=a"(lo), "=d"(hi) : "c"(0));
    return ((uint64_t)hi << 32) | lo;
}
#endif
#endif

#define M3_BIT(reg, n) (((reg) >> (n)) & 1u)

void m3_cpu_isa_detect(m3_cpu_isa_info *out)
{
    memset(out, 0, sizeof *out);
#if defined(M3_X86_64)
    {
        uint32_t r0[4], r1[4] = {0, 0, 0, 0}, r7[4] = {0, 0, 0, 0}, rx[4], rx1[4] = {0, 0, 0, 0};
        uint64_t xcr0 = 0;
        int os_ymm, os_zmm;
        size_t start = 0;
        m3_cpuid(0, 0, r0);
        memcpy(out->vendor, &r0[1], 4);
        memcpy(out->vendor + 4, &r0[3], 4);
        memcpy(out->vendor + 8, &r0[2], 4);
        if (r0[0] >= 1u) {
            m3_cpuid(1, 0, r1);
        }
        if (r0[0] >= 7u) {
            m3_cpuid(7, 0, r7);
        }
        m3_cpuid(0x80000000u, 0, rx);
        if (rx[0] >= 0x80000001u) {
            m3_cpuid(0x80000001u, 0, rx1);
        }
        if (rx[0] >= 0x80000004u) {
            uint32_t part[4];
            uint32_t leaf;
            for (leaf = 0; leaf < 3u; ++leaf) {
                m3_cpuid(0x80000002u + leaf, 0, part);
                memcpy(out->brand + leaf * 16u, part, 16);
            }
            out->brand[48] = '\0';
            while (out->brand[start] == ' ') {
                ++start;
            }
            memmove(out->brand, out->brand + start, strlen(out->brand + start) + 1u);
        }
        if (M3_BIT(r1[2], 27)) {
            xcr0 = m3_xgetbv0();
        }
        os_ymm = (xcr0 & 0x6u) == 0x6u;
        os_zmm = os_ymm && (xcr0 & 0xE0u) == 0xE0u;
        out->v3 = os_ymm && M3_BIT(r1[2], 28) && M3_BIT(r7[1], 5) && M3_BIT(r1[2], 12) && M3_BIT(r7[1], 3)
                  && M3_BIT(r7[1], 8) && M3_BIT(rx1[2], 5) && M3_BIT(r1[2], 22) && M3_BIT(r1[2], 29);
        out->v4 = out->v3 && os_zmm && M3_BIT(r7[1], 16) && M3_BIT(r7[1], 17) && M3_BIT(r7[1], 28)
                  && M3_BIT(r7[1], 30) && M3_BIT(r7[1], 31);
    }
#else
    memcpy(out->vendor, "generic", 8);
    memcpy(out->brand, "generic CPU", 12);
#endif
}

const char *m3_cpu_isa_name(uint32_t isa)
{
    switch (isa) {
    case M3VCPU_ISA_SCALAR:
        return "scalar";
    case M3VCPU_ISA_AVX2:
        return "avx2";
    case M3VCPU_ISA_AVX512:
        return "avx512";
    default:
        return "auto";
    }
}
