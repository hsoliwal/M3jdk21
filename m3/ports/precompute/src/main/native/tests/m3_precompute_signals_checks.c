/* SPDX-License-Identifier: Apache-2.0 */
#include "m3_precompute_signals.h"

/* The checks ARE the test: keep assert() live in Release builds (-DNDEBUG), otherwise every
   assertion compiles out and -Werror rejects the then-unused helper and results. */
#undef NDEBUG
#include <assert.h>
#include <stdint.h>

static unsigned lane(uint64_t packed, unsigned shift) {
    return (unsigned)((packed >> shift) & UINT64_C(0xff));
}

int main(void) {
    const uint16_t code[] = {
        'w','h','i','l','e',' ','(','x','!','=','0',')',' ','{','x','&','=','x','-','1',';','}'
    };
    uint64_t packed =
            m3_precompute_code_text_signal(code, sizeof(code) / sizeof(code[0]));
    assert((packed & UINT64_C(1)) != 0U);
    assert((packed & (UINT64_C(1) << 1U)) != 0U);
    assert((packed & (UINT64_C(1) << 2U)) != 0U);
    assert((packed & (UINT64_C(1) << 7U)) != 0U);
    assert((packed & (UINT64_C(1) << 8U)) != 0U);
    assert(lane(packed, 16U) == 1U);
    assert(lane(packed, 24U) == 2U);
    assert(lane(packed, 32U) == 2U);

    const uint16_t regex[] = {'^','[','a','-','z',']','+','\\','d','{','2','}','$'};
    packed = m3_precompute_code_text_signal(regex, sizeof(regex) / sizeof(regex[0]));
    assert((packed & (UINT64_C(1) << 3U)) != 0U);
    assert((packed & (UINT64_C(1) << 4U)) != 0U);
    assert((packed & (UINT64_C(1) << 9U)) != 0U);
    assert((packed & (UINT64_C(1) << 10U)) != 0U);
    assert(lane(packed, 40U) == 1U);
    assert(lane(packed, 48U) >= 6U);

    const uint16_t whitespace[] = {UINT16_C(0x1680), UINT16_C(0x2003), UINT16_C(0x3000)};
    packed = m3_precompute_code_text_signal(
            whitespace, sizeof(whitespace) / sizeof(whitespace[0]));
    assert((packed & (UINT64_C(1) << 15U)) != 0U);

    assert(m3_precompute_code_text_signal(NULL, 0U) == UINT64_C(0));
    assert(m3_precompute_code_text_signal(NULL, 1U) == UINT64_MAX);
    return 0;
}
