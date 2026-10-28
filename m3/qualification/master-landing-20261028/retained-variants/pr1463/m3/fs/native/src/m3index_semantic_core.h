// SPDX-License-Identifier: Apache-2.0
#ifndef M3FS_SEMANTIC_CORE_H
#define M3FS_SEMANTIC_CORE_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

#define M3_MINDEX_IMAGE_MAGIC UINT64_C(0x4d334d4944583031)
#define M3_MINDEX_IMAGE_FORMAT 1U
#define M3_MINDEX_IMAGE_HEADER_BYTES 64U

#define M3_MINDEX_REGEX_OP_CHAR 1U
#define M3_MINDEX_REGEX_OP_SPLIT 2U
#define M3_MINDEX_REGEX_OP_JUMP 3U
#define M3_MINDEX_REGEX_OP_ASSERT_START 4U
#define M3_MINDEX_REGEX_OP_ASSERT_END 5U
#define M3_MINDEX_REGEX_OP_MATCH 6U
#define M3_MINDEX_REGEX_OP_ASSERT_WORD_BOUNDARY 7U
#define M3_MINDEX_REGEX_OP_ASSERT_NOT_WORD_BOUNDARY 8U

#define M3_MINDEX_REGEX_MATCHES 0
#define M3_MINDEX_REGEX_FIND 1
#define M3_MINDEX_REGEX_LOOKING_AT 2

#define M3_MINDEX_REGEX_OK 0
#define M3_MINDEX_REGEX_UNSUPPORTED 1
#define M3_MINDEX_REGEX_INVALID -1
#define M3_MINDEX_REGEX_NO_MEMORY -2

typedef struct m3_mindex_view {
  const uint8_t *data;
  size_t bytes;
  uint32_t value_count;
  uint32_t token_count;
  uint32_t utf16_count;
  uint32_t token_starts;
  uint32_t utf16_starts;
  uint32_t token_ids;
  uint32_t utf16_units;
  uint32_t java_hashes;
  uint32_t signals;
} m3_mindex_view;

typedef struct m3_mindex_regex_program {
  uint32_t state_count;
  uint32_t class_count;
  uint32_t range_count;
  uint32_t start_state;
  uint32_t match_state;
  int32_t min_length;
  int32_t max_length;
  uint64_t required_presence64;
  int may_consume_surrogate_unit;

  uint8_t *op;
  int32_t *out1;
  int32_t *out2;
  int32_t *class_id;
  uint64_t *ascii_low;
  uint64_t *ascii_high;
  int32_t *range_offset;
  int32_t *range_size;
  uint16_t *range_low;
  uint16_t *range_high;

  uint64_t *active;
  uint64_t *next;
  uint32_t *mark;
  uint32_t *stack;
  uint32_t words;
  uint32_t stamp;
} m3_mindex_regex_program;

int m3_mindex_open(
    const void *data,
    size_t bytes,
    m3_mindex_view *out_view);

uint32_t m3_mindex_value_token_count(
    const m3_mindex_view *view,
    uint32_t value);

uint64_t m3_mindex_value_token(
    const m3_mindex_view *view,
    uint32_t value,
    uint32_t ordinal);

uint32_t m3_mindex_value_utf16_length(
    const m3_mindex_view *view,
    uint32_t value);

uint16_t m3_mindex_value_char(
    const m3_mindex_view *view,
    uint32_t value,
    uint32_t ordinal);

int32_t m3_mindex_value_java_hash(
    const m3_mindex_view *view,
    uint32_t value);

uint64_t m3_mindex_value_signal64(
    const m3_mindex_view *view,
    uint32_t value);

int m3_mindex_value_equals(
    const m3_mindex_view *view,
    uint32_t left,
    uint32_t right);

int m3_mindex_value_compare(
    const m3_mindex_view *view,
    uint32_t left,
    uint32_t right);

int32_t m3_mindex_token_index_of(
    const m3_mindex_view *view,
    uint32_t value,
    const uint64_t *needle,
    uint32_t needle_count,
    uint32_t from_token);

int m3_mindex_regex_program_init(
    m3_mindex_regex_program *program,
    const uint8_t *op,
    const int32_t *out1,
    const int32_t *out2,
    const int32_t *class_id,
    uint32_t state_count,
    const uint64_t *ascii_low,
    const uint64_t *ascii_high,
    const int32_t *range_offset,
    const int32_t *range_size,
    uint32_t class_count,
    const uint16_t *range_low,
    const uint16_t *range_high,
    uint32_t range_count,
    uint32_t start_state,
    uint32_t match_state,
    int32_t min_length,
    int32_t max_length,
    uint64_t required_presence64,
    int may_consume_surrogate_unit);

void m3_mindex_regex_program_destroy(m3_mindex_regex_program *program);

/**
 * Executes one capture-free program against one MIndex value.
 *
 * Returns M3_MINDEX_REGEX_OK and writes 0/1 to out_match, M3_MINDEX_REGEX_UNSUPPORTED for a
 * surrogate-sensitive program/value pair, or a negative status.
 */
int m3_mindex_regex_execute(
    m3_mindex_regex_program *program,
    const m3_mindex_view *view,
    uint32_t value,
    int mode,
    int *out_match);

#ifdef __cplusplus
}
#endif

#endif
