/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * GPLv2 with the Classpath exception.
 *
 * Target-owned native counterpart of java.lang.M3StringRegexDp. No retained JNI
 * pointer, copied text buffer, native allocation, foreign runtime or new interner.
 */
#include <jni.h>
#include <stdint.h>
#include <stddef.h>
#include "java_lang_M3StringRegexDp.h"

#define M3_REGEX_STAR UINT32_C(65536)
#define M3_REGEX_MAX_ATOMS 32
#define M3_REGEX_MAX_UNITS 8192

static jlong refuse(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) {
        (void) (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
    return (jlong) -1;
}

static int ascii_atom(uint32_t unit) {
    return unit == (uint32_t) '.' || (unit >= (uint32_t) 'a' && unit <= (uint32_t) 'z')
            || (unit >= (uint32_t) 'A' && unit <= (uint32_t) 'Z')
            || (unit >= (uint32_t) '0' && unit <= (uint32_t) '9');
}

static int line_terminator(uint32_t unit) {
    return unit == 10U || unit == 13U || unit == 133U || unit == 8232U || unit == 8233U;
}

/* Input is nonzero; all consuming state bits are in [0,31]. No compiler-specific intrinsic. */
static unsigned int lowest_token(uint32_t bits) {
    unsigned int token = 0U;
    if ((bits & UINT32_C(0xffff)) == 0U) { bits >>= 16U; token += 16U; }
    if ((bits & UINT32_C(0xff)) == 0U) { bits >>= 8U; token += 8U; }
    if ((bits & UINT32_C(0xf)) == 0U) { bits >>= 4U; token += 4U; }
    if ((bits & UINT32_C(0x3)) == 0U) { bits >>= 2U; token += 2U; }
    return token + ((bits & UINT32_C(1)) == 0U ? 1U : 0U);
}

JNIEXPORT jlong JNICALL Java_java_lang_M3StringRegexDp_advance0(
        JNIEnv *env, jclass ignored, jlong address, jint capacity, jint start, jint length,
        jint width, jboolean big_endian, jlong incoming, jintArray atom_array,
        jlongArray closure_array) {
    jint atoms[M3_REGEX_MAX_ATOMS];
    jlong closure[M3_REGEX_MAX_ATOMS + 1];
    jsize count;
    uint64_t valid;
    uint64_t state;
    uint64_t raw_address;
    uintptr_t begin;
    uint64_t skip;
    uint64_t bytes;
    const unsigned char *text;
    int i;
    (void) ignored;
    if (atom_array == NULL || closure_array == NULL) return refuse(env, "missing regex plan");
    count = (*env)->GetArrayLength(env, atom_array);
    if (count < 1 || count > M3_REGEX_MAX_ATOMS
            || (*env)->GetArrayLength(env, closure_array) != count + 1) {
        return refuse(env, "invalid regex plan shape");
    }
    if (capacity < 0 || start < 0 || start > capacity || length < 0
            || length > capacity - start || length > M3_REGEX_MAX_UNITS
            || (width != 1 && width != 2)) {
        return refuse(env, "invalid canonical regex range");
    }
    (*env)->GetIntArrayRegion(env, atom_array, 0, count, atoms);
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    (*env)->GetLongArrayRegion(env, closure_array, 0, count + 1, closure);
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    valid = (UINT64_C(1) << (unsigned int) (count + 1)) - UINT64_C(1);
    if (incoming < 0 || (((uint64_t) incoming) & ~valid) != 0) {
        return refuse(env, "invalid incoming regex frontier");
    }
    if ((uint64_t) closure[count] != (UINT64_C(1) << (unsigned int) count)) {
        return refuse(env, "invalid accepting closure");
    }
    for (i = (int) count - 1; i >= 0; i--) {
        uint32_t atom = (uint32_t) atoms[i];
        uint32_t unit = atom & UINT32_C(65535);
        uint64_t expected = UINT64_C(1) << (unsigned int) i;
        if (!ascii_atom(unit) || (atom & ~(M3_REGEX_STAR | UINT32_C(65535))) != 0) {
            return refuse(env, "invalid regex opcode");
        }
        if ((atom & M3_REGEX_STAR) != 0) expected |= (uint64_t) closure[i + 1];
        if ((uint64_t) closure[i] != expected) return refuse(env, "invalid epsilon closure");
    }
    state = (uint64_t) incoming;
    // All published frontiers are epsilon closed. Refuse forged/noncanonical state packets.
    {
        uint64_t closed = 0;
        for (i = 0; i <= (int) count; i++) {
            if ((state & (UINT64_C(1) << (unsigned int) i)) != 0) closed |= (uint64_t) closure[i];
        }
        if (closed != state) return refuse(env, "incoming frontier is not closed");
    }
    if (length == 0) return incoming;
    raw_address = (uint64_t) address;
    skip = (uint64_t) (unsigned int) start * (uint64_t) (unsigned int) width;
    bytes = (uint64_t) (unsigned int) length * (uint64_t) (unsigned int) width;
    if (address <= 0 || raw_address > (uint64_t) UINTPTR_MAX
            || skip > (uint64_t) UINTPTR_MAX - raw_address
            || bytes - UINT64_C(1) > (uint64_t) UINTPTR_MAX - raw_address - skip) {
        return refuse(env, "invalid native regex address");
    }
    begin = (uintptr_t) raw_address + (uintptr_t) skip;
    text = (const unsigned char *) begin;
    /* Physical byte storage cannot hide a surrogate after a dead frontier. */
    if (width == 1) {
        /* Keep the historical small-plan loop: bit-index dispatch costs more at this size. */
        if (count < 8) {
            for (i = 0; i < length && state != UINT64_C(0);) {
                int fence = length - i < 64 ? length : i + 64;
                for (; i < fence; i++) {
                    uint32_t unit = (uint32_t) text[(size_t) i];
                    uint64_t next = 0;
                    int token;
                    for (token = 0; token < (int) count; token++) {
                        uint32_t atom = (uint32_t) atoms[token];
                        uint32_t required = atom & UINT32_C(65535);
                        int accepts = required == (uint32_t) '.' ? !line_terminator(unit) : required == unit;
                        if (accepts && (state & (UINT64_C(1) << (unsigned int) token)) != 0) {
                            int destination = (atom & M3_REGEX_STAR) != 0 ? token : token + 1;
                            next |= (uint64_t) closure[destination];
                        }
                    }
                    state = next;
                }
            }
            return (jlong) state;
        }
        for (i = 0; i < length && state != UINT64_C(0);) {
            int fence = length - i < 64 ? length : i + 64;
            for (; i < fence; i++) {
                uint32_t unit = (uint32_t) text[(size_t) i];
                uint64_t next = 0;
                int token;
                uint64_t active = state & (valid >> 1U); /* Exclude the accepting state. */
                uint64_t rest = active & (active - UINT64_C(1));
                if (active != 0U && (rest & (rest - UINT64_C(1))) == 0U) {
                    /* At most two consuming states: inspect only those tokens. */
                    do {
                        unsigned int sparse = lowest_token((uint32_t) active);
                        uint32_t atom = (uint32_t) atoms[sparse];
                        uint32_t required = atom & UINT32_C(65535);
                        int accepts = required == (uint32_t) '.' ? !line_terminator(unit) : required == unit;
                        if (accepts) {
                            unsigned int destination = (atom & M3_REGEX_STAR) != 0 ? sparse : sparse + 1U;
                            next |= (uint64_t) closure[destination];
                        }
                        active &= active - UINT64_C(1);
                    } while (active != 0U);
                } else if (active != 0U) {
                    /* Dense frontiers retain the original bounded token scan. */
                    for (token = 0; token < (int) count; token++) {
                        uint32_t atom = (uint32_t) atoms[token];
                        uint32_t required = atom & UINT32_C(65535);
                        int accepts = required == (uint32_t) '.' ? !line_terminator(unit) : required == unit;
                        if (accepts && (state & (UINT64_C(1) << (unsigned int) token)) != 0) {
                            int destination = (atom & M3_REGEX_STAR) != 0 ? token : token + 1;
                            next |= (uint64_t) closure[destination];
                        }
                    }
                }
                state = next;
            }
        }
        return (jlong) state;
    }
    /* Keep the historical small-plan loop: bit-index dispatch costs more at this size. */
    if (count < 8) {
        for (i = 0; i < length; i++) {
            size_t at = (size_t) i * (size_t) width;
            uint32_t unit = (uint32_t) text[at];
            uint64_t next = 0;
            int token;
            if (width == 2) {
                uint32_t second = (uint32_t) text[at + 1U];
                unit = big_endian ? (unit << 8U) | second : (second << 8U) | unit;
            }
            if (unit >= UINT32_C(0xd800) && unit <= UINT32_C(0xdfff)) return (jlong) -1;
            // Even a dead frontier reads the remaining units to retain surrogate refusal.
            for (token = 0; token < (int) count; token++) {
                uint32_t atom = (uint32_t) atoms[token];
                uint32_t required = atom & UINT32_C(65535);
                int accepts = required == (uint32_t) '.' ? !line_terminator(unit) : required == unit;
                if (accepts && (state & (UINT64_C(1) << (unsigned int) token)) != 0) {
                    int destination = (atom & M3_REGEX_STAR) != 0 ? token : token + 1;
                    next |= (uint64_t) closure[destination];
                }
            }
            state = next;
        }
        return (jlong) state;
    }
    for (i = 0; i < length; i++) {
        size_t at = (size_t) i * (size_t) width;
        uint32_t unit = (uint32_t) text[at];
        uint64_t next = 0;
        int token;
        if (width == 2) {
            uint32_t second = (uint32_t) text[at + 1U];
            unit = big_endian ? (unit << 8U) | second : (second << 8U) | unit;
        }
        if (unit >= UINT32_C(0xd800) && unit <= UINT32_C(0xdfff)) return (jlong) -1;
        // Even a dead frontier reads the remaining units to retain surrogate refusal.
        uint64_t active = state & (valid >> 1U); /* Exclude the accepting state. */
        uint64_t rest = active & (active - UINT64_C(1));
        if (active != 0U && (rest & (rest - UINT64_C(1))) == 0U) {
            /* At most two consuming states: inspect only those tokens. */
            do {
                unsigned int sparse = lowest_token((uint32_t) active);
                uint32_t atom = (uint32_t) atoms[sparse];
                uint32_t required = atom & UINT32_C(65535);
                int accepts = required == (uint32_t) '.' ? !line_terminator(unit) : required == unit;
                if (accepts) {
                    unsigned int destination = (atom & M3_REGEX_STAR) != 0 ? sparse : sparse + 1U;
                    next |= (uint64_t) closure[destination];
                }
                active &= active - UINT64_C(1);
            } while (active != 0U);
        } else if (active != 0U) {
            /* Dense frontiers retain the original bounded token scan. */
            for (token = 0; token < (int) count; token++) {
                uint32_t atom = (uint32_t) atoms[token];
                uint32_t required = atom & UINT32_C(65535);
                int accepts = required == (uint32_t) '.' ? !line_terminator(unit) : required == unit;
                if (accepts && (state & (UINT64_C(1) << (unsigned int) token)) != 0) {
                    int destination = (atom & M3_REGEX_STAR) != 0 ? token : token + 1;
                    next |= (uint64_t) closure[destination];
                }
            }
        }
        state = next;
    }
    return (jlong) state;
}
