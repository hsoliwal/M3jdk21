/* SPDX-License-Identifier: Apache-2.0 */
#ifndef M3_ARRAYS_H
#define M3_ARRAYS_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

int m3_compare_utf16(
    const uint16_t *left,
    size_t left_start,
    const uint16_t *right,
    size_t right_start,
    size_t count);

#ifdef __cplusplus
}
#endif

#endif
