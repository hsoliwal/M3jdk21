/* SPDX-License-Identifier: Apache-2.0 */
#include "m3_precompute_signals.h"

#define HAS_BRACE UINT32_C(1)
#define HAS_PAREN (UINT32_C(1) << 1U)
#define HAS_SEMICOLON (UINT32_C(1) << 2U)
#define HAS_BACKSLASH (UINT32_C(1) << 3U)
#define HAS_REGEX_META (UINT32_C(1) << 4U)
#define HAS_ARROW (UINT32_C(1) << 5U)
#define HAS_METHOD_REFERENCE (UINT32_C(1) << 6U)
#define HAS_ASSIGN (UINT32_C(1) << 7U)
#define HAS_COMPARISON (UINT32_C(1) << 8U)
#define HAS_QUANTIFIER (UINT32_C(1) << 9U)
#define HAS_CLASS_BRACKET (UINT32_C(1) << 10U)
#define HAS_DOT (UINT32_C(1) << 11U)
#define HAS_COMMA (UINT32_C(1) << 12U)
#define HAS_COLON (UINT32_C(1) << 13U)
#define HAS_AMP_PIPE (UINT32_C(1) << 14U)
#define HAS_WHITESPACE (UINT32_C(1) << 15U)

static uint64_t lane(unsigned value, unsigned shift) {
    uint64_t bounded = value > 255U ? UINT64_C(255) : (uint64_t)value;
    return bounded << shift;
}

static int java_whitespace(uint16_t value) {
    return value == UINT16_C(0x0009)
            || value == UINT16_C(0x000a)
            || value == UINT16_C(0x000b)
            || value == UINT16_C(0x000c)
            || value == UINT16_C(0x000d)
            || value == UINT16_C(0x001c)
            || value == UINT16_C(0x001d)
            || value == UINT16_C(0x001e)
            || value == UINT16_C(0x001f)
            || value == UINT16_C(0x0020)
            || value == UINT16_C(0x1680)
            || (value >= UINT16_C(0x2000) && value <= UINT16_C(0x2006))
            || (value >= UINT16_C(0x2008) && value <= UINT16_C(0x200a))
            || value == UINT16_C(0x2028)
            || value == UINT16_C(0x2029)
            || value == UINT16_C(0x205f)
            || value == UINT16_C(0x3000);
}

uint64_t m3_precompute_code_text_signal(const uint16_t *units, size_t length) {
    uint32_t flags = 0U;
    unsigned semicolons = 0U;
    unsigned braces = 0U;
    unsigned parens = 0U;
    unsigned backslashes = 0U;
    unsigned regex_meta = 0U;
    unsigned punctuation = 0U;

    if (length != 0U && units == NULL) return UINT64_MAX;

    for (size_t index = 0U; index < length; index++) {
        uint16_t unit = units[index];
        switch (unit) {
            case (uint16_t)'{':
            case (uint16_t)'}':
                flags |= HAS_BRACE;
                braces++;
                regex_meta++;
                punctuation++;
                break;
            case (uint16_t)'(':
            case (uint16_t)')':
                flags |= HAS_PAREN;
                parens++;
                regex_meta++;
                punctuation++;
                break;
            case (uint16_t)';':
                flags |= HAS_SEMICOLON;
                semicolons++;
                punctuation++;
                break;
            case (uint16_t)'\\':
                flags |= HAS_BACKSLASH;
                backslashes++;
                punctuation++;
                break;
            case (uint16_t)'.':
                flags |= HAS_REGEX_META | HAS_DOT;
                regex_meta++;
                punctuation++;
                break;
            case (uint16_t)'*':
            case (uint16_t)'+':
            case (uint16_t)'?':
                flags |= HAS_REGEX_META | HAS_QUANTIFIER;
                regex_meta++;
                punctuation++;
                break;
            case (uint16_t)'|':
                flags |= HAS_REGEX_META | HAS_AMP_PIPE;
                regex_meta++;
                punctuation++;
                break;
            case (uint16_t)'^':
            case (uint16_t)'$':
                flags |= HAS_REGEX_META;
                regex_meta++;
                punctuation++;
                break;
            case (uint16_t)'[':
            case (uint16_t)']':
                flags |= HAS_REGEX_META | HAS_CLASS_BRACKET;
                regex_meta++;
                punctuation++;
                break;
            case (uint16_t)'=':
                flags |= HAS_ASSIGN;
                punctuation++;
                break;
            case (uint16_t)'<':
            case (uint16_t)'>':
            case (uint16_t)'!':
                flags |= HAS_COMPARISON;
                punctuation++;
                break;
            case (uint16_t)',':
                flags |= HAS_COMMA;
                punctuation++;
                break;
            case (uint16_t)':':
                flags |= HAS_COLON;
                punctuation++;
                if (index + 1U < length && units[index + 1U] == (uint16_t)':') {
                    flags |= HAS_METHOD_REFERENCE;
                }
                break;
            case (uint16_t)'&':
                flags |= HAS_AMP_PIPE;
                punctuation++;
                break;
            case (uint16_t)'-':
                punctuation++;
                if (index + 1U < length && units[index + 1U] == (uint16_t)'>') {
                    flags |= HAS_ARROW;
                }
                break;
            default:
                if (java_whitespace(unit)) flags |= HAS_WHITESPACE;
                break;
        }
    }

    return (uint64_t)flags
            | lane(semicolons, 16U)
            | lane(braces, 24U)
            | lane(parens, 32U)
            | lane(backslashes, 40U)
            | lane(regex_meta, 48U)
            | lane(punctuation, 56U);
}
