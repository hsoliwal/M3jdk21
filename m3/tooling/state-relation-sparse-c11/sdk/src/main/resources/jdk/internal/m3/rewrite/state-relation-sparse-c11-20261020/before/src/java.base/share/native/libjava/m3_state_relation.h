/* M3JDK21 adaptation: C/JNI/header identifiers only; Synexia remains the source owner. */
/* SPDX-License-Identifier: Apache-2.0 */
#ifndef M3JDK_INTERNAL_STATE_RELATION_H
#define M3JDK_INTERNAL_STATE_RELATION_H
#include <stddef.h>
#include <stdint.h>
/* Ordered Boolean relation composition; output may alias either input.
 * Returns 0 on success, -1 for invalid pointers, state count, or state bits.
 * All buffers must expose at least states uint64_t elements. */
int m3_state_relation_compose(const uint64_t *left, const uint64_t *right,
                                 uint64_t *output, size_t states);
/* Nonnegative repetition; count zero yields identity after validating the input.
 * Output may alias (including partially overlap) rows. On invalid input output is unchanged.
 * Uses bounded stack rows and O(states^2 log(count + 1)) work, never expands count copies. */
int m3_state_relation_repeat(const uint64_t *rows, uint64_t *output,
                                size_t states, uint64_t count);
#endif
