/* SPDX-License-Identifier: Apache-2.0 */
/* m3vcpu_cpu_isa.h - runtime x86-64 ISA level detection (CPUID + XGETBV) for kernel dispatch. */
#ifndef M3VCPU_CPU_ISA_H
#define M3VCPU_CPU_ISA_H

#include <stdint.h>

typedef struct m3_cpu_isa_info {
    int v3;              /* AVX2 FMA BMI1 BMI2 LZCNT MOVBE F16C + OS YMM state  */
    int v4;              /* v3 + AVX-512 F BW CD DQ VL + OS ZMM/opmask state   */
    char vendor[16];
    char brand[64];
} m3_cpu_isa_info;

void m3_cpu_isa_detect(m3_cpu_isa_info *out);
const char *m3_cpu_isa_name(uint32_t isa);

#endif /* M3VCPU_CPU_ISA_H */
